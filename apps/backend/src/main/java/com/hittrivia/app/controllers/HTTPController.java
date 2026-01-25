package com.hittrivia.app.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

import com.hittrivia.app.dto.GameCountResponse;

import com.hittrivia.app.service.GameService;

@RestController // This is an annotation, telling spring boot that this is a REST class, meaning it will handle HTTP requests.
@RequestMapping("/api") // All the endpoints is prefixed with this.
public class HTTPController {
    private final GameService gameService;

    public HTTPController(GameService gameService) {
        this.gameService = gameService;
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
}