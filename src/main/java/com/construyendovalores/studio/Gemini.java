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
        return generarGuion(cfg, title, value, scenes, "");
    }

    public static String generateScript(Config cfg, String title, String value, int scenes, String scenarioLabel) throws IOException, InterruptedException {
        return generarGuion(cfg, title, value, scenes, scenarioLabel);
    }

    /** Recuento original de un cuento clasico (dominio publico): reescribe la historia
     *  con nuestras propias palabras, nuestra pandilla y la ensenanza del valor. */
    public static String generateRetelling(Config cfg, String cuento, String valor, int scenes) throws IOException, InterruptedException {
        String prompt = "Reescribe el cuento clasico '" + cuento + "' como un guion ORIGINAL para un "
                + "canal infantil de YouTube (este cuento es de dominio publico, pero debes narrar la historia "
                + "con tus propias palabras, nunca copiar una traduccion existente).\n"
                + "Usa la pandilla del canal como personajes (Casquito el maestro, Tito y Nico los ninos, "
                + "Luna, Pipo, Tico y Sofia). Manten la trama clasica reconocible, reduce el tono de peligro "
                + "para edad preescolar y termina ensenando este valor: " + valor + ".\n"
                + "Devuelve UNICAMENTE un JSON con estas claves exactas:\n"
                + "video_title (titulo del video), description, scenes (array de escenas, cada una con "
                + "character, narration y scenario). Entre 4 y " + scenes + " escenas, narraciones de 1 a 2 frases.";
        return generarGuion(cfg, "Cuento: " + cuento, valor, scenes, "", prompt);
    }

    /** Guion con resiliencia gratis: gemini -> otras IAs gratuitas -> plantilla local. Bono opcional: crítica de mejora. */
    private static String generarGuion(Config cfg, String title, String value, int scenes, String scenarioLabel) throws IOException, InterruptedException {
        String prompt = scenarioLabel == null || scenarioLabel.isBlank()
                ? Prompt.build(title, value, scenes)
                : Prompt.build(title, value, scenes, scenarioLabel);
        return generarGuion(cfg, title, value, scenes, scenarioLabel, prompt);
    }

    private static String generarGuion(Config cfg, String title, String value, int scenes, String scenarioLabel, String prompt) throws IOException, InterruptedException {
        String crudo;
        try {
            crudo = generate(cfg, prompt);
        } catch (RuntimeException e) {
            String fallo = e.getMessage();
            try {
                crudo = wrap(MultiIA.generar(cfg, Prompt.SYSTEM, prompt, s -> {
                }));
            } catch (Exception e2) {
                System.err.println("AVISO: todas las IAs fallaron (Gemini=" + fallo
                        + "; respaldo=" + (e2.getMessage() == null ? "error" : e2.getMessage().split("\\n")[0])
                        + "). Usando guion local.");
                crudo = wrap(plantillaGuion(title, value, scenes));
            }
        }
        if ("true".equalsIgnoreCase(cfg.get("ia.critica", "false"))) {
            try {
                String json = GeminiParser.extractJson(crudo);
                if (json.contains("\"scenes\"")) {
                    String critica = "Eres el editor jefe de un canal infantil. Mejora este guion: narrador mas natural, "
                            + "valor educacional reforzado al final, tono amable y gancho para que el nino repita/cante la frase.\n"
                            + "Reemplaza cada valor de los campos, pero CONSERVA la estructura exacta.\n"
                            + "RESPONDE UNICAMENTE con el JSON. Sin bloques de codigo, sin explicaciones, sin markdown.\n"
                            + "Las claves exactas son: video_title, description y scenes (cada escena con character, narration y scenario).\n\n"
                            + json;
                    String mejorado = null;
                    try {
                        mejorado = GeminiParser.extractJson(generate(cfg, critica));
                    } catch (Exception ex) {
                        try {
                            mejorado = GeminiParser.extractJson(MultiIA.generar(cfg, Prompt.SYSTEM, critica, s -> { }));
                        } catch (Exception ex2) {
                            mejorado = null;
                        }
                    }
                    if (mejorado != null && mejorado.contains("\"scenes\":") && mejorado.contains("\"narration\"")) {
                        crudo = wrap(mejorado);
                    }
                }
            } catch (Exception ignored) {
                // si la crítica falla, se usa el guion original
            }
        }
        return crudo;
    }

    /** Guion genérico local para que la producción nunca se detenga sin Internet/cuota. */
    static String plantillaGuion(String title, String value, int scenes) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"video_title\":\"").append(json(title)).append("\",\"description\":\"").append(json(
                "Aprende a " + value.toLowerCase() + " con este video lleno de alegria y valores para ninos."))
                .append("\",\"scenes\":[");
        String[] frases = {
                "Hola amiguitos bienvenidos al canal",
                "Hoy vamos a descubrir juntos que es la " + value.toLowerCase(),
                "Cuando ayudamos y somos amables, el mundo brilla mas",
                "Recuerda que " + value.toLowerCase() + " empieza en tu manita y llega a tu corazonsito",
                "Practica hoy con tu familia y veras cuanto cambia todo",
                "Dale like, suscribete y comparte este abrazo de " + value.toLowerCase() + " con quien quieras"
        };
        int n = Math.min(scenes, frases.length);
        if (n == 0) n = 1;
        for (int i = 0; i < n; i++) {
            if (i > 0) sb.append(",");
            sb.append("{\"character\":\"").append(i == 0 ? "Narrador" : "Maite")
                    .append("\",\"narration\":\"").append(json(frases[i])).append("\",\"scenario\":\"parque\"}");
        }
        sb.append("]}");
        return sb.toString();
    }

    /** Envuelve texto plano en el sobre JSON de Gemini para que los parsers existan sin cambios. */
    static String wrap(String text) {
        return "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":"
                + json(text) + "}]}}]}";
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