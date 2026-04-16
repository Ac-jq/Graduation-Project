<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { fetchNotificationsApi, markAllNotificationsReadApi, markNotificationReadApi } from '@/api/notification'
import type { NotificationItem } from '@/api/types'
import { toErrorMessage } from '@/views/shared/page-logic'

const loading = ref(false)
const processing = ref(false)
const errorMessage = ref('')
const notifications = ref<NotificationItem[]>([])

// 前端分页状态
const currentPage = ref(1)
const pageSize = 8

const unreadCount = computed(() => notifications.value.filter((notification) => !notification.read).length)
const latestNotification = computed(() => notifications.value[0] ?? null)
const readRate = computed(() => {
  if (notifications.value.length === 0) return 0
  return Math.round(((notifications.value.length - unreadCount.value) / notifications.value.length) * 100)
})

const totalPages = computed(() => Math.max(1, Math.ceil(notifications.value.length / pageSize)))
const pagedNotifications = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return notifications.value.slice(start, start + pageSize)
})

function formatFullDate(value: string | null): string {
  if (!value) return '未记录'
  const date = new Date(value)
  return `${date.getFullYear()}/${String(date.getMonth() + 1).padStart(2, '0')}/${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

function formatDatePart(value: string | null): string {
  if (!value) return '--/--'
  const d = new Date(value)
  return `${String(d.getMonth() + 1).padStart(2, '0')}/${String(d.getDate()).padStart(2, '0')}`
}

function formatTimePart(value: string | null): string {
  if (!value) return '--:--'
  const d = new Date(value)
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

async function loadNotifications(): Promise<void> {
  loading.value = true
  errorMessage.value = ''

  try {
    notifications.value = await fetchNotificationsApi()
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

async function markRead(notificationId: number): Promise<void> {
  processing.value = true
  errorMessage.value = ''

  try {
    await markNotificationReadApi(notificationId)
    await loadNotifications()
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    processing.value = false
  }
}

async function markAllRead(): Promise<void> {
  processing.value = true
  errorMessage.value = ''

  try {
    await markAllNotificationsReadApi()
    await loadNotifications()
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    processing.value = false
  }
}

onMounted(() => {
  void loadNotifications()
})
</script>

<template>
  <main class="editorial-dispatch-page">
    <div class="page-container">

      <header class="dispatch-header">
        <div class="header-main">
          <span class="header-tag">Counselor Dispatch</span>
          <h1 class="header-title">系统简报与通知</h1>
          <p class="header-desc">
            这里按时间轴汇总了系统派发的预约流转、聊天室变更及平台提醒。处理完毕后的信件将自动褪去高亮，安静地归档在列表下方。
          </p>
        </div>

        <div class="header-stats">
          <div class="stat-item">
            <span class="stat-label">信件总数</span>
            <span class="stat-value">{{ loading ? '-' : notifications.length }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">待阅览</span>
            <span class="stat-value highlight">{{ loading ? '-' : unreadCount }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">已读率</span>
            <span class="stat-text">{{ loading ? '-' : `${readRate}%` }}</span>
          </div>
        </div>
      </header>

      <div v-if="errorMessage" class="error-banner">{{ errorMessage }}</div>

      <div v-if="loading" class="loading-state">
        <div class="spinner"></div>
        <p>正在整理公文记录...</p>
      </div>

      <div v-else-if="!notifications.length" class="empty-state">
        <h2 class="empty-title">收件箱是空的</h2>
        <p class="empty-desc">当前没有任何待处理的系统通知或业务流转记录。</p>
      </div>

      <section v-else class="dispatch-list-section">

        <div class="list-toolbar">
          <span class="toolbar-status">
            {{ unreadCount > 0 ? `当前仍有 ${unreadCount} 条 Pending 简报` : '所有系统简报均已查阅' }}
          </span>
          <button
              v-if="unreadCount > 0"
              class="action-link action-link--primary"
              type="button"
              :disabled="processing"
              @click="markAllRead"
          >
            {{ processing ? '处理中...' : '将全部标为已阅' }} <span class="arrow">→</span>
          </button>
        </div>

        <div class="dispatch-stream">
          <article
              v-for="notification in pagedNotifications"
              :key="notification.notificationId"
              class="dispatch-row"
              :class="{ 'is-unread': !notification.read }"
          >
            <div class="unread-indicator" aria-hidden="true"></div>

            <div class="row-left">
              <span class="huge-date">{{ formatDatePart(notification.createdAt) }}</span>
              <span class="time-stamp">{{ formatTimePart(notification.createdAt) }}</span>
            </div>

            <div class="row-center">
              <div class="center-topline">
                <span class="serial-no">#{{ notification.notificationId }}</span>
                <h3 class="subject-title">{{ notification.title }}</h3>
              </div>
              <p class="subject-content">{{ notification.contentText }}</p>
            </div>

            <div class="row-right">
              <div v-if="notification.read" class="read-status">
                <span class="status-label">已归档</span>
                <span class="status-time">{{ formatFullDate(notification.readAt) }}</span>
              </div>
              <button
                  v-else
                  class="action-link"
                  type="button"
                  :disabled="processing"
                  @click="markRead(notification.notificationId)"
              >
                标记查阅 <span class="arrow">→</span>
              </button>
            </div>
          </article>
        </div>

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
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700;800&family=Noto+Serif+SC:wght@500;600;700&display=swap');

/* 全局极简白纸底色 */
.editorial-dispatch-page {
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
.dispatch-header {
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

.stat-value.highlight {
  color: #5c6b60;
}

.stat-text {
  font-family: 'Manrope', sans-serif;
  font-size: 1.4rem;
  font-weight: 600;
  color: #2a362e;
  margin-top: 0.5rem;
}

/* 列表控制栏 */
.list-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
  padding: 0 1rem;
}

.toolbar-status {
  font-family: 'Noto Serif SC', serif;
  font-size: 1.05rem;
  font-weight: 600;
  color: #1e2821;
}

.action-link {
  background: transparent;
  border: none;
  font-family: 'Noto Serif SC', serif;
  font-size: 1rem;
  font-weight: 600;
  color: #5c6b60;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0;
  transition: color 0.3s ease;
}

.action-link:hover:not(:disabled) {
  color: #1e2821;
}

.action-link--primary {
  color: #2a362e;
}

.action-link:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 信件流行排版 */
.dispatch-stream {
  display: flex;
  flex-direction: column;
}

.dispatch-row {
  position: relative;
  display: grid;
  grid-template-columns: 140px minmax(0, 1fr) 140px;
  gap: 3rem;
  padding: 2.5rem 1.5rem;
  border-bottom: 1px solid rgba(42, 54, 46, 0.08);
  transition: background 0.4s ease;
}

.dispatch-row:hover {
  background: rgba(255, 255, 255, 0.6);
}

/* 已读状态的褪色效果 (营造归档感) */
.dispatch-row:not(.is-unread) {
  opacity: 0.7;
}
.dispatch-row:not(.is-unread):hover {
  opacity: 1;
}

/* 未读状态锚点修饰线 */
.unread-indicator {
  position: absolute;
  left: 0;
  top: 2.5rem;
  bottom: 2.5rem;
  width: 3px;
  background: transparent;
  transition: background 0.3s ease;
}

.is-unread .unread-indicator {
  background: #2a362e;
}

/* 左侧：巨型日期 */
.row-left {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.huge-date {
  font-family: 'Manrope', sans-serif;
  font-size: 2.2rem;
  font-weight: 800;
  letter-spacing: -0.02em;
  color: #8a9c90;
  line-height: 1;
  transition: color 0.3s ease;
}

.is-unread .huge-date {
  color: #2a362e;
}

.time-stamp {
  font-family: 'Manrope', sans-serif;
  font-size: 1rem;
  color: #a3b0a7;
  font-weight: 500;
}

.is-unread .time-stamp {
  color: #5c6b60;
}

/* 中间：正文内容 */
.row-center {
  display: flex;
  flex-direction: column;
  gap: 0.8rem;
}

.center-topline {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.serial-no {
  font-family: 'Manrope', sans-serif;
  font-size: 0.85rem;
  color: #b5c2b9;
}

.subject-title {
  font-family: 'Noto Serif SC', serif;
  font-size: 1.3rem;
  font-weight: 500;
  color: #5c6b60;
  margin: 0;
  transition: color 0.3s ease;
}

.is-unread .subject-title {
  font-weight: 600;
  color: #1e2821;
}

.subject-content {
  font-family: 'Noto Serif SC', serif;
  font-size: 1.05rem;
  line-height: 1.8;
  color: #7b8c80;
  margin: 0;
  transition: color 0.3s ease;
}

.is-unread .subject-content {
  color: #4a5c51;
}

/* 右侧：动作与状态 */
.row-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: flex-start;
  padding-top: 0.2rem;
}

.read-status {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0.4rem;
}

.status-label {
  font-family: 'Noto Serif SC', serif;
  font-size: 0.85rem;
  font-weight: 600;
  color: #8a9c90;
  border: 1px solid rgba(130, 150, 138, 0.3);
  padding: 0.2rem 0.8rem;
  border-radius: 4px;
}

.status-time {
  font-family: 'Manrope', sans-serif;
  font-size: 0.8rem;
  color: #b5c2b9;
  text-align: right;
}

/* 状态提示 */
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

/* 交互动画 */
.arrow {
  font-family: 'Manrope', sans-serif;
  transition: transform 0.3s ease;
}

.action-link:hover:not(:disabled) .arrow,
.page-btn:hover:not(:disabled) .arrow:last-child {
  transform: translateX(4px);
}
.page-btn:hover:not(:disabled) .arrow:first-child {
  transform: translateX(-4px);
}

/* 响应式 */
@media (max-width: 1024px) {
  .dispatch-row {
    grid-template-columns: 100px minmax(0, 1fr) 100px;
    gap: 2rem;
  }

  .huge-date {
    font-size: 1.8rem;
  }
}

@media (max-width: 768px) {
  .dispatch-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 2rem;
  }

  .header-stats {
    flex-wrap: wrap;
    gap: 2rem;
  }

  .dispatch-row {
    grid-template-columns: 1fr;
    gap: 1rem;
    padding: 2rem 1.5rem;
  }

  .unread-indicator {
    top: 2rem;
    bottom: 2rem;
  }

  .row-left {
    flex-direction: row;
    align-items: baseline;
    gap: 0.8rem;
  }

  .huge-date {
    font-size: 1.5rem;
  }

  .row-right {
    align-items: flex-start;
    margin-top: 1rem;
  }

  .read-status {
    align-items: flex-start;
    flex-direction: row;
    align-items: center;
  }
}
</style>