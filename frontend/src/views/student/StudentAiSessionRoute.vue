<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { fetchStudentAiSessionMessagesApi, fetchStudentAiSessionsApi, sendStudentAiChatMessageApi } from '@/api/ai-chat'
import type { AiChatMessage, AiChatSession } from '@/api/types'
import { toErrorMessage, toNumberParam } from '@/views/shared/page-logic'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const sending = ref(false)
const errorMessage = ref('')
const messages = ref<AiChatMessage[]>([])
const sessions = ref<AiChatSession[]>([])
const draft = ref('')
const messageViewport = ref<HTMLElement | null>(null)

const sessionId = computed(() => toNumberParam(route.params.sessionId))
const activeSession = computed(() => sessions.value.find((item) => item.sessionId === sessionId.value) ?? null)

function resolveSessionStatusText(status: string | null | undefined): string {
  switch (status) {
    case 'ACTIVE': return '进行中'
    case 'ARCHIVED': return '已归档'
    case 'CLOSED': return '已结束'
    default: return '进行中'
  }
}

function formatDateTime(value: string | null | undefined): string {
  if (!value) return '暂无'
  const d = new Date(value)
  return `${String(d.getMonth() + 1).padStart(2, '0')}/${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

async function scrollToBottom(smooth = true): Promise<void> {
  await nextTick()
  if (!messageViewport.value) return
  messageViewport.value.scrollTo({
    top: messageViewport.value.scrollHeight,
    behavior: smooth ? 'smooth' : 'auto'
  })
}

async function loadSession(): Promise<void> {
  if (!sessionId.value) {
    errorMessage.value = '会话编号无效'
    messages.value = []
    sessions.value = []
    return
  }

  loading.value = true
  errorMessage.value = ''

  try {
    const [sessionList, messageList] = await Promise.all([
      fetchStudentAiSessionsApi(),
      fetchStudentAiSessionMessagesApi(sessionId.value)
    ])
    sessions.value = sessionList
    messages.value = messageList
    await scrollToBottom(false)
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    loading.value = false
  }
}

async function goBack(): Promise<void> {
  await router.push({ name: 'student-ai-sessions' })
}

async function sendMessage(): Promise<void> {
  if (!sessionId.value || sending.value) return

  const content = draft.value.trim()
  if (!content) {
    ElMessage.warning('请输入想和 AI 导师说的话')
    return
  }

  sending.value = true
  errorMessage.value = ''

  try {
    const response = await sendStudentAiChatMessageApi(sessionId.value, { content })
    draft.value = ''
    messages.value = [...messages.value, response.studentMessage, response.aiMessage]
    sessions.value = sessions.value.map((item) =>
        item.sessionId === sessionId.value
            ? {
              ...item,
              summaryText: response.studentMessage.content.slice(0, 80),
              lastActiveAt: response.aiMessage.createdAt
            }
            : item
    )
    await scrollToBottom()
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    sending.value = false
  }
}

function handleComposerKeydown(event: KeyboardEvent): void {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    void sendMessage()
  }
}

watch(() => route.params.sessionId, () => {
  void loadSession()
})

watch(messages, async () => {
  if (messages.value.length) {
    await scrollToBottom()
  }
})

onMounted(() => {
  void loadSession()
})
</script>

<template>
  <main class="split-editorial-page">
    <div class="page-container">

      <aside class="editorial-sidebar">
        <div class="sidebar-sticky">

          <nav class="dossier-nav">
            <button class="ghost-link" type="button" @click="goBack">
              <span class="arrow">←</span> 封存并返回列表
            </button>
          </nav>

          <header class="side-header">
            <span class="eyebrow">Mindful Journal</span>
            <h1 class="side-title">{{ activeSession?.title || `倾诉会话 #${sessionId || '-'}` }}</h1>
            <p class="side-lead">
              这里不是任务面板，而是一个可以慢下来整理自己状态的地方。不需要有“秒回”的压力。
            </p>
          </header>

          <div class="thick-accent-line"></div>

          <div class="side-tips">
            <h3 class="tips-title">对话引导</h3>
            <ul class="tips-list">
              <li>你可以说具体的事件，也可以只描述模糊的感受。</li>
              <li>如果不想说完整故事，只写一小段也可以。</li>
              <li>AI 导师会在这里安静地倾听。</li>
            </ul>
          </div>

          <div class="side-meta">
            <dl class="meta-grid">
              <div>
                <dt>会话编号</dt>
                <dd>#{{ sessionId || '-' }}</dd>
              </div>
              <div>
                <dt>开启时间</dt>
                <dd>{{ formatDateTime(activeSession?.createdAt) }}</dd>
              </div>
              <div>
                <dt>当前状态</dt>
                <dd>{{ resolveSessionStatusText(activeSession?.status) }}</dd>
              </div>
            </dl>
          </div>

        </div>
      </aside>

      <section class="editorial-chat-area">

        <div class="connection-status">
          <div class="status-indicator"></div>
          <span class="status-text">DeepSeek 认知模型已接入并准备倾听</span>
        </div>

        <div v-if="errorMessage" class="error-banner">{{ errorMessage }}</div>

        <div class="transcript-wrapper" ref="messageViewport">

          <div v-if="loading" class="loading-state">
            <div class="spinner"></div>
            <p>正在展卷记录...</p>
          </div>

          <div v-else-if="!messages.length" class="empty-state">
            <p class="empty-desc">当前还没有记录。<br>试着写下此刻最想被看见的一句话，AI 导师会在这里回应你。</p>
          </div>

          <div v-else class="transcript-stream">
            <article
                v-for="message in messages"
                :key="message.messageId"
                class="transcript-row"
                :class="message.senderType === 'STUDENT' ? 'is-student' : 'is-ai'"
            >
              <div class="row-actor">
                <span class="actor-name">{{ message.senderType === 'STUDENT' ? 'YOU' : 'AI MENTOR' }}</span>
                <span class="actor-time">{{ formatDateTime(message.createdAt) }}</span>
              </div>

              <div class="row-content">
                <p class="message-content">{{ message.content }}</p>
              </div>
            </article>
          </div>
        </div>

        <footer class="composer-desk">
          <label class="composer-label" for="student-ai-composer">记录此刻的感受...</label>
          <textarea
              id="student-ai-composer"
              v-model="draft"
              class="sleek-textarea"
              rows="3"
              maxlength="1000"
              placeholder="例如：我最近总觉得胸口发紧，晚上躺下后脑子停不下来..."
              @keydown="handleComposerKeydown"
          />
          <div class="composer-footer">
            <p class="composer-hint">
              Enter 直接发送，Shift + Enter 换行。
            </p>
            <button class="action-btn" type="button" :disabled="sending || !draft.trim()" @click="sendMessage">
              {{ sending ? '传递中...' : '发送' }} <span class="arrow">→</span>
            </button>
          </div>
        </footer>

      </section>

    </div>
  </main>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700;800&family=Noto+Serif+SC:wght@500;600;700&display=swap');

/* 全局极简纸张底色 */
.split-editorial-page {
  min-height: 100vh;
  background: #fcfbf9;
  color: #1e2821;
  font-family: 'Manrope', 'Noto Serif SC', sans-serif;
  padding: 4rem 2vw 4rem;
  box-sizing: border-box;
}

.page-container {
  max-width: 1200px;
  margin: 0 auto;
  display: grid;
  /* 严格的左右对半分栏，左侧稍微收紧，右半部分留给聊天 */
  grid-template-columns: 360px minmax(0, 1fr);
  gap: 6rem;
  align-items: start;
}

/* ================= 左侧静态控制台 ================= */
.editorial-sidebar {
  position: relative;
}

.sidebar-sticky {
  position: sticky;
  top: 4rem;
  display: flex;
  flex-direction: column;
  gap: 3rem;
}

.dossier-nav {
  margin-bottom: 1rem;
}

.ghost-link {
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

.ghost-link:hover {
  color: #1e2821;
}

.side-header {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.eyebrow {
  font-family: 'Manrope', sans-serif;
  font-size: 0.85rem;
  font-weight: 700;
  letter-spacing: 0.15em;
  color: #8a9c90;
  text-transform: uppercase;
}

/* 缩小的标题尺寸，克制且专业 */
.side-title {
  font-family: 'Noto Serif SC', serif;
  font-size: 2.2rem;
  font-weight: 600;
  color: #1e2821;
  margin: 0;
  line-height: 1.25;
  letter-spacing: 0.02em;
}

.side-lead {
  font-size: 1.05rem;
  color: #5c6b60;
  line-height: 1.8;
  margin: 0;
}

.thick-accent-line {
  width: 100%;
  height: 4px;
  background: #2a362e;
}

.tips-title {
  font-family: 'Noto Serif SC', serif;
  font-size: 1.15rem;
  font-weight: 600;
  color: #1e2821;
  margin: 0 0 1rem 0;
}

.tips-list {
  margin: 0;
  padding-left: 1.2rem;
  color: #5c6b60;
  font-size: 0.95rem;
  line-height: 1.8;
}

.tips-list li {
  margin-bottom: 0.5rem;
}

.meta-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1.5rem 1rem;
  margin: 0;
  padding-top: 1.5rem;
  border-top: 1px solid rgba(42, 54, 46, 0.1);
}

.meta-grid dt {
  font-family: 'Manrope', sans-serif;
  font-size: 0.8rem;
  font-weight: 700;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: #8a9c90;
  margin-bottom: 0.4rem;
}

.meta-grid dd {
  margin: 0;
  font-family: 'Noto Serif SC', serif;
  font-size: 1rem;
  color: #2a362e;
  font-weight: 600;
}

/* ================= 右半部分会话界面 ================= */
.editorial-chat-area {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 8rem); /* 让右侧区域适应屏幕高度 */
}

/* 呼吸灯状态 */
.connection-status {
  display: inline-flex;
  align-items: center;
  gap: 0.8rem;
  margin-bottom: 2rem;
  padding: 0.6rem 1.2rem;
  border-radius: 100px;
  background: rgba(255, 255, 255, 0.6);
  border: 1px solid rgba(42, 54, 46, 0.08);
  align-self: flex-start;
  flex-shrink: 0;
}

.status-indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #5c8c6b;
  animation: pulse-green 2s infinite;
}

@keyframes pulse-green {
  0% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(92, 140, 107, 0.4); }
  70% { transform: scale(1); box-shadow: 0 0 0 6px rgba(92, 140, 107, 0); }
  100% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(92, 140, 107, 0); }
}

.status-text {
  font-family: 'Noto Serif SC', serif;
  font-size: 0.85rem;
  font-weight: 600;
  color: #5c6b60;
}

/* 无框聊天流 */
.transcript-wrapper {
  flex: 1;
  overflow-y: auto;
  padding-right: 1.5rem;
  margin-bottom: 2rem;
  border-bottom: 1px solid rgba(42, 54, 46, 0.1);
}

.transcript-wrapper::-webkit-scrollbar {
  width: 4px;
}
.transcript-wrapper::-webkit-scrollbar-thumb {
  background: rgba(42, 54, 46, 0.15);
  border-radius: 4px;
}

.transcript-stream {
  display: flex;
  flex-direction: column;
}

.transcript-row {
  display: grid;
  grid-template-columns: 80px minmax(0, 1fr);
  gap: 2rem;
  padding: 2rem 0;
  border-bottom: 1px dashed rgba(42, 54, 46, 0.06);
  align-items: start;
}

.transcript-row:last-child {
  border-bottom: none;
}

/* 角色与时间 */
.row-actor {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
  text-align: right;
  padding-top: 0.4rem;
}

.actor-name {
  font-family: 'Manrope', sans-serif;
  font-size: 0.85rem;
  font-weight: 800;
  letter-spacing: 0.1em;
  color: #5c7062; /* AI导师的颜色 */
}

.is-student .actor-name {
  color: #8c6a5c; /* 学生自身的颜色 */
}

.actor-time {
  font-family: 'Manrope', sans-serif;
  font-size: 0.8rem;
  color: #b5c2b9;
}

/* 消息正文 */
.row-content {
  display: flex;
  flex-direction: column;
}

.message-content {
  font-family: 'Noto Serif SC', serif;
  font-size: 1.1rem;
  line-height: 1.9;
  color: #1e2821;
  margin: 0;
  white-space: pre-wrap;
}

.is-student .message-content {
  color: #4a5c51;
}

/* 沉浸式书写台 */
.composer-desk {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  flex-shrink: 0;
}

.composer-label {
  font-family: 'Noto Serif SC', serif;
  font-size: 0.95rem;
  font-weight: 600;
  color: #8a9c90;
}

.sleek-textarea {
  width: 100%;
  border: none;
  border-bottom: 1px dashed rgba(42, 54, 46, 0.2);
  background: transparent;
  padding: 0.5rem 0;
  font-family: 'Noto Serif SC', serif;
  font-size: 1.1rem;
  line-height: 1.8;
  color: #1e2821;
  resize: none;
  outline: none;
  transition: border-color 0.3s ease;
}

.sleek-textarea::placeholder {
  color: #b5c2b9;
  font-style: italic;
}

.sleek-textarea:focus {
  border-bottom-color: #2a362e;
  border-bottom-style: solid;
}

.composer-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 0.5rem;
}

.composer-hint {
  font-family: 'Manrope', sans-serif;
  font-size: 0.85rem;
  color: #a3b0a7;
  margin: 0;
}

.action-btn {
  background: transparent;
  border: 1px solid rgba(42, 54, 46, 0.3);
  color: #2a362e;
  padding: 0.8rem 1.8rem;
  border-radius: 100px;
  font-family: 'Noto Serif SC', serif;
  font-size: 0.95rem;
  font-weight: 600;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 0.6rem;
  transition: all 0.3s ease;
}

.action-btn:hover:not(:disabled) {
  background: #2a362e;
  color: #ffffff;
  border-color: #2a362e;
  box-shadow: 0 8px 16px rgba(42, 54, 46, 0.15);
}

.action-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
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
  padding: 4rem 0;
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

.empty-desc {
  line-height: 1.8;
}

/* 交互动画 */
.arrow {
  font-family: 'Manrope', sans-serif;
  transition: transform 0.3s ease;
}

.ghost-link:hover .arrow {
  transform: translateX(-4px);
}

.action-btn:hover:not(:disabled) .arrow {
  transform: translateX(4px);
}

/* 响应式 */
@media (max-width: 900px) {
  .page-container {
    grid-template-columns: 1fr;
    gap: 3rem;
  }

  .sidebar-sticky {
    position: relative;
    top: 0;
    gap: 2rem;
  }

  .editorial-chat-area {
    height: 70vh; /* 移动端给予一定的独立滚动区域 */
  }
}

@media (max-width: 600px) {
  .transcript-row {
    grid-template-columns: 1fr;
    gap: 0.5rem;
    padding: 1.5rem 0;
  }

  .row-actor {
    flex-direction: row;
    align-items: baseline;
    justify-content: flex-start;
    gap: 0.8rem;
    text-align: left;
    padding: 0;
  }

  .composer-footer {
    flex-direction: column;
    align-items: flex-start;
    gap: 1.5rem;
  }

  .action-btn {
    width: 100%;
    justify-content: center;
  }
}
</style>