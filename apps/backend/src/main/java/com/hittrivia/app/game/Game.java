package com.hittrivia.app.game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.hittrivia.app.dto.MessageType;
import com.hittrivia.app.model.Track;
import com.hittrivia.app.service.AppleMusicCatalogService;

import lombok.Getter;
import tools.jackson.databind.ObjectMapper;

@Getter
public class Game {
    private String id;
    private List<String> players;

    public record GuessResult(int round, String guess, int titleScore, int artistScore, int albumScore) {
        public int total() { return titleScore + artistScore + albumScore; } 
    }
    
    private Map<String,String> playerNames;
    private Map<String, List<GuessResult>> playerGuesses = new HashMap<>();

    private String admin;
    private Quizz quizz;
    private List<Track> tracks;

    private Phase phase = Phase.WAITING_CONFIG;
    private int currentRound = 0;

    private AppleMusicCatalogService catalogService;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> currentTask;

    // Tracks the active timer so reconnecting players can get remaining time
    private long phaseStartTimestamp = 0;
    private long phaseEndTimestamp = 0;
    private Phase nextPhase = null;

    private BiConsumer<MessageType, Map<String, Object>> messageBroadcaster;
    private Consumer<Phase> phaseChangeCallback;

    private final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public Phase getPhase() {
        return phase;
    }

    public Game(String gameId) {
        this.id = gameId;
        this.players = new ArrayList<>();
        this.playerNames = new HashMap<>();
        this.admin = null;
        this.quizz = new Quizz();
    }

    public void setPlayerName(String playerId, String name) {
        playerNames.put(playerId, name);
    }

    public String getPlayerName(String playerId) {
        return playerNames.getOrDefault(playerId, "Player");
    }

    public void setCatalogService(AppleMusicCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    public Quizz getQuizz() {
        return this.quizz;
    }

    /**
     * Normalizes a string for fuzzy comparison: lowercases, strips accents,
     * removes all non-alphanumeric characters, and collapses whitespace.
     */
    private String normalize(String s) {
        if (s == null) return "";
        // Decompose accented characters, strip combining marks
        String decomposed = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        // Remove everything except letters, digits, and spaces, then collapse whitespace
        return decomposed.toLowerCase().replaceAll("[^a-z0-9 ]", "").replaceAll("\\s+", " ").trim();
    }

    /**
     * Checks whether two strings are a fuzzy match.
     * Returns true if either contains the other (after normalization).
     */
    private boolean fuzzyContains(String guess, String answer) {
        if (guess.isEmpty() || answer.isEmpty()) return false;
        return guess.contains(answer) || answer.contains(guess);
    }

    /**
     * Checks the player's guess against the current track.
     * Returns a GuessResult with per-field scores, or null if the guess is rejected
     * (e.g. already guessed this round, no current track, blank input).
     */
    public GuessResult checkGuess(String playerId, String guess) {
        Track track = getCurrentTrack();
        if (track == null || guess == null) return null;

        String normalizedGuess = normalize(guess);
        if (normalizedGuess.isBlank()) return null;

        // Prevent duplicate guesses for the same round
        List<GuessResult> guesses = playerGuesses.computeIfAbsent(playerId, k -> new ArrayList<>());
        if (guesses.stream().anyMatch(g -> g.round() == currentRound)) return null;

        String title  = normalize(track.title());
        String artist = normalize(track.artist());
        String album  = normalize(track.album());

        int titleScore  = fuzzyContains(normalizedGuess, title)  ? 1 : 0;
        int artistScore = fuzzyContains(normalizedGuess, artist) ? 1 : 0;
        int albumScore  = fuzzyContains(normalizedGuess, album)  ? 1 : 0;

        GuessResult result = new GuessResult(currentRound, guess, titleScore, artistScore, albumScore);
        guesses.add(result);

        return result;
    }

    /**
     * Returns the total score for a player across all rounds.
     */
    public int getPlayerScore(String playerId) {
        return playerGuesses.getOrDefault(playerId, List.of()).stream()
                .mapToInt(GuessResult::total)
                .sum();
    }

    // This also starts the game...
    public void setConfiguration(JsonNode configuration) {

        System.out.println("configuration in game class: " + configuration);

        quizz.loadTracks(configuration, catalogService);

        broadcastMessage(MessageType.DATA, Map.of("tracks", quizz.getTracks()));

        // Send initial round info
        broadcastMessage(MessageType.DATA, Map.of("currentRound", currentRound));

        // We have to tell the clients that we are in the waiting phase right now.
        broadcastMessage(MessageType.DATA, Map.of("phase", Map.of("newPhase", Phase.WAITING)));

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

        long startTimestamp = System.currentTimeMillis();
        long endTimestamp = startTimestamp + (durationSeconds * 1000);
        this.phaseStartTimestamp = startTimestamp;
        this.phaseEndTimestamp = endTimestamp;
        this.nextPhase = newPhase;

        broadcastMessage(MessageType.DATA, Map.of(
            "phaseChange", Map.of(
                "newPhase", newPhase.toString(),
                "startTimestamp", startTimestamp,
                "endTimestamp", endTimestamp)
        ));

        currentTask = scheduler.schedule(() -> {
            this.phaseStartTimestamp = 0;
            this.phaseEndTimestamp = 0;
            this.nextPhase = null;
            this.phase = newPhase;
            // Broadcast that we're now in this phase
            broadcastMessage(MessageType.DATA, Map.of("phase", Map.of("newPhase", newPhase.toString())));
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
                broadcastMessage(MessageType.DATA, Map.of("currentRound", currentRound));
                // Start the guessing phase after the music has played.
                startPhaseWithTimer(Phase.GUESSING, PhaseDelays.MUSIC_DELAY);
                break;
            case GUESSING:
                // Start reveal phase after the guessing phase is done.
                startPhaseWithTimer(Phase.REVEAL, PhaseDelays.GUESS_DELAY);
                break;
            case REVEAL:
                // Broadcast current round for reveal phase
                broadcastMessage(MessageType.DATA, Map.of("currentRound", currentRound));
                currentRound++;
                if (currentRound < quizz.getTracks().size()) {
                    startPhaseWithTimer(Phase.WAITING, 5);
                } else {
                    startPhaseWithTimer(Phase.FINISHED, PhaseDelays.REVEAL_DELAY);
                }
                break;
            case FINISHED:
                broadcastFinalScores();
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

    /**
     * Builds and broadcasts the final scoreboard to all players.
     * Each entry contains playerId, rank, and total score.
     */
    private void broadcastFinalScores() {
        int totalRounds = quizz.getTracks() != null ? quizz.getTracks().size() : 0;
        int maxScore = totalRounds * 3; // 3 points per round (title + artist + album)

        // Build score entry for every player, sorted by score descending
        List<Map<String, Object>> scoreboard = players.stream()
                .map(pid -> {
                    int score = getPlayerScore(pid);
                    return Map.<String, Object>of(
                            "playerId", pid,
                            "name", getPlayerName(pid),
                            "score", score
                    );
                })
                .sorted((a, b) -> Integer.compare((int) b.get("score"), (int) a.get("score")))
                .collect(java.util.stream.Collectors.toList());

        // Assign ranks (1-based, tied scores get the same rank)
        List<Map<String, Object>> ranked = new ArrayList<>();
        int rank = 1;
        for (int i = 0; i < scoreboard.size(); i++) {
            if (i > 0 && !scoreboard.get(i).get("score").equals(scoreboard.get(i - 1).get("score"))) {
                rank = i + 1;
            }
            Map<String, Object> entry = new HashMap<>(scoreboard.get(i));
            entry.put("rank", rank);
            ranked.add(entry);
        }

        broadcastMessage(MessageType.DATA, Map.of(
                "finalScores", Map.of(
                        "scoreboard", ranked,
                        "maxScore", maxScore,
                        "totalRounds", totalRounds
                )
        ));
    }

    public void setMessageBroadcaster(BiConsumer<MessageType, Map<String, Object>> broadcaster) {
        this.messageBroadcaster = broadcaster;
    }

    private void broadcastMessage(MessageType messageType, Map<String, Object> message) {
        if (messageBroadcaster != null) {
            try {
                messageBroadcaster.accept(messageType, message);
            } catch (Exception e) {
                System.out.println("Failed to broadcast message: " + message);
                e.printStackTrace();
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
        private static final int GUESS_DELAY = 15;
        private static final int REVEAL_DELAY = 15;
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

    /**
     * Returns true if the game is still accepting new players.
     * Only allows joining during the WAITING_CONFIG phase (lobby).
     */
    public boolean isJoinable() {
        return phase == Phase.WAITING_CONFIG;
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
