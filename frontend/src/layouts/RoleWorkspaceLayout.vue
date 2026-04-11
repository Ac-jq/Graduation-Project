<template>
  <div class="workspace-viewport" :class="themeClass">
    <aside class="workspace-sidebar">
      <div class="sidebar-brand">
        <div class="brand-logo"></div>
        <span class="brand-name">JQPro</span>
      </div>

      <div class="user-profile">
        <div class="avatar">
          {{ currentUser?.displayName?.charAt(0) || 'U' }}
        </div>
        <div class="info">
          <strong>{{ currentUser?.displayName || '未登录' }}</strong>
          <span>{{ roleLabel }}</span>
        </div>
      </div>

      <nav class="sidebar-nav">
        <button class="nav-item" :class="{ 'is-active': $route.path === homePath }" @click="goHome">
          <span class="nav-icon">⌂</span>
          工作台首页
        </button>
        <button class="nav-item" :class="{ 'is-active': $route.path === accountPath }" @click="goAccount">
          <span class="nav-icon">⚙</span>
          账户安全
        </button>
      </nav>

      <div class="sidebar-footer">
        <button class="logout-btn" @click="handleLogout">
          退出登录
        </button>
      </div>
    </aside>

    <main class="workspace-main">
      <div class="page-container">
        <router-view />
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { resolveRoleHome } from '@/core/session'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const currentUser = computed(() => authStore.currentUser)

// 动态主题类名计算
const themeClass = computed(() => {
  const role = currentUser.value?.roleCode
  if (role === 'STUDENT') return 'theme-student'
  if (role === 'COUNSELOR') return 'theme-counselor'
  if (role === 'ADMIN') return 'theme-admin'
  return 'theme-default'
})

const roleLabel = computed(() => {
  const role = currentUser.value?.roleCode
  if (role === 'STUDENT') return '学生用户'
  if (role === 'COUNSELOR') return '心理咨询师'
  if (role === 'ADMIN') return '系统管理员'
  return '游客'
})

const homePath = computed(() => {
  const roleCode = currentUser.value?.roleCode
  return roleCode ? resolveRoleHome(roleCode) : '/login'
})

const accountPath = computed(() => {
  switch (currentUser.value?.roleCode) {
    case 'STUDENT': return '/student/account'
    case 'COUNSELOR': return '/counselor/account'
    case 'ADMIN': return '/admin/account'
    default: return '/login'
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
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600&family=Noto+Serif+SC:wght@400;500;600&family=JetBrains+Mono:wght@400;500&display=swap');

/* ==========================================
   全局主题变量定义 (Theming System)
   我们在外层容器定义变量，子页面将继承这些变量
========================================== */

.workspace-viewport {
  display: flex;
  height: 100vh;
  width: 100vw;
  overflow: hidden;
  transition: background-color 0.5s ease, color 0.5s ease;

  /* 基础兜底变量 */
  --bg-app: #f5f5f5;
  --bg-sidebar: #ffffff;
  --bg-surface: #ffffff;
  --text-primary: #111111;
  --text-secondary: #666666;
  --border-color: rgba(0,0,0,0.1);
  --accent-color: #000000;
  --radius-base: 8px;
  --font-sans: 'Manrope', sans-serif;
  --font-serif: 'Noto Serif SC', serif;
  --font-mono: 'JetBrains Mono', monospace;

  background-color: var(--bg-app);
  color: var(--text-primary);
  font-family: var(--font-sans);
}

/* 🌿 学生端：安静、疗愈、有机形态 */
.theme-student {
  --bg-app: #F4F1EA;
  --bg-sidebar: #EBE7E0;
  --bg-surface: #FFFFFF;
  --text-primary: #2C302B;
  --text-secondary: #7A7D75;
  --border-color: rgba(44, 48, 43, 0.06);
  --accent-color: #6A7A6B; /* 柔和的鼠尾草绿 */
  --radius-base: 24px;     /* 夸张的圆角表现陪伴感 */
}

/* 📐 咨询师端：专业、秩序、严谨 */
.theme-counselor {
  --bg-app: #F0F4F8;
  --bg-sidebar: #FFFFFF;
  --bg-surface: #FFFFFF;
  --text-primary: #1E293B;
  --text-secondary: #64748B;
  --border-color: #E2E8F0;
  --accent-color: #3B82F6; /* 专业理性的蓝色 */
  --radius-base: 6px;      /* 严谨的微圆角 */
}

/* ⚙️ 管理员端：工业感、控制中枢、暗色/高反差 */
.theme-admin {
  --bg-app: #0F1115;
  --bg-sidebar: #16181D;
  --bg-surface: #1E2128;
  --text-primary: #E2E8F0;
  --text-secondary: #94A3B8;
  --border-color: #2D313A;
  --accent-color: #F59E0B; /* 警示/行动感的琥珀色 */
  --radius-base: 0px;      /* 绝对的直角结构 */
}

/* ==========================================
   布局与组件样式
========================================== */

.workspace-sidebar {
  width: 260px;
  background-color: var(--bg-sidebar);
  border-right: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  padding: 2rem 1.5rem;
  z-index: 10;
}

.sidebar-brand {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 3rem;
}

.brand-logo {
  width: 24px;
  height: 24px;
  background-color: var(--accent-color);
  border-radius: calc(var(--radius-base) / 2);
}

.brand-name {
  font-weight: 700;
  font-size: 1.2rem;
  letter-spacing: 0.05em;
  color: var(--text-primary);
}

.theme-admin .brand-name {
  font-family: var(--font-mono);
}

.user-profile {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 2rem;
  padding: 1rem;
  background: var(--bg-app);
  border-radius: var(--radius-base);
  border: 1px solid var(--border-color);
}

.avatar {
  width: 36px;
  height: 36px;
  background: var(--accent-color);
  color: var(--bg-surface);
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: calc(var(--radius-base) / 2);
  font-weight: 600;
}

.theme-student .avatar { border-radius: 50%; }

.info {
  display: flex;
  flex-direction: column;
}

.info strong {
  font-size: 0.9rem;
  color: var(--text-primary);
}

.info span {
  font-size: 0.75rem;
  color: var(--text-secondary);
}

.sidebar-nav {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  flex-grow: 1;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.85rem 1rem;
  background: transparent;
  border: none;
  width: 100%;
  text-align: left;
  color: var(--text-secondary);
  font-size: 0.95rem;
  font-weight: 500;
  border-radius: var(--radius-base);
  cursor: pointer;
  transition: all 0.2s ease;
}

.nav-icon {
  font-size: 1.2rem;
}

.nav-item:hover {
  background: rgba(0,0,0,0.03);
  color: var(--text-primary);
}

.theme-admin .nav-item:hover { background: rgba(255,255,255,0.05); }

.nav-item.is-active {
  background: var(--accent-color);
  color: var(--bg-sidebar);
}

.sidebar-footer {
  margin-top: auto;
  padding-top: 1.5rem;
  border-top: 1px solid var(--border-color);
}

.logout-btn {
  width: 100%;
  padding: 0.85rem;
  background: transparent;
  border: 1px solid var(--border-color);
  color: var(--text-primary);
  border-radius: var(--radius-base);
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.logout-btn:hover {
  border-color: var(--text-primary);
  background: var(--text-primary);
  color: var(--bg-sidebar);
}

.workspace-main {
  flex-grow: 1;
  overflow-y: auto;
  padding: 2rem;
}

.page-container {
  max-width: 1200px;
  margin: 0 auto;
  min-height: 100%;
  /* 子页面可以通过 var(--bg-surface) 等变量来绘制卡片 */
}
</style>