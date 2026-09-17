package com.construyendovalores.studio;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Conexión con la YouTube Data API v3 (gratis) usando OAuth 2.0:
 * autorización local (loopback), refresco de token y subida programada.
 */
public final class Youtube {

    private static final String SCOPE = "https://www.googleapis.com/auth/youtube";
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String UPLOAD_URL = "https://www.googleapis.com/upload/youtube/v3/videos";
    private static final String CHANNELS_URL = "https://www.googleapis.com/youtube/v3/channels";
    private static Path tokenFile() {
        return Paths.get(System.getProperty("user.home"), ".construyendo-valores", "youtube_tokens.json");
    }

    public record UploadResult(String videoId, String url, String privacyStatus, boolean scheduled) {}

    public record Estadisticas(String id, String titulo, String fecha, String estado,
                               long vistas, long meGusta, long comentarios) {}

    private Youtube() {}

    // ------------------------------------------------------------------ OAuth

    /** Ejecuta el flujo de autorización por única vez (abre el navegador). */
    public static void authorize(Config cfg, Consumer<String> log) throws Exception {
        String clientId = cfg.get("youtube.clientId", "");
        String clientSecret = cfg.get("youtube.clientSecret", "");
        if (clientId.isBlank() || clientSecret.isBlank()) {
            throw new IllegalStateException("Falta el Client ID o el Client Secret de YouTube. "
                    + "Configúralos en la pestaña 'Configuración' (guión GUIA_GOOGLE_CLOUD.md).");
        }

        int port = 8765;
        CountDownLatch latch = new CountDownLatch(1);
        String[] code = new String[1];
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", ex -> {
            String q = ex.getRequestURI().getRawQuery();
            String got = null;
            if (q != null) {
                for (String p : q.split("&")) {
                    if (p.startsWith("code=")) {
                        got = p.substring(5);
                        break;
                    }
                }
            }
            byte[] body = (got == null
                    ? "Error de autorización. Cierra esta pestaña y revisa la app."
                    : "Autorizado. Ya puedes cerrar esta pestaña y volver a la app.").getBytes(StandardCharsets.UTF_8);
            if (got != null) {
                code[0] = got;
            }
            ex.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
            ex.sendResponseHeaders(200, body.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(body);
            }
            latch.countDown();
        });
        server.start();

        String redirect = "http://localhost:" + port + "/";
        String url = AUTH_URL + "?client_id=" + enc(clientId)
                + "&redirect_uri=" + enc(redirect)
                + "&response_type=code"
                + "&scope=" + enc(SCOPE)
                + "&access_type=offline"
                + "&prompt=consent";
        log.accept("Abriendo el navegador... Autoriza y acepta el permiso.");
        try {
            java.awt.Desktop.getDesktop().browse(URI.create(url));
        } catch (Exception e) {
            log.accept("Abre manualmente en tu navegador:\n" + url);
        }
        if (!latch.await(5, TimeUnit.MINUTES)) {
            server.stop(0);
            throw new IllegalStateException("No se recibió la autorización (tiempo agotado).");
        }
        server.stop(0);
        if (code[0] == null) {
            throw new IllegalStateException("La autorización fue rechazada o falló.");
        }

        JsonObject body = tokenPost(TOKEN_URL,
                "code=" + enc(code[0])
                        + "&client_id=" + enc(clientId)
                        + "&client_secret=" + enc(clientSecret)
                        + "&redirect_uri=" + enc(redirect)
                        + "&grant_type=authorization_code");
        saveTokens(body);
        log.accept("¡Vinculado! Tu canal está conectado con la app.");
    }

    private static JsonObject tokenPost(String url, String form) throws Exception {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(90))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        JsonObject o = JsonParser.parseString(resp.body()).getAsJsonObject();
        if (resp.statusCode() != 200) {
            String desc = o.has("error_description") ? o.get("error_description").getAsString() : o.toString();
            throw new RuntimeException("OAuth falló (" + resp.statusCode() + "): " + desc);
        }
        return o;
    }

    public static boolean isLinked() {
        return tokenFile().toFile().exists();
    }

    /** Refresca (o lee) el access token. */
    public static synchronized String accessToken(Config cfg) throws Exception {
        if (!tokenFile().toFile().exists()) {
            throw new IllegalStateException("YouTube no está vinculado todavía. "
                    + "Usa 'Conectar YouTube' (o --youtube-auth).");
        }
        Properties p = new Properties();
        p.load(Files.newBufferedReader(tokenFile(), StandardCharsets.UTF_8));
        String refresh = p.getProperty("refresh_token", "");
        if (refresh.isBlank()) {
            throw new IllegalStateException("Token de refresco vacío. Vuelve a conectar YouTube.");
        }
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
        String form = "grant_type=refresh_token&refresh_token=" + enc(refresh)
                + "&client_id=" + enc(cfg.get("youtube.clientId", ""))
                + "&client_secret=" + enc(cfg.get("youtube.clientSecret", ""));
        HttpRequest req = HttpRequest.newBuilder(URI.create(TOKEN_URL))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        JsonObject o = JsonParser.parseString(resp.body()).getAsJsonObject();
        if (resp.statusCode() != 200) {
            throw new RuntimeException("No se pudo renovar el acceso a YouTube (" + resp.statusCode() + "). "
                    + "Vuelve a conectar con 'Conectar YouTube'.");
        }
        return o.get("access_token").getAsString();
    }

    private static void saveTokens(JsonObject o) throws IOException {
        Properties p = new Properties();
        if (o.has("access_token")) p.setProperty("access_token", o.get("access_token").getAsString());
        if (o.has("refresh_token")) p.setProperty("refresh_token", o.get("refresh_token").getAsString());
        Files.createDirectories(tokenFile().getParent());
        p.store(Files.newBufferedWriter(tokenFile(), StandardCharsets.UTF_8), "YouTube OAuth");
    }

    public static String channelName(Config cfg) throws Exception {
        String at = accessToken(cfg);
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
        HttpRequest req = HttpRequest.newBuilder(URI.create(CHANNELS_URL + "?part=snippet&mine=true"))
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "Bearer " + at)
                .GET().build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        JsonObject o = JsonParser.parseString(resp.body()).getAsJsonObject();
        if (resp.statusCode() != 200) {
            throw new RuntimeException("No fue posible leer tu canal (" + resp.statusCode() + "): " + o);
        }
        return o.getAsJsonArray("items").size() == 0 ? "?" : o.getAsJsonArray("items").get(0).getAsJsonObject()
                .getAsJsonObject("snippet").get("title").getAsString();
    }

    /** Devuelve la URL publica del canal autorizado (para el formulario del audit). */
    public static String channelUrl(Config cfg) throws Exception {
        String at = accessToken(cfg);
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
        HttpRequest req = HttpRequest.newBuilder(URI.create(CHANNELS_URL + "?part=snippet&mine=true"))
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "Bearer " + at)
                .GET().build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        JsonObject o = JsonParser.parseString(resp.body()).getAsJsonObject();
        if (resp.statusCode() != 200) {
            throw new RuntimeException("No fue posible leer tu canal (" + resp.statusCode() + "): " + o);
        }
        if (o.getAsJsonArray("items").size() == 0) {
            return "?";
        }
        JsonObject item = o.getAsJsonArray("items").get(0).getAsJsonObject();
        String id = item.get("id").getAsString();
        String custom = item.getAsJsonObject("snippet").has("customUrl")
                ? item.getAsJsonObject("snippet").get("customUrl").getAsString() : "";
        return "https://www.youtube.com/channel/" + id + (custom.isEmpty() ? "" : "  (@" + custom.replaceFirst("^@", "") + ")");
    }

    // ------------------------------------------------------------------ Subida

    public static UploadResult upload(Config cfg, Path video, String title, String description,
                                      List<String> tags, OffsetDateTime publishAt, String privacyStatus) throws Exception {
        return upload(cfg, video, title, description, tags, publishAt, privacyStatus, s -> { });
    }

    /**
     * Sube un video con la privacidad pedida y programa su publicación en publishAt.
     * Reintenta automáticamente (3 intentos) ante fallos de red o errores 5xx/429.
     * Usa "public" para contenidos del canal. Nota: si el proyecto de Google Cloud
     * no ha pasado el "audit" de YouTube, YouTube forzará la privacidad a "private"
     * (lo devuelve en el resultado y la app avisa al final).
     */
    public static UploadResult upload(Config cfg, Path video, String title, String description,
                                      List<String> tags, OffsetDateTime publishAt, String privacyStatus,
                                      Consumer<String> log) throws Exception {
        String at = accessToken(cfg);
        long size = Files.size(video);
        String asked = privacyStatus == null ? "private" : privacyStatus;

        // YouTube solo permite programar (publishAt) con privacyStatus "private": el video se
        // publica automáticamente cuando llega la hora. Si no se programa, se usa la privacidad
        // pedida (public/private/unlisted) de inmediato.
        String effectivePrivacy = publishAt != null ? "private" : asked;

        String status = "\"status\":{\"privacyStatus\":\"" + effectivePrivacy + "\",\"selfDeclaredMadeForKids\":true,"
                + "\"containsSyntheticMedia\":false"
                + (publishAt == null ? "" : ",\"publishAt\":\"" + publishAt.atZoneSameInstant(ZoneOffset.UTC)
                        .format(DateTimeFormatter.ISO_INSTANT) + "\"") + "}";
        StringBuilder sn = new StringBuilder("\"snippet\":{\"title\":" + json(title)
                + ",\"description\":" + json(description)
                + ",\"categoryId\":\"24\",\"defaultLanguage\":\"es\"");
        if (tags != null && !tags.isEmpty()) {
            StringBuilder t = new StringBuilder();
            for (String tag : tags) {
                t.append(',').append(json(tag.trim()));
            }
            sn.append(",\"tags\":[").append(t.length() > 0 ? t.substring(1) : "").append("]");
        }
        sn.append("}");
        String metadata = "{" + sn + "," + status + "}";

        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
        HttpRequest init = HttpRequest.newBuilder(URI.create(UPLOAD_URL
                + "?uploadType=resumable&part=snippet%2Cstatus"))
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "Bearer " + at)
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("X-Upload-Content-Type", "video/mp4")
                .header("X-Upload-Content-Length", String.valueOf(size))
                .POST(HttpRequest.BodyPublishers.ofString(metadata, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> initResp = sendRetry(client, init, log, "inicio de subida a YouTube");
        if (initResp.statusCode() / 100 != 2) {
            throw new RuntimeException("El inicio de la subida falló (" + initResp.statusCode() + "): "
                    + initResp.body());
        }
        String location = initResp.headers().firstValue("Location").orElseThrow(
                () -> new RuntimeException("Respuesta sin Location (subida a YouTube)."));

        HttpRequest data = HttpRequest.newBuilder(URI.create(location))
                .timeout(Duration.ofMinutes(15))
                .header("Authorization", "Bearer " + at)
                .header("Content-Type", "video/mp4")
                .PUT(HttpRequest.BodyPublishers.ofFile(video))
                .build();
        HttpResponse<String> dataResp = sendRetry(client, data, log, "subida del video a YouTube");
        String body = dataResp.body();
        if (dataResp.statusCode() / 100 != 2) {
            throw new RuntimeException("La subida del video falló (" + dataResp.statusCode() + "): "
                    + body);
        }
        JsonObject o = JsonParser.parseString(body).getAsJsonObject();
        String id = o.has("id") ? o.get("id").getAsString() : "?";
        String statusInfo = o.has("status") && o.getAsJsonObject("status").has("privacyStatus")
                ? o.getAsJsonObject("status").get("privacyStatus").getAsString() : "?";
        return new UploadResult(id, "https://youtu.be/" + id, statusInfo, publishAt != null);
    }

    /** Lista los últimos 'max' videos del canal (id, estado y título) para depurar. */
    public static List<String> listMine(Config cfg, int max) throws Exception {
        String at = accessToken(cfg);
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
        // 1. Playlist de subidas del canal.
        HttpRequest ch = HttpRequest.newBuilder(URI.create("https://www.googleapis.com/youtube/v3/channels"
                + "?part=contentDetails&mine=true"))
                .timeout(Duration.ofSeconds(60)).header("Authorization", "Bearer " + at).GET().build();
        HttpResponse<String> rch = client.send(ch, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (rch.statusCode() / 100 != 2) {
            throw new RuntimeException("No se pudo leer el canal (" + rch.statusCode() + "): " + rch.body());
        }
        String uploadsPlaylistId = JsonParser.parseString(rch.body()).getAsJsonObject()
                .getAsJsonArray("items").get(0)
                .getAsJsonObject().getAsJsonObject("contentDetails")
                .getAsJsonObject().get("relatedPlaylists").getAsJsonObject().get("uploads").getAsString();
        // 2. Últimos ítems de esa playlist (id + título).
        HttpRequest pi = HttpRequest.newBuilder(URI.create("https://www.googleapis.com/youtube/v3/playlistItems"
                + "?part=snippet&playlistId=" + enc(uploadsPlaylistId) + "&maxResults=" + max))
                .timeout(Duration.ofSeconds(60)).header("Authorization", "Bearer " + at).GET().build();
        HttpResponse<String> rpi = client.send(pi, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (rpi.statusCode() / 100 != 2) {
            throw new RuntimeException("No se pudo listar (" + rpi.statusCode() + "): " + rpi.body());
        }
        JsonObject po = JsonParser.parseString(rpi.body()).getAsJsonObject();
        java.util.List<String> ids = new ArrayList<>();
        java.util.Map<String, String> titles = new java.util.LinkedHashMap<>();
        for (var it = po.getAsJsonArray("items").iterator(); it.hasNext(); ) {
            var v = (JsonObject) it.next();
            String id = v.getAsJsonObject("snippet").get("resourceId").getAsJsonObject().get("videoId").getAsString();
            titles.put(id, v.getAsJsonObject("snippet").get("title").getAsString());
            ids.add(id);
        }
        // 3. Estado de cada uno.
        String q = String.join(",", ids);
        HttpRequest vq = HttpRequest.newBuilder(URI.create("https://www.googleapis.com/youtube/v3/videos"
                + "?part=status&id=" + enc(q)))
                .timeout(Duration.ofSeconds(60)).header("Authorization", "Bearer " + at).GET().build();
        HttpResponse<String> rv = client.send(vq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (rv.statusCode() / 100 != 2) {
            throw new RuntimeException("No se pudo leer el estado (" + rv.statusCode() + "): " + rv.body());
        }
        List<String> out = new ArrayList<>();
        JsonObject vo = JsonParser.parseString(rv.body()).getAsJsonObject();
        for (var it = vo.getAsJsonArray("items").iterator(); it.hasNext(); ) {
            var v = (JsonObject) it.next();
            String id = v.get("id").getAsString();
            String st = v.getAsJsonObject("status").get("privacyStatus").getAsString();
            out.add(id + " | " + st + " | " + titles.get(id));
        }
        return out;
    }

    /** Reintenta un envío ante errores de red o 5xx/429 (los 4xx no se reintentan: no merecen cuota). */
    private static HttpResponse<String> sendRetry(HttpClient client, HttpRequest req, Consumer<String> log, String que)
            throws InterruptedException {
        Exception last = null;
        for (int intento = 1; intento <= 3; intento++) {
            if (intento > 1) Thread.sleep((intento == 2 ? 8 : 30) * 1000L);
            try {
                HttpResponse<String> r = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                int sc = r.statusCode();
                if (sc / 100 == 2 || (sc != 500 && sc != 502 && sc != 503 && sc != 429)) {
                    return r;
                }
                last = new RuntimeException(que + " (" + sc + "): " + r.body());
                log.accept("  Reintentando " + que + " (" + sc + ")... intento " + intento + "/3.");
            } catch (java.io.IOException e) {
                last = e;
                log.accept("  Error de red al " + que + ": " + e.getMessage() + " (intento " + intento + "/3).");
            }
        }
        throw new RuntimeException("No se pudo completar " + que + " tras 3 intentos.", last);
    }

    /** Últimos videos del canal con título, fecha, estado y estadísticas (para métricas). */
    public static List<Estadisticas> listEstadisticas(Config cfg, int max) throws Exception {
        String at = accessToken(cfg);
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
        HttpRequest ch = HttpRequest.newBuilder(URI.create("https://www.googleapis.com/youtube/v3/channels"
                + "?part=contentDetails&mine=true"))
                .timeout(Duration.ofSeconds(60)).header("Authorization", "Bearer " + at).GET().build();
        HttpResponse<String> rch = client.send(ch, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (rch.statusCode() / 100 != 2) {
            throw new RuntimeException("No se pudo leer el canal (" + rch.statusCode() + "): " + rch.body());
        }
        String uploadsPlaylistId = JsonParser.parseString(rch.body()).getAsJsonObject()
                .getAsJsonArray("items").get(0)
                .getAsJsonObject().getAsJsonObject("contentDetails")
                .getAsJsonObject().get("relatedPlaylists").getAsJsonObject().get("uploads").getAsString();
        HttpRequest pi = HttpRequest.newBuilder(URI.create("https://www.googleapis.com/youtube/v3/playlistItems"
                + "?part=snippet&playlistId=" + enc(uploadsPlaylistId) + "&maxResults=" + max))
                .timeout(Duration.ofSeconds(60)).header("Authorization", "Bearer " + at).GET().build();
        HttpResponse<String> rpi = client.send(pi, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (rpi.statusCode() / 100 != 2) {
            throw new RuntimeException("No se pudo listar (" + rpi.statusCode() + "): " + rpi.body());
        }
        List<String> ids = new ArrayList<>();
        JsonObject po = JsonParser.parseString(rpi.body()).getAsJsonObject();
        for (var it = po.getAsJsonArray("items").iterator(); it.hasNext(); ) {
            var v = (JsonObject) it.next();
            ids.add(v.getAsJsonObject("snippet").get("resourceId").getAsJsonObject().get("videoId").getAsString());
        }
        if (ids.isEmpty()) return List.of();

        HttpRequest vq = HttpRequest.newBuilder(URI.create("https://www.googleapis.com/youtube/v3/videos"
                + "?part=statistics,status,snippet&id=" + enc(String.join(",", ids))))
                .timeout(Duration.ofSeconds(60)).header("Authorization", "Bearer " + at).GET().build();
        HttpResponse<String> rv = client.send(vq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (rv.statusCode() / 100 != 2) {
            throw new RuntimeException("No se pudo leer el rendimiento (" + rv.statusCode() + "): " + rv.body());
        }
        List<Estadisticas> out = new ArrayList<>();
        JsonObject vo = JsonParser.parseString(rv.body()).getAsJsonObject();
        for (var it = vo.getAsJsonArray("items").iterator(); it.hasNext(); ) {
            var v = (JsonObject) it.next();
            JsonObject sn = v.getAsJsonObject("snippet");
            JsonObject st = v.has("status") ? v.getAsJsonObject("status") : new JsonObject();
            JsonObject stats = v.has("statistics") ? v.getAsJsonObject("statistics") : new JsonObject();
            out.add(new Estadisticas(v.get("id").getAsString(),
                    sn.has("title") ? sn.get("title").getAsString() : "",
                    sn.has("publishedAt") ? sn.get("publishedAt").getAsString() : "",
                    st.has("privacyStatus") ? st.get("privacyStatus").getAsString() : "",
                    num(stats, "viewCount"), num(stats, "likeCount"), num(stats, "commentCount")));
        }
        return out;
    }

    private static long num(JsonObject o, String key) {
        try {
            return o.has(key) ? o.get(key).getAsLong() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    /** Intenta hacer público un video ya subido (videos.update). Para proyectos con audit aprobado. */
    public static String setPublic(Config cfg, String videoId) throws Exception {
        String at = accessToken(cfg);
        String payload = "{\"status\":{\"privacyStatus\":\"public\"}}";
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
        HttpRequest req = HttpRequest.newBuilder(URI.create("https://www.googleapis.com/youtube/v3/videos"
                + "?part=status&id=" + enc(videoId)))
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "Bearer " + at)
                .header("Content-Type", "application/json; charset=UTF-8")
                .PUT(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() / 100 != 2) {
            throw new RuntimeException("YouTube rechazó hacer público el video (" + resp.statusCode()
                    + "): " + resp.body() + "\n\nTu proyecto de Google Cloud aún no pasó el audit de YouTube. "
                    + "Hazlo gratis aquí: https://support.google.com/youtube/contact/yt_api_form . "
                    + "Hasta entonces, los videos subidos por la app quedan privados.");
        }
        JsonObject o = JsonParser.parseString(resp.body()).getAsJsonObject();
        return o.getAsJsonObject("status").get("privacyStatus").getAsString();
    }

    private static String json(String s) {
        return "\"" + (s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "")) + "\"";
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}