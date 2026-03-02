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
        sessionContexts.remove(session);
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        super.handleTextMessage(session, message);

        GameWebSocketSession gameSession = sessionContexts.getOrDefault(session, null);

        if (gameSession == null) {
            // Session was already removed (e.g. game didn't exist and connection was closed)
            System.out.println("Ignoring message for unknown session (already closed)");
            return;
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

        // Process playerName BEFORE playerId so the name is available during handlePlayerJoin
        if (jsonMessage.has("playerName")) {
            gameSession.getSession().getAttributes().put("playerName",
                jsonMessage.get("playerName").asText("Player"));
        }

        jsonMessage.fields().forEachRemaining(field -> {
            String key = field.getKey();
            JsonNode value = field.getValue();

            if (key.equals("type") || key.equals("playerName")) return;

            handleMessageField(gameSession, key, value);
        });
    }

    private void handleMessageField(GameWebSocketSession gameSession, String key, JsonNode value) {
        switch (key) {
            case "playerId":
                System.out.println("playerId?" + value.asText());
                handlePlayerJoin(gameSession, value.asText(null));
                break;
            case "configuration":
                handleConfiguration(gameSession, value);
                break;
            case "action":
                handlePlayerAction(gameSession, value);
                break;
            default:
                System.out.println("Unknown field: " + key);
                break;
        }
    }

    private void handlePlayerAction(GameWebSocketSession gameSession, JsonNode value) {
        String actionType = value.get("type").asText();

        switch(actionType) {
            case "guess":
                handleActionGuess(gameSession, value.get("guess").asText());
                break;
            case "requestTracks":
                handleRequestTracks(gameSession);
                break;
            default:
                System.out.println("Unknown action type:" + actionType);
                break;
        }
        
    }

    private void handleConfiguration(GameWebSocketSession gameSession, JsonNode value) {
        Game game = gameService.getGame(gameSession.getGameId());

        if (Objects.equals(game.getAdmin(), gameSession.getPlayerId()) == false) {
            try {
                gameSession.sendJsonMessage(MessageType.ERROR, Map.of("message", "Un-authorized request, you are not the admin."));
            } catch(Exception e) {
                e.printStackTrace();
            }
            return;
        }

        game.setCatalogService(catalogService);
        game.setConfiguration(value);
    }

    // Add function on session for getting the game id directly
    private void handleActionGuess(GameWebSocketSession session, String guess) {
        Game game = gameService.getGame(session.getGameId());
        if (game == null) return;

        Game.GuessResult result = game.checkGuess(session.getPlayerId(), guess);

        if (result != null) {
            try {
                session.sendJsonMessage(MessageType.DATA, Map.of(
                    "guessResult", Map.of(
                        "round", result.round(),
                        "titleScore", result.titleScore(),
                        "artistScore", result.artistScore(),
                        "albumScore", result.albumScore(),
                        "total", result.total()
                    )
                ));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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
                        // System.out.println("Sending: " + messageType + jsonMessage);
                        clientSession.sendJsonMessage(messageType, jsonMessage);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void handlePlayerJoin(GameWebSocketSession session, String playerId) {
        Game game = gameService.getGame(session.getGameId());

        System.out.println("Is old player?: " + game.getPlayers().contains(playerId));

        if (game.isPlayer(playerId)) {
            // Re-assign the session to the player
            session.getSession().getAttributes().put("playerId", playerId);

            // Update player name if provided
            // String rejoinName = (String) session.getSession().getAttributes().get("playerName");
            
            String playerName = game.getPlayerName(playerId);

            if (playerName == null) {
                // Then theres something wrong I believe
                new Error("Yeet");
            }

            // Tell the user they have rejoined successfully.
            try {
                session.sendJsonMessage(MessageType.DATA, Map.of("gameState", game.getPhase(), "playerName", playerName));
                sendGameStateIfStarted(session, game);

                if (Objects.equals(game.getAdmin(), playerId)) {
                    session.sendJsonMessage(MessageType.DATA, Map.of(GamePayloadType.admin, true));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return;
        }

        System.out.println("New player wants to join game");

        // Block new players only if the game is finished
        if (!game.isJoinable()) {
            try {
                session.sendJsonMessage(MessageType.ERROR, Map.of(
                    "message", "This game has already finished.",
                    "code", "GAME_FINISHED"
                ));
                session.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            return;
        }

        // This means its a new player that wants to join the game.
        String newPlayerId = UUID.randomUUID().toString();
        session.getSession().getAttributes().put("playerId", newPlayerId);

        // Store the player's display name
        String playerName = (String) session.getSession().getAttributes().get("playerName");
        if (playerName != null) {
            game.setPlayerName(newPlayerId, playerName);
        }

        try {
            game.addPlayer(newPlayerId);

            // This here might be a rotten solution.
            if (Objects.equals(game.getAdmin(), null)) {
                game.setAdmin(newPlayerId);

                setupGameBroadcaster(game);

                session.sendJsonMessage(MessageType.DATA, Map.of(GamePayloadType.admin, true));
                // Maybe, when we want information from the client we get use a like
                // "wants": "configuration"
                // from the server?
                session.sendJsonMessage(MessageType.DATA, Map.of(GamePayloadType.configuration, true));
            } else {
                session.sendJsonMessage(MessageType.DATA, Map.of(GamePayloadType.admin, false));
            }
        } catch (Exception addError) {
            System.out.println("Could not add player to game: " + addError.getMessage());

            try {
                // We do not want to send internal errors directly to the user, only
                // that something failed.
                session.sendJsonMessage(MessageType.SERVER_ERROR, Map.of("message","Could not add player to game"));
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
            session.sendJsonMessage(MessageType.DATA, Map.of("playerId", newPlayerId, "gameState", game.getPhase()));
            sendGameStateIfStarted(session, game);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }

    private void handleRequestTracks(GameWebSocketSession session) {
        Game game = gameService.getGame(session.getGameId());
        if (game == null) return;

        if (game.getPhase() == Game.Phase.WAITING_CONFIG) return;
        if (game.getQuizz() == null || game.getQuizz().getTracks() == null) return;

        try {
            session.sendJsonMessage(MessageType.DATA, Map.of("tracks", game.getQuizz().getTracks()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends tracks, currentRound, and active phase timer to a player.
     * Only sends when the game is past the WAITING_CONFIG phase.
     */
    private void sendGameStateIfStarted(GameWebSocketSession session, Game game) throws Exception {
        if (game.getPhase() == Game.Phase.WAITING_CONFIG) {
            return;
        }

        session.sendJsonMessage(MessageType.DATA, Map.of("tracks", game.getQuizz().getTracks()));
        session.sendJsonMessage(MessageType.DATA, Map.of("currentRound", game.getCurrentRound()));

        // Send the player's guess result for the current round (if any)
        String playerId = session.getPlayerId();
        if (playerId != null) {
            Game.GuessResult result = game.getGuessResultForRound(playerId, game.getCurrentRound());
            if (result != null) {
                session.sendJsonMessage(MessageType.DATA, Map.of(
                    "guessResult", Map.of(
                        "round", result.round(),
                        "titleScore", result.titleScore(),
                        "artistScore", result.artistScore(),
                        "albumScore", result.albumScore(),
                        "total", result.total()
                    )
                ));
            }
        }

        if (game.getPhaseEndTimestamp() > System.currentTimeMillis() && game.getNextPhase() != null) {
            session.sendJsonMessage(MessageType.DATA, Map.of(
                "phaseChange", Map.of(
                    "newPhase", game.getNextPhase().toString(),
                    "startTimestamp", game.getPhaseStartTimestamp(),
                    "endTimestamp", game.getPhaseEndTimestamp()
                )
            ));
        }
    }
}
