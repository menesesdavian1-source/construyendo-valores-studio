package com.construyendovalores.studio;

import java.nio.file.Path;
import java.util.Properties;

public final class Tts {
    private Tts() {}

    public static void synth(Config cfg, String character, String voice, String text, Path outMp3) {
        String clean = text == null ? "" : text.trim().replaceAll("\\s+", " ");
        if (!clean.isEmpty() && !clean.endsWith(".") && !clean.endsWith("!") && !clean.endsWith("?")) {
            clean += ".";
        }
        StringBuilder log = new StringBuilder();
        java.util.List<String> cmd = new java.util.ArrayList<>();
        for (String part : cfg.pythonCmd().trim().split("\\s+")) cmd.add(part);
        cmd.add("-m");
        cmd.add("edge_tts");
        cmd.add("--voice=" + voice);
        cmd.add("--pitch=" + cfg.voicePitchFor(character));
        cmd.add("--rate=" + cfg.voiceRateFor(character));
        cmd.add("--volume=" + cfg.voiceVolumeFor(character));
        cmd.add("--text=" + clean);
        cmd.add("--write-media=" + outMp3.toString());
        int code = Util.run(cmd, null, log);
        if (code != 0) {
            throw new RuntimeException("edge-tts terminó con código " + code + "\n" + log);
        }
    }

    /** Progresa un narration de edge-tts simple: aplica 15% de silencio al final. */
    public static double durationWithPause(Path mp3, String ffprobe) {
        return Util.mediaDurationSeconds(mp3, ffprobe) * 1.15 + 0.2;
    }
}