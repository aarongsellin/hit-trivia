## What should we require of this application?

I have decided to format this document as a sort of story for the entire event loop from when a game is created until it is finished or left by the players.

### Creation

1. A user clicks "create game", and get routed to the game page.
2. A QR code will show with a link to this page.

### Start

**configuration:**

1. Admin sends which genres, obscurity parameters and timeframe of the songs. Also how many questions they want.
2. The server will then get tracks by those parameters from YouTube and store them in redis with the game id.

**Search Engine**:
We could go a second or two right before the most watched part of the video?

3. The server tells the users that the quizz is ready!

### Loop

1. Music plays for 5 - 10 seconds.
2. Users get 15 - 30 seconds to guess. Track name and artist match count as points.
3. The track name and artist appears, also with video playing in embedded YouTube player.

### End

1. A summary is shown, with all the participants scores.

### At Server crash/restart

We want to save the states of all games to Redis to ensure that even if the game server goes down,
everybodies games are not aborted or stop working. This means we need good cleanup to prevent
memory leaks.
