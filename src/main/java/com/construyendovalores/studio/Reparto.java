package com.construyendovalores.studio;

import java.util.List;
import java.util.Locale;

/**
 * Elenco fijo del canal y rotacion determinista por historia.
 * Centraliza la lista para que guiones, prompts, animacion y stills usen
 * siempre los mismos personajes (incluidos Rubi y Lumi).
 */
public final class Reparto {

    private Reparto() {}

    public static final List<String> ELENCO = List.of(
            "Casquito", "Tito", "Sofía", "Nico", "Luna", "Pipo",
            "Tico", "Bruno", "Memo", "Coco", "Rubí", "Lumi");

    /** Desfase estable: el mismo titulo siempre reparte los mismos papeles. */
    public static int rotacion(String titulo) {
        if (titulo == null || titulo.isBlank()) return 0;
        return Math.floorMod(titulo.toLowerCase(Locale.ROOT).hashCode(), ELENCO.size());
    }

    /** Personaje que habla en la escena idx (la numeracion empieza en 1). */
    public static String actor(String titulo, int idx) {
        return ELENCO.get(Math.floorMod(rotacion(titulo) + idx, ELENCO.size()));
    }

    /** Nombres permitidos en los JSON de guion. */
    public static String nombres() {
        return String.join(", ", ELENCO) + ", Narrador";
    }

    /** Fichas breves para los prompts: el modelo elige entre todos, no solo los cuatro de siempre. */
    public static String fichas() {
        return "Casquito (caracol maestro con casco amarillo), "
                + "Tito (excavadora curiosa e impaciente), "
                + "Sofía (patita creativa y organizada), "
                + "Nico (constructor principiante que aprende de sus errores), "
                + "Luna (conejita mágica y soñadora), "
                + "Pipo (perrito bromista que hace reír), "
                + "Tico (tortuga sabia y paciente), "
                + "Bruno (camión volquete leal que ayuda al equipo), "
                + "Memo (búho sereno que enseña a calmarse), "
                + "Coco (guacamaya valiente y cantarina), "
                + "Rubí (dragona pequeña y valiente que anima a todos) y "
                + "Lumi (luciernaga mágica, curiosa y serena)";
    }
}
