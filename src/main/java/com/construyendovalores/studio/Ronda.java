package com.construyendovalores.studio;

import java.util.List;

/** Una ronda infantil con su letra y la melodía (notación de MelodySynth) por línea. */
public record Ronda(String id, String titulo, int bpm, String escenario, String valor,
                    String descripcion, String etiquetas, String intro, List<RondaLine> lineas) {

    public record RondaLine(String texto, String notas) {}
}