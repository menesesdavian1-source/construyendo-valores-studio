package com.construyendovalores.studio;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;

/** Generacion de imagenes con la API de OpenAI (gpt-image-1). Solo si hay openai.apiKey en config. */
public final class OpenaiImagen {
    private OpenaiImagen() {}

    /** Genera una imagen vertical y la guarda en 'out'. Devuelve true si se guardo correctamente. */
    public static boolean descargar(Config cfg, String prompt, Path out) throws Exception {
        String key = cfg.get("openai.apiKey", "");
        if (key.isBlank()) return false;
        String model = cfg.get("openai.imagenModel", "gpt-image-1");

        String body = "{\"model\":\"" + model + "\",\"prompt\":"
                + json(prompt) + ",\"size\":\"1024x1536\",\"n\":1,\"quality\":\"high\"}";

        URI uri = URI.create("https://api.openai.com/v1/images/generations");
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();

        long[] waits = {6000, 18000};
        for (int attempt = 0; attempt <= waits.length; attempt++) {
            HttpRequest req = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(180))
                    .header("Authorization", "Bearer " + key)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> resp;
            try {
                resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            } catch (Exception e) {
                if (attempt < waits.length) { Thread.sleep(waits[attempt]); continue; }
                return false;
            }
            if (resp.statusCode() / 100 == 2) {
                byte[] img = extraer(resp.body());
                if (img != null && img.length > 20_000) {
                    Files.createDirectories(out.getParent());
                    Files.write(out, img);
                    return true;
                }
                throw new RuntimeException("2xx sin imagen: " + snippet(resp.body()));
            }
            if (resp.statusCode() == 429 || resp.statusCode() == 500 || resp.statusCode() == 503) {
                if (attempt < waits.length) { Thread.sleep(waits[attempt]); continue; }
                throw new RuntimeException(resp.statusCode() + " agotado: " + snippet(resp.body()));
            }
            throw new RuntimeException("HTTP " + resp.statusCode() + ": " + snippet(resp.body()));
        }
        return false;
    }

    private static String snippet(String body) {
        if (body == null) return "";
        String t = body.replace('\n', ' ');
        return t.length() <= 220 ? t : t.substring(0, 220);
    }

    /** Extrae la imagen base64 de data[0].b64_json (o de la URL en data[0].url). */
    private static byte[] extraer(String body) {
        if (body == null) return null;
        int bi = body.indexOf("\"b64_json\":\"");
        if (bi >= 0) {
            int start = bi + "\"b64_json\":\"".length();
            int end = body.indexOf('"', start);
            if (end > start) {
                try {
                    return Base64.getDecoder().decode(body.substring(start, end));
                } catch (Exception e) {
                    return null;
                }
            }
        }
        int ui = body.indexOf("\"url\":\"");
        if (ui >= 0) {
            int start = ui + "\"url\":\"".length();
            int end = body.indexOf('"', start);
            if (end > start) {
                try {
                    var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
                    var req = HttpRequest.newBuilder(URI.create(body.substring(start, end)))
                            .timeout(Duration.ofSeconds(120)).GET().build();
                    HttpResponse<byte[]> r = client.send(req, HttpResponse.BodyHandlers.ofByteArray());
                    if (r.statusCode() / 100 == 2) return r.body();
                } catch (Exception e) {
                    return null;
                }
            }
        }
        return null;
    }

    private static String json(String s) {
        if (s == null) return "\"\"";
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "") + "\"";
    }
}