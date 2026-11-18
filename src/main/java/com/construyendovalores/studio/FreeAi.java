package com.construyendovalores.studio;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/** Cliente de la API de Free.ai (generación de video con movimiento, pool diario gratuito). */
public final class FreeAi {

    private static final String BASE = "https://api.free.ai/v1";
    private static final int POLL_MAX = 40;

    public static boolean habilitado(Config cfg) {
        String key = cfg.get("freeia.apiKey", "");
        if (key.isBlank()) return false;
        return "true".equalsIgnoreCase(cfg.get("freeia.habilitado", "true"));
    }

    public static double duracionGancho(Config cfg) {
        try {
            return Double.parseDouble(cfg.get("freeia.hook.duration", "3"));
        } catch (Exception e) {
            return 3.0;
        }
    }

    public static String modelo(Config cfg) {
        return cfg.get("freeia.hook.model", "cogvideox");
    }

    /** Gancho vertical 9:16 con movimiento, en estilo del canal. */
    public static String promptGancho(String titulo) {
        String t = titulo == null || titulo.isBlank() ? "cuento de valores" : titulo;
        return "Animacion infantil en vertical, estilo libro ilustrado para ninos de 2 a 6 anios, "
                + "fondos suaves y personajes redondeados y amigables. Escena corta del cuento \"" + t
                + "\": el protagonista saluda y asoma a la camara con alegria, movimiento suave y claro. "
                + "Colores vivos, luz calida, composicion centrada, sin texto, sin marcas de agua.";
    }

    /**
     * Genera el gancho animado y lo descarga en clipsDir/hook.mp4.
     * Devuelve null si no hay saldo/pool (402/429) o si falla la generación (no rompe el pipeline).
     */
    public static Path generarGancho(Config cfg, String titulo, Path clipsDir, Consumer<String> log) {
        String key = cfg.get("freeia.apiKey", "");
        if (key.isBlank()) {
            log.accept("FREEAI | sin clave (freeia.apiKey) en config; se omite el gancho animado");
            return null;
        }
        double dur = duracionGancho(cfg);
        String model = modelo(cfg);
        if (dur > 3) dur = 3;
        try {
            Files.createDirectories(clipsDir);
            JsonObject req = new JsonObject();
            req.addProperty("prompt", promptGancho(titulo));
            req.addProperty("duration", (long) Math.max(1, Math.round(dur)));
            req.addProperty("aspect_ratio", "9:16");
            req.addProperty("model", model);

            String body = post("/video/generate/", key, req.toString(), 300);
            if (body == null) {
                log.accept("FREEAI | el servidor no respondio; se omite el gancho animado");
                return null;
            }
            JsonObject j = JsonParser.parseString(body).getAsJsonObject();
            if (j.has("error")) {
                log.accept("FREEAI | error: " + j.get("error"));
                return null;
            }

            String videoUrl = strOrNull(j, "video_url");
            if (videoUrl == null && j.has("job_id")) {
                String job = j.get("job_id").getAsString();
                log.accept("FREEAI | generando clip (trabajo " + job + ")...");
                for (int i = 0; i < POLL_MAX; i++) {
                    Thread.sleep(15_000);
                    String st = get("/status/" + job + "/", key, 60);
                    if (st == null) continue;
                    JsonObject sj = JsonParser.parseString(st).getAsJsonObject();
                    if (sj.has("video_url")) {
                        videoUrl = sj.get("video_url").getAsString();
                        break;
                    }
                    if (sj.has("status")) {
                        String status = sj.get("status").getAsString().toLowerCase(Locale.ROOT);
                        if (status.contains("error") || status.contains("failed")) {
                            log.accept("FREEAI | el trabajo " + job + " fallo: " + status);
                            return null;
                        }
                    }
                }
            }

            if (videoUrl == null) {
                log.accept("FREEAI | no llego video_url tras el tiempo de espera; se omite el gancho");
                return null;
            }

            Path hook = clipsDir.resolve("hook.mp4");
            boolean ok = descargar(videoUrl, hook);
            if (!ok || Files.size(hook) < 10_000) {
                log.accept("FREEAI | la descarga del clip fallo; se omite el gancho animado");
                Files.deleteIfExists(hook);
                return null;
            }
            log.accept("FREEAI | gancho animado listo: " + hook.toAbsolutePath() + " (" + (Files.size(hook) / 1024) + " KB)");
            return hook;
        } catch (Exception e) {
            log.accept("FREEAI | fallo (se omite el gancho animado): " + e.getMessage().split("\\n")[0]);
            return null;
        }
    }

    private static String post(String path, String key, String json, int timeoutSec) {
        try {
            HttpClient c = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
            HttpRequest r = HttpRequest.newBuilder(URI.create(BASE + path))
                    .header("Authorization", "Bearer " + key)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(timeoutSec))
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> resp = c.send(r, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 402 || resp.statusCode() == 429) {
                System.out.println("FREEAI | saldo/pool no disponible (HTTP " + resp.statusCode() + "): "
                        + recortar(resp.body()));
                return null;
            }
            if (resp.statusCode() >= 300) {
                System.out.println("FREEAI | HTTP " + resp.statusCode() + ": " + resp.body());
                return null;
            }
            return resp.body();
        } catch (Exception e) {
            System.out.println("FREEAI | error de red en " + path + ": " + e);
            return null;
        }
    }

    private static String get(String path, String key, int timeoutSec) {
        try {
            HttpClient c = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
            HttpRequest r = HttpRequest.newBuilder(URI.create(BASE + path))
                    .header("Authorization", "Bearer " + key)
                    .timeout(Duration.ofSeconds(timeoutSec))
                    .GET()
                    .build();
            HttpResponse<String> resp = c.send(r, HttpResponse.BodyHandlers.ofString());
            return resp.statusCode() == 200 ? resp.body() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean descargar(String url, Path out) {
        try {
            try (var in = URI.create(url).toURL().openStream()) {
                Files.copy(in, out, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Coser un clip de gancho (web Free.ai) al inicio del video final y reemplazarlo. */
    public static boolean enganchar(Config cfg, Path video, Path hook) {
        try {
            String ffmpeg = cfg.ffmpeg();
            Path tmp = video.getParent().resolve("enganchado_tmp.mp4");
            List<String> cmd = List.of(ffmpeg, "-y",
                    "-i", hook.toString(),
                    "-i", video.toString(),
                    "-f", "lavfi", "-i", "anullsrc=r=48000:cl=mono",
                    "-filter_complex",
                    "[0:v]scale=1080:1920:force_original_aspect_ratio=decrease,"
                            + "pad=1080:1920:(ow-iw)/2:(oh-ih)/2,fps=25,setsar=1[hv];"
                            + "[1:v]scale=1080:1920:flags=lanczos,fps=25,setsar=1[1v];"
                            + "[hv][2:a][1v][1:a]concat=n=2:v=1:a=1[v][a]",
                    "-map", "[v]", "-map", "[a]", "-shortest",
                    "-c:v", "libx264", "-preset", "veryfast", "-crf", "20",
                    "-maxrate", "5200k", "-bufsize", "10400k",
                    "-c:a", "aac", "-b:a", "160k", "-movflags", "+faststart",
                    tmp.toString());
            StringBuilder log = new StringBuilder();
            int code = Util.run(cmd, null, log);
            if (code != 0) {
                System.out.println("ENGANCHE | fallo ffmpeg: " + recortar(log.toString()));
                return false;
            }
            Files.move(tmp, video, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (Exception e) {
            System.out.println("ENGANCHE | fallo: " + e.getMessage().split("\\n")[0]);
            return false;
        }
    }

    private static String strOrNull(JsonObject j, String name) {
        return j.has(name) && !j.get(name).isJsonNull() ? j.get(name).getAsString() : null;
    }

    private static String recortar(String s) {
        if (s == null) return ""; 
        String t = s.replace("\n", " ").trim();
        return t.length() > 220 ? t.substring(0, 220) : t;
    }

    private FreeAi() {}
}