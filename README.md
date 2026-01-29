# Introduction

Hit Trivia, som Hitser fast online. Öppna ett spel, välj årtal och eller gener, starta, QR kod visas på enheten och alla kan gå med.

# How to setup

# Developer Story

Here I explain why I made the design choices that I made and also go into detail about the stack and what the different parts are used for.

### Stack

Stack:
Frontend: Vue + localStorage (för player state)
Backend: Spring Boot + WebSocket (för game coordination)
Database: PostgreSQL (för game history/stats och eventuellt tracks) (mest nice to have) & Redis (to restore games after a crash or quick restart)

### Depth on design and optimization choices.

Here I go over the steps for optimization that I made and why.

**Tracks & Apple WebKit**: when a game starts, all clients get the information about all the tracks that will be played during the run of the game. During each WAIT phase before the PLAYING_MUSIC phase we pre-load the part of the track that we will use, this way there is no buffering for the client.

**Time Conformity**: All phase changes are based around a detla from what the server regards as the current time.

**Crash Stuff**: We store each started game in Redis, when anything changes we update the relevant template in redis for each game. When the server startsup it will first check Redis for any games that were started and that are still in progress prior to the crash. This ensures that games get re-instated and not lost even after a crash.
