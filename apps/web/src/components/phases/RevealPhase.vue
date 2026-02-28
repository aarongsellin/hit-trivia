<template>
  <div class="reveal-phase">
    <div class="reveal-container">
      <h2>The Answer Is...</h2>

      <!-- Music Video Player -->
      <div v-if="track?.musicVideoUrl" class="video-container">
        <video
          ref="videoPlayer"
          :src="track.musicVideoUrl"
          :muted="muted"
          autoplay
          playsinline
          @error="videoError = true"
        ></video>
      </div>

      <!-- Fallback: Album Artwork (if no music video) -->
      <div v-else-if="track?.artworkUrl" class="artwork-container">
        <img :src="track.artworkUrl" :alt="track?.title" class="artwork" />
        <!-- Audio-only fallback -->
        <audio
          v-if="track?.previewUrl"
          ref="audioPlayer"
          :src="track.previewUrl"
          :muted="muted"
          autoplay
        ></audio>
      </div>

      <div class="answer-card">
        <div class="song-title">{{ track?.title || 'Unknown Song' }}</div>
        <div class="artist-name">{{ track?.artist || 'Unknown Artist' }}</div>
        <div class="album-name" v-if="track?.album">{{ track.album }}</div>
      </div>

      <div class="scores">
        <h3>Your Score</h3>

        <!-- Player submitted a guess -->
        <div v-if="guessResult" class="score-breakdown">
          <div
            class="score-row"
            :class="{ correct: guessResult.titleScore > 0 }"
          >
            <span class="score-label">Title</span>
            <span class="score-icon"
              ><svg
                v-if="guessResult.titleScore > 0"
                width="18"
                height="18"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2.5"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <polyline points="20 6 9 17 4 12" /></svg
              ><svg
                v-else
                width="18"
                height="18"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2.5"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <line x1="18" y1="6" x2="6" y2="18" />
                <line x1="6" y1="6" x2="18" y2="18" /></svg
            ></span>
          </div>
          <div
            class="score-row"
            :class="{ correct: guessResult.artistScore > 0 }"
          >
            <span class="score-label">Artist</span>
            <span class="score-icon"
              ><svg
                v-if="guessResult.artistScore > 0"
                width="18"
                height="18"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2.5"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <polyline points="20 6 9 17 4 12" /></svg
              ><svg
                v-else
                width="18"
                height="18"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2.5"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <line x1="18" y1="6" x2="6" y2="18" />
                <line x1="6" y1="6" x2="18" y2="18" /></svg
            ></span>
          </div>
          <div
            class="score-row"
            :class="{ correct: guessResult.albumScore > 0 }"
          >
            <span class="score-label">Album</span>
            <span class="score-icon"
              ><svg
                v-if="guessResult.albumScore > 0"
                width="18"
                height="18"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2.5"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <polyline points="20 6 9 17 4 12" /></svg
              ><svg
                v-else
                width="18"
                height="18"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2.5"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <line x1="18" y1="6" x2="6" y2="18" />
                <line x1="6" y1="6" x2="18" y2="18" /></svg
            ></span>
          </div>
          <div class="score-total">
            <span>Round Score</span>
            <span class="total-value">{{ guessResult.total }} / 3</span>
          </div>
        </div>

        <!-- Player didn't guess -->
        <div v-else class="no-guess">
          <p>You didn't submit a guess this round.</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'RevealPhase',
  props: {
    track: {
      type: Object,
      default: null,
    },
    musicDuration: {
      type: Number,
      default: 0,
    },
    muted: {
      type: Boolean,
      default: false,
    },
    guessResult: {
      type: Object,
      default: null,
    },
  },
  data() {
    return {
      videoError: false,
    };
  },
  watch: {
    muted(val) {
      const video = this.$refs.videoPlayer;
      if (video) video.muted = val;
      const audio = this.$refs.audioPlayer;
      if (audio) audio.muted = val;
    },
  },
  beforeUnmount() {
    const video = this.$refs.videoPlayer;
    if (video) {
      video.pause();
      video.src = '';
    }
    const audio = this.$refs.audioPlayer;
    if (audio) {
      audio.pause();
      audio.src = '';
    }
  },
};
</script>

<style scoped>
.reveal-phase {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 70vh;
}

.reveal-container {
  background: white;
  padding: 50px 60px;
  border-radius: 8px;
  border: 1px solid #ddd;
  text-align: center;
  width: 100%;
  max-width: 700px;
}

.reveal-container h2 {
  margin: 0 0 24px 0;
  color: #333;
  font-size: 24px;
}

.video-container {
  margin-bottom: 24px;
  border-radius: 8px;
  overflow: hidden;
  background: #000;
}

.video-container video {
  width: 100%;
  max-height: 400px;
  display: block;
  border-radius: 8px;
}

.answer-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 32px;
  border-radius: 8px;
  margin-bottom: 32px;
}

.song-title {
  font-size: 28px;
  font-weight: bold;
  color: white;
  margin-bottom: 8px;
}

.artist-name {
  font-size: 20px;
  color: rgba(255, 255, 255, 0.9);
}

.album-name {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.8);
  margin-top: 4px;
  font-style: italic;
}

.artwork-container {
  margin-bottom: 24px;
}

.artwork {
  width: 200px;
  height: 200px;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

audio {
  display: none;
}

.scores {
  margin-top: 24px;
}

.scores h3 {
  margin: 0 0 16px 0;
  color: #333;
  font-size: 18px;
}

.score-breakdown {
  background: #f9f9f9;
  border-radius: 8px;
  padding: 16px 20px;
  text-align: left;
}

.score-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #eee;
  color: #999;
}

.score-row:last-of-type {
  border-bottom: none;
}

.score-row.correct {
  color: #333;
}

.score-label {
  font-size: 15px;
  font-weight: 500;
}

.score-icon {
  font-size: 18px;
  font-weight: 600;
}

.score-row.correct .score-icon {
  color: #4caf50;
}

.score-row:not(.correct) .score-icon {
  color: #ccc;
}

.score-total {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 2px solid #eee;
  font-weight: 600;
  font-size: 16px;
  color: #333;
}

.total-value {
  font-size: 20px;
  color: #667eea;
}

.no-guess {
  background: #f5f5f5;
  padding: 20px;
  border-radius: 8px;
}

.no-guess p {
  color: #999;
  font-size: 14px;
  margin: 0;
}

@media (max-width: 768px) {
  .reveal-container {
    padding: 30px 20px;
  }

  .song-title {
    font-size: 22px;
  }

  .artist-name {
    font-size: 18px;
  }
}
</style>
