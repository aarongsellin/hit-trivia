package com.hittrivia.app.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;

import com.hittrivia.app.dto.GameCountResponse;
import com.hittrivia.app.dto.NewGameResponse;

import com.hittrivia.app.handlers.SocketConnectionHandler;

@RestController // This is an annotation, telling spring boot that this is a REST class, meaning it will handle HTTP requests.
@RequestMapping("/api") // All the endpoints is prefixed with this.
public class HTTPController {

    @GetMapping("/new-game")
    public NewGameResponse newGame() {
        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        
        SocketConnectionHandler.createRoom(id);

        // Here we should also add some sort of rate limiting to prevent
        // malicious users from spamming new rooms.

        System.out.println("New game created with ID: " + id);

        return new NewGameResponse(id);
    }

    @GetMapping("/game-count")
    public GameCountResponse gameCount() {
        Integer count = SocketConnectionHandler.getRoomCount();
        return new GameCountResponse(count);
    }
}