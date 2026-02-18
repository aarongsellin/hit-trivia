package com.hittrivia.app.model;

public record Track(
    String title,
    String artist,
    String album,
    String previewUrl,
    String artworkUrl,
    String musicVideoUrl,
    int startTimeSeconds
) {}