package com.hittrivia.app.handlers;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.CloseStatus;

import com.fasterxml.jackson.databind.JsonNode;
import com.hittrivia.app.dto.MessageType;
import com.hittrivia.app.game.Game;
import com.hittrivia.app.model.GameWebSocketSession;
import com.hittrivia.app.dto.GamePayloadType;
import com.hittrivia.app.service.AppleMusicCatalogService;
import com.hittrivia.app.service.GameService;
import com.hittrivia.app.validators.JsonValidator;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class GameSocketHandler extends TextWebSocketHandler {
    private final Map<WebSocketSession, GameWebSocketSession> sessionContexts = new ConcurrentHashMap<>();
    private final GameService gameService;
    private final AppleMusicCatalogService catalogService;
    private static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public GameSocketHandler(GameService gameService, AppleMusicCatalogService catalogService) {
        this.gameService = gameService;
        this.catalogService = catalogService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);

        GameWebSocketSession gameSession = new GameWebSocketSession(session);

        sessionContexts.put(session, gameSession);

        String gameId = gameSession.getGameId();
        
        if (gameService.getGame(gameId) == null) {
            try {
                gameSession.sendJsonMessage(MessageType.ERROR, Map.of("message","Game with ID " + gameId + " does not exist."));
                gameSession.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        System.out.println("Someone connected to game: " + gameId);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);


    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        super.handleTextMessage(session, message);

        GameWebSocketSession gameSession = sessionContexts.getOrDefault(session, null);

        if (gameSession == null) {
            throw new Error("Could not find corresponding GameWebSocketSession");
        }

        String payload = message.getPayload();

        if (!JsonValidator.validate(payload)) {
            // sendError(session, "Invalid JSON");
            return;
        }

        JsonNode jsonMessage = OBJECT_MAPPER.readTree(payload);

        if (jsonMessage.fieldNames().hasNext() == false) {
            // sendError(gameSession, "Empty message");
            return;
        }

                System.out.println("Received data: " + message);

        if (!jsonMessage.has("type")) {
            try {
                gameSession.sendJsonMessage(MessageType.ERROR, Map.of("message", "Message missing type field"));
            } catch (Exception e) {
                e.printStackTrace();
            }

            return;
        }

        String messageType = jsonMessage.get("type").asText(null);

        if (!MessageType.isValid(messageType)) {
            try {
                gameSession.sendJsonMessage(MessageType.ERROR, Map.of("message","Unknown message type: " + messageType));
            } catch (Exception e) {
                e.printStackTrace();
            }

            return;
        }

        jsonMessage.fields().forEachRemaining(field -> {
            String key = field.getKey();
            JsonNode value = field.getValue();

            if (key.equals("type")) return;

            handleMessageField(gameSession, key, value);
        });
    }

    private void handleMessageField(GameWebSocketSession gameSession, String key, JsonNode value) {
        switch (key) {
            case "playerId":
                handlePlayerJoin(gameSession, value.asText(null));
                break;
            case "configuration":
                handleConfiguration(gameSession, value);
                break;
            case "action":
                handleAction(gameSession, value);
                break;
            default:
                System.out.println("Unknown field: " + key);
                break;
        }
    }

    private void handleAction(GameWebSocketSession gameSession, JsonNode value) {
        String actionType = value.get("type").asText();

        switch(actionType) {
            case "guess":
                handleActionGuess(gameSession, value.get("guess").toString());
                break;
            default:
                System.out.println("Unknown action type:" + actionType);
                break;
        }
        
    }

    private void handleConfiguration(GameWebSocketSession gameSession, JsonNode value) {
        Game game = gameService.getGame(gameSession.getGameId());

        game.setCatalogService(catalogService);
        game.setConfiguration(value);
    }

    // Add function on session for getting the game id directly
    private void handleActionGuess(GameWebSocketSession session, String guess) {
    }

    private String createPlayerId() {
        return UUID.randomUUID().toString();
    }

    private void setupGameBroadcaster(Game game) {
        game.setMessageBroadcaster((messageType, jsonMessage) -> {
            List<GameWebSocketSession> sessions = sessionContexts.values().stream().filter(s -> {
                try {
                    Boolean isPlayer = s.getPlayerId() != null && game.isPlayer(s.getPlayerId());

                    if (isPlayer == false) return false;

                    if (s.getSession().isOpen() == false) return false;

                    return true;
                } catch (Exception e) {
                    return false;
                }
                
            }).toList();

            if (sessions != null) {
                for (GameWebSocketSession clientSession : sessions) {
                    try {
                        System.out.println("Sending: " + messageType + jsonMessage);
                        clientSession.sendJsonMessage(messageType, jsonMessage);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void handlePlayerJoin(GameWebSocketSession gameSession, String playerId) {
        Game game = gameService.getGame(gameSession.getGameId());

        if (game.isPlayer(gameSession.getPlayerId())) {
            // Re-assign the session to the playe

            // Tell the user they have rejoined successfully.
            try {
                gameSession.sendJsonMessage(MessageType.DATA, Map.of("gameState", game.getPhase()));

                if (Objects.equals(game.getAdmin(), gameSession.getPlayerId())) {
                    gameSession.sendJsonMessage(MessageType.DATA, Map.of(GamePayloadType.admin, true));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return;
        }

        // This means its a new player that wants to join the game.
        String newPlayerId = createPlayerId();
        gameSession.getSession().getAttributes().put("playerId", newPlayerId);

        try {
            game.addPlayer(newPlayerId);

            // This here might be a rotten solution.
            if (Objects.equals(game.getAdmin(), null)) {
                game.setAdmin(newPlayerId);

                setupGameBroadcaster(game);

                gameSession.sendJsonMessage(MessageType.DATA, Map.of(GamePayloadType.admin, true));
                // Maybe, when we want information from the client we get use a like
                // "wants": "configuration"
                // from the server?
                gameSession.sendJsonMessage(MessageType.DATA, Map.of(GamePayloadType.configuration, true));
            } else {
                gameSession.sendJsonMessage(MessageType.DATA, Map.of(GamePayloadType.admin, false));
            }
        } catch (Exception addError) {
            System.out.println("Could not add player to game: " + addError.getMessage());

            try {
                // We do not want to send internal errors directly to the user, only
                // that something failed.
                gameSession.sendJsonMessage(MessageType.SERVER_ERROR, Map.of("message","Could not add player to game"));
            } catch (Exception sendError) {
                System.out.println("Failed to send error message: " + sendError.getMessage());
                try {
                    gameSession.close();
                } catch (Exception exc) {
                    System.out.println("Could not close session after failing to add player.");
                }
            }
            return;
        }
            
        // Tell the user they have been added successfully!
        try {
            gameSession.sendJsonMessage(MessageType.DATA, Map.of("playerId", newPlayerId, "gameState", game.getPhase()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }
}
