package com.construyendovalores.studio;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Orquestador multi-IA: intenta generar una respuesta con la primera IA disponible.
 * Orden configurable: ia.orden (por defecto gemini;groq;together;mistral;cerebras).
 * Cada proveedor libre tiene su clave opcional: ia.<id>.apiKey / ia.<id>.model / ia.<id>.url.
 * Si ninguna IA responde, lanza error para que el llamador use su respaldo local.
 */
public final class MultiIA {
    private MultiIA() {}

    public static final List<String> ORDEN = List.of(
            "gemini", "groq", "together", "mistral", "cerebras");

    private static final java.util.Map<String, String> URLS = java.util.Map.of(
            "groq", "https://api.groq.com/openai/v1/chat/completions",
            "together", "https://api.together.xyz/v1/chat/completions",
            "mistral", "https://api.mistral.ai/v1/chat/completions",
            "cerebras", "https://api.cerebras.ai/v1/chat/completions");

    private static final java.util.Map<String, String> MODELOS = java.util.Map.of(
            "groq", "llama-3.3-70b-versatile",
            "together", "meta-llama/Llama-3.3-70B-Instruct-Turbo",
            "mistral", "mistral-small-latest",
            "cerebras", "llama-3.3-70b");

    public static List<String> orden(Config cfg) {
        String raw = cfg.get("ia.orden", "gemini;groq;together;mistral;cerebras");
        List<String> out = new ArrayList<>();
        for (String s : raw.split(";")) {
            String t = s.trim();
            if (!t.isEmpty() && !out.contains(t)) out.add(t);
        }
        return out;
    }

    /** Devuelve el texto de la primera IA que responda. */
    public static String generar(Config cfg, String system, String user, Consumer<String> log) throws Exception {
        List<String> fallos = new ArrayList<>();
        for (String id : orden(cfg)) {
            try {
                String r = llamar(cfg, id, system, user);
                if (r != null && !r.isBlank()) return r;
                fallos.add(id + " (respuesta vacia)");
            } catch (Exception e) {
                String msg = e.getMessage();
                fallos.add(id + " (" + (msg == null ? "error" : msg.split("\\n")[0]) + ")");
            }
        }
        throw new RuntimeException("Ninguna IA respondio: " + String.join(" | ", fallos));
    }

    /** Prueba una IA con una peticion minima. Devuelve "OK" o motivo de fallo. */
    public static String probar(Config cfg, String id) {
        try {
            String texto = llamar(cfg, id, "", "Responde solo con la palabra OK.");
            if (texto != null && texto.toUpperCase().contains("OK")) return "OK";
            return "OK pero respuesta inusual: " + (texto == null ? "vacia" : texto.trim().substring(0, Math.min(40, texto.trim().length())));
        } catch (Exception e) {
            return "FALLO: " + (e.getMessage() == null ? e.toString() : e.getMessage().split("\\n")[0]);
        }
    }

    /** Consulta directa a un proveedor concreto (publica para --ia-consulta). */
    public static String llamarPublico(Config cfg, String id, String system, String user) throws Exception {
        return llamar(cfg, id, system, user);
    }

    private static String llamar(Config cfg, String id, String system, String user) throws Exception {
        if ("gemini".equals(id)) {
            String completo = system == null || system.isBlank() ? user : system + "\n\n" + user;
            return GeminiParser.extractText(Gemini.generate(cfg, completo));
        }
        String key = cfg.get("ia." + id + ".apiKey", "");
        if (key.isBlank()) {
            throw new IllegalStateException("sin ia." + id + ".apiKey en config");
        }
        String url = cfg.get("ia." + id + ".url", URLS.getOrDefault(id, ""));
        String model = cfg.get("ia." + id + ".model", MODELOS.getOrDefault(id, id));
        if (url.isBlank()) throw new IllegalStateException("sin url para " + id);
        String body = "{\"model\":\"" + model + "\",\"messages\":["
                + (system == null || system.isBlank() ? "" :
                "{\"role\":\"system\",\"content\":" + json(system) + "},")
                + "{\"role\":\"user\",\"content\":" + json(user) + "}],"
                + "\"temperature\":0.6,\"max_tokens\":2048}";
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(120))
                .header("Authorization", "Bearer " + key)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() / 100 != 2) {
            String detalle = resp.body();
            if (detalle != null && detalle.length() > 200) detalle = detalle.substring(0, 200);
            throw new RuntimeException(id + " respondio " + resp.statusCode() + (detalle == null ? "" : ": " + detalle));
        }
        String contenido = extractContent(resp.body());
        if (contenido == null) {
            throw new RuntimeException(id + " sin campo content");
        }
        return contenido;
    }

    private static String extractContent(String json) {
        if (json == null) return null;
        int idx = json.lastIndexOf("\"content\":");
        if (idx < 0) return null;
        int start = idx + "\"content\":".length();
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) {
            start++;
        }
        if (start >= json.length() || json.charAt(start) != '"') return null;
        start++;
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char n = json.charAt(i + 1);
                switch (n) {
                    case 'n' -> { sb.append('\n'); i++; }
                    case 't' -> { sb.append('\t'); i++; }
                    case 'r' -> { sb.append('\r'); i++; }
                    case '"' -> { sb.append('"'); i++; }
                    case '\\' -> { sb.append('\\'); i++; }
                    case '/' -> { sb.append('/'); i++; }
                    case 'u' -> {
                        if (i + 5 < json.length()) {
                            try {
                                sb.append((char) Integer.parseInt(json.substring(i + 2, i + 6), 16));
                                i += 5;
                            } catch (NumberFormatException ignored) {
                                sb.append('u');
                            }
                        }
                    }
                    default -> { sb.append(n); i++; }
                }
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }
        String out = sb.toString().trim();
        return out.isEmpty() ? null : out;
    }

    private static String json(String s) {
        if (s == null) return "\"\"";
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "") + "\"";
    }
}