package com.construyendovalores.studio;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public final class Gemini {
    private Gemini() {}

    public static String generateScript(Config cfg, String title, String value, int scenes) throws IOException, InterruptedException {
        return generate(cfg, Prompt.build(title, value, scenes));
    }

    public static String generateScript(Config cfg, String title, String value, int scenes, String scenarioLabel) throws IOException, InterruptedException {
        return generate(cfg, Prompt.build(title, value, scenes, scenarioLabel));
    }

    public static String generate(Config cfg, String prompt) throws IOException, InterruptedException {
        String key = cfg.apiKey();
        if (key.isBlank()) {
            throw new IllegalStateException("Falta la API key de Gemini. Configúrala en el panel 'Configuración'.");
        }

        String body = "{"
                + "\"system_instruction\":{\"parts\":[{\"text\":"
                + json(Prompt.SYSTEM) + "}]},"
                + "\"contents\":[{\"parts\":[{\"text\":"
                + json(prompt) + "}]}]"
                + "}";

        URI uri = URI.create("https://generativelanguage.googleapis.com/v1beta/models/"
                + cfg.geminiModel() + ":generateContent");

        HttpRequest req = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(90))
                .header("Content-Type", "application/json")
                .header("X-goog-api-key", key)
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();

        int attempts = 3;
        long[] waits = {5000, 30000};
        for (int attempt = 0; attempt < attempts; attempt++) {
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() == 200) {
                return resp.body();
            }
            String msg = resp.body();
            boolean quota = resp.statusCode() == 429 && msg.toLowerCase().contains("quota");
            boolean isTemporary = resp.statusCode() == 503 || resp.statusCode() == 500;
            if ((quota || isTemporary) && attempt < attempts - 1) {
                Thread.sleep(waits[attempt]);
                continue;
            }
            if (quota) {
                throw new RuntimeException("Cuota diaria de Gemini agotada (capa gratis ~20 peticiones/día). "
                        + "Espera a que se renueve o usa otra clave en Configuración.");
            }
            throw new RuntimeException("Gemini respondió " + resp.statusCode() + ": " + truncate(msg, 300));
        }
        throw new RuntimeException("Gemini no respondió.");
    }

    private static String json(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "") + "\"";
    }

    private static String truncate(String s, int n) {
        return s.length() <= n ? s : s.substring(0, n) + "...";
    }
}