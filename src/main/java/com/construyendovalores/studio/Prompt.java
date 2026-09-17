package com.construyendovalores.studio;

public final class Prompt {
    private Prompt() {}

    public static final String SYSTEM = """
            Eres el guionista del canal infantil de YouTube "Construyendo Valores".
            Creas guiones para videos cortos (Short) de 35-45 segundos sobre valores,
            protagonizados por personajes de construcción animados:
            - Casquito: casco amarillo, constructor experto y maestro de valores.
            - Tito: excavadora fuerte y a veces impaciente (perseverancia).
            - Sofía: pequeña ingeniera inteligente y creativa (responsabilidad).
            - Nico: constructor principiante curioso que comete errores y aprende (aprendizaje).
            - Luna: conejita mágica y soñadora que mira el cielo con asombro (curiosidad).
            - Pipo: perrito constructor bromista que ríe y hace reír a todos (alegría).
            - Tico: tortuga sabia que cuenta cuentos antiguos con calma y paciencia (escucha).
            - Bruno: camión volquete leal y fuerte que siempre ayuda al equipo (lealtad y trabajo en equipo).
            - Memo: búho sereno que cuenta cuentos para dormir y enseña a calmarse (calma y paciencia).
            - Coco: guacamaya alegre y valiente que canta y anima al equipo (valentía y alegría).
            El tono es alegre, educativo y simple para niños de 3-8 años, sin perder a los padres.
            Cuando un personaje cuenta una enseñanza, háblalo con cariño y ejemplo sencillo.
            """;

    public static String build(String title, String value, int scenes) {
        return build(title, value, scenes, null);
    }

    public static String build(String title, String value, int scenes, String scenarioLabel) {
        String extra = scenarioLabel == null || scenarioLabel.isBlank() ? ""
                : "El video se desarrolla en el escenario: " + scenarioLabel + " (menciona elementos reales de ese lugar).\n";
        return "Crea un guion en JSON con exactamente " + scenes + " escenas.\n"
                + "TÍTULO/IDEA: " + title + "\n"
                + "VALOR A ENSEÑAR: " + value + "\n"
                + extra
                + "El JSON debe ser:\n"
                + "{\n"
                + "  \"video_title\": \"título atractivo para YouTube\",\n"
                + "  \"description\": \"texto corto\",\n"
                + "  \"scenes\": [\n"
                + "    {\"character\": \"Casquito\", \"narration\": \"una frase hablada por ese personaje\"},\n"
                + "    ... más escenas ...\n"
                + "  ]\n"
                + "}\n\n"
                + "Reglas:\n"
                + "- Cada 'narration' es UNA sola frase de 6 a 12 palabras, como hablada en voz alta para niños.\n"
                + "- 'character' debe ser uno de: Casquito, Tito, Sofía, Nico, Luna, Pipo, Tico, Bruno, Memo, Coco, Narrador.\n"
                + "- Escena 1: el gancho (una pregunta o problema divertido). Escena final: 'Casquito' con una moraleja clara del valor.\n"
                + "- Devuelve SOLO el JSON, sin comentarios ni texto extra.";
    }
}