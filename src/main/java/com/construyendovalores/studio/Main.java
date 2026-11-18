package com.construyendovalores.studio;

import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;

public final class Main {

    public static void main(String[] args) {
        String cmd = args.length > 0 ? args[0] : "";
        try {
            Config cfg = new Config();
            switch (cmd) {
                case "--test-gemini" -> {
                    String raw = Gemini.generateScript(cfg, "Compartir los juguetes", "Generosidad", 4);
                    System.out.println(GeminiParser.extractJson(raw));
                    return;
                }
                case "--auto" -> {
                    String dir = args[1];
                    String tema = args[2];
                    String valor = args[3];
                    if (args.length >= 5) cfg.set("video.scenario", args[4]);
                    String raw = Gemini.generateScript(cfg, tema, valor, 6);
                    String guionJson = GeminiParser.extractJson(raw);
                    ScriptParser.Script s = ScriptParser.parse(guionJson);
                    Path out = Paths.get(dir);
                    Pipeline.exec(cfg, s, out, System.out::println);
                    Files.writeString(out.resolve("guion.json"), guionJson);
                    if ("true".equalsIgnoreCase(cfg.get("calidad.auto", "true"))) {
                        CalidadTotal.evaluar(cfg, out, null, System.out::println);
                    }
                    System.out.println("OK | " + out.toAbsolutePath());
                    return;
                }
                case "--recuento" -> {
                    String cuento = args[1];
                    String dir = args[2];
                    String valor = args.length >= 4 ? args[3] : Recuento.valorDe(cuento);
                    String raw = Gemini.generateRetelling(cfg, cuento, valor, 5);
                    String guionJson = GeminiParser.extractJson(raw);
                    ScriptParser.Script s = ScriptParser.parse(guionJson);
                    Path out = Paths.get(dir);
                    Pipeline.exec(cfg, s, out, System.out::println);
                    Files.writeString(out.resolve("guion.json"), guionJson);
                    if ("true".equalsIgnoreCase(cfg.get("calidad.auto", "true"))) {
                        CalidadTotal.evaluar(cfg, out, null, System.out::println);
                    }
                    System.out.println("OK | " + out.toAbsolutePath());
                    return;
                }
                case "--demo" -> {
                    Path out = Paths.get(args[1]);
                    Pipeline.exec(cfg, Pipeline.example(), out, System.out::println);
                    System.out.println("OK | " + out.toAbsolutePath());
                    return;
                }
                case "--contenido" -> {
                    Path out = Paths.get(args[1]);
                    Path md = ContentGenerator.generate(cfg, out, System.out::println);
                    System.out.println("CONTENIDO | " + md.toAbsolutePath());
                    return;
                }
                case "--art" -> {
                    Path out = Paths.get(args[1]);
                    ChannelArt.makeAvatar(out);
                    ChannelArt.makeBanner(out, null);
                    System.out.println("ARTE | " + out.toAbsolutePath());
                    return;
                }
                case "--youtube-auth" -> {
                    Youtube.authorize(cfg, System.out::println);
                    System.out.println("OK | Canal: " + Youtube.channelName(cfg));
                    return;
                }
                case "--youtube-test" -> {
                    System.out.println("Canal conectado: " + Youtube.channelName(cfg));
                    return;
                }
                case "--youtube-canal" -> {
                    System.out.println("Canal autorizado: " + Youtube.channelUrl(cfg));
                    return;
                }
                case "--youtube-publicar-todos" -> {
                    List<Youtube.Estadisticas> vids = Youtube.listEstadisticas(cfg, 50);
                    if (vids.isEmpty()) {
                        System.out.println("PUBLICAR-TODOS | no hay videos subidos.");
                        return;
                    }
                    int ok = 0, fail = 0;
                    for (Youtube.Estadisticas e : vids) {
                        if ("public".equals(e.estado())) {
                            System.out.println("Ya público | " + e.titulo() + " | " + e.id());
                            continue;
                        }
                        try {
                            String s = Youtube.setPublic(cfg, e.id());
                            System.out.println("HECHO | " + e.titulo() + " -> " + s + " | https://youtu.be/" + e.id());
                            ok++;
                        } catch (Exception ex) {
                            System.out.println("FALLO | " + e.titulo() + ": " + ex.getMessage().split("\\n")[0]);
                            fail++;
                        }
                    }
                    System.out.println("PUBLICAR-TODOS | hechos=" + ok + " fallo=" + fail
                            + (fail > 0 ? " (¿falta el audit de YouTube?)" : ""));
                    return;
                }
                case "--youtube-publicar" -> {
                    String id = args[1].replace("https://youtu.be/", "").replace("https://www.youtube.com/watch?v=", "")
                            .split("[?&#]")[0].trim();
                    String applied = Youtube.setPublic(cfg, id);
                    System.out.println("PUBLICADO | " + id + " -> " + applied);
                    return;
                }
                case "--youtube-ultimos" -> {
                    for (String v : Youtube.listMine(cfg, 10)) {
                        System.out.println(v);
                    }
                    return;
                }
                case "--youtube-privacidad" -> {
                    String p = args[1].toLowerCase();
                    if (!p.equals("public") && !p.equals("private") && !p.equals("unlisted")) {
                        System.out.println("Usa: public, private o unlisted");
                        return;
                    }
                    cfg.set("youtube.privacyStatus", p);
                    cfg.save();
                    System.out.println("Privacidad de las proximas subidas: " + p);
                    return;
                }
                case "--retitular" -> {
                    boolean aplicar = false;
                    boolean catalogo = false;
                    for (int i = 1; i < args.length; i++) {
                        if (args[i].equalsIgnoreCase("--aplicar")) aplicar = true;
                        if (args[i].equalsIgnoreCase("--catalogo")) catalogo = true;
                    }
                    if (!Youtube.isLinked()) {
                        System.out.println("Primero vincula el canal con --youtube-auth.");
                        return;
                    }
                    java.util.Map<String, Integer> repetidos = new java.util.HashMap<>();
                    int cambios = 0;
                    for (String linea : Youtube.listMine(cfg, 50)) {
                        String[] p = linea.split(" \\| ", 3);
                        if (p.length < 3) continue;
                        String id = p[0];
                        String titulo = p[2];
                        String nuevo = tituloMejorado(titulo);
                        boolean cambiaTitulo = !nuevo.equals(titulo);
                        if (!cambiaTitulo && !catalogo) continue;
                        int rep = repetidos.merge(nuevo, 1, Integer::sum);
                        if (rep > 1) nuevo = nuevo.replace(" #Shorts", " | Parte " + rep + " #Shorts");
                        System.out.println(id + " | " + titulo);
                        System.out.println("   -> " + nuevo + (cambiaTitulo ? "" : "   (solo categoría y etiquetas)"));
                        if (aplicar) {
                            try {
                                Youtube.setMetadata(cfg, id, nuevo, etiquetasTitulo(nuevo),
                                        cfg.get("youtube.categoryId", "27"));
                                cambios++;
                                Thread.sleep(400);
                            } catch (Exception ex) {
                                System.out.println("   FALLO: " + ex.getMessage().split("\\n")[0]);
                            }
                        }
                    }
                    System.out.println(aplicar
                            ? "Listo. " + cambios + " videos actualizados."
                            : "Vista previa. Para aplicar los cambios: --retitular --aplicar");
                    if (!aplicar) {
                        System.out.println("Para arreglar también categoría (27) y etiquetas de todo el catálogo: "
                                + "--retitular --catalogo --aplicar");
                    }
                    return;
                }
                case "--estado" -> {
                    int max = 5;
                    if (args.length > 1) {
                        try {
                            max = Math.max(1, Integer.parseInt(args[1]));
                        } catch (NumberFormatException ignored) {
                            // usa el valor por defecto
                        }
                    }
                    String hoy = java.time.LocalDate.now().toString();
                    Path cola = Paths.get("C:\\Users\\Davia\\ConstruyendoValores\\videos_diarios");
                    System.out.println("=== ESTADO DEL CANAL · " + java.time.LocalDateTime.now().withNano(0) + " ===");
                    System.out.println("YouTube vinculado: " + (Youtube.isLinked() ? "si" : "NO (ejecuta --youtube-auth)"));
                    System.out.println("Ultima subida (config): " + cfg.get("canal.lastUploadDate", "?")
                            + " | videos=" + cfg.get("canal.videoCount", "?")
                            + " | ultimo id=" + cfg.get("canal.lastVideoId", "?"));
                    System.out.println("Privacidad por defecto: " + cfg.get("youtube.privacyStatus", "public"));
                    System.out.println("Motor: TTS=" + cfg.get("tts.engine", "edge")
                            + " | imagen=" + cfg.get("pollinations.model", "?")
                            + " | cine=" + cfg.get("cine.habilitado", "true"));
                    Path hoyDir = cola.resolve("Programadas").resolve(hoy);
                    System.out.println("Cola de hoy: "
                            + (Files.exists(hoyDir.resolve("dia.properties")) ? "SI (" + hoyDir + ")" : "no"));
                    try (var dias = Files.list(cola.resolve("Programadas"))) {
                        long pend = dias.filter(Files::isDirectory)
                                .filter(d -> d.getFileName().toString().compareTo(hoy) >= 0)
                                .count();
                        System.out.println("Dias programados por delante: " + pend);
                    } catch (Exception ignored) {
                        // aun no existe la carpeta de programadas
                    }
                    if (!Youtube.isLinked()) {
                        System.out.println("(Sin vinculo no puedo consultar estadisticas en linea.)");
                        return;
                    }
                    try {
                        System.out.println("Canal: " + Youtube.channelName(cfg) + " | " + Youtube.channelUrl(cfg));
                        System.out.println("--- Ultimos videos ---");
                        for (Youtube.Estadisticas e : Youtube.listEstadisticas(cfg, max)) {
                            System.out.println(String.format(Locale.ROOT,
                                    "%s | %s | %s | %d vistas | %d likes | %d comentarios | %s",
                                    e.fecha(), e.estado(), e.titulo(), e.vistas(), e.meGusta(),
                                    e.comentarios(), e.id()));
                        }
                    } catch (Exception ex) {
                        System.out.println("No pude leer YouTube: " + ex.getMessage().split("\\n")[0]);
                    }
                    return;
                }
                case "--borrar" -> {
                    if (!Youtube.isLinked()) {
                        System.out.println("No vinculado: ejecuta --youtube-auth primero.");
                        return;
                    }
                    Youtube.delete(cfg, args[1]);
                    System.out.println("BORRADO | " + args[1]);
                    return;
                }
                case "--subir" -> {
                    Path v = Paths.get(args[1]);
                    if (!Files.exists(v)) {
                        System.out.println("No existe el video: " + v);
                        return;
                    }
                    String titulo = args[2];
                    String desc = args[3];
                    String priv = args.length > 4 ? args[4].toLowerCase() : "public";
                    if (!priv.equals("public") && !priv.equals("private") && !priv.equals("unlisted")) {
                        System.out.println("Usa privacidad: public, private o unlisted");
                        return;
                    }
                    List<String> tags = List.of();
                    Path tagsPath = v.getParent().resolve("etiquetas.txt");
                    if (Files.exists(tagsPath)) {
                        tags = Files.readAllLines(tagsPath).stream().map(String::trim)
                                .filter(s -> !s.isEmpty()).toList();
                    }
                    if (!Youtube.isLinked()) {
                        System.out.println("No vinculado: ejecuta --youtube-auth primero.");
                        return;
                    }
                    System.out.println("SUBIENDO | " + v.getFileName() + " | " + titulo + " | " + priv);
                    Youtube.UploadResult r = Youtube.upload(cfg, v, titulo, desc, tags, null, priv,
                            s -> System.out.println("   " + s));
                    System.out.println("SUBIDO | " + r.url() + " | privacidad=" + r.privacyStatus());
                    return;
                }
                case "--youtube-preview" -> {
                    Path v = Paths.get(args[1]);
                    if (!Files.exists(v)) {
                        System.out.println("No existe el video: " + v);
                        return;
                    }
                    String t = args.length > 2 ? args[2] : "Demo API Construyendo Valores";
                    String d = args.length > 3 ? args[3]
                            : "Caso de uso del audit: subida de un video mediante la YouTube Data API v3 (OAuth 2.0, resumable).";
                    String priv = args.length > 4 ? args[4].toLowerCase() : "private";
                    if (!priv.equals("public") && !priv.equals("private") && !priv.equals("unlisted")) {
                        System.out.println("Usa privacidad: public, private o unlisted");
                        return;
                    }
                    System.out.println("PASO 1/6  Verificando vinculo OAuth 2.0 ...");
                    if (!Youtube.isLinked()) {
                        System.out.println("   No vinculado todavia: ejecuta --youtube-auth primero.");
                        return;
                    }
                    System.out.println("   Canal: " + Youtube.channelName(cfg));
                    System.out.println("   URL:   " + Youtube.channelUrl(cfg));
                    System.out.println("   Archivo: " + v + " (" + String.format("%,d bytes", Files.size(v)) + ")");
                    System.out.println("PASO 2/6  Solicitando token de acceso ...");
                    String at = Youtube.accessToken(cfg);
                    System.out.println("   Token OK (" + (at.startsWith("ya29.") ? "ya29..." : "ok") + ")");
                    System.out.println("PASO 3/6  Iniciando subida resumable ...");
                    System.out.println("PASO 4/6  Transfiriendo el video (multipart upload) ...");
                    Youtube.UploadResult r = Youtube.upload(cfg, v, t, d,
                            List.of("construyendo valores", "canciones infantiles", "música para niños"), null, priv,
                            s -> System.out.println("   " + s));
                    System.out.println("PASO 5/6  Video en la cuenta:");
                    for (String vv : Youtube.listMine(cfg, 5)) {
                        System.out.println("   " + vv);
                    }
                    System.out.println("PASO 6/6  Resultado final:");
                    System.out.println("   ID  : " + r.videoId());
                    System.out.println("   URL : " + r.url());
                    System.out.println("   Privacidad: " + r.privacyStatus() + (r.scheduled() ? " (programado)" : ""));
                    return;
                }
                case "--diario" -> {
                    Path out = Paths.get(args[1]);
                    if (args.length >= 3) cfg.set("youtube.publishHour", args[2]);
                    DailyVideo.Result r = DailyVideo.run(cfg, out, System.out::println);
                    System.out.println("DIARIO | " + (r.message() == null ? "ok" : r.message()));
                    return;
                }
                case "--rondas" -> {
                    Path out = Paths.get(args[1]);
                    Path csv = RondaLibrary.writePlan(out);
                    for (Ronda r : RondaLibrary.clasicas()) {
                        System.out.println(" - [" + r.id() + "] " + r.titulo() + " (" + r.escenario() + ")");
                    }
                    System.out.println("RONDAS | " + csv.toAbsolutePath());
                    return;
                }
                case "--shorts" -> {
                    Path dir = Paths.get(args[1]).toAbsolutePath();
                    double dur = args.length >= 3 ? Double.parseDouble(args[2]) : 38;
                    try (var stream = Files.list(dir)) {
                        for (Path v : stream.filter(f -> f.toString().toLowerCase().endsWith(".mp4"))
                                .filter(f -> !f.getFileName().toString().toLowerCase().contains("short"))
                                .filter(f -> !f.getFileName().toString().toLowerCase().contains("merged"))
                                .sorted().toList()) {
                            Path s = Shorts.make(cfg, v, dir, dur, System.out::println);
                            System.out.println("SHORT | " + s);
                        }
                    }
                    return;
                }
                case "--ronda" -> {
                    Path out = Paths.get(args[2]);
                    Ronda r = RondaLibrary.byIdOrIndex(args[1]);
                    RondaVideo.build(cfg, r, out, System.out::println);
                    System.out.println("OK | " + out.toAbsolutePath());
                    return;
                }
                case "--ronda-tech" -> {
                    Path out = Paths.get(args[2]);
                    Ronda r = TechLibrary.rondaTechByIdOrIndex(args[1]);
                    RondaVideo.build(cfg, r, out, System.out::println);
                    System.out.println("OK | " + out.toAbsolutePath());
                    return;
                }
                case "--tecnologia" -> {
                    Path out = Paths.get(args[1]);
                    Path md = TechLibrary.writePlan(out);
                    for (Ronda r : TechLibrary.rondasTech()) {
                        System.out.println(" - RONDA  [" + r.id() + "] " + r.titulo());
                    }
                    for (TechLibrary.Listicle l : TechLibrary.listicles()) {
                        System.out.println(" - LISTA  [" + l.id() + "] " + l.titulo());
                    }
                    for (TechLibrary.Trivia t : TechLibrary.trivias()) {
                        System.out.println(" - TRIVIA  [" + t.id() + "] " + t.titulo());
                    }
                    for (TechLibrary.Historia h : TechLibrary.historias()) {
                        System.out.println(" - HISTORIA [" + h.id() + "] " + h.titulo());
                    }
                    System.out.println("TECNOLOGIA | " + md.toAbsolutePath());
                    return;
                }
                case "--listicle" -> {
                    Path out = Paths.get(args[2]);
                    TechLibrary.Listicle l = TechLibrary.listicleByIdOrIndex(args[1]);
                    TechVideo.buildListicle(cfg, l, out, System.out::println);
                    System.out.println("OK | " + out.resolve(l.titulo().replaceAll("[\\\\/:*?\"<>|]", "_") + ".mp4").toAbsolutePath());
                    return;
                }
                case "--trivia" -> {
                    Path out = Paths.get(args[2]);
                    TechLibrary.Trivia t = TechLibrary.triviaByIdOrIndex(args[1]);
                    TechVideo.buildTrivia(cfg, t, out, System.out::println);
                    System.out.println("OK | " + out.resolve(t.titulo().replaceAll("[\\\\/:*?\"<>|]", "_") + ".mp4").toAbsolutePath());
                    return;
                }
                case "--guion-tech" -> {
                    TechLibrary.Historia h = TechLibrary.historiaByIdOrIndex(args[1]);
                    cfg.set("video.scenario", h.escenario());
                    String ctx = "Historia de tecnologia para ninos. Personajes: Casquito (maestro, amable), "
                            + "Tito (curioso e impaciente), Sofia (organizada), Nico (entusiasta), "
                            + "Luna (conejita magica y asombrada), Pipo (perrito bromista que hace reir), "
                            + "Tico (tortuga sabia que cuenta cuentos). "
                            + "Narracion corta, una idea por escena, lenguaje sencillo, final con el valor ";
                    String raw = Gemini.generateScript(cfg, h.titulo(), h.valor() + " (" + ctx + ")", 5,
                            Scenario.fromId(h.escenario()).label);
                    String json = GeminiParser.extractJson(raw);
                    ScriptParser.Script s = ScriptParser.parse(json);
                    System.out.println("TITULO: " + s.title());
                    System.out.println("DESCRIPCION: " + s.description());
                    System.out.println("ESCENAS (" + s.scenes().size() + "):");
                    for (Scene sc : s.scenes()) {
                        System.out.println("  [" + sc.index + "] " + sc.character + " -> " + sc.narration);
                    }
                    System.out.println("JSON | " + json);
                    return;
                }
                case "--historia-tech" -> {
                    Path out = Paths.get(args[2]);
                    TechLibrary.Historia h = TechLibrary.historiaByIdOrIndex(args[1]);
                    cfg.set("video.scenario", h.escenario());
                    ScriptParser.Script s;
                    try {
                        s = ScriptParser.parse(GeminiParser.extractJson(
                                Gemini.generateScript(cfg, h.titulo(), h.valor(), 4,
                                        Scenario.fromId(h.escenario()).label)));
                        System.out.println("Guion generado por IA.");
                    } catch (Exception e) {
                        System.out.println("IA no disponible (" + e.getMessage() + "). Usando respaldo.");
                        s = TechLibrary.fallbackHistoria(h);
                    }
                    Pipeline.exec(cfg, s, out, System.out::println);
                    System.out.println("OK | " + out.resolve(s.title() + ".mp4").toAbsolutePath());
                    return;
                }
                case "--tiktok-auth" -> {
                    TikTokPoster.authorize(cfg, System.out::println);
                    System.out.println("OK | TikTok conectado");
                    return;
                }
                case "--tiktok-auto" -> {
                    boolean on = "true".equalsIgnoreCase(args[1]);
                    cfg.set("tiktok.auto", String.valueOf(on));
                    cfg.save();
                    System.out.println("Publicación automática en TikTok: " + (on ? "ACTIVADA (además de la ficha)."
                            + " Requiere app aprobada y --tiktok-auth." : "desactivada (solo ficha lista)."));
                    return;
                }
                case "--tiktok-publicar" -> {
                    Path v = Paths.get(args[1]);
                    String tit = v.getFileName().toString().replace(".mp4", "").replace('_', ' ');
                    TikTokPoster.postVideo(cfg, v, tit, "Video nuevo de Construyendo Valores.", System.out::println);
                    System.out.println("TIKTOK | enviado a publicar (revisa la cuenta en TikTok)");
                    return;
                }
                case "--tiktok-cuenta" -> {
                    String h = args[1].replace("@", "").trim();
                    if (h.isBlank()) {
                        System.out.println("Cuenta actual: @" + cfg.get("tiktok.handle", "historiasrea28"));
                        return;
                    }
                    cfg.set("tiktok.handle", h);
                    cfg.save();
                    System.out.println("Cuenta TikTok establecida: @" + h);
                    return;
                }
                case "--tiktok" -> {
                    Path v = Paths.get(args[1]);
                    Path dir = Paths.get(args[2]);
                    String tit = v.getFileName().toString().replace(".mp4", "").replace('_', ' ');
                    TikTokPackage.prepare(cfg, v, tit, "", java.util.List.of(), dir, System.out::println);
                    System.out.println("TIKTOK | preparado (revisa " + dir.resolve("ParaTikTok").toAbsolutePath() + ")");
                    return;
                }
                case "--semanario" -> {
                    Path out = Paths.get(args[1]);
                    int dias = args.length >= 3 ? Integer.parseInt(args[2]) : 7;
                    SemanaVideo.generar(cfg, out, dias, System.out::println);
                    return;
                }
                case "--cuento" -> {
                    Path out = Paths.get(args[1]);
                    int idx = args.length >= 3 ? Integer.parseInt(args[2]) : 1;
                    ScriptParser.Script s = SerieLibrary.cuentoByIndex(idx);
                    cfg.set("video.scenario", SerieLibrary.scenarioOf(s, "obra"));
                    Pipeline.exec(cfg, s, out, System.out::println);
                    Cover.makeVariants(s.title(), "CONSTRUYENDO VALORES", out);
                    System.out.println("CUENTO | " + out.resolve(s.title().replaceAll("[\\\\/:*?\"<>|]", "_") + ".mp4")
                            + " | miniaturas: 3 variantes en " + out);
                    return;
                }
                case "--stills" -> {
                    Path out = Paths.get(args[1]);
                    Files.createDirectories(out);
                    String[] chars = {"Casquito", "Tito", "Sofía", "Nico", "Luna", "Pipo", "Tico", "Bruno", "Memo", "Coco"};
                    for (int i = 0; i < chars.length; i++) {
                        BufferedImage img = new BufferedImage(720, 1280, BufferedImage.TYPE_INT_RGB);
                        Graphics2D g = img.createGraphics();
                        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                        Color base = SceneImage.colorFor(chars[i]);
                        g.setPaint(new GradientPaint(0, 0, SceneImage.lighten(base, 0.55f), 0, 1280, base.darker()));
                        g.fillRect(0, 0, 720, 1280);
                        g.setColor(new Color(255, 255, 255, 70));
                        g.fillOval(-40, 700, 200, 200);
                        g.fillOval(180, 760, 160, 160);
                        Actor.draw(g, 360, 990, 0.7, 1.15, base, Actor.moodFor(i), i % 2 == 0, Actor.designFor(chars[i]));
                        g.setFont(new Font("Arial", Font.BOLD, 44));
                        g.setColor(new Color(20, 20, 40, 200));
                        g.fillRoundRect(150, 1120, 420, 84, 20, 20);
                        g.setColor(Color.WHITE);
                        int nw = g.getFontMetrics().stringWidth(chars[i]);
                        g.drawString(chars[i], 360 - nw / 2, 1178);
                        ImageIO.write(img, "png", out.resolve(chars[i] + ".png").toFile());
                        g.dispose();
                    }
                    Cover.makeVariants("La torre que besó a las nubes", "CONSTRUYENDO VALORES", out);
                    ChannelArt.makeAvatar(out);
                    ChannelArt.makeBanner(out, "Construimos algo más importante que edificios: valores.");
                    System.out.println("STILLS | " + out.toAbsolutePath());
                    return;
                }
                case "--muestra" -> {
                    Path out = Paths.get(args[1]);
                    Files.createDirectories(out);
                    Path ff = Paths.get(cfg.ffmpeg()).getParent().resolve("ffprobe.exe");
                    if (!Files.exists(ff)) ff = Paths.get(cfg.ffmpeg().replace("ffmpeg", "ffprobe"));
                    String[][] picks = {
                            {"Casquito", "¡Hola, amiguitos! Soy Casquito y te doy la bienvenida a Construyendo Valores. ¿Listos para construir y jugar?"},
                            {"Luna", "¡Hola! Soy Luna, la conejita de las estrellas. Vamos a soñar y a aprender juntos."},
                            {"Pipo", "¡Jajajá! Yo soy Pipo y hoy venimos a reír y a cantar. ¿Nos acompañas?"}
                    };
                    for (String[] p : picks) {
                        String ch = p[0];
                        Path mp3 = out.resolve("muestra_" + ch + ".mp3");
                        Tts.synth(cfg, ch, cfg.voiceFor(ch), p[1], mp3);
                        Scene sc = new Scene(1, ch, p[1]);
                        sc.audioFile = mp3.toString();
                        sc.audioSeconds = Tts.durationWithPause(mp3, ff.toString());
                        sc.scenario = "parque";
                        Path sub = out.resolve(ch);
                        Files.createDirectories(sub);
                        Animator.animate(cfg, sc, "Bienvenidos a Construyendo Valores", sub);
                        System.out.println("MUESTRA | " + sc.video);
                    }
                    return;
                }
                case "--compilacion" -> {
                    Path out = Paths.get(args[1]);
                    Path video = Compilacion.crear(cfg, out, System.out::println);
                    if (video == null) return;
                    VideoCheck.Result q = VideoCheck.check(cfg, video, null);
                    q.avisos().forEach(w -> System.out.println("  AVISO: " + w));
                    if (!q.errores().isEmpty()) {
                        System.out.println("COMPILACION | control de calidad reprobado, no se sube:");
                        q.errores().forEach(e -> System.out.println("  ERROR: " + e));
                        return;
                    }
                    String titulo = "Compilación de canciones y cuentos para niños";
                    String desc = "Lo mejor de la semana de Construyendo Valores: canciones, cuentos y retos "
                            + "para toda la familia.\n\n"
                            + "Construyendo Valores: construimos algo más importante que edificios: valores.\n"
                            + "Suscríbete y activa la campanita 🔔";
                    List<String> tags = List.of("compilación", "canciones para niños", "cuentos para niños",
                            "Construyendo Valores", "videos para niños");
                    int hora = Integer.parseInt(cfg.get("youtube.publishHour", "9"));
                    String privacy = cfg.get("youtube.privacyStatus", "public");
                    Youtube.UploadResult up = Youtube.upload(cfg, video, titulo, desc, tags,
                            DailyVideo.nextPublish(hora), privacy, System.out::println);
                    System.out.println("Compilación programada: " + up.url() + " [" + up.privacyStatus() + "]");
                    Path shortV = Shorts.make(cfg, video, out, 38, System.out::println);
                    List<String> shortTags = new java.util.ArrayList<>(tags);
                    shortTags.add("Shorts");
                    Youtube.UploadResult ups = Youtube.upload(cfg, shortV, "Compilación #Shorts",
                            "Lo mejor de la semana en un minuto. Mira el video completo en el canal y suscríbete 🔔.",
                            shortTags, DailyVideo.nextPublish(hora), privacy, System.out::println);
                    System.out.println("Short de la compilación: " + ups.url() + " [" + ups.privacyStatus() + "]");
                    Metricas.registrar(cfg, java.time.LocalDate.now().toString(), "Compilación", titulo,
                            video.toString(), "SUBIDO", up.videoId(), up.url(), System.out::println);
                    return;
                }
                case "--check" -> {
                    Path v = Paths.get(args[1]);
                    Double esperado = args.length >= 3 ? Double.parseDouble(args[2]) : null;
                    VideoCheck.Result q = VideoCheck.check(cfg, v, esperado);
                    System.out.println("CHECK | " + v.getFileName());
                    System.out.println("  duración: " + String.format(java.util.Locale.ROOT, "%.1f", q.duracionSegundos())
                            + " s · tamaño: " + q.tamanioBytes() + " bytes");
                    if (q.errores().isEmpty() && q.avisos().isEmpty()) {
                        System.out.println("  APTO (sin errores ni avisos)");
                    }
                    for (String e : q.errores()) System.out.println("  ERROR: " + e);
                    for (String a : q.avisos()) System.out.println("  AVISO: " + a);
                    return;
                }
                case "--metricas" -> {
                    Metricas.refrescar(cfg, System.out::println);
                    return;
                }
                case "--mejora" -> {
                    Path mej = CicloMejora.ejecutar(cfg, Paths.get(args[1]), System.out::println);
                    System.out.println("MEJORA | " + mej.toAbsolutePath());
                    String lecc = Competidores.barrerDiario(cfg, System.out::println);
                    if (!lecc.isBlank()) System.out.println("LECCIONES | " + lecc.trim().replace("\n", " | "));
                    Decisor.decidir(cfg, Paths.get(args[1]), System.out::println);
                    return;
                }
                case "--competidores" -> {
                    String lecc = Competidores.barrerDiario(cfg, System.out::println);
                    if (!lecc.isBlank()) System.out.println(lecc);
                    return;
                }
                case "--calidad" -> {
                    IndiceCalidad.evaluar(cfg, Paths.get(args[1]), System.out::println);
                    return;
                }
                case "--qa" -> {
                    Path q = Paths.get(args[1]);
                    Double esperado = args.length >= 3 ? Double.parseDouble(args[2]) : null;
                    CalidadTotal.evaluar(cfg, q, esperado, System.out::println);
                    return;
                }
                case "--voces" -> {
                    List<String> voces = Voices.spanishVoices(cfg);
                    System.out.println("VOCES | " + voces.size() + " voces es-* detectadas en edge-tts:");
                    String[] preferidas = {"es-MX-DaliaNeural", "es-CO-GonzaloNeural", "es-MX-JorgeNeural",
                            "es-CL-CatalinaNeural", "es-EC-AndreaNeural", "es-ES-ElviraNeural", "es-PE-AlexNeural"};
                    for (String v : voces) {
                        boolean ok = false;
                        for (String p : preferidas) if (v.equals(p)) ok = true;
                        System.out.println("  " + (ok ? "* " : "  ") + v);
                    }
                    System.out.println("  (*) recomendada por la investigación de carátula 2026");
                    System.out.println("REPARTO ACTUAL:");
                    for (String ch : Config.CHARACTER_VOICES.keySet()) {
                        System.out.println("  " + ch + " -> " + cfg.voiceFor(ch));
                    }
                    return;
                }
                case "--supervision" -> {
                    Path su = Paths.get(args[1]);
                    Files.createDirectories(su);
                    String lecciones = Competidores.barrerDiario(cfg, System.out::println);

                    StringBuilder sv = new StringBuilder("# SUPERVISIÓN DIARIA DEL PROYECTO\n\n");
                    sv.append("## Competencia (vistas/me gusta/comentarios)\n");
                    sv.append(lecciones == null || lecciones.isBlank()
                            ? "- (sin datos hoy: revisa la configuración de competidores)"
                            : lecciones.replace("\n", "\n")).append('\n');

                    if (CalidadTotal.existeVideoDirecto(su)) {
                        CalidadTotal.evaluar(cfg, su, null, System.out::println);
                        sv.append("\n## Informe de calidad de la última pieza\n");
                        Path informe = su.resolve("informe_calidad.md");
                        if (Files.exists(informe)) sv.append(Files.readString(informe)).append('\n');
                        sv.append("\n## Mejoras para el próximo video\n");
                        Path mejoras = su.resolve("mejoras_siguiente.md");
                        if (Files.exists(mejoras)) sv.append(Files.readString(mejoras)).append('\n');
                    } else {
                        List<CalidadTotal.Informe> infos =
                                CalidadTotal.evaluarRaiz(cfg, su, System.out::println);
                        sv.append("\n## Estado de calidad de todas las producciones\n");
                        if (infos.isEmpty()) {
                            sv.append("- No se encontraron videos para evaluar.\n");
                        } else {
                            sv.append("| Producción | Índice | IA | Supera | APTO |\n|---|---|---|---|---|\n");
                            for (CalidadTotal.Informe in : infos) {
                                sv.append("| ").append(in.nombre())
                                        .append(" | ").append(String.format(Locale.ROOT, "%.1f", in.indice())).append("/100")
                                        .append(" | ").append(in.notaIA() == null ? "n/a"
                                                : String.format(Locale.ROOT, "%.1f", in.notaIA()) + "/10")
                                        .append(" | ").append(in.mejor() < 0 ? "n/a"
                                                : in.superaAnterior() ? "SÍ" : "NO")
                                        .append(" | ").append(in.apto() ? "SÍ" : "NO").append(" |\n");
                            }
                            sv.append("\nVer `informe_calidad.md` y `mejoras_siguiente.md` en cada carpeta.\n");
                        }
                    }
                    sv.append("\n## Regla activa\n")
                            .append("- Cada video debe superar al anterior (índice calidad.csv).\n")
                            .append("- Si el QA marca error, no publicar hasta corregir.\n")
                            .append("- Publicar solo lo que el veredicto apruebe como APTO.\n");
                    Files.writeString(su.resolve("SUPERVISION.md"), sv.toString());
                    System.out.println("SUPERVISION | " + su.resolve("SUPERVISION.md").toAbsolutePath());
                    return;
                }
                case "--decidir" -> {
                    Decisor.decidir(cfg, Paths.get(args[1]), System.out::println);
                    return;
                }
                case "--preview" -> {
                    Path out = Paths.get(args[1]);
                    Files.createDirectories(out);
                    String title = "Vista previa de escenario";
                    for (Scenario sc : Scenario.values()) {
                        Scene s = new Scene(5, "Casquito", "Este es el escenario " + sc.label + ".");
                        s.scenario = sc.id;
                        Animator.renderStill(s, title, out.resolve("escenario_" + sc.id + ".png"));
                    }
                    System.out.println("PREVIEW | " + out.toAbsolutePath());
                    return;
                }
                case "--personaje" -> {
                    String idea = args[1];
                    Path dir = Paths.get(args[2]);
                    Path md = PersonajeCreator.crear(cfg, idea, dir, System.out::println);
                    System.out.println("PERSONAJE | " + md.toAbsolutePath());
                    return;
                }
                case "--gancho" -> {
                    String titulo = args[1];
                    Path dir = Paths.get(args[2]);
                    Path hook = FreeAi.generarGancho(cfg, titulo, dir.resolve("clips"), System.out::println);
                    System.out.println(hook != null
                            ? "GANCHO | " + hook.toAbsolutePath()
                            : "GANCHO | no generado (revisa saldo/pool)");
                    return;
                }
                case "--enganche" -> {
                    Path video = Paths.get(args[1]);
                    Path hook = Paths.get(args[2]);
                    if (!Files.exists(hook)) {
                        System.out.println("ENGANCHE | no existe el clip: " + hook.toAbsolutePath());
                        return;
                    }
                    boolean ok = FreeAi.enganchar(cfg, video, hook);
                    if (ok) {
                        System.out.println("ENGANCHE | gancho cosido: " + video.toAbsolutePath());
                        CalidadTotal.evaluar(cfg, video.getParent(), null, System.out::println);
                    }
                    return;
                }
                case "--ensamblar" -> {
                    Path dir = Paths.get(args[1]);
                    ScriptParser.Script sc2 = Pipeline.remontar(cfg, dir, System.out::println);
                    System.out.println("ENSAMBLAR | videofinal restaurado para: " + sc2.title());
                    CalidadTotal.evaluar(cfg, dir, null, System.out::println);
                    return;
                }
                case "--prueba-cine" -> {
                    Path dir = Paths.get(args[1]).toAbsolutePath();
                    int idx = Integer.parseInt(args[2]);
                    java.util.List<Scene> escs = Pipeline.scenesConAudio(cfg, dir);
                    Scene sc = null;
                    double offset = 0;
                    for (Scene e : escs) {
                        if (e.index == idx) { sc = e; break; }
                        offset += Math.max(1.2, e.audioSeconds + 0.45);
                    }
                    if (sc == null) { System.out.println("No existe escena " + idx); return; }
                    Path videoFinal = null;
                    long best = 0;
                    for (Path f : Files.list(dir).toList()) {
                        String n = f.getFileName().toString().toLowerCase();
                        if (n.endsWith(".mp4") && !n.contains("merged") && !n.contains("short")) {
                            long sz = Files.size(f);
                            if (sz > best) { best = sz; videoFinal = f; }
                        }
                    }
                    if (videoFinal == null) { System.out.println("No hay video final"); return; }

                    double tMuestra = Math.min(0.55, Math.max(1.2, sc.audioSeconds + 0.45) * 0.4);
                    Path antes = dir.resolve("_cine_antes.jpg");
                    Path nuevo = dir.resolve("_cine_nuevo.jpg");
                    Path lado = dir.resolve("cine_antes_despues_esc" + idx + ".png");
                    StringBuilder lg = new StringBuilder();
                    Util.run(java.util.List.of(cfg.ffmpeg(), "-y", "-ss",
                            String.format(java.util.Locale.US, "%.2f", offset + tMuestra),
                            "-i", videoFinal.toString(), "-frames:v", "1",
                            "-q:v", "2", "-y", antes.toString()), dir, lg);
                    BufferedImage nuevoImg = Animator.frameFor(sc, "", tMuestra,
                            "Intro".equalsIgnoreCase(sc.character), Math.max(1.2, sc.audioSeconds + 0.45));
                    javax.imageio.ImageIO.write(nuevoImg, "jpg", nuevo.toFile());
                    Util.run(java.util.List.of(cfg.ffmpeg(), "-y",
                            "-i", antes.toString(), "-i", nuevo.toString(),
                            "-filter_complex",
                            "[0:v]scale=540:960:flags=lanczos[a];[1:v]scale=540:960:flags=lanczos[b];[a][b]hstack",
                            "-frames:v", "1", lado.toString()), dir, lg);
                    System.out.println("PRUEBA-CINE | antes vs despues (escena " + idx + "): "
                            + lado.toAbsolutePath());
                    return;
                }
                case "--imagen-probe" -> {
                    Path out = Paths.get(args[1]);
                    Files.createDirectories(out);
                    System.out.println("PROBANDO | generando escena y personaje con las IAs disponibles...");
                    Path esc = out.resolve("probe_escena.png");
                    String p1 = "Fondo vacio para video infantil, animacion 3D premium tipo pelicula Pixar: "
                            + "parque infantil con arboles, flores y un tobogan, ambiente alegre,"
                            + " vista amplia, sin personajes, sin texto, sin marcas de agua";
                    String r1 = ImagenIA.generar(cfg, p1, 720, 1280, 314159, "9:16", esc, System.out::println);
                    System.out.println("  escena  -> " + (r1.isBlank() ? "FALLÓ" : "ok (" + r1 + ")") + " | " + esc.toAbsolutePath());
                    Path per = out.resolve("probe_personaje.png");
                    String p2 = "Personaje infantil, animacion 3D premium tipo pelicula Pixar: "
                            + "Casquito, un caracol cartagenero maestro constructor con casco de obra, "
                            + "sonrisa amable y caparazon amarillo-naranja, retrato de cuerpo completo "
                            + "en blanco puro, sin sombras";
                    String r2 = ImagenIA.generar(cfg, p2, 720, 1280, 2718, "9:16", per, System.out::println);
                    System.out.println("  personaje -> " + (r2.isBlank() ? "FALLÓ" : "ok (" + r2 + ")") + " | " + per.toAbsolutePath());
                    return;
                }
                case "--cine" -> {
                    Path dir = Paths.get(args[1]).toAbsolutePath();
                    cfg.set("cine.habilitado", "true");
                    cfg.save();
                    String raw = Util.read(dir.resolve("guion.json"));
                    ScriptParser.Script script = ScriptParser.parse(raw);
                    java.util.List<Scene> escs = Pipeline.scenesConAudio(cfg, dir);
                    int k = 0;
                    for (Scene sc : escs) {
                        k++;
                        System.out.println("CINE " + k + "/" + escs.size() + " | escena " + sc.index
                                + " (" + sc.character + ")");
                        long ini = System.currentTimeMillis();
                        try {
                            CineIA.renderEscena(cfg, sc, script.title(), dir);
                        } catch (Exception e) {
                            String msg = e.getMessage() == null ? "error" : e.getMessage().split("\\n")[0];
                            System.out.println("   AVISO: cine fallo (" + msg + "). Animacion clasica.");
                            Animator.animate(cfg, sc, script.title(), dir);
                        }
                        System.out.println("   ok en " + ((System.currentTimeMillis() - ini) / 1000) + "s");
                    }
                    System.out.println("CINE | ensamblando...");
                    Path video = Assembler.assemble(cfg, escs, dir, script.title());
                    System.out.println("CINE | " + video.toAbsolutePath());
                    return;
                }
                case "--planificar" -> {
                    Path pv = Paths.get(args[1]);
                    String pt = args[2];
                    String pd = args[3];
                    java.time.LocalDate pf = java.time.LocalDate.parse(args[4]);
                    Clipper.planificarUn(cfg, pv, pt, pd, pf);
                    return;
                }
                case "--lanzar" -> {
                    Path carp = args.length >= 2 ? Paths.get(args[1]) : Paths.get("clips_campana");
                    java.time.LocalDate desde = args.length >= 3
                            ? java.time.LocalDate.parse(args[2]) : null;
                    int max = args.length >= 4 ? Integer.parseInt(args[3]) : 0;
                    Clipper.lanzar(cfg, carp, desde, max);
                    return;
                }
case "--clips-subir" -> {
                    Path carp = args.length >= 2 ? Paths.get(args[1]) : Paths.get("clips_campana");
                    int max = args.length >= 3 ? Integer.parseInt(args[2]) : 0;
                    Clipper.subir(cfg, carp, max);
                    return;
                }
                case "--escenas" -> {
                    Path cd = Paths.get(args[1]);
                    Path so = args.length >= 3 ? Paths.get(args[2]) : Paths.get("clips_campana");
                    String tb = args.length >= 4 ? args[3] : null;
                    int dm = args.length >= 5 ? Integer.parseInt(args[4]) : 20;
                    Clipper.escenas(cfg, cd, so, tb, dm);
                    return;
                }
                case "--clip" -> {
                    Path video = Paths.get(args[1]);
                    if (!Files.exists(video)) {
                        System.out.println("No existe el video: " + video);
                        return;
                    }
                    Path out = args.length >= 3 ? Paths.get(args[2]) : Paths.get("clips_campana");
                    int n = args.length >= 4 ? Integer.parseInt(args[3]) : 6;
                    double dur = args.length >= 5 ? Double.parseDouble(args[4]) : 40.0;
                    Clipper.cortar(cfg, video, out, n, dur);
                    return;
                }
                case "--portada" -> {
                    String prompt = args[1];
                    Path out = Paths.get(args[2]);
                    Files.createDirectories(out);
                    long seed = System.currentTimeMillis() % 1_000_000_000L;
                    Path png = out.resolve("portada_AI_16x9.png");
                    boolean ok = Pollinations.download(prompt, 1280, 720, seed, png);
                    System.out.println(ok ? "PORTADA | " + png.toAbsolutePath()
                            : "PORTADA | no se pudo generar (revisa red)");
                    return;
                }
                case "--publicidad" -> {
                    Path src = Paths.get(args[1]);
                    Path dir = Paths.get(args[2]);
                    Publicidad.generar(cfg, src, dir, System.out::println);
                    return;
                }
                case "--ia-probe" -> {
                    System.out.println("SANEO MULTI-IA | estado real de cada proveedor:");
                    for (String id : MultiIA.orden(cfg)) {
                        System.out.println("  - " + id + " -> " + MultiIA.probar(cfg, id));
                    }
                    return;
                }
                case "--ia-consulta" -> {
                    String pregunta = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
                    System.out.println("PREGUNTA: " + pregunta);
                    for (String id : MultiIA.orden(cfg)) {
                        String r;
                        try {
                            r = MultiIA.llamarPublico(cfg, id, "", pregunta);
                        } catch (Exception e) {
                            r = "NO DISPONIBLE (" + (e.getMessage() == null ? "?" : e.getMessage().split("\\n")[0]) + ")";
                        }
                        System.out.println("\n--- [" + id + "] ---\n" + r);
                    }
                    return;
                }
                default -> SwingUtilities.invokeLater(() -> new MainFrame(new Config()).setVisible(true));
            }
        } catch (Exception e) {
            System.err.println("FALLO: " + e.getMessage());
            if (e.getCause() != null) System.err.println("CAUSA: " + e.getCause().getMessage());
            System.exit(1);
        }
    }

    /**
     * Reescribe un título con la fórmula que sí funciona en el canal.
     * Solo toca los títulos con la plantilla genérica que dejaron los videos en 0-3 vistas
     * ("en el mejor momento para ver en familia"); lo demás se deja igual.
     */
    static String tituloMejorado(String viejo) {
        if (viejo == null) return null;
        String low = viejo.toLowerCase(java.util.Locale.ROOT);
        boolean filler = low.contains("en el mejor momento para ver en familia")
                || low.contains("no dejes de ver el final")
                || low.startsWith("la parte mas bonita de")
                || low.startsWith("la parte más bonita de");
        if (!filler) return viejo;

        String t = viejo.replaceAll("(?i)\\s*#shorts\\s*$", "").trim();
        t = t.replaceAll("(?i)\\s+en el mejor momento para ver en familia", "").trim();
        t = t.replaceAll("(?i)^la parte m[áa]s bonita de\\s+", "").trim();
        t = t.replaceAll("(?i)\\s*\\|\\s*no dejes de ver el final", "").trim();
        t = t.replaceAll("[\\s|]+$", "").trim();
        if (t.isEmpty()) return viejo;

        String sufijo = t.toLowerCase(java.util.Locale.ROOT).contains("canci")
                ? " | Canción infantil" : " | Cuento para niños";
        String nuevo = t + sufijo;
        if (nuevo.length() > 100) {
            int corte = nuevo.lastIndexOf(' ', 99);
            nuevo = nuevo.substring(0, corte > 0 ? corte : 100).trim();
        }
        return nuevo + " #Shorts";
    }

    /** Etiquetas temáticas para el catálogo (hoy todos los videos tienen solo "shorts"). */
    static java.util.List<String> etiquetasTitulo(String titulo) {
        java.util.List<String> t = new java.util.ArrayList<>();
        t.add("cuento para niños");
        t.add("cuentos infantiles");
        t.add("valores para niños");
        t.add("construyendo valores");
        if (titulo != null && titulo.toLowerCase(java.util.Locale.ROOT).contains("canci")) {
            t.add("canciones infantiles");
        }
        t.add("Shorts");
        return t;
    }

    private Main() {}
}