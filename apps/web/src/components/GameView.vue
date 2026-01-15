<template>
  <p>Game View {{ gameId }}</p>
</template>

<script>
import { useRoute } from 'vue-router';
export default {
  data() {
    const route = useRoute();
    return { gameId: route.query.id };
  },
};
</script>
