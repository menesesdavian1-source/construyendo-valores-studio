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

/** Generacion de imagenes con la API de Gemini (mismo apiKey del canal, sin coste extra en la capa gratis). */
public final class GeminiImagen {
    private GeminiImagen() {}

    /** Genera una imagen y la guarda en 'out'. Devuelve true si se guardo correctamente. */
    public static boolean descargar(Config cfg, String prompt, String aspectRatio, Path out) throws Exception {
        String key = cfg.apiKey();
        if (key.isBlank()) return false;
        String model = cfg.get("gemini.imagenModel", "gemini-2.5-flash-image");

        URI uri = URI.create("https://generativelanguage.googleapis.com/v1beta/models/"
                + model + ":generateContent");
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();

        boolean sinAspecto = false;
        long[] waits = {6000, 18000};
        for (int attempt = 0; attempt <= waits.length; attempt++) {
            String body = "{"
                    + "\"contents\":[{\"parts\":[{\"text\":" + json(prompt) + "}]}],"
                    + "\"generationConfig\":{\"responseModalities\":[\"TEXT\",\"IMAGE\"]"
                    + (sinAspecto ? "" : ",\"imageConfig\":{\"aspectRatio\":\"" + aspectRatio + "\"}")
                    + "}}";
            HttpRequest req = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(180))
                    .header("Content-Type", "application/json")
                    .header("X-goog-api-key", key)
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> resp;
            try {
                resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            } catch (Exception e) {
                if (attempt < waits.length) { Thread.sleep(waits[attempt]); continue; }
                return false;
            }
            if (resp.statusCode() == 200) {
                byte[] img = extraer(resp.body());
                if (img != null && img.length > 20_000) {
                    Files.createDirectories(out.getParent());
                    Files.write(out, img);
                    return true;
                }
                throw new RuntimeException("200 sin imagen: " + snippet(resp.body()));
            }
            if (!sinAspecto && resp.statusCode() == 400
                    && (resp.body().toLowerCase().contains("aspectratio")
                    || resp.body().toLowerCase().contains("imageconfig"))) {
                sinAspecto = true;
                continue;
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

    /** Extrae la imagen base64 de la respuesta de generateContent. */
    private static byte[] extraer(String body) {
        if (body == null) return null;
        String key = "\"inlineData\":";
        int idx = body.indexOf(key);
        if (idx < 0) return null;
        String seg = body.substring(idx);
        int dataIdx = seg.indexOf("\"data\":\"");
        if (dataIdx < 0) return null;
        int start = dataIdx + "\"data\":\"".length();
        int end = seg.indexOf('"', start);
        if (end < 0) return null;
        try {
            return Base64.getDecoder().decode(seg.substring(start, end));
        } catch (Exception e) {
            return null;
        }
    }

    private static String json(String s) {
        if (s == null) return "\"\"";
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "") + "\"";
    }
}