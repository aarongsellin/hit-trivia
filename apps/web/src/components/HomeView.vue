<template>
  <div class="home-container">
    <div class="content">
      <h1>Hit Trivia</h1>
      <p class="subtitle">Guess the song</p>

      <button @click="startGame" class="start-button">Start Game</button>

      <div class="stats">{{ totalGameCount }} games played</div>
    </div>
  </div>
</template>

<script>
import { useRouter } from 'vue-router';

export default {
  setup() {
    const router = useRouter();
    return { router };
  },
  data() {
    return {
      totalGameCount: 0,
      apiUrl: process.env.VUE_APP_API_URL,
    };
  },
  mounted() {
    this.fetchGameCount();
  },
  methods: {
    async fetchGameCount() {
      try {
        const res = await fetch(`${this.apiUrl}/api/game-count`);
        const data = await res.json();
        this.totalGameCount = data.count;
      } catch (err) {
        console.error('Error fetching game count:', err);
      }
    },
    async startGame() {
      const res = await fetch(`${this.apiUrl}/api/new-game`);
      const resJson = await res.json();
      const id = resJson.data.id;

      if (id) this.router.push({ path: `/game`, query: { id } });
    },
  },
};
</script>

<style scoped>
.home-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: white;
  padding: 20px;
}

.content {
  text-align: center;
  max-width: 400px;
}

h1 {
  font-size: 48px;
  font-weight: 700;
  color: #000;
  margin: 0 0 8px 0;
  letter-spacing: -1px;
}

.subtitle {
  font-size: 18px;
  color: #666;
  margin: 0 0 40px 0;
  font-weight: 400;
}

.start-button {
  padding: 14px 48px;
  font-size: 16px;
  font-weight: 500;
  color: white;
  background: #000;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: background 0.15s ease;
}

.start-button:hover {
  background: #333;
}

.start-button:active {
  background: #000;
}

.stats {
  margin-top: 40px;
  font-size: 14px;
  color: #999;
}

@media (max-width: 768px) {
  h1 {
    font-size: 36px;
  }

  .subtitle {
    font-size: 16px;
  }
}
</style>
