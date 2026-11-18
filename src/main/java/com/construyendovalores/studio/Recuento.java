package com.construyendovalores.studio;

import java.util.Map;

/** Catalogo de cuentos clasicos de dominio publico y el valor que enseña cada uno. */
public final class Recuento {

    public static final Map<String, String> VALORES = Map.ofEntries(
            Map.entry("caperucita roja", "prudencia y obediencia"),
            Map.entry("los tres cerditos", "esfuerzo y preparacion"),
            Map.entry("la tortuga y la liebre", "perseverancia"),
            Map.entry("el leon y el raton", "gratitud y amistad"),
            Map.entry("el patito feo", "autoestima y aceptacion"),
            Map.entry("hansel y gretel", "valentia y cuidado familiar"),
            Map.entry("el gato con botas", "ingenio y audacia"),
            Map.entry("ricitos de oro", "respeto"),
            Map.entry("pinocho", "honestidad"),
            Map.entry("el pastor mentiroso", "honestidad"),
            Map.entry("peter y el lobo", "honestidad"),
            Map.entry("el rey midas", "cautela y generosidad"),
            Map.entry("la cigarra y la hormiga", "responsabilidad"),
            Map.entry("el lobo que queria cambiar de color", "aceptacion de uno mismo"),
            Map.entry("los músicos de bremen", "cooperacion"),
            Map.entry("la princesa y el guisante", "empata y sencillez"),
            Map.entry("el traje nuevo del emperador", "valentia para decir la verdad"),
            Map.entry("la sirenita", "sacrificio y amor familiar"),
            Map.entry("el soldadito de plomo", "valentia y firmeza en el amor"),
            Map.entry("blancanieves", "bondad y amistad"),
            Map.entry("la bella durmiente", "paciencia y bondad"),
            Map.entry("cenerentola o cenicienta", "bondad y esfuerzo"),
            Map.entry("rapunzel", "liberacion y esperanza"),
            Map.entry("el flautista de hamelin", "responsabilidad y palabra cumplida"),
            Map.entry("pedro y el lobo", "honestidad")
    );

    /** Valor por defecto para un cuento (fiable sin usar IA). */
    public static String valorDe(String cuento) {
        String c = cuento.toLowerCase();
        for (var e : VALORES.entrySet()) {
            if (c.contains(e.getKey())) return e.getValue();
        }
        return "respeto y amistad";
    }

    private Recuento() {}
}