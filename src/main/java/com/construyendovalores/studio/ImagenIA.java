package com.construyendovalores.studio;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Orquestador de generacion de imagenes IA: en orden configurable (imagen.orden,
 * por defecto gemini;openai;pollinations) intenta cada proveedor con la imagen
 * lista y devuelve cual respondio. Gemini usa la misma apiKey del canal; OpenAI
 * solo si hay openai.apiKey; Pollinations es el respaldo gratis (marca de agua).
 */
public final class ImagenIA {
    private ImagenIA() {}

    /** Genera la imagen en 'out'. Devuelve el proveedor usado ("" si ninguno pudo). */
    public static String generar(Config cfg, String prompt, int width, int height, long seed,
                                 String aspectRatio, Path out, Consumer<String> log) {
        List<String> fallos = new ArrayList<>();
        for (String id : orden(cfg)) {
            try {
                boolean ok;
                String a = aspectRatio == null ? "9:16" : aspectRatio;
                switch (id) {
                    case "gemini" -> ok = GeminiImagen.descargar(cfg, prompt, a, out);
                    case "openai" -> ok = OpenaiImagen.descargar(cfg, prompt, out);
                    default -> ok = Pollinations.downloadAuth(cfg, prompt, width, height, seed, out);
                }
                if (ok && Files.exists(out) && Files.size(out) > 20_000) {
                    if (log != null) log.accept("imagen: " + out.getFileName() + " <- " + id);
                    return id;
                }
                Files.deleteIfExists(out);
                fallos.add(id + " (respuesta invalida)");
            } catch (Exception e) {
                fallos.add(id + " (" + (e.getMessage() == null ? "error" : e.getMessage().split("\\n")[0]) + ")");
            }
            if (log != null) log.accept("  " + id + " -> FALLO (" + fallos.get(fallos.size() - 1) + ")");
        }
        return "";
    }

    private static List<String> orden(Config cfg) {
        String raw = cfg.get("imagen.orden", "gemini;openai;pollinations");
        List<String> out = new ArrayList<>();
        for (String s : raw.split(";")) {
            String t = s.trim();
            if (!t.isEmpty() && !out.contains(t)) out.add(t);
        }
        return out;
    }
}