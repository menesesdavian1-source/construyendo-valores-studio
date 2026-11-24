# DECISIONES DE CONTENIDO - 2026-11-24

1. Regla +10%/dia: indice anterior 66,9 (objetivo global 73,6). El eje con margen es el movimiento: 16,9 -> objetivo > 18,6.
2. Energia de movimiento (calidad.movimiento): 1,60 -> 1,60 (MAX alcanzado).
3. Barrido competidores -> El competidor rinde mas con: Veo Veo Ventanas de Colores 🌿🦁 En la Selva | Video Didáctico | Plim Plim | Formato: video corto (248 s) | Mezcla del barrido: 12 Shorts, 12 videos largos | Apostar por Shorts frecuentes (descubrimiento) como los competidores.
4. Hoy: publicar 1 Short vertical de descubrimiento. Proxima produccion: --ronda barco (El barco chiquitito).
5. Control final: medir el nuevo video con --calidad; si el eje de movimiento no supera un +10% al anterior, se vuelve a subir la energia (calidad.movimiento) y se regenera.

## CONTROL DE CALIDAD EXTRA (2026-11-24, tarde)
6. Encuadre: `Animator` y `CineIA` ahora rasterizan a 1080x1920 con coordenadas logicas 720x1280 (`LW/LH`) antes de `g.scale(1.5)`. Antes se mezclaban coordenadas 1080x1920 con el escalado y el cuadro salia desplazado/recortado. Prueba: frame 1080x1920, centro de color a +1 px del centro real.
7. Intro: eliminada la escena generica "¡Bienvenidos a Construyendo Valores!" en `Pipeline`. Prueba: `--cuento 1` genera audio desde `escena_1`, sin `escena_0`. La marca queda como rotulo visual.
8. Voces: `edge-tts` con 14 voces neurales diferenciadas, deltas de tono/ritmo suaves y sin `aecho` (la cola artificial sonaba robotica). OpenAI TTS sigue sin creditos. Audio final medido a -15,2 LUFS (objetivo -16).
9. Imagen: prompt fotorrealista con bloque anti-blur, un solo `unsharp` final en `Assembler` y personajes Rubi/Lumi anadidos a `LOOK`, paleta, voces y prompts.
10. Rotacion: nuevo `Reparto.java` con 12 personajes; `TechVideo`, `Prompt`, `Gemini`, `Animator`, `Main --stills` y `--muestra` ya los exploran todos con desfase estable segun el titulo.
11. Commit de codigo: `027143a` pusheado a `menesesdavian1-source/construyendo-valores-studio`. Subidas a YouTube siguen bloqueadas por `uploadLimitExceeded` (Pipo/Tortuga/Sofia pendientes).
