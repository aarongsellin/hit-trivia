package com.hittrivia.app.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.hittrivia.app.config.GameConfig;
import com.hittrivia.app.model.Track;

import lombok.Getter;
import tools.jackson.databind.ObjectMapper;

@Getter
public class Game {
    private String id;
    private List<String> players;
    private String admin;
    private GameConfig config;
    private Quizz quizz;
    private List<Track> tracks;

    private Phase phase = Phase.WAITING_CONFIG;
    private int currentRound = 0;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> currentTask;

    private Consumer<String> messageBroadcaster;

    private final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public Game(String gameId) {
        this.id = gameId;
        this.players = new ArrayList<>();
        this.admin = null;
        this.quizz = new Quizz();
    }

    public void setConfiguration(JsonNode configuration) {

        System.out.println("configuration in game class: " + configuration);

        // Here we find tracks with Apple Web Kit
        // But for now we are using YouTube so whatever.

        // We can send all the tracks to all the users at once here.

        this.quizz.loadTracks(configuration);

        broadcastMessage(Map.of("type","data", "tits", "hurrr"));
    }

    public void setMessageBroadcaster(Consumer<String> broadcaster) {
        this.messageBroadcaster = broadcaster;
    }

    private void broadcastMessage(Map<String, Object> message) {
        if (messageBroadcaster != null) {
            try {
                messageBroadcaster.accept(OBJECT_MAPPER.writeValueAsString(message));
            } catch (Exception e) {
                System.out.println("Failed to broadcast message: " + message);
            }
        }
    }

    public enum Phase {
        WAITING,
        WAITING_CONFIG,
        PLAYING_MUSIC,
        GUESSING,
        REVEAL,
        FINISHED,
    }



    public void startGame(Consumer<Phase> onPhaseChange) {
        // if (phase != Phase.WAITING_CONFIG) return;
        advancePhase(onPhaseChange);
    }

    public static class PhaseDelays {
        private static final int MUSIC_DELAY = 30;
        private static final int GUESS_DELAY = 15;
        private static final int REVEAL_DELAY = 15;
        private static final int FINISHED_DELAY = 120;
    }

    public void advancePhase(Consumer<Phase> onPhaseChange) {
        switch(phase) {
            case WAITING_CONFIG:
                break;
            case WAITING:
                phase = Phase.PLAYING_MUSIC;
                onPhaseChange.accept(phase);
                scheduleNext(() -> advancePhase(onPhaseChange), PhaseDelays.MUSIC_DELAY);
                break;
            case PLAYING_MUSIC:
                phase = Phase.GUESSING;
                onPhaseChange.accept(phase);
                scheduleNext(() -> advancePhase(onPhaseChange), PhaseDelays.MUSIC_DELAY);
                break;
            case GUESSING:
                phase = Phase.REVEAL;
                onPhaseChange.accept(phase);
                scheduleNext(() -> advancePhase(onPhaseChange), PhaseDelays.GUESS_DELAY);
                break;
            case REVEAL:
                currentRound++;
                if (currentRound >= tracks.size()) {
                    phase = Phase.FINISHED;
                    onPhaseChange.accept(phase);
                    shutdown();
                } else {
                    phase = Phase.PLAYING_MUSIC;
                    onPhaseChange.accept(phase);
                    scheduleNext(() -> advancePhase(onPhaseChange), PhaseDelays.FINISHED_DELAY);
                }
            case FINISHED:
                break;
        }
    }
    
    private void scheduleNext(Runnable task, int delaySeconds) {
        currentTask = scheduler.schedule(task, delaySeconds, TimeUnit.SECONDS);
    }

    public void shutdown() {
        if (currentTask != null) currentTask.cancel(false);
        scheduler.shutdown();
    }

    public Track getCurrentTrack() {
        if (tracks == null || currentRound >= tracks.size()) return null;
        return tracks.get(currentRound);
    }

    public void freezePlayer(String playerId) {
        // The player has left.
    }

    public void unfreezePlayer(String playerId) {
        // The player has rejoined.
    }

    public void addPlayer(String playerId) {
        players.add(playerId);
    }

    public boolean isPlayer(String playerId) {
        return players.contains(playerId);
    }

    public void removePlayer(String playerId) {
        players.remove(playerId);
    }

    public boolean isAdmin(String playerId) {
        return admin.equals(playerId);
    }

    public String getAdmin() {
        return this.admin;
    }

    public void setAdmin(String playerId) {
        this.admin = playerId;

        // Move the game to the configuration phase, meaning the user should now be allowed
        // to display the QR code on the website and that they can now configure the game.
        
    }

    public List<String> getPlayers() {
        return players;
    }

    public int getPlayerCount() {
        return players.size();
    }
}
