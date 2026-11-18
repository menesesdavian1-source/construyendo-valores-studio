package com.construyendovalores.studio;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

/**
 * Bucle de mejora diaria del canal. Se ejecuta tras cada subida (--mejora):
 * lee el rendimiento real de la YouTube API (via Metricas), aplica reglas de
 * productor y escribe un plan concreto "MEJORAS_AAAA-MM-DD.md" con lo que se
 * mejora para el proximo video. Objetivo: cada dia que se sube, el contenido
 * sube de nivel (titulos, formato, compilacion, cadencia).
 */
public final class CicloMejora {

    private static final String[] VALORES = {"Ayudar", "Amistad", "Orden", "Paciencia",
            "Naturaleza", "Gratitud", "Ejercicio", "Escuchar"};

    /** Ejecuta el ciclo y devuelve el informe generado. */
    public static Path ejecutar(Config cfg, Path dir, Consumer<String> log) throws Exception {
        try {
            Metricas.refrescar(cfg, log);
        } catch (Exception e) {
            log.accept("AVISO: la API no respondio (" + e.getMessage().split("\\n")[0]
                    + "). Se usan los datos del ultimo CSV.");
        }

        List<Video> vids = leerCSV(Metricas.rutaCSV());
        LocalDate hoy = LocalDate.now();

        StringBuilder md = new StringBuilder();
        md.append("# MEJORAS DIARIAS - ").append(hoy).append("\n\n");
        md.append("Regla fija: cada video publicado dispara esta revision. "
                + "Se mide vistas/dia y engagement y se decide la mejora del proximo.\n\n");

        long[] orden = new long[]{};

        if (vids.isEmpty()) {
            md.append("## ESTADO\nAun no hay videos medidos. Publica los 3 primeros "
                    + "(cancion + 2 shorts) con la cadencia y vuelve a ejecutar manana.\n\n");
        } else {
            for (Video v : vids) {
                v.pace = v.vistas / Math.max(1L, v.dias);
            }
            orden = vids.stream().mapToLong(v -> v.pace).sorted().toArray();
            long mediana = orden.length > 0 ? orden[orden.length / 2] : 0;

            Video ganador = vids.stream().max((a, b) -> Long.compare(a.pace, b.pace)).orElse(null);
            Video perdedor = vids.stream().min((a, b) -> Long.compare(a.pace, b.pace)).orElse(null);

            md.append("## VIDEOS MEDIDOS (").append(vids.size()).append(")\n");
            md.append("| fecha | tipo | titulo | vistas | like | comen | vistas/dia |\n");
            md.append("|---|---|---|---|---|---|---|\n");
            for (Video v : vids) {
                md.append("| ").append(v.fecha).append(" | ").append(v.tipo).append(" | ")
                        .append(v.titulo.replace("|", "/")).append(" | ").append(v.vistas)
                        .append(" | ").append(v.megusta).append(" | ").append(v.comentarios)
                        .append(" | ").append(v.pace).append(" |\n");
            }

            md.append("\n## REGLAS APLICADAS\n");
            if (ganador != null && ganador.pace > Math.max(1, mediana)) {
                md.append("- COPIA LO QUE FUNCIONA: el formato `").append(ganador.titulo)
                        .append("` rinde ").append(ganador.pace)
                        .append(" vistas/dia. El proximo video usara el MISMO patron de "
                        + "titulo, etiquetas y hora de publicacion.\n");
            } else {
                md.append("- Aun no hay un ganador claro (experimentar con titulo/portada).\n");
            }
            if (perdedor != null && orden.length > 2 && perdedor.pace < Math.max(1, mediana)) {
                md.append("- REHACE LO QUE NO: `").append(perdedor.titulo).append("` (" )
                        .append(perdedor.pace)
                        .append(" vistas/dia). Opciones: titulo A/B de la lista, portada nueva, "
                        + "version 40s como Short, o archivarlo.\n");
            }
        }

        md.append("\n## MEJORAS CONCRETAS PARA EL PROXIMO VIDEO\n");
        List<String> mejoras = mejorasParaProximo(vids, cadencia(hoy));
        int n = 1;
        for (String m : mejoras) {
            md.append(n++).append(". ").append(m).append("\n");
        }

        Ronda sig = siguienteCancion(cfg);
        List<String> ab = titulosAB(sig == null ? "La cancion de la aldea" : sig.titulo());
        md.append("\n## TITULOS A/B PARA EL PROXIMO\n");
        n = 1;
        for (String t : ab) {
            md.append(n++).append(". `").append(t).append("`\n");
        }

        md.append("\n## CADENCIA HOY (")
                .append(cadencia(hoy)).append(")\n");
        md.append(cadenciaTexto(hoy, sig)).append("\n\n");

        md.append("## PROXIMA CANCION DE LA ALDEA\n");
        if (sig == null) {
            md.append("No hay canciones en el catalogo. Crea la primera aldea.\n");
        } else {
            md.append("- id: `").append(sig.id()).append("`\n");
            md.append("- titulo: ").append(sig.titulo()).append("\n");
            md.append("- valor: ").append(sig.valor()).append("\n");
            md.append("- escenario: ").append(sig.escenario()).append("\n");
            md.append("Se genera con `--ronda ").append(sig.id()).append(" <carpeta>` y se publica el sabado.\n");
        }

        Path out = dir.resolve("MEJORAS_" + hoy + ".md");
        Files.createDirectories(dir);
        Files.write(out, md.toString().getBytes(StandardCharsets.UTF_8));

        Path logm = dir.resolve("MEJORAS_LOG.md");
        Files.createDirectories(dir);
        Files.write(logm, ("\n## " + hoy + "\n" + md.substring(md.indexOf("## REGLAS APLICADAS"),
                md.indexOf("## PROXIMA CANCION")).trim() + "\n").getBytes(StandardCharsets.UTF_8),
                Files.exists(logm) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE);
        try {
            cfg.set("proceso.mejoraVersion", String.valueOf(Integer.parseInt(cfg.get("proceso.mejoraVersion", "0")) + 1));
        } catch (NumberFormatException ignored) {
            cfg.set("proceso.mejoraVersion", "1");
        }
        cfg.save();
        return out;
    }

    private static Ronda siguienteCancion(Config cfg) {
        try {
            return RondaLibrary.byIdOrIndex(String.valueOf(RondaLibrary.nextRondaIndex(cfg)));
        } catch (Exception e) {
            return null;
        }
    }

    /** Reglas aplicadas; siempre incluyen la mejora obligatoria del dia. */
    private static List<String> mejorasParaProximo(List<Video> vids, String hoy) {
        List<String> out = new ArrayList<>();
        out.add("Aplica el titulo A/B indicado abajo y la misma portada viva del canal.");
        if ("sabado".equals(hoy) || "domingo".equals(hoy)) {
            out.add("La cancion de la aldea sale HOY o MANANA: verifica que este generada "
                    + "con el pipeline pulido (reverb + -14 LUFS + marca) antes de publicar.");
        }
        if (vids.stream().anyMatch(v -> v.tipo != null && v.tipo.contains("cancion"))) {
            out.add("El estribillo (2-3 lineas) tambien va como Short de 30-40s el mismo dia "
                    + "para doble descubrimiento.");
        }
        boolean hayCompila = vids.stream().anyMatch(v -> v.tipo != null && v.tipo.contains("compil"));
        out.add(hayCompila
                ? "Ya existe compilacion: renovarla cada domingo agregando las canciones nuevas."
                : "Preparar la compilacion larga (8+ min) del domingo con las canciones publicadas "
                        + "para subir los minutos de visionado (clave para monetizar).");
        return out;
    }

    private static String cadencia(LocalDate d) {
        switch (d.getDayOfWeek()) {
            case SATURDAY: return "sabado";
            case SUNDAY: return "domingo";
            case MONDAY: return "lunes";
            case TUESDAY: return "martes";
            case WEDNESDAY: return "miercoles";
            case THURSDAY: return "jueves";
            default: return "viernes";
        }
    }

    private static String cadenciaTexto(LocalDate d, Ronda sig) {
        switch (d.getDayOfWeek()) {
            case SATURDAY:
                return "SABADO: publicar la cancion de la aldea (o verificarla). Nunca faltar: "
                        + (sig != null ? "--ronda " + sig.id() : "") + ".";
            case SUNDAY:
                return "DOMINGO: publicar la compilacion larga de la semana (8+ min).";
            case WEDNESDAY:
                return "MIERCOLES: publicar el cuento de la aldea y su Short.";
            case MONDAY:
            case TUESDAY:
            case THURSDAY:
            case FRIDAY:
            default:
                return "HOY: publicar 1 Short vertical de descubrimiento (cuento o cancion) "
                        + "para alimentar el feed de Shorts.";
        }
    }

    private static List<String> titulosAB(String base) {
        return List.of(
                base + " | Canciones Infantiles - Construyendo Valores",
                base + " (Short) #Shorts",
                base + " - Valor de la semana | Cuento Infantil",
                "Aprendemos valores con " + base + " | Construyendo Valores");
    }

    private static List<Video> leerCSV(Path csv) {
        List<Video> out = new ArrayList<>();
        try {
            if (!Files.exists(csv)) return out;
            List<String> lineas = Files.readAllLines(csv, StandardCharsets.UTF_8);
            LocalDate hoy = LocalDate.now();
            for (int i = 1; i < lineas.size(); i++) {
                List<String> f = DailyVideo.splitCsv(lineas.get(i));
                if (f.size() < 10) continue;
                Video v = new Video();
                v.fecha = f.get(0);
                v.tipo = f.get(1);
                v.titulo = f.get(2);
                v.estado = f.get(4);
                v.vistas = num(f.get(7));
                v.comentarios = num(f.get(8));
                v.megusta = num(f.get(9));
                v.dias = 1;
                if (!v.fecha.isBlank() && v.fecha.length() >= 10) {
                    try {
                        LocalDate fd = LocalDate.parse(v.fecha.substring(0, 10));
                        v.dias = Math.max(1, (int) ChronoUnit.DAYS.between(fd, hoy));
                    } catch (Exception ignored) {
                    }
                }
                if (!"private".equals(v.estado)) out.add(v);
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    private static long num(String s) {
        try {
            return Long.parseLong(s.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private static final class Video {
        String fecha;
        String tipo;
        String titulo;
        String estado;
        long vistas;
        long comentarios;
        long megusta;
        long dias;
        long pace;
    }

    private CicloMejora() {}
}