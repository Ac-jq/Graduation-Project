<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { buildConsultChatWebSocketUrl, fetchConsultChatMessagesApi, fetchConsultChatSessionApi } from '@/api/chat'
import type { ConsultChatMessage, ConsultChatSession, ConsultChatSocketPayload } from '@/api/types'
import { getToken } from '@/core/session'
import { toErrorMessage, toNumberParam } from '@/views/shared/page-logic'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const errorMessage = ref('')
const chatSession = ref<ConsultChatSession | null>(null)
const messages = ref<ConsultChatMessage[]>([])
const socketState = ref<'idle' | 'connecting' | 'connected' | 'closed'>('idle')
const composeForm = reactive({
  content: ''
})
const socketRef = ref<WebSocket | null>(null)
const appointmentId = computed(() => toNumberParam(route.params.appointmentId))

const socketStateText = computed(() => {
  switch (socketState.value) {
    case 'connected': return '实时通道已连接'
    case 'connecting': return '正在建立安全连接...'
    case 'closed': return '连接已断开'
    default: return '通道待命'
  }
})

function formatTime(value: string | Date): string {
  const d = new Date(value)
  return `${String(d.getMonth() + 1).padStart(2, '0')}/${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

function disconnectSocket(): void {
  socketRef.value?.close()
  socketRef.value = null
  socketState.value = 'closed'
}

function connectSocket(): void {
  if (!appointmentId.value) {
    errorMessage.value = '无效的预约编号'
    return
  }

  const token = getToken()
  if (!token) {
    errorMessage.value = '缺少身份凭证'
    return
  }

  disconnectSocket()
  socketState.value = 'connecting'
  const socket = new WebSocket(buildConsultChatWebSocketUrl(appointmentId.value, token))

  socket.onopen = () => {
    socketState.value = 'connected'
  }
  socket.onmessage = (event) => {
    try {
      const payload = JSON.parse(event.data) as ConsultChatSocketPayload
      if (payload.type === 'MESSAGE' && payload.message) {
        messages.value = [...messages.value, payload.message]
        scrollToBottom()
      }
      if (payload.type === 'ERROR' && payload.tip) {
        errorMessage.value = payload.tip
      }
    } catch (error) {
      errorMessage.value = toErrorMessage(error)
    }
  }
  socket.onclose = () => {
    socketState.value = 'closed'
  }
  socket.onerror = () => {
    errorMessage.value = 'WebSocket 异常'
    socketState.value = 'closed'
  }

  socketRef.value = socket
}

async function loadChatContext(): Promise<void> {
  if (!appointmentId.value) {
    errorMessage.value = '无效的预约编号'
    chatSession.value = null
    messages.value = []
    return
  }

  loading.value = true
  errorMessage.value = ''

  try {
    const [session, history] = await Promise.all([
      fetchConsultChatSessionApi(appointmentId.value),
      fetchConsultChatMessagesApi(appointmentId.value)
    ])
    chatSession.value = session
    messages.value = history
    connectSocket()
    scrollToBottom()
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    loading.value = false
  }
}

function sendMessage(): void {
  if (!composeForm.content || socketRef.value?.readyState !== WebSocket.OPEN) {
    return
  }

  socketRef.value.send(JSON.stringify({ content: composeForm.content }))
  composeForm.content = ''
}

function scrollToBottom() {
  setTimeout(() => {
    window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' })
  }, 100)
}

function goBack(): void {
  router.push({ name: 'counselor-appointments' })
}

watch(() => route.params.appointmentId, () => {
  void loadChatContext()
})

onMounted(() => {
  void loadChatContext()
})

onBeforeUnmount(() => {
  disconnectSocket()
})
</script>

<template>
  <main class="editorial-chat-page">
    <div class="page-container">

      <nav class="dossier-nav">
        <button class="nav-ghost-btn" @click="goBack">
          <span class="arrow">←</span> 返回接诊台账
        </button>
      </nav>

      <header class="transcript-header">
        <div class="header-main">
          <span class="header-tag">Consultation Transcript</span>
          <h1 class="huge-title">沟通实录</h1>
          <p class="header-lead">
            当前正在与 <strong>预约 #{{ appointmentId || '-' }}</strong> 的发起人进行私密会谈。所有的历史记录均已解密并按照时间轴展开。
          </p>
        </div>

        <div class="connection-status">
          <div class="status-indicator" :class="`is-${socketState}`"></div>
          <span class="status-text">{{ socketStateText }}</span>
        </div>
      </header>

      <div v-if="errorMessage" class="error-banner">{{ errorMessage }}</div>

      <section class="transcript-grid">

        <div class="transcript-stream-wrapper">
          <div class="section-head">
            <h2 class="section-title">记录详情</h2>
            <span class="section-subtitle">Message Log</span>
          </div>

          <div v-if="loading" class="loading-state">
            <div class="spinner"></div>
            <p>正在解密并同步上下文...</p>
          </div>

          <div v-else-if="!messages.length" class="empty-state">
            <p class="empty-desc">当前会谈室暂无任何发言记录，您可以作为咨询师首先发起问候。</p>
          </div>

          <div v-else class="transcript-stream">
            <article
                v-for="message in messages"
                :key="message.messageId"
                class="message-row"
                :class="{ 'is-counselor': message.senderType === 'COUNSELOR' }"
            >
              <div class="message-actor">
                {{ message.senderType === 'COUNSELOR' ? 'YOU' : 'CLIENT' }}
              </div>
              <div class="message-body">
                <span class="message-time">{{ formatTime(message.createdAt) }}</span>
                <p class="message-content">{{ message.content }}</p>
              </div>
            </article>
          </div>
        </div>

        <aside class="compose-desk">
          <div class="desk-sticky-container">

            <div class="session-meta-panel">
              <h3 class="meta-heading">会话控制台</h3>
              <dl class="meta-list">
                <div>
                  <dt>实录编号</dt>
                  <dd>#{{ chatSession?.chatSessionId || '待分配' }}</dd>
                </div>
                <div>
                  <dt>当前状态</dt>
                  <dd>{{ chatSession?.status || '未初始化' }}</dd>
                </div>
                <div>
                  <dt>是否归档封存</dt>
                  <dd>{{ chatSession?.sealed ? '已封存 (不可回复)' : '保持开启' }}</dd>
                </div>
              </dl>
            </div>

            <div class="compose-area">
              <label class="compose-label">起草回复</label>
              <textarea
                  v-model="composeForm.content"
                  class="sleek-textarea"
                  rows="6"
                  maxlength="2000"
                  :disabled="chatSession?.sealed || socketState !== 'connected'"
                  placeholder="在此写下对学生的回复、关怀或下一步建议。按下回车并不会发送，请点击下方按钮。"
              />
              <button
                  class="action-btn action-btn--primary"
                  type="button"
                  :disabled="!composeForm.content || socketState !== 'connected' || chatSession?.sealed"
                  @click="sendMessage"
              >
                发送回复 <span class="arrow">→</span>
              </button>
            </div>

          </div>
        </aside>

      </section>
    </div>
  </main>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700;800&family=Noto+Serif+SC:wght@500;600;700&display=swap');

/* 全局极简白纸底色 */
.editorial-chat-page {
  min-height: 100vh;
  background: #fcfbf9;
  color: #1e2821;
  font-family: 'Manrope', 'Noto Serif SC', sans-serif;
  padding: 2rem 2vw 8rem;
  box-sizing: border-box;
}

.page-container {
  max-width: 1100px;
  margin: 0 auto;
}

/* 顶部导航 */
.dossier-nav {
  margin-bottom: 2.5rem;
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
.transcript-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  padding-bottom: 3rem;
  margin-bottom: 3rem;
  border-bottom: 1px solid rgba(42, 54, 46, 0.12);
  gap: 4rem;
}

.header-main {
  max-width: 640px;
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
  color: #5c6b60;
  line-height: 1.8;
  margin: 0;
}

.header-lead strong {
  color: #2a362e;
}

/* 连接状态呼吸灯 */
.connection-status {
  display: flex;
  align-items: center;
  gap: 0.8rem;
  background: rgba(255, 255, 255, 0.6);
  padding: 0.8rem 1.2rem;
  border-radius: 100px;
  border: 1px solid rgba(42, 54, 46, 0.06);
}

.status-indicator {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #cbd5cf;
}

.status-indicator.is-connected {
  background: #5c8c6b;
  box-shadow: 0 0 0 0 rgba(92, 140, 107, 0.4);
  animation: pulse-green 2s infinite;
}

.status-indicator.is-connecting {
  background: #d4a36a;
  animation: pulse-amber 1.5s infinite;
}

.status-indicator.is-closed {
  background: #a65e5e;
}

@keyframes pulse-green {
  0% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(92, 140, 107, 0.7); }
  70% { transform: scale(1); box-shadow: 0 0 0 8px rgba(92, 140, 107, 0); }
  100% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(92, 140, 107, 0); }
}

@keyframes pulse-amber {
  0% { opacity: 1; }
  50% { opacity: 0.5; }
  100% { opacity: 1; }
}

.status-text {
  font-family: 'Noto Serif SC', serif;
  font-size: 0.9rem;
  font-weight: 600;
  color: #5c6b60;
}

/* 核心双栏排版 */
.transcript-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(320px, 0.8fr);
  gap: 5rem;
  align-items: start;
}

/* ================= 左栏：剧本式消息流 ================= */
.section-head {
  margin-bottom: 2rem;
}

.section-title {
  font-family: 'Noto Serif SC', serif;
  font-size: 1.4rem;
  font-weight: 600;
  color: #1e2821;
  margin: 0 0 0.2rem 0;
}

.section-subtitle {
  font-family: 'Manrope', sans-serif;
  font-size: 0.85rem;
  color: #8a9c90;
  text-transform: uppercase;
  letter-spacing: 0.1em;
}

.transcript-stream {
  display: flex;
  flex-direction: column;
  gap: 2.5rem;
}

.message-row {
  display: grid;
  grid-template-columns: 80px minmax(0, 1fr);
  gap: 1.5rem;
  align-items: start;
}

/* 角色标签（左侧栏） */
.message-actor {
  font-family: 'Manrope', sans-serif;
  font-size: 0.85rem;
  font-weight: 800;
  letter-spacing: 0.1em;
  color: #8a9c90;
  text-align: right;
  padding-top: 0.4rem;
  position: relative;
}

/* 自己（咨询师）发言的样式调整 */
.message-row.is-counselor .message-actor {
  color: #4a5c51;
}

.message-row.is-counselor .message-body {
  border-left: 2px solid rgba(42, 54, 46, 0.2);
  padding-left: 1.5rem;
}

/* 消息内容区 */
.message-body {
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}

.message-time {
  font-family: 'Manrope', sans-serif;
  font-size: 0.85rem;
  color: #a3b0a7;
}

.message-content {
  font-family: 'Noto Serif SC', serif;
  font-size: 1.05rem;
  line-height: 1.85;
  color: #1e2821;
  margin: 0;
  white-space: pre-wrap; /* 保留换行 */
}

/* ================= 右栏：起草台 ================= */
.compose-desk {
  position: relative;
}

.desk-sticky-container {
  position: sticky;
  top: 2rem;
  display: flex;
  flex-direction: column;
  gap: 3rem;
}

/* 元数据面板 */
.session-meta-panel {
  padding: 1.5rem;
  background: rgba(255, 255, 255, 0.5);
  border: 1px solid rgba(42, 54, 46, 0.08);
  border-radius: 16px;
}

.meta-heading {
  font-family: 'Noto Serif SC', serif;
  font-size: 1.1rem;
  font-weight: 600;
  color: #2a362e;
  margin: 0 0 1.2rem 0;
  padding-bottom: 0.8rem;
  border-bottom: 1px solid rgba(42, 54, 46, 0.08);
}

.meta-list {
  display: grid;
  gap: 1rem;
  margin: 0;
}

.meta-list dt {
  font-family: 'Manrope', sans-serif;
  font-size: 0.8rem;
  font-weight: 700;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: #8a9c90;
}

.meta-list dd {
  font-family: 'Noto Serif SC', serif;
  font-size: 1rem;
  color: #4a5c51;
  margin: 0.3rem 0 0 0;
}

/* 输入区 */
.compose-area {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.compose-label {
  font-family: 'Noto Serif SC', serif;
  font-size: 1.1rem;
  font-weight: 600;
  color: #1e2821;
}

.sleek-textarea {
  width: 100%;
  box-sizing: border-box;
  border: none;
  border-bottom: 1px dashed rgba(42, 54, 46, 0.2);
  background: transparent;
  padding: 0.8rem 0;
  font-family: 'Noto Serif SC', serif;
  font-size: 1.05rem;
  line-height: 1.7;
  color: #1e2821;
  resize: vertical;
  outline: none;
  transition: border-color 0.3s ease;
}

.sleek-textarea::placeholder {
  color: #a3b0a7;
  font-style: italic;
}

.sleek-textarea:focus {
  border-bottom-color: #2a362e;
  border-bottom-style: solid;
}

.sleek-textarea:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.action-btn {
  align-self: flex-start;
  margin-top: 1rem;
  padding: 1.2rem 2.2rem;
  border-radius: 100px;
  font-family: 'Noto Serif SC', serif;
  font-size: 1.05rem;
  font-weight: 600;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 0.6rem;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.action-btn--primary {
  background: #2a362e;
  border: none;
  color: #ffffff;
  box-shadow: 0 12px 24px rgba(42, 54, 46, 0.15);
}

.action-btn--primary:hover:not(:disabled) {
  background: #1c2620;
  transform: translateY(-2px);
  box-shadow: 0 16px 32px rgba(42, 54, 46, 0.25);
}

.action-btn:disabled {
  background: #8a9c90;
  box-shadow: none;
  cursor: not-allowed;
}

/* 状态样式 */
.error-banner {
  background: rgba(140, 74, 74, 0.08);
  color: #8c4a4a;
  padding: 1.5rem;
  border-radius: 12px;
  text-align: center;
  font-family: 'Noto Serif SC', serif;
  margin-bottom: 3rem;
}

.loading-state,
.empty-state {
  text-align: center;
  padding: 6rem 0;
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

/* 交互动画 */
.arrow {
  font-family: 'Manrope', sans-serif;
  transition: transform 0.3s ease;
}

.nav-ghost-btn:hover .arrow {
  transform: translateX(-4px);
}

.action-btn:hover:not(:disabled) .arrow {
  transform: translateX(4px);
}

/* 响应式 */
@media (max-width: 900px) {
  .transcript-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 2rem;
  }

  .transcript-grid {
    grid-template-columns: 1fr;
    gap: 4rem;
  }

  .desk-sticky-container {
    position: relative;
    top: 0;
  }

  .message-row {
    grid-template-columns: 60px minmax(0, 1fr);
    gap: 1rem;
  }
}

@media (max-width: 600px) {
  .message-row {
    grid-template-columns: 1fr; /* 移动端角色标签移至上方 */
    gap: 0.5rem;
  }

  .message-actor {
    text-align: left;
    padding-top: 0;
  }

  .message-row.is-counselor .message-body {
    border-left: none;
    padding-left: 0;
  }
}
</style>