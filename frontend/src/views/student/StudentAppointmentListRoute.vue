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

// 分页状态
const currentPage = ref(1)
const pageSize = 6
const totalPages = computed(() => Math.max(1, Math.ceil(appointments.value.length / pageSize)))
const pagedAppointments = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return appointments.value.slice(start, start + pageSize)
})

const activeCount = computed(() =>
    appointments.value.filter((item) => item.status === 'ACCEPTED' || item.status === 'IN_PROGRESS').length
)

const completedCount = computed(() =>
    appointments.value.filter((item) => item.status === 'COMPLETED').length
)

// 为画报风排版拆分日期和时间
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

function resolveStatusLabel(status: string): string {
  switch (status) {
    case 'PENDING': return '等待确认'
    case 'ACCEPTED': return '约定期'
    case 'IN_PROGRESS': return '正在沟通'
    case 'COMPLETED': return '已结束'
    case 'REJECTED': return '未能安排'
    case 'CANCELLED': return '已取消'
    default: return status
  }
}

function resolveStatusTone(status: string): AppointmentTone {
  switch (status) {
    case 'ACCEPTED': return 'accepted'
    case 'IN_PROGRESS': return 'active'
    case 'COMPLETED': return 'done'
    case 'PENDING': return 'pending'
    default: return 'muted'
  }
}

function canOpenChat(status: string): boolean {
  return status === 'ACCEPTED' || status === 'IN_PROGRESS' || status === 'COMPLETED'
}

async function loadAppointments(): Promise<void> {
  loading.value = true
  errorMessage.value = ''

  try {
    appointments.value = await fetchStudentAppointmentsApi()
    currentPage.value = 1
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    loading.value = false
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

onMounted(() => {
  void loadAppointments()
})
</script>

<template>
  <main class="editorial-appointment-page">
    <div class="page-container">

      <header class="journal-header">
        <div class="header-main">
          <span class="header-tag">Consultation Journal</span>
          <h1 class="header-title">会谈札记</h1>
          <p class="header-desc">
            这里按时间轴记录了你所有的预约行程与沟通轨迹。<br>
            当预约被受理后，你可以直接从这里的条目中推开那扇“私密聊天室”的门。
          </p>
        </div>

        <div class="header-stats">
          <div class="stat-item">
            <span class="stat-label">历史总计</span>
            <span class="stat-value">{{ loading ? '-' : appointments.length }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">即将进行 / 沟通中</span>
            <span class="stat-value">{{ loading ? '-' : activeCount }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">已沉淀</span>
            <span class="stat-value">{{ loading ? '-' : completedCount }}</span>
          </div>
        </div>
      </header>

      <div v-if="errorMessage" class="error-banner">{{ errorMessage }}</div>

      <div v-if="loading" class="loading-state">
        <div class="spinner"></div>
        <p>正在翻阅记录...</p>
      </div>

      <div v-else-if="!appointments.length" class="empty-state">
        <h2 class="empty-title">尚未留下足迹</h2>
        <p class="empty-desc">这里还是一张白纸，当你准备好倾诉时，可以去挑选一个安静的时段。</p>
        <button class="ghost-btn" @click="router.push({ name: 'student-appointment-slots' })">
          挑选可预约时段 <span class="arrow">→</span>
        </button>
      </div>

      <section v-else class="journal-list">

        <div class="list-head">
          <span class="list-kicker">所有条目</span>
          <span class="list-note">允许慢一点。如果还没准备好，你可以只是在这里看看，等准备好了再进入对话。</span>
        </div>

        <article
            v-for="appointment in pagedAppointments"
            :key="appointment.appointmentId"
            class="journal-entry"
            :class="`entry--${resolveStatusTone(appointment.status)}`"
        >
          <div class="entry-date-col">
            <span class="huge-date">{{ getDayMonth(appointment.startTime) }}</span>
            <span class="time-span">{{ getTimeSpan(appointment.startTime, appointment.endTime) }}</span>
            <span class="status-pill">{{ resolveStatusLabel(appointment.status) }}</span>
          </div>

          <div class="entry-content-col">
            <div class="entry-topline">
              <h3 class="counselor-name">
                与 咨询师 {{ appointment.counselorName || '待安排' }} 的会谈
              </h3>
              <span class="entry-id">#{{ appointment.appointmentId }}</span>
            </div>

            <blockquote class="issue-quote">
              “{{ appointment.issueSummary || '未填写具体摘要...' }}”
            </blockquote>

            <div v-if="appointment.resultMessage" class="result-message">
              <strong>系统/回复：</strong> {{ appointment.resultMessage }}
            </div>

            <div class="entry-footer">
              <div class="meta-tags">
                <span>身份：{{ appointment.anonymousName }}</span>
                <span class="dot">·</span>
                <span>创建于 {{ formatFullDate(appointment.createdAt) }}</span>
              </div>

              <button
                  v-if="canOpenChat(appointment.status)"
                  class="action-chat-btn"
                  @click="openChat(appointment.appointmentId)"
              >
                进入私密聊天室 <span class="arrow">→</span>
              </button>
              <span v-else class="disabled-action">聊天室未开放</span>
            </div>
          </div>
        </article>

        <nav class="pagination-nav" v-if="totalPages > 1">
          <button class="page-btn" :disabled="currentPage <= 1" @click="prevPage">
            <span class="arrow">←</span> 往前翻
          </button>

          <div class="page-indicator">
            <span>{{ currentPage }}</span> / <span>{{ totalPages }}</span>
          </div>

          <button class="page-btn" :disabled="currentPage >= totalPages" @click="nextPage">
            往后翻 <span class="arrow">→</span>
          </button>
        </nav>

      </section>
    </div>
  </main>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;800&family=Noto+Serif+SC:wght@500;600;700&display=swap');

/* 全局极简白纸底色 */
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

/* 头部排版 */
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

/* 列表头部 */
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
  max-width: 400px;
  text-align: right;
}

/* 札记条目（无框排版，依靠极大的留白区分） */
.journal-list {
  display: flex;
  flex-direction: column;
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

/* 左侧巨幕日期 */
.entry-date-col {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  position: relative;
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

/* 不同状态下，左侧锚点的颜色暗示 */
.entry--accepted .huge-date, .entry--active .huge-date {
  color: #3b4d40;
}
.entry--accepted .status-pill, .entry--active .status-pill {
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

/* 右侧内容区 */
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

/* 摘录样式的正文 */
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

/* 按钮交互 */
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

/* 状态样式 */
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
  to { transform: rotate(360deg); }
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

/* 分页器 */
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

/* 响应式 */
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
    grid-template-columns: 1fr; /* 移动端改为单列，巨型日期居顶 */
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
    gap: 1.5rem;
  }
}
</style>