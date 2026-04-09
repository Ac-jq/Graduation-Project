<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { fetchStudentAiSessionMessagesApi, sendStudentAiChatMessageApi } from '@/api/ai-chat'
import type { AiChatMessage, SendAiChatMessageResponse } from '@/api/types'
import { toErrorMessage, toNumberParam } from '@/views/shared/page-logic'

const route = useRoute()
const loading = ref(false)
const sending = ref(false)
const errorMessage = ref('')
const messages = ref<AiChatMessage[]>([])
const sendResult = ref<SendAiChatMessageResponse | null>(null)
const composeForm = reactive({
  content: ''
})
const sessionId = computed(() => toNumberParam(route.params.sessionId))

async function loadMessages(): Promise<void> {
  if (!sessionId.value) {
    errorMessage.value = 'Invalid sessionId'
    messages.value = []
    return
  }

  loading.value = true
  errorMessage.value = ''

  try {
    messages.value = await fetchStudentAiSessionMessagesApi(sessionId.value)
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    loading.value = false
  }
}

async function sendMessage(): Promise<void> {
  if (!sessionId.value) {
    errorMessage.value = 'Invalid sessionId'
    return
  }

  sending.value = true
  errorMessage.value = ''

  try {
    sendResult.value = await sendStudentAiChatMessageApi(sessionId.value, { content: composeForm.content })
    composeForm.content = ''
    await loadMessages()
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    sending.value = false
  }
}

watch(() => route.params.sessionId, () => {
  void loadMessages()
})

onMounted(() => {
  void loadMessages()
})
</script>

<template>
  <section class="ai-chat-page">
    <div class="chat-shell">
      <header class="chat-hero">
        <div class="hero-copy">
          <p class="eyebrow">Reflective Dialogue</p>
          <h1>把脑海里的碎片化念头，整理成一段能被倾听的叙述。</h1>
          <p class="lead">
            你的每条消息都来自真实后端Conversation Archive。发送后，系统会即时返回 AI 回应并刷新完整消息流。
          </p>
        </div>
        <div class="hero-aside">
          <div class="metric-card">
            <span>会话编号</span>
            <strong>#{{ sessionId ?? '-' }}</strong>
          </div>
          <div class="metric-card">
            <span>Messages量</span>
            <strong>{{ messages.length }}</strong>
          </div>
        </div>
      </header>

      <div class="chat-grid">
        <section class="conversation-panel">
          <div class="section-head">
            <p class="section-kicker">Conversation Archive</p>
            <h2>消息流</h2>
          </div>
          <p v-if="loading" class="state-text">正在同步会话内容...</p>
          <p v-else-if="!messages.length" class="state-text">当前会话还没有消息，发送第一段内容开始对话。</p>

          <div v-else class="message-stack">
            <article
              v-for="message in messages"
              :key="message.messageId"
              class="message-card"
              :class="{ 'message-card--student': message.senderType === 'STUDENT' }"
            >
              <div class="message-meta">
                <span class="message-role">
                  {{ message.senderType === 'STUDENT' ? '我' : 'AI 导师' }}
                </span>
                <span>{{ new Date(message.createdAt).toLocaleString('zh-CN') }}</span>
                <span v-if="message.riskLevel">Level {{ message.riskLevel }}</span>
              </div>
              <p class="message-content">{{ message.content }}</p>
              <p v-if="message.hitKeywords" class="message-tip">命中关键词：{{ message.hitKeywords }}</p>
            </article>
          </div>
        </section>

        <aside class="compose-panel glass-panel">
          <div class="section-head">
            <p class="section-kicker">Compose</p>
            <h2>继续表达</h2>
          </div>
          <textarea
            v-model="composeForm.content"
            class="compose-textarea"
            rows="10"
            maxlength="2000"
            placeholder="可以从今天最强烈的感受开始写，或者描述一个具体场景。"
          />
          <button class="primary-button" type="button" :disabled="sending" @click="sendMessage">
            {{ sending ? '正在发送...' : '发送消息' }}
          </button>
          <p v-if="sendResult" class="result-note">
            已写入消息 #{{ sendResult.studentMessage.messageId }}，AI 最近一次返回Level：
            {{ sendResult.riskLevel || '常规' }}
          </p>
          <p v-if="errorMessage" class="error-text">{{ errorMessage }}</p>
        </aside>
      </div>
    </div>
  </section>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700&family=Noto+Serif+SC:wght@400;500;600;700&display=swap');

:global(body) {
  background:
    radial-gradient(circle at 18% 14%, rgba(210, 221, 213, 0.3), transparent 26%),
    radial-gradient(circle at 82% 18%, rgba(228, 216, 203, 0.3), transparent 24%),
    linear-gradient(180deg, #f4efe5 0%, #f8f4ec 100%);
}

.ai-chat-page {
  min-height: 100vh;
  padding: 44px 28px 70px;
  color: #293028;
}

.chat-shell {
  max-width: 1360px;
  margin: 0 auto;
}

.chat-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(250px, 0.6fr);
  gap: 28px;
  align-items: end;
  margin-bottom: 32px;
}

.hero-copy {
  border-top: 1px solid rgba(62, 70, 61, 0.18);
  padding-top: 18px;
}

.eyebrow,
.section-kicker {
  margin: 0 0 10px;
  font: 700 0.76rem/1 'Manrope', sans-serif;
  letter-spacing: 0.24em;
  text-transform: uppercase;
  color: #7d6754;
}

.hero-copy h1,
.section-head h2 {
  margin: 0;
  font-family: 'Noto Serif SC', serif;
  font-size: clamp(2rem, 3vw, 3.4rem);
  line-height: 1.16;
  font-weight: 600;
  letter-spacing: -0.02em;
}

.lead {
  max-width: 700px;
  margin: 18px 0 0;
  font: 400 1rem/1.84 'Manrope', sans-serif;
  color: rgba(41, 48, 40, 0.74);
}

.hero-aside {
  display: grid;
  gap: 14px;
}

.metric-card,
.glass-panel,
.conversation-panel {
  border: 1px solid rgba(76, 84, 75, 0.14);
  background: rgba(255, 251, 245, 0.74);
  box-shadow: 0 24px 70px rgba(91, 81, 67, 0.08);
  backdrop-filter: blur(16px);
}

.metric-card {
  padding: 18px 20px;
}

.metric-card span,
.message-meta,
.state-text,
.result-note {
  font-family: 'Manrope', sans-serif;
}

.metric-card span {
  display: block;
  margin-bottom: 8px;
  font-size: 0.78rem;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: rgba(66, 73, 63, 0.56);
}

.metric-card strong {
  font-family: 'Noto Serif SC', serif;
  font-size: 1.6rem;
  font-weight: 600;
}

.chat-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.28fr) minmax(320px, 0.72fr);
  gap: 28px;
  align-items: start;
}

.conversation-panel,
.compose-panel {
  padding: 24px;
}

.section-head {
  margin-bottom: 18px;
}

.message-stack {
  display: grid;
  gap: 16px;
}

.message-card {
  border-top: 1px solid rgba(94, 103, 92, 0.14);
  padding-top: 16px;
}

.message-card--student {
  margin-left: 52px;
}

.message-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 14px;
  margin-bottom: 10px;
  font-size: 0.8rem;
  letter-spacing: 0.05em;
  color: rgba(68, 74, 66, 0.58);
}

.message-role {
  color: #6f5744;
  font-weight: 700;
}

.message-content {
  margin: 0;
  white-space: pre-wrap;
  font: 400 1rem/1.92 'Manrope', sans-serif;
  color: #293028;
}

.message-tip {
  margin: 10px 0 0;
  font: 600 0.84rem/1.6 'Manrope', sans-serif;
  color: #8a533d;
}

.compose-textarea {
  width: 100%;
  resize: vertical;
  border: 1px solid rgba(80, 88, 79, 0.16);
  background: rgba(255, 255, 255, 0.74);
  padding: 16px 18px;
  font: 400 0.98rem/1.8 'Manrope', sans-serif;
  color: #293028;
  outline: none;
  transition: border-color 0.28s ease, box-shadow 0.28s ease, transform 0.28s ease;
}

.compose-textarea:focus {
  border-color: rgba(94, 113, 100, 0.5);
  box-shadow: 0 18px 38px rgba(78, 97, 85, 0.12);
  transform: translateY(-1px);
}

.primary-button {
  margin-top: 18px;
  border: none;
  background: linear-gradient(135deg, #263229 0%, #445348 100%);
  color: #f8f5ef;
  padding: 15px 20px;
  font: 700 0.92rem/1 'Manrope', sans-serif;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  cursor: pointer;
  transition: transform 0.28s ease, box-shadow 0.28s ease, opacity 0.28s ease;
}

.primary-button:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 22px 38px rgba(38, 50, 41, 0.18);
}

.primary-button:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.result-note {
  margin: 16px 0 0;
  font-size: 0.92rem;
  line-height: 1.8;
  color: rgba(41, 48, 40, 0.68);
}

.state-text,
.error-text {
  margin: 0;
  font-size: 0.96rem;
  line-height: 1.8;
}

.error-text {
  margin-top: 16px;
  font-weight: 600;
  color: #a64939;
}

@media (max-width: 980px) {
  .chat-hero,
  .chat-grid {
    grid-template-columns: 1fr;
  }

  .message-card--student {
    margin-left: 0;
  }
}

@media (max-width: 640px) {
  .ai-chat-page {
    padding: 28px 16px 46px;
  }

  .hero-copy h1,
  .section-head h2 {
    font-size: 1.86rem;
  }
}
</style>

