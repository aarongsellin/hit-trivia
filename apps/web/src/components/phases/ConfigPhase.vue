<template>
  <div class="config-layout">
    <!-- Loading overlay -->
    <div v-if="isLoading" class="loading-overlay">
      <div class="spinner-container">
        <div class="spinner"></div>
        <p>Loading tracks from Apple Music...</p>
      </div>
    </div>

    <div v-else class="split-screen">
      <!-- Left: Game Configuration -->
      <div class="config-section">
        <h2>Game Configuration</h2>

        <div class="config-group">
          <label>Genre</label>
          <div class="option-grid">
            <button
              v-for="genre in genres"
              :key="genre"
              @click="$emit('update:genre', genre)"
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
              @click="$emit('update:decade', decade)"
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
              :value="selectedObscurity"
              @input="$emit('update:obscurity', Number($event.target.value))"
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

        <div class="config-group">
          <label>Rounds</label>
          <div class="obscurity-slider">
            <input
              :value="selectedRounds"
              @input="$emit('update:rounds', Number($event.target.value))"
              type="range"
              min="1"
              max="10"
              class="slider"
            />
            <span class="obscurity-label"
              >{{ selectedRounds }}
              {{ selectedRounds === 1 ? 'round' : 'rounds' }}</span
            >
          </div>
        </div>

        <button @click="handleStart" class="start-btn">Start Game</button>

        <div class="host-music-hint">
          <span class="hint-icon">🔊</span>
          <span
            >Music plays on your device by default. Other players are muted —
            they can unmute with the speaker button.</span
          >
        </div>
      </div>

      <!-- Right: QR Code & Join Info -->
      <div class="join-section">
        <h2>Join the Game</h2>
        <div class="qr-wrapper">
          <QRCodeVue :value="gameUrl" :size="280" level="M" />
        </div>
        <p class="join-label">Scan to join</p>
        <div class="join-url-box">
          <span class="join-url">{{ gameUrl }}</span>
          <button class="copy-btn" @click="copyUrl">{{ copyLabel }}</button>
        </div>
        <p class="player-count" v-if="playerCount !== null">
          {{ playerCount }} player{{ playerCount !== 1 ? 's' : '' }} connected
        </p>
      </div>
    </div>
  </div>
</template>

<script>
import QRCodeVue from 'qrcode.vue';

export default {
  name: 'ConfigPhase',
  components: { QRCodeVue },
  props: {
    selectedGenre: String,
    selectedDecade: String,
    selectedObscurity: Number,
    selectedRounds: Number,
    gameUrl: {
      type: String,
      default: '',
    },
    playerCount: {
      type: Number,
      default: null,
    },
  },
  emits: [
    'update:genre',
    'update:decade',
    'update:obscurity',
    'update:rounds',
    'start-game',
  ],
  data() {
    return {
      genres: ['Pop', 'Rock', 'Hip-Hop', 'Country', 'Electronic', 'Jazz'],
      decades: ['1960s', '1970s', '1980s', '1990s', '2000s', '2010s', '2020s'],
      obscurityLabels: [
        'Very Famous',
        'Famous',
        'Known',
        'Obscure',
        'Very Obscure',
      ],
      isLoading: false,
      copyLabel: 'Copy',
    };
  },
  methods: {
    handleStart() {
      this.isLoading = true;
      this.$emit('start-game');
    },
    async copyUrl() {
      try {
        await navigator.clipboard.writeText(this.gameUrl);
        this.copyLabel = 'Copied!';
        setTimeout(() => {
          this.copyLabel = 'Copy';
        }, 2000);
      } catch {
        this.copyLabel = 'Failed';
      }
    },
  },
};
</script>

<style scoped>
.config-layout {
  width: 100%;
}

/* Loading overlay */
.loading-overlay {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 70vh;
}

.spinner-container {
  text-align: center;
}

.spinner {
  width: 48px;
  height: 48px;
  border: 4px solid #e0e0e0;
  border-top-color: #000;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin: 0 auto 20px;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.spinner-container p {
  color: #666;
  font-size: 16px;
  margin: 0;
}

/* Split screen */
.split-screen {
  display: flex;
  gap: 32px;
  max-width: 1100px;
  margin: 0 auto;
  align-items: flex-start;
}

.config-section {
  flex: 1;
  background: white;
  padding: 40px;
  border-radius: 8px;
  border: 1px solid #ddd;
}

.join-section {
  width: 360px;
  flex-shrink: 0;
  background: white;
  padding: 40px;
  border-radius: 8px;
  border: 1px solid #ddd;
  text-align: center;
}

.join-section h2 {
  margin: 0 0 24px 0;
  color: #333;
  font-size: 20px;
}

.qr-wrapper {
  display: flex;
  justify-content: center;
  margin-bottom: 16px;
}

.join-label {
  color: #999;
  font-size: 14px;
  margin: 0 0 20px;
}

.join-url-box {
  display: flex;
  align-items: center;
  gap: 8px;
  background: #f5f5f5;
  padding: 10px 12px;
  border-radius: 4px;
  margin-bottom: 16px;
}

.join-url {
  flex: 1;
  font-size: 12px;
  color: #666;
  word-break: break-all;
  text-align: left;
}

.copy-btn {
  padding: 6px 14px;
  background: #000;
  color: white;
  border: none;
  border-radius: 3px;
  font-size: 12px;
  cursor: pointer;
  flex-shrink: 0;
  transition: background 0.15s ease;
}

.copy-btn:hover {
  background: #333;
}

.player-count {
  color: #4caf50;
  font-size: 14px;
  font-weight: 500;
  margin: 0;
}

.config-section h2 {
  margin: 0 0 28px 0;
  color: #333;
  font-size: 20px;
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

.host-music-hint {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-top: 20px;
  padding: 14px 16px;
  background: #f0f9ff;
  border: 1px solid #bae6fd;
  border-radius: 8px;
  font-size: 13px;
  color: #475569;
  line-height: 1.5;
}

.hint-icon {
  font-size: 18px;
  flex-shrink: 0;
  margin-top: 1px;
}

@media (max-width: 768px) {
  .split-screen {
    flex-direction: column-reverse;
  }

  .join-section {
    width: 100%;
  }

  .config-section {
    padding: 20px;
  }

  .option-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
