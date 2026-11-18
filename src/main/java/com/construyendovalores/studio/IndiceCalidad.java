package com.construyendovalores.studio;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Indice de calidad medible de un video (0-100). Alimenta la regla "+10% por
 * video": se mide antes y despues de cada produccion; si el nuevo no supera al
 * anterior en un 10%, el bucle de decisiones sube la energia del movimiento.
 *
 * Componentes:
 *  - tecnica (0-1): resolucion, fps y duracion.
 *  - audio  (0-1): loudness integrado objetivo -14 LUFS.
 *  - movimiento (0-1): energia visual entre frames (blend=difference).
 */
public final class IndiceCalidad {

    public record Resultado(String archivo, double indice, double movimiento, double audio, double tecnica) {
    }

    private static Path csv() {
        return Paths.get(System.getProperty("user.home"), ".construyendo-valores", "calidad.csv");
    }

    /** Evalua un video y lo anota en calidad.csv. */
    public static Resultado evaluar(Config cfg, Path video, Consumer<String> log) {
        String ff = cfg.ffmpeg();
        String ffprobe = Util.ffprobeBin(cfg);

        List<String> media = List.of(ffprobe, "-v", "error", "-select_streams", "v:0",
                "-show_entries", "stream=width,height,avg_frame_rate:format=duration",
                "-of", "csv=p=0", video.toString());
        String out = Util.capture(media, null);
        double ancho = 0, alto = 0, dur = 0, fps = 0;
        java.util.regex.Matcher vm = java.util.regex.Pattern
                .compile("^(\\d+),(\\d+),([\\d/]+)$", java.util.regex.Pattern.MULTILINE).matcher(out);
        if (vm.find()) {
            ancho = Double.parseDouble(vm.group(1));
            alto = Double.parseDouble(vm.group(2));
            fps = fpsFrac(vm.group(3));
        }
        java.util.regex.Matcher dm = java.util.regex.Pattern
                .compile("(\\d+(?:\\.\\d+))").matcher(out);
        if (dm.find()) dur = Double.parseDouble(dm.group(1));

        // Audio: loudness integrado (Loudness Units Full Scale).
        List<String> acmd = List.of(ff, "-hide_banner", "-i", video.toString(),
                "-filter_complex", "ebur128=metadata=1", "-f", "null", "-");
        double iLufs = Double.NaN;
        String aout = Util.capture(acmd, null);
        Matcher m = Pattern.compile("I:\\s*(-?\\d+(?:\\.\\d+)?) LUFS").matcher(aout);
        if (m.find()) {
            try {
                iLufs = Double.parseDouble(m.group(1));
            } catch (NumberFormatException ignored) {
            }
        }

        // Movimiento: energia entre frames consecutivos (escala reducida para agilizar).
        List<String> mcmd = List.of(ff, "-hide_banner", "-i", video.toString(),
                "-vf", "scale=176:314:flags=bilinear,tblend=all_mode=difference,"
                        + "signalstats,metadata=print:file=-",
                "-an", "-f", "null", "-");
        double mediaDiff = 0;
        int nY = 0;
        Matcher ym = Pattern.compile("YAVG=([\\d.]+)").matcher(Util.capture(mcmd, null));
        while (ym.find()) {
            mediaDiff += Double.parseDouble(ym.group(1));
            nY++;
        }
        if (nY > 0) mediaDiff /= nY;

        double res = ancho * alto > 0 ? Math.min(1, (ancho * alto) / 2_073_600.0) : 0;
        double tec = (ancho > 0 || alto > 0) ? (0.5 * res + 0.25 * Math.min(1, (fps > 0 ? fps : 0) / 25.0)
                + 0.25 * Math.min(1, dur / 45.0)) : 0;
        double aud = Double.isNaN(iLufs) ? 0.5 : Math.max(0, Math.min(1, 1 - Math.abs(iLufs + 14.0) / 14.0));
        double mov = Math.max(0, Math.min(1, mediaDiff / 12.0));
        double indice = 100 * (0.35 * tec + 0.30 * aud + 0.35 * mov);

        Resultado r = new Resultado(video.toString(), round(indice), round(mov * 100),
                round(aud * 100), round(tec * 100));
        anotar(r);
        log.accept(String.format("CALIDAD | %s | indice %.1f/100 | movimiento %.1f | audio %.1f | tecnica %.1f",
                video.getFileName(), r.indice(), r.movimiento(), r.audio(), r.tecnica()));
        return r;
    }

    /** Lee la ultima evaluacion del archivo dado (si existe). */
    public static Resultado ultima(Path archivo) {
        try {
            if (!Files.exists(csv())) return null;
            List<String> ls = Files.readAllLines(csv(), StandardCharsets.UTF_8);
            Resultado r = null;
            for (int i = 1; i < ls.size(); i++) {
                List<String> f = DailyVideo.splitCsv(ls.get(i));
                if (f.size() >= 6 && f.get(1).equals(archivo.toString())) {
                    r = new Resultado(f.get(1), Double.parseDouble(f.get(2)),
                            Double.parseDouble(f.get(3)), Double.parseDouble(f.get(4)),
                            Double.parseDouble(f.get(5)));
                }
            }
            return r;
        } catch (Exception e) {
            return null;
        }
    }

    /** Mejor indice historico anterior a la evaluacion del archivo indicado (-1 si no hay). */
    public static double mejorAnterior(Path archivo) {
        try {
            if (!Files.exists(csv())) return -1;
            List<String> ls = Files.readAllLines(csv(), StandardCharsets.UTF_8);
            double mejor = -1;
            for (int i = 1; i < ls.size(); i++) {
                List<String> f = DailyVideo.splitCsv(ls.get(i));
                if (f.size() >= 3 && !f.get(1).equals(archivo.toString())) {
                    mejor = Math.max(mejor, Double.parseDouble(f.get(2)));
                }
            }
            return mejor;
        } catch (Exception e) {
            return -1;
        }
    }

    private static void anotar(Resultado r) {
        try {
            Files.createDirectories(csv().getParent());
            List<String> ls = Files.exists(csv())
                    ? Files.readAllLines(csv(), StandardCharsets.UTF_8) : new ArrayList<>();
            StringBuilder sb = new StringBuilder();
            if (ls.isEmpty()) sb.append("fecha;archivo;indice;mov;audio;tecnica\n");
            sb.append(LocalDate.now()).append(';').append(r.archivo().replace(';', '|'))
                    .append(';').append(r.indice()).append(';').append(r.movimiento())
                    .append(';').append(r.audio()).append(';').append(r.tecnica()).append('\n');
            Files.write(csv(), sb.toString().getBytes(StandardCharsets.UTF_8),
                    ls.isEmpty() ? StandardOpenOption.CREATE : StandardOpenOption.APPEND);
        } catch (Exception ignored) {
        }
    }

    private static double num(String[] p, int i) {
        try {
            return i < p.length && !p[i].isBlank() ? Double.parseDouble(p[i]) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private static double fpsFrac(String s) {
        try {
            if (s.contains("/")) {
                String[] p = s.split("/");
                return Double.parseDouble(p[0]) / Double.parseDouble(p[1]);
            }
            return Double.parseDouble(s);
        } catch (Exception e) {
            return 0;
        }
    }

    private static double round(double d) {
        return Math.round(d * 10.0) / 10.0;
    }

    private IndiceCalidad() {}
}