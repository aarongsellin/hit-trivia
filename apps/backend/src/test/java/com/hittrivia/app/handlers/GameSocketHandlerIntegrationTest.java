package com.hittrivia.app.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hittrivia.app.service.AppleMusicCatalogService;
import com.hittrivia.app.service.AppleMusicTokenService;
import com.hittrivia.app.service.GameService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the WebSocket game handler.
 * Boots the full Spring context, connects real WebSocket clients, and
 * verifies player-join, admin election, rejoin, and error flows.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GameSocketHandlerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private GameService gameService;

    @MockitoBean
    private AppleMusicTokenService tokenService;

    @MockitoBean
    private AppleMusicCatalogService catalogService;

    private final ObjectMapper mapper = new ObjectMapper();
    private static final String TEST_ROOM = "test-room";

    @BeforeEach
    void setUp() {
        gameService.createGame(TEST_ROOM);
    }

    @AfterEach
    void tearDown() {
        gameService.deleteGame(TEST_ROOM);
    }

    // ── Player join ────────────────────────────────────────────────

    @Test
    @DisplayName("New player joining receives a playerId and WAITING_CONFIG state")
    void newPlayerJoin_receivesPlayerIdAndGameState() throws Exception {
        TestSocketClient client = connectAndJoin(TEST_ROOM, null, "Alice", 3);

        // The first message should contain admin status (true since first player)
        // The second should contain configuration request
        // Then playerId + gameState
        List<JsonNode> messages = client.getMessages();
        assertThat(messages).isNotEmpty();

        // Find the message containing playerId
        JsonNode playerIdMsg = findMessageContaining(messages, "playerId");
        assertThat(playerIdMsg).isNotNull();
        assertThat(playerIdMsg.get("playerId").asText()).isNotEmpty();
        assertThat(playerIdMsg.has("gameState")).isTrue();

        client.close();
    }

    // ── Admin election ─────────────────────────────────────────────

    @Test
    @DisplayName("First player to join becomes admin")
    void firstPlayer_becomesAdmin() throws Exception {
        TestSocketClient client = connectAndJoin(TEST_ROOM, null, "Alice", 3);

        List<JsonNode> messages = client.getMessages();

        JsonNode adminMsg = findMessageContaining(messages, "admin");
        assertThat(adminMsg).isNotNull();
        assertThat(adminMsg.get("admin").asBoolean()).isTrue();

        client.close();
    }

    @Test
    @DisplayName("Second player to join is NOT admin")
    void secondPlayer_isNotAdmin() throws Exception {
        // First player joins (becomes admin)
        TestSocketClient client1 = connectAndJoin(TEST_ROOM, null, "Alice", 3);
        client1.getMessages(); // consume messages

        // Second player joins
        TestSocketClient client2 = connectAndJoin(TEST_ROOM, null, "Bob", 3);
        List<JsonNode> messages2 = client2.getMessages();

        JsonNode adminMsg = findMessageContaining(messages2, "admin");
        assertThat(adminMsg).isNotNull();
        assertThat(adminMsg.get("admin").asBoolean()).isFalse();

        client1.close();
        client2.close();
    }

    // ── Player rejoin ──────────────────────────────────────────────

    @Test
    @DisplayName("Player can rejoin with existing playerId")
    void playerRejoin_withExistingId() throws Exception {
        // First join to get a playerId
        TestSocketClient client1 = connectAndJoin(TEST_ROOM, null, "Alice", 3);
        List<JsonNode> messages1 = client1.getMessages();

        JsonNode playerIdMsg = findMessageContaining(messages1, "playerId");
        assertThat(playerIdMsg).isNotNull();
        String playerId = playerIdMsg.get("playerId").asText();

        client1.close();

        // Wait a bit for cleanup
        Thread.sleep(200);

        // Rejoin with the same playerId
        TestSocketClient client2 = connectAndJoin(TEST_ROOM, playerId, "Alice", 3);
        List<JsonNode> messages2 = client2.getMessages();

        // On rejoin we get a gameState message (and optionally playerName)
        JsonNode rejoinMsg = findMessageContaining(messages2, "gameState");
        assertThat(rejoinMsg).isNotNull();

        client2.close();
    }

    // ── Non-existent game ──────────────────────────────────────────

    @Test
    @DisplayName("Connecting to a non-existent room sends an error and closes")
    void nonExistentRoom_sendsError() throws Exception {
        TestSocketClient client = connectAndJoin("no-such-room", null, "Alice", 3);
        List<JsonNode> messages = client.getMessages();

        // The handler should send an error message about the game not existing
        JsonNode errorMsg = findMessageContaining(messages, "message");
        assertThat(errorMsg).isNotNull();
        assertThat(errorMsg.get("message").asText()).containsIgnoringCase("does not exist");

        // Wait for server to close
        Thread.sleep(500);
    }

    // ── Invalid message (no type field) ────────────────────────────

    @Test
    @DisplayName("Message without 'type' field results in error response")
    void messageWithoutType_getsError() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        List<JsonNode> received = Collections.synchronizedList(new ArrayList<>());

        StandardWebSocketClient wsClient = new StandardWebSocketClient();
        URI uri = URI.create("ws://localhost:" + port + "/ws/game/" + TEST_ROOM);

        WebSocketSession session = wsClient.execute(new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession s, TextMessage msg) throws Exception {
                received.add(mapper.readTree(msg.getPayload()));
                latch.countDown();
            }
        }, new WebSocketHttpHeaders(), uri).get(5, TimeUnit.SECONDS);

        // Send a message without a "type" field
        session.sendMessage(new TextMessage("{\"foo\": \"bar\"}"));

        latch.await(5, TimeUnit.SECONDS);

        JsonNode errorMsg = findMessageContaining(received, "message");
        assertThat(errorMsg).isNotNull();
        assertThat(errorMsg.get("message").asText()).containsIgnoringCase("type");

        session.close();
    }

    // ── Helpers ────────────────────────────────────────────────────

    /**
     * Connects to the WebSocket endpoint:
     * - Sends a join message with the given playerId (null = new player).
     * - Waits up to {@code expectMessages} messages or 3 seconds.
     */
    private TestSocketClient connectAndJoin(String room, String playerId,
                                             String playerName, int expectMessages) throws Exception {
        TestSocketClient client = new TestSocketClient(mapper, expectMessages);

        StandardWebSocketClient wsClient = new StandardWebSocketClient();
        URI uri = URI.create("ws://localhost:" + port + "/ws/game/" + room);

        WebSocketSession session = wsClient.execute(client, new WebSocketHttpHeaders(), uri)
                .get(5, TimeUnit.SECONDS);
        client.setSession(session);

        // Build join message
        String msg;
        if (playerId != null) {
            msg = mapper.writeValueAsString(java.util.Map.of(
                "type", "data", "playerId", playerId, "playerName", playerName));
        } else {
            // For a new player, playerId is null — send it as JSON null
            msg = String.format("{\"type\":\"data\",\"playerId\":null,\"playerName\":\"%s\"}", playerName);
        }

        session.sendMessage(new TextMessage(msg));

        // Wait for responses
        client.awaitMessages(5, TimeUnit.SECONDS);

        return client;
    }

    /**
     * Finds the first message in the list that contains the given field name.
     */
    private JsonNode findMessageContaining(List<JsonNode> messages, String fieldName) {
        return messages.stream()
                .filter(m -> m.has(fieldName))
                .findFirst()
                .orElse(null);
    }

    /**
     * Simple WebSocket client that collects received messages.
     */
    private static class TestSocketClient extends TextWebSocketHandler {
        private final ObjectMapper mapper;
        private final List<JsonNode> messages = Collections.synchronizedList(new ArrayList<>());
        private final CountDownLatch latch;
        private WebSocketSession session;

        TestSocketClient(ObjectMapper mapper, int expectedMessages) {
            this.mapper = mapper;
            this.latch = new CountDownLatch(expectedMessages);
        }

        void setSession(WebSocketSession session) {
            this.session = session;
        }

        @Override
        protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
            messages.add(mapper.readTree(message.getPayload()));
            latch.countDown();
        }

        List<JsonNode> getMessages() {
            return new ArrayList<>(messages);
        }

        void awaitMessages(long timeout, TimeUnit unit) throws InterruptedException {
            latch.await(timeout, unit);
        }

        void close() throws Exception {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }
}
