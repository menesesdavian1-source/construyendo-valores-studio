package com.construyendovalores.studio;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Compilación semanal (--compilacion): une con ffmpeg los videos completos que
 * ya se subieron en la última semana (según metricas.csv) en un video largo de
 * 10-15 minutos, ideal para retención y para los programas de monetización.
 *
 * No usa la fecha de creación del archivo sino metricas.csv (fecha de subida),
 * así la compilación nunca adelanta videos que todavía no salieron al aire.
 */
public final class Compilacion {

    private static final int MIN = 5;
    private static final int MAX = 10;

    /** Concatena los videos de la semana. Devuelve null si aún no hay suficientes. */
    public static Path crear(Config cfg, Path dir, Consumer<String> log) throws Exception {
        List<Path> videos = videosDeLaSemana(log);
        if (videos.size() < MIN) {
            log.accept("COMPILACION | solo hay " + videos.size() + " videos de la semana (mínimo " + MIN
                    + "). Se espera al domingo.");
            return null;
        }
        if (videos.size() > MAX) {
            videos = videos.subList(videos.size() - MAX, videos.size());
        }

        Files.createDirectories(dir);
        Path lista = dir.resolve("compilacion_lista.txt");
        StringBuilder sb = new StringBuilder();
        for (Path v : videos) {
            String p = v.toAbsolutePath().toString().replace("\\", "/").replace("'", "''");
            sb.append("file '").append(p).append("'\n");
        }
        Files.write(lista, sb.toString().getBytes(StandardCharsets.UTF_8));

        Path out = dir.resolve("Compilación de canciones y cuentos - " + LocalDate.now() + ".mp4");

        StringBuilder outLog = new StringBuilder();
        List<String> cmd = new ArrayList<>();
        cmd.add(cfg.ffmpeg());
        cmd.add("-y");
        cmd.add("-f"); cmd.add("concat");
        cmd.add("-safe"); cmd.add("0");
        cmd.add("-i"); cmd.add(lista.toString());
        cmd.add("-c"); cmd.add("copy");
        cmd.add(out.toString());
        log.accept("COMPILACION | uniendo " + videos.size() + " videos (" + out.getFileName() + ")...");
        int rc = Util.run(cmd, dir, outLog);
        if (rc != 0) {
            log.accept("  Unión directa falló; re-codificando...");
            List<String> cmd2 = new ArrayList<>(List.of(cfg.ffmpeg(), "-y", "-f", "concat", "-safe", "0",
                    "-i", lista.toString(),
                    "-c:v", "libx264", "-preset", "veryfast", "-crf", "20", "-pix_fmt", "yuv420p",
                    "-c:a", "aac", "-b:a", "192k", "-movflags", "+faststart",
                    out.toString()));
            rc = Util.run(cmd2, dir, outLog);
        }
        if (rc != 0) {
            String[] tail = outLog.toString().split("\\r?\\n");
            int from = Math.max(0, tail.length - 10);
            StringBuilder msg = new StringBuilder("ffmpeg no pudo crear la compilación (código " + rc + "):\n");
            for (int i = from; i < tail.length; i++) msg.append(tail[i]).append('\n');
            throw new IllegalStateException(msg.toString());
        }
        log.accept("COMPILACION | listo: " + out.toAbsolutePath());
        return out;
    }

    /** Videos completos subidos en los últimos 7 días según metricas.csv. */
    private static List<Path> videosDeLaSemana(Consumer<String> log) {
        List<Path> out = new ArrayList<>();
        Path csv = Metricas.rutaCSV();
        try {
            if (!Files.exists(csv)) {
                log.accept("COMPILACION | sin metricas.csv todavía: no hay historial de subidas.");
                return out;
            }
            List<String> lineas = Files.readAllLines(csv, StandardCharsets.UTF_8);
            LocalDate limite = LocalDate.now().minusDays(7);
            for (int i = 1; i < lineas.size(); i++) {
                List<String> f = DailyVideo.splitCsv(lineas.get(i));
                if (f.size() < 5) continue;
                if (!"SUBIDO".equals(f.get(4))) continue;
                String tipo = f.get(1);
                String titulo = f.get(2);
                if ((tipo != null && tipo.contains("Short")) || (titulo != null && titulo.contains("#Shorts"))) {
                    continue;
                }
                try {
                    if (LocalDate.parse(f.get(0)).isBefore(limite)) continue;
                } catch (Exception e) {
                    continue;
                }
                if (f.get(3).isBlank()) continue;
                Path v = Path.of(f.get(3));
                if (!Files.isRegularFile(v)) continue;
                out.add(v);
            }
            out.sort((a, b) -> {
                try {
                    return Long.compare(Files.getLastModifiedTime(a).toMillis(),
                            Files.getLastModifiedTime(b).toMillis());
                } catch (Exception e) {
                    return 0;
                }
            });
        } catch (Exception e) {
            log.accept("COMPILACION | no se pudo leer metricas.csv: " + e.getMessage());
        }
        return out;
    }

    private Compilacion() {}
}