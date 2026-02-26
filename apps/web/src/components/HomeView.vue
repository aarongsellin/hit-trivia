<template>
  <div class="landing">
    <!-- Hero Section -->
    <section class="hero">
      <div class="hero-glow"></div>
      <div class="hero-glow hero-glow--secondary"></div>

      <nav class="nav">
        <div class="nav-logo">
          <span class="logo-icon">♫</span>
          <span class="logo-text">Hit Trivia</span>
        </div>
        <div v-if="totalGameCount > 0" class="nav-stat">
          <span class="stat-dot"></span>
          {{ totalGameCount.toLocaleString() }} games played
        </div>
      </nav>

      <div class="hero-content">
        <div class="hero-badge">
          <span class="badge-pulse"></span>
          Music Trivia Game
        </div>

        <h1 class="hero-title">
          Think you know<br />
          <span class="hero-title--gradient">your music?</span>
        </h1>

        <p class="hero-subtitle">
          Challenge your friends to guess songs from short clips. Put your music
          knowledge to the test.
        </p>

        <div class="hero-actions">
          <button
            @click="createGame"
            class="btn btn--primary"
            :disabled="isLoading"
          >
            <span v-if="!isLoading">Create Game</span>
            <span v-else>Creating...</span>
          </button>

          <button @click="scrollToHowItWorks" class="btn btn--ghost">
            How it works
            <svg
              width="16"
              height="16"
              viewBox="0 0 16 16"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <path d="M4 6l4 4 4-4" />
            </svg>
          </button>
        </div>

        <div class="hero-affiliate">
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
              class="apple-badge"
            />
          </a>
        </div>

        <div class="hero-visual">
          <div class="waveform">
            <div
              v-for="i in 100"
              :key="i"
              class="waveform-bar"
              :style="{
                animationDelay: `${i * 0.05}s`,
                height: `${
                  100 + Math.sin(i * 0.5) * 15 + Math.random() * 20
                }px`,
                width: `100vw`,
              }"
            ></div>
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

        <div class="steps">
          <div class="step" v-for="(step, index) in steps" :key="index">
            <div class="step-number">{{ index + 1 }}</div>
            <div class="step-icon">{{ step.icon }}</div>
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
            <div class="feature-icon">{{ feature.icon }}</div>
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
      <p>Built with love &dash; Hit Trivia</p>
      <p class="footer-apple">
        Apple Music and the Apple Music logo are trademarks of Apple Inc.
      </p>
    </footer>
  </div>
</template>

<script>
import { useRouter } from 'vue-router';
import appleBadge from '../assets/listen-on-apple-music.svg';
import appleBadgeBlack from '../assets/listen-on-apple-music-black.svg';

export default {
  setup() {
    const router = useRouter();
    return { router, appleBadge, appleBadgeBlack };
  },
  data() {
    return {
      totalGameCount: 0,
      apiUrl: process.env.VUE_APP_API_URL,
      affiliateToken: process.env.VUE_APP_APPLE_AFFILIATE_TOKEN || '',
      isLoading: false,
      steps: [
        {
          icon: '🎧',
          title: 'Create a game',
          desc: 'Create a game and share the QR code with your friends to join.',
        },
        {
          icon: '🎵',
          title: 'Listen & guess',
          desc: 'A short music clip plays. Race to guess the song title and artist before time runs out.',
        },
        {
          icon: '🏆',
          title: 'See who wins',
          desc: 'Points are awarded for correct guesses. The scoreboard reveals the ultimate music nerd.',
        },
      ],
      features: [
        {
          icon: '⚡',
          title: 'Real-time multiplayer',
          desc: 'Play with friends instantly via shared link or QR code. No app download needed.',
        },
        {
          icon: '🎛️',
          title: 'Customizable rounds',
          desc: "Pick genres, decades, and obscurity levels to match your group's vibe.",
        },
        {
          icon: '📱',
          title: 'Works everywhere',
          desc: 'Play on any device with a browser. Desktop, tablet, or phone.',
        },
        {
          icon: '🔊',
          title: 'Powered by Apple Music',
          desc: 'High-quality audio streamed from Apple Music.',
          hasAffiliate: true,
        },
      ],
    };
  },
  mounted() {
    this.fetchGameCount();
  },
  computed: {
    affiliateUrl() {
      const base = 'https://music.apple.com/subscribe';
      if (!this.affiliateToken) return base;
      return `${base}?at=${encodeURIComponent(this.affiliateToken)}`;
    },
  },
  methods: {
    async fetchGameCount() {
      try {
        const res = await fetch(`${this.apiUrl}/api/game-count`);
        const data = await res.json();
        this.totalGameCount = data.count;
      } catch (err) {
        console.error('Error fetching game count:', err);
      }
    },
    async createGame() {
      this.isLoading = true;

      try {
        const res = await fetch(`${this.apiUrl}/api/new-game`);
        const resJson = await res.json();
        const id = resJson.data.id;

        if (id) {
          this.router.push({ path: '/game', query: { id } });
        }
      } catch (err) {
        console.error('Error creating game:', err);
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
/* ─── Layout ───────────────────────────────────── */

.landing {
  min-height: 100vh;
  background: #ffffff;
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
}

.nav-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 700;
  font-size: 18px;
  color: #1a1a1a;
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
  color: #999;
  font-weight: 500;
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
  position: relative;
  overflow: hidden;
  padding-bottom: 80px;
}

.hero-glow {
  position: absolute;
  top: -200px;
  left: 50%;
  transform: translateX(-50%);
  width: 800px;
  height: 600px;
  background: radial-gradient(
    ellipse,
    rgba(250, 45, 72, 0.06) 0%,
    transparent 70%
  );
  pointer-events: none;
}

.hero-glow--secondary {
  top: -100px;
  left: 30%;
  width: 600px;
  background: radial-gradient(
    ellipse,
    rgba(139, 92, 246, 0.04) 0%,
    transparent 70%
  );
}

.hero-content {
  max-width: 1080px;
  margin: 0 auto;
  padding: 60px 32px 0;
  text-align: center;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: #fff0f2;
  border: 1px solid #fecdd3;
  color: #e11d48;
  padding: 6px 16px;
  border-radius: 100px;
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 32px;
  letter-spacing: 0.3px;
}

.badge-pulse {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #e11d48;
  animation: pulse-dot 1.5s ease-in-out infinite;
}

.hero-title {
  font-size: clamp(42px, 7vw, 76px);
  font-weight: 900;
  line-height: 1.05;
  letter-spacing: -2px;
  color: #1a1a1a;
  margin-bottom: 24px;
}

.hero-title--gradient {
  background: linear-gradient(135deg, #e11d48 0%, #9333ea 50%, #2563eb 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.hero-subtitle {
  font-size: 18px;
  line-height: 1.7;
  color: #6b7280;
  max-width: 520px;
  margin: 0 auto 48px;
  font-weight: 400;
}

/* ─── Buttons ──────────────────────────────────── */

.hero-actions {
  display: flex;
  align-items: center;
  justify-content: center;
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

/* ─── Waveform Visual ──────────────────────────── */

.hero-visual {
  margin-top: 64px;
}

.waveform {
  display: flex;
  align-items: flex-end;
  justify-content: center;
  gap: 3px;
  height: 60px;
  opacity: 0.35;
}

.waveform-bar {
  width: 4px;
  border-radius: 4px;
  background: linear-gradient(to top, #e11d48, #9333ea);
  animation: wave 1.8s ease-in-out infinite alternate;
}

@keyframes wave {
  0% {
    transform: scaleY(0.4);
  }
  100% {
    transform: scaleY(1);
  }
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
  background: #fafafa;
  border: 1px solid #f3f4f6;
  transition: all 0.3s ease;
}

.step:hover {
  background: #f5f5f5;
  border-color: #e5e7eb;
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
  background: #fafafa;
  border: 1px solid #f3f4f6;
  transition: all 0.3s ease;
}

.feature:hover {
  background: #f5f5f5;
  border-color: #e5e7eb;
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
  background: #fafafa;
  border: 1px solid #f3f4f6;
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
  border-top: 1px solid #f3f4f6;
  font-size: 13px;
  color: #000000;
}

.footer-apple {
  margin-top: 8px;
  font-size: 11px;
  color: #afb0b3;
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
    padding: 40px 20px 0;
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

  .waveform {
    gap: 2px;
  }

  .waveform-bar {
    width: 3px;
  }
}
</style>
