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
        @playing="isPlaying = true"
        @ended="isPlaying = false"
      ></audio>

      <div class="music-visualizer" :class="{ playing: isPlaying }">
        <div class="bar" v-for="i in 8" :key="i"></div>
      </div>
      <p class="instruction">Guess the song...</p>
      <p v-if="audioError" class="audio-error">
        ⚠️ Could not play audio preview
      </p>
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
    };
  },
  watch: {
    track: {
      handler(newTrack) {
        if (newTrack?.previewUrl) {
          this.audioError = false;
          this.$nextTick(() => {
            const audio = this.$refs.audioPlayer;
            if (audio) {
              audio.load();
              audio.play().catch((err) => {
                console.error('Audio playback failed:', err);
                this.audioError = true;
              });
            }
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
