package com.construyendovalores.studio;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Diseño de sonido de cine: ambiente (room tone), transiciones (whoosh+boom) y
 *  optimizaciones de voz. Todo se genera con ffmpeg, sin archivos externos. */
public final class SoundDesign {

    /** Fondo de ambiente suave (aire), bucleable. Crea fondo_ambiente.wav. */
    public static Path ensureAmbience(Config cfg, Path workDir) throws Exception {
        Path out = workDir.resolve("fondo_ambiente.wav").toAbsolutePath();
        if (Files.exists(out) && Files.size(out) > 1_000) return out;
        String ffmpeg = cfg.ffmpeg();
        List<String> cmd = List.of(ffmpeg, "-y",
                "-f", "lavfi", "-i",
                "anoisesrc=colour=pink:sample_rate=48000:amplitude=0.6:duration=60",
                "-filter_complex",
                "lowpass=f=240,tremolo=f=0.55:d=0.28,volume=2.2[a]",
                "-map", "[a]", "-c:a", "pcm_s16le", out.toString());
        StringBuilder log = new StringBuilder();
        int code = Util.run(cmd, workDir, log);
        if (code != 0) throw new RuntimeException("Fallo al crear ambiente: " + tail(log));
        return out;
    }

    /** Muestra de transición: whoosh de aire + golpe grave sutil (1 segundo). */
    public static Path ensureWhoosh(Config cfg, Path workDir) throws Exception {
        Path out = workDir.resolve("fx_whoosh.wav").toAbsolutePath();
        if (Files.exists(out) && Files.size(out) > 1_000) return out;
        String ffmpeg = cfg.ffmpeg();
        List<String> cmd = List.of(ffmpeg, "-y",
                "-f", "lavfi", "-i",
                "anoisesrc=colour=pink:sample_rate=48000:amplitude=0.75:duration=1.0",
                "-f", "lavfi", "-i",
                "sine=frequency=52:sample_rate=48000:duration=1.0",
                "-filter_complex",
                "[0:a]lowpass=f=2000,afade=t=out:st=0.55:d=0.45,volume=0.9[w];"
                        + "[1:a]volume=0.55,afade=t=in:st=0:d=0.03,afade=t=out:st=0.22:d=0.78[b];"
                        + "[w][b]amix=inputs=2:duration=first:normalize=0,afade=t=in:st=0:d=0.14[fx]",
                "-map", "[fx]", "-t", "1.0", "-c:a", "pcm_s16le", out.toString());
        StringBuilder log = new StringBuilder();
        int code = Util.run(cmd, workDir, log);
        if (code != 0) throw new RuntimeException("Fallo al crear transición: " + tail(log));
        return out;
    }

    /** Pista finita con un whoosh al inicio de cada escena (salvo la intro), sincronizada. */
    public static Path makeTransiciones(Config cfg, Path workDir, List<Scene> scenes) throws Exception {
        ensureAmbience(cfg, workDir);
        Path whoosh = ensureWhoosh(cfg, workDir);
        Path sfx = workDir.resolve("sfx_mix.wav").toAbsolutePath();
        if (Files.exists(sfx)) Files.delete(sfx);

        String ffmpeg = cfg.ffmpeg();
        List<String> parts = new ArrayList<>();
        int sceneIdx = 0;
        for (Scene sc : scenes) {
            double dur = Math.max(1.2, sc.audioSeconds + 0.45);
            if (sceneIdx > 0) {
                // insertamos el whoosh en los primeros 1.0s de la escena
                String w = workDir.resolve("fx_esc_" + sceneIdx + ".wav").toAbsolutePath().toString();
                List<String> cmd = List.of(ffmpeg, "-y",
                        "-i", whoosh.toString(),
                        "-af", String.format(Locale.US,
                                "apad=pad_dur=%.2f,atrim=0:%.2f,asetpts=N/SR/TB", dur, dur),
                        "-t", String.format(Locale.US, "%.2f", dur),
                        "-c:a", "pcm_s16le", w);
                StringBuilder log = new StringBuilder();
                int code = Util.run(cmd, workDir, log);
                if (code != 0) throw new RuntimeException("Fallo fx escena: " + tail(log));
                parts.add(w);
            } else {
                parts.add(null); // escena 0: silencio (el gancho entra solo)
            }
            sceneIdx++;
        }

        List<String> concat = new ArrayList<>();
        concat.add(ffmpeg);
        concat.add("-y");
        int n = 0;
        for (int i = 0; i < scenes.size(); i++) {
            if (parts.get(i) != null) {
                concat.add("-i");
                concat.add(parts.get(i));
                n++;
            }
        }
        if (n == 0) return null;
        StringBuilder fg = new StringBuilder();
        for (int i = 0; i < n; i++) fg.append("[").append(i).append(":a]");
        fg.append("concat=n=").append(n).append(":v=0:a=1[out]");
        concat.add("-filter_complex");
        concat.add(fg.toString());
        concat.add("-map"); concat.add("[out]");
        concat.add("-c:a"); concat.add("pcm_s16le");
        concat.add(sfx.toString());
        StringBuilder log = new StringBuilder();
        int code = Util.run(concat, workDir, log);
        if (code != 0) throw new RuntimeException("Fallo al unir transiciones: " + tail(log));
        return sfx;
    }

    private static String tail(StringBuilder log) {
        String s = log.toString();
        return s.length() > 320 ? s.substring(s.length() - 320) : s;
    }

    private SoundDesign() {}
}