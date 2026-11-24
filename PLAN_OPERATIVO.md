# PLAN OPERATIVO — Construyendo Valores

Canal infantil de YouTube (Colombia) · formato Short (todo 1080x1920, 15-64 s).
Meta de monetización: YouTube Partner Program — nivel inicial **500 subs + 3M vistas de Shorts en 90 días** (ventana actual: hasta feb 2027).

## Pilares

| # | Pilar | Estado | Regla |
|---|-------|--------|-------|
| 1 | Producción automatizada | En marcha | Nunca un día sin contenido agendado |
| 2 | Barra de calidad premium | En marcha | Nada bajo el umbral se sube (`calidad.umbral=50`) |
| 3 | Producto y empaque | Mejora continua | Serie mensual + títulos con gancho + miniaturas |
| 4 | Distribución | YouTube (short) / TikTok (vitrina) | Clipping de terceros SOLO en cuentas desechables |
| 5 | Medición | Tracker + revisión semanal | Más de lo que gana, parar lo que pierde |

## Automatización activa (Windows)

| Tarea | Horario | Función |
|-------|---------|---------|
| `ConstruyendoValores_Diario` | Diario 07:00 | Genera el video del día y lo agenda para las 9:00 (`construyendo_diario.bat`) |
| `Construyendo Valores - Mejora Diaria` | Diario 07:30 | Ciclo de mejora + barrido de competidores + decisión |
| `ConstruyendoValores_Calidad` | Diario 07:35 | Supervisión de calidad de `salidas` |
| `ConstruyendoValores_Semana` | Domingo 20:00 | Renderiza 7 videos a la cola + compilación semanal |

- **Desactivadas por obsoletas:** `MotorDiario` y `PlanificarPiloto` (apuntaban a scripts temporales
  que ya no existen; el `--diario` de las 07:00 cumple esa función).
- Las cuatro tareas activas usan **Java 21 (Temurin)**, permiten inicio con batería y recuperación
  si el PC estaba apagado (`StartWhenAvailable`).
- Reintento manual si un día falla: `construyendo_diario.bat` (idempotente: no repite el video del día).

## Calidad (estándar Piloto Premium)

El piloto `¡Puente de Colores!` (índice 73,8/100) fijó la barra:
- **Guion**: gancho en 2 s, arco (problema→intento→fracaso→idea→triunfo→moraleja→CTA), onomatopeyas, 4-9 palabras/frase.
- **Emoción por escena** (`emotion`): sorpresa/alegre/triste/curioso/asustado/enojado/tranquilo/orgulloso → matiza tono y ritmo de voz.
- **Voces**: edge-tts por personaje (es-CO Narrador, es-MX niñas/niños) + pulido de estudio (audio 16 LUFS).
- **Música**: bed C-Am-F-G (loop 8 s) con ducking bajo la voz.
- **Animación**: cuadro a cuadro (personajes en movimiento) + cámara cinematográfica + subtítulos quemados.
- **Gate**: `calidad.umbral` en config; por debajo → no se sube (se registra en métricas).

## Plan NOVIEMBRE–DICIEMBRE 2026

Objetivo: retomar el ritmo diario (1 Short/día) y cerrar el año con 30+ videos y métricas limpias.

| Fecha | Pieza | Fuente |
|-------|-------|--------|
| 13-30/11 | Reanudar motor diario (rotación ronda→historia→serie→tecnología) | motor diario (`--diario`) |
| 13-20/11 | Publicar el piloto re-renderizado con GPT-Image-2 (cuando haya pollen) | `salidas/piloto_sofia` |
| 21-30/11 | Serie de recuentos de dominio público | `--recuento` |
| 01-15/12 | Especial de Navidad y valores + canciones | `--semanario` |
| 16-31/12 | Compilación de los mejores 10 + balance del año | `--compilacion` |

**Cola de recuentos (dominio público)** para alimentar el motor: cigarra y la hormiga (esfuerzo) · el león y el ratón (gratitud/amistad) · el viento y el sol (amabilidad) · pinocho (honestidad) · liebre y tortuga (perseverancia).

## Métricas y decisiones

- Cada subida se registra en `Metricas` (CSV). Revisión semanal (lunes) de 3 números: **retención % de Shorts**, **suscriptores/semana**, **click-through**.
- Regla: el tema que gana → producir más del mismo; el que pierde → reciclaje o parar.
- A/B en curso: piloto premium vs CLIP_01_sofia (mide a partir del 15/10, ~2 semanas).

## Ritual semanal (tiempo humano: ~30 min)

1. Lunes: leer informe de métricas (lo prepara el sistema).
2. Publicar/revisar canarios y videos de reserva pendientes.
3. Responder 2-3 comentarios (sube la señal del algoritmo).
4. Decidir la cola de recuentos de la semana.

## Fronteras (lo que NO se hace en este canal)

- No trocear contenido protegido de terceros en el canal principal.
- No mezclar nichos ni formatos de campañas (Content Rewards) con Construyendo Valores.
- Si se prueba el clipping: cuentas desechables, nunca el canal.

## Pendientes manuales resueltos

- ✅ Borrados `wAAKoIkvICY` (prueba) y `Vprh7sj2dZM` (canario).

## 06/10 — Barra visual "Cine IA" (render 2.5D)

**Nuevo pipeline de imagen:** `CineIA` · `ImagenIA` · `GeminiImagen` · `OpenaiImagen` · `Pollinations`.
Cada escena = **una ilustración IA completa** (personaje + ambiente, prompts de personaje desde
`personaje.<alias>.descripcionArte`), con cámara cinematográfica (pan/zoom/easing), rebote de
luz, partículas, rótulos, `tarjetaIntro`, grade de color y viñeta. Mantiene guion con arco+emoción,
voces, subtítulos y música con ducking. Re-render de carpeta existente: `--cine <outDir>`.
Respaldo automático al animador clásico si la IA falla (`cine.habilitado=true` por defecto).

**Estado de proveedores (probe 06/10):**
- Gemini imagen: `gemini-2.5-flash-image` responde pero **cuota (429) superada** hoy; revisar plan/billing.
- OpenAI imagen: key activa pero **sin crédito** (`insufficient_quota`) — pendiente recarga del usuario.
- Pollinations: **funciona gratis** (modelo flux). Con cuenta (key `sk_...` en
  `pollinations.token`) **sin marca de agua** — probado v1 OK; anónimo = con marca + 1 req/15s.

**Verificado en piloto re-renderizado:** 1080x1920 · 25 fps · H.264+AAC · 35,2 s · 7/7 escenas con IA
de cuenta (sin marca de agua). Hoja de contactos: `%TEMP%\opencode\cine_limpio\hoja.png`.

**Pendientes manuales:** (a) revisar visual del piloto (hoja de contactos); (b) `calidad.umbral` se
evalúa tras esa revisión; (c) decisiones de estilo (tamaño de personaje, paleta) si algo no convence.

## 12/11 — Mejora de imagen: modelo premium

- Cliente Pollinations ampliado: cuenta `sk_...`, API v1 con `size` vertical (`1024x1536`), `enhance`,
  modelo configurable (`pollinations.model`, `pollinations.enhance`), respaldo legacy y anónimo.
- **Modelo elegido: `openai/gpt-image-2`** (mejor seguimiento de prompt; 1024x1536).
- **Bloqueo:** GPT-Image-2 exige **pollen pagado** (~$0.046/imagen); saldo pagado actual $0.0000,
  quest pollen $0.0006. Sin recarga no se puede generar. Alternativas quest: `z-image-turbo`
  (~$0.004/img) y `flux.2-klein` (~$0.005/img), para lo que hacen falta quests.
- **Mejora aplicada sin costo:** arte actual reescalado a 1152x2048 (lanczos + unsharp) y re-render.
  Video: 1080x1920 · 25 fps · 35,2 s. Hoja: `%TEMP%\opencode\piloto_final\hoja.png`.
- Próximo paso: recargar pollen (top-up) para GPT-Image-2 o completar quests para el modelo económico.

## 12/11 — Restauración de la automatización diaria

- **`--estado` reparado:** el `case` contenía por error el código de `--youtube-privacidad` y fallaba con
  `Index 1 out of bounds`. Ahora reporta vínculo, última subida, motor, cola del día y últimos videos
  con vistas/likes/comentarios. Uso: `--estado [n]` (n = cuántos videos listar, por defecto 5).
- **TTS reparado:** `edge-tts` pasó de 7.2.8 a **7.3.1**; el 403 de `speech.platform.bing.com` quedó
  resuelto. Prueba de humo `--demo` completa: 7/7 voces + video final generado.
- **Tareas programadas:**
  - `ConstruyendoValores_Semana` tenía la ruta partida (`...\Default` + `Project\...`); corregida.
  - Las cuatro tareas activas: `DisallowStartIfOnBatteries=false`, `StopIfGoingOnBatteries=false`,
    `StartWhenAvailable=true` (antes fallaban con `0x800710E0` en portátil con batería).
  - `MotorDiario` y `PlanificarPiloto` desactivadas (scripts en `%TEMP%` inexistentes).
  - `construyendo_diario.bat` y `construyendo_semana.bat` ahora usan Java 21 (Temurin) con respaldo a
    `java` del PATH y registran inicio/fin y código de salida en el log.
- **Pendiente bloqueante:** el token de YouTube devuelve `400` al renovar; hay que reconectar con
  `--youtube-auth` (abre el navegador) antes de que las subidas diarias vuelvan a funcionar.