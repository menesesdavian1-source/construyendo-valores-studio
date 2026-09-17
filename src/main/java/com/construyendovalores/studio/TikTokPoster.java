package com.construyendovalores.studio;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Consumer;

/**
 * Publicación automática en TikTok vía Content Posting API (oficial).
 *
 * Requisito previo: crear una app en https://developers.tiktok.com, solicitar el permiso
 * "Content Posting API" (aprobado por TikTok) y poner sus credenciales en la configuración:
 *   tiktok.clientKey, tiktok.clientSecret, tiktok.handle
 * En ese momento ejecuta --tiktok-auth y autoriza con la cuenta @historiasrea28.
 *
 * El texto de la solicitud a TikTok está en SOLICITUD_TIKTOK.txt.
 */
public final class TikTokPoster {

    private static final String BASE = "https://open.tiktokapis.com/";
    private static final String DEVICE = "urn:ietf:params:oauth:grant-type:device_code";

    public static boolean isConfigured(Config cfg) {
        return !cfg.get("tiktok.clientKey", "").isBlank()
                && !cfg.get("tiktok.clientSecret", "").isBlank();
    }

    /** Login de la cuenta: obtiene un código para el navegador y espera la autorización. */
    public static void authorize(Config cfg, Consumer<String> log) throws Exception {
        String key = cfg.get("tiktok.clientKey", "");
        if (key.isBlank()) {
            throw new IllegalStateException("Falta tiktok.clientKey (app creada en developers.tiktok.com y aprobada "
                    + "para Content Posting API).");
        }
        String scopes = URLEncoder.encode("user.info.basic,video.upload,video.publish", StandardCharsets.UTF_8);
        String body = "client_key=" + key + "&scopes=" + scopes;
        String resp = post(BASE + "v2/oauth/device_authorize/",
                "application/x-www-form-urlencoded", body, null);
        String deviceCode = jsonGet(resp, "device_code");
        String userCode = jsonGet(resp, "user_code");
        String uri = jsonGet(resp, "verification_uri_complete");
        if (deviceCode.isBlank()) {
            throw new IllegalStateException("TikTok no devolvió código de dispositivo: " + resp);
        }
        log.accept("Abre " + uri + " y pega este codigo: " + userCode);

        int interval = Math.max(5, parseInt(jsonGet(resp, "interval"), 5));
        long deadline = System.currentTimeMillis() + 5 * 60 * 1000;
        while (System.currentTimeMillis() < deadline) {
            Thread.sleep(interval * 1000L);
            String tok = post(BASE + "v2/oauth/token/", "application/x-www-form-urlencoded",
                    "grant_type=" + DEVICE
                            + "&client_key=" + key
                            + "&client_secret=" + cfg.get("tiktok.clientSecret", "")
                            + "&device_code=" + URLEncoder.encode(deviceCode, StandardCharsets.UTF_8),
                    null);
            if (jsonGet(tok, "access_token").isBlank()) {
                String err = jsonGet(tok, "message");
                log.accept("Esperando autorización... (" + (err.isBlank() ? "pendiente" : err) + ")");
                continue;
            }
            saveTokens(cfg, tok);
            log.accept("TikTok conectado. Publicado automáticamente en @"
                    + cfg.get("tiktok.handle", "historiasrea28"));
            return;
        }
        throw new IllegalStateException("Tiempo de espera de autorización agotado.");
    }

    /** Publica un video. Usa el Short si existe junto a él, si no el video completo. */
    public static void postVideo(Config cfg, Path video, String titulo, String desc, Consumer<String> log) throws Exception {
        if (!isConfigured(cfg)) {
            throw new IllegalStateException("TikTok no configurado: tiktok.clientKey/clientSecret. Ver SOLICITUD_TIKTOK.txt.");
        }
        Path file = preferShort(video);
        String token = accessToken(cfg);
        String initResp = post(BASE + "v2/video/publish/init/", "application/json",
                "{\"post_info\":{\"title\":" + jstr(titulo)
                        + ",\"privacy_level\":\"PUBLIC_TO_EVERYONE\"},"
                        + "\"source_info\":{\"source\":\"FILE_UPLOAD\"}}", token);
        String uploadUrl = jsonGet(initResp, "upload_url");
        String publishId = jsonGet(initResp, "publish_id");
        if (uploadUrl.isBlank()) {
            throw new IllegalStateException("TikTok no dio upload_url: " + initResp);
        }
        log.accept("Subiendo a TikTok: " + file.getFileName());
        upload(uploadUrl, file);
        String pubResp = post(BASE + "v2/video/publish/", "application/json",
                "{\"publish_id\":" + jstr(publishId)
                        + ",\"post_info\":{\"title\":" + jstr(titulo) + ",\"privacy_level\":\"PUBLIC_TO_EVERYONE\"}}",
                token);
        log.accept("TikTok: enviado a procesar (publish_id " + jsonGet(pubResp, "publish_id") + "). "
                + "Aparecerá en https://www.tiktok.com/@" + cfg.get("tiktok.handle", "historiasrea28"));
    }

    static Path preferShort(Path video) {
        String name = video.getFileName().toString().replaceFirst("(?i)\\.mp4$", "");
        Path shortCandidate = video.resolveSibling(name + " - Short.mp4");
        return Files.exists(shortCandidate) ? shortCandidate : video;
    }

    private static void upload(String url, Path file) throws Exception {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofMinutes(10))
                .header("Content-Type", "video/mp4")
                .PUT(HttpRequest.BodyPublishers.ofFile(file))
                .build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        int sc = res.statusCode();
        if (sc < 200 || sc >= 300) {
            throw new IllegalStateException("Subida a TikTok falló (" + sc + "): " + res.body());
        }
    }

    /** Devuelve un access_token válido, refrescándolo si hace falta. */
    private static String accessToken(Config cfg) throws Exception {
        String at = cfg.get("tiktok.accessToken", "");
        long exp = parseLong(cfg.get("tiktok.tokenExpiresAt", "0"), 0);
        if (!at.isBlank() && exp > Instant.now().getEpochSecond() + 120) return at;

        String rt = cfg.get("tiktok.refreshToken", "");
        if (!rt.isBlank()) {
            String resp = post(BASE + "v2/oauth/token/", "application/x-www-form-urlencoded",
                    "grant_type=refresh_token"
                            + "&client_key=" + cfg.get("tiktok.clientKey", "")
                            + "&client_secret=" + cfg.get("tiktok.clientSecret", "")
                            + "&refresh_token=" + URLEncoder.encode(rt, StandardCharsets.UTF_8),
                    null);
            if (!jsonGet(resp, "access_token").isBlank()) {
                saveTokens(cfg, resp);
                return cfg.get("tiktok.accessToken", "");
            }
            throw new IllegalStateException("El acceso a TikTok caducó: vuelve a ejecutar --tiktok-auth. " + resp);
        }
        throw new IllegalStateException("No hay sesión de TikTok. Ejecuta --tiktok-auth.");
    }

    private static void saveTokens(Config cfg, String json) {
        String access = jsonGet(json, "access_token");
        long exp = Instant.now().getEpochSecond() + parseLong(jsonGet(json, "expires_in"), "86400");
        cfg.set("tiktok.accessToken", access);
        cfg.set("tiktok.tokenExpiresAt", String.valueOf(exp));
        cfg.set("tiktok.refreshToken", jsonGet(json, "refresh_token"));
        cfg.set("tiktok.openId", jsonGet(json, "open_id"));
        cfg.save();
    }

    private static String post(String url, String contentType, String body, String bearer) throws Exception {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(90))
                .header("Content-Type", contentType)
                .header("Accept", contentType.startsWith("application/json") ? "application/json" : "*/*")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (bearer != null && !bearer.isBlank()) b.header("Authorization", "Bearer " + bearer);
        HttpResponse<String> res = client.send(b.build(), HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() >= 500 || res.statusCode() == 401) {
            throw new IllegalStateException("TikTok respondió " + res.statusCode() + ": " + res.body()
                    + (res.statusCode() == 401 ? " (¿token caducado? ejecuta --tiktok-auth)" : ""));
        }
        return res.body();
    }

    private static String jstr(String s) {
        return "\"" + (s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"")) + "\"";
    }

    private static String jsonGet(String json, String key) {
        if (json == null) return "";
        int i = json.indexOf("\"" + key + "\"");
        if (i < 0) return "";
        int colon = json.indexOf(':', i);
        if (colon < 0) return "";
        int p = colon + 1;
        while (p < json.length() && Character.isWhitespace(json.charAt(p))) p++;
        if (p >= json.length()) return "";
        if (json.charAt(p) == '"') {
            StringBuilder sb = new StringBuilder();
            boolean esc = false;
            for (int k = p + 1; k < json.length(); k++) {
                char c = json.charAt(k);
                if (esc) {
                    sb.append(c);
                    esc = false;
                } else if (c == '\\') {
                    esc = true;
                } else if (c == '"') {
                    break;
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }
        int end = p;
        while (end < json.length() && ",}] \t".indexOf(json.charAt(end)) < 0) end++;
        return end > p ? json.substring(p, end).trim() : "";
    }

    private static int parseInt(String s, int def) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return def;
        }
    }

    private static long parseLong(String s, long def) {
        try {
            return Long.parseLong(s);
        } catch (Exception e) {
            return def;
        }
    }

    private static long parseLong(String s, String def) {
        return parseLong(s, Long.parseLong(def));
    }

    private TikTokPoster() {}
}