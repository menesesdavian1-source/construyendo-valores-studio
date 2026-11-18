package com.construyendovalores.studio;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Creador automatico de personajes nuevos:
 *  - Gemini (flash, ya integrado) disena la ficha del personaje en JSON.
 *  - Pollinations (gratis, sin clave) dibuja el retrato oficial.
 *  - Se registra en config para futuras canciones/cuentos (`personaje.<norm>.*` y `personajes.lista`).
 */
public final class PersonajeCreator {
    private PersonajeCreator() {}

    private static final Pattern KV = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");

    private static final List<String> VOCES_EDGE = List.of(
            "es-MX-JorgeNeural", "es-MX-DaliaNeural", "es-AR-TomasNeural", "es-CO-GonzaloNeural",
            "es-CL-CatalinaNeural", "es-ES-ElviraNeural", "es-EC-AndreaNeural", "es-PE-AlexNeural");

    public static final class Personaje {
        public String nombre;
        public String valor;
        public String frase;
        public String color;
        public String escenario;
        public String personalidad;
        public String descripcionArte;
        public String vozEdge;
        public String vozEleven;
    }

    /** Genera la ficha + retrato + tarjeta, registra en config y devuelve la tarjeta .md. */
    public static Path crear(Config cfg, String idea, Path dir, Consumer<String> log) throws Exception {
        Personaje p = disenar(cfg, idea);
        String norm = norm(p.nombre);
        Path carpeta = dir.resolve("personajes").resolve(norm);
        Path md = carpeta.resolve("personaje.md");
        Files.createDirectories(carpeta);

        long seed = cfg.getDouble("personaje." + norm + ".seed", 0d) == 0d
                ? System.currentTimeMillis() % 1_000_000_000L
                : Double.doubleToLongBits(cfg.getDouble("personaje." + norm + ".seed", 0d));

        String artPrompt = "personaje infantil de dibujos animados estilo Pixar/Plim Plim, retrato de cuerpo"
                + " completo, " + p.descripcionArte + ", colores vivos, contorno grueso, expresion amable,"
                + " fondo limpio";
        Path png = carpeta.resolve(norm + ".png");
        boolean ok = Pollinations.download(artPrompt, 720, 1280, seed, png);

        Files.writeString(md, tarjeta(p, artPrompt, ok), StandardCharsets.UTF_8);
        registrar(cfg, p, norm, seed);
        cfg.save();

        log.accept("Nombre     : " + p.nombre);
        log.accept("Valor      : " + p.valor);
        log.accept("Frase      : \"" + p.frase + "\"");
        log.accept("Color      : " + p.color + "  | Escenario: " + p.escenario);
        log.accept("Voz edge   : " + p.vozEdge + "  | Voz ElevenLabs: " + p.vozEleven);
        log.accept("Arte IA    : " + (ok ? png.toAbsolutePath() : "NO generado (revisa red)"));
        log.accept("Ficha      : " + md.toAbsolutePath());
        log.accept("Registrado : personaje." + norm + ".* en config + personajes.lista");
        return md;
    }

    private static Personaje disenar(Config cfg, String idea) throws Exception {
        try {
            String prompt = "Eres el disenador de personajes de un canal infantil que ensena valores."
                    + " Ideas: personaje \"" + idea.trim() + "\". Responde SOLO con JSON valido, sin markdown,"
                    + " sin comentarios, con estas claves exactas y valores entre comillas:"
                    + " nombre, valor, frase, color, escenario, personalidad, descripcionArte, vozEdge, vozEleven."
                    + " Reglas: nombre infantil corto en espanol; valor = una virtud para ninos;"
                    + " frase = saludo/reclamo de <8 palabras;"
                    + " color = hexadecima; escenario en (parque, ciudad, playa, bosque, obra, escuela, granja, espacio);"
                    + " descripcionArte = apariencia fisica, vestuario y detalle unico (2 lineas);"
                    + " vozEdge = una de: " + String.join(", ", VOCES_EDGE) + ";"
                    + " vozEleven = onyx, alloy, shimmer, nova o echo segun genero y edad.";
            String raw = MultiIA.generar(cfg, "Eres el disenador de personajes de un canal infantil.",
                    prompt, s -> {
                    });
            Personaje p = parse(GeminiParser.extractJson(raw));
            if (p.nombre == null || p.nombre.isBlank()) throw new IllegalArgumentException("JSON vacío");
            validar(p);
            return p;
        } catch (Exception e) {
            System.err.println("AVISO: Gemini no diseno el personaje (" + e.getMessage()
                    + "). Usando plantilla local.");
            return plantilla(idea);
        }
    }

    private static Personaje parse(String json) {
        Personaje p = new Personaje();
        Matcher m = KV.matcher(json == null ? "" : json);
        while (m.find()) {
            String k = m.group(1).toLowerCase();
            String v = m.group(2).replace("\\\"", "\"").replace("\\\\", "\\");
            switch (k) {
                case "nombre" -> p.nombre = v;
                case "valor" -> p.valor = v;
                case "frase" -> p.frase = v;
                case "color" -> p.color = v;
                case "escenario" -> p.escenario = v;
                case "personalidad" -> p.personalidad = v;
                case "descripcionarte" -> p.descripcionArte = v;
                case "vozedge" -> p.vozEdge = v;
                case "vozeleven" -> p.vozEleven = v;
                default -> { }
            }
        }
        return p;
    }

    private static void validar(Personaje p) {
        if (p.nombre == null || p.nombre.isBlank()) p.nombre = "Lumi";
        if (p.valor == null || p.valor.isBlank()) p.valor = "Amistad";
        if (p.frase == null || p.frase.isBlank()) p.frase = "¡A construir valores!";
        if (p.color == null || !p.color.matches("#[0-9a-fA-F]{6}")) p.color = "#4FC3F7";
        if (p.escenario == null || p.escenario.isBlank()) p.escenario = "parque";
        if (p.personalidad == null || p.personalidad.isBlank()) p.personalidad = "curioso(a) y alegre";
        if (p.descripcionArte == null || p.descripcionArte.isBlank()) p.descripcionArte = "criatura cabezona con ojos grandes y una mochila";
        boolean okEdge = false;
        for (String v : VOCES_EDGE) {
            if (v.equalsIgnoreCase(p.vozEdge)) {
                okEdge = true;
                break;
            }
        }
        if (!okEdge) p.vozEdge = "es-MX-JorgeNeural";
        if (p.vozEleven == null || !List.of("onyx", "alloy", "shimmer", "nova", "echo").contains(p.vozEleven.toLowerCase())) {
            p.vozEleven = "alloy";
        } else {
            p.vozEleven = p.vozEleven.toLowerCase();
        }
    }

    private static Personaje plantilla(String idea) {
        Personaje p = new Personaje();
        p.nombre = "Lumi";
        p.valor = "Amistad";
        p.frase = "¡Juntos brillamos!";
        p.color = "#4FC3F7";
        p.escenario = "parque";
        p.personalidad = "curiosa y alegre";
        p.descripcionArte = "una criatura luminosa cabezona, ojos grandes, orejas de estrella, mochila de arcoiris"
                + " (inspirada en: " + idea + ")";
        p.vozEdge = "es-EC-AndreaNeural";
        p.vozEleven = "shimmer";
        return p;
    }

    private static String tarjeta(Personaje p, String artPrompt, boolean arteOk) {
        return "# " + p.nombre + "\n\n"
                + "**Valor:** " + p.valor + "\n"
                + "**Frase:** \"" + p.frase + "\"\n"
                + "**Color:** " + p.color + "\n"
                + "**Escenario:** " + p.escenario + "\n"
                + "**Personalidad:** " + p.personalidad + "\n"
                + "**Voz edge-tts:** " + p.vozEdge + "\n"
                + "**Voz ElevenLabs:** " + p.vozEleven + "\n"
                + "**Arte IA:** " + (arteOk ? "generado (png)" : "pendiente") + "\n"
                + "**Prompt de arte:**\n```\n" + artPrompt + "\n```\n";
    }

    private static void registrar(Config cfg, Personaje p, String norm, long seed) {
        cfg.set("personaje." + norm + ".nombre", p.nombre);
        cfg.set("personaje." + norm + ".valor", p.valor);
        cfg.set("personaje." + norm + ".frase", p.frase);
        cfg.set("personaje." + norm + ".color", p.color);
        cfg.set("personaje." + norm + ".escenario", p.escenario);
        cfg.set("personaje." + norm + ".personalidad", p.personalidad);
        cfg.set("personaje." + norm + ".descripcionArte", p.descripcionArte);
        cfg.set("personaje." + norm + ".vozEdge", p.vozEdge);
        cfg.set("personaje." + norm + ".vozEleven", p.vozEleven);
        cfg.set("personaje." + norm + ".seed", String.valueOf(seed));
        List<String> lista = new ArrayList<>();
        for (String s : cfg.get("personajes.lista", "").split(";")) {
            if (!s.isBlank()) lista.add(s.trim());
        }
        if (!lista.contains(norm)) lista.add(norm);
        cfg.set("personajes.lista", String.join(";", lista));
    }

    private static String norm(String s) {
        return s.toLowerCase()
                .replace('\u00e1', 'a').replace('\u00e9', 'e').replace('\u00ed', 'i')
                .replace('\u00f3', 'o').replace('\u00fa', 'u').replace('\u00f1', 'n')
                .replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
    }
}