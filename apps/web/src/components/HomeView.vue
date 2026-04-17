<template>
  <div class="landing">
    <!-- Hero Section -->
    <section class="hero">
      <nav class="nav">
        <div class="nav-logo">
          <span class="logo-text">Hit Trivia</span>
        </div>
        <div v-if="totalGameCount > 0" class="nav-stat">
          <span class="stat-dot"></span>
          {{ totalGameCount.toLocaleString() }} games played
        </div>
      </nav>

      <div class="hero-content">
        <div class="hero-left">
          <p class="hero-kicker">Music Trivia</p>
          <h1 class="hero-title">Think you<br />know<br /><em>music?</em></h1>
          <p class="hero-subtitle">
            Guess songs from short clips. Challenge your friends. See who really
            knows their music.
          </p>
          <div class="hero-actions">
            <button
              @click="createGame"
              class="btn btn--primary"
              :disabled="isLoading"
            >
              <span v-if="!isLoading">Create a game</span>
              <span v-else>Creating…</span>
            </button>
            <button @click="scrollToHowItWorks" class="btn btn--ghost">
              How it works
            </button>
          </div>
        </div>

        <div class="hero-right">
          <div class="vinyl">
            <div class="vinyl-label">
              <span class="vinyl-label-title">HIT</span>
              <span class="vinyl-label-sub">TRIVIA</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Live Games Marquee -->
    <section v-if="activeGames.length > 0" class="live-marquee">
      <div class="marquee-label">
        <span class="marquee-dot"></span>
        Live now
      </div>
      <div class="marquee-track" ref="marqueeTrack">
        <div class="marquee-inner">
          <div
            class="marquee-card"
            v-for="(game, i) in repeatedGames"
            :key="'a-' + i"
          >
            <div class="marquee-card-icon">
              <svg
                width="16"
                height="16"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <path d="M9 18V5l12-2v13" />
                <circle cx="6" cy="18" r="3" />
                <circle cx="18" cy="16" r="3" />
              </svg>
            </div>
            <span class="marquee-card-text">
              <strong>{{ game.host }}</strong>
              <template v-if="game.players > 1">
                and {{ game.players - 1 }}
                {{ game.players === 2 ? 'friend' : 'friends' }}</template
              >
              {{ game.players === 1 ? 'is' : 'are' }} listening to
              <span class="marquee-card-tag" v-if="game.decade && game.genre"
                >{{ game.decade }} {{ game.genre }}</span
              >
              <span class="marquee-card-tag" v-else-if="game.decade"
                >{{ game.decade }} music</span
              >
              <span class="marquee-card-tag" v-else-if="game.genre">{{
                game.genre
              }}</span>
              <span class="marquee-card-tag" v-else>music</span>
            </span>
          </div>
        </div>
      </div>
    </section>

    <!-- How It Works -->
    <section ref="howItWorks" class="how-it-works">
      <div class="section-container">
        <div class="section-header">
          <span class="section-label">How it works</span>
          <h2 class="section-title">Three simple steps</h2>
          <p class="section-subtitle">
            From setup to showdown in under a minute
          </p>
        </div>

        <div class="beta-banner">
          <span class="beta-tag">BETA</span>
          Song generation is in an experimental phase - Searches are hit or miss
        </div>

        <div class="steps">
          <div class="step" v-for="(step, index) in steps" :key="index">
            <div class="step-number">{{ index + 1 }}</div>
            <div class="step-icon" v-html="iconSvg(step.icon)"></div>
            <h3 class="step-title">{{ step.title }}</h3>
            <p class="step-desc">{{ step.desc }}</p>
          </div>
        </div>
      </div>
    </section>

    <!-- Features -->
    <section class="features">
      <div class="section-container">
        <div class="features-grid">
          <div class="feature" v-for="(feature, i) in features" :key="i">
            <div class="feature-icon" v-html="iconSvg(feature.icon)"></div>
            <h3 class="feature-title">{{ feature.title }}</h3>
            <p class="feature-desc">{{ feature.desc }}</p>
            <a
              v-if="feature.hasAffiliate"
              :href="affiliateUrl"
              target="_blank"
              rel="noopener noreferrer"
              class="apple-badge-link"
              aria-label="Listen on Apple Music"
            >
              <img
                :src="appleBadge"
                alt="Listen on Apple Music"
                class="apple-badge apple-badge--tiny"
              />
            </a>
          </div>
        </div>
      </div>
    </section>

    <!-- CTA Section -->
    <section class="cta">
      <div class="section-container">
        <div class="cta-card">
          <div class="cta-glow"></div>
          <h2 class="cta-title">Ready to play?</h2>
          <p class="cta-subtitle">
            Start a game in seconds and challenge your friends.
          </p>
          <button
            @click="createGame"
            class="btn btn--primary btn--large"
            :disabled="isLoading"
          >
            Start a game
          </button>
          <div class="cta-badge-row">
            <a
              :href="affiliateUrl"
              target="_blank"
              rel="noopener noreferrer"
              class="apple-badge-link"
              aria-label="Listen on Apple Music"
            >
              <img
                :src="appleBadge"
                alt="Listen on Apple Music"
                class="apple-badge apple-badge--small"
              />
            </a>
          </div>
        </div>
      </div>
    </section>

    <footer class="footer">
      <p>
        Built by
        <a
          href="https://aarongsellin.dev"
          target="_blank"
          rel="noopener noreferrer"
          class="footer-link"
          >aarongsellin.dev</a
        >
      </p>
      <p class="footer-apple">
        Apple Music and the Apple Music logo are trademarks of Apple Inc.
      </p>
    </footer>
  </div>
</template>

<script>
import { useRouter, useRoute } from 'vue-router';
import appleBadge from '../assets/listen-on-apple-music.svg';
import appleBadgeBlack from '../assets/listen-on-apple-music-black.svg';

export default {
  setup() {
    const router = useRouter();
    const route = useRoute();

    // If someone arrives at /?id=xxx, redirect them to /game?id=xxx
    const gameId = route.query.id;
    if (gameId) {
      router.replace({ path: '/game', query: { id: gameId } });
    }

    return { router, appleBadge, appleBadgeBlack };
  },
  data() {
    return {
      totalGameCount: 0,
      activeGames: [],
      affiliateToken: '',
      isLoading: false,
      steps: [
        {
          icon: 'headphones',
          title: 'Create a game',
          desc: 'Create a game and share the QR code with your friends to join.',
        },
        {
          icon: 'music',
          title: 'Listen & guess',
          desc: 'A short music clip plays. Race to guess the song title and artist before time runs out.',
        },
        {
          icon: 'trophy',
          title: 'See who wins',
          desc: 'Points are awarded for correct guesses. The scoreboard reveals the ultimate music nerd.',
        },
      ],
      features: [
        {
          icon: 'zap',
          title: 'Real-time multiplayer',
          desc: 'Play with friends instantly via shared link or QR code. No app download needed.',
        },
        {
          icon: 'sliders',
          title: 'Customizable rounds',
          desc: "Pick genres, decades, and obscurity levels to match your group's vibe.",
        },
        {
          icon: 'smartphone',
          title: 'Works everywhere',
          desc: 'Play on any device with a browser. Desktop, tablet, or phone.',
        },
        {
          icon: 'volume',
          title: 'Powered by Apple Music',
          desc: 'High-quality audio streamed from Apple Music.',
          hasAffiliate: true,
        },
      ],
    };
  },
  mounted() {
    this.fetchGameCount();
    this.fetchActiveGames();
    this._activeGamesInterval = setInterval(
      () => this.fetchActiveGames(),
      15000
    );
  },
  beforeUnmount() {
    if (this._activeGamesInterval) clearInterval(this._activeGamesInterval);
  },
  computed: {
    affiliateUrl() {
      const base = 'https://music.apple.com/subscribe';
      if (!this.affiliateToken) return base;
      return `${base}?at=${encodeURIComponent(this.affiliateToken)}`;
    },
    repeatedGames() {
      // Duplicate games enough times so the marquee can scroll continuously
      if (this.activeGames.length === 0) return [];
      const minCards = 20;
      const repeats = Math.ceil(minCards / this.activeGames.length);
      const result = [];
      for (let r = 0; r < repeats * 2; r++) {
        for (const game of this.activeGames) {
          result.push(game);
        }
      }
      return result;
    },
  },
  methods: {
    iconSvg(name) {
      const s = (d) =>
        `<svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">${d}</svg>`;
      const icons = {
        headphones: s(
          '<path d="M3 18v-6a9 9 0 0 1 18 0v6"/><path d="M21 19a2 2 0 0 1-2 2h-1a2 2 0 0 1-2-2v-3a2 2 0 0 1 2-2h3v5z"/><path d="M3 19a2 2 0 0 0 2 2h1a2 2 0 0 0 2-2v-3a2 2 0 0 0-2-2H3v5z"/>'
        ),
        music: s(
          '<path d="M9 18V5l12-2v13"/><circle cx="6" cy="18" r="3"/><circle cx="18" cy="16" r="3"/>'
        ),
        trophy: s(
          '<path d="M6 9H4.5a2.5 2.5 0 0 1 0-5H6"/><path d="M18 9h1.5a2.5 2.5 0 0 0 0-5H18"/><path d="M4 22h16"/><path d="M10 14.66V17c0 .55-.47.98-.97 1.21C7.85 18.75 7 20.24 7 22"/><path d="M14 14.66V17c0 .55.47.98.97 1.21C16.15 18.75 17 20.24 17 22"/><path d="M18 2H6v7a6 6 0 0 0 12 0V2Z"/>'
        ),
        zap: s('<polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/>'),
        sliders: s(
          '<line x1="4" y1="21" x2="4" y2="14"/><line x1="4" y1="10" x2="4" y2="3"/><line x1="12" y1="21" x2="12" y2="12"/><line x1="12" y1="8" x2="12" y2="3"/><line x1="20" y1="21" x2="20" y2="16"/><line x1="20" y1="12" x2="20" y2="3"/><line x1="1" y1="14" x2="7" y2="14"/><line x1="9" y1="8" x2="15" y2="8"/><line x1="17" y1="16" x2="23" y2="16"/>'
        ),
        smartphone: s(
          '<rect x="5" y="2" width="14" height="20" rx="2" ry="2"/><line x1="12" y1="18" x2="12.01" y2="18"/>'
        ),
        volume: s(
          '<polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"/><path d="M19.07 4.93a10 10 0 0 1 0 14.14"/><path d="M15.54 8.46a5 5 0 0 1 0 7.07"/>'
        ),
      };
      return icons[name] || '';
    },
    async fetchGameCount() {
      try {
        const res = await fetch('/api/game-count');
        const data = await res.json();
        this.totalGameCount = data.count;
      } catch {
        // silently ignore
      }
    },
    async fetchActiveGames() {
      try {
        const res = await fetch('/api/active-games');
        this.activeGames = await res.json();
      } catch {
        // silently ignore
      }
    },
    async createGame() {
      this.isLoading = true;

      try {
        const res = await fetch('/api/new-game');
        const resJson = await res.json();
        const id = resJson.data.id;

        if (id) {
          this.router.push({ path: '/game', query: { id } });
        }
      } catch {
        // silently ignore
      } finally {
        this.isLoading = false;
      }
    },
    scrollToHowItWorks() {
      this.$refs.howItWorks.scrollIntoView({ behavior: 'smooth' });
    },
  },
};
</script>

<style scoped>
/* ─── Beta Banner ──────────────────────────────── */

.beta-banner {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  margin-bottom: 20px;
  background: #fff8e1;
  border: 1px solid #ffe082;
  border-radius: 10px;
  font-size: 0.85rem;
  color: #6d5a00;
}

.beta-tag {
  flex-shrink: 0;
  padding: 2px 8px;
  background: #f59e0b;
  color: #fff;
  font-weight: 700;
  font-size: 0.7rem;
  letter-spacing: 0.05em;
  border-radius: 4px;
}

/* ─── Layout ───────────────────────────────────── */

.landing {
  min-height: 100vh;
  background: #fdf7e5;
  color: #1a1a1a;
}

.section-container {
  max-width: 1080px;
  margin: 0 auto;
  padding: 0 24px;
}

/* ─── Nav ──────────────────────────────────────── */

.nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24px 32px;
  max-width: 1080px;
  margin: 0 auto;
  width: 100%;
  box-sizing: border-box;
  flex-wrap: nowrap;
  gap: 16px;
}

.nav-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 700;
  font-size: 18px;
  color: #fff;
}

.logo-icon {
  font-size: 22px;
  color: #fa2d48;
}

.logo-text {
  letter-spacing: -0.3px;
}

.nav-stat {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.45);
  font-weight: 500;
  white-space: nowrap;
}

.stat-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #22c55e;
  animation: pulse-dot 2s ease-in-out infinite;
}

@keyframes pulse-dot {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.4;
  }
}

/* ─── Hero ─────────────────────────────────────── */

.hero {
  background: #111;
  color: #fff;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.hero-content {
  flex: 1;
  max-width: 1080px;
  margin: 0 auto;
  padding: 60px 32px 80px;
  display: flex;
  align-items: center;
  gap: 80px;
  width: 100%;
}

.hero-left {
  flex: 1;
  min-width: 0;
}

.hero-kicker {
  font-size: 12px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 2.5px;
  color: #e11d48;
  margin-bottom: 20px;
}

.hero-title {
  font-size: clamp(52px, 8vw, 96px);
  font-weight: 900;
  line-height: 1;
  letter-spacing: -3px;
  color: #fff;
  margin-bottom: 28px;
}

.hero-title em {
  font-style: italic;
  color: #e11d48;
}

.hero-subtitle {
  font-size: 18px;
  line-height: 1.7;
  color: #888;
  max-width: 440px;
  margin-bottom: 48px;
  font-weight: 400;
}

.hero-right {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* ─── Vinyl Record ─────────────────────────────── */

.vinyl {
  width: 420px;
  height: 420px;
  border-radius: 50%;
  background: repeating-radial-gradient(
    circle at center,
    #111 0px,
    #2a2a2a 2px,
    #111 6px,
    #222 8px,
    #111 12px
  );
  position: relative;
  animation: vinyl-spin 10s linear infinite;
  box-shadow: 0 0 120px rgba(225, 29, 72, 0.2), 0 32px 80px rgba(0, 0, 0, 0.8);
}

.vinyl-label {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 34%;
  height: 34%;
  border-radius: 50%;
  background: #e11d48;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
}

.vinyl-label-title {
  font-size: 32px;
  font-weight: 900;
  color: #fff;
  letter-spacing: 3px;
  line-height: 1;
}

.vinyl-label-sub {
  font-size: 15px;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.75);
  letter-spacing: 2px;
  text-transform: uppercase;
}

@keyframes vinyl-spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

/* ─── Buttons ──────────────────────────────────── */

.hero-actions {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.btn {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  font-size: 15px;
  font-weight: 600;
  border-radius: 12px;
  border: none;
  padding: 14px 28px;
  transition: all 0.2s ease;
  letter-spacing: -0.2px;
}

.btn-icon {
  flex-shrink: 0;
}

.btn--primary {
  background: #1a1a1a;
  color: #fff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.12), 0 4px 16px rgba(0, 0, 0, 0.08);
}

.btn--primary:hover:not(:disabled) {
  background: #333;
  transform: translateY(-1px);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.15), 0 8px 24px rgba(0, 0, 0, 0.12);
}

.btn--primary:active:not(:disabled) {
  transform: translateY(0);
}

.btn--primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn--ghost {
  background: transparent;
  color: #6b7280;
  border: 1px solid #e5e7eb;
}

.btn--ghost:hover {
  border-color: #d1d5db;
  color: #1a1a1a;
  background: #f9fafb;
}

.btn--large {
  padding: 18px 36px;
  font-size: 17px;
  border-radius: 14px;
}

/* ─── Hero button overrides (dark background) ─── */

.hero .btn--primary {
  background: #e11d48;
  box-shadow: 0 1px 3px rgba(225, 29, 72, 0.3),
    0 4px 16px rgba(225, 29, 72, 0.15);
}

.hero .btn--primary:hover:not(:disabled) {
  background: #be123c;
  transform: translateY(-1px);
  box-shadow: 0 2px 6px rgba(225, 29, 72, 0.4),
    0 8px 24px rgba(225, 29, 72, 0.2);
}

.hero .btn--ghost {
  border-color: rgba(255, 255, 255, 0.15);
  color: rgba(255, 255, 255, 0.6);
}

.hero .btn--ghost:hover {
  border-color: rgba(255, 255, 255, 0.3);
  color: #fff;
  background: rgba(255, 255, 255, 0.05);
}

/* ─── Live Marquee ─────────────────────────────── */

.live-marquee {
  position: relative;
  overflow: hidden;
  padding: 20px 0;
  background: #ffffff;
  border-top: 1px solid #e8e3db;
  border-bottom: 1px solid #e8e3db;
}

.marquee-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  position: absolute;
  left: 24px;
  top: 50%;
  transform: translateY(-50%);
  z-index: 2;
  background: #ffffff;
  padding: 6px 16px 6px 8px;
  font-size: 12px;
  font-weight: 600;
  color: #22c55e;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.marquee-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #22c55e;
  animation: pulse-dot 2s ease-in-out infinite;
}

.marquee-track {
  overflow: hidden;
  mask-image: linear-gradient(
    to right,
    transparent 0%,
    black 10%,
    black 90%,
    transparent 100%
  );
  -webkit-mask-image: linear-gradient(
    to right,
    transparent 0%,
    black 10%,
    black 90%,
    transparent 100%
  );
}

.marquee-inner {
  display: flex;
  gap: 12px;
  width: max-content;
  animation: marquee-scroll 60s linear infinite;
}

.marquee-inner:hover {
  animation-play-state: paused;
}

@keyframes marquee-scroll {
  0% {
    transform: translateX(0);
  }
  100% {
    transform: translateX(-50%);
  }
}

.marquee-card {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: #f9f7f0;
  border: 1px solid #e8e3db;
  border-radius: 100px;
  padding: 8px 16px;
  white-space: nowrap;
  font-size: 13px;
  color: #4b5563;
  transition: border-color 0.2s, box-shadow 0.2s;
  flex-shrink: 0;
}

.marquee-card:hover {
  border-color: #d1d5db;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.marquee-card-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #fff0f2;
  color: #e11d48;
  flex-shrink: 0;
}

.marquee-card-icon svg {
  width: 12px;
  height: 12px;
}

.marquee-card-text strong {
  color: #1a1a1a;
  font-weight: 600;
}

.marquee-card-tag {
  color: #e11d48;
  font-weight: 600;
}

/* ─── How It Works ─────────────────────────────── */

.how-it-works {
  padding: 120px 0;
  border-top: 1px solid #f3f4f6;
}

.section-header {
  text-align: center;
  margin-bottom: 72px;
}

.section-label {
  display: inline-block;
  font-size: 13px;
  font-weight: 600;
  color: #e11d48;
  text-transform: uppercase;
  letter-spacing: 1.5px;
  margin-bottom: 16px;
}

.section-title {
  font-size: clamp(28px, 4vw, 44px);
  font-weight: 800;
  color: #1a1a1a;
  letter-spacing: -1px;
  margin-bottom: 12px;
}

.section-subtitle {
  font-size: 17px;
  color: #9ca3af;
  font-weight: 400;
}

.steps {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 32px;
}

.step {
  text-align: center;
  padding: 40px 28px;
  border-radius: 20px;
  background: #ffffff;
  border: 1px solid #e8e3db;
  transition: all 0.3s ease;
}

.step:hover {
  background: #e8e3db;
  border-color: #d6d0c4;
  transform: translateY(-4px);
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.04);
}

.step-number {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: #fff0f2;
  color: #e11d48;
  font-size: 13px;
  font-weight: 700;
  margin-bottom: 20px;
}

.step-icon {
  font-size: 36px;
  margin-bottom: 20px;
}

.step-title {
  font-size: 18px;
  font-weight: 700;
  color: #1a1a1a;
  margin-bottom: 10px;
}

.step-desc {
  font-size: 15px;
  line-height: 1.6;
  color: #9ca3af;
}

/* ─── Features ─────────────────────────────────── */

.features {
  padding: 0 0 120px;
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
}

.feature {
  padding: 28px 24px;
  border-radius: 16px;
  background: #ffffff;
  border: 1px solid #e8e3db;
  transition: all 0.3s ease;
}

.feature:hover {
  background: #e8e3db;
  border-color: #d6d0c4;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.03);
}

.feature-icon {
  font-size: 28px;
  margin-bottom: 16px;
}

.feature-title {
  font-size: 15px;
  font-weight: 700;
  color: #1a1a1a;
  margin-bottom: 8px;
}

.feature-desc {
  font-size: 14px;
  line-height: 1.6;
  color: #9ca3af;
}

/* ─── CTA ──────────────────────────────────────── */

.cta {
  padding: 0 0 80px;
}

.cta-card {
  position: relative;
  overflow: hidden;
  text-align: center;
  padding: 80px 40px;
  border-radius: 24px;
  background: #ffffff;
  border: 1px solid #e8e3db;
}

.cta-glow {
  position: absolute;
  top: -100px;
  left: 50%;
  transform: translateX(-50%);
  width: 500px;
  height: 300px;
  background: radial-gradient(
    ellipse,
    rgba(225, 29, 72, 0.04) 0%,
    transparent 70%
  );
  pointer-events: none;
}

.cta-title {
  font-size: clamp(28px, 4vw, 40px);
  font-weight: 800;
  color: #1a1a1a;
  letter-spacing: -1px;
  margin-bottom: 12px;
  position: relative;
}

.cta-subtitle {
  font-size: 17px;
  color: #9ca3af;
  margin-bottom: 36px;
  position: relative;
}

/* ─── Affiliate ────────────────────────────────── */

.affiliate {
  padding: 0 0 120px;
}

.affiliate-card {
  text-align: center;
  padding: 56px 40px;
  border-radius: 24px;
  background: linear-gradient(135deg, #fafafa 0%, #fff0f2 100%);
  border: 1px solid #fecdd3;
}

.affiliate-apple-icon {
  width: 48px;
  height: 48px;
  margin-bottom: 16px;
  border-radius: 12px;
}

.affiliate-title {
  font-size: 22px;
  font-weight: 800;
  color: #1a1a1a;
  margin-bottom: 8px;
  letter-spacing: -0.5px;
}

.affiliate-desc {
  font-size: 16px;
  line-height: 1.6;
  color: #6b7280;
  max-width: 440px;
  margin: 0 auto 28px;
}

/* ─── Apple Music Badges ──────────────────────── */

.apple-badge-link {
  display: inline-block;
  transition: opacity 0.15s ease;
}

.apple-badge-link:hover {
  opacity: 0.8;
}

.apple-badge {
  height: 56px;
  width: auto;
}

.apple-badge--small {
  height: 44px;
}

.apple-badge--tiny {
  height: 38px;
  margin-top: 12px;
}

.hero-affiliate {
  margin-top: 28px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.hero-affiliate-text {
  font-size: 13px;
  color: #9ca3af;
}

.affiliate-link {
  color: #e11d48;
  font-weight: 600;
  text-decoration: none;
  transition: color 0.15s ease;
}

.affiliate-link:hover {
  color: #be123c;
  text-decoration: underline;
}

.cta-badge-row {
  margin-top: 24px;
  position: relative;
}

.footer {
  text-align: center;
  padding: 40px 24px;
  border-top: 1px solid #e8e3db;
  font-size: 13px;
  color: #6b7280;
}

.footer-link {
  color: #1a1a1a;
  font-weight: 600;
  text-decoration: none;
}

.footer-link:hover {
  text-decoration: underline;
}

.footer-apple {
  margin-top: 8px;
  font-size: 11px;
  color: #b0aaa2;
}

/* responsive */

@media (max-width: 900px) {
  .features-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .nav {
    padding: 20px 20px;
  }

  .hero-content {
    flex-direction: column;
    padding: 40px 20px 60px;
    gap: 40px;
  }

  .hero-right {
    order: -1;
  }

  .vinyl {
    width: 280px;
    height: 280px;
  }

  .hero-subtitle {
    font-size: 16px;
  }

  .steps {
    grid-template-columns: 1fr;
    gap: 20px;
  }

  .features-grid {
    grid-template-columns: 1fr;
  }

  .hero-actions {
    flex-direction: column;
  }

  .btn {
    width: 100%;
    justify-content: center;
  }

  .cta-card {
    padding: 48px 24px;
  }

  .affiliate-card {
    padding: 40px 20px;
  }
}
</style>
