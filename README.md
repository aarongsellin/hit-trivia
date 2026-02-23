#### JUST NU

Vi behöver ett sätt i frontend att sätta en spelare mitt i en phase. En check i gameView för när de får gameState, om en timestamp är inom en viss ram av tid så ska den automatiskt sätta upp allting ordentligt, och skippa ordentligt i playern. Vi behöver skicka med en startTimestamp och inte bara endTimestamp för gameState/phaseChange

# Introduction

Hit Trivia is an online music quizz game that I built to learn Spring Boot. Anyone can create a game, invite their friends with a QR code, and see who has the best music knowledge.

# How to setup

```
npx run dev
```

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

**Analytics**: We track failed requests to a Redis database, that way we can quickly see if a new update is increasing client failures.

### Monetization

Apple Music has a very strict roster of who is allowed to become an affiliate, only websites that drive strong and meaningfull trafic are allowed into their "" program. Therefor in the beginning the most viable option is to play an add before starting the game. But we have to be carefull as to not make it too intrisive, a banner add on each device as things are playing is OK and maybe a video add for the creator of the game when they hit create game.

Why only the host is allowed to play the Music.

Game creator need to be able to set timer states.

Autoplay songs or click to continue.

Användare behöver skriva in ett namn

Tydligt visa för användaren om de tappar uppkoppling till spelet

Tungt focus på re-connect, om en användare tappar uppkoppling, laddar om sidan, etc, så får användaren upplevelsen som att ingenting hände.
