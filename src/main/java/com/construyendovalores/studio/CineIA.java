package com.construyendovalores.studio;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Render "Cine IA 2.5D": escena de fondo ilustrada por IA (ImagenIA) sobre la
 * que se compone el personaje recortado de su lamina oficial (blanco-key), con
 * camara cinematografica, rebote, sombra suave, destello de luz y particulas.
 * Reutiliza animacion por frames + encode del Animator y el ensamblado final.
 */
public final class CineIA {

    private static final int W = 1080;
    private static final int H = 1920;
    private static final double STAGE = 1.5;
    private static final int FPS = 25;

    private static final String ESTILO = "animacion 3D premium para ninos tipo pelicula (estetica Pixar/Plim Plim), "
            + "render volumetrico, luz suave y magica, colores vivos saturados, alto detalle, "
            + "sin texto, sin letras, sin marcas de agua, sin firmas";

    private static final Map<String, String> ESCENARIO = new HashMap<>();

    static {
        ESCENARIO.put("obra", "obra de construccion con bloques de colores, gruas, ladrillos y un puente en construccion");
        ESCENARIO.put("parque", "parque infantil con arboles, flores, columpios y tobogan");
        ESCENARIO.put("taller", "taller de manualidades con mesa de trabajo, herramientas de juguete y telas de colores");
        ESCENARIO.put("bosque", "bosque magico con arboles encantados, setas luminosas y caminos de piedra");
        ESCENARIO.put("playa", "playa soleada con castillos de arena, olas suaves y conchas");
        ESCENARIO.put("ciudad", "ciudad de juguete con edificios de colores y calles alegres");
        ESCENARIO.put("escuela", "salon de clases infantil con pizarron, libros y material escolar de colores");
        ESCENARIO.put("granja", "granja alegre con huerta, granero rojo y animales de juguete");
        ESCENARIO.put("espacio", "espacio exterior estrellado con planetas de colores y cohetes de juguete");
    }

    private static final Map<String, String> EMOCION = new HashMap<>();

    static {
        EMOCION.put("sorpresa", "momento sorprendente de asombro y ojos abiertos");
        EMOCION.put("curioso", "ambiente de curiosidad y exploracion");
        EMOCION.put("alegre", "ambiente alegre con brillo calido");
        EMOCION.put("triste", "tonos suaves y calma melancolica");
        EMOCION.put("orgulloso", "ambiente de logro con colores brillantes y calidos");
        EMOCION.put("tranquilo", "ambiente sereno con luz suave y ensonadora");
        EMOCION.put("enojado", "energia viva con tonos calidos dramaticos");
    }

    private static final Map<String, String> LOOK = new HashMap<>();

    static {
        LOOK.put("casquito", "Casquito, un caracol cartagenero maestro constructor con casco de obra, sonrisa amable y caparazon amarillo-naranja");
        LOOK.put("tito", "Tito, una excavadora amarilla con cara en la cabina, ojos grandes, curiosa e impaciente");
        LOOK.put("sofia", "Sofia, una patita rosa con casco de obra y mochila de artista, alegre y creativa, pico naranja");
        LOOK.put("nico", "Nico, un monito cafe con casco de obra y gafas de seguridad rojas, entusiasta y jugueton");
        LOOK.put("luna", "Luna, una conejita blanca-lila de orejas largas, ojos asombrados y una estrella en el pecho, magica y curiosa");
        LOOK.put("pipo", "Pipo, un perrito cafe con orejas caidas y casco amarillo, muy bromista y sonriente");
        LOOK.put("tico", "Tico, una tortuga verde sabia con concha marron y anteojos, tranquila y cuentacuentos");
        LOOK.put("bruno", "Bruno, un camion volquete rojo con cara en la cabina y ruedas grandes, trabajador y fuerte");
        LOOK.put("memo", "Memo, un buho cafe con grandes ojos ambar y corbata, inteligente y cuidadoso");
        LOOK.put("coco", "Coco, una guacamaya colorida roja azul y amarilla, crestas vivas, alegre y parlanchina");
    }

    public static void renderEscena(Config cfg, Scene sc, String title, Path outDir) throws Exception {
        Path clipsDir = outDir.resolve("clips");
        Path artDir = outDir.resolve("arte");
        Files.createDirectories(clipsDir);
        Files.createDirectories(artDir);

        boolean intro = "Intro".equalsIgnoreCase(sc.character);
        BufferedImage bg = cuadroIA(cfg, sc, artDir);

        double dur = Math.max(1.2, sc.audioSeconds + 0.45);
        Path framesDir = clipsDir.resolve("c_" + String.format("%02d", sc.index));
        Files.createDirectories(framesDir);
        int n = (int) Math.max(1, Math.ceil(dur * FPS));
        for (int i = 0; i < n; i++) {
            double t = i / (double) FPS;
            BufferedImage img = cuadro(bg, sc, title, t, intro, dur);
            Path frame = framesDir.resolve(String.format("f_%05d.jpg", i + 1));
            javax.imageio.ImageWriter writer = javax.imageio.ImageIO.getImageWritersBySuffix("jpg").next();
            javax.imageio.ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(0.95f);
            try (javax.imageio.stream.ImageOutputStream ios =
                         javax.imageio.ImageIO.createImageOutputStream(frame.toFile())) {
                writer.setOutput(ios);
                writer.write(null, new javax.imageio.IIOImage(img, null, null), param);
            }
            writer.dispose();
        }
        Animator.encode(cfg, sc, dur, framesDir, clipsDir);
    }

    /** Genera (o reutiliza) la ilustracion completa de la escena: personaje + ambiente en un solo cuadro. */
    private static BufferedImage cuadroIA(Config cfg, Scene sc, Path artDir) {
        Path archivo = artDir.resolve("escena_" + String.format("%02d", sc.index) + ".png");
        try {
            if (Files.exists(archivo) && Files.size(archivo) > 20_000) {
                BufferedImage img = ImageIO.read(archivo.toFile());
                if (img != null) return img;
            }
            String esc = sc.scenario == null ? "" : sc.scenario.trim().toLowerCase();
            String desc = ESCENARIO.getOrDefault(esc, ESCENARIO.get("obra"));
            String norm = norm(caracteres(sc.character == null ? "" : sc.character));
            boolean conPersonaje = !norm.isEmpty() && !norm.equals("intro") && !norm.equals("narrador");
            String personaje = "";
            if (conPersonaje) {
                personaje = cfg.get("personaje." + norm + ".descripcionArte", LOOK.getOrDefault(norm, ""))
                        .replaceAll("\\\\n", " ").replaceAll("\\s+", " ").trim();
                if (personaje.isBlank()) personaje = LOOK.getOrDefault(norm, "");
            }
            String hum = EMOCION.getOrDefault(sc.emotion, EMOCION.get("alegre"));
            String prota = conPersonaje ? "En primer plano aparece " + personaje + ", figura completa visible," : "";
            String base = "Ilustracion para video infantil vertical, " + ESTILO + ": " + prota
                    + " escenario " + desc + ", " + hum;
            long seed = 0x9E3779B97F4A7C15L ^ ((sc.index + 1) * 2654435761L)
                    ^ (conPersonaje ? (norm.hashCode() & 0xFFFFFFFFL) : 0L);
            String[] tries = {base, "Escena animada infantil 3D premium, " + prota
                    + " " + desc + ", " + hum + ", sin texto"};
            for (String prompt : tries) {
                String prov = ImagenIA.generar(cfg, prompt, 720, 1280, seed & 0x7FFFFFFF, "9:16", archivo, null);
                if (!prov.isBlank()) {
                    reescalarArte(cfg, archivo);
                    BufferedImage img = ImageIO.read(archivo.toFile());
                    if (img != null) return img;
                }
                Files.deleteIfExists(archivo);
            }
        } catch (Exception ignored) {
        }
        System.err.println("AVISO: sin ilustracion IA para escena " + sc.index + ". Usando degradado.");
        return null;
    }

    /** Sube la definicion del arte barato (576x1024) a 1152x2048 con lanczos + unsharp, sin costo de IA. */
    private static void reescalarArte(Config cfg, Path archivo) {
        try {
            BufferedImage img = ImageIO.read(archivo.toFile());
            if (img == null || (img.getWidth() >= 900 && img.getHeight() >= 1600)) return;
            String ff = Ffmpeg.locate();
            if (ff == null || ff.isBlank()) return;
            Path tmp = archivo.resolveSibling(archivo.getFileName().toString().replace(".png", ".hd.png"));
            List<String> cmd = new ArrayList<>();
            cmd.add(ff);
            cmd.add("-y");
            cmd.add("-v");
            cmd.add("error");
            cmd.add("-i");
            cmd.add(archivo.toString());
            cmd.add("-vf");
            cmd.add("scale=1152:2048:flags=lanczos,unsharp=5:5:0.55:5:5:0.0");
            cmd.add(tmp.toString());
            if (Util.run(cmd, null, null) == 0 && Files.exists(tmp) && Files.size(tmp) > 20_000) {
                Files.move(tmp, archivo, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                System.err.println("arte: reescalado a 1152x2048 (lanczos+unsharp)");
            } else {
                Files.deleteIfExists(tmp);
            }
        } catch (Exception ignored) {
            // si el reescalado falla se conserva la imagen original
        }
    }

    private static BufferedImage cuadro(BufferedImage bg, Scene sc, String title,
                                        double t, boolean intro, double dur) {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.scale(STAGE, STAGE);

        double[] cam = cameraAt(sc.index, intro, t, dur);
        g.translate(W / 2.0, H / 2.0);
        g.scale(cam[2], cam[2]);
        g.translate(-W / 2.0 + cam[0], -H / 2.0 + cam[1]);

        if (bg != null) {
            double s = Math.max(W / (double) bg.getWidth(), H / (double) bg.getHeight());
            int dw = (int) Math.ceil(bg.getWidth() * s);
            int dh = (int) Math.ceil(bg.getHeight() * s);
            g.drawImage(bg, (W - dw) / 2, (H - dh) / 2, dw, dh, null);
        } else {
            Color b = SceneImage.colorFor(sc.character);
            g.setPaint(new GradientPaint(0, 0, SceneImage.lighten(b, 0.55f), 0, H, b.darker()));
            g.fillRect(0, 0, W, H);
        }

        bottomGradiente(g);
        destello(g, t);
        if (!intro) {
            motas(g, t);
        }
        if (intro) {
            tarjetaIntro(g, t);
        } else {
            rotulo(g, title, sc);
        }
        grade(g);
        viñeta(g);

        g.dispose();
        return img;
    }

    private static void motas(Graphics2D g, double t) {
        for (int i = 0; i < 14; i++) {
            double x = (i * 137 + t * 42) % (W + 80) - 40;
            double y = 220 + (i % 5) * 130 + Math.sin(t * 1.7 + i) * 16;
            int alfa = 70 + (i % 4) * 45;
            if (i % 3 == 0) {
                g.setColor(new Color(255, 235, 130, alfa));
                g.fillOval((int) x, (int) y, 14, 14);
            } else {
                g.setColor(new Color(255, 255, 255, alfa));
                g.fillOval((int) x, (int) y, 9, 9);
            }
        }
    }

    private static void destello(Graphics2D g, double t) {
        double fase = ((t * 130) % 1500 - 150) / 1000.0;
        if (fase < -0.4 || fase > 1.2) return;
        g.setComposite(java.awt.AlphaComposite.SrcOver.derive(0.05f));
        g.setPaint(new GradientPaint(
                (int) (W * 0.2), (int) (H * 0.15 + fase * 700),
                new Color(255, 255, 240),
                (int) (W * 0.85), (int) (H * 0.45 + fase * 700),
                new Color(255, 255, 240, 0)));
        g.fillRect(0, 0, W, H);
        g.setComposite(java.awt.AlphaComposite.SrcOver);
    }

    private static void bottomGradiente(Graphics2D g) {
        g.setPaint(new GradientPaint(0, H * 0.62f, new Color(40, 20, 60, 0), 0, H, new Color(30, 16, 46, 120)));
        g.fillRect(0, 0, W, H);
    }

    private static void rotulo(Graphics2D g, String title, Scene sc) {
        g.setColor(new Color(255, 255, 255, 235));
        g.setFont(new Font("Arial", Font.BOLD, 44));
        g.drawString("CONSTRUYENDO VALORES", 40, 62);

        g.setFont(new Font("Arial", Font.BOLD, 24));
        g.setColor(new Color(255, 255, 255, 200));
        g.drawString("Escena " + sc.index, 40, 96);

        if (title != null && !title.isBlank()) {
            g.setFont(new Font("Arial", Font.BOLD, 30));
            FontMetrics fm = g.getFontMetrics();
            String tt = title.length() > 34 ? title.substring(0, 34) : title;
            int tw = fm.stringWidth(tt);
            g.setColor(new Color(20, 20, 40, 195));
            g.fillRoundRect((W - tw) / 2 - 26, 120, tw + 52, 60, 16, 16);
            g.setColor(Color.WHITE);
            g.drawString(tt, (W - tw) / 2, 158);
        }

        if (sc.character != null && !sc.character.isBlank() && !"Narrador".equalsIgnoreCase(sc.character)) {
            g.setFont(new Font("Arial", Font.BOLD, 26));
            g.setColor(new Color(20, 20, 40, 205));
            String who = sc.character.trim();
            FontMetrics fm = g.getFontMetrics();
            int tw = fm.stringWidth(who);
            int chip = (W - tw) / 2 - 20;
            g.fillRoundRect(chip, 200, tw + 40, 52, 14, 14);
            g.setColor(Color.WHITE);
            g.drawString(who, (W - tw) / 2, 233);
        } else if (sc.character != null && "Narrador".equalsIgnoreCase(sc.character)) {
            g.setFont(new Font("Arial", Font.BOLD, 24));
            g.setColor(new Color(255, 255, 255, 210));
            String who = "Narrando…";
            FontMetrics fm = g.getFontMetrics();
            int tw = fm.stringWidth(who);
            g.setColor(new Color(20, 20, 40, 180));
            g.fillRoundRect((W - tw) / 2 - 18, 200, tw + 36, 46, 12, 12);
            g.setColor(new Color(255, 235, 130));
            g.drawString(who, (W - tw) / 2, 228);
        }
    }

    private static void tarjetaIntro(Graphics2D g, double t) {
        double pop = Math.min(1, Math.max(0, t * 2.2));
        double scale = 0.7 + 0.3 * (1 - Math.pow(1 - pop, 3));

        g.setColor(new Color(20, 20, 40, 210));
        g.fillRoundRect(60, 500, W - 120, 470, 44, 44);

        g.translate(W / 2, 630);
        g.scale(scale, scale);
        g.translate(-W / 2, -630);

        String t1 = "CONSTRUYENDO";
        String t2 = "VALORES";
        g.setFont(new Font("Arial", Font.BOLD, 66));
        g.setColor(Color.WHITE);
        FontMetrics fm = g.getFontMetrics();
        int w1 = fm.stringWidth(t1);
        g.drawString(t1, (W - w1) / 2, 620);
        g.setFont(new Font("Arial", Font.BOLD, 110));
        fm = g.getFontMetrics();
        int w2 = fm.stringWidth(t2);
        g.setColor(new Color(255, 235, 130));
        g.drawString(t2, (W - w2) / 2, 780);
        g.setFont(new Font("Arial", Font.BOLD, 26));
        fm = g.getFontMetrics();
        String tag = "Aprendemos valores mientras construimos";
        int wt = fm.stringWidth(tag);
        g.setColor(new Color(220, 220, 230));
        g.drawString(tag, (W - wt) / 2, 892);

        g.translate(W / 2, 630);
        g.scale(1 / scale, 1 / scale);
        g.translate(-W / 2, -630);
    }

    private static void grade(Graphics2D g) {
        g.setComposite(java.awt.AlphaComposite.SrcOver.derive(0.085f));
        g.setPaint(new GradientPaint(0, H * 0.32f, new Color(255, 150, 70), 0, H, new Color(90, 40, 10)));
        g.fillRect(0, 0, W, (int) (H * 0.62f));
        g.setComposite(java.awt.AlphaComposite.SrcOver.derive(0.06f));
        g.setPaint(new GradientPaint(0, 0, new Color(70, 130, 180), 0, H * 0.55f, new Color(70, 130, 180, 0)));
        g.fillRect(0, 0, W, (int) (H * 0.55f));
        g.setComposite(java.awt.AlphaComposite.SrcOver);
    }

    private static void viñeta(Graphics2D g) {
        RadialGradientPaint v = new RadialGradientPaint(W / 2f, H / 2f, H * 0.78f,
                new float[]{0f, 0.78f, 1f},
                new Color[]{new Color(0, 0, 0, 0), new Color(0, 0, 0, 0), new Color(16, 10, 30, 72)});
        g.setPaint(v);
        g.fillRect(0, 0, W, H);
    }

    /** Camara cinematografica: plano por indice, easing suave, pulso de mano. */
    private static double[] cameraAt(int index, boolean intro, double t, double dur) {
        double u = Math.min(1.0, Math.max(0.0, t / Math.max(0.01, dur)));
        double e = (1 - Math.cos(Math.PI * u)) / 2.0;
        double px = 0, py = 0, zoom = 1.0;
        int move = intro ? 0 : index % 5;
        switch (move) {
            case 0 -> {
                zoom = 1.0 + 0.05 * e;
                py = -8 * e;
            }
            case 1 -> {
                px = lerp(-10, 10, e);
                zoom = 1.02 + 0.016 * u;
            }
            case 2 -> {
                zoom = 1.07 - 0.045 * e;
                px = 4 * Math.sin(u * Math.PI);
            }
            case 3 -> {
                px = lerp(-8, 5, e);
                py = lerp(7, -7, e);
                zoom = 1.03;
            }
            default -> {
                px = 9 * Math.sin(2 * Math.PI * u * 1.1);
                zoom = 1.0 + 0.022 * Math.sin(2 * Math.PI * u * 2.0);
            }
        }
        double hx = 2.3 * Math.sin(2 * Math.PI * 9.7 * t + index * 1.7);
        double hy = 1.9 * Math.cos(2 * Math.PI * 8.3 * t + index * 0.9);
        return new double[]{px + hx, py + hy, zoom};
    }

    private static double lerp(double a, double b, double u) {
        return a + (b - a) * u;
    }

    private static String norm(String s) {
        return s.toLowerCase()
                .replace('\u00e1', 'a').replace('\u00e9', 'e').replace('\u00ed', 'i')
                .replace('\u00f3', 'o').replace('\u00fa', 'u').replace('\u00f1', 'n')
                .replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
    }

    /** Quita acentos y convierte a "Sofia" -> "sofia". */
    private static String caracteres(String s) {
        return s.replace('\u00e1', 'a').replace('\u00e9', 'e').replace('\u00ed', 'i')
                .replace('\u00f3', 'o').replace('\u00fa', 'u').replace('\u00f1', 'n')
                .replace('\u00dc', 'U').replace('\u00fc', 'u');
    }

    private CineIA() {}
}