package com.construyendovalores.studio;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Consumer;

/** Secuencia completa: escenas -> voces -> fondos -> animación -> video final + portada. */
public final class Pipeline {

    public static ScriptParser.Script exec(Config cfg, ScriptParser.Script script, Path outDir, Consumer<String> progress) throws Exception {
        Path audioDir = outDir.resolve("audio");
        Path clipsDir = outDir.resolve("clips");
        Files.createDirectories(audioDir);
        Files.createDirectories(clipsDir);

        // Sin tarjeta de introduccion: el cuento empieza en el primer fotograma
        // (retencion) y la marca vive en el rotulo, no en una voz generica.
        List<Scene> scenes = new java.util.ArrayList<>(script.scenes());
        String globalScenario = cfg.get("video.scenario", Scenario.OBRA.id);
        for (Scene sc : scenes) {
            if (sc.scenario == null || sc.scenario.isBlank()) {
                sc.scenario = globalScenario;
            }
        }
        for (Scene sc : scenes) {
            progress.accept("Voz " + sc.index + "/" + scenes.size() + " (" + sc.character + ")");
            Path mp3 = audioDir.resolve("escena_" + sc.index + ".mp3");
            String voice = cfg.voiceFor(sc.character);
            Tts.synth(cfg, sc.character, voice, sc.narration, mp3, sc.emotion);
            Path studio = audioDir.resolve("escena_" + sc.index + "_studio.mp3");
            AudioStudio.polishVoice(cfg, mp3, studio, new StringBuilder());
            sc.audioFile = studio.toString();
            sc.audioSeconds = AudioStudio.durationWithPause(cfg, studio);
        }

        boolean cine = "true".equalsIgnoreCase(cfg.get("cine.habilitado", "true"));
        for (Scene sc : scenes) {
            if (cine) {
                try {
                    progress.accept("Cine IA escena " + sc.index + " (fondo con IA + personaje)...");
                    CineIA.renderEscena(cfg, sc, script.title(), outDir);
                    continue;
                } catch (Exception e) {
                    String msg = e.getMessage() == null ? "error" : e.getMessage().split("\\n")[0];
                    progress.accept("AVISO: Cine IA falló en " + sc.index + " (" + msg
                            + "). Usando animación clásica.");
                }
            }
            progress.accept("Animando escena " + sc.index + " (cuadro a cuadro)...");
            Animator.animate(cfg, sc, script.title(), clipsDir);
        }

        if (FreeAi.habilitado(cfg) && "true".equalsIgnoreCase(cfg.get("freeia.hook", "true"))) {
            Path manual = clipsDir.resolve("hook.mp4");
            if (Files.exists(manual) && Files.size(manual) > 10_000) {
                progress.accept("Gancho animado (hook.mp4 descargado a mano, vía web gratis).");
                Scene h = new Scene(-1, "Gancho", "");
                h.video = manual.toString();
                h.audioSeconds = FreeAi.duracionGancho(cfg);
                h.scenario = "";
                scenes.add(0, h);
            } else {
                progress.accept("Gancho animado (Free.ai)...");
                Path hook = FreeAi.generarGancho(cfg, script.title(), clipsDir, progress);
                if (hook != null) {
                    Scene h = new Scene(-1, "Gancho", "");
                    h.video = hook.toString();
                    h.audioSeconds = FreeAi.duracionGancho(cfg);
                    h.scenario = "";
                    scenes.add(0, h);
                } else {
                    progress.accept("AVISO: sin gancho animado hoy (el video se sube igual, sin movimiento).");
                }
            }
        }

        progress.accept("Uniendo video final...");
        Path video = Assembler.assemble(cfg, scenes, outDir, script.title());
        progress.accept("Portada...");
        Cover.make(script.title(), "CONSTRUYENDO VALORES", outDir);
        progress.accept("Listo: " + video.toString());
        return script;
    }

    public static String exampleJson() {
        return """
                {
                  "video_title": "La torre que no quiso rendirse",
                  "description": "Casquito y sus amigos aprenden a no rendirse.",
                  "scenes": [
                    {"character": "Casquito", "narration": "¿Por qué a veces sentimos que ya no podemos?"},
                    {"character": "Tito", "narration": "¡Mi torre se cayó otra vez! ¡Qué frustrado estoy!"},
                    {"character": "Luna", "narration": "Miren las estrellas: hasta ellas esperan su turno para brillar."},
                    {"character": "Pipo", "narration": "¡Ja ja ja! Yo me caí dos veces y subí tres. ¡Ríete y sigue!"},
                    {"character": "Tico", "narration": "Como en mis cuentos: despacio y sin rendirse se llega alto."},
                    {"character": "Casquito", "narration": "Perseverancia: construir de nuevo si hace falta."}
                  ]
                }
                """;
    }

    public static ScriptParser.Script example() {
        String json = """
                {
                  "video_title": "La torre que no quiso rendirse",
                  "description": "Casquito y sus amigos aprenden a no rendirse.",
                  "scenes": [
                    {"character": "Casquito", "narration": "¿Por qué a veces sentimos que ya no podemos?"},
                    {"character": "Tito", "narration": "¡Mi torre se cayó otra vez! ¡Qué frustrado estoy!"},
                    {"character": "Luna", "narration": "Miren las estrellas: hasta ellas esperan su turno para brillar."},
                    {"character": "Pipo", "narration": "¡Ja ja ja! Yo me caí dos veces y subí tres. ¡Ríete y sigue!"},
                    {"character": "Tico", "narration": "Como en mis cuentos: despacio y sin rendirse se llega alto."},
                    {"character": "Casquito", "narration": "Perseverancia: construir de nuevo si hace falta."}
                  ]
                }
                """;
        return ScriptParser.parse(json);
    }

    /** Reconstruye la lista de escenas con su audio ya renderizado (sin volver a animar). */
    public static java.util.List<Scene> scenesConAudio(Config cfg, Path outDir) throws Exception {
        String raw = Files.readString(outDir.resolve("guion.json"));
        ScriptParser.Script script = ScriptParser.parse(raw);
        java.util.List<Scene> scenes = new java.util.ArrayList<>(script.scenes());
        for (Scene sc : scenes) {
            Path audio = outDir.resolve("audio").resolve("escena_" + sc.index + "_studio.mp3");
            if (!Files.exists(audio)) {
                throw new RuntimeException("Falta audio de escena " + sc.index + ": " + audio);
            }
            sc.audioFile = audio.toString();
            sc.audioSeconds = AudioStudio.durationWithPause(cfg, audio);
        }
        return scenes;
    }

    /** Reconstruye el video FINAL a partir de merged.mp4 + audio + clips ya renderizados
     *  (sin volver a animar ni sintetizar voces). Sirve para recuperar el video
     *  original de calidad tras un re-encode accidental o para re-quemar subtítulos. */
    public static ScriptParser.Script remontar(Config cfg, Path outDir, Consumer<String> progress) throws Exception {
        String raw = Files.readString(outDir.resolve("guion.json"));
        ScriptParser.Script script = ScriptParser.parse(raw);

        List<Scene> scenes = new java.util.ArrayList<>(script.scenes());
        for (Scene sc : scenes) {
            Path audio = outDir.resolve("audio").resolve("escena_" + sc.index + "_studio.mp3");
            if (!Files.exists(audio)) {
                throw new RuntimeException("Falta audio de escena " + sc.index + ": " + audio);
            }
            sc.audioFile = audio.toString();
            sc.audioSeconds = AudioStudio.durationWithPause(cfg, audio);
            sc.video = outDir.resolve("clips").resolve(String.format("clip_%02d.mp4", sc.index)).toString();
            if (!Files.exists(Paths.get(sc.video))) {
                throw new RuntimeException("Falta clip " + sc.video);
            }
        }

        progress.accept("Remontando video final...");
        Path video = Assembler.assemble(cfg, scenes, outDir, script.title());
        progress.accept("Listo: " + video.toString());
        return script;
    }

    private Pipeline() {}
}