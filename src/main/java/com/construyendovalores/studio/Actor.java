package com.construyendovalores.studio;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.geom.Path2D;
import java.util.Locale;

/**
 * Personajes de construccion con look profesional tipo animacion 2D/3D:
 * proporciones chibi, sombreado radial con volumen, contorno "tinta",
 * iris con brillos, sonrojo y accesorios por especie. Se dibujan en
 * funcion de t para animarse: rebote, parpadeo, balanceo y saludo.
 *
 * Especies (10): mascota/maestro, caracol (Casquito), excavadora (Tito),
 * pata (Sofia), mono (Nico), conejita (Luna), perrito (Pipo), tortuga
 * (Tico), camion volquete (Bruno), buho (Memo) y guacamaya (Coco).
 */
final class Actor {

    private static final int MOOD_HAPPY = 0;
    private static final int MOOD_SURPRISED = 1;
    private static final int MOOD_DETERMINED = 2;

    private static final Color INK = new Color(54, 38, 34);

    static final String DESIGN_MASCOT = "mascot";
    static final String DESIGN_CARACOL = "caracol";
    static final String DESIGN_EXCAVADORA = "excavadora";
    static final String DESIGN_PATA = "pata";
    static final String DESIGN_MONO = "mono";
    static final String DESIGN_LUNA = "conejita";
    static final String DESIGN_PIPO = "perrito";
    static final String DESIGN_TICO = "tortuga";
    static final String DESIGN_BRUNO = "camion";
    static final String DESIGN_MEMO = "buho";
    static final String DESIGN_COCO = "guacamaya";

    /** Energía de movimiento global (calidad.movimiento): 1.0 = base, +10% diario. */
    static volatile double MOV = 1.0;

    static void configuraMovimiento(double v) {
        MOV = Math.max(0.7, Math.min(1.8, v));
    }

    static void draw(Graphics2D g, int cx, int bottomY, double t, double scale, Color accent, int mood, boolean waving) {
        draw(g, cx, bottomY, t, scale, accent, mood, waving, DESIGN_MASCOT);
    }

    static void draw(Graphics2D g, int cx, int bottomY, double t, double scale, Color accent, int mood, boolean waving, String design) {
        double bob = Math.sin(2 * Math.PI * 1.15 * t);
        int yOff = (int) (bob * 22 * scale * MOV);
        double tilt = 0.05 * Math.sin(2 * Math.PI * 0.45 * t) * MOV;
        boolean blink = (t % 1.9) < 0.10;

        g.translate(cx, bottomY - yOff);

        if (DESIGN_EXCAVADORA.equals(design)) {
            drawExcavadora(g, scale, accent, mood, blink, t, waveAng(t, waving));
        } else if (DESIGN_BRUNO.equals(design)) {
            drawCamion(g, scale, accent, mood, blink, t);
        } else {
            drawChibi(g, scale, accent, mood, blink, t, waving, tilt, design);
        }

        g.translate(-cx, -(bottomY - yOff));
    }

    // ============================================================ CHIBI

    private static void drawChibi(Graphics2D g, double scale, Color accent, int mood,
                                  boolean blink, double t, boolean waving, double tilt, String design) {
        int w = (int) (90 * scale);
        int h = (int) (210 * scale);
        double sw = Math.max(2.4, 3.2 * scale);

        drawShadow(g, w, (int) (40 * scale));
        drawLegs(g, w, h, sw);
        drawBackExtras(g, w, h, t, accent, design, sw);
        drawArmBack(g, w, h, t, accent, sw);
        if (DESIGN_TICO.equals(design)) {
            drawShell(g, w, h, accent, sw);
        } else {
            drawTorso(g, w, h, accent, mood, design, sw);
        }
        drawHead(g, w, h, tilt, blink, accent, mood, design, sw);
        drawArmFront(g, w, h, t, accent, mood, waving, design, sw);
    }

    private static void drawShadow(Graphics2D g, int w, int sh) {
        g.setColor(new Color(30, 24, 44, 70));
        g.fillOval((int) (-w * 1.25), (int) (210 * w / 90.0 - 46 * w / 90.0), (int) (w * 2.5), sh);
        g.setColor(new Color(255, 255, 255, 60));
        g.fillOval((int) (-w * 0.95), (int) (210 * w / 90.0 - 60 * w / 90.0), (int) (w * 1.55), (int) (sh * 0.55));
    }

    private static void drawLegs(Graphics2D g, int w, int h, double sw) {
        int legW = (int) (w * 0.40);
        int legH = (int) (h * 0.20);
        int y = (int) (h * 0.80);
        for (int side = -1; side <= 1; side += 2) {
            int x = side > 0 ? (int) (w * 0.06) : (int) (-w * 0.46);
            ink(g, x, y, legW, legH, legH / 2, sw * 0.9);
            shadeOval(g, x, y, legW, legH, side < 0 ? fur(1) : fur(1));
        }
    }

    private static void ink(Graphics2D g, int x, int y, int w, int h, int arc, double sw) {
        g.setColor(INK);
        g.fillRoundRect(x - (int) sw, y - (int) sw, w + (int) sw * 2, h + (int) sw * 2, arc, arc);
    }

    private static void inkOval(Graphics2D g, int x, int y, int w, int h, double sw) {
        g.setColor(INK);
        g.fillOval(x - (int) sw, y - (int) sw, w + (int) sw * 2, h + (int) sw * 2);
    }

    /** Sombreado con volumen: luz arriba-izquierda, sombra abajo-derecha. */
    private static void shadeOval(Graphics2D g, int x, int y, int w, int h, Color base) {
        RadialGradientPaint p = new RadialGradientPaint(
                x + w * 0.40f, y + h * 0.26f, Math.max(w, h) * 0.82f,
                new float[]{0f, 0.55f, 1f},
                new Color[]{lighten(base, 0.48f), base, darken(base, 0.38f)});
        g.setPaint(p);
        g.fillOval(x, y, w, h);
    }

    private static void shadeRound(Graphics2D g, int x, int y, int w, int h, int arc, Color base) {
        RadialGradientPaint p = new RadialGradientPaint(
                x + w * 0.38f, y + h * 0.25f, Math.max(w, h) * 0.85f,
                new float[]{0f, 0.55f, 1f},
                new Color[]{lighten(base, 0.45f), base, darken(base, 0.36f)});
        g.setPaint(p);
        g.fillRoundRect(x, y, w, h, arc, arc);
    }

    /** Personaje ropa/cuerpo: color de base según la especie. */
    private static Color fur(int kind) {
        return switch (kind) {
            case 0 -> new Color(255, 234, 205);   // mascota maestro
            case 1 -> new Color(255, 234, 205);   // patas/brazos
            case 2 -> new Color(246, 233, 255);   // conejita
            case 3 -> new Color(238, 196, 150);   // perrito
            case 4 -> new Color(172, 224, 160);   // caracol
            case 5 -> new Color(200, 152, 112);   // mono
            case 6 -> new Color(152, 208, 142);   // tortuga
            case 7 -> new Color(193, 150, 108);   // buho
            default -> new Color(255, 150, 92);   // guacamaya
        };
    }

    private static void drawTorso(Graphics2D g, int w, int h, Color accent, int mood, String design, double sw) {
        int tx = (int) (-w * 0.72);
        int ty = (int) (h * 0.18);
        int tw = (int) (w * 1.44);
        int th = (int) (h * 0.60);
        ink(g, tx, ty, tw, th, (int) (w * 0.30), sw);
        shadeRound(g, tx, ty, tw, th, (int) (w * 0.30), torsoBase(design));

        // parche de barriga mas claro
        int by = (int) (ty + th * 0.20);
        g.setColor(new Color(255, 255, 255, 120));
        g.fillOval((int) (tx + tw * 0.22), by, (int) (tw * 0.56), (int) (th * 0.55));

        // cinturon / cinta de obra
        g.setColor(accent);
        g.fillRoundRect((int) (tx + tw * 0.08), (int) (ty + th * 0.68), (int) (tw * 0.84), (int) (w * 0.18), 10, 10);
        g.setColor(new Color(255, 255, 255, 90));
        g.fillRect((int) (tx + tw * 0.08), (int) (ty + th * 0.72), (int) (tw * 0.84), 4);
        g.setColor(Color.WHITE);
        g.fillOval((int) (tx + tw * 0.42), (int) (ty + th * 0.62), (int) (w * 0.16), (int) (w * 0.16));

        if (DESIGN_LUNA.equals(design)) {
            drawStar(g, 0, ty + th * 0.30, w * 0.46, accent);
        } else if (DESIGN_PIPO.equals(design)) {
            g.setColor(new Color(255, 130, 160));
            g.fillOval((int) (w * 0.66), ty + (int) (th * 0.28), (int) (w * 0.16), (int) (w * 0.16));
        }
    }

    private static Color torsoBase(String design) {
        return switch (design) {
            case DESIGN_PATA -> new Color(255, 214, 110);
            case DESIGN_LUNA -> new Color(226, 208, 255);
            case DESIGN_MONO -> new Color(185, 138, 96);
            case DESIGN_CARACOL -> new Color(160, 214, 145);
            case DESIGN_MEMO -> new Color(150, 118, 84);
            case DESIGN_COCO -> new Color(233, 92, 70);
            default -> new Color(255, 224, 178);
        };
    }

    /** Rasgos traseros segun especie: cola, concha o plumaje. */
    private static void drawBackExtras(Graphics2D g, int w, int h, double t, Color accent, String design, double sw) {
        if (DESIGN_TICO.equals(design)) {
            return; // la concha se dibuja como cuerpo
        }
        if (DESIGN_PIPO.equals(design)) {
            double wag = Math.sin(2 * Math.PI * 6 * t);
            int tx = (int) (-w * 1.10);
            int ty = (int) (h * 0.30);
            inkOval(g, tx, ty, (int) (w * 0.62), (int) (w * 0.62), sw * 0.8);
            shadeOval(g, tx, ty, (int) (w * 0.62), (int) (w * 0.62), new Color(205, 138, 84));
        } else if (DESIGN_LUNA.equals(design)) {
            int tx = (int) (-w * 1.05);
            int ty = (int) (h * 0.26);
            inkOval(g, tx, ty, (int) (w * 0.62), (int) (w * 0.62), sw * 0.8);
            shadeOval(g, tx, ty, (int) (w * 0.62), (int) (w * 0.62), Color.WHITE);
        } else if (DESIGN_MONO.equals(design)) {
            double curl = Math.sin(2 * Math.PI * 1.1 * t + 1) * w * 0.10;
            g.setStroke(new BasicStroke((float) Math.max(3, w * 0.09)));
            g.setColor(new Color(160, 104, 62));
            Path2D.Double p = new Path2D.Double();
            p.moveTo(-w * 0.66, -h * 0.10);
            p.curveTo(-w * 1.35, -h * 0.30 + curl, -w * 1.45, -h * 0.08 + curl, -w * 1.18, h * 0.12 + curl * 0.5);
            g.draw(p);
        } else if (DESIGN_CARACOL.equals(design)) {
            int sxx = (int) (-w * 0.94);
            int syy = (int) (-h * 0.10);
            int srr = (int) (w * 0.44);
            inkOval(g, sxx - srr, syy - srr, srr * 2, srr * 2, sw * 0.8);
            shadeOval(g, sxx - srr, syy - srr, srr * 2, srr * 2, new Color(222, 138, 60));
            g.setColor(new Color(120, 66, 30));
            g.setStroke(new BasicStroke((float) Math.max(2.2, w * 0.06)));
            g.drawArc(sxx - srr / 2, syy - srr / 2, srr, srr, 30, 300);
        } else if (DESIGN_COCO.equals(design)) {
            for (int i = -1; i <= 1; i++) {
                int lx = (int) (i * w * 0.16) - (int) (w * 0.20);
                int ly = (int) (h * 0.02) + i * (int) (w * 0.06);
                ink(g, lx, ly, (int) (w * 0.42), (int) (h * 0.52), 18, sw * 0.7);
                shadeRound(g, lx, ly, (int) (w * 0.42), (int) (h * 0.52), 18,
                        i < 0 ? new Color(255, 214, 70) : i > 0 ? new Color(56, 130, 235) : new Color(233, 92, 70));
            }
        }
    }

    /** Concha de tortuga que hace de cuerpo de Tico. */
    private static void drawShell(Graphics2D g, int w, int h, Color accent, double sw) {
        int sx = (int) (-w * 0.98);
        int sy = (int) (-h * 0.34);
        int swW = (int) (w * 1.96);
        int swH = (int) (h * 1.14);
        ink(g, sx, sy, swW, swH, (int) (w * 0.5), sw);
        RadialGradientPaint p = new RadialGradientPaint(
                sx + swW * 0.42f, sy + swH * 0.22f, swW * 0.82f,
                new float[]{0f, 0.55f, 1f},
                new Color[]{lighten(accent, 0.45f), accent, darken(accent, 0.40f)});
        g.setPaint(p);
        g.fillRoundRect(sx, sy, swW, swH, (int) (w * 0.5), (int) (w * 0.5));

        g.setColor(new Color(255, 255, 255, 90));
        g.fillOval((int) (sx + swW * 0.16), (int) (sy + swH * 0.14), (int) (swW * 0.26), (int) (swH * 0.30));
        g.setColor(new Color(60, 40, 30, 90));
        int[][] pts = {{0, (int) (sy - swH * 0.05)}, {0, (int) (sy + swH * 0.45)}};
        for (int[] p2 : pts) {
            g.drawLine((int) (sx + swW * 0.30), p2[1], (int) (sx + swW * 0.70), p2[1]);
        }
    }

    private static void drawHead(Graphics2D g, int w, int h, double tilt, boolean blink,
                                 Color accent, int mood, String design, double sw) {
        int r = (int) (w * 0.94);
        int cy = (int) (-h * 0.30);

        g.rotate(tilt, 0, cy);

        boolean hats = DESIGN_MASCOT.equals(design) || DESIGN_CARACOL.equals(design)
                || DESIGN_PATA.equals(design) || DESIGN_MONO.equals(design);

        if (DESIGN_MONO.equals(design)) {
            drawMonoEars(g, r, cy, sw);
        } else if (DESIGN_LUNA.equals(design)) {
            drawRabbitEars(g, r, cy, tint(DESIGN_LUNA), sw);
        } else if (DESIGN_PIPO.equals(design)) {
            drawDogEars(g, r, cy, new Color(205, 138, 84), sw);
        } else if (DESIGN_MEMO.equals(design)) {
            drawBuhoTuffs(g, r, cy, sw);
        } else if (DESIGN_COCO.equals(design)) {
            drawCocoCrest(g, r, cy, sw);
        }

        inkOval(g, -r, cy - r, r * 2, r * 2, sw);
        shadeOval(g, -r, cy - r, r * 2, r * 2, headFur(design));

        if (DESIGN_COCO.equals(design)) {
            g.setColor(new Color(46, 120, 220));
            g.fillArc(-r, cy - r, r * 2, r * 2, 150, 130);
            g.setColor(new Color(220, 175, 60));
            g.fillRect((int) (-r * 0.62), cy - (int) (r * 0.42), 18, (int) (r * 0.9));
            g.fillRect((int) (r * 0.62), cy - (int) (r * 0.42), 18, (int) (r * 0.9));
        }

        if (hats) {
            drawHat(g, w, h, r, cy, accent, sw);
        }

        drawFace(g, r, cy, w, mood, blink, design, sw);
        g.rotate(-tilt, 0, cy);
    }

    private static Color headFur(String design) {
        return switch (design) {
            case DESIGN_MASCOT -> new Color(255, 234, 205);
            case DESIGN_PATA -> new Color(255, 214, 110);
            case DESIGN_LUNA -> new Color(240, 228, 255);
            case DESIGN_PIPO -> new Color(240, 200, 156);
            case DESIGN_CARACOL -> new Color(168, 222, 152);
            case DESIGN_MONO -> new Color(200, 152, 112);
            case DESIGN_TICO -> new Color(152, 208, 142);
            case DESIGN_MEMO -> new Color(188, 145, 104);
            case DESIGN_COCO -> new Color(255, 234, 205);
            default -> new Color(255, 226, 190);
        };
    }

    private static Color tint(String design) {
        return DESIGN_LUNA.equals(design) ? new Color(230, 190, 255) : new Color(255, 190, 220);
    }

    private static void drawFace(Graphics2D g, int r, int cy, int w, int mood, boolean blink, String design, double sw) {
        int eyeY = cy - (int) (r * 0.02);
        for (int side = -1; side <= 1; side += 2) {
            int cx = side * (int) (r * 0.40);
            int ew = (int) (r * 0.30);
            int eh = (int) (r * 0.40);
            g.setColor(new Color(255, 255, 255));
            g.fillOval(cx - ew / 2, eyeY - eh / 2, ew, eh);
            g.setColor(new Color(45, 32, 30));
            g.setStroke(new BasicStroke((float) (Math.max(1.4, sw * 0.45))));
            g.drawOval(cx - ew / 2, eyeY - eh / 2, ew, eh);
            if (!blink) {
                g.setColor(irisFor(design));
                g.fillOval(cx - ew / 5, eyeY - eh / 5, ew * 2 / 5, eh * 2 / 3);
                g.setColor(new Color(40, 26, 22));
                g.fillOval(cx - ew / 8, eyeY - eh / 5 + 2, ew * 3 / 10, eh * 2 / 3 - 2);
                g.setColor(new Color(255, 255, 255));
                g.fillOval(cx - ew / 3, eyeY - eh / 3, Math.max(2, ew / 3), Math.max(2, ew / 3));
                g.fillOval(cx + ew / 8, eyeY, Math.max(2, ew / 6), Math.max(2, ew / 6));
            } else {
                g.setColor(new Color(45, 32, 30));
                g.setStroke(new BasicStroke((float) Math.max(2.6, sw * 0.9)));
                g.drawLine(cx - ew / 2 + 2, eyeY, cx + ew / 2 - 2, eyeY);
            }
        }

        // cejas y boca segun mood
        Color brow = new Color(88, 60, 40);
        g.setStroke(new BasicStroke((float) Math.max(2.4, w * 0.055)));
        int browY = eyeY - (int) (r * 0.30);
        int bwd = (int) (r * 0.42);
        g.setColor(brow);
        if (mood == MOOD_HAPPY) {
            g.drawArc((int) (-r * 0.64), browY - 8, bwd, 16, 0, 180);
            g.drawArc((int) (r * 0.22), browY - 8, bwd, 16, 0, 180);
        } else if (mood == MOOD_SURPRISED) {
            g.drawArc((int) (-r * 0.64), browY - 22, bwd, 12, 0, 180);
            g.drawArc((int) (r * 0.22), browY - 22, bwd, 12, 0, 180);
        } else {
            g.drawLine((int) (-r * 0.66), browY - 6, (int) (-r * 0.26), browY + 4);
            g.drawLine((int) (r * 0.66), browY - 6, (int) (r * 0.26), browY + 4);
        }

        if (mood == MOOD_HAPPY) {
            g.setColor(new Color(255, 130, 155, 180));
            g.fillOval((int) (-r * 0.92), cy + (int) (r * 0.28), (int) (r * 0.34), (int) (r * 0.18));
            g.fillOval((int) (r * 0.58), cy + (int) (r * 0.28), (int) (r * 0.34), (int) (r * 0.18));
        }

        int mouthY = cy + (int) (r * 0.42);
        if (mood == MOOD_HAPPY) {
            int mw = (int) (r * 0.60);
            g.setColor(new Color(96, 40, 30));
            g.fillRoundRect((int) (-mw / 2), mouthY, mw, (int) (r * 0.34), (int) (r * 0.16), (int) (r * 0.16));
            g.setColor(Color.WHITE);
            g.fillRoundRect((int) (-mw * 0.36), mouthY, (int) (mw * 0.72), (int) (r * 0.12), 8, 8);
            if (DESIGN_PIPO.equals(design)) {
                g.setColor(new Color(255, 110, 140));
                g.fillOval((int) (-mw * 0.24), mouthY + (int) (r * 0.10), (int) (mw * 0.48), (int) (r * 0.14));
            }
        } else if (mood == MOOD_SURPRISED) {
            int ow = (int) (r * 0.34);
            g.setColor(new Color(96, 40, 30));
            g.fillOval((int) (-ow / 2), mouthY, ow, (int) (r * 0.30));
        } else {
            g.setStroke(new BasicStroke((float) Math.max(2.6, sw)));
            g.setColor(new Color(96, 40, 30));
            g.drawLine((int) (-r * 0.34), mouthY + 4, (int) (r * 0.34), mouthY + 4);
            g.drawArc((int) (-r * 0.18), mouthY - 4, (int) (r * 0.36), 12, 0, 180);
        }

        // pico de pata o pico curvo de guacamaya
        if (DESIGN_PATA.equals(design)) {
            drawDuckBeak(g, r, cy, sw);
        } else if (DESIGN_COCO.equals(design)) {
            drawCocoBeak(g, r, cy, sw);
        } else if (DESIGN_MEMO.equals(design)) {
            g.setColor(new Color(232, 178, 66));
            Path2D.Double bk = new Path2D.Double();
            bk.moveTo(-r * 0.12, mouthY);
            bk.lineTo(r * 0.16, mouthY);
            bk.lineTo(0, mouthY + r * 0.12);
            bk.closePath();
            g.fill(bk);
        }
    }

    private static Color irisFor(String design) {
        if (DESIGN_MEMO.equals(design)) return new Color(255, 170, 40);
        if (DESIGN_COCO.equals(design)) return new Color(233, 90, 60);
        if (DESIGN_LUNA.equals(design)) return new Color(146, 96, 224);
        return new Color(92, 64, 56);
    }

    private static void drawHat(Graphics2D g, int w, int h, int r, int cy, Color accent, double sw) {
        int topY = cy - r;
        int domeH = (int) (r * 0.52);
        inkOval(g, (int) (-r * 0.86), topY - 12, (int) (r * 1.72), domeH, sw * 0.9);
        shadeOval(g, (int) (-r * 0.86), topY - 12, (int) (r * 1.72), domeH, accent);
        g.setColor(new Color(255, 255, 255, 130));
        g.fillOval((int) (-r * 0.66), topY - 4, (int) (r * 0.9), (int) (r * 0.16));
        ink(g, (int) (-r * 1.10), topY + (int) (r * 0.34), (int) (r * 2.2), (int) (r * 0.22), (int) (r * 0.11), sw * 0.8);
        shadeRound(g, (int) (-r * 1.10), topY + (int) (r * 0.34), (int) (r * 2.2), (int) (r * 0.22), (int) (r * 0.11), darker(accent, 0.15f));
    }

    /** Orejas largas de coneja (Luna). */
    private static void drawRabbitEars(Graphics2D g, int r, int cy, Color inner, double sw) {
        int top = (int) (cy - r - r * 1.28);
        for (int side = -1; side <= 1; side += 2) {
            int ex = (int) (side * r * 0.58);
            inkOval(g, ex - (int) (r * 0.30), top, (int) (r * 0.60), (int) (r * 1.55), sw * 0.8);
            shadeOval(g, ex - (int) (r * 0.30), top, (int) (r * 0.60), (int) (r * 1.55), new Color(255, 238, 220));
            g.setColor(inner);
            g.fillOval(ex - (int) (r * 0.13), top + (int) (r * 0.38), (int) (r * 0.26), (int) (r * 0.95));
        }
    }

    /** Orejas caidas de perrito (Pipo). */
    private static void drawDogEars(Graphics2D g, int r, int cy, Color ear, double sw) {
        for (int side = -1; side <= 1; side += 2) {
            int ex = (int) (side * r * 0.82);
            g.rotate(side * 0.14, ex, cy - r / 2);
            inkOval(g, ex - (int) (r * 0.36), cy - (int) (r * 0.52), (int) (r * 0.72), (int) (r * 1.10), sw * 0.8);
            shadeOval(g, ex - (int) (r * 0.36), cy - (int) (r * 0.52), (int) (r * 0.72), (int) (r * 1.10), ear);
            g.setColor(new Color(190, 118, 70));
            g.fillOval(ex - (int) (r * 0.18), cy - (int) (r * 0.30), (int) (r * 0.36), (int) (r * 0.70));
            g.rotate(-side * 0.14, ex, cy - r / 2);
        }
    }

    private static void drawMonoEars(Graphics2D g, int r, int cy, double sw) {
        for (int side = -1; side <= 1; side += 2) {
            int ex = side * (int) (r * 0.94);
            inkOval(g, ex - (int) (r * 0.30), cy - (int) (r * 0.62), (int) (r * 0.60), (int) (r * 0.60), sw * 0.8);
            shadeOval(g, ex - (int) (r * 0.30), cy - (int) (r * 0.62), (int) (r * 0.60), (int) (r * 0.60), new Color(200, 152, 112));
            g.setColor(new Color(255, 224, 186));
            g.fillOval(ex - (int) (r * 0.14), cy - (int) (r * 0.46), (int) (r * 0.28), (int) (r * 0.28));
        }
        // cara mas clara del mono
        g.setColor(new Color(255, 232, 196));
        g.fillOval((int) (-r * 0.52), (int) (cy - r * 0.18), (int) (r * 1.04), (int) (r * 0.92));
    }

    private static void drawBuhoTuffs(Graphics2D g, int r, int cy, double sw) {
        for (int side = -1; side <= 1; side += 2) {
            int bx = side * (int) (r * 0.42);
            int top = (int) (cy - r);
            Path2D.Double tuft = new Path2D.Double();
            tuft.moveTo(bx - r * 0.18, top + 6);
            tuft.lineTo(bx, top - r * 0.30);
            tuft.lineTo(bx + r * 0.18, top + 6);
            tuft.closePath();
            ink(g, (int) tuft.getBounds().getX(), (int) tuft.getBounds().getY(),
                    (int) tuft.getBounds().getWidth(), (int) tuft.getBounds().getHeight(), 6, sw * 0.7);
            g.setColor(new Color(150, 118, 84));
            g.fill(tuft);
        }
    }

    private static void drawCocoCrest(Graphics2D g, int r, int cy, double sw) {
        for (int i = 0; i < 3; i++) {
            int bx = (int) ((i - 1) * r * 0.22);
            int top = (int) (cy - r + i * r * 0.04);
            Path2D.Double crest = new Path2D.Double();
            crest.moveTo(bx - r * 0.10, top + r * 0.10);
            crest.lineTo(bx, top - r * 0.28 - i * r * 0.05);
            crest.lineTo(bx + r * 0.22, top + r * 0.12);
            crest.closePath();
            g.setColor(i == 1 ? new Color(255, 190, 60) : new Color(46, 120, 220));
            g.fill(crest);
        }
    }

    private static void drawDuckBeak(Graphics2D g, int r, int cy, double sw) {
        g.setColor(new Color(244, 166, 52));
        g.fillOval((int) (-r * 0.34), cy + (int) (r * 0.40), (int) (r * 0.68), (int) (r * 0.22));
        g.setColor(new Color(214, 120, 30));
        g.fillOval((int) (-r * 0.34), cy + (int) (r * 0.47), (int) (r * 0.68), (int) (r * 0.10));
    }

    private static void drawCocoBeak(Graphics2D g, int r, int cy, double sw) {
        double mx = 0, my = cy + r * 0.42;
        Path2D.Double bk = new Path2D.Double();
        bk.moveTo(-r * 0.10, my - 4);
        bk.curveTo(r * 0.28, my - 8, r * 0.34, my + 6, r * 0.06, my + 14);
        bk.closePath();
        g.setColor(new Color(150, 112, 60));
        g.fill(bk);
        g.setColor(new Color(210, 178, 96));
        g.fillOval((int) (-r * 0.12), (int) (my - 6), (int) (r * 0.22), (int) (r * 0.14));
    }

    private static void drawArmBack(Graphics2D g, int w, int h, double t, Color accent, double sw) {
        int sx = (int) (-w * 0.74);
        int sy = (int) (-h * 0.18);
        double ang = Math.PI * 0.3 + 0.10 * Math.sin(2 * Math.PI * 0.7 * t);
        g.translate(sx, sy);
        g.rotate(ang);
        int aw = (int) (w * 0.34);
        int al = (int) (h * 0.40);
        ink(g, -aw / 2, 0, aw, al, 14, sw * 0.8);
        shadeRound(g, -aw / 2, 0, aw, al, 14, darker(torsoBase("mascot"), 0.18f));
        g.rotate(-ang);
        g.translate(-sx, -sy);
    }

    private static void drawArmFront(Graphics2D g, int w, int h, double t, Color accent,
                                     int mood, boolean waving, String design, double sw) {
        int sx = (int) (w * 0.70);
        int sy = (int) (-h * 0.18);
        double ang = waving
                ? Math.PI * 0.55 + Math.sin(2 * Math.PI * 2.4 * t) * 0.50
                : Math.PI * 0.22 + 0.08 * Math.sin(2 * Math.PI * 0.9 * t);
        g.translate(sx, sy);
        g.rotate(ang);
        int aw = (int) (w * 0.34);
        int al = (int) (h * 0.40);
        ink(g, -aw / 2, 0, aw, al, 14, sw * 0.9);
        shadeRound(g, -aw / 2, 0, aw, al, 14, torsoBase(design));
        // manita
        int handR = (int) (w * 0.20);
        inkOval(g, -handR / 2, al - handR / 2, handR, handR, sw * 0.8);
        shadeOval(g, -handR / 2, al - handR / 2, handR, handR, fur(1));
        g.rotate(-ang);
        g.translate(-sx, -sy);
    }

    // ============================================================ VEHICULOS

    /** Excavadora (Tito): cabina con ojos, pista, brazo con cucharon que excava. */
    private static void drawExcavadora(Graphics2D g, double scale, Color accent, int mood,
                                       boolean blink, double t, double armAng) {
        int w = (int) (90 * scale);
        int h = (int) (210 * scale);
        double sw = Math.max(2.4, 3.2 * scale);

        drawShadow(g, w, (int) (40 * scale));

        // pista
        ink(g, (int) (-w * 0.98), (int) (h * 0.62), (int) (w * 1.96), (int) (h * 0.36), 26, sw);
        shadeRound(g, (int) (-w * 0.98), (int) (h * 0.62), (int) (w * 1.96), (int) (h * 0.36), 26,
                darken(new Color(252, 186, 50), 0.05f));
        g.setColor(new Color(90, 62, 40));
        for (int i = -2; i <= 2; i++) {
            g.fillOval((int) (i * w * 0.44) - 16, (int) (h * 0.72), 32, 32);
        }

        // brazo con cucharon (detras de la cabina, cavando al frente)
        g.translate((int) (-w * 0.30), (int) (h * 0.42));
        g.rotate(armAng);
        g.setColor(new Color(238, 158, 40));
        g.setStroke(new BasicStroke((float) (w * 0.16)));
        g.drawLine(0, 0, (int) (w * 1.1), 0);
        g.setColor(new Color(60, 60, 66));
        Path2D.Double bucket = new Path2D.Double();
        bucket.moveTo(w * 1.06, -w * 0.22);
        bucket.lineTo(w * 1.06, w * 0.26);
        bucket.lineTo(w * 1.42, w * 0.10);
        bucket.lineTo(w * 1.38, -w * 0.10);
        bucket.closePath();
        g.fill(bucket);
        g.rotate(-armAng);
        g.translate(num(w * 0.30), -num(h * 0.42));

        // cabina
        int cx = 0;
        int cy = (int) (-h * 0.10);
        int cwW = (int) (w * 1.42);
        int cwH = (int) (h * 0.56);
        ink(g, cx - cwW / 2, cy - cwH / 2, cwW, cwH, 26, sw);
        RadialGradientPaint p = new RadialGradientPaint(cx - cwW * 0.1f, cy - cwH * 0.2f, cwW * 0.75f,
                new float[]{0f, 0.55f, 1f},
                new Color[]{lighten(accent, 0.42f), accent, darken(accent, 0.35f)});
        g.setPaint(p);
        g.fillRoundRect(cx - cwW / 2, cy - cwH / 2, cwW, cwH, 26, 26);

        // techo y tubo de escape
        g.setColor(darker(accent, 0.28f));
        g.fillRoundRect(cx - cwW / 2 - 6, cy - cwH / 2 - 18, cwW + 12, 34, 14, 14);
        g.fillRoundRect(cx + cwW / 2 - 24, cy - cwH / 2 - 70, 26, 54, 10, 10);

        // ventanilla con ojos
        int vw = (int) (cwW * 0.72);
        int vh = (int) (cwH * 0.44);
        g.setColor(new Color(140, 216, 244));
        g.fillRoundRect(cx - vw / 2, cy - vh / 2, vw, vh, 18, 18);
        g.setColor(new Color(255, 255, 255, 120));
        g.fillRoundRect(cx - vw / 2 + 8, cy - vh / 2 + 6, vw / 3, vh / 3, 10, 10);

        int eyeW = (int) (w * 0.30);
        int eyeH = (int) (w * 0.40);
        int eyeY = cy - 4;
        for (int side = -1; side <= 1; side += 2) {
            int ex = cx + side * (int) (w * 0.22);
            g.setColor(Color.WHITE);
            g.fillOval(ex - eyeW / 2, eyeY - eyeH / 2, eyeW, eyeH);
            if (!blink) {
                g.setColor(irisFor("tito"));
                g.fillOval(ex - eyeW / 5, eyeY - eyeH / 5, eyeW * 2 / 5, eyeH * 2 / 3);
                g.setColor(new Color(30, 26, 22));
                g.fillOval(ex - eyeW / 8, eyeY - eyeH / 5 + 2, eyeW * 3 / 10, eyeH * 2 / 3 - 2);
                g.setColor(Color.WHITE);
                g.fillOval(ex - eyeW / 3, eyeY - eyeH / 3, Math.max(2, eyeW / 3), Math.max(2, eyeW / 3));
            }
        }
        // boca + cejas
        g.setStroke(new BasicStroke((float) Math.max(2.6, w * 0.06)));
        g.setColor(new Color(88, 60, 40));
        int browY = eyeY - (int) (w * 0.22);
        if (mood == MOOD_HAPPY) {
            g.drawArc(cx - (int) (w * 0.42), browY - 6, (int) (w * 0.34), 12, 0, 180);
            g.drawArc(cx + (int) (w * 0.08), browY - 6, (int) (w * 0.34), 12, 0, 180);
            g.setColor(new Color(96, 40, 30));
            g.fillRoundRect(cx - (int) (w * 0.20), eyeY + (int) (w * 0.30), (int) (w * 0.40), (int) (w * 0.16), 8, 8);
        } else {
            g.drawLine(cx - (int) (w * 0.42), browY, cx - (int) (w * 0.08), browY + 4);
            g.drawLine(cx + (int) (w * 0.08), browY + 4, cx + (int) (w * 0.42), browY);
        }
        // focos
        g.setColor(new Color(255, 236, 140));
        g.fillOval(cx - (int) (w * 0.62), cy + (int) (w * 0.10), (int) (w * 0.20), (int) (w * 0.14));
        g.fillOval(cx + (int) (w * 0.42), cy + (int) (w * 0.10), (int) (w * 0.20), (int) (w * 0.14));
    }

    /** Camion volquete (Bruno): volquete rojo con ruedas y cabina con ojos. */
    private static void drawCamion(Graphics2D g, double scale, Color accent, int mood,
                                   boolean blink, double t) {
        int w = (int) (90 * scale);
        int h = (int) (210 * scale);
        double sw = Math.max(2.4, 3.2 * scale);

        drawShadow(g, w, (int) (40 * scale));

        // ruedas
        for (int side = -1; side <= 1; side += 2) {
            int wx = side * (int) (w * 0.52);
            int wy = (int) (h * 0.82);
            int r = (int) (w * 0.30);
            g.setColor(new Color(40, 34, 42));
            g.fillOval(wx - r, wy - r, r * 2, r * 2);
            g.setColor(new Color(210, 190, 170));
            g.fillOval(wx - r / 2, wy - r / 2, r, r);
            g.setColor(new Color(90, 80, 90));
            g.fillOval(wx - r / 4, wy - r / 4, r / 2, r / 2);
        }

        // caja volquete
        double tilt = 0.14 * Math.sin(2 * Math.PI * 0.5 * t);
        g.translate((int) (-w * 0.55), (int) (-h * 0.42));
        g.rotate(tilt);
        ink(g, 0, 0, (int) (w * 0.90), (int) (h * 0.42), 16, sw * 0.8);
        shadeRound(g, 0, 0, (int) (w * 0.90), (int) (h * 0.42), 16, accent);
        g.setColor(new Color(255, 255, 255, 100));
        g.fillRoundRect(8, 10, (int) (w * 0.90) - 16, 16, 8, 8);
        g.rotate(-tilt);
        g.translate(num(w * 0.55), num(h * 0.42));

        // chasis
        ink(g, (int) (-w * 0.56), (int) (h * 0.52), (int) (w * 1.14), (int) (h * 0.22), 14, sw * 0.7);
        shadeRound(g, (int) (-w * 0.56), (int) (h * 0.52), (int) (w * 1.14), (int) (h * 0.22), 14,
                new Color(96, 80, 104));

        // cabina con ojos
        int cwW = (int) (w * 0.78);
        int cwH = (int) (h * 0.46);
        int cx = (int) (w * 0.50);
        int cy = (int) (-h * 0.06);
        ink(g, cx - cwW / 2, cy - cwH / 2, cwW, cwH, 22, sw);
        RadialGradientPaint p = new RadialGradientPaint(cx - cwW * 0.15f, cy - cwH * 0.20f, cwW * 0.8f,
                new float[]{0f, 0.55f, 1f},
                new Color[]{lighten(new Color(233, 70, 56), 0.4f), new Color(233, 70, 56), darken(new Color(233, 70, 56), 0.3f)});
        g.setPaint(p);
        g.fillRoundRect(cx - cwW / 2, cy - cwH / 2, cwW, cwH, 22, 22);
        g.setColor(new Color(120, 24, 24));
        g.fillRoundRect(cx - cwW / 2 + 10, cy - cwH / 2 - 8, cwW - 20, 14, 7, 7);

        int vw = (int) (cwW * 0.72);
        int vh = (int) (cwH * 0.5);
        g.setColor(new Color(140, 216, 244));
        g.fillRoundRect(cx - vw / 2, cy - vh / 2, vw, vh, 14, 14);
        g.setColor(new Color(255, 255, 255, 120));
        g.fillRoundRect(cx - vw / 2 + 6, cy - vh / 2 + 5, vw / 4, vh / 3, 8, 8);

        int eyeW = (int) (w * 0.26);
        int eyeH = (int) (w * 0.34);
        for (int side = -1; side <= 1; side += 2) {
            int ex = cx + side * (int) (w * 0.16);
            g.setColor(Color.WHITE);
            g.fillOval(ex - eyeW / 2, cy - eyeH / 2, eyeW, eyeH);
            if (!blink) {
                g.setColor(new Color(150, 88, 60));
                g.fillOval(ex - eyeW / 5, cy - eyeH / 5, eyeW * 2 / 5, eyeH * 2 / 3);
                g.setColor(new Color(30, 26, 22));
                g.fillOval(ex - eyeW / 8, cy - eyeH / 5 + 2, eyeW * 3 / 10, eyeH * 2 / 3 - 4);
                g.setColor(Color.WHITE);
                g.fillOval(ex - eyeW / 3, cy - eyeH / 3, Math.max(2, eyeW / 3), Math.max(2, eyeW / 3));
            }
        }
        g.setStroke(new BasicStroke((float) Math.max(2.4, w * 0.055)));
        g.setColor(new Color(88, 60, 40));
        int browY = cy - (int) (w * 0.20);
        if (mood == MOOD_HAPPY) {
            g.drawArc(cx - (int) (w * 0.36), browY - 4, (int) (w * 0.28), 10, 0, 180);
            g.drawArc(cx + (int) (w * 0.08), browY - 4, (int) (w * 0.28), 10, 0, 180);
            g.setColor(new Color(96, 40, 30));
            g.fillRoundRect(cx - (int) (w * 0.16), cy + (int) (w * 0.24), (int) (w * 0.32), (int) (w * 0.14), 7, 7);
        }
    }

    private static int num(double d) {
        return (int) d;
    }

    private static double waveAng(double t, boolean waving) {
        return waving
                ? Math.PI * 0.55 + Math.sin(2 * Math.PI * 2.2 * t) * 0.55
                : Math.PI * 0.25 + 0.08 * Math.sin(2 * Math.PI * 0.9 * t);
    }

    // ============================================================ UTILIDADES

    static int moodFor(int index) {
        return (index + 1) % 3;
    }

    /** Diseno (especie) segun el nombre del personaje. */
    static String designFor(String character) {
        if (character == null) return DESIGN_MASCOT;
        String n = norm(character);
        if (n.equals(DESIGN_CARACOL)) return DESIGN_CARACOL;
        if (n.equals(DESIGN_EXCAVADORA)) return DESIGN_EXCAVADORA;
        if (n.equals(DESIGN_PATA)) return DESIGN_PATA;
        if (n.equals(DESIGN_MONO)) return DESIGN_MONO;
        if (n.equals(DESIGN_LUNA)) return DESIGN_LUNA;
        if (n.equals(DESIGN_PIPO)) return DESIGN_PIPO;
        if (n.equals(DESIGN_TICO)) return DESIGN_TICO;
        if (n.equals(DESIGN_BRUNO)) return DESIGN_BRUNO;
        if (n.equals(DESIGN_MEMO)) return DESIGN_MEMO;
        if (n.equals(DESIGN_COCO)) return DESIGN_COCO;
        // Rubi y Lumi no tienen malla propia en el dibujado vectorial:
        // usan siluetas ya probadas con color propio hasta que haya diseno exclusivo.
        if (n.equals("rubi")) return DESIGN_COCO;
        if (n.equals("lumi")) return DESIGN_LUNA;
        return DESIGN_MASCOT;
    }

    private static String norm(String s) {
        return s.toLowerCase(Locale.ROOT)
                .replace('\u00e1', 'a').replace('\u00e9', 'e').replace('\u00ed', 'i')
                .replace('\u00f3', 'o').replace('\u00fa', 'u').replace('\u00f1', 'n').trim();
    }

    /** Estrella rellena (Luna). */
    private static void drawStar(Graphics2D g, double cx, double cy, double r, Color c) {
        g.setColor(c);
        int n = 10;
        int[] xs = new int[n];
        int[] ys = new int[n];
        for (int i = 0; i < n; i++) {
            double ang = i * Math.PI / 5 - Math.PI / 2;
            double rr = (i % 2 == 0) ? r : r * 0.45;
            xs[i] = (int) (cx + Math.cos(ang) * rr);
            ys[i] = (int) (cy + Math.sin(ang) * rr);
        }
        g.fillPolygon(xs, ys, n);
    }

    private static Color darker(Color c, float f) {
        return new Color(Math.max(0, (int) (c.getRed() * (1 - f))),
                Math.max(0, (int) (c.getGreen() * (1 - f))),
                Math.max(0, (int) (c.getBlue() * (1 - f))));
    }

    private static Color lighten(Color c, float f) {
        return new Color(Math.min(255, (int) (c.getRed() + (255 - c.getRed()) * f)),
                Math.min(255, (int) (c.getGreen() + (255 - c.getGreen()) * f)),
                Math.min(255, (int) (c.getBlue() + (255 - c.getBlue()) * f)));
    }

    private static Color darken(Color c, float f) {
        return darker(c, f);
    }

    private Actor() {}
}