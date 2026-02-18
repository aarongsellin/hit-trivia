package com.hittrivia.app.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.UUID;

import com.hittrivia.app.dto.GameCountResponse;
import com.hittrivia.app.dto.AppleMusicTokenResponse;
import com.hittrivia.app.service.AppleMusicTokenService;
import com.hittrivia.app.service.GameService;

@RestController
@RequestMapping("/api")
public class HTTPController {
    private final GameService gameService;
    private final AppleMusicTokenService appleMusicTokenService;

    public HTTPController(GameService gameService, AppleMusicTokenService appleMusicTokenService) {
        this.gameService = gameService;
        this.appleMusicTokenService = appleMusicTokenService;
    }

    @GetMapping("/new-game")
    public Map<String, Object> newGame() {
        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        
        gameService.createGame(id);

        // Here we should also add some sort of rate limiting to prevent malicious users from spamming new rooms.

        System.out.println("New game created with ID: " + id);

        return Map.of("type", "gameId", "data", Map.of("id", id));
    }

    @GetMapping("/game-count")
    public GameCountResponse gameCount() {
        int count = gameService.getGameCount();
        return new GameCountResponse(count);
    }

    @GetMapping("/token/apple-music")
    public AppleMusicTokenResponse appleMusicToken() {
        String token = appleMusicTokenService.getDeveloperToken();
        System.out.println("Token: " + token);
        return new AppleMusicTokenResponse(token);
    }

    /**
     * Test endpoint: verifies the developer token against Apple Music API.
     * Hit GET /api/test-apple-token to see if the token is accepted.
     */
    @GetMapping("/test-apple-token")
    public ResponseEntity<Map<String, Object>> testAppleToken() {
        try {
            String token = appleMusicTokenService.getDeveloperToken();

            // Try a simple API call – fetch a single well-known song
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.music.apple.com/v1/catalog/us/songs/203709340"))
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return ResponseEntity.ok(Map.of(
                        "status", "OK",
                        "message", "Developer token is valid!",
                        "appleMusicResponse", response.statusCode()
                ));
            } else {
                return ResponseEntity.status(response.statusCode()).body(Map.of(
                        "status", "FAILED",
                        "message", "Apple Music rejected the token",
                        "httpStatus", response.statusCode(),
                        "appleResponse", response.body(),
                        "tokenPreview", token.substring(0, Math.min(50, token.length())) + "..."
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "ERROR",
                    "message", "Failed to generate or test token: " + e.getMessage()
            ));
        }
    }
}