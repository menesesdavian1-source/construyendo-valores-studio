package com.construyendovalores.studio;

import java.util.ArrayList;
import java.util.List;

/** Obtiene las voces disponibles de edge-tts (idioma es-*). */
public final class Voices {

    public static List<String> spanishVoices(Config cfg) {
        List<String> out = new ArrayList<>(List.of(
                "es-MX-JorgeNeural", "es-MX-DaliaNeural", "es-AR-TomasNeural",
                "es-CO-GonzaloNeural", "es-CL-CatalinaNeural", "es-ES-ElviraNeural",
                "es-EC-AndreaNeural", "es-PE-AlexNeural"));
        try {
            List<String> cmd = new ArrayList<>();
            for (String part : cfg.pythonCmd().trim().split("\\s+")) cmd.add(part);
            cmd.add("-m");
            cmd.add("edge_tts");
            cmd.add("--list-voices");
            StringBuilder sb = new StringBuilder();
            Util.run(cmd, null, sb);
            List<String> found = new ArrayList<>();
            for (String line : sb.toString().split("\\r?\\n")) {
                String l = line.trim();
                if (l.startsWith("es-") && l.contains(" ")) {
                    String voice = l.substring(0, l.indexOf(' ')).trim();
                    if (!found.contains(voice)) found.add(voice);
                }
            }
            if (!found.isEmpty()) return found;
        } catch (Exception ignored) {
        }
        return out;
    }

    private Voices() {}
}