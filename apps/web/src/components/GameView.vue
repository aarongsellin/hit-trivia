<template>
  <div>
    <div class="status">
      <p>Game View - Status: {{ socket.status }}</p>

      <p>
        Player ID: <b>{{ playerId }}</b>
      </p>
    </div>

    <div>
      <button @click="sendMessage(inputMessage)">Send</button>
      <button @click="clearStorage()">CLEAR</button>
      <button @click="socket.close()">CLOSE</button>
      <button @click="socket.open()">OPEN</button>
      <button @click="test()">JOIN</button>
    </div>

    <div class="send">
      <input v-model="inputMessage" />
    </div>

    <div class="messages">
      <div v-for="(msg, index) in messages" :key="index">
        {{ msg.sender }}: {{ msg.text }}
      </div>
    </div>
  </div>
</template>

<script>
import { useRoute } from 'vue-router';
import { useWebSocket } from '@vueuse/core';

const isJson = (data) => {
  try {
    JSON.parse(data);
    return true;
  } catch (err) {
    return false;
  }
};

export default {
  data() {
    return {
      messages: [],
      inputMessage: '',
      playerId: localStorage.getItem('playerId') || null,
      socket: null,
      test: this.handlePlayerJoin,
    };
  },
  created: function () {
    const apiUrl = process.env.VUE_APP_API_URL.replace('http://', '').replace(
      'https://',
      ''
    );

    const route = useRoute();
    const gameId = route.query.id;

    this.socket = useWebSocket(`ws://${apiUrl}/ws/game/${gameId}`, {
      autoReconnect: true,
    });

    this.socket.open();
  },
  watch: {
    'socket.data': function () {
      this.handleMessage(this.socket.data);
    },
  },
  methods: {
    clearStorage() {
      this.playerId = null;
      localStorage.setItem('playerId', null);
    },
    handlePlayerJoin() {
      const toSend = JSON.stringify({
        type: 'playerId',
        data: { playerId: localStorage.getItem('playerId') || null },
      });

      this.socket.send(toSend);
    },
    handleLeave() {
      this.socket.send(
        JSON.stringify({
          type: 'data',
          action: 'leave',
        })
      );
    },
    handleMessage(data) {
      this.messages.push({
        text: data,
        sender: 'Server',
      });

      if (isJson(data)) {
        const parsed = JSON.parse(data);
        const type = parsed?.type;

        if (type === 'data') {
          const keys = Object.keys(parsed);

          for (let i = 1; i < Object.keys(parsed).length; ++i) {
            const key = keys.at(i);
            const element = parsed[key];

            switch (key) {
              case 'playerId':
                this.playerId = element;
                localStorage.setItem('playerId', this.playerId);
                break;
              default:
                console.log('Unknown data contents', { key });
            }
          }
        }

        if (type === 'error') {
          console.log('received erro');
        }
      }
    },
    sendMessage(message) {
      if (this.socket.status === 'CLOSED') {
        console.log('Socket is closed, can not send!');

        return;
      }

      this.socket.send(message);

      this.messages.push({
        text: message,
        sender: 'Me',
      });

      this.inputMessage = '';
    },
  },
  /**
   * Vad händer om en användare avbryter ett spel, och aldrig kommer tillbaka?
   *
   * Sedan går de med i ett spel i framtiden och har cachat sitt gamla playerId?
   * Servern måste cross-checka att spelarens ID faktiskt finns med i spelet.
   * Om det inte gör det bör de få ett nytt id.
   */
};
</script>
