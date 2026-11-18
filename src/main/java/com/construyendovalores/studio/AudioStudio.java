package com.construyendovalores.studio;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Cadena de voz "de estudio" para narraciones y canciones:
 * alta impedancia (quita ruido grave), ecualizacion (limpia + presencia),
 * eco breve de sala y loudness I=-16 LUFS. Aplica a rondas y cuentos por igual.
 */
public final class AudioStudio {

    /** Pule un mp3 de voz generado por TTS y devuelve la ruta del archivo estudiado. */
    public static Path polishVoice(Config cfg, Path src, Path dst, StringBuilder log) throws Exception {
        List<String> cmd = List.of(cfg.ffmpeg(), "-y", "-i", src.toString(),
                "-af", "highpass=f=70,equalizer=f=260:t=q:w=1.1:g=-2,"
                        + "equalizer=f=3000:t=q:w=1:g=1.5,"
                        + "aecho=0.72:0.62:60|115:0.26|0.19,"
                        + "loudnorm=I=-16:TP=-1.5:LRA=11",
                "-c:a", "libmp3lame", "-q:a", "4", dst.toString());
        int code = Util.run(cmd, null, log);
        if (code != 0) {
            throw new RuntimeException("Fallo el pulido de voz:\n" + log);
        }
        return dst;
    }

    /** Duracion de un mp3 de voz ya estudiado, con pausa natural al final. */
    public static double durationWithPause(Config cfg, Path mp3) {
        return Tts.durationWithPause(mp3, Util.ffprobeBin(cfg));
    }

    /** Verifica que la ruta destino exista (comodidad para callers de oro). */
    static boolean existe(Path p) {
        return Files.exists(p);
    }

    private AudioStudio() {}
}