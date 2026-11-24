# Construyendo Valores Studio

An internal, single-channel automation tool that produces and publishes short educational animated stories for children (ages 3–7, Spanish-speaking audience) on the YouTube channel **Construyendo Valores** (`@ConstruyendoValores-i2r`).

- Channel: https://www.youtube.com/@ConstruyendoValores-i2r
- Public website (home, privacy policy, terms): https://menesesdavian1-source.github.io/construyendo-valores-web/
- Documentación en español: [README.es.md](README.es.md)

## What this tool is (and is not)

- It is operated by the channel owner, for the channel owner's **own channel only**.
- It is **not** a service offered to third parties. There are no other users, no accounts, and no sign-up.
- It does not read, store, or process personal data about viewers.
- All uploaded videos are declared as **made for kids** (`status.selfDeclaredMadeForKids = true`).

## How it works

```
AI-assisted script  →  text-to-speech narration  →  2D animation (Java)
        →  ffmpeg rendering  →  quality check  →  YouTube upload (OAuth 2.0)
```

1. A script is generated with an AI model (Google Gemini by default; other providers are configurable) from a fixed cast of original characters and a values-based story template.
2. Narration is synthesized with a text-to-speech engine (edge-tts by default).
3. Scenes are animated and rendered to 1080×1920 (H.264 + AAC) with ffmpeg.
4. An automatic quality score is computed for each video (the threshold is configurable) and recorded in a local report.
5. The finished video is uploaded to the owner's channel using the YouTube Data API v3 with a resumable upload. The owner can also run this step from a scheduled daily task.

## YouTube API usage

The tool authenticates with **OAuth 2.0** as the channel owner. It uses the following YouTube Data API v3 resources, only on the owner's own channel and videos:

| Resource | Purpose |
|---|---|
| `upload/youtube/v3/videos` | Resumable upload of the owner's finished videos, with title, description, tags, and the made-for-kids declaration. |
| `youtube/v3/videos` (list) | Reading the snippet, status, and basic public statistics of the owner's own videos, for local reporting. |
| `youtube/v3/videos` (update, `part=status`) | Changing the privacy status (public / private / unlisted) of the owner's own videos, for example to take a video offline while it is being improved. |
| `youtube/v3/videos` (update, `part=snippet`) | Improving the title, tags, and category of the owner's own already-uploaded videos. The current description is read first and preserved. |
| `youtube/v3/videos` (delete) | Deleting the owner's own videos that are no longer wanted (for example, replaced or rejected by the owner's quality review). Only the linked channel's videos can be affected. |
| `youtube/v3/playlistItems` | Listing the owner's own uploaded videos (read-only), via the channel's uploads playlist, for local reporting. |
| `youtube/v3/channels` | Reading the owner's own channel information (including the uploads playlist ID) and confirming that the authorization is valid. |

OAuth scope requested: `https://www.googleapis.com/auth/youtube`. This scope is needed because the tool updates and deletes the owner's own videos (`videos.update`, `videos.delete`), which narrower upload-only or read-only scopes do not allow.

The tool does **not** access any other channel's private data, does not post comments on behalf of others, and does not scrape or republish third-party content.

## Data handling

- OAuth tokens and API keys are stored **locally on the owner's computer**, outside this repository, in a private configuration folder. They are never committed (see `.gitignore`).
- Video metadata and view counts of the owner's own videos are stored locally in a CSV file for the owner's own reporting.
- No viewer data is collected, shared, sold, or transferred to third parties.
- See the privacy policy on the public website linked above.

## Configuration

The tool reads its settings from `%USERPROFILE%\.construyendo-valores\config.properties`, which lives **outside this repository**. Use `config.properties.example` as a template: copy it to that location, rename it to `config.properties`, and fill in your own credentials. Never commit that file; `*.properties` is already listed in `.gitignore`.

You must create your own Google Cloud project, enable the YouTube Data API v3, and create OAuth 2.0 credentials for a desktop application.

## Build

Requirements: Java 17+, Maven 3.9+ (build only), Python 3.12 with `pip install edge-tts`, and ffmpeg available on the system path. A free Google Gemini API key is used for script generation.

```
mvn package
java -jar target/construyendo-valores-studio-1.0.0.jar --estado
```

## Content policy

The channel follows YouTube's policies for children's content. Stories are positive, non-violent, and centered on values such as honesty, respect, teamwork, and perseverance. Stories are produced by the channel owner with AI assistance; some are retellings of public-domain tales.

## Contact

Channel owner: contact through the public website linked above.
