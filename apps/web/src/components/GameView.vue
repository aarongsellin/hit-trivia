<template>
  <div class="game-container">
    <!-- Progress Bar -->
    <div v-if="phaseEndTime" class="progress-bar-container">
      <div
        class="progress-bar"
        :style="{ width: progressPercentage + '%' }"
      ></div>
    </div>

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

    <!-- Phase Components -->
    <ConfigPhase
      v-if="gameState === 'WAITING_CONFIG' && isAdmin"
      :selectedGenre="selectedGenre"
      :selectedDecade="selectedDecade"
      :selectedObscurity="selectedObscurity"
      @update:genre="selectedGenre = $event"
      @update:decade="selectedDecade = $event"
      @update:obscurity="selectedObscurity = $event"
      @start-game="startGame"
    />

    <WaitingConfigPhase
      v-else-if="gameState === 'WAITING_CONFIG' && !isAdmin"
    />

    <WaitingPhase v-else-if="gameState === 'WAITING'" />

    <PlayingMusicPhase
      v-else-if="gameState === 'PLAYING_MUSIC'"
      :track="currentTrack"
    />

    <GuessingPhase
      v-else-if="gameState === 'GUESSING'"
      @submit-guess="handleGuessSubmit"
    />

    <RevealPhase
      v-else-if="gameState === 'REVEAL'"
      :track="currentTrack"
      :musicDuration="musicDuration"
    />

    <FinishedPhase
      v-else-if="gameState === 'FINISHED'"
      @play-again="handlePlayAgain"
    />

    <!-- Initial Loading - Not joined yet -->
    <div v-else class="loading">
      <p>Connecting to game...</p>
    </div>

    <div class="send">
      <input v-model="inputMessage" placeholder="Debug message" />
    </div>

    <div class="messages">
      <div v-for="(msg, index) in messages" :key="index">
        <strong>{{ msg.sender }}:</strong> {{ msg.text }}
      </div>
    </div>
  </div>
</template>

<script>
import { useRoute } from 'vue-router';
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
      isAdmin: null,
      socket: null,
      test: this.handlePlayerJoin,
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

      // Tracks and current track
      tracks: [],
      currentTrack: null,
      currentRound: 0,
      musicDuration: 0, // How long the music played for
      musicPhaseStartTime: null, // When PLAYING_MUSIC phase started
    };
  },
  created: function () {
    const apiUrl = process.env.VUE_APP_API_URL.replace('http://', '').replace(
      'https://',
      ''
    );

    const route = useRoute();
    this.gameId = route.query.id;
    this.gameUrl = `${window.location.origin}?id=${this.gameId}`;

    this.socket = useWebSocket(`ws://${apiUrl}/ws/game/${this.gameId}`, {
      autoReconnect: true,
    });

    this.socket.open();
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
    handlePlayerJoin() {
      const toSend = JSON.stringify({
        type: 'data',
        playerId: localStorage.getItem('playerId') || null,
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
    startPhaseCountdown(newPhase, endTimestamp) {
      // Clear any existing interval
      if (this.progressInterval) {
        clearInterval(this.progressInterval);
      }

      // Set times
      this.phaseStartTime = Date.now();
      this.phaseEndTime = endTimestamp;

      const totalDuration = endTimestamp - this.phaseStartTime;

      // Update progress every 50ms for smooth animation
      this.progressInterval = setInterval(() => {
        const now = Date.now();
        const elapsed = now - this.phaseStartTime;
        const percentage = Math.min((elapsed / totalDuration) * 100, 100);

        this.progressPercentage = percentage;

        // When complete, clear interval and trigger phase change
        if (percentage >= 100) {
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
      this.messages.push({
        text: data,
        sender: 'Server',
      });

      if (isJson(data)) {
        const parsed = JSON.parse(data);
        const type = parsed?.type;

        if (type === 'data') {
          const keys = Object.keys(parsed);

          console.log({ keys });

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
                break;
              case 'phase': {
                const newPhase = element.newPhase;
                this.gameState = newPhase;

                // Track when PLAYING_MUSIC phase starts
                if (newPhase === 'PLAYING_MUSIC') {
                  this.musicPhaseStartTime = Date.now();
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
                  element.endTimestamp
                );
                break;
              case 'tracks':
                this.tracks = element;
                console.log('Received tracks:', element);
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
          console.log('received error');
        }
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
}
</style>
