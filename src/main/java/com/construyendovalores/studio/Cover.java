package com.construyendovalores.studio;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Crea miniaturas (portadas) para YouTube, 1280x720, con variantes según la estrategia. */
public final class Cover {

    private static final int W = 1280;
    private static final int H = 720;

    public static Path make(String title, String channel, Path workDir) {
        try {
            return makeVariant(title, channel, workDir, 1);
        } catch (Exception e) {
            throw new RuntimeException("No se pudo guardar la portada", e);
        }
    }

    /** 3 miniaturas distintas por video: colores, composición, personaje e icono. */
    public static List<Path> makeVariants(String title, String channel, Path workDir) throws Exception {
        List<Path> outs = new ArrayList<>();
        for (int v = 1; v <= 3; v++) {
            outs.add(makeVariant(title, channel, workDir, v));
        }
        return outs;
    }

    private static Path makeVariant(String title, String channel, Path workDir, int variant) throws Exception {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        Color[] pair = colorsOf(variant);
        String badge = badgeOf(variant);
        String charName = characterOf(variant);
        Color base = SceneImage.colorFor(charName);

        GradientPaint bg = new GradientPaint(0, 0, pair[0], W, H, pair[1]);
        g.setPaint(bg);
        g.fillRect(0, 0, W, H);

        g.setColor(new Color(255, 240, 150, 210));
        g.fillOval(W - 300, 56, 190, 190);
        g.setColor(new Color(255, 255, 255, 200));
        for (int i = 0; i < 3; i++) {
            g.fillOval((int) (i * 420), 90 + i * 60, 120, 58);
            g.fillOval((int) (i * 420 + 62), 62 + i * 60, 100, 66);
            g.fillOval((int) (i * 420 + 130), 90 + i * 60, 112, 54);
        }

        g.setColor(new Color(15, 15, 35, 90));
        g.fillRoundRect(50, H - 180, 860, 120, 34, 34);

        // texto grande y de alto contraste (sombra + relleno vivo)
        g.setFont(new Font("Arial", Font.BOLD, 86));
        g.setColor(new Color(15, 15, 35));
        drawMultiline(g, title, 96, 320, W - 520, H - 200);
        g.setColor(Color.WHITE);
        drawMultiline(g, title, 90, 316, W - 520, H - 200);

        // personaje en la derecha (busto, recortado por el borde inferior)
        Actor.draw(g, W - 250, 560, 0.55, 1.35, base, Actor.moodFor(0), true, Actor.designFor(charName));

        // icono/palabra de la variante
        g.setColor(new Color(0, 0, 0, 150));
        g.fillOval(W - 250, 40, 130, 130);
        g.setColor(new Color(255, 214, 60));
        g.fillOval(W - 242, 48, 114, 114);
        g.setColor(new Color(30, 20, 0));
        g.setFont(new Font("Arial", Font.BOLD, 44));
        drawWrapped(g, badge, W - 242, 100, W, 40);

        // franja con nombre del canal
        String sub = channel == null || channel.isBlank() ? "CONSTRUYENDO VALORES" : channel;
        g.setFont(new Font("Arial", Font.BOLD, 34));
        FontMetrics fm = g.getFontMetrics();
        int tw = fm.stringWidth(sub);
        g.setColor(new Color(15, 15, 35, 220));
        g.fillRoundRect((W - tw) / 2 - 26, H - 78, tw + 52, 56, 20, 20);
        g.setColor(new Color(255, 235, 130));
        g.drawString(sub, (W - tw) / 2, H - 36);

        g.dispose();
        Files.createDirectories(workDir);
        Path out = workDir.resolve("miniatura_" + variant + ".png");
        ImageIO.write(img, "png", out.toFile());
        return out;
    }

    private static Color[] colorsOf(int variant) {
        return switch (variant) {
            case 2 -> new Color[]{new Color(24, 26, 58), new Color(140, 70, 210)};
            case 3 -> new Color[]{new Color(0, 150, 120), new Color(60, 145, 60)};
            default -> new Color[]{new Color(255, 176, 60), new Color(233, 66, 53)};
        };
    }

    private static String badgeOf(int variant) {
        return switch (variant) {
            case 2 -> "VALORES";
            case 3 -> "JUEGA";
            default -> "CONSTRUYE";
        };
    }

    private static String characterOf(int variant) {
        return switch (variant) {
            case 2 -> "Luna";
            case 3 -> "Pipo";
            default -> "Casquito";
        };
    }

    private static void drawMultiline(Graphics2D g, String s, int x, int baseline, int maxW, int maxH) {
        if (s == null || s.isBlank()) {
            g.drawString("¡Nuevo video!", x, baseline);
            return;
        }
        String[] words = s.trim().split("\\s+");
        StringBuilder line = new StringBuilder();
        int y = baseline;
        FontMetrics fm = g.getFontMetrics();
        for (String word : words) {
            String test = line.isEmpty() ? word : line + " " + word;
            if (fm.stringWidth(test) > maxW && !line.isEmpty()) {
                g.drawString(line.toString(), x, y);
                y += fm.getHeight();
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(test);
            }
            if (y > maxH - 40) break;
        }
        if (!line.isEmpty()) g.drawString(line.toString(), x, y);
    }

    private static void drawWrapped(Graphics2D g, String s, int cx, int cy, int w, int maxW) {
        FontMetrics fm = g.getFontMetrics();
        int tw = fm.stringWidth(s);
        if (tw > maxW - 20) {
            s = "…";
            tw = fm.stringWidth(s);
        }
        g.drawString(s, cx + (110 - tw) / 2, cy);
    }

    private Cover() {}
}