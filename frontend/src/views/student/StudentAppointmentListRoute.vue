<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { fetchStudentAppointmentsApi } from '@/api/appointment'
import type { Appointment } from '@/api/types'
import { toErrorMessage } from '@/views/shared/page-logic'

type AppointmentTone = 'pending' | 'accepted' | 'active' | 'done' | 'muted'

const router = useRouter()
const loading = ref(false)
const errorMessage = ref('')
const appointments = ref<Appointment[]>([])

const activeCount = computed(() =>
  appointments.value.filter((item) => item.status === 'ACCEPTED' || item.status === 'IN_PROGRESS').length
)

const completedCount = computed(() =>
  appointments.value.filter((item) => item.status === 'COMPLETED').length
)

function formatDateTime(value: string): string {
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value))
}

function resolveStatusLabel(status: string): string {
  switch (status) {
    case 'PENDING':
      return '待处理'
    case 'ACCEPTED':
      return '已接受'
    case 'IN_PROGRESS':
      return '沟通中'
    case 'COMPLETED':
      return '已完成'
    case 'REJECTED':
      return '未通过'
    case 'CANCELLED':
      return '已取消'
    default:
      return status
  }
}

function resolveStatusTone(status: string): AppointmentTone {
  switch (status) {
    case 'ACCEPTED':
      return 'accepted'
    case 'IN_PROGRESS':
      return 'active'
    case 'COMPLETED':
      return 'done'
    case 'PENDING':
      return 'pending'
    default:
      return 'muted'
  }
}

function resolveInitials(value: string | null | undefined, fallback = '咨询'): string {
  const source = (value || fallback).replace(/\s+/g, '')
  return source.slice(0, 2).toUpperCase()
}

function canOpenChat(status: string): boolean {
  return status === 'ACCEPTED' || status === 'IN_PROGRESS' || status === 'COMPLETED'
}

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
          <div class="hero-eyebrow-row">
            <p class="eyebrow">预约台账</p>
            <span class="hero-badge">
              <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
                <path d="M7.25 3.5V6" />
                <path d="M16.75 3.5V6" />
                <path d="M3.5 9.25h17" />
                <path d="M5.25 5.25h13.5A1.75 1.75 0 0 1 20.5 7v11.25A1.75 1.75 0 0 1 18.75 20H5.25A1.75 1.75 0 0 1 3.5 18.25V7A1.75 1.75 0 0 1 5.25 5.25Z" />
              </svg>
              安排与跟进一处查看
            </span>
          </div>
          <h1>把已发起的预约集中放在一处，追踪进度，也保留后续沟通入口。</h1>
          <p class="lead">
            这里会呈现你所有真实预约记录。预约进入可沟通状态后，可以直接从当前卡片进入私密聊天室，
            不需要再重新查找入口。
          </p>

          <div class="hero-metrics">
            <article class="metric-card">
              <span>预约总数</span>
              <strong>{{ appointments.length }}</strong>
              <p>按时间完整留档，方便回看</p>
            </article>
            <article class="metric-card">
              <span>处理中</span>
              <strong>{{ activeCount }}</strong>
              <p>已接受或正在沟通的预约</p>
            </article>
            <article class="metric-card metric-card--warm">
              <span>已完成</span>
              <strong>{{ completedCount }}</strong>
              <p>可继续回看结论与沟通结果</p>
            </article>
          </div>
        </div>

        <aside class="hero-aside">
          <div class="hero-aside-card">
            <div class="hero-aside-card__top">
              <span>贴心提示</span>
              <div class="hero-aside-card__icon">
                <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
                  <path d="M12 20.25s-6.75-4.02-6.75-9.28A3.97 3.97 0 0 1 12 7.89a3.97 3.97 0 0 1 6.75 3.08c0 5.26-6.75 9.28-6.75 9.28Z" />
                </svg>
              </div>
            </div>
            <strong>允许慢一点</strong>
            <p>如果暂时不想立刻进入对话，也可以先回看预约状态，等准备好再开始沟通。</p>
          </div>
        </aside>
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
        <p v-else-if="!appointments.length" class="state-text">
          你还没有预约记录，先前往可预约时段页面发起一条新的预约。
        </p>

        <div v-else class="appointment-stack">
          <article
            v-for="appointment in appointments"
            :key="appointment.appointmentId"
            class="appointment-card"
            :class="`appointment-card--${resolveStatusTone(appointment.status)}`"
          >
            <div class="appointment-card__glow"></div>

            <div class="appointment-topline">
              <div class="appointment-identity">
                <div class="appointment-avatar">
                  {{ resolveInitials(appointment.counselorName, appointment.anonymousName) }}
                </div>
                <div>
                  <p class="appointment-title">预约 #{{ appointment.appointmentId }}</p>
                  <p class="appointment-time">
                    {{ formatDateTime(appointment.startTime) }} - {{ formatDateTime(appointment.endTime) }}
                  </p>
                </div>
              </div>

              <span class="status-pill" :class="`status-pill--${resolveStatusTone(appointment.status)}`">
                {{ resolveStatusLabel(appointment.status) }}
              </span>
            </div>

            <p class="issue-summary">{{ appointment.issueSummary }}</p>

            <div class="appointment-meta">
              <span class="info-chip">匿名名称 · {{ appointment.anonymousName }}</span>
              <span class="info-chip">咨询师 · {{ appointment.counselorName || '待分配' }}</span>
              <span class="info-chip">创建于 {{ formatDateTime(appointment.createdAt) }}</span>
            </div>

            <div v-if="appointment.resultMessage" class="result-message">
              <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
                <path d="M12 4.75v14.5" />
                <path d="M4.75 12h14.5" />
              </svg>
              <p>{{ appointment.resultMessage }}</p>
            </div>

            <div class="appointment-footer">
              <div class="appointment-footer__note">
                <span>状态会影响是否开放私密聊天室入口</span>
              </div>

              <button
                v-if="canOpenChat(appointment.status)"
                class="ghost-button"
                type="button"
                @click="openChat(appointment.appointmentId)"
              >
                进入私密聊天室
                <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
                  <path d="M5 12h14" />
                  <path d="m13 6 6 6-6 6" />
                </svg>
              </button>
            </div>
          </article>
        </div>

        <p v-if="errorMessage" class="error-text">{{ errorMessage }}</p>
      </section>
    </div>
  </section>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700;800&family=Noto+Serif+SC:wght@500;600;700&display=swap');

.appointment-list-page {
  min-height: 100%;
  padding: 0.4rem 0 2.4rem;
  color: #2c3028;
}

.page-shell {
  max-width: 1280px;
  margin: 0 auto;
}

.page-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(300px, 0.65fr);
  gap: 1.5rem;
  align-items: stretch;
  margin-bottom: 1.6rem;
}

.hero-copy,
.hero-aside-card,
.metric-card,
.list-panel,
.appointment-card {
  border: 1px solid rgba(78, 86, 77, 0.05);
  border-radius: 24px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.94) 0%, rgba(248, 244, 237, 0.86) 100%);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.04);
  backdrop-filter: blur(16px);
}

.hero-copy {
  padding: 1.8rem;
}

.hero-eyebrow-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0.85rem;
  align-items: center;
}

.eyebrow,
.section-kicker,
.metric-card span,
.hero-aside-card__top span {
  margin: 0;
  font: 800 0.74rem/1 'Manrope', sans-serif;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: #7b6857;
}

.hero-badge,
.status-chip,
.info-chip,
.status-pill {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  padding: 0.52rem 0.8rem;
  border-radius: 999px;
  font: 800 0.72rem/1 'Manrope', sans-serif;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.hero-badge {
  background: rgba(97, 122, 105, 0.12);
  color: #5e7465;
}

.hero-badge svg,
.hero-aside-card__icon svg,
.result-message svg,
.ghost-button svg {
  width: 18px;
  height: 18px;
  stroke: currentColor;
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.hero-copy h1,
.section-head h2,
.appointment-title {
  margin: 0;
  font-family: 'Noto Serif SC', serif;
  font-weight: 600;
}

.hero-copy h1 {
  margin-top: 0.95rem;
  font-size: clamp(2rem, 3.2vw, 3.45rem);
  line-height: 1.12;
}

.lead,
.metric-card p,
.issue-summary,
.appointment-time,
.appointment-meta,
.result-message p,
.error-text,
.state-text,
.appointment-footer__note span,
.hero-aside-card p {
  font-family: 'Manrope', sans-serif;
}

.lead {
  margin: 1rem 0 0;
  max-width: 720px;
  font-size: 1rem;
  line-height: 1.84;
  color: rgba(44, 48, 40, 0.74);
}

.hero-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 1rem;
  margin-top: 1.6rem;
}

.metric-card {
  padding: 1.2rem 1.25rem;
}

.metric-card--warm {
  background:
    linear-gradient(180deg, rgba(255, 249, 241, 0.96) 0%, rgba(249, 242, 232, 0.88) 100%);
}

.metric-card strong,
.hero-aside-card strong {
  display: block;
  margin-top: 0.68rem;
  font-family: 'Noto Serif SC', serif;
  font-size: 1.72rem;
  font-weight: 600;
  color: #283128;
}

.metric-card p {
  margin: 0.7rem 0 0;
  font-size: 0.88rem;
  line-height: 1.72;
  color: rgba(44, 48, 40, 0.66);
}

.hero-aside-card {
  height: 100%;
  padding: 1.55rem;
  background:
    radial-gradient(circle at top right, rgba(107, 134, 114, 0.16), transparent 34%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.96) 0%, rgba(241, 246, 239, 0.88) 100%);
}

.hero-aside-card__top {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
}

.hero-aside-card__icon {
  width: 3rem;
  height: 3rem;
  border-radius: 18px;
  display: grid;
  place-items: center;
  background: rgba(97, 122, 105, 0.12);
  color: #5e7465;
}

.hero-aside-card p {
  margin: 0.9rem 0 0;
  font-size: 0.92rem;
  line-height: 1.82;
  color: rgba(44, 48, 40, 0.68);
}

.list-panel {
  padding: 1.55rem;
}

.section-head {
  margin-bottom: 1.15rem;
}

.section-head-inline {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: end;
}

.status-chip {
  background: rgba(255, 250, 240, 0.82);
  color: #696152;
  border: 1px solid rgba(88, 93, 84, 0.12);
}

.appointment-stack {
  display: grid;
  gap: 1.1rem;
}

.appointment-card {
  position: relative;
  overflow: hidden;
  padding: 1.45rem;
  transition: all 0.3s ease;
}

.appointment-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 18px 34px rgba(55, 67, 57, 0.1);
}

.appointment-card__glow {
  position: absolute;
  right: -2rem;
  bottom: -2.5rem;
  width: 7.5rem;
  height: 7.5rem;
  border-radius: 999px;
  opacity: 0.48;
  pointer-events: none;
}

.appointment-card--pending .appointment-card__glow {
  background: radial-gradient(circle, rgba(210, 177, 120, 0.28), transparent 70%);
}

.appointment-card--accepted .appointment-card__glow,
.appointment-card--active .appointment-card__glow {
  background: radial-gradient(circle, rgba(107, 134, 114, 0.26), transparent 70%);
}

.appointment-card--done .appointment-card__glow {
  background: radial-gradient(circle, rgba(132, 156, 164, 0.24), transparent 70%);
}

.appointment-topline {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: start;
  margin-bottom: 0.95rem;
}

.appointment-identity {
  display: flex;
  gap: 0.95rem;
  align-items: center;
}

.appointment-avatar {
  width: 3.4rem;
  height: 3.4rem;
  border-radius: 18px;
  display: grid;
  place-items: center;
  flex-shrink: 0;
  background: linear-gradient(135deg, #6f8774, #ccd7c7);
  color: #fffdf8;
  font: 700 0.92rem/1 'Manrope', sans-serif;
}

.appointment-title {
  font-size: 1.3rem;
  line-height: 1.24;
}

.appointment-time {
  margin: 0.45rem 0 0;
  font-size: 0.9rem;
  color: rgba(44, 48, 40, 0.62);
}

.status-pill {
  flex-shrink: 0;
}

.status-pill--pending {
  background: rgba(216, 182, 120, 0.16);
  color: #996d28;
}

.status-pill--accepted {
  background: rgba(98, 129, 107, 0.14);
  color: #5a725f;
}

.status-pill--active {
  background: rgba(79, 116, 134, 0.15);
  color: #406879;
}

.status-pill--done {
  background: rgba(113, 140, 150, 0.15);
  color: #59737d;
}

.status-pill--muted {
  background: rgba(128, 124, 117, 0.12);
  color: #716960;
}

.issue-summary {
  margin: 0;
  font-size: 0.98rem;
  line-height: 1.84;
  color: rgba(44, 48, 40, 0.74);
}

.appointment-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  margin-top: 1rem;
}

.info-chip {
  background: rgba(255, 255, 255, 0.76);
  color: rgba(68, 74, 66, 0.7);
}

.result-message {
  display: grid;
  grid-template-columns: 18px minmax(0, 1fr);
  gap: 0.7rem;
  align-items: start;
  margin-top: 1rem;
  padding: 1rem 1.05rem;
  border-radius: 18px;
  background: rgba(248, 240, 231, 0.9);
  color: #8a533e;
}

.result-message p {
  margin: 0;
  font-size: 0.92rem;
  line-height: 1.78;
}

.appointment-footer {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
  margin-top: 1.15rem;
}

.appointment-footer__note span {
  display: block;
  font-size: 0.84rem;
  line-height: 1.68;
  color: rgba(44, 48, 40, 0.58);
}

.ghost-button {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  border: 1px solid rgba(55, 67, 57, 0.12);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.72);
  color: #2c3028;
  min-height: 3rem;
  padding: 0 1.05rem;
  font: 800 0.76rem/1 'Manrope', sans-serif;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  cursor: pointer;
  transition: all 0.3s ease;
}

.ghost-button:hover {
  transform: translateY(-2px);
  border-color: rgba(55, 67, 57, 0.2);
  box-shadow: 0 16px 30px rgba(55, 67, 57, 0.08);
}

.state-text,
.error-text {
  margin: 0.8rem 0 0;
  font-size: 0.96rem;
  line-height: 1.8;
}

.error-text {
  font-weight: 700;
  color: #a64939;
}

@media (max-width: 980px) {
  .page-hero {
    grid-template-columns: 1fr;
  }

  .hero-metrics {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .hero-copy,
  .hero-aside-card,
  .list-panel,
  .appointment-card {
    padding: 1.25rem;
  }

  .appointment-topline,
  .section-head-inline,
  .appointment-footer {
    flex-direction: column;
    align-items: start;
  }
}
</style>
