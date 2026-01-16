<template>
  <p>Game View - Status: {{ status }}</p>

  <div class="send">
    <input v-model="inputMessage" />

    <button @click="sendMessage">Send</button>
  </div>

  <div class="messages">
    <div v-for="msg in messages" :key="msg.id">
      {{ msg.sender }}: {{ msg.text }}
    </div>
  </div>
</template>

<script>
import { useRoute } from 'vue-router';
import { useWebSocket } from '@vueuse/core';
import { ref, watch } from 'vue';

export default {
  setup() {
    const route = useRoute();
    const gameId = route.query.id;
    const messages = ref([]);
    const inputMessage = ref('');
    let messageCount = 0;

    const apiUrl = process.env.VUE_APP_API_URL.replace('http://', '').replace(
      'https://',
      ''
    );

    const { status, open, data, send } = useWebSocket(
      `ws://${apiUrl}/ws/game/${gameId}`
    );

    const handleMessage = () => {
      if (data.value) {
        messages.value.push({
          id: messageCount++,
          text: data.value,
          sender: 'Other User',
        });
      }
    };

    const sendMessage = () => {
      if (inputMessage.value.trim()) {
        send(inputMessage.value);
        messages.value.push({
          id: messageCount++,
          text: inputMessage.value,
          sender: 'You',
        });

        inputMessage.value = '';
      }
    };

    watch(data, handleMessage);

    open();

    return { status, messages, sendMessage, inputMessage };
  },
};
</script>
