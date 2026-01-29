<template>
  <div class="reveal-phase">
    <div class="reveal-container">
      <h2>The Answer Is...</h2>
      <div class="answer-card">
        <div class="song-title">{{ track?.title || 'Unknown Song' }}</div>
        <div class="artist-name">{{ track?.artist || 'Unknown Artist' }}</div>
        <div class="album-name" v-if="track?.album">{{ track.album }}</div>
      </div>

      <!-- YouTube Player -->
      <div v-if="youtubeVideoId" class="video-container">
        <iframe
          :src="youtubeEmbedUrl"
          frameborder="0"
          allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
          allowfullscreen
        ></iframe>
      </div>

      <div class="scores">
        <h3>Scores This Round</h3>
        <div class="score-list">
          <!-- Scores will be populated here -->
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
  computed: {
    youtubeVideoId() {
      if (!this.track?.url) return null;

      // Extract video ID from various YouTube URL formats
      const url = this.track.url;

      // Match youtube.com/watch?v=VIDEO_ID
      const watchMatch = url.match(/[?&]v=([^&]+)/);
      if (watchMatch) return watchMatch[1];

      // Match youtu.be/VIDEO_ID
      const shortMatch = url.match(/youtu\.be\/([^?]+)/);
      if (shortMatch) return shortMatch[1];

      // Match youtube.com/embed/VIDEO_ID
      const embedMatch = url.match(/\/embed\/([^?]+)/);
      if (embedMatch) return embedMatch[1];

      return null;
    },
    youtubeEmbedUrl() {
      if (!this.youtubeVideoId) return null;

      // Start where the music left off: original start time + music duration
      const startTime =
        (this.track?.startTimeSeconds || 0) + this.musicDuration;
      return `https://www.youtube.com/embed/${this.youtubeVideoId}?autoplay=1&start=${startTime}`;
    },
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
  max-width: 600px;
}

.reveal-container h2 {
  margin: 0 0 24px 0;
  color: #333;
  font-size: 24px;
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

.video-container {
  margin-bottom: 32px;
  border-radius: 8px;
  overflow: hidden;
  background: #000;
}

.video-container iframe {
  width: 100%;
  height: 315px;
  display: block;
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
