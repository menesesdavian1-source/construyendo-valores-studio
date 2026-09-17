package com.construyendovalores.studio;

import java.nio.charset.StandardCharsets;

/**
 * Extrae el texto limpio de una respuesta de Gemini (modelo generativo): el payload
 * viene como JSON con "candidates.candidates[].content.parts[].text". Se toma lo que
 * venga entre las primeras llaves JSON de cierre del primer texto.
 */
public final class GeminiParser {

    public static String extractText(String rawResponse) throws IllegalArgumentException {
        int idx = rawResponse.indexOf("\"text\":");
        if (idx < 0) {
            throw new IllegalArgumentException("No se encontró texto en la respuesta de Gemini:\n" + truncate(rawResponse, 300));
        }
        int start = idx + "\"text\":".length();
        while (start < rawResponse.length() && Character.isWhitespace(rawResponse.charAt(start))) {
            start++;
        }
        if (start >= rawResponse.length() || rawResponse.charAt(start) != '"') {
            throw new IllegalArgumentException("Respuesta inesperada:\n" + truncate(rawResponse, 300));
        }
        start++; // abre comilla
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < rawResponse.length(); i++) {
            char c = rawResponse.charAt(i);
            if (c == '\\' && i + 1 < rawResponse.length()) {
                char n = rawResponse.charAt(i + 1);
                switch (n) {
                    case 'n' -> { sb.append('\n'); i++; }
                    case 't' -> { sb.append('\t'); i++; }
                    case 'r' -> { sb.append('\r'); i++; }
                    case '"' -> { sb.append('"'); i++; }
                    case '\\' -> { sb.append('\\'); i++; }
                    case 'u' -> {
                        if (i + 5 < rawResponse.length()) {
                            try {
                                sb.append((char) Integer.parseInt(rawResponse.substring(i + 2, i + 6), 16));
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
        String text = sb.toString().trim();
        if (text.isEmpty()) {
            throw new IllegalArgumentException("Texto vacío en la respuesta:\n" + truncate(rawResponse, 300));
        }
        return text;
    }

    /** Desenvuelve el text de Gemini y deja únicamente el JSON del guion. */
    public static String extractJson(String rawResponse) throws IllegalArgumentException {
        String t = extractText(rawResponse);
        int start = t.indexOf('{');
        int end = t.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return t.substring(start, end + 1);
        }
        return t;
    }

    private static String truncate(String s, int n) {
        return s.length() <= n ? s : s.substring(0, n) + "...";
    }

    private GeminiParser() {}
}