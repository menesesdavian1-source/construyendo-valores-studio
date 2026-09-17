package com.construyendovalores.studio;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

/**
 * Música de fondo: usa un archivo del usuario si existe (config "music.path",
 * o 'musica.mp3'/'musica/' junto a la carpeta de salida) y si no, sintetiza
 * una melodía propia amable de 12s con ffmpeg. Coste: cero.
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

        String notes = "0.20*sin(2*PI*523.25*t)*exp(-2.6*t)"
                + "+0.18*sin(2*PI*659.26*t)*exp(-2.6*(t-0.30))"
                + "+0.16*sin(2*PI*783.99*t)*exp(-2.6*(t-0.65))"
                + "+0.13*sin(2*PI*1046.50*t)*exp(-2.6*(t-0.98))"
                + "+0.10*sin(2*PI*1318.51*t)*exp(-2.6*(t-1.30))";

        List<String> cmd = List.of(ffmpeg, "-y", "-f", "lavfi",
                "-i", "aevalsrc=" + notes + ":d=12:s=44100",
                "-af", "lowpass=f=2400,afade=t=in:d=1,afade=t=out:st=11:d=1,volume=0.55",
                "-c:a", "libmp3lame", "-q:a", "5", synth.toString());
        StringBuilder lg = new StringBuilder();
        int code = Util.run(cmd, workDir, lg);
        if (code != 0) return null;
        return Files.exists(synth) ? synth : null;
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