package com.construyendovalores.studio;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;

/**
 * Automatización diaria: alterna una ronda infantil y una historia del plan de
 * 30. Las rondas usan melodía sintetizada (sin IA); las historias generan guion
 * con IA (o respaldo). Todo se renderiza y sube programado para la hora elegida.
 *
 * Estado guardado en config: canal.videoCount (total publicados), canal.videoIndex
 * (rotación de historias), canal.rondaIndex (rotación de rondas) y canal.lastUploadDate.
 */
public final class DailyVideo {

    public record Row(int id, String valor, String formato, String titulo, String descripcion, String etiquetas) {}

    public record Result(boolean alreadyDone, Integer videoNumber, String title, String publishAt, String url,
                         String privacyStatus, String message) {}

    private DailyVideo() {}

    public static Result run(Config cfg, Path outDir, Consumer<String> log) throws Exception {
        // No repetir el video del día.
        String today = LocalDate.now().toString();
        if (today.equals(cfg.get("canal.lastUploadDate", ""))) {
            return new Result(true, 0, null, null, null, null,
                    "Hoy ya se publicó el video del día. Nada que hacer (rotación diaria).");
        }

        Files.createDirectories(outDir);
        if (!Youtube.isLinked()) {
            throw new IllegalStateException("YouTube no está vinculado. Conecta primero (pestaña Automatización "
                    + "o --youtube-auth). Si solo quieres generar el video sin subir, usa '--demo', '--ronda' o la app.");
        }

        // Si --semanario ya dejó el video de hoy en la cola, solo se sube.
        Path cola = queuedForToday(outDir);
        if (cola != null) {
            return subirDesdeCola(cfg, outDir, log, today, cola);
        }

        int vc = parseNum(cfg, "canal.videoCount", 1);
        int turno = Math.floorMod(vc, 4);
        if (turno == 1) {
            return runRonda(cfg, outDir, log, today, vc);
        }
        if (turno == 2) {
            return runHistoria(cfg, outDir, log, today, vc);
        }
        if (turno == 3) {
            return runSerie(cfg, outDir, log, today, vc);
        }
        return runTecnologia(cfg, outDir, log, today, vc);
    }

    private static Result runRonda(Config cfg, Path outDir, Consumer<String> log, String today, int vc) throws Exception {
        int rondaN = RondaLibrary.nextRondaIndex(cfg);
        Ronda r = RondaLibrary.byIdOrIndex(String.valueOf(rondaN));
        cfg.set("video.scenario", r.escenario());
        log.accept("Video del día: RONDA " + rondaN + "/" + RondaLibrary.clasicas().size() + " -> " + r.titulo());
        log.accept("Generando la ronda (melodía sintetizada, sin IA)...");
        RondaVideo.build(cfg, r, outDir, log);

        Path video = outDir.resolve(r.titulo() + ".mp4");
        if (!Files.exists(video)) {
            throw new IllegalStateException("No se pudo localizar el video final: " + video);
        }

        List<String> tags = new ArrayList<>(tagsOf(r.etiquetas() != null ? r.etiquetas() : ""));
        tags.add("Construyendo Valores");
        tags.add("canciones para niños");
        int hour = Integer.parseInt(cfg.get("youtube.publishHour", "9"));
        OffsetDateTime publishAt = nextPublish(hour);
        Youtube.UploadResult up = subirSeguro(cfg, log, video, r.titulo(), rondaDescription(r), tags, "Ronda");
        log.accept("Subido: " + up.url() + " [" + up.privacyStatus() + "]");
        if (!"public".equals(up.privacyStatus()) && !up.scheduled()) {
            log.accept("AVISO: sin el audit gratuito de YouTube los videos quedan privados. "
                    + "Para publicarlos: (A) aplica al audit o (B) publícalos manualmente en YouTube Studio.");
        }

        subirShort(cfg, log, video, r.titulo(), rondaDescription(r), tags, "Ronda");
        TikTokPackage.prepare(cfg, video, r.titulo(), rondaDescription(r), tags, outDir, log);

        int next = (rondaN % RondaLibrary.clasicas().size()) + 1;
        cfg.set("canal.rondaIndex", String.valueOf(next));
        cfg.set("canal.videoCount", String.valueOf(vc + 1));
        cfg.set("canal.lastVideoId", up.videoId());
        cfg.set("canal.lastUploadDate", today);
        cfg.save();
        log.accept("Rotación avanzada a la Ronda " + next + " (para la próxima vez).");

        return new Result(false, rondaN, r.titulo(), publishAt.toString(), up.url(), up.privacyStatus(),
                "Ronda " + rondaN + " subida. Se publicará en " + up.url());
    }

    private static Result runHistoria(Config cfg, Path outDir, Consumer<String> log, String today, int vc) throws Exception {
        List<Row> plan = loadPlan(outDir, log);
        if (plan.isEmpty()) {
            throw new IllegalStateException("No se encontró CONTENIDO_30.csv. "
                    + "Ejecuta primero '--contenido' para generar el plan de 30 videos.");
        }

        int idx = Integer.parseInt(cfg.get("canal.videoIndex", "1"));
        idx = ((idx - 1) % plan.size()) + 1;
        Row row = plan.get(idx - 1);
        log.accept("Video del día: " + idx + "/" + plan.size() + " -> " + row.formato + " | " + row.titulo);

        String scenario = scenarioFor(row.formato);
        cfg.set("video.scenario", scenario);

        ScriptParser.Script script;
        try {
            script = ScriptParser.parse(GeminiParser.extractJson(
                    Gemini.generateScript(cfg, row.titulo, row.valor, 4, Scenario.fromId(scenario).label)));
            log.accept("Guion generado por IA.");
        } catch (Exception e) {
            log.accept("La IA no respondió (" + e.getMessage() + "). Usando guion de respaldo.");
            script = Pipeline.example();
        }

        log.accept("Generando el video...");
        Pipeline.exec(cfg, script, outDir, log);
        Path video = outDir.resolve(script.title() + ".mp4");
        if (!Files.exists(video)) {
            throw new IllegalStateException("No se pudo localizar el video final: " + video);
        }

        List<String> tags = tagsOf(row.etiquetas != null ? row.etiquetas : "");
        int hour = Integer.parseInt(cfg.get("youtube.publishHour", "9"));
        OffsetDateTime publishAt = nextPublish(hour);
        Youtube.UploadResult up = subirSeguro(cfg, log, video, row.titulo, descOf(row), tags, "Historia");
        log.accept("Subido: " + up.url() + " [" + up.privacyStatus() + "]");
        if (!"public".equals(up.privacyStatus()) && !up.scheduled()) {
            log.accept("AVISO: sin el audit gratuito de YouTube los videos quedan privados. "
                    + "Para publicarlos: (A) aplica al audit o (B) publícalos manualmente en YouTube Studio.");
        }

        subirShort(cfg, log, video, row.titulo, descOf(row), tags, "Historia");
        TikTokPackage.prepare(cfg, video, row.titulo, descOf(row), tags, outDir, log);

        int next = (idx % plan.size()) + 1;
        cfg.set("canal.videoIndex", String.valueOf(next));
        cfg.set("canal.videoCount", String.valueOf(vc + 1));
        cfg.set("canal.lastVideoId", up.videoId());
        cfg.set("canal.lastUploadDate", today);
        cfg.save();
        log.accept("Rotación avanzada al Video " + next + " (para mañana).");

        return new Result(false, idx, row.titulo, publishAt.toString(), up.url(), up.privacyStatus(),
                "Video " + idx + " listo. Se publicará en " + up.url());
    }

    private static Result runTecnologia(Config cfg, Path outDir, Consumer<String> log, String today, int vc) throws Exception {
        int tipo = parseNum(cfg, "canal.techIndex", 1);
        int tipoN = Math.floorMod(tipo - 1, 4);

        String titulo;
        Path video;
        String desc;
        List<String> tags;
        Runnable avance = () -> { };

        switch (tipoN) {
            case 0 -> {
                int i = parseNum(cfg, "canal.techListicle", 1);
                TechLibrary.Listicle l = TechLibrary.listicleByIndex(i);
                log.accept("Video del día: TECNOLOGÍA [LISTA 5 DATOS] -> " + l.titulo());
                TechVideo.buildListicle(cfg, l, outDir, log);
                video = outDir.resolve(l.titulo() + ".mp4");
                if (!Files.exists(video)) throw new IllegalStateException("No se localizó el listicle: " + video);
                titulo = l.titulo();
                desc = l.valor() + ". " + l.intro() + "\n\nTecnología e historia para niños: inventos que cambian el mundo.\n"
                        + l.etiquetas();
                tags = tagsOf(l.etiquetas() != null ? l.etiquetas() : "");
                tags.add("tecnología para niños");
                final int fi = i;
                avance = () -> cfg.set("canal.techListicle", String.valueOf(fi + 1));
            }
            case 1 -> {
                int i = parseNum(cfg, "canal.techTrivia", 1);
                TechLibrary.Trivia t = TechLibrary.triviaByIndex(i);
                log.accept("Video del día: TECNOLOGÍA [TRIVIA] -> " + t.titulo());
                TechVideo.buildTrivia(cfg, t, outDir, log);
                video = outDir.resolve(t.titulo() + ".mp4");
                if (!Files.exists(video)) throw new IllegalStateException("No se localizó la trivia: " + video);
                titulo = t.titulo();
                desc = t.valor() + ". Adivina el invento antes de que aparezca la respuesta.\n\n"
                        + "Tecnología e historia para niños.\n" + t.etiquetas();
                tags = tagsOf(t.etiquetas() != null ? t.etiquetas() : "");
                tags.add("adivinanzas para niños");
                final int fi = i;
                avance = () -> cfg.set("canal.techTrivia", String.valueOf(fi + 1));
            }
            case 2 -> {
                int i = parseNum(cfg, "canal.techRonda", 1);
                Ronda r = TechLibrary.rondaTechByIndex(i);
                log.accept("Video del día: TECNOLOGÍA [RONDA] -> " + r.titulo());
                cfg.set("video.scenario", r.escenario());
                RondaVideo.build(cfg, r, outDir, log);
                video = outDir.resolve(r.titulo() + ".mp4");
                if (!Files.exists(video)) throw new IllegalStateException("No se localizó la ronda: " + video);
                titulo = r.titulo();
                desc = r.descripcion() + "\n\n🎶 Ronda de tecnología para cantar y bailar.\n"
                        + "Construyendo Valores: construimos algo más importante que edificios: valores.\n" + r.etiquetas();
                tags = tagsOf(r.etiquetas() != null ? r.etiquetas() : "");
                tags.add("canciones de tecnología para niños");
                final int fi = i;
                avance = () -> cfg.set("canal.techRonda", String.valueOf(fi + 1));
            }
            default -> {
                int i = parseNum(cfg, "canal.techHistoria", 1);
                TechLibrary.Historia h = TechLibrary.historiaByIndex(i);
                log.accept("Video del día: TECNOLOGÍA [HISTORIA] -> " + h.titulo());
                cfg.set("video.scenario", h.escenario());
                ScriptParser.Script script;
                try {
                    script = ScriptParser.parse(GeminiParser.extractJson(
                            Gemini.generateScript(cfg, h.titulo(), h.valor(), 4,
                                    Scenario.fromId(h.escenario()).label)));
                    log.accept("Guion de historia generado por IA.");
                } catch (Exception e) {
                    log.accept("La IA no respondió (" + e.getMessage() + "). Usando historia de respaldo.");
                    script = TechLibrary.fallbackHistoria(h);
                }
                Pipeline.exec(cfg, script, outDir, log);
                video = outDir.resolve(script.title() + ".mp4");
                if (!Files.exists(video)) throw new IllegalStateException("No se localizó la historia: " + video);
                titulo = script.title();
                desc = h.valor() + ". Historia de la tecnología para niños.\n\n"
                        + h.datoClave() + "\n" + h.etiquetas();
                tags = tagsOf(h.etiquetas() != null ? h.etiquetas() : "");
                tags.add("historia de la tecnología para niños");
                final int fi = i;
                avance = () -> cfg.set("canal.techHistoria", String.valueOf(fi + 1));
            }
        }

        avance.run();
        int nextTipo = tipo + 1;
        cfg.set("canal.techIndex", String.valueOf(nextTipo));
        log.accept("Rotación de tecnología: próxima vez formato " + nextTipo + "/4 (1 lista, 2 trivia, 3 ronda, 4 historia).");

        return subirYAvanzar(cfg, log, today, video, titulo, desc, tags, vc, "Tecnología");
    }

    /** Turno 3: series propias del canal (pregunta, Tito, inventor, canta o reto). */
    private static Result runSerie(Config cfg, Path outDir, Consumer<String> log, String today, int vc) throws Exception {
        int tipo = parseNum(cfg, "canal.serieIndex", 1);
        int tipoN = Math.floorMod(tipo - 1, 5);

        String titulo;
        Path video;
        String desc;
        List<String> tags;
        Runnable avance = () -> { };

        switch (tipoN) {
            case 0 -> { // La pregunta de hoy
                int i = parseNum(cfg, "canal.seriePregunta", 1);
                TechLibrary.Trivia t = SerieLibrary.preguntaByIndex(i);
                log.accept("Video del día: SERIE [LA PREGUNTA DE HOY] -> " + t.titulo());
                TechVideo.buildTrivia(cfg, t, outDir, log);
                video = outDir.resolve(t.titulo() + ".mp4");
                if (!Files.exists(video)) throw new IllegalStateException("No se localizó la pregunta: " + video);
                titulo = t.titulo();
                desc = SerieLibrary.preguntaDesc(t);
                tags = SerieLibrary.preguntaTags(t);
                final int fi = i;
                avance = () -> cfg.set("canal.seriePregunta", String.valueOf(fi + 1));
            }
            case 1 -> { // Tito en 60 segundos
                int i = parseNum(cfg, "canal.serieTito", 1);
                int cap = parseNum(cfg, "canal.titoCapitulo", 1);
                ScriptParser.Script base = SerieLibrary.microcuentoByIndex(i);
                String cTitle = SerieLibrary.titoTitulo(base, cap);
                ScriptParser.Script script = new ScriptParser.Script(cTitle, base.description(), base.scenes());
                cfg.set("video.scenario", SerieLibrary.scenarioOf(base, "parque"));
                log.accept("Video del día: SERIE [TITO EN 60 SEGUNDOS] -> " + cTitle);
                Pipeline.exec(cfg, script, outDir, log);
                video = outDir.resolve(cTitle + ".mp4");
                if (!Files.exists(video)) throw new IllegalStateException("No se localizó el mini cuento: " + video);
                titulo = cTitle;
                desc = SerieLibrary.titoDesc(cTitle, base.title());
                tags = SerieLibrary.titoTags();
                final int fi = i, fcap = cap;
                avance = () -> {
                    cfg.set("canal.serieTito", String.valueOf(fi + 1));
                    cfg.set("canal.titoCapitulo", String.valueOf(fcap + 1));
                };
            }
            case 2 -> { // Inventor secreto
                int i = parseNum(cfg, "canal.serieInventor", 1);
                ScriptParser.Script script = SerieLibrary.inventorByIndex(i);
                cfg.set("video.scenario", SerieLibrary.scenarioOf(script, "taller"));
                log.accept("Video del día: SERIE [INVENTOR SECRETO] -> " + script.title());
                Pipeline.exec(cfg, script, outDir, log);
                video = outDir.resolve(script.title() + ".mp4");
                if (!Files.exists(video)) throw new IllegalStateException("No se localizó el inventor: " + video);
                titulo = script.title();
                desc = SerieLibrary.inventorDesc(script);
                tags = SerieLibrary.inventorTags();
                final int fi = i;
                avance = () -> cfg.set("canal.serieInventor", String.valueOf(fi + 1));
            }
            case 3 -> { // Canta y aprende
                int i = parseNum(cfg, "canal.serieCanta", 1);
                Ronda r = SerieLibrary.cantonByIndex(i);
                log.accept("Video del día: SERIE [CANTA Y APRENDE] -> " + r.titulo());
                cfg.set("video.scenario", r.escenario());
                RondaVideo.build(cfg, r, outDir, log);
                video = outDir.resolve(r.titulo() + ".mp4");
                if (!Files.exists(video)) throw new IllegalStateException("No se localizó la canción: " + video);
                titulo = r.titulo();
                desc = SerieLibrary.cantaDesc(r);
                tags = SerieLibrary.cantaTags(r);
                final int fi = i;
                avance = () -> cfg.set("canal.serieCanta", String.valueOf(fi + 1));
            }
            default -> { // Reto en familia
                int i = parseNum(cfg, "canal.serieReto", 1);
                Ronda r = SerieLibrary.retoByIndex(i);
                log.accept("Video del día: SERIE [RETO EN FAMILIA] -> " + r.titulo());
                cfg.set("video.scenario", r.escenario());
                RondaVideo.build(cfg, r, outDir, log);
                video = outDir.resolve(r.titulo() + ".mp4");
                if (!Files.exists(video)) throw new IllegalStateException("No se localizó el reto: " + video);
                titulo = r.titulo();
                desc = SerieLibrary.retoDesc(r);
                tags = SerieLibrary.retoTags(r);
                final int fi = i;
                avance = () -> cfg.set("canal.serieReto", String.valueOf(fi + 1));
            }
        }

        avance.run();
        int nextTipo = tipo + 1;
        cfg.set("canal.serieIndex", String.valueOf(nextTipo));
        log.accept("Rotación de series: próxima vez formato " + nextTipo + "/5 "
                + "(1 pregunta, 2 Tito, 3 inventor, 4 canta, 5 reto).");

        return subirYAvanzar(cfg, log, today, video, titulo, desc, tags, vc, "Serie");
    }

    private static Result subirYAvanzar(Config cfg, Consumer<String> log, String today, Path video, String titulo,
                                        String desc, List<String> tags, int vc, String tipo) throws Exception {
        int hour = Integer.parseInt(cfg.get("youtube.publishHour", "9"));
        OffsetDateTime publishAt = nextPublish(hour);
        Youtube.UploadResult up = subirSeguro(cfg, log, video, titulo, desc, tags, tipo);
        log.accept("Subido: " + up.url() + " [" + up.privacyStatus() + "]");
        if (!"public".equals(up.privacyStatus()) && !up.scheduled()) {
            log.accept("AVISO: sin el audit gratuito de YouTube los videos quedan privados. "
                    + "Para publicarlos: (A) aplica al audit o (B) publícalos manualmente en YouTube Studio.");
        }
        subirShort(cfg, log, video, titulo, desc, tags, tipo);
        TikTokPackage.prepare(cfg, video, titulo, desc, tags, video.getParent(), log);
        cfg.set("canal.videoCount", String.valueOf(vc + 1));
        cfg.set("canal.lastVideoId", up.videoId());
        cfg.set("canal.lastUploadDate", today);
        cfg.save();
        return new Result(false, 0, titulo, publishAt.toString(), up.url(), up.privacyStatus(),
                tipo + " listo. Se publicará en " + up.url());
    }

    /** ¿Existe un video programado por --semanario para hoy? */
    private static Path queuedForToday(Path outDir) {
        String today = LocalDate.now().toString();
        Path dayDir = outDir.resolve("Programadas").resolve(today);
        if (Files.isDirectory(dayDir)) {
            Path p = dayDir.resolve("dia.properties");
            if (Files.exists(p)) return dayDir;
        }
        return null;
    }

    /** Sube el video de la cola de --semanario y aplica la rotación ya calculada. */
    private static Result subirDesdeCola(Config cfg, Path outDir, Consumer<String> log, String today, Path dayDir)
            throws Exception {
        Properties p = new Properties();
        p.load(Files.newBufferedReader(dayDir.resolve("dia.properties"), StandardCharsets.UTF_8));
        String archivo = p.getProperty("archivo", "");
        Path video = dayDir.resolve(archivo);
        if (!Files.exists(video)) {
            throw new IllegalStateException("El video programado no existe: " + video
                    + ". Rerenderiza con --semanario o elimina la carpeta del día.");
        }
        String titulo = p.getProperty("titulo", archivo);
        String desc = p.getProperty("desc", "");
        String tipo = p.getProperty("tipo", "?");
        List<String> tags = tagsOf(p.getProperty("tags", ""));
        log.accept("Video programado de la cola: " + titulo + " (" + tipo + ")");

        Youtube.UploadResult up = subirSeguro(cfg, log, video, titulo, desc, tags, tipo);
        log.accept("Subido desde cola: " + up.url() + " [" + up.privacyStatus() + "]");
        if (!"public".equals(up.privacyStatus()) && !up.scheduled()) {
            log.accept("AVISO: sin el audit gratuito de YouTube los videos quedan privados. "
                    + "Para publicarlos: (A) aplica al audit o (B) publícalos manualmente en YouTube Studio.");
        }

        subirShort(cfg, log, video, titulo, desc, tags, tipo);
        TikTokPackage.prepare(cfg, video, titulo, desc, tags, outDir, log);

        for (String key : p.stringPropertyNames()) {
            if (key.startsWith("post.")) {
                cfg.set(key.substring("post.".length()), p.getProperty(key));
            }
        }
        cfg.set("canal.lastVideoId", up.videoId());
        cfg.set("canal.lastUploadDate", today);
        cfg.save();

        int hour = Integer.parseInt(cfg.get("youtube.publishHour", "9"));
        OffsetDateTime publishAt = nextPublish(hour);
        return new Result(false, 0, titulo, publishAt.toString(), up.url(), up.privacyStatus(),
                tipo + " subido desde la cola. Se publicará en " + up.url());
    }

    /** Genera el Short (30-45 s con gancho y CTA) del video del día y lo sube a YouTube como Short. */
    private static String subirShort(Config cfg, Consumer<String> log, Path video, String titulo,
                                     String desc, List<String> tags, String tipo) throws Exception {
        Path shortVideo = Shorts.make(cfg, video, video.getParent(), 38, log);
        List<String> shortTags = new ArrayList<>(tags);
        shortTags.add("Shorts");
        shortTags.add("shorts para niños");
        String shortTitle = titulo + " #Shorts";
        String shortDesc = desc + "\n\n▶ Mira el video completo en el canal y suscríbete para más #Shorts.";
        Youtube.UploadResult up = subirSeguro(cfg, log, shortVideo, shortTitle, shortDesc, shortTags, tipo + " Short");
        log.accept("Short subido: " + up.url() + " [" + up.privacyStatus() + "]");
        if (!"public".equals(up.privacyStatus()) && !up.scheduled()) {
            log.accept("AVISO: sin el audit gratuito de YouTube el Short también queda privado.");
        }
        return up.url();
    }

    /** Control de calidad + subida con reintentos + registro de métricas. */
    private static Youtube.UploadResult subirSeguro(Config cfg, Consumer<String> log, Path video,
                                                    String titulo, String desc, List<String> tags, String tipo)
            throws Exception {
        VideoCheck.Result q = VideoCheck.check(cfg, video, null);
        if (!q.errores().isEmpty()) {
            log.accept("CONTROL DE CALIDAD: el video NO cumple los mínimos. No se subirá.");
            q.errores().forEach(e -> log.accept("  ERROR: " + e));
            Metricas.registrar(cfg, LocalDate.now().toString(), tipo, titulo, video.toString(),
                    "FALLO_CHECK", "", "", log);
            throw new IllegalStateException("Video reprobado (" + video.getFileName() + "): "
                    + String.join("; ", q.errores()));
        }
        q.avisos().forEach(w -> log.accept("  AVISO: " + w));

        double umbral = cfg.getDouble("calidad.umbral", 0);
        if (umbral > 0) {
            try {
                IndiceCalidad.Resultado iq = IndiceCalidad.evaluar(cfg, video, log);
                if (iq.indice() < umbral) {
                    log.accept("PORTÓN DE CALIDAD: índice " + String.format(java.util.Locale.ROOT, "%.1f", iq.indice())
                            + " < umbral " + umbral + ". No se sube hoy.");
                    Metricas.registrar(cfg, LocalDate.now().toString(), tipo, titulo, video.toString(),
                            "FALLO_UMBRAL", "", "", log);
                    throw new IllegalStateException("Video bajo el umbral de calidad (" + video.getFileName() + ").");
                }
                log.accept("  Índice de calidad: " + String.format(java.util.Locale.ROOT, "%.0f", iq.indice())
                        + "/100 (umbral " + umbral + ")");
            } catch (IllegalStateException e) {
                throw e;
            } catch (Exception e) {
                log.accept("  AVISO: no se pudo medir el índice de calidad (" + e.getMessage().split("\\n")[0]
                        + "); se sube con el control técnico únicamente.");
            }
        }

        int hour = Integer.parseInt(cfg.get("youtube.publishHour", "9"));
        OffsetDateTime publishAt = nextPublish(hour);
        log.accept("Subiendo a YouTube (se publicará a las " + hour + ":00)...");
        Youtube.UploadResult up = Youtube.upload(cfg, video, titulo, desc, tags, publishAt,
                cfg.get("youtube.privacyStatus", "public"), log);
        Metricas.registrar(cfg, LocalDate.now().toString(), tipo, titulo, video.toString(),
                "SUBIDO", up.videoId(), up.url(), log);
        return up;
    }

    static int parseNum(Config cfg, String key, int def) {
        try {
            return Integer.parseInt(cfg.get(key, String.valueOf(def)));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public static OffsetDateTime nextPublish(int hour) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime target = now.withHour(hour).withMinute(0).withSecond(0).withNano(0);
        if (!target.isAfter(now)) {
            target = target.plusDays(1);
        }
        return target.atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }

    static String scenarioFor(String formato) {
        String f = formato == null ? "" : formato.toLowerCase().replace("á", "a").replace("í", "i");
        if (f.contains("reto")) return Scenario.PARQUE.id;
        if (f.contains("habito") || f.contains("hábito")) return Scenario.TALLER.id;
        return Scenario.OBRA.id;
    }

    static List<String> tagsOf(String etiquetas) {
        List<String> out = new ArrayList<>();
        if (etiquetas == null) return out;
        for (String t : etiquetas.split("[,\\s]+")) {
            String clean = t.replace("#", "").trim();
            if (!clean.isBlank() && clean.length() > 2) out.add(clean);
        }
        return out;
    }

    static String descOf(Row row) {
        String d = row.descripcion;
        if (d == null || d.isBlank()) d = row.titulo;
        return d + "\n\nConstruyendo Valores: construimos algo más importante que edificios: valores.\n"
                + "Suscríbete y activa la campanita 🔔\n" + (row.etiquetas == null ? "" : row.etiquetas);
    }

    static String rondaDescription(Ronda r) {
        String d = r.descripcion() == null || r.descripcion().isBlank() ? r.titulo() : r.descripcion();
        return d + "\n\n🎶 Ronda infantil clásica para cantar, saltar y bailar.\n"
                + "Construyendo Valores: construimos algo más importante que edificios: valores.\n"
                + "Suscríbete y activa la campanita 🔔\n" + r.etiquetas();
    }

    static List<Row> loadPlan(Path outDir, Consumer<String> log) throws Exception {
        List<Path> candidates = new ArrayList<>();
        Path inOut = outDir.resolve("CONTENIDO_30.csv");
        if (Files.exists(inOut)) candidates.add(inOut);
        Path project = Paths.get(System.getProperty("user.dir"), "videos_muestra")
                .resolve("CONTENIDO_30.csv");
        if (Files.exists(project)) candidates.add(project);
        Path home = Paths.get(System.getProperty("user.home"))
                .resolve(".construyendo-valores").resolve("CONTENIDO_30.csv");
        if (Files.exists(home)) candidates.add(home);

        List<Row> rows = new ArrayList<>();
        for (Path p : candidates) {
            rows = parseCsv(p);
            if (!rows.isEmpty()) {
                return rows;
            }
        }
        log.accept("Buscando el plan de 30 en: " + candidates);
        return rows;
    }

    static List<Row> parseCsv(Path p) throws Exception {
        List<Row> rows = new ArrayList<>();
        List<String> lines = Files.readAllLines(p, StandardCharsets.UTF_8);
        for (int i = 1; i < lines.size(); i++) {
            List<String> f = splitCsv(lines.get(i));
            if (f.size() >= 6) {
                try {
                    rows.add(new Row(Integer.parseInt(f.get(0)), f.get(1), f.get(2),
                            f.get(3), f.get(4), f.get(5)));
                } catch (NumberFormatException ignore) {
                    // fila malformada, se salta
                }
            }
        }
        return rows;
    }

    static List<String> splitCsv(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQ = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQ) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        cur.append('"');
                        i++;
                    } else {
                        inQ = false;
                    }
                } else {
                    cur.append(c);
                }
            } else if (c == '"') {
                inQ = true;
            } else if (c == ';') {
                out.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        out.add(cur.toString());
        return out;
    }
}