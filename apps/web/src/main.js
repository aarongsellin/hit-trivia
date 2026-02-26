import { createApp } from 'vue';
import GameView from './components/GameView.vue';
import HomeView from './components/HomeView.vue';
import { createRouter, createWebHistory } from 'vue-router';
import App from './App.vue';

const routes = [
  {
    path: '/',
    component: HomeView,
  },
  {
    path: '/game',
    component: GameView,
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

const app = createApp(App);
app.use(router);
app.mount('#app');
