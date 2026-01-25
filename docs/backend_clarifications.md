### Player join states

- Null playerId means they are new and want to join
- Already have an ID means that they were in the game previously.
- The player can have an old playerID cached, we cross-check against existing player ids and handle that outcome.

### Player roles

**Admin** Only one allowed to configure the game-loop and is the creator of the game.

**Player** Can only send answers to questions.
