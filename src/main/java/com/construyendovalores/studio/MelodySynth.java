package com.construyendovalores.studio;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Sintetizador de melodías "de cajita musical" para rondas infantiles.
 * Notación compacta: qC4 eG4 hD3 sR ...
 * - Duración: w (redonda) h (blanca) q (negra) e (corchea) s (semicorchea)
 * - Nota: A-G seguida opcionalmente de # (sostenido) o b (bemol) y un dígito de octava.
 * - R = silencio (p. ej. qR).
 */
public final class MelodySynth {
    private static final int SR = 48000;

    public record Note(double freq, double beats) {}

    public static List<Note> parse(String notation) {
        List<Note> out = new ArrayList<>();
        if (notation == null) return out;
        for (String t : notation.trim().split("\\s+")) {
            if (t.isBlank()) continue;
            double beats = 1.0;
            int i = 0;
            char d = t.charAt(0);
            if ("whqes".indexOf(d) >= 0) {
                beats = switch (d) {
                    case 'w' -> 4.0;
                    case 'h' -> 2.0;
                    case 'q' -> 1.0;
                    case 'e' -> 0.5;
                    default -> 0.25;
                };
                i = 1;
            }
            if (i >= t.length()) continue;
            char n = Character.toUpperCase(t.charAt(i++));
            if (n == 'R') {
                out.add(new Note(0, beats));
                continue;
            }
            int acc = 0;
            if (i < t.length() && (t.charAt(i) == '#' || t.charAt(i) == 'b')) {
                acc = t.charAt(i) == '#' ? 1 : -1;
                i++;
            }
            int oct = 4;
            if (i < t.length() && Character.isDigit(t.charAt(i))) {
                oct = t.charAt(i) - '0';
            }
            int si = scaleIndex(n);
            if (si < 0) continue;
            out.add(new Note(midiFreq(oct, si, acc), beats));
        }
        return out;
    }

    private static int scaleIndex(char n) {
        return switch (n) {
            case 'C' -> 0;
            case 'D' -> 2;
            case 'E' -> 4;
            case 'F' -> 5;
            case 'G' -> 7;
            case 'A' -> 9;
            case 'B' -> 11;
            default -> -1;
        };
    }

    private static double midiFreq(int octave, int semitone, int acc) {
        double midi = 12 * (octave + 1) + semitone + acc;
        return 440.0 * Math.pow(2, (midi - 69) / 12.0);
    }

    public static double phraseSeconds(String notation, int bpm) {
        double beats = 0;
        for (Note n : parse(notation)) beats += n.beats();
        return beats * 60.0 / bpm;
    }

    /** Momentos (segundos) de cada golpe de la frase, para que los personajes reboten al ritmo. */
    public static List<Double> beatTimes(String notation, int bpm) {
        List<Double> out = new ArrayList<>();
        double spb = 60.0 / bpm;
        double t = 0;
        out.add(0.0);
        for (Note n : parse(notation)) {
            t += n.beats() * spb;
            out.add(t);
        }
        return out;
    }

    /** Renderiza la frase completa a WAV 48kHz monofónico. Devuelve la duración en segundos. */
    public static double renderWav(String notation, int bpm, Path out) throws Exception {
        List<Note> notes = parse(notation);
        double spb = 60.0 / bpm;
        double totalBeats = 0;
        for (Note n : notes) totalBeats += n.beats();
        double totalSec = totalBeats * spb;
        double endGap = 0.18;
        int N = (int) Math.floor((totalSec + endGap) * SR) + 1;
        double[] buf = new double[N];

        double beatTotal = 0;
        int beatIndex = 0;
        for (Note note : notes) {
            double s0 = beatTotal * spb;
            double dur = note.beats() * spb;
            int a = (int) (s0 * SR);
            int b = Math.min(N, (int) ((s0 + dur) * SR));
            if (note.freq() > 0) {
                double f = note.freq();
                for (int k = a; k < b; k++) {
                    double tt = (k - a) / (double) SR;
                    double vib = dur > 0.62 ? 2.4 * Math.sin(2 * Math.PI * 5.2 * tt) : 0.0;
                    double env = Math.min(1.0, tt / 0.008) * Math.exp(-tt / 0.11);
                    double w = Math.sin(2 * Math.PI * (f + vib) * tt)
                            + 0.5 * Math.sin(2 * Math.PI * 2 * (f + vib) * tt)
                            + 0.28 * Math.sin(2 * Math.PI * 3 * (f + vib) * tt)
                            + 0.12 * Math.sin(2 * Math.PI * 4 * (f + vib) * tt)
                            + 0.06 * Math.sin(2 * Math.PI * 5 * (f + vib) * tt);
                    buf[k] += env * w * 0.30;
                    if (tt < 0.22) buf[k] += Math.sin(2 * Math.PI * f / 2 * tt) * Math.exp(-8 * tt) * 0.16;
                }
            }
            beatTotal += note.beats();
            beatIndex++;
        }

        // Bajo rítmico estilo Granja de Zenón: bombo en cada pulso, palmada en pulsos alternos.
        for (int bi = 0; bi < beatIndex; bi++) {
            double bt = bi * spb;
            if (bt >= totalSec) break;
            int sk = Math.min(N - 1, (int) (bt * SR));
            // Bombo (súb-grave con golpe de cuerpo).
            int nKick = Math.min(N, sk + (int) (0.10 * SR));
            for (int k = sk; k < nKick; k++) {
                double tk = (k - sk) / (double) SR;
                buf[k] += (Math.sin(2 * Math.PI * 118 * tk) + 0.35 * Math.sin(2 * Math.PI * 59 * tk))
                        * Math.exp(-tk * 30) * 0.42;
            }
            if (bi % 2 == 1) {
                // Palmada (handclap): dos transientes por encima de 1 kHz.
                int nClap = Math.min(N, sk + (int) (0.09 * SR));
                for (int k = sk; k < nClap; k++) {
                    double tk = (k - sk) / (double) SR;
                    buf[k] += (Math.sin(2 * Math.PI * 1850 * tk) * Math.exp(-tk * 85)
                            + 0.6 * Math.sin(2 * Math.PI * 2900 * tk) * Math.exp(-tk * 120)) * 0.24;
                }
            } else {
                // "Tik" de contratiempo sutil.
                int nTik = Math.min(N, sk + (int) (0.03 * SR));
                for (int k = sk; k < nTik; k++) {
                    double tk = (k - sk) / (double) SR;
                    buf[k] += Math.sin(2 * Math.PI * 5200 * tk) * Math.exp(-tk * 180) * 0.06;
                }
            }
        }

        // Maracas: corcheas brillantes entre cada pulso para un ritmo más alegre.
        int eightSteps = (int) (totalBeats * 2);
        for (int ei = 0; ei < eightSteps; ei++) {
            double et = ei * 0.5 * spb;
            if (et >= totalSec) break;
            int sk = Math.min(N - 1, (int) (et * SR));
            int nSh = Math.min(N, sk + (int) (0.05 * SR));
            double amp = (ei % 2 == 0) ? 0.055 : 0.030;
            for (int k = sk; k < nSh; k++) {
                double tk = (k - sk) / (double) SR;
                buf[k] += (Math.sin(2 * Math.PI * 3160 * tk) * Math.exp(-tk * 210)
                        + 0.55 * Math.sin(2 * Math.PI * 4870 * tk) * Math.exp(-tk * 300)) * amp;
            }
        }

        double peak = 0;
        for (double v : buf) peak = Math.max(peak, Math.abs(v));
        double gain = peak > 0.95 ? 0.95 / peak : 1.0;
        gain = Math.min(gain, 0.9);

        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(out.toFile()))) {
            writeAscii(dos, "RIFF");
            le32(dos, 36 + N * 2);
            writeAscii(dos, "WAVE");
            writeAscii(dos, "fmt ");
            le32(dos, 16);
            le16(dos, 1);
            le16(dos, 1);
            le32(dos, SR);
            le32(dos, SR * 2);
            le16(dos, 2);
            le16(dos, 16);
            writeAscii(dos, "data");
            le32(dos, N * 2);
            for (double v : buf) {
                le16(dos, (int) Math.round(clamp(v * gain, -1, 1) * 32767));
            }
        }
        return totalSec + endGap;
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static void writeAscii(DataOutputStream dos, String s) throws Exception {
        for (char c : s.toCharArray()) dos.writeByte(c);
    }

    private static void le16(DataOutputStream dos, int v) throws Exception {
        dos.writeByte(v & 0xFF);
        dos.writeByte((v >> 8) & 0xFF);
    }

    private static void le32(DataOutputStream dos, long v) throws Exception {
        dos.writeByte((int) (v & 0xFF));
        dos.writeByte((int) ((v >> 8) & 0xFF));
        dos.writeByte((int) ((v >> 16) & 0xFF));
        dos.writeByte((int) ((v >> 24) & 0xFF));
    }

    private MelodySynth() {}
}