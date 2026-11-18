# YouTube API usage — evidence for the compliance audit

**API Client:** Construyendo Valores Studio
**Google Cloud project number:** 738382352973
**Channel operated:** https://www.youtube.com/channel/UCMD3CbCEBS0qH_-5_JlD9IQ
**OAuth scope:** `https://www.googleapis.com/auth/youtube` (single scope, nothing else)

This repository contains the **complete, working application** (not a snippet). It is a
single-user desktop tool: the only person who can run it is the channel owner, and the only
channel it can touch is the one authorized through OAuth.

## Endpoints actually used

| Endpoint | Where in the code | Why |
|---|---|---|
| `videos.insert` (resumable upload) | `Youtube.upload` | Upload the video the app produced |
| `videos.update` (part=snippet) | `Youtube.setMetadata` | Fix title / tags / category on the owner's own videos |
| `videos.update` (part=status) | `Youtube.setPrivacy` | Publish or unpublish the owner's own videos |
| `videos.list` (mine) | `Youtube.listMine`, `Youtube.listEstadisticas` | Read the owner's own uploads and view counts |
| `channels.list` (mine) | `Youtube.channelUrl`, `Youtube.channelName` | Confirm which channel is linked |
| `playlistItems.list` | `Youtube.listMine` | Enumerate the owner's uploads playlist |
| `playlists.insert` / `playlists.delete` | `Compilacion` | Group the owner's own videos into compilations |
| `videos.delete` | `Youtube.delete` | Delete the owner's own videos |
| `thumbnails.set` | `Youtube.setThumbnail` | Set the thumbnail of the owner's own videos |

No other YouTube endpoint is called. There is **no** use of `search.list`, comments, captions,
live streaming, or any endpoint that reads another channel's content.

## How the upload flow works

1. `--youtube-auth` starts a local loopback listener on `http://localhost:8765/` and opens the
   Google consent screen for the single scope above.
2. The authorization code is exchanged for an access token and a refresh token, stored in
   `%USERPROFILE%\.construyendo-valores\youtube_tokens.json` on the owner's computer only.
3. `--subir <archivo.mp4> "Title" "Description"` uploads with `videos.insert`, resumable
   (`uploadType=resumable`), declaring `selfDeclaredMadeForKids=true`.
4. The app never runs a server, has no accounts, no sign-up, no ads and no analytics.

## Build and run

```powershell
mvn package
java -jar target\construyendo-valores-studio-1.0.0.jar --youtube-auth
java -jar target\construyendo-valores-studio-1.0.0.jar --subir "video.mp4" "Title" "Description"
```

Requirements: Java 21 and Maven. The app compiles from this repository with `mvn package`.

## What the application does with the videos

Every video is an **original production**: the script is written by the owner (optionally
assisted by a language model), the narration is synthesized with `edge-tts`, the illustrations
are AI-generated, and the final edit is assembled locally with `ffmpeg`. The app does not
download, re-upload, mirror or re-use any third party's video.

## Privacy

- https://menesesdavian1-source.github.io/construyendo-valores-web/
- https://menesesdavian1-source.github.io/construyendo-valores-web/privacy-policy.html
- https://menesesdavian1-source.github.io/construyendo-valores-web/terms.html

Contact: menesesdavian1@gmail.com
