package com.hittrivia.app.handlers;

import org.springframework.stereotype.Service;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.CloseStatus;

import com.fasterxml.jackson.databind.JsonNode;
import com.hittrivia.app.dto.ErrorResponse;
import com.hittrivia.app.dto.GameResponse;
import com.hittrivia.app.game.Game;
import com.hittrivia.app.service.GameMessageService;
import com.hittrivia.app.service.GameService;
import com.hittrivia.app.validators.JsonValidator;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.http.WebSocket;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class GameSocketHandler extends TextWebSocketHandler {
    private final GameMessageService gameMessageService;

    private static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public GameSocketHandler(GameMessageService gameMessageService) {
        this.gameMessageService = gameMessageService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);
        String roomId = getRoomIdFromSession(session);
        gameMessageService.handleConnectionEstablished(session, roomId);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
        gameMessageService.handleConnectionClosed(session.getAttributes().get("playerId").toString(), getRoomIdFromSession(session));
    }

    private void sendError(WebSocketSession session, String error) throws Exception {
        ErrorResponse msg = new ErrorResponse(error);
        session.sendMessage(new TextMessage(OBJECT_MAPPER.writeValueAsString(msg)));
    }

    private void broadcastToGame(String roomId, String type, Object data) throws Exception {
        // GameResponse<Object> response = new GameResponse<>(type, data);
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        super.handleTextMessage(session, message);

        String payload = message.getPayload();

        if (!JsonValidator.validate(payload)) {
            sendError(session, "Invalid JSON");
            return;
        }

        JsonNode jsonMessage = OBJECT_MAPPER.readTree(payload);
        String messageType = jsonMessage.fieldNames().next();
        String playerId = (String) session.getAttributes().get("playerId");
        String roomId = getRoomIdFromSession(session);

        gameMessageService.handleMessage(messageType, jsonMessage, playerId, roomId, session);
    }

    private String getRoomIdFromSession(WebSocketSession session) {
        String path = session.getUri().getPath();
        return path.substring(path.lastIndexOf("/") + 1);
    }
}
