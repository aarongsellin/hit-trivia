<template>
  <div class="music-phase">
    <div class="music-container">
      <h2>🎵 Listen Carefully!</h2>

      <!-- Apple Music Preview Audio -->
      <audio
        ref="audioPlayer"
        :src="track?.previewUrl"
        autoplay
        @error="onAudioError"
        @playing="onPlaying"
        @ended="isPlaying = false"
      ></audio>

      <!-- Tap-to-play overlay when autoplay is blocked (e.g. after page reload) -->
      <button
        v-if="autoplayBlocked"
        class="tap-to-play"
        @click="resumePlayback"
      >
        <span class="tap-icon">▶</span>
        <span>Tap to play music</span>
      </button>

      <div v-else class="music-visualizer" :class="{ playing: isPlaying }">
        <div class="bar" v-for="i in 8" :key="i"></div>
      </div>
      <p class="instruction">Guess the song...</p>
    </div>
  </div>
</template>

<script>
export default {
  name: 'PlayingMusicPhase',
  props: {
    track: {
      type: Object,
      default: null,
    },
  },
  data() {
    return {
      isPlaying: false,
      audioError: false,
      autoplayBlocked: false,
    };
  },
  watch: {
    track: {
      handler(newTrack) {
        if (newTrack?.previewUrl) {
          this.audioError = false;
          this.autoplayBlocked = false;
          this.$nextTick(() => {
            this.tryPlay();
          });
        }
      },
      immediate: true,
    },
  },
  beforeUnmount() {
    const audio = this.$refs.audioPlayer;
    if (audio) {
      audio.pause();
      audio.src = '';
    }
  },
  methods: {
    tryPlay() {
      const audio = this.$refs.audioPlayer;
      if (!audio) return;
      audio.load();
      audio.play().catch((err) => {
        if (err.name === 'NotAllowedError') {
          // Browser blocked autoplay — show tap-to-play button
          this.autoplayBlocked = true;
        } else {
          console.error('Audio playback failed:', err);
          this.audioError = true;
        }
      });
    },
    resumePlayback() {
      this.autoplayBlocked = false;
      this.$nextTick(() => {
        const audio = this.$refs.audioPlayer;
        if (audio) {
          audio.play().catch((err) => {
            console.error('Audio playback failed after tap:', err);
            this.audioError = true;
          });
        }
      });
    },
    onPlaying() {
      this.isPlaying = true;
      this.autoplayBlocked = false;
    },
    onAudioError(e) {
      console.error('Audio error:', e);
      this.audioError = true;
    },
  },
};
</script>

<style scoped>
.music-phase {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 70vh;
}

.music-container {
  background: white;
  padding: 60px 80px;
  border-radius: 8px;
  border: 1px solid #ddd;
  text-align: center;
}

.music-container h2 {
  margin: 0 0 32px 0;
  color: #333;
  font-size: 28px;
}

audio {
  display: none;
}

.music-visualizer {
  display: flex;
  align-items: flex-end;
  justify-content: center;
  gap: 8px;
  height: 100px;
  margin-bottom: 32px;
}

.music-visualizer .bar {
  width: 12px;
  background: #ccc;
  border-radius: 4px;
  height: 20px;
  transition: background 0.3s;
}

.music-visualizer.playing .bar {
  background: linear-gradient(to top, #2196f3, #1976d2);
  animation: bounce 0.8s ease-in-out infinite;
}

.bar:nth-child(1) {
  animation-delay: 0s;
}
.bar:nth-child(2) {
  animation-delay: 0.1s;
}
.bar:nth-child(3) {
  animation-delay: 0.2s;
}
.bar:nth-child(4) {
  animation-delay: 0.3s;
}
.bar:nth-child(5) {
  animation-delay: 0.4s;
}
.bar:nth-child(6) {
  animation-delay: 0.5s;
}
.bar:nth-child(7) {
  animation-delay: 0.6s;
}
.bar:nth-child(8) {
  animation-delay: 0.7s;
}

@keyframes bounce {
  0%,
  100% {
    height: 20px;
  }
  50% {
    height: 80px;
  }
}

.instruction {
  font-size: 16px;
  color: #666;
  margin: 0;
}

.tap-to-play {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 24px 40px;
  margin-bottom: 32px;
  background: #f5f5f5;
  border: 2px dashed #bbb;
  border-radius: 12px;
  cursor: pointer;
  font-size: 16px;
  color: #555;
  transition: all 0.2s;
}

.tap-to-play:hover {
  background: #e8f4fd;
  border-color: #2196f3;
  color: #1976d2;
}

.tap-icon {
  font-size: 36px;
  line-height: 1;
}

.audio-error {
  color: #e53935;
  font-size: 14px;
  margin-top: 12px;
}

@media (max-width: 768px) {
  .music-container {
    padding: 40px 30px;
  }

  .music-container h2 {
    font-size: 24px;
  }

  .music-visualizer {
    height: 80px;
  }
}
</style>
