package com.construyendovalores.studio;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public final class Ffmpeg {
    private Ffmpeg() {}

    private static volatile String cached;

    public static String locate() {
        if (cached != null) return cached;
        List<String> candidates = new ArrayList<>();

        String env = System.getenv("PATH");
        if (env != null) {
            for (String dir : env.split(File.pathSeparator)) {
                if (!dir.isBlank()) {
                    candidates.add(Paths.get(dir.trim(), "ffmpeg.exe").toString());
                }
            }
        }

        String home = System.getProperty("user.home");
        if (home != null) {
            Path winget = Paths.get(home, "AppData", "Local", "Microsoft", "WinGet", "Packages");
            if (Files.isDirectory(winget)) {
                try (Stream<Path> s = Files.walk(winget, 6)) {
                    s.filter(p -> p.getFileName() != null && p.getFileName().toString().equals("ffmpeg.exe"))
                            .forEach(p -> candidates.add(0, p.toString()));
                } catch (Exception ignored) {
                }
            }
        }

        for (String c : candidates) {
            try {
                if (Files.exists(Paths.get(c))) {
                    cached = c;
                    return c;
                }
            } catch (Exception ignored) {
            }
        }
        return "ffmpeg";
    }
}