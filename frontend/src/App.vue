<template>
  <router-view v-slot="{ Component, route }">
    <transition :name="route.meta.useAuthTransition ? 'auth-slide' : ''" mode="out-in">
      <component :is="Component" :key="route.meta.isAuthPage ? route.name || route.fullPath : undefined" />
    </transition>
  </router-view>
</template>

<style>
.auth-slide-enter-active,
.auth-slide-leave-active {
  transition:
    opacity 0.42s ease,
    transform 0.42s cubic-bezier(0.16, 1, 0.3, 1),
    filter 0.42s ease;
}

.auth-slide-enter-from {
  opacity: 0;
  transform: translateX(34px) scale(0.985);
  filter: blur(8px);
}

.auth-slide-leave-to {
  opacity: 0;
  transform: translateX(-34px) scale(0.985);
  filter: blur(8px);
}
</style>
