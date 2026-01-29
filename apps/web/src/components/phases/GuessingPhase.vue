<template>
  <div class="guessing-phase">
    <div class="guessing-container">
      <h2>Make Your Guess!</h2>
      <div class="input-group">
        <input
          v-model="guess"
          @keyup.enter="submitGuess"
          type="text"
          placeholder="Enter song title or artist..."
          class="guess-input"
          autofocus
        />
        <button @click="submitGuess" class="submit-btn">Submit</button>
      </div>
      <p class="hint">Type your answer and press Enter or Submit</p>
    </div>
  </div>
</template>

<script>
export default {
  name: 'GuessingPhase',
  data() {
    return {
      guess: '',
    };
  },
  methods: {
    submitGuess() {
      if (this.guess.trim()) {
        this.$emit('submit-guess', this.guess.trim());
        this.guess = '';
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
  max-width: 600px;
}

.guessing-container h2 {
  margin: 0 0 24px 0;
  color: #333;
  font-size: 26px;
}

.input-group {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.guess-input {
  flex: 1;
  padding: 12px 16px;
  border: 2px solid #ddd;
  border-radius: 4px;
  font-size: 16px;
  transition: border-color 0.2s;
}

.guess-input:focus {
  outline: none;
  border-color: #2196f3;
}

.submit-btn {
  padding: 12px 28px;
  background: #000;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.15s ease;
}

.submit-btn:hover {
  background: #333;
}

.hint {
  font-size: 14px;
  color: #999;
  margin: 0;
}

@media (max-width: 768px) {
  .guessing-container {
    padding: 30px 20px;
  }

  .input-group {
    flex-direction: column;
  }

  .submit-btn {
    width: 100%;
  }
}
</style>
