package com.hittrivia.app.handlers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

public class SocketConnectionHandler extends TextWebSocketHandler {
    // Here we store the connections in-memory.
    // List<WebSocketSession> webSocketSessions
    //     = Collections.synchronizedList(new ArrayList<>());
    private static final Map<String, List<WebSocketSession>> rooms = Collections.synchronizedMap(new HashMap<>());

    // This function runs when a client tris to connect.
    @Override // <-- Overrides parent method, only works if parent has afterConnectionEstablished method.
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);

        // Extract room ID from URL path.
        String path = session.getUri().getPath();
        String room = path.substring(path.lastIndexOf("/") + 1);
        System.out.println("New connection to: " + room);

        // Add this session to the room.
        rooms.computeIfAbsent(room, k -> Collections.synchronizedList(new ArrayList<>())).add(session);

        // Logging the connection ID with Connected Message
        System.out.println(session.getId() + " Connected to room: " + room);
    }

    // When client disconnect from WebSocket then this
    // method is called
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);

        String path = session.getUri().getPath();
        String room = path.substring(path.lastIndexOf("/") + 1);

        rooms.getOrDefault(room, new ArrayList<>()).remove(session);

        System.out.println(session.getId() + " Disconnected from room: " + room);
    }

    public static void createRoom(String roomId) {
        rooms.putIfAbsent(roomId, Collections.synchronizedList(new ArrayList<>()));
        System.out.println("WebSocket room created: " + roomId);

        // The behaviour for the room should be that the first person
        // that connected to the room becomes the "admin".
        // Meaning that we only allow them to configure and start the game.
    }

    public static Integer getRoomCount() {
        return rooms.size();
    }

    @Override
    public void handleMessage(WebSocketSession session,WebSocketMessage<?> message) throws Exception {
        super.handleMessage(session, message);

        String path = session.getUri().getPath();
        String room = path.substring(path.lastIndexOf("/") + 1);

        for (WebSocketSession roomSession : rooms.getOrDefault(room, new ArrayList<>())) {
            if (session != roomSession) {
                roomSession.sendMessage(message);
            }
        }
    }

}
