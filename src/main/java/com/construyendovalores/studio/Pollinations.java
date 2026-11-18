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
import java.util.Base64;

/**
 * Generacion gratuita de imagenes mediante Pollinations.
 * Sin cuenta: funciona (marca de agua, 1 peticion/15s). Con cuenta gratuita
 * (enter.pollinations.ai -> key sk_... en config pollinations.token) se retira
 * la marca de agua y sube el limite. Se intenta primero la API moderna v1 y la
 * URL legacy antigua como respaldo, terminando en el modo anonimo clasico.
 */
public final class Pollinations {
    private Pollinations() {}

    /** Ultima llamada anonima (min 16s entre peticiones para no saltar el rate-limit). */
    private static volatile long ULTIMA = 0L;

    /** Descarga una imagen generada por IA a 'out'. Devuelve true si se guardo correctamente. */
    public static boolean download(String prompt, int width, int height, long seed, Path out) {
        return descargar(prompt, width, height, seed, out, "");
    }

    /** Igual, pero usando el token de cuenta si existe (quita la marca de agua: pollinations.token). */
    public static boolean downloadAuth(Config cfg, String prompt, int width, int height, long seed, Path out) {
        String token = cfg == null ? "" : cfg.get("pollinations.token", "").trim();
        if (token.startsWith("pk_")) {
            token = ""; // pk_ es la publicada para OAuth, no sirve como key de generacion
        }
        String modelo = cfg == null ? "openai/gpt-image-2" : cfg.get("pollinations.model", "openai/gpt-image-2");
        String enhance = cfg == null ? "true" : cfg.get("pollinations.enhance", "true");
        if (!token.isBlank()) {
            if (v1(prompt, width, height, modelo, enhance, token, out)) {
                System.err.println("pollinations: cuenta v1 [" + modelo + "] (sin marca de agua)");
                return true;
            }
            String comun = "&nologo=true&model=" + modelo + "&enhance=" + enhance;
            if (descargar(prompt, width, height, seed, out, comun + "&key=" + token)) {
                System.err.println("pollinations: cuenta legacy key [" + modelo + "]");
                return true;
            }
            if (descargar(prompt, width, height, seed, out, comun + "&token=" + token)) {
                System.err.println("pollinations: cuenta legacy token [" + modelo + "]");
                return true;
            }
            System.err.println("pollinations: cuenta no disponible, uso anonimo (con marca de agua)");
        }
        return descargar(prompt, width, height, seed, out, "");
    }

    /** API moderna (OpenAI-compatible) de generacion de imagenes, con key de cuenta. */
    private static boolean v1(String prompt, int width, int height, String modelo, String enhance,
                              String token, Path out) {
        try {
            String size = height > width * 1.15 ? "1024x1536"
                    : (width > height * 1.15 ? "1536x1024" : "1024x1024");
            String body = "{\"model\":" + json(modelo) + ",\"prompt\":" + json(prompt)
                    + ",\"size\":\"" + size + "\",\"nologo\":true,\"enhance\":" + enhance + "}";
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
            HttpRequest req = HttpRequest.newBuilder(URI.create("https://gen.pollinations.ai/v1/images/generations"))
                    .timeout(Duration.ofSeconds(180))
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "ConstruyendoValoresStudio/1.0")
                    .header("Authorization", "Bearer " + token)
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() / 100 == 2) {
                String b64 = entre(resp.body(), "\"b64_json\":\"", "\"");
                if (b64 != null) {
                    byte[] img = Base64.getDecoder().decode(b64);
                    if (img.length > 10_000) {
                        Files.createDirectories(out.getParent());
                        Files.write(out, img);
                        return true;
                    }
                }
                String url = entre(resp.body(), "\"url\":\"", "\"");
                if (url != null && url.startsWith("http")) {
                    HttpResponse<byte[]> ri = client.send(HttpRequest.newBuilder(URI.create(url))
                                    .timeout(Duration.ofSeconds(120)).GET().build(),
                            HttpResponse.BodyHandlers.ofByteArray());
                    if (ri.statusCode() / 100 == 2 && ri.body().length > 10_000) {
                        Files.createDirectories(out.getParent());
                        Files.write(out, ri.body());
                        return true;
                    }
                    System.err.println("pollinations v1: url fallo HTTP " + ri.statusCode());
                } else {
                    String cuerpo = resp.body() == null ? "" : resp.body().replaceAll("\\s+", " ");
                    System.err.println("pollinations v1: 2xx sin b64/url -> "
                            + cuerpo.substring(0, Math.min(300, cuerpo.length())));
                }
            } else {
                String t = resp.body() == null ? "" : resp.body().replaceAll("\\s+", " ");
                System.err.println("pollinations v1: HTTP " + resp.statusCode() + " -> "
                        + t.substring(0, Math.min(300, t.length())));
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private static boolean descargar(String prompt, int width, int height, long seed, Path out, String extra) {
        String p = prompt == null ? "" : prompt.trim();
        String encoded = URLEncoder.encode(p, StandardCharsets.UTF_8).replace("+", "%20");
        String url = "https://image.pollinations.ai/prompt/" + encoded
                + "?width=" + width + "&height=" + height + "&seed=" + seed
                + "&nologo=false&model=flux" + extra;
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(180))
                .header("User-Agent", "ConstruyendoValoresStudio/1.0")
                .GET()
                .build();
        long[] waits = {5000, 15000};
        for (int attempt = 0; attempt <= waits.length; attempt++) {
            try {
                sembrar();
                HttpResponse<byte[]> resp = client.send(req, HttpResponse.BodyHandlers.ofByteArray());
                if (resp.statusCode() / 100 == 2 && resp.body().length > 10_000) {
                    Files.createDirectories(out.getParent());
                    Files.write(out, resp.body());
                    return true;
                }
                if (attempt < waits.length) Thread.sleep(waits[attempt]);
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    /** Respeta el limite anonimo: como minimo 16s entre peticiones. */
    private static void sembrar() {
        long faltan = 16_000 - (System.currentTimeMillis() - ULTIMA);
        if (faltan > 0) {
            try {
                Thread.sleep(faltan);
            } catch (InterruptedException ignored) {
            }
        }
        ULTIMA = System.currentTimeMillis();
    }

    /** Devuelve el texto entre 'ini' y 'fin' en body, o null si no aparece. */
    private static String entre(String body, String ini, String fin) {
        if (body == null) return null;
        int i = body.indexOf(ini);
        if (i < 0) return null;
        i += ini.length();
        int j = body.indexOf(fin, i);
        if (j < 0) return null;
        return body.substring(i, j);
    }

    /** Escapa un string como literal JSON con control de limite. */
    private static String json(String s) {
        if (s == null || s.isBlank()) return "\"\"";
        String t = s.trim();
        if (t.length() > 1200) t = t.substring(0, 1200);
        return "\"" + t.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", " ").replace("\r", " ") + "\"";
    }
}