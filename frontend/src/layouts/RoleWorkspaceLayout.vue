<template>
  <div class="workspace-shell" :class="themeClass">
    <aside class="workspace-sidebar">
      <div class="sidebar-brand">
        <div class="brand-mark"></div>
        <div>
          <p class="brand-eyebrow">心理服务工作台</p>
          <strong class="brand-name">JQPro</strong>
        </div>
      </div>

      <div class="user-card">
        <div class="user-avatar">
          <img v-if="sidebarAvatarUrl" :src="sidebarAvatarUrl" alt="avatar" class="user-avatar__img">
          <span v-else>{{ currentUser?.displayName?.slice(0, 1) || 'U' }}</span>
        </div>
        <div class="user-copy">
          <strong>{{ currentUser?.displayName || '未登录用户' }}</strong>
          <span>{{ roleLabel }}</span>
        </div>
      </div>

      <div class="sidebar-note">
        <span class="sidebar-note-label">{{ sidebarNote.label }}</span>
        <p>{{ sidebarNote.copy }}</p>
      </div>

      <nav class="sidebar-nav" aria-label="工作台导航">
        <button
            v-for="item in navItems"
            :key="item.path"
            class="nav-item"
            :class="{ 'is-active': isNavItemActive(item.path) }"
            :aria-current="isNavItemActive(item.path) ? 'page' : undefined"
            @click="router.push(item.path)"
        >
          <span class="nav-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none">
              <path
                  v-for="segment in navIcons[item.icon]"
                  :key="segment"
                  :d="segment"
              />
            </svg>
          </span>
          <span class="nav-copy">
            <span class="nav-title">{{ item.label }}</span>
            <span class="nav-caption">{{ item.caption }}</span>
          </span>
        </button>
      </nav>

      <div class="sidebar-footer">
        <button class="secondary-action" @click="router.push(accountPath)">账户安全</button>
        <button class="primary-action" @click="handleLogout">退出登录</button>
      </div>
    </aside>

    <main class="workspace-main">
      <div class="workspace-stage">
        <router-view />
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchStudentProfileApi } from '@/api/user'
import { useAuthStore } from '@/stores/auth'

type NavIconName =
    | 'home'
    | 'heart'
    | 'folder'
    | 'calendar'
    | 'users'
    | 'bell'
    | 'layers'
    | 'chart'

type NavItem = {
  path: string
  label: string
  caption: string
  icon: NavIconName
}

const navIcons: Record<NavIconName, string[]> = {
  home: [
    'M2.75 10.5L10.72 3.35a1.9 1.9 0 0 1 2.56 0l7.97 7.15',
    'M5.5 9.25V18.25A1.75 1.75 0 0 0 7.25 20h9.5a1.75 1.75 0 0 0 1.75-1.75V9.25',
    'M9.25 20V12.75h5.5V20'
  ],
  heart: [
    'M12 20.25s-6.75-4.02-6.75-9.28A3.97 3.97 0 0 1 12 7.89a3.97 3.97 0 0 1 6.75 3.08c0 5.26-6.75 9.28-6.75 9.28Z'
  ],
  folder: [
    'M3.5 6.75A1.75 1.75 0 0 1 5.25 5h4l1.8 1.8H18.75A1.75 1.75 0 0 1 20.5 8.55v8.7A1.75 1.75 0 0 1 18.75 19H5.25A1.75 1.75 0 0 1 3.5 17.25v-10.5Z',
    'M3.5 10.25h17'
  ],
  calendar: [
    'M7.25 3.5V6',
    'M16.75 3.5V6',
    'M3.5 9.25h17',
    'M5.25 5.25h13.5A1.75 1.75 0 0 1 20.5 7v11.25A1.75 1.75 0 0 1 18.75 20H5.25A1.75 1.75 0 0 1 3.5 18.25V7A1.75 1.75 0 0 1 5.25 5.25Z',
    'M8 13.25h3',
    'M13 13.25h3',
    'M8 16.5h3'
  ],
  users: [
    'M9.5 11.25a2.75 2.75 0 1 0 0-5.5a2.75 2.75 0 0 0 0 5.5Z',
    'M4.75 19.25v-1a3.75 3.75 0 0 1 3.75-3.75h2a3.75 3.75 0 0 1 3.75 3.75v1',
    'M17 10.5a2.25 2.25 0 1 0 0-4.5',
    'M18.75 19.25v-.75a3.4 3.4 0 0 0-2.5-3.27'
  ],
  bell: [
    'M9.75 20a2.25 2.25 0 0 0 4.5 0',
    'M6.5 15.25h11',
    'M7.35 15.25a1.1 1.1 0 0 1-.95-1.64l.92-1.61c.21-.37.33-.79.33-1.21V9.75a4.35 4.35 0 1 1 8.7 0v1.04c0 .42.12.84.33 1.21l.92 1.61a1.1 1.1 0 0 1-.95 1.64'
  ],
  layers: [
    'M12 4L19.25 8L12 12L4.75 8L12 4Z',
    'M4.75 12L12 16L19.25 12',
    'M4.75 16L12 20L19.25 16'
  ],
  chart: [
    'M4.5 19.5h15',
    'M7.5 17V11.5',
    'M12 17V7.5',
    'M16.5 17v-3.75'
  ]
}

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const assetOrigin = `${window.location.protocol}//${window.location.hostname}:8080`
const studentAvatarStorageKey = 'jqpro.student-avatar-url'
const avatarEventName = 'jqpro:student-avatar-updated'
const studentAvatarUrl = ref(localStorage.getItem(studentAvatarStorageKey))

const currentUser = computed(() => authStore.currentUser)
const sidebarAvatarUrl = computed(() => {
  if (currentUser.value?.roleCode === 'STUDENT') {
    return studentAvatarUrl.value || `${assetOrigin}/assets/avatars/roles/student-default.jpg`
  }
  if (currentUser.value?.roleCode === 'COUNSELOR') {
    return `${assetOrigin}/assets/avatars/roles/counselor-default.jpg`
  }
  if (currentUser.value?.roleCode === 'ADMIN') {
    return `${assetOrigin}/assets/avatars/roles/admin-default.jpg`
  }
  return null
})

const themeClass = computed(() => {
  switch (currentUser.value?.roleCode) {
    case 'STUDENT':
      return 'theme-student'
    case 'COUNSELOR':
      return 'theme-counselor'
    case 'ADMIN':
      return 'theme-admin'
    default:
      return 'theme-default'
  }
})

const roleLabel = computed(() => {
  switch (currentUser.value?.roleCode) {
    case 'STUDENT':
      return '学生工作台'
    case 'COUNSELOR':
      return '咨询师工作台'
    case 'ADMIN':
      return '管理员工作台'
    default:
      return '访客'
  }
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

const navItems = computed<NavItem[]>(() => {
  switch (currentUser.value?.roleCode) {
    case 'STUDENT':
      return [
        { path: '/student', label: '首页概览', caption: '个人入口与状态概览', icon: 'home' },
        { path: '/student/scales', label: '心理测评', caption: '量表列表、作答与结果', icon: 'heart' },
        { path: '/student/reports', label: '报告归档', caption: '历史报告与详细解释', icon: 'folder' },
        { path: '/student/appointments', label: '咨询预约', caption: '查看预约与进入沟通', icon: 'calendar' }
      ]
    case 'COUNSELOR':
      return [
        { path: '/counselor', label: '首页概览', caption: '学生与待处理事项', icon: 'home' },
        { path: '/counselor/students', label: '学生名单', caption: '查看已绑定学生', icon: 'users' },
        { path: '/counselor/appointments', label: '预约处理', caption: '接单、拒绝与跟进', icon: 'calendar' },
        { path: '/counselor/notifications', label: '通知中心', caption: '查看系统流转消息', icon: 'bell' }
      ]
    case 'ADMIN':
      return [
        { path: '/admin', label: '管理首页', caption: '系统总览', icon: 'home' },
        { path: '/admin/users', label: '用户管理', caption: '学生与咨询师账号治理', icon: 'users' },
        { path: '/admin/scales', label: '量表管理', caption: '量表与规则维护', icon: 'layers' },
        { path: '/admin/resources', label: '资源管理', caption: '心理资源与分类', icon: 'folder' },
        { path: '/admin/statistics', label: '统计分析', caption: '系统指标与趋势', icon: 'chart' },
        { path: '/admin/ai-tasks', label: 'AI 运维', caption: '自然语言解析与确认执行', icon: 'heart' },
        { path: '/admin/audit-logs', label: '审计日志', caption: '关键操作回溯', icon: 'bell' }
      ]
    default:
      return []
  }
})

function isNavItemActive(path: string): boolean {
  return route.path === path || route.path.startsWith(`${path}/`)
}

const sidebarNote = computed(() => {
  switch (currentUser.value?.roleCode) {
    case 'ADMIN':
      return {
        label: '治理与校准',
        copy: '在资源、量表、用户与审计之间保持统一节奏，让治理操作清晰、克制、可追踪。'
      }
    case 'COUNSELOR':
      return {
        label: '陪伴式支持',
        copy: '把预约、沟通与学生状态收束在同一节奏里，保持温和、清晰、值得信赖的体验。'
      }
    default:
      return {
        label: '陪伴式心理支持',
        copy: '在每一次浏览、测评与咨询之间，保持温柔、清晰、值得信赖的体验。'
      }
  }
})

async function handleLogout(): Promise<void> {
  await authStore.signOut(true)
  await router.push('/login')
}

function syncStudentAvatar(): void {
  studentAvatarUrl.value = localStorage.getItem(studentAvatarStorageKey)
}

async function syncStudentAvatarFromProfile(): Promise<void> {
  if (currentUser.value?.roleCode !== 'STUDENT') {
    return
  }
  try {
    const profile = await fetchStudentProfileApi()
    if (profile.avatarUrl) {
      localStorage.setItem(studentAvatarStorageKey, profile.avatarUrl)
      studentAvatarUrl.value = profile.avatarUrl
    }
  } catch {
    // 保持静默，避免因为头像拉取失败影响整体导航可用性。
  }
}

onMounted(() => {
  window.addEventListener(avatarEventName, syncStudentAvatar)
  void syncStudentAvatarFromProfile()
})

onBeforeUnmount(() => {
  window.removeEventListener(avatarEventName, syncStudentAvatar)
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700;800&family=Noto+Serif+SC:wght@500;600;700&display=swap');

.workspace-shell {
  --bg-sidebar: rgba(255, 250, 244, 0.88);
  --sidebar-edge: rgba(255, 255, 255, 0.6);
  --text-primary: #1f2220;
  --text-secondary: #6f6a63;
  --border-color: rgba(36, 34, 30, 0.08);
  --accent: #617a69;
  --accent-soft: rgba(97, 122, 105, 0.13);
  --nav-hover-bg: #f3f4f6;
  --active-text: #31443a;
  height: 100vh;
  width: 100vw;
  display: flex;
  overflow: hidden;
  background:
      radial-gradient(circle at top right, rgba(126, 147, 132, 0.16), transparent 18%),
      radial-gradient(circle at left 30%, rgba(211, 195, 173, 0.22), transparent 24%),
      linear-gradient(180deg, #f5f0e7 0%, #f7f4ee 100%);
  color: var(--text-primary);
  font-family: 'Manrope', sans-serif;
}

.theme-counselor {
  --accent: #47687f;
  --accent-soft: rgba(71, 104, 127, 0.14);
  --active-text: #29465b;
  background:
      radial-gradient(circle at top right, rgba(88, 119, 142, 0.15), transparent 18%),
      radial-gradient(circle at left 35%, rgba(208, 218, 230, 0.28), transparent 24%),
      linear-gradient(180deg, #eff4f8 0%, #f7fafc 100%);
}

.theme-admin {
  --bg-sidebar: rgba(255, 250, 244, 0.88);
  --sidebar-edge: rgba(255, 255, 255, 0.72);
  --text-primary: #232b25;
  --text-secondary: #7e756c;
  --border-color: rgba(45, 52, 45, 0.08);
  --accent: #8c7357;
  --accent-soft: rgba(140, 115, 87, 0.14);
  --nav-hover-bg: rgba(255, 255, 255, 0.66);
  --active-text: #47392b;
  background:
      radial-gradient(circle at top right, rgba(173, 151, 122, 0.18), transparent 18%),
      radial-gradient(circle at left 38%, rgba(191, 207, 197, 0.16), transparent 24%),
      linear-gradient(180deg, #f7f2e9 0%, #f5f1ea 100%);
}

.workspace-sidebar {
  flex-shrink: 0;
  width: 310px;
  height: 100%;
  padding: 1.6rem;
  box-sizing: border-box; /* 核心修复：防止 padding 撑破高度 */
  display: flex;
  flex-direction: column;
  gap: 1rem;
  overflow-y: auto;
  background:
      linear-gradient(180deg, var(--bg-sidebar), rgba(255, 255, 255, 0.52)),
      linear-gradient(180deg, transparent, transparent);
  border-right: 1px solid var(--sidebar-edge);
  box-shadow: 24px 0 48px rgba(31, 34, 32, 0.06);
  backdrop-filter: blur(26px);
}

.theme-admin .workspace-sidebar {
  background:
      linear-gradient(180deg, var(--bg-sidebar), rgba(255, 255, 255, 0.58)),
      linear-gradient(180deg, transparent, transparent);
  box-shadow: 24px 0 48px rgba(58, 52, 46, 0.08);
}

.sidebar-brand {
  display: flex;
  gap: 0.9rem;
  align-items: center;
}

.brand-mark {
  position: relative;
  width: 20px;
  height: 20px;
  border-radius: 6px;
  background: linear-gradient(135deg, var(--accent), rgba(255, 255, 255, 0.85));
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.08);
}

.brand-mark::after {
  content: '';
  position: absolute;
  inset: -6px;
  border-radius: 10px;
  background: radial-gradient(circle, var(--accent-soft), transparent 70%);
  z-index: -1;
}

.brand-eyebrow {
  margin: 0 0 0.2rem;
  font-size: 0.68rem;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: var(--text-secondary);
}

.brand-name {
  font-size: 1.28rem;
  font-family: 'Noto Serif SC', serif;
}

.user-card,
.sidebar-note {
  border-radius: 22px;
  border: 1px solid var(--border-color);
}

.user-card {
  display: grid;
  grid-template-columns: 52px minmax(0, 1fr);
  gap: 0.9rem;
  align-items: center;
  padding: 1rem;
  background: rgba(255, 255, 255, 0.48);
  box-shadow: 0 18px 36px rgba(58, 52, 46, 0.06);
}

.theme-admin .user-card {
  background: rgba(255, 255, 255, 0.03);
  box-shadow: none;
}

.user-avatar {
  width: 52px;
  height: 52px;
  border-radius: 16px;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, var(--accent), rgba(255, 255, 255, 0.7));
  color: white;
  font-weight: 700;
  font-size: 1rem;
  overflow: hidden;
}

.user-avatar__img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
}

.user-copy {
  display: grid;
  gap: 0.2rem;
}

.user-copy strong {
  font-size: 0.95rem;
}

.user-copy span {
  color: var(--text-secondary);
  font-size: 0.8rem;
}

.sidebar-note {
  padding: 1rem 1.05rem;
  background: rgba(255, 255, 255, 0.36);
}

.theme-admin .sidebar-note {
  background: rgba(255, 255, 255, 0.03);
}

.sidebar-note-label {
  display: inline-flex;
  margin-bottom: 0.45rem;
  padding: 0.3rem 0.7rem;
  border-radius: 999px;
  background: var(--accent-soft);
  color: var(--accent);
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.sidebar-note p {
  margin: 0;
  color: var(--text-secondary);
  font-size: 0.8rem;
  line-height: 1.65;
}

.sidebar-nav {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  align-items: stretch;
  justify-content: flex-start;
  gap: 0.5rem;
  min-height: 0;
}

.nav-item {
  position: relative;
  display: grid;
  grid-template-columns: 52px minmax(0, 1fr);
  align-items: center;
  gap: 0.9rem;
  width: 100%;
  min-height: 86px;
  height: max-content;
  padding: 0.95rem 1rem;
  margin: 0;
  border: 1px solid transparent;
  border-radius: 18px;
  background: transparent;
  color: var(--text-primary);
  text-align: left;
  cursor: pointer;
  flex: 0 0 auto;
  overflow: hidden;
  transition: all 0.3s ease;
}

.nav-item::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, var(--accent-soft), rgba(255, 255, 255, 0));
  opacity: 0;
  transition: opacity 0.3s ease;
}

.nav-item:hover:not(.is-active) {
  border-color: var(--border-color);
  background: var(--nav-hover-bg);
  transform: translateX(4px);
}

.nav-item:hover:not(.is-active)::before {
  opacity: 1;
}

.nav-item.is-active {
  border-color: rgba(255, 255, 255, 0.72);
  background: #ffffff;
  box-shadow:
      0 4px 12px rgba(0, 0, 0, 0.05),
      0 18px 28px rgba(40, 45, 42, 0.08);
  transform: translateX(6px);
}

.nav-icon {
  position: relative;
  z-index: 1;
  width: 52px;
  height: 52px;
  border-radius: 16px;
  display: grid;
  place-items: center;
  background: var(--accent-soft);
  color: var(--accent);
  transition: all 0.3s ease;
}

.nav-icon svg {
  width: 22px;
  height: 22px;
  stroke: currentColor;
  stroke-width: 1.7;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.nav-copy {
  position: relative;
  z-index: 1;
  display: grid;
  gap: 0.28rem;
  min-width: 0;
}

.nav-item.is-active .nav-icon {
  background: linear-gradient(135deg, var(--accent), rgba(255, 255, 255, 0.68));
  color: #ffffff;
  box-shadow: 0 12px 22px rgba(97, 122, 105, 0.22);
}

.theme-counselor .nav-item.is-active .nav-icon {
  box-shadow: 0 12px 22px rgba(71, 104, 127, 0.22);
}

.theme-admin .nav-item.is-active .nav-icon {
  box-shadow: 0 12px 22px rgba(232, 169, 62, 0.28);
}

.nav-item.is-active .nav-title {
  color: var(--active-text);
}

.nav-item.is-active .nav-caption {
  color: #61707c;
}

.theme-admin .nav-item.is-active .nav-caption {
  color: #6b6f76;
}

.nav-title {
  font-size: 0.97rem;
  font-weight: 800;
  line-height: 1.25;
}

.nav-caption {
  color: var(--text-secondary);
  font-size: 0.78rem;
  line-height: 1.58;
}

.sidebar-footer {
  display: grid;
  gap: 0.75rem;
  flex-shrink: 0;
  padding-top: 0.2rem;
}

.primary-action,
.secondary-action {
  min-height: 2.95rem;
  border-radius: 16px;
  border: none;
  cursor: pointer;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  transition: all 0.25s ease;
}

.primary-action:hover,
.secondary-action:hover {
  transform: translateY(-1px);
}

.primary-action {
  background: linear-gradient(135deg, var(--accent), rgba(255, 255, 255, 0.2));
  color: white;
  box-shadow: 0 12px 24px rgba(97, 122, 105, 0.2);
}

.theme-counselor .primary-action {
  box-shadow: 0 12px 24px rgba(71, 104, 127, 0.2);
}

.theme-admin .primary-action {
  box-shadow: 0 12px 24px rgba(232, 169, 62, 0.18);
}

.secondary-action {
  border: 1px solid var(--border-color);
  background: rgba(255, 255, 255, 0.22);
  color: var(--text-primary);
}

.theme-admin .secondary-action {
  background: rgba(255, 255, 255, 0.02);
}

.workspace-main {
  flex: 1;
  height: 100%;
  min-width: 0;
  min-height: 0;
  padding: 1.75rem;
  overflow-x: hidden;
  overflow-y: auto;
  box-sizing: border-box;
}

.workspace-stage {
  min-height: 100%;
}

@media (max-width: 1080px) {
  .workspace-shell {
    flex-direction: column;
    height: auto;
    min-height: 100vh;
    overflow: visible;
  }

  .workspace-sidebar {
    min-height: auto;
    width: 100%;
    height: auto;
    border-right: none;
    border-bottom: 1px solid var(--sidebar-edge);
    overflow: visible;
  }

  .workspace-main {
    height: auto;
    min-height: 0;
    padding: 1rem;
    overflow: visible;
  }
}

@media (max-width: 640px) {
  .workspace-sidebar {
    padding: 1rem;
  }

  .nav-item {
    grid-template-columns: 44px minmax(0, 1fr);
    min-height: 78px;
    padding: 0.85rem 0.9rem;
  }

  .nav-icon {
    width: 44px;
    height: 44px;
    border-radius: 14px;
  }

  .workspace-main {
    padding: 0.85rem;
  }
}
</style>
