<template>
  <div class="finished-phase">
    <div class="finished-container">
      <h2>Game Over!</h2>
      <p class="rounds-label" v-if="finalScores">{{ finalScores.totalRounds }} rounds</p>

      <div v-if="finalScores" class="final-scores">
        <!-- Podium for top 3 -->
        <div class="podium">
          <!-- 2nd place (left) -->
          <div
            v-if="podiumPlayers[1]"
            class="podium-slot podium-slot--second"
            :class="{ 'podium-slot--you': podiumPlayers[1].playerId === playerId }"
          >
            <div class="podium-medal">
              <svg width="36" height="36" viewBox="0 0 36 36" fill="none">
                <circle cx="18" cy="14" r="12" fill="#94a3b8" />
                <circle cx="18" cy="14" r="9" fill="#cbd5e1" />
                <text x="18" y="18" text-anchor="middle" font-size="12" font-weight="700" fill="#475569">2</text>
                <path d="M12 24l6 10 6-10" fill="#94a3b8" />
              </svg>
            </div>
            <div class="podium-name">
              {{ podiumPlayers[1].name }}
              <span v-if="podiumPlayers[1].playerId === playerId" class="you-badge">you</span>
            </div>
            <div class="podium-score">{{ podiumPlayers[1].score }}</div>
            <div class="podium-block podium-block--second"></div>
          </div>
          <div v-else class="podium-slot podium-slot--second podium-slot--empty">
            <div class="podium-block podium-block--second"></div>
          </div>

          <!-- 1st place (center) -->
          <div
            v-if="podiumPlayers[0]"
            class="podium-slot podium-slot--first"
            :class="{ 'podium-slot--you': podiumPlayers[0].playerId === playerId }"
          >
            <div class="podium-medal">
              <svg width="44" height="44" viewBox="0 0 44 44" fill="none">
                <circle cx="22" cy="16" r="14" fill="#f59e0b" />
                <circle cx="22" cy="16" r="10.5" fill="#fbbf24" />
                <text x="22" y="20.5" text-anchor="middle" font-size="14" font-weight="700" fill="#92400e">1</text>
                <path d="M14 28l8 14 8-14" fill="#f59e0b" />
              </svg>
            </div>
            <div class="podium-name">
              {{ podiumPlayers[0].name }}
              <span v-if="podiumPlayers[0].playerId === playerId" class="you-badge">you</span>
            </div>
            <div class="podium-score">{{ podiumPlayers[0].score }}</div>
            <div class="podium-block podium-block--first"></div>
          </div>
          <div v-else class="podium-slot podium-slot--first podium-slot--empty">
            <div class="podium-block podium-block--first"></div>
          </div>

          <!-- 3rd place (right) -->
          <div
            v-if="podiumPlayers[2]"
            class="podium-slot podium-slot--third"
            :class="{ 'podium-slot--you': podiumPlayers[2].playerId === playerId }"
          >
            <div class="podium-medal">
              <svg width="36" height="36" viewBox="0 0 36 36" fill="none">
                <circle cx="18" cy="14" r="12" fill="#b45309" />
                <circle cx="18" cy="14" r="9" fill="#d97706" />
                <text x="18" y="18" text-anchor="middle" font-size="12" font-weight="700" fill="#78350f">3</text>
                <path d="M12 24l6 10 6-10" fill="#b45309" />
              </svg>
            </div>
            <div class="podium-name">
              {{ podiumPlayers[2].name }}
              <span v-if="podiumPlayers[2].playerId === playerId" class="you-badge">you</span>
            </div>
            <div class="podium-score">{{ podiumPlayers[2].score }}</div>
            <div class="podium-block podium-block--third"></div>
          </div>
          <div v-else class="podium-slot podium-slot--third podium-slot--empty">
            <div class="podium-block podium-block--third"></div>
          </div>
        </div>

        <!-- Remaining players table -->
        <table v-if="remainingPlayers.length" class="rest-table">
          <thead>
            <tr>
              <th class="col-rank">#</th>
              <th class="col-name">Player</th>
              <th class="col-score">Score</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="entry in remainingPlayers"
              :key="entry.playerId"
              :class="{ 'row--you': entry.playerId === playerId }"
            >
              <td class="col-rank">{{ entry.rank }}</td>
              <td class="col-name">
                {{ entry.name }}
                <span v-if="entry.playerId === playerId" class="you-badge">you</span>
              </td>
              <td class="col-score">
                {{ entry.score }}<span class="score-max"> / {{ finalScores.maxScore }}</span>
              </td>
            </tr>
          </tbody>
        </table>
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
  computed: {
    podiumPlayers() {
      if (!this.finalScores?.scoreboard) return [];
      return this.finalScores.scoreboard.filter((e) => e.rank <= 3);
    },
    remainingPlayers() {
      if (!this.finalScores?.scoreboard) return [];
      return this.finalScores.scoreboard.filter((e) => e.rank > 3);
    },
  },
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

.finished-container h2 {
  margin: 0 0 4px 0;
  color: #1a1a1a;
  font-size: 28px;
  font-weight: 800;
  letter-spacing: -0.5px;
}

.rounds-label {
  font-size: 13px;
  color: #9ca3af;
  font-weight: 500;
  margin: 0 0 28px 0;
}

.final-scores {
  margin-bottom: 32px;
}

/* ─── Podium ─────────────────────────────────── */
.podium {
  display: flex;
  align-items: flex-end;
  justify-content: center;
  gap: 6px;
  margin-bottom: 32px;
}

.podium-slot {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 120px;
}

.podium-slot--empty {
  /* Reserve space even if fewer than 3 players */
  visibility: hidden;
}

.podium-medal {
  margin-bottom: 6px;
}

.podium-name {
  font-size: 14px;
  font-weight: 700;
  color: #1a1a1a;
  margin-bottom: 2px;
  max-width: 110px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

.podium-score {
  font-size: 20px;
  font-weight: 800;
  color: #1a1a1a;
  margin-bottom: 8px;
}

.podium-block {
  width: 100%;
  border-radius: 8px 8px 0 0;
  min-height: 20px;
}

.podium-block--first {
  height: 100px;
  background: linear-gradient(180deg, #fde68a 0%, #f59e0b 100%);
}

.podium-block--second {
  height: 72px;
  background: linear-gradient(180deg, #e2e8f0 0%, #94a3b8 100%);
}

.podium-block--third {
  height: 52px;
  background: linear-gradient(180deg, #fde68a 0%, #b45309 100%);
}

.podium-slot--you .podium-name {
  color: #2563eb;
}

/* ─── Remaining players table ────────────────── */
.rest-table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
  font-size: 14px;
}

.rest-table thead th {
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: #9ca3af;
  font-weight: 600;
  padding: 0 12px 8px;
  border-bottom: 1px solid #f3f4f6;
}

.rest-table tbody tr {
  border-bottom: 1px solid #f3f4f6;
}

.rest-table tbody td {
  padding: 10px 12px;
  color: #374151;
}

.rest-table .col-rank {
  width: 40px;
  font-weight: 700;
  color: #9ca3af;
}

.rest-table .col-name {
  font-weight: 600;
}

.rest-table .col-score {
  text-align: right;
  font-weight: 700;
  white-space: nowrap;
}

.row--you td {
  background: #f0f9ff;
}

.row--you .col-name {
  color: #2563eb;
}

.you-badge {
  display: inline-block;
  font-size: 10px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  color: #2563eb;
  background: #dbeafe;
  padding: 1px 6px;
  border-radius: 6px;
  margin-left: 2px;
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

  .podium-slot {
    width: 100px;
  }

  .podium-name {
    font-size: 12px;
    max-width: 90px;
  }

  .podium-score {
    font-size: 16px;
  }

  .podium-block--first {
    height: 80px;
  }

  .podium-block--second {
    height: 56px;
  }

  .podium-block--third {
    height: 40px;
  }
}
</style>
