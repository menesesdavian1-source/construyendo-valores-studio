package com.construyendovalores.studio;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Kit de publicidad del canal:
 *  - Anuncio vertical (gancho + momento clave + tarjeta de suscripcion) con musica propia.
 *  - Textos de campana por plataforma (TikTok, Instagram, X, comunidad de YouTube).
 *  - Miniaturas A/B reutilizando Cover.
 */
public final class Publicidad {
    private Publicidad() {}

    public static void generar(Config cfg, Path src, Path dir, Consumer<String> log) throws Exception {
        if (src == null || !Files.exists(src)) {
            throw new IllegalArgumentException("El video base no existe: " + src);
        }
        Files.createDirectories(dir);

        String ff = cfg.ffmpeg();
        String ffprobe = Util.ffprobeBin(cfg);
        double dur = Util.mediaDurationSeconds(src, ffprobe);
        if (dur < 14) {
            throw new IllegalArgumentException("El video base dura " + String.format("%.1f", dur)
                    + "s; se necesita al menos 14s para montar el anuncio.");
        }

        double mid = Math.min(Math.max(dur * 0.45, 8), dur - 9);
        int aEnd = (int) Math.min(6, dur - 10);
        int bStart = (int) Math.round(mid);
        int bEnd = (int) Math.min(bStart + 6, dur - 2.5);
        double total = aEnd + (bEnd - bStart) + 5;

        Path fondo = buscarFondo(cfg, src);
        Path out = dir.resolve("anuncio_construyendo_valores.mp4");

        List<String> cmd = new ArrayList<>();
        cmd.add(ff);
        cmd.add("-y");
        cmd.add("-i");
        cmd.add(src.toString());
        if (fondo != null) {
            cmd.add("-i");
            cmd.add(fondo.toString());
        } else {
            cmd.add("-f");
            cmd.add("lavfi");
            cmd.add("-i");
            cmd.add("anullsrc=channel_layout=stereo:sample_rate=44100");
        }
        cmd.add("-filter_complex");
        cmd.add(filtros(aEnd, bStart, bEnd));
        cmd.add("-map");
        cmd.add("[vout]");
        cmd.add("-map");
        cmd.add("[aout]");
        cmd.add("-t");
        cmd.add("" + (int) Math.ceil(total));
        cmd.add("-r");
        cmd.add("30");
        cmd.add("-c:v");
        cmd.add("libx264");
        cmd.add("-preset");
        cmd.add("fast");
        cmd.add("-crf");
        cmd.add("22");
        cmd.add("-c:a");
        cmd.add("aac");
        cmd.add("-b:a");
        cmd.add("160k");
        cmd.add("-ar");
        cmd.add("44100");
        cmd.add("-movflags");
        cmd.add("+faststart");
        cmd.add(out.toString());

        StringBuilder logBuf = new StringBuilder();
        int code = Util.run(cmd, null, logBuf);
        if (code != 0) {
            throw new RuntimeException("ffmpeg no monto un anuncio (" + code + "):\n"
                    + logBuf.substring(0, Math.min(logBuf.length(), 800)));
        }

        Cover.makeVariants("Construyendo Valores | Canciones y cuentos para ninos", "CONSTRUYENDO VALORES", dir);
        Path textos = dir.resolve("textos_publicidad.md");
        Files.writeString(textos, textosCampana(src.getFileName().toString()), StandardCharsets.UTF_8);

        log.accept("Anuncio : " + out.toAbsolutePath() + " (" + String.format("%,d bytes", Files.size(out)) + ")");
        log.accept("Textos  : " + textos.toAbsolutePath());
        log.accept("Miniaturas: 3 variantes generadas en " + dir);
    }

    private static String filtros(int aEnd, int bStart, int bEnd) {
        int bDur = bEnd - bStart;
        String shared = "setsar=1,scale=720:1280:force_original_aspect_ratio=increase,crop=720:1280,format=yuv420p";
        String ft = "fontfile='C\\:/Windows/Fonts/arialbd.ttf'";
        return "[0:v]split=2[va][vb];"
                + "[va]trim=start=0:end=" + aEnd + ",setpts=PTS-STARTPTS," + shared
                + ",drawtext=" + ft + ":text='CONSTRUYENDO VALORES':x=(w-text_w)/2:y=110:fontsize=72:fontcolor=white:borderw=8:bordercolor=black@0.8"
                + ",drawtext=" + ft + ":text='Canciones y cuentos para ninos':x=(w-text_w)/2:y=205:fontsize=38:fontcolor=white:borderw=6:bordercolor=black@0.8"
                + ",fade=t=in:st=0:d=0.5[f1];"
                + "[vb]trim=start=" + bStart + ":end=" + bEnd + ",setpts=PTS-STARTPTS," + shared
                + ",fade=t=in:st=0:d=0.4,fade=t=out:st=" + Math.max(bDur - 0.6, 0.2) + ":d=0.6[f2];"
                + "color=c=black:s=720x1280:d=5.5"
                + ",drawtext=" + ft + ":text='SUSCRIBETE':x=(w-text_w)/2:y=520:fontsize=110:fontcolor=white:borderw=12:bordercolor=black"
                + ",drawtext=" + ft + ":text='Activa la campanita':x=(w-text_w)/2:y=650:fontsize=46:fontcolor=white:borderw=7:bordercolor=black"
                + ",drawtext=" + ft + ":text='para no perderte los estrenos':x=(w-text_w)/2:y=720:fontsize=36:fontcolor=white:borderw=6:bordercolor=black"
                + ",drawtext=" + ft + ":text='Construyendo Valores':x=(w-text_w)/2:y=1130:fontsize=44:fontcolor=#FFD54F:borderw=6:bordercolor=black"
                + ",fade=t=in:st=0:d=0.5[f3];"
                + "[f1][f2][f3]concat=n=3:v=1:a=0[vout];"
                + "[1:a]atrim=0:" + (aEnd + bDur + 5) + ",asetpts=PTS-STARTPTS,volume=0.35"
                + ",afade=t=in:st=0:d=1,afade=t=out:st=" + (aEnd + bDur + 4) + ":d=3[aout]";
    }

    private static Path buscarFondo(Config cfg, Path src) {
        String conf = cfg.musicPath();
        if (conf != null && !conf.isBlank()) {
            Path p = Paths.get(conf);
            if (Files.exists(p)) return p;
        }
        Path cur = src.toAbsolutePath().getParent();
        for (int i = 0; cur != null && i < 6; i++, cur = cur.getParent()) {
            Path cand = cur.resolve("fondo_musica.mp3");
            if (Files.exists(cand)) return cand;
            try (Stream<Path> s = Files.list(cur)) {
                List<Path> mp3 = s.filter(f -> f.toString().toLowerCase().endsWith(".mp3")).limit(3).toList();
                if (!mp3.isEmpty()) return mp3.get(0);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private static String textosCampana(String fuente) {
        return "# Campana publicitaria - Construyendo Valores\n\n"
                + "Fuente del anuncio: " + fuente + "\n\n"
                + "## Ganchos (titulos A/B del anuncio)\n"
                + "- A: Canciones y cuentos que ensenan valores\n"
                + "- B: El lugar donde los ninos aprenden jugando\n"
                + "- C: Construimos algo mas importante que edificios\n\n"
                + "## Descripcion del anuncio (YouTube/Shorts)\n"
                + "Canciones originales, cuentos y retos para ninos que ensenan valores: generosidad,"
                + " amistad, creatividad, perseverancia y mucho mas. Nuevos videos cada semana.\n\n"
                + "Suscribete y activa la campanita para no perderte los estrenos.\n\n"
                + "Construyendo Valores: construimos algo mas importante que edificios: valores.\n\n"
                + "## Copy TikTok (20 s)\n"
                + "Que ves cuando los ninos escuchan una cancion que ademas ensena valores? -> Yo veo"
                + " un canal sin humo, sin gritos y con musica original. -> Cada semana: cancion,"
                + " cuento y reto nuevos. -> Sigueme para no perderte el proximo estreno. [enlace]\n\n"
                + "## Copy Instagram (Reel + pie de foto)\n"
                + "Descubre canciones y cuentos que les dejan algo: valores. Musica 100% original,"
                + " ritmo que a los mas pequenos les encanta y mensajes que siembran amabilidad.\n"
                + "Mirado del reves tambien es divertido. Enlace en bio. #Concells #Valores #Ninada\n\n"
                + "## Copy X / Twitter\n"
                + "Un canal infantil donde cada cancion ensena un valor. Semana a semana: musica,"
                + " cuentos y un reto. Esto es Construyendo Valores. [video]\n\n"
                + "## Publicacion en la comunidad de YouTube\n"
                + "Nueva cancion muy pronto. Mientras tanto: deja en comentarios el valor que"
                + " ensenariamos la semana que viene.\n\n"
                + "## Hashtags por plataforma\n"
                + "- YouTube/Shorts: #Shorts #CancionesInfantiles #CuentosParaNinos #Valores"
                + " #MusicaParaNinos #EducacionInfantil\n"
                + "- TikTok: #parati #fyp #cancionesinfantiles #valores #cuentosparaninos #aprendeconmigo\n"
                + "- Instagram: #cancionesparaninos #cuentos #valores #familia #crianza #educacion\n\n"
                + "## Checklist de publicacion del anuncio\n"
                + "- [ ] Subir anuncio como Short publico (o video normal landscape si se rehace 16:9).\n"
                + "- [ ] Programar 2 copias en TikTok/Instagram con ganchos B y C.\n"
                + "- [ ] Incluir enlace del canal en la bio de cada red.\n"
                + "- [ ] Publicar en la comunidad de YouTube avisando del proximo estreno.\n"
                + "- [ ] Reutilizar las miniaturas A/B en los proximos 3 videos y comparar CTR.\n";
    }
}