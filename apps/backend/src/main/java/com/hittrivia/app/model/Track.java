package com.hittrivia.app.model;

public record Track(
    String title,
    String artist,
    String album,
    String url,
    int startTimeSeconds
) {}