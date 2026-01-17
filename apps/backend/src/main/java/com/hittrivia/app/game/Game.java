package com.hittrivia.app.game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import com.hittrivia.app.config.GameConfig;

import tools.jackson.databind.ObjectMapper;

public class Game {
    private String roomId;
    private List<WebSocketSession> players;
    private WebSocketSession admin;
    private GameState state;
    private GameConfig config;

    public enum GameState {
        WAITING_CONFIG, IN_PROGRESS, FINISHED
    }

    public Game(String roomId, WebSocketSession firstPlayer) {
        this.roomId = roomId;
        this.players = new ArrayList<>();
        this.admin = firstPlayer;
        this.state = GameState.WAITING_CONFIG;
        this.players.add(firstPlayer);
    }

    public void addPlayer(WebSocketSession session) {
        if (session == null) {
            throw new IllegalArgumentException("Session cannot be null");
        }

        if (players.contains(session)) {
            session.getAttributes().put("error", "Already joined");
            return;
        }

        if (admin == null) {
            setAdmin(session);
        }

        players.add(session);
    }

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private void setAdmin(WebSocketSession session) {
        this.admin = session;

        session.getAttributes().put("isAdmin", true);

        Map<String, Object> message = new HashMap<>();
        message.put("type", "role_assigned");
        message.put("role", "admin");
        message.put("roomId", roomId);

        try {
            String jsonMessage = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(jsonMessage));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isPlayer(WebSocketSession session) {
        return players.contains(session);
    }

    public void removePlayer(WebSocketSession session) {
        players.remove(session);
    }

    public boolean isAdmin(WebSocketSession session) {
        return admin.equals(session);
    }

    public void setConfig(GameConfig config) {
        // Here we want to validate that the config is "legal"
        this.config = config;
        this.state = GameState.IN_PROGRESS;
    }

    public GameState getState() {
        return state;
    }

    public List<WebSocketSession> getPlayers() {
        return players;
    }

    public int getPlayerCount() {
        return players.size();
    }
}
