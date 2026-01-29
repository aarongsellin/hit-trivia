package com.hittrivia.app.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.hittrivia.app.model.Track;
import com.hittrivia.app.service.GameMessageService;

import lombok.Getter;
import tools.jackson.databind.ObjectMapper;

@Getter
public class Game {
    private String id;
    private List<String> players;
    private String admin;
    private Quizz quizz;
    private List<Track> tracks;

    private Phase phase = Phase.WAITING_CONFIG;
    private int currentRound = 0;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> currentTask;

    private BiConsumer<GameMessageService.MessageType, Map<String, Object>> messageBroadcaster;
    private Consumer<Phase> phaseChangeCallback;

    private final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public Phase getPhase() {
        return phase;
    }

    public Game(String gameId) {
        this.id = gameId;
        this.players = new ArrayList<>();
        this.admin = null;
        this.quizz = new Quizz();
    }

    // This also starts the game...
    public void setConfiguration(JsonNode configuration) {

        System.out.println("configuration in game class: " + configuration);

        // Here we find tracks with Apple Web Kit
        // But for now we are using YouTube so whatever.

        // We can send all the tracks to all the users at once here.

        quizz.loadTracks(configuration);

        broadcastMessage(GameMessageService.MessageType.DATA, Map.of("tracks", quizz.getTracks()));

        // Send initial round info
        broadcastMessage(GameMessageService.MessageType.DATA, Map.of("currentRound", currentRound));

        // We have to tell the clients that we are in the waiting phase right now.
        broadcastMessage(GameMessageService.MessageType.DATA, Map.of("phase", Map.of("newPhase", Phase.WAITING)));

        // Tell the clients that the music phase begins in x seconds.
        startPhaseWithTimer(Phase.PLAYING_MUSIC, 3);
    }

    /*
    This function starts a new phase after durationSeconds has passed.
    */
    private void startPhaseWithTimer(Phase newPhase, int durationSeconds) {
        // If there is a task, and the currentTask is not done.
        // Then cancel?
        if (currentTask != null && !currentTask.isDone()) {
            // But here we do not interrupt the task if it is running?
            currentTask.cancel(false);
        }

        broadcastMessage(GameMessageService.MessageType.DATA, Map.of(
            "phaseChange", Map.of(
                "newPhase", newPhase.toString(),
                "endTimestamp", System.currentTimeMillis() + (durationSeconds * 1000))
        ));

        currentTask = scheduler.schedule(() -> {
            this.phase = newPhase;
            // Broadcast that we're now in this phase
            broadcastMessage(GameMessageService.MessageType.DATA, Map.of("phase", Map.of("newPhase", newPhase.toString())));
            handlePhaseTransition(newPhase);
        }, durationSeconds, TimeUnit.SECONDS);

        // We have to be careful here not to start a bunch of threads that take up things on the processor.
    }

    private void handlePhaseTransition(Phase newPhase) {
        switch (newPhase) {
            case WAITING:
                // A wait for start.
                startPhaseWithTimer(Phase.PLAYING_MUSIC, PhaseDelays.WAIT_DELAY);
                break;
            case PLAYING_MUSIC:
                // Broadcast current round so frontend knows which track to play
                broadcastMessage(GameMessageService.MessageType.DATA, Map.of("currentRound", currentRound));
                // Start the guessing phase after the music has played.
                startPhaseWithTimer(Phase.GUESSING, PhaseDelays.MUSIC_DELAY);
                break;
            case GUESSING:
                // Start reveal phase after the guessing phase is done.
                startPhaseWithTimer(Phase.REVEAL, PhaseDelays.GUESS_DELAY);
                break;
            case REVEAL:
                // Broadcast current round for reveal phase
                broadcastMessage(GameMessageService.MessageType.DATA, Map.of("currentRound", currentRound));
                currentRound++;
                if (currentRound < quizz.getTracks().size()) {
                    startPhaseWithTimer(Phase.WAITING, 5);
                } else {
                    startPhaseWithTimer(Phase.FINISHED, PhaseDelays.REVEAL_DELAY);
                }
                break;
            case FINISHED:
                cleanup();
                break;
        }
    }

    public Track getCurrentTrack() {
        if (quizz.getTracks() == null || currentRound >= quizz.getTracks().size()) return null;
        return quizz.getTracks().get(currentRound);
    }

    public void shutdown() {
        if (currentTask != null) {
            currentTask.cancel(false);
        }
        // Don't shutdown shared scheduler!
    }

    private void cleanup() {
        shutdown();
        // Additional cleanup logic
    }

    public void setMessageBroadcaster(BiConsumer<GameMessageService.MessageType, Map<String, Object>> broadcaster) {
        this.messageBroadcaster = broadcaster;
    }

    private void broadcastMessage(GameMessageService.MessageType dataType, Map<String, Object> message) {
        if (messageBroadcaster != null) {
            try {
                messageBroadcaster.accept(dataType,message);
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

    public static class PhaseDelays {
        private static final int MUSIC_DELAY = 15;
        private static final int GUESS_DELAY = 3;
        private static final int REVEAL_DELAY = 3;
        private static final int WAIT_DELAY = 3;

        private static final int FINISHED_DELAY = 120;
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
