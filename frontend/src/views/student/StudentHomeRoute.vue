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

const unreadCount = computed(() => notifications.value.filter((item) => !item.read).length)

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

onMounted(() => {
  void loadDashboard()
})
</script>

<template>
  <div class="student-dashboard">
    <div class="noise-overlay" />

    <header class="dashboard-header">
      <div class="brand-mark">
        心语<span>空间</span>
      </div>

      <div class="header-actions">
        <button class="notification-button" type="button" @click="openNotifications">
          <span v-if="unreadCount" class="notification-dot" />
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.2" aria-hidden="true">
            <path stroke-linecap="round" stroke-linejoin="round" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
          </svg>
        </button>

        <button class="profile-chip" type="button" @click="router.push({ name: 'student-profile' })">
          <img v-if="profile?.avatarUrl" :src="profile.avatarUrl" alt="avatar">
          <div v-else class="profile-fallback">{{ (profile?.displayName || '学').slice(0, 1) }}</div>
          <span>{{ profile?.displayName || '学生' }}</span>
        </button>
      </div>
    </header>

    <main class="dashboard-main">
      <section class="hero-grid">
        <div class="hero-copy">
          <div>
            <h1>
              探索<br>内在的平静。
            </h1>
            <div class="identity-row">
              <span>{{ profile?.displayName || '未命名学生' }}</span>
              <span class="dot" />
              <span>{{ profile?.college || '学院未填写' }}</span>
              <span class="dot" />
              <span>{{ profile?.grade || '年级未填写' }}</span>
            </div>
          </div>

          <div class="hero-links">
            <button class="hero-link" type="button" @click="openScaleArchive">
              <span>心理测评档案</span>
              <span>&rarr;</span>
            </button>
            <button class="hero-link" type="button" @click="openAppointmentCenter">
              <span>人工咨询预约</span>
              <span>&rarr;</span>
            </button>
            <button class="hero-link hero-link--last" type="button" @click="open收藏数">
              <span>我的资源收藏</span>
              <span>&rarr;</span>
            </button>
          </div>
        </div>

        <div class="aura-stage">
          <div class="aura-backdrop">
            <div class="aura-shape aura-shape--sage" />
            <div class="aura-shape aura-shape--clay" />
            <div class="aura-blur" />
          </div>

          <div class="aura-content">
            <div class="aura-copy">
              <div class="aura-status">
                <span class="pulse" />
                <span>{{ activeSession ? 'AI Session Active' : 'AI Session Ready' }}</span>
              </div>
              <h2>
                无论昼夜，<br>随时倾听你的心声。
              </h2>
            </div>

            <button class="hero-cta" type="button" @click="openAiSession">
              {{ activeSession ? '继续倾诉' : '发起倾诉' }}
            </button>
          </div>
        </div>
      </section>

      <section class="dashboard-content">
        <div class="status-column">
          <div class="section-label">近期状态</div>

          <article class="status-block">
            <div class="status-meta">
              <span>{{ latestReport ? new Date(latestReport.createdAt).toLocaleDateString('zh-CN') : '暂无记录' }} / 报告</span>
              <button class="inline-link" type="button" @click="openScaleArchive">历史记录</button>
            </div>
            <h3>{{ latestReport?.scaleName || '尚无测评报告' }}</h3>
            <div class="status-info">
              <span class="chip chip--clay">{{ latestReport?.levelCode || '待生成' }}</span>
              <span>{{ latestReport ? `总分 ${latestReport.totalScore}` : '完成测评后将在此展示' }}</span>
            </div>
          </article>

          <div class="mini-divider" />

          <article class="status-block">
            <div class="status-meta">
              <span>{{ latestAppointment ? new Date(latestAppointment.createdAt).toLocaleDateString('zh-CN') : '暂无记录' }} / 预约</span>
              <button class="inline-link" type="button" @click="router.push({ name: 'student-appointments' })">查看全部</button>
            </div>
            <h3>{{ latestAppointment ? '一对一心理辅导（匿名）' : '尚无预约记录' }}</h3>
            <div class="status-info">
              <span class="chip chip--sage">{{ latestAppointment?.status || '待预约' }}</span>
              <span>
                {{ latestAppointment
                  ? `${new Date(latestAppointment.startTime).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })} - ${new Date(latestAppointment.endTime).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })}`
                  : '预约后将在此展示时间段' }}
              </span>
            </div>
          </article>
        </div>

        <div class="resource-column">
          <div class="resource-head">
            <div class="section-label">精选资源</div>
            <button class="inline-link" type="button" @click="router.push({ name: 'student-resources' })">浏览全部</button>
          </div>

          <div v-if="loading" class="state-panel">正在同步首页数据...</div>
          <div v-else-if="errorMessage" class="state-panel state-panel--error">{{ errorMessage }}</div>
          <div v-else class="resource-grid">
            <article v-for="resource in featuredResources" :key="resource.resourceId" class="resource-card" @click="openResource(resource.resourceId)">
              <div class="resource-visual">
                <img v-if="resource.coverUrl" :src="resource.coverUrl" :alt="resource.title">
                <div v-else class="resource-fallback">{{ resource.title.slice(0, 2) }}</div>
              </div>
              <div class="resource-body">
                <div class="resource-topline">
                  <h4>{{ resource.title }}</h4>
                  <span>{{ resource.categoryName }}</span>
                </div>
              </div>
            </article>
          </div>
        </div>
      </section>
    </main>
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Noto+Sans+SC:wght@300;400;500&family=Noto+Serif+SC:wght@400;600;900&display=swap');

.student-dashboard {
  min-height: 100vh;
  position: relative;
  background: #fdfbf7;
  color: #2c352d;
  padding-bottom: 80px;
  overflow: hidden;
  font-family: 'Noto Sans SC', sans-serif;
}

.noise-overlay {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 0;
  opacity: 0.035;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noiseFilter'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.85' numOctaves='3' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noiseFilter)'/%3E%3C/svg%3E");
}

.dashboard-header,
.dashboard-main {
  position: relative;
  z-index: 1;
}

.dashboard-header {
  width: 100%;
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px 32px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.brand-mark {
  font-family: 'Noto Serif SC', serif;
  font-weight: 900;
  font-size: 1.7rem;
  letter-spacing: 0.18em;
}

.brand-mark span {
  color: #8fa08e;
  font-style: italic;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 18px;
}

.notification-button {
  position: relative;
  width: 44px;
  height: 44px;
  border: none;
  background: transparent;
  color: rgba(44, 53, 45, 0.72);
  cursor: pointer;
}

.notification-button svg {
  width: 24px;
  height: 24px;
}

.notification-dot {
  position: absolute;
  top: 8px;
  right: 7px;
  width: 10px;
  height: 10px;
  border-radius: 999px;
  background: #e2b49a;
  border: 2px solid #fdfbf7;
}

.profile-chip {
  display: flex;
  align-items: center;
  gap: 12px;
  border: 1px solid transparent;
  background: transparent;
  padding: 6px 12px;
  cursor: pointer;
  transition: border-color 220ms ease;
}

.profile-chip:hover {
  border-color: #eae8e3;
}

.profile-chip img,
.profile-fallback {
  width: 34px;
  height: 34px;
  border-radius: 999px;
  object-fit: cover;
}

.profile-fallback {
  display: grid;
  place-items: center;
  background: rgba(143, 160, 142, 0.18);
  color: #2c352d;
  font-family: 'Noto Serif SC', serif;
}

.dashboard-main {
  max-width: 1200px;
  margin: 0 auto;
  padding: 12px 24px 0;
}

.hero-grid {
  display: flex;
  flex-direction: column;
  gap: 32px;
}

@media (min-width: 1024px) {
  .hero-grid {
    display: grid;
    grid-template-columns: minmax(0, 5fr) minmax(0, 7fr);
    gap: 64px;
    align-items: stretch;
  }
}

.hero-copy {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 64px;
}

.hero-copy h1 {
  margin: 0;
  font-family: 'Noto Serif SC', serif;
  font-size: clamp(3rem, 7vw, 4.9rem);
  line-height: 1.04;
}

.identity-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 14px;
  margin-top: 24px;
  color: rgba(44, 53, 45, 0.62);
  font-size: 0.8rem;
  font-weight: 300;
  text-transform: uppercase;
  letter-spacing: 0.18em;
}

.dot {
  width: 4px;
  height: 4px;
  border-radius: 999px;
  background: #e2b49a;
}

.hero-links {
  display: grid;
}

.hero-link {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 16px 20px 0;
  border: none;
  border-top: 1px solid #eae8e3;
  background: transparent;
  font-family: 'Noto Serif SC', serif;
  font-size: 1.1rem;
  color: #2c352d;
  cursor: pointer;
  transition: transform 260ms ease;
}

.hero-link--last {
  border-bottom: 1px solid #eae8e3;
}

.hero-link:hover {
  transform: translateX(8px);
}

.aura-stage {
  position: relative;
  min-height: 400px;
  display: flex;
  align-items: flex-end;
  padding: 32px;
}

@media (min-width: 1024px) {
  .aura-stage {
    min-height: 460px;
    padding: 48px;
  }
}

.aura-backdrop {
  position: absolute;
  inset: 0;
  border-radius: 28px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.55);
  background: rgba(211, 221, 209, 0.3);
}

.aura-shape {
  position: absolute;
  filter: blur(64px);
  animation: aura 12s ease-in-out infinite alternate;
}

.aura-shape--sage {
  top: -10%;
  left: -10%;
  width: 80%;
  height: 80%;
  background: linear-gradient(135deg, rgba(143, 160, 142, 0.42), transparent 70%);
}

.aura-shape--clay {
  right: -10%;
  bottom: -20%;
  width: 90%;
  height: 90%;
  background: linear-gradient(135deg, rgba(226, 180, 154, 0.3), transparent 72%);
  animation-direction: reverse;
  animation-duration: 15s;
}

.aura-blur {
  position: absolute;
  inset: 0;
  backdrop-filter: blur(60px);
  mix-blend-mode: overlay;
}

.aura-content {
  position: relative;
  z-index: 1;
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

@media (min-width: 768px) {
  .aura-content {
    flex-direction: row;
    align-items: end;
    justify-content: space-between;
    gap: 24px;
  }
}

.aura-status,
.section-label,
.status-meta,
.inline-link,
.resource-topline span {
  font-size: 0.72rem;
  text-transform: uppercase;
  letter-spacing: 0.18em;
  color: rgba(44, 53, 45, 0.62);
}

.aura-status {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.pulse {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: #8fa08e;
  animation: pulse 1.8s infinite;
}

.aura-content h2,
.status-block h3,
.resource-topline h4 {
  margin: 0;
  font-family: 'Noto Serif SC', serif;
}

.aura-content h2 {
  font-size: clamp(2rem, 4vw, 3rem);
  line-height: 1.2;
}

.hero-cta {
  border: none;
  border-radius: 999px;
  padding: 16px 28px;
  background: #2c352d;
  color: #fdfbf7;
  font-size: 0.92rem;
  letter-spacing: 0.08em;
  cursor: pointer;
  transition: background 260ms ease;
}

.hero-cta:hover {
  background: #8fa08e;
}

.dashboard-content {
  margin-top: 64px;
  padding-top: 56px;
  border-top: 1px solid #eae8e3;
  display: grid;
  gap: 48px;
}

@media (min-width: 1024px) {
  .dashboard-content {
    grid-template-columns: minmax(0, 4fr) minmax(0, 8fr);
    gap: 56px;
  }
}

.status-column {
  display: grid;
  gap: 28px;
}

.status-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 10px;
}

.inline-link {
  border: none;
  background: transparent;
  cursor: pointer;
  text-decoration: underline;
  text-underline-offset: 4px;
}

.status-block h3 {
  font-size: 1.14rem;
  margin-bottom: 10px;
}

.status-info {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  align-items: center;
  font-size: 0.92rem;
  color: rgba(44, 53, 45, 0.68);
}

.chip {
  padding: 4px 8px;
  border-radius: 4px;
  border: 1px solid rgba(226, 180, 154, 0.3);
  color: #c28467;
}

.chip--sage {
  border-color: rgba(143, 160, 142, 0.3);
  color: #718570;
}

.mini-divider {
  width: 48px;
  height: 1px;
  background: #eae8e3;
}

.resource-head {
  display: flex;
  justify-content: space-between;
  align-items: end;
  gap: 16px;
  margin-bottom: 24px;
}

.resource-grid {
  display: grid;
  gap: 28px;
}

@media (min-width: 768px) {
  .resource-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

.resource-card {
  cursor: pointer;
}

.resource-visual {
  width: 100%;
  aspect-ratio: 4 / 3;
  overflow: hidden;
  margin-bottom: 16px;
  border-radius: 3px;
  background: rgba(234, 232, 227, 0.32);
}

.resource-visual img,
.resource-fallback {
  width: 100%;
  height: 100%;
}

.resource-visual img {
  object-fit: cover;
  filter: brightness(0.95);
  transition: transform 700ms ease, filter 700ms ease;
}

.resource-card:hover .resource-visual img {
  transform: scale(1.05);
  filter: brightness(1);
}

.resource-fallback {
  display: grid;
  place-items: center;
  font-family: 'Noto Serif SC', serif;
  font-size: 2rem;
  color: rgba(44, 53, 45, 0.38);
}

.resource-topline {
  display: flex;
  justify-content: space-between;
  align-items: start;
  gap: 16px;
}

.resource-topline h4 {
  font-size: 1.12rem;
}

.state-panel {
  padding: 18px 20px;
  border: 1px solid rgba(44, 53, 45, 0.08);
  background: rgba(255, 255, 255, 0.55);
  font-size: 0.95rem;
  color: rgba(44, 53, 45, 0.72);
}

.state-panel--error {
  color: #a44f46;
}

@keyframes aura {
  0% {
    transform: rotate(0deg) scale(1);
    border-radius: 40% 60% 70% 30% / 40% 50% 60% 50%;
  }
  100% {
    transform: rotate(360deg) scale(1.1);
    border-radius: 60% 40% 30% 70% / 60% 30% 70% 40%;
  }
}

@keyframes pulse {
  0% { box-shadow: 0 0 0 0 rgba(143, 160, 142, 0.45); }
  70% { box-shadow: 0 0 0 10px rgba(143, 160, 142, 0); }
  100% { box-shadow: 0 0 0 0 rgba(143, 160, 142, 0); }
}

@media (max-width: 768px) {
  .dashboard-header {
    padding: 20px 16px;
  }

  .dashboard-main {
    padding: 8px 16px 0;
  }

  .profile-chip span {
    display: none;
  }
}
</style>

