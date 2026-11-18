package com.construyendovalores.studio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class Util {
    private Util() {}

    public static String read(Path p) {
        try {
            return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo leer " + p, e);
        }
    }

    public static void write(Path p, String content) {
        try {
            if (p.getParent() != null) Files.createDirectories(p.getParent());
            Files.write(p, content.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException("No se pudo escribir " + p, e);
        }
    }

    public static int run(List<String> cmd, Path workDir, StringBuilder log) {
        if (log != null) log.append("$ ").append(String.join(" ", cmd)).append("\n");
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            if (workDir != null) pb.directory(workDir.toFile());
            pb.redirectErrorStream(true);
            Process p = pb.start();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    if (log != null) log.append(line).append("\n");
                }
            }
            return p.waitFor();
        } catch (Exception e) {
            throw new RuntimeException("Fallo al ejecutar: " + String.join(" ", cmd), e);
        }
    }

    public static double mediaDurationSeconds(Path media, String ffprobeBin) {
        List<String> cmd = List.of(ffprobeBin, "-v", "quiet", "-show_entries", "format=duration",
                "-of", "csv=p=0", media.toString());
        StringBuilder sb = new StringBuilder();
        run(cmd, null, sb);
        for (String line : sb.toString().split("\\r?\\n")) {
            String t = line.trim();
            if (!t.isEmpty()) {
                try {
                    return Double.parseDouble(t);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return 2.0;
    }

    /** Ruta del ffprobe junto al ffmpeg configurado (fallback: "ffprobe"). */
    public static String ffprobeBin(Config cfg) {
        String ff = cfg.ffmpeg();
        if (ff == null || ff.isBlank()) return "ffprobe";
        String f2 = ff.replaceFirst("(?i)ffmpeg\\.exe$", "ffprobe.exe")
                .replaceFirst("(?i)ffmpeg$", "ffprobe");
        java.nio.file.Path probe = java.nio.file.Paths.get(f2);
        return Files.exists(probe) ? f2 : "ffprobe";
    }

    /** Ejecuta un comando y captura su salida completa sin bloquear al proceso. */
    public static String capture(List<String> cmd, Path workDir) {
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            if (workDir != null) pb.directory(workDir.toFile());
            pb.redirectErrorStream(true);
            Path tmp = java.nio.file.Files.createTempFile("cvout", ".txt");
            pb.redirectOutput(tmp.toFile());
            Process p = pb.start();
            boolean ok = p.waitFor(180, java.util.concurrent.TimeUnit.SECONDS);
            if (!ok) p.destroyForcibly();
            String out = Files.exists(tmp) ? new String(Files.readAllBytes(tmp), StandardCharsets.UTF_8) : "";
            Files.deleteIfExists(tmp);
            return out;
        } catch (Exception e) {
            return "";
        }
    }
}