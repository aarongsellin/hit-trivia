package com.hittrivia.app.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Forwards non-API, non-WebSocket requests to index.html so that
 * Vue Router's history mode works when served from Spring Boot.
 */
@Controller
public class SpaController {

    @GetMapping(value = {"/game", "/game/**"})
    public String forwardToIndex() {
        return "forward:/index.html";
    }
}
