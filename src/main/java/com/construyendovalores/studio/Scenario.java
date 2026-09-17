package com.construyendovalores.studio;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

/**
 * Variantes de ambientación para cada video: Obra (por defecto), Parque y Taller.
 * Cada una dibuja su propio suelo, fondos y atrezzo temático.
 */
public enum Scenario {

    OBRA("obra", "Obra de construcción"),
    PARQUE("parque", "Parque"),
    TALLER("taller", "Taller");

    public final String id;
    public final String label;

    Scenario(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public static Scenario fromId(String s) {
        if (s != null) {
            String n = s.trim().toLowerCase().replace('\u00e1', 'a');
            for (Scenario v : values()) {
                if (v.id.equals(n) || v.name().equalsIgnoreCase(s)) return v;
            }
        }
        return OBRA;
    }

    public void paint(Graphics2D g, int W, int H, int seed, double t) {
        switch (this) {
            case PARQUE -> paintParque(g, W, H, seed, t);
            case TALLER -> paintTaller(g, W, H, seed, t);
            default -> paintObra(g, W, H, seed, t);
        }
    }

    private static void paintObra(Graphics2D g, int W, int H, int seed, double t) {
        g.setColor(new Color(122, 89, 56));
        g.fillRect(0, H - 430, W, 430);
        g.setColor(new Color(140, 104, 66));
        g.fillRect(0, H - 430, W, 26);

        g.setColor(new Color(200, 170, 120));
        for (int i = 0; i < 4; i++) {
            int x = (seed * 47 + i * 173) % W;
            g.fillRect(x, H - 410, 30, 14);
        }

        g.setColor(new Color(90, 60, 35));
        g.fillRect(40, H - 150, 150, 150);
        g.setColor(new Color(70, 45, 25));
        for (int i = 0; i < 3; i++) {
            g.fillRect(55 + i * 42, H - 96, 34, 26);
        }

        g.setColor(new Color(150, 55, 45));
        g.fillRect(W - 190, H - 120, 150, 120);
        g.setColor(new Color(230, 225, 210));
        g.fillRect(W - 175, H - 102, 120, 18);
        g.fillRect(W - 175, H - 58, 120, 18);
        g.fillRect(W - 156, H - 102, 16, 102);

        drawSiteBuilding(g, W, H, Color.WHITE, Color.BLACK, seed);

        g.setColor(new Color(230, 140, 60));
        int swing = (int) (Math.sin(2 * Math.PI * 0.22 * t) * 26);
        g.setStroke(new BasicStroke(5f));
        g.drawLine(W / 2 + 130, H - 660, W / 2 + 250 + swing, H - 520);
        g.fillRoundRect(W / 2 + 250 + swing - 20, H - 520, 40, 26, 10, 10);
    }

    private static void paintParque(Graphics2D g, int W, int H, int seed, double t) {
        g.setColor(new Color(86, 158, 74));
        g.fillRect(0, H - 430, W, 430);
        g.setColor(new Color(108, 180, 86));
        g.fillRect(0, H - 430, W, 24);

        g.setColor(new Color(245, 240, 220));
        for (int i = 0; i < 8; i++) {
            int x = (i * 83 + seed * 13) % W;
            g.fillOval(x, H - 390, 22, 8);
        }
        g.setColor(new Color(240, 130, 160));
        for (int i = 0; i < 7; i++) {
            int x = (i * 103 + 40) % W;
            g.fillRoundRect(x, H - 386, 8, 18, 6, 6);
            g.fillOval(x - 5, H - 392, 18, 18);
        }

        drawTree(g, 70, H - 430, 0);
        drawTree(g, W - 120, H - 430, 40);

        drawSlide(g, W, H);

        g.setColor(new Color(228, 205, 160));
        g.fillRoundRect(W / 2 - 160, H - 150, 320, 150, 16, 16);
        g.setColor(new Color(160, 130, 90));
        g.fillRoundRect(W / 2 - 160, H - 40, 320, 40, 8, 8);
    }

    private static void paintTaller(Graphics2D g, int W, int H, int seed, double t) {
        g.setColor(new Color(112, 82, 58));
        g.fillRect(0, H - 430, W, 430);
        g.setColor(new Color(140, 105, 76));
        g.fillRect(0, H - 430, W, 22);

        g.setColor(new Color(70, 52, 38));
        g.fillRoundRect(40, 520, 170, 150, 12, 12);
        g.setColor(new Color(160, 124, 90));
        for (int i = 0; i < 3; i++) {
            g.fillRect(52 + i * 50, 532, 36, 40);
        }
        g.setColor(new Color(190, 150, 110));
        for (int i = 0; i < 3; i++) {
            g.fillRect(52 + i * 50, 582, 36, 46);
        }

        drawPegboard(g, W - 260, 500);

        drawWorkbench(g, W, H, t);

        g.setColor(new Color(255, 220, 130, 180));
        g.fillOval(W / 2 - 180, 60, 60, 60);
        g.setStroke(new BasicStroke(4f));
        g.drawLine(W / 2 - 150, 90, W / 2 - 60, 260);
        g.fillOval(W / 2 - 70, 250, 46, 46);
    }

    private static void drawSiteBuilding(Graphics2D g, int W, int H, Color wall, Color dark, int seed) {
        int bw = 360;
        int bx = (W - bw) / 2;
        int bh = 350;
        int by = H - 430;
        g.setColor(wall);
        g.fillRect(bx, by - bh, bw, bh);
        g.setColor(dark);
        g.fillRect(bx + bw / 2 - 30, by - bh - 60, 60, 60);

        g.setColor(new Color(120, 120, 140));
        for (int i = 0; i < 3; i++) {
            g.fillRect(bx + 24 + i * 112, by - bh + 30, 80, 80);
        }
        g.setColor(dark);

        g.setColor(new Color(80, 80, 100));
        for (int i = 0; i < 3; i++) {
            g.fillRect(bx + 16 + i * 112, by - bh - 18, 12, by - (by - bh));
        }
        for (int i = 0; i < 3; i++) {
            g.fillRect(bx - 6 + i * 56, by - bh + 20, 60, 10);
            g.fillRect(bx - 6 + i * 56, by - bh + 60, 60, 10);
            g.fillRect(bx - 6 + i * 56, by - bh + 100, 60, 10);
        }
    }

    private static void drawTree(Graphics2D g, int x, int baseY, int f) {
        g.setColor(new Color(120, 84, 52));
        g.fillRoundRect(x + 28, baseY - 130, 24, 130, 10, 10);
        g.setColor(new Color(58, 134, 74));
        g.fillOval(x + 6, baseY - 180, 68, 90);
        g.fillOval(x + 24, baseY - 220, 52, 80);
        g.fillOval(x + 40, baseY - 180, 58, 84);
    }

    private static void drawSlide(Graphics2D g, int W, int H) {
        g.setColor(new Color(70, 90, 140));
        int x = W - 330;
        int y = H - 300;
        g.setStroke(new BasicStroke(18f));
        g.drawArc(x, y, 240, 260, 0, -100);
        g.setColor(new Color(100, 130, 190));
        g.setStroke(new BasicStroke(20f));
        g.drawArc(x, y, 240, 260, 0, -100);
        g.setColor(new Color(60, 70, 110));
        g.setStroke(new BasicStroke(10f));
        g.drawLine(x + 40, y + 22, x + 22, y - 60);
        g.drawLine(x + 40, y + 22, x + 96, y - 4);
        g.setColor(new Color(220, 170, 90));
        g.fillRoundRect(x+ 8, y - 70, 90, 14, 8, 8);
    }

    private static void drawPegboard(Graphics2D g, int x, int y) {
        g.setColor(new Color(160, 130, 95));
        g.fillRoundRect(x, y, 180, 200, 12, 12);
        g.setColor(new Color(200, 175, 135));
        g.drawRect(x + 10, y + 10, 160, 180);
        g.setColor(new Color(120, 95, 65));
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 3; j++) {
                g.fillOval(x + 16 + i * 46, y + 18 + j * 60, 10, 10);
            }
        }
        g.setColor(new Color(230, 230, 235));
        g.fillRoundRect(x + 24, y + 22, 26, 90, 8, 8);
        g.fillRoundRect(x + 66, y + 34, 22, 120, 8, 8);
        g.fillRoundRect(x + 104, y + 42, 34, 90, 10, 10);
        g.fillRoundRect(x + 140, y + 36, 10, 110, 6, 6);
    }

    private static void drawWorkbench(Graphics2D g, int W, int H, double t) {
        int bx = W / 2 - 190;
        int by = H - 150;
        g.setColor(new Color(150, 105, 70));
        g.fillRoundRect(bx - 20, by - 60, 420, 60, 10, 10);
        g.setColor(new Color(90, 62, 42));
        g.fillRoundRect(bx - 10, by, 26, 150, 8, 8);
        g.fillRoundRect(bx + 170, by - 60, 26, 150, 8, 8);

        g.setColor(new Color(110, 78, 56));
        g.fillRoundRect(bx + 40, by - 132, 120, 70, 10, 10);
        g.setColor(new Color(180, 140, 100));
        g.fillRect(bx + 44, by - 132, 112, 12);

        g.setColor(new Color(200, 200, 210));
        double saw = Math.sin(2 * Math.PI * 1.4 * t) * 0.4;
        g.translate(bx + 210, by - 110);
        g.rotate(saw);
        g.fillRoundRect(-4, -90, 8, 90, 4, 4);
        g.setColor(new Color(160, 160, 170));
        g.fillRoundRect(-34, -100, 68, 16, 8, 8);
        g.rotate(-saw);
        g.translate(-(bx + 210), -(by - 110));

        g.setColor(new Color(232, 232, 220));
        g.fillRoundRect(bx + 290, by - 40, 90, 30, 8, 8);
        g.fillRoundRect(bx + 290, by - 130, 90, 30, 8, 8);
    }
}