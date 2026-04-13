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
const sessionStatusLabel = computed(() => {
  if (activeSession.value?.riskFlag) {
    return '需要更多现实支持'
  }
  return '平稳陪伴中'
})
const activeRiskLevel = computed(() => activeSession.value?.riskLevel || 'LOW')

function formatDateTime(value: string | null | undefined): string {
  if (!value) {
    return '暂无'
  }
  return new Date(value).toLocaleString('zh-CN', { hour12: false })
}

function riskTone(level: string | null | undefined): string {
  if (level === 'HIGH') {
    return 'risk-high'
  }
  if (level === 'MEDIUM') {
    return 'risk-medium'
  }
  return 'risk-low'
}

async function scrollToBottom(smooth = true): Promise<void> {
  await nextTick()
  if (!messageViewport.value) {
    return
  }
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
  if (!sessionId.value || sending.value) {
    return
  }

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
            riskFlag: response.riskFlag,
            riskLevel: response.riskLevel,
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
  <section class="ai-session-page">
    <div class="session-shell">
      <div class="session-header">
        <button class="ghost-link" type="button" @click="goBack">
          <span class="ghost-link__icon">←</span>
          返回会话列表
        </button>

        <div class="session-header__main">
          <div class="session-header__copy">
            <p class="eyebrow">AI Mentor Session</p>
            <h1>{{ activeSession?.title || `倾诉会话 #${sessionId || '-'}` }}</h1>
            <p class="lead">
              在这里把难以整理的情绪慢慢说出来。AI 导师会先接住你的感受，再帮你把问题拆小。
            </p>
          </div>

          <div class="session-header__meta">
            <div class="meta-card meta-card--mesh">
              <span class="meta-label">会话状态</span>
              <strong>{{ sessionStatusLabel }}</strong>
              <p>{{ activeSession?.summaryText || '刚开始也没关系，先从你现在最想说的一句话开始。' }}</p>
            </div>
            <div class="meta-card">
              <span class="meta-label">风险等级</span>
              <strong :class="riskTone(activeRiskLevel)">{{ activeRiskLevel }}</strong>
              <p>最近活跃：{{ formatDateTime(activeSession?.lastActiveAt || activeSession?.createdAt) }}</p>
            </div>
          </div>
        </div>
      </div>

      <div class="session-grid">
        <aside class="session-side">
          <article class="side-card side-card--warm">
            <p class="side-kicker">Gentle Prompt</p>
            <h2>探索内在的平静</h2>
            <p>
              这里不是任务面板，而是一个可以慢下来整理自己状态的地方。你可以说事件，也可以只说感受。
            </p>
          </article>

          <article class="side-card">
            <p class="side-kicker">Conversation Notes</p>
            <ul class="ritual-list">
              <li>先描述你现在最强烈的感受。</li>
              <li>如果不想说完整故事，只写一小段也可以。</li>
              <li>若涉及安全风险，请尽快联系老师、家人或线下支持。</li>
            </ul>
          </article>

          <article class="side-card side-card--muted">
            <p class="side-kicker">Session Detail</p>
            <dl class="detail-grid">
              <div>
                <dt>会话编号</dt>
                <dd>#{{ sessionId || '-' }}</dd>
              </div>
              <div>
                <dt>创建时间</dt>
                <dd>{{ formatDateTime(activeSession?.createdAt) }}</dd>
              </div>
              <div>
                <dt>当前状态</dt>
                <dd>{{ activeSession?.status || 'ACTIVE' }}</dd>
              </div>
              <div>
                <dt>消息数量</dt>
                <dd>{{ messages.length }}</dd>
              </div>
            </dl>
          </article>
        </aside>

        <main class="session-main">
          <div class="chat-card">
            <header class="chat-card__header">
              <div>
                <p class="side-kicker">Dialogue</p>
                <h2>把还没有说出口的话，交给这个安静空间</h2>
              </div>
              <span class="online-pill">
                <span class="online-pill__dot"></span>
                DeepSeek 已接入
              </span>
            </header>

            <div v-if="loading" class="state-panel">正在同步当前会话内容...</div>
            <div v-else-if="errorMessage" class="state-panel state-panel--error">{{ errorMessage }}</div>
            <div v-else-if="!messages.length" class="state-panel">
              当前还没有消息。先写下此刻最想被看见的一句话，AI 导师会基于真实模型回复你。
            </div>

            <div v-else ref="messageViewport" class="message-viewport">
              <article
                v-for="message in messages"
                :key="message.messageId"
                class="message-bubble"
                :class="message.senderType === 'STUDENT' ? 'message-bubble--student' : 'message-bubble--ai'"
              >
                <div class="message-bubble__avatar" :class="message.senderType === 'STUDENT' ? 'is-student' : 'is-ai'">
                  {{ message.senderType === 'STUDENT' ? '我' : 'AI' }}
                </div>

                <div class="message-bubble__body">
                  <div class="message-bubble__meta">
                    <span>{{ message.senderType === 'STUDENT' ? '你' : 'AI 导师' }}</span>
                    <span>{{ formatDateTime(message.createdAt) }}</span>
                    <span v-if="message.riskLevel" :class="['risk-badge', riskTone(message.riskLevel)]">
                      {{ message.riskLevel }}
                    </span>
                  </div>
                  <p class="message-bubble__content">{{ message.content }}</p>
                  <p v-if="message.hitKeywords" class="message-bubble__tip">风险提示关键词：{{ message.hitKeywords }}</p>
                </div>
              </article>
            </div>

            <footer class="composer-card">
              <label class="composer-label" for="student-ai-composer">输入你此刻最想说的话</label>
              <textarea
                id="student-ai-composer"
                v-model="draft"
                class="composer-input"
                rows="4"
                maxlength="1000"
                placeholder="例如：我最近总觉得胸口发紧，晚上躺下后脑子停不下来，不知道该怎么让自己松下来。"
                @keydown="handleComposerKeydown"
              />
              <div class="composer-footer">
                <p class="composer-hint">
                  Enter 发送，Shift + Enter 换行。AI 导师使用真实模型回复，不再使用本地兜底话术。
                </p>
                <button class="send-button" type="button" :disabled="sending" @click="sendMessage">
                  {{ sending ? '正在发送...' : '发送给 AI 导师' }}
                </button>
              </div>
            </footer>
          </div>
        </main>
      </div>
    </div>
  </section>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700;800&family=Noto+Serif+SC:wght@400;500;600;700&display=swap');

.ai-session-page {
  min-height: 100%;
  padding: 20px 0 36px;
  color: #263228;
}

.session-shell {
  max-width: 1480px;
  margin: 0 auto;
}

.ghost-link {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  border: none;
  background: transparent;
  padding: 0;
  font: 700 0.8rem/1 'Manrope', sans-serif;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: #6a7a6b;
  cursor: pointer;
  transition: transform 0.28s ease, color 0.28s ease;
}

.ghost-link:hover {
  color: #324238;
  transform: translateX(-2px);
}

.session-header {
  display: grid;
  gap: 22px;
  margin-bottom: 28px;
}

.session-header__main {
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(320px, 0.95fr);
  gap: 24px;
  align-items: stretch;
}

.eyebrow,
.side-kicker,
.meta-label,
.composer-label {
  margin: 0 0 10px;
  font: 700 0.78rem/1 'Manrope', sans-serif;
  letter-spacing: 0.22em;
  text-transform: uppercase;
  color: #897866;
}

.session-header__copy h1,
.chat-card__header h2,
.side-card h2 {
  margin: 0;
  font-family: 'Noto Serif SC', serif;
  font-weight: 600;
  letter-spacing: -0.02em;
}

.session-header__copy h1 {
  max-width: 10em;
  font-size: clamp(2.4rem, 3.6vw, 4.4rem);
  line-height: 1.04;
}

.lead,
.meta-card p,
.side-card p,
.ritual-list,
.detail-grid,
.message-bubble__meta,
.message-bubble__content,
.message-bubble__tip,
.composer-hint,
.state-panel {
  font-family: 'Manrope', sans-serif;
}

.lead {
  max-width: 720px;
  margin: 18px 0 0;
  font-size: 1rem;
  line-height: 1.95;
  color: rgba(38, 50, 40, 0.68);
}

.session-header__meta,
.session-grid {
  display: grid;
  gap: 22px;
}

.session-header__meta {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.meta-card,
.side-card,
.chat-card,
.message-bubble,
.composer-card {
  border-radius: 28px;
  background: rgba(255, 253, 249, 0.9);
  box-shadow: 0 22px 60px rgba(68, 58, 46, 0.08);
}

.meta-card,
.side-card {
  padding: 24px 24px 22px;
}

.meta-card strong {
  display: block;
  margin-bottom: 10px;
  font-family: 'Noto Serif SC', serif;
  font-size: 1.55rem;
  font-weight: 600;
}

.meta-card p {
  margin: 0;
  color: rgba(38, 50, 40, 0.64);
  line-height: 1.8;
}

.meta-card--mesh {
  position: relative;
  overflow: hidden;
  background:
    radial-gradient(circle at 12% 18%, rgba(176, 198, 183, 0.72), transparent 34%),
    radial-gradient(circle at 82% 18%, rgba(233, 203, 176, 0.78), transparent 28%),
    radial-gradient(circle at 52% 88%, rgba(248, 240, 229, 0.9), transparent 42%),
    linear-gradient(135deg, rgba(246, 248, 243, 0.96), rgba(255, 251, 245, 0.92));
}

.meta-card--mesh::after {
  content: '';
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.18) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.18) 1px, transparent 1px);
  background-size: 34px 34px;
  opacity: 0.28;
  pointer-events: none;
}

.risk-low {
  color: #5d7567;
}

.risk-medium {
  color: #9b6f4d;
}

.risk-high {
  color: #9f4f43;
}

.session-grid {
  grid-template-columns: minmax(290px, 0.68fr) minmax(0, 1.32fr);
  align-items: start;
}

.session-side {
  display: grid;
  gap: 18px;
}

.side-card {
  padding: 24px;
}

.side-card--warm {
  background: linear-gradient(180deg, rgba(245, 239, 228, 0.96), rgba(255, 252, 247, 0.96));
}

.side-card--muted {
  background: linear-gradient(180deg, rgba(246, 248, 242, 0.94), rgba(255, 253, 249, 0.94));
}

.side-card h2 {
  font-size: 1.8rem;
  line-height: 1.2;
}

.side-card p {
  margin: 14px 0 0;
  line-height: 1.85;
  color: rgba(38, 50, 40, 0.68);
}

.ritual-list {
  margin: 12px 0 0;
  padding-left: 1.2rem;
  line-height: 1.9;
  color: rgba(38, 50, 40, 0.72);
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-top: 12px;
}

.detail-grid dt {
  margin-bottom: 6px;
  font-size: 0.78rem;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: rgba(90, 88, 79, 0.6);
}

.detail-grid dd {
  margin: 0;
  font-size: 0.95rem;
  font-weight: 600;
  color: #324238;
}

.chat-card {
  min-height: 760px;
  padding: 24px;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  gap: 20px;
  background: linear-gradient(180deg, rgba(252, 251, 248, 0.96), rgba(255, 255, 255, 0.9));
}

.chat-card__header {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: start;
}

.chat-card__header h2 {
  font-size: clamp(1.85rem, 2.6vw, 2.7rem);
  line-height: 1.15;
}

.online-pill {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  align-self: center;
  padding: 12px 16px;
  border-radius: 999px;
  background: rgba(235, 242, 235, 0.9);
  color: #4f6558;
  font: 700 0.78rem/1 'Manrope', sans-serif;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.online-pill__dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: #7f9b87;
  box-shadow: 0 0 0 10px rgba(127, 155, 135, 0.12);
}

.state-panel {
  min-height: 240px;
  display: grid;
  place-items: center;
  padding: 30px;
  border-radius: 24px;
  background: rgba(246, 242, 236, 0.72);
  color: rgba(38, 50, 40, 0.65);
  text-align: center;
  line-height: 1.9;
}

.state-panel--error {
  background: rgba(246, 230, 226, 0.82);
  color: #9f4f43;
}

.message-viewport {
  min-height: 0;
  max-height: 62vh;
  overflow-y: auto;
  padding-right: 6px;
  display: grid;
  gap: 16px;
}

.message-viewport::-webkit-scrollbar {
  width: 8px;
}

.message-viewport::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: rgba(100, 120, 107, 0.22);
}

.message-bubble {
  display: grid;
  grid-template-columns: 54px minmax(0, 1fr);
  gap: 14px;
  padding: 18px;
}

.message-bubble--student {
  background: linear-gradient(180deg, rgba(250, 246, 240, 0.96), rgba(255, 252, 247, 0.96));
}

.message-bubble--ai {
  background: linear-gradient(180deg, rgba(245, 248, 243, 0.98), rgba(255, 252, 247, 0.98));
}

.message-bubble__avatar {
  width: 54px;
  height: 54px;
  border-radius: 20px;
  display: grid;
  place-items: center;
  font: 700 0.92rem/1 'Manrope', sans-serif;
}

.message-bubble__avatar.is-student {
  background: linear-gradient(135deg, rgba(218, 200, 177, 0.9), rgba(239, 228, 213, 0.98));
  color: #6f5645;
}

.message-bubble__avatar.is-ai {
  background: linear-gradient(135deg, rgba(186, 206, 192, 0.96), rgba(234, 240, 228, 0.98));
  color: #3d5a4b;
}

.message-bubble__body {
  min-width: 0;
}

.message-bubble__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 14px;
  align-items: center;
  margin-bottom: 10px;
  font-size: 0.8rem;
  letter-spacing: 0.06em;
  color: rgba(38, 50, 40, 0.56);
}

.risk-badge {
  display: inline-flex;
  align-items: center;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.74);
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.message-bubble__content {
  margin: 0;
  white-space: pre-wrap;
  font-size: 1rem;
  line-height: 1.9;
  color: #263228;
}

.message-bubble__tip {
  margin: 12px 0 0;
  color: #985847;
  font-size: 0.84rem;
  line-height: 1.7;
}

.composer-card {
  padding: 22px;
  background: linear-gradient(180deg, rgba(252, 248, 242, 0.96), rgba(255, 254, 251, 0.96));
}

.composer-input {
  width: 100%;
  min-height: 128px;
  resize: vertical;
  border: none;
  outline: none;
  border-radius: 24px;
  background: #f6f1ea;
  padding: 18px 20px;
  font: 500 0.98rem/1.85 'Manrope', sans-serif;
  color: #263228;
  transition: box-shadow 0.28s ease, transform 0.28s ease;
}

.composer-input:focus {
  box-shadow: 0 18px 40px rgba(79, 95, 83, 0.12);
  transform: translateY(-1px);
}

.composer-footer {
  margin-top: 16px;
  display: flex;
  justify-content: space-between;
  align-items: end;
  gap: 16px;
}

.composer-hint {
  margin: 0;
  max-width: 560px;
  color: rgba(38, 50, 40, 0.58);
  line-height: 1.75;
}

.send-button {
  flex-shrink: 0;
  border: none;
  border-radius: 18px;
  padding: 15px 22px;
  background: linear-gradient(135deg, #24322b, #4f6558);
  color: #fffdf9;
  font: 700 0.88rem/1 'Manrope', sans-serif;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  cursor: pointer;
  transition: transform 0.28s ease, box-shadow 0.28s ease, opacity 0.28s ease;
}

.send-button:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 18px 30px rgba(36, 50, 43, 0.2);
}

.send-button:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

@media (max-width: 1200px) {
  .session-header__main,
  .session-grid {
    grid-template-columns: 1fr;
  }

  .session-header__meta {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 760px) {
  .ai-session-page {
    padding: 8px 0 24px;
  }

  .session-header__copy h1,
  .chat-card__header h2,
  .side-card h2 {
    font-size: 1.9rem;
  }

  .session-header__meta,
  .detail-grid {
    grid-template-columns: 1fr;
  }

  .chat-card__header,
  .composer-footer {
    flex-direction: column;
    align-items: start;
  }

  .message-bubble {
    grid-template-columns: 1fr;
  }
}
</style>
