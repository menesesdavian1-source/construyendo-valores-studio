# ESTRATEGIA ESTILO PLIM PLIM — PARA CONSTRUYENDO VALORES (adaptada, original)

Objetivo: replicar la maquina de contenido de El Payaso Plim Plim aplicando NUESTRA
marca (aldea magica de constructores, valores, 10 personajes nuevos). Nada de
pintarse de payaso: nuestro heroe es Casquito, constructor del corazon.

## QUE HACE PLIM PLIM Y COMO LO ADAPTAMOS

1. LA MUSICA ES EL MOTOR DE CRECIMIENTO
   Plim Plim: una cancion nueva CADA SABADO; las canciones son la puerta de entrada
   (primero se viraliza la cancion, despues se ven los episodios).
   Nosotros: creamos UNA CANCION DEL VALOR por semana sobre nuestros personajes:
   - "Casquito canta la perseverancia" (torre que besa las nubes)
   - "Sofia construye ideas" (creatividad)
   - "Pipo comparte sus huesos" (generosidad)
   - "Luna guarda los deseos" (empatia / escuchar)
   Formato: 2-3 min, 16:9, letra grande tipo karaoke, estribillo repetible.
   Herramienta gratis: melodia sencilla + voz edge-tts ritmada + letra en pantalla.
   (Mas adelante se mejora con Suno para voces que cantan de verdad.)

2. RECURRENCIA Y CALENDARIO (la clave de la repeticion)
   Plim Plim: todos los sabados hay cancion nueva, sin saltarse semanas.
   Nosotros (plan semanal libre de credito):
   - Lunes: Short (40 s, vertical) del valor de la semana
   - Miercoles: Cuento de la aldea (2-4 min) con narrador Tico
   - Sabado: CANCION nueva (el episodio estrella)
   - Dominical: Compilacion de la semana (une los 3) para subir minutos de visionado
   Con Artlist/IA: los clips 3D reemplazan a los montajes locales de los cuentos.

3. LOS CORTOS (40-60 s) son la cantera de suscriptores infantiles
   Plim Plim: polleras de cortos verticales que alimentan el descubrimiento.
   Nosotros: de cada cuento/cancion sacamos 2-3 Shorts con el momento mas divertido
   (ej.: el "cataplun" de la torre, Pipo riendose, el beso a la nube).
   Nuestro Shorts.java ya genera el formato vertical.

4. ESTRIBILLOS PEGAJOSOS CON MELODIAS SIMPLES
   Plim Plim: su hit usa una melodia tonta y universal que los ninos repiten.
   Nosotros: construir los estribillos en 3-4 notas, palabras cortas que repitan el
   valor: "Persis, persis, perseverar / bloque a bloque, llegar y tocar".
   MelodySynth.java ya compone melodias simples de prueba.

5. VALOR COMO SELLO DE CONFIANZA PARA LOS PADRES
   Plim Plim: "ver Plim Plim es seguro" - heroe dialogante, sin golpes ni superpoderes.
   Nosotros: nuestra promesa = aldea donde cada dificultad se resuelve con trabajo en
   equipo y perseverancia. Que quede en descripciones y en los "Para los padres"
   (seccion breve al final de cada descripcion).

6. ESCUCHAR A LA AUDIENCIA (pedir temas)
   Plim Plim: padres, maestros y psicopedagogos proponen los temas.
   Nosotros: CLAVE: fijamos en cada video el comentario fijado:
   "Que valor quieres que la aldea cante la semana que viene? Dejalo aqui."
   Y respondemos haciendo la cancion pidiendo tema al publico.

7. NO COPIAR: nuestra diferenciacion
   - Plim: payaso azul, musica de cancion de cuna universal.
   - Nosotros: aldea magica DE CONSTRUCCION, objetos magicos constructores
     (brujula de Casquito, plano de Sofia, caja de Nico), cada personaje es
     "superheroe del corazon" con SU valor y SU color de marca.
   - Chao de los personajes: Casquito (lider), Pipo (mascota), Tico (narrador),
     Luna (sonadora), Sofia (inventora), Nico (aprendiz), Tito, Bruno, Memo, Coco.

## PRIMERA CANCION LISTA PARA HACER
"LA CANCION DE CASQUITO - PERSEVERANCIA" (2-3 min)
Estribillo:
   "Perseverar, perseverar, bloque a bloque hay que subir,
    si la torre se cayo, con amigos subira, perseverar, perseverar!"
Versos narran el cuento de la torre que beso a las nubes (Casquito, Pipo, Luna, Tico).
Imagenes: character_sheets_bing (01, 08, 04, 09) en modo karaoke.

## PASOS DE PRODUCCION (todo gratis, sin IA costosa)
1. Letra + estribillo (lista en este documento).
2. Melodia 8 notas con Music.java/MelodySynth (o pista libre de YouTube Audio Library).
3. Voz por escena: edge-tts (Tico narra, Casquito "canta" el estribillo).
4. Slideshow/karaoke de los personajes nuevos + subtitulos (ffmpeg, ya dominado).
5. Short vertical de 40 s del estribillo (corte).
6. Subida con el pipeline (--youtube-preview) y publicacion el Sabado.

## NIVEL SEMI-PROFESIONAL (ejemplo: El Negro Cirilo / Canti Rondas)
Referencia que el usuario quiere copiar: https://www.youtube.com/watch?v=EIcuS79pLyA
(El Negro Cirilo - Canti Rondas - Canciones Infantiles). LECCION: esos canales NO son
peliculas 3D. Son sing-along animados limpios que FACTURAN: personaje fijo, colores
vivos, letra rebotando al ritmo, bucle de baile, audio alegre y compilaciones largas.
Traducido a nuestro pipeline (todo automatizable y gratis):

- Loops de baile: fila de amigos bailando + protagonista (Animator ya lo dibuja).
- Karaoke que rebota palabra a palabra (Animator.karaoke ya lo hace).
- Palmas + bombo + maracas en la melodia (MelodySynth ya los incluye).
- PULIDO SEMI-PRO (pendiente): eco/reverb de estudio con ffmpeg (aecho), voz mas
  alegre (pitch +), paleta de colores fija por personaje, marca "CONSTRUYENDO
  VALORES" siempre visible, sin texto suelto en pantalla.
- Compilaciones largas (RNDA 3-5 canciones seguidas) para subir minutos vistos.
- Meta-datos copiando el patron del ejemplo: "Canciones Infantiles (Musica para
  Ninos) - Construyendo Valores".

## PLAN DE FACTURACION (requisitos YPP: 1000 subs + 4000 h)
1. Semana 1-2: publicar la cancion de Casquito + 2 shorts/dia (descubrimiento).
2. Semana 3-4: 4 canciones originales + 1 compilacion de 10+ min.
3. Semana 5-8: cuento+musica+shorts cada semana sin cortar la cadencia.
4. Cuando pasemos 1000 subs: activar YPP en YouTube Studio y validar el audit de
   cuota de API (que ya respondimos).
5. Renovacion mensual: revisar etiquetas, titulos y portadas; eliminar videos de
   menos de 0 RPM solo si bajan el promedio.

## PRIMERA CANCION LISTA PARA HACER