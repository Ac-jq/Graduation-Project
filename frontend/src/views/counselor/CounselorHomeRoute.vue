<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { fetchCounselorAppointmentsApi } from '@/api/appointment'
import { fetchNotificationsApi } from '@/api/notification'
import { fetchCounselorStudentsApi } from '@/api/user'
import type { Appointment, CounselorStudentSummary, NotificationItem } from '@/api/types'
import { toErrorMessage } from '@/views/shared/page-logic'

const router = useRouter()
const loading = ref(false)
const errorMessage = ref('')
const students = ref<CounselorStudentSummary[]>([])
const appointments = ref<Appointment[]>([])
const notifications = ref<NotificationItem[]>([])

const pendingAppointments = computed(() => appointments.value.filter((item) => item.status === 'PENDING'))
const unreadNotifications = computed(() => notifications.value.filter((item) => !item.read))
const latestStudent = computed(() => students.value[0] ?? null)
const latestAppointment = computed(() => appointments.value[0] ?? null)

function formatDateTime(value: string | Date): string {
  const d = new Date(value)
  return `${d.getMonth() + 1}月${d.getDate()}日 ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
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

onMounted(() => {
  void loadDashboard()
})
</script>

<template>
  <main class="editorial-desk-page">
    <div class="desk-container">

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

      <section class="desk-grid">

        <aside class="desk-menu">
          <div class="section-head">
            <h2 class="section-title">工作台入口</h2>
            <span class="section-subtitle">Workstreams</span>
          </div>

          <nav class="menu-list">
            <a class="menu-item" @click="openStudents">
              <div class="item-text">
                <span class="item-title">学生档案库</span>
                <span class="item-desc">查阅报告、AI 会话与绑定信息</span>
              </div>
              <span class="arrow">→</span>
            </a>

            <a class="menu-item" @click="openAppointments">
              <div class="item-text">
                <span class="item-title">预约处理台</span>
                <span class="item-desc">受理请求、拒绝或留言跟进</span>
              </div>
              <span class="arrow">→</span>
            </a>

            <a class="menu-item" @click="openNotifications">
              <div class="item-text">
                <span class="item-title">系统通知中心</span>
                <span class="item-desc">查阅流转提醒与系统简报</span>
              </div>
              <span class="arrow">→</span>
            </a>
          </nav>
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
                {{ latestStudent ? `${latestStudent.college || '学院未知'} · ${latestStudent.grade || '年级未知'} · 学号 ${latestStudent.studentNo || '未记录'}` : '当有学生绑定您为咨询师后，其最近的动态会展示在这里。' }}
              </p>
              <button
                  class="action-btn"
                  type="button"
                  :disabled="!latestStudent"
                  @click="openLatestStudentReports"
              >
                查阅此学生报告 <span class="arrow">→</span>
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
                  当前状态：<strong>{{ latestAppointment.status }}</strong>
                </template>
                <template v-else>
                  目前没有学生向您发起咨询预约，您可以去通知中心查看其他消息。
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

/* 极简纸张底色 */
.editorial-desk-page {
  min-height: 100vh;
  background: #fcfbf9;
  color: #1e2821;
  font-family: 'Manrope', 'Noto Serif SC', sans-serif;
  padding: 4rem 2vw 8rem;
  box-sizing: border-box;
}

.desk-container {
  max-width: 1100px;
  margin: 0 auto;
}

/* 案头导语区 */
.desk-hero {
  margin-bottom: 5rem;
}

.hero-copy {
  margin-bottom: 3.5rem;
  padding-bottom: 2.5rem;
  border-bottom: 1px solid rgba(42, 54, 46, 0.1);
}

.hero-tag {
  display: block;
  font-family: 'Manrope', sans-serif;
  font-size: 0.85rem;
  font-weight: 700;
  letter-spacing: 0.15em;
  color: #8a9c90;
  text-transform: uppercase;
  margin-bottom: 1.2rem;
}

.hero-title {
  font-family: 'Noto Serif SC', serif;
  font-size: clamp(2.8rem, 4vw, 4.2rem);
  font-weight: 600;
  color: #1e2821;
  margin: 0 0 1.2rem 0;
  line-height: 1.1;
  letter-spacing: 0.02em;
}

.hero-lead {
  font-size: 1.1rem;
  color: #5c6b60;
  line-height: 1.8;
  margin: 0;
  max-width: 680px;
}

/* 散文式数据看板（无框） */
.hero-metrics {
  display: flex;
  align-items: center;
  gap: 3.5rem;
  flex-wrap: wrap;
}

.metric-block {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.metric-label {
  font-family: 'Noto Serif SC', serif;
  font-size: 0.85rem;
  color: #8a9c90;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.metric-value {
  font-family: 'Manrope', sans-serif;
  font-size: 3.2rem;
  font-weight: 600;
  color: #2a362e;
  line-height: 1;
}

.metric-value.highlight {
  color: #8c4a4a; /* 如果有待处理预约，用莫兰迪红强调 */
}

.metric-divider {
  width: 1px;
  height: 3rem;
  background: rgba(42, 54, 46, 0.15);
}

/* 核心网格排版 */
.desk-grid {
  display: grid;
  grid-template-columns: 340px minmax(0, 1fr);
  gap: 6rem;
  align-items: start;
}

/* 通用区块头部 */
.section-head {
  margin-bottom: 2rem;
}

.section-title {
  font-family: 'Noto Serif SC', serif;
  font-size: 1.4rem;
  font-weight: 600;
  color: #1e2821;
  margin: 0 0 0.2rem 0;
}

.section-subtitle {
  font-family: 'Manrope', sans-serif;
  font-size: 0.85rem;
  color: #8a9c90;
  text-transform: uppercase;
  letter-spacing: 0.1em;
}

/* ================= 左侧：工作流目录 ================= */
.menu-list {
  display: flex;
  flex-direction: column;
}

.menu-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.5rem 0;
  border-bottom: 1px solid rgba(42, 54, 46, 0.08);
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  text-decoration: none;
}

.menu-item:first-child {
  border-top: 1px solid rgba(42, 54, 46, 0.08);
}

.item-text {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.item-title {
  font-family: 'Noto Serif SC', serif;
  font-size: 1.15rem;
  font-weight: 600;
  color: #2a362e;
  transition: color 0.3s ease;
}

.item-desc {
  font-size: 0.85rem;
  color: #7b8c80;
}

.menu-item .arrow {
  color: #b5c2b9;
  font-size: 1.2rem;
  transition: all 0.3s ease;
}

/* 目录悬停交互：文字变色、整体右移缩进 */
.menu-item:hover {
  transform: translateX(8px);
}

.menu-item:hover .item-title {
  color: #5c6b60;
}

.menu-item:hover .arrow {
  color: #2a362e;
  transform: translateX(4px);
}

/* ================= 右侧：最新线索专栏 ================= */
.insights-columns {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 3rem;
}

.insight-article {
  display: flex;
  flex-direction: column;
}

.thick-accent-line {
  width: 100%;
  height: 4px;
  background: #2a362e;
  margin-bottom: 1.5rem;
}

.article-kicker {
  font-family: 'Manrope', 'Noto Serif SC', sans-serif;
  font-size: 0.85rem;
  font-weight: 700;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: #8a9c90;
  margin: 0 0 1rem 0;
}

.article-headline {
  font-family: 'Noto Serif SC', serif;
  font-size: 1.6rem;
  font-weight: 600;
  color: #1e2821;
  margin: 0 0 1rem 0;
  line-height: 1.3;
}

.article-copy {
  font-size: 1rem;
  line-height: 1.8;
  color: #5c6b60;
  margin: 0 0 2rem 0;
}

.article-copy strong {
  color: #2a362e;
  font-weight: 600;
}

/* 专栏操作按钮 */
.action-btn {
  align-self: flex-start;
  margin-top: auto;
  background: transparent;
  border: 1px solid rgba(130, 150, 138, 0.4);
  color: #2a362e;
  padding: 0.8rem 1.8rem;
  border-radius: 100px;
  font-family: 'Noto Serif SC', serif;
  font-size: 0.95rem;
  font-weight: 600;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  transition: all 0.3s ease;
}

.action-btn:hover:not(:disabled) {
  background: #2a362e;
  border-color: #2a362e;
  color: #ffffff;
  transform: translateY(-2px);
}

.action-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.action-btn .arrow {
  transition: transform 0.3s ease;
}

.action-btn:hover:not(:disabled) .arrow {
  transform: translateX(4px);
}

/* 状态样式 */
.error-banner {
  background: rgba(140, 74, 74, 0.08);
  color: #8c4a4a;
  padding: 1.5rem;
  border-radius: 12px;
  text-align: center;
  font-family: 'Noto Serif SC', serif;
  margin-bottom: 3rem;
}

.loading-state {
  padding: 4rem 0;
  color: #7b8c80;
  font-family: 'Noto Serif SC', serif;
  display: flex;
  align-items: center;
  gap: 1rem;
}

.spinner {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  border: 2px solid rgba(130, 150, 138, 0.2);
  border-top-color: #2a362e;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 响应式 */
@media (max-width: 1024px) {
  .desk-grid {
    grid-template-columns: 1fr;
    gap: 4rem;
  }

  .menu-list {
    margin-bottom: 2rem;
  }
}

@media (max-width: 768px) {
  .hero-metrics {
    gap: 2rem;
    flex-direction: column;
    align-items: flex-start;
  }

  .metric-divider {
    display: none;
  }

  .insights-columns {
    grid-template-columns: 1fr;
    gap: 3rem;
  }
}
</style>