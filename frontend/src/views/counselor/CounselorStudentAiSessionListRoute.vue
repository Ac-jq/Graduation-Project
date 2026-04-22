<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchCounselorStudentAiSessionMessagesApi, fetchCounselorStudentAiSessionsApi } from '@/api/ai-chat'
import type { AiChatMessage, AiChatSession } from '@/api/types'
import { toErrorMessage, toNumberParam } from '@/views/shared/page-logic'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const messagesLoading = ref(false)
const errorMessage = ref('')
const sessions = ref<AiChatSession[]>([])
const messages = ref<AiChatMessage[]>([])
const selectedSessionId = ref<number | null>(null)
const studentUserId = computed(() => toNumberParam(route.params.studentUserId))

// 分页状态
const currentPage = ref(1)
const pageSize = 6

const totalPages = computed(() => Math.max(1, Math.ceil(sessions.value.length / pageSize)))
const pagedSessions = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return sessions.value.slice(start, start + pageSize)
})

const alertCount = computed(() => sessions.value.filter(s => Boolean(s.riskFlag)).length)
const selectedSession = computed(() => sessions.value.find((session) => session.sessionId === selectedSessionId.value) ?? null)
const selectedAlertCount = computed(() => messages.value.filter((message) => Boolean(message.hitKeywords)).length)

// 日期格式化
function getDayMonth(value: string | Date): string {
  const d = new Date(value)
  return `${String(d.getMonth() + 1).padStart(2, '0')}/${String(d.getDate()).padStart(2, '0')}`
}

function getTime(value: string | Date): string {
  const d = new Date(value)
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

function formatFullDate(value: string | Date): string {
  const d = new Date(value)
  return `${d.getFullYear()}/${String(d.getMonth() + 1).padStart(2, '0')}/${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

function formatMessageTime(value: string | Date | null | undefined): string {
  if (!value) return '暂无记录'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return '暂无记录'
  return `${String(d.getMonth() + 1).padStart(2, '0')}/${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

function resolveSessionStatus(status: string | null | undefined): string {
  if (status === 'ARCHIVED') return '已归档'
  if (status === 'ACTIVE') return '进行中'
  return status || '未知'
}

function resolveRiskLevel(level: string | null | undefined): string {
  if (level === 'HIGH') return '高关注'
  if (level === 'MEDIUM') return '中等关注'
  return '常规'
}

async function loadSessions(): Promise<void> {
  if (!studentUserId.value) {
    errorMessage.value = '无法定位到该学生档案'
    sessions.value = []
    return
  }

  loading.value = true
  errorMessage.value = ''

  try {
    sessions.value = await fetchCounselorStudentAiSessionsApi(studentUserId.value)
    selectedSessionId.value = sessions.value[0]?.sessionId ?? null
    if (selectedSessionId.value) {
      await loadMessages(selectedSessionId.value)
    } else {
      messages.value = []
    }
    currentPage.value = 1
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    loading.value = false
  }
}

async function loadMessages(sessionId: number): Promise<void> {
  if (!studentUserId.value) return

  messagesLoading.value = true
  errorMessage.value = ''
  try {
    messages.value = await fetchCounselorStudentAiSessionMessagesApi(studentUserId.value, sessionId)
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    messagesLoading.value = false
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

async function openSession(sessionId: number): Promise<void> {
  selectedSessionId.value = sessionId
  await loadMessages(sessionId)
}

function goBack(): void {
  router.push({ name: 'counselor-students' })
}

watch(() => route.params.studentUserId, () => {
  void loadSessions()
})

onMounted(() => {
  void loadSessions()
})
</script>

<template>
  <main class="editorial-archive-page">
    <div class="page-container">

      <nav class="dossier-nav">
        <button class="nav-ghost-btn" @click="goBack">
          <span class="arrow">←</span> 返回来访者名册
        </button>
      </nav>

      <header class="archive-header">
        <div class="header-main">
          <span class="header-tag">AI Interview Archive</span>
          <h1 class="huge-title">AI 访谈案卷</h1>
          <p class="header-lead">
            当前正在查阅学生 <strong>#{{ studentUserId || '-' }}</strong> 与 AI 导师的历史会话记录。请仔细审阅这些包含高关注标记的线索，作为线下沟通的辅助参考。
          </p>
        </div>

        <div class="header-stats">
          <div class="stat-item">
            <span class="stat-label">会话总计</span>
            <span class="stat-value">{{ loading ? '-' : sessions.length }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">高关注标记</span>
            <span class="stat-value highlight">{{ loading ? '-' : alertCount }}</span>
          </div>
        </div>
      </header>

      <div v-if="errorMessage" class="error-banner">{{ errorMessage }}</div>

      <div v-if="loading" class="loading-state">
        <div class="spinner"></div>
        <p>正在翻阅案卷记录...</p>
      </div>

      <div v-else-if="!sessions.length" class="empty-state">
        <h2 class="empty-title">卷宗尚为空白</h2>
        <p class="empty-desc">该学生尚未与 AI 导师进行过任何会话。</p>
      </div>

      <section v-else class="archive-list-section">

        <div class="list-toolbar">
          <span class="toolbar-status">当前显示第 {{ currentPage }} 页，共 {{ totalPages }} 页</span>
        </div>

        <div class="archive-workspace">
          <aside class="session-history-panel">
            <div class="archive-stream">
              <article
                  v-for="session in pagedSessions"
                  :key="session.sessionId"
                  class="archive-row"
                  :class="{ 'row--alert': Boolean(session.riskFlag), 'is-selected': selectedSessionId === session.sessionId }"
                  @click="openSession(session.sessionId)"
              >
                <div class="row-time-col">
                  <span class="huge-date">{{ getDayMonth(session.createdAt) }}</span>
                  <span class="time-stamp">{{ getTime(session.createdAt) }}</span>
                  <span v-if="session.riskFlag" class="risk-badge">重点关注</span>
                </div>

                <div class="row-content-col">
                  <div class="content-topline">
                    <span class="session-id">Session #{{ session.sessionId }}</span>
                    <h3 class="session-title">{{ session.title || '未命名会话' }}</h3>
                  </div>

                  <blockquote class="session-quote">
                    “{{ session.summaryText || '暂无摘要，请在右侧查看完整对话。' }}”
                  </blockquote>

                  <div class="session-meta">
                    <span class="meta-item">{{ resolveRiskLevel(session.riskLevel) }}</span>
                    <span class="dot">·</span>
                    <span class="meta-item">{{ resolveSessionStatus(session.status) }}</span>
                  </div>
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
          </aside>

          <article class="transcript-detail-panel">
            <header class="detail-panel-header">
              <span class="session-id">Session #{{ selectedSession?.sessionId || '-' }}</span>
              <h2>{{ selectedSession?.title || '请选择左侧会话' }}</h2>
              <p>
                {{ selectedSession ? `共 ${messages.length} 条发言，触发关注 ${selectedAlertCount} 次` : '点击左侧归档会话后，在这里查看完整聊天实录。' }}
              </p>
            </header>

            <div v-if="messagesLoading" class="detail-loading">
              <div class="spinner"></div>
              <p>正在同步聊天实录...</p>
            </div>

            <div v-else-if="!selectedSession" class="detail-empty">
              <p>暂无选中的会话。</p>
            </div>

            <div v-else-if="!messages.length" class="detail-empty">
              <p>该会话中尚未产生发言记录。</p>
            </div>

            <div v-else class="detail-transcript">
              <section
                v-for="message in messages"
                :key="message.messageId"
                class="detail-message"
                :class="{ 'is-student': message.senderType === 'STUDENT' }"
              >
                <div class="message-meta">
                  <strong>{{ message.senderType === 'STUDENT' ? '来访学生' : 'AI 导师' }}</strong>
                  <span>{{ formatMessageTime(message.createdAt) }}</span>
                </div>
                <p>{{ message.content }}</p>
                <span v-if="message.hitKeywords" class="keyword-pill">触发关键词 {{ message.hitKeywords }}</span>
              </section>
            </div>
          </article>
        </div>

      </section>
    </div>
  </main>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700;800&family=Noto+Serif+SC:wght@500;600;700&display=swap');

/* 全局极简白纸底色 */
.editorial-archive-page {
  min-height: 100vh;
  background: #fcfbf9;
  color: #1e2821;
  font-family: 'Manrope', 'Noto Serif SC', sans-serif;
  padding: 2rem 2vw 8rem;
  box-sizing: border-box;
}

.page-container {
  max-width: 1280px;
  margin: 0 auto;
}

/* 顶部导航 */
.dossier-nav {
  margin-bottom: 3rem;
}

.nav-ghost-btn {
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

.nav-ghost-btn:hover {
  color: #1e2821;
}

/* 头部排版 */
.archive-header {
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
  font-size: 2.8rem;
  font-weight: 600;
  color: #1e2821;
  margin: 0 0 1.2rem 0;
  letter-spacing: 0.05em;
}

.header-lead {
  font-size: 1.05rem;
  color: #6a7c70;
  line-height: 1.8;
  margin: 0;
}

.header-lead strong {
  color: #2a362e;
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

/* 控制栏 */
.list-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
  padding: 0 0.5rem;
}

.toolbar-status {
  font-family: 'Noto Serif SC', serif;
  font-size: 0.95rem;
  color: #8a9c90;
}

.archive-workspace {
  display: grid;
  grid-template-columns: minmax(320px, 0.82fr) minmax(0, 1.18fr);
  gap: 3rem;
  align-items: start;
}

.session-history-panel {
  min-width: 0;
}

.transcript-detail-panel {
  position: sticky;
  top: 2rem;
  max-height: calc(100vh - 4rem);
  overflow: hidden;
  border-radius: 28px;
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.82), rgba(248, 246, 242, 0.9));
  box-shadow: 0 32px 72px rgba(54, 66, 58, 0.08);
  display: flex;
  flex-direction: column;
}

.detail-panel-header {
  padding: 2rem 2rem 1.4rem;
  border-bottom: 1px solid rgba(42, 54, 46, 0.07);
}

.detail-panel-header h2 {
  font-family: 'Noto Serif SC', serif;
  font-size: clamp(1.5rem, 2.4vw, 2.2rem);
  font-weight: 600;
  color: #1e2821;
  margin: 0.5rem 0 0.8rem;
}

.detail-panel-header p {
  margin: 0;
  color: #7b8c80;
  line-height: 1.7;
}

.detail-transcript {
  overflow-y: auto;
  padding: 0.5rem 2rem 2rem;
}

.detail-transcript::-webkit-scrollbar {
  width: 5px;
}

.detail-transcript::-webkit-scrollbar-thumb {
  background: rgba(138, 156, 144, 0.28);
  border-radius: 999px;
}

.detail-message {
  padding: 1.55rem 0;
  border-bottom: 1px solid rgba(42, 54, 46, 0.06);
}

.message-meta {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 0.8rem;
  color: #8a9c90;
  font-family: 'Manrope', sans-serif;
  font-size: 0.85rem;
}

.message-meta strong {
  color: #5c7062;
  font-family: 'Noto Serif SC', serif;
  font-size: 0.98rem;
}

.detail-message.is-student .message-meta strong {
  color: #8c6a5c;
}

.detail-message p {
  white-space: pre-wrap;
  margin: 0;
  color: #1e2821;
  font-family: 'Noto Serif SC', serif;
  font-size: 1.04rem;
  line-height: 1.85;
}

.detail-loading,
.detail-empty {
  padding: 5rem 2rem;
  text-align: center;
  color: #7b8c80;
}

.keyword-pill {
  display: inline-flex;
  margin-top: 1rem;
  padding: 0.35rem 0.75rem;
  border-radius: 999px;
  background: rgba(193, 150, 83, 0.1);
  color: #9e7330;
  font-family: 'Noto Serif SC', serif;
  font-size: 0.84rem;
  font-weight: 600;
}

/* 会话流行排版 */
.archive-stream {
  display: flex;
  flex-direction: column;
}

.archive-row {
  display: grid;
  grid-template-columns: 112px minmax(0, 1fr);
  gap: 1.4rem;
  padding: 1.6rem 1rem;
  border-bottom: 1px solid rgba(42, 54, 46, 0.08);
  cursor: pointer;
  border-radius: 18px;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.archive-row:hover {
  background: rgba(255, 255, 255, 0.6);
}

.archive-row.is-selected {
  background: rgba(255, 255, 255, 0.82);
  box-shadow: 0 18px 44px rgba(54, 66, 58, 0.07);
}

/* 左侧：巨幕时间 */
.row-time-col {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.huge-date {
  font-family: 'Manrope', sans-serif;
  font-size: 2.6rem;
  font-weight: 800;
  letter-spacing: -0.04em;
  color: #2a362e;
  line-height: 1;
  margin-bottom: 0.2rem;
  transition: color 0.3s ease;
}

.time-stamp {
  font-family: 'Manrope', sans-serif;
  font-size: 1rem;
  color: #8a9c90;
  font-weight: 500;
}

.risk-badge {
  display: inline-flex;
  align-self: flex-start;
  margin-top: 1.2rem;
  font-family: 'Noto Serif SC', serif;
  font-size: 0.85rem;
  font-weight: 600;
  padding: 0.3rem 0.8rem;
  border-radius: 6px;
  background: rgba(140, 74, 74, 0.08);
  color: #8c4a4a;
  border: 1px solid rgba(140, 74, 74, 0.2);
}

.row--alert .huge-date {
  color: #8c4a4a;
}

/* 中间：正文摘要 */
.row-content-col {
  display: flex;
  flex-direction: column;
}

.content-topline {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
  margin-bottom: 1.5rem;
}

.session-id {
  font-family: 'Manrope', sans-serif;
  font-size: 0.85rem;
  color: #8a9c90;
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

.session-title {
  font-family: 'Noto Serif SC', serif;
  font-size: 1.45rem;
  font-weight: 600;
  color: #1e2821;
  margin: 0;
  transition: color 0.3s ease;
}

.archive-row:hover .session-title {
  color: #5c6b60;
}

/* 杂志风引言摘要 */
.session-quote {
  margin: 0 0 1.4rem 0;
  padding-left: 1.5rem;
  border-left: 3px solid rgba(42, 54, 46, 0.15);
  font-size: 1.05rem;
  line-height: 1.8;
  color: #5c6b60;
}

.row--alert .session-quote {
  border-left-color: rgba(140, 74, 74, 0.3);
  color: #7a5c5c;
}

.session-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  margin-top: auto;
  font-family: 'Noto Serif SC', serif;
  font-size: 0.9rem;
  color: #8a9c90;
}

.dot {
  margin: 0 0.6rem;
  color: #cbd5cf;
}

/* 右侧：动作按钮 */
.row-action-col {
  display: none;
  align-items: center;
}

.action-link {
  background: transparent;
  border: none;
  font-family: 'Noto Serif SC', serif;
  font-size: 1.05rem;
  font-weight: 600;
  color: #5c6b60;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0;
  transition: color 0.3s ease;
}

.archive-row:hover .action-link {
  color: #1e2821;
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

.nav-ghost-btn:hover .arrow {
  transform: translateX(-4px);
}

.archive-row:hover .action-link .arrow,
.page-btn:hover:not(:disabled) .arrow:last-child {
  transform: translateX(4px);
}

.page-btn:hover:not(:disabled) .arrow:first-child {
  transform: translateX(-4px);
}

/* 响应式 */
@media (max-width: 900px) {
  .archive-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 2rem;
  }

  .archive-workspace {
    grid-template-columns: 1fr;
  }

  .transcript-detail-panel {
    position: static;
    max-height: none;
  }

  .header-stats {
    flex-wrap: wrap;
    gap: 2rem;
  }

  .archive-row {
    grid-template-columns: 1fr;
    gap: 1.5rem;
    padding: 2.5rem 0;
  }

  .row-time-col {
    flex-direction: row;
    align-items: center;
    flex-wrap: wrap;
    gap: 1rem;
  }

  .huge-date {
    font-size: 2.2rem;
    margin: 0;
  }

  .risk-badge {
    margin-top: 0;
  }

  .row-action-col {
    justify-content: flex-start;
    margin-top: 1rem;
  }
}
</style>
