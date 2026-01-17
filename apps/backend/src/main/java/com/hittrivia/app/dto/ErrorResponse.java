package com.hittrivia.app.dto;

import lombok.Getter;

@Getter
public class ErrorResponse {
    private String type = "error";
    private String message;
    private long timestamp;

    public ErrorResponse(String message) {
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }
}
