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
        // Voces neurales distintas por personaje: se reconocen sin exagerar tono/ritmo.
        CHARACTER_VOICES.put("Intro", "es-MX-JorgeNeural");
        CHARACTER_VOICES.put("Casquito", "es-CO-GonzaloNeural");
        CHARACTER_VOICES.put("Tito", "es-PE-AlexNeural");
        CHARACTER_VOICES.put("Sofía", "es-MX-DaliaNeural");
        CHARACTER_VOICES.put("Nico", "es-CR-JuanNeural");
        CHARACTER_VOICES.put("Narrador", "es-MX-JorgeNeural");
        CHARACTER_VOICES.put("Luna", "es-CO-SalomeNeural");
        CHARACTER_VOICES.put("Pipo", "es-MX-JorgeNeural");
        CHARACTER_VOICES.put("Tico", "es-ES-AlvaroNeural");
        CHARACTER_VOICES.put("Bruno", "es-AR-TomasNeural");
        CHARACTER_VOICES.put("Memo", "es-UY-MateoNeural");
        CHARACTER_VOICES.put("Coco", "es-CO-SalomeNeural");
        CHARACTER_VOICES.put("Rubí", "es-MX-DaliaNeural");
        CHARACTER_VOICES.put("Lumi", "es-ES-ElviraNeural");
    }

    // pitch en Hz (positivo = más agudo/infantil) y rate en % (positivo = más rápido)
    private static final Map<String, int[]> VOICE_STYLES = new LinkedHashMap<>();

    static {
        // Deltas suaves: antes habia +45 Hz / +14%, suficiente para sonar sintetico.
        VOICE_STYLES.put("Intro", new int[]{4, 2});
        VOICE_STYLES.put("Casquito", new int[]{-4, 1});
        VOICE_STYLES.put("Tito", new int[]{-6, 2});
        VOICE_STYLES.put("Sofía", new int[]{10, 2});
        VOICE_STYLES.put("Nico", new int[]{8, 3});
        VOICE_STYLES.put("Narrador", new int[]{-3, 1});
        VOICE_STYLES.put("Luna", new int[]{9, 2});
        VOICE_STYLES.put("Pipo", new int[]{7, 3});
        VOICE_STYLES.put("Tico", new int[]{-8, 0});
        VOICE_STYLES.put("Bruno", new int[]{-10, 0});
        VOICE_STYLES.put("Memo", new int[]{-9, -1});
        VOICE_STYLES.put("Coco", new int[]{10, 3});
        VOICE_STYLES.put("Rubí", new int[]{8, 2});
        VOICE_STYLES.put("Lumi", new int[]{10, 1});
    }

    // volumen (en %) para que las voces se escuchen claras y con energía
    private static final Map<String, String> VOICE_VOLUMES = new LinkedHashMap<>();

    static {
        VOICE_VOLUMES.put("Intro", "+4%");
        VOICE_VOLUMES.put("Casquito", "+5%");
        VOICE_VOLUMES.put("Tito", "+4%");
        VOICE_VOLUMES.put("Sofía", "+5%");
        VOICE_VOLUMES.put("Nico", "+5%");
        VOICE_VOLUMES.put("Narrador", "+4%");
        VOICE_VOLUMES.put("Luna", "+5%");
        VOICE_VOLUMES.put("Pipo", "+5%");
        VOICE_VOLUMES.put("Tico", "+3%");
        VOICE_VOLUMES.put("Bruno", "+4%");
        VOICE_VOLUMES.put("Memo", "+3%");
        VOICE_VOLUMES.put("Coco", "+5%");
        VOICE_VOLUMES.put("Rubí", "+5%");
        VOICE_VOLUMES.put("Lumi", "+4%");
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