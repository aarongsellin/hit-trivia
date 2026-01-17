package com.hittrivia.app.service;

import java.util.Map;
import java.util.UUID;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class GameMessageService {
    private final GameService gameService;

    public GameMessageService(GameService gameService) {
        this.gameService = gameService;
    }

    private final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public void handleMessage(String messageType, JsonNode message, String playerId, String roomId, WebSocketSession session) {
        switch (messageType) {
            case "reconnect":
                handleReconnect(message, session, roomId);
                break;
        }
    }

    private void handleReconnect(JsonNode message, WebSocketSession session, String roomId) {
        // Get player id from data

        // Assign the player id back to the game.
        gameService.getGame(roomId);
    }

    public void handleConnectionClosed(String playerId, String roomId) {

    }

    private String createPlayerId() {
        return UUID.randomUUID().toString();
    }

    public static class MessageType {
        public static final String RECONNECT = "reconnect";
        public static final String DATA = "data";
    }

    private void sendJsonMessage(WebSocketSession session, String type, Object data) throws Exception {
        Map<String, Object> msg = Map.of("type", type, "data", data);
        session.sendMessage(new TextMessage(OBJECT_MAPPER.writeValueAsString(msg)));
    }

    public void handleConnectionEstablished(WebSocketSession session, String gameId) {
        Object playerId = session.getAttributes().getOrDefault("playerId", null);
        
        if (playerId == null) {
            String newPlayerId = createPlayerId();
            session.getAttributes().put("playerId", newPlayerId);
            
            // Todo: Add better fallback logic here as this is a critical part.
            try {
                sendJsonMessage(session, "data", Map.of("playerId", newPlayerId));
            } catch(Exception err) {
                System.out.println(err);
            }
        }
    }
}
