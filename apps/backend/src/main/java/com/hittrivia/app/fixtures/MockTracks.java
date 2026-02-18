package com.hittrivia.app.fixtures;

import java.util.List;

import com.hittrivia.app.model.Track;

public class MockTracks {
    public static List<Track> getMockTracks() {
        return List.of(
            new Track("So Far Away", "Dire Straits", "Brothers in Arms", "", "", "", 22),
            new Track("Brothers In Arms", "Dire Straits", "Brothers In Arms", "", "", "", 65),
            new Track("Money For Nothing", "Dire Straits", "Brothers In Arms", "", "", "", 96)
        );
    }
}
