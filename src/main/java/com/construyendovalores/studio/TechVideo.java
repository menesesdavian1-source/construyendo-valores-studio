package com.construyendovalores.studio;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Consumer;

/** Genera videos de tecnología sin IA: listas de "5 datos" y trivias con la
 *  palabra gigante en pantalla (las rondas de tecnología usan RondaVideo). */
public final class TechVideo {

    public static Path buildListicle(Config cfg, TechLibrary.Listicle l, Path outDir, Consumer<String> progress) throws Exception {
        Files.createDirectories(outDir.resolve("audio"));
        Files.createDirectories(outDir.resolve("clips"));

        List<Scene> scenes = new java.util.ArrayList<>();
        Scene intro = new Scene(0, "Intro", l.intro());
        intro.scenario = "taller";
        scenes.add(intro);

        String[] chars = {"Casquito", "Tito", "Sofía", "Nico"};
        int idx = 1;
        for (TechLibrary.Dato d : l.datos()) {
            Scene sc = new Scene(idx, chars[idx % chars.length], d.narra());
            sc.scenario = "taller";
            sc.bigWord = d.palabra();
            sc.listNumber = idx;
            scenes.add(sc);
            idx++;
        }
        Scene cierre = new Scene(idx, "Casquito", l.cierre());
        cierre.scenario = "taller";
        scenes.add(cierre);

        synthVoices(cfg, scenes, outDir, progress);
        renderAll(cfg, scenes, l.titulo(), outDir, progress);

        progress.accept("Uniendo video final...");
        Path video = Assembler.assemble(cfg, scenes, outDir, l.titulo());
        progress.accept("Portada...");
        Cover.make(l.titulo() + " | 5 DATOS", "TECNOLOGÍA · CONSTRUYENDO VALORES", outDir);
        progress.accept("Listo: " + video.toString());
        return video;
    }

    public static Path buildTrivia(Config cfg, TechLibrary.Trivia t, Path outDir, Consumer<String> progress) throws Exception {
        Files.createDirectories(outDir.resolve("audio"));
        Files.createDirectories(outDir.resolve("clips"));

        List<Scene> scenes = new java.util.ArrayList<>();
        Scene intro = new Scene(0, "Intro", t.intro());
        intro.scenario = "taller";
        scenes.add(intro);

        String[] chars = {"Casquito", "Sofía", "Tito", "Nico"};
        int idx = 1;
        for (TechLibrary.Dato it : t.items()) {
            Scene sc = new Scene(idx, chars[idx % chars.length], it.narra());
            sc.scenario = "taller";
            sc.bigWord = it.palabra();
            sc.revealAt = 0.62;
            scenes.add(sc);
            idx++;
        }
        Scene cierre = new Scene(idx, "Casquito",
                "¿Cuántas acertaste? ¡Déjanos tu respuesta en los comentarios! Hasta mañana, curioso.");
        cierre.scenario = "taller";
        scenes.add(cierre);

        synthVoices(cfg, scenes, outDir, progress);
        renderAll(cfg, scenes, t.titulo(), outDir, progress);

        progress.accept("Uniendo video final...");
        Path video = Assembler.assemble(cfg, scenes, outDir, t.titulo());
        progress.accept("Portada...");
        Cover.make(t.titulo() + " | ADIVINA", "TECNOLOGÍA · CONSTRUYENDO VALORES", outDir);
        progress.accept("Listo: " + video.toString());
        return video;
    }

    private static void synthVoices(Config cfg, List<Scene> scenes, Path outDir, Consumer<String> progress) throws Exception {
        Path audioDir = outDir.resolve("audio");
        Path ff = Paths.get(cfg.ffmpeg()).getParent().resolve("ffprobe.exe");
        if (!Files.exists(ff)) ff = Paths.get(cfg.ffmpeg().replace("ffmpeg", "ffprobe"));
        for (Scene sc : scenes) {
            progress.accept("Voz " + sc.index + "/" + scenes.size() + " (" + sc.character + ")");
            Path mp3 = audioDir.resolve("escena_" + sc.index + ".mp3");
            Tts.synth(cfg, sc.character, cfg.voiceFor(sc.character), sc.narration, mp3);
            sc.audioFile = mp3.toString();
            sc.audioSeconds = Tts.durationWithPause(mp3, ff.toString());
        }
    }

    private static void renderAll(Config cfg, List<Scene> scenes, String title, Path outDir, Consumer<String> progress) throws Exception {
        Path clipsDir = outDir.resolve("clips");
        for (Scene sc : scenes) {
            progress.accept("Animando escena " + sc.index + " (cuadro a cuadro)...");
            Animator.animate(cfg, sc, title, clipsDir);
        }
    }

    private TechVideo() {}
}