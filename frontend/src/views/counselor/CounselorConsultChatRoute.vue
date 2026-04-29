<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { buildConsultChatWebSocketUrl, fetchConsultChatMessagesApi, fetchConsultChatSessionApi } from '@/api/chat'
import type { ConsultChatMessage, ConsultChatSession, ConsultChatSocketPayload } from '@/api/types'
import { getToken } from '@/core/session'
import { toErrorMessage, toNumberParam } from '@/views/shared/page-logic'

const route = useRoute()
const router = useRouter()
const backendOrigin = import.meta.env.VITE_API_BASE_URL ?? `${window.location.protocol}//${window.location.hostname}:8080`

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
      return '等待连接'
  }
})
const peerStatusText = computed(() => {
  if (!chatSession.value) {
    return '正在读取会话'
  }
  if (chatSession.value.sealed || chatSession.value.status === 'CLOSED') {
    return '聊天室已结束'
  }
  if (chatSession.value.status === 'ARCHIVED') {
    return '聊天室已归档'
  }
  if (socketState.value !== 'connected') {
    return '等待通道恢复'
  }
  return peerOnline.value ? '学生已在线' : '学生暂未进入'
})

function parseChatDate(value: string | Date | number[] | null | undefined): Date | null {
  if (!value) {
    return null
  }
  if (value instanceof Date) {
    return Number.isNaN(value.getTime()) ? null : value
  }
  if (Array.isArray(value)) {
    const [year, month, day, hour = 0, minute = 0, second = 0, nano = 0] = value
    const parsed = new Date(year, month - 1, day, hour, minute, second, Math.floor(nano / 1_000_000))
    return Number.isNaN(parsed.getTime()) ? null : parsed
  }
  const parsed = new Date(String(value).trim().replace(' ', 'T'))
  return Number.isNaN(parsed.getTime()) ? null : parsed
}

function normalizeMessage(message: ConsultChatMessage): ConsultChatMessage {
  const parsedDate = parseChatDate(message.createdAt)
  return {
    ...message,
    createdAt: (parsedDate ?? new Date()).toISOString()
  }
}

function formatDateTime(value: string | Date | number[] | null | undefined): string {
  const safeDate = parseChatDate(value) ?? new Date()
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(safeDate)
}

function resolveSenderName(message: ConsultChatMessage): string {
  if (message.senderDisplayName?.trim()) {
    return message.senderDisplayName.trim()
  }
  if (message.senderType === 'COUNSELOR') {
    return '我'
  }
  if (message.senderType === 'SYSTEM') {
    return '系统消息'
  }
  return '来访学生'
}

function resolveSenderInitial(message: ConsultChatMessage): string {
  return resolveSenderName(message).slice(0, 1) || (message.senderType === 'COUNSELOR' ? '我' : '学')
}

function resolveAvatarSrc(avatarUrl: string | null | undefined): string {
  if (!avatarUrl) {
    return ''
  }
  if (avatarUrl.startsWith('http://') || avatarUrl.startsWith('https://')) {
    return avatarUrl
  }
  if (avatarUrl.startsWith('/')) {
    return `${backendOrigin}${avatarUrl}`
  }
  return avatarUrl
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
    messages.value = [...messages.value, normalizeMessage(payload.message)]
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
    errorMessage.value = '无法识别当前预约。'
    return
  }

  const token = getToken()
  if (!token) {
    errorMessage.value = '缺少登录凭证，请重新登录。'
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
    errorMessage.value = '实时通道连接异常，请稍后重试。'
    socketState.value = 'closed'
    peerOnline.value = false
  }

  socketRef.value = socket
}

async function loadChatContext(): Promise<void> {
  if (!appointmentId.value) {
    errorMessage.value = '无法识别当前预约。'
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
    messages.value = history.map(normalizeMessage)
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
  void router.push({ name: 'counselor-appointments' })
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
  <main class="counselor-chat-page">
    <div class="page-container">
      <nav class="page-nav">
        <button class="nav-link" type="button" @click="goBack">
          <span class="nav-link__arrow">←</span>
          返回预约处理
        </button>
      </nav>

      <header class="page-header">
        <div class="page-header__copy">
          <span class="page-tag">Consultation Transcript</span>
          <h1 class="page-title">会话记录</h1>
          <p class="page-summary">
            当前正在查看预约 #{{ appointmentId || '-' }} 的私密沟通记录。
            学生进入聊天室后，系统会自动切换为可交流状态，全程不需要刷新页面。
          </p>
        </div>

        <div class="connection-badge">
          <span class="connection-dot" :class="`connection-dot--${socketState}`"></span>
          <span>{{ socketStateText }}</span>
        </div>
      </header>

      <p v-if="errorMessage" class="error-banner">{{ errorMessage }}</p>

      <section class="layout-grid">
        <section class="stream-panel">
          <header class="section-head">
            <div>
              <h2 class="section-title">沟通记录</h2>
              <p class="section-subtitle">Message Log</p>
            </div>
            <div class="section-status">{{ peerStatusText }}</div>
          </header>

          <div v-if="loading" class="state-panel">
            <div class="spinner"></div>
            <p>正在读取历史消息并建立实时通道...</p>
          </div>

          <div v-else-if="!messages.length" class="state-panel">
            <p>当前还没有消息记录。你可以先发送一条问候，等待学生上线后继续交流。</p>
          </div>

          <div v-else ref="messageViewportRef" class="message-stream">
            <article
              v-for="message in messages"
              :key="message.messageId"
              class="message-row"
              :class="{ 'message-row--self': message.senderType === 'COUNSELOR' }"
            >
              <div class="message-actor">
                <img
                  v-if="resolveAvatarSrc(message.senderAvatarUrl)"
                  class="message-avatar"
                  :src="resolveAvatarSrc(message.senderAvatarUrl)"
                  :alt="resolveSenderName(message)"
                >
                <span v-else class="message-avatar message-avatar--placeholder">
                  {{ resolveSenderInitial(message) }}
                </span>
                <div class="message-actor__text">
                  <span>{{ resolveSenderName(message) }}</span>
                  <time>{{ formatDateTime(message.createdAt) }}</time>
                </div>
              </div>
              <div class="message-body">
                <p class="message-content">{{ message.content }}</p>
              </div>
            </article>
          </div>
        </section>

        <aside class="sidebar-panel">
          <section class="meta-panel">
            <h3 class="meta-title">会话控制台</h3>
            <dl class="meta-list">
              <div>
                <dt>记录编号</dt>
                <dd>#{{ chatSession?.chatSessionId || '待生成' }}</dd>
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
                <dt>封存状态</dt>
                <dd>{{ chatSession?.sealed ? '已结束' : '进行中' }}</dd>
              </div>
            </dl>
          </section>

          <section class="compose-panel">
            <label class="compose-label">发送回复</label>
            <textarea
              v-model="composeForm.content"
              class="compose-input"
              rows="6"
              maxlength="2000"
              :disabled="!canSend"
              placeholder="在此写下对学生的回应、关怀或下一步建议。"
              @keydown.enter.exact.prevent="sendMessage"
            />
            <button
              class="submit-btn"
              type="button"
              :disabled="!composeForm.content.trim() || !canSend"
              @click="sendMessage"
            >
              发送回复
              <span class="submit-btn__arrow">→</span>
            </button>
          </section>
        </aside>
      </section>
    </div>
  </main>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700;800&family=Noto+Serif+SC:wght@500;600;700&display=swap');

.counselor-chat-page {
  min-height: 100vh;
  padding: 2rem 2vw 8rem;
  box-sizing: border-box;
  background:
    radial-gradient(circle at 12% 10%, rgba(196, 224, 207, 0.22), transparent 26rem),
    linear-gradient(180deg, #ffffff, #fbfdfc);
  color: #1e2821;
}

.page-container {
  max-width: 1120px;
  margin: 0 auto;
}

.page-nav {
  margin-bottom: 2rem;
}

.nav-link {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  border: none;
  background: transparent;
  padding: 0;
  color: #5c6b60;
  cursor: pointer;
  font-family: 'Noto Serif SC', serif;
  font-size: 1rem;
  font-weight: 600;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.nav-link:hover {
  color: #1e2821;
  transform: translateX(-4px);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 3rem;
  padding-bottom: 2.8rem;
  margin-bottom: 2.8rem;
  border-bottom: 1px solid rgba(42, 54, 46, 0.055);
}

.page-header__copy {
  max-width: 660px;
}

.page-tag,
.section-subtitle,
.meta-list dt,
.message-actor__text span,
.message-actor__text time {
  font-family: 'Manrope', sans-serif;
  font-size: 0.8rem;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #8a9c90;
}

.page-title {
  margin: 0.95rem 0 0;
  font-family: 'Noto Serif SC', serif;
  font-size: clamp(2.4rem, 4vw, 3.2rem);
  font-weight: 600;
  letter-spacing: 0.04em;
}

.page-summary {
  margin: 1.15rem 0 0;
  color: #5c6b60;
  font-family: 'Noto Serif SC', serif;
  font-size: 1.04rem;
  line-height: 1.85;
}

.connection-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.8rem;
  padding: 0.85rem 1.2rem;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 18px 40px rgba(54, 66, 58, 0.055);
  font-family: 'Noto Serif SC', serif;
  color: #5c6b60;
}

.connection-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #cbd5cf;
}

.connection-dot--connected {
  background: #78a884;
  box-shadow: 0 0 0 0 rgba(92, 140, 107, 0.4);
  animation: pulse-green 2s infinite;
}

.connection-dot--connecting {
  background: #d4a36a;
  animation: pulse-amber 1.5s infinite;
}

.connection-dot--closed {
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

.layout-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.42fr) minmax(320px, 0.78fr);
  gap: 4rem;
  align-items: start;
}

.stream-panel,
.meta-panel,
.compose-panel,
.state-panel {
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.96), rgba(247, 252, 249, 0.92));
  border: 1px solid rgba(255, 255, 255, 0.86);
  border-radius: 28px;
  box-shadow: 0 24px 56px rgba(54, 66, 58, 0.06);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
}

.stream-panel {
  padding: 2rem;
}

.section-head {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: flex-end;
  margin-bottom: 1.8rem;
}

.section-title,
.meta-title,
.compose-label {
  margin: 0;
  font-family: 'Noto Serif SC', serif;
  font-size: 1.3rem;
  font-weight: 600;
  color: #1e2821;
}

.section-status {
  color: #5c6b60;
  font-family: 'Noto Serif SC', serif;
  font-size: 0.98rem;
}

.state-panel {
  text-align: center;
  padding: 4rem 1.5rem;
  color: #7b8c80;
  font-family: 'Noto Serif SC', serif;
}

.spinner {
  width: 40px;
  height: 40px;
  margin: 0 auto 1.4rem;
  border-radius: 50%;
  border: 2px solid rgba(130, 150, 138, 0.2);
  border-top-color: #2a362e;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.message-stream {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  max-height: 70vh;
  overflow-y: auto;
  padding-right: 0.4rem;
}

.message-stream::-webkit-scrollbar {
  width: 6px;
}

.message-stream::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: rgba(95, 133, 110, 0.18);
}

.message-row {
  display: grid;
  gap: 0.8rem;
}

.message-actor {
  display: flex;
  align-items: center;
  gap: 0.8rem;
}

.message-avatar {
  width: 2.5rem;
  height: 2.5rem;
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

.message-actor__text {
  display: flex;
  flex-direction: column;
  gap: 0.18rem;
}

.message-body {
  padding: 1.15rem 1.25rem;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.95);
  box-shadow: inset 0 0 0 1px rgba(42, 54, 46, 0.04);
}

.message-row--self .message-body {
  background: linear-gradient(145deg, rgba(229, 244, 235, 0.88), rgba(255, 255, 255, 0.96));
}

.message-content {
  margin: 0;
  color: #1e2821;
  font-family: 'Noto Serif SC', serif;
  font-size: 1.02rem;
  line-height: 1.85;
  white-space: pre-wrap;
}

.sidebar-panel {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  position: sticky;
  top: 2rem;
}

.meta-panel,
.compose-panel {
  padding: 1.6rem;
}

.meta-list {
  display: grid;
  gap: 1rem;
  margin: 1.2rem 0 0;
}

.meta-list dd {
  margin: 0.3rem 0 0;
  color: #4a5c51;
  font-family: 'Noto Serif SC', serif;
  font-size: 1rem;
}

.compose-panel {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.compose-input {
  width: 100%;
  box-sizing: border-box;
  border: none;
  background: rgba(255, 255, 255, 0.96);
  padding: 1rem 1.1rem;
  border-radius: 22px;
  font-family: 'Noto Serif SC', serif;
  font-size: 1.02rem;
  line-height: 1.75;
  color: #1e2821;
  resize: vertical;
  outline: none;
  box-shadow:
    0 18px 42px rgba(54, 66, 58, 0.055),
    inset 0 0 0 1px rgba(42, 54, 46, 0.04);
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.compose-input::placeholder {
  color: #a3b0a7;
}

.compose-input:focus {
  transform: translateY(-3px);
  box-shadow:
    0 24px 52px rgba(54, 66, 58, 0.075),
    inset 0 0 0 1px rgba(120, 168, 132, 0.22);
}

.compose-input:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.submit-btn {
  align-self: flex-start;
  margin-top: 0.5rem;
  padding: 1.05rem 2rem;
  border: none;
  border-radius: 999px;
  background: linear-gradient(135deg, #5f856e, #7ca98a);
  color: #ffffff;
  font-family: 'Noto Serif SC', serif;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 0.55rem;
  box-shadow: 0 18px 36px rgba(95, 133, 110, 0.18);
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.submit-btn:hover:not(:disabled) {
  transform: translateY(-3px);
  box-shadow: 0 24px 42px rgba(42, 54, 46, 0.18);
}

.submit-btn:disabled {
  background: #c8d8cc;
  box-shadow: none;
  cursor: not-allowed;
}

.error-banner {
  margin: 0 0 2rem;
  padding: 1.2rem 1.4rem;
  border-radius: 20px;
  background: rgba(140, 74, 74, 0.08);
  color: #8c4a4a;
  font-family: 'Noto Serif SC', serif;
  text-align: center;
}

@media (max-width: 960px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 1.6rem;
  }

  .layout-grid {
    grid-template-columns: 1fr;
    gap: 2rem;
  }

  .sidebar-panel {
    position: static;
    top: auto;
  }
}

@media (max-width: 640px) {
  .counselor-chat-page {
    padding: 1rem 1rem 5rem;
  }

  .stream-panel,
  .meta-panel,
  .compose-panel,
  .state-panel {
    padding: 1.35rem;
    border-radius: 22px;
  }

  .submit-btn {
    width: 100%;
    justify-content: center;
  }
}
</style>
