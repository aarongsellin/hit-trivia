package com.hittrivia.app.handlers;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class GeneralSocketHandler extends TextWebSocketHandler {
    // Here we store the connections in-memory.
    // List<WebSocketSession> webSocketSessions
    //     = Collections.synchronizedList(new ArrayList<>());

    // This function runs when a client tris to connect.
    @Override // <-- Overrides parent method, only works if parent has afterConnectionEstablished method.
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
    }

    @Override
    public void handleMessage(WebSocketSession session,WebSocketMessage<?> message) throws Exception {
        super.handleMessage(session, message);
    }

}
