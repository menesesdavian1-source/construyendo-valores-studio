# ACTIVAR MEJORAS DE CALIDAD (3 claves + 1 cuenta, ~15 min en total)

Estos 4 pasos te dan el mayor salto de calidad del canal. Cuando tengas las
claves, pílaselas al asistente (idioma: espaZol) y el activa `--ia-probe` / la nueva
voz / el QA.

## 1. ElevenLabs Free (voz v3/v4 premium) - 5 min - IMPACTO ALTO
1. Ir a https://elevenlabs.io -> Sign up (puede ser con Google).
2. Perfil > API Key (o https://elevenlabs.io/app/settings/api-keys) -> Create key.
3. Copiar la clave (empieza por `sk_`) y editar
   `%USERPROFILE%\.construyendo-valores\config.properties`:
   ```
   ttspremium.apiKey=TU_CLAVE
   ttspremium.model=eleven_tts_v3_5
   ttspremium.cuota.meses.voz=TU_PERSONAJE  (ej: nico, pinocho)
   ```
   - Voz de protagonista recomendada (vida de niZino, es-ES): `pNInz6obpgDQGcFmaJgB`
     (Rachel). Pruebala con `--voces` primero.
   - Apenas 10 min/mes gratis: elige 1 personaje protagonista y deja los de reparto
     en edge-tts (gratis ilimitado).

## 2. Free.ai (video con movimiento) - IMPACTO MUY ALTO en el indice
El punto mas flojo del QA es "movimiento" (~13/100; tecnica y audio en 98-100).
Estado REAL (verificado con la clave): las claves FREE de la API NO incluyen
generacion de video (el servidor responde HTTP 429 con ese aviso). El video
gratis solo existe en la herramienta web. Dos caminos:

1. V0 (cero euros): cada 2 dias, en https://free.ai/video/ generas el clip 9:16
   del gancho (estilo del canal, 3 s), lo descargas y lo guardas como
   `salidas\TU_VIDEO\clips\hook.mp4`. Luego corre `--enganche "video.mp4" "hook.mp4"`
   y el QA se ejecuta solo. En resumen: 2 minutos manuales por video.
2. V1 ($1, todo automatico): anadir 200K tokens en https://free.ai/pricing/
   (minimo $1) permite el video por API (CogVideoX = 30K tokens/clip -> ~6 clips).
   Con eso el pipeline genera el gancho solo (--recuento) y cada video saldra
   con movimiento sin tocar nada.

Comando util del codigo: `--gancho <titulo> <dir>` genera el clip por API;
`--enganche <video> <hook>` lo cose al inicio y re-ejecuta el QA.

## 3. Together IA (guion/revision IA extra) - 5 min - impacto MEDIO
1. Ir a https://api.together.ai -> Set API Key (plan gratuito con credito inicial).
2. Copiar clave (empieza por `tgp_v1_...`), config:
   ```
   ia.together.apiKey=TU_CLAVE
   ```
3. `--ia-probe` para confirmar.

## 4. Mistral (revision IA extra) - 5 min - impacto MEDIO
1. Ir a https://console.mistral.ai -> Sign up (plan free) -> API Keys.
2. Copiar clave, config:
   ```
   ia.mistral.apiKey=TU_CLAVE
   ```
3. `--ia-probe` para confirmar.

## Comprobar todo
1. `--ia-probe` -> deben salir a la vez: gemini (cuota), groq, together, mistral.
2. `--voces` -> probar la voz premium marcada para el personaje.
3. `--supervision salidas` -> ver que el QA sigue todo APTO.
4. Tareas diarias ya programadas: 07:00 diario, 07:30 mejora, 07:35 calidad
   (`ConstruyendoValores_Calidad`), domingo 20:00 semanal.