<template>
  <div class="guessing-phase">
    <div class="guessing-container">
      <h2>{{ submitted ? 'Guess Locked In' : "What's the song?" }}</h2>
      <p v-if="!submitted" class="guess-hint">
        You can guess the <strong>title</strong>, <strong>artist</strong>, or
        <strong>album</strong>
      </p>

      <div
        class="input-wrapper"
        :class="{ 'input-wrapper--locked': submitted }"
      >
        <div v-if="submitted" class="lock-icon">✓</div>
        <input
          ref="guessInput"
          v-model="guess"
          @keyup.enter="submitGuess"
          type="text"
          :placeholder="submitted ? '' : 'Song title, artist, or album...'"
          class="guess-input"
          :class="{ 'guess-input--locked': submitted }"
          :readonly="submitted"
          autofocus
        />
      </div>

      <p class="hint" v-if="!submitted">Press Enter to lock in your guess</p>
      <p class="hint hint--locked" v-else>Waiting for reveal...</p>
    </div>
  </div>
</template>

<script>
export default {
  name: 'GuessingPhase',
  props: {
    gameId: {
      type: String,
      default: null,
    },
    currentRound: {
      type: Number,
      default: 0,
    },
  },
  data() {
    return {
      guess: '',
      submitted: false,
    };
  },
  computed: {
    storageKey() {
      return `guess_${this.gameId}_${this.currentRound}`;
    },
  },
  mounted() {
    // Restore guess from localStorage on reload
    const saved = localStorage.getItem(this.storageKey);
    if (saved) {
      this.guess = saved;
      this.submitted = true;
    } else {
      this.$refs.guessInput?.focus();
    }
  },
  methods: {
    submitGuess() {
      if (this.guess.trim() && !this.submitted) {
        this.submitted = true;
        localStorage.setItem(this.storageKey, this.guess.trim());
        this.$emit('submit-guess', this.guess.trim());
      }
    },
  },
};
</script>

<style scoped>
.guessing-phase {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 70vh;
}

.guessing-container {
  background: white;
  padding: 50px 60px;
  border-radius: 8px;
  border: 1px solid #ddd;
  text-align: center;
  width: 100%;
  max-width: 560px;
}

.guessing-container h2 {
  margin: 0 0 8px 0;
  color: #1a1a1a;
  font-size: 24px;
  font-weight: 700;
}

.guess-hint {
  color: #9ca3af;
  font-size: 14px;
  margin: 0 0 24px 0;
}

.guess-hint strong {
  color: #6b7280;
}

.input-wrapper {
  position: relative;
  margin-bottom: 16px;
  transition: all 0.3s ease;
}

.input-wrapper--locked {
  background: #f0fdf4;
  border-radius: 12px;
}

.lock-icon {
  position: absolute;
  left: 16px;
  top: 50%;
  transform: translateY(-50%);
  width: 24px;
  height: 24px;
  background: #22c55e;
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  z-index: 1;
}

.guess-input {
  width: 100%;
  padding: 16px 20px;
  border: 2px solid #e5e7eb;
  border-radius: 12px;
  font-size: 18px;
  font-weight: 500;
  color: #1a1a1a;
  background: #fafafa;
  transition: all 0.2s ease;
  box-sizing: border-box;
  text-align: center;
}

.guess-input::placeholder {
  color: #9ca3af;
  font-weight: 400;
}

.guess-input:focus {
  outline: none;
  border-color: #1a1a1a;
  background: white;
  box-shadow: 0 0 0 4px rgba(0, 0, 0, 0.05);
}

.guess-input--locked {
  border-color: #22c55e;
  background: transparent;
  color: #166534;
  padding-left: 48px;
  text-align: left;
  cursor: default;
}

.guess-input--locked:focus {
  border-color: #22c55e;
  box-shadow: none;
}

.hint {
  font-size: 14px;
  color: #9ca3af;
  margin: 0;
}

.hint--locked {
  color: #22c55e;
  font-weight: 500;
}

@media (max-width: 768px) {
  .guessing-container {
    padding: 30px 20px;
  }

  .guess-input {
    font-size: 16px;
    padding: 14px 16px;
  }

  .guess-input--locked {
    padding-left: 44px;
  }
}
</style>
