package com.hittrivia.app.config;

import lombok.Getter;

@Getter
public class GameConfig {
    private int numQuestions;
    private int difficulty;
    private String category;

    public GameConfig(int numQuestions, int difficulty, String category) {
        this.numQuestions = numQuestions;
        this.difficulty = difficulty;
        this.category = category;
    }
}
