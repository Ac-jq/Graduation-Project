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
  <main class="counselor-home">
    <section class="counselor-home__hero">
      <div class="hero-copy">
        <p class="eyebrow">Counselor Desk</p>
        <h1>把学生线索、待处理预约与后续跟进收束到一个安静的工作台。</h1>
        <p class="lead">
          这一页直接聚合咨询师已绑定学生、预约池与通知中心的真实数据，用于开始今日的工作梳理。
        </p>
      </div>
      <div class="hero-metrics">
        <div class="metric-card">
          <span>绑定学生</span>
          <strong>{{ students.length }}</strong>
        </div>
        <div class="metric-card">
          <span>待处理预约</span>
          <strong>{{ pendingAppointments.length }}</strong>
        </div>
        <div class="metric-card">
          <span>未读通知</span>
          <strong>{{ unreadNotifications.length }}</strong>
        </div>
      </div>
    </section>

    <section class="counselor-home__grid">
      <article class="launch-panel glass-panel">
        <div class="section-head">
          <p class="section-kicker">Workstreams</p>
          <h2>快速入口</h2>
        </div>
        <div class="action-stack">
          <button class="action-card" type="button" @click="openStudents">
            <span>绑定学生列表</span>
            <small>查看学生报告、AI 会话与绑定信息</small>
          </button>
          <button class="action-card" type="button" @click="openAppointments">
            <span>预约处理台</span>
            <small>接单、拒绝与跟进结果留言</small>
          </button>
          <button class="action-card" type="button" @click="openNotifications">
            <span>Notification Center</span>
            <small>查看系统通知与预约流转提醒</small>
          </button>
        </div>
      </article>

      <article class="insight-panel">
        <div class="section-head section-head-inline">
          <div>
            <p class="section-kicker">最新概况</p>
            <h2>当前线索</h2>
          </div>
          <span class="status-chip">{{ loading ? '同步中' : '已更新' }}</span>
        </div>

        <p v-if="errorMessage" class="error-text">{{ errorMessage }}</p>

        <div class="insight-stack">
          <section class="insight-card">
            <p class="insight-label">最近学生</p>
            <h3>{{ latestStudent?.studentName || '暂无绑定学生' }}</h3>
            <p class="insight-copy">
              {{ latestStudent ? `${latestStudent.college || '学院未填'} · ${latestStudent.grade || '年级未填'} · 学号 ${latestStudent.studentNo || '-'}` : '等待学生绑定后将在此展示。' }}
            </p>
            <button class="ghost-button" type="button" @click="openLatestStudentReports">查看学生报告</button>
          </section>

          <section class="insight-card">
            <p class="insight-label">最近预约</p>
            <h3>{{ latestAppointment ? `预约 #${latestAppointment.appointmentId}` : '暂无预约记录' }}</h3>
            <p class="insight-copy">
              {{ latestAppointment ? `${latestAppointment.anonymousName} · ${latestAppointment.status} · ${new Date(latestAppointment.startTime).toLocaleString('zh-CN')}` : '新的学生预约将显示在这里。' }}
            </p>
            <button class="ghost-button" type="button" @click="openAppointments">进入处理台</button>
          </section>
        </div>
      </article>
    </section>
  </main>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700&family=Noto+Serif+SC:wght@400;500;600;700&display=swap');

.counselor-home {
  min-height: 100vh;
  padding: 44px 28px 72px;
  color: #283128;
  background:
    radial-gradient(circle at 14% 10%, rgba(205, 220, 210, 0.26), transparent 22%),
    radial-gradient(circle at 84% 18%, rgba(228, 212, 198, 0.24), transparent 20%),
    linear-gradient(180deg, #f5f0e5 0%, #f9f5ee 100%);
}

.counselor-home__hero,
.counselor-home__grid {
  max-width: 1320px;
  margin: 0 auto;
}

.counselor-home__hero {
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) minmax(260px, 0.7fr);
  gap: 28px;
  align-items: end;
  margin-bottom: 34px;
}

.hero-copy {
  border-top: 1px solid rgba(59, 69, 59, 0.16);
  padding-top: 18px;
}

.eyebrow,
.section-kicker,
.insight-label {
  margin: 0 0 10px;
  font: 700 0.76rem/1 'Manrope', sans-serif;
  letter-spacing: 0.22em;
  text-transform: uppercase;
  color: #7b6857;
}

.hero-copy h1,
.section-head h2,
.insight-card h3 {
  margin: 0;
  font-family: 'Noto Serif SC', serif;
  font-weight: 600;
}

.hero-copy h1 {
  font-size: clamp(2rem, 3vw, 3.45rem);
  line-height: 1.16;
}

.lead,
.insight-copy,
.error-text {
  font-family: 'Manrope', sans-serif;
}

.lead {
  max-width: 720px;
  margin: 18px 0 0;
  font-size: 1rem;
  line-height: 1.84;
  color: rgba(40, 49, 40, 0.72);
}

.hero-metrics {
  display: grid;
  gap: 14px;
}

.metric-card,
.glass-panel,
.insight-card {
  border: 1px solid rgba(77, 86, 77, 0.14);
  background: rgba(255, 252, 247, 0.74);
  box-shadow: 0 24px 70px rgba(91, 80, 66, 0.08);
  backdrop-filter: blur(16px);
}

.metric-card {
  padding: 18px 20px;
}

.metric-card span,
.status-chip {
  display: block;
  font: 700 0.78rem/1 'Manrope', sans-serif;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: rgba(68, 74, 66, 0.56);
}

.metric-card span {
  margin-bottom: 8px;
}

.metric-card strong {
  font: 600 1.6rem/1 'Noto Serif SC', serif;
}

.counselor-home__grid {
  display: grid;
  grid-template-columns: minmax(320px, 0.82fr) minmax(0, 1.18fr);
  gap: 28px;
}

.launch-panel,
.insight-panel {
  padding: 24px;
}

.section-head {
  margin-bottom: 18px;
}

.section-head-inline {
  display: flex;
  justify-content: space-between;
  align-items: end;
  gap: 16px;
}

.status-chip {
  border: 1px solid rgba(88, 93, 84, 0.14);
  background: rgba(255, 250, 240, 0.82);
  padding: 9px 14px;
  color: #696152;
}

.action-stack,
.insight-stack {
  display: grid;
  gap: 16px;
}

.action-card {
  border: 1px solid rgba(79, 88, 79, 0.12);
  background: rgba(255, 255, 255, 0.58);
  padding: 18px 18px 16px;
  text-align: left;
  cursor: pointer;
  transition: transform 0.28s ease, box-shadow 0.28s ease, border-color 0.28s ease;
}

.action-card:hover,
.ghost-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 18px 32px rgba(61, 73, 63, 0.1);
}

.action-card span,
.ghost-button {
  font: 700 1rem/1.4 'Noto Serif SC', serif;
  color: #283128;
}

.action-card small {
  display: block;
  margin-top: 8px;
  font: 400 0.9rem/1.7 'Manrope', sans-serif;
  color: rgba(40, 49, 40, 0.66);
}

.insight-card {
  padding: 20px;
}

.insight-card h3 {
  font-size: 1.32rem;
  line-height: 1.35;
}

.insight-copy {
  margin: 12px 0 0;
  font-size: 0.96rem;
  line-height: 1.82;
  color: rgba(40, 49, 40, 0.7);
}

.ghost-button {
  margin-top: 16px;
  border: 1px solid rgba(54, 65, 56, 0.2);
  background: rgba(255, 255, 255, 0.6);
  padding: 12px 16px;
  cursor: pointer;
}

.error-text {
  margin: 0 0 16px;
  font-size: 0.95rem;
  line-height: 1.8;
  color: #a44f46;
}

@media (max-width: 980px) {
  .counselor-home {
    padding: 28px 16px 46px;
  }

  .counselor-home__hero,
  .counselor-home__grid {
    grid-template-columns: 1fr;
  }
}
</style>

