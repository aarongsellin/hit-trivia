<template>
  <div class="finished-phase">
    <div class="finished-container">
      <h2>🏆 Game Over!</h2>
      <div class="final-scores">
        <h3>Final Scores</h3>
        <div class="podium">
          <!-- Final leaderboard will go here -->
          <p class="placeholder">Calculating final results...</p>
        </div>
      </div>
      <a href="/"> <button class="play-again-btn">Play Again</button></a>
    </div>
  </div>
</template>

<script>
export default {
  name: 'FinishedPhase',
  emits: ['play-again'],
};
</script>

<style scoped>
.finished-phase {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 70vh;
}

.finished-container {
  background: white;
  padding: 60px 80px;
  border-radius: 8px;
  border: 1px solid #ddd;
  text-align: center;
  width: 100%;
  max-width: 700px;
}

.finished-container h2 {
  margin: 0 0 32px 0;
  color: #333;
  font-size: 32px;
}

.final-scores {
  margin-bottom: 32px;
}

.final-scores h3 {
  margin: 0 0 20px 0;
  color: #333;
  font-size: 20px;
}

.podium {
  background: #f5f5f5;
  padding: 24px;
  border-radius: 8px;
  min-height: 150px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.placeholder {
  color: #999;
  font-size: 16px;
  margin: 0;
}

.play-again-btn {
  padding: 14px 40px;
  background: #000;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.15s ease;
}

.play-again-btn:hover {
  background: #333;
}

@media (max-width: 768px) {
  .finished-container {
    padding: 40px 30px;
  }

  .finished-container h2 {
    font-size: 26px;
  }
}
</style>
