package com.hittrivia.app.service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hittrivia.app.model.Track;

/**
 * Fetches songs from Apple Music using the catalog search endpoint:
 * GET /v1/catalog/{storefront}/search?term=...&types=songs&limit=25
 *
 * No genre-id mapping, no charts API — just pure search.
 * Genre filtering is done post-search using the genreNames field from the API response.
 */
@Service
public class AppleMusicCatalogService {

    /** Internal wrapper that carries genre tags through the filtering pipeline. */
    private record TaggedTrack(Track track, List<String> genreNames) {}

    private static final Logger log = LoggerFactory.getLogger(AppleMusicCatalogService.class);
    private static final String BASE_URL = "https://api.music.apple.com/v1/catalog";
    private static final int SEARCH_LIMIT = 25; // Apple Music max per request

    private final AppleMusicTokenService tokenService;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AppleMusicCatalogService(AppleMusicTokenService tokenService) {
        this.tokenService = tokenService;
    }

    /**
     * Search for songs in the Apple Music catalog.
     */
    public List<Track> searchSongs(String term, String storefront, int limit) throws Exception {
        return searchSongsTagged(term, storefront, limit).stream()
                .map(TaggedTrack::track)
                .toList();
    }

    /** Internal search that preserves genre tags for filtering. */
    private List<TaggedTrack> searchSongsTagged(String term, String storefront, int limit) throws Exception {
        int capped = Math.min(limit, SEARCH_LIMIT);
        String encoded = URLEncoder.encode(term, StandardCharsets.UTF_8);
        String url = String.format("%s/%s/search?term=%s&types=songs&limit=%d",
                BASE_URL, storefront, encoded, capped);

        JsonNode root = makeRequest(url);
        JsonNode data = root.path("results").path("songs").path("data");

        List<TaggedTrack> results = new ArrayList<>();
        if (data.isArray()) {
            for (JsonNode song : data) {
                TaggedTrack tt = parseSongNodeTagged(song);
                if (tt != null) results.add(tt);
            }
        }
        log.info("Search '{}' returned {} songs", term, results.size());
        return results;
    }

    /**
     * Get songs for a game round. Searches Apple Music with the host's
     * raw search term, deduplicates, shuffles and returns the requested count.
     */
    public List<Track> getTracksForGame(String searchTerm,
                                        String storefront, int count) throws Exception {

        String term = (searchTerm != null && !searchTerm.isBlank()) ? searchTerm.trim() : "top hits";
        log.info("getTracksForGame — term='{}', storefront={}, count={}", term, storefront, count);

        // Fetch up to 25 results (API max)
        List<TaggedTrack> tagged = searchSongsTagged(term, storefront, SEARCH_LIMIT);

        List<Track> pool = tagged.stream().map(TaggedTrack::track).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

        // Drop tracks without a preview URL and deduplicate by title+artist
        LinkedHashMap<String, Track> seen = new LinkedHashMap<>();
        for (Track t : pool) {
            if (t.previewUrl() != null && !t.previewUrl().isEmpty()) {
                String key = normalize(t.title()) + "|" + normalize(t.artist());
                seen.putIfAbsent(key, t);
            }
        }
        pool = new ArrayList<>(seen.values());

        // Shuffle and take the requested count
        Collections.shuffle(pool);
        List<Track> selected = pool.subList(0, Math.min(pool.size(), count));

        log.info("Returning {} tracks", selected.size());
        return enrichWithMusicVideos(selected, storefront);
    }

    /**
     * For each track, search Apple Music for a matching music video
     * and attach the video preview URL.
     */
    public List<Track> enrichWithMusicVideos(List<Track> tracks, String storefront) {
        return tracks.parallelStream()
                .map(track -> {
                    String videoUrl = findMusicVideoPreview(
                            track.title(), track.artist(), storefront);
                    return new Track(
                            track.title(), track.artist(), track.album(),
                            track.previewUrl(), track.artworkUrl(),
                            videoUrl != null ? videoUrl : "",
                            track.startTimeSeconds(), track.releaseYear());
                })
                .toList();
    }

    private JsonNode makeRequest(String url) throws Exception {
        String token = tokenService.getDeveloperToken();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();

        log.debug("Apple Music API → {}", url);

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            log.error("Apple Music API error: {} — {}", response.statusCode(), response.body());
            throw new RuntimeException("Apple Music API returned " + response.statusCode());
        }
        return objectMapper.readTree(response.body());
    }

    private TaggedTrack parseSongNodeTagged(JsonNode song) {
        JsonNode attrs = song.path("attributes");

        String previewUrl = "";
        JsonNode previews = attrs.path("previews");
        if (previews.isArray() && !previews.isEmpty()) {
            previewUrl = previews.get(0).path("url").asText("");
        }

        String artworkUrl = attrs.path("artwork").path("url").asText("")
                .replace("{w}", "300").replace("{h}", "300");

        int releaseYear = 0;
        String releaseDate = attrs.path("releaseDate").asText("");
        if (releaseDate.length() >= 4) {
            try { releaseYear = Integer.parseInt(releaseDate.substring(0, 4)); }
            catch (NumberFormatException ignored) {}
        }

        // Extract genre names from the API response for post-filtering
        List<String> genreNames = new ArrayList<>();
        JsonNode genres = attrs.path("genreNames");
        if (genres.isArray()) {
            for (JsonNode g : genres) {
                genreNames.add(g.asText());
            }
        }

        Track track = new Track(
                attrs.path("name").asText("Unknown"),
                attrs.path("artistName").asText("Unknown"),
                attrs.path("albumName").asText(""),
                previewUrl,
                artworkUrl,
                "",
                0,
                releaseYear
        );
        return new TaggedTrack(track, genreNames);
    }

    private String findMusicVideoPreview(String title, String artist, String storefront) {
        try {
            String encoded = URLEncoder.encode(title + " " + artist, StandardCharsets.UTF_8);
            String url = String.format("%s/%s/search?term=%s&types=music-videos&limit=5",
                    BASE_URL, storefront, encoded);

            JsonNode data = makeRequest(url)
                    .path("results").path("music-videos").path("data");

            if (data.isArray()) {
                String normTitle  = normalize(title);
                String normArtist = normalize(artist);

                for (JsonNode video : data) {
                    JsonNode a = video.path("attributes");
                    String vTitle  = normalize(a.path("name").asText(""));
                    String vArtist = normalize(a.path("artistName").asText(""));

                    boolean titleOk  = vTitle.contains(normTitle) || normTitle.contains(vTitle);
                    boolean artistOk = vArtist.contains(normArtist) || normArtist.contains(vArtist);

                    if (titleOk && artistOk) {
                        JsonNode previews = a.path("previews");
                        if (previews.isArray() && !previews.isEmpty()) {
                            String direct = previews.get(0).path("url").asText("");
                            if (!direct.isEmpty()) return direct;
                            String hls = previews.get(0).path("hlsUrl").asText("");
                            if (!hls.isEmpty()) return hls;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Music video lookup failed for '{}' by '{}': {}", title, artist, e.getMessage());
        }
        return null;
    }

    /** Lowercase, strip accents and non-alphanumeric chars. */
    private String normalize(String s) {
        if (s == null) return "";
        String d = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return d.toLowerCase().replaceAll("[^a-z0-9 ]", "").replaceAll("\\s+", " ").trim();
    }

}
