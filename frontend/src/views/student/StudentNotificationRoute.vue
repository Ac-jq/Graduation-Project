<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { fetchNotificationsApi, markAllNotificationsReadApi, markNotificationReadApi } from '@/api/notification'
import type { NotificationItem } from '@/api/types'
import { toErrorMessage } from '@/views/shared/page-logic'

const loading = ref(false)
const processing = ref(false)
const errorMessage = ref('')
const notifications = ref<NotificationItem[]>([])

const unreadCount = computed(() => notifications.value.filter((notification) => !notification.read).length)
const latestNotification = computed(() => notifications.value[0] ?? null)

function formatDate(value: string | null): string {
  if (!value) {
    return '未记录'
  }

  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value))
}

async function loadNotifications(): Promise<void> {
  loading.value = true
  errorMessage.value = ''

  try {
    notifications.value = await fetchNotificationsApi()
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    loading.value = false
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
  <main class="notification-page">
    <section class="notification-page__masthead">
      <div class="notification-page__heading">
        <p class="notification-page__eyebrow">Notification Center</p>
        <h1 class="notification-page__title">Notification Center</h1>
        <p class="notification-page__summary">
          这里汇总系统推送给你的预约更新、测评结果提醒与聊天相关通知。已读动作会直接写回后端，不保留前端假状态。
        </p>
      </div>

      <aside class="notification-page__snapshot">
        <p class="notification-page__label">Inbox Snapshot</p>
        <dl>
          <div>
            <dt>Total</dt>
            <dd>{{ notifications.length }}</dd>
          </div>
          <div>
            <dt>Unread</dt>
            <dd>{{ unreadCount }}</dd>
          </div>
          <div>
            <dt>最新时间</dt>
            <dd>{{ latestNotification ? formatDate(latestNotification.createdAt) : '暂无通知' }}</dd>
          </div>
        </dl>
      </aside>
    </section>

    <section class="notification-page__toolbar">
      <div class="notification-page__toolbar-copy">
        <p>{{ unreadCount > 0 ? `当前还有 ${unreadCount} 条未读通知。` : '当前没有未读通知。' }}</p>
      </div>
      <button class="notification-page__primary" type="button" :disabled="processing || unreadCount === 0" @click="markAllRead">
        {{ processing ? '处理中...' : '全部标为已读' }}
      </button>
    </section>

    <p v-if="errorMessage" class="notification-page__alert">{{ errorMessage }}</p>

    <section v-if="loading" class="notification-page__status-panel">
      <p>正在加载通知...</p>
    </section>

    <section v-else-if="notifications.length" class="notification-page__list">
      <article
        v-for="notification in notifications"
        :key="notification.notificationId"
        class="notification-card"
        :class="{ 'notification-card--unread': !notification.read }"
      >
        <header class="notification-card__header">
          <div>
            <p class="notification-card__serial">Notification #{{ notification.notificationId }}</p>
            <h2>{{ notification.title }}</h2>
          </div>
          <span class="notification-card__state">
            {{ notification.read ? '已读' : '未读' }}
          </span>
        </header>

        <p class="notification-card__content">{{ notification.contentText }}</p>

        <footer class="notification-card__footer">
          <div class="notification-card__time-block">
            <p>创建时间：{{ formatDate(notification.createdAt) }}</p>
            <p>已读时间：{{ notification.read ? formatDate(notification.readAt) : '尚未阅读' }}</p>
          </div>
          <button
            v-if="!notification.read"
            class="notification-page__ghost"
            type="button"
            :disabled="processing"
            @click="markRead(notification.notificationId)"
          >
            标记为已读
          </button>
        </footer>
      </article>
    </section>

    <section v-else class="notification-page__status-panel">
      <p>当前没有通知记录。</p>
    </section>
  </main>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700&family=Noto+Serif+SC:wght@400;500;600;700&display=swap');

.notification-page {
  --paper: #f4efe5;
  --ink: #201c18;
  --muted: #6e665f;
  --line: rgba(32, 28, 24, 0.12);
  --glass: rgba(255, 251, 245, 0.68);
  --accent: #6a8372;
  min-height: 100vh;
  padding: 2rem;
  color: var(--ink);
  background:
    radial-gradient(circle at top right, rgba(114, 136, 121, 0.18), transparent 26%),
    radial-gradient(circle at left center, rgba(198, 186, 168, 0.22), transparent 30%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.14), transparent 38%),
    var(--paper);
}

.notification-page__masthead {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(300px, 0.85fr);
  gap: 1.5rem;
  align-items: end;
  padding-bottom: 1.4rem;
  border-bottom: 1px solid var(--line);
}

.notification-page__eyebrow,
.notification-page__label,
.notification-page__snapshot dt,
.notification-card__serial,
.notification-card__state {
  margin: 0;
  font: 600 0.72rem/1.4 'Manrope', sans-serif;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--muted);
}

.notification-page__title {
  margin: 0.95rem 0 0;
  font: 600 clamp(2.8rem, 5vw, 5.1rem)/0.98 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.notification-page__summary {
  max-width: 46rem;
  margin: 1rem 0 0;
  color: var(--muted);
  font: 400 1rem/1.9 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.notification-page__snapshot,
.notification-page__toolbar,
.notification-card,
.notification-page__status-panel {
  border: 1px solid var(--line);
  background: var(--glass);
  backdrop-filter: blur(18px);
  box-shadow: 0 22px 48px rgba(80, 70, 58, 0.08);
}

.notification-page__snapshot {
  padding: 1.2rem;
}

.notification-page__snapshot dl {
  display: grid;
  gap: 0.9rem;
  margin: 1rem 0 0;
}

.notification-page__snapshot dd {
  margin: 0.35rem 0 0;
  font: 600 1.04rem/1.45 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.notification-page__toolbar {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
  margin-top: 1.5rem;
  padding: 1rem 1.2rem;
}

.notification-page__toolbar-copy p,
.notification-card__content,
.notification-card__time-block p,
.notification-page__status-panel p {
  margin: 0;
  color: var(--muted);
  font: 400 0.98rem/1.85 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.notification-page__primary,
.notification-page__ghost {
  min-height: 3rem;
  padding: 0 1.15rem;
  font: 600 0.84rem/1 'Manrope', sans-serif;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  cursor: pointer;
  transition: transform 180ms ease, border-color 180ms ease, box-shadow 180ms ease;
}

.notification-page__primary {
  border: none;
  background: linear-gradient(135deg, #6b8473, #4f6656);
  color: #faf6f0;
  box-shadow: 0 18px 36px rgba(79, 102, 86, 0.24);
}

.notification-page__ghost {
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.48);
  color: var(--ink);
}

.notification-page__primary:hover:not(:disabled),
.notification-page__ghost:hover:not(:disabled),
.notification-card:hover {
  transform: translateY(-2px);
}

.notification-page__primary:disabled,
.notification-page__ghost:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.notification-page__alert {
  margin: 1.25rem 0 0;
  color: #8d4747;
  font: 600 0.9rem/1.6 'Manrope', sans-serif;
}

.notification-page__list {
  display: grid;
  gap: 1rem;
  margin-top: 1.5rem;
}

.notification-card {
  display: grid;
  gap: 1rem;
  padding: 1.25rem;
  transition: transform 180ms ease, border-color 180ms ease, box-shadow 180ms ease;
}

.notification-card--unread {
  border-color: rgba(106, 131, 114, 0.34);
  background: linear-gradient(180deg, rgba(106, 131, 114, 0.1), rgba(255, 251, 245, 0.72));
}

.notification-card__header,
.notification-card__footer {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
}

.notification-card__header h2 {
  margin: 0.65rem 0 0;
  font: 600 1.45rem/1.34 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.notification-card__state {
  padding: 0.4rem 0.65rem;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.5);
}

.notification-card__footer {
  padding-top: 1rem;
  border-top: 1px solid var(--line);
}

.notification-card__time-block {
  display: grid;
  gap: 0.3rem;
}

.notification-page__status-panel {
  margin-top: 1.5rem;
  padding: 1.35rem;
}

@media (max-width: 900px) {
  .notification-page {
    padding: 1rem;
  }

  .notification-page__masthead,
  .notification-page__toolbar,
  .notification-card__header,
  .notification-card__footer {
    grid-template-columns: 1fr;
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>

