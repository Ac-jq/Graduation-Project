<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
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
const peerOnline = ref(false)
const composeForm = reactive({
  content: ''
})
const socketRef = ref<WebSocket | null>(null)
const messageViewportRef = ref<HTMLElement | null>(null)

const appointmentId = computed(() => toNumberParam(route.params.appointmentId))
const canSend = computed(() => {
  if (!chatSession.value) {
    return false
  }

  return !chatSession.value.sealed
    && chatSession.value.status !== 'ARCHIVED'
    && chatSession.value.status !== 'CLOSED'
    && socketState.value === 'connected'
    && peerOnline.value
})
const socketStateText = computed(() => {
  switch (socketState.value) {
    case 'connected':
      return '实时通道已连接'
    case 'connecting':
      return '正在建立连接'
    case 'closed':
      return '连接已断开'
    default:
      return '通道待命'
  }
})
const peerStatusText = computed(() => {
  if (!chatSession.value) {
    return '正在载入'
  }
  if (chatSession.value.sealed || chatSession.value.status === 'CLOSED') {
    return '聊天室已结束'
  }
  if (chatSession.value.status === 'ARCHIVED') {
    return '聊天室已归档'
  }
  if (socketState.value !== 'connected') {
    return '等待连接恢复'
  }
  return peerOnline.value ? '学生已在线' : '学生暂未进入'
})

function parseChatDate(value: string | Date | number[] | null | undefined): Date | null {
  if (!value) return null
  if (value instanceof Date) return Number.isNaN(value.getTime()) ? null : value
  if (Array.isArray(value)) {
    const [year, month, day, hour = 0, minute = 0, second = 0, nano = 0] = value
    const parsed = new Date(year, month - 1, day, hour, minute, second, Math.floor(nano / 1000000))
    return Number.isNaN(parsed.getTime()) ? null : parsed
  }
  const parsed = new Date(String(value).trim().replace(' ', 'T'))
  return Number.isNaN(parsed.getTime()) ? null : parsed
}

function formatTime(value: string | Date | number[] | null | undefined): string {
  const d = parseChatDate(value)
  if (!d) return '暂无记录'
  return `${String(d.getMonth() + 1).padStart(2, '0')}/${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

function resolveSenderName(message: ConsultChatMessage): string {
  if (message.senderDisplayName?.trim()) {
    return message.senderDisplayName.trim()
  }
  return message.senderType === 'COUNSELOR' ? '我' : '来访学生'
}

function resolveSenderInitial(message: ConsultChatMessage): string {
  return resolveSenderName(message).slice(0, 1) || (message.senderType === 'COUNSELOR' ? '我' : '生')
}

async function scrollToBottom(): Promise<void> {
  await nextTick()
  const viewport = messageViewportRef.value
  if (!viewport) {
    return
  }
  viewport.scrollTop = viewport.scrollHeight
}

function disconnectSocket(): void {
  if (socketRef.value) {
    socketRef.value.onclose = null
    socketRef.value.close()
  }
  socketRef.value = null
  socketState.value = 'closed'
  peerOnline.value = false
}

function applySocketPayload(payload: ConsultChatSocketPayload): void {
  if (payload.type === 'CONNECTED') {
    peerOnline.value = (payload.onlineCount ?? 0) > 1
    errorMessage.value = ''
    return
  }

  if (payload.type === 'MESSAGE' && payload.message) {
    messages.value = [...messages.value, payload.message]
    void scrollToBottom()
    return
  }

  if (payload.type === 'SYSTEM') {
    if (payload.action === 'USER_JOINED') {
      peerOnline.value = true
    }
    if (payload.action === 'WAITING_PEER' || payload.action === 'USER_LEFT') {
      peerOnline.value = false
    }
    if (payload.action === 'CHAT_CLOSED') {
      if (payload.session) {
        chatSession.value = payload.session
      } else if (chatSession.value) {
        chatSession.value = {
          ...chatSession.value,
          status: 'CLOSED',
          sealed: true
        }
      }
      peerOnline.value = false
      composeForm.content = ''
    }
    return
  }

  if (payload.type === 'ERROR' && payload.tip) {
    errorMessage.value = payload.tip
  }
}

function connectSocket(): void {
  if (!appointmentId.value) {
    errorMessage.value = '无效的预约编号。'
    return
  }

  const token = getToken()
  if (!token) {
    errorMessage.value = '缺少身份凭证。'
    return
  }

  disconnectSocket()
  socketState.value = 'connecting'
  const socket = new WebSocket(buildConsultChatWebSocketUrl(appointmentId.value, token))

  socket.onopen = () => {
    socketState.value = 'connected'
    errorMessage.value = ''
  }

  socket.onmessage = (event) => {
    try {
      const payload = JSON.parse(event.data) as ConsultChatSocketPayload
      applySocketPayload(payload)
    } catch (error) {
      errorMessage.value = toErrorMessage(error)
    }
  }

  socket.onclose = () => {
    socketState.value = 'closed'
    peerOnline.value = false
  }

  socket.onerror = () => {
    errorMessage.value = 'WebSocket 连接异常。'
    socketState.value = 'closed'
    peerOnline.value = false
  }

  socketRef.value = socket
}

async function loadChatContext(): Promise<void> {
  if (!appointmentId.value) {
    errorMessage.value = '无效的预约编号。'
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
    await scrollToBottom()
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    loading.value = false
  }
}

function sendMessage(): void {
  const content = composeForm.content.trim()
  if (!content || socketRef.value?.readyState !== WebSocket.OPEN || !canSend.value) {
    return
  }

  socketRef.value.send(JSON.stringify({ content }))
  composeForm.content = ''
}

function goBack(): void {
  router.push({ name: 'counselor-appointments' })
}

watch(
  () => route.params.appointmentId,
  () => {
    void loadChatContext()
  }
)

watch(
  () => messages.value.length,
  async () => {
    await scrollToBottom()
  }
)

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
          <span class="arrow">←</span> 返回预约处理
        </button>
      </nav>

      <header class="transcript-header">
        <div class="header-main">
          <span class="header-tag">Consultation Transcript</span>
          <h1 class="huge-title">沟通实录</h1>
          <p class="header-lead">
            当前正在查看 <strong>预约 #{{ appointmentId || '-' }}</strong> 的私密会话记录。
            当学生进入聊天室后，系统会自动切换为可聊天状态，无需手动刷新页面。
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
            <p>正在读取历史消息并建立实时通道...</p>
          </div>

          <div v-else-if="!messages.length" class="empty-state">
            <p class="empty-desc">当前还没有消息记录。你可以先发送一条问候，等待学生上线后继续交流。</p>
          </div>

          <div v-else ref="messageViewportRef" class="transcript-stream">
            <article
              v-for="message in messages"
              :key="message.messageId"
              class="message-row"
              :class="{ 'is-counselor': message.senderType === 'COUNSELOR' }"
            >
              <div class="message-actor">
                <img
                  v-if="message.senderAvatarUrl"
                  class="message-avatar"
                  :src="message.senderAvatarUrl"
                  :alt="resolveSenderName(message)"
                >
                <span v-else class="message-avatar message-avatar--placeholder">
                  {{ resolveSenderInitial(message) }}
                </span>
                <span>{{ resolveSenderName(message) }}</span>
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
                  <dt>记录编号</dt>
                  <dd>#{{ chatSession?.chatSessionId || '待分配' }}</dd>
                </div>
                <div>
                  <dt>当前状态</dt>
                  <dd>{{ peerStatusText }}</dd>
                </div>
                <div>
                  <dt>会话标记</dt>
                  <dd>{{ chatSession?.status || '未初始化' }}</dd>
                </div>
                <div>
                  <dt>是否封存</dt>
                  <dd>{{ chatSession?.sealed ? '已结束' : '进行中' }}</dd>
                </div>
              </dl>
            </div>

            <div class="compose-area">
              <label class="compose-label">发送回复</label>
              <textarea
                v-model="composeForm.content"
                class="sleek-textarea"
                rows="6"
                maxlength="2000"
                :disabled="!canSend"
                placeholder="在此写下对学生的回应、关怀或下一步建议。"
                @keydown.enter.exact.prevent="sendMessage"
              />
              <button
                class="action-btn action-btn--primary"
                type="button"
                :disabled="!composeForm.content.trim() || !canSend"
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

.editorial-chat-page {
  min-height: 100vh;
  background:
    radial-gradient(circle at 12% 10%, rgba(196, 224, 207, 0.26), transparent 26rem),
    linear-gradient(180deg, #ffffff, #fbfdfc);
  color: #1e2821;
  font-family: 'Manrope', 'Noto Serif SC', sans-serif;
  padding: 2rem 2vw 8rem;
  box-sizing: border-box;
}

.page-container {
  max-width: 1100px;
  margin: 0 auto;
}

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
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.nav-ghost-btn:hover {
  color: #1e2821;
  transform: translateX(-4px);
}

.transcript-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  padding-bottom: 3rem;
  margin-bottom: 3rem;
  border-bottom: 1px solid rgba(42, 54, 46, 0.055);
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

.connection-status {
  display: flex;
  align-items: center;
  gap: 0.8rem;
  background: rgba(255, 255, 255, 0.92);
  padding: 0.8rem 1.2rem;
  border-radius: 100px;
  box-shadow: 0 18px 40px rgba(54, 66, 58, 0.055);
}

.status-indicator {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #cbd5cf;
}

.status-indicator.is-connected {
  background: #78a884;
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

.transcript-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(320px, 0.8fr);
  gap: 5rem;
  align-items: start;
}

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
  max-height: 70vh;
  overflow-y: auto;
  padding-right: 0.5rem;
  scrollbar-width: thin;
  scrollbar-color: rgba(95, 133, 110, 0.18) transparent;
}

.transcript-stream::-webkit-scrollbar {
  width: 6px;
}

.transcript-stream::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: rgba(95, 133, 110, 0.18);
}

.message-row {
  display: grid;
  grid-template-columns: 104px minmax(0, 1fr);
  gap: 1.5rem;
  align-items: start;
}

.message-actor {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0.45rem;
  font-family: 'Manrope', sans-serif;
  font-size: 0.76rem;
  font-weight: 800;
  letter-spacing: 0.08em;
  color: #8a9c90;
  text-align: right;
  padding-top: 0.1rem;
  position: relative;
}

.message-avatar {
  width: 2.4rem;
  height: 2.4rem;
  border-radius: 999px;
  object-fit: cover;
  box-shadow: 0 12px 28px rgba(54, 66, 58, 0.08);
}

.message-avatar--placeholder {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(145deg, #edf8f1, #ffffff);
  color: #5f856e;
  font-family: 'Noto Serif SC', serif;
  font-size: 1rem;
  font-weight: 700;
}

.message-row.is-counselor .message-actor {
  color: #4a5c51;
}

.message-row.is-counselor .message-body {
  background: linear-gradient(145deg, rgba(229, 244, 235, 0.88), rgba(255, 255, 255, 0.96));
}

.message-body {
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
  padding: 1.15rem 1.25rem;
  border-radius: 26px;
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.98), rgba(247, 252, 249, 0.94));
  box-shadow: 0 18px 44px rgba(54, 66, 58, 0.055);
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
  white-space: pre-wrap;
}

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

.session-meta-panel {
  padding: 1.5rem;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 24px 56px rgba(54, 66, 58, 0.06);
  border-radius: 26px;
}

.meta-heading {
  font-family: 'Noto Serif SC', serif;
  font-size: 1.1rem;
  font-weight: 600;
  color: #2a362e;
  margin: 0 0 1.2rem 0;
  padding-bottom: 0.8rem;
  border-bottom: 1px solid rgba(42, 54, 46, 0.055);
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
  background: rgba(255, 255, 255, 0.96);
  padding: 1rem 1.1rem;
  border-radius: 22px;
  font-family: 'Noto Serif SC', serif;
  font-size: 1.05rem;
  line-height: 1.7;
  color: #1e2821;
  resize: vertical;
  outline: none;
  box-shadow:
    0 18px 42px rgba(54, 66, 58, 0.055),
    inset 0 0 0 1px rgba(42, 54, 46, 0.04);
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.sleek-textarea::placeholder {
  color: #a3b0a7;
  font-style: italic;
}

.sleek-textarea:focus {
  transform: translateY(-3px);
  box-shadow:
    0 24px 52px rgba(54, 66, 58, 0.075),
    inset 0 0 0 1px rgba(120, 168, 132, 0.22);
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
  background: linear-gradient(135deg, #5f856e, #7ca98a);
  border: none;
  color: #ffffff;
  box-shadow: 0 18px 36px rgba(95, 133, 110, 0.18);
}

.action-btn--primary:hover:not(:disabled) {
  background: linear-gradient(135deg, #567a64, #72a17f);
  transform: translateY(-2px);
  box-shadow: 0 16px 32px rgba(42, 54, 46, 0.25);
}

.action-btn:disabled {
  background: #c8d8cc;
  box-shadow: none;
  cursor: not-allowed;
}

.error-banner {
  background: rgba(140, 74, 74, 0.06);
  color: #8c4a4a;
  padding: 1.5rem;
  border-radius: 22px;
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
    grid-template-columns: 1fr;
    gap: 0.5rem;
  }

  .message-actor {
    text-align: left;
    padding-top: 0;
    align-items: flex-start;
    flex-direction: row;
    align-items: center;
  }

  .message-row.is-counselor .message-body {
    padding-left: 1.25rem;
  }
}
</style>
