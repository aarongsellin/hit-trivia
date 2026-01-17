package com.hittrivia.app.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import com.hittrivia.app.handlers.GameSocketHandler;
import com.hittrivia.app.handlers.GeneralSocketHandler;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final GeneralSocketHandler generalSocketHandler;
    private final GameSocketHandler gameSocketHandler;

    public WebSocketConfig(GeneralSocketHandler generalSocketHandler, GameSocketHandler gameSocketHandler) {
        this.generalSocketHandler = generalSocketHandler;
        this.gameSocketHandler = gameSocketHandler;
    }

    // Overriding a method which register the socket
    // handlers into a Registry
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry webSocketHandlerRegistry) {
        // For adding a Handler we give the Handler class we
        // created before with End point Also we are managing
        // the CORS policy for the handlers so that other
        // domains can also access the socket
        webSocketHandlerRegistry
            .addHandler(gameSocketHandler, "/ws/game/{room}")
            .setAllowedOrigins("*");

        webSocketHandlerRegistry
            .addHandler(generalSocketHandler, "/ws/general")
            .setAllowedOrigins("*");
    }
}