package com.construyendovalores.studio;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Sintesis de voz.
 *  - Motor por defecto: edge-tts (neural, gratuito), con voz/tono/ritmo por personaje.
 *  - Motor opcional "openai": voces tts-1-hd mas organicas si hay openai.apiKey.
 *  - Siempre cae en edge-tts si el motor premium falla (la grabacion nunca se pierde).
 */
public final class Tts {
    private Tts() {}

    /** Voces OpenAI por personaje (tts-1-hd). Config: openai.voice.<personaje>. */
    private static final Map<String, String> OPENAI_VOICES = Map.ofEntries(
            Map.entry("intro", "onyx"), Map.entry("casquito", "alloy"), Map.entry("tito", "onyx"),
            Map.entry("sofia", "shimmer"), Map.entry("nico", "alloy"), Map.entry("narrador", "onyx"),
            Map.entry("luna", "shimmer"), Map.entry("pipo", "alloy"), Map.entry("tico", "nova"),
            Map.entry("bruno", "onyx"), Map.entry("memo", "onyx"), Map.entry("coco", "nova"),
            Map.entry("rubi", "shimmer"), Map.entry("lumi", "nova"));

    /** Voces ElevenLabs por personaje (multilingüe). Config: elevenlabs.voice.<personaje>. */
    private static final Map<String, String> ELEVEN_VOICES = Map.ofEntries(
            Map.entry("intro", "ErXwobaYiN019PkySvjV"), Map.entry("narrador", "ErXwobaYiN019PkySvjV"),
            Map.entry("casquito", "21m00Tcm4TlvDq8ikWAM"), Map.entry("tito", "ErXwobaYiN019PkySvjV"),
            Map.entry("sofia", "21m00Tcm4TlvDq8ikWAM"), Map.entry("nico", "TxGEqnHWrfWFTfGW9XjX"),
            Map.entry("luna", "EXAVITQu4vr4cSD2ZMEz"), Map.entry("pipo", "pNInz6obpgDQGcFmaJgB"),
            Map.entry("tico", "iP95p4xoKVk53GoZ742B"), Map.entry("bruno", "ErXwobaYiN019PkySvjV"),
            Map.entry("memo", "nPczCjzI2devNBz1zQrb"), Map.entry("coco", "EXAVITQu4vr4cSD2ZMEz"),
            Map.entry("rubi", "EXAVITQu4vr4cSD2ZMEz"), Map.entry("lumi", "21m00Tcm4TlvDq8ikWAM"));

    public static void synth(Config cfg, String character, String voice, String text, Path outMp3) {
        synth(cfg, character, voice, text, outMp3, "");
    }

    /** Igual que synth() pero matiza ritmo y tono según la emoción de la escena. */
    public static void synth(Config cfg, String character, String voice, String text, Path outMp3,
                             String emotion) {
        String engine = cfg.get("tts.engine", "edge").trim().toLowerCase();
        if ("openai".equals(engine) && !cfg.get("openai.apiKey", "").isBlank()) {
            try {
                if (openai(cfg, character, text, outMp3)) return;
            } catch (Exception e) {
                System.err.println("AVISO: voz OpenAI falló (" + e.getMessage()
                        + "); se usa edge-tts.");
            }
        } else if ("elevenlabs".equals(engine) && !cfg.get("elevenlabs.apiKey", "").isBlank()) {
            try {
                if (elevenlabs(cfg, character, text, outMp3)) return;
            } catch (Exception e) {
                System.err.println("AVISO: voz ElevenLabs falló (" + e.getMessage()
                        + "); se usa edge-tts.");
            }
        }
        edge(cfg, character, voice, text, outMp3, emotion);
    }

    /** Voz neural gratuita de Azure/Edge con prosodia por personaje y matiz de emoción. */
    private static void edge(Config cfg, String character, String voice, String text, Path outMp3,
                             String emotion) {
        String clean = text == null ? "" : text.trim().replaceAll("\\s+", " ");
        if (!clean.isEmpty() && !clean.endsWith(".") && !clean.endsWith("!") && !clean.endsWith("?")) {
            clean += ".";
        }
        int[] m = emotionMatiz(emotion);
        int rate = clamp(parseDelta(cfg.voiceRateFor(character)) + m[0], -12, 16);
        int pitch = clamp(parseDelta(cfg.voicePitchFor(character)) + m[1], -25, 30);
        StringBuilder log = new StringBuilder();
        List<String> cmd = new ArrayList<>();
        for (String part : cfg.pythonCmd().trim().split("\\s+")) cmd.add(part);
        cmd.add("-m");
        cmd.add("edge_tts");
        cmd.add("--voice=" + voice);
        cmd.add("--pitch=" + String.format(java.util.Locale.ROOT, "%+dHz", pitch));
        cmd.add("--rate=" + String.format(java.util.Locale.ROOT, "%+d%%", rate));
        cmd.add("--volume=" + cfg.voiceVolumeFor(character));
        cmd.add("--text=" + clean);
        cmd.add("--write-media=" + outMp3.toString());
        int code = Util.run(cmd, null, log);
        if (code != 0) {
            throw new RuntimeException("edge-tts terminó con código " + code + "\n" + log);
        }
    }

    /** Deltas de ritmo (%) y tono (Hz) para dar vida a la interpretación por escena. */
    private static int[] emotionMatiz(String emotion) {
        if (emotion == null) emotion = "";
        switch (emotion.trim().toLowerCase().replace('á', 'a').replace('é', 'e')
                .replace('í', 'i').replace('ó', 'o').replace('ú', 'u')) {
            // Deltas suaves: los valores altos son los que suenan roboticos.
            case "sorpresa": return new int[]{6, 12};
            case "alegre":   return new int[]{5, 10};
            case "curioso":  return new int[]{3, 6};
            case "asustado": return new int[]{4, 8};
            case "enojado":  return new int[]{7, 4};
            case "triste":   return new int[]{-4, -12};
            case "tranquilo":return new int[]{-2, -4};
            case "orgulloso":return new int[]{3, 6};
            default: return new int[]{0, 0};
        }
    }

    private static int parseDelta(String s) {
        if (s == null) return 0;
        String d = s.trim().replaceAll("[^+\\-0-9]", "");
        if (d.isEmpty()) return 0;
        try {
            return Integer.parseInt(d);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    /** Voces organicas de OpenAI (tts-1-hd). Requiere openai.apiKey en config. */
    private static boolean openai(Config cfg, String character, String text, Path outMp3) throws Exception {
        String key = cfg.get("openai.apiKey", "");
        String voz = cfg.get("openai.voice." + norm(character),
                OPENAI_VOICES.getOrDefault(norm(character), "onyx"));
        String model = cfg.get("openai.model", "tts-1-hd");
        String payload = "{\"model\":\"" + model + "\",\"voice\":\"" + voz
                + "\",\"input\":" + jsonStr(text) + "}";
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
        HttpRequest req = HttpRequest.newBuilder(URI.create("https://api.openai.com/v1/audio/speech"))
                .timeout(Duration.ofSeconds(120))
                .header("Authorization", "Bearer " + key)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();
        HttpResponse<byte[]> resp = client.send(req, HttpResponse.BodyHandlers.ofByteArray());
        if (resp.statusCode() / 100 != 2) {
            throw new RuntimeException("OpenAI TTS " + resp.statusCode() + ": "
                    + new String(resp.body(), StandardCharsets.UTF_8));
        }
        Files.createDirectories(outMp3.getParent());
        Files.write(outMp3, resp.body());
        return true;
    }

    /** Voces naturales de ElevenLabs (modelo multilingual). Requiere elevenlabs.apiKey. */
    private static boolean elevenlabs(Config cfg, String character, String text, Path outMp3) throws Exception {
        String key = cfg.get("elevenlabs.apiKey", "");
        String voz = cfg.get("elevenlabs.voice." + norm(character),
                ELEVEN_VOICES.getOrDefault(norm(character), "ErXwobaYiN019PkySvjV"));
        String model = cfg.get("elevenlabs.model", "eleven_multilingual_v2");
        String payload = "{\"text\":" + jsonStr(text) + ",\"model_id\":\"" + model
                + "\",\"voice_settings\":{\"stability\":0.4,\"similarity_boost\":0.8}}";
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
        HttpRequest req = HttpRequest.newBuilder(
                URI.create("https://api.elevenlabs.io/v1/text-to-speech/" + voz))
                .timeout(Duration.ofSeconds(120))
                .header("xi-api-key", key)
                .header("Content-Type", "application/json")
                .header("Accept", "audio/mpeg")
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();
        HttpResponse<byte[]> resp = client.send(req, HttpResponse.BodyHandlers.ofByteArray());
        if (resp.statusCode() / 100 != 2) {
            throw new RuntimeException("ElevenLabs " + resp.statusCode() + ": "
                    + new String(resp.body(), StandardCharsets.UTF_8));
        }
        Files.createDirectories(outMp3.getParent());
        Files.write(outMp3, resp.body());
        return true;
    }

    private static String norm(String character) {
        if (character == null) return "";
        return character.trim().toLowerCase().replace('í', 'i').replace('é', 'e')
                .replace('á', 'a').replace('ó', 'o').replace('ú', 'u')
                .replace('ñ', 'n');
    }

    private static String jsonStr(String s) {
        if (s == null) return "\"\"";
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", " ").replace("\r", "") + "\"";
    }

    /** Progresa un narration de edge-tts simple: aplica 15% de silencio al final. */
    public static double durationWithPause(Path mp3, String ffprobe) {
        return Util.mediaDurationSeconds(mp3, ffprobe) * 1.15 + 0.2;
    }
}