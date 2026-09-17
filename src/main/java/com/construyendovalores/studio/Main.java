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
                    ScriptParser.Script s = ScriptParser.parse(GeminiParser.extractJson(raw));
                    Path out = Paths.get(dir);
                    Pipeline.exec(cfg, s, out, System.out::println);
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
                case "--youtube-preview" -> {
                    Path v = Paths.get(args[1]);
                    if (!Files.exists(v)) {
                        System.out.println("No existe el video: " + v);
                        return;
                    }
                    String t = args.length > 2 ? args[2] : "Demo API Construyendo Valores";
                    String d = args.length > 3 ? args[3]
                            : "Caso de uso del audit: subida de un video mediante la YouTube Data API v3 (OAuth 2.0, resumable).";
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
                            List.of("construyendo valores", "infantil", "demo api"), null, "private",
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
                    Path dir = Paths.get(args[1]);
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
                default -> SwingUtilities.invokeLater(() -> new MainFrame(new Config()).setVisible(true));
            }
        } catch (Exception e) {
            System.err.println("FALLO: " + e.getMessage());
            if (e.getCause() != null) System.err.println("CAUSA: " + e.getCause().getMessage());
            System.exit(1);
        }
    }

    private Main() {}
}