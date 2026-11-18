package com.construyendovalores.studio;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

/**
 * Música de fondo: usa un archivo del usuario si existe (config "music.path",
 * o 'musica.mp3'/'musica/' junto a la carpeta de salida) y si no, compone un
 * loop amable de 8 segundos (progresión C-Am-F-G con bajo y notas tipo pizzicato)
 * generado sintéticamente con ffmpeg. Coste: cero.
 */
public final class Music {

    public static Path ensure(String ffmpeg, String configured, Path workDir) {
        if (configured != null && !configured.isBlank()) {
            Path p = Paths.get(configured);
            if (Files.exists(p)) return p;
        }
        Path found = findLocal(workDir);
        if (found != null) return found;

        Path synth = workDir.resolve("fondo_musica.mp3");
        if (Files.exists(synth)) return synth;

        try {
            synthBed(ffmpeg, workDir, synth);
            return Files.exists(synth) ? synth : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static void synthBed(String ffmpeg, Path workDir, Path synth) {
        double[] notes = {
                261.63, 329.63, 392.00, 523.25, // C mayor
                220.00, 261.63, 329.63, 440.00, // la menor
                174.61, 220.00, 261.63, 349.23, // F mayor
                196.00, 246.94, 293.66, 392.00  // G mayor
        };
        double root = 0.5;
        StringBuilder e = new StringBuilder();
        for (int i = 0; i < notes.length; i++) {
            double t0 = i * root;
            if (i > 0) e.append("+");
            e.append("0.16*sin(2*PI*").append(notes[i]).append("*t)*")
                    .append("exp(-3.8*max(0,(t-").append(t0).append(")))*")
                    .append("if(gt(t,").append(t0).append("),1,0)*")
                    .append("if(lt(t,").append(t0 + 0.6).append("),1,0)");
        }
        e.append("+0.30*(sin(2*PI*if(lt(t,2),130.81,if(lt(t,4),110.00,if(lt(t,6),87.31,98.00)))*t)*exp(-0.30*t))");

        List<String> cmd = List.of(ffmpeg, "-y", "-f", "lavfi",
                "-i", "aevalsrc=" + e + ":d=8:s=44100",
                "-af", "lowpass=f=3200,afade=t=in:st=0:d=0.8,afade=t=out:st=7.2:d=0.8,volume=0.55",
                "-c:a", "libmp3lame", "-q:a", "5", synth.toString());
        StringBuilder lg = new StringBuilder();
        int code = Util.run(cmd, workDir, lg);
        if (code != 0) {
            String s = lg.toString();
            String tail = s.length() > 320 ? s.substring(s.length() - 320) : s;
            throw new RuntimeException("Fallo al componer el fondo: " + tail);
        }
    }

    private static Path findLocal(Path workDir) {
        for (String ext : new String[]{"mp3", "wav", "ogg", "m4a"}) {
            Path p = workDir.resolve("musica." + ext);
            if (Files.exists(p)) return p;
        }
        Path dir = workDir.resolve("musica");
        if (Files.isDirectory(dir)) {
            try (Stream<Path> s = Files.list(dir)) {
                for (Path p : (Iterable<Path>) s::iterator) {
                    String n = p.getFileName().toString().toLowerCase();
                    if (n.endsWith(".mp3") || n.endsWith(".wav") || n.endsWith(".ogg")) {
                        return p;
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private Music() {}
}