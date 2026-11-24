# Construyendo Valores Studio

Software gratuito (presupuesto $0) para producir videos animados en español para un canal de YouTube infantil
que enseña valores con personajes de construcción: **Casquito**, **Tito**, **Sofía** y **Nico**.

La app edita los videos con IA: genera el guion con Google Gemini, crea las voces con edge-tts, anima los
personajes cuadro por cuadro (Java 2D) y ensambla el video final con ffmpeg (música y subtítulos incluidos).
También vincula el canal de YouTube y **publica un video diario de forma automática** usando la YouTube Data
API v3 (OAuth 2.0).

## Características

- **Pipeline completo**: guion (IA) → voz (TTS) → animación (Java 2D) → video (ffmpeg) → subida (YouTube API).
- **Publicación programada**: tarea diaria que genera y sube el video del día a una hora fija, sin intervención.
- **Rondas y karaoke**: canciones infantiles con letra en pantalla.
- **Identidad del canal**: avatar (800x800) y banner (2560x1440) generados por IA.
- **Interfaz gráfica** (Swing) y **línea de comandos** completa.

## Requisitos

- Java 17+
- Python 3.12 con `pip install edge-tts`
- ffmpeg en el PATH (o se autodetecta en su carpeta de instalación)
- Una API key gratis de Google Gemini (`https://aistudio.google.com/apikey`)
- Maven 3.9+ (solo para compilar)

## Compilar y ejecutar

```powershell
mvn package
java -jar target\construyendo-valores-studio-1.0.0.jar          # abre la interfaz gráfica
```

La primera vez pide la API key de Gemini (se guarda en `%USERPROFILE%\.construyendo-valores\config.properties`,
fuera del repositorio).

## Línea de comandos

| Comando | Descripción |
| --- | --- |
| `--auto <carpeta> <tema> <valor> [escenario]` | Guion con IA → video completo |
| `--demo <carpeta>` | Video de ejemplo sin IA |
| `--contenido <carpeta>` | Genera los 30 títulos + descripciones del canal (IA) |
| `--art <carpeta>` | Genera avatar y banner del canal |
| `--preview <carpeta>` | Previews de los 3 escenarios (obra, parque, taller) |
| `--test-gemini` | Comprueba la conexión con la IA |
| `--youtube-auth` | Vincula la app con tu canal de YouTube (una vez, OAuth 2.0) |
| `--youtube-test` | Comprueba la conexión con YouTube |
| `--diario <carpeta> [hora]` | Crea y sube el video del día (rotación del plan de 30) |

Ejemplo: `java -jar ... --auto videos\v1 "La torre que no quiso rendirse" Perseverancia obra`

## Pipeline de video

1. **Guion** — Gemini genera un guion JSON con escenas narradas por personaje.
2. **Voz** — edge-tts sintetiza cada línea con pitch y ritmo únicos por personaje.
3. **Animación** — Java 2D dibuja 25 fotogramas por segundo (720x1280) con movimiento, cámara y escenarios.
4. **Ensamblado** — ffmpeg une clips, quema subtítulos, mezcla música y genera la portada.

## Estructura del código

```text
src/main/java/com/construyendovalores/studio/
  Main.java            CLI + punto de entrada
  MainFrame.java       interfaz gráfica (Swing)
  Config.java          configuración local (API keys, OAuth)
  Gemini.java          integración con la API de Google Gemini
  GeminiParser.java    parseo del guion JSON generado por IA
  Tts.java             sintetización de voz (edge-tts)
  Animator.java        animación de personajes (Java 2D)
  Scenario.java        escenarios (obra, parque, taller)
  Assembler.java       ensamblado final con ffmpeg
  Youtube.java         YouTube Data API v3 (OAuth 2.0, subida resumable)
  TikTokPoster.java    publicación en TikTok
  SerieLibrary.java    plan de contenido y rotación diaria
  RondaLibrary.java    canciones infantiles con karaoke
  MelodySynth.java     síntesis de música de fondo
```

## Publicación automática diaria

La app puede crear y subir un video cada día (rotando el plan de 30) y programar su publicación a una hora fija
(por defecto 9:00). La comunicación con tu canal usa tokens OAuth guardados en tu perfil de usuario, nunca en el
repositorio.

## Notas de seguridad

- Las claves de API y credenciales viven en `%USERPROFILE%\.construyendo-valores\config.properties` (fuera del
  repositorio). No subas ese archivo.
- Este repositorio excluye los artefactos generados (JAR, videos, audio) con `.gitignore`.