<template>
  <div class="waiting-replay-layout">
    <div v-if="isAdmin" class="admin-container">
      <h2>Låten är klar!</h2>
      <p class="subtitle">Vad vill du göra?</p>
      <div class="btn-row">
        <button class="btn btn-replay" @click="$emit('replay')">
          <svg
            width="20"
            height="20"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2.5"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <polyline points="1 4 1 10 7 10" />
            <path d="M3.51 15a9 9 0 1 0 .49-3.5" />
          </svg>
          Spela igen
        </button>
        <button class="btn btn-continue" @click="$emit('continue')">
          Gå vidare till avslöjning
          <svg
            width="20"
            height="20"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2.5"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <polyline points="9 18 15 12 9 6" />
          </svg>
        </button>
      </div>
    </div>

    <div v-else class="waiting-message">
      <p>Väntar på host...</p>
    </div>
  </div>
</template>

<script>
export default {
  name: 'WaitingReplayPhase',
  props: {
    isAdmin: {
      type: Boolean,
      default: false,
    },
  },
  emits: ['replay', 'continue'],
};
</script>

<style scoped>
.waiting-replay-layout {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 70vh;
}

.admin-container {
  background: white;
  padding: 50px 60px;
  border-radius: 8px;
  border: 1px solid #ddd;
  text-align: center;
  width: 100%;
  max-width: 500px;
}

.admin-container h2 {
  margin: 0 0 8px 0;
  font-size: 26px;
  font-weight: 700;
  color: #1a1a1a;
}

.subtitle {
  color: #9ca3af;
  font-size: 15px;
  margin: 0 0 32px 0;
}

.btn-row {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 16px 24px;
  border-radius: 10px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  border: none;
  transition: all 0.15s ease;
}

.btn:active {
  transform: scale(0.97);
}

.btn-replay {
  background: #f3f4f6;
  color: #374151;
  border: 1px solid #e5e7eb;
}

.btn-replay:hover {
  background: #e5e7eb;
}

.btn-continue {
  background: #e11d48;
  color: white;
}

.btn-continue:hover {
  background: #be123c;
}

.waiting-message {
  background: white;
  padding: 40px 60px;
  border-radius: 8px;
  border: 1px solid #ddd;
  text-align: center;
}

.waiting-message p {
  font-size: 18px;
  color: #333;
  margin: 0;
  font-weight: 500;
}

@media (max-width: 768px) {
  .admin-container {
    padding: 36px 24px;
  }

  .waiting-message {
    padding: 30px 40px;
  }
}
</style>
