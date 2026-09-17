# Guía del canal — Construyendo Valores

Plan para lanzar y mantener un canal de YouTube de cuentos animados para niños (3-8 años)
con 30 videos (10 valores x 3 formatos), precio total: $0.

## Identidad del canal

- **Nombre:** Construyendo Valores
- **Eslogan:** "Construimos algo más importante que edificios: valores."
- **Personajes:** Casquito (casco, ánimo), Tito (excavadora, esfuerzo), Sofía (ingeniera), Nico (aprendiz curioso).
- **Estilo visual:** fondos en azul y naranja, escenarios de obra, parque y taller (ver escenario_*.png).
- **Avatar y banner:** `avatar_canal.png` (800x800) y `banner_canal.png` (2560x1440), generados con `--art`.

## Los 30 videos

1. Perseverancia (Historia / Reto / Hábito)
2. Responsabilidad (Historia / Reto / Hábito)
3. Aprendizaje (Historia / Reto / Hábito)
4. Amistad (Historia / Reto / Hábito)
5. Honestidad (Historia / Reto / Hábito)
6. Gratitud (Historia / Reto / Hábito)
7. Empatía (Historia / Reto / Hábito)
8. Orden (Historia / Reto / Hábito)
9. Valentía (Historia / Reto / Hábito)
10. Generosidad (Historia / Reto / Hábito)

Títulos, descripciones y etiquetas ya generados por IA en `CONTENIDO_30.md` y `CONTENIDO_30.csv`.

## Formato de video (3 formatos)

| Formato | Idea | Duración objetivo |
| --- | --- | --- |
| Historia | Un personaje vive el valor y lo resuelve | 45-90 s |
| Reto | El personaje reta al niño a practicar el valor en casa | 30-60 s |
| Hábito | Recordatorio corto y repetible de una frase-mantra | 20-40 s |

## Rutina de publicación

Publicar **lunes, miércoles y viernes** manteniendo el orden del plan (10 riesgos ordenados por dificultad
de guion: historia → reto → hábito). En 10 semanas completas los 30 videos.

1. Abrir la app → pestaña **Contenido** → "Generar 30 títulos + descripciones" (ya hecho; regenéralo cuando quieras variaciones).
2. Pestaña **Guion**: pide el guion del valor del día (elige escenario que combine: obra para historias, parque para retos, taller para hábitos).
3. **Revisa el guion** siempre: los textos generados por IA se deben editar para el tono familiar del canal.
4. Pestaña **Render**: genera el video. Salida: `video_final.mp4` + `video_final_portada.png`.
5. **Sube a YouTube**: título y descripción desde `CONTENIDO_30.md`, pegarlas tal cual, con la miniatura generada.
6. Agrega pantalla final ("Suscríbete") en YouTube Studio y una tarjeta al video anterior.

## Carga de videos para YouTube

- Resolución 720x1280 (vertical, para Shorts). Para subir como Shorts son 60 s; los de 45-90 s pueden durar más.
- Si quieres video horizontal clásico, cambia W/H en `Animator.java` (p. ej. 1280x720) y vuelve a compilar.
- Música: coloca tu propia pista como `musica.mp3` junto al video, o la app la sintetiza gratis.

## Sugerencias para crecer sin pagar nada

- Mantén el mismo estilo visual para que los niños reconozcan el canal en el feed.
- Contesta cada comentario con el personaje del día (Casquito responde con emojis de construcción).
- Colabora tu lista como "Construyendo Valores: todos los episodios" en la descripción.
- La serie Cortos se recicla como Shorts de YouTube (Recuerda: valor + escena de obra).
- Meta inicial: 3 videos semanales constantes; el algoritmo premia la constancia sobre la perfección.

## Modo automático (100 minutos de trabajo al día → 0)

Una vez seguida la **GUIA_GOOGLE_CLOUD.md** (una sola vez, ~10 min) puedes publicar
solo: cada día a las 7:00 la Tarea de Windows (`ConstruyendoValores_Diario`)
genera el siguiente video del plan de 30 y lo sube programado para las 9:00.
- La app sigue el orden del plan (Video 1 → 2 → ... → 30 → vuelve al 1).
- Si la IA no responde ese día, usa un guion de respaldo para no fallar la subida.
- Revisas los videos subidos en YouTube Studio y solo intervienes si quieres.

## Guión de referencia (estructura de historia)

1. Hook: el personaje prepara su máquina y hay un problema.
2. Conflicto: se frustra, duda, o se equivoca.
3. Ayuda: otro personaje le recuerda el valor.
4. Solución: lo practica y lo logra.
5. Cierre: frase-mantra del valor + invitación a suscribirse.