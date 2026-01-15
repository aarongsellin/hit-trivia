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
  methods: {
    async startGame() {
      const res = await fetch(`${this.apiUrl}/api/new-game`);
      const { id } = await res.json();
      this.router.push({ path: `/game`, query: { id } });
    },
  },
};
</script>
