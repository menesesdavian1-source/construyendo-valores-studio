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
import java.util.List;

/**
 * Animación real por frames: cada escena se dibuja cuadro a cuadro con
 * personaje moviéndose (rebote, parpadeo, saludo), grúa oscilante y nubes
 * a la deriva, y después se codifica junto al audio en un clip. Adiós al zoom.
 */
public final class Animator {

    // Raster nativo 1080x1920. Las coordenadas de dibujo son LOGICAS 720x1280:
    // g.scale(1.5) las lleva al raster. Mezclar W/H (raster) con el escalado
    // era lo que desplazaba y recortaba el cuadro (videos descentrados).
    private static final int W = 1080;
    private static final int H = 1920;
    private static final double STAGE = 1.5;
    private static final int LW = 720;
    private static final int LH = 1280;
    private static final int FPS = 25;

    public static void animate(Config cfg, Scene sc, String title, Path clipsDir) throws Exception {
        Actor.configuraMovimiento(cfg.getDouble("calidad.movimiento", 1.0));
        double dur = Math.max(1.2, sc.audioSeconds + 0.45);
        Path framesDir = clipsDir.resolve("f_" + String.format("%02d", sc.index));
        Files.createDirectories(framesDir);

        int n = (int) Math.max(1, Math.ceil(dur * FPS));
        boolean intro = "Intro".equalsIgnoreCase(sc.character);
        for (int i = 0; i < n; i++) {
            double t = i / (double) FPS;
            Path frame = framesDir.resolve(String.format("f_%05d.jpg", i + 1));
            BufferedImage img = paintFrame(sc, title, t, intro, dur);
            writeJpeg(img, frame);
        }

        encode(cfg, sc, dur, framesDir, clipsDir);
    }

    public static void renderStill(Scene sc, String title, Path outPng) throws Exception {
        BufferedImage img = paintFrame(sc, title, 0.35, false, 2.2);
        Files.createDirectories(outPng.getParent());
        ImageIO.write(img, "png", outPng.toFile());
    }

    /** Fotograma de una escena (para pruebas y comparativas). */
    public static BufferedImage frameFor(Scene sc, String title, double t, boolean intro, double dur) {
        return paintFrame(sc, title, t, intro, dur);
    }

    /** Cámara cinematográfica: plano elegido por índice de escena, easing suave y pulso de mano. */
    private static double[] cameraAt(int index, boolean intro, double t, double dur) {
        double u = Math.min(1.0, Math.max(0.0, t / Math.max(0.01, dur)));
        double e = (1 - Math.cos(Math.PI * u)) / 2.0; // easeInOutCubic suave
        double px = 0, py = 0, zoom = 1.0;
        int move = intro ? 0 : index % 5;
        switch (move) {
            case 0 -> { // push-in (clima/emocion)
                zoom = 1.0 + 0.045 * e;
                py = -7 * e;
            }
            case 1 -> { // paneo lento derecha con leve zoom
                px = lerp(-9, 9, e);
                zoom = 1.02 + 0.015 * u;
            }
            case 2 -> { // zoom-out revela escena
                zoom = 1.06 - 0.04 * e;
                px = 4 * Math.sin(u * Math.PI);
            }
            case 3 -> { // traveling diagonal
                px = lerp(-7, 4, e);
                py = lerp(6, -6, e);
                zoom = 1.03;
            }
            default -> { // balanceo suave (contemplativo)
                px = 8 * Math.sin(2 * Math.PI * u * 1.1);
                zoom = 1.0 + 0.02 * Math.sin(2 * Math.PI * u * 2.0);
            }
        }
        // pulso de mano casi imperceptible: da vida sin marear
        double hx = 2.1 * Math.sin(2 * Math.PI * 9.7 * t + index * 1.7);
        double hy = 1.7 * Math.cos(2 * Math.PI * 8.3 * t + index * 0.9);
        return new double[]{px + hx, py + hy, zoom};
    }

    private static double lerp(double a, double b, double u) {
        return a + (b - a) * u;
    }

    private static BufferedImage paintFrame(Scene sc, String title, double t, boolean intro, double dur) {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.scale(STAGE, STAGE); // dibujamos en coordenadas de 720x1280, raster nativo 1080x1920

        double[] cam = cameraAt(sc.index, intro, t, dur);
        g.translate(LW / 2.0, LH / 2.0);
        g.scale(cam[2], cam[2]);
        g.translate(-LW / 2.0 + cam[0], -LH / 2.0 + cam[1]);

        Color base = SceneImage.colorFor(sc.character);
        GradientPaint sky = new GradientPaint(0, 0, SceneImage.lighten(base, 0.55f), 0, LH, base.darker());
        g.setPaint(sky);
        g.fillRect(0, 0, LW, LH);
        g.setPaint(new GradientPaint(0, LH * 0.55f, new Color(0, 0, 0, 0), 0, LH, new Color(40, 20, 60, 52)));
        g.fillRect(0, 0, LW, LH);

        drawSun(g);
        drawClouds(g, t);
        Scenario scen = intro ? Scenario.OBRA : Scenario.fromId(sc.scenario);
        scen.paint(g, LW, LH, sc.index, t);

        int bottomY = 990;
        if (intro) {
            drawIntroCard(g, t);
        } else {
            drawCaption(g, title, sc);
            drawBigWord(g, sc, dur > 0 ? t / dur : 0);
        }

        Actor.draw(g, LW / 2, bottomY, t, 1.15, base, Actor.moodFor(sc.index), sc.index % 2 == 0, Actor.designFor(sc.character));
        if (intro) drawConfetti(g, t);
        grade(g);
        drawVignette(g);

        g.dispose();
        return img;
    }

    /** Corrección de color cinematográfica suave: sombras tibias / luces frías. */
    private static void grade(Graphics2D g) {
        int w = LW, h = LH;
        g.setComposite(java.awt.AlphaComposite.SrcOver.derive(0.085f));
        g.setPaint(new GradientPaint(0, h * 0.32f, new Color(255, 150, 70), 0, h, new Color(90, 40, 10)));
        g.fillRect(0, 0, w, (int) (h * 0.62f));
        g.setComposite(java.awt.AlphaComposite.SrcOver.derive(0.06f));
        g.setPaint(new GradientPaint(0, 0, new Color(70, 130, 180), 0, h * 0.55f, new Color(70, 130, 180, 0)));
        g.fillRect(0, 0, w, (int) (h * 0.55f));
        g.setComposite(java.awt.AlphaComposite.SrcOver.derive(0.10f));
        g.setPaint(new GradientPaint(0, h * 0.55f, new Color(120, 60, 20, 0), 0, h, new Color(60, 25, 8)));
        g.fillRect(0, (int) (h * 0.55f), w, h - (int) (h * 0.55f));
        g.setComposite(java.awt.AlphaComposite.SrcOver);
    }

    private static BufferedImage paintKaraokeFrame(Scene sc, String title, double t) {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.scale(STAGE, STAGE);

        double[] cam = cameraAt(sc.index, false, t, Math.max(1.2, sc.audioSeconds + 0.45));
        g.translate(LW / 2.0, LH / 2.0);
        g.scale(cam[2], cam[2]);
        g.translate(-LW / 2.0 + cam[0], -LH / 2.0 + cam[1]);

        Color base = SceneImage.colorFor(sc.character);
        GradientPaint sky = new GradientPaint(0, 0, SceneImage.lighten(base, 0.55f), 0, LH, base.darker());
        g.setPaint(sky);
        g.fillRect(0, 0, LW, LH);

        drawSun(g);
        drawClouds(g, t * 1.3);
        Scenario.fromId(sc.scenario).paint(g, LW, LH, sc.index, t);

        String hdr = "CONSTRUYENDO VALORES · CANCIÓN";
        g.setFont(new Font("Arial", Font.BOLD, 27));
        int hw = g.getFontMetrics().stringWidth(hdr);
        g.setColor(new Color(20, 20, 40, 192));
        g.fillRoundRect(26, 26, hw + 40, 50, 25, 25);
        g.setColor(new Color(255, 235, 130));
        g.drawString(hdr, 46, 62);
        if (title != null && !title.isBlank()) {
            FontMetrics fm = g.getFontMetrics();
            String tt = title.length() > 30 ? title.substring(0, 30) + "…" : title;
            int tw = fm.stringWidth(tt);
            g.setColor(new Color(20, 20, 40, 200));
            g.fillRoundRect((LW - tw) / 2 - 22, 104, tw + 44, 54, 16, 16);
            g.setColor(new Color(255, 235, 130));
            g.drawString(tt, (LW - tw) / 2, 140);
        }

        double lift = karaokeLift(sc, t);
        int step = sc.index;
        String[] pals = Reparto.ELENCO.toArray(new String[0]);
        double[] fx = {0.07, 0.19, 0.31, 0.43, 0.57, 0.69, 0.81, 0.93};
        int[] fy = {875, 875, 875, 875, 875, 875, 875, 875};
        double[] fs = {0.50, 0.50, 0.50, 0.50, 0.50, 0.50, 0.50, 0.50};
        for (int i = 0; i < fx.length; i++) {
            double ph = (i % 2) == 0 ? lift : lift * 0.55;
            g.translate(0, -ph);
            String ch = pals[(i + step) % pals.length];
            Actor.draw(g, (int) (LW * fx[i]), fy[i], t, fs[i],
                    SceneImage.colorFor(ch), moodForIndex(i + step), (i % 2) == 0,
                    Actor.designFor(ch));
            g.translate(0, ph);
        }
        g.translate(0, -lift);
        String lead = pals[(step + 4) % pals.length];
        Actor.draw(g, (int) (LW * 0.50), 1085, t, 1.05,
                SceneImage.colorFor(lead), 0, true, Actor.designFor(lead));
        g.translate(0, lift);

        drawMusicNotes(g, t);
        drawLyricPanel(g, sc, t);
        grade(g);
        drawVignette(g);

        g.dispose();
        return img;
    }

    private static double karaokeLift(Scene sc, double t) {
        double phase = beatPhase(sc, t);
        return phase < 0.7 ? 34 * Math.exp(-7 * phase) * Actor.MOV : 0;
    }

    private static int moodForIndex(int i) {
        return i % 3;
    }

    private static double beatPhase(Scene sc, double t) {
        double last = 0;
        for (Double b : sc.beats) {
            if (b <= t + 0.001) last = b;
            else break;
        }
        return t - last;
    }

    private static void drawMusicNotes(Graphics2D g, double t) {
        g.setFont(new Font("Arial", Font.BOLD, 42));
        for (int i = 0; i < 6; i++) {
            double x = (i * 137 + t * 55) % (LW + 80) - 40;
            double y = 220 + (i % 3) * 120 + Math.sin(t * 2 + i) * 18;
            g.setColor(new Color(255, 255, 255, 150));
            g.drawString("♪", (int) x, (int) y);
            g.setColor(new Color(255, 235, 130, 120));
            g.drawString("♪", (int) ((x + 240) % LW), (int) ((y + 60) % 560 + 220));
        }
        drawConfetti(g, t * 0.7);
    }

    private static void drawLyricPanel(Graphics2D g, Scene sc, double t) {
        String txt = sc.karaokeText == null ? "" : sc.karaokeText.trim();
        if (txt.isBlank()) return;
        double pulse = beatPhase(sc, t) < 0.6 ? 1 + 0.05 * Math.exp(-6 * beatPhase(sc, t)) : 1;
        int cx = LW / 2;
        int cy = 1130;
        g.translate(cx, cy);
        g.scale(pulse, pulse);
        g.translate(-cx, -cy);

        g.setColor(new Color(15, 15, 35, 225));
        g.fillRoundRect(40, 1030, LW - 80, 200, 40, 40);
        g.setColor(new Color(255, 200, 60));
        g.setStroke(new BasicStroke(6f));
        g.drawRoundRect(40, 1030, LW - 80, 200, 40, 40);

        String[] words = txt.split("\\s+");
        int cur = currentWord(sc, t, words.length);
        Font fBase = new Font("Arial", Font.BOLD, 42);
        Font fCur = new Font("Arial", Font.BOLD, 50);
        g.setFont(fBase);
        FontMetrics fm = g.getFontMetrics();
        FontMetrics fmc = g.getFontMetrics(fCur);
        int x = 66;
        int y = 1158;
        for (int i = 0; i < words.length; i++) {
            String w = words[i];
            boolean isCur = i == cur;
            int ww = isCur ? fmc.stringWidth(w) : fm.stringWidth(w);
            if (x + ww > LW - 66) { y += 74; x = 66; }
            if (y > 1195) break;
            if (isCur) {
                g.setFont(fCur);
                g.setColor(new Color(255, 214, 60));
                g.fillRoundRect(x - 10, y - 42, ww + 20, 62, 14, 14);
                g.setColor(new Color(30, 20, 0));
                g.drawString(w, x, y);
            } else {
                g.setFont(fBase);
                g.setColor(new Color(255, 255, 255, i < cur ? 200 : 120));
                g.drawString(w, x, y);
            }
            x += ww + 16;
        }
        g.translate(cx, cy);
        g.scale(1 / pulse, 1 / pulse);
        g.translate(-cx, -cy);
    }

    private static int currentWord(Scene sc, double t, int nWords) {
        int elapsed = 0;
        double lastBeat = 0;
        for (Double b : sc.beats) {
            if (b <= t + 0.001) { elapsed++; lastBeat = b; } else break;
        }
        if (elapsed <= 0) return 0;
        if (lastBeat >= sc.audioSeconds) return nWords - 1;
        int total = Math.max(1, sc.beats.size() - 1);
        return Math.min(nWords - 1, (int) ((elapsed - 1) * nWords / (double) total));
    }

    private static String[] wrapLyric(String txt, FontMetrics fm, int maxW) {
        String[] words = txt.split("\\s+");
        java.util.List<String> lines = new java.util.ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String w : words) {
            String test = cur.isEmpty() ? w : cur + " " + w;
            if (fm.stringWidth(test) > maxW && !cur.isEmpty()) {
                lines.add(cur.toString());
                cur = new StringBuilder(w);
            } else {
                cur = new StringBuilder(test);
            }
        }
        if (!cur.isEmpty()) lines.add(cur.toString());
        return lines.size() > 2 ? lines.subList(0, 2).toArray(new String[0])
                : lines.toArray(new String[0]);
    }

    private static void drawSun(Graphics2D g) {
        int cx = LW - 155;
        int cy = 165;
        int r = 92;
        RadialGradientPaint glow = new RadialGradientPaint(cx, cy, r * 1.7f,
                new float[]{0f, 0.5f, 1f},
                new Color[]{new Color(255, 246, 190, 170), new Color(255, 224, 120, 90),
                        new Color(255, 224, 120, 0)});
        g.setPaint(glow);
        g.fillOval(cx - r * 2, cy - r * 2, r * 4, r * 4);
        RadialGradientPaint body = new RadialGradientPaint(cx - 16, cy - 12, r,
                new float[]{0f, 0.7f, 1f},
                new Color[]{new Color(255, 252, 214), new Color(255, 228, 128), new Color(255, 196, 66)});
        g.setPaint(body);
        g.fillOval(cx - r, cy - r, r * 2, r * 2);
        g.setColor(new Color(255, 255, 255, 130));
        g.fillOval((int) (cx - r * 0.36), (int) (cy - r * 0.36), (int) (r * 0.52), (int) (r * 0.52));
    }

    private static void drawClouds(Graphics2D g, double t) {
        for (int i = 0; i < 3; i++) {
            double speed = 12 + i * 8;
            double x = (i * 260 + t * speed) % (LW + 420) - 210;
            int y = 92 + i * 130;
            int[] puffs = {90, 74, 84};
            int[] dx = {0, 44, 100};
            int[] dy = {0, -22, 0};
            for (int p = 0; p < 3; p++) {
                RadialGradientPaint puff = new RadialGradientPaint(
                        (float) (x + dx[p] - puffs[p] * 0.22f), (float) (y + dy[p] - puffs[p] * 0.18f), puffs[p] * 0.9f,
                        new float[]{0f, 0.75f, 1f},
                        new Color[]{new Color(255, 255, 255, 250), new Color(244, 240, 252, 220),
                                new Color(222, 218, 238, 200)});
                g.setPaint(puff);
                g.fillOval((int) x + dx[p], y + dy[p], puffs[p], (int) (puffs[p] * 0.52));
            }
            g.setColor(new Color(196, 190, 214, 110));
            g.fillOval((int) (x + 78), y + 6, 88, 16);
        }
    }

    private static void drawVignette(Graphics2D g) {
        RadialGradientPaint v = new RadialGradientPaint(LW / 2f, LH / 2f, LH * 0.78f,
                new float[]{0f, 0.78f, 1f},
                new Color[]{new Color(0, 0, 0, 0), new Color(0, 0, 0, 0), new Color(16, 10, 30, 68)});
        g.setPaint(v);
        g.fillRect(0, 0, LW, LH);
    }

    private static void drawCaption(Graphics2D g, String title, Scene sc) {
        g.setColor(new Color(255, 255, 255, 235));
        g.setFont(new Font("Arial", Font.BOLD, 44));
        g.drawString("CONSTRUYENDO VALORES", 40, 62);

        g.setFont(new Font("Arial", Font.BOLD, 24));
        g.setColor(new Color(255, 255, 255, 200));
        g.drawString("Escena " + sc.index, 40, 96);

        if (title != null && !title.isBlank()) {
            g.setFont(new Font("Arial", Font.BOLD, 30));
            FontMetrics fm = g.getFontMetrics();
            String t = title.length() > 34 ? title.substring(0, 34) : title;
            int tw = fm.stringWidth(t);
            g.setColor(new Color(20, 20, 40, 200));
            g.fillRoundRect((LW - tw) / 2 - 26, 120, tw + 52, 60, 16, 16);
            g.setColor(Color.WHITE);
            g.drawString(t, (LW - tw) / 2, 158);
        }

        if (sc.character != null && !sc.character.isBlank()) {
            g.setFont(new Font("Arial", Font.BOLD, 26));
            g.setColor(new Color(20, 20, 40, 210));
            String who = "Dice " + sc.character;
            FontMetrics fm = g.getFontMetrics();
            int tw = fm.stringWidth(who);
            g.fillRoundRect((LW - tw) / 2 - 20, 200, tw + 40, 52, 14, 14);
            g.setColor(Color.WHITE);
            g.drawString(who, (LW - tw) / 2, 233);
        }
    }

    private static void drawIntroCard(Graphics2D g, double t) {
        g.setColor(new Color(20, 20, 40, 230));
        g.fillRoundRect(60, 460, LW - 120, 480, 44, 44);

        String t1 = "CONSTRUYENDO";
        String t2 = "VALORES";
        g.setFont(new Font("Arial", Font.BOLD, 66));
        g.setColor(Color.WHITE);
        FontMetrics fm = g.getFontMetrics();
        int w1 = fm.stringWidth(t1);
        g.drawString(t1, (LW - w1) / 2, 620);

        g.setFont(new Font("Arial", Font.BOLD, 110));
        fm = g.getFontMetrics();
        int w2 = fm.stringWidth(t2);
        g.setColor(new Color(255, 235, 130));
        g.drawString(t2, (LW - w2) / 2, 780);

        g.setFont(new Font("Arial", Font.BOLD, 26));
        fm = g.getFontMetrics();
        String tag = "Aprendemos valores mientras construimos";
        int wt = fm.stringWidth(tag);
        g.setColor(new Color(220, 220, 230));
        g.drawString(tag, (LW - wt) / 2, 896);
    }

    private static void drawConfetti(Graphics2D g, double t) {
        g.setColor(new Color(255, 200, 60));
        for (int i = 0; i < 26; i++) {
            double x = (i * 97 + t * 60) % LW;
            double y = ((i * 113) + t * (70 + i % 5 * 22)) % (LH - 200);
            double s = 7 + (i % 3) * 3;
            if (i % 4 == 0) g.setColor(new Color(255, 120, 120));
            else if (i % 4 == 1) g.setColor(new Color(120, 200, 120));
            else if (i % 4 == 2) g.setColor(new Color(120, 170, 255));
            else g.setColor(new Color(255, 200, 60));
            g.fillRoundRect((int) x, (int) y, (int) s, (int) (s * 1.6), 6, 6);
        }
    }

    private static void drawBigWord(Graphics2D g, Scene sc, double frac) {
        if (sc.bigWord == null || sc.bigWord.isBlank()) return;
        double f = frac - sc.revealAt;
        if (f < 0) return;
        double pop = Math.min(1, f * 5);
        double s = 0.35 + 0.65 * (1 - Math.pow(1 - pop, 3));

        String w = sc.bigWord.trim();
        int maxW = LW - 120;
        Font afont = new Font("Arial", Font.BOLD, 84);
        g.setFont(afont);
        String[] lines = wrapBig(w, g.getFontMetrics(), maxW);
        int fs = 84;
        while (lines.length > 2 && fs > 52) {
            fs -= 6;
            afont = new Font("Arial", Font.BOLD, fs);
            g.setFont(afont);
            lines = wrapBig(w, g.getFontMetrics(), maxW);
        }
        FontMetrics fm = g.getFontMetrics();
        int lh = fm.getHeight() + 10;
        int panelW = maxW + 56;
        int panelH = (sc.listNumber > 0 ? 34 : 0) + lines.length * lh + 26;

        g.translate(LW / 2, 470);
        g.scale(s, s);
        g.translate(-LW / 2, -470);

        int px = (LW - panelW) / 2;
        int py = 470 - panelH / 2 - 30;

        g.setColor(new Color(15, 15, 35, 232));
        g.fillRoundRect(px, py, panelW, panelH, 30, 30);
        g.setColor(new Color(255, 200, 60));
        g.setStroke(new BasicStroke(5f));
        g.drawRoundRect(px, py, panelW, panelH, 30, 30);

        int ty = py + 8;
        if (sc.listNumber > 0) {
            g.setColor(new Color(233, 90, 70));
            g.fillOval(px + 22, ty, 52, 52);
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 34));
            String n = String.valueOf(sc.listNumber);
            int nw = g.getFontMetrics().stringWidth(n);
            g.drawString(n, px + 22 + (52 - nw) / 2, ty + 38);
            ty += 66;
            g.setFont(afont);
        }
        ty += lh - fm.getHeight() + 4;
        g.setColor(Color.WHITE);
        for (String line : lines) {
            g.drawString(line, (LW - fm.stringWidth(line)) / 2, ty);
            ty += lh;
        }

        g.translate(LW / 2, 470);
        g.scale(1 / s, 1 / s);
        g.translate(-LW / 2, -470);
    }

    private static String[] wrapBig(String s, FontMetrics fm, int maxW) {
        String[] words = s.split("\\s+");
        java.util.List<String> lines = new java.util.ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String w : words) {
            String test = cur.isEmpty() ? w : cur + " " + w;
            if (fm.stringWidth(test) > maxW && !cur.isEmpty()) {
                lines.add(cur.toString());
                cur = new StringBuilder(w);
            } else {
                cur = new StringBuilder(test);
            }
        }
        if (!cur.isEmpty()) lines.add(cur.toString());
        return lines.toArray(new String[0]);
    }

    private static void writeJpeg(BufferedImage img, Path out) throws Exception {
        javax.imageio.ImageWriter writer =
                javax.imageio.ImageIO.getImageWritersBySuffix("jpg").next();
        javax.imageio.ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(0.98f);
        try (javax.imageio.stream.ImageOutputStream ios =
                     javax.imageio.ImageIO.createImageOutputStream(out.toFile())) {
            writer.setOutput(ios);
            writer.write(null, new javax.imageio.IIOImage(img, null, null), param);
        }
        writer.dispose();
    }

    static void encode(Config cfg, Scene sc, double dur, Path framesDir, Path clipsDir) throws Exception {
        String ffmpeg = cfg.ffmpeg();
        String pattern = framesDir.resolve("f_%05d.jpg").toString();
        Path clip = clipsDir.resolve("clip_" + String.format("%02d", sc.index) + ".mp4");

        List<String> cmd = List.of(ffmpeg, "-y", "-framerate", String.valueOf(FPS),
                "-i", pattern, "-i", sc.audioFile,
                "-t", String.format(java.util.Locale.US, "%.2f", dur),
                "-c:v", "libx264", "-preset", "medium", "-crf", "20",
                "-pix_fmt", "yuv420p", "-r", String.valueOf(FPS),
                "-c:a", "aac", "-b:a", "128k",
                clip.toString());

        StringBuilder log = new StringBuilder();
        int code = Util.run(cmd, null, log);
        if (code != 0) {
            throw new RuntimeException("Fallo al codificar la escena " + sc.index + ":\n" + log);
        }
        sc.video = clip.toString();
    }

    private Animator() {}
}