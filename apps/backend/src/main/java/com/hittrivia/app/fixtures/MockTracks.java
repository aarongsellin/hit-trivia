package com.hittrivia.app.fixtures;

import java.util.List;

import com.hittrivia.app.model.Track;

public class MockTracks {
    public static List<Track> getMockTracks() {
        return List.of(
            new Track("So Far Away", "Dire Straits", "Brothers in Arms", "https://www.youtube.com/watch?v=ZtdRdgHkUsU", 22),
            new Track("Brothers In Arms", "Dire Straits", "Brothers In Arms", "https://www.youtube.com/watch?v=9ykZc5E6UEE", 65),
            new Track("Money For Nothing", "Dire Straits", "Brothers In Arms", "https://www.youtube.com/watch?v=6K6ODTZ7c10", 96)
        );
    }
}
