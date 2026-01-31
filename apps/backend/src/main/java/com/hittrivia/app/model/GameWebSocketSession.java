package com.hittrivia.app.model;

import org.springframework.web.reactive.socket.WebSocketSession;

public class GameWebSocketSession {
    private final WebSocketSession session;
    private final String gameId;
    private final String playerId;

    GameWebSocketSession(WebSocketSession session, String gameId, String playerId) {
        this.session = session;
        this.gameId = gameId;
        this.playerId = playerId;
    }

    public String getGameId() {
        return gameId;
    }

    public String getPlayerId() {
        return playerId;
    }

    public WebSocketSession getSession() {
        return session;
    }

    public boolean isOpen() {
        return session.isOpen();
    }
}
