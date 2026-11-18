package com.construyendovalores.studio;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Bucle de decisiones de contenido. Tras cada video:
 *  1. compara el indice de calidad con lo anterior (objetivo: +10% minimo);
 *  2. incorpora las lecciones del barrido diario de competidores;
 *  3. decide las mejoras concretas para la produccion siguiente;
 *  4. aplica automaticamente la mejora de movimiento (calidad.movimiento x1.10).
 * Escribe DECISIONES_AAAA-MM-DD.md con todo lo decidido.
 */
public final class Decisor {

    /** MAXIMO de energia de movimiento permitido (evita animacion nerviosa). */
    private static final double MOV_MAX = 1.6;

    public static List<String> decidir(Config cfg, Path dir, Consumer<String> log) throws Exception {
        List<String> decisiones = new ArrayList<>();

        // Contexto: ultima calidad medida y lecciones de competidores.
        Path referencia = Paths.get(cfg.get("calidad.referencia", ""));
        IndiceCalidad.Resultado previo = null;
        if (Files.exists(referencia)) previo = IndiceCalidad.ultima(referencia);
        if (previo == null) {
            Path foto = ultimaEnCalidadCSV();
            if (foto != null) {
                previo = IndiceCalidad.ultima(foto);
                referencia = foto;
            }
        }
        String lecciones = Competidores.lecciones(cfg);

        if (previo == null) {
            decisiones.add("Linea base: aun no hay indice previo; el proximo video fija el punto de partida.");
        } else {
            double objetivo = previo.indice() * 1.10;
            double objetivoMov = previo.movimiento() * 1.10;
            decisiones.add(String.format("Regla +10%%/dia: indice anterior %.1f (objetivo global %.1f). "
                    + "El eje con margen es el movimiento: %.1f -> objetivo > %.1f.",
                    previo.indice(), objetivo, previo.movimiento(), objetivoMov));
        }

        // Mejora de movimiento: subir energia +10% salvo que ya este al maximo.
        double mov = cfg.getDouble("calidad.movimiento", 1.0);
        double nuevo = Math.min(MOV_MAX, mov * 1.10);
        decisiones.add(String.format("Energia de movimiento (calidad.movimiento): %.2f -> %.2f (%s).",
                mov, nuevo, nuevo >= MOV_MAX ? "MAX alcanzado" : "+10%"));
        cfg.set("calidad.movimiento", String.valueOf(Math.round(nuevo * 100.0) / 100.0));
        if (Files.exists(referencia)) cfg.set("calidad.referencia", referencia.toString());
        cfg.save();

        if (!lecciones.isBlank()) {
            decisiones.add("Barrido competidores -> " + lecciones.trim().replace("\n", " | "));
        } else {
            decisiones.add("Barrido competidores: sin datos aun (se consultaran con --competidores).");
        }

        decisiones.add(cadenciaYProximo());
        decisiones.add("Control final: medir el nuevo video con --calidad; si el eje de movimiento no supera "
                + "un +10% al anterior, se vuelve a subir la energia (calidad.movimiento) y se regenera.");

        Path out = dir.resolve("DECISIONES_" + LocalDate.now() + ".md");
        Files.createDirectories(dir);
        StringBuilder md = new StringBuilder("# DECISIONES DE CONTENIDO - ").append(LocalDate.now()).append("\n\n");
        int n = 1;
        for (String d : decisiones) {
            md.append(n++).append(". ").append(d).append("\n");
        }
        Files.write(out, md.toString().getBytes(StandardCharsets.UTF_8));

        Path logFile = dir.resolve("DECISIONES_LOG.md");
        Files.write(logFile, ("\n## " + LocalDate.now() + "\n" + md.substring(md.indexOf("1.")).trim() + "\n")
                        .getBytes(StandardCharsets.UTF_8),
                Files.exists(logFile) ? java.nio.file.StandardOpenOption.APPEND : java.nio.file.StandardOpenOption.CREATE);

        log.accept("DECISION | energia movimiento -> " + Math.round(nuevo * 100.0) / 100.0);
        log.accept("DECISIONES | " + out.toAbsolutePath());
        return decisiones;
    }

    private static Path ultimaEnCalidadCSV() {
        try {
            Path csv = Paths.get(System.getProperty("user.home"), ".construyendo-valores", "calidad.csv");
            if (!Files.exists(csv)) return null;
            List<String> ls = Files.readAllLines(csv, StandardCharsets.UTF_8);
            for (int i = ls.size() - 1; i >= 1; i--) {
                List<String> f = DailyVideo.splitCsv(ls.get(i));
                if (f.size() >= 3 && !f.get(1).isBlank()) return Paths.get(f.get(1));
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static String cadenciaYProximo() {
        LocalDate d = LocalDate.now();
        StringBuilder s = new StringBuilder();
        switch (d.getDayOfWeek()) {
            case SATURDAY -> s.append("Hoy sabado: publicar la cancion de la aldea. ");
            case SUNDAY -> s.append("Hoy domingo: publicar la compilacion larga. ");
            case WEDNESDAY -> s.append("Hoy miercoles: publicar el cuento de la aldea. ");
            default -> s.append("Hoy: publicar 1 Short vertical de descubrimiento. ");
        }
        try {
            Ronda r = RondaLibrary.byIdOrIndex(String.valueOf(RondaLibrary.nextRondaIndex(new Config())));
            if (r != null) s.append("Proxima produccion: --ronda ").append(r.id()).append(" (")
                    .append(r.titulo()).append(").");
        } catch (Exception ignored) {
        }
        return s.toString();
    }

    private Decisor() {}
}