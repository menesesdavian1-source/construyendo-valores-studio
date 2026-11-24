package com.construyendovalores.studio;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;

/**
 * Producción en lote (--semanario): renderiza los próximos N videos a la cola
 * Programadas\YYYY-MM-DD\ con su dia.properties. Luego, cada mañana la tarea
 * diaria solo sube el video que ya está preparado (ve DailyVideo.queuedForToday).
 *
 * La planificación replica exactamente la rotación diaria (4 días: ronda,
 * historia, serie, tecnología) sin tocar la config real: los contadores "post.*" de
 * cada día se aplican cuando ese día se sube desde la cola.
 */
public final class SemanaVideo {

    private static final String[] KEYS = {
            "canal.videoCount", "canal.videoIndex", "canal.rondaIndex",
            "canal.techIndex", "canal.techListicle", "canal.techTrivia",
            "canal.techRonda", "canal.techHistoria",
            "canal.serieIndex", "canal.seriePregunta", "canal.serieTito",
            "canal.serieInventor", "canal.serieCanta", "canal.serieReto",
            "canal.titoCapitulo"};

    public static void generar(Config cfg, Path dir, int dias, Consumer<String> log) throws Exception {
        if (dias < 1) dias = 7;
        Path programadas = dir.resolve("Programadas");
        Files.createDirectories(programadas);

        Map<String, Integer> st = new HashMap<>();
        for (String k : KEYS) st.put(k, DailyVideo.parseNum(cfg, k, defaultValue(k)));

        List<String> plan = new ArrayList<>();
        plan.add("PLAN SEMANA - generado " + LocalDate.now() + " (la tarea diaria solo subira cada video)");
        plan.add("fecha | tipo | titulo");

        String today = LocalDate.now().toString();
        for (int d = 0; d < dias; d++) {
            LocalDate fecha = LocalDate.now().plusDays(d + 1);
            int counter = st.get("canal.videoCount");
            int turno = Math.floorMod(counter, 4);
            Path dayDir = programadas.resolve(fecha.toString());
            Files.createDirectories(dayDir);
            Path props = dayDir.resolve("dia.properties");

            if (Files.exists(props)) {
                Properties old = new Properties();
                old.load(new java.io.InputStreamReader(Files.newInputStream(props), StandardCharsets.UTF_8));
                for (String key : old.stringPropertyNames()) {
                    if (key.startsWith("post.")) {
                        st.put(key.substring("post.".length()), parse(old.getProperty(key)));
                    }
                }
                log.accept("Ya estaba en cola: " + fecha + " (se mantiene).");
                plan.add(fecha + " | (ya generado) | " + old.getProperty("titulo", ""));
                continue;
            }

            Properties dia = new Properties();
            dia.setProperty("fecha", fecha.toString());
            String tipo;

            if (turno == 1) {
                String[] out = rondaDia(cfg, dayDir, st, post(dia, st, counter));
                tipo = out[0];
                dia.setProperty("tipo", tipo);
                dia.setProperty("titulo", out[1]);
            } else if (turno == 2) {
                String[] out = historiaDia(cfg, dir, dayDir, log, st, post(dia, st, counter));
                tipo = out[0];
                dia.setProperty("tipo", tipo);
                dia.setProperty("titulo", out[1]);
            } else if (turno == 3) {
                String[] out = seriesDia(cfg, dayDir, log, st, post(dia, st, counter));
                tipo = out[0];
                dia.setProperty("tipo", tipo);
                dia.setProperty("titulo", out[1]);
            } else {
                String[] out = tecnologiaDia(cfg, dayDir, log, st, post(dia, st, counter));
                tipo = out[0];
                dia.setProperty("tipo", tipo);
                dia.setProperty("titulo", out[1]);
            }
            assert checkArchivo(dia, "archivo"); // el archivo lo fija cada helper
            try (var w = Files.newBufferedWriter(props, StandardCharsets.UTF_8)) {
                dia.store(w, "planificado por --semanario");
            }
            // Recargar los "post.*" para mantener el estado simulado coherente.
            Properties reload = new Properties();
            reload.load(new java.io.InputStreamReader(Files.newInputStream(props), StandardCharsets.UTF_8));
            for (String key : reload.stringPropertyNames()) {
                if (key.startsWith("post.")) {
                    st.put(key.substring("post.".length()), parse(reload.getProperty(key)));
                }
            }
            log.accept("Lote: " + fecha + " -> " + tipo + " | " + dia.getProperty("titulo"));
            plan.add(fecha + " | " + tipo + " | " + dia.getProperty("titulo"));
        }

        Util.write(programadas.resolve("plan_semana.txt"), String.join("\n", plan));
        log.accept("SEMANARIO | " + dias + " videos en cola en " + programadas.toAbsolutePath()
                + " (revisa plan_semana.txt)");
    }

    // ------------------------------------------------------------- días

    private static String[] rondaDia(Config cfg, Path dayDir, Map<String, Integer> st, Properties dia) throws Exception {
        int n = ((st.get("canal.rondaIndex") - 1) % RondaLibrary.clasicas().size()) + 1;
        Ronda r = RondaLibrary.byIdOrIndex(String.valueOf(n));
        cfg.set("video.scenario", r.escenario());
        RondaVideo.build(cfg, r, dayDir, s -> { });
        List<String> tags = new ArrayList<>(DailyVideo.tagsOf(r.etiquetas() != null ? r.etiquetas() : ""));
        tags.add("Construyendo Valores");
        tags.add("canciones para niños");
        dia.setProperty("archivo", Util.safeName(r.titulo()) + ".mp4");
        dia.setProperty("desc", DailyVideo.rondaDescription(r));
        dia.setProperty("tags", String.join(",", tags));
        dia.setProperty("post.canal.rondaIndex", String.valueOf((n % RondaLibrary.clasicas().size()) + 1));
        return new String[]{"RONDA", r.titulo()};
    }

    private static String[] historiaDia(Config cfg, Path dir, Path dayDir, Consumer<String> log,
                                        Map<String, Integer> st, Properties dia) throws Exception {
        List<DailyVideo.Row> plan = DailyVideo.loadPlan(dir, log);
        if (plan.isEmpty()) {
            throw new IllegalStateException("Se necesita CONTENIDO_30.csv para planificar historias. "
                    + "Ejecuta --contenido primero.");
        }
        int idx = ((st.get("canal.videoIndex") - 1) % plan.size()) + 1;
        DailyVideo.Row row = plan.get(idx - 1);
        cfg.set("video.scenario", DailyVideo.scenarioFor(row.formato()));
        ScriptParser.Script script;
        try {
            script = ScriptParser.parse(GeminiParser.extractJson(
                    Gemini.generateScript(cfg, row.titulo(), row.valor(), 4,
                            Scenario.fromId(DailyVideo.scenarioFor(row.formato())).label)));
        } catch (Exception e) {
            log.accept("  (historia) IA no respondió: " + e.getMessage() + " -> respaldo.");
            script = Pipeline.example();
        }
        Pipeline.exec(cfg, script, dayDir, s -> { });
        dia.setProperty("archivo", Util.safeName(script.title()) + ".mp4");
        dia.setProperty("desc", DailyVideo.descOf(row));
        dia.setProperty("tags", String.join(",", DailyVideo.tagsOf(row.etiquetas() != null ? row.etiquetas() : "")));
        dia.setProperty("post.canal.videoIndex", String.valueOf((idx % plan.size()) + 1));
        return new String[]{"HISTORIA", row.titulo()};
    }

    private static String[] tecnologiaDia(Config cfg, Path dayDir, Consumer<String> log,
                                          Map<String, Integer> st, Properties dia) throws Exception {
        int tipoN = Math.floorMod(st.get("canal.techIndex") - 1, 4);
        switch (tipoN) {
            case 0 -> {
                int i = st.get("canal.techListicle");
                TechLibrary.Listicle l = TechLibrary.listicleByIndex(i);
                TechVideo.buildListicle(cfg, l, dayDir, s -> { });
                List<String> tags = new ArrayList<>(DailyVideo.tagsOf(l.etiquetas() != null ? l.etiquetas() : ""));
                tags.add("tecnología para niños");
                dia.setProperty("archivo", Util.safeName(l.titulo()) + ".mp4");
                dia.setProperty("desc", l.valor() + ". " + l.intro() + "\n\nTecnología e historia para niños: "
                        + "inventos que cambian el mundo.\n" + l.etiquetas());
                dia.setProperty("tags", String.join(",", tags));
                dia.setProperty("post.canal.techListicle", String.valueOf(i + 1));
                dia.setProperty("post.canal.techIndex", String.valueOf(st.get("canal.techIndex") + 1));
                return new String[]{"TECNO-LISTA", l.titulo()};
            }
            case 1 -> {
                int i = st.get("canal.techTrivia");
                TechLibrary.Trivia t = TechLibrary.triviaByIndex(i);
                TechVideo.buildTrivia(cfg, t, dayDir, s -> { });
                List<String> tags = new ArrayList<>(DailyVideo.tagsOf(t.etiquetas() != null ? t.etiquetas() : ""));
                tags.add("adivinanzas para niños");
                dia.setProperty("archivo", Util.safeName(t.titulo()) + ".mp4");
                dia.setProperty("desc", t.valor() + ". Adivina el invento antes de que aparezca la respuesta.\n\n"
                        + "Tecnología e historia para niños.\n" + t.etiquetas());
                dia.setProperty("tags", String.join(",", tags));
                dia.setProperty("post.canal.techTrivia", String.valueOf(i + 1));
                dia.setProperty("post.canal.techIndex", String.valueOf(st.get("canal.techIndex") + 1));
                return new String[]{"TECNO-TRIVIA", t.titulo()};
            }
            case 2 -> {
                int i = st.get("canal.techRonda");
                Ronda r = TechLibrary.rondaTechByIndex(i);
                cfg.set("video.scenario", r.escenario());
                RondaVideo.build(cfg, r, dayDir, s -> { });
                List<String> tags = new ArrayList<>(DailyVideo.tagsOf(r.etiquetas() != null ? r.etiquetas() : ""));
                tags.add("canciones de tecnología para niños");
                dia.setProperty("archivo", Util.safeName(r.titulo()) + ".mp4");
                dia.setProperty("desc", r.descripcion() + "\n\nRonda de tecnología para cantar y bailar.\n"
                        + "Construyendo Valores: construimos algo más importante que edificios: valores.\n"
                        + r.etiquetas());
                dia.setProperty("tags", String.join(",", tags));
                dia.setProperty("post.canal.techRonda", String.valueOf(i + 1));
                dia.setProperty("post.canal.techIndex", String.valueOf(st.get("canal.techIndex") + 1));
                return new String[]{"TECNO-RONDA", r.titulo()};
            }
            default -> {
                int i = st.get("canal.techHistoria");
                TechLibrary.Historia h = TechLibrary.historiaByIndex(i);
                cfg.set("video.scenario", h.escenario());
                ScriptParser.Script script;
                try {
                    script = ScriptParser.parse(GeminiParser.extractJson(
                            Gemini.generateScript(cfg, h.titulo(), h.valor(), 4,
                                    Scenario.fromId(h.escenario()).label)));
                } catch (Exception e) {
                    log.accept("  (historia tech) IA no respondió: " + e.getMessage() + " -> respaldo.");
                    script = TechLibrary.fallbackHistoria(h);
                }
                Pipeline.exec(cfg, script, dayDir, s -> { });
                List<String> tags = new ArrayList<>(DailyVideo.tagsOf(h.etiquetas() != null ? h.etiquetas() : ""));
                tags.add("historia de la tecnología para niños");
                dia.setProperty("archivo", Util.safeName(script.title()) + ".mp4");
                dia.setProperty("desc", h.valor() + ". Historia de la tecnología para niños.\n\n"
                        + h.datoClave() + "\n" + h.etiquetas());
                dia.setProperty("tags", String.join(",", tags));
                dia.setProperty("post.canal.techHistoria", String.valueOf(i + 1));
                dia.setProperty("post.canal.techIndex", String.valueOf(st.get("canal.techIndex") + 1));
                return new String[]{"TECNO-HISTORIA", script.title()};
            }
        }
    }

    private static String[] seriesDia(Config cfg, Path dayDir, Consumer<String> log,
                                      Map<String, Integer> st, Properties dia) throws Exception {
        int tipoN = Math.floorMod(st.get("canal.serieIndex") - 1, 5);
        switch (tipoN) {
            case 0 -> { // La pregunta de hoy
                int i = st.get("canal.seriePregunta");
                TechLibrary.Trivia t = SerieLibrary.preguntaByIndex(i);
                TechVideo.buildTrivia(cfg, t, dayDir, s -> { });
                dia.setProperty("archivo", Util.safeName(t.titulo()) + ".mp4");
                dia.setProperty("desc", SerieLibrary.preguntaDesc(t));
                dia.setProperty("tags", String.join(",", SerieLibrary.preguntaTags(t)));
                dia.setProperty("post.canal.seriePregunta", String.valueOf(i + 1));
                dia.setProperty("post.canal.serieIndex", String.valueOf(st.get("canal.serieIndex") + 1));
                return new String[]{"SERIE-PREGUNTA", t.titulo()};
            }
            case 1 -> { // Tito en 60 segundos
                int i = st.get("canal.serieTito");
                int cap = st.get("canal.titoCapitulo");
                ScriptParser.Script base = SerieLibrary.microcuentoByIndex(i);
                String cTitle = SerieLibrary.titoTitulo(base, cap);
                ScriptParser.Script script = new ScriptParser.Script(cTitle, base.description(), base.scenes());
                cfg.set("video.scenario", SerieLibrary.scenarioOf(base, "parque"));
                Pipeline.exec(cfg, script, dayDir, s -> { });
                dia.setProperty("archivo", Util.safeName(cTitle) + ".mp4");
                dia.setProperty("desc", SerieLibrary.titoDesc(cTitle, base.title()));
                dia.setProperty("tags", String.join(",", SerieLibrary.titoTags()));
                dia.setProperty("post.canal.serieTito", String.valueOf(i + 1));
                dia.setProperty("post.canal.titoCapitulo", String.valueOf(cap + 1));
                dia.setProperty("post.canal.serieIndex", String.valueOf(st.get("canal.serieIndex") + 1));
                return new String[]{"SERIE-TITO", cTitle};
            }
            case 2 -> { // Inventor secreto
                int i = st.get("canal.serieInventor");
                ScriptParser.Script script = SerieLibrary.inventorByIndex(i);
                cfg.set("video.scenario", SerieLibrary.scenarioOf(script, "taller"));
                Pipeline.exec(cfg, script, dayDir, s -> { });
                dia.setProperty("archivo", Util.safeName(script.title()) + ".mp4");
                dia.setProperty("desc", SerieLibrary.inventorDesc(script));
                dia.setProperty("tags", String.join(",", SerieLibrary.inventorTags()));
                dia.setProperty("post.canal.serieInventor", String.valueOf(i + 1));
                dia.setProperty("post.canal.serieIndex", String.valueOf(st.get("canal.serieIndex") + 1));
                return new String[]{"SERIE-INVENTOR", script.title()};
            }
            case 3 -> { // Canta y aprende
                int i = st.get("canal.serieCanta");
                Ronda r = SerieLibrary.cantonByIndex(i);
                cfg.set("video.scenario", r.escenario());
                RondaVideo.build(cfg, r, dayDir, s -> { });
                dia.setProperty("archivo", Util.safeName(r.titulo()) + ".mp4");
                dia.setProperty("desc", SerieLibrary.cantaDesc(r));
                dia.setProperty("tags", String.join(",", SerieLibrary.cantaTags(r)));
                dia.setProperty("post.canal.serieCanta", String.valueOf(i + 1));
                dia.setProperty("post.canal.serieIndex", String.valueOf(st.get("canal.serieIndex") + 1));
                return new String[]{"SERIE-CANTA", r.titulo()};
            }
            default -> { // Reto en familia
                int i = st.get("canal.serieReto");
                Ronda r = SerieLibrary.retoByIndex(i);
                cfg.set("video.scenario", r.escenario());
                RondaVideo.build(cfg, r, dayDir, s -> { });
                dia.setProperty("archivo", Util.safeName(r.titulo()) + ".mp4");
                dia.setProperty("desc", SerieLibrary.retoDesc(r));
                dia.setProperty("tags", String.join(",", SerieLibrary.retoTags(r)));
                dia.setProperty("post.canal.serieReto", String.valueOf(i + 1));
                dia.setProperty("post.canal.serieIndex", String.valueOf(st.get("canal.serieIndex") + 1));
                return new String[]{"SERIE-RETO", r.titulo()};
            }
        }
    }

    // ------------------------------------------------------------- ayuda

    /** Deja listo el contador base de videoCount en el Properties del día. */
    private static Properties post(Properties dia, Map<String, Integer> st, int counter) {
        dia.setProperty("post.canal.videoCount", String.valueOf(counter + 1));
        return dia;
    }

    private static boolean checkArchivo(Properties dia, String key) {
        if (dia.getProperty(key) == null) {
            throw new IllegalStateException("El día quedó sin archivo (bug del planificador).");
        }
        return true;
    }

    private static int defaultValue(String key) {
        return "canal.videoCount".equals(key) ? 1
                : "canal.videoIndex".equals(key) ? 1
                : "canal.rondaIndex".equals(key) ? 1
                : "canal.techIndex".equals(key) ? 1
                : 1;
    }

    private static int parse(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private SemanaVideo() {}
}