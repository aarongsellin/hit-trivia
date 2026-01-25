<template>
  <div>
    <h1>Welcome to Hit Trivia!</h1>
    <button @click="startGame">Start a game</button>
    <p>Games started this far: {{ totalGameCount }}</p>
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
