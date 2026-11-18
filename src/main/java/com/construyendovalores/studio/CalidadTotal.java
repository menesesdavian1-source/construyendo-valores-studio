package com.construyendovalores.studio;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Automatización de calidad de una producción completa:
 *  - técnica: VideoCheck (estructura, orientación, resolución, audio presente).
 *  - audio: nivel medio y pico (distorsión / silencio).
 *  - índice global 0-100 (IndiceCalidad: técnica + LUFS + movimiento).
 *  - subtítulos: existencia, cobertura del video y tiempos dentro de la duración.
 *  - piezas: miniatura y portada generadas.
 *  - contenido: si existe guion.json, una IA (Multi-IA) revisa valor, estructura,
 *    tono infantil, gancho y cierre; se suma como aviso si la nota es baja.
 * Escribe informe_calidad.md en el directorio. Apto = sin errores.
 */
public final class CalidadTotal {

    public record RevisionIA(Double nota, List<String> mejoras) {}

    public record Informe(String nombre, boolean apto, List<String> errores, List<String> avisos,
                          double indice, Double notaIA, double mejor, boolean superaAnterior,
                          List<String> mejorasIA) {}

    public static Informe evaluar(Config cfg, Path dir, Double esperado, Consumer<String> log) throws Exception {
        List<String> errores = new ArrayList<>();
        List<String> avisos = new ArrayList<>();

        Path video = localizarVideo(dir);
        if (video == null) {
            errores.add("No se encontró ningún video .mp4 final en " + dir);
            return emitir(cfg, dir, video, errores, avisos, 0, null, false, new ArrayList<>(), -1, log);
        }

        VideoCheck.Result c = VideoCheck.check(cfg, video, esperado);
        errores.addAll(c.errores());
        avisos.addAll(c.avisos());

        String ff = cfg.ffmpeg();
        StringBuilder vol = new StringBuilder();
        Util.run(List.of(ff, "-v", "info", "-i", video.toString(),
                "-map", "0:a", "-af", "volumedetect", "-f", "null", "NUL"), null, vol);
        Double pico = parseDb(vol.toString(), "max_volume");
        Double media = parseDb(vol.toString(), "mean_volume");
        if (pico != null && pico > -0.5) {
            avisos.add("Audio muy fuerte (pico " + pico + " dB); riesgo de distorsión en altavoces.");
        }
        if (media != null && media < -35) {
            avisos.add("Audio flojo (media " + media + " dB).");
        }

        IndiceCalidad.Resultado iq = IndiceCalidad.evaluar(cfg, video, log);
        double indice = iq.indice();

        Path srt = dir.resolve("subtitulos.srt");
        if (Files.exists(srt)) {
            String texto = Files.readString(srt, StandardCharsets.UTF_8);
            Matcher m = Pattern.compile("(\\d+):(\\d{2}):(\\d{2}),(\\d{3})\\s+-->\\s+"
                    + "(\\d+):(\\d{2}):(\\d{2}),(\\d{3})").matcher(texto);
            long cues = 0;
            double primerInicio = -1, ultimoFin = -1;
            while (m.find()) {
                cues++;
                double ini = seg(m.group(1), m.group(2), m.group(3), m.group(4));
                double fin = seg(m.group(5), m.group(6), m.group(7), m.group(8));
                if (primerInicio < 0) primerInicio = ini;
                ultimoFin = Math.max(ultimoFin, fin);
            }
            if (cues == 0) {
                avisos.add("subtitulos.srt existe pero no tiene señales de tiempo válidas.");
            } else {
                double dur = c.duracionSegundos();
                if (dur > 0 && ultimoFin > dur + 1.0) {
                    avisos.add("Subtítulos se extienden más allá del video (último fin "
                            + String.format(Locale.ROOT, "%.1f", ultimoFin) + " s de "
                            + String.format(Locale.ROOT, "%.1f", dur) + " s).");
                }
                if (dur > 0 && primerInicio >= 0) {
                    double cobertura = (ultimoFin - primerInicio) / dur;
                    if (cobertura < 0.4) {
                        avisos.add("Subtítulos solo cubren el " + String.format(Locale.ROOT, "%.0f", cobertura * 100)
                                + "% del video; demasiado silencio sin diálogo.");
                    }
                }
            }
        } else {
            avisos.add("No hay subtitulos.srt junto al video.");
        }

        if (archivoVerde(dir, "miniatura_1.png", 20_000) == null
                && archivoVerde(dir, "portada", 20_000) == null
                && archivoVerde(dir, "portada_AI", 20_000) == null) {
            avisos.add("Falta miniatura (miniatura_1.png) y portada (portada*.png) para la publicación.");
        }

        Double notaIA = null;
        List<String> mejorasIA = new ArrayList<>();
        Path guion = dir.resolve("guion.json");
        if (Files.exists(guion)) {
            try {
                RevisionIA rev = revisionIA(cfg, Files.readString(guion, StandardCharsets.UTF_8));
                notaIA = rev.nota();
                mejorasIA.addAll(rev.mejoras());
                if (notaIA != null && notaIA < 6.5) {
                    avisos.add("Revisión IA del guion: " + String.format(Locale.ROOT, "%.1f", notaIA)
                            + "/10. Revisa las mejoras antes de publicar (mejoras_siguiente.md).");
                }
            } catch (Exception ex) {
                log.accept("CALIDAD | revisión IA no disponible: " + ex.getMessage().split("\\n")[0]);
            }
        }

        double mejor = IndiceCalidad.mejorAnterior(video.toAbsolutePath());
        boolean supera = mejor < 0 || indice >= mejor;
        if (mejor >= 0 && !supera) {
            avisos.add("Índice " + String.format(Locale.ROOT, "%.1f", indice)
                    + " no supera al mejor anterior (" + String.format(Locale.ROOT, "%.1f", mejor)
                    + "). Aplica las mejoras propuestas en el próximo video.");
        }

        return emitir(cfg, dir, video, errores, avisos, indice, notaIA, supera, mejorasIA, mejor, log);
    }

    /** Pide a la primera IA disponible que puntúe el guion (funciona con Groq aunque Gemini no tenga cuota). */
    private static RevisionIA revisionIA(Config cfg, String guionJson) throws Exception {
        String prompt = "Eres el control de calidad de un canal infantil de valores. Puntúa este guion "
                + "(entre 0 y 10 cada nota) con criterio exigente:\n"
                + "valor_educativo (¿se nota claramente el valor?), estructura (inicio-nudo-final), "
                + "tono_infantil (lenguaje natural para 2-6 años), gancho (¿engancha en los primeros 3 s?), "
                + "cierre (¿repite el valor y llama a participar?).\n"
                + "RESPONDE UNICAMENTE con JSON puro, SIN bloques de codigo, SIN markdown, SIN texto extra, "
                + "con esta forma exacta:\n"
                + "{\"nota_total\":7.5,\"notas\":{\"valor_educativo\":8,\"estructura\":7,\"tono_infantil\":8,"
                + "\"gancho\":7,\"cierre\":8},\"mejoras\":[\"frase de mejora 1\",\"frase de mejora 2\"]}\n\n"
                + guionJson;
        String raw = MultiIA.generar(cfg, Prompt.SYSTEM, prompt, s -> { });
        String json = null;
        try {
            json = GeminiParser.extractJson(raw);
        } catch (Exception ex) {
            json = null;
        }
        if (json == null || !json.contains("\"nota_total\"")) {
            int a = raw.indexOf('{');
            int b = raw.lastIndexOf('}');
            if (a >= 0 && b > a) {
                String candidato = raw.substring(a, b + 1);
                if (candidato.contains("\"nota_total\"")) {
                    try {
                        JsonParser.parseString(candidato);
                        json = candidato;
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        if (json == null || !json.contains("\"nota_total\"")) return new RevisionIA(null, new ArrayList<>());
        JsonObject o = JsonParser.parseString(json).getAsJsonObject();
        Double nota = o.has("nota_total") ? o.get("nota_total").getAsDouble() : null;
        List<String> mejoras = new ArrayList<>();
        if (o.has("mejoras") && o.get("mejoras").isJsonArray()) {
            for (var it = o.getAsJsonArray("mejoras").iterator(); it.hasNext(); ) {
                String m = it.next().getAsString();
                if (m != null && !m.isBlank()) mejoras.add(m);
            }
        }
        return new RevisionIA(nota, mejoras);
    }

    private static Informe emitir(Config cfg, Path dir, Path video, List<String> errores, List<String> avisos,
                                  double indice, Double notaIA, boolean supera, List<String> mejorasIA,
                                  double mejor, Consumer<String> log) throws Exception {
        Files.createDirectories(dir);
        String nombre = dir.getFileName() == null ? "?" : dir.getFileName().toString();
        StringBuilder md = new StringBuilder("# Informe de calidad\n\n");
        md.append("- Video: ").append(video == null ? "(no encontrado)" : video.getFileName()).append('\n');
        md.append("- Índice global: ").append(String.format(Locale.ROOT, "%.1f", indice)).append("/100\n");
        if (mejor >= 0) {
            md.append("- Mejor anterior: ").append(String.format(Locale.ROOT, "%.1f", mejor))
                    .append("/100 (regla: cada video debe superarlo)\n");
            md.append("- ¿Supera al anterior? ").append(supera ? "SÍ" : "NO").append('\n');
        }
        md.append("- Revisión IA del guion: ")
                .append(notaIA == null ? "no disponible" : String.format(Locale.ROOT, "%.1f", notaIA) + "/10")
                .append('\n');
        md.append("\n## Errores\n");
        if (errores.isEmpty()) md.append("- ninguno\n");
        for (String e : errores) md.append("- ERROR: ").append(e).append('\n');
        md.append("\n## Avisos\n");
        if (avisos.isEmpty()) md.append("- ninguno\n");
        for (String a : avisos) md.append("- AVISO: ").append(a).append('\n');
        boolean apto = errores.isEmpty();
        md.append("\n## Veredicto\n").append(apto ? "APTO para publicar." : "REVISAR manualmente antes de publicar.").append('\n');
        if (!mejorasIA.isEmpty()) {
            md.append("\n## Mejoras propuestas por la IA para el próximo video\n");
            for (int i = 0; i < mejorasIA.size(); i++) {
                md.append(i + 1).append(". ").append(mejorasIA.get(i)).append('\n');
            }
        }
        Files.writeString(dir.resolve("informe_calidad.md"), md.toString(), StandardCharsets.UTF_8);

        StringBuilder sig = new StringBuilder("# Mejoras para el próximo video\n\n");
        sig.append("Regla: cada publicación debe superar la anterior.\n\n");
        if (mejorasIA.isEmpty() && notaIA == null) {
            sig.append("- (No hubo revisión IA; revisa manualmente este video antes de producir el siguiente).\n");
        }
        for (int i = 0; i < mejorasIA.size(); i++) {
            sig.append(i + 1).append(". ").append(mejorasIA.get(i)).append('\n');
        }
        if (apto) sig.append("\nRecordatorio: este video fue APTO; las mejoras son para destacar aún más.\n");
        else sig.append("\nIMPORTANTE: este video tuvo errores; corrige antes de seguir.\n");
        Files.writeString(dir.resolve("mejoras_siguiente.md"), sig.toString(), StandardCharsets.UTF_8);

        log.accept("CALIDAD | " + (video == null ? dir : video.getFileName())
                + " | índice " + String.format(Locale.ROOT, "%.1f", indice) + "/100 | IA "
                + (notaIA == null ? "n/a" : String.format(Locale.ROOT, "%.1f", notaIA) + "/10")
                + " | supera anterior=" + (mejor < 0 ? "n/a (primera vez)" : (supera ? "SÍ" : "NO"))
                + " | apto=" + (apto ? "SÍ" : "NO con " + errores.size() + " error(es)")
                + (avisos.isEmpty() ? "" : " | avisos=" + avisos.size()));
        log.accept("CALIDAD | informe: " + dir.resolve("informe_calidad.md").toAbsolutePath());
        return new Informe(nombre, apto, errores, avisos, indice, notaIA, mejor, supera, mejorasIA);
    }

    /** ¿Hay un video directamente en dir (no solo en subcarpetas)? */
    public static boolean existeVideoDirecto(Path dir) {
        return localizarVideo(dir) != null;
    }

    /** Evalúa todas las subcarpetas de producción de una raíz y devuelve sus informes. */
    public static List<Informe> evaluarRaiz(Config cfg, Path raiz, Consumer<String> log) throws Exception {
        List<Informe> informes = new ArrayList<>();
        if (raiz == null || !Files.isDirectory(raiz)) return informes;
        try (var stream = Files.list(raiz)) {
            for (Path sub : stream.filter(Files::isDirectory).sorted().toList()) {
                if (vidDirecto(sub) != null) {
                    try {
                        informes.add(evaluar(cfg, sub, null, log));
                    } catch (Exception e) {
                        log.accept("CALIDAD | " + sub.getFileName() + " no evaluado: " + e.getMessage().split("\\n")[0]);
                    }
                }
            }
        }
        return informes;
    }

    /** Elige el .mp4 final (excluye merged/short y se queda con el de mayor tamaño). */
    static Path localizarVideo(Path dir) {
        if (dir != null && Files.isRegularFile(dir) && dir.toString().toLowerCase().endsWith(".mp4")) {
            return dir;
        }
        if (dir == null || !Files.isDirectory(dir)) return null;
        try (var stream = Files.list(dir)) {
            return stream.filter(f -> f.toString().toLowerCase().endsWith(".mp4"))
                    .filter(f -> {
                        String n = f.getFileName().toString().toLowerCase();
                        return !n.contains("merged") && !n.contains("short");
                    })
                    .max(Comparator.comparingLong(f -> safeSize(f)))
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private static Path vidDirecto(Path dir) {
        if (dir == null || !Files.isDirectory(dir)) return null;
        try (var stream = Files.list(dir)) {
            return stream.filter(f -> f.toString().toLowerCase().endsWith(".mp4"))
                    .filter(f -> {
                        String n = f.getFileName().toString().toLowerCase();
                        return !n.contains("merged") && !n.contains("short");
                    })
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private static long safeSize(Path f) {
        try {
            return Files.size(f);
        } catch (Exception e) {
            return 0;
        }
    }

    private static Path archivoVerde(Path dir, String prefijo, long minBytes) {
        if (dir == null || !Files.isDirectory(dir)) return null;
        try (var stream = Files.list(dir)) {
            return stream.filter(f -> f.getFileName().toString().toLowerCase().startsWith(prefijo.toLowerCase()))
                    .filter(f -> safeSize(f) >= minBytes)
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private static double seg(String h, String mi, String s, String ms) {
        return Integer.parseInt(h) * 3600 + Integer.parseInt(mi) * 60
                + Integer.parseInt(s) + Integer.parseInt(ms) / 1000.0;
    }

    private static Double parseDb(String log, String label) {
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
        return null;
    }

    private CalidadTotal() {}
}