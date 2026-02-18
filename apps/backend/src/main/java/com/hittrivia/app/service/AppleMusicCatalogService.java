package com.hittrivia.app.service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hittrivia.app.model.Track;

@Service
public class AppleMusicCatalogService {

    private static final Logger log = LoggerFactory.getLogger(AppleMusicCatalogService.class);
    private static final String BASE_URL = "https://api.music.apple.com/v1/catalog";

    private final AppleMusicTokenService tokenService;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AppleMusicCatalogService(AppleMusicTokenService tokenService) {
        this.tokenService = tokenService;
    }

    /**
     * Search for songs in the Apple Music catalog.
     * Only requires the developer token — no user auth needed.
     */
    public List<Track> searchSongs(String query, String storefront, int limit) throws Exception {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = String.format("%s/%s/search?term=%s&types=songs&limit=%d",
                BASE_URL, storefront, encodedQuery, limit);

        JsonNode root = makeRequest(url);

        List<Track> tracks = new ArrayList<>();
        JsonNode songsData = root.path("results").path("songs").path("data");

        for (JsonNode song : songsData) {
            Track track = parseSongNode(song);
            if (track != null) tracks.add(track);
        }

        return tracks;
    }

    /**
     * Get top chart songs, optionally filtered by genre.
     * Genre IDs: Pop=14, Rock=21, Hip-Hop/Rap=18, Electronic=7, R&B/Soul=15,
     * Alternative=20, Country=6, Latin=12, Classical=5, Jazz=11
     */
    public List<Track> getChartSongs(String storefront, int limit, String genreId) throws Exception {
        String url;
        if (genreId != null && !genreId.isEmpty()) {
            url = String.format("%s/%s/charts?types=songs&limit=%d&genre=%s",
                    BASE_URL, storefront, limit, genreId);
        } else {
            url = String.format("%s/%s/charts?types=songs&limit=%d",
                    BASE_URL, storefront, limit);
        }

        JsonNode root = makeRequest(url);

        List<Track> tracks = new ArrayList<>();
        JsonNode songsData = root.path("results").path("songs");

        // Charts response wraps in an array
        if (songsData.isArray() && songsData.size() > 0) {
            JsonNode chartData = songsData.get(0).path("data");
            for (JsonNode song : chartData) {
                Track track = parseSongNode(song);
                if (track != null) tracks.add(track);
            }
        }

        return tracks;
    }

    /**
     * Get songs based on game configuration (genre, decade, etc.)
     * Returns a shuffled list of tracks with preview URLs.
     */
    public List<Track> getTracksForGame(String genre, String decade, String storefront, int count) throws Exception {
        List<Track> tracks;

        String genreId = mapGenreToAppleId(genre);

        if (decade != null && !decade.isEmpty()) {
            // Search by decade + genre
            String searchTerm = buildDecadeSearchTerm(decade, genre);
            tracks = searchSongs(searchTerm, storefront, count * 2);
        } else if (genreId != null) {
            // Use charts for the genre
            tracks = getChartSongs(storefront, count * 2, genreId);
        } else {
            // General top songs
            tracks = getChartSongs(storefront, count * 2, null);
        }

        // Filter out tracks without preview URLs
        tracks = tracks.stream()
                .filter(t -> t.previewUrl() != null && !t.previewUrl().isEmpty())
                .toList();

        // Shuffle and take the requested count
        List<Track> mutableTracks = new ArrayList<>(tracks);
        Collections.shuffle(mutableTracks);

        List<Track> selected = mutableTracks.subList(0, Math.min(mutableTracks.size(), count));

        // Enrich with music video preview URLs for the reveal phase
        return enrichWithMusicVideos(selected, storefront);
    }

    private JsonNode makeRequest(String url) throws Exception {
        String token = tokenService.getDeveloperToken();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();

        log.debug("Apple Music API request: {}", url);

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            log.error("Apple Music API error: {} - {}", response.statusCode(), response.body());
            throw new RuntimeException("Apple Music API returned " + response.statusCode());
        }

        return objectMapper.readTree(response.body());
    }

    private Track parseSongNode(JsonNode song) {
        JsonNode attrs = song.path("attributes");
        String previewUrl = "";

        JsonNode previews = attrs.path("previews");
        if (previews.isArray() && previews.size() > 0) {
            previewUrl = previews.get(0).path("url").asText("");
        }

        String artworkUrl = attrs.path("artwork").path("url").asText("")
                .replace("{w}", "300").replace("{h}", "300");

        return new Track(
                attrs.path("name").asText("Unknown"),
                attrs.path("artistName").asText("Unknown"),
                attrs.path("albumName").asText(""),
                previewUrl,
                artworkUrl,
                "", // musicVideoUrl — filled in later by enrichWithMusicVideos
                0
        );
    }

    /**
     * For each track, search Apple Music for a matching music video
     * and set the musicVideoUrl (video preview) if found.
     */
    public List<Track> enrichWithMusicVideos(List<Track> tracks, String storefront) {
        List<Track> enriched = new ArrayList<>();
        for (Track track : tracks) {
            String videoUrl = findMusicVideoPreview(track.title(), track.artist(), storefront);
            enriched.add(new Track(
                    track.title(),
                    track.artist(),
                    track.album(),
                    track.previewUrl(),
                    track.artworkUrl(),
                    videoUrl != null ? videoUrl : "",
                    track.startTimeSeconds()
            ));
        }
        return enriched;
    }

    /**
     * Search for a music video preview URL for a given song.
     * Returns the HLS or direct preview URL, or null if not found.
     */
    private String findMusicVideoPreview(String title, String artist, String storefront) {
        try {
            String query = URLEncoder.encode(title + " " + artist, StandardCharsets.UTF_8);
            String url = String.format("%s/%s/search?term=%s&types=music-videos&limit=1",
                    BASE_URL, storefront, query);

            JsonNode root = makeRequest(url);
            JsonNode videosData = root.path("results").path("music-videos").path("data");

            if (videosData.isArray() && videosData.size() > 0) {
                JsonNode video = videosData.get(0);
                JsonNode attrs = video.path("attributes");
                JsonNode previews = attrs.path("previews");

                if (previews.isArray() && previews.size() > 0) {
                    // Prefer hlsUrl for video playback, fall back to url
                    String hlsUrl = previews.get(0).path("hlsUrl").asText("");
                    if (!hlsUrl.isEmpty()) return hlsUrl;
                    return previews.get(0).path("url").asText("");
                }
            }
        } catch (Exception e) {
            log.warn("Failed to find music video for '{}' by '{}': {}", title, artist, e.getMessage());
        }
        return null;
    }

    /**
     * Map genre names from game config to Apple Music genre IDs.
     */
    private String mapGenreToAppleId(String genre) {
        if (genre == null) return null;
        return switch (genre.toLowerCase()) {
            case "pop" -> "14";
            case "rock" -> "21";
            case "hip-hop", "hiphop", "rap" -> "18";
            case "electronic", "dance" -> "7";
            case "r&b", "rnb", "soul" -> "15";
            case "alternative", "indie" -> "20";
            case "country" -> "6";
            case "latin" -> "12";
            case "classical" -> "5";
            case "jazz" -> "11";
            case "metal" -> "1153";
            case "reggae" -> "24";
            default -> null;
        };
    }

    private String buildDecadeSearchTerm(String decade, String genre) {
        // Build a search query that targets a specific decade
        // Apple Music doesn't have a direct decade filter, so we use popular artists/songs from that era
        String genrePart = (genre != null && !genre.isEmpty()) ? " " + genre : "";
        return switch (decade) {
            case "60s", "1960s" -> "greatest hits 1960s" + genrePart;
            case "70s", "1970s" -> "greatest hits 1970s" + genrePart;
            case "80s", "1980s" -> "greatest hits 1980s" + genrePart;
            case "90s", "1990s" -> "greatest hits 1990s" + genrePart;
            case "2000s" -> "greatest hits 2000s" + genrePart;
            case "2010s" -> "hits 2010s" + genrePart;
            case "2020s" -> "hits 2020s" + genrePart;
            default -> "greatest hits" + genrePart;
        };
    }
}
