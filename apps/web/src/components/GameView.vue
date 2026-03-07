<template>
  <div class="game-container">
    <!-- Progress Bar -->
    <div v-if="phaseEndTime" class="progress-bar-container">
      <div
        class="progress-bar"
        :style="{ width: progressPercentage + '%' }"
      ></div>
    </div>

    <!-- Floating Mute Button -->
    <button
      v-if="showMuteButton"
      class="mute-btn"
      @click="toggleMute"
      :title="musicMuted ? 'Unmute music' : 'Mute music'"
    >
      <span v-if="musicMuted"
        ><svg
          width="24"
          height="24"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5" />
          <line x1="23" y1="9" x2="17" y2="15" />
          <line x1="17" y1="9" x2="23" y2="15" /></svg
      ></span>
      <span v-else
        ><svg
          width="24"
          height="24"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5" />
          <path d="M19.07 4.93a10 10 0 0 1 0 14.14" />
          <path d="M15.54 8.46a5 5 0 0 1 0 7.07" /></svg
      ></span>
    </button>

    <!-- DEBUG: Uncomment for development
    <div class="header">
      <p>Status: {{ socket.status }}</p>
      <p>
        Player ID: <b>{{ playerId }}</b>
      </p>
      <p>GameState: {{ gameState }}</p>
      <div class="debug-buttons">
        <button @click="sendMessage(inputMessage)">Send</button>
        <button @click="clearStorage()">CLEAR</button>
        <button @click="socket.close()">CLOSE</button>
        <button @click="socket.open()">OPEN</button>
        <button @click="test()">JOIN</button>
      </div>
    </div>
    -->

    <!-- Name Entry Screen -->
    <div v-if="!playerName && !waitingForServer" class="name-entry-phase">
      <div class="name-entry-container">
        <div class="name-entry-icon">
          <svg
            width="36"
            height="36"
            viewBox="0 0 24 24"
            fill="none"
            stroke="#e11d48"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <path d="M9 18V5l12-2v13" />
            <circle cx="6" cy="18" r="3" />
            <circle cx="18" cy="16" r="3" />
          </svg>
        </div>
        <h2>Join the game</h2>
        <p class="name-entry-subtitle">Enter your name to get started</p>
        <input
          ref="nameInput"
          v-model="nameInput"
          @keyup.enter="submitName"
          type="text"
          placeholder="Your name..."
          class="name-input"
          maxlength="20"
          autofocus
        />
        <button
          @click="submitName"
          class="name-submit-btn"
          :disabled="!nameInput.trim()"
        >
          Join Game
        </button>
      </div>
    </div>

    <!-- Phase Components -->
    <ConfigPhase
      v-if="playerName && gameState === 'WAITING_CONFIG' && isAdmin"
      :selectedGenre="selectedGenre"
      :selectedDecade="selectedDecade"
      :selectedRounds="selectedRounds"
      :gameUrl="gameUrl"
      @update:genre="selectedGenre = $event"
      @update:decade="selectedDecade = $event"
      @update:rounds="selectedRounds = $event"
      @start-game="startGame"
    />

    <WaitingConfigPhase
      v-else-if="playerName && gameState === 'WAITING_CONFIG' && !isAdmin"
    />

    <WaitingPhase v-else-if="gameState === 'WAITING'" />

    <PlayingMusicPhase
      v-else-if="gameState === 'PLAYING_MUSIC'"
      :track="currentTrack"
      :seek-offset="musicSeekOffset"
      :muted="musicMuted"
    />

    <GuessingPhase
      v-else-if="gameState === 'GUESSING'"
      :game-id="gameId"
      :current-round="currentRound"
      @submit-guess="handleGuessSubmit"
    />

    <!-- Preload current track's music video during play/guess phases -->
    <link
      v-if="
        (gameState === 'PLAYING_MUSIC' || gameState === 'GUESSING') &&
        currentTrack?.musicVideoUrl
      "
      rel="preload"
      :href="currentTrack.musicVideoUrl"
      as="video"
      type="video/mp4"
    />

    <RevealPhase
      v-else-if="gameState === 'REVEAL'"
      :track="currentTrack"
      :musicDuration="musicDuration"
      :guessResult="guessResult"
      :muted="musicMuted"
    />

    <FinishedPhase
      v-else-if="gameState === 'FINISHED'"
      :finalScores="finalScores"
      :playerId="playerId"
      @play-again="handlePlayAgain"
    />

    <!-- Initial Loading - Not joined yet -->
    <!-- <div v-else class="loading">
      <p>Connecting to game...</p>
    </div> -->

    <!-- DEBUG: Uncomment for development
    <div class="send">
      <input v-model="inputMessage" placeholder="Debug message" />
    </div>

    <div class="messages">
      <div v-for="(msg, index) in messages" :key="index">
        <strong>{{ msg.sender }}:</strong> {{ msg.text }}
      </div>
    </div>
    -->
  </div>
</template>

<script>
import { useRoute, useRouter } from 'vue-router';
import { useWebSocket } from '@vueuse/core';
import ConfigPhase from './phases/ConfigPhase.vue';
import WaitingConfigPhase from './phases/WaitingConfigPhase.vue';
import WaitingPhase from './phases/WaitingPhase.vue';
import PlayingMusicPhase from './phases/PlayingMusicPhase.vue';
import GuessingPhase from './phases/GuessingPhase.vue';
import RevealPhase from './phases/RevealPhase.vue';
import FinishedPhase from './phases/FinishedPhase.vue';

const isJson = (data) => {
  try {
    JSON.parse(data);
    return true;
  } catch (err) {
    return false;
  }
};

export default {
  components: {
    ConfigPhase,
    WaitingConfigPhase,
    WaitingPhase,
    PlayingMusicPhase,
    GuessingPhase,
    RevealPhase,
    FinishedPhase,
  },
  data() {
    return {
      messages: [],
      inputMessage: '',
      playerId: null,
      playerName: null,
      nameInput: '',
      isAdmin: null,
      socket: null,
      gameState: null,
      gameId: null,
      waitingForServer: false,
      countdown: null,
      gameUrl: '',

      // Progress bar
      phaseEndTime: null,
      phaseStartTime: null,
      progressPercentage: 0,
      progressInterval: null,
      pendingPhase: null, // The next phase announced by the server
      queuedPhaseChange: null, // Pre-sent next countdown from server
      clockOffset: 0, // serverTime - Date.now(); add to Date.now() to get server-equivalent time

      // Configuration options
      selectedGenre: 'Pop',
      selectedDecade: '2000s',
      selectedObscurity: 3,
      selectedRounds: 5,

      // Tracks and current track
      tracks: [],
      currentTrack: null,
      currentRound: 0,
      musicDuration: 0, // How long the music played for
      musicPhaseStartTime: null, // When PLAYING_MUSIC phase started
      preloadedVideoUrl: null, // Currently preloaded video URL

      // Guess result for the current round
      guessResult: null,

      // Final scores for the finished phase
      finalScores: null,

      // Music mute state — muted by default for non-host players
      musicMuted: true, // overridden in created() from localStorage
    };
  },
  created: function () {
    const wsHost = window.location.host;
    const wsProtocol = window.location.protocol === 'https:' ? 'wss' : 'ws';

    const route = useRoute();
    this.router = useRouter();
    this.gameId = route.query.id;
    this.playerId = localStorage.getItem(`playerId_${this.gameId}`) || null;
    this.waitingForServer = !!this.playerId;

    // Restore mute preference for this game
    const storedMute = localStorage.getItem(`musicMuted_${this.gameId}`);
    if (storedMute !== null) {
      this.musicMuted = storedMute === 'true';
    }

    this.gameUrl = `${window.location.origin}/game?id=${this.gameId}`;

    this.socket = useWebSocket(
      `${wsProtocol}://${wsHost}/ws/game/${this.gameId}`,
      {
        immediate: true,
        autoReconnect: true,
        onConnected: () => {
          // If we have a stored playerId, attempt to rejoin and let the
          // server send back the playerName. Otherwise wait for the user
          // to enter a name.
          if (this.playerId) {
            this.handlePlayerJoin();
          }
        },
      }
    );
  },
  computed: {
    musicSeekOffset() {
      // Seconds elapsed since the PLAYING_MUSIC phase started (for reconnect seek)
      // Adjust for clock offset between server and client
      if (this.phaseStartTime && this.gameState === 'PLAYING_MUSIC') {
        const now = Date.now() + this.clockOffset;
        return Math.max(0, (now - this.phaseStartTime) / 1000);
      }
      return 0;
    },
    showMuteButton() {
      return (
        this.playerName &&
        ['PLAYING_MUSIC', 'GUESSING', 'REVEAL', 'WAITING'].includes(
          this.gameState
        )
      );
    },
  },
  beforeUnmount() {
    if (this.progressInterval) {
      clearInterval(this.progressInterval);
    }
  },
  watch: {
    'socket.data': function () {
      this.handleMessage(this.socket.data);
    },
  },
  methods: {
    clearStorage() {
      this.playerId = null;
      this.playerName = null;
      this.nameInput = '';
      localStorage.removeItem(`playerId_${this.gameId}`);
      localStorage.removeItem(`musicMuted_${this.gameId}`);
      this.isAdmin = false;
      this.gameState = null;
    },
    toggleMute() {
      this.musicMuted = !this.musicMuted;
      localStorage.setItem(`musicMuted_${this.gameId}`, this.musicMuted);
    },
    unlockAudio() {
      // Play a tiny silent WAV to unlock the browser's autoplay policy.
      // This must happen inside a user-gesture handler (click / tap).
      try {
        const silence = new Audio(
          'data:audio/wav;base64,UklGRiQAAABXQVZFZm10IBAAAAABAAEARKwAAIhYAQACABAAZGF0YQAAAAA='
        );
        silence.volume = 0;
        silence
          .play()
          .then(() => silence.pause())
          .catch(() => {});
      } catch (_) {
        /* ignore */
      }
      try {
        const ctx = new (window.AudioContext || window.webkitAudioContext)();
        ctx.resume().catch(() => {});
      } catch (_) {
        /* ignore */
      }
    },
    submitName() {
      const name = this.nameInput.trim();
      if (!name) return;
      this.unlockAudio();
      this.playerName = name;
      this.handlePlayerJoin();
    },
    handlePlayerJoin() {
      const toSend = JSON.stringify({
        type: 'data',
        playerId: this.playerId,
        playerName: this.playerName,
      });

      this.socket.send(toSend);
    },
    handleLeave() {
      this.socket.send(
        JSON.stringify({
          type: 'data',
          action: 'leave',
        })
      );
    },
    startGame() {
      const config = {
        genre: this.selectedGenre,
        decade: this.selectedDecade,
        obscurity: this.selectedObscurity,
        trackCount: this.selectedRounds,
      };

      const toSend = JSON.stringify({
        type: 'data',
        configuration: config,
      });

      this.socket.send(toSend);
    },
    handleGuessSubmit(guess) {
      // Send guess to backend
      this.socket.send(
        JSON.stringify({
          type: 'data',
          action: { type: 'guess', guess },
        })
      );
    },
    handlePlayAgain() {
      // Reset game or navigate back to lobby
    },
    requestTracks() {
      this.socket.send(
        JSON.stringify({
          type: 'data',
          action: { type: 'requestTracks' },
        })
      );
    },
    startPhaseCountdown(newPhase, startTimestamp, endTimestamp, serverTime) {
      // Clear any existing interval
      if (this.progressInterval) {
        clearInterval(this.progressInterval);
      }

      // Remember what phase we're counting down to
      this.pendingPhase = newPhase;

      // Compute clock offset between server and client.
      // If server clock is 500ms ahead, offset = +500 → we add 500 to Date.now()
      // to get the "server-equivalent" time.
      const clockOffset = serverTime ? serverTime - Date.now() : 0;
      this.clockOffset = clockOffset;

      // Use the server's original start time to calculate total duration
      // so the bar shows the correct proportion even after a page reload
      this.phaseStartTime = startTimestamp;
      this.phaseEndTime = endTimestamp;

      const totalDuration = endTimestamp - startTimestamp;

      // Start at the correct remaining percentage (adjusted for clock offset)
      const now = Date.now() + clockOffset;
      const remaining = endTimestamp - now;
      this.progressPercentage = Math.max((remaining / totalDuration) * 100, 0);

      // Update progress every 50ms for smooth animation
      this.progressInterval = setInterval(() => {
        const now = Date.now() + clockOffset;
        const remaining = endTimestamp - now;
        const percentage = Math.max((remaining / totalDuration) * 100, 0);

        this.progressPercentage = percentage;

        // When complete, clear interval and trigger phase change
        if (percentage <= 0) {
          clearInterval(this.progressInterval);
          this.phaseChangeCleanup();
        }
      }, 50);
    },
    phaseChangeCleanup() {
      // Clear progress bar
      this.phaseEndTime = null;
      this.progressPercentage = 0;
      if (this.progressInterval) {
        clearInterval(this.progressInterval);
      }

      // Switch to the announced phase immediately instead of waiting
      // for the server's confirmation message (eliminates network delay).
      // The server's 'phase' message will still arrive as a sync backup.
      if (this.pendingPhase) {
        this.applyPhaseSwitch(this.pendingPhase);
        this.pendingPhase = null;
      }

      // If the server pre-sent the next countdown, start it immediately
      // so there is zero gap between phases on the client.
      if (this.queuedPhaseChange) {
        const next = this.queuedPhaseChange;
        this.queuedPhaseChange = null;
        this.startPhaseCountdown(
          next.newPhase,
          next.startTimestamp,
          next.endTimestamp,
          next.serverTime
        );
      }
    },
    applyPhaseSwitch(newPhase) {
      // Already in this phase (e.g. countdown already switched us) — skip
      if (this.gameState === newPhase) return;

      this.gameState = newPhase;

      // If we enter a phase that needs tracks but have none, request them
      if (
        ['PLAYING_MUSIC', 'GUESSING', 'REVEAL'].includes(newPhase) &&
        (!this.tracks || this.tracks.length === 0)
      ) {
        this.requestTracks();
      }

      // Track when PLAYING_MUSIC phase starts
      if (newPhase === 'PLAYING_MUSIC') {
        this.musicPhaseStartTime = Date.now();
        this.guessResult = null;
        this.preloadVideo();
        // Clean up guess from previous round
        if (this.currentRound > 0) {
          localStorage.removeItem(
            `guess_${this.gameId}_${this.currentRound - 1}`
          );
        }
      }
      // Calculate music duration when leaving PLAYING_MUSIC phase
      else if (this.musicPhaseStartTime && newPhase !== 'PLAYING_MUSIC') {
        this.musicDuration = Math.floor(
          (Date.now() - this.musicPhaseStartTime) / 1000
        );
      }
    },
    handleMessage(data) {
      this.messages.push({
        text: data,
        sender: 'Server',
      });

      if (isJson(data)) {
        const parsed = JSON.parse(data);
        const type = parsed?.type;

        if (type === 'data') {
          const keys = Object.keys(parsed);

          // If this message contains a playerId that isn't ours,
          // it's a broadcast about another player — skip identity fields.
          const msgPlayerId = parsed.playerId;
          const isAboutUs =
            !msgPlayerId || !this.playerId || msgPlayerId === this.playerId;

          for (let i = 0; i < Object.keys(parsed).length; ++i) {
            const key = keys.at(i);
            const element = parsed[key];

            switch (key) {
              case 'playerId':
                if (!isAboutUs) break; // not our data — ignore
                this.playerId = element;
                localStorage.setItem(`playerId_${this.gameId}`, this.playerId);
                // If the server sent a new playerId without playerName,
                // this is a new-player assignment — stop waiting so the
                // name entry screen appears.
                if (!parsed.playerName) {
                  this.waitingForServer = false;
                }
                break;
              case 'playerName':
                if (!isAboutUs) break;
                this.playerName = element;
                this.waitingForServer = false;
                break;
              case 'admin':
                if (!isAboutUs) break; // not our data — ignore
                this.isAdmin = element;
                // Host plays music by default; guests are muted
                if (element === true) {
                  this.musicMuted = false;
                  localStorage.setItem(`musicMuted_${this.gameId}`, 'false');
                }
                break;
              case 'gameState':
                this.gameState = element;
                // On reconnect during PLAYING_MUSIC, set the start time so
                // music duration tracking works when the phase transitions
                if (element === 'PLAYING_MUSIC' && !this.musicPhaseStartTime) {
                  this.musicPhaseStartTime = Date.now();
                }
                // If we landed in a phase that needs tracks but have none, request them
                if (
                  ['PLAYING_MUSIC', 'GUESSING', 'REVEAL'].includes(element) &&
                  (!this.tracks || this.tracks.length === 0)
                ) {
                  this.requestTracks();
                }
                break;
              case 'phase': {
                // Server confirmation of phase change — apply it.
                // If the client already switched via countdown, this is a no-op.
                this.applyPhaseSwitch(element.newPhase);
                break;
              }
              case 'phaseChange':
                // Store the look-ahead countdown if the server included one
                this.queuedPhaseChange = element.nextPhaseChange || null;
                this.startPhaseCountdown(
                  element.newPhase,
                  element.startTimestamp,
                  element.endTimestamp,
                  element.serverTime
                );
                break;
              case 'tracks':
                this.tracks = element;
                // If currentRound is already set (e.g. reconnect), resolve currentTrack now
                if (
                  this.tracks &&
                  this.tracks[this.currentRound] &&
                  !this.currentTrack
                ) {
                  this.currentTrack = this.tracks[this.currentRound];
                }
                break;
              case 'guessResult':
                this.guessResult = element;
                break;
              case 'finalScores':
                this.finalScores = element;
                // Clean up the last round's guess from localStorage
                localStorage.removeItem(
                  `guess_${this.gameId}_${this.currentRound}`
                );
                // Clear mute preference at game end
                localStorage.removeItem(`musicMuted_${this.gameId}`);
                break;
              case 'currentRound':
                this.currentRound = element;
                if (this.tracks && this.tracks[element]) {
                  this.currentTrack = this.tracks[element];
                }
                break;
              default:
                break;
            }
          }
        }

        if (type === 'error') {
          this.waitingForServer = false;
          if (parsed?.code === 'GAME_FINISHED') {
            // Game is over — redirect home
            this.router.replace('/');
          } else if (
            parsed?.message &&
            parsed.message.includes('does not exist')
          ) {
            // Invalid game ID — redirect home
            this.router.replace('/');
          }
        }
      }
    },
    preloadVideo() {
      if (
        this.currentTrack?.musicVideoUrl &&
        this.currentTrack.musicVideoUrl !== this.preloadedVideoUrl
      ) {
        const video = document.createElement('video');
        video.preload = 'auto';
        video.src = this.currentTrack.musicVideoUrl;
        video.load();
        this.preloadedVideoUrl = this.currentTrack.musicVideoUrl;
      }
    },
    sendMessage(message) {
      if (this.socket.status === 'CLOSED') {
        return;
      }

      this.socket.send(message);

      this.messages.push({
        text: message,
        sender: 'Me',
      });

      this.inputMessage = '';
    },
  },
};
</script>

<style scoped>
.game-container {
  width: 100%;
  min-height: 100vh;
  padding: 20px;
  background: #f5f5f5;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  color: #333;
  position: relative;
}

/* Progress Bar */
.progress-bar-container {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 14px;
  background: rgba(0, 0, 0, 0.1);
  z-index: 1000;
  overflow: hidden;
}

.progress-bar {
  height: 100%;
  background: linear-gradient(90deg, #2196f3, #1976d2);
  transition: width 0.05s linear;
  box-shadow: 0 0 10px rgba(33, 150, 243, 0.5);
}

/* Floating Mute Button */
.mute-btn {
  position: fixed;
  bottom: 24px;
  right: 24px;
  z-index: 1000;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  border: 2px solid #e5e7eb;
  background: white;
  font-size: 24px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  transition: all 0.2s ease;
}

.mute-btn:hover {
  transform: scale(1.08);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.15);
}

.mute-btn:active {
  transform: scale(0.95);
}

.header {
  background: white;
  padding: 15px 20px;
  border-radius: 4px;
  margin-bottom: 30px;
  border: 1px solid #e0e0e0;
}

.header p {
  margin: 8px 0;
  font-size: 14px;
  color: #666;
}

.debug-buttons {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}

.debug-buttons button {
  padding: 6px 12px;
  background: #fff;
  color: #333;
  border: 1px solid #ddd;
  border-radius: 3px;
  cursor: pointer;
  font-size: 12px;
}

.debug-buttons button:hover {
  background: #f9f9f9;
  border-color: #999;
}

.loading {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 80vh;
  color: #666;
  font-size: 16px;
}

.send {
  background: white;
  padding: 12px;
  border-radius: 4px;
  margin-bottom: 16px;
  border: 1px solid #e0e0e0;
}

.send input {
  width: 100%;
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 3px;
  font-size: 13px;
}

.messages {
  background: white;
  padding: 16px;
  border-radius: 4px;
  max-height: 250px;
  overflow-y: auto;
  border: 1px solid #e0e0e0;
}

.messages div {
  padding: 6px 0;
  border-bottom: 1px solid #f5f5f5;
  font-size: 12px;
  color: #666;
}

.messages div:last-child {
  border-bottom: none;
}

@media (max-width: 768px) {
  .game-container {
    padding: 10px;
  }

  .name-entry-container {
    padding: 32px 20px;
  }

  .name-input {
    font-size: 16px;
    padding: 14px 16px;
  }
}

/* ─── Name Entry ───────────────────────────────── */

.name-entry-phase {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 70vh;
}

.name-entry-container {
  background: white;
  padding: 48px 56px;
  border-radius: 16px;
  border: 1px solid #e5e7eb;
  text-align: center;
  width: 100%;
  max-width: 420px;
}

.name-entry-icon {
  font-size: 36px;
  color: #e11d48;
  margin-bottom: 8px;
}

.name-entry-container h2 {
  margin: 0 0 8px 0;
  color: #1a1a1a;
  font-size: 24px;
  font-weight: 800;
  letter-spacing: -0.5px;
}

.name-entry-subtitle {
  color: #9ca3af;
  font-size: 15px;
  margin: 0 0 28px 0;
}

.name-input {
  width: 100%;
  padding: 16px 20px;
  border: 2px solid #e5e7eb;
  border-radius: 12px;
  font-size: 18px;
  font-weight: 500;
  color: #1a1a1a;
  background: #fafafa;
  text-align: center;
  box-sizing: border-box;
  transition: all 0.2s ease;
  margin-bottom: 16px;
}

.name-input::placeholder {
  color: #9ca3af;
  font-weight: 400;
}

.name-input:focus {
  outline: none;
  border-color: #1a1a1a;
  background: white;
  box-shadow: 0 0 0 4px rgba(0, 0, 0, 0.05);
}

.name-submit-btn {
  width: 100%;
  padding: 14px 28px;
  background: #1a1a1a;
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.name-submit-btn:hover:not(:disabled) {
  background: #333;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.name-submit-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
</style>
