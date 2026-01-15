package com.hittrivia.app;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController // This is an annotation, telling spring boot that this is a REST class, meaning it will handle HTTP requests.
@RequestMapping("/api") // All the endpoints is prefixed with this.
public class HTTPController {

    @GetMapping("/new-game")
    public Map<String, String> newGame() {
        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        Map<String, String> response = new HashMap<>();
        response.put("id", id);
        
        return response;
    }
}