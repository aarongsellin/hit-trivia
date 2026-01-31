package com.hittrivia.app.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hittrivia.app.game.Game;
import com.hittrivia.app.model.GameWebSocketSession;

@Service
public class GameMessageService {
    private final GameService gameService;
    private final Map<WebSocketSession, GameWebSocketSession> sessionContexts = new ConcurrentHashMap<>();

    public GameMessageService(GameService gameService) {
        this.gameService = gameService;
    }

    private final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static class PayloadType {
        public static final String playerID = "playerId";
        public static final String configuration = "configuration";
        public static final String phaseChange = "phaseChange";
        public static final String admin = "admin";
    }

    public void handleMessage(WebSocketSession session, JsonNode message, String gameId) {
        System.out.println("Received data: " + message);

        if (!message.has("type")) {
            try {
                sendJsonMessage(session, MessageType.ERROR, Map.of("message", "Message missing type field"));
            } catch (Exception e) {
                e.printStackTrace();
            }

            return;
        }

        String messageType = message.get("type").asText(null);

        if (!MessageType.isValid(messageType)) {
            try {
                sendJsonMessage(session, MessageType.ERROR, Map.of("message","Unknown message type: " + messageType));
            } catch (Exception e) {
                e.printStackTrace();
            }

            return;
        }

        message.fields().forEachRemaining(field -> {
            String key = field.getKey();
            JsonNode value = field.getValue();

            if (key.equals("type")) return;

            handleMessageField(session, gameId, key, value);
        });
    }

    // var hade varit en smart plats att sätta in admin korrekt?

    private void handleMessageField(WebSocketSession session, String gameId, String key, JsonNode value) {
        switch (key) {
            case "playerId":
                String playerId = value.asText(null);
                handlePlayerJoin(playerId, session, gameId);
                break;
            case "configuration":
                handleConfiguration(session, gameId, value);
                break;
            case "action":
                handleAction(session, gameId, value);
                break;
            default:
                System.out.println("Unknown field: " + key);
                break;
        }
    }

    private void handleAction(WebSocketSession session, String gameId, JsonNode value) {
        String actionType = value.get("type").asText();

        switch(actionType) {
            case "guess":
                handleActionGuess(session, value.get("guess"));
                break;
            default:
                System.out.println("Unknown action type:" + actionType);
                break;
        }
        
    }

    private void handleConfiguration(WebSocketSession session, String gameId, JsonNode value) {
        // We save the configuration to the game object
        
        // We start procuring songs!
        Game game = gameService.getGame(gameId);

        // Set up the broadcaster callback so the Game can send messages to all players
        game.setMessageBroadcaster((messageType, jsonMessage) -> {
            List<WebSocketSession> sessions = gameSessions.get(gameId);
            if (sessions != null) {
                sessions.removeIf(s -> !s.isOpen());

                for (WebSocketSession clientSession : sessions) {
                    try {
                        // clientSession.sendMessage(new TextMessage(jsonMessage));
                        sendJsonMessage(clientSession, messageType, jsonMessage);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        game.setConfiguration(value);
    }

    // Add function on session for getting the game id directly
    private void handleActionGuess(WebSocketSession session, String gameId, String guess) {
        GameService.getGame()
    }

    private void handlePlayerJoin(String playerId, WebSocketSession session, String gameId) {
        Game game = gameService.getGame(gameId);

        if (game.isPlayer(playerId)) {
            // Re-assign the session to the player
            System.out.println("Player " + playerId + " rejoined game " + gameId);

            // Tell the user they have rejoined successfully.
            try {
                sendJsonMessage(session, MessageType.DATA, Map.of("gameState", game.getPhase()));

                if (Objects.equals(game.getAdmin(), playerId)) {
                    sendJsonMessage(session, MessageType.DATA, Map.of(PayloadType.admin, true));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return;
        } else {
            System.out.println("Player " + playerId + " not found in game " + gameId);
        }

        // This means its a new player that wants to join the game.
        String newPlayerId = createPlayerId();
        session.getAttributes().put("playerId", newPlayerId);
        System.out.println("Adding new player (" + newPlayerId + ") to game " + gameId);

        try {
            game.addPlayer(newPlayerId);

            // This here might be a rotten solution.
            if (Objects.equals(game.getAdmin(), null)) {
                game.setAdmin(newPlayerId);

                sendJsonMessage(session, MessageType.DATA, Map.of(PayloadType.admin, true));
                // Maybe, when we want information from the client we get use a like
                // "wants": "configuration"
                // from the server?
                sendJsonMessage(session, MessageType.DATA, Map.of(PayloadType.configuration, true));
            } else {
                sendJsonMessage(session, MessageType.DATA, Map.of(PayloadType.admin, false));
            }
        } catch (Exception addError) {
            System.out.println("Could not add player to game: " + addError.getMessage());

            try {
                // We do not want to send internal errors directly to the user, only
                // that something failed.
                sendJsonMessage(session, MessageType.SERVER_ERROR, Map.of("message","Could not add player to game"));
            } catch (Exception sendError) {
                System.out.println("Failed to send error message: " + sendError.getMessage());
                try {
                    session.close();
                } catch (Exception exc) {
                    System.out.println("Could not close session after failing to add player.");
                }
            }
            return;
        }
            
        // Tell the user they have been added successfully!
        try {
            sendJsonMessage(session, MessageType.DATA, Map.of("playerId", newPlayerId, "gameState", game.getPhase()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }

    public void handleConnectionClosed(String playerId, String gameId) {
        System.out.println("Connection closed for player: " + playerId + " in room: " + gameId);

        List<WebSocketSession> sessions = gameSessions.get(gameId);
        if (sessions != null) {
            sessions.removeIf(s -> !s.isOpen());
        }
    }

    private String createPlayerId() {
        return UUID.randomUUID().toString();
    }

    public enum MessageType {
        DATA("data"),
        SUCCESS("success"),
        ERROR("error"),
        SERVER_ERROR("server_error");

        private final String value;

        MessageType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static boolean isValid(String type) {
            for (MessageType mt : values()) {
                if (mt.value.equals(type)) {
                    return true;
                }
            }
            return false;
        }
    }

    private void sendJsonMessage(WebSocketSession session, MessageType type, Map<String, Object> data) throws Exception {
        Map<String, Object> msg = new HashMap<>();

        msg.put("type", type.getValue());
        msg.putAll(data);
        msg.put("timestamp", System.currentTimeMillis());

        session.sendMessage(new TextMessage(OBJECT_MAPPER.writeValueAsString(msg)));
    }

    public void handleConnectionEstablished(WebSocketSession session) {
        String path = session.getUri().getPath();
        String gameId =  path.substring(path.lastIndexOf("/") + 1);
        
        if (gameService.getGame(gameId) == null) {
            try {
                sendJsonMessage(session, MessageType.ERROR, Map.of("message","Game with ID " + gameId + " does not exist."));
                session.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        gameSessions.computeIfAbsent(gameId, k -> new CopyOnWriteArrayList<>()).add(session);

        System.out.println("Someone connected to game: " + gameId);

    }
}
