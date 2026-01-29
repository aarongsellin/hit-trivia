<template>
  <div class="config-layout">
    <div class="config-section">
      <h2>Game Configuration</h2>

      <div class="config-group">
        <label>Genre</label>
        <div class="option-grid">
          <button
            v-for="genre in genres"
            :key="genre"
            @click="$emit('update:genre', genre)"
            :class="['option-btn', { active: selectedGenre === genre }]"
          >
            {{ genre }}
          </button>
        </div>
      </div>

      <div class="config-group">
        <label>Decade</label>
        <div class="option-grid">
          <button
            v-for="decade in decades"
            :key="decade"
            @click="$emit('update:decade', decade)"
            :class="['option-btn', { active: selectedDecade === decade }]"
          >
            {{ decade }}
          </button>
        </div>
      </div>

      <div class="config-group">
        <label>Obscurity</label>
        <div class="obscurity-slider">
          <input
            :value="selectedObscurity"
            @input="$emit('update:obscurity', Number($event.target.value))"
            type="range"
            min="1"
            max="5"
            class="slider"
          />
          <span class="obscurity-label">{{
            obscurityLabels[selectedObscurity - 1]
          }}</span>
        </div>
      </div>

      <button @click="$emit('start-game')" class="start-btn">Start Game</button>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ConfigPhase',
  props: {
    selectedGenre: String,
    selectedDecade: String,
    selectedObscurity: Number,
  },
  emits: ['update:genre', 'update:decade', 'update:obscurity', 'start-game'],
  data() {
    return {
      genres: ['Pop', 'Rock', 'Hip-Hop', 'Country', 'Electronic', 'Jazz'],
      decades: ['1960s', '1970s', '1980s', '1990s', '2000s', '2010s', '2020s'],
      obscurityLabels: [
        'Very Famous',
        'Famous',
        'Known',
        'Obscure',
        'Very Obscure',
      ],
    };
  },
};
</script>

<style scoped>
.config-layout {
  max-width: 800px;
  margin: 0 auto 30px;
}

.config-section {
  background: white;
  padding: 40px;
  border-radius: 8px;
  border: 1px solid #ddd;
}

.config-group {
  margin-bottom: 32px;
}

.config-group label {
  display: block;
  font-weight: 600;
  color: #000;
  margin-bottom: 12px;
  font-size: 15px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.option-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.option-btn {
  padding: 10px 14px;
  background: #fff;
  border: 1px solid #ddd;
  border-radius: 3px;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.15s ease;
}

.option-btn:hover {
  background: #f9f9f9;
  border-color: #999;
}

.option-btn.active {
  background: #000;
  color: white;
  border-color: #000;
}

.obscurity-slider {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.slider {
  width: 100%;
  height: 6px;
  border-radius: 3px;
  background: #ddd;
  outline: none;
  -webkit-appearance: none;
}

.slider::-webkit-slider-thumb {
  -webkit-appearance: none;
  appearance: none;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #000;
  cursor: pointer;
}

.slider::-moz-range-thumb {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #000;
  cursor: pointer;
  border: none;
}

.obscurity-label {
  text-align: center;
  color: #333;
  font-weight: 500;
  font-size: 13px;
}

.start-btn {
  width: 100%;
  padding: 12px;
  background: #000;
  color: white;
  border: none;
  border-radius: 3px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.15s ease;
  margin-top: 16px;
}

.start-btn:hover {
  background: #333;
}

@media (max-width: 768px) {
  .config-section {
    padding: 20px;
  }

  .option-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
