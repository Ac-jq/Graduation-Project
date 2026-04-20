<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { fetchStudentAppointmentsApi } from '@/api/appointment'
import type { Appointment } from '@/api/types'
import { toErrorMessage } from '@/views/shared/page-logic'

type AppointmentTone = 'pending' | 'accepted' | 'active' | 'done' | 'muted'

const router = useRouter()
const loading = ref(false)
const errorMessage = ref('')
const appointments = ref<Appointment[]>([])
const currentPage = ref(1)
const pageSize = 6
let refreshTimer: number | null = null

const orderedAppointments = computed(() => {
  const groupWeight = (item: Appointment) => {
    if (item.chatAvailable) {
      return 0
    }
    if (item.status === 'PENDING') {
      return 1
    }
    return 2
  }

  return [...appointments.value].sort((left, right) => {
    const weightDiff = groupWeight(left) - groupWeight(right)
    if (weightDiff !== 0) {
      return weightDiff
    }
    return new Date(right.createdAt).getTime() - new Date(left.createdAt).getTime()
  })
})

const totalPages = computed(() => Math.max(1, Math.ceil(orderedAppointments.value.length / pageSize)))
const pagedAppointments = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return orderedAppointments.value.slice(start, start + pageSize)
})

const activeCount = computed(() => appointments.value.filter((item) => item.chatAvailable).length)
const pendingCount = computed(() => appointments.value.filter((item) => item.status === 'PENDING').length)
const completedCount = computed(() => appointments.value.filter((item) => item.chatEnded).length)

function getDayMonth(value: string): string {
  const d = new Date(value)
  return `${String(d.getMonth() + 1).padStart(2, '0')}/${String(d.getDate()).padStart(2, '0')}`
}

function getTimeSpan(start: string, end: string): string {
  const s = new Date(start)
  const e = new Date(end)
  const formatTime = (date: Date) => `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
  return `${formatTime(s)} - ${formatTime(e)}`
}

function formatFullDate(value: string): string {
  const d = new Date(value)
  return `${d.getFullYear()}/${String(d.getMonth() + 1).padStart(2, '0')}/${String(d.getDate()).padStart(2, '0')}`
}

function resolveStatusLabel(appointment: Appointment): string {
  if (appointment.chatAvailable) {
    return '可进入'
  }
  if (appointment.status === 'PENDING') {
    return '等待确认'
  }
  if (appointment.chatEnded) {
    return '已结束'
  }

  switch (appointment.status) {
    case 'ACCEPTED':
      return '已接受'
    case 'IN_PROGRESS':
      return '沟通中'
    case 'REJECTED':
      return '未通过'
    case 'CANCELED':
      return '已取消'
    case 'COMPLETED':
      return '已结束'
    default:
      return appointment.status
  }
}

function resolveStatusTone(appointment: Appointment): AppointmentTone {
  if (appointment.chatAvailable) {
    return 'active'
  }
  if (appointment.status === 'PENDING') {
    return 'pending'
  }
  if (appointment.chatEnded) {
    return 'done'
  }
  if (appointment.status === 'ACCEPTED') {
    return 'accepted'
  }
  return 'muted'
}

function resolveGroupLabel(appointment: Appointment): string {
  if (appointment.chatAvailable) {
    return '可进入'
  }
  if (appointment.status === 'PENDING') {
    return '等待确认'
  }
  return '已结束'
}

function shouldShowGroupLabel(appointment: Appointment, index: number): boolean {
  if (index === 0) {
    return true
  }
  return resolveGroupLabel(appointment) !== resolveGroupLabel(pagedAppointments.value[index - 1])
}

async function loadAppointments(silent = false): Promise<void> {
  if (!silent) {
    loading.value = true
  }
  errorMessage.value = ''

  try {
    appointments.value = await fetchStudentAppointmentsApi()
    if (!silent) {
      currentPage.value = 1
    }
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    if (!silent) {
      loading.value = false
    }
  }
}

function prevPage(): void {
  if (currentPage.value > 1) {
    currentPage.value--
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
}

function nextPage(): void {
  if (currentPage.value < totalPages.value) {
    currentPage.value++
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
}

async function openChat(appointmentId: number): Promise<void> {
  await router.push({ name: 'student-chat', params: { appointmentId } })
}

function startRefreshTimer(): void {
  stopRefreshTimer()
  refreshTimer = window.setInterval(() => {
    void loadAppointments(true)
  }, 5000)
}

function stopRefreshTimer(): void {
  if (refreshTimer !== null) {
    window.clearInterval(refreshTimer)
    refreshTimer = null
  }
}

onMounted(() => {
  void loadAppointments()
  startRefreshTimer()
})

onBeforeUnmount(() => {
  stopRefreshTimer()
})
</script>

<template>
  <main class="editorial-appointment-page">
    <div class="page-container">
      <header class="journal-header">
        <div class="header-main">
          <span class="header-tag">Consultation Journal</span>
          <h1 class="header-title">预约记录</h1>
          <p class="header-desc">
            这里会按状态整理你的预约记录。可直接进入的聊天室会优先显示，等待老师确认的记录排在其后，
            已结束的会话会自动收在最后，避免误点进入无效聊天室。
          </p>
        </div>

        <div class="header-stats">
          <div class="stat-item">
            <span class="stat-label">全部预约</span>
            <span class="stat-value">{{ loading ? '-' : appointments.length }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">可进入</span>
            <span class="stat-value">{{ loading ? '-' : activeCount }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">已结束</span>
            <span class="stat-value">{{ loading ? '-' : completedCount }}</span>
          </div>
        </div>
      </header>

      <div v-if="errorMessage" class="error-banner">{{ errorMessage }}</div>

      <div v-if="loading" class="loading-state">
        <div class="spinner"></div>
        <p>正在整理预约记录...</p>
      </div>

      <div v-else-if="!appointments.length" class="empty-state">
        <h2 class="empty-title">还没有预约记录</h2>
        <p class="empty-desc">你可以先选择咨询师和时间段，系统会在这里持续更新预约状态。</p>
        <button class="ghost-btn" @click="router.push({ name: 'student-appointment-slots' })">
          去预约时段 <span class="arrow">→</span>
        </button>
      </div>

      <section v-else class="journal-list">
        <div class="list-head">
          <span class="list-kicker">预约分组</span>
          <span class="list-note">顺序固定为：可进入 → 等待确认 → 已结束。</span>
        </div>

        <template v-for="(appointment, index) in pagedAppointments" :key="appointment.appointmentId">
          <div v-if="shouldShowGroupLabel(appointment, index)" class="group-label">
            {{ resolveGroupLabel(appointment) }}
          </div>

          <article class="journal-entry" :class="`entry--${resolveStatusTone(appointment)}`">
            <div class="entry-date-col">
              <span class="huge-date">{{ getDayMonth(appointment.startTime) }}</span>
              <span class="time-span">{{ getTimeSpan(appointment.startTime, appointment.endTime) }}</span>
              <span class="status-pill">{{ resolveStatusLabel(appointment) }}</span>
            </div>

            <div class="entry-content-col">
              <div class="entry-topline">
                <h3 class="counselor-name">与 {{ appointment.counselorName || '待分配咨询师' }} 的会谈</h3>
                <span class="entry-id">#{{ appointment.appointmentId }}</span>
              </div>

              <blockquote class="issue-quote">
                “{{ appointment.issueSummary || '本次预约未填写摘要。' }}”
              </blockquote>

              <div v-if="appointment.resultMessage" class="result-message">
                <strong>老师回复：</strong> {{ appointment.resultMessage }}
              </div>

              <div class="entry-footer">
                <div class="meta-tags">
                  <span>身份：{{ appointment.anonymousName }}</span>
                  <span class="dot">·</span>
                  <span>创建于 {{ formatFullDate(appointment.createdAt) }}</span>
                  <span class="dot">·</span>
                  <span>聊天室：{{ appointment.chatStatus || '未开启' }}</span>
                </div>

                <button
                  v-if="appointment.chatAvailable"
                  class="action-chat-btn"
                  @click="openChat(appointment.appointmentId)"
                >
                  进入聊天室 <span class="arrow">→</span>
                </button>
                <span v-else class="disabled-action">
                  {{ appointment.status === 'PENDING' ? '等待老师确认后开放' : '当前聊天室不可进入' }}
                </span>
              </div>
            </div>
          </article>
        </template>

        <nav class="pagination-nav" v-if="totalPages > 1">
          <button class="page-btn" :disabled="currentPage <= 1" @click="prevPage">
            <span class="arrow">←</span> 上一页
          </button>

          <div class="page-indicator">
            <span>{{ currentPage }}</span> / <span>{{ totalPages }}</span>
          </div>

          <button class="page-btn" :disabled="currentPage >= totalPages" @click="nextPage">
            下一页 <span class="arrow">→</span>
          </button>
        </nav>
      </section>
    </div>
  </main>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;800&family=Noto+Serif+SC:wght@500;600;700&display=swap');

.editorial-appointment-page {
  min-height: 100vh;
  background: #fcfbf9;
  color: #1e2821;
  font-family: 'Manrope', 'Noto Serif SC', sans-serif;
  padding: 4rem 2vw 8rem;
  box-sizing: border-box;
}

.page-container {
  max-width: 1060px;
  margin: 0 auto;
}

.journal-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  padding-bottom: 3rem;
  margin-bottom: 2rem;
  border-bottom: 1px solid rgba(42, 54, 46, 0.12);
  gap: 4rem;
}

.header-main {
  max-width: 580px;
}

.header-tag {
  display: block;
  font-family: 'Manrope', sans-serif;
  font-size: 0.85rem;
  font-weight: 700;
  letter-spacing: 0.15em;
  color: #8a9c90;
  text-transform: uppercase;
  margin-bottom: 1rem;
}

.header-title {
  font-family: 'Noto Serif SC', serif;
  font-size: 2.5rem;
  font-weight: 600;
  color: #1e2821;
  margin: 0 0 1.2rem 0;
  letter-spacing: 0.05em;
}

.header-desc {
  font-size: 1.05rem;
  color: #6a7c70;
  line-height: 1.8;
  margin: 0;
}

.header-stats {
  display: flex;
  gap: 3rem;
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.stat-label {
  font-family: 'Noto Serif SC', serif;
  font-size: 0.85rem;
  color: #8a9c90;
}

.stat-value {
  font-family: 'Manrope', sans-serif;
  font-size: 2.2rem;
  font-weight: 600;
  color: #2a362e;
  line-height: 1;
}

.list-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
  padding: 0 1rem;
}

.list-kicker {
  font-family: 'Noto Serif SC', serif;
  font-size: 1.1rem;
  font-weight: 600;
  color: #1e2821;
}

.list-note {
  font-size: 0.9rem;
  color: #8a9c90;
  font-style: italic;
  max-width: 420px;
  text-align: right;
}

.journal-list {
  display: flex;
  flex-direction: column;
}

.group-label {
  padding: 1rem;
  margin-top: 1.5rem;
  color: #5c6b60;
  font-family: 'Noto Serif SC', serif;
  font-size: 1rem;
  font-weight: 600;
  border-top: 1px solid rgba(42, 54, 46, 0.08);
}

.journal-entry {
  display: grid;
  grid-template-columns: 200px minmax(0, 1fr);
  gap: 4rem;
  padding: 3.5rem 1rem;
  border-bottom: 1px solid rgba(42, 54, 46, 0.06);
  transition: background 0.4s ease;
}

.journal-entry:hover {
  background: rgba(255, 255, 255, 0.6);
}

.entry-date-col {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.huge-date {
  font-family: 'Manrope', sans-serif;
  font-size: 3.8rem;
  font-weight: 800;
  letter-spacing: -0.04em;
  color: #2a362e;
  line-height: 1;
  margin-bottom: 0.2rem;
  transition: color 0.3s ease;
}

.time-span {
  font-family: 'Manrope', sans-serif;
  font-size: 1.05rem;
  color: #8a9c90;
  font-weight: 500;
}

.status-pill {
  display: inline-flex;
  align-self: flex-start;
  margin-top: 1rem;
  font-family: 'Noto Serif SC', serif;
  font-size: 0.85rem;
  font-weight: 600;
  padding: 0.4rem 1rem;
  border-radius: 100px;
  background: rgba(130, 150, 138, 0.15);
  color: #5c6b60;
  transition: all 0.3s ease;
}

.entry--accepted .huge-date,
.entry--active .huge-date {
  color: #3b4d40;
}

.entry--accepted .status-pill,
.entry--active .status-pill {
  background: #2a362e;
  color: #ffffff;
}

.entry--done .huge-date {
  color: #7b8c80;
}

.entry--pending .status-pill {
  background: rgba(193, 150, 83, 0.15);
  color: #9e7330;
}

.entry--muted .huge-date {
  color: #b5c2b9;
}

.entry-content-col {
  display: flex;
  flex-direction: column;
}

.entry-topline {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}

.counselor-name {
  font-family: 'Noto Serif SC', serif;
  font-size: 1.4rem;
  font-weight: 600;
  color: #1e2821;
  margin: 0;
}

.entry-id {
  font-family: 'Manrope', sans-serif;
  font-size: 0.95rem;
  color: #b5c2b9;
}

.issue-quote {
  margin: 0 0 2rem 0;
  padding-left: 1.5rem;
  border-left: 2px solid rgba(42, 54, 46, 0.15);
  font-size: 1.1rem;
  line-height: 1.8;
  color: #4a5c51;
}

.result-message {
  margin-bottom: 2rem;
  padding: 1.2rem 1.5rem;
  background: rgba(245, 240, 235, 0.6);
  border-radius: 12px;
  font-size: 0.95rem;
  line-height: 1.6;
  color: #8c6a5c;
}

.result-message strong {
  font-weight: 600;
  color: #6b4d42;
}

.entry-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: auto;
  gap: 1rem;
}

.meta-tags {
  font-family: 'Noto Serif SC', serif;
  font-size: 0.9rem;
  color: #8a9c90;
}

.dot {
  margin: 0 0.8rem;
  color: #cbd5cf;
}

.action-chat-btn {
  background: transparent;
  border: none;
  font-family: 'Noto Serif SC', serif;
  font-size: 1.05rem;
  font-weight: 600;
  color: #2a362e;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0;
  transition: color 0.3s ease;
}

.action-chat-btn:hover {
  color: #5c6b60;
}

.disabled-action {
  font-family: 'Noto Serif SC', serif;
  font-size: 0.95rem;
  color: #b5c2b9;
}

.error-banner {
  background: rgba(140, 74, 74, 0.08);
  color: #8c4a4a;
  padding: 1.5rem;
  border-radius: 12px;
  text-align: center;
  font-family: 'Noto Serif SC', serif;
  margin-bottom: 2rem;
}

.loading-state,
.empty-state {
  text-align: center;
  padding: 8rem 0;
  color: #7b8c80;
  font-family: 'Noto Serif SC', serif;
}

.spinner {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: 2px solid rgba(130, 150, 138, 0.2);
  border-top-color: #2a362e;
  animation: spin 0.8s linear infinite;
  margin: 0 auto 1.5rem;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.empty-title {
  font-family: 'Noto Serif SC', serif;
  font-size: 1.6rem;
  color: #2a362e;
  margin: 0 0 1rem 0;
}

.empty-desc {
  margin-bottom: 2rem;
}

.ghost-btn {
  background: transparent;
  border: 1px solid rgba(42, 54, 46, 0.3);
  color: #2a362e;
  padding: 1rem 2.2rem;
  border-radius: 100px;
  font-family: 'Noto Serif SC', serif;
  font-size: 1.05rem;
  font-weight: 600;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  transition: all 0.3s ease;
}

.ghost-btn:hover {
  background: rgba(42, 54, 46, 0.05);
  border-color: #2a362e;
}

.pagination-nav {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 2rem;
  margin-top: 4rem;
  padding-top: 2rem;
  border-top: 1px solid rgba(42, 54, 46, 0.08);
}

.page-btn {
  background: transparent;
  border: none;
  font-family: 'Noto Serif SC', serif;
  font-size: 1.05rem;
  font-weight: 600;
  color: #2a362e;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  transition: all 0.3s ease;
}

.page-btn:hover:not(:disabled) {
  color: #5c6b60;
}

.page-btn:disabled {
  color: #cbd5cf;
  cursor: not-allowed;
}

.page-indicator {
  font-family: 'Manrope', sans-serif;
  font-size: 1rem;
  color: #8a9c90;
  letter-spacing: 0.1em;
}

.page-indicator span {
  color: #2a362e;
  font-weight: 600;
}

.arrow {
  font-family: 'Manrope', sans-serif;
  transition: transform 0.3s ease;
}

.action-chat-btn:hover .arrow,
.ghost-btn:hover .arrow,
.page-btn:hover:not(:disabled) .arrow:last-child {
  transform: translateX(4px);
}

.page-btn:hover:not(:disabled) .arrow:first-child {
  transform: translateX(-4px);
}

@media (max-width: 1024px) {
  .journal-entry {
    grid-template-columns: 160px minmax(0, 1fr);
    gap: 2.5rem;
  }

  .huge-date {
    font-size: 3rem;
  }
}

@media (max-width: 768px) {
  .journal-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 2rem;
  }

  .header-stats {
    flex-wrap: wrap;
    gap: 2rem;
  }

  .list-head {
    flex-direction: column;
    align-items: flex-start;
    gap: 1rem;
    padding: 0;
  }

  .list-note {
    text-align: left;
  }

  .journal-entry {
    grid-template-columns: 1fr;
    gap: 1.5rem;
    padding: 2.5rem 0;
  }

  .huge-date {
    font-size: 2.5rem;
    display: inline-block;
    margin-right: 1rem;
  }

  .time-span {
    display: inline-block;
  }

  .status-pill {
    margin-top: 0;
    margin-left: 1rem;
  }

  .entry-footer {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
