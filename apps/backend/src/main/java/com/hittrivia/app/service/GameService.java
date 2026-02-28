package com.hittrivia.app.service;

import com.hittrivia.app.game.Game;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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

    /**
     * Returns a snapshot list of active (non-FINISHED, non-WAITING_CONFIG) games
     * with summary info suitable for a public "who's playing" feed.
     */
    public List<Map<String, Object>> getActiveGames() {
        // vi borde kunna göra någon typ av "låst" klass som håller denna datan.
        // då behöver vi inte generera den på nytt varje gång någon ber om den
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (Game game : games.values()) {
            // Only show games that have actually started (past config)
            var phase = game.getPhase();
            if (phase == Game.Phase.WAITING_CONFIG || phase == Game.Phase.FINISHED) continue;

            String adminId = game.getAdmin();
            String hostName = adminId != null ? game.getPlayerName(adminId) : null;
            if (hostName == null || hostName.isBlank()) continue;

            int playerCount = game.getPlayerCount();
            String genre = game.getGenre();
            String decade = game.getDecade();

            Map<String, Object> entry = new java.util.LinkedHashMap<>();
            entry.put("host", hostName);
            entry.put("players", playerCount);
            if (genre != null && !genre.isEmpty()) entry.put("genre", genre);
            if (decade != null && !decade.isEmpty()) entry.put("decade", decade);
            entry.put("phase", phase.toString());
            result.add(entry);
        }
        return result;
    }
}
