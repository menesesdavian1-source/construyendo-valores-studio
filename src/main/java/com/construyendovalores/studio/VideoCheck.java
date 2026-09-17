package com.construyendovalores.studio;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Control de calidad antes de subir un video: estructura, orientación,
 * resolución mínima, pista de audio y silencio (evita videos rotos al público).
 */
public final class VideoCheck {

    public record Result(boolean ok, List<String> errores, List<String> avisos,
                         double duracionSegundos, long tamanioBytes) {}

    public static Result check(Config cfg, Path video, Double esperadoSegundos) {
        List<String> errores = new ArrayList<>();
        List<String> avisos = new ArrayList<>();
        if (video == null || !Files.exists(video)) {
            return new Result(false, List.of("No existe el archivo: " + video), avisos, 0, 0);
        }
        long size;
        try {
            size = Files.size(video);
        } catch (Exception e) {
            return new Result(false, List.of("No se puede leer el archivo: " + e.getMessage()), avisos, 0, 0);
        }
        if (size < 50_000) {
            errores.add("Archivo sospechosamente pequeño (" + size + " bytes).");
        }

        String probe = ffprobe(cfg);
        StringBuilder sb = new StringBuilder();
        int rc;
        try {
            rc = Util.run(List.of(probe, "-v", "error", "-print_format", "json",
                    "-show_format", "-show_streams", video.toString()), null, sb);
        } catch (Exception e) {
            return new Result(false, List.of("ffprobe no disponible: " + e.getMessage()), avisos, 0, size);
        }
        if (rc != 0 || sb.toString().isBlank()) {
            errores.add("ffprobe no pudo leer el video (código " + rc + ").");
            return new Result(false, errores, avisos, 0, size);
        }
        String texto = sb.toString();
        int jsonStart = texto.indexOf('{');
        if (jsonStart < 0) {
            errores.add("ffprobe devolvió una respuesta inesperada.");
            return new Result(false, errores, avisos, 0, size);
        }
        String json = texto.substring(jsonStart);

        double dur = 0;
        int ancho = 0, alto = 0;
        boolean hayVideo = false, hayAudio = false;
        try {
            JsonObject o = JsonParser.parseString(json).getAsJsonObject();
            if (o.has("format")) {
                JsonObject fmt = o.getAsJsonObject("format");
                if (fmt.has("duration")) dur = fmt.get("duration").getAsDouble();
            }
            JsonArray streams = o.has("streams") ? o.getAsJsonArray("streams") : new JsonArray();
            for (var it = streams.iterator(); it.hasNext(); ) {
                JsonObject st = ((JsonObject) it.next());
                String tipo = st.has("codec_type") ? st.get("codec_type").getAsString() : "";
                if ("video".equals(tipo)) {
                    hayVideo = true;
                    ancho = st.has("width") ? st.get("width").getAsInt() : 0;
                    alto = st.has("height") ? st.get("height").getAsInt() : 0;
                } else if ("audio".equals(tipo)) {
                    hayAudio = true;
                }
            }
        } catch (Exception e) {
            errores.add("No se pudo interpretar ffprobe: " + e.getMessage());
        }

        if (!hayVideo) errores.add("El video no tiene pista de video.");
        if (hayVideo && (ancho <= 0 || alto <= 0)) errores.add("Resolución desconocida.");
        if (hayVideo && ancho > alto) errores.add("El video está horizontal (" + ancho + "x" + alto + "); debe ser vertical.");
        if (hayVideo && ancho > 0 && alto > 0 && alto < 1280) {
            avisos.add("En resolución " + ancho + "x" + alto + " (se recomienda 1080x1920).");
        }
        if (!hayAudio) {
            errores.add("El video no tiene pista de audio.");
        } else {
            StringBuilder vol = new StringBuilder();
            Util.run(List.of(cfg.ffmpeg(), "-v", "info", "-i", video.toString(),
                    "-map", "0:a", "-af", "volumedetect", "-f", "null", "NUL"), null, vol);
            double maxDb = parseDb(vol.toString(), "max_volume");
            if (maxDb < -50) {
                errores.add("El audio parece silencioso (máximo " + maxDb + " dB). ¿Se generó bien la voz?");
            }
        }

        if (esperadoSegundos != null && dur > 0) {
            double tol = Math.max(8, dur * 0.12);
            if (Math.abs(dur - esperadoSegundos) > tol) {
                avisos.add("Duración inesperada: esperado ~" + String.format(java.util.Locale.ROOT, "%.0f", esperadoSegundos)
                        + " s, obtenido " + String.format(java.util.Locale.ROOT, "%.1f", dur) + " s.");
            }
        }
        return new Result(errores.isEmpty(), errores, avisos, dur, size);
    }

    private static double parseDb(String log, String label) {
        for (String line : log.split("\\r?\\n")) {
            int i = line.indexOf(label + ":");
            if (i >= 0) {
                String rest = line.substring(i + label.length() + 1)
                        .replace("dB", "").replace("inf", "0").trim();
                try {
                    return Double.parseDouble(rest);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return 0.0; // no se pudo medir: no se asume silencio
    }

    private static String ffprobe(Config cfg) {
        String ff = cfg.ffmpeg();
        if (ff == null || ff.isBlank()) return "ffprobe";
        Path p = Paths.get(ff);
        String name = p.getFileName().toString().replaceFirst("(?i)ffmpeg", "ffprobe");
        Path probe = p.getParent() == null ? Paths.get(name) : p.getParent().resolve(name);
        return Files.exists(probe) ? probe.toString() : "ffprobe";
    }

    private VideoCheck() {}
}