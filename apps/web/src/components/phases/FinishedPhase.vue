<template>
  <div class="finished-phase">
    <div class="finished-container">
      <div class="trophy">🏆</div>
      <h2>Game Over!</h2>

      <div v-if="finalScores" class="final-scores">
        <div class="scores-header">
          <h3>Final Scores</h3>
          <span class="rounds-label">{{ finalScores.totalRounds }} rounds</span>
        </div>

        <div class="scoreboard">
          <div
            v-for="entry in finalScores.scoreboard"
            :key="entry.playerId"
            class="score-entry"
            :class="{
              'score-entry--you': entry.playerId === playerId,
              'score-entry--first': entry.rank === 1,
              'score-entry--second': entry.rank === 2,
              'score-entry--third': entry.rank === 3,
            }"
          >
            <div class="score-rank">
              <span v-if="entry.rank === 1" class="rank-medal">🥇</span>
              <span v-else-if="entry.rank === 2" class="rank-medal">🥈</span>
              <span v-else-if="entry.rank === 3" class="rank-medal">🥉</span>
              <span v-else class="rank-number">#{{ entry.rank }}</span>
            </div>

            <div class="score-name">
              {{ entry.playerId === playerId ? 'You' : entry.name }}
              <span v-if="entry.playerId === playerId" class="you-badge"
                >★</span
              >
            </div>

            <div class="score-bar-container">
              <div
                class="score-bar"
                :style="{
                  width:
                    finalScores.maxScore > 0
                      ? (entry.score / finalScores.maxScore) * 100 + '%'
                      : '0%',
                }"
              ></div>
            </div>

            <div class="score-value">
              {{ entry.score
              }}<span class="score-max"> / {{ finalScores.maxScore }}</span>
            </div>
          </div>
        </div>
      </div>

      <div v-else class="loading-scores">
        <p>Calculating final results...</p>
      </div>

      <a href="/"><button class="play-again-btn">Play Again</button></a>
    </div>
  </div>
</template>

<script>
export default {
  name: 'FinishedPhase',
  props: {
    finalScores: {
      type: Object,
      default: null,
    },
    playerId: {
      type: String,
      default: null,
    },
  },
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
  padding: 48px 56px;
  border-radius: 16px;
  border: 1px solid #e5e7eb;
  text-align: center;
  width: 100%;
  max-width: 580px;
}

.trophy {
  font-size: 48px;
  margin-bottom: 8px;
}

.finished-container h2 {
  margin: 0 0 32px 0;
  color: #1a1a1a;
  font-size: 28px;
  font-weight: 800;
  letter-spacing: -0.5px;
}

.final-scores {
  margin-bottom: 32px;
  text-align: left;
}

.scores-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.scores-header h3 {
  margin: 0;
  color: #1a1a1a;
  font-size: 17px;
  font-weight: 700;
}

.rounds-label {
  font-size: 13px;
  color: #9ca3af;
  font-weight: 500;
}

.scoreboard {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.score-entry {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: 12px;
  background: #fafafa;
  border: 1px solid #f3f4f6;
  transition: all 0.2s ease;
}

.score-entry--you {
  background: #f0f9ff;
  border-color: #bae6fd;
}

.score-entry--first {
  background: #fffbeb;
  border-color: #fde68a;
}

.score-entry--first.score-entry--you {
  background: linear-gradient(135deg, #fffbeb 0%, #f0f9ff 100%);
  border-color: #fde68a;
}

.score-rank {
  flex-shrink: 0;
  width: 32px;
  text-align: center;
}

.rank-medal {
  font-size: 22px;
}

.rank-number {
  font-size: 14px;
  font-weight: 700;
  color: #9ca3af;
}

.score-name {
  flex-shrink: 0;
  width: 90px;
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
  display: flex;
  align-items: center;
  gap: 4px;
}

.you-badge {
  color: #2563eb;
  font-size: 12px;
}

.score-bar-container {
  flex: 1;
  height: 8px;
  background: #e5e7eb;
  border-radius: 4px;
  overflow: hidden;
}

.score-bar {
  height: 100%;
  border-radius: 4px;
  background: linear-gradient(90deg, #e11d48, #9333ea);
  transition: width 0.8s ease;
  min-width: 2px;
}

.score-entry--first .score-bar {
  background: linear-gradient(90deg, #f59e0b, #ef4444);
}

.score-value {
  flex-shrink: 0;
  font-size: 16px;
  font-weight: 700;
  color: #1a1a1a;
  min-width: 60px;
  text-align: right;
}

.score-max {
  font-size: 12px;
  font-weight: 500;
  color: #9ca3af;
}

.loading-scores {
  background: #f5f5f5;
  padding: 32px;
  border-radius: 12px;
  margin-bottom: 32px;
}

.loading-scores p {
  color: #9ca3af;
  font-size: 15px;
  margin: 0;
}

.play-again-btn {
  padding: 14px 40px;
  background: #1a1a1a;
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.play-again-btn:hover {
  background: #333;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

@media (max-width: 768px) {
  .finished-container {
    padding: 32px 20px;
  }

  .score-name {
    width: 70px;
    font-size: 13px;
  }

  .score-value {
    min-width: 50px;
    font-size: 14px;
  }
}
</style>
