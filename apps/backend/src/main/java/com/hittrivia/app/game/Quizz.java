package com.hittrivia.app.game;

import java.util.ArrayList;
import java.util.List;

import com.hittrivia.app.fixtures.MockTracks;
import com.hittrivia.app.model.Track;

import lombok.Getter;

@Getter
public class Quizz {
    private List<Track> tracks = new ArrayList<>();
    private int currentTrack = 0;
    // private final HttpClient httpClient = HttpClient.newHttpClient();

    public boolean loadTracks() {
        this.tracks = MockTracks.getMockTracks();

        return true;
    }

    public void nextTrack() {
        this.currentTrack += 1;
    }
}
