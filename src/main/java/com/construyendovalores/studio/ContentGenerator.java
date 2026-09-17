package com.construyendovalores.studio;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Genera con IA los 30 títulos + descripciones + etiquetas del plan del canal,
 * en lotes de 6, y los guarda como CONTENIDO_30.md (y .csv).
 */
public final class ContentGenerator {

    public record Entry(int id, String valor, String formato, String idea) {}

    public static List<Entry> entries() {
        List<Entry> e = new ArrayList<>();
        String[][] rows = {
                {"Perseverancia", "Historia", "Tito intenta levantar la torre mas alta y se le cae 3 veces"},
                {"Perseverancia", "Reto", "construir una torre de bloques sin que se caiga"},
                {"Perseverancia", "Habito", "contar juntos: intenta una vez mas"},
                {"Responsabilidad", "Historia", "Sofia olvida cuidar su casco en el trabajo"},
                {"Responsabilidad", "Reto", "cumplir 3 tareas de la casa hoy"},
                {"Responsabilidad", "Habito", "dejas tus huellitas donde trabajas"},
                {"Aprendizaje", "Historia", "Nico se equivoca al armar la escalera"},
                {"Aprendizaje", "Reto", "cometer un error nuevo y corregirlo"},
                {"Aprendizaje", "Habito", "tu mayor obstaculo puede ser tu maestro"},
                {"Amistad", "Historia", "Tito se atora y sus amigos lo ayudan a salir"},
                {"Amistad", "Reto", "ayudar a alguien sin que te lo pida"},
                {"Amistad", "Habito", "un valor se duplica cuando lo compartes"},
                {"Honestidad", "Historia", "Casquito rompe un cristal y confiesa la verdad"},
                {"Honestidad", "Reto", "decir la verdad aunque cueste"},
                {"Honestidad", "Habito", "la verdad construye mas que la mentira"},
                {"Gratitud", "Historia", "los personajes agradecen sus herramientas"},
                {"Gratitud", "Reto", "decir tres gracias de verdad hoy"},
                {"Gratitud", "Habito", "agradecer convierte lo que tienes en suficiente"},
                {"Empatia", "Historia", "Nico esta triste y los demas lo escuchan"},
                {"Empatia", "Reto", "preguntar hoy como se siente un amigo"},
                {"Empatia", "Habito", "un gran constructor escucha antes de construir"},
                {"Orden", "Historia", "el taller esta desordenado y no se puede trabajar"},
                {"Orden", "Reto", "ordenar tu cuarto en 5 minutos con cronometro"},
                {"Orden", "Habito", "el orden es la calma de la mente"},
                {"Valentia", "Historia", "Sofia tiene miedo de hablar frente a todos"},
                {"Valentia", "Reto", "hacer hoy algo que de un poco de miedo sano"},
                {"Valentia", "Habito", "valiente es avanzar aunque haya miedo"},
                {"Generosidad", "Historia", "Tito no quiere prestar sus bloques nuevos"},
                {"Generosidad", "Reto", "regalar un dibujo o compartir tus bloques"},
                {"Generosidad", "Habito", "al construir para otros, todos ganamos"}
        };
        for (int i = 0; i < rows.length; i++) {
            e.add(new Entry(i + 1, rows[i][0], rows[i][1], rows[i][2]));
        }
        return e;
    }

    public static Path generate(Config cfg, Path outDir, Consumer<String> progress) throws Exception {
        List<Entry> all = entries();

        progress.accept("Generando 30 títulos + descripciones con IA (una sola petición)...");
        String raw = Gemini.generate(cfg, allPrompt(all));
        String json = GeminiParser.extractText(raw);
        int a = json.indexOf('[');
        int b = json.lastIndexOf(']');
        if (a < 0 || b <= a) {
            throw new IllegalStateException("La IA no devolvió un arreglo JSON válido.");
        }
        JsonArray arr = JsonParser.parseString(json.substring(a, b + 1)).getAsJsonArray();
        List<String[]> results = new ArrayList<>();
        for (JsonElement el : arr) {
            JsonObject o = el.getAsJsonObject();
            String id = o.has("id") ? o.get("id").toString().replace("\"", "") : "";
            String title = o.has("title") ? o.get("title").getAsString() : "";
            String desc = o.has("description") ? o.get("description").getAsString() : "";
            String tags = o.has("hashtags") ? o.get("hashtags").getAsString() : "";
            results.add(new String[]{id, title, desc, tags});
        }

        Files.createDirectories(outDir);
        Path md = outDir.resolve("CONTENIDO_30.md");
        StringBuilder sb = new StringBuilder("# CONTENIDO - 30 VIDEOS DE CONSTRUYENDO VALORES (generado con IA)\n\n");
        for (int i = 0; i < results.size() && i < all.size(); i++) {
            Entry en = all.get(i);
            String[] r = results.get(i);
            sb.append("## Video ").append(en.id()).append(" - ").append(en.valor()).append(" (").append(en.formato()).append(")\n\n");
            sb.append("- **Título:** ").append(r.length > 1 && !r[1].isBlank() ? r[1] : str(en.idea())).append("\n");
            sb.append("- **Descripción:** ").append(r.length > 2 && !r[2].isBlank() ? r[2] : "").append("\n");
            sb.append("- **Etiquetas:** ").append(r.length > 3 ? r[3] : "").append("\n\n");
        }
        sb.append("---\n*Generador por Construyendo Valores Studio (IA). Revisa y personaliza antes de publicar.*\n");
        Files.write(md, sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        buildCsv(all, results, outDir.resolve("CONTENIDO_30.csv"));
        return md;
    }

    private static String str(String s) {
        return s == null ? "" : s;
    }

    private static String allPrompt(List<Entry> all) {
        StringBuilder sb = new StringBuilder();
        sb.append("Genera para cada petición (30 en total) un TÍTULO llamativo (40-60 caracteres, con gancho y sin "
                + "clickbait), una DESCRIPCIÓN de 2-3 frases para YouTube (niños 3-8 años, tono cálido y cercano) y "
                + "ETIQUETAS iniciando con #ConstruyendoValores. Cada video enseña un valor con personajes de "
                + "construcción (Casquito, Tito, Sofía y Nico). No repitas títulos. Devuelve SOLAMENTE un arreglo "
                + "JSON completo con 30 objetos:\n"
                + "[{\"id\":1,\"title\":\"...\",\"description\":\"...\",\"hashtags\":\"#...\"}, {...}]\n\n");
        for (Entry e : all) {
            sb.append(e.id()).append(". Valor: ").append(e.valor())
                    .append(" | Formato: ").append(e.formato())
                    .append(" | Idea: ").append(e.idea()).append("\n");
        }
        return sb.toString();
    }

    public static void buildCsv(List<Entry> all, List<String[]> results, Path csv) throws Exception {
        StringBuilder sb = new StringBuilder("id;valor;formato;titulo;descripcion;etiquetas\n");
        for (int i = 0; i < results.size() && i < all.size(); i++) {
            Entry en = all.get(i);
            String[] r = results.get(i);
            sb.append(en.id()).append(';').append(en.valor()).append(';').append(en.formato()).append(';')
                    .append(csv(r.length > 1 ? r[1] : "")).append(';')
                    .append(csv(r.length > 2 ? r[2] : "")).append(';')
                    .append(csv(r.length > 3 ? r[3] : "")).append('\n');
        }
        Files.write(csv, sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static String csv(String s) {
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    private ContentGenerator() {}
}