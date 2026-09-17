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

        List<Scene> scenes = new java.util.ArrayList<>();
        scenes.add(introScene());
        scenes.addAll(script.scenes());
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
            Tts.synth(cfg, sc.character, voice, sc.narration, mp3);
            sc.audioFile = mp3.toString();
            Path ff = Paths.get(cfg.ffmpeg()).getParent().resolve("ffprobe.exe");
            if (!Files.exists(ff)) ff = Paths.get(cfg.ffmpeg().replace("ffmpeg", "ffprobe"));
            sc.audioSeconds = Tts.durationWithPause(mp3, ff.toString());
        }

        for (Scene sc : scenes) {
            progress.accept("Animando escena " + sc.index + " (cuadro a cuadro)...");
            Animator.animate(cfg, sc, script.title(), clipsDir);
        }

        progress.accept("Uniendo video final...");
        Path video = Assembler.assemble(cfg, scenes, outDir, script.title());
        progress.accept("Portada...");
        Cover.make(script.title(), "CONSTRUYENDO VALORES", outDir);
        progress.accept("Listo: " + video.toString());
        return script;
    }

    public static Scene introScene() {
        return new Scene(0, "Intro", "¡Bienvenidos a Construyendo Valores! Hoy aprenderemos algo grandioso.");
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

    private Pipeline() {}
}