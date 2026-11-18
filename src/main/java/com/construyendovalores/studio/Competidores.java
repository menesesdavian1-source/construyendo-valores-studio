package com.construyendovalores.studio;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Barrido diario de los competidores: baja los ultimos videos publicos de los
 * canales configurados en competidores.ids (separados por ';') y los guarda en
 * competidores.csv con vistas/me gusta/comentarios/duracion. Devuelve las
 * "lecciones" (patrones de formato y titulo que funcionan) para alimentar el
 * bucle de decisiones del contenido.
 */
public final class Competidores {

    private static final String IDS_DEFECTO = "UCYQo8CdhXD22qfwUBUw591Q";

    private static Path csv() {
        return Paths.get(System.getProperty("user.home"), ".construyendo-valores", "competidores.csv");
    }

    /** Baja los ultimos videos de los competidores (una vez al dia) y devuelve las lecciones. */
    public static String barrerDiario(Config cfg, Consumer<String> log) throws Exception {
        long last = 0;
        try {
            last = Long.parseLong(cfg.get("competidores.ultimaConsulta", "0"));
        } catch (NumberFormatException ignored) {
        }
        boolean refrescar = (System.currentTimeMillis() - last) >= 86_400_000L;

        String ids = cfg.get("competidores.ids", IDS_DEFECTO);
        List<String[]> filas = new ArrayList<>();
        for (String id : ids.split(";")) {
            id = id.trim();
            if (id.isEmpty()) continue;
            try {
                List<Youtube.CompetidorVideo> vids = Youtube.listCompetidor(cfg, id, 12);
                for (Youtube.CompetidorVideo v : vids) {
                    filas.add(new String[]{LocalDate.now().toString(), id, v.id(), esc(v.titulo()),
                            String.valueOf(v.vistas()), String.valueOf(v.megusta()),
                            String.valueOf(v.comentarios()), String.valueOf(v.duracion())});
                }
                log.accept("Competidores | " + id + " -> " + vids.size() + " videos.");
            } catch (Exception e) {
                log.accept("AVISO competidor " + id + ": " + e.getMessage().split("\\n")[0]);
            }
        }
        if (refrescar && !filas.isEmpty()) {
            Files.createDirectories(csv().getParent());
            StringBuilder sb = new StringBuilder("fecha;canal;videoId;titulo;vistas;megusta;comentarios;duracion\n");
            for (String[] r : filas) sb.append(String.join(";", r)).append('\n');
            Files.write(csv(), sb.toString().getBytes(StandardCharsets.UTF_8));
            cfg.set("competidores.ultimaConsulta", String.valueOf(System.currentTimeMillis()));
            cfg.save();
            log.accept("Competidores | CSV actualizado.");
        }
        return lecciones(cfg);
    }

    /** Extrae patrones del ultimo barrido: formato, titulo y cadencia del que mas rinde. */
    public static String lecciones(Config cfg) {
        List<String[]> f = leer();
        if (f.isEmpty()) return "";
        long best = -1;
        String[] top = null;
        int largos = 0, cortos = 0;
        for (String[] r : f) {
            try {
                long v = Long.parseLong(r[4]);
                if (v > best) {
                    best = v;
                    top = r;
                }
                long d = Long.parseLong(r[7]);
                if (d >= 85) largos++;
                else cortos++;
            } catch (Exception ignored) {
            }
        }
        StringBuilder sb = new StringBuilder();
        if (top != null) {
            sb.append("El competidor rinde mas con: ").append(top[3]).append('\n');
            long d = safeLong(top[7]);
            sb.append("Formato: ").append(d >= 600 ? "compilacion/entrega larga ("
                    + (d / 60) + " min)" : d >= 85 ? "video corto (" + d + " s)" : "Short (" + d + " s)").append('\n');
        }
        if (largos + cortos > 0) {
            sb.append("Mezcla del barrido: ").append(cortos).append(" Shorts, ")
                    .append(largos).append(" videos largos\n");
            if (largos > cortos) sb.append("Apostar por entregas largas (compilaciones/cuentos) como los competidores.\n");
            else sb.append("Apostar por Shorts frecuentes (descubrimiento) como los competidores.\n");
        }
        return sb.toString();
    }

    private static List<String[]> leer() {
        List<String[]> out = new ArrayList<>();
        try {
            if (!Files.exists(csv())) return out;
            List<String> ls = Files.readAllLines(csv(), StandardCharsets.UTF_8);
            for (int i = 1; i < ls.size(); i++) {
                List<String> f = DailyVideo.splitCsv(ls.get(i));
                if (f.size() >= 8) out.add(f.toArray(new String[0]));
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    private static long safeLong(String s) {
        try {
            return Long.parseLong(s.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace(";", "/").replace("\n", " ");
    }

    private Competidores() {}
}