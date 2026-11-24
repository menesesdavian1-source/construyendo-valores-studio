# RESPONDER A YOUTUBE API — SOLICITUD DE AUDIT (CUOTA)

Correo recibido de "youtube-disputes+187di0hv26ixp1n@google.com" pidiendo:
"script or screencast of the complete use case showing how the videos are being uploaded
to YouTube by using YouTube API services."

## QUE ENVIAR (3 piezas)
1. El script: `DEMO_YOUTUBE_API.ps1` (re-ejecutable).
2. El log de una ejecucion real: guarda la salida del script en un .txt y adjuntalo.
3. (Opcional pero recomendado) Screencast de 1-2 minutos ejecutando el script:
   - Abre PowerShell como se indica abajo y graba con la tecla Win+G (Xbox Game Bar) o OBS.
   - Muestra: la subida, la URL del video resultante y la confirmacion en YouTube Studio.

## COMO VOLVER A EJECUTAR (para grabar)
```powershell
cd "C:\Users\Davia\OneDrive\Documents\Default Project\ConstruyendoValoresStudio"
powershell -NoProfile -ExecutionPolicy Bypass -File .\DEMO_YOUTUBE_API.ps1
```
Cada subida nueva consume cuota (una unidad cada una) y queda PRIVADA en el canal.
Eliminalas despues del audit si quieres (YouTube Studio > Contenido > Privado).

## CASO DE USO (texto para copiar en el correo)
"Aplicacion de escritorio Java (Construyendo Valores Studio), para un canal infantil.
Flujo de subida con la YouTube Data API v3:
1) OAuth 2.0 con scopes
   https://www.googleapis.com/auth/youtube (dialogo del navegador, token guardado
   y refrescado automaticamente: access_token + refresh_token).
2) videos.insert con uploadType=resumable y part=snippet,status.
   Snippet: title, description, categoryId=24 (Entretenimiento), defaultLanguage=es.
   Status: privacyStatus (private durante el audit), selfDeclaredMadeForKids=true,
   containsSyntheticMedia=false.
3) Subida del archivo mp4 en chunks al endpoint resumable, con reintentos ante 5xx/429.
4) Verificacion: videos.list de la playlist de subidas del canal autenticado.
El canal autenticado es: Construyendo Valores (@construyendovalores-i2r,
UC UCMD3CbCEBS0qH_-5_JlD9IQ), unico canal."

## LOG REAL (ejemplo de la ultima ejecucion - adjuntar el archivo)
```
PASO 1 - Verificar conexion y permiso de la API
Canal conectado: Construyendo Valores
PASO 2 - Obtener datos del canal autenticado
Canal autorizado: https://www.youtube.com/channel/UCMD3CbCEBS0qH_-5_JlD9IQ  (@construyendovalores-i2r)
PASO 3 - Subir video (resumable, privado)
  Canal: Construyendo Valores
  Token OK (ya29...)
  Archivo: ...\videos_piloto\piloto_torrecuente_storyboard.mp4 (7.590.270 bytes)
  ID  : qx5VgqkzeDU
  URL : https://youtu.be/qx5VgqkzeDU
  Privacidad: private
PASO 4 - Listar ultimos videos del canal
  qx5VgqkzeDU | private | Construyendo Valores - Demo subida API (caso de uso audit)
  zRRjAjD6jpI | private | Construyendo Valores - Demo subida API (caso de uso audit)
```
(Videos de prueba PRIVADOS en el canal UCMD3CbCEBS0qH_-5_JlD9IQ.)

## BORRADOR DE CORREO (responder a youtube-disputes+187di0hv26ixp1n@google.com)

Asunto: RE: Quota increase request - YouTube API - supplemental material

Hello YouTube API Services Team,

Thank you for reviewing our request. Below is the complete use case of how videos are
uploaded to YouTube using the YouTube Data API v3, with supporting material attached.

Purpose: A desktop Java application (Construyendo Valores Studio) for a kids' channel
that produces educational videos. The upload flow is fully automated and OAuth 2.0 based.

Flow:
1. OAuth 2.0 (client authorization code) with scope
   https://www.googleapis.com/auth/youtube. The access token is refreshed automatically.
2. videos.insert called with uploadType=resumable and part=snippet,status.
   - snippet: title, description, categoryId=24, defaultLanguage=es
   - status: privacyStatus=private (while the API compliance review is pending),
     selfDeclaredMadeForKids=true, containsSyntheticMedia=false
3. The mp4 file is streamed to the resumable upload endpoint, with automatic retries
   on 5xx and 429 responses.
4. Verification: videos.list over the channel's uploads playlist of the authenticated
   channel: "Construyendo Valores" (@construyendovalores-i2r,
   channel id UCMD3CbCEBS0qH_-5_JlD9IQ). This is the only channel used.

Attachments:
- DEMO_YOUTUBE_API.ps1 : executable script of the complete use case.
- demo_youtube_api_log.txt : real execution log showing a successful private upload
  (https://youtu.be/qx5VgqkzeDU) confirmed in the channel's video list.
- (optional) screencast.mp4 : video recording of the script running end to end.

Please let us know if you need any additional information.

Best regards,
The Construyendo Valores team