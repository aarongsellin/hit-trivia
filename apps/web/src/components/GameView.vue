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

    <!-- Admin Configuration View -->
    <div v-if="gameState === 'WAITING_CONFIG' && isAdmin" class="config-layout">
      <!-- <div class="qr-section">
        <h2>Share Game</h2>
        <qrcode-vue :value="gameUrl" size="300"></qrcode-vue>
        <p class="qr-text">Scan to join the game</p>
      </div>

      <div class="separator"></div> -->

      <div class="config-section">
        <h2>Game Configuration</h2>

        <div class="config-group">
          <label>Genre</label>
          <div class="option-grid">
            <button
              v-for="genre in genres"
              :key="genre"
              @click="selectedGenre = genre"
              :class="['option-btn', { active: selectedGenre === genre }]"
            >
              {{ genre }}
            </button>
          </div>
        </div>

        <div class="config-group">
          <label>Decade</label>
          <div class="option-grid">
            <button
              v-for="decade in decades"
              :key="decade"
              @click="selectedDecade = decade"
              :class="['option-btn', { active: selectedDecade === decade }]"
            >
              {{ decade }}
            </button>
          </div>
        </div>

        <div class="config-group">
          <label>Obscurity</label>
          <div class="obscurity-slider">
            <input
              v-model.number="selectedObscurity"
              type="range"
              min="1"
              max="5"
              class="slider"
            />
            <span class="obscurity-label">{{
              obscurityLabels[selectedObscurity - 1]
            }}</span>
          </div>
        </div>

        <button @click="startGame()" class="start-btn">Start Game</button>
      </div>
    </div>

    <!-- Non-Admin View -->
    <div v-else-if="playerId && !isAdmin" class="waiting-layout">
      <!-- <div class="qr-section-full">
        <h2>Join Game</h2>
        <qrcode-vue :value="gameUrl" size="300"></qrcode-vue>
        <p class="qr-text">Scan to join</p>
      </div> -->

      <div class="waiting-message">
        <p>Waiting for game creator to start...</p>
      </div>
    </div>

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
// import QrcodeVue from 'qrcode.vue';

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
    // QrcodeVue,
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
      genres: ['Pop', 'Rock', 'Hip-Hop', 'Country', 'Electronic', 'Jazz'],
      decades: ['1960s', '1970s', '1980s', '1990s', '2000s', '2010s', '2020s'],
      obscurityLabels: [
        'Very Famous',
        'Famous',
        'Known',
        'Obscure',
        'Very Obscure',
      ],

      selectedGenre: 'Pop',
      selectedDecade: '2000s',
      selectedObscurity: 3,
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

      // Handle the phase transition
      console.log('Phase changed to:', phase);
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
              case 'phaseChange':
                this.startPhaseCountdown(
                  element.newPhase,
                  element.endTimestamp
                );
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
.game-container {
  width: 100%;
  min-height: 100vh;
  padding: 20px;
  background: #f5f5f5;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  color: #333;
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

/* Config Layout - Admin with Configuration */
.config-layout {
  max-width: 800px;
  margin: 0 auto 30px;
}

.qr-section {
  background: white;
  padding: 40px;
  border-radius: 4px;
  border: 1px solid #e0e0e0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.qr-section h2,
.config-section h2 {
  margin-top: 0;
  color: #333;
  font-size: 20px;
  font-weight: 600;
}

.qr-section-full {
  background: white;
  padding: 40px;
  border-radius: 4px;
  border: 1px solid #e0e0e0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  max-width: 400px;
  margin: 50px auto;
}

.qr-text {
  margin-top: 16px;
  font-size: 14px;
  color: #666;
}

.separator {
  width: 1px;
  background: #e0e0e0;
  min-height: 400px;
}

.config-section {
  background: white;
  padding: 40px;
  border-radius: 8px;
  border: 1px solid #ddd;
}

.config-group {
  margin-bottom: 32px;
}

.config-group label {
  display: block;
  font-weight: 600;
  color: #000;
  margin-bottom: 12px;
  font-size: 15px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.option-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.option-btn {
  padding: 10px 14px;
  background: #fff;
  border: 1px solid #ddd;
  border-radius: 3px;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.15s ease;
}

.option-btn:hover {
  background: #f9f9f9;
  border-color: #999;
}

.option-btn.active {
  background: #000;
  color: white;
  border-color: #000;
}

.obscurity-slider {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.slider {
  width: 100%;
  height: 6px;
  border-radius: 3px;
  background: #ddd;
  outline: none;
  -webkit-appearance: none;
}

.slider::-webkit-slider-thumb {
  -webkit-appearance: none;
  appearance: none;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #000;
  cursor: pointer;
}

.slider::-moz-range-thumb {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #000;
  cursor: pointer;
  border: none;
}

.obscurity-label {
  text-align: center;
  color: #333;
  font-weight: 500;
  font-size: 13px;
}

.start-btn {
  width: 100%;
  padding: 12px;
  background: #000;
  color: white;
  border: none;
  border-radius: 3px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.15s ease;
  margin-top: 16px;
}

.start-btn:hover {
  background: #333;
}

/* Waiting Layout - Non-Admin */
.waiting-layout {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 70vh;
}

.waiting-message {
  background: white;
  padding: 40px 60px;
  border-radius: 8px;
  border: 1px solid #ddd;
  text-align: center;
}

.waiting-message p {
  font-size: 18px;
  color: #333;
  margin: 0;
  font-weight: 500;
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

/* Responsive Design */
@media (max-width: 1024px) {
  .config-layout {
    grid-template-columns: 1fr;
  }

  .separator {
    width: 100%;
    height: 2px;
    min-height: auto;
  }

  .option-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 768px) {
  .game-container {
    padding: 10px;
  }

  .config-layout {
    grid-template-columns: 1fr;
    gap: 20px;
  }

  .option-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .qr-section,
  .config-section {
    padding: 20px;
  }

  .waiting-layout {
    gap: 20px;
  }

  .qr-section-full {
    padding: 20px;
    margin: 30px auto;
  }
}
</style>
