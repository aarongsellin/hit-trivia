package com.hittrivia.app.model;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hittrivia.app.dto.MessageType;

public class GameWebSocketSession {
    private final WebSocketSession session;

    private static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public GameWebSocketSession(WebSocketSession session) {
        this.session = session;
    }

    public void sendJsonMessage(MessageType type, Map<String, Object> data) throws Exception {
        Map<String, Object> msg = new HashMap<>();

        msg.put("type", type.getValue());
        msg.putAll(data);
        msg.put("timestamp", System.currentTimeMillis());

        session.sendMessage(new TextMessage(OBJECT_MAPPER.writeValueAsString(msg)));
    }

    public String getGameId() {
        String path = session.getUri().getPath();
        String gameId =  path.substring(path.lastIndexOf("/") + 1);

        return gameId;
    }

    public String getPlayerId() {
        Object playerId = session.getAttributes().getOrDefault("playerId", null);

        if (playerId == null) {
            return null;
        }

        return playerId.toString();
    }

    public WebSocketSession getSession() {
        return session;
    }

    public boolean isOpen() {
        return session.isOpen();
    }

    public void close() throws Exception {
        session.close();
    }
}
