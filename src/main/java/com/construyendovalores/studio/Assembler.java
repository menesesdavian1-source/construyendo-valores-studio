package com.construyendovalores.studio;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Une los clips, quema subtítulos y mezcla música de fondo en un solo video. */
public final class Assembler {

    private static double clipDuration(Scene sc) {
        return Math.max(1.2, sc.audioSeconds + 0.45);
    }

    public static Path assemble(Config cfg, List<Scene> scenes, Path workDir, String outName) {
        return assemble(cfg, scenes, workDir, outName, Music.ensure(cfg.ffmpeg(), cfg.musicPath(), workDir));
    }

    /** bgMusic == null: solo voz/audio de escenas (ideal para rondas, que ya llevan su melodía). */
    public static Path assemble(Config cfg, List<Scene> scenes, Path workDir, String outName, Path bgMusic) {
        String ffmpeg = cfg.ffmpeg();
        Path merged = workDir.resolve("merged.mp4");

        List<String> concatArgs = new ArrayList<>();
        concatArgs.add(ffmpeg);
        concatArgs.add("-y");
        for (Scene sc : scenes) {
            concatArgs.add("-i");
            concatArgs.add(sc.video);
        }
        concatArgs.add("-filter_complex");
        StringBuilder fg = new StringBuilder();
        for (int i = 0; i < scenes.size(); i++) {
            fg.append("[").append(i).append(":v][").append(i).append(":a]");
        }
        fg.append("concat=n=").append(scenes.size()).append(":v=1:a=1[v][a]");
        concatArgs.add(fg.toString());
        concatArgs.add("-map"); concatArgs.add("[v]");
        concatArgs.add("-map"); concatArgs.add("[a]");
        concatArgs.add("-c:v"); concatArgs.add("libx264");
        concatArgs.add("-preset"); concatArgs.add("veryfast");
        concatArgs.add("-crf"); concatArgs.add("23");
        concatArgs.add("-c:a"); concatArgs.add("aac");
        concatArgs.add("-shortest");
        concatArgs.add(merged.toString());
        StringBuilder lg = new StringBuilder();
        int code = Util.run(concatArgs, workDir, lg);
        if (code != 0) {
            throw new RuntimeException("Fallo al unir clips:\n" + lg);
        }

        Util.write(workDir.resolve("subtitulos.srt"), buildSrt(scenes));

        Path outFile = workDir.resolve(safeName(outName) + ".mp4");
        boolean hasMusic = bgMusic != null && Files.exists(bgMusic);

        List<String> fin = new ArrayList<>();
        fin.add(ffmpeg);
        fin.add("-y");
        fin.add("-i");
        fin.add(merged.toString());
        if (hasMusic) {
            fin.add("-stream_loop");
            fin.add("-1");
            fin.add("-i");
            fin.add(bgMusic.toString());
        } else {
            fin.add("-f");
            fin.add("lavfi");
            fin.add("-i");
            fin.add("anullsrc=r=48000:cl=mono");
        }

        StringBuilder fc = new StringBuilder();
        fc.append("[0:v]scale=1080:1920:flags=lanczos,unsharp=5:5:0.6:5:5:0.0,subtitles=subtitulos.srt:force_style='")
                .append("FontName=Arial,FontSize=18,Bold=1,PrimaryColour=&H00FFFFFF,OutlineColour=&H00141414,")
                .append("BorderStyle=1,Outline=2,Shadow=1,BackColour=&H80000000,Alignment=2,WrapStyle=0,")
                .append("MarginL=20,MarginR=20,MarginV=24")
                .append("'[v]");
        if (hasMusic) {
            // la música baja automáticamente mientras alguien habla (ducking profesional)
            fc.append(";[1:a]aresample=48000,volume=0.22[mus]")
                    .append(";[0:a]asplit=2[voz][side]")
                    .append(";[mus][side]sidechaincompress=")
                    .append("threshold=0.03:ratio=12:attack=15:release=350:makeup=1[musf]")
                    .append(";[voz][musf]amix=inputs=2:duration=first:normalize=0,aresample=48000,alimiter=limit=0.9[a]");
        } else {
            fc.append(";[0:a]aresample=48000,alimiter=limit=0.9[a]");
        }

        fin.add("-filter_complex");
        fin.add(fc.toString());
        fin.add("-map"); fin.add("[v]");
        fin.add("-map"); fin.add("[a]");
        fin.add("-c:v"); fin.add("libx264");
        fin.add("-preset"); fin.add("veryfast");
        fin.add("-crf"); fin.add("20");
        fin.add("-maxrate"); fin.add("5200k");
        fin.add("-bufsize"); fin.add("10400k");
        fin.add("-c:a"); fin.add("aac");
        fin.add("-b:a"); fin.add("160k");
        fin.add("-movflags"); fin.add("+faststart");
        fin.add(outFile.toString());

        lg = new StringBuilder();
        code = Util.run(fin, workDir, lg);
        if (code != 0) {
            throw new RuntimeException("Fallo al quemar subtítulos/música:\n" + lg);
        }
        return outFile;
    }

    private static String buildSrt(List<Scene> scenes) {
        StringBuilder sb = new StringBuilder();
        double offset = 0.0;
        for (int i = 0; i < scenes.size(); i++) {
            Scene s = scenes.get(i);
            double dur = clipDuration(s);
            sb.append(i + 1).append("\n");
            sb.append(ts(offset)).append(" --> ").append(ts(offset + dur)).append("\n");
            sb.append(s.narration).append("\n\n");
            offset += dur;
        }
        return sb.toString();
    }

    private static String ts(double sec) {
        int ms = (int) Math.round(sec * 1000);
        int h = ms / 3600000;
        ms %= 3600000;
        int m = ms / 60000;
        ms %= 60000;
        int s = ms / 1000;
        ms %= 1000;
        return String.format(Locale.ROOT, "%02d:%02d:%02d,%03d", h, m, s, ms);
    }

    private static String safeName(String name) {
        return name == null || name.isBlank() ? "video" : name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private Assembler() {}
}