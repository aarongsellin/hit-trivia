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
     *
     * Strategy:
     * 1. When a decade is selected, search for well-known artists of that era
     *    (filtered by genre if provided) and post-filter by releaseDate.
     * 2. When only a genre is selected, use the charts API with genre filter.
     * 3. Fallback to general top charts.
     */
    public List<Track> getTracksForGame(String genre, String decade, String storefront, int count) throws Exception {
        List<Track> pool = new ArrayList<>();
        String genreId = mapGenreToAppleId(genre);

        if (decade != null && !decade.isEmpty()) {
            // Build multiple targeted searches using iconic artists/terms from the era+genre
            List<String> searchTerms = buildDecadeSearchTerms(decade, genre);
            for (String term : searchTerms) {
                try {
                    List<Track> batch = searchSongs(term, storefront, 25);
                    pool.addAll(batch);
                } catch (Exception e) {
                    log.warn("Search failed for term '{}': {}", term, e.getMessage());
                }
                // Stop early if we have plenty of candidates
                if (pool.size() >= count * 6) break;
            }

            // Post-filter: keep only tracks whose release year falls within the decade
            int startYear = parseDecadeStartYear(decade);
            int endYear = startYear + 9;
            pool = pool.stream()
                    .filter(t -> t.releaseYear() >= startYear && t.releaseYear() <= endYear)
                    .toList();
        } else if (genreId != null) {
            pool = getChartSongs(storefront, count * 3, genreId);
        } else {
            pool = getChartSongs(storefront, count * 3, null);
        }

        // Filter out tracks without preview URLs and deduplicate by title+artist
        java.util.LinkedHashMap<String, Track> seen = new java.util.LinkedHashMap<>();
        for (Track t : pool) {
            if (t.previewUrl() != null && !t.previewUrl().isEmpty()) {
                String key = normalizeForMatch(t.title()) + "|" + normalizeForMatch(t.artist());
                seen.putIfAbsent(key, t);
            }
        }
        pool = new ArrayList<>(seen.values());

        // Shuffle and take the requested count
        List<Track> mutableTracks = new ArrayList<>(pool);
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

        int releaseYear = 0;
        String releaseDate = attrs.path("releaseDate").asText("");
        if (releaseDate.length() >= 4) {
            try {
                releaseYear = Integer.parseInt(releaseDate.substring(0, 4));
            } catch (NumberFormatException ignored) {}
        }

        return new Track(
                attrs.path("name").asText("Unknown"),
                attrs.path("artistName").asText("Unknown"),
                attrs.path("albumName").asText(""),
                previewUrl,
                artworkUrl,
                "", // musicVideoUrl — filled in later by enrichWithMusicVideos
                0,
                releaseYear
        );
    }

    /**
     * For each track, search Apple Music for a matching music video
     * and set the musicVideoUrl (video preview) if found.
     */
    public List<Track> enrichWithMusicVideos(List<Track> tracks, String storefront) {
        return tracks.parallelStream()
            .map(track -> {
                String videoUrl = findMusicVideoPreview(
                    track.title(),
                    track.artist(),
                    storefront
                );

                return new Track(
                    track.title(),
                    track.artist(),
                    track.album(),
                    track.previewUrl(),
                    track.artworkUrl(),
                    videoUrl != null ? videoUrl : "",
                    track.startTimeSeconds(),
                    track.releaseYear()
                );
            })
            .toList();
    }

    /**
     * Search for a music video preview URL for a given song.
     * Returns the direct preview URL, or null if not found or no confident match.
     */
    private String findMusicVideoPreview(String title, String artist, String storefront) {
        try {
            String query = URLEncoder.encode(title + " " + artist, StandardCharsets.UTF_8);
            String url = String.format("%s/%s/search?term=%s&types=music-videos&limit=5",
                    BASE_URL, storefront, query);

            JsonNode root = makeRequest(url);
            JsonNode videosData = root.path("results").path("music-videos").path("data");

            if (videosData.isArray()) {
                String normalizedTitle = normalizeForMatch(title);
                String normalizedArtist = normalizeForMatch(artist);

                for (JsonNode video : videosData) {
                    JsonNode attrs = video.path("attributes");
                    String videoTitle = normalizeForMatch(attrs.path("name").asText(""));
                    String videoArtist = normalizeForMatch(attrs.path("artistName").asText(""));

                    // Verify the result actually matches our song
                    boolean titleMatch = videoTitle.contains(normalizedTitle) || normalizedTitle.contains(videoTitle);
                    boolean artistMatch = videoArtist.contains(normalizedArtist) || normalizedArtist.contains(videoArtist);

                    if (titleMatch && artistMatch) {
                        JsonNode previews = attrs.path("previews");
                        if (previews.isArray() && previews.size() > 0) {
                            // Prefer direct url (mp4) over hlsUrl since HLS (.m3u8) doesn't
                            // play in a plain <video> tag on non-Safari browsers
                            String directUrl = previews.get(0).path("url").asText("");
                            if (!directUrl.isEmpty()) return directUrl;
                            String hlsUrl = previews.get(0).path("hlsUrl").asText("");
                            if (!hlsUrl.isEmpty()) return hlsUrl;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to find music video for '{}' by '{}': {}", title, artist, e.getMessage());
        }
        return null;
    }

    /**
     * Normalize a string for loose matching: lowercase, strip accents and non-alphanumeric chars.
     */
    private String normalizeForMatch(String s) {
        if (s == null) return "";
        String decomposed = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return decomposed.toLowerCase().replaceAll("[^a-z0-9 ]", "").replaceAll("\\s+", " ").trim();
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



    /**
     * Parse a decade string like "1960s" or "60s" into the start year (e.g. 1960).
     */
    private int parseDecadeStartYear(String decade) {
        if (decade == null) return 0;
        String cleaned = decade.replaceAll("[^0-9]", "");
        try {
            int num = Integer.parseInt(cleaned);
            // Handle two-digit decades: 60 -> 1960, 00 -> 2000
            if (num < 100) {
                num = num >= 30 ? 1900 + num : 2000 + num;
            }
            return num;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Build multiple search terms targeting well-known artists and songs from a
     * specific decade, optionally scoped to a genre. This produces much better
     * results than a single "greatest hits 1960s" query.
     */
    private List<String> buildDecadeSearchTerms(String decade, String genre) {
        // Map of decade -> list of representative artists that Apple Music indexes well.
        // We search by artist name which reliably returns their catalogue;
        // the releaseDate post-filter then keeps only tracks from the right decade.
        var artistsByDecade = java.util.Map.ofEntries(
            java.util.Map.entry("1960", List.of(
                "The Beatles", "The Rolling Stones", "Bob Dylan", "Aretha Franklin",
                "The Supremes", "Jimi Hendrix", "The Beach Boys", "Elvis Presley",
                "Simon & Garfunkel", "Stevie Wonder", "Marvin Gaye", "The Who"
            )),
            java.util.Map.entry("1970", List.of(
                "Led Zeppelin", "Fleetwood Mac", "David Bowie", "Queen",
                "Eagles", "Pink Floyd", "ABBA", "Bee Gees",
                "Elton John", "Stevie Wonder", "Donna Summer", "The Clash"
            )),
            java.util.Map.entry("1980", List.of(
                "Michael Jackson", "Prince", "Madonna", "U2",
                "Whitney Houston", "Depeche Mode", "Bruce Springsteen", "Bon Jovi",
                "a-ha", "Tears for Fears", "Phil Collins", "Guns N' Roses"
            )),
            java.util.Map.entry("1990", List.of(
                "Nirvana", "Oasis", "TLC", "Backstreet Boys",
                "Tupac", "Notorious B.I.G.", "Spice Girls", "Radiohead",
                "Alanis Morissette", "Red Hot Chili Peppers", "Britney Spears", "R.E.M."
            )),
            java.util.Map.entry("2000", List.of(
                "Eminem", "Beyoncé", "Coldplay", "OutKast",
                "Usher", "Nelly", "Green Day", "Alicia Keys",
                "50 Cent", "Shakira", "Linkin Park", "The Black Eyed Peas"
            )),
            java.util.Map.entry("2010", List.of(
                "Taylor Swift", "Drake", "Ed Sheeran", "Adele",
                "Bruno Mars", "Kendrick Lamar", "The Weeknd", "Rihanna",
                "Billie Eilish", "Ariana Grande", "Post Malone", "Imagine Dragons"
            )),
            java.util.Map.entry("2020", List.of(
                "Olivia Rodrigo", "Dua Lipa", "Bad Bunny", "The Weeknd",
                "Harry Styles", "Doja Cat", "Morgan Wallen", "SZA",
                "Miley Cyrus", "Taylor Swift", "Sabrina Carpenter", "Tyler the Creator"
            ))
        );

        int startYear = parseDecadeStartYear(decade);
        String decadeKey = String.valueOf(startYear);
        List<String> artists = artistsByDecade.getOrDefault(decadeKey, List.of());

        // If we have representative artists, search for each one
        // (limit to 6 to keep API calls reasonable)
        List<String> terms = new ArrayList<>();
        if (!artists.isEmpty()) {
            List<String> shuffled = new ArrayList<>(artists);
            Collections.shuffle(shuffled);
            for (String artist : shuffled.subList(0, Math.min(6, shuffled.size()))) {
                terms.add(artist);
            }
        }

        // Always add a genre+decade fallback term
        String genrePart = (genre != null && !genre.isEmpty()) ? genre + " " : "";
        terms.add(genrePart + "hits " + decade);

        return terms;
    }
}
