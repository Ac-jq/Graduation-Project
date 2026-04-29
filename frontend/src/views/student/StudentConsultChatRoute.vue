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
const messageCount = computed(() => messages.value.length)
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
const socketStateLabel = computed(() => {
  switch (socketState.value) {
    case 'connecting':
      return '正在连接'
    case 'connected':
      return '连接正常'
    case 'closed':
      return '连接已断开'
    default:
      return '等待连接'
  }
})
const roomStatusLabel = computed(() => {
  if (!chatSession.value) {
    return '正在读取会话状态'
  }
  if (chatSession.value.sealed || chatSession.value.status === 'CLOSED') {
    return '本次聊天室已结束'
  }
  if (chatSession.value.status === 'ARCHIVED') {
    return '本次聊天室已归档'
  }
  if (socketState.value !== 'connected') {
    return '正在建立实时通道'
  }
  return peerOnline.value ? '咨询师已上线，可以开始交流' : '你已进入聊天室，正在等待咨询师上线'
})
const composerHint = computed(() => {
  if (!chatSession.value) {
    return '正在加载聊天室信息，请稍候。'
  }
  if (chatSession.value.sealed || chatSession.value.status === 'CLOSED') {
    return '本次预约沟通已经结束，当前页面仅保留历史记录查看。'
  }
  if (chatSession.value.status === 'ARCHIVED') {
    return '聊天室已归档，当前无法继续发送消息。'
  }
  if (socketState.value !== 'connected') {
    return '正在连接实时通道，请稍候。'
  }
  if (!peerOnline.value) {
    return '系统会在咨询师进入后自动开放输入区，无需刷新页面。'
  }
  return '可以从最近最具体的一件事说起，系统会保留你们本次预约后的沟通记录。'
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

function formatDate(value: string | Date | number[] | null | undefined): string {
  const parsedDate = parseChatDate(value)
  const safeDate = parsedDate ?? new Date()
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
    return '咨询师'
  }
  if (message.senderType === 'SYSTEM') {
    return '系统消息'
  }
  return '我'
}

function resolveSenderInitial(message: ConsultChatMessage): string {
  return resolveSenderName(message).slice(0, 1) || (message.senderType === 'COUNSELOR' ? '咨' : '我')
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

function resolveBubbleClass(senderType: string): string {
  if (senderType === 'STUDENT') {
    return 'message-card message-card--self'
  }
  if (senderType === 'COUNSELOR') {
    return 'message-card message-card--peer'
  }
  return 'message-card message-card--system'
}

async function scrollToLatestMessage(): Promise<void> {
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
    void scrollToLatestMessage()
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
    errorMessage.value = '无法识别当前预约会话。'
    return
  }

  const token = getToken()
  if (!token) {
    errorMessage.value = '缺少登录凭证，请重新登录后再试。'
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
    errorMessage.value = '无法识别当前预约会话。'
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
    await scrollToLatestMessage()
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
  void router.push({ name: 'student-appointments' })
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
      await scrollToLatestMessage()
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
  <main class="healing-chat-page">
    <div class="app-window">
      <aside class="app-sidebar">
        <nav class="sidebar-nav">
          <button class="nav-link" type="button" @click="goBack">
            <span class="nav-link__arrow">←</span>
            返回预约记录
          </button>
        </nav>

        <header class="sidebar-hero">
          <p class="overline">学生预约沟通</p>
          <h1 class="hero-title">一间安静的<br/>聊天室。</h1>
          <p class="hero-summary">
            把想说的话留在这里。对方上线后会立刻切换到可聊天状态，无需手动刷新页面。
          </p>
        </header>

        <div v-if="chatSession && !loading" class="sidebar-info">
          <section class="info-group highlight-group">
            <p class="overline">交流建议</p>
            <p class="body-text">
              不用一次说完整，先从最具体的感受开始。比如最近最明显的睡眠变化、最反复出现的一件事、最难压下来的想法。
            </p>
          </section>

          <section class="info-group">
            <p class="overline">会话概览</p>
            <div class="info-row">
              <span>预约编号</span>
              <strong>#{{ chatSession.appointmentId }}</strong>
            </div>
            <div class="info-row">
              <span>开放时间</span>
              <strong>{{ formatDate(chatSession.openTime) }}</strong>
            </div>
            <div class="info-row">
              <span>咨询师</span>
              <strong>{{ peerOnline ? '已上线' : '离线中' }}</strong>
            </div>
            <div class="info-row">
              <span>消息数量</span>
              <strong>{{ messageCount }}</strong>
            </div>
          </section>
        </div>
      </aside>

      <section class="app-main">
        <p v-if="errorMessage" class="alert-banner">{{ errorMessage }}</p>

        <div v-if="loading" class="state-container">
          <div class="placeholder-orb"></div>
          <h2>正在进入聊天室</h2>
          <p>会话信息、历史消息和实时通道正在同步...</p>
        </div>

        <div v-else-if="chatSession" class="chat-workspace">
          <header class="chat-header">
            <div class="header-titles">
              <h2 class="header-title">当前交流空间</h2>
              <p class="header-subtitle">{{ roomStatusLabel }}</p>
            </div>
            <div class="status-pill" :class="`status-pill--${socketState}`">
              <span class="status-dot"></span>
              {{ socketStateLabel }}
            </div>
          </header>

          <div ref="messageViewportRef" class="message-viewport">
            <div v-if="messages.length" class="message-stream">
              <article
                  v-for="message in messages"
                  :key="message.messageId"
                  :class="resolveBubbleClass(message.senderType)"
              >
                <div class="message-card__meta">
                  <img
                      v-if="resolveAvatarSrc(message.senderAvatarUrl)"
                      class="message-avatar"
                      :src="resolveAvatarSrc(message.senderAvatarUrl)"
                      :alt="resolveSenderName(message)"
                  >
                  <span v-else class="message-avatar message-avatar--placeholder">
                    {{ resolveSenderInitial(message) }}
                  </span>
                  <div class="message-card__meta-text">
                    <span>{{ resolveSenderName(message) }}</span>
                    <time>{{ formatDate(message.createdAt) }}</time>
                  </div>
                </div>
                <p class="message-card__content">{{ message.content }}</p>
              </article>
            </div>

            <div v-else class="empty-stream">
              <p class="overline">暂无历史消息</p>
              <h3>第一句话，不需要很完整。</h3>
              <p>可以先从“我最近总是睡不着”或“我今天特别想找人说说话”开始。</p>
            </div>
          </div>

          <div class="composer-area" :class="{ 'composer-area--disabled': !canSend }">
            <div class="composer-hints">
              <h3 class="composer-hints__title">写下此刻最真实的一句话</h3>
              <p class="composer-hints__text">{{ composerHint }}</p>
            </div>

            <div class="composer-input-wrapper">
              <textarea
                  v-model="composeForm.content"
                  class="composer-input"
                  :disabled="!canSend"
                  placeholder="例如：我最近总在夜里反复想同一件事..."
                  @keydown.enter.exact.prevent="sendMessage"
              />
              <button
                  class="submit-btn"
                  type="button"
                  :disabled="!canSend || !composeForm.content.trim()"
                  @click="sendMessage"
              >
                发送 ↵
              </button>
            </div>
          </div>
        </div>

        <div v-else class="state-container">
          <h2>没有找到对应的聊天室</h2>
          <p>请先确认预约已经进入可沟通状态，再重新打开本页面。</p>
        </div>
      </section>
    </div>
  </main>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700&family=Noto+Serif+SC:wght@500;600;700&display=swap');

.healing-chat-page {
  --page-bg: #eef2ef;
  --ink: #1e2821;
  --muted: #667268;
  --border: rgba(44, 48, 43, 0.08);
  --primary: #5b7e69;

  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 2.5vh 3vw;
  box-sizing: border-box;
  background: var(--page-bg);
  color: var(--ink);
  overflow: hidden; /* 防止整个页面滚动 */
}

/* 核心窗口布局 */
.app-window {
  display: flex;
  width: 100%;
  max-width: 1360px;
  height: 100%;
  max-height: 900px;
  background: #ffffff;
  border-radius: 24px;
  box-shadow: 0 32px 80px rgba(54, 66, 58, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.8);
  overflow: hidden;
}

/* ================= 左侧边栏 ================= */
.app-sidebar {
  width: 340px;
  flex-shrink: 0;
  background: linear-gradient(180deg, #f8faf8, #f0f4f1);
  border-right: 1px solid var(--border);
  display: flex;
  flex-direction: column;
  padding: 2rem;
  overflow-y: auto;
}

.app-sidebar::-webkit-scrollbar {
  width: 4px;
}
.app-sidebar::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.05);
  border-radius: 4px;
}

.sidebar-nav {
  margin-bottom: 2.5rem;
}

.nav-link {
  border: none;
  background: transparent;
  padding: 0;
  color: var(--muted);
  font-family: 'Noto Serif SC', serif;
  font-size: 0.95rem;
  font-weight: 600;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  transition: all 0.3s ease;
}

.nav-link:hover {
  color: var(--ink);
  transform: translateX(-4px);
}

.nav-link__arrow {
  font-family: 'Manrope', sans-serif;
}

.overline {
  margin: 0;
  color: var(--muted);
  font-family: 'Manrope', sans-serif;
  font-size: 0.76rem;
  font-weight: 700;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.sidebar-hero {
  margin-bottom: 2.5rem;
}

.hero-title {
  margin: 0.8rem 0 0;
  font-family: 'Noto Serif SC', serif;
  font-size: 2.2rem;
  font-weight: 600;
  line-height: 1.2;
}

.hero-summary {
  margin: 1rem 0 0;
  color: var(--muted);
  font-family: 'Noto Serif SC', serif;
  font-size: 0.96rem;
  line-height: 1.8;
}

.sidebar-info {
  display: flex;
  flex-direction: column;
  gap: 1.8rem;
  margin-top: auto;
}

.info-group {
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
}

.highlight-group {
  padding: 1.5rem;
  background: rgba(255, 255, 255, 0.6);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.8);
}

.body-text {
  margin: 0;
  font-family: 'Noto Serif SC', serif;
  font-size: 0.94rem;
  line-height: 1.8;
  color: var(--muted);
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  font-family: 'Manrope', sans-serif;
  font-size: 0.92rem;
  color: var(--muted);
}

.info-row strong {
  color: var(--ink);
  font-family: 'Noto Serif SC', serif;
  font-weight: 600;
}

/* ================= 右侧主工作区 ================= */
.app-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  position: relative;
  min-width: 0; /* 防止子元素撑破 flex 容器 */
}

.chat-workspace {
  display: flex;
  flex-direction: column;
  height: 100%;
}

/* 顶部状态栏 */
.chat-header {
  flex-shrink: 0;
  padding: 1.5rem 2.5rem;
  border-bottom: 1px solid var(--border);
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(8px);
  z-index: 10;
}

.header-title {
  margin: 0;
  font-family: 'Noto Serif SC', serif;
  font-size: 1.25rem;
  font-weight: 600;
}

.header-subtitle {
  margin: 0.25rem 0 0;
  color: var(--muted);
  font-family: 'Noto Serif SC', serif;
  font-size: 0.9rem;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  border-radius: 999px;
  font-family: 'Manrope', sans-serif;
  font-size: 0.85rem;
  font-weight: 700;
  letter-spacing: 0.05em;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}

.status-pill--idle, .status-pill--closed { background: #f0f2f0; color: #7a827b; }
.status-pill--connecting { background: #fff5eb; color: #a87e5b; }
.status-pill--connected { background: #eff6f1; color: var(--primary); }

/* 消息流区域 (弹性撑满、独立滚动) */
.message-viewport {
  flex: 1;
  overflow-y: auto;
  padding: 2.5rem;
  scroll-behavior: smooth;
}

.message-viewport::-webkit-scrollbar { width: 6px; }
.message-viewport::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.1);
  border-radius: 999px;
}

.message-stream {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.empty-stream {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  color: var(--muted);
}

.empty-stream h3 {
  margin: 1rem 0 0.5rem;
  font-family: 'Noto Serif SC', serif;
  font-size: 1.4rem;
  color: var(--ink);
}

/* 消息气泡样式 (保留原汁原味) */
.message-card {
  max-width: 80%;
  padding: 1.2rem 1.4rem;
  border-radius: 20px;
  animation: fadeIn 0.3s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.message-card--self {
  align-self: flex-end;
  background: linear-gradient(145deg, rgba(91, 126, 105, 1), rgba(118, 155, 130, 0.95));
  color: #fff;
  border-bottom-right-radius: 4px;
}

.message-card--peer {
  align-self: flex-start;
  background: #f8faf8;
  color: var(--ink);
  border: 1px solid var(--border);
  border-bottom-left-radius: 4px;
}

.message-card--system {
  align-self: center;
  background: transparent;
  color: var(--muted);
  font-size: 0.9rem;
  text-align: center;
  max-width: 60%;
}

.message-card__meta {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  margin-bottom: 0.6rem;
}

.message-card__meta-text {
  display: flex;
  align-items: baseline;
  gap: 0.5rem;
}

.message-card__meta-text span {
  font-family: 'Manrope', sans-serif;
  font-size: 0.8rem;
  font-weight: 700;
}

.message-card__meta-text time {
  font-family: 'Manrope', sans-serif;
  font-size: 0.75rem;
  opacity: 0.7;
}

.message-avatar {
  width: 2rem;
  height: 2rem;
  border-radius: 50%;
  object-fit: cover;
}

.message-avatar--placeholder {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.2);
  font-family: 'Noto Serif SC', serif;
  font-weight: 700;
  font-size: 0.9rem;
}

.message-card--peer .message-avatar--placeholder {
  background: #e9eee9;
  color: var(--primary);
}

.message-card__content {
  margin: 0;
  font-family: 'Noto Serif SC', serif;
  font-size: 1.05rem;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
}

/* 底部输入区 */
.composer-area {
  flex-shrink: 0;
  padding: 1.5rem 2.5rem 2.5rem;
  background: #fff;
  border-top: 1px solid var(--border);
  transition: opacity 0.3s;
}

.composer-area--disabled {
  opacity: 0.6;
  pointer-events: none;
}

.composer-hints {
  margin-bottom: 1rem;
}

.composer-hints__title {
  margin: 0 0 0.25rem;
  font-family: 'Noto Serif SC', serif;
  font-size: 1.05rem;
  font-weight: 600;
}

.composer-hints__text {
  margin: 0;
  color: var(--muted);
  font-size: 0.9rem;
  font-family: 'Noto Serif SC', serif;
}

.composer-input-wrapper {
  display: flex;
  gap: 1rem;
  align-items: flex-end;
}

.composer-input {
  flex: 1;
  min-height: 3rem;
  max-height: 8rem;
  padding: 0.8rem 1.2rem;
  border: 1px solid rgba(44, 48, 43, 0.15);
  border-radius: 16px;
  background: #fcfcfc;
  font-family: 'Noto Serif SC', serif;
  font-size: 1rem;
  line-height: 1.6;
  resize: none;
  outline: none;
  transition: all 0.3s ease;
}

.composer-input:focus {
  border-color: var(--primary);
  background: #fff;
  box-shadow: 0 4px 12px rgba(91, 126, 105, 0.08);
}

.submit-btn {
  height: 3.2rem;
  padding: 0 1.5rem;
  border: none;
  border-radius: 16px;
  background: var(--ink);
  color: #fff;
  font-family: 'Manrope', sans-serif;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.submit-btn:hover:not(:disabled) {
  background: var(--primary);
  transform: translateY(-2px);
}

.submit-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 异常与加载状态 */
.alert-banner {
  position: absolute;
  top: 1rem;
  left: 50%;
  transform: translateX(-50%);
  z-index: 20;
  margin: 0;
  padding: 0.75rem 1.5rem;
  background: #fee;
  color: #c0392b;
  border-radius: 999px;
  font-size: 0.9rem;
  box-shadow: 0 12px 24px rgba(192, 57, 43, 0.15);
}

.state-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 3rem;
}

.placeholder-orb {
  width: 4rem;
  height: 4rem;
  border-radius: 50%;
  background: linear-gradient(135deg, #e0e8e2, #f5f0eb);
  margin-bottom: 1.5rem;
  animation: pulse 2s infinite alternate;
}

@keyframes pulse {
  from { transform: scale(0.95); opacity: 0.8; }
  to { transform: scale(1.05); opacity: 1; }
}

.state-container h2 {
  font-family: 'Noto Serif SC', serif;
  margin: 0 0 0.5rem;
}

.state-container p {
  color: var(--muted);
  font-family: 'Noto Serif SC', serif;
}

/* 响应式设计：移动端降级为上下结构 */
@media (max-width: 900px) {
  .healing-chat-page {
    padding: 0;
  }

  .app-window {
    flex-direction: column;
    border-radius: 0;
    max-height: none;
    border: none;
  }

  .app-sidebar {
    width: 100%;
    height: auto;
    max-height: 35vh;
    border-right: none;
    border-bottom: 1px solid var(--border);
    padding: 1.5rem;
  }

  .sidebar-hero {
    margin-bottom: 1.5rem;
  }

  .hero-title {
    font-size: 1.8rem;
  }

  .chat-header, .composer-area, .message-viewport {
    padding: 1.25rem;
  }

  .message-card {
    max-width: 90%;
  }
}
</style>
