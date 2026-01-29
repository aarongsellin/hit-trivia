<template>
  <div class="waiting-config-layout">
    <div class="waiting-message">
      <p>Waiting for game creator to start...</p>
    </div>
  </div>
</template>

<script>
export default {
  name: 'WaitingConfigPhase',
};
</script>

<style scoped>
.waiting-config-layout {
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

.waiting-message p {
  font-size: 18px;
  color: #333;
  margin: 0;
  font-weight: 500;
}

@media (max-width: 768px) {
  .waiting-message {
    padding: 30px 40px;
  }

  .waiting-message p {
    font-size: 16px;
  }
}
</style>
