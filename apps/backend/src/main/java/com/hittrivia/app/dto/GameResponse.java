package com.hittrivia.app.dto;

import lombok.Getter;

@Getter
public class GameResponse<T> {
    private ResponseType responseType;
    private T data;
    private long timestamp;

    public GameResponse(ResponseType responseType, T data) {
        this.responseType = responseType;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    public class ResponseType {

    }
}
