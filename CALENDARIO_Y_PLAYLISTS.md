# Calendario y Playlists — Construyendo Valores (plan 2026)

## Calendario semanal (ritmo del canal)
- Miercoles 17:00 -> Cuento o recuento clasico (video largo).
- Sabado 17:00 -> Cancion (ronda) + Short el mismo dia.
- Domingo 09:00 -> Compilacion de la semana (engancha nuevos).
- Lunes/Jueves -> Shorts cortos recortados del video de la semana (hook de 30-40 s).

## Playlists por valor (crear despues de cada publicacion)
- Perseverancia: La tortuga y la liebre (+ La semilla que crece - paciencia cuando se publique).
- Honestidad: Pinocho (Nico el muno) / proximo El pastor mentiroso.
- Paciencia: La semilla que crece.
- Amistad: El leon y el raton.
- Respeto: Ricitos de oro / El rey midas (cautela).
- Responsabilidad: La cigarra y la hormiga.
- Valentia: Caperucita roja / El gato con botas.
- Autoestima: El patito feo.

> POLITICA DE CADENCIA (decisión 24/09/2026, ACTUALIZADA): sin presupuesto. Se sube
> lo que esté listo y APTO, con calidad mínima garantizada por QA. Objetivo de
> referencia: 1 video cada 2-3 días. El gancho animado de Free.ai es OPCIONAL:
> si hay un `clips/hook.mp4` descargado a mano (pool gratis de la web, 1 clip/2 días),
> el pipeline lo cose automáticamente; si no, el video se sube igual. La API gratis
> NO incluye video (HTTP 429) -> los tokens son un extra opcional de $1 si algún día
> hay presupuesto.

## Serie "Recuentos dominios publicos" (motor listo con --recuento)
| # | Cuento | Valor | Estado |
|---|---|---|---|
| 1 | La tortuga y la liebre | Perseverancia | PUBLICADO https://youtu.be/xhuiXnEgS7o |
| 2 | Pinocho | Honestidad | PUBLICADO https://youtu.be/gSCwi3Mmodc |
| 3 | La cigarra y la hormiga | Responsabilidad | PUBLICADO https://youtu.be/9MuSK4ylGQQ |
| 4 | El pastor mentiroso | Honestidad | PUBLICADO https://youtu.be/04h8OfXhR_8 |
| 5 | El patito feo | Autoestima | PUBLICADO https://youtu.be/xL6u8oSO034 |
| 6 | El rey midas | Cautela | PUBLICADO https://youtu.be/lGzmzdMs4_I |
| 7 | El leon y el raton | Amistad | (siguiente produccion) |
| 8 | Ricitos de oro | Respeto |

## Medio de lanzamiento (comando nuevo)
- `--subir <video> <titulo> <descripcion> <privacidad>` sube con las etiquetas del
  archivo `etiquetas.txt` que viva junto al video (SEO real, probado 22/09/2026).
- Privacidad recomendada: `public` (o `unlisted` para pre-estrenos).

## Rutina de lanzamiento (20 min por video)
1. Ver video finalizado (revisar 1 vez, nada de mas) y recortar Short.
2. Subir con titulo beneficio + descripcion + etiquetas del paquete generado.
3. Miniatura = cara grande + color vivo (Portada AI ya generada); ajustar si hace falta.
4. Publicar comentario fijo (pregunta sobre el valor).
5. Anadir a la playlist de su valor y publicar el Short al dia siguiente.

## Log de lanzamiento
- 22/09/2026 Estreno doble publico:
  - La tortuga y la liebre (Perseverancia) -> https://youtu.be/xhuiXnEgS7o
  - Pinocho (Honestidad) -> https://youtu.be/gSCwi3Mmodc
  - Falta (manual, 1 min en Studio): comentario fijo de cada paquete y crear las
    playlists "Perseverancia" y "Honestidad" para encadenar reproducciones.
- 22/09/2026 (miércoles de serie) publicado:
  - La cigarra y la hormiga (Responsabilidad) -> https://youtu.be/9MuSK4ylGQQ
  - Falta: comentario fijo del paquete y playlist "Responsabilidad".
  - QA automatizado: apto, revision IA del guion 6.8/10, with página corta.
- 22/09/2026 (rutina Shorts) publicados 3 shorts:
  - Perseverancia https://youtu.be/LCaTZLyI-q0
  - Honestidad https://youtu.be/EGujcxJMVSI
  - Responsabilidad https://youtu.be/xKLBlJmfVm4
- 22/09/2026 automatizacion de supervision: tarea 07:35
  "ConstruyendoValores_Calidad" corre `--supervision salidas` y deja SUPERVISION.md
  en la raiz de salidas cada manana.
- 22/09/2026 publicado El pastor y el lobo (Honestidad):
  - Cuento -> https://youtu.be/04h8OfXhR_8
  - Short -> https://youtu.be/5hFDYQUCj4o
  - QA apto (indice 67-68, IA revision 7.2/10). Tercer video seguido que NO supera
    el 69.1 de Pinocho: techo real sin movimiento animado (movimiento ~13-15/100).
    La palanca es la cuenta FREE de Free.ai -> ACTIVAR_MEJORAS.md.
  - Falta (Studio): comentario fijo + playlist "Honestidad".
- 24/09/2026 publicado El patito feo (Autoestima):
  - Cuento -> https://youtu.be/xL6u8oSO034
  - Short -> https://youtu.be/0nuE0YNdi90
  - QA apto (67.5/100, IA 7.0/10). Quinto video seguido que no supera el 69.1 de
    Pinocho (movimiento ~15.8; sin gancho animado). El gancho queda opcional por la
    vía web gratis. Herramientas nuevas: `--ensamblar <dir>` (restaura el video
    final a su calidad original, sin re-render) y `--enganche <video> <hook>`.
  - Falta (Studio): comentario fijo + playlist "Autoestima".
- 24/09/2026 publicado El rey midas (Cautela):
  - Cuento -> https://youtu.be/lGzmzdMs4_I
  - Short -> https://youtu.be/yQ03s5byyfw
  - QA apto (68.9/100, IA 6.8/10; tecnica 100.0, audio 97.1). Sin gancho animado.
    Rechazo del primer intento de subida por titulo >100 caracteres -> titulo acortado.
    Sexto video sin romper 69.1 (techo de movimiento).
  - Falta (Studio): comentario fijo + playlist "Cautela".
- 24/09/2026 GIRA DE CALIDAD (rebrand): se decide privatizar la serie inicial (6 cuentos
  + 5 shorts) y rehacerla con calidad cinematografica antes de seguir publicando.
  - Fase 1 (motor visual): render nativo 1080x1920, camara con easing/plano por escena,
    pulso de mano, correccion de color (grade tibio/frio), rotulos Comic Sans MS.
    Comparativa: salidas/rey_midas/cine_antes_despues_esc{1,4}.png (izq=antes, der=despues).
  - Fase 2 (sonido de cine): voz procesada (highpass/lowpass/deesser/eco sala),
    fondo de ambiente generado, whoosh+boom en transiciones de escena (SoundDesign.java).
  - PRUEBA (remontaje del midas con Fases 1+2, clips viejos): indice 70.9 -> ROMPE
    el techo de 69.1 por primera vez, supera anterior=SI, 1 solo aviso.
  - Falta (manual, sin API): privatizar los 6+5 videos en Studio (re-verificacion
    YouTube pendiente bloquea la API). Audit gratuito: support.google.com/youtube/contact/yt_api_form.
  - Siguiente: re-renderizar la serie completa con --recuento (motor cinema) y
    publicar el rebrand empezando por La tortuga y la liebre.
- Siguiente produccion programada: re-render serie con calidad cinematografica.

## Reglas de forma (marca)
- Mismo avatar/banner en todas las redes; slogan: "Cuentos y canciones para aprender valores".
- Miniaturas uniformes (personajes + letra gruesa + fondo vivo).
- Nombre visible "Construyendo Valores" en cada video (ya quemado en el ensamble).