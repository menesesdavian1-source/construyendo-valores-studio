package com.construyendovalores.studio;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Mini-Clipper PRO: corta momentos "calientes" de videos largos en clips 9:16
 *  listos para Shorts/TikTok/Reels. Deteccion por energia de audio (ebur128).
 *  Sin IA externa: cero coste y funciona offline, para corteditable por campaÃ±as
 *  con licencia (Content Rewards y similares). */
public final class Clipper {

    private static final Pattern M = Pattern.compile("t:\\s*([\\d.]+).*?M:\\s*(-?\\d+(?:\\.\\d+)?|-inf)");

    public record Clip(double start, double dur, double pico) {}

    public static void cortar(Config cfg, Path video, Path outDir, int nMax, double durObjetivo) throws Exception {
        outDir = outDir.toAbsolutePath();
        Files.createDirectories(outDir);
        Path srtPath = video.resolveSibling(sinExt(video) + ".srt");

        double total = Util.mediaDurationSeconds(video, Util.ffprobeBin(cfg));
        double[] curva = curvaSonoridad(cfg, video);
        List<Clip> picos = detectarPicos(curva, total, cfg.getDouble("clipper.umbral", 4.5),
                nMax, durObjetivo, 8);

        if (picos.isEmpty()) {
            System.out.println("CLIP | sin picos claros (prueba a bajar clipper.umbral a 3.5)");
            return;
        }

        String base = sinExt(video);
        for (int i = 0; i < picos.size(); i++) {
            Clip c = picos.get(i);
            double start = Math.max(0, c.start);
            double d = Math.min(c.dur, durObjetivo);
            if (start + d > total) d = Math.max(0, total - start);
            if (d < 6) continue;

            Path out = outDir.resolve(String.format(Locale.ROOT, "CLIP_%02d_%s.mp4", i + 1, base));
            Path log = outDir.resolve(String.format("clip_%02d.log", i + 1));
            StringBuilder fc = new StringBuilder();
            fc.append("scale=").append("1080:1920:force_original_aspect_ratio=increase")
                    .append(",crop=1080:1920")
                    .append(",scale=720:1280")
                    .append(",zoompan=z='1+0.10*in/")
                    .append(String.format(Locale.ROOT, "%.0f", Math.max(1, d * 25 / 1)))
                    .append("':d=1:x='iw/2-iw/zoom/2:y='ih/2-ih/zoom/2':s=1080x1920:fps=25");

            if (Files.exists(srtPath)) {
                String srtAbs = srtPath.toAbsolutePath().toString()
                        .replace("\\", "/").replace(":", "\\:");
                fc.append(",subtitles='").append(srtAbs).append("':force_style='")
                        .append("FontName=Comic Sans MS,Fontsize=30,Bold=1,PrimaryColour=&H00FFFFFF,"
                                + "OutlineColour=&H00141414,BorderStyle=1,Outline=3,Shadow=2,"
                                + "BackColour=&H80000000,Alignment=2,WrapStyle=2,MarginV=44'");
            }
            String gancho = cfg.get("clipper.gancho", "");
            String agua = cfg.get("clipper.agua", "");
            String raf = guardarFuente(cfg, outDir);
            if (!gancho.isBlank() && raf != null) {
                Path hf = outDir.resolve("_gancho.txt");
                Util.write(hf, gancho);
                fc.append(",drawtext=fontfile=").append(raf)
                        .append(":fontsize=66:fontcolor=white:borderw=5:bordercolor=black@0.6")
                        .append(":textfile=").append(hf.getFileName().toString().replace(":", "\\:"))
                        .append(":x=(w-text_w)/2:y=h*0.08:enable='lt(t,2.2)'");
            }
            if (!agua.isBlank() && raf != null) {
                Path wf = outDir.resolve("_agua.txt");
                Util.write(wf, agua);
                fc.append(",drawtext=fontfile=").append(raf)
                        .append(":fontsize=36:fontcolor=white@0.9:borderw=2:bordercolor=black@0.55")
                        .append(":textfile=").append(wf.getFileName().toString().replace(":", "\\:"))
                        .append(":x=w-text_w-40:y=h-80:enable='gt(t,0.4)'");
            }

            List<String> cmd = new ArrayList<>();
            cmd.add(cfg.ffmpeg());
            cmd.add("-y");
            cmd.add("-ss"); cmd.add(String.format(Locale.ROOT, "%.2f", start));
            cmd.add("-t");  cmd.add(String.format(Locale.ROOT, "%.2f", d));
            cmd.add("-i");  cmd.add(video.toAbsolutePath().toString());
            cmd.add("-vf"); cmd.add(fc.toString());
            cmd.add("-c:v"); cmd.add("libx264");
            cmd.add("-preset"); cmd.add("veryfast");
            cmd.add("-crf"); cmd.add("20");
            cmd.add("-pix_fmt"); cmd.add("yuv420p");
            cmd.add("-c:a"); cmd.add("aac");
            cmd.add("-b:a"); cmd.add("160k");
            cmd.add("-af"); cmd.add("loudnorm=I=-14:TP=-1.5:LRA=11");
            cmd.add("-movflags"); cmd.add("+faststart");
            cmd.add(out.toString());

            StringBuilder lg = new StringBuilder();
            int code = Util.run(cmd, outDir, lg);
            if (code != 0) {
                System.out.println("CLIP | fallo al cortar CLIP_" + (i + 1) + " (" + lg + ")");
                continue;
            }
            Util.write(outDir.resolve("CLIP_" + String.format(Locale.ROOT, "%02d_%s_copy.txt", i + 1, base)),
                    textoClips(base, i + 1, start, d, c.pico()));
            registrar(outDir, base, start, d, out);
            System.out.println(String.format(Locale.ROOT,
                    "CLIP | %02d  seg=%.0f dur=%.0f pico=%.1fLU  ->  %s",
                    i + 1, start, d, c.pico(), out.getFileName()));
        }
        System.out.println("CLIP | copias de texto en " + outDir.toAbsolutePath()
                + " | tracker en clips_tracker.csv");
    }

    /** Curva de sonoridad momentanea por segundo (LUFS, -70 => silencio). */
    private static double[] curvaSonoridad(Config cfg, Path video) throws Exception {
        List<String> cmd = List.of(cfg.ffmpeg(), "-v", "info", "-i", video.toString(),
                "-af", "ebur128", "-f", "null", "-");
        String out = Util.capture(cmd, null);
        List<double[]> pts = new ArrayList<>(); // {t, m}
        Matcher m = M.matcher(out);
        while (m.find()) {
            pts.add(new double[]{Double.parseDouble(m.group(1)), m.group(2).equals("-inf")
                    ? -70.0 : Math.max(-70.0, Double.parseDouble(m.group(2)))});
        }
        if (pts.isEmpty()) return new double[0];
        int segs = (int) Math.ceil(pts.get(pts.size() - 1)[0]);
        double[] bucket = new double[Math.max(1, segs)];
        int[] n = new int[bucket.length];
        for (double[] p : pts) {
            int k = (int) Math.round(p[0]);
            if (k >= bucket.length) continue;
            bucket[k] += p[1];
            n[k]++;
        }
        for (int i = 0; i < bucket.length; i++) {
            bucket[i] = n[i] == 0 ? -70.0 : bucket[i] / n[i];
        }
        return bucket;
    }

    private static List<Clip> detectarPicos(double[] s, double total, double umbral,
                                            int nMax, double dur, double minDur) {
if (s.length < 4) return new ArrayList<>();
        double step = 1.0; // la curva ya viene por segundo
        int w = 2;
        int gap = 3;
        double[] smooth = new double[s.length];
        for (int i = 0; i < s.length; i++) {
            smooth[i] = promedio(s, Math.max(0, i - w), Math.min(s.length - 1, i + w));
        }
        double[] orden = smooth.clone();
        java.util.Arrays.sort(orden);
        double base = orden[orden.length / 2];

        List<double[]> cl = new ArrayList<>(); // {inicio, dur, excesoPico, posPico}
        int i = 0;
        while (i < s.length) {
            if (smooth[i] - base < umbral) { i++; continue; }
            int k = i;
            double pk = 0, ppos = i;
            while (k < s.length) {
                if (smooth[k] - base < umbral) {
                    int nnext = k;
                    boolean sigue = false;
                    while (nnext <= k + gap && nnext < s.length) {
                        if (smooth[nnext] - base >= umbral) { sigue = true; break; }
                        nnext++;
                    }
                    if (sigue) { k = nnext; continue; }
                    break;
                }
                if (smooth[k] > pk) { pk = smooth[k]; ppos = k; }
                k++;
            }
            double durSeg = (k - i) * step;
            if (durSeg >= minDur) {
                cl.add(new double[]{i * step, durSeg, pk - base, ppos * step});
            }
            i = Math.max(i + 1, k);
        }

        cl.sort((a, b) -> Double.compare(b[2], a[2]));
        List<double[]> picks = new ArrayList<>();
        for (double[] c : cl) {
            boolean lejos = true;
            for (double[] q : picks) {
                if (Math.abs(q[0] - c[0]) < 8.0) { lejos = false; break; }
            }
            if (lejos) picks.add(c);
            if (picks.size() >= nMax) break;
        }

        List<Clip> res = new ArrayList<>();
        for (double[] c : picks) {
            double inicio = c[0];
            double fin = Math.min(total, inicio + c[1]);
            double start = Math.max(0, inicio - 2.0);   // 2s de pre-rodaje
            double dd = Math.min(dur, Math.max(6, fin - start));
            if (start + dd > total) { start = Math.max(0, total - dd); }
            res.add(new Clip(Math.min(start, total - 0.1), Math.min(dd, total - start), c[2]));
        }
        return res;
    }

    private static double promedio(double[] a, int from, int to) {
        double s = 0;
        int n = 0;
        for (int i = from; i <= to && i < a.length; i++) { s += a[i]; n++; }
        return n == 0 ? -90 : s / n;
    }

    /** Separa "Nombre del cuento | Valor" en sus dos partes. */
    private static String nombreDe(String base) {
        if (base == null) return "";
        int i = base.lastIndexOf('|');
        return i > 0 ? base.substring(0, i).trim() : base.trim();
    }

    private static String valorDe(String base) {
        if (base == null) return "";
        int i = base.lastIndexOf('|');
        return i > 0 && i + 1 < base.length() ? base.substring(i + 1).trim() : "";
    }

    /**
     * Titulos para un clip. Se apoyan en lo que si funciona en el canal: nombrar el cuento
     * y decir el valor. Nunca se repite una plantilla generica entre videos.
     */
    private static List<String> titulosDe(String base) {
        String nombre = nombreDe(base);
        String valor = valorDe(base);
        String v = valor.isEmpty() ? "" : valor.substring(0, 1).toUpperCase() + valor.substring(1);
        List<String> t = new ArrayList<>();
        if (!v.isEmpty()) {
            t.add(nombre + " | " + v.toUpperCase(java.util.Locale.ROOT) + " para niños");
            t.add(nombre + ": el cuento que enseña " + v);
            t.add("¿Qué harías tú? | " + nombre + " | " + v);
        } else {
            t.add(nombre + " para niños | Cuento con valores");
            t.add(nombre + ": el cuento que enseña algo nuevo");
            t.add("¿Qué harías tú? | " + nombre);
        }
        return t;
    }

    /** Etiquetas con señal tematica real (no solo "shorts"). */
    private static List<String> etiquetasDe(String base) {
        String nombre = nombreDe(base);
        String valor = valorDe(base);
        List<String> tags = new ArrayList<>();
        tags.add("cuento para niños");
        tags.add("cuentos infantiles");
        tags.add("valores para niños");
        tags.add("construyendo valores");
        if (!valor.isEmpty()) tags.add(valor.toLowerCase(java.util.Locale.ROOT));
        if (!nombre.isEmpty() && nombre.length() <= 40) tags.add(nombre);
        return tags;
    }

    /** Etiquetas para subir: usa las del _copy.txt y completa con las del nicho. */
    private static List<String> etiquetasSubida(String metaTags) {
        List<String> tags = new ArrayList<>();
        if (metaTags != null && !metaTags.isBlank()) {
            for (String t : metaTags.split(",")) {
                String v = t.trim();
                if (!v.isEmpty() && !tags.contains(v)) tags.add(v);
            }
        }
        if (tags.size() < 3) {
            for (String t : new String[]{"cuento para niños", "cuentos infantiles",
                    "valores para niños", "construyendo valores"}) {
                if (!tags.contains(t)) tags.add(t);
            }
        }
        if (!tags.contains("Shorts")) tags.add("Shorts");
        return tags;
    }

    private static String textoClips(String base, int n, double start, double dur, double pico) {
        int m = (int) (start / 60);
        int s = (int) (start % 60);
        List<String> titulos = titulosDe(base);
        StringBuilder sb = new StringBuilder();
        sb.append("Fuente: ").append(base).append("\n");
        sb.append("Inicio: ").append(m).append("min ").append(s).append("s | DuraciÃ³n: ")
                .append(String.format(Locale.ROOT, "%.0f", dur)).append("s | Pico: ")
                .append(String.format(Locale.ROOT, "%.1f", pico)).append(" LU\n\n");
        sb.append("Titulos sugeridos:\n");
        for (String t : titulos) sb.append("- ").append(t).append("\n");
        sb.append("\nEtiquetas: ").append(String.join(", ", etiquetasDe(base))).append(",\n");
        sb.append("\nSubir a: TikTok + Shorts + Reels. Pega el enlace en la campaÃ±a para cobrar.\n");
        return sb.toString();
    }

    private static void registrar(Path outDir, String base, double start, double dur, Path out) {
        Path csv = outDir.resolve("clips_tracker.csv");
        StringBuilder sb = new StringBuilder();
        boolean nuevo = !Files.exists(csv);
        if (nuevo) {
            sb.append("\uFEFFfecha;fuente;inicio;s;archivo;titulo;vistas;pago\n"); // BOM para Excel
        }
        sb.append(LocalDate.now()).append(";").append(base).append(";")
                .append(String.format(Locale.ROOT, "%.0f", start)).append(";")
                .append(String.format(Locale.ROOT, "%.0f", dur)).append(";")
                .append(out.getFileName()).append(";;;\n");
        try {
            Files.write(csv, sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND);
        } catch (Exception ignored) {
        }
    }

    private static String guardarFuente(Config cfg, Path outDir) throws Exception {
        Path fontRel = outDir.resolve("_clip_font.ttf");
        if (!Files.exists(fontRel)) {
            Path f = Files.exists(Paths.get("C:/Windows/Fonts/arialbd.ttf"))
                    ? Paths.get("C:/Windows/Fonts/arialbd.ttf") : Paths.get("C:/Windows/Fonts/arial.ttf");
            if (!Files.exists(f)) return null;
            Files.copy(f, fontRel, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        return fontRel.getFileName().toString().replace(":", "\\:");
    }

    /** Sube todos los CLIP_*.mp4 de una carpeta al canal de YouTube (Shorts). */
    public static void subir(Config cfg, Path carpeta, int nMax) throws Exception {
        carpeta = carpeta.toAbsolutePath();
        if (!Files.isDirectory(carpeta)) {
            System.out.println("SUBIR | no existe la carpeta: " + carpeta);
            return;
        }
        List<Path> clips = new ArrayList<>();
        try (var s = Files.list(carpeta)) {
            s.filter(p -> p.getFileName().toString().matches("(?i)CLIP_.*\\.mp4"))
                    .sorted().forEach(clips::add);
        }
        if (clips.isEmpty()) {
            System.out.println("SUBIR | no hay CLIP_*.mp4 en " + carpeta);
            return;
        }
        if (nMax > 0 && clips.size() > nMax) clips = clips.subList(0, nMax);
        int ok = 0;
        for (Path c : clips) {
            String[] meta = leerMeta(carpeta, c);
            String titulo = meta[0] == null
                    ? String.format(Locale.ROOT, "Momento destacado #Shorts %s", sinExt(c))
                    : meta[0];
            if (titulo.toLowerCase().contains("#shorts")) {
                if (titulo.length() > 100) titulo = titulo.substring(0, 100).trim();
            } else {
                if (titulo.length() > 92) titulo = titulo.substring(0, 92).trim();
                titulo = titulo + " #Shorts";
            }
            titulo = titulo.replaceAll("[\\s|]+$", "");
            String desc = (meta[1] == null ? "" : meta[1].trim() + "\n\n") + "#Shorts";
            System.out.println("SUBIR | subiendo " + c.getFileName() + " ...");
            Youtube.UploadResult r = Youtube.upload(cfg, c, titulo, desc, etiquetasSubida(meta[1]),
                    null, cfg.get("youtube.privacyStatus", "public"), System.out::println);
            System.out.println("SUBIR | ok -> " + r.url());
            ok++;
            Thread.sleep(1500);
        }
        System.out.println("SUBIR | " + ok + "/" + clips.size() + " subidos al canal.");
    }

    /** Programa los CLIP_*.mp4 de una carpeta: 1 por dia a la hora de publicacion. */
    public static void lanzar(Config cfg, Path carpeta, LocalDate desde, int max) throws Exception {
        carpeta = carpeta.toAbsolutePath();
        if (!Files.isDirectory(carpeta)) {
            System.out.println("LANZAR | no existe la carpeta: " + carpeta);
            return;
        }
        List<Path> clips = new ArrayList<>();
        try (var s = Files.list(carpeta)) {
            s.filter(p -> p.getFileName().toString().matches("(?i)CLIP_.*\\.mp4"))
                    .sorted().forEach(clips::add);
        }
        if (clips.isEmpty()) {
            System.out.println("LANZAR | no hay CLIP_*.mp4 en " + carpeta);
            return;
        }
        if (max > 0 && clips.size() > max) clips = clips.subList(0, max);
        int hour = Integer.parseInt(cfg.get("youtube.publishHour", "9"));
        LocalDate d = desde == null ? LocalDate.now().plusDays(1) : desde.plusDays(1);
        int ok = 0;
        for (Path c : clips) {
            Path mk = carpeta.resolve(c.getFileName() + ".programado");
            if (Files.exists(mk)) {
                System.out.println("LANZAR | (ya programado) " + c.getFileName());
                continue;
            }
            String[] meta = leerMeta(carpeta, c);
            String titulo = meta[0] == null
                    ? String.format(Locale.ROOT, "Momento destacado #Shorts %s", sinExt(c)) : meta[0];
            if (titulo.toLowerCase().contains("#shorts")) {
                if (titulo.length() > 100) titulo = titulo.substring(0, 100).trim();
            } else {
                if (titulo.length() > 92) titulo = titulo.substring(0, 92).trim();
                titulo = titulo + " #Shorts";
            }
            titulo = titulo.replaceAll("[\\s|]+$", "");
            String desc = (meta[1] == null ? "" : meta[1].trim() + "\n\n") + "#Shorts";
            OffsetDateTime pub = d.atTime(hour, 0).atOffset(ZoneOffset.ofHours(-5));
            System.out.println("LANZAR | " + pub.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    + " | " + c.getFileName());
            Youtube.UploadResult r = Youtube.upload(cfg, c, titulo, desc, etiquetasSubida(meta[1]),
                    pub, "private", System.out::println);
            System.out.println("LANZAR | ok -> " + r.url());
            Util.write(mk, pub.toString());
            ok++;
            d = d.plusDays(1);
        }
        System.out.println("LANZAR | " + ok + " Shorts programados (1 por dia) a las " + hour + ":00.");
    }

    /** Programa un video concreto para publicarse en la fecha dada a la hora configurada. */
    public static void planificarUn(Config cfg, Path video, String titulo, String desc,
                                    LocalDate fecha) throws Exception {
        Path mk = video.resolveSibling(video.getFileName().toString() + ".programado");
        if (Files.exists(mk)) {
            System.out.println("PLANIFICAR | (ya programado) " + video.getFileName());
            return;
        }
        int hour = Integer.parseInt(cfg.get("youtube.publishHour", "9"));
        OffsetDateTime pub = fecha.atTime(hour, 0).atOffset(ZoneOffset.ofHours(-5));
        System.out.println("PLANIFICAR | " + pub.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                + " | " + video.getFileName());
        Youtube.UploadResult r = Youtube.upload(cfg, video, titulo, desc, etiquetasSubida(null), pub,
                "private", System.out::println);
        Util.write(mk, pub.toString());
        System.out.println("PLANIFICAR | ok -> " + r.url());
    }

    private static String[] leerMeta(Path carpeta, Path clip) {
        String base = sinExt(clip);
        for (Path p : List.of(carpeta.resolve(base + "_copy.txt"),
                carpeta.resolve(base + ".txt"))) {
            if (!Files.exists(p)) continue;
            String txt = Util.read(p);
            String titulo = null, tags = null;
            for (String line : txt.split("\\r?\\n")) {
                String l = line.trim();
                if (titulo == null && l.startsWith("- ")) titulo = l.substring(2).trim();
                if (l.startsWith("Etiquetas:")) tags = l.substring("Etiquetas:".length()).trim();
            }
            return new String[]{titulo, tags};
        }
        return new String[]{null, null};
    }

    /** Une escenas verticales ya producidas (clip_XX.mp4) en Shorts de ~15-25s. */
    public static void escenas(Config cfg, Path clipsDir, Path outDir, String tituloBase, int durMeta)
            throws Exception {
        clipsDir = clipsDir.toAbsolutePath();
        outDir = outDir.toAbsolutePath();
        Files.createDirectories(outDir);
        List<Path> escenas = new ArrayList<>();
        try (var s = Files.list(clipsDir)) {
            s.filter(p -> p.getFileName().toString().matches("(?i)clip_.*\\.mp4"))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString())).forEach(escenas::add);
        }
        if (escenas.isEmpty()) {
            System.out.println("SHORTS | no hay clip_*.mp4 en " + clipsDir);
            return;
        }
        String base = sinExt(clipsDir);
        if (base.equalsIgnoreCase("clips") && clipsDir.getParent() != null) {
            base = sinExt(clipsDir.getParent()); // ficciÃ³n: clips -> nombre del cuento
        }
        int grupo = 0, idx = 1, salida = 0;
        List<Path> cur = new ArrayList<>();
        double acum = 0;
        while (idx <= escenas.size()) {
            Path p = escenas.get(idx - 1);
            double d = Util.mediaDurationSeconds(p, Util.ffprobeBin(cfg));
            cur.add(p);
            acum += d;
            if (acum >= durMeta || idx == escenas.size()) {
                salida++;
                Path out = outDir.resolve(String.format(Locale.ROOT, "CLIP_%02d_%s.mp4", salida, base));
                fileConcat(cfg, cur, out);
                if (Files.exists(out)) {
                    String t0 = tituloBase == null ? base : tituloBase;
                    Util.write(outDir.resolve(String.format(Locale.ROOT, "CLIP_%02d_%s_copy.txt", salida, base)),
                            textoClips(t0, salida, 0, acum, 0));
                    System.out.println(String.format(Locale.ROOT, "SHORTS | %02d (%ds) -> %s",
                            salida, Math.round(acum), out.getFileName()));
                }
                cur = new ArrayList<>();
                acum = 0;
                grupo++;
            }
            idx++;
        }
    }

    private static void fileConcat(Config cfg, List<Path> parts, Path out) {
        try {
            Path list = Files.createTempFile("concat", ".txt");
            StringBuilder sb = new StringBuilder();
            for (Path p : parts) sb.append("file '").append(p.toAbsolutePath().toString().replace("'", "'\\''"))
                    .append("'\n");
            StringBuilder log = new StringBuilder();
            Util.write(list, sb.toString());
            int code = Util.run(List.of(cfg.ffmpeg(), "-y", "-f", "concat", "-safe", "0", "-i",
                    list.toString(), "-c", "copy", "-movflags", "+faststart", out.toString()), out.getParent(), log);
            Files.deleteIfExists(list);
            if (code != 0) throw new IllegalStateException("concat demuxer fallÃ³");
        } catch (Exception e) {
            StringBuilder fc = new StringBuilder();
            for (int i = 0; i < parts.size(); i++) {
                if (i > 0) fc.append(";");
                fc.append("[").append(i).append(":v][").append(i).append(":a]");
            }
            fc.append("concat=n=").append(parts.size()).append(":v=1:a=1[outv][outa]");
            List<String> cmd = new ArrayList<>();
            cmd.add(cfg.ffmpeg()); cmd.add("-y");
            for (Path p : parts) { cmd.add("-i"); cmd.add(p.toString()); }
            cmd.add("-filter_complex"); cmd.add(fc.toString());
            cmd.add("-map"); cmd.add("[outv]"); cmd.add("-map"); cmd.add("[outa]");
            cmd.add("-c:v"); cmd.add("libx264"); cmd.add("-crf"); cmd.add("20");
            cmd.add("-preset"); cmd.add("veryfast"); cmd.add("-c:a"); cmd.add("aac");
            cmd.add("-movflags"); cmd.add("+faststart");
            cmd.add(out.toString());
            StringBuilder lg = new StringBuilder();
            int code = Util.run(cmd, out.getParent(), lg);
            if (code != 0) System.out.println("SHORTS | no se pudo concatenar: " + lg);
        }
    }

    private static String sinExt(Path p) {
        String n = p.getFileName().toString();
        int i = n.lastIndexOf('.');
        return i > 0 ? n.substring(0, i) : n;
    }

    private Clipper() {}
}