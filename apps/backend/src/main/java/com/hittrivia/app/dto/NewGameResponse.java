package com.hittrivia.app.dto;

public class NewGameResponse {
    private String id;

    public NewGameResponse(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}