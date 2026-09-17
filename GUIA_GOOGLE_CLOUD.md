# Conectar la app a YouTube (una sola vez, gratis, ~10 minutos)

La app necesita permiso para subir videos a TU canal. Este permiso se crea en
**Google Cloud Console** con tu cuenta de Google (la misma del canal). Es gratis
y no requiere tarjeta.

## Paso 1 — Crear el proyecto

1. Entra en <https://console.cloud.google.com> con tu cuenta de Google.
2. Arriba a la izquierda, en el selector de proyectos, pulsa **Nuevo proyecto**.
3. Nombre: `Construyendo Valores`. Crea.
4. Selecciónalo en el selector (si no aparece, recarga).

## Paso 2 — Activar la API de YouTube

1. Menú (☰) → **APIs y servicios** → **Biblioteca**.
2. Busca **YouTube Data API v3** → pulsa **Habilitar**.

## Paso 3 — Crear el Cliente OAuth (tipo "App de escritorio")

1. Menú (☰) → **APIs y servicios** → **Pantalla de consentimiento de OAuth**.
2. Marca **Externo** → Crear. Rellena Nombre (`Construyendo Valores Studio`),
   email de soporte y email para avisos. Guarda.
3. Agrega tu cuenta de Google como **usuario de prueba** (pestaña "Usuarios de prueba").
4. En la pestaña **Audiencia**, cambia el estado de publicación a
   **EN PRODUCCIÓN** (se publica la app para uso personal; aparece una advertencia
   de "app no verificada", que puedes ignorar: tu propia cuenta puede autorizarla).
   *Por qué es importante:* en modo "Pruebas" el acceso expira a los 7 días;
   en producción no expira.
5. **Clientes OAuth 2.0** → **Crear credenciales** → **ID de cliente de OAuth**.
6. Tipo de aplicación: **App de escritorio** → Crear.
7. Copia el **ID de cliente** y el **secreto de cliente**.

## Paso 4 — Pegar los datos en la app

1. Abre la app → pestaña **Configuración**.
2. Pega el *Client ID* en "YouTube Client ID (OAuth)" y el *Client Secret*.
3. Pulsa **Guardar configuración**.

## Paso 5 — Conectar (abre el navegador)

Pestaña **Automatización** → **Conectar YouTube**. El navegador abre una página
de Google: elige tu cuenta, acepta (puede advertir "app no verificada" → *Avanzado*
→ *Continuar*). Al final dice **Autorizado** y la app queda vinculada.
(O desde la línea de comandos: `--youtube-auth`.)

Para comprobar que todo está listo: pestaña Automatización → **Probar conexión**.

## Nota importante sobre la publicación pública

Videos subidos con la API desde un proyecto **no auditado** (creado después de
julio de 2020) quedan **privados** y no pueden publicarse solos. Para que el
publicado automático a las 9:00 funcione del todo, Google pide una revisión
gratuita del proyecto:

1. En YouTube Studio, sube un video como prueba.
2. Síguelo: si aparece "Este video se ha subido mediante una API no auditada",
   pulsa el aviso para **iniciar la auditoría** (explicas que es un canal propio
   de cuentos infantiles; tarda entre 1 y 30 días).
3. Mientras tanto, **todo sigue funcionando**: la app sube y programa los videos
   en privado. Los verás completos en tu canal (solo a ti), y en cuanto llegen
   la auditoría, se publicarán solos a las 9:00.

Mientras tanto, si quieres publicar manualmente algún video ya subido: YouTube
Studio → Videos → el marcado como privado → menú → "Publicar" (dos clics).

## Factor de limpieza

- El acceso se guarda en `%USERPROFILE%\.construyendo-valores\youtube_tokens.json`.
- Para revocarlo cuando quieras: cuenta de Google → Seguridad → Aplicaciones
  de terceros → quitar "Construyendo Valores Studio".