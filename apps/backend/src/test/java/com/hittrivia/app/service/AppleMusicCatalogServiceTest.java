package com.hittrivia.app.service;

import com.hittrivia.app.model.Track;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AppleMusicCatalogService}.
 *
 * Mocks the {@link HttpClient} at the field level so the entire real pipeline
 * (JSON parsing → genre filtering → decade filtering → dedup → enrichment)
 * runs against realistic Apple Music API response payloads.
 */
@SuppressWarnings("unchecked")
class AppleMusicCatalogServiceTest {

    private AppleMusicTokenService tokenService;
    private AppleMusicCatalogService service;
    private HttpClient mockHttpClient;

    @Test
    @DisplayName("debug: verify application.properties is on the test classpath")
    void debugClasspath() {
        var resource = getClass().getClassLoader().getResource("application.properties");
        System.out.println("Resolved from: " + resource);
        assertThat(resource).isNotNull();
    }

    @BeforeEach
    void setUp() throws Exception {
        tokenService = mock(AppleMusicTokenService.class);
        when(tokenService.getDeveloperToken()).thenReturn("fake-token");

        service = new AppleMusicCatalogService(tokenService);

        // Replace the real HttpClient with a mock so no network calls happen
        mockHttpClient = mock(HttpClient.class);
        ReflectionTestUtils.setField(service, "httpClient", mockHttpClient);
    }

    // ────────────────────────────────────────────────────────────────
    //  searchSongs — end-to-end through real JSON parsing
    // ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("searchSongs parses a realistic API response into Track objects")
    void searchSongs_parsesApiResponse() throws Exception {
        stubHttpResponse(appleMusicSongsJson(
            song("Blinding Lights", "The Weeknd", "After Hours", "2020-03-20",
                 List.of("Pop", "R&B/Soul"), "https://audio.example.com/preview1.m4a"),
            song("Save Your Tears", "The Weeknd", "After Hours", "2020-08-09",
                 List.of("Pop"), "https://audio.example.com/preview2.m4a")
        ));

        List<Track> tracks = service.searchSongs("The Weeknd", "us", 25);

        assertThat(tracks).hasSize(2);
        assertThat(tracks.get(0).title()).isEqualTo("Blinding Lights");
        assertThat(tracks.get(0).artist()).isEqualTo("The Weeknd");
        assertThat(tracks.get(0).album()).isEqualTo("After Hours");
        assertThat(tracks.get(0).releaseYear()).isEqualTo(2020);
        assertThat(tracks.get(0).previewUrl()).isEqualTo("https://audio.example.com/preview1.m4a");
        assertThat(tracks.get(0).artworkUrl()).contains("300");

        assertThat(tracks.get(1).title()).isEqualTo("Save Your Tears");
    }

    @Test
    @DisplayName("searchSongs returns empty list when API data array is empty")
    void searchSongs_emptyResults() throws Exception {
        stubHttpResponse("{\"results\":{\"songs\":{\"data\":[]}}}");

        List<Track> tracks = service.searchSongs("nonexistent", "us", 25);
        assertThat(tracks).isEmpty();
    }

    @Test
    @DisplayName("searchSongs throws on non-200 status code")
    void searchSongs_throwsOnApiError() throws Exception {
        stubHttpResponse(403, "{\"errors\":[{\"title\":\"Forbidden\"}]}");

        assertThatThrownBy(() -> service.searchSongs("test", "us", 25))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("403");
    }

    // ────────────────────────────────────────────────────────────────
    //  getTracksForGame — genre filtering
    // ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getTracksForGame filters out tracks that don't match the genre")
    void getTracksForGame_genreFiltering() throws Exception {
        // Mix of Pop and Rock songs — request Pop, expect only Pop tracks
        stubHttpResponses(
            // Song search response
            appleMusicSongsJson(
                song("Pop Hit", "Pop Artist", "Pop Album", "2015-06-01",
                     List.of("Pop"), "https://audio.example.com/pop.m4a"),
                song("Rock Hit", "Rock Artist", "Rock Album", "2015-03-15",
                     List.of("Rock"), "https://audio.example.com/rock.m4a"),
                song("Pop Banger", "Another Pop", "Pop Album 2", "2016-01-01",
                     List.of("Pop", "Dance"), "https://audio.example.com/pop2.m4a")
            ),
            // Music video search responses (one per matching track)
            emptyMusicVideoJson(),
            emptyMusicVideoJson()
        );

        List<Track> result = service.getTracksForGame("Pop", null, "us", 10);

        assertThat(result).hasSize(2);
        assertThat(result).allSatisfy(t ->
            assertThat(t.title()).containsIgnoringCase("pop")
        );
        // The Rock track should have been filtered out
        assertThat(result).noneMatch(t -> t.title().equals("Rock Hit"));
    }

    // ────────────────────────────────────────────────────────────────
    //  getTracksForGame — decade filtering
    // ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getTracksForGame filters tracks by decade (2010-2019)")
    void getTracksForGame_decadeFiltering() throws Exception {
        stubHttpResponses(
            appleMusicSongsJson(
                song("Song 2015", "Artist", "Album", "2015-06-01",
                     List.of("Pop"), "https://audio.example.com/2015.m4a"),
                song("Song 2005", "Artist", "Album", "2005-03-15",
                     List.of("Pop"), "https://audio.example.com/2005.m4a"),
                song("Song 2018", "Artist B", "Album B", "2018-11-20",
                     List.of("Pop"), "https://audio.example.com/2018.m4a")
            ),
            emptyMusicVideoJson(),
            emptyMusicVideoJson()
        );

        List<Track> result = service.getTracksForGame(null, "2010s", "us", 10);

        assertThat(result).hasSize(2);
        assertThat(result).allSatisfy(t -> {
            assertThat(t.releaseYear()).isBetween(2010, 2019);
        });
        assertThat(result).noneMatch(t -> t.title().equals("Song 2005"));
    }

    // ────────────────────────────────────────────────────────────────
    //  getTracksForGame — combined genre + decade filtering
    // ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getTracksForGame applies both genre and decade filters together")
    void getTracksForGame_genreAndDecade() throws Exception {
        stubHttpResponses(
            appleMusicSongsJson(
                song("Pop 2015", "Artist A", "Album", "2015-01-01",
                     List.of("Pop"), "https://audio.example.com/p1.m4a"),
                song("Rock 2015", "Artist B", "Album", "2015-06-01",
                     List.of("Rock"), "https://audio.example.com/r1.m4a"),
                song("Pop 2005", "Artist C", "Album", "2005-04-01",
                     List.of("Pop"), "https://audio.example.com/p2.m4a"),
                song("Rock 2005", "Artist D", "Album", "2005-07-01",
                     List.of("Rock"), "https://audio.example.com/r2.m4a")
            ),
            emptyMusicVideoJson()
        );

        List<Track> result = service.getTracksForGame("Pop", "2010s", "us", 10);

        // Only "Pop 2015" matches both Pop genre AND 2010s decade
        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("Pop 2015");
    }

    // ────────────────────────────────────────────────────────────────
    //  getTracksForGame — deduplication
    // ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getTracksForGame deduplicates tracks with same title+artist")
    void getTracksForGame_deduplication() throws Exception {
        stubHttpResponses(
            appleMusicSongsJson(
                song("Same Song", "Same Artist", "Album 1", "2015-06-01",
                     List.of("Pop"), "https://audio.example.com/v1.m4a"),
                song("Same Song", "Same Artist", "Album 2 (Deluxe)", "2015-09-01",
                     List.of("Pop"), "https://audio.example.com/v2.m4a"),
                song("Different Song", "Other Artist", "Album 3", "2016-01-01",
                     List.of("Pop"), "https://audio.example.com/v3.m4a")
            ),
            emptyMusicVideoJson(),
            emptyMusicVideoJson()
        );

        List<Track> result = service.getTracksForGame("Pop", null, "us", 10);

        // "Same Song" by "Same Artist" should appear only once
        assertThat(result).hasSize(2);
        long sameSongCount = result.stream()
            .filter(t -> t.title().equals("Same Song"))
            .count();
        assertThat(sameSongCount).isEqualTo(1);
    }

    // ────────────────────────────────────────────────────────────────
    //  getTracksForGame — drops tracks without preview URL
    // ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getTracksForGame drops tracks that have no preview URL")
    void getTracksForGame_dropsNoPreview() throws Exception {
        stubHttpResponses(
            appleMusicSongsJson(
                song("Has Preview", "Artist", "Album", "2015-01-01",
                     List.of("Pop"), "https://audio.example.com/yes.m4a"),
                song("No Preview", "Artist B", "Album B", "2015-06-01",
                     List.of("Pop"), "")  // empty preview
            ),
            emptyMusicVideoJson()
        );

        List<Track> result = service.getTracksForGame("Pop", null, "us", 10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("Has Preview");
    }

    // ────────────────────────────────────────────────────────────────
    //  getTracksForGame — respects count limit
    // ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getTracksForGame returns at most 'count' tracks")
    void getTracksForGame_respectsCountLimit() throws Exception {
        stubHttpResponses(
            appleMusicSongsJson(
                song("Song A", "Artist A", "Album", "2015-01-01",
                     List.of("Pop"), "https://audio.example.com/a.m4a"),
                song("Song B", "Artist B", "Album", "2015-03-01",
                     List.of("Pop"), "https://audio.example.com/b.m4a"),
                song("Song C", "Artist C", "Album", "2015-06-01",
                     List.of("Pop"), "https://audio.example.com/c.m4a")
            ),
            emptyMusicVideoJson(),
            emptyMusicVideoJson()
        );

        List<Track> result = service.getTracksForGame("Pop", null, "us", 2);

        assertThat(result).hasSize(2);
    }

    // ────────────────────────────────────────────────────────────────
    //  enrichWithMusicVideos — attaches video URL via real lookup
    // ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("enrichWithMusicVideos attaches music video URL from API response")
    void enrichWithMusicVideos_attachesVideoUrl() throws Exception {
        stubHttpResponse(musicVideoJson(
            "Blinding Lights", "The Weeknd", "https://video.example.com/blink.m4v"
        ));

        Track input = new Track("Blinding Lights", "The Weeknd", "After Hours",
            "https://audio.example.com/p.m4a", "https://art.example.com/a.jpg",
            "", 0, 2020);

        List<Track> result = service.enrichWithMusicVideos(List.of(input), "us");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).musicVideoUrl()).isEqualTo("https://video.example.com/blink.m4v");
        // Original fields preserved
        assertThat(result.get(0).title()).isEqualTo("Blinding Lights");
        assertThat(result.get(0).previewUrl()).isEqualTo("https://audio.example.com/p.m4a");
    }

    @Test
    @DisplayName("enrichWithMusicVideos returns empty string when no video matches")
    void enrichWithMusicVideos_noMatch() throws Exception {
        stubHttpResponse(emptyMusicVideoJson());

        Track input = new Track("Unknown", "Nobody", "Album",
            "https://audio.example.com/p.m4a", "", "", 0, 2020);

        List<Track> result = service.enrichWithMusicVideos(List.of(input), "us");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).musicVideoUrl()).isEmpty();
    }

    @Test
    @DisplayName("enrichWithMusicVideos doesn't match video with wrong artist")
    void enrichWithMusicVideos_wrongArtistNoMatch() throws Exception {
        // Response has a video with matching title but different artist
        stubHttpResponse(musicVideoJson(
            "Same Title", "Completely Different Artist", "https://video.example.com/v.m4v"
        ));

        Track input = new Track("Same Title", "Original Artist", "Album",
            "https://audio.example.com/p.m4a", "", "", 0, 2020);

        List<Track> result = service.enrichWithMusicVideos(List.of(input), "us");

        assertThat(result).hasSize(1);
        // Should NOT match because artist doesn't fuzzy-match
        assertThat(result.get(0).musicVideoUrl()).isEmpty();
    }

    // ────────────────────────────────────────────────────────────────
    //  Private helpers (still worth covering directly)
    // ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("normalize strips accents, lowercases, removes special chars")
    void normalize_stripsAccentsAndSpecialChars() {
        String result = invokeNormalize("Hôtel Califörnia!!");
        assertThat(result).isEqualTo("hotel california");
    }

    @Test
    @DisplayName("normalize handles null")
    void normalize_null() {
        assertThat(invokeNormalize(null)).isEmpty();
    }

    @Test
    @DisplayName("normalize collapses whitespace")
    void normalize_collapsesWhitespace() {
        assertThat(invokeNormalize("  Hello    World  ")).isEqualTo("hello world");
    }

    @Test
    @DisplayName("parseDecadeStartYear: '2010s' → 2010, '80s' → 1980, null → 0")
    void parseDecadeStartYear_variants() {
        assertThat(invokeParseDecade("2010s")).isEqualTo(2010);
        assertThat(invokeParseDecade("80s")).isEqualTo(1980);
        assertThat(invokeParseDecade("1990s")).isEqualTo(1990);
        assertThat(invokeParseDecade("20s")).isEqualTo(2020);
        assertThat(invokeParseDecade(null)).isEqualTo(0);
    }

    @Test
    @DisplayName("buildSearchTerm combines genre + decade, defaults to 'top hits'")
    void buildSearchTerm_variants() {
        assertThat(invokeBuildSearchTerm("pop", "2010s")).isEqualTo("pop 2010s");
        assertThat(invokeBuildSearchTerm("rock", null)).isEqualTo("rock");
        assertThat(invokeBuildSearchTerm(null, "1990s")).isEqualTo("1990s");
        assertThat(invokeBuildSearchTerm(null, null)).isEqualTo("top hits");
    }

    @Test
    @DisplayName("parseSongNodeTagged extracts all fields from realistic JSON")
    void parseSongNodeTagged_fullParsing() throws Exception {
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        var songNode = mapper.readTree(song(
            "Blinding Lights", "The Weeknd", "After Hours", "2020-03-20",
            List.of("Pop", "R&B/Soul"), "https://audio.example.com/preview.m4a"
        ));

        Object result = ReflectionTestUtils.invokeMethod(service, "parseSongNodeTagged", songNode);
        assertThat(result).isNotNull();

        Track t = (Track) result.getClass().getMethod("track").invoke(result);
        assertThat(t.title()).isEqualTo("Blinding Lights");
        assertThat(t.artist()).isEqualTo("The Weeknd");
        assertThat(t.album()).isEqualTo("After Hours");
        assertThat(t.releaseYear()).isEqualTo(2020);
        assertThat(t.previewUrl()).isEqualTo("https://audio.example.com/preview.m4a");
        assertThat(t.artworkUrl()).contains("300");

        List<String> genres = (List<String>) result.getClass().getMethod("genreNames").invoke(result);
        assertThat(genres).containsExactly("Pop", "R&B/Soul");
    }

    @Test
    @DisplayName("parseSongNodeTagged handles missing optional fields")
    void parseSongNodeTagged_missingFields() throws Exception {
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        var songNode = mapper.readTree("{\"attributes\":{\"name\":\"Minimal\"}}");

        Object result = ReflectionTestUtils.invokeMethod(service, "parseSongNodeTagged", songNode);
        Track t = (Track) result.getClass().getMethod("track").invoke(result);

        assertThat(t.title()).isEqualTo("Minimal");
        assertThat(t.artist()).isEqualTo("Unknown");
        assertThat(t.album()).isEmpty();
        assertThat(t.releaseYear()).isEqualTo(0);
        assertThat(t.previewUrl()).isEmpty();
    }

    // ════════════════════════════════════════════════════════════════
    //  Test infrastructure — fake Apple Music API responses
    // ════════════════════════════════════════════════════════════════

    /** Stubs HttpClient to return a single 200 response. */
    private void stubHttpResponse(String json) throws Exception {
        stubHttpResponse(200, json);
    }

    /** Stubs HttpClient to return a response with the given status code. */
    private void stubHttpResponse(int statusCode, String json) throws Exception {
        HttpResponse<String> resp = mock(HttpResponse.class);
        when(resp.statusCode()).thenReturn(statusCode);
        when(resp.body()).thenReturn(json);
        // Use doReturn to avoid generic type inference issues with HttpResponse<Object> vs <String>
        doReturn(resp).when(mockHttpClient).send(any(HttpRequest.class), any());
    }

    /**
     * Stubs HttpClient to return different responses for consecutive calls.
     * First call gets responses[0], second gets responses[1], etc.
     */
    private void stubHttpResponses(String... responses) throws Exception {
        HttpResponse<String>[] mocks = new HttpResponse[responses.length];
        for (int i = 0; i < responses.length; i++) {
            mocks[i] = mock(HttpResponse.class);
            when(mocks[i].statusCode()).thenReturn(200);
            when(mocks[i].body()).thenReturn(responses[i]);
        }

        AtomicInteger callIndex = new AtomicInteger(0);
        doAnswer(inv -> {
            int idx = Math.min(callIndex.getAndIncrement(), mocks.length - 1);
            return mocks[idx];
        }).when(mockHttpClient).send(any(HttpRequest.class), any());
    }

    /** Builds a realistic Apple Music songs search JSON response. */
    private String appleMusicSongsJson(String... songJsons) {
        return """
            {"results":{"songs":{"data":[%s]}}}
            """.formatted(String.join(",", songJsons));
    }

    /** Builds a single Apple Music song JSON node. */
    private String song(String name, String artist, String album, String releaseDate,
                        List<String> genres, String previewUrl) {
        String genreArray = genres.stream()
            .map(g -> "\"" + g + "\"")
            .reduce((a, b) -> a + "," + b)
            .orElse("");

        String previewsArray = (previewUrl != null && !previewUrl.isEmpty())
            ? "[{\"url\":\"" + previewUrl + "\"}]"
            : "[]";

        return """
            {
              "attributes": {
                "name": "%s",
                "artistName": "%s",
                "albumName": "%s",
                "releaseDate": "%s",
                "genreNames": [%s],
                "previews": %s,
                "artwork": {"url": "https://art.example.com/{w}x{h}img.jpg"}
              }
            }
            """.formatted(name, artist, album, releaseDate, genreArray, previewsArray);
    }

    /** Builds a music video search JSON response with one matching video. */
    private String musicVideoJson(String name, String artist, String videoUrl) {
        return """
            {"results":{"music-videos":{"data":[{
              "attributes": {
                "name": "%s",
                "artistName": "%s",
                "previews": [{"url": "%s"}]
              }
            }]}}}
            """.formatted(name, artist, videoUrl);
    }

    /** Returns an empty music video search response. */
    private String emptyMusicVideoJson() {
        return "{\"results\":{\"music-videos\":{\"data\":[]}}}";
    }

    private String invokeNormalize(String input) {
        return ReflectionTestUtils.invokeMethod(service, "normalize", input);
    }

    private int invokeParseDecade(String decade) {
        return ReflectionTestUtils.invokeMethod(service, "parseDecadeStartYear", decade);
    }

    private String invokeBuildSearchTerm(String genre, String decade) {
        return ReflectionTestUtils.invokeMethod(service, "buildSearchTerm", genre, decade);
    }
}
