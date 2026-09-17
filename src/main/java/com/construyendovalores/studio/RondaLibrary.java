package com.construyendovalores.studio;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Rondas infantiles clásicas (dominio público) con letra y melodía. Las melodías
 *  son aproximaciones editables: cambia las cadenas "notas" para afinarlas. */
public final class RondaLibrary {

    private static final List<Ronda> RONDAS = new ArrayList<>();

    static {
        add(new Ronda("patio", "El patio de mi casa", 118, "parque", "Alegría",
                "La ronda clásica para saltar, agacharte y bailar. ¡El patio de mi casa es particular!",
                "ronda infantil,canciones infantiles,el patio de mi casa,bailes para niños,música para niños",
                "¡Hola a todos! Soy Casquito. Hoy vamos a cantar y bailar la ronda de El patio de mi casa. ¡A bailar!",
                List.of(
                        new Ronda.RondaLine("El patio de mi casa es particular,",
                                "qE4 eG4 eA4 qG4 eE4 eD4 qD4 hC4"),
                        new Ronda.RondaLine("cuando llueve se moja como los demás.",
                                "qC5 eB4 eA4 qG4 eA4 qG4 eE4 eD4 hC4"),
                        new Ronda.RondaLine("Agáchate y vuélvete a agachar:",
                                "qE4 eG4 eA4 qG4 eE4 eD4 qD4 hC4"),
                        new Ronda.RondaLine("que los agachaditos no saben bailar.",
                                "qC5 eB4 eA4 qG4 eA4 eG4 qE4 hD4"),
                        new Ronda.RondaLine("¡Y ahora todos juntos a bailar!",
                                "qC5 eC5 eB4 eA4 qG4 eA4 eG4 qE4 hC4"))));

        add(new Ronda("barco", "El barco chiquitito", 108, "parque", "Perseverancia",
                "Había una vez un barco chiquitito que no podía navegar... ¡hasta que lo logró!",
                "ronda infantil,canciones infantiles,el barco chiquitito,perseverancia,mar",
                "¡Hola! Yo soy Tito. ¿Conocen la ronda del barco chiquitito? ¡Vamos a cantarla juntos!",
                List.of(
                        new Ronda.RondaLine("Había una vez un barco chiquitito,",
                                "qC4 eD4 eE4 qG4 eE4 eD4 hE4"),
                        new Ronda.RondaLine("que no podía, que no podía navegar.",
                                "eC4 eD4 eE4 eG4 eE4 eD4 qE4 hD4"),
                        new Ronda.RondaLine("Pasaron una, dos, tres, cuatro, cinco, seis, siete semanas,",
                                "eC4 eD4 eE4 qG4 eA4 eG4 qE4 eD4 eE4 eF4 hE4"),
                        new Ronda.RondaLine("y mi barco, y mi barco navegó.",
                                "eC4 eD4 eE4 eG4 eA4 qG4 eE4 eD4 hC4"))));

        add(new Ronda("boton", "Debajo de un botón", 122, "obra", "Curiosidad",
                "La rima rimosa del ratón chiquitín: ¡debajo de un botón, ton ton!",
                "ronda infantil,debajo de un botón,canciones infantiles,rimas para niños,curiosidad",
                "¡Hola, pequeños curiosos! Soy Sofía. Vamos a buscar al ratón que vive debajo de un botón.",
                List.of(
                        new Ronda.RondaLine("Debajo de un botón, ton ton,",
                                "qC4 eE4 eG4 eC5 eB4 qG4 eG4 hD4"),
                        new Ronda.RondaLine("que encontró Martín, tin tin,",
                                "qC4 eE4 eG4 eC5 eB4 qE4 eG4 hE4"),
                        new Ronda.RondaLine("había un ratón, ton ton,",
                                "qC4 eE4 eG4 eC5 eA4 qG4 eA4 hF4"),
                        new Ronda.RondaLine("ay, qué chiquitín, tin tin.",
                                "qC4 eE4 eG4 eC5 eA4 qE4 eG4 hC4"))));

        add(new Ronda("lobo", "Juguemos en el bosque", 112, "parque", "Amistad",
                "¿Está el lobo? ¡No! Mientras no está, cantamos y jugamos todos juntos.",
                "ronda infantil,el lobo,juguemos en el bosque,canciones infantiles,amistad",
                "¡Hola! Soy Nico. ¿Jugamos en el bosque? ¡Pero cuidado con el lobo!",
                List.of(
                        new Ronda.RondaLine("Juguemos en el bosque mientras el lobo no está.",
                                "qC4 eE4 eG4 eC5 qB4 eA4 eG4 qE4 eD4 hC4"),
                        new Ronda.RondaLine("¿Lobo estás? ¡No! Juguemos en el bosque.",
                                "qC4 eE4 eG4 eC5 qB4 eA4 eG4 qG4 hE4"),
                        new Ronda.RondaLine("¡El lobo se viste! ¡Corramos a bailar!",
                                "qE4 eG4 eA4 qG4 eE4 qD4 hC4"),
                        new Ronda.RondaLine("Juguemos en el bosque, juguemos sin parar.",
                                "qC4 eD4 eE4 eG4 eA4 qG4 eE4 eD4 hC4"))));

        add(new Ronda("cucaracha", "La cucaracha", 120, "taller", "Alegría",
                "La clásica que nadie puede dejar de cantar. ¡La cucaracha ya no puede caminar!",
                "ronda infantil,la cucaracha,canciones infantiles,fiesta para niños,alegría",
                "¡Hola a todos! Soy Casquito. A cantar con la cucaracha, que ya no puede caminar. ¡Ja, ja, ja!",
                List.of(
                        new Ronda.RondaLine("La cucaracha, la cucaracha",
                                "eE4 eE4 eE4 eE4 eC4 eD4 qE4 eE4 eE4 eE4 eC4 eD4 qE4"),
                        new Ronda.RondaLine("ya no puede caminar,",
                                "eG4 eA4 eG4 eF4 eE4 eD4 qE4 qR"),
                        new Ronda.RondaLine("porque tiene, porque le faltan",
                                "qE4 eE4 eE4 eE4 eC4 eD4 qE4 qR"),
                        new Ronda.RondaLine("las dos patitas de atrás.",
                                "eG4 eF4 eE4 eD4 qC4 qR"))));

        add(new Ronda("aserin", "Aserrín, aserrán", 118, "obra", "Compartir",
                "La ronda de los maderos de San Juan: piden pan, no les dan; piden queso, les dan hueso... ¡y se van!",
                "ronda infantil,aserrín aserrán,canciones infantiles,rimas para niños,compartir",
                "¡Hola, amigos! Soy Tito. A cortar madera con el aserrín, aserrán. ¡Todos a trabajar!",
                List.of(
                        new Ronda.RondaLine("Aserrín, aserrán, los maderos de San Juan",
                                "qG4 eG4 qE4 eE4 qA4 eA4 hF4"),
                        new Ronda.RondaLine("piden pan, no les dan; piden queso, les dan hueso",
                                "qG4 eG4 qE4 eE4 qF4 eF4 hD4"),
                        new Ronda.RondaLine("y se van, se van, se van.",
                                "qE4 eE4 qC4 eC4 qD4 eD4 hC4"),
                        new Ronda.RondaLine("¡Aserrín, aserrán, y a trabajar!",
                                "qG4 eG4 qE4 eA4 qG4 eG4 hC4"))));
    }

    private static void add(Ronda r) {
        RONDAS.add(r);
    }

    public static List<Ronda> clasicas() {
        return new ArrayList<>(RONDAS);
    }

    public static Ronda byIdOrIndex(String s) {
        if (s == null) throw new IllegalArgumentException("Indica una ronda (id o número).");
        if (s.matches("\\d+")) {
            int i = Integer.parseInt(s);
            if (i < 1 || i > RONDAS.size()) throw new IllegalArgumentException("No existe la ronda " + i);
            return RONDAS.get(i - 1);
        }
        for (Ronda r : RONDAS) {
            if (r.id().equalsIgnoreCase(s) || r.titulo().equalsIgnoreCase(s)) return r;
        }
        throw new IllegalArgumentException("No se encontró la ronda '" + s + "'.");
    }

    /** Siguiente ronda del ciclo, usando canal.rondaIndex de la config. */
    public static int nextRondaIndex(Config cfg) {
        int i;
        try {
            i = Integer.parseInt(cfg.get("canal.rondaIndex", "1"));
        } catch (NumberFormatException e) {
            i = 1;
        }
        return ((i - 1) % RONDAS.size()) + 1;
    }

    public static Path writePlan(Path dir) throws Exception {
        Files.createDirectories(dir);
        StringBuilder csv = new StringBuilder("id;titulo;bpm;escenario;valor;descripcion;etiquetas\n");
        StringBuilder md = new StringBuilder("# RONDAS INFANTILES - plan del canal\n\n");
        for (Ronda r : RONDAS) {
            csv.append(r.id()).append(';').append(r.titulo()).append(';').append(r.bpm()).append(';')
                    .append(r.escenario()).append(';').append(r.valor()).append(';')
                    .append(clean(r.descripcion())).append(';').append(clean(r.etiquetas())).append('\n');
            md.append("## ").append(r.titulo()).append("  \n")
                    .append("Valor: **").append(r.valor()).append("** · Escenario: ").append(r.escenario())
                    .append(" · BPM: ").append(r.bpm()).append("  \n")
                    .append(r.descripcion()).append("\n\n");
            for (Ronda.RondaLine l : r.lineas()) {
                md.append("   - ").append(l.texto()).append(" `").append(l.notas()).append("`\n");
            }
            md.append("\n");
        }
        Path csvPath = dir.resolve("RONDAS.csv");
        Path mdPath = dir.resolve("RONDAS.md");
        Files.write(csvPath, csv.toString().getBytes(StandardCharsets.UTF_8));
        Files.write(mdPath, md.toString().getBytes(StandardCharsets.UTF_8));
        return csvPath;
    }

    private static String clean(String s) {
        return s == null ? "" : s.replace("\"", "'").replace(";", ",");
    }

    private RondaLibrary() {}
}