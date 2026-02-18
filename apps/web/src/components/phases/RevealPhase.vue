<template>
  <div class="reveal-phase">
    <div class="reveal-container">
      <h2>The Answer Is...</h2>

      <!-- Music Video Player -->
      <div v-if="track?.musicVideoUrl" class="video-container">
        <video
          ref="videoPlayer"
          :src="track.musicVideoUrl"
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
          autoplay
        ></audio>
      </div>

      <div class="answer-card">
        <div class="song-title">{{ track?.title || 'Unknown Song' }}</div>
        <div class="artist-name">{{ track?.artist || 'Unknown Artist' }}</div>
        <div class="album-name" v-if="track?.album">{{ track.album }}</div>
      </div>

      <div class="scores">
        <h3>Scores This Round</h3>
        <div class="score-list">
          <p class="placeholder">Calculating scores...</p>
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
  },
  data() {
    return {
      videoError: false,
    };
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

.score-list {
  background: #f5f5f5;
  padding: 16px;
  border-radius: 4px;
}

.placeholder {
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
