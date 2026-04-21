<template>
  <div class="bg-layer-aurora" aria-hidden="true"></div>
  <div class="bg-layer-grid" aria-hidden="true"></div>
</template>

<style scoped>
.bg-layer-aurora {
  position: fixed;
  inset: 0;
  z-index: -2;
  background:
      radial-gradient(circle at 10% 20%, rgba(179, 202, 185, 0.45), transparent 30rem),
      radial-gradient(circle at 90% 10%, rgba(229, 194, 164, 0.35), transparent 30rem),
      radial-gradient(circle at 50% 80%, rgba(199, 215, 205, 0.35), transparent 40rem);
  filter: blur(60px);
  pointer-events: none;
}

.bg-layer-grid {
  position: fixed;
  inset: 0;
  z-index: -1;
  background-image:
      linear-gradient(rgba(42, 54, 46, 0.04) 1px, transparent 1px),
      linear-gradient(90deg, rgba(42, 54, 46, 0.04) 1px, transparent 1px);
  background-size: 32px 32px;
  mask-image: linear-gradient(to bottom, rgba(0, 0, 0, 0.8) 0%, rgba(0, 0, 0, 0.1) 80%, transparent 100%);
  pointer-events: none;
}
</style>
