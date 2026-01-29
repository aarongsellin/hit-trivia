<template>
  <div class="music-phase">
    <div class="music-container">
      <h2>🎵 Listen Carefully!</h2>

      <!-- Hidden YouTube Player for Audio -->
      <div v-if="youtubeVideoId" class="video-container">
        <iframe
          :src="youtubeEmbedUrl"
          frameborder="0"
          allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
          allowfullscreen
        ></iframe>
      </div>

      <div class="music-visualizer">
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
  computed: {
    youtubeVideoId() {
      if (!this.track?.url) return null;

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

      const startTime = this.track?.startTimeSeconds || 0;
      return `https://www.youtube.com/embed/${this.youtubeVideoId}?autoplay=1&start=${startTime}`;
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

.music-visualizer {
  display: flex;
  align-items: flex-end;
  justify-content: center;
  gap: 8px;
  height: 100px;
  margin-bottom: 32px;
}

.bar {
  width: 12px;
  background: linear-gradient(to top, #2196f3, #1976d2);
  border-radius: 4px;
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
