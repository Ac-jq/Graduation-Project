<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { fetchStudentAiSessionsApi } from '@/api/ai-chat'
import { fetchStudentAppointmentsApi } from '@/api/appointment'
import { fetchStudentReportsApi } from '@/api/assessment'
import { fetchNotificationsApi } from '@/api/notification'
import { fetchResourcesApi } from '@/api/resource'
import { fetchStudentProfileApi } from '@/api/user'
import type { AiChatSession, Appointment, NotificationItem, ReportSummary, ResourceSummary, StudentProfile } from '@/api/types'
import { toErrorMessage } from '@/views/shared/page-logic'

const router = useRouter()
const loading = ref(false)
const errorMessage = ref('')
const profile = ref<StudentProfile | null>(null)
const latestReport = ref<ReportSummary | null>(null)
const latestAppointment = ref<Appointment | null>(null)
const featuredResources = ref<ResourceSummary[]>([])
const activeSession = ref<AiChatSession | null>(null)
const notifications = ref<NotificationItem[]>([])

// UI State 控制下拉菜单显示/隐藏
const showDropdown = ref(false)

const unreadCount = computed(() => notifications.value.filter((item) => !item.read).length)

function resolveLevelLabel(levelCode?: string | null): string {
  switch (levelCode) {
    case 'LOW': return '状态平稳'
    case 'MEDIUM': return '需适度关注'
    case 'HIGH': return '建议重点关注'
    default: return '待生成'
  }
}

function resolveAppointmentStatusLabel(status?: string | null): string {
  switch (status) {
    case 'PENDING': return '待处理'
    case 'ACCEPTED': return '已接受'
    case 'IN_PROGRESS': return '沟通中'
    case 'COMPLETED': return '已完成'
    case 'REJECTED': return '未通过'
    case 'CANCELLED': return '已取消'
    default: return '待预约'
  }
}

async function loadDashboard(): Promise<void> {
  loading.value = true
  errorMessage.value = ''

  try {
    const [profileData, reports, appointments, resources, sessions, notificationData] = await Promise.all([
      fetchStudentProfileApi(),
      fetchStudentReportsApi(),
      fetchStudentAppointmentsApi(),
      fetchResourcesApi(),
      fetchStudentAiSessionsApi(),
      fetchNotificationsApi()
    ])

    profile.value = profileData
    latestReport.value = reports[0] ?? null
    latestAppointment.value = appointments[0] ?? null
    featuredResources.value = resources.slice(0, 2)
    activeSession.value = sessions[0] ?? null
    notifications.value = notificationData
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    loading.value = false
  }
}

async function openScaleArchive(): Promise<void> {
  await router.push({ name: 'student-scales' })
}

async function openAppointmentCenter(): Promise<void> {
  await router.push({ name: 'student-appointment-slots' })
}

async function open收藏数(): Promise<void> {
  await router.push({ name: 'student-favorites' })
}

async function openAiSession(): Promise<void> {
  if (activeSession.value) {
    await router.push({ name: 'student-ai-session-detail', params: { sessionId: activeSession.value.sessionId } })
    return
  }
  await router.push({ name: 'student-ai-sessions' })
}

async function openResource(resourceId: number): Promise<void> {
  await router.push({ name: 'student-resource-detail', params: { resourceId } })
}

async function openNotifications(): Promise<void> {
  await router.push({ name: 'student-notifications' })
}

// 下拉菜单点击事件处理
function handleDropdownClick(action: string) {
  showDropdown.value = false
  if (action === 'role') {
    router.push({ name: 'student-profile' })
  }
  // 其他路由根据需求补充
}

onMounted(() => {
  void loadDashboard()
})
</script>

<template>
  <div class="app-layout">
    <div class="noise-overlay" />

    <div class="main-card-container">

      <header class="glass-nav">
        <div class="brand-mark">
          心语<span>空间</span>
        </div>

        <div class="nav-actions">
          <button class="notification-btn" type="button" @click="openNotifications">
            <span v-if="unreadCount" class="notification-dot" />
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true">
              <path stroke-linecap="round" stroke-linejoin="round" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
            </svg>
          </button>

          <div class="profile-dropdown-wrapper" @mouseleave="showDropdown = false">
            <button class="avatar-btn" type="button" @click="showDropdown = !showDropdown">
              <img v-if="profile?.avatarUrl" :src="profile.avatarUrl" alt="avatar">
              <div v-else class="avatar-fallback">{{ (profile?.displayName || '学').slice(0, 1) }}</div>
              <span class="avatar-name">{{ profile?.displayName || '学生' }}</span>
              <svg class="chevron" :class="{ 'chevron-up': showDropdown }" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M19 9l-7 7-7-7" />
              </svg>
            </button>

            <transition name="fade-slide">
              <ul v-show="showDropdown" class="dropdown-menu">
                <li @click="handleDropdownClick('role')">角色</li>
                <li @click="handleDropdownClick('home')">首页</li>
                <li @click="handleDropdownClick('security')">账户安全</li>
                <li class="logout" @click="handleDropdownClick('logout')">退出登录</li>
              </ul>
            </transition>
          </div>
        </div>
      </header>

      <main class="dashboard-content">
        <section class="compact-hero-grid">
          <div class="hero-left">
            <div class="hero-titles">
              <h1>探索内在的平静。</h1>
              <div class="user-tags">
                <span>{{ profile?.displayName || '未命名学生' }}</span>
                <span class="dot" />
                <span>{{ profile?.college || '学院未填写' }}</span>
                <span class="dot" />
                <span>{{ profile?.grade || '年级未填写' }}</span>
              </div>
            </div>

            <div class="hero-quick-links">
              <button class="link-item" type="button" @click="openScaleArchive">
                <div class="link-content">
                  <span class="icon-box icon-box--sage">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path stroke-linecap="round" stroke-linejoin="round" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" /></svg>
                  </span>
                  <span>心理测评档案</span>
                </div>
                <span class="arrow">&rarr;</span>
              </button>
              <button class="link-item" type="button" @click="openAppointmentCenter">
                <div class="link-content">
                  <span class="icon-box icon-box--sun">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path stroke-linecap="round" stroke-linejoin="round" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" /></svg>
                  </span>
                  <span>人工咨询预约</span>
                </div>
                <span class="arrow">&rarr;</span>
              </button>
              <button class="link-item" type="button" @click="open收藏数">
                <div class="link-content">
                  <span class="icon-box icon-box--clay">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path stroke-linecap="round" stroke-linejoin="round" d="M5 5a2 2 0 012-2h10a2 2 0 012 2v16l-7-3.5L5 21V5z" /></svg>
                  </span>
                  <span>我的资源收藏</span>
                </div>
                <span class="arrow">&rarr;</span>
              </button>
            </div>
          </div>

          <div class="compact-aura-stage">
            <div class="aura-bg">
              <div class="aura-blob aura-blob--sage" />
              <div class="aura-blob aura-blob--clay" />
              <div class="aura-blob aura-blob--sun" />
              <div class="aura-glass-layer" />
            </div>

            <div class="aura-inner">
              <div class="aura-text">
                <div class="aura-indicator">
                  <span class="pulse-dot" />
                  <span>{{ activeSession ? 'AI 会话已接通' : 'AI 会话已就绪' }}</span>
                </div>
                <h2>无论昼夜，随时倾听心声。</h2>
              </div>
              <button class="btn-primary" type="button" @click="openAiSession">
                {{ activeSession ? '继续倾诉' : '发起倾诉' }}
              </button>
            </div>
          </div>
        </section>

        <section class="dashboard-modules">
          <div class="status-panel">
            <div class="module-header">
              <span class="section-label">近期状态</span>
            </div>

            <div class="status-cards-wrapper">
              <article class="status-card">
                <div class="status-header">
                  <span class="status-date">{{ latestReport ? new Date(latestReport.createdAt).toLocaleDateString('zh-CN') : '暂无记录' }} / 报告</span>
                  <button class="text-btn" type="button" @click="openScaleArchive">历史记录</button>
                </div>
                <h3>{{ latestReport?.scaleName || '尚无测评报告' }}</h3>
                <div class="status-tags">
                  <span class="tag tag--clay">{{ resolveLevelLabel(latestReport?.levelCode) }}</span>
                  <span class="tag-text">{{ latestReport ? `总分 ${latestReport.totalScore}` : '完成测评后展示' }}</span>
                </div>
              </article>

              <article class="status-card">
                <div class="status-header">
                  <span class="status-date">{{ latestAppointment ? new Date(latestAppointment.createdAt).toLocaleDateString('zh-CN') : '暂无记录' }} / 预约</span>
                  <button class="text-btn" type="button" @click="router.push({ name: 'student-appointments' })">查看全部</button>
                </div>
                <h3>{{ latestAppointment ? '一对一心理辅导（匿名）' : '尚无预约记录' }}</h3>
                <div class="status-tags">
                  <span class="tag tag--sage">{{ resolveAppointmentStatusLabel(latestAppointment?.status) }}</span>
                  <span class="tag-text">
                    {{ latestAppointment
                      ? `${new Date(latestAppointment.startTime).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })} - ${new Date(latestAppointment.endTime).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })}`
                      : '预约后展示时间段' }}
                  </span>
                </div>
              </article>
            </div>
          </div>

          <div class="resources-panel">
            <div class="module-header">
              <span class="section-label">精选资源</span>
              <button class="text-btn" type="button" @click="router.push({ name: 'student-resources' })">浏览全部</button>
            </div>

            <div v-if="loading" class="empty-state">正在同步首页数据...</div>
            <div v-else-if="errorMessage" class="empty-state empty-state--error">{{ errorMessage }}</div>

            <div v-else class="compact-resource-grid">
              <article v-for="resource in featuredResources" :key="resource.resourceId" class="resource-item" @click="openResource(resource.resourceId)">
                <div class="resource-img-box">
                  <img v-if="resource.coverUrl" :src="resource.coverUrl" :alt="resource.title">
                  <div v-else class="img-placeholder">{{ resource.title.slice(0, 2) }}</div>
                </div>
                <div class="resource-info">
                  <h4>{{ resource.title }}</h4>
                  <span class="category">{{ resource.categoryName }}</span>
                </div>
              </article>
            </div>
          </div>
        </section>
      </main>
    </div>
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Noto+Sans+SC:wght@300;400;500&family=Noto+Serif+SC:wght@400;600;900&display=swap');

/* =========================================
   全局背景：更温暖、治愈的底色
========================================= */
.app-layout {
  min-height: 100vh;
  background-color: #f6f7f4; /* 调暖一丝丝，增加呼吸感 */
  padding: 3vh 4vw;
  display: flex;
  justify-content: center;
  align-items: flex-start;
  font-family: 'Noto Sans SC', sans-serif;
  color: #2c352d;
  position: relative;
}

.noise-overlay {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 0;
  opacity: 0.035;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noiseFilter'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.85' numOctaves='3' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noiseFilter)'/%3E%3C/svg%3E");
}

.main-card-container {
  width: 100%;
  max-width: 1400px;
  background: #fdfbf7;
  border-radius: 24px;
  /* 增加阴影的暖色调，显得有温度 */
  box-shadow: 0 24px 60px rgba(135, 126, 115, 0.08), 0 4px 12px rgba(44, 53, 45, 0.03);
  overflow: hidden;
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
}

/* =========================================
   顶部导航：Q弹的交互
========================================= */
.glass-nav {
  position: sticky;
  top: 0;
  z-index: 100;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 32px;
  background: rgba(253, 251, 247, 0.8);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-bottom: 1px solid rgba(44, 53, 45, 0.04);
}

.brand-mark {
  font-family: 'Noto Serif SC', serif;
  font-weight: 900;
  font-size: 1.4rem;
  letter-spacing: 0.15em;
  /* 鼠标悬浮时有微弹放大 */
  transition: transform 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.brand-mark:hover {
  transform: scale(1.03) rotate(-1deg);
}
.brand-mark span {
  color: #8fa08e;
  font-style: italic;
}

.nav-actions { display: flex; align-items: center; gap: 16px; }

.notification-btn {
  position: relative;
  background: rgba(44, 53, 45, 0.04);
  border: none;
  width: 38px;
  height: 38px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #2c352d;
  /* 果冻弹簧曲线 */
  transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.notification-btn:hover {
  background: #fff;
  transform: translateY(-3px) scale(1.1);
  box-shadow: 0 8px 20px rgba(226, 180, 154, 0.25); /* 阳光暖色阴影 */
  color: #d68762;
}
.notification-btn svg { width: 18px; height: 18px; }
.notification-dot {
  position: absolute;
  top: 8px; right: 8px; width: 6px; height: 6px;
  border-radius: 50%;
  background: #e88656;
  box-shadow: 0 0 0 2px #fdfbf7;
  /* 带有呼吸感的闪烁 */
  animation: pulse-ring-warm 1s infinite;
}

/* 下拉菜单 & 头像 */
.profile-dropdown-wrapper { position: relative; }

.avatar-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  background: transparent;
  border: 1px solid transparent;
  padding: 4px 10px 4px 4px;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.avatar-btn:hover {
  background: #fff;
  box-shadow: 0 6px 16px rgba(44, 53, 45, 0.06);
  transform: translateY(-1px);
}
.avatar-btn img, .avatar-fallback {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  object-fit: cover;
}
.avatar-fallback {
  background: rgba(143, 160, 142, 0.2);
  display: grid;
  place-items: center;
  font-family: 'Noto Serif SC', serif;
  font-size: 0.85rem;
}
.avatar-name { font-size: 0.9rem; font-weight: 500; }
.chevron {
  width: 14px; height: 14px; opacity: 0.6;
  transition: transform 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.chevron-up { transform: rotate(180deg); }

.dropdown-menu {
  position: absolute;
  top: calc(100% + 10px);
  right: 0;
  width: 140px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(44, 53, 45, 0.08);
  border-radius: 14px;
  box-shadow: 0 16px 40px rgba(135, 126, 115, 0.12);
  list-style: none;
  padding: 8px;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
  transform-origin: top right;
}

/* 透明桥梁，解决鼠标划过消失问题 */
.dropdown-menu::before {
  content: '';
  position: absolute;
  top: -12px; left: 0; right: 0; height: 12px;
  background: transparent;
}

.dropdown-menu li {
  padding: 10px 12px;
  border-radius: 8px;
  font-size: 0.85rem;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.dropdown-menu li:hover {
  background: rgba(143, 160, 142, 0.1);
  color: #8fa08e;
  transform: translateX(4px);
}
.dropdown-menu li.logout {
  color: #d16b6b;
  border-top: 1px solid rgba(44, 53, 45, 0.04);
  margin-top: 4px;
  border-radius: 0 0 8px 8px;
}
.dropdown-menu li.logout:hover { background: rgba(209, 107, 107, 0.08); }

.fade-slide-enter-active, .fade-slide-leave-active {
  transition: opacity 0.3s cubic-bezier(0.34, 1.56, 0.64, 1), transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.fade-slide-enter-from, .fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-10px) scale(0.95);
}

/* =========================================
   主内容 & 错落弹跳入场动画
========================================= */
.dashboard-content {
  padding: 32px 48px 48px;
  display: flex;
  flex-direction: column;
  gap: 40px;
}

@keyframes springUp {
  0% { opacity: 0; transform: translateY(30px); }
  60% { opacity: 1; transform: translateY(-3px); } /* 微弱的过冲回弹 */
  100% { opacity: 1; transform: translateY(0); }
}

.hero-left {
  opacity: 0;
  animation: springUp 0.8s cubic-bezier(0.34, 1.56, 0.64, 1) forwards;
}
.compact-aura-stage {
  opacity: 0;
  animation: springUp 0.8s cubic-bezier(0.34, 1.56, 0.64, 1) 0.15s forwards;
}
.status-panel {
  opacity: 0;
  animation: springUp 0.8s cubic-bezier(0.34, 1.56, 0.64, 1) 0.3s forwards;
}
.resources-panel {
  opacity: 0;
  animation: springUp 0.8s cubic-bezier(0.34, 1.56, 0.64, 1) 0.45s forwards;
}

/* =========================================
   英雄区与快捷图标
========================================= */
.compact-hero-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 24px;
}
@media (min-width: 1024px) {
  .compact-hero-grid {
    grid-template-columns: 4fr 5fr;
    gap: 48px;
    align-items: center;
  }
}

.hero-left { display: flex; flex-direction: column; gap: 32px; }

.hero-titles h1 {
  margin: 0;
  font-family: 'Noto Serif SC', serif;
  font-size: clamp(2.2rem, 4.5vw, 3.2rem);
  line-height: 1.1;
  /* 渐变中加入了一丝丝治愈的暖灰色 */
  background: linear-gradient(135deg, #2b332c 0%, #687a6a 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.user-tags {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 16px;
  font-size: 0.75rem;
  color: rgba(44, 53, 45, 0.5);
  text-transform: uppercase;
  letter-spacing: 0.1em;
}
.dot { width: 4px; height: 4px; border-radius: 50%; background: #e2b49a; }

.hero-quick-links { display: flex; flex-direction: column; gap: 4px; }
.link-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px 12px 0;
  border: none;
  background: transparent;
  border-radius: 12px;
  font-family: 'Noto Serif SC', serif;
  font-size: 0.95rem;
  color: #2c352d;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.link-content { display: flex; align-items: center; gap: 14px; }
.icon-box {
  width: 36px; height: 36px;
  border-radius: 10px;
  display: grid; place-items: center;
  transition: transform 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.icon-box--sage { background: rgba(143, 160, 142, 0.15); color: #536b52; }
.icon-box--sun { background: rgba(226, 180, 154, 0.18); color: #b06541; }
.icon-box--clay { background: rgba(189, 115, 82, 0.12); color: #a16449; }

.link-item:hover {
  background: #fff;
  padding-left: 12px; /* 产生向右推进的吸附感 */
  box-shadow: 0 4px 16px rgba(44, 53, 45, 0.04);
}
.link-item:hover .icon-box { transform: scale(1.1); }
.arrow {
  opacity: 0;
  transform: translateX(-10px);
  transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.link-item:hover .arrow {
  opacity: 1;
  transform: translateX(0);
}

/* =========================================
   Aura 情绪舞台：阳光与呼吸
========================================= */
.compact-aura-stage {
  position: relative;
  min-height: 200px;
  border-radius: 24px;
  display: flex;
  align-items: center;
  padding: 24px 32px;
  overflow: hidden;
  box-shadow: 0 12px 32px rgba(135, 126, 115, 0.15);
  transition: transform 0.5s cubic-bezier(0.34, 1.56, 0.64, 1), box-shadow 0.5s ease;
}
.compact-aura-stage:hover {
  transform: translateY(-6px) scale(1.02);
  box-shadow: 0 24px 48px rgba(135, 126, 115, 0.2);
}

.aura-bg { position: absolute; inset: 0; background: rgba(211, 221, 209, 0.15); z-index: 0; }
.aura-blob {
  position: absolute;
  filter: blur(40px);
  animation: aura-float 12s ease-in-out infinite alternate;
}
.aura-blob--sage {
  top: -20%; left: -10%; width: 70%; height: 140%;
  background: linear-gradient(135deg, rgba(143, 160, 142, 0.6), transparent);
}
.aura-blob--clay {
  bottom: -20%; right: -10%; width: 60%; height: 120%;
  background: linear-gradient(135deg, rgba(226, 180, 154, 0.5), transparent);
  animation-direction: reverse;
}
/* 核心：阳光光晕 */
.aura-blob--sun {
  top: 10%; left: 30%; width: 80%; height: 100%;
  background: radial-gradient(circle, rgba(255, 194, 122, 0.35) 0%, transparent 70%);
  animation: aura-breathe 8s ease-in-out infinite alternate;
}

.aura-glass-layer { position: absolute; inset: 0; backdrop-filter: blur(24px); }

.aura-inner {
  position: relative;
  z-index: 1;
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 24px;
}
@media (max-width: 640px) {
  .aura-inner { flex-direction: column; align-items: flex-start; }
}

.aura-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.75rem;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: #536b52;
  margin-bottom: 8px;
}
.pulse-dot {
  width: 8px; height: 8px;
  border-radius: 50%;
  background: #8fa08e;
  animation: pulse-ring 1s infinite;
}
.aura-text h2 {
  margin: 0;
  font-family: 'Noto Serif SC', serif;
  font-size: 1.6rem;
  line-height: 1.3;
}

.btn-primary {
  flex-shrink: 0;
  padding: 14px 32px;
  background: #2c352d;
  color: #fff;
  border: none;
  border-radius: 14px;
  font-size: 0.95rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
  box-shadow: 0 8px 24px rgba(44, 53, 45, 0.25);
}
.btn-primary:hover {
  background: #536b52;
  transform: translateY(-4px) scale(1.05);
  box-shadow: 0 12px 32px rgba(83, 107, 82, 0.4);
}
.btn-primary:active { transform: scale(0.95); }

/* =========================================
   底部模块：悬浮呼吸与拟态高光
========================================= */
.dashboard-modules {
  display: grid;
  grid-template-columns: 1fr;
  gap: 40px;
}
@media (min-width: 1024px) {
  .dashboard-modules { grid-template-columns: 1fr 1.5fr; gap: 64px; }
}

.module-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.section-label {
  font-size: 0.8rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: #8a968a;
}
.text-btn {
  background: none; border: none; font-size: 0.8rem; color: #8a968a;
  cursor: pointer; transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.text-btn:hover { color: #bd7352; transform: translateX(-4px); }

/* 状态卡片 */
.status-cards-wrapper { display: flex; flex-direction: column; gap: 16px; }
.status-card {
  padding: 24px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.9);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 1), 0 4px 16px rgba(135, 126, 115, 0.04);
  transition: all 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.status-card:hover {
  background: #fff;
  transform: translateY(-6px);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 1), 0 16px 40px rgba(135, 126, 115, 0.12);
}
.status-header { display: flex; justify-content: space-between; margin-bottom: 12px; }
.status-date { font-size: 0.75rem; font-weight: 500; color: #a1aa9f; }
.status-card h3 { margin: 0 0 16px 0; font-size: 1.1rem; font-weight: 600; }
.status-tags { display: flex; align-items: center; gap: 12px; }
.tag { padding: 4px 12px; border-radius: 8px; font-size: 0.75rem; font-weight: 600; }
.tag--clay { background: rgba(226, 180, 154, 0.2); color: #b06541; }
.tag--sage { background: rgba(143, 160, 142, 0.2); color: #536b52; }
.tag-text { font-size: 0.85rem; color: #788577; }

/* 资源卡片 */
.compact-resource-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 24px;
}
.resource-item {
  cursor: pointer;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.5);
  border: 1px solid rgba(255, 255, 255, 0.8);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 1), 0 4px 16px rgba(135, 126, 115, 0.04);
  padding: 14px;
  transition: all 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.resource-item:hover {
  background: #fff;
  transform: translateY(-8px) scale(1.02);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 1), 0 20px 48px rgba(135, 126, 115, 0.15);
}
.resource-img-box {
  width: 100%; aspect-ratio: 16 / 10;
  border-radius: 12px;
  overflow: hidden;
  margin-bottom: 16px;
  background: #eae8e3;
}
.resource-img-box img {
  width: 100%; height: 100%; object-fit: cover;
  transition: transform 0.8s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.resource-item:hover .resource-img-box img { transform: scale(1.1); }
.img-placeholder {
  width: 100%; height: 100%; display: grid; place-items: center;
  font-size: 1.2rem; color: rgba(44, 53, 45, 0.3); font-family: 'Noto Serif SC', serif;
}
.resource-info h4 {
  margin: 0 0 6px 0; font-size: 1rem; font-weight: 600; color: #2c352d;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
  transition: color 0.3s;
}
.resource-item:hover .resource-info h4 { color: #bd7352; }
.category { font-size: 0.8rem; font-weight: 500; color: #a1aa9f; }

.empty-state {
  padding: 32px; text-align: center; font-size: 0.9rem; color: #a1aa9f;
  background: rgba(255, 255, 255, 0.5); border-radius: 20px; border: 2px dashed rgba(255, 255, 255, 0.8);
}
.empty-state--error { color: #c9655c; border-color: rgba(201, 101, 92, 0.2); }

/* =========================================
   动画定义：呼吸与阳光
========================================= */
@keyframes aura-float {
  0% { transform: translate(0, 0) scale(1) rotate(0deg); }
  100% { transform: translate(6%, 4%) scale(1.1) rotate(5deg); }
}
@keyframes aura-breathe {
  0% { transform: scale(0.9); opacity: 0.6; }
  100% { transform: scale(1.2); opacity: 1; }
}
@keyframes pulse-ring {
  0% { box-shadow: 0 0 0 0 rgba(143, 160, 142, 0.6); }
  70% { box-shadow: 0 0 0 10px rgba(143, 160, 142, 0); }
  100% { box-shadow: 0 0 0 0 rgba(143, 160, 142, 0); }
}
@keyframes pulse-ring-warm {
  0% { box-shadow: 0 0 0 0 rgba(232, 134, 86, 0.6); }
  70% { box-shadow: 0 0 0 8px rgba(232, 134, 86, 0); }
  100% { box-shadow: 0 0 0 0 rgba(232, 134, 86, 0); }
}

@media (max-width: 768px) {
  .app-layout { padding: 0; }
  .main-card-container { border-radius: 0; box-shadow: none; min-height: 100vh; }
  .dashboard-content { padding: 24px 20px; }
  .glass-nav { padding: 12px 20px; }
  .avatar-name, .chevron { display: none; }
}
</style>
