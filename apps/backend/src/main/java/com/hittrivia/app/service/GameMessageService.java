package com.hittrivia.app.service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hittrivia.app.game.Game;

import lombok.experimental.SuperBuilder;

@Service
public class GameMessageService {
    private final GameService gameService;

    public GameMessageService(GameService gameService) {
        this.gameService = gameService;
    }

    private final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static class PayloadType {
        public static final String playerID = "playerId";
        public static final String configuration = "configuration";
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
        
        // switch (messageType) {
        //     case "playerId":
        //         if (message.has("data") == true && message.get("data").has("playerId")) {
        //             String playerId = message.get("data").get("playerId").asText(null);
        //             handlePlayerJoin(playerId, session, gameId);
        //         } else {
        //             System.out.println("playerId message missing data.playerId field");

        //             try {
        //                 sendJsonMessage(session, "error", Map.of("message","playerId message missing data.playerId field"));
        //             } catch (Exception e) {
        //                 e.printStackTrace();
        //             }
        //         }
        //         break;
        //     case "configuration":

        //         break;
        //     default:
        //         System.out.println("Unknown message type: " + messageType);
        //         break;
        // }
    }

    private void handleMessageField(WebSocketSession session, String gameId, String key, JsonNode value) {
        switch (key) {
            case "playerId":
                String playerId = value.asText(null);
                handlePlayerJoin(playerId, session, gameId);
                break;
            case "configuration":
                handleConfiguration(session, gameId, value);
                break;
            case "guess":
                handleGuess(session, gameId, value);
                break;
            default:
                System.out.println("Unknown field: " + key);
                break;
        }
    }

    private void handleConfiguration(WebSocketSession session, String gameId, JsonNode value) {

    }

    private void handleGuess(WebSocketSession session, String gameId, JsonNode value) {
        
    }

    private void handlePlayerJoin(String playerId, WebSocketSession session, String gameId) {
        // Get player id from data

        // Assign the player id back to the game.
        Game game = gameService.getGame(gameId);

        if (game.isPlayer(playerId)) {
            // Reassign the session to the player
            System.out.println("Player " + playerId + " rejoined game " + gameId);

            session.getAttributes().put("playerId", playerId);

            // Tell the user they have rejoined successfully.
            try {
                sendJsonMessage(session, MessageType.SUCCESS, Map.of("message", "successfully rejoined"));
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
            sendJsonMessage(session, MessageType.DATA, Map.of("playerId", newPlayerId));
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }

    public void handleConnectionClosed(String playerId, String roomId) {
        System.out.println("Connection closed for player: " + playerId + " in room: " + roomId);

        // gameService.getGame(roomId).freezePlayer(playerId);
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

    private void sendJsonMessage(WebSocketSession session, MessageType type, Map data) throws Exception {
        Map<String, Object> msg = new HashMap<>();

        msg.put("type", type.getValue());
        msg.putAll(data);

        session.sendMessage(new TextMessage(OBJECT_MAPPER.writeValueAsString(msg)));
    }

    public void handleConnectionEstablished(WebSocketSession session, String gameId) {
        // Well here we can really only check if the game exists
        if (gameService.getGame(gameId) == null) {
            System.out.println("Game with ID " + gameId + " does not exist.");

            try {
                sendJsonMessage(session, MessageType.ERROR, Map.of("message","Game with ID " + gameId + " does not exist."));
                session.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        System.out.println("Someone connected to game: " + gameId);

    }
}
