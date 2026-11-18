package com.construyendovalores.studio;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

public final class Config {
    private static final Path FILE = Paths.get(System.getProperty("user.home"),
            ".construyendo-valores", "config.properties");

    public static final Map<String, String> CHARACTER_VOICES = new LinkedHashMap<>();

    static {
        CHARACTER_VOICES.put("Intro", "es-MX-JorgeNeural");
        CHARACTER_VOICES.put("Casquito", "es-CO-GonzaloNeural");
        CHARACTER_VOICES.put("Tito", "es-CO-GonzaloNeural");
        CHARACTER_VOICES.put("Sofía", "es-MX-DaliaNeural");
        CHARACTER_VOICES.put("Nico", "es-MX-JorgeNeural");
        CHARACTER_VOICES.put("Narrador", "es-MX-JorgeNeural");
        CHARACTER_VOICES.put("Luna", "es-MX-DaliaNeural");
        CHARACTER_VOICES.put("Pipo", "es-MX-JorgeNeural");
        CHARACTER_VOICES.put("Tico", "es-CO-GonzaloNeural");
        CHARACTER_VOICES.put("Bruno", "es-CO-GonzaloNeural");
        CHARACTER_VOICES.put("Memo", "es-MX-JorgeNeural");
        CHARACTER_VOICES.put("Coco", "es-MX-BeatrizNeural");
    }

    // pitch en Hz (positivo = más agudo/infantil) y rate en % (positivo = más rápido)
    private static final Map<String, int[]> VOICE_STYLES = new LinkedHashMap<>();

    static {
        VOICE_STYLES.put("Intro", new int[]{12, 4});
        VOICE_STYLES.put("Casquito", new int[]{-6, 3});
        VOICE_STYLES.put("Tito", new int[]{-16, 1});
        VOICE_STYLES.put("Sofía", new int[]{38, 8});
        VOICE_STYLES.put("Nico", new int[]{45, 10});
        VOICE_STYLES.put("Narrador", new int[]{-6, 3});
        VOICE_STYLES.put("Luna", new int[]{42, 9});
        VOICE_STYLES.put("Pipo", new int[]{32, 13});
        VOICE_STYLES.put("Tico", new int[]{-12, 1});
        VOICE_STYLES.put("Bruno", new int[]{-18, 1});
        VOICE_STYLES.put("Memo", new int[]{-24, -3});
        VOICE_STYLES.put("Coco", new int[]{40, 14});
    }

    // volumen (en %) para que las voces se escuchen claras y con energía
    private static final Map<String, String> VOICE_VOLUMES = new LinkedHashMap<>();

    static {
        VOICE_VOLUMES.put("Intro", "+6%");
        VOICE_VOLUMES.put("Casquito", "+8%");
        VOICE_VOLUMES.put("Tito", "+5%");
        VOICE_VOLUMES.put("Sofía", "+8%");
        VOICE_VOLUMES.put("Nico", "+10%");
        VOICE_VOLUMES.put("Narrador", "+6%");
        VOICE_VOLUMES.put("Luna", "+9%");
        VOICE_VOLUMES.put("Pipo", "+10%");
        VOICE_VOLUMES.put("Tico", "+5%");
        VOICE_VOLUMES.put("Bruno", "+7%");
        VOICE_VOLUMES.put("Memo", "+4%");
        VOICE_VOLUMES.put("Coco", "+10%");
    }

    private final Properties props = new Properties();

    public Config() {
        try {
            if (Files.exists(FILE)) {
                props.load(Files.newBufferedReader(FILE, StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            props.clear();
        }
    }

    public String get(String key, String def) {
        return props.getProperty(key, def);
    }

    /** Valor numérico con defecto (para calidad.movimiento y similares). */
    public double getDouble(String key, double def) {
        try {
            return Double.parseDouble(get(key, "").replace(',', '.'));
        } catch (Exception e) {
            return def;
        }
    }

    public void set(String key, String value) {
        props.setProperty(key, value);
    }

    public String apiKey() {
        return get("gemini.apiKey", "");
    }

    public String geminiModel() {
        return get("gemini.model", "gemini-flash-latest");
    }

    public String pythonCmd() {
        return get("python.cmd", "py -3.12");
    }

    public String ffmpeg() {
        return get("ffmpeg.path", Ffmpeg.locate());
    }

    public String musicPath() {
        return get("music.path", "");
    }

    public String voiceFor(String character) {
        String key = norm(character == null ? "" : character);
        for (Map.Entry<String, String> e : CHARACTER_VOICES.entrySet()) {
            if (norm(e.getKey()).equals(key)) {
                String over = get("voice." + e.getKey(), null);
                return over != null ? over : e.getValue();
            }
        }
        String over = get("voice." + character, null);
        return over != null ? over : CHARACTER_VOICES.get("Narrador");
    }

    private static int[] styleFor(String character) {
        String key = norm(character == null ? "" : character);
        for (Map.Entry<String, int[]> e : VOICE_STYLES.entrySet()) {
            if (norm(e.getKey()).equals(key)) return e.getValue();
        }
        return new int[]{0, 5};
    }

    public String voicePitchFor(String character) {
        String over = get("voice.pitch." + character, null);
        if (over != null) return over;
        return String.format(java.util.Locale.ROOT, "%+dHz", styleFor(character)[0]);
    }

    public String voiceRateFor(String character) {
        String over = get("voice.rate." + character, null);
        if (over != null) return over;
        return String.format(java.util.Locale.ROOT, "%+d%%", styleFor(character)[1]);
    }

    public String voiceVolumeFor(String character) {
        String over = get("voice.volume." + character, null);
        if (over != null) return over;
        String key = norm(character == null ? "" : character);
        for (Map.Entry<String, String> e : VOICE_VOLUMES.entrySet()) {
            if (norm(e.getKey()).equals(key)) return e.getValue();
        }
        return "+6%";
    }

    private static String norm(String s) {
        return s.toLowerCase()
                .replace('\u00e1', 'a').replace('\u00e9', 'e')
                .replace('\u00ed', 'i').replace('\u00f3', 'o')
                .replace('\u00fa', 'u').replace('\u00f1', 'n')
                .trim();
    }

    public void save() {
        try {
            Files.createDirectories(FILE.getParent());
            props.store(Files.newBufferedWriter(FILE, StandardCharsets.UTF_8),
                    "Construyendo Valores Studio - config");
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar la configuración", e);
        }
    }
}