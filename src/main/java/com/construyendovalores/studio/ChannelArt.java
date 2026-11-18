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
import java.nio.file.Files;
import java.nio.file.Path;

/** Crea el logo/avatar y el banner oficial del canal (estilo coherente con los videos). */
public final class ChannelArt {

    public static Path makeAvatar(Path outDir) throws Exception {
        int W = 800;
        int H = 800;
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        GradientPaint bg = new GradientPaint(0, 0, new Color(24, 30, 70), W, H, new Color(214, 90, 60));
        g.setPaint(bg);
        g.fillRect(0, 0, W, H);

        g.setColor(new Color(20, 20, 40, 120));
        g.fillOval(60, 60, W - 120, H - 120);

        g.setColor(new Color(255, 255, 255));
        g.fillOval(130, 110, W - 260, H - 260);

        Actor.draw(g, W / 2, H - 60, 0.55, 2.4, SceneImage.colorFor("Casquito"),
                Actor.moodFor(2), true, Actor.designFor("Casquito"));

        g.setFont(new Font("Arial", Font.BOLD, 44));
        FontMetrics fm = g.getFontMetrics();
        String s = "CONSTRUYENDO VALORES";
        int tw = fm.stringWidth(s);
        g.setColor(new Color(255, 235, 130));
        g.fillRoundRect((W - tw) / 2 - 34, H - 132, tw + 68, 70, 26, 26);
        g.setColor(new Color(20, 20, 40));
        g.drawString(s, (W - tw) / 2, H - 82);

        g.dispose();
        Path out = outDir.resolve("avatar_canal.png");
        Files.createDirectories(outDir);
        ImageIO.write(img, "png", out.toFile());
        return out;
    }

    public static Path makeBanner(Path outDir, String slogan) throws Exception {
        int W = 2560;
        int H = 1440;
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        GradientPaint sky = new GradientPaint(0, 0, new Color(80, 150, 220), 0, H, new Color(20, 26, 60));
        g.setPaint(sky);
        g.fillRect(0, 0, W, H);

        g.setColor(new Color(255, 240, 150, 200));
        g.fillOval(W - 420, 90, 260, 260);

        g.setColor(new Color(255, 255, 255, 200));
        g.fillOval(300, 140, 220, 110);
        g.fillOval(480, 100, 200, 130);
        g.fillOval(700, 160, 240, 100);
        g.fillOval(1700, 90, 220, 110);
        g.fillOval(1950, 130, 240, 110);

        g.setColor(new Color(38, 42, 66));
        g.fillRect(0, H - 620, W, 620);

        Scenario.OBRA.paint(g, W, H, 3, 1.0);

        g.setColor(new Color(20, 20, 40, 200));
        g.fillRoundRect(W / 2 - 760, 260, 1520, 330, 70, 70);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 150));
        g.drawString("CONSTRUYENDO VALORES", W / 2 - 640, 440);

        g.setFont(new Font("Arial", Font.BOLD, 64));
        g.setColor(new Color(255, 235, 130));
        String sl = slogan == null || slogan.isBlank()
                ? "Cuentos y canciones para aprender valores"
                : slogan;
        FontMetrics fm = g.getFontMetrics();
        int tw = fm.stringWidth(sl);
        g.drawString(sl, (W - tw) / 2, 548);

        Actor.draw(g, 520, H - 120, 0.35, 2.6, SceneImage.colorFor("Casquito"), Actor.moodFor(2), true, Actor.designFor("Casquito"));
        Actor.draw(g, 980, H - 120, 0.85, 2.6, SceneImage.colorFor("Luna"), Actor.moodFor(1), false, Actor.designFor("Luna"));
        Actor.draw(g, 1450, H - 120, 0.15, 2.6, SceneImage.colorFor("Tico"), Actor.moodFor(0), false, Actor.designFor("Tico"));
        Actor.draw(g, 1900, H - 120, 0.6, 2.6, SceneImage.colorFor("Pipo"), Actor.moodFor(0), true, Actor.designFor("Pipo"));
        Actor.draw(g, 2290, H - 120, 0.45, 2.6, SceneImage.colorFor("Coco"), Actor.moodFor(0), true, Actor.designFor("Coco"));

        g.dispose();
        Path out = outDir.resolve("banner_canal.png");
        Files.createDirectories(outDir);
        ImageIO.write(img, "png", out.toFile());
        return out;
    }

    private ChannelArt() {}
}