package com.construyendovalores.studio;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Consumer;

/** Genera una ronda infantil: introducción hablada + escenas de karaoke con la
 *  melodía sintetizada y personajes bailando al ritmo. Sin necesidad de IA. */
public final class RondaVideo {
    private static final int PASADAS = 2;

    public static Path build(Config cfg, Ronda r, Path outDir, Consumer<String> progress) throws Exception {
        Path audioDir = outDir.resolve("audio");
        Path clipsDir = outDir.resolve("clips");
        Files.createDirectories(audioDir);
        Files.createDirectories(clipsDir);

        List<Scene> scenes = new java.util.ArrayList<>();
        int lineCount = r.lineas().size();
        int total = 1 + PASADAS * lineCount + 1;
        String hero = heroFor(r.id());

        Scene intro = new Scene(0, "Intro", r.intro());
        intro.scenario = r.escenario();
        scenes.add(intro);

        // La canción se canta PASADAS veces (como en La Granja de Zenón) y cada
        // línea usa el MISMO wav ya sintetizado, así las pasadas son baratas.
        int idx = 1;
        List<String[]> wavCache = new java.util.ArrayList<>();
        for (int p = 0; p < PASADAS; p++) {
            for (int i = 0; i < lineCount; i++) {
                Ronda.RondaLine line = r.lineas().get(i);
                Scene sc = new Scene(idx, hero, "");
                sc.karaoke = true;
                sc.karaokeText = line.texto();
                sc.scenario = r.escenario();
                sc.beats = MelodySynth.beatTimes(line.notas(), r.bpm());
                scenes.add(sc);
                idx++;
            }
        }

        Scene outro = new Scene(idx, hero,
                "¿Te gustó la ronda? Canta con nosotros mañana aquí en Construyendo Valores. ¡Hasta la próxima, amiguito!");
        outro.scenario = r.escenario();
        scenes.add(outro);

        // Voz de intro y cierre + sintetizar la melodía de cada línea UNA sola vez.
        for (Scene sc : scenes) {
            progress.accept("Voz " + sc.index + "/" + total + " (" + sc.character + ")");
            if (sc.narration != null && !sc.narration.isBlank()) {
                String voice = sc.index == 0 ? cfg.voiceFor(hero) : cfg.voiceFor(sc.character);
                Path mp3 = audioDir.resolve("escena_" + sc.index + ".mp3");
                Tts.synth(cfg, sc.character, voice, sc.narration, mp3);
                Path studio = audioDir.resolve("escena_" + sc.index + "_studio.mp3");
                AudioStudio.polishVoice(cfg, mp3, studio, new StringBuilder());
                sc.audioFile = studio.toString();
                sc.audioSeconds = AudioStudio.durationWithPause(cfg, studio);
            }
        }
        for (int i = 0; i < lineCount; i++) {
            Ronda.RondaLine line = r.lineas().get(i);
            Path wav = audioDir.resolve("ronda_" + i + ".wav");
            MelodySynth.renderWav(line.notas(), r.bpm(), wav);
            double secs = MelodySynth.phraseSeconds(line.notas(), r.bpm()) + 0.5;
            for (int p = 0; p < PASADAS; p++) {
                Scene sc = scenes.get(1 + p * lineCount + i);
                sc.audioFile = wav.toString();
                sc.audioSeconds = secs;
            }
        }

        for (Scene sc : scenes) {
            progress.accept("Animando escena " + sc.index + " (cuadro a cuadro)...");
            Animator.animate(cfg, sc, r.titulo(), clipsDir);
        }

        progress.accept("Uniendo video final...");
        Path video = Assembler.assemble(cfg, scenes, outDir, r.titulo(), null);
        progress.accept("Portada...");
        Cover.make(r.titulo() + " | RONDA", "RONDAS INFANTILES · CONSTRUYENDO VALORES", outDir);
        progress.accept("Listo: " + video.toString());
        return video;
    }

    /** Héroe protagonista de cada canción (define el color del cielo y la voz del narrador). */
    private static String heroFor(String id) {
        if (id == null) return "Casquito";
        switch (id) {
            case "barco": return "Tito";
            case "boton": return "Sofía";
            case "lobo": return "Nico";
            case "aserin": return "Tito";
            case "pipo": return "Pipo";
            default: return "Casquito";
        }
    }

    private RondaVideo() {}
}