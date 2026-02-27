<template>
  <div class="music-phase" @click="resumeIfBlocked">
    <div class="music-container">
      <h2>🎵 Listen Carefully!</h2>

      <!-- Apple Music Preview Audio -->
      <audio
        ref="audioPlayer"
        :src="track?.previewUrl"
        :muted="muted"
        autoplay
        @error="onAudioError"
        @playing="onPlaying"
        @ended="isPlaying = false"
      ></audio>

      <div class="music-visualizer" :class="{ playing: isPlaying && !muted }">
        <div class="bar" v-for="i in 8" :key="i"></div>
      </div>
      <p v-if="muted" class="instruction muted-hint">
        🔇 Music is muted — tap the speaker button to listen
      </p>
      <p v-else-if="autoplayBlocked" class="instruction tap-hint">
        Tap anywhere to play music
      </p>
      <p v-else class="instruction">Guess the song...</p>
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
    seekOffset: {
      type: Number,
      default: 0,
    },
    muted: {
      type: Boolean,
      default: false,
    },
  },
  data() {
    return {
      isPlaying: false,
      audioError: false,
      autoplayBlocked: false,
      hasSeeked: false,
    };
  },
  watch: {
    // When track changes AFTER mount (e.g. next round), the :src binding
    // updates and autoplay kicks in. Just reset state.
    track(newTrack) {
      if (newTrack?.previewUrl) {
        this.audioError = false;
        this.autoplayBlocked = false;
        this.hasSeeked = false;
      }
    },
  },
  mounted() {
    // After a short delay, check if autoplay was silently blocked
    this._autoplayCheck = setTimeout(() => {
      const audio = this.$refs.audioPlayer;
      if (audio && audio.paused && this.track?.previewUrl) {
        // autoplay didn't fire — try manually
        audio
          .play()
          .then(() => {
            this.isPlaying = true;
            this.seekToOffset();
          })
          .catch((err) => {
            if (err.name === 'NotAllowedError') {
              this.autoplayBlocked = true;
            }
          });
      }
    }, 500);

    // Listen for any click/tap on the document to unlock audio
    this._docClickHandler = () => {
      if (this.autoplayBlocked) {
        const audio = this.$refs.audioPlayer;
        if (audio) {
          audio
            .play()
            .then(() => {
              this.autoplayBlocked = false;
              this.isPlaying = true;
              this.seekToOffset();
            })
            .catch(() => {});
        }
      }
    };
    document.addEventListener('click', this._docClickHandler);
    document.addEventListener('touchstart', this._docClickHandler);
  },
  beforeUnmount() {
    if (this._autoplayCheck) {
      clearTimeout(this._autoplayCheck);
    }
    const audio = this.$refs.audioPlayer;
    if (audio) {
      audio.pause();
      audio.src = '';
    }
    document.removeEventListener('click', this._docClickHandler);
    document.removeEventListener('touchstart', this._docClickHandler);
  },
  methods: {
    seekToOffset() {
      if (this.hasSeeked || this.seekOffset <= 0) return;
      const audio = this.$refs.audioPlayer;
      if (!audio || !audio.duration) return;
      const target = Math.min(this.seekOffset, audio.duration - 0.5);
      if (target > 0) {
        audio.currentTime = target;
      }
      this.hasSeeked = true;
    },
    resumeIfBlocked() {
      if (!this.autoplayBlocked) return;
      const audio = this.$refs.audioPlayer;
      if (audio) {
        audio
          .play()
          .then(() => {
            this.autoplayBlocked = false;
            this.isPlaying = true;
            this.seekToOffset();
          })
          .catch(() => {});
      }
    },
    onPlaying() {
      this.isPlaying = true;
      this.autoplayBlocked = false;
      this.seekToOffset();
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

.tap-hint {
  color: #2196f3;
  font-weight: 600;
  animation: pulse-text 1.5s ease-in-out infinite;
  cursor: pointer;
}

.muted-hint {
  color: #9ca3af;
  font-size: 14px;
}

@keyframes pulse-text {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
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
