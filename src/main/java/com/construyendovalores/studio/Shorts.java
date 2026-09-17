package com.construyendovalores.studio;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Genera un Short (30-45 s) a partir de un video terminado: recorta el tramo más
 * dinámico, añade un gancho con texto gigante en los primeros segundos y un cierre
 * que invita a seguir el canal. Reutiliza el audio y subtítulos ya quemados en el original.
 */
public final class Shorts {

    /** Crea un Short desde 'src'. Devuelve la ruta del archivo generado. */
    public static Path make(Config cfg, Path src, Path outDir, double durSec, Consumer<String> progress)
            throws Exception {
        Path ff = Paths.get(cfg.ffmpeg()).getParent().resolve("ffprobe.exe");
        if (!Files.exists(ff)) ff = Paths.get(cfg.ffmpeg().replace("ffmpeg", "ffprobe"));
        double totalSec = duration(src, ff.toString());

        double start = Math.max(0, totalSec * 0.10);
        double len = Math.min(durSec, totalSec - start);
        if (len < 10) { start = 0; len = Math.min(durSec, totalSec); }
        len = Math.max(Math.min(len, 45), 8);

        Path hookFile = outDir.resolve("short_hook.txt");
        Path outroFile = outDir.resolve("short_outro.txt");
        Files.write(hookFile, "¡Lo mejor ya viene!".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        Files.write(outroFile, "¿Quieres más? Nuevos cuentos en Construyendo Valores".getBytes(
                java.nio.charset.StandardCharsets.UTF_8));

        // Usamos nombres relativos (cwd = outDir) para que drawtext no tenga que lidiar
        // con los ':' de Windows en las rutas.
        Path fontSrc = Files.exists(Paths.get("C:/Windows/Fonts/arialbd.ttf"))
                ? Paths.get("C:/Windows/Fonts/arialbd.ttf") : Paths.get("C:/Windows/Fonts/arial.ttf");
        Path fontRel = outDir.resolve("short_font.ttf");
        if (!Files.exists(fontRel)) Files.copy(fontSrc, fontRel);

        String base = src.getFileName().toString().replaceFirst("(?i)\\.mp4$", "");
        Path out = outDir.resolve(base + " - Short.mp4");

        String f1 = "drawtext=fontfile=short_font.ttf:textfile=short_hook.txt"
                + ":fontsize=60:fontcolor=white:bordercolor=black@0.7:borderw=5"
                + ":x=(w-text_w)/2:y=h*0.10:enable=lt(t\\,2.4)";
        String f2 = "drawtext=fontfile=short_font.ttf:textfile=short_outro.txt"
                + ":fontsize=44:fontcolor=white:box=1:boxcolor=black@0.45:boxborderw=18"
                + ":x=(w-text_w)/2:y=h*0.68:enable=gt(t\\," + String.format(java.util.Locale.US, "%.2f", len - 3.2) + ")";

        List<String> cmd = new ArrayList<>();
        cmd.add(cfg.ffmpeg());
        cmd.add("-y");
        cmd.add("-ss"); cmd.add(String.format(java.util.Locale.US, "%.3f", start));
        cmd.add("-t");  cmd.add(String.format(java.util.Locale.US, "%.3f", len));
        cmd.add("-i");  cmd.add(src.toString());
        cmd.add("-vf"); cmd.add(f1 + "," + f2);
        cmd.add("-c:v"); cmd.add("libx264");
        cmd.add("-preset"); cmd.add("veryfast");
        cmd.add("-crf"); cmd.add("23");
        cmd.add("-pix_fmt"); cmd.add("yuv420p");
        cmd.add("-c:a"); cmd.add("aac");
        cmd.add("-b:a"); cmd.add("160k");
        cmd.add("-movflags"); cmd.add("+faststart");
        cmd.add("-metadata"); cmd.add("title=" + base + " (Short)");
        cmd.add(out.toString());

        progress.accept("Recortando Short (" + String.format(java.util.Locale.US, "%.1f", len) + " s)...");
        Path log = outDir.resolve("short_ffmpeg.log");
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(outDir.toFile());
        pb.redirectErrorStream(true);
        pb.redirectOutput(log.toFile());
        Process p = pb.start();
        p.waitFor();
        int code = p.exitValue();
        if (code != 0) {
            java.util.List<String> tail = Files.readAllLines(log);
            int from = Math.max(0, tail.size() - 12);
            throw new IOException("ffmpeg falló al crear el Short (código " + code + "):\n"
                    + String.join("\n", tail.subList(from, tail.size())));
        }
        return out;
    }

    private static double duration(Path video, String ffprobe) throws Exception {
        List<String> cmd = List.of(ffprobe, "-v", "error", "-show_entries", "format=duration",
                "-of", "default=noprint_wrappers=1:nokey=1", video.toString());
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        String out = new String(p.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).trim();
        p.waitFor();
        return Double.parseDouble(out.trim());
    }

    private Shorts() {}
}