# Hit Trivia

A real-time multiplayer music trivia game. Create a game, invite friends with a QR code, and see who can guess the song from a short clip.

**Live:** [hit-trivia-production.up.railway.app](https://hit-trivia-production.up.railway.app)

## How It Works

1. One player creates a game and configures the genre, decade, obscurity, and number of rounds
2. Other players join by scanning the QR code or sharing the link
3. Each round: a short music clip plays → everyone guesses the title, artist, or album → scores are revealed
4. After all rounds, the final scoreboard shows the winner

## Tech Stack

| Layer    | Technology                                               |
| -------- | -------------------------------------------------------- |
| Frontend | Vue 3 (Options API), Vue Router, @vueuse/core            |
| Backend  | Java 21, Spring Boot 4, WebSocket (TextWebSocketHandler) |
| Music    | Apple Music MusicKit API (track previews & music videos) |
| Build    | Nx monorepo, Maven, Docker multi-stage build             |
| Deploy   | Railway (Dockerfile builder)                             |

## Project Structure

```
apps/
  web/           Vue 3 frontend (served as static files from Spring Boot)
  backend/       Spring Boot backend (WebSocket game server + REST API)
```

## Local Development

```bash
# Install frontend dependencies
cd apps/web && npm install && cd ../..

# Run both frontend and backend in parallel
npm run dev
```

_Ensure that [Java 21](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html) is installed and [Maven](https://maven.apache.org/)_

The frontend dev server runs on `http://localhost:3000` and the backend on `http://localhost:8080`.

### Environment Variables

| Variable                  | Description                                | Default                 |
| ------------------------- | ------------------------------------------ | ----------------------- |
| `APPLE_MUSIC_TEAM_ID`     | Apple Developer Team ID                    | —                       |
| `APPLE_MUSIC_KEY_ID`      | MusicKit private key ID                    | —                       |
| `APPLE_MUSIC_PRIVATE_KEY` | MusicKit private key (PKCS8, Base64)       | —                       |
| `ALLOWED_ORIGINS`         | CORS allowed origins (comma-separated)     | `http://localhost:3000` |
| `SPRING_PROFILES_ACTIVE`  | Spring profile (`local` / `production`)    | `local`                 |
| `PORT`                    | Server port (set automatically by Railway) | `8080`                  |

For local development, create `apps/backend/src/main/resources/application-local.properties` with your Apple credentials:

```
apple.music.team-id=1234asdf
apple.music.key-id=1234asdf
apple.music.private-key=1234asdf
```

## Docker

Uses the same `application-local.properties` you already have for local development — no extra config needed.

```bash
docker compose up --build
```

Open `http://localhost:8080`. The Vue frontend and backend API are served from the same origin.

The multi-stage Dockerfile builds the Vue frontend, packages it into the Spring Boot static resources, and produces a minimal JRE image.

## Architecture

```mermaid
flowchart TB
    subgraph Client ["Browser (Vue 3)"]
        UI[Vue Components]
        WS_C[WebSocket Client<br>@vueuse/core]
        Audio[Audio Player<br>preloads clips]
    end

    subgraph Server ["Spring Boot 4 (Java 21)"]
        direction TB
        REST[REST Controller<br>/api/new-game<br>/api/game-count<br>/api/active-games]
        WSH[GameSocketHandler<br>/ws/game/room-id]
        GS[GameService<br>manages rooms]
        Game[Game Engine<br>phase machine & scoring]
        AMS[AppleMusicCatalogService<br>search & filter tracks]
        Token[AppleMusicTokenService<br>ES256 JWT generation]
    end

    subgraph Apple ["Apple Music API"]
        Catalog[Catalog Search<br>/v1/catalog/us/search]
        MV[Music Videos<br>/v1/catalog/us/music-videos]
    end

    UI -- "HTTP GET" --> REST
    UI <-- "WebSocket (JSON)" --> WS_C
    WS_C <-- "WebSocket" --> WSH
    WSH --> GS
    GS --> Game
    Game -- "fetch tracks on game start" --> AMS
    AMS -- "JWT Bearer token" --> Token
    AMS -- "search songs" --> Catalog
    AMS -- "enrich with videos" --> MV
    Game -- "tracks + phase changes" --> WSH
    WSH -- "broadcast to room" --> WS_C
    WS_C -- "preview URLs" --> Audio
```

### Game Phase Lifecycle

```mermaid
stateDiagram-v2
    [*] --> WAITING_CONFIG: Host creates game
    WAITING_CONFIG --> WAITING: Host submits config<br>(tracks fetched from Apple Music)

    WAITING --> PLAYING_MUSIC: 3 s countdown
    PLAYING_MUSIC --> GUESSING: 15 s clip plays
    GUESSING --> REVEAL: 30 s to guess
    REVEAL --> WAITING: 15 s scores shown<br>(next round)
    REVEAL --> FINISHED: Last round revealed

    FINISHED --> [*]: 120 s auto-cleanup
```

## Deployment

```mermaid
flowchart LR
    subgraph Dev ["Developer"]
        Push[git push]
    end

    subgraph Railway ["Railway"]
        direction TB
        Trigger[Deploy trigger<br>railway.toml]
        subgraph Docker ["Multi-stage Dockerfile"]
            direction TB
            S1["Stage 1: node<br>npm install -> vue-cli-service build<br> -> /app/apps/web/dist"]
            S2["Stage 2: maven<br>Copy dist -> static/<br>mvn package -> fat JAR"]
            S3["Stage 3: temurin<br>COPY app.jar<br>java -jar app.jar"]
            S1 --> S2 --> S3
        end
        Health["Healthcheck<br>GET /api/game-count<br>timeout: 30 s"]
        Restart["Restart policy<br>ON_FAILURE x 3"]
    end

    subgraph Env ["Runtime Environment"]
        PORT["PORT (Railway)"]
        APPLE["APPLE_MUSIC_TEAM_ID<br>APPLE_MUSIC_KEY_ID<br>APPLE_MUSIC_PRIVATE_KEY"]
        ORIGINS["ALLOWED_ORIGINS"]
    end

    Push --> Trigger
    Trigger --> Docker
    S3 --> Health
    S3 --> Restart
    Env --> S3
```

## Design Decisions

**All tracks sent upfront** - When a game starts, every client receives the full track list. During each WAITING phase before PLAYING_MUSIC, the browser preloads the next audio clip so there's zero buffering.

**Server-authoritative timing** - All phase countdowns use server timestamps. Clients compute progress bars from `startTimestamp` / `endTimestamp`, so even a page reload shows the correct remaining time.

**Reconnect-first** - Player ID stored in localStorage. On page reload, the client auto-reconnects to the WebSocket and the server sends the full current game state (phase, tracks, round, scores). The experience is seamless.

**Mute by default for guests** - Only the host plays music out loud by default. Other players are muted to avoid cacophony in the same room. Everyone can toggle with the floating speaker button.

**Music Engine** - Currently using the Top lists from Apple Music for genre and decade.
