package com.hittrivia.app.service;

import com.hittrivia.app.game.Game;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class GameService {
    private final Map<String, Game> games = Collections.synchronizedMap(new HashMap<>());

    public Game createGame(String roomId) {
        Game game = new Game(roomId);
        games.put(roomId, game);
        return game;
    }

    public Game getGame(String roomId) {
        return games.get(roomId);
    }

    public void deleteGame(String roomId) {
        games.remove(roomId);
        System.out.println("Game deleted: " + roomId);
    }

    public int getGameCount() {
        return games.size();
    }
}
