package com.construyendovalores.studio;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

/** Prepara el paquete "ParaTikTok": video completo + Short de 30 s + ficha con
 *  título, descripción y etiquetas, listo para pegar en TikTok Studio web. */
public final class TikTokPackage {

    public static Path prepare(Config cfg, Path video, String titulo, String desc, List<String> tags,
                               Path baseDir, Consumer<String> log) throws Exception {
        String clean = (titulo == null || titulo.isBlank() ? "video" : titulo)
                .replaceAll("[\\\\/:*?\"<>|]", "_");
        Path bundle = baseDir.resolve("ParaTikTok").resolve(LocalDate.now().toString()).resolve(clean);
        Files.createDirectories(bundle);

        Path full = bundle.resolve(clean + ".mp4");
        Files.copy(video, full, StandardCopyOption.REPLACE_EXISTING);

        log.accept("Generando Short de 30 s para TikTok...");
        Shorts.make(cfg, video, bundle, 30, log);

        Path ficha = bundle.resolve("FICHA.txt");
        String handle = cfg.get("tiktok.handle", "historiasrea28");
        String cleanHandle = handle.replace("@", "").trim();
        String hook = hookFor(titulo);
        String cta = ctaFor(titulo);
        StringBuilder sb = new StringBuilder();
        sb.append("CUENTA TikTok: https://www.tiktok.com/@").append(cleanHandle).append("\n\n");
        sb.append("TITULO SUGERIDO (max 150):\n").append(suggestedTitle(hook, titulo)).append("\n\n");
        sb.append("GANCHO de la primera linea:\n").append(hook).append("\n\n");
        sb.append("DESCRIPCION (max 2200):\n").append(hook).append("\n\n")
                .append(desc == null ? "" : desc).append("\n\n").append(cta).append("\n\n");
        sb.append("PREGUNTA para tu primer comentario:\n")
                .append(primeraComenta(titulo)).append("\n\n");
        sb.append("HASHTAGS:\n").append("#ConstruyendoValores");
        if (tags != null) {
            for (String t : tags) {
                if (t != null && !t.isBlank()) {
                    sb.append(' ').append(t.indexOf('#') == 0 ? t : "#" + t.replace(' ', '_'));
                }
            }
        }
        sb.append("\n\nPASO A PASO (ruta manual):\n")
                .append("1. Entra en https://www.tiktok.com/upload CON la cuenta @").append(cleanHandle)
                .append(" (TikTok Studio).\n")
                .append("2. Sube \"- Short.mp4\" (o el video completo si prefieres).\n")
                .append("3. Pega el titulo sugerido y la descripcion.\n")
                .append("4. Publica y deja tu primer comentario con la pregunta.\n");
        Files.write(ficha, sb.toString().getBytes(StandardCharsets.UTF_8));

        if ("true".equalsIgnoreCase(cfg.get("tiktok.auto", "false")) && TikTokPoster.isConfigured(cfg)) {
            log.accept("Publicando automaticamente en TikTok (@"
                    + cfg.get("tiktok.handle", "historiasrea28") + ")...");
            TikTokPoster.postVideo(cfg, video, titulo, hook + "\n\n" + desc + "\n\n" + cta, log);
        }

        log.accept("TikTok listo: " + bundle.toAbsolutePath() + " (sube el Short de 30 s en TikTok Studio).");
        return bundle;
    }

    private static String suggestedTitle(String hook, String titulo) {
        String t = (titulo == null || titulo.isBlank()) ? "Video nuevo" : titulo;
        String cand = hook.replace(".", "") + " | " + t;
        return cand.length() <= 150 ? cand : t;
    }

    private static String hookFor(String titulo) {
        String t = titulo == null ? "" : titulo.toLowerCase();
        String[] pool;
        if (t.contains("rueda")) {
            pool = new String[]{
                    "Gira y gira: un invento tan simple que mueve el mundo entero. ¿Adivinas cuál es?",
                    "Un disco de madera cambió la historia para siempre. Mira el dato que prefieres."};
        } else if (t.contains("tren")) {
            pool = new String[]{
                    "Chucu-chucu: cada vagón empuja al siguiente, igual que tú con tu esfuerzo diario.",
                    "Este invento conecta ciudades enteras. ¿Sabías que aceleró el progreso?"};
        } else if (t.contains("robot")) {
            pool = new String[]{
                    "Tic-tic-tac: el robot aprendió paso a paso. Al final, ¡baila mejor que nosotros!",
                    "¿Los robots se cansan? Te cuento el secreto mientras ellos trabajan."};
        } else if (t.contains("invento") || t.contains("máquina") || t.contains("mundo")) {
            pool = new String[]{
                    "¿Sabías que un solo invento cambió el mundo entero? Te cuento el dato exacto.",
                    "Los datos más asombrosos caben en 30 segundos. Mira lo que sigue."};
        } else if (t.contains("qué soy") || t.contains("adivina")) {
            pool = new String[]{
                    "TE RETO a adivinar antes de que aparezca la respuesta. Deja tu pista en comentarios.",
                    "Una pista, una respuesta y un premio para los más rápidos: ¿qué soy?"};
        } else if (t.contains("historia")) {
            pool = new String[]{
                    "Todos usan este invento, pero pocas personas conocen su historia.",
                    "Un día, alguien preguntó \"¿y si se puede hacer mejor?\" y cambió todo."};
        } else {
            pool = new String[]{
                    "Prepara tus oídos porque esto te va a encantar (y sorprender).",
                    "¿Cuál crees que es la respuesta? Comenta antes de ver el final."};
        }
        return pool[Math.floorMod(titulo == null ? 0 : titulo.hashCode(), pool.length)];
    }

    private static String ctaFor(String titulo) {
        String t = titulo == null ? "" : titulo.toLowerCase();
        if (t.contains("qué soy") || t.contains("adivina") || t.contains("reto")) {
            return "Acertaste una, dos o tres? Cuéntalo en comentarios y dinos cuál fue la más fácil.";
        }
        if (t.contains("rueda") || t.contains("tren") || t.contains("robot")) {
            return "Comenta cuál es tu favorito de la canción. Y si te gustó, comparte con un amigo constructor.";
        }
        return "Comenta la palabra que más te gustó y dime hasta qué punto llegaste viendo el video.";
    }

    private static String primeraComenta(String titulo) {
        String t = titulo == null ? "" : titulo.toLowerCase();
        if (t.contains("qué soy") || t.contains("adivina")) return "¿Cuántas acertaste? Escribe tu puntaje aqui abajo.";
        if (t.contains("robot")) return "Si fueras un robot: baile, pintura o construir torres? Responde con un numero.";
        if (t.contains("tren")) return "Para ti: cambio palo grande por maquina, sigo, que te cruzas. Escribe SI o NO.";
        if (t.contains("rueda")) return "Menciona una cosa con ruedas que uses hoy y te leo en el proximo video.";
        if (t.contains("historia")) return "Que invento quieres que contemos manana? Deja tu idea aqui.";
        return "Cual es tu dato favorito? Responde con el numero del video.";
    }

    private TikTokPackage() {}
}