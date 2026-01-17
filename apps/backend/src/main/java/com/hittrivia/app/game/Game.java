package com.hittrivia.app.game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import com.hittrivia.app.config.GameConfig;

import lombok.Getter;
import tools.jackson.databind.ObjectMapper;

@Getter
public class Game {
    private String gameId;
    private List<String> players;
    private String admin; // This class should not store the websocket session, it should store ids.
    private GameState state;
    private GameConfig config;

    public enum GameState {
        WAITING_CONFIG, IN_PROGRESS, FINISHED
    }

    public Game(String gameId, WebSocketSession firstPlayer) {
        this.gameId = gameId;
        this.players = new ArrayList<>();
        this.admin = firstPlayer.getAttributes().get("playerId").toString();
        this.state = GameState.WAITING_CONFIG;
        this.players.add(firstPlayer.getAttributes().get("playerId").toString());
    }

    public void reinstatePlayer(String playerId) {
    }

    public void addPlayer(WebSocketSession session) {
    }

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private void setAdmin(WebSocketSession session) {
        
    }

    public boolean isPlayer(String playerId) {
        return players.contains(playerId);
    }

    public void removePlayer(String playerId) {
        players.remove(playerId);
    }

    public boolean isAdmin(String playerId) {
        return admin.equals(playerId);
    }

    public void setConfig(GameConfig config) {
        // Here we want to validate that the config is "legal"
        this.config = config;
        this.state = GameState.IN_PROGRESS;
    }

    public GameState getState() {
        return state;
    }

    public List<String> getPlayers() {
        return players;
    }

    public int getPlayerCount() {
        return players.size();
    }
}
