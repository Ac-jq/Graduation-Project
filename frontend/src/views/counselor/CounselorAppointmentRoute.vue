<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { acceptAppointmentApi, fetchCounselorAppointmentsApi, rejectAppointmentApi } from '@/api/appointment'
import { closeConsultChatSessionApi } from '@/api/chat'
import type { Appointment, AppointmentActionRequest } from '@/api/types'
import { toErrorMessage } from '@/views/shared/page-logic'
import SaaSBackground from '@/components/SaaSBackground.vue'

type AppointmentTone = 'pending' | 'accepted' | 'active' | 'done' | 'muted'

const router = useRouter()
const loading = ref(false)
const processing = ref(false)
const errorMessage = ref('')
const appointments = ref<Appointment[]>([])
const actionForm = reactive<Record<number, AppointmentActionRequest>>({})
const searchKeyword = ref('')
const currentPage = ref(1)
const pageSize = 6
let refreshTimer: number | null = null

const orderedAppointments = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase()
  const filtered = keyword
    ? appointments.value.filter((appointment) => {
        const fields = [
          String(appointment.appointmentId),
          appointment.anonymousName,
          appointment.counselorName,
          appointment.issueSummary,
          appointment.status,
          appointment.resultMessage,
          appointment.chatStatus
        ]
        return fields.some((field) => (field ?? '').toLowerCase().includes(keyword))
      })
    : appointments.value

  const groupWeight = (item: Appointment) => {
    if (item.chatAvailable) {
      return 0
    }
    if (item.status === 'PENDING') {
      return 1
    }
    return 2
  }

  return [...filtered].sort((left, right) => {
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
const pendingCount = computed(() => appointments.value.filter((item) => item.status === 'PENDING').length)
const activeCount = computed(() => appointments.value.filter((item) => item.chatAvailable).length)

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

function resolveStatusLabel(appointment: Appointment): string {
  if (appointment.chatAvailable) {
    return '可进入'
  }
  if (appointment.status === 'PENDING') {
    return '待处理'
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
      return '已拒绝'
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
    return '等待处理'
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
    appointments.value = await fetchCounselorAppointmentsApi()
    for (const appointment of appointments.value) {
      actionForm[appointment.appointmentId] ??= { resultMessage: appointment.resultMessage ?? '' }
    }
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

function resolveActionPayload(appointmentId: number): AppointmentActionRequest {
  return actionForm[appointmentId] ?? { resultMessage: '' }
}

function ensureActionModel(appointmentId: number): AppointmentActionRequest {
  actionForm[appointmentId] ??= { resultMessage: '' }
  return actionForm[appointmentId]
}

async function acceptAppointment(appointmentId: number): Promise<void> {
  processing.value = true
  errorMessage.value = ''
  try {
    await acceptAppointmentApi(appointmentId, resolveActionPayload(appointmentId))
    await loadAppointments()
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    processing.value = false
  }
}

async function rejectAppointment(appointmentId: number): Promise<void> {
  processing.value = true
  errorMessage.value = ''
  try {
    await rejectAppointmentApi(appointmentId, resolveActionPayload(appointmentId))
    await loadAppointments()
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    processing.value = false
  }
}

async function closeChat(appointmentId: number): Promise<void> {
  processing.value = true
  errorMessage.value = ''
  try {
    await closeConsultChatSessionApi(appointmentId)
    await loadAppointments()
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    processing.value = false
  }
}

async function openChat(appointmentId: number): Promise<void> {
  await router.push({ name: 'counselor-chat', params: { appointmentId } })
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

function startRefreshTimer(): void {
  stopRefreshTimer()
  refreshTimer = window.setInterval(() => {
    void loadAppointments(true)
  }, 10000)
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

watch(searchKeyword, () => {
  currentPage.value = 1
})
</script>

<template>
  <main class="editorial-ledger-page">
    <SaaSBackground />
    <div class="page-container">
      <header class="ledger-header">
        <div class="header-main">
          <span class="header-tag">Appointment Ledger</span>
          <h1 class="huge-title">预约处理</h1>
          <p class="header-lead">
            这里按优先顺序整理所有与你有关的预约。可进入聊天室的预约置顶，待处理预约在中间，
            已结束的记录自动沉到底部，方便你快速判断下一步动作。
          </p>
        </div>

        <div class="header-stats">
          <div class="stat-item">
            <span class="stat-label">全部预约</span>
            <span class="stat-value">{{ loading ? '-' : appointments.length }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">待处理</span>
            <span class="stat-value highlight">{{ loading ? '-' : pendingCount }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">可进入</span>
            <span class="stat-value">{{ loading ? '-' : activeCount }}</span>
          </div>
        </div>
      </header>

      <div v-if="errorMessage" class="error-banner">{{ errorMessage }}</div>

      <div v-if="loading" class="loading-state">
        <div class="spinner"></div>
        <p>正在加载预约处理列表...</p>
      </div>

      <div v-else-if="!appointments.length" class="empty-state">
        <h2 class="empty-title">当前没有预约请求</h2>
        <p class="empty-desc">新的学生预约会自动出现在这里。</p>
      </div>

      <section v-else class="ledger-section">
        <div class="list-toolbar">
          <input
            v-model.trim="searchKeyword"
            class="toolbar-search"
            type="search"
            placeholder="搜索预约编号、学生别名、摘要或状态"
          >
          <span class="toolbar-status">第 {{ currentPage }} / {{ totalPages }} 页</span>
        </div>

        <div class="ledger-stream">
          <template v-for="(appointment, index) in pagedAppointments" :key="appointment.appointmentId">
            <div v-if="shouldShowGroupLabel(appointment, index)" class="group-label">
              {{ resolveGroupLabel(appointment) }}
            </div>

            <article class="ledger-entry" :class="`entry--${resolveStatusTone(appointment)}`">
              <div class="entry-time-col">
                <span class="huge-date">{{ getDayMonth(appointment.startTime) }}</span>
                <span class="time-span">{{ getTimeSpan(appointment.startTime, appointment.endTime) }}</span>
                <span class="status-pill">{{ resolveStatusLabel(appointment) }}</span>
              </div>

              <div class="entry-content-col">
                <div class="entry-topline">
                  <h3 class="student-name">{{ appointment.anonymousName || '匿名来访者' }}</h3>
                  <span class="entry-id">Req #{{ appointment.appointmentId }}</span>
                </div>

                <blockquote class="issue-quote">
                  “{{ appointment.issueSummary || '该学生未填写本次预约摘要。' }}”
                </blockquote>

                <div class="counselor-reply-area">
                  <label class="reply-label">给学生的回复</label>
                  <textarea
                    v-model="ensureActionModel(appointment.appointmentId).resultMessage"
                    class="sleek-textarea"
                    rows="2"
                    placeholder="例如：预约已确认，请按时进入聊天室。"
                  />
                </div>

                <div class="entry-actions">
                  <div class="action-group">
                    <button
                      v-if="appointment.status === 'PENDING'"
                      class="action-btn action-btn--primary"
                      type="button"
                      :disabled="processing"
                      @click="acceptAppointment(appointment.appointmentId)"
                    >
                      接受预约 <span class="arrow">→</span>
                    </button>
                    <button
                      v-if="appointment.status === 'PENDING'"
                      class="action-btn action-btn--danger"
                      type="button"
                      :disabled="processing"
                      @click="rejectAppointment(appointment.appointmentId)"
                    >
                      拒绝预约
                    </button>
                    <button
                      v-if="appointment.chatAvailable"
                      class="action-btn"
                      type="button"
                      :disabled="processing"
                      @click="closeChat(appointment.appointmentId)"
                    >
                      结束聊天室
                    </button>
                  </div>

                  <button
                    v-if="appointment.chatAvailable"
                    class="action-link"
                    type="button"
                    @click="openChat(appointment.appointmentId)"
                  >
                    进入聊天室 <span class="arrow">→</span>
                  </button>
                  <span v-else class="action-muted">
                    {{ appointment.status === 'PENDING' ? '待确认后开放聊天室' : '当前聊天室不可进入' }}
                  </span>
                </div>
              </div>
            </article>
          </template>
        </div>

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
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700;800&family=Noto+Serif+SC:wght@500;600;700&display=swap');

.editorial-ledger-page {
  min-height: 100vh;
  position: relative;
  isolation: isolate;
  background: #fcfbf9;
  color: #1e2821;
  font-family: 'Manrope', 'Noto Serif SC', sans-serif;
  padding: 4rem 2vw 8rem;
  box-sizing: border-box;
  overflow-x: hidden;
}

.page-container {
  max-width: 1060px;
  margin: 0 auto;
  position: relative;
  z-index: 1;
}

.ledger-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  padding-bottom: 3rem;
  margin-bottom: 2rem;
  border-bottom: 1px solid rgba(42, 54, 46, 0.12);
  gap: 4rem;
}

.header-main {
  max-width: 600px;
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

.huge-title {
  font-family: 'Noto Serif SC', serif;
  font-size: 2.6rem;
  font-weight: 600;
  color: #1e2821;
  margin: 0 0 1.2rem 0;
  letter-spacing: 0.05em;
}

.header-lead {
  font-size: 1.05rem;
  color: #5c6b60;
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

.stat-value.highlight {
  color: #8c4a4a;
}

.list-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
  padding: 0 0.5rem;
  gap: 1rem;
  flex-wrap: wrap;
}

.toolbar-status {
  font-family: 'Noto Serif SC', serif;
  font-size: 0.95rem;
  color: #8a9c90;
}

.toolbar-search {
  min-width: 300px;
  padding: 0.9rem 1.1rem;
  border-radius: 999px;
  border: 1px solid rgba(42, 54, 46, 0.08);
  background: rgba(255, 255, 255, 0.82);
  color: #2a362e;
  font: 500 0.95rem/1 'Manrope', sans-serif;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.toolbar-search:focus {
  outline: none;
  border-color: rgba(92, 107, 96, 0.18);
  box-shadow: 0 16px 32px rgba(54, 66, 58, 0.08);
  transform: translateY(-1px);
}

.ledger-stream {
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

.ledger-entry {
  display: grid;
  grid-template-columns: 180px minmax(0, 1fr);
  gap: 4rem;
  padding: 3.5rem 1rem;
  border-bottom: 1px solid rgba(42, 54, 46, 0.08);
  transition: background 0.4s ease;
}

.ledger-entry:hover {
  background: rgba(255, 255, 255, 0.6);
}

.entry-time-col {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.huge-date {
  font-family: 'Manrope', sans-serif;
  font-size: 3.2rem;
  font-weight: 800;
  letter-spacing: -0.04em;
  color: #2a362e;
  line-height: 1;
  margin-bottom: 0.2rem;
  transition: color 0.3s ease;
}

.time-span {
  font-family: 'Manrope', sans-serif;
  font-size: 1rem;
  color: #8a9c90;
  font-weight: 500;
}

.status-pill {
  display: inline-flex;
  align-self: flex-start;
  margin-top: 1.2rem;
  font-family: 'Noto Serif SC', serif;
  font-size: 0.85rem;
  font-weight: 600;
  padding: 0.4rem 1rem;
  border-radius: 100px;
  background: rgba(130, 150, 138, 0.15);
  color: #5c6b60;
  transition: all 0.3s ease;
}

.entry--pending .status-pill {
  background: rgba(213, 176, 115, 0.18);
  color: #9e7330;
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

.student-name {
  font-family: 'Noto Serif SC', serif;
  font-size: 1.6rem;
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
  margin: 0 0 2.5rem 0;
  padding-left: 1.5rem;
  border-left: 3px solid rgba(42, 54, 46, 0.15);
  font-size: 1.1rem;
  line-height: 1.8;
  color: #4a5c51;
}

.counselor-reply-area {
  display: flex;
  flex-direction: column;
  gap: 0.8rem;
  margin-bottom: 2.5rem;
  background: rgba(255, 255, 255, 0.5);
  padding: 1.5rem;
  border-radius: 16px;
  border: 1px solid rgba(130, 150, 138, 0.2);
}

.reply-label {
  font-family: 'Noto Serif SC', serif;
  font-size: 0.9rem;
  font-weight: 600;
  color: #5c6b60;
}

.sleek-textarea {
  width: 100%;
  border: none;
  border-bottom: 1px dashed rgba(42, 54, 46, 0.2);
  background: transparent;
  padding: 0.5rem 0;
  font-family: 'Noto Serif SC', serif;
  font-size: 1rem;
  line-height: 1.6;
  color: #1e2821;
  resize: vertical;
  outline: none;
  transition: border-color 0.3s ease;
}

.sleek-textarea::placeholder {
  color: #b5c2b9;
}

.sleek-textarea:focus {
  border-bottom-color: #2a362e;
  border-bottom-style: solid;
}

.entry-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: auto;
  flex-wrap: wrap;
  gap: 1.5rem;
}

.action-group {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
}

.action-btn {
  background: transparent;
  border: 1px solid rgba(130, 150, 138, 0.4);
  color: #2a362e;
  padding: 0.8rem 1.6rem;
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
  background: rgba(255, 255, 255, 0.8);
  border-color: #2a362e;
}

.action-btn--primary {
  background: #2a362e;
  border: none;
  color: #ffffff;
  box-shadow: 0 8px 16px rgba(42, 54, 46, 0.15);
}

.action-btn--primary:hover:not(:disabled) {
  background: #1c2620;
  color: #ffffff;
  transform: translateY(-2px);
  box-shadow: 0 12px 24px rgba(42, 54, 46, 0.25);
}

.action-btn--danger {
  color: #8c4a4a;
  border-color: rgba(140, 74, 74, 0.3);
}

.action-btn--danger:hover:not(:disabled) {
  background: rgba(140, 74, 74, 0.05);
  border-color: #8c4a4a;
}

.action-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.action-link {
  background: transparent;
  border: none;
  font-family: 'Noto Serif SC', serif;
  font-size: 1.05rem;
  font-weight: 600;
  color: #2a362e;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0;
  transition: color 0.3s ease;
}

.action-link:hover {
  color: #5c6b60;
}

.action-muted {
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
  margin: 0;
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

.action-link:hover .arrow,
.action-btn:hover:not(:disabled) .arrow,
.page-btn:hover:not(:disabled) .arrow:last-child {
  transform: translateX(4px);
}

.page-btn:hover:not(:disabled) .arrow:first-child {
  transform: translateX(-4px);
}

@media (max-width: 1024px) {
  .ledger-entry {
    grid-template-columns: 140px minmax(0, 1fr);
    gap: 2.5rem;
  }

  .huge-date {
    font-size: 2.5rem;
  }
}

@media (max-width: 768px) {
  .ledger-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 2rem;
  }

  .header-stats {
    flex-wrap: wrap;
    gap: 2rem;
  }

  .ledger-entry {
    grid-template-columns: 1fr;
    gap: 1.5rem;
    padding: 2.5rem 0;
  }

  .entry-time-col {
    flex-direction: row;
    align-items: center;
    flex-wrap: wrap;
    gap: 1rem;
  }

  .huge-date {
    font-size: 2.2rem;
  }

  .status-pill {
    margin-top: 0;
  }

  .entry-actions {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
