package com.hittrivia.app.game;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.hittrivia.app.model.Track;
import com.hittrivia.app.service.AppleMusicCatalogService;

import lombok.Getter;

@Getter
public class Quizz {
    private List<Track> tracks = new ArrayList<>();
    private int currentTrack = 0;

    private static final int DEFAULT_TRACK_COUNT = 1;
    private static final String DEFAULT_STOREFRONT = "se";

    public boolean loadTracks(JsonNode configuration, AppleMusicCatalogService catalogService) {
        System.out.println("Quizz load tracks with configuration: " + configuration);

        String genre = configuration.has("genre") ? configuration.get("genre").asText(null) : null;
        String decade = configuration.has("decade") ? configuration.get("decade").asText(null) : null;
        String storefront = configuration.has("storefront") ? configuration.get("storefront").asText(DEFAULT_STOREFRONT) : DEFAULT_STOREFRONT;
        int trackCount = configuration.has("trackCount") ? configuration.get("trackCount").asInt(DEFAULT_TRACK_COUNT) : DEFAULT_TRACK_COUNT;

        if (catalogService != null) {
            try {
                this.tracks = catalogService.getTracksForGame(genre, decade, storefront, trackCount);
                System.out.println("this.tracks" + this.tracks);
                if (!this.tracks.isEmpty()) {
                    System.out.println("Loaded " + this.tracks.size() + " tracks from Apple Music catalog");
                    return true;
                }
            } catch (Exception e) {
                System.out.println("Apple Music catalog fetch failed, using mock: " + e.getMessage());
            }
        }

        return false;
    }

    public List<Track> getTracks() {
        return tracks;
    }

    public void nextTrack() {
        this.currentTrack += 1;
    }
}
