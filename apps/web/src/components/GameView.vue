<template>
  <div class="game-container">
    <!-- Progress Bar -->
    <div v-if="phaseEndTime" class="progress-bar-container">
      <div
        class="progress-bar"
        :style="{ width: progressPercentage + '%' }"
      ></div>
    </div>

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

    <!-- Blocked from joining -->
    <div v-if="joinBlocked" class="name-entry-phase">
      <div class="name-entry-container">
        <div class="name-entry-icon">🚫</div>
        <h2>Can't join</h2>
        <p class="name-entry-subtitle">{{ joinBlockedMessage }}</p>
        <a href="/"><button class="name-submit-btn">Back to Home</button></a>
      </div>
    </div>

    <!-- Name Entry Screen -->
    <div v-else-if="!playerName" class="name-entry-phase">
      <div class="name-entry-container">
        <div class="name-entry-icon">♫</div>
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
      :selectedObscurity="selectedObscurity"
      :selectedRounds="selectedRounds"
      :gameUrl="gameUrl"
      @update:genre="selectedGenre = $event"
      @update:decade="selectedDecade = $event"
      @update:obscurity="selectedObscurity = $event"
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
      playerId: localStorage.getItem('playerId') || null,
      playerName: localStorage.getItem('playerName') || null,
      nameInput: localStorage.getItem('playerName') || '',
      isAdmin: null,
      socket: null,
      gameState: null,
      gameId: null,
      countdown: null,
      gameUrl: '',

      // Progress bar
      phaseEndTime: null,
      phaseStartTime: null,
      progressPercentage: 0,
      progressInterval: null,

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

      // Whether the player was blocked from joining
      joinBlocked: false,
      joinBlockedMessage: '',
    };
  },
  created: function () {
    // In production (same-origin), use window.location.host.
    // In dev, VUE_APP_API_URL points to the backend dev server.
    const envUrl = process.env.VUE_APP_API_URL;
    let wsHost;
    let wsProtocol;
    if (envUrl) {
      wsHost = envUrl.replace(/^https?:\/\//, '');
      wsProtocol = 'ws';
    } else {
      wsHost = window.location.host;
      wsProtocol = window.location.protocol === 'https:' ? 'wss' : 'ws';
    }

    const route = useRoute();
    this.router = useRouter();
    this.gameId = route.query.id;
    this.gameUrl = `${window.location.origin}?id=${this.gameId}`;

    this.socket = useWebSocket(
      `${wsProtocol}://${wsHost}/ws/game/${this.gameId}`,
      {
        autoReconnect: true,
        onConnected: () => {
          // Only auto-join if we already have a name (e.g. reconnect/refresh)
          if (this.playerName) {
            this.handlePlayerJoin();
          }
        },
      }
    );

    this.socket.open();
  },
  computed: {
    musicSeekOffset() {
      // Seconds elapsed since the PLAYING_MUSIC phase started (for reconnect seek)
      if (this.phaseStartTime && this.gameState === 'PLAYING_MUSIC') {
        return Math.max(0, (Date.now() - this.phaseStartTime) / 1000);
      }
      return 0;
    },
  },
  watch: {
    'socket.data': function () {
      this.handleMessage(this.socket.data);
    },
  },
  methods: {
    clearStorage() {
      this.playerId = null;
      localStorage.setItem('playerId', null);
      this.isAdmin = false;
      this.gameState = null;
    },
    submitName() {
      const name = this.nameInput.trim();
      if (!name) return;
      this.playerName = name;
      localStorage.setItem('playerName', name);
      this.handlePlayerJoin();
    },
    handlePlayerJoin() {
      const toSend = JSON.stringify({
        type: 'data',
        playerId: localStorage.getItem('playerId') || null,
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
      console.log('Guess submitted:', guess);
      // Send guess to backend
      this.socket.send(
        JSON.stringify({
          type: 'data',
          action: { type: 'guess', guess },
        })
      );
    },
    handlePlayAgain() {
      console.log('Play again clicked');
      // Reset game or navigate back to lobby
    },
    startPhaseCountdown(newPhase, startTimestamp, endTimestamp) {
      // Clear any existing interval
      if (this.progressInterval) {
        clearInterval(this.progressInterval);
      }

      // Use the server's original start time to calculate total duration
      // so the bar shows the correct proportion even after a page reload
      this.phaseStartTime = startTimestamp;
      this.phaseEndTime = endTimestamp;

      const totalDuration = endTimestamp - startTimestamp;

      // Start at the correct remaining percentage
      const now = Date.now();
      const remaining = endTimestamp - now;
      this.progressPercentage = Math.max((remaining / totalDuration) * 100, 0);

      // Update progress every 50ms for smooth animation
      this.progressInterval = setInterval(() => {
        const now = Date.now();
        const remaining = endTimestamp - now;
        const percentage = Math.max((remaining / totalDuration) * 100, 0);

        this.progressPercentage = percentage;

        // When complete, clear interval and trigger phase change
        if (percentage <= 0) {
          clearInterval(this.progressInterval);
          this.handlePhaseChange(newPhase);
        }
      }, 50);
    },
    handlePhaseChange(phase) {
      // Clear progress bar
      this.phaseEndTime = null;
      this.progressPercentage = 0;
      if (this.progressInterval) {
        clearInterval(this.progressInterval);
      }

      // Timer completed - backend will send the phase update message
      console.log('Countdown completed for:', phase);
    },
    handleMessage(data) {
      console.log(
        'Received message from server: ',
        JSON.stringify(data, null, 2)
      );
      this.messages.push({
        text: data,
        sender: 'Server',
      });

      if (isJson(data)) {
        const parsed = JSON.parse(data);
        const type = parsed?.type;

        if (type === 'data') {
          const keys = Object.keys(parsed);

          for (let i = 0; i < Object.keys(parsed).length; ++i) {
            const key = keys.at(i);
            const element = parsed[key];

            switch (key) {
              case 'playerId':
                this.playerId = element;
                localStorage.setItem('playerId', this.playerId);
                break;
              case 'admin':
                this.isAdmin = element;
                break;
              case 'gameState':
                this.gameState = element;
                // On reconnect during PLAYING_MUSIC, set the start time so
                // music duration tracking works when the phase transitions
                if (element === 'PLAYING_MUSIC' && !this.musicPhaseStartTime) {
                  this.musicPhaseStartTime = Date.now();
                }
                break;
              case 'phase': {
                const newPhase = element.newPhase;
                this.gameState = newPhase;

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
                else if (
                  this.musicPhaseStartTime &&
                  newPhase !== 'PLAYING_MUSIC'
                ) {
                  this.musicDuration = Math.floor(
                    (Date.now() - this.musicPhaseStartTime) / 1000
                  );
                  console.log(
                    'Music played for:',
                    this.musicDuration,
                    'seconds'
                  );
                }
                break;
              }
              case 'phaseChange':
                this.startPhaseCountdown(
                  element.newPhase,
                  element.startTimestamp,
                  element.endTimestamp
                );
                break;
              case 'tracks':
                this.tracks = element;
                console.log('Received tracks:', element);
                // If currentRound is already set (e.g. reconnect), resolve currentTrack now
                if (
                  this.tracks &&
                  this.tracks[this.currentRound] &&
                  !this.currentTrack
                ) {
                  this.currentTrack = this.tracks[this.currentRound];
                  console.log(
                    'Resolved current track from tracks arrival:',
                    this.currentTrack
                  );
                }
                break;
              case 'guessResult':
                this.guessResult = element;
                console.log('Guess result:', element);
                break;
              case 'finalScores':
                this.finalScores = element;
                // Clean up the last round's guess from localStorage
                localStorage.removeItem(
                  `guess_${this.gameId}_${this.currentRound}`
                );
                console.log('Final scores:', element);
                break;
              case 'currentRound':
                this.currentRound = element;
                if (this.tracks && this.tracks[element]) {
                  this.currentTrack = this.tracks[element];
                  console.log('Current track:', this.currentTrack);
                }
                break;
              default:
                console.log('Unknown data contents', { key });
            }
          }
        }

        if (type === 'error') {
          console.log('received error:', parsed);
          if (parsed?.code === 'GAME_ALREADY_STARTED') {
            this.joinBlocked = true;
            this.joinBlockedMessage =
              parsed?.message || 'This game has already started.';
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
        console.log('Preloading video:', this.currentTrack.musicVideoUrl);
      }
    },
    sendMessage(message) {
      if (this.socket.status === 'CLOSED') {
        console.log('Socket is closed, can not send!');
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
  height: 6px;
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
