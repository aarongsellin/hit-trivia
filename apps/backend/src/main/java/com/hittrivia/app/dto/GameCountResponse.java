package com.hittrivia.app.dto;

public class GameCountResponse {
    private Integer count;

    public GameCountResponse(Integer count) {
        this.count = count;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }
}