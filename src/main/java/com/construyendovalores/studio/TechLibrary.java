package com.construyendovalores.studio;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Contenido de tecnología e historia, sin depender de IA: rondas propias,
 *  listas de "5 datos" (estilo los shorts de referencia), trivias de adivinanza
 *  y mini-historias de inventos. Todo con un valor constructivo detrás. */
public final class TechLibrary {

    public record Dato(String palabra, String narra, String valor) {}

    public record Listicle(String id, String titulo, String valor, String etiquetas, String intro, String cierre,
                           List<Dato> datos) {}

    public record Trivia(String id, String titulo, String valor, String etiquetas, String intro, List<Dato> items) {}

    public record Historia(String id, String titulo, String valor, String escenario, String etiquetas,
                           String datoClave) {}

    private static final List<Ronda> RONDAS_TECH = new ArrayList<>();
    private static final List<Listicle> LISTICLES = new ArrayList<>();
    private static final List<Trivia> TRIVIAS = new ArrayList<>();
    private static final List<Historia> HISTORIAS = new ArrayList<>();

    static {
        RONDAS_TECH.add(new Ronda("rueda", "La rueda gira", 124, "taller", "Progreso",
                "Una canción sobre el invento que mueve el mundo entero: la rueda.",
                "ronda infantil,canción de tecnología,la rueda,inventos,progreso",
                "¡Hola, constructores! Soy Tito. Hoy cantamos la canción del invento que hace girar al mundo: ¡la rueda!",
                List.of(
                        new Ronda.RondaLine("La rueda gira que te gira,",
                                "qC4 eE4 eG4 qC5 eB4 eA4 hG4"),
                        new Ronda.RondaLine("no se cansa de rodar;",
                                "qG4 eE4 eG4 qA4 eG4 eE4 hD4"),
                        new Ronda.RondaLine("lleva, trae y da la vuelta,",
                                "qE4 eG4 eA4 qC5 eA4 eG4 hE4"),
                        new Ronda.RondaLine("¡y nos ayuda a llegar!",
                                "qC5 eB4 eA4 qG4 eE4 eD4 hC4"))));

        RONDAS_TECH.add(new Ronda("tren", "El tren chucu-chucu", 126, "obra", "Constancia",
                "Cada vagón empuja al siguiente: sigue tú, que el tren avanza.",
                "ronda infantil,el tren,canción de tecnología,constancia,transporte",
                "¡Hola! Soy Sofía. ¡Vamos a cantar como una locomotora bien trabajadora! Chucu-chucu, ¡todos a bordo!",
                List.of(
                        new Ronda.RondaLine("El tren chucu-chucu, chucu-chucu, chucu-chu,",
                                "eG4 eG4 eA4 eG4 eB4 eG4 qD5 eD5 eC5 eD5 hB4"),
                        new Ronda.RondaLine("sube la montaña sin nunca parar;",
                                "eD5 eC5 eD5 eB4 qG4 eG4 eA4 eG4 hG4"),
                        new Ronda.RondaLine("vagón a vagón, que la ruta se hace,",
                                "eG4 eA4 eG4 eB4 qD5 eC5 eB4 eA4 hF#4"),
                        new Ronda.RondaLine("¡chucu-chucu-chu, hoy vamos a llegar!",
                                "eG4 eA4 eB4 qD5 eC5 eB4 eA4 hG4"))));

        RONDAS_TECH.add(new Ronda("robot", "El baile del robot", 128, "taller", "Aprendizaje",
                "El robot aprende a bailar paso a paso, ¡y tú aprendes igual!",
                "ronda infantil,el robot,baile del robot,canción de tecnología,aprendizaje",
                "¡Hola! Soy Casquito. Robot al frente: muévete tic-tic-tac y aprende el baile nuevo. ¡Todos a imitar!",
                List.of(
                        new Ronda.RondaLine("El robot da vueltas tic-tic-tac,",
                                "eE4 eE4 eE4 eE4 qG4 eE4 eD4 qC4 hC4"),
                        new Ronda.RondaLine("mueve un brazo y no sabe cantar;",
                                "eG4 eG4 eG4 eG4 qE4 eC4 eD4 qE4 hD4"),
                        new Ronda.RondaLine("aprende un paso, luego otro paso,",
                                "eA4 eA4 eA4 eA4 qG4 eE4 eG4 qE4 hE4"),
                        new Ronda.RondaLine("¡y al final aprende el baile total!",
                                "eC4 eD4 eE4 qG4 eA4 eG4 qE4 hC4"))));
    }

    static {
        LISTICLES.add(new Listicle("inventos", "5 inventos que cambiaron el mundo", "Curiosidad",
                "tecnología,inventos,la rueda,la imprenta,la bombilla,historia para niños",
                "¡Hola, pequeños constructores! Soy Tito. Hoy les traigo 5 inventos que cambiaron al mundo. ¡Contamos juntos!",
                "¿Cuál invento te gustó más? Cuéntanos en los comentarios. Mañana: ¡5 máquinas asombrosas!",
                List.of(
                        new Dato("LA RUEDA", "Hace más de cinco mil años, alguien inventó la rueda. Con ella, transportar se volvió fácil y todo empezó a rodar.", "Colaboración"),
                        new Dato("LA IMPRENTA", "Antes, copiar un solo libro a mano tomaba meses. Con la imprenta, los libros llegaron a miles de niños. ¡Esfuerzo!", "Esfuerzo"),
                        new Dato("LA BOMBILLA", "Thomas Edison probó miles de materiales antes de encender la primera bombilla. No se rindió: ¡perseverancia!", "Perseverancia"),
                        new Dato("EL TELÉFONO", "En 1876, Graham Bell hizo sonar el teléfono por primera vez y llamó a su ayudante. ¡Comunicación y unión!", "Comunicación"),
                        new Dato("EL AVIÓN", "Los hermanos Wright soñaron con volar y lo lograron: ¡12 segundos en el aire! Valentía de soñadores.", "Valentía"))));

        LISTICLES.add(new Listicle("maquinas", "5 máquinas asombrosas", "Asombro",
                "tecnología,máquinas,la grúa,el robot,el tren,el computador,para niños",
                "¡Hola! Soy Sofía. ¿Listos para conocer a 5 máquinas que parecen de cuento? ¡Al turrón!",
                "¿Qué máquina te gustaría manejar? Cuéntanos. ¡Y mañana otro listado increíble!",
                List.of(
                        new Dato("LA GRÚA", "Una grúa puede levantar hasta cien autos juntos. ¡Como si alzara una casa con una sola mano!", "Fuerza"),
                        new Dato("EL ROBOT", "Los robots pueden ensamblar un auto completo y hasta pintarlo. Trabajan sin descanso y aprenden cada día.", "Dedicación"),
                        new Dato("EL TREN", "Un tren eléctrico puede llevar a cientos de pasajeros y recorrer 300 kilómetros en una hora. ¡Velocidad!", "Eficiencia"),
                        new Dato("LA COMPUTADORA", "Una computadora puede hacer un millón de cálculos mientras tú parpadeas. ¡Mente veloz!", "Mente veloz"),
                        new Dato("EL SUBMARINO", "Bajo el mar, hay naves que exploran lo profundo y descifran misterios. ¡Curiosidad sin límites!", "Curiosidad"))));

        LISTICLES.add(new Listicle("ninos", "5 inventos de niños", "Creatividad",
                "tecnología,inventos de niños,el trampolín,el futbolín,creatividad,para niños",
                "¡Hola, inventores! Soy Nico. ¿Sabían que muchos inventos fueron ideas de niños? ¡Vamos a descubrirlos!",
                "¿Qué inventarías tú? ¡Cuéntanos! Tal vez ese sea el próximo invento genial del canal.",
                List.of(
                        new Dato("EL TRAMPOLÍN", "Un niño de 16 años inventó el trampolín en un garaje. ¡Saltar alto también se inventa!", "Imaginación"),
                        new Dato("EL FUTBOLÍN", "Un niño de un pueblo inventó el futbolín para jugar cuando llovía. ¡El juego nunca para!", "Ingenio"),
                        new Dato("EL HELADO DE PALITO", "Un niño de 11 años dejó su bebida afuera una noche helada y nació el helado de palito. ¡Hallazgo delicioso!", "Observación"),
                        new Dato("LAS OREJERAS", "Un niño de 15 años, en el frío, creó las orejeras para no pasar frío. ¡Protección genial!", "Cuidado"),
                        new Dato("EL PARAPENTE", "Un niño, siguiendo a un papalote, inventó el parapente: ¡volar como un ave pero con idea humana!", "Sueños"))));
    }

    static {
        TRIVIAS.add(new Trivia("que-soy", "¿Qué soy? Inventos de casa", "Asombro",
                "tecnología,adivinanzas,qué soy,inventos,para niños",
                "¡Hola! Soy Casquito. Yo te digo una pista y tú adivinas de qué invento hablo. ¡A jugar!",
                List.of(
                        new Dato("¡LA RUEDA!", "Redondita y girando, lleva a la casa rodando. Soy un invento muy antiguo que hoy mueve autos y bicis. ¿Qué soy?", "Progreso"),
                        new Dato("¡LA BOMBILLA!", "Nací en un tubito y doy luz en la noche. Sin mí, el cuento se queda en la oscuridad. ¿Qué soy?", "Luz"),
                        new Dato("¡LA COMPUTADORA!", "Tengo pantalla, teclas y guardo tus dibujos. Pienso rapidísimo y me preguntan de todo. ¿Qué soy?", "Mente"))));

        TRIVIAS.add(new Trivia("adivina", "Adivina el invento volador", "Curiosidad",
                "tecnología,adivinanzas,el avión,el globo,el satélite,curiosidad",
                "¡Hola! Soy Sofía. Escucha bien: vamos a adivinar inventos que surcan el cielo.",
                List.of(
                        new Dato("¡EL AVIÓN!", "Tengo alas grandes y subo al cielo con pasajeros. Vuelo sobre los mares sin cansarme. ¿Qué soy?", "Valentía"),
                        new Dato("¡EL GLOBO!", "Soy gordo y redondo, floto sin motor porque soy más leve que el aire. ¿Qué soy?", "Ligereza"),
                        new Dato("¡EL SATÉLITE!", "Giro alrededor de la Tierra y traigo comunicaciones y mapas. ¿Qué soy?", "Conexión"))));
    }

    static {
        HISTORIAS.add(new Historia("rueda", "La historia de la rueda", "Progreso", "taller",
                "tecnología,historia de la rueda,inventos,progreso",
                "Se inventó hace más de cinco mil años y empezó como un disco de madera para mover cosas pesadas."));
        HISTORIAS.add(new Historia("luz", "La historia de la luz", "Perseverancia", "obra",
                "tecnología,historia de la electricidad,la bombilla,perseverancia",
                "Edison probó miles de materiales antes de encontrar el filamento que alumbrara sin quemarse."));
        HISTORIAS.add(new Historia("volar", "La historia de volar", "Valentía", "parque",
                "tecnología,historia de la aviación,los hermanos Wright,volar",
                "Los hermanos Wright, con un taller de bicicletas, lanzaron el primer vuelo de 12 segundos en 1903."));
        HISTORIAS.add(new Historia("computadora", "La historia de la computadora", "Aprendizaje", "taller",
                "tecnología,historia de la computadora,informática,aprendizaje",
                "Las primeras computadoras eran enormes, del tamaño de una sala, y hoy caben en tu mano."));
    }

    public static List<Ronda> rondasTech() {
        return new ArrayList<>(RONDAS_TECH);
    }

    public static List<Listicle> listicles() {
        return new ArrayList<>(LISTICLES);
    }

    public static List<Trivia> trivias() {
        return new ArrayList<>(TRIVIAS);
    }

    public static List<Historia> historias() {
        return new ArrayList<>(HISTORIAS);
    }

    public static Listicle listicleByIdOrIndex(String s) {
        if (s.matches("\\d+")) return listicleByIndex(Integer.parseInt(s));
        for (Listicle l : LISTICLES) {
            if (l.id().equalsIgnoreCase(s) || l.titulo().equalsIgnoreCase(s)) return l;
        }
        throw new IllegalArgumentException("No se encontró el listicle '" + s + "'.");
    }

    public static Trivia triviaByIdOrIndex(String s) {
        if (s.matches("\\d+")) return triviaByIndex(Integer.parseInt(s));
        for (Trivia t : TRIVIAS) {
            if (t.id().equalsIgnoreCase(s) || t.titulo().equalsIgnoreCase(s)) return t;
        }
        throw new IllegalArgumentException("No se encontró la trivia '" + s + "'.");
    }

    public static Historia historiaByIdOrIndex(String s) {
        if (s.matches("\\d+")) return historiaByIndex(Integer.parseInt(s));
        for (Historia h : HISTORIAS) {
            if (h.id().equalsIgnoreCase(s) || h.titulo().equalsIgnoreCase(s)) return h;
        }
        throw new IllegalArgumentException("No se encontró la historia '" + s + "'.");
    }

    public static Listicle listicleByIndex(int i) {
        return LISTICLES.get(Math.floorMod(i - 1, LISTICLES.size()));
    }

    public static Trivia triviaByIndex(int i) {
        return TRIVIAS.get(Math.floorMod(i - 1, TRIVIAS.size()));
    }

    public static Ronda rondaTechByIdOrIndex(String s) {
        if (s.matches("\\d+")) return rondaTechByIndex(Integer.parseInt(s));
        for (Ronda r : RONDAS_TECH) {
            if (r.id().equalsIgnoreCase(s) || r.titulo().equalsIgnoreCase(s)) return r;
        }
        throw new IllegalArgumentException("No se encontró la ronda de tecnología '" + s + "'.");
    }

    public static Ronda rondaTechByIndex(int i) {
        return RONDAS_TECH.get(Math.floorMod(i - 1, RONDAS_TECH.size()));
    }

    public static Historia historiaByIndex(int i) {
        return HISTORIAS.get(Math.floorMod(i - 1, HISTORIAS.size()));
    }

    /** Respaldo sin IA: una mini-historia sencilla del invento con su valor. */
    public static ScriptParser.Script fallbackHistoria(Historia h) {
        List<Scene> scenes = new ArrayList<>();
        addS(scenes, 1, "Casquito", hookDe(h), h.escenario());
        addS(scenes, 2, "Tito", antesDe(h), h.escenario());
        addS(scenes, 3, "Sofía", procesoDe(h), h.escenario());
        addS(scenes, 4, "Nico", datoDe(h), h.escenario());
        addS(scenes, 5, "Casquito", valorDe(h), h.escenario());
        return new ScriptParser.Script(h.titulo(), h.valor() + ". Historia de la tecnología para niños.", scenes);
    }

    private static void addS(List<Scene> scenes, int n, String ch, String tx, String esc) {
        Scene sc = new Scene(n, ch, tx);
        sc.scenario = esc;
        scenes.add(sc);
    }

    private static String hookDe(Historia h) {
        return switch (h.id()) {
            case "rueda" -> "¿Cuántas cosas usan ruedas hoy? Bicis, autos, carritos... ¡casi todo! Pero ¿quién inventó la rueda?";
            case "luz" -> "¿Cuántas veces al día tocas un interruptor y se hace la luz? ¡Eso se lo debes a un invento enorme!";
            case "volar" -> "¿Te imaginas mirar el cielo y ver a tu transporte pasar por encima? A todos nos gustaría volar. ¡Te cuento cómo empezó!";
            default -> "¿Sabes cómo nació un invento que hoy usamos todos los días? ¡Te lo cuento en un segundo!";
        };
    }

    private static String antesDe(Historia h) {
        return switch (h.id()) {
            case "rueda" -> "Antes de la rueda, para mover las cosas pesadas había que arrastrarlas o cargarlas. ¡Trabajos agotadores!";
            case "luz" -> "Antes de la bombilla, las casas se alumbraban con velas y lámparas de aceite, y todo se llenaba de humo.";
            case "volar" -> "Antes de volar, los humanos solo miraban las aves con deseo. La gente creía que jamás podríamos dejar el suelo.";
            default -> "Antes, guardar información era lento: se escribía a mano y cada libro tardaba meses en copiarse.";
        };
    }

    private static String procesoDe(Historia h) {
        return switch (h.id()) {
            case "rueda" -> "Hace más de cinco mil años, alguien cortó un disco de madera, le hizo un agujero en el centro y lo puso al lado de una carreta. ¡Y todo empezó a rodar!";
            case "luz" -> "Edison, en su taller, probó más de seis mil materiales buscando un hilo que alumbrara mucho tiempo sin quemarse.";
            case "volar" -> "Los hermanos Wright, en un taller de bicicletas, construyeron una máquina con alas y motor. Nadie lo había logrado antes.";
            default -> "Las primeras computadoras eran enormes, del tamaño de una sala completa, y pocas personas podían tocarlas.";
        };
    }

    private static String datoDe(Historia h) {
        return "Aquí va el dato increíble: " + h.datoClave();
    }

    private static String valorDe(Historia h) {
        return switch (h.id()) {
            case "rueda" -> "Ese disco sencillo mueve el mundo entero. ¡La historia empieza con una idea simple y mucha observación!";
            case "luz" -> "Eso es perseverancia: intentar, intentar e intentar hasta alumbrar. ¡Y así se iluminó el mundo!";
            case "volar" -> "Eso es valentía: creer en tu sueño aunque nadie lo haya logrado. ¡Y ahora todos podemos mirar el cielo desde arriba!";
            default -> "Eso es aprendizaje continuo: cada invento nos enseña a hacerlo mejor. ¿Qué invento te gustaría crear tú?";
        };
    }

    public static Path writePlan(Path dir) throws Exception {
        Files.createDirectories(dir);
        StringBuilder md = new StringBuilder("# TECNOLOGÍA - plan del canal (rondas, listas, trivias e historias)\n\n");

        md.append("## Rondas de tecnología\n\n");
        for (Ronda r : RONDAS_TECH) {
            md.append("- **").append(r.titulo()).append("** (").append(r.escenario()).append(" · ").append(r.bpm())
                    .append(" bpm): ").append(r.descripcion()).append("\n");
        }

        md.append("\n## Listas de 5 datos\n\n");
        for (Listicle l : LISTICLES) {
            md.append("- **").append(l.titulo()).append("** (").append(l.valor()).append("):\n");
            int n = 1;
            for (Dato d : l.datos()) {
                md.append("  ").append(n++).append(". **").append(d.palabra()).append("** - ")
                        .append(d.narra()).append("\n");
            }
        }

        md.append("\n## Trivias\n\n");
        for (Trivia t : TRIVIAS) {
            md.append("- **").append(t.titulo()).append("** (").append(t.valor()).append("): ");
            md.append(String.join(" | ", t.items().stream().map(Dato::palabra).toList())).append("\n");
        }

        md.append("\n## Mini-historias\n\n");
        for (Historia h : HISTORIAS) {
            md.append("- **").append(h.titulo()).append("** (").append(h.valor()).append(" · ")
                    .append(h.escenario()).append("): ").append(h.datoClave()).append("\n");
        }

        Path mdPath = dir.resolve("TECNOLOGIA.md");
        Files.write(mdPath, md.toString().getBytes(StandardCharsets.UTF_8));
        return mdPath;
    }

    private TechLibrary() {}
}