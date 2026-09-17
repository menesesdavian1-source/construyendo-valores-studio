package com.construyendovalores.studio;

import java.util.ArrayList;
import java.util.List;

/**
 * Series propias del canal para el turno "serie" (rotación de 4 días):
 * <ol>
 *   <li>La pregunta de hoy (trivia de comentarios)</li>
 *   <li>Tito en 60 segundos (mini cuentos serializados)</li>
 *   <li>Inventor secreto (mini biografías de inventores)</li>
 *   <li>Canta y aprende (canciones educativas)</li>
 *   <li>Reto en familia (reto para jugar en casa)</li>
 * </ol>
 * Contenido fijo y sin IA para que sea reproducible por --semanario y --diario.
 */
public final class SerieLibrary {

    private static final List<TechLibrary.Trivia> PREGUNTAS = new ArrayList<>();
    private static final List<ScriptParser.Script> MICROCUENTOS = new ArrayList<>();
    private static final List<ScriptParser.Script> INVENTORES = new ArrayList<>();
    private static final List<Ronda> CANTONES = new ArrayList<>();
    private static final List<Ronda> RETOS = new ArrayList<>();
    private static final List<ScriptParser.Script> CUENTOS = new ArrayList<>();

    static {
        PREGUNTAS.add(new TechLibrary.Trivia("pq-luz", "La pregunta de hoy: ¿Quién encendió la luz?", "Perseverancia",
                "la pregunta de hoy,adivinanzas,la bombilla,edison,curiosidades para niños",
                "¡Hola, curiosos! Soy Casquito. La pregunta de hoy: ¿quién probó miles de materiales hasta lograr "
                        + "encender la luz eléctrica? Tres pistas... ¡y cuéntanos tu respuesta en los comentarios!",
                List.of(
                        new TechLibrary.Dato("¡EDISON!", "Pista uno: trabajaba en un taller lleno de herramientas y jamás se daba por vencido.", "Perseverancia"),
                        new TechLibrary.Dato("¡TOMÁS!", "Pista dos: su nombre termina como 'león' y como 'Dios'... casi, casi. ¿Se les ocurre?", "Concentración"),
                        new TechLibrary.Dato("¡LA BOMBILLA!", "Pista tres: cada vez que enciendes una luz en casa estás usando su invento. ¿Quién es?", "Luz"))));

        PREGUNTAS.add(new TechLibrary.Trivia("pq-guepardo", "La pregunta de hoy: ¿Cuál es el animal más rápido?", "Curiosidad",
                "la pregunta de hoy,animales,el guepardo,curiosidades para niños",
                "¡Hola! Soy Nico. La pregunta de hoy: ¿cuál es el animal más veloz corriendo por la tierra? "
                        + "Tres pistas y a responder en los comentarios.",
                List.of(
                        new TechLibrary.Dato("¡EL GUEPARDO!", "Pista uno: tiene manchitas y corre más rápido que un carro en la ciudad.", "Velocidad"),
                        new TechLibrary.Dato("¡CIEN!", "Pista dos: alcanza casi los cien kilómetros por hora en unos segundos.", "Asombro"),
                        new TechLibrary.Dato("¡LA COLA!", "Pista tres: usa su larga cola para no caerse al girar. ¿Ya sabes quién es?", "Equilibrio"))));

        PREGUNTAS.add(new TechLibrary.Trivia("pq-invierno", "La pregunta de hoy: ¿Qué les pasa a los árboles en invierno?", "Observación",
                "la pregunta de hoy,naturaleza,el invierno,los árboles,curiosidades para niños",
                "¡Hola! Soy Sofía. Miren por la ventana: hoy pregunto, ¿qué les pasa a los árboles cuando llega el "
                        + "invierno? ¡Pistas y comentarios!",
                List.of(
                        new TechLibrary.Dato("¡PIERDEN SUS HOJAS!", "Pista uno: en muchas ciudades, en invierno los árboles quedan desnuditos.", "Cambio"),
                        new TechLibrary.Dato("¡SE DUERMEN!", "Pista dos: descansan en secreto hasta que vuelve el calor y florecen otra vez.", "Paciencia"),
                        new TechLibrary.Dato("¡LA PRIMAVERA!", "Pista tres: cuando regresa el sol brotan hojas nuevas. ¿Qué estación es esa?", "Renovación"))));

        PREGUNTAS.add(new TechLibrary.Trivia("pq-perro", "La pregunta de hoy: ¿Cuántas patas tiene un perro?", "Atención",
                "la pregunta de hoy,animales,los perros,curiosidades para niños",
                "¡Hola! Soy Tito. La pregunta de hoy es muy fácil si miras bien: ¿cuántas patas tiene un perro? "
                        + "Cuenta conmigo y responde en los comentarios.",
                List.of(
                        new TechLibrary.Dato("¡CUATRO!", "Pista uno: dos adelante y dos atrás, para correr y saltar.", "Atención"),
                        new TechLibrary.Dato("¡CUATRO!", "Pista dos: es el mismo número de ruedas que tiene un auto.", "Repetición"),
                        new TechLibrary.Dato("¡CUATRO PATAS!", "Pista tres: wof, wof, ¡ya lo sabías! ¡Escríbelo en los comentarios!", "Alegría"))));

        PREGUNTAS.add(new TechLibrary.Trivia("pq-avion", "La pregunta de hoy: ¿Qué invento te lleva volando?", "Imaginación",
                "la pregunta de hoy,inventos,el avión,volando,curiosidades para niños",
                "¡Hola! Soy Casquito. La pregunta de hoy: ¿qué invento de los hermanos Wright te lleva por el cielo "
                        + "con pasajeros? ¡Tres pistas y a volar en los comentarios!",
                List.of(
                        new TechLibrary.Dato("¡EL AVIÓN!", "Pista uno: tiene dos alas grandes y motores que rugen.", "Valentía"),
                        new TechLibrary.Dato("¡DOCE SEGUNDOS!", "Pista dos: su primer vuelo duró solo doce segundos.", "Asombro"),
                        new TechLibrary.Dato("¡EL CIELO!", "Pista tres: hoy cruza el mar con cientos de pasajeros a bordo. ¿Qué es?", "Sueños"))));

        PREGUNTAS.add(new TechLibrary.Trivia("pq-luciernaga", "La pregunta de hoy: ¿Qué brilla en la noche?", "Curiosidad",
                "la pregunta de hoy,naturaleza,la luciérnaga,curiosidades para niños",
                "¡Hola, soñadores! Soy Luna. Miren conmigo el cielo y el bosque: pregunto, ¿qué brilla de noche y parece "
                        + "una estrellita que vuela? ¡Cuéntanos en los comentarios!",
                List.of(
                        new TechLibrary.Dato("¡LA LUCIÉRNAGA!", "Pista uno: es chiquita, brilla en el bosque y no es ninguna estrella.", "Asombro"),
                        new TechLibrary.Dato("¡CON SU LUZ JUEGA!", "Pista dos: prende y apaga su luz para hablar con sus amigos.", "Lenguaje"),
                        new TechLibrary.Dato("¡LA LUZ BONITA!", "Pista tres: cuando la noche da un poco de miedo, su lucecita nos dice que todo está bien. ¿Qué es?", "Calma"))));
    }

    static {
        MICROCUENTOS.add(script("La torre de bloques de Tito",
                "Tito aprende que caerse no es rendirse.",
                new String[][]{
                        {"Casquito", "¿A dónde llegará la torre de bloques de Tito si sigue y sigue? ¡Síganme!", "obra"},
                        {"Tito", "¡Llevo ocho bloques... pero cataplún! ¡Mi torre se cayó otra vez!", "obra"},
                        {"Sofía", "Fíjate, Tito: la base está torcida. Pon abajo los bloques grandes y la torre se sostendrá.", "obra"},
                        {"Tito", "¡Doce bloques y firme! Caerse no es malo si cada vez construimos mejor. ¡Perseverancia!", "obra"}}));

        MICROCUENTOS.add(script("La rueda prestada de Tito",
                "Compartir hace más divertido jugar.",
                new String[][]{
                        {"Casquito", "Tito tiene un carrito de ruedas muy veloz. Hoy el patio está lleno de amigos...", "parque"},
                        {"Tito", "¡Miren mi carrito! Pero... Nico se quedó mirando sin nada que empujar.", "parque"},
                        {"Sofía", "Compartir no quita lo tuyo: dos amigos empujando juntos cargan el doble de piedras.", "parque"},
                        {"Tito", "¡Toma, Nico, empuja tú! Con una rueda y dos amigos, ¡el juego es mejor! ¡Compartir!", "parque"}}));

        MICROCUENTOS.add(script("El tren de las semillas",
                "Sin parar, como un tren, se llega a la meta.",
                new String[][]{
                        {"Casquito", "Al pie de la montaña hay semillas que quieren llegar muy arriba. ¿Quién las sube?", "obra"},
                        {"Tito", "Yo haré las veces de tren: una vuelta, dos vueltas, tres... ¡esto sigue y sigue!", "obra"},
                        {"Nico", "Vagón a vagón, sin parar. Cada semilla plantada es un paso más a la cima.", "obra"},
                        {"Tito", "¡Llegamos! Arriba crecerán árboles nuevos. Sin parar, como el tren: ¡constancia!", "parque"}}));

        MICROCUENTOS.add(script("El robot que aprendió a pintar",
                "Aprender paso a paso también es un baile.",
                new String[][]{
                        {"Casquito", "En el taller hay un robot muy nuevo. Quiere pintar un corazón, pero solo sabe hacer líneas rectas.", "taller"},
                        {"Tito", "¡Línea, línea, línea! ¡No me sale el corazón!", "taller"},
                        {"Sofía", "Primero aprende una curva, luego otra. Paso a paso, todo es posible.", "taller"},
                        {"Tito", "¡Curva aquí, curva allá... es un corazón! Lo logramos paso a paso. ¡Aprendizaje!", "taller"}}));

        MICROCUENTOS.add(script("El puente para las hormigas",
                "Una pequeña ayuda hace un gran camino.",
                new String[][]{
                        {"Casquito", "En el patio hay un charco muy grande y las hormigas no saben cruzar. ¡Necesitan ayuda!", "parque"},
                        {"Sofía", "Una tabla no tan larga... ¿será suficiente? Las hormigas van de aquí para allá.", "parque"},
                        {"Nico", "¡Cuidado! La tabla se mueve. Pongamos piedras de apoyo para que no se caiga.", "parque"},
                        {"Tito", "¡Mira, ya cruzan una, dos, tres hormigas! Ayudar a otros, por pequeños que sean, es amor en acción.", "parque"}}));

        MICROCUENTOS.add(script("La estrella de Tito",
                "Cada sueño es una estrella en el cielo.",
                new String[][]{
                        {"Casquito", "Es de noche y Tito mira el cielo. Hay una estrellita que brilla más que todas. ¿Qué deseará?", "parque"},
                        {"Tito", "Si pudiera pedir un deseo por cada estrella, regalaría casas para los pájaros.", "parque"},
                        {"Sofía", "Los deseos se construyen: primero dibuja tu sueño y luego da el primer pasito.", "obra"},
                        {"Tito", "Mañana dibujaré mi casa para pájaros y construiré una de verdad. ¡Mi imaginación es el techo!", "obra"}}));

        MICROCUENTOS.add(script("El cuento de Tico y la semilla",
                "Con paciencia, las semillas se vuelven árboles.",
                new String[][]{
                        {"Casquito", "Tico tiene un cuento de los antiguos, uno que viaja de abuelo en abuelo. ¿Nos sentamos a escucharlo?", "obra"},
                        {"Tico", "Érase una vez una semillita que esperaba, esperaba, bajo la tierra fría.", "obra"},
                        {"Pipo", "¡Yo no puedo esperar! Voy a brincar y brincar mientras crece. ¡Ja, ja, ja!", "parque"},
                        {"Tico", "Brincando no crece; regando y con paciencia, la semilla se hace árbol.", "obra"},
                        {"Luna", "Y el árbol dio sombra y frutos para todos. ¡Qué bonito es esperar con amor!", "parque"}}));

        MICROCUENTOS.add(script("La risa de Pipo",
                "La alegría se contagia y ayuda a arreglar todo.",
                new String[][]{
                        {"Casquito", "En el taller algo se rompió y nadie sabía qué hacer. Pipo llegó riendo desde la puerta.", "taller"},
                        {"Pipo", "¡Aplasté mi naranja de nuevo, ja, ja! ¡A mí también me pasa! ¡Riamos y luego arreglamos!", "taller"},
                        {"Nico", "La risa de Pipo se me contagió... ¡y se me olvidó el susto!", "taller"},
                        {"Luna", "Reír juntos es gratis y aligera el corazón. ¡Qué superpoder!", "taller"},
                        {"Casquito", "Alegría: todo se arregla mejor si lo arreglamos sonriendo.", "taller"}}));

        MICROCUENTOS.add(script("El búho Memo y la estrellita",
                "La calma ayuda a escuchar y a encontrar respuestas.",
                new String[][]{
                        {"Casquito", "Cuando el taller se puso ruidoso, todos corrieron a buscar a Memo para que los calmara.", "obra"},
                        {"Memo", "Shhh... si callamos un poquito, el corazón tiene espacio para escuchar. Como la noche, que escucha a las estrellas.", "obra"},
                        {"Nico", "Cerré los ojos, conté despacito... y la idea que buscaba apareció sola.", "obra"},
                        {"Luna", "Memo, la estrellita más pequeña también se escucha cuando hay silencio.", "obra"},
                        {"Memo", "Calma: pausa, respira y escucha. La respuesta llega solita.", "obra"}}));
    }

    static {
        INVENTORES.add(script("Inventor secreto: Thomas Edison",
                "La luz se enciende gracias a la perseverancia.",
                new String[][]{
                        {"Casquito", "¿Quién fue el inventor secreto que alumbró el mundo entero? La pista es su apellido: Edison.", "taller"},
                        {"Sofía", "En su taller probó más de seis mil materiales buscando un hilo que diera luz sin quemarse.", "taller"},
                        {"Nico", "Mil veces pareció imposible y mil veces volvió a intentar con una idea nueva.", "taller"},
                        {"Tito", "Y una noche la bombilla brilló durante cuarenta horas. ¡Perseverancia que iluminó al mundo!", "taller"}}));

        INVENTORES.add(script("Inventor secreto: el teléfono de Bell",
                "La voz viajó por un hilo y acercó a las familias.",
                new String[][]{
                        {"Casquito", "¿Sabes quién enseñó al mundo a hablar de lejos? El inventor secreto de hoy es Graham Bell.", "obra"},
                        {"Sofía", "Trabajaba con personas sordas y creía que la voz podía viajar por un hilo eléctrico.", "obra"},
                        {"Nico", "En 1876, en un taller, sonó el teléfono por primera vez: ¡Watson, venga acá!", "obra"},
                        {"Tito", "Desde entonces, papá y abuela hablan a muchos kilómetros de distancia. ¡La voz unió familias!", "obra"}}));

        INVENTORES.add(script("Inventor secreto: Marie Curie y la ciencia",
                "La curiosidad de Marie Curie abrió caminos.",
                new String[][]{
                        {"Casquito", "La inventora secreta de hoy es Marie Curie: descubrió cosas que brillan en la oscuridad.", "taller"},
                        {"Sofía", "Trabajaba día y noche en su laboratorio y nunca dejó de preguntar por qué.", "taller"},
                        {"Tito", "Estudió tanto que fue la primera persona en ganar dos premios Nobel.", "taller"},
                        {"Nico", "Sus descubrimientos ayudaron a sanar a muchas personas. ¡La curiosidad también salva vidas!", "taller"}}));

        INVENTORES.add(script("Inventor secreto: el niño del trampolín",
                "A los 16 años, un niño inventó el trampolín.",
                new String[][]{
                        {"Casquito", "¿Sabías que el trampolín lo inventó un niño de 16 años? Ese es el inventor secreto de hoy.", "parque"},
                        {"Tito", "George miraba a los artistas del circo saltar y pensó: ¿y si el que salta pudiera saltar más alto?", "parque"},
                        {"Nico", "Con tela resistente probó en su propio patio hasta lograr el primer salto.", "parque"},
                        {"Sofía", "Su idea dio saltos por todo el mundo. ¡Tú también puedes inventar jugando!", "parque"}}));
    }

    static {
        CANTONES.add(new Ronda("canta-numeros", "Canta y aprende los números", 118, "parque", "Aprendizaje",
                "La serie educativa de Construyendo Valores: cantamos para aprender los números del uno al diez.",
                "canta y aprende,números,números para niños,canciones educativas,música infantil",
                "¡Hola, científicos! Soy Sofía. Hoy cantamos y aprendemos los números del uno al diez. ¡Todos a contar!",
                List.of(
                        new Ronda.RondaLine("Uno, dos y tres, contamos otra vez,", "qE4 eG4 eA4 qG4 eE4 eD4 qD4 hC4"),
                        new Ronda.RondaLine("cuatro, cinco y seis, ya los sabes de una vez.", "qC5 eB4 eA4 qG4 eA4 qG4 eE4 eD4 hC4"),
                        new Ronda.RondaLine("siete, ocho, nueve, diez,", "qE4 eG4 eA4 qG4 eE4 eD4 qD4 hC4"),
                        new Ronda.RondaLine("¡y contando otra vez, ya sabes quién eres!", "qC5 eB4 eA4 qG4 eA4 eG4 qE4 hD4"))));

        CANTONES.add(new Ronda("canta-colores", "Canta y aprende los colores", 120, "taller", "Creatividad",
                "Pintamos de colores mientras cantamos: rojo, amarillo, azul y verde.",
                "canta y aprende,colores,colores para niños,canciones educativas,pintura",
                "¡Hola, artistas! Soy Nico. Hoy pintamos de colores mientras cantamos. ¡A mezclar!",
                List.of(
                        new Ronda.RondaLine("Rojo como la manzana,", "eE4 eE4 eE4 eE4 eC4 eD4 qE4 eE4 eE4 eE4 eC4 eD4 qE4"),
                        new Ronda.RondaLine("amarillo como el sol,", "eG4 eA4 eG4 eF4 eE4 eD4 qE4 qR"),
                        new Ronda.RondaLine("azulito como el cielo", "qE4 eE4 eE4 eE4 eC4 eD4 qE4 qR"),
                        new Ronda.RondaLine("y verde como el melón.", "eG4 eF4 eE4 eD4 qC4 qR"))));

        CANTONES.add(new Ronda("canta-abece", "Canta y aprende el abecedario", 116, "obra", "Curiosidad",
                "Cantamos de la A a la Z para que las letras se queden en nuestra cabeza.",
                "canta y aprende,abecedario,las letras,canciones educativas,para niños",
                "¡Hola, lectores! Soy Tito. Hoy cantamos de la A a la Z para aprender las letras. ¡Todos a cantar!",
                List.of(
                        new Ronda.RondaLine("A, B, C, D, E, F, G,", "qC4 eD4 eE4 qG4 eE4 eD4 hE4"),
                        new Ronda.RondaLine("la H y la I son amigas de la J.", "eC4 eD4 eE4 eG4 eE4 eD4 qE4 hD4"),
                        new Ronda.RondaLine("K, L, M, N, Ñ, O, P,", "eC4 eD4 eE4 qG4 eA4 eG4 qE4 eD4 eE4 eF4 hE4"),
                        new Ronda.RondaLine("¡y con todas las letras ya se puede leer!", "eC4 eD4 eE4 eG4 eA4 qG4 eE4 eD4 hC4"))));

        CANTONES.add(new Ronda("canta-habitos", "Canta y aprende los hábitos", 122, "taller", "Responsabilidad",
                "Hábitos que nos cuidan: manos limpias, dientes brillantes y dormir temprano.",
                "canta y aprende,hábitos para niños,lavarse las manos,cepillarse,canciones educativas",
                "¡Hola! Soy Casquito. Hoy cantamos y aprendemos hábitos que nos cuidan: manos y dientes brillantes.",
                List.of(
                        new Ronda.RondaLine("Lavarse las manos, pon, pon, pon,", "qC4 eE4 eG4 eC5 eB4 qG4 eG4 hD4"),
                        new Ronda.RondaLine("con agua y jabón, pon, pon, pon.", "qC4 eE4 eG4 eC5 eB4 qE4 eG4 hE4"),
                        new Ronda.RondaLine("Cepillar los dientes con paciencia", "qC4 eE4 eG4 eC5 eA4 qG4 eA4 hF4"),
                        new Ronda.RondaLine("y dormir temprano son hábitos geniales.", "qC4 eE4 eG4 eC5 eA4 qE4 eG4 hC4"))));

        CANTONES.add(new Ronda("canta-animales", "Canta y aprende los animales", 114, "parque", "Amistad",
                "Conocemos a los animales y sus sonidos: guau, miau y cuac. ¡Todos son amigos!",
                "canta y aprende,animales,los animales para niños,canciones educativas,la granja",
                "¡Hola, amigos! Soy Sofía. Hoy las mascotas cantan con nosotros: conozcamos a los animales y sus sonidos.",
                List.of(
                        new Ronda.RondaLine("El perro dice guau, guau, guau,", "qC4 eE4 eG4 eC5 qB4 eA4 eG4 qE4 eD4 hC4"),
                        new Ronda.RondaLine("el gato dice miau y el pato cuac, cuac.", "qC4 eE4 eG4 eC5 qB4 eA4 eG4 qG4 hE4"),
                        new Ronda.RondaLine("Todos los animales son amigos", "qE4 eG4 eA4 qG4 eE4 qD4 hC4"),
                        new Ronda.RondaLine("¡y cuidarlos con cariño es un superpoder!", "qC4 eD4 eE4 eG4 eA4 qG4 eE4 eD4 hC4"))));

        CANTONES.add(new Ronda("canta-alegria", "Canta y aprende a reír con Pipo", 128, "parque", "Alegría",
                "La alegría se contagia: reímos, sonreímos y cantamos todos juntos.",
                "canta y aprende,la alegría,reír,canciones educativas,para niños,la risa",
                "¡Hola, carcajadas! Soy Pipo. Hoy cantamos para reírnos bien fuerte: ¡ja, ja, ja, a cantar!",
                List.of(
                        new Ronda.RondaLine("Con una risa, jajajá,", "qE4 eG4 eA4 qG4 eE4 eD4 qD4 hC4"),
                        new Ronda.RondaLine("el corazón feliz está,", "qC5 eB4 eA4 qG4 eA4 qG4 eE4 eD4 hC4"),
                        new Ronda.RondaLine("sonríe, brinca, pinta sol,", "qE4 eG4 eA4 qG4 eE4 eD4 qD4 hC4"),
                        new Ronda.RondaLine("¡y con risa todo es mejor!", "qC5 eB4 eA4 qG4 eA4 eG4 qE4 hD4"))));

        CANTONES.add(new Ronda("canta-bruno", "Canta con Bruno: el baile del volquete", 124, "obra", "Trabajo en equipo",
                "El volquete carga, lleva y regresa: juntos la obra avanza.",
                "canta y aprende,el volquete,bruno,trabajo en equipo,canciones educativas,para niños",
                "¡Rrrruuumm! Soy Bruno, el volquete. Canto con mi motor y mis ruedas: ¡todos a cargar y bailar!",
                List.of(
                        new Ronda.RondaLine("Cargo los ladrillos, rum, rum, rum,", "qE4 eG4 eA4 qG4 eE4 eD4 qD4 hC4"),
                        new Ronda.RondaLine("los llevo a la obra uno a uno,", "qC5 eB4 eA4 qG4 eA4 qG4 eE4 eD4 hC4"),
                        new Ronda.RondaLine("giro las ruedas sin parar,", "qE4 eG4 eA4 qG4 eE4 eD4 qD4 hC4"),
                        new Ronda.RondaLine("¡y en equipo todo se arma!", "qC5 eB4 eA4 qG4 eA4 eG4 qE4 hD4"))));
    }

    static {
        RETOS.add(new Ronda("reto-salto", "Reto en familia: el salto estrella", 126, "parque", "Alegría",
                "Pausa el video, párate en tu lugar y salta en familia como una estrella.",
                "reto en familia,salto estrella,actividad para niños,juego en casa,construyendo valores",
                "¡Hola, familias! Soy Nico. Pausa el video, párate en tu lugar y hagamos juntos el salto estrella. ¿Listos?",
                List.of(
                        new Ronda.RondaLine("Salta hacia adelante y salta otra vez,", "qE4 eG4 eA4 qG4 eE4 eD4 qD4 hC4"),
                        new Ronda.RondaLine("abre tus brazos, gira sin caer.", "qC5 eB4 eA4 qG4 eA4 qG4 eE4 eD4 hC4"),
                        new Ronda.RondaLine("agáchate bajo y salta arriba, bien,", "qE4 eG4 eA4 qG4 eE4 eD4 qD4 hC4"),
                        new Ronda.RondaLine("¡y llega a la meta con mamá y papá!", "qC5 eB4 eA4 qG4 eA4 eG4 qE4 hD4"))));

        RETOS.add(new Ronda("reto-estatua", "Reto en familia: la estatua musical", 120, "taller", "Alegría",
                "Cuando suena la música todos bailan; cuando se detiene, ¡a ser una estatua!",
                "reto en familia,la estatua musical,actividad para niños,juego en casa",
                "¡Hola! Soy Tito. Reto en familia: cuando la música suena, todos bailan; cuando se detiene, ¡a ser estatua!",
                List.of(
                        new Ronda.RondaLine("Suena la música, a bailar,", "eE4 eE4 eE4 eE4 eC4 eD4 qE4 eE4 eE4 eE4 eC4 eD4 qE4"),
                        new Ronda.RondaLine("la estatua se tiene que quedar,", "eG4 eA4 eG4 eF4 eE4 eD4 qE4 qR"),
                        new Ronda.RondaLine("una, dos y tres, quietos, quietos,", "qE4 eE4 eE4 eE4 eC4 eD4 qE4 qR"),
                        new Ronda.RondaLine("¡y el que se mueve vuelve a saltar!", "eG4 eF4 eE4 eD4 qC4 qR"))));

        RETOS.add(new Ronda("reto-carcajada", "Reto en familia: la carcajada", 126, "parque", "Alegría",
                "Todos en círculo: uno ríe y los demás lo imitan hasta que la risa es de verdad.",
                "reto en familia,la carcajada,actividad para niños,juego en casa,la risa",
                "¡Hola, familias! Soy Pipo. Reto en familia: reímonos en cadena hasta que la risa se contagie. ¿Listos?",
                List.of(
                        new Ronda.RondaLine("Yo río ja, ja, ja,", "qE4 eG4 eA4 qG4 eE4 eD4 qD4 hC4"),
                        new Ronda.RondaLine("y tú ríes jiji, ji,", "qC5 eB4 eA4 qG4 eA4 qG4 eE4 eD4 hC4"),
                        new Ronda.RondaLine("todos en el círculo,", "qE4 eG4 eA4 qG4 eE4 eD4 qD4 hC4"),
                        new Ronda.RondaLine("¡que no pare la alegría!", "qC5 eB4 eA4 qG4 eA4 eG4 qE4 hD4"))));

        RETOS.add(new Ronda("reto-imita", "Reto en familia: imita al animal", 118, "parque", "Creatividad",
                "Yo digo un animal y todos lo imitan: el cuerpo y el sonido. ¡A crear!",
                "reto en familia,imita al animal,actividad para niños,juego en casa",
                "¡Hola, artistas! Soy Sofía. Yo digo un animal y todos lo imitan: el cuerpo, el sonido... ¡a crear!",
                List.of(
                        new Ronda.RondaLine("Imita al conejo que salta sin parar,", "qE4 eG4 eA4 qG4 eE4 eD4 qD4 hC4"),
                        new Ronda.RondaLine("imita al elefante y su trompa al caminar,", "qC5 eB4 eA4 qG4 eA4 qG4 eE4 eD4 hC4"),
                        new Ronda.RondaLine("imita al gatito que dice miau, miau,", "qE4 eG4 eA4 qG4 eE4 eD4 qD4 hC4"),
                        new Ronda.RondaLine("¡y al final la familia es un zoológico feliz!", "qC5 eB4 eA4 qG4 eA4 eG4 qE4 hD4"))));

        RETOS.add(new Ronda("reto-manos", "Reto en familia: el baile de manos", 124, "taller", "Colaboración",
                "Aplaude, choca y baila solo con las manos. ¡Mucho ritmo en casa!",
                "reto en familia,baile de manos,actividad para niños,juego en casa",
                "¡Hola! Soy Casquito. Reto en familia: aplaude, choca y baila solo con las manos. ¡Mucho ritmo!",
                List.of(
                        new Ronda.RondaLine("Aplaude con una y aplaude con dos,", "qE4 eG4 eA4 qG4 eE4 eD4 qD4 hC4"),
                        new Ronda.RondaLine("choca las manos al ritmo de un son,", "qC5 eB4 eA4 qG4 eA4 qG4 eE4 eD4 hC4"),
                        new Ronda.RondaLine("gira las palmas como ruedas veloz,", "qE4 eG4 eA4 qG4 eE4 eD4 qD4 hC4"),
                        new Ronda.RondaLine("¡y las manos juntas bailan mejor!", "qC5 eB4 eA4 qG4 eA4 eG4 qE4 hD4"))));

        RETOS.add(new Ronda("reto-bloques", "Reto en familia: la torre de los 5 bloques", 120, "parque", "Cooperación",
                "Apila 5 objetos sin usar las manos: en equipo, todo es más fácil.",
                "reto en familia,la torre de bloques,cooperación,actividad para niños,juego en casa",
                "¡Hola, familias! Soy Pipo. Reto en familia: apilemos 5 bloques sin manos. ¡Quien lo intenta, ya gana!",
                List.of(
                        new Ronda.RondaLine("Uno, dos, tres, cuatro, cinco,", "qE4 eG4 eA4 qG4 eE4 eD4 qD4 hC4"),
                        new Ronda.RondaLine("sin manos, ¡qué difícil, tic, tic!,", "qC5 eB4 eA4 qG4 eA4 qG4 eE4 eD4 hC4"),
                        new Ronda.RondaLine("con los pies y respirando,", "qE4 eG4 eA4 qG4 eE4 eD4 qD4 hC4"),
                        new Ronda.RondaLine("¡en equipo lo estás logrando!", "qC5 eB4 eA4 qG4 eA4 eG4 qE4 hD4"))));

        RETOS.add(new Ronda("reto-coco", "Reto en familia: congela el baile", 128, "parque", "Valentía",
                "Bailamos con la música y cuando para... ¡nos quedamos como estatuas!",
                "reto en familia,congela el baile,valentía,juego en casa,actividad para niños",
                "¡Hola, voladores! Soy Coco. A mover el cuerpo con la música y... ¡cuando para, congelados! ¿Quién aguanta mejor?",
                List.of(
                        new Ronda.RondaLine("Baila, baila, mueve el pico,", "qE4 eG4 eA4 qG4 eE4 eD4 qD4 hC4"),
                        new Ronda.RondaLine("si la música para, ni un poquito,", "qC5 eB4 eA4 qG4 eA4 qG4 eE4 eD4 hC4"),
                        new Ronda.RondaLine("mueve el ala y la colita,", "qE4 eG4 eA4 qG4 eE4 eD4 qD4 hC4"),
                        new Ronda.RondaLine("¡el que se mueve, una carcajada nos comparte!", "qC5 eB4 eA4 qG4 eA4 eG4 qE4 hD4"))));
    }

    static {
        CUENTOS.add(script("La torre que besó a las nubes",
                "Un cuento de perseverancia: una torre que soñó con tocar el cielo. Narrado por Tico.",
                new String[][]{
                        {"Casquito", "Buenas noches, soñadores. Tico nos va a contar un cuento de los antiguos.", "obra"},
                        {"Tico", "Érase una noche en que la ciudad dormía y una torre quería besar a las nubes.", "obra"},
                        {"Tico", "Casquito colocaba un bloque, otro bloque y otro más, siempre muy despacito.", "parque"},
                        {"Pipo", "¡Ja, ja, ja! ¡Esos bloques no van a llegar nunca, nunca, nunca!", "parque"},
                        {"Luna", "Shh... en el cielo cada estrella guarda un deseo. Yo deseo que la torre suba.", "parque"},
                        {"Tico", "Pero la base se torció y cataplún: la torre cayó con un gran estruendo.", "obra"},
                        {"Tico", "El silencio cayó sobre el patio. Pipo dejó de reír y bajó la cabeza.", "obra"},
                        {"Luna", "Casquito no se rindió: puso bloques grandes abajo, donde más se necesita.", "parque"},
                        {"Tico", "Bloque a bloque la torre volvió a crecer, esta vez más firme que antes.", "parque"},
                        {"Pipo", "¿Y si la coronamos juntos? ¡Yo sostengo y tú empujas, buen compañero!", "parque"},
                        {"Luna", "La última ventana se encendió... y la torre por fin besó a una nube sonriente.", "obra"},
                        {"Casquito", "Perseverancia: caerse duele una vez, pero levantarse vale para siempre. Buenas noches.", "obra"}}));
    }

    // ------------------------------------------------------------- acceso

    public static List<TechLibrary.Trivia> preguntas() {
        return new ArrayList<>(PREGUNTAS);
    }

    public static List<ScriptParser.Script> microcuentos() {
        return new ArrayList<>(MICROCUENTOS);
    }

    public static List<ScriptParser.Script> inventores() {
        return new ArrayList<>(INVENTORES);
    }

    public static List<Ronda> cantones() {
        return new ArrayList<>(CANTONES);
    }

    public static List<Ronda> retos() {
        return new ArrayList<>(RETOS);
    }

    public static List<ScriptParser.Script> cuentos() {
        return new ArrayList<>(CUENTOS);
    }

    public static ScriptParser.Script cuentoByIndex(int i) {
        return CUENTOS.get(Math.floorMod(i - 1, CUENTOS.size()));
    }

    public static String cuentoDesc(ScriptParser.Script s) {
        return s.description() + "\n\nCuento narrado en la serie Cuentos de Construyendo Valores.\n"
                + "Construyendo Valores: construimos algo más importante que edificios: valores.\n"
                + "Suscríbete y activa la campanita 🔔";
    }

    public static List<String> cuentoTags() {
        return new ArrayList<>(List.of("cuentos para niños", "cuento antes de dormir",
                "perseverancia para niños", "valores para niños", "historias infantiles"));
    }

    public static TechLibrary.Trivia preguntaByIndex(int i) {
        return PREGUNTAS.get(Math.floorMod(i - 1, PREGUNTAS.size()));
    }

    public static ScriptParser.Script microcuentoByIndex(int i) {
        return MICROCUENTOS.get(Math.floorMod(i - 1, MICROCUENTOS.size()));
    }

    public static ScriptParser.Script inventorByIndex(int i) {
        return INVENTORES.get(Math.floorMod(i - 1, INVENTORES.size()));
    }

    public static Ronda cantonByIndex(int i) {
        return CANTONES.get(Math.floorMod(i - 1, CANTONES.size()));
    }

    public static Ronda retoByIndex(int i) {
        return RETOS.get(Math.floorMod(i - 1, RETOS.size()));
    }

    /** Título de un episodio de la serie "Tito en 60 segundos", con su capítulo. */
    public static String titoTitulo(ScriptParser.Script base, int cap) {
        return "Tito en 60 segundos - " + base.title() + " (Cap. " + cap + ")";
    }

    /** Primer escenario no vacío del guion, o el predeterminado. */
    public static String scenarioOf(ScriptParser.Script s, String def) {
        for (Scene sc : s.scenes()) {
            if (sc.scenario != null && !sc.scenario.isBlank()) return sc.scenario;
        }
        return def;
    }

    // ------------------------------------------------------------- desc/tags (una sola fuente de verdad)

    public static String preguntaDesc(TechLibrary.Trivia t) {
        return t.valor() + ". " + t.intro() + "\n\nLa pregunta de hoy: responde en los comentarios y activa la campanita 🔔\n"
                + "Construyendo Valores: construimos algo más importante que edificios: valores.\n" + t.etiquetas();
    }

    public static List<String> preguntaTags(TechLibrary.Trivia t) {
        List<String> tags = new ArrayList<>(DailyVideo.tagsOf(t.etiquetas() != null ? t.etiquetas() : ""));
        tags.add("la pregunta de hoy");
        tags.add("adivinanzas para niños");
        return tags;
    }

    public static String titoDesc(String tituloEpisodio, String baseTitulo) {
        return "Un mini cuento de Tito para aprender y dormir feliz. Hoy: " + baseTitulo + ".\n\n"
                + "Serie \"Tito en 60 segundos\": no te pierdas el próximo capítulo 🔔\n"
                + "Construyendo Valores: construimos algo más importante que edificios: valores.";
    }

    public static List<String> titoTags() {
        return new ArrayList<>(List.of("cuentos para niños", "Tito en 60 segundos",
                "cuentos antes de dormir", "cuentos infantiles"));
    }

    public static String inventorDesc(ScriptParser.Script s) {
        return s.description() + "\n\nInventor secreto: una mini biografía para niños curiosos.\n"
                + "Construyendo Valores: construimos algo más importante que edificios: valores.\n"
                + "Suscríbete y activa la campanita 🔔";
    }

    public static List<String> inventorTags() {
        return new ArrayList<>(List.of("Inventor secreto", "inventos para niños",
                "biografías para niños", "historias para niños"));
    }

    public static String cantaDesc(Ronda r) {
        return r.descripcion() + "\n\nCanta y aprende: la serie de canciones educativas de Construyendo Valores.\n"
                + "Suscríbete y activa la campanita 🔔\n" + r.etiquetas();
    }

    public static List<String> cantaTags(Ronda r) {
        List<String> tags = new ArrayList<>(DailyVideo.tagsOf(r.etiquetas() != null ? r.etiquetas() : ""));
        tags.add("canta y aprende");
        tags.add("canciones educativas");
        return tags;
    }

    public static String retoDesc(Ronda r) {
        return r.descripcion() + "\n\nReto en familia: pausa el video, juega con tu familia y cuéntanos en los comentarios.\n"
                + "Construyendo Valores: valores para toda la familia.\n" + r.etiquetas();
    }

    public static List<String> retoTags(Ronda r) {
        List<String> tags = new ArrayList<>(DailyVideo.tagsOf(r.etiquetas() != null ? r.etiquetas() : ""));
        tags.add("reto en familia");
        tags.add("juego para niños");
        return tags;
    }

    // ------------------------------------------------------------- ayuda

    private static ScriptParser.Script script(String titulo, String desc, String[][] escenas) {
        List<Scene> list = new ArrayList<>();
        for (String[] sc : escenas) {
            Scene s = new Scene(list.size() + 1, sc[0], sc[1]);
            s.scenario = sc[2];
            list.add(s);
        }
        return new ScriptParser.Script(titulo, desc, list);
    }

    private SerieLibrary() {}
}