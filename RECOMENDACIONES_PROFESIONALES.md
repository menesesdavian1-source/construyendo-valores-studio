# BARRIO DE RECOMENDACIONES PROFESIONALES - CONSTRUYENDO VALORES

Auditoria integral del canal y su pipeline de produccion. Objetivo: competir
al nivel de canales infantiles de referencia (Plim Plim, Canti Rondas) y
construir hacia la monetizacion (YPP) con calidad medible.

---

## 1. RESUMEN EJECUTIVO (5 prioridades)

1. **Voz mas humana**: cadena de estudio aplicada hoy en todas las voces
   (highpass + EQ + eco + -16 LUFS) y motor premium opcional (OpenAI). Como
   toma de experiencia: el narrador es la cara del canal, debe ser la voz mas
   cuidada. Hoja de ruta en seccion 2.
2. **Movimiento**: ya medible (+10% diario gobernado por el bucle de
   decisiones). Cuando la energia 2D llegue a su tope, pasar los clips a
   Seedance/Artlist (hojas de tomas ya preparadas en PROMPTS_CANCION_ARTLIST.md).
3. **Empaque**: titulo A/B, miniatura con una emocion clara, descripcion con
   "para los padres". Es el 50% de la diferencia entre 100 y 10.000 vistas.
4. **Cadencia estable**: la tarea diaria ya publica el plan; sostener
   Shorts diarios + cancion sabado + cuento miercoles + compilacion domingo.
5. **Datos**: medir todo (calidad.csv, competidores.csv, metricas.csv) y que
   cada video nuevo supere el anterior.

---

## 2. DIRECCION DE VOZ (LO PEDIDO: MAS REALES Y HUMANAS)

### 2.1 Estado de hoy (aplicado)
- Sintesis neural edge-tts por personaje con tono/ritmo/volumen propios
  (Config: voice.<personaje>, voice.pitch.<personaje>, voice.rate.<personaje>).
- Cadena de estudio profesional en TODAS las voces (rondas y cuentos):
  `highpass=70` (quita grosor), `EQ 260Hz -2dB` (limpia), `EQ 3kHz +1.5dB`
  (presencia/articulacion), `aecho` (sala breve), `loudnorm I=-16 TP=-1.5`
  (consistencia de volumen). Archivos `escena_N_studio.mp3`.

### 2.2 Motor premium opcional (listo en el codigo)
- Config: `tts.engine=openai` + `openai.apiKey=<clave>` + opcional
  `openai.model=tts-1-hd` y `openai.voice.<personaje>=<alloy|onyx|shimmer|nova>`.
- Si la API no responde, el sistema cae automaticamente en edge-tts (nunca
  se pierde una grabacion).
- Recomendacion: probarlo en 1 cuento antes de activarlo en todo el canal.

### 2.3 Direccion vocal (reglas profesionales)
- **Narrador**: voz masculina pausada, tono estable; es quien abre y cierra.
  No acelerar. Es la voz "marca" del canal.
- **Personajes ninos**: pitch alto, ritmo rapido, energia. No exagerar el
  pitch (suena a chipmunk).
- **Momento de emocion**: escenas de sorpresa/celebracion van mas rapidas y
  agudas; escenas de aprendizaje van mas lentas y en calma.
- **Frases cortas**: las frases de menos de 12 palabras suenan mas naturales
  que los parrafos largos. El guion (Gemini) debe producirlas forzosamente.
- **Sin risas sinteticas**: no incluir "jajaja" en el texto TTS; suena falso.

### 2.4 Hoja de ruta hacia "voz humana real"
1. **Hoy**: cadena de estudio + prosodia por personaje (hecho).
2. **Corta (1-2 semanas)**: activar OpenAI tts-1-hd en narraciones si la
   calidad en espanol satisface; comparar a ciegas con la versión edge.
3. **Media (1-3 meses)**: ElevenLabs Spanish v2 (si hay presupuesto) para la
   voz del narrador; duplica naturalidad.
4. **Largo (marca)**: grabar el narrador con persona/actor real o (mejor)
   doblaje dirigido; el TTS queda para personajes secundarios.

---

## 3. PRODUCCION AUDIOVISUAL

- **Formato de estudio**: 1080x1920 vertical, 25 fps, master final -14 LUFS
  con pico real -1.5 (comprobado por el indice de calidad).
- **Movimiento**: el indice de calidad mide energia entre frames; la regla
  profesional del canal es +10% por video (bucle de decisiones). La energia
  (calidad.movimiento) sube automaticamente mientras rinda.
- **Ruta 3D/pelicula**: los 12 tomas de la cancion (PROMPTS_CANCION_ARTLIST.md)
  se generan en Seedance/Artlist y se montan sobre el audio master ya pulido.
- **Micro-camara**: ya activo (respiro de zoom). Recomendable agregar lento
  push-in en el estribillo y crane-up en el cierre.
- **Consistencia de personaje**: mantener la misma silueta/paleta por
  personaje en cualquier estilo (2D o Seedance); la frase de consistencia
  esta en las hojas de tomas.

---

## 4. GUION Y ESTRUCTURA QUE CONVIERTEN

- **Gancho 0-3 s**: la primera frase debe generar curiosidad o un reto
  ("Que pasa cuando una torre quiere besar las nubes?"), nunca "bienvenidos".
- **Canciones**: estribillo de 2-4 lineas repetible, pegadizo; versos de 4
  lineas. El karaoke (subtitulos .srt + letra en pantalla) aumenta la
  retencion de los ninos que cantan.
- **Loops**: las canciones cortas (60 s) se revisan mas; disenar el final
  para invitar a repetir ("de nuevo!").
- **Cuentos**: 2-4 min, estructura emocional (problema -> esfuerzo -> logro),
  un solo valor por entrega.
- **Compilaciones (la mina de minutos)**: 20-30 min uniendo canciones y
  cuentos; los competidores demuestran que es su mejor formato (ver barrido
  diario). Publicar los domingos.
- **Cierre estandar**: "Suscribete para mas cuentos y canciones" + llamado a
  cantar con nosotros.

---

## 5. EMPAQUE YOUTUBE (CTR y descubrimiento)

- **Titulo A/B**: el bucle de mejora sugiere 4 variantes por video. Regla de
  oro: persona/personaje + accion o valor + formato (cancion/cuento/short).
- **Miniatura**: una cara grande con emocion extrema + maximo 3 palabras.
  Siempre el mismo estilo de borde y tipografia (marca).
- **Descripcion**: plantilla fija - primera linea = titulo con mas keywords,
  2a linea el valor, 3a "PARA LOS PADRES: ..." (confianza), hashtags.
- **Etiquetas**: conjunto fijo del canal (construyendo valores, canciones
  infantiles, musica para ninos, cuentos infantiles) + 2 especificas.
- **Capítulos y subtitulos**: los .srt ya se generan; activarlos siempre.
  En cuentos largos, usar ctrl+historia en el texto.
- **Playlists por valor** (Perseverancia, Compartir, Creatividad...) para
  run time y descubrimiento cruzado. Tarjetas/end screens al siguiente video.

---

## 6. CRECIMIENTO Y CADENCIA

- Shots diarios verticales de descubrimiento (cuentos/canciones) + cancion
  sabado + cuento miercoles + compilacion domingo.
- Comentario fijado en cada video pidiendo temas ("Que valor quieres
  aprender manana?").
- Responder a todos los comentarios los primeros 60 dias.
- Comunidad del canal: avisos de estreno y votos.
- Crosspost a Instagram/TikTok (Shorts reciclados) y Pinterest para padres.
- Evaluar fechas-hora de subida con Analytics cuando haya trafico.

---

## 7. DATOS Y KPIs (tabla profesional)

| Metrica | Umbral bueno | Donde se mide |
|---|---|---|
| CTR (impresiones a clic) | 4-6 % | Analytics por video |
| Retencion media (1-4 min) | 50-60 % | Analytics |
| Retencion Shorts (ver completo) | 80 % | Analytics |
| Vistas/dia primera semana | subiendo | metricas.csv |
| Indice de calidad (interno) | +10 % por video | calidad.csv |
| Like ratio (me gusta/vistas) | 3-5 % | metricas.csv |
| Subs ganados por video | >0 y creciendo | Analytics |

El bucle de decisiones lee esto cada manana y propone el siguiente movimiento.

---

## 8. MONETIZACION Y POLITICAS

- Meta YPP: 1000 subs + 4000 h publicas en 12 meses (o 10 M de Shorts en 90
  dias). Las compilaciones de 20-30 min aceleran las horas.
- Contenido infantil: verificar "Made for Kids" (activa SafeSearch y limita
  datos). Respeta COPPA: sin manipulacion de datos de ninos.
- Sin choques bruscos, sin sonidos estridentes, sin asustar: "harmless".
- Cuando se active YPP: el RPM infantil es bajo, el volumen lo compensa;
  priorizar compilaciones + Shorts que lleven a videos largos.

---

## 9. ROADMAP 30-60-90

- SEMANA 1: seguir canal publicado hasta 10+ videos; activar motor OpenAI en
  narraciones; hacer la compilacion dominica con las canciones actuales.
- MES 1: catalogo 8 canciones originales, 4 cuentos, compilaciones, miniatura
  unica del canal; medir CTR y retencion de cada formato.
- MES 2: en funcion de datos, duplicar el formato ganador; probar el primer
  video-musical con movimiento de Seedance (tomas de PROMPTS_...).
- MES 3: voz con actor real/doblaje si el TTS premium no basta; colaboracion
  con canales infantiles pequenos; evaluar comunidad fuerte (peticiones).

---

## 10. CHECKLIST SEMANAL (dia a dia)

- [ ] Lunes: 1 Short de descubrimiento + revision de METRICA/MEJORAS.
- [ ] Martes: 1 Short + producir material de la semana.
- [ ] Miercoles: cuento de la aldea + su Short.
- [ ] Jueves: 1 Short + A/B de titulos/miniatura del sabado.
- [ ] Viernes: 1 Short + repaso de calidad (indice >10 % del anterior).
- [ ] Sabado: cancion de la aldea + su Short (hora fija).
- [ ] Domingo: compilacion larga + resumen semanal en Comunidad.

Regla del canal: **cada video debe medir mas que el anterior**. El sistema
ya lo vigila solo; la seccion 2 (voz) es hoy la mayor palanca de "realismo".