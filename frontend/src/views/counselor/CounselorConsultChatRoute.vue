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
    errorMessage.value = 'Missing token'
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
    errorMessage.value = 'WebSocket error'
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
  <section class="c-chat-page">
    <div class="page-shell">
      <header class="page-hero">
        <div class="hero-copy">
          <p class="eyebrow">Consult Chat</p>
          <h1>在预约关联的私密聊天室中继续完成文字支持与跟进记录。</h1>
          <p class="lead">聊天室会先拉取历史消息，再建立 WebSocket Socket进行实时同步。</p>
        </div>
        <div class="hero-metrics">
          <div class="metric-card">
            <span>预约编号</span>
            <strong>#{{ appointmentId || '-' }}</strong>
          </div>
          <div class="metric-card">
            <span>连接状态</span>
            <strong>{{ socketState }}</strong>
          </div>
        </div>
      </header>

      <div class="chat-grid">
        <section class="conversation-panel glass-panel">
          <div class="section-head section-head-inline">
            <div>
              <p class="section-kicker">History</p>
              <h2>消息记录</h2>
            </div>
            <span class="status-chip">{{ chatSession?.status || '未初始化' }}</span>
          </div>
          <p v-if="loading" class="state-text">正在同步聊天室上下文...</p>
          <p v-else-if="!messages.length" class="state-text">当前聊天室暂无消息。</p>
          <div v-else class="message-stack">
            <article v-for="message in messages" :key="message.messageId" class="message-card" :class="{ 'message-card--self': message.senderType === 'COUNSELOR' }">
              <div class="message-meta">
                <span>{{ message.senderType }}</span>
                <span>{{ new Date(message.createdAt).toLocaleString('zh-CN') }}</span>
              </div>
              <p class="message-content">{{ message.content }}</p>
            </article>
          </div>
          <p v-if="errorMessage" class="error-text">{{ errorMessage }}</p>
        </section>

        <aside class="compose-panel glass-panel">
          <div class="section-head">
            <p class="section-kicker">Compose</p>
            <h2>发送消息</h2>
          </div>
          <dl class="session-meta-list">
            <div><dt>聊天室编号</dt><dd>{{ chatSession?.chatSessionId || '-' }}</dd></div>
            <div><dt>是否封存</dt><dd>{{ chatSession?.sealed ? '是' : '否' }}</dd></div>
          </dl>
          <textarea v-model="composeForm.content" class="compose-textarea" rows="10" maxlength="2000" placeholder="向学生发送后续说明、关怀或下一步建议。" />
          <button class="primary-button" type="button" @click="sendMessage">发送文字消息</button>
        </aside>
      </div>
    </div>
  </section>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700&family=Noto+Serif+SC:wght@400;500;600;700&display=swap');

.c-chat-page{min-height:100vh;padding:44px 28px 72px;color:#283128;background:linear-gradient(180deg,#f5f0e5 0%,#f8f4ed 100%)}
.page-shell{max-width:1320px;margin:0 auto}.page-hero{display:grid;grid-template-columns:minmax(0,1.3fr) minmax(240px,.7fr);gap:28px;align-items:end;margin-bottom:30px}.hero-copy{border-top:1px solid rgba(59,69,59,.16);padding-top:18px}.eyebrow,.section-kicker,.session-meta-list dt{margin:0 0 10px;font:700 .76rem/1 'Manrope',sans-serif;letter-spacing:.22em;text-transform:uppercase;color:#7b6857}.hero-copy h1,.section-head h2{margin:0;font-family:'Noto Serif SC',serif;font-weight:600}.hero-copy h1{font-size:clamp(2rem,3vw,3.25rem);line-height:1.16}.lead,.message-meta,.message-content,.error-text,.state-text,.session-meta-list dd,.compose-textarea{font-family:'Manrope',sans-serif}.lead{margin:18px 0 0;line-height:1.84;color:rgba(40,49,40,.72)}.hero-metrics{display:grid;gap:14px}.metric-card,.glass-panel,.message-card{border:1px solid rgba(77,86,77,.14);background:rgba(255,252,247,.76);box-shadow:0 24px 70px rgba(91,80,66,.08);backdrop-filter:blur(16px)}.metric-card{padding:18px 20px}.metric-card span,.status-chip{display:block;margin-bottom:8px;font:700 .78rem/1 'Manrope',sans-serif;letter-spacing:.16em;text-transform:uppercase;color:rgba(68,74,66,.56)}.metric-card strong{font:600 1.6rem/1 'Noto Serif SC',serif}.chat-grid{display:grid;grid-template-columns:minmax(0,1.2fr) minmax(320px,.8fr);gap:28px}.conversation-panel,.compose-panel{padding:24px}.section-head{margin-bottom:18px}.section-head-inline{display:flex;justify-content:space-between;align-items:end;gap:16px}.status-chip{border:1px solid rgba(88,93,84,.14);background:rgba(255,250,240,.82);padding:9px 14px;color:#696152}.message-stack{display:grid;gap:16px}.message-card{padding:18px}.message-card--self{margin-left:48px}.message-meta{display:flex;flex-wrap:wrap;gap:10px 14px;font-size:.82rem;color:rgba(40,49,40,.58);margin-bottom:10px}.message-content{margin:0;white-space:pre-wrap;font-size:.98rem;line-height:1.92;color:#283128}.session-meta-list{display:grid;gap:12px;margin:0 0 18px}.session-meta-list dd{margin:6px 0 0;font-size:.96rem;color:rgba(40,49,40,.72)}.compose-textarea{width:100%;border:1px solid rgba(80,88,79,.16);background:rgba(255,255,255,.74);padding:14px 16px;resize:vertical;color:#283128;outline:none}.primary-button{margin-top:16px;border:none;background:linear-gradient(135deg,#253128 0%,#47564b 100%);color:#f8f5ef;padding:14px 18px;font:700 .84rem/1 'Manrope',sans-serif;letter-spacing:.08em;text-transform:uppercase;cursor:pointer}.error-text{margin-top:16px;color:#a44f46}
@media (max-width:980px){.c-chat-page{padding:28px 16px 46px}.page-hero,.chat-grid{grid-template-columns:1fr}.message-card--self,.section-head-inline{margin-left:0;flex-direction:column;align-items:start}}
</style>

