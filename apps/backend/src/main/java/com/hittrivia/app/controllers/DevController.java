package com.hittrivia.app.controllers;

import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hittrivia.app.model.Track;
import com.hittrivia.app.service.AppleMusicCatalogService;

/**
 * Dev-only endpoints for fine-tuning Apple Music search logic.
 * This controller is only loaded when the "local" Spring profile is active.
 */
@RestController
@RequestMapping("/api/dev")
@Profile("local")
public class DevController {

    private final AppleMusicCatalogService musicCatalogService;

    public DevController(AppleMusicCatalogService musicCatalogService) {
        this.musicCatalogService = musicCatalogService;
    }

    @GetMapping("/query-tracks")
    public ResponseEntity<?> queryTracks(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "se") String storefront,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<Track> tracks = musicCatalogService.searchSongs(query, storefront, limit);
            return ResponseEntity.ok(tracks);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/query-game-tracks")
    public ResponseEntity<?> queryGameTracks(
            @RequestParam(required = false) String searchTerm,
            @RequestParam(defaultValue = "se") String storefront,
            @RequestParam(defaultValue = "5") int count) {
        try {
            List<Track> tracks = musicCatalogService.getTracksForGame(searchTerm, storefront, count);
            return ResponseEntity.ok(tracks);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", e.getMessage()));
        }
    }
}
