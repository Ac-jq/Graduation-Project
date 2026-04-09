<template>
  <div class="workspace-shell">
    <header class="workspace-shell__header">
      <div class="workspace-shell__brand" @click="goHome">
        <p>JQPro Workspace</p>
        <strong>{{ currentUser?.displayName || '未登录用户' }}</strong>
      </div>

      <nav class="workspace-shell__nav">
        <button class="workspace-shell__nav-button" type="button" @click="goHome">角色首页</button>
        <button class="workspace-shell__nav-button" type="button" @click="goAccount">账户安全</button>
        <button class="workspace-shell__nav-button workspace-shell__nav-button--primary" type="button" @click="handleLogout">
          退出登录
        </button>
      </nav>
    </header>

    <main class="workspace-shell__content">
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { resolveRoleHome } from '@/core/session'

const router = useRouter()
const authStore = useAuthStore()

const currentUser = computed(() => authStore.currentUser)
const homePath = computed(() => {
  const roleCode = currentUser.value?.roleCode
  return roleCode ? resolveRoleHome(roleCode) : '/login'
})
const accountPath = computed(() => {
  switch (currentUser.value?.roleCode) {
    case 'STUDENT':
      return '/student/account'
    case 'COUNSELOR':
      return '/counselor/account'
    case 'ADMIN':
      return '/admin/account'
    default:
      return '/login'
  }
})

async function goHome(): Promise<void> {
  await router.push(homePath.value)
}

async function goAccount(): Promise<void> {
  await router.push(accountPath.value)
}

async function handleLogout(): Promise<void> {
  await authStore.signOut(true)
  await router.push('/login')
}
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700&family=Noto+Serif+SC:wght@400;500;600;700&display=swap');

.workspace-shell {
  min-height: 100vh;
  background: #efe9de;
}

.workspace-shell__header {
  position: sticky;
  top: 0;
  z-index: 20;
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
  padding: 1rem 1.5rem;
  border-bottom: 1px solid rgba(32, 27, 23, 0.1);
  background: rgba(248, 243, 236, 0.82);
  backdrop-filter: blur(18px);
}

.workspace-shell__brand {
  display: grid;
  gap: 0.25rem;
  cursor: pointer;
}

.workspace-shell__brand p,
.workspace-shell__nav-button {
  margin: 0;
  font: 600 0.76rem/1.3 'Manrope', sans-serif;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.workspace-shell__brand p {
  color: #6c645d;
}

.workspace-shell__brand strong {
  font: 600 1.1rem/1.3 'Noto Serif SC', 'Source Han Serif SC', serif;
  color: #201b17;
}

.workspace-shell__nav {
  display: flex;
  flex-wrap: wrap;
  gap: 0.7rem;
}

.workspace-shell__nav-button {
  min-height: 2.85rem;
  padding: 0 1rem;
  border: 1px solid rgba(32, 27, 23, 0.12);
  background: rgba(255, 255, 255, 0.46);
  color: #201b17;
  cursor: pointer;
  transition: transform 180ms ease, border-color 180ms ease, box-shadow 180ms ease;
}

.workspace-shell__nav-button--primary {
  border: none;
  background: linear-gradient(135deg, #64806e, #4d6657);
  color: #faf6f0;
  box-shadow: 0 16px 30px rgba(77, 102, 87, 0.2);
}

.workspace-shell__nav-button:hover {
  transform: translateY(-2px);
}

.workspace-shell__content {
  min-height: calc(100vh - 84px);
}

@media (max-width: 780px) {
  .workspace-shell__header {
    flex-direction: column;
    align-items: flex-start;
    padding: 0.9rem 1rem;
  }

  .workspace-shell__nav {
    width: 100%;
  }

  .workspace-shell__nav-button {
    flex: 1 1 100%;
  }
}
</style>

