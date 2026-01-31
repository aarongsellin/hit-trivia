package com.hittrivia.app.handlers;

import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.CloseStatus;

import com.fasterxml.jackson.databind.JsonNode;
import com.hittrivia.app.dto.ErrorResponse;
import com.hittrivia.app.service.GameMessageService;
import com.hittrivia.app.validators.JsonValidator;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class GameSocketHandler extends TextWebSocketHandler {
    private final GameMessageService gameMessageService;

    private static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public GameSocketHandler(GameMessageService gameMessageService) {
        this.gameMessageService = gameMessageService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);
        gameMessageService.handleConnectionEstablished(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
        gameMessageService.handleConnectionClosed((String) session.getAttributes().getOrDefault("playerId", null), getRoomIdFromSession(session));
    }

    private void sendError(WebSocketSession session, String error) throws Exception {
        ErrorResponse msg = new ErrorResponse(error);

        session.sendMessage(new TextMessage(OBJECT_MAPPER.writeValueAsString(msg)));
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

        if (jsonMessage.fieldNames().hasNext() == false) {
            sendError(session, "Empty message");
            return;
        }

        gameMessageService.handleMessage(session, jsonMessage, getRoomIdFromSession(session));
    }

    private String getRoomIdFromSession(WebSocketSession session) {
        String path = session.getUri().getPath();
        return path.substring(path.lastIndexOf("/") + 1);
    }
}
