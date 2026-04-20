<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { buildConsultChatWebSocketUrl, fetchConsultChatMessagesApi, fetchConsultChatSessionApi } from '@/api/chat'
import type { ConsultChatMessage, ConsultChatSession, ConsultChatSocketPayload } from '@/api/types'
import { getToken } from '@/core/session'
import { toErrorMessage, toNumberParam } from '@/views/shared/page-logic'

const route = useRoute()

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
      return '已连接'
    case 'closed':
      return '已断开'
    default:
      return '未连接'
  }
})
const roomAvailabilityText = computed(() => {
  if (!chatSession.value) {
    return '正在读取会话状态'
  }
  if (chatSession.value.sealed || chatSession.value.status === 'CLOSED') {
    return '聊天室已结束'
  }
  if (chatSession.value.status === 'ARCHIVED') {
    return '聊天室已归档'
  }
  if (socketState.value !== 'connected') {
    return '正在建立连接'
  }
  return peerOnline.value ? '双方已上线，可以开始聊天' : '你已进入聊天室，正在等待对方上线'
})
const composerDescription = computed(() => {
  if (!chatSession.value) {
    return '正在加载聊天室信息。'
  }
  if (chatSession.value.sealed || chatSession.value.status === 'CLOSED') {
    return '聊天室已经结束，当前只能查看历史记录。'
  }
  if (chatSession.value.status === 'ARCHIVED') {
    return '聊天室已经归档，当前不能继续发送消息。'
  }
  if (socketState.value !== 'connected') {
    return '正在连接聊天室，请稍候。'
  }
  if (!peerOnline.value) {
    return '你已经进入聊天室，系统会在对方上线后自动开放输入。'
  }
  return '对方已经上线，你可以继续和咨询师交流。'
})

function formatDate(value: string | null): string {
  if (!value) {
    return '暂无记录'
  }

  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value))
}

function resolveSenderLabel(senderType: string): string {
  switch (senderType) {
    case 'STUDENT':
      return '我'
    case 'COUNSELOR':
      return '咨询老师'
    case 'SYSTEM':
      return '系统'
    default:
      return senderType
  }
}

function resolveMessageClass(senderType: string): string {
  if (senderType === 'STUDENT') {
    return 'chat-bubble chat-bubble--self'
  }
  if (senderType === 'COUNSELOR') {
    return 'chat-bubble chat-bubble--peer'
  }
  return 'chat-bubble chat-bubble--system'
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
    if (payload.tip) {
      errorMessage.value = ''
    }
    return
  }

  if (payload.type === 'MESSAGE' && payload.message) {
    messages.value = [...messages.value, payload.message]
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
    if (payload.tip) {
      errorMessage.value = ''
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
    errorMessage.value = '实时连接出现异常，请稍后重试。'
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
    messages.value = history
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
    <div class="healing-chat-shell">
      <header class="healing-chat-hero">
        <div class="healing-chat-copy">
          <p class="healing-chat-eyebrow">学生私密交流空间</p>
          <h1 class="healing-chat-title">把想说的话，轻轻放在这里。</h1>
          <p class="healing-chat-summary">
            这里不是任务面板，也不是冷冰冰的工单窗口。它更像一间被安静留白包裹的谈话室，
            你可以在这里继续预约后的交流，让问题慢一点展开，让情绪有地方落下。
          </p>
        </div>

        <div v-if="chatSession" class="healing-chat-hero-card">
          <p class="hero-card__label">本次会话</p>
          <div class="hero-card__row">
            <span>预约编号</span>
            <strong>#{{ chatSession.appointmentId }}</strong>
          </div>
          <div class="hero-card__row">
            <span>当前状态</span>
            <strong>{{ roomAvailabilityText }}</strong>
          </div>
          <div class="hero-card__row">
            <span>实时连接</span>
            <strong>{{ socketStateLabel }}</strong>
          </div>
        </div>
      </header>

      <p v-if="errorMessage" class="healing-chat-alert">{{ errorMessage }}</p>

      <section v-if="loading" class="healing-chat-status">
        <div class="status-orb"></div>
        <h2>正在进入谈话室</h2>
        <p>会话资料、历史消息与实时连接正在同步。</p>
      </section>

      <section v-else-if="chatSession" class="healing-chat-workspace">
        <aside class="healing-chat-sidebar">
          <section class="floating-panel floating-panel--mesh">
            <p class="panel-eyebrow">交流概览</p>
            <h2 class="panel-title">这是一段预约后的继续对话。</h2>
            <p class="panel-body">
              你不需要一次把所有话都说完。可以从此刻最明显的情绪、最绕不过去的困扰，
              或者最想被理解的一件小事开始。
            </p>
          </section>

          <section class="floating-panel">
            <div class="metric-grid">
              <article class="metric-card">
                <span>开放时间</span>
                <strong>{{ formatDate(chatSession.openTime) }}</strong>
              </article>
              <article class="metric-card">
                <span>结束时间</span>
                <strong>{{ formatDate(chatSession.closeTime) }}</strong>
              </article>
              <article class="metric-card">
                <span>消息数量</span>
                <strong>{{ messageCount }}</strong>
              </article>
              <article class="metric-card">
                <span>对方状态</span>
                <strong>{{ peerOnline ? '已上线' : '等待加入' }}</strong>
              </article>
            </div>
          </section>

          <section class="floating-panel">
            <p class="panel-eyebrow">交流提醒</p>
            <ul class="gentle-list">
              <li>尽量描述具体情境，而不是只说“我很难受”。</li>
              <li>如果暂时不知道怎么表达，可以先说身体感受、睡眠变化或最近反复出现的念头。</li>
              <li>当对方暂时离线时，系统会自动切换为等待状态，不需要手动刷新页面。</li>
            </ul>
          </section>
        </aside>

        <section class="healing-chat-main">
          <section class="floating-panel floating-panel--conversation">
            <header class="conversation-header">
              <div>
                <p class="panel-eyebrow">正在交流</p>
                <h2 class="conversation-title">安静地说，也可以慢慢说。</h2>
              </div>
              <div class="conversation-status" :class="`conversation-status--${socketState}`">
                {{ socketStateLabel }}
              </div>
            </header>

            <div ref="messageViewportRef" class="message-viewport">
              <div v-if="messages.length" class="message-stream">
                <article
                  v-for="message in messages"
                  :key="message.messageId"
                  :class="resolveMessageClass(message.senderType)"
                >
                  <div class="chat-bubble__meta">
                    <span>{{ resolveSenderLabel(message.senderType) }}</span>
                    <time>{{ formatDate(message.createdAt) }}</time>
                  </div>
                  <p class="chat-bubble__content">{{ message.content }}</p>
                </article>
              </div>

              <div v-else class="empty-conversation">
                <p class="panel-eyebrow">还没有历史消息</p>
                <h3>第一句话，不需要很完整。</h3>
                <p>可以从“我最近睡得不好”或“我对明天有点害怕”这样的一句话开始。</p>
              </div>
            </div>
          </section>

          <section class="floating-panel floating-panel--composer" :class="{ 'floating-panel--disabled': !canSend }">
            <div class="composer-copy">
              <p class="panel-eyebrow">发送新消息</p>
              <h2 class="composer-title">把此刻最真实的一句话写下来。</h2>
              <p class="composer-description">
                {{ composerDescription }}
              </p>
            </div>

            <textarea
              v-model="composeForm.content"
              :disabled="!canSend"
              class="composer-input"
              placeholder="例如：我最近总在夜里反复想同一件事，白天很难集中注意力。"
              @keydown.enter.exact.prevent="sendMessage"
            />

            <div class="composer-actions">
              <button
                class="composer-submit"
                type="button"
                :disabled="!canSend || !composeForm.content.trim()"
                @click="sendMessage"
              >
                发送这句话
              </button>
              <p class="composer-hint">按 Enter 发送，Shift + Enter 可换行。</p>
            </div>
          </section>
        </section>
      </section>

      <section v-else class="healing-chat-status">
        <h2>没有找到对应的谈话室</h2>
        <p>请先确认预约是否已经进入可交流状态，再重新打开此页面。</p>
      </section>
    </div>
  </main>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700&family=Noto+Serif+SC:wght@500;600;700&display=swap');

.healing-chat-page {
  --chat-bg: #fcfbfa;
  --chat-ink: #1e2821;
  --chat-muted: #667268;
  --chat-soft: rgba(42, 54, 46, 0.08);
  --chat-panel: linear-gradient(145deg, rgba(255, 255, 255, 0.75), rgba(248, 246, 242, 0.85));
  --chat-shadow: 0 40px 80px rgba(54, 66, 58, 0.06);
  --chat-highlight: #34453a;
  --chat-sage: #91a693;
  --chat-sand: #d8b79d;
  min-height: 100vh;
  padding: clamp(2rem, 4vw, 4rem);
  background:
    radial-gradient(circle at top left, rgba(145, 166, 147, 0.16), transparent 24%),
    radial-gradient(circle at 85% 18%, rgba(216, 183, 157, 0.16), transparent 20%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.45), rgba(252, 251, 250, 0.92)),
    var(--chat-bg);
  color: var(--chat-ink);
  box-sizing: border-box;
}

.healing-chat-shell {
  max-width: 1420px;
  margin: 0 auto;
  display: grid;
  gap: 2rem;
}

.healing-chat-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(320px, 0.8fr);
  gap: 2rem;
  align-items: end;
}

.healing-chat-copy {
  padding: clamp(1rem, 2vw, 1.75rem) 0;
}

.healing-chat-eyebrow,
.panel-eyebrow,
.hero-card__label,
.metric-card span,
.chat-bubble__meta span,
.chat-bubble__meta time,
.composer-hint {
  margin: 0;
  color: var(--chat-muted);
  font-family: 'Manrope', sans-serif;
  font-size: 0.76rem;
  font-weight: 600;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.healing-chat-title {
  margin: 1rem 0 0;
  color: var(--chat-ink);
  font-family: 'Noto Serif SC', serif;
  font-size: clamp(2.7rem, 5vw, 5rem);
  font-weight: 600;
  line-height: 1.04;
  letter-spacing: 0.02em;
}

.healing-chat-summary {
  max-width: 46rem;
  margin: 1.4rem 0 0;
  color: var(--chat-muted);
  font-family: 'Noto Serif SC', serif;
  font-size: 1.06rem;
  line-height: 1.95;
}

.healing-chat-hero-card,
.floating-panel,
.healing-chat-status {
  background: var(--chat-panel);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border: 1px solid rgba(255, 255, 255, 0.8);
  border-radius: 28px;
  box-shadow: var(--chat-shadow);
}

.healing-chat-hero-card {
  padding: 2rem;
  display: grid;
  gap: 1rem;
}

.hero-card__row {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: baseline;
  color: var(--chat-muted);
  font-family: 'Manrope', sans-serif;
  font-size: 0.98rem;
}

.hero-card__row strong {
  color: var(--chat-ink);
  font-family: 'Noto Serif SC', serif;
  font-size: 1.08rem;
  font-weight: 600;
  text-align: right;
}

.healing-chat-alert {
  margin: 0;
  padding: 1rem 1.3rem;
  border-radius: 18px;
  background: rgba(164, 92, 92, 0.1);
  color: #835252;
  font-family: 'Manrope', sans-serif;
  font-size: 0.94rem;
  line-height: 1.7;
}

.healing-chat-status {
  padding: 3rem;
  text-align: center;
}

.healing-chat-status h2 {
  margin: 1rem 0 0;
  color: var(--chat-ink);
  font-family: 'Noto Serif SC', serif;
  font-size: clamp(1.8rem, 3vw, 2.5rem);
  font-weight: 600;
}

.healing-chat-status p {
  margin: 0.9rem auto 0;
  max-width: 34rem;
  color: var(--chat-muted);
  font-family: 'Noto Serif SC', serif;
  font-size: 1rem;
  line-height: 1.9;
}

.status-orb {
  width: 5rem;
  height: 5rem;
  margin: 0 auto;
  border-radius: 999px;
  background:
    radial-gradient(circle at 35% 35%, rgba(255, 255, 255, 0.9), transparent 30%),
    linear-gradient(145deg, rgba(145, 166, 147, 0.85), rgba(216, 183, 157, 0.9));
  box-shadow: 0 24px 48px rgba(116, 136, 122, 0.2);
}

.healing-chat-workspace {
  display: grid;
  grid-template-columns: minmax(300px, 0.72fr) minmax(0, 1.28fr);
  gap: 2rem;
  align-items: start;
}

.healing-chat-sidebar,
.healing-chat-main {
  display: grid;
  gap: 1.5rem;
}

.floating-panel {
  padding: 2rem;
}

.floating-panel--mesh {
  background:
    radial-gradient(circle at 18% 25%, rgba(146, 170, 156, 0.42), transparent 34%),
    radial-gradient(circle at 85% 18%, rgba(220, 181, 151, 0.42), transparent 28%),
    linear-gradient(145deg, rgba(255, 255, 255, 0.72), rgba(248, 246, 242, 0.82));
}

.panel-title,
.conversation-title,
.composer-title,
.empty-conversation h3 {
  margin: 0.85rem 0 0;
  color: var(--chat-ink);
  font-family: 'Noto Serif SC', serif;
  font-size: clamp(1.55rem, 2.6vw, 2.3rem);
  font-weight: 600;
  line-height: 1.28;
}

.panel-body,
.gentle-list,
.composer-description,
.empty-conversation p {
  margin: 1rem 0 0;
  color: var(--chat-muted);
  font-family: 'Noto Serif SC', serif;
  font-size: 0.98rem;
  line-height: 1.9;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
}

.metric-card {
  padding: 1.2rem;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.46);
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.metric-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 24px 40px rgba(54, 66, 58, 0.08);
}

.metric-card strong {
  display: block;
  margin-top: 0.55rem;
  color: var(--chat-ink);
  font-family: 'Noto Serif SC', serif;
  font-size: 1.04rem;
  font-weight: 600;
  line-height: 1.6;
}

.gentle-list {
  padding-left: 1.2rem;
}

.gentle-list li + li {
  margin-top: 0.7rem;
}

.floating-panel--conversation,
.floating-panel--composer {
  padding: 2.1rem;
}

.conversation-header {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: flex-start;
}

.conversation-status {
  min-width: 7.5rem;
  padding: 0.8rem 1rem;
  border-radius: 999px;
  text-align: center;
  font-family: 'Manrope', sans-serif;
  font-size: 0.84rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.conversation-status--idle,
.conversation-status--closed {
  background: rgba(120, 127, 122, 0.12);
  color: #617066;
}

.conversation-status--connecting {
  background: rgba(216, 183, 157, 0.18);
  color: #8a6854;
}

.conversation-status--connected {
  background: rgba(145, 166, 147, 0.18);
  color: #48604f;
}

.message-viewport {
  max-height: 58vh;
  margin-top: 1.8rem;
  overflow-y: auto;
  padding-right: 0.45rem;
}

.message-viewport::-webkit-scrollbar {
  width: 8px;
}

.message-viewport::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: rgba(52, 69, 58, 0.14);
}

.message-stream {
  display: grid;
  gap: 1rem;
}

.chat-bubble {
  max-width: min(82%, 40rem);
  padding: 1.25rem 1.35rem;
  border-radius: 24px;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.chat-bubble:hover {
  transform: translateY(-4px);
  box-shadow: 0 24px 42px rgba(54, 66, 58, 0.08);
}

.chat-bubble--self {
  margin-left: auto;
  background: linear-gradient(145deg, rgba(60, 81, 68, 0.92), rgba(84, 108, 92, 0.82));
  color: #f8f6f2;
}

.chat-bubble--peer {
  margin-right: auto;
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.92), rgba(246, 241, 235, 0.9));
  color: var(--chat-ink);
}

.chat-bubble--system {
  margin: 0 auto;
  background: rgba(216, 183, 157, 0.16);
  color: #6f5b4f;
}

.chat-bubble__meta {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
}

.chat-bubble--self .chat-bubble__meta span,
.chat-bubble--self .chat-bubble__meta time {
  color: rgba(248, 246, 242, 0.8);
}

.chat-bubble__content {
  margin: 0.75rem 0 0;
  font-family: 'Noto Serif SC', serif;
  font-size: 1rem;
  line-height: 1.9;
  white-space: pre-wrap;
}

.empty-conversation {
  min-height: 16rem;
  display: grid;
  place-items: center;
  text-align: center;
  padding: 2rem;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.42);
}

.composer-copy {
  max-width: 42rem;
}

.composer-input {
  width: 100%;
  min-height: 10rem;
  margin-top: 1.6rem;
  padding: 1.35rem 1.45rem;
  box-sizing: border-box;
  border: 1px solid rgba(255, 255, 255, 0.8);
  border-radius: 24px;
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.78), rgba(248, 246, 242, 0.82));
  color: var(--chat-ink);
  font-family: 'Noto Serif SC', serif;
  font-size: 1rem;
  line-height: 1.9;
  resize: vertical;
  outline: none;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.composer-input:focus {
  transform: translateY(-4px);
  box-shadow: 0 28px 52px rgba(54, 66, 58, 0.08);
}

.composer-input:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.composer-actions {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
  margin-top: 1.3rem;
}

.composer-submit {
  min-height: 3.5rem;
  padding: 0 1.7rem;
  border: none;
  border-radius: 999px;
  background: linear-gradient(135deg, #34453a, #516656);
  color: #f7f3ec;
  font-family: 'Manrope', sans-serif;
  font-size: 0.9rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  cursor: pointer;
  box-shadow: 0 28px 46px rgba(52, 69, 58, 0.18);
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.composer-submit:hover:not(:disabled) {
  transform: translateY(-4px);
  box-shadow: 0 34px 56px rgba(52, 69, 58, 0.22);
}

.composer-submit:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  box-shadow: none;
}

.floating-panel--disabled {
  opacity: 0.82;
}

@media (max-width: 1080px) {
  .healing-chat-hero,
  .healing-chat-workspace {
    grid-template-columns: 1fr;
  }

  .message-viewport {
    max-height: none;
  }
}

@media (max-width: 720px) {
  .healing-chat-page {
    padding: 1rem;
  }

  .healing-chat-shell {
    gap: 1rem;
  }

  .floating-panel,
  .healing-chat-hero-card,
  .healing-chat-status {
    padding: 1.4rem;
    border-radius: 22px;
  }

  .metric-grid {
    grid-template-columns: 1fr;
  }

  .conversation-header,
  .composer-actions {
    flex-direction: column;
    align-items: flex-start;
  }

  .composer-submit {
    width: 100%;
  }

  .chat-bubble {
    max-width: 100%;
  }
}
</style>
