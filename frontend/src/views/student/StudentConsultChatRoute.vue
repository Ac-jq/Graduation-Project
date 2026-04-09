<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
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
const composeForm = reactive({
  content: ''
})
const socketRef = ref<WebSocket | null>(null)
const appointmentId = computed(() => toNumberParam(route.params.appointmentId))
const canSend = computed(() => {
  if (!chatSession.value) {
    return false
  }

  return !chatSession.value.sealed && chatSession.value.status !== 'ARCHIVED' && socketState.value === 'connected'
})
const socketStateLabel = computed(() => {
  switch (socketState.value) {
    case 'connecting':
      return 'Socket中'
    case 'connected':
      return '已Socket'
    case 'closed':
      return '已断开'
    default:
      return '未建立Socket'
  }
})

function formatDate(value: string | null): string {
  if (!value) {
    return '未设置'
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
      return '学生'
    case 'COUNSELOR':
      return '咨询师'
    case 'SYSTEM':
      return '系统'
    default:
      return senderType
  }
}

function disconnectSocket(): void {
  socketRef.value?.close()
  socketRef.value = null
  socketState.value = 'closed'
}

function connectSocket(): void {
  if (!appointmentId.value) {
    errorMessage.value = '无效的预约编号。'
    return
  }

  const token = getToken()
  if (!token) {
    errorMessage.value = '缺少登录令牌。'
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
    errorMessage.value = 'WebSocket Socket异常。'
    socketState.value = 'closed'
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
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    loading.value = false
  }
}

function sendMessage(): void {
  if (!composeForm.content.trim() || socketRef.value?.readyState !== WebSocket.OPEN) {
    return
  }

  socketRef.value.send(JSON.stringify({ content: composeForm.content.trim() }))
  composeForm.content = ''
}

watch(
  () => route.params.appointmentId,
  () => {
    void loadChatContext()
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
  <main class="chat-page">
    <section class="chat-page__masthead">
      <div class="chat-page__heading">
        <p class="chat-page__eyebrow">Private Consultation Room</p>
        <h1 class="chat-page__title">私密聊天室</h1>
        <p class="chat-page__summary">
          这里承接预约后的私密沟通。系统会先拉取历史消息，再建立 WebSocket 连接进行实时收发。
        </p>
      </div>

      <aside class="chat-page__snapshot" v-if="chatSession">
        <p class="chat-page__label">Session Snapshot</p>
        <dl>
          <div>
            <dt>预约编号</dt>
            <dd>#{{ chatSession.appointmentId }}</dd>
          </div>
          <div>
            <dt>会话状态</dt>
            <dd>{{ chatSession.status }}</dd>
          </div>
          <div>
            <dt>Socket</dt>
            <dd>{{ socketStateLabel }}</dd>
          </div>
          <div>
            <dt>Sealed</dt>
            <dd>{{ chatSession.sealed ? '是' : '否' }}</dd>
          </div>
        </dl>
      </aside>
    </section>

    <p v-if="errorMessage" class="chat-page__alert">{{ errorMessage }}</p>

    <section v-if="loading" class="chat-page__status-panel">
      <p>正在加载聊天室上下文...</p>
    </section>

    <template v-else-if="chatSession">
      <section class="chat-page__meta-strip">
        <div>
          <p class="chat-page__label">Open Time</p>
          <strong>{{ formatDate(chatSession.openTime) }}</strong>
        </div>
        <div>
          <p class="chat-page__label">Close Time</p>
          <strong>{{ formatDate(chatSession.closeTime) }}</strong>
        </div>
        <div>
          <p class="chat-page__label">Messages</p>
          <strong>{{ messages.length }}</strong>
        </div>
      </section>

      <section class="chat-page__conversation">
        <div v-if="messages.length" class="chat-page__message-list">
          <article
            v-for="message in messages"
            :key="message.messageId"
            class="chat-message"
            :class="{ 'chat-message--self': message.senderType === 'STUDENT' }"
          >
            <header class="chat-message__header">
              <span>{{ resolveSenderLabel(message.senderType) }}</span>
              <time>{{ formatDate(message.createdAt) }}</time>
            </header>
            <p>{{ message.content }}</p>
          </article>
        </div>

        <div v-else class="chat-page__empty-state">
          <p>当前没有历史消息。</p>
        </div>
      </section>

      <section class="chat-page__composer" :class="{ 'chat-page__composer--disabled': !canSend }">
        <div class="chat-page__composer-copy">
          <p class="chat-page__label">Message Composer</p>
          <p>
            {{ canSend ? '连接已建立，可以发送新消息。' : '当前聊天室不可发送消息，请检查连接状态或会话是否已封存。' }}
          </p>
        </div>
        <textarea
          v-model="composeForm.content"
          :disabled="!canSend"
          placeholder="输入你想发送的文字消息。"
          @keydown.enter.exact.prevent="sendMessage"
        />
        <button class="chat-page__primary" type="button" :disabled="!canSend || !composeForm.content.trim()" @click="sendMessage">
          发送消息
        </button>
      </section>
    </template>

    <section v-else class="chat-page__status-panel">
      <p>未找到对应聊天室上下文。</p>
    </section>
  </main>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700&family=Noto+Serif+SC:wght@400;500;600;700&display=swap');

.chat-page {
  --paper: #f4efe5;
  --ink: #201c18;
  --muted: #6e665f;
  --line: rgba(32, 28, 24, 0.12);
  --glass: rgba(255, 251, 245, 0.68);
  --accent: #667f6f;
  min-height: 100vh;
  padding: 2rem;
  color: var(--ink);
  background:
    radial-gradient(circle at top right, rgba(114, 136, 121, 0.18), transparent 26%),
    radial-gradient(circle at left center, rgba(198, 186, 168, 0.22), transparent 30%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.14), transparent 38%),
    var(--paper);
}

.chat-page__masthead {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(300px, 0.85fr);
  gap: 1.5rem;
  align-items: end;
  padding-bottom: 1.4rem;
  border-bottom: 1px solid var(--line);
}

.chat-page__eyebrow,
.chat-page__label,
.chat-page__snapshot dt,
.chat-message__header span,
.chat-message__header time {
  margin: 0;
  font: 600 0.72rem/1.4 'Manrope', sans-serif;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--muted);
}

.chat-page__title {
  margin: 0.95rem 0 0;
  font: 600 clamp(2.8rem, 5vw, 5.1rem)/0.98 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.chat-page__summary {
  max-width: 46rem;
  margin: 1rem 0 0;
  color: var(--muted);
  font: 400 1rem/1.9 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.chat-page__snapshot,
.chat-page__meta-strip,
.chat-page__conversation,
.chat-page__composer,
.chat-page__status-panel,
.chat-message {
  border: 1px solid var(--line);
  background: var(--glass);
  backdrop-filter: blur(18px);
  box-shadow: 0 22px 48px rgba(80, 70, 58, 0.08);
}

.chat-page__snapshot {
  padding: 1.2rem;
}

.chat-page__snapshot dl {
  display: grid;
  gap: 0.9rem;
  margin: 1rem 0 0;
}

.chat-page__snapshot dd,
.chat-page__meta-strip strong {
  margin: 0.35rem 0 0;
  font: 600 1.04rem/1.45 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.chat-page__alert {
  margin: 1.25rem 0 0;
  color: #8d4747;
  font: 600 0.9rem/1.6 'Manrope', sans-serif;
}

.chat-page__meta-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 1rem;
  margin-top: 1.5rem;
  padding: 1rem 1.2rem;
}

.chat-page__conversation {
  margin-top: 1.5rem;
  padding: 1.2rem;
}

.chat-page__message-list {
  display: grid;
  gap: 1rem;
}

.chat-message {
  display: grid;
  gap: 0.8rem;
  padding: 1rem;
}

.chat-message--self {
  border-color: rgba(102, 127, 111, 0.36);
  background: linear-gradient(180deg, rgba(102, 127, 111, 0.12), rgba(255, 251, 245, 0.74));
}

.chat-message__header {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
}

.chat-message p,
.chat-page__composer-copy p:last-child,
.chat-page__status-panel p,
.chat-page__empty-state p {
  margin: 0;
  color: var(--muted);
  font: 400 0.98rem/1.85 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.chat-page__empty-state {
  padding: 0.5rem 0;
}

.chat-page__composer {
  display: grid;
  gap: 1rem;
  margin-top: 1.5rem;
  padding: 1.2rem;
}

.chat-page__composer--disabled {
  opacity: 0.8;
}

.chat-page__composer textarea {
  min-height: 9rem;
  resize: vertical;
  padding: 1rem;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.5);
  color: var(--ink);
  font: 400 1rem/1.7 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.chat-page__primary {
  justify-self: start;
  min-height: 3rem;
  padding: 0 1.15rem;
  border: none;
  background: linear-gradient(135deg, #6b8473, #4f6656);
  color: #faf6f0;
  font: 600 0.84rem/1 'Manrope', sans-serif;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  cursor: pointer;
  box-shadow: 0 18px 36px rgba(79, 102, 86, 0.24);
  transition: transform 180ms ease, box-shadow 180ms ease;
}

.chat-page__primary:hover:not(:disabled) {
  transform: translateY(-2px);
}

.chat-page__primary:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.chat-page__status-panel {
  margin-top: 1.5rem;
  padding: 1.35rem;
}

@media (max-width: 900px) {
  .chat-page,
  .notification-page {
    padding: 1rem;
  }

  .chat-page__masthead,
  .chat-page__meta-strip,
  .chat-message__header,
  .notification-page__masthead,
  .notification-page__toolbar,
  .notification-card__header,
  .notification-card__footer {
    grid-template-columns: 1fr;
    flex-direction: column;
    align-items: flex-start;
  }

  .chat-page__meta-strip {
    grid-template-columns: 1fr;
  }

  .chat-page__primary {
    width: 100%;
    justify-self: stretch;
  }
}
</style>

