<template>
  <div class="waiting-layout">
    <div class="waiting-message">
      <h2>Get Ready!</h2>
      <p>Next round starting soon...</p>
    </div>
  </div>
</template>

<script>
export default {
  name: 'WaitingPhase',
};
</script>

<style scoped>
.waiting-layout {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 70vh;
}

.waiting-message {
  background: white;
  padding: 40px 60px;
  border-radius: 8px;
  border: 1px solid #ddd;
  text-align: center;
}

.waiting-message h2 {
  margin-top: 0;
  margin-bottom: 16px;
  color: #333;
  font-size: 24px;
}

.waiting-message p {
  font-size: 18px;
  color: #666;
  margin: 0;
}

@media (max-width: 768px) {
  .waiting-message {
    padding: 30px 40px;
  }

  .waiting-message h2 {
    font-size: 20px;
  }

  .waiting-message p {
    font-size: 16px;
  }
}
</style>
