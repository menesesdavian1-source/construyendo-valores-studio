package com.construyendovalores.studio;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** Genera un fondo ilustrado por escena en Java, con un color por personaje. */
public final class SceneImage {

    private static final int W = 1080;
    private static final int H = 1920;
    private static final Map<String, Color> PALETTE = new HashMap<>();

    static {
        PALETTE.put("Casquito", new Color(0xFFB300));
        PALETTE.put("Tito", new Color(0xE8590C));
        PALETTE.put("Sofía", new Color(0x339AF0));
        PALETTE.put("Nico", new Color(0x51CF66));
        PALETTE.put("Narrador", new Color(0x94D82D));
        PALETTE.put("Intro", new Color(0xFFB300));
        PALETTE.put("Luna", new Color(0x9775FA));
        PALETTE.put("Pipo", new Color(0xF783AC));
        PALETTE.put("Tico", new Color(0x20C997));
        PALETTE.put("Bruno", new Color(0xE8592E));
        PALETTE.put("Memo", new Color(0xB08968));
        PALETTE.put("Coco", new Color(0xFA5252));
    }

    public static Color colorFor(String character) {
        return PALETTE.getOrDefault(character, new Color(0xADB5BD));
    }

    public static void render(Scene sc, String title, Path workDir) {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        Color base = colorFor(sc.character);
        GradientPaint sky = new GradientPaint(0, 0, lighten(base, 0.55f), 0, H, base.darker());
        g.setPaint(sky);
        g.fillRect(0, 0, W, H);

        drawSun(g);
        drawCity(g, base, sc.index);
        if ("Intro".equalsIgnoreCase(sc.character)) {
            drawIntroCard(g);
        } else {
            drawText(g, title, sc.index, sc.character);
        }

        g.dispose();
        Path out = workDir.resolve("scene_" + String.format("%02d", sc.index) + ".png");
        try {
            ImageIO.write(img, "png", out.toFile());
        } catch (Exception e) {
            throw new RuntimeException("No se pudo guardar el fondo " + out, e);
        }
        sc.imageFile = out.toString();
    }

    static Color lighten(Color c, float f) {
        return new Color(Math.min(255, (int) (c.getRed() + (255 - c.getRed()) * f)),
                Math.min(255, (int) (c.getGreen() + (255 - c.getGreen()) * f)),
                Math.min(255, (int) (c.getBlue() + (255 - c.getBlue()) * f)));
    }

    private static void drawSun(Graphics2D g) {
        g.setColor(new Color(255, 240, 150, 200));
        g.fillOval(W - 340, 130, 220, 220);
        g.setStroke(new BasicStroke(10f));
        g.setColor(new Color(255, 240, 150, 220));
        for (int i = 0; i < 10; i++) {
            double a = Math.PI * 2 * i / 10;
            int x1 = (int) (W - 230 + Math.cos(a) * 130);
            int y1 = (int) (240 + Math.sin(a) * 130);
            int x2 = (int) (W - 230 + Math.cos(a) * 160);
            int y2 = (int) (240 + Math.sin(a) * 160);
            g.drawLine(x1, y1, x2, y2);
        }
    }

    private static void drawCity(Graphics2D g, Color base, int idx) {
        g.setColor(new Color(40, 40, 60));
        g.fillRect(0, H - 720, W, 720);

        g.setColor(base.darker().darker());
        for (int i = 0; i < 5; i++) {
            int x = (idx * 60 + i * 190) % (W + 200) - 100;
            int w = 150 + (i * 31) % 90;
            int h = 260 + (i * 97) % 260;
            g.fillRect(x, H - 720 - h, w, h);

            g.setColor(new Color(70, 70, 90));
            for (int wy = 0; wy < h / 90; wy++) {
                for (int wx = 0; wx < w / 50; wx++) {
                    g.fillRect(x + 15 + wx * 50, H - 720 - h + 20 + wy * 90, 24, 40);
                }
            }
            g.setColor(base.darker().darker());
        }

        g.setColor(base);
        int bw = 760;
        int bx = (W - bw) / 2;
        int bh = 560;
        int by = H - 720;
        g.setColor(new Color(230, 228, 212));
        g.fillRect(bx, by - bh, bw, bh);

        g.setColor(new Color(35, 35, 50));
        g.fillRect(bx + bw / 2 - 90, by - bh - 120, 180, 120);
        g.fillRect(bx + bw / 2 - 20, by - bh - 260, 40, 140);

        g.setColor(new Color(255, 200, 90));
        g.fillOval(bx + bw / 2 - 20, by - bh - 300, 40, 40);

        g.setColor(new Color(90, 90, 110));
        for (int i = 0; i < 3; i++) {
            g.fillRect(bx + bw / 2 - 130 + i * 130, by - bh + 30, 90, 100);
        }
        g.setColor(base.darker().darker());

        g.setColor(new Color(60, 60, 80));
        g.fillRoundRect(bx, by - bh + 160, bw, 120, 10, 10);
        g.setColor(new Color(240, 240, 230));
        int cw = 120;
        for (int i = 0; i < 5; i++) {
            g.fillRect(bx + 20 + i * (cw + 70), by - bh + 190, cw, 60);
        }
    }

    private static void drawIntroCard(Graphics2D g) {
        g.setColor(new Color(20, 20, 40, 235));
        g.fillRoundRect(90, 620, W - 180, 640, 60, 60);

        g.setColor(new Color(255, 200, 60));
        int s = 260;
        g.fillOval((W - s) / 2, 340, s, s);
        g.setColor(new Color(40, 40, 40));
        g.fillRoundRect((W - s) / 2 - 30, 340, s + 60, 60, 30, 30);
        g.fillRoundRect((W - s) / 2 + 40, 380, s - 80, 120, 40, 40);

        String t1 = "CONSTRUYENDO";
        String t2 = "VALORES";
        g.setFont(new Font("Arial", Font.BOLD, 96));
        g.setColor(Color.WHITE);
        FontMetrics fm = g.getFontMetrics();
        int w1 = fm.stringWidth(t1);
        g.drawString(t1, (W - w1) / 2, 860);
        g.setFont(new Font("Arial", Font.BOLD, 150));
        fm = g.getFontMetrics();
        int w2 = fm.stringWidth(t2);
        g.setColor(new Color(255, 235, 130));
        g.drawString(t2, (W - w2) / 2, 1030);

        g.setFont(new Font("Arial", Font.BOLD, 42));
        fm = g.getFontMetrics();
        String tag = "Aprendemos valores mientras construimos";
        int wt = fm.stringWidth(tag);
        g.setColor(new Color(220, 220, 230));
        g.drawString(tag, (W - wt) / 2, 1160);
    }

    private static void drawText(Graphics2D g, String title, int idx, String character) {
        g.setColor(new Color(255, 255, 255, 235));
        g.setFont(new Font("Arial", Font.BOLD, 88));
        g.drawString("CONSTRUYENDO VALORES", 60, 140);

        g.setFont(new Font("Arial", Font.BOLD, 40));
        g.setColor(new Color(255, 255, 255, 210));
        g.drawString("Escena " + idx, 60, 210);

        if (title != null && !title.isBlank()) {
            g.setFont(new Font("Arial", Font.BOLD, 64));
            FontMetrics fm = g.getFontMetrics();
            String t = title.length() > 26 ? title.substring(0, 26) : title;
            int tw = fm.stringWidth(t);
            g.setColor(new Color(20, 20, 40, 200));
            g.fillRoundRect((W - tw) / 2 - 40, 320, tw + 80, 110, 24, 24);
            g.setColor(Color.WHITE);
            g.drawString(t, (W - tw) / 2, 395);
        }

        if (character != null && !character.isBlank()) {
            g.setFont(new Font("Arial", Font.BOLD, 52));
            g.setColor(new Color(20, 20, 40, 210));
            String who = "Dice " + character;
            FontMetrics fm = g.getFontMetrics();
            int tw = fm.stringWidth(who);
            g.fillRoundRect((W - tw) / 2 - 30, 470, tw + 60, 90, 20, 20);
            g.setColor(Color.WHITE);
            g.drawString(who, (W - tw) / 2, 532);
        }
    }

    private SceneImage() {}
}