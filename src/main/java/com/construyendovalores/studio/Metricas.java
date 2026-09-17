package com.construyendovalores.studio;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Registro diario de publicaciones y rendimiento (metricas.csv).
 *
 * La cola diaria apunta el resultado de cada subida; --metricas actualiza
 * vistas/me gusta/comentarios desde la YouTube Data API (una vez al día).
 */
public final class Metricas {

    private static final String[] HEADER = {"fecha", "tipo", "titulo", "archivo", "estado", "videoId", "url",
            "vistas", "comentarios", "megusta"};

    private static Path file() {
        return Paths.get(System.getProperty("user.home"), ".construyendo-valores", "metricas.csv");
    }

    public static Path rutaCSV() {
        return file();
    }

    /** Apunta una publicación (subida programada, fallo de check, etc.). */
    public static synchronized void registrar(Config cfg, String fecha, String tipo, String titulo, String archivo,
                                              String estado, String videoId, String url, Consumer<String> log) {
        try {
            Path f = file();
            boolean nuevo = !Files.exists(f);
            StringBuilder linea = new StringBuilder();
            String[] v = {fecha, tipo, titulo, archivo, estado, videoId, url, "", "", ""};
            for (int i = 0; i < v.length; i++) {
                if (i > 0) linea.append(';');
                linea.append(esc(v[i]));
            }
            if (nuevo) {
                Files.createDirectories(f.getParent());
                Files.write(f, (String.join(";", HEADER) + "\n").getBytes(StandardCharsets.UTF_8));
            }
            Files.write(f, (linea + "\n").getBytes(StandardCharsets.UTF_8), java.nio.file.StandardOpenOption.APPEND);
            log.accept("METRICAS | registro: " + estado + " | " + titulo);
        } catch (Exception e) {
            log.accept("AVISO: no se pudo registrar la métrica (" + e.getMessage() + ").");
        }
    }

    /** Actualiza el CSV con el rendimiento real y muestra el resumen. Una vez por día. */
    public static synchronized void refrescar(Config cfg, Consumer<String> log) throws Exception {
        long last = 0;
        try {
            last = Long.parseLong(cfg.get("metricas.ultimaConsulta", "0"));
        } catch (NumberFormatException ignored) {
        }
        long ahora = System.currentTimeMillis();
        List<Youtube.Estadisticas> datos = Youtube.listEstadisticas(cfg, 50);
        if (datos.isEmpty()) {
            log.accept("Metricas | aun no hay videos en el canal.");
            return;
        }

        // Conservar tipo/fecha/archivo de los registros previos (aparecen igual de nuevo).
        Map<String, String[]> viejos = leerActuales();
        List<String[]> filas = new ArrayList<>();
        for (Youtube.Estadisticas d : datos) {
            String[] antes = viejos.get(d.id());
            String fecha = antes != null ? antes[0] : (d.fecha() != null && d.fecha().length() >= 10 ? d.fecha().substring(0, 10) : "");
            String tipo = antes != null ? antes[1] : "?";
            String archivo = antes != null ? antes[3] : "";
            filas.add(new String[]{fecha, tipo, d.titulo(), archivo, d.estado(), d.id(),
                    "https://youtu.be/" + d.id(),
                    String.valueOf(d.vistas()), String.valueOf(d.comentarios()), String.valueOf(d.meGusta())});
        }
        if (ahora - last < 86_400_000L) {
            log.accept("Metricas | datos ya vistos hoy; se mantiene el CSV sin re-consultar (YouTube guarda cuota).");
        } else {
            Path f = file();
            Files.createDirectories(f.getParent());
            StringBuilder csv = new StringBuilder(String.join(";", HEADER)).append('\n');
            for (String[] r : filas) {
                for (int i = 0; i < r.length; i++) {
                    if (i > 0) csv.append(';');
                    csv.append(esc(r[i]));
                }
                csv.append('\n');
            }
            Files.write(f, csv.toString().getBytes(StandardCharsets.UTF_8));
            cfg.set("metricas.ultimaConsulta", String.valueOf(ahora));
            cfg.save();
            log.accept("Metricas | CSV actualizado (" + filas.size() + " videos).");
        }

        log.accept("---------------- RESUMEN ----------------");
        for (String[] r : filas) {
            log.accept(String.format("  %s | %-8s | vistas %6s | me gusta %5s | comen %4s | %s",
                    r[0], r[1], r[7], r[9], r[8], r[2]));
        }
        log.accept("CSV: " + file().toAbsolutePath());
    }

    private static Map<String, String[]> leerActuales() {
        Map<String, String[]> out = new LinkedHashMap<>();
        try {
            if (!Files.exists(file())) return out;
            List<String> lineas = Files.readAllLines(file(), StandardCharsets.UTF_8);
            for (int i = 1; i < lineas.size(); i++) {
                List<String> f = DailyVideo.splitCsv(lineas.get(i));
                if (f.size() >= 9 && !f.get(5).isBlank()) {
                    out.put(f.get(5), f.toArray(new String[0]));
                }
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    private static String esc(String s) {
        if (s == null) return "";
        if (s.contains(";") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    private Metricas() {}
}