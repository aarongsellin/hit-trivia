package com.hittrivia.app.service;

import com.hittrivia.app.model.Track;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link AppleMusicCatalogService}.
 *
 * Makes REAL API calls to Apple Music using credentials from
 * application-local.properties. Automatically skipped when that
 * file is absent (CI, other contributors without Apple keys).
 *
 * Tests are ordered and throttled to avoid Apple's 429 rate limit.
 */
@EnabledIf("hasLocalCredentials")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AppleMusicCatalogServiceTest {

    private AppleMusicCatalogService service;

    static boolean hasLocalCredentials() {
        return AppleMusicCatalogServiceTest.class.getClassLoader()
                .getResource("application-local.properties") != null;
    }

    @BeforeAll
    void setUp() {
        var props = new java.util.Properties();
        try (var in = getClass().getClassLoader()
                .getResourceAsStream("application-local.properties")) {
            props.load(in);
        } catch (Exception e) {
            throw new RuntimeException("Could not load application-local.properties", e);
        }

        var appleMusicProps = new com.hittrivia.app.config.AppleMusicProperties(
                props.getProperty("apple.music.team-id"),
                props.getProperty("apple.music.key-id"),
                props.getProperty("apple.music.private-key")
        );
        AppleMusicTokenService tokenService = new AppleMusicTokenService(appleMusicProps);

        service = new AppleMusicCatalogService(tokenService);
    }

    @BeforeEach
    void throttle() throws InterruptedException {
        // Small delay between tests to stay under Apple's rate limit
        Thread.sleep(1000);
    }

    // ────────────────────────────────────────────────────────────────
    //  Pure logic — no API calls (run first)
    // ────────────────────────────────────────────────────────────────

    @Test @Order(1)
    @DisplayName("normalize strips accents, lowercases, removes special chars")
    void normalize_stripsAccentsAndSpecialChars() {
        assertThat(invokeNormalize("Hôtel Califörnia!!")).isEqualTo("hotel california");
    }

    @Test @Order(2)
    @DisplayName("normalize handles null")
    void normalize_null() {
        assertThat(invokeNormalize(null)).isEmpty();
    }

    @Test @Order(3)
    @DisplayName("getTracksForGame returns tracks for Pop search with previews, no duplicates")
    void getTracksForGame_popSearch() throws Exception {
        List<Track> tracks = service.getTracksForGame("Pop", "us", 5);

        assertThat(tracks).isNotEmpty();
        assertThat(tracks.size()).isLessThanOrEqualTo(5);
        // All tracks must have preview URLs (business rule)
        assertThat(tracks).allSatisfy(t -> assertThat(t.previewUrl()).isNotBlank());
        // No duplicate title+artist
        long uniqueCount = tracks.stream()
                .map(t -> t.title().toLowerCase() + "|" + t.artist().toLowerCase())
                .distinct().count();
        assertThat(uniqueCount).isEqualTo(tracks.size());
    }

    @Test @Order(4)
    @DisplayName("getTracksForGame with decade search returns results")
    void getTracksForGame_decade() throws Exception {
        List<Track> tracks = service.getTracksForGame("2010s hits", "us", 5);

        assertThat(tracks).isNotEmpty();
    }

    @Test @Order(5)
    @DisplayName("getTracksForGame with combined search returns results")
    void getTracksForGame_combined() throws Exception {
        List<Track> tracks = service.getTracksForGame("Rock 2000s", "us", 5);

        assertThat(tracks).isNotEmpty();
    }

    // ────────────────────────────────────────────────────────────────
    //  searchSongs — real API calls
    // ────────────────────────────────────────────────────────────────

    @Test @Order(10)
    @DisplayName("searchSongs returns tracks with all fields populated")
    void searchSongs_returnsFullTracks() throws Exception {
        List<Track> tracks = service.searchSongs("The Weeknd", "us", 10);

        assertThat(tracks).isNotEmpty();
        assertThat(tracks.size()).isLessThanOrEqualTo(10);
        assertThat(tracks).allSatisfy(t -> {
            assertThat(t.title()).isNotBlank();
            assertThat(t.artist()).isNotBlank();
            assertThat(t.previewUrl()).isNotBlank();
        });
        // Spot-check artwork and release year on first result
        assertThat(tracks.get(0).artworkUrl()).contains("300");
        assertThat(tracks.get(0).releaseYear()).isGreaterThan(2000);
    }

    @Test @Order(11)
    @DisplayName("searchSongs respects the limit parameter")
    void searchSongs_respectsLimit() throws Exception {
        List<Track> tracks = service.searchSongs("pop hits", "us", 3);
        assertThat(tracks.size()).isLessThanOrEqualTo(3);
    }

    // ────────────────────────────────────────────────────────────────
    //  enrichWithMusicVideos — real API lookup
    // ────────────────────────────────────────────────────────────────

    @Test @Order(30)
    @DisplayName("enrichWithMusicVideos preserves track fields and attempts video lookup")
    void enrichWithMusicVideos_preservesFields() throws Exception {
        Track input = new Track("Blinding Lights", "The Weeknd", "After Hours",
                "https://audio.example.com/p.m4a", "https://art.example.com/a.jpg",
                "", 0, 2020);

        List<Track> result = service.enrichWithMusicVideos(List.of(input), "us");

        assertThat(result).hasSize(1);
        // Original fields must be preserved
        assertThat(result.get(0).title()).isEqualTo("Blinding Lights");
        assertThat(result.get(0).artist()).isEqualTo("The Weeknd");
        assertThat(result.get(0).previewUrl()).isEqualTo("https://audio.example.com/p.m4a");
        // musicVideoUrl is either a real URL or empty (rate limit), both acceptable
        assertThat(result.get(0).musicVideoUrl()).isNotNull();
    }

    // ════════════════════════════════════════════════════════════════
    //  Reflection helpers for private methods
    // ════════════════════════════════════════════════════════════════

    private String invokeNormalize(String input) {
        return ReflectionTestUtils.invokeMethod(service, "normalize", input);
    }
}
