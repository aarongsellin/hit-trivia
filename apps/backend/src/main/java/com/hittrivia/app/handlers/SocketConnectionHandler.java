package com.hittrivia.app.handlers;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.hittrivia.app.service.GameService;
import com.nimbusds.jose.shaded.gson.JsonObject;
import com.hittrivia.app.game.Game;

import com.hittrivia.app.validators.JsonValidator;
@Service
public class SocketConnectionHandler extends TextWebSocketHandler {
    // Here we store the connections in-memory.
    // List<WebSocketSession> webSocketSessions
    //     = Collections.synchronizedList(new ArrayList<>());
    private final GameService gameService;

    public SocketConnectionHandler(GameService gameService) {
        this.gameService = gameService;
    }

    private String extractRoomId(WebSocketSession session) {
        String path = session.getUri().getPath();
        return path.substring(path.lastIndexOf("/") + 1);
    }

    // This function runs when a client tris to connect.
    @Override // <-- Overrides parent method, only works if parent has afterConnectionEstablished method.
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);

        // Extract room ID from URL path.
        String roomId = extractRoomId(session);
        Game game = gameService.getGame(roomId);

        System.out.println("New connection to: " + roomId);

        if (game == null) {
            session.sendMessage(new TextMessage("Error: Game not found"));
            session.close();
            return;
        }

        game.addPlayer(session);
        // We have to give the user a unique ID they can use later on to get their session state back.

        String userId = UUID.randomUUID().toString();

        JsonObject userIdJsonObject = new JsonObject();

        userIdJsonObject.addProperty("userId", userId);

        session.sendMessage(new TextMessage(userIdJsonObject.toString()));

        // Logging the connection ID with Connected Message
        System.out.println(session.getId() + " Connected to game: " + roomId);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);

        String roomId = extractRoomId(session);
        Game game = gameService.getGame(roomId);

        if (game != null) {
            game.removePlayer(session);
        }
    }

    @Override
    public void handleMessage(WebSocketSession session,WebSocketMessage<?> message) throws Exception {
        super.handleMessage(session, message);

        String roomId = extractRoomId(session);
        Game game = gameService.getGame(roomId);

        if (game == null) {
            session.sendMessage(new TextMessage("Error: Game not found"));
            return;
        }

        if (!game.isPlayer(session)) {
            session.sendMessage(new TextMessage("Error: You are not part of this game"));
            return;
        }

        String payload = message.getPayload().toString();

        if (JsonValidator.validate(payload) == false) {
            session.sendMessage(new TextMessage("Error: Invalid JSON format"));
            return;
        }

        



        // Broadcast message to all other players in the room
        // for (WebSocketSession player : game.getPlayers()) {
        //     if (player != null && session != player) {
        //         player.sendMessage(message);
        //     }
        // }
    }

}
