package com.construyendovalores.studio;

import java.util.ArrayList;
import java.util.List;

/** Convierte el JSON {video_title, description, scenes:[{character, narration}]} en Scene[]. */
public final class ScriptParser {

    public record Script(String title, String description, List<Scene> scenes) {}

    public static Script parse(String json) {
        List<Scene> scenes = new ArrayList<>();
        String title = valueOf(json, "video_title");
        String description = valueOf(json, "description");

        int scenesStart = json.indexOf("\"scenes\"");
        if (scenesStart < 0) {
            throw new IllegalArgumentException("El JSON no contiene 'scenes'.");
        }
        String body = json.substring(scenesStart);
        int listStart = body.indexOf('[');
        int listEnd = body.lastIndexOf(']');
        String list = body.substring(listStart + 1, listEnd);

        int i = 0;
        while (true) {
            int so = list.indexOf('{', i);
            if (so < 0) break;
            int end = list.indexOf('}', so);
            if (end < 0) break;
            String obj = list.substring(so, end + 1);
            String character = valueOf(obj, "character");
            String narration = valueOf(obj, "narration");
            String scenario = valueOf(obj, "scenario");
            if (!narration.isBlank()) {
                Scene sc = new Scene(scenes.size() + 1, character, narration);
                sc.scenario = scenario;
                scenes.add(sc);
            }
            i = end + 1;
        }
        return new Script(title, description, scenes);
    }

    private static String valueOf(String json, String key) {
        String needle = "\"" + key + "\"";
        int idx = json.indexOf(needle);
        if (idx < 0) return "";
        int colon = json.indexOf(':', idx);
        if (colon < 0) return "";
        int q = json.indexOf('"', colon + 1);
        if (q < 0) return "";
        int end = json.indexOf('"', q + 1);
        return end < 0 ? "" : json.substring(q + 1, end)
                .replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\t", "\t")
                .replace("\\\\", "\\");
    }

    private ScriptParser() {}
}