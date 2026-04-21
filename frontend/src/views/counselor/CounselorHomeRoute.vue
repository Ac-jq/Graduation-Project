<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { fetchCounselorAppointmentsApi } from '@/api/appointment'
import { fetchNotificationsApi } from '@/api/notification'
import { fetchCounselorStudentsApi } from '@/api/user'
import { useAuthStore } from '@/stores/auth'
import type { Appointment, CounselorStudentSummary, NotificationItem } from '@/api/types'
import { toErrorMessage } from '@/views/shared/page-logic'
import SaaSBackground from '@/components/SaaSBackground.vue'

const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const errorMessage = ref('')
const students = ref<CounselorStudentSummary[]>([])
const appointments = ref<Appointment[]>([])
const notifications = ref<NotificationItem[]>([])
const showDropdown = ref(false)

const pendingAppointments = computed(() => appointments.value.filter((item) => item.status === 'PENDING'))
const unreadNotifications = computed(() => notifications.value.filter((item) => !item.read))
const latestStudent = computed(() => students.value[0] ?? null)
const latestAppointment = computed(() => appointments.value[0] ?? null)
const todayAppointments = computed(() => {
  const now = new Date()
  return appointments.value
      .filter((item) => isSameDate(item.startTime, now))
      .sort((left, right) => new Date(left.startTime).getTime() - new Date(right.startTime).getTime())
})
const todayPendingAppointments = computed(() => todayAppointments.value.filter((item) => item.status === 'PENDING'))
const focusStudentCount = computed(() => {
  const uniqueStudentIds = new Set(
      pendingAppointments.value
          .map((item) => item.studentUserId)
          .filter((id): id is number => typeof id === 'number')
  )
  return uniqueStudentIds.size
})
const currentUser = computed(() => authStore.currentUser)
const defaultCounselorAvatarUrl = `${window.location.protocol}//${window.location.hostname}:8080/assets/avatars/roles/counselor-default.jpg`
const roleAvatarUrl = computed(() => currentUser.value?.avatarUrl || defaultCounselorAvatarUrl)

function isSameDate(value: string | Date, target: Date): boolean {
  const d = new Date(value)
  return d.getFullYear() === target.getFullYear()
      && d.getMonth() === target.getMonth()
      && d.getDate() === target.getDate()
}

function formatDateTime(value: string | Date): string {
  const d = new Date(value)
  return `${d.getMonth() + 1}月${d.getDate()}日 ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

function formatTimeRange(start: string | Date, end: string | Date): string {
  const begin = new Date(start)
  const finish = new Date(end)
  return `${String(begin.getHours()).padStart(2, '0')}:${String(begin.getMinutes()).padStart(2, '0')} - ${String(finish.getHours()).padStart(2, '0')}:${String(finish.getMinutes()).padStart(2, '0')}`
}

function resolveAppointmentStatus(status: string): string {
  switch (status) {
    case 'PENDING': return '待回应'
    case 'ACCEPTED': return '已接单'
    case 'REJECTED': return '已拒绝'
    case 'COMPLETED': return '已完成'
    case 'CANCELED': return '已取消'
    default: return status
  }
}

async function loadDashboard(): Promise<void> {
  loading.value = true
  errorMessage.value = ''

  try {
    const [studentData, appointmentData, notificationData] = await Promise.all([
      fetchCounselorStudentsApi(),
      fetchCounselorAppointmentsApi(),
      fetchNotificationsApi()
    ])

    students.value = studentData
    appointments.value = appointmentData
    notifications.value = notificationData
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    loading.value = false
  }
}

async function openStudents(): Promise<void> {
  await router.push({ name: 'counselor-students' })
}

async function openAppointments(): Promise<void> {
  await router.push({ name: 'counselor-appointments' })
}

async function openNotifications(): Promise<void> {
  await router.push({ name: 'counselor-notifications' })
}

async function openLatestStudentReports(): Promise<void> {
  if (!latestStudent.value) {
    await openStudents()
    return
  }
  await router.push({ name: 'counselor-student-reports', params: { studentUserId: latestStudent.value.studentUserId } })
}

async function handleDropdownClick(action: 'role' | 'home' | 'security' | 'logout'): Promise<void> {
  showDropdown.value = false

  if (action === 'role') {
    await router.push('/counselor/account')
    return
  }

  if (action === 'home') {
    await router.push('/counselor')
    return
  }

  if (action === 'security') {
    await router.push('/counselor/account')
    return
  }

  await authStore.signOut(true)
  await router.push('/login')
}

onMounted(() => {
  void loadDashboard()
})
</script>

<template>
  <main class="editorial-desk-page">
    <SaaSBackground />

    <div class="desk-container">
      <header class="glass-nav">
        <div class="brand-mark">
          心语<span>空间</span>
        </div>

        <div class="nav-actions">
          <button class="notification-btn" type="button" @click="openNotifications">
            <span v-if="unreadNotifications.length" class="notification-dot" />
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true">
              <path stroke-linecap="round" stroke-linejoin="round" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
            </svg>
          </button>

          <div class="profile-dropdown-wrapper" @mouseleave="showDropdown = false">
            <button class="avatar-btn" type="button" @click="showDropdown = !showDropdown">
              <img :src="roleAvatarUrl" alt="咨询师头像">
              <span class="avatar-name">{{ currentUser?.displayName || currentUser?.realName || '咨询师' }}</span>
              <svg class="chevron" :class="{ 'chevron-up': showDropdown }" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M19 9l-7 7-7-7" />
              </svg>
            </button>

            <transition name="fade-slide">
              <ul v-show="showDropdown" class="dropdown-menu">
                <li @click="handleDropdownClick('home')">首页</li>
                <li @click="handleDropdownClick('role')">角色信息</li>
                <li @click="handleDropdownClick('security')">账户安全</li>
                <li class="logout" @click="handleDropdownClick('logout')">退出登录</li>
              </ul>
            </transition>
          </div>
        </div>
      </header>

      <header class="desk-hero">
        <div class="hero-copy">
          <span class="hero-tag">Counselor Desk</span>
          <h1 class="hero-title">平静地开启今日的工作。</h1>
          <p class="hero-lead">
            这里聚合了您负责的学生档案、需要回应的预约请求以及系统的流转通知。<br>
            不需要急于处理所有事情，先从掌握全局开始。
          </p>
        </div>

        <div class="hero-metrics">
          <div class="metric-block">
            <span class="metric-label">绑定学生档案</span>
            <strong class="metric-value">{{ loading ? '-' : students.length }}</strong>
          </div>
          <div class="metric-divider"></div>
          <div class="metric-block">
            <span class="metric-label">待回应预约</span>
            <strong class="metric-value highlight">{{ loading ? '-' : pendingAppointments.length }}</strong>
          </div>
          <div class="metric-divider"></div>
          <div class="metric-block">
            <span class="metric-label">未读通知</span>
            <strong class="metric-value">{{ loading ? '-' : unreadNotifications.length }}</strong>
          </div>
        </div>
      </header>

      <div v-if="errorMessage" class="error-banner">{{ errorMessage }}</div>

      <section class="todo-card-grid">
        <article class="todo-card todo-card--warm">
          <span class="todo-label">今日待处理预约</span>
          <strong>{{ loading ? '-' : todayPendingAppointments.length }}</strong>
          <p>优先处理学生新提交的预约请求。</p>
        </article>

        <article class="todo-card">
          <span class="todo-label">未读消息</span>
          <strong>{{ loading ? '-' : unreadNotifications.length }}</strong>
          <p>来自预约流转、公告与平台提醒。</p>
        </article>

        <article class="todo-card">
          <span class="todo-label">重点关注学生数</span>
          <strong>{{ loading ? '-' : focusStudentCount }}</strong>
          <p>依据待处理预约聚合，方便先做响应。</p>
        </article>
      </section>

      <section class="desk-grid">

        <aside class="desk-sidebar">
          <div class="schedule-board">
            <div class="schedule-head">
              <h3>今日日程</h3>
              <span>{{ todayAppointments.length }} 项</span>
            </div>

            <div v-if="loading" class="schedule-empty">正在整理今日安排...</div>
            <div v-else-if="!todayAppointments.length" class="schedule-empty">今天暂时没有安排，可以处理归档。</div>

            <div v-else class="schedule-list">
              <article
                  v-for="appointment in todayAppointments"
                  :key="appointment.appointmentId"
                  class="schedule-item"
              >
                <time>{{ formatTimeRange(appointment.startTime, appointment.endTime) }}</time>
                <div>
                  <strong>{{ appointment.anonymousName || '匿名来访者' }}</strong>
                  <span>{{ resolveAppointmentStatus(appointment.status) }}</span>
                </div>
              </article>
            </div>
          </div>
        </aside>

        <section class="desk-insights">
          <div class="section-head">
            <h2 class="section-title">最新线索</h2>
            <span class="section-subtitle">Current Clues</span>
          </div>

          <div v-if="loading" class="loading-state">
            <div class="spinner"></div>
            <p>正在整理案头资料...</p>
          </div>

          <div v-else class="insights-columns">
            <article class="insight-article">
              <div class="thick-accent-line"></div>
              <h3 class="article-kicker">重点关注对象</h3>
              <h4 class="article-headline">{{ latestStudent?.studentName || '暂无绑定学生' }}</h4>
              <p class="article-copy">
                {{ latestStudent ? `${latestStudent.college || '学院未知'} · ${latestStudent.grade || '年级未知'} · 学号 ${latestStudent.studentNo || '未记录'}` : '绑定学生后，其动态会展示在这里。' }}
              </p>
              <button
                  class="action-btn"
                  type="button"
                  :disabled="!latestStudent"
                  @click="openLatestStudentReports"
              >
                查阅学生报告 <span class="arrow">→</span>
              </button>
            </article>

            <article class="insight-article">
              <div class="thick-accent-line"></div>
              <h3 class="article-kicker">最新预约请求</h3>
              <h4 class="article-headline">
                {{ latestAppointment ? `预约编号 #${latestAppointment.appointmentId}` : '暂无预约记录' }}
              </h4>
              <p class="article-copy">
                <template v-if="latestAppointment">
                  <strong>{{ latestAppointment.anonymousName }}</strong> 提交于 {{ formatDateTime(latestAppointment.createdAt) }}。<br>
                  当前状态：<strong>{{ resolveAppointmentStatus(latestAppointment.status) }}</strong>
                </template>
                <template v-else>
                  目前没有学生向您发起咨询预约。
                </template>
              </p>
              <button
                  class="action-btn"
                  type="button"
                  :disabled="!latestAppointment"
                  @click="openAppointments"
              >
                前往处理 <span class="arrow">→</span>
              </button>
            </article>
          </div>
        </section>

      </section>
    </div>
  </main>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700;800&family=Noto+Serif+SC:wght@500;600;700&display=swap');

.editorial-desk-page {
  min-height: 100vh;
  position: relative;
  isolation: isolate;
  background-color: #faf9f6;
  color: #1e2821;
  font-family: 'Manrope', 'Noto Serif SC', sans-serif;
  padding: 1.5rem clamp(1rem, 2vw, 2rem) 3rem;
  box-sizing: border-box;
  overflow-x: hidden;
}

.desk-container {
  max-width: 1100px;
  margin: 0 auto;
  position: relative;
  z-index: 1;
}

.glass-nav {
  position: sticky;
  top: 0;
  z-index: 100;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 24px;
  margin-bottom: 1.5rem;
  background: rgba(250, 249, 246, 0.75);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-bottom: 1px solid rgba(44, 53, 45, 0.06);
  border-radius: 16px;
}

.brand-mark {
  font-family: 'Noto Serif SC', serif;
  font-weight: 900;
  font-size: 1.3rem;
  letter-spacing: 0.1em;
}

.brand-mark span {
  color: #8fa08e;
  font-style: italic;
}

.nav-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.notification-btn {
  position: relative;
  background: rgba(44, 53, 45, 0.04);
  border: none;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #2c352d;
  transition: all 0.3s ease;
}

.notification-btn:hover {
  background: #fff;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(44, 53, 45, 0.08);
}

.notification-btn svg { width: 18px; height: 18px; }

.notification-dot {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #e88656;
  box-shadow: 0 0 0 2px #fdfbf7;
  animation: breathe 2s ease-in-out infinite;
}

@keyframes breathe {
  0%, 100% {
    opacity: 0.6;
    transform: scale(0.95);
    box-shadow: 0 0 0 2px #fdfbf7, 0 0 0 0 rgba(232, 134, 86, 0);
  }

  50% {
    opacity: 1;
    transform: scale(1.1);
    box-shadow: 0 0 0 2px #fdfbf7, 0 0 12px rgba(232, 134, 86, 0.42);
  }
}

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
  transition: all 0.3s ease;
}

.avatar-btn:hover {
  background: #fff;
  box-shadow: 0 4px 12px rgba(44, 53, 45, 0.08);
}

.avatar-btn img {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  object-fit: cover;
}

.avatar-name {
  font-size: 0.9rem;
  font-weight: 600;
}

.chevron {
  width: 14px;
  height: 14px;
  opacity: 0.6;
  transition: transform 0.3s ease;
}

.chevron-up { transform: rotate(180deg); }

.dropdown-menu {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  width: 130px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(44, 53, 45, 0.08);
  border-radius: 12px;
  box-shadow: 0 12px 32px rgba(135, 126, 115, 0.12);
  list-style: none;
  padding: 6px;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
  z-index: 1000;
}

.dropdown-menu::before {
  content: '';
  position: absolute;
  top: -15px;
  left: 0;
  width: 100%;
  height: 15px;
  background: transparent;
}

.dropdown-menu li {
  padding: 8px 12px;
  border-radius: 8px;
  font-size: 0.85rem;
  cursor: pointer;
  transition: all 0.2s ease;
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

.fade-slide-enter-active, .fade-slide-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.fade-slide-enter-from, .fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-8px) scale(0.95);
}

/* 案头导语区 */
.desk-hero {
  margin-bottom: 1.5rem;
}

.hero-copy {
  margin-bottom: 1rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid rgba(42, 54, 46, 0.08);
}

.hero-tag {
  display: block;
  font-family: 'Manrope', sans-serif;
  font-size: 0.8rem;
  font-weight: 700;
  letter-spacing: 0.15em;
  color: #8a9c90;
  text-transform: uppercase;
  margin-bottom: 0.5rem;
}

.hero-title {
  font-family: 'Noto Serif SC', serif;
  font-size: clamp(2.2rem, 3.5vw, 3.2rem);
  font-weight: 600;
  color: #1e2821;
  margin: 0 0 0.5rem 0;
  line-height: 1.1;
}

.hero-lead {
  font-size: 1rem;
  color: #5c6b60;
  line-height: 1.6;
  margin: 0;
  max-width: 680px;
}

/* 数据核心指标 */
.hero-metrics {
  display: flex;
  align-items: center;
  gap: 2rem;
  flex-wrap: wrap;
}

.metric-block {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.metric-label {
  font-family: 'Noto Serif SC', serif;
  font-size: 0.8rem;
  color: #8a9c90;
  text-transform: uppercase;
}

.metric-value {
  font-family: 'Manrope', sans-serif;
  font-size: 2.2rem;
  font-weight: 600;
  color: #2a362e;
  line-height: 1;
}

.metric-value.highlight { color: #8c4a4a; }

.metric-divider {
  width: 1px;
  height: 2.5rem;
  background: rgba(42, 54, 46, 0.1);
}

/* ----------------------------------------------------
   重构区：横向 3 列的待办卡片网格
------------------------------------------------------- */
.todo-card-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1.5rem;
  margin-bottom: 1.5rem; /* 与下方模块的间距 */
}

.todo-card {
  padding: 1.25rem 1.5rem;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.85);
  box-shadow: 0 8px 24px rgba(54, 66, 58, 0.04), inset 0 0 0 1px rgba(255,255,255,1);
  backdrop-filter: blur(10px);
}

.todo-card--warm {
  background: linear-gradient(135deg, rgba(255, 253, 250, 0.9), rgba(246, 237, 230, 0.6));
}

.todo-label {
  display: block;
  margin-bottom: 0.4rem;
  color: #7b8c80;
  font-family: 'Noto Serif SC', serif;
  font-size: 0.85rem;
  font-weight: 600;
}

.todo-card strong {
  display: block;
  color: #2a362e;
  font-family: 'Manrope', sans-serif;
  font-size: 2.2rem;
  font-weight: 700;
  line-height: 1;
}

.todo-card p {
  margin: 0.6rem 0 0;
  color: #5c6b60;
  font-size: 0.85rem;
  line-height: 1.5;
}

/* ----------------------------------------------------
   重构区：下半部分左右分栏布局
------------------------------------------------------- */
.desk-grid {
  display: grid;
  grid-template-columns: 340px minmax(0, 1fr); /* 严格限制左侧宽度，右侧自适应 */
  gap: 2rem;
  align-items: start;
}

/* 左侧：日程表容器 */
.schedule-board {
  padding: 1.25rem;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.75);
  box-shadow: 0 12px 32px rgba(54, 66, 58, 0.03), inset 0 0 0 1px rgba(255,255,255,1);
  backdrop-filter: blur(10px);
}

.schedule-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1rem;
}

.schedule-head h3 {
  margin: 0;
  color: #1e2821;
  font-family: 'Noto Serif SC', serif;
  font-size: 1rem;
  font-weight: 600;
}

.schedule-head span, .schedule-empty {
  color: #8a9c90;
  font-size: 0.8rem;
}

/* 策略 2：日程列表局部滚动，彻底防止撑破页面 */
.schedule-list {
  display: grid;
  gap: 0.5rem;
  max-height: 260px; /* 控制最大高度，超出会出现滚动条 */
  overflow-y: auto;
  padding-right: 8px; /* 为滚动条留出间距 */
}

/* 美化内部滚动条 */
.schedule-list::-webkit-scrollbar {
  width: 4px;
}
.schedule-list::-webkit-scrollbar-track {
  background: transparent;
}
.schedule-list::-webkit-scrollbar-thumb {
  background: rgba(130, 150, 138, 0.25);
  border-radius: 4px;
}
.schedule-list::-webkit-scrollbar-thumb:hover {
  background: rgba(130, 150, 138, 0.5);
}

.schedule-item {
  display: grid;
  grid-template-columns: 5.5rem minmax(0, 1fr);
  gap: 0.8rem;
  align-items: center;
  padding: 0.7rem 0;
  border-top: 1px solid rgba(42, 54, 46, 0.05);
}

.schedule-item time {
  color: #2a362e;
  font-family: 'Manrope', sans-serif;
  font-size: 0.8rem;
  font-weight: 700;
}

.schedule-item div {
  display: grid;
  gap: 0.2rem;
}

.schedule-item strong {
  color: #1e2821;
  font-family: 'Noto Serif SC', serif;
  font-size: 0.9rem;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.schedule-item span {
  color: #7b8c80;
  font-size: 0.75rem;
}

/* 右侧：线索专栏 */
.section-head { margin-bottom: 1rem; }

.section-title {
  font-family: 'Noto Serif SC', serif;
  font-size: 1.2rem;
  font-weight: 600;
  color: #1e2821;
  margin: 0 0 0.2rem 0;
}

.section-subtitle {
  font-family: 'Manrope', sans-serif;
  font-size: 0.75rem;
  color: #8a9c90;
  text-transform: uppercase;
  letter-spacing: 0.1em;
}

.insights-columns {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 1.5rem;
}

.insight-article {
  display: flex;
  flex-direction: column;
  background: rgba(255, 255, 255, 0.5);
  padding: 1.5rem;
  border-radius: 16px;
  box-shadow: 0 4px 16px rgba(0,0,0,0.02);
}

.thick-accent-line {
  width: 32px;
  height: 3px;
  background: #2a362e;
  margin-bottom: 1rem;
  border-radius: 2px;
}

.article-kicker {
  font-family: 'Manrope', 'Noto Serif SC', sans-serif;
  font-size: 0.8rem;
  font-weight: 700;
  letter-spacing: 0.1em;
  color: #8a9c90;
  margin: 0 0 0.6rem 0;
}

.article-headline {
  font-family: 'Noto Serif SC', serif;
  font-size: 1.3rem;
  font-weight: 600;
  color: #1e2821;
  margin: 0 0 0.6rem 0;
  line-height: 1.3;
}

.article-copy {
  font-size: 0.95rem;
  line-height: 1.6;
  color: #5c6b60;
  margin: 0 0 1.5rem 0;
}

.action-btn {
  align-self: flex-start;
  margin-top: auto;
  background: rgba(255,255,255,0.8);
  border: 1px solid rgba(130, 150, 138, 0.4);
  color: #2a362e;
  padding: 0.7rem 1.4rem;
  border-radius: 100px;
  font-family: 'Noto Serif SC', serif;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  transition: all 0.3s ease;
}

.action-btn:hover:not(:disabled) {
  background: #2a362e;
  border-color: #2a362e;
  color: #ffffff;
}

.error-banner {
  background: rgba(140, 74, 74, 0.08);
  color: #8c4a4a;
  padding: 1rem;
  border-radius: 12px;
  text-align: center;
  font-size: 0.9rem;
  margin-bottom: 1.5rem;
}

.loading-state {
  padding: 2rem 0;
  color: #7b8c80;
  display: flex;
  align-items: center;
  gap: 1rem;
  font-size: 0.9rem;
}

.spinner {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  border: 2px solid rgba(130, 150, 138, 0.2);
  border-top-color: #2a362e;
  animation: spin 0.8s linear infinite;
}

@keyframes spin { to { transform: rotate(360deg); } }

/* 响应式调整 */
@media (max-width: 1024px) {
  .todo-card-grid {
    grid-template-columns: 1fr; /* 窄屏幕下待办卡片变回单列堆叠 */
  }
  .desk-grid {
    grid-template-columns: 1fr; /* 左右分栏变为上下堆叠 */
    gap: 1.5rem;
  }
}
</style>
