<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { fetchStudentAppointmentsApi } from '@/api/appointment'
import type { Appointment } from '@/api/types'
import { toErrorMessage } from '@/views/shared/page-logic'

const router = useRouter()
const loading = ref(false)
const errorMessage = ref('')
const appointments = ref<Appointment[]>([])

async function loadAppointments(): Promise<void> {
  loading.value = true
  errorMessage.value = ''

  try {
    appointments.value = await fetchStudentAppointmentsApi()
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    loading.value = false
  }
}

async function openChat(appointmentId: number): Promise<void> {
  await router.push({ name: 'student-chat', params: { appointmentId } })
}

onMounted(() => {
  void loadAppointments()
})
</script>

<template>
  <section class="appointment-list-page">
    <div class="page-shell">
      <header class="page-hero">
        <div class="hero-copy">
          <p class="eyebrow">预约台账</p>
          <h1>把已发起的预约放在一处，方便你追踪处理进度与后续沟通入口。</h1>
          <p class="lead">
            这里显示你所有真实预约记录。若预约已进入可沟通状态，可直接从这里进入私密聊天室。
          </p>
        </div>
        <div class="hero-aside">
          <div class="metric-card">
            <span>预约总数</span>
            <strong>{{ appointments.length }}</strong>
          </div>
        </div>
      </header>

      <section class="list-panel">
        <div class="section-head section-head-inline">
          <div>
            <p class="section-kicker">预约记录</p>
            <h2>我的预约</h2>
          </div>
          <span class="status-chip">{{ loading ? '加载中' : `${appointments.length} 条记录` }}</span>
        </div>

        <p v-if="loading" class="state-text">正在同步预约记录...</p>
        <p v-else-if="!appointments.length" class="state-text">你还没有预约记录，先前往可预约时段页面发起一条新的预约。</p>

        <div v-else class="appointment-stack">
          <article v-for="appointment in appointments" :key="appointment.appointmentId" class="appointment-card">
            <div class="appointment-topline">
              <div>
                <p class="appointment-title">预约 #{{ appointment.appointmentId }}</p>
                <p class="appointment-time">
                  {{ new Date(appointment.startTime).toLocaleString('zh-CN') }} -
                  {{ new Date(appointment.endTime).toLocaleString('zh-CN') }}
                </p>
              </div>
              <span class="status-pill">{{ appointment.status }}</span>
            </div>
            <p class="issue-summary">{{ appointment.issueSummary }}</p>
            <div class="appointment-meta">
              <span>匿名名称：{{ appointment.anonymousName }}</span>
              <span>咨询师：{{ appointment.counselorName || '待分配' }}</span>
              <span>创建时间：{{ new Date(appointment.createdAt).toLocaleString('zh-CN') }}</span>
            </div>
            <p v-if="appointment.resultMessage" class="result-message">{{ appointment.resultMessage }}</p>
            <button
              v-if="appointment.status === 'ACCEPTED' || appointment.status === 'IN_PROGRESS' || appointment.status === 'COMPLETED'"
              class="ghost-button"
              type="button"
              @click="openChat(appointment.appointmentId)"
            >
              进入私密聊天室
            </button>
          </article>
        </div>

        <p v-if="errorMessage" class="error-text">{{ errorMessage }}</p>
      </section>
    </div>
  </section>
</template>
<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700&family=Noto+Serif+SC:wght@400;500;600;700&display=swap');

:global(body) {
  background:
    radial-gradient(circle at 12% 16%, rgba(205, 221, 208, 0.3), transparent 24%),
    radial-gradient(circle at 86% 18%, rgba(228, 217, 203, 0.3), transparent 24%),
    linear-gradient(180deg, #f4efe5 0%, #f8f4ed 100%);
}

.appointment-list-page {
  min-height: 100vh;
  padding: 44px 28px 72px;
  color: #2c3028;
}

.page-shell {
  max-width: 1280px;
  margin: 0 auto;
}

.page-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(220px, 0.55fr);
  gap: 28px;
  align-items: end;
  margin-bottom: 30px;
}

.hero-copy {
  border-top: 1px solid rgba(63, 71, 63, 0.16);
  padding-top: 18px;
}

.eyebrow,
.section-kicker {
  margin: 0 0 10px;
  font: 700 0.76rem/1 'Manrope', sans-serif;
  letter-spacing: 0.22em;
  text-transform: uppercase;
  color: #7f6a57;
}

.hero-copy h1,
.section-head h2 {
  margin: 0;
  font-family: 'Noto Serif SC', serif;
  font-size: clamp(1.96rem, 3vw, 3.2rem);
  line-height: 1.16;
  font-weight: 600;
}

.lead {
  margin: 18px 0 0;
  max-width: 720px;
  font: 400 1rem/1.84 'Manrope', sans-serif;
  color: rgba(44, 48, 40, 0.74);
}

.metric-card,
.list-panel,
.appointment-card {
  border: 1px solid rgba(78, 86, 77, 0.14);
  background: rgba(255, 252, 247, 0.74);
  box-shadow: 0 24px 70px rgba(91, 80, 66, 0.08);
  backdrop-filter: blur(16px);
}

.metric-card {
  padding: 18px 20px;
}

.metric-card span,
.appointment-time,
.appointment-meta,
.issue-summary,
.result-message,
.error-text,
.state-text {
  font-family: 'Manrope', sans-serif;
}

.metric-card span {
  display: block;
  margin-bottom: 8px;
  font-size: 0.78rem;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: rgba(68, 74, 66, 0.56);
}

.metric-card strong {
  font-family: 'Noto Serif SC', serif;
  font-size: 1.6rem;
  font-weight: 600;
}

.list-panel {
  padding: 24px;
}

.section-head {
  margin-bottom: 18px;
}

.section-head-inline {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: end;
}

.status-chip {
  border: 1px solid rgba(88, 93, 84, 0.14);
  background: rgba(255, 250, 240, 0.82);
  padding: 9px 14px;
  font: 700 0.76rem/1 'Manrope', sans-serif;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #696152;
}

.appointment-stack {
  display: grid;
  gap: 18px;
}

.appointment-card {
  padding: 22px 22px 20px;
}

.appointment-topline {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: start;
  margin-bottom: 12px;
}

.appointment-title {
  margin: 0;
  font-family: 'Noto Serif SC', serif;
  font-size: 1.28rem;
  line-height: 1.3;
}

.appointment-time {
  margin: 8px 0 0;
  font-size: 0.92rem;
  color: rgba(44, 48, 40, 0.62);
}

.status-pill {
  flex-shrink: 0;
  border: 1px solid rgba(98, 112, 99, 0.16);
  background: rgba(242, 244, 237, 0.94);
  padding: 8px 12px;
  font: 700 0.74rem/1 'Manrope', sans-serif;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #66735f;
}

.issue-summary {
  margin: 0 0 14px;
  font-size: 0.98rem;
  line-height: 1.84;
  color: rgba(44, 48, 40, 0.74);
}

.appointment-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 18px;
  font-size: 0.84rem;
  color: rgba(68, 74, 66, 0.58);
}

.result-message {
  margin: 14px 0 0;
  font-size: 0.92rem;
  line-height: 1.8;
  color: #8a533e;
}

.ghost-button {
  margin-top: 16px;
  border: 1px solid rgba(55, 67, 57, 0.22);
  background: rgba(255, 255, 255, 0.56);
  color: #2c3028;
  padding: 12px 16px;
  font: 700 0.84rem/1 'Manrope', sans-serif;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  cursor: pointer;
  transition: transform 0.28s ease, border-color 0.28s ease, box-shadow 0.28s ease;
}

.ghost-button:hover {
  transform: translateY(-2px);
  border-color: rgba(55, 67, 57, 0.34);
  box-shadow: 0 16px 30px rgba(55, 67, 57, 0.1);
}

.state-text,
.error-text {
  margin: 14px 0 0;
  font-size: 0.96rem;
  line-height: 1.8;
}

.error-text {
  font-weight: 600;
  color: #a64939;
}

@media (max-width: 900px) {
  .page-hero {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .appointment-list-page {
    padding: 28px 16px 46px;
  }

  .hero-copy h1,
  .section-head h2 {
    font-size: 1.82rem;
  }

  .appointment-topline,
  .section-head-inline {
    flex-direction: column;
    align-items: start;
  }
}
</style>

