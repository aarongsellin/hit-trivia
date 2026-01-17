package com.hittrivia.app.service;

import com.hittrivia.app.game.Game;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

@Service
public class GameService {
        private final Map<String, Game> games = 
        Collections.synchronizedMap(new HashMap<>());

    public Game createGame(String roomId, WebSocketSession admin) {
        Game game = new Game(roomId, admin);
        games.put(roomId, game);
        System.out.println("Game created: " + roomId);
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
