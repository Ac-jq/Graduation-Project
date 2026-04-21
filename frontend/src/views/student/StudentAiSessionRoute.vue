<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { fetchStudentAiSessionMessagesApi, fetchStudentAiSessionsApi, sendStudentAiChatMessageApi } from '@/api/ai-chat'
import type { AiChatMessage, AiChatSession } from '@/api/types'
import { toErrorMessage, toNumberParam } from '@/views/shared/page-logic'

interface AiPersonaConfig {
  name: string
  avatar: string
}

type AiChatMessageView = AiChatMessage & {
  optimistic?: boolean
  typing?: boolean
}

const route = useRoute()
const router = useRouter()

const PERSONA_STORAGE_KEY = 'jqpro.student.ai-persona'
const DEFAULT_PERSONA: AiPersonaConfig = {
  name: '青禾导师',
  avatar: '🌿'
}
const avatarOptions = ['🌿', '🕯️', '☁️', '🍃', '🌙', '🫧']

const loading = ref(false)
const sending = ref(false)
const errorMessage = ref('')
const messages = ref<AiChatMessageView[]>([])
const sessions = ref<AiChatSession[]>([])
const draft = ref('')
const messageViewport = ref<HTMLElement | null>(null)
const personaDialogVisible = ref(false)
const aiPersona = ref<AiPersonaConfig>({ ...DEFAULT_PERSONA })
const personaForm = ref<AiPersonaConfig>({ ...DEFAULT_PERSONA })
const transientMessageId = ref(-1)

const sessionId = computed(() => toNumberParam(route.params.sessionId))
const activeSession = computed(() => sessions.value.find((item) => item.sessionId === sessionId.value) ?? null)
const aiPersonaInitial = computed(() => aiPersona.value.name.trim().slice(0, 1) || '青')

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
  if (Number.isNaN(d.getTime())) return '暂无'
  return `${String(d.getMonth() + 1).padStart(2, '0')}/${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

function formatMessageTime(value: string | null | undefined): string {
  if (!value) return '刚刚'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return '刚刚'
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

function createTransientMessage(senderType: 'STUDENT' | 'AI', content: string, typing = false): AiChatMessageView {
  return {
    messageId: transientMessageId.value--,
    sessionId: sessionId.value ?? 0,
    senderType,
    content,
    riskLevel: null,
    hitKeywords: null,
    createdAt: new Date().toISOString(),
    optimistic: true,
    typing
  }
}

function loadAiPersona(): void {
  const rawConfig = localStorage.getItem(PERSONA_STORAGE_KEY)
  if (!rawConfig) return

  try {
    const parsedConfig = JSON.parse(rawConfig) as Partial<AiPersonaConfig>
    aiPersona.value = {
      name: parsedConfig.name?.trim() || DEFAULT_PERSONA.name,
      avatar: parsedConfig.avatar || DEFAULT_PERSONA.avatar
    }
  } catch {
    localStorage.removeItem(PERSONA_STORAGE_KEY)
  }
}

function openPersonaDialog(): void {
  personaForm.value = { ...aiPersona.value }
  personaDialogVisible.value = true
}

function savePersonaConfig(): void {
  const name = personaForm.value.name.trim()
  if (!name) {
    ElMessage.warning('请给 AI 导师起一个名字')
    return
  }

  aiPersona.value = {
    name: name.slice(0, 12),
    avatar: personaForm.value.avatar || DEFAULT_PERSONA.avatar
  }
  localStorage.setItem(PERSONA_STORAGE_KEY, JSON.stringify(aiPersona.value))
  personaDialogVisible.value = false
  ElMessage.success('AI 形象已更新')
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
    draft.value = ''
    const optimisticStudentMessage = createTransientMessage('STUDENT', content)
    const typingMessage = createTransientMessage('AI', '', true)
    messages.value = [...messages.value, optimisticStudentMessage, typingMessage]
    await scrollToBottom()

    const response = await sendStudentAiChatMessageApi(sessionId.value, { content })
    messages.value = messages.value
        .filter((message) =>
            message.messageId !== optimisticStudentMessage.messageId
            && message.messageId !== typingMessage.messageId
        )
        .concat(response.studentMessage, response.aiMessage)
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
    messages.value = messages.value.filter((message) => !message.typing)
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
  loadAiPersona()
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
          <div class="status-copy">
            <div class="status-indicator"></div>
            <span class="status-text">你的专属导师在线</span>
          </div>
          <button class="persona-edit-btn" type="button" aria-label="设置 AI 导师形象" @click="openPersonaDialog">
            <span>{{ aiPersona.avatar }}</span>
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="M15.7 5.3 18.7 8.3 8.6 18.4 5 19l.6-3.6L15.7 5.3Zm1.4-1.4a1.8 1.8 0 0 1 2.5 0l.5.5a1.8 1.8 0 0 1 0 2.5l-.5.5-3-3 .5-.5Z" />
            </svg>
          </button>
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
                <span class="actor-avatar" :class="message.senderType === 'STUDENT' ? 'is-you' : 'is-mentor'">
                  {{ message.senderType === 'STUDENT' ? '你' : aiPersona.avatar }}
                </span>
                <span class="actor-name">{{ message.senderType === 'STUDENT' ? '我' : aiPersona.name }}</span>
                <span class="actor-time">{{ formatMessageTime(message.createdAt) }}</span>
              </div>

              <div class="row-content">
                <div v-if="message.typing" class="typing-bubble" aria-live="polite">
                  <span></span>
                  <span></span>
                  <span></span>
                  <em>对方正在输入...</em>
                </div>
                <p v-else class="message-content">{{ message.content }}</p>
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

    <el-dialog
      v-model="personaDialogVisible"
      title="设置你的 AI 导师"
      width="480px"
      destroy-on-close
      class="persona-dialog"
    >
      <div class="persona-dialog-body">
        <div class="persona-preview">
          <div class="preview-avatar">{{ personaForm.avatar || aiPersonaInitial }}</div>
          <div>
            <p class="preview-kicker">你的倾听伙伴</p>
            <h2>{{ personaForm.name || DEFAULT_PERSONA.name }}</h2>
          </div>
        </div>

        <label class="persona-field">
          <span>AI 的名字</span>
          <input v-model="personaForm.name" maxlength="12" type="text" placeholder="例如：青禾导师">
        </label>

        <div class="persona-field">
          <span>选择头像</span>
          <div class="avatar-picker">
            <button
              v-for="avatar in avatarOptions"
              :key="avatar"
              class="avatar-option"
              :class="{ 'is-selected': personaForm.avatar === avatar }"
              type="button"
              @click="personaForm.avatar = avatar"
            >
              {{ avatar }}
            </button>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="persona-dialog-footer">
          <button class="dialog-btn" type="button" @click="personaDialogVisible = false">取消</button>
          <button class="dialog-btn dialog-btn--primary" type="button" @click="savePersonaConfig">保存</button>
        </div>
      </template>
    </el-dialog>
  </main>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700;800&family=Noto+Serif+SC:wght@500;600;700&display=swap');

/* 全局亮白日记底色 */
.split-editorial-page {
  min-height: 100vh;
  background:
    radial-gradient(circle at 8% 10%, rgba(229, 244, 236, 0.5), transparent 28rem),
    linear-gradient(180deg, #ffffff 0%, #fbfcfc 100%);
  color: #1e2821;
  font-family: 'Manrope', 'Noto Serif SC', sans-serif;
  padding: 3.5rem clamp(1.2rem, 3vw, 3rem);
  box-sizing: border-box;
}

.page-container {
  max-width: 1240px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: 340px minmax(0, 1fr);
  gap: clamp(3rem, 6vw, 6.5rem);
  align-items: start;
}

/* ================= 左侧静态控制台 ================= */
.editorial-sidebar {
  position: relative;
}

.sidebar-sticky {
  position: sticky;
  top: 3.5rem;
  display: flex;
  flex-direction: column;
  gap: 2.6rem;
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
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.ghost-link:hover {
  color: #1e2821;
  transform: translateX(-3px);
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
  font-size: clamp(2rem, 3vw, 2.65rem);
  font-weight: 600;
  color: #1e2821;
  margin: 0;
  line-height: 1.18;
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
  height: 1px;
  background: linear-gradient(90deg, rgba(42, 54, 46, 0.18), transparent);
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
  padding-left: 1rem;
  color: #5c6b60;
  font-size: 0.95rem;
  line-height: 1.9;
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
  border-top: 1px solid rgba(42, 54, 46, 0.06);
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
  height: calc(100vh - 7rem);
  min-height: 660px;
  padding: clamp(1.2rem, 2vw, 1.8rem);
  border-radius: 34px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow:
    0 30px 80px rgba(35, 48, 39, 0.06),
    inset 0 0 0 1px rgba(42, 54, 46, 0.035);
  backdrop-filter: blur(18px);
}

/* 呼吸灯状态 */
.connection-status {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  margin-bottom: 1.4rem;
  padding: 0.2rem 0.1rem 0.8rem;
  flex-shrink: 0;
}

.status-copy {
  display: inline-flex;
  align-items: center;
  gap: 0.75rem;
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
  font-size: 0.92rem;
  font-weight: 600;
  color: #5c6b60;
}

.persona-edit-btn {
  border: none;
  background: rgba(247, 249, 248, 0.9);
  color: #2a362e;
  min-width: 3rem;
  height: 2.55rem;
  padding: 0 0.75rem;
  border-radius: 999px;
  box-shadow: inset 0 0 0 1px rgba(42, 54, 46, 0.055);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.45rem;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.persona-edit-btn svg {
  width: 1rem;
  height: 1rem;
  fill: currentColor;
  opacity: 0.62;
}

.persona-edit-btn:hover {
  transform: translateY(-2px);
  background: #ffffff;
  box-shadow: 0 14px 30px rgba(42, 54, 46, 0.08);
}

/* 无框聊天流 */
.transcript-wrapper {
  flex: 1;
  overflow-y: auto;
  padding: 0.8rem 1rem 0.8rem 0.25rem;
  margin-bottom: 1.4rem;
  border-bottom: 1px solid rgba(42, 54, 46, 0.055);
}

.transcript-wrapper::-webkit-scrollbar {
  width: 4px;
}
.transcript-wrapper::-webkit-scrollbar-thumb {
  background: rgba(42, 54, 46, 0.12);
  border-radius: 4px;
}

.transcript-stream {
  display: flex;
  flex-direction: column;
}

.transcript-row {
  display: grid;
  grid-template-columns: 104px minmax(0, 1fr);
  gap: 1.8rem;
  padding: 1.7rem 0;
  border-bottom: 1px solid rgba(42, 54, 46, 0.045);
  align-items: start;
}

.transcript-row:last-child {
  border-bottom: none;
}

/* 角色与时间 */
.row-actor {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
  text-align: right;
  align-items: flex-end;
  padding-top: 0.2rem;
}

.actor-avatar {
  width: 2.45rem;
  height: 2.45rem;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-family: 'Noto Serif SC', serif;
  font-size: 1.15rem;
  font-weight: 700;
  color: #2a362e;
  box-shadow: 0 12px 28px rgba(42, 54, 46, 0.07);
}

.actor-avatar.is-mentor {
  background: linear-gradient(145deg, #f7fbf8, #eef6f1);
}

.actor-avatar.is-you {
  background: linear-gradient(145deg, #fff9f3, #f7efe5);
  color: #8c6a5c;
}

.actor-name {
  font-family: 'Manrope', sans-serif;
  font-size: 0.82rem;
  font-weight: 800;
  letter-spacing: 0.08em;
  color: #5c7062;
}

.is-student .actor-name {
  color: #8c6a5c; /* 学生自身的颜色 */
}

.actor-time {
  font-family: 'Manrope', sans-serif;
  font-size: 0.76rem;
  color: #aebbb2;
}

/* 消息正文 */
.row-content {
  display: flex;
  flex-direction: column;
}

.message-content {
  font-family: 'Noto Serif SC', serif;
  font-size: 1.08rem;
  line-height: 1.95;
  color: #1e2821;
  margin: 0;
  white-space: pre-wrap;
}

.is-student .message-content {
  color: #4a5c51;
}

.typing-bubble {
  display: inline-flex;
  align-items: center;
  gap: 0.42rem;
  width: max-content;
  max-width: 100%;
  padding: 0.78rem 1rem;
  border-radius: 999px;
  background: linear-gradient(145deg, rgba(247, 251, 248, 0.98), rgba(238, 246, 241, 0.86));
  color: #6f7e73;
  box-shadow: 0 16px 38px rgba(42, 54, 46, 0.06);
}

.typing-bubble span {
  width: 0.38rem;
  height: 0.38rem;
  border-radius: 50%;
  background: #8fb19a;
  animation: typing-dot 1.05s infinite ease-in-out;
}

.typing-bubble span:nth-child(2) {
  animation-delay: 0.14s;
}

.typing-bubble span:nth-child(3) {
  animation-delay: 0.28s;
}

.typing-bubble em {
  margin-left: 0.25rem;
  font-family: 'Noto Serif SC', serif;
  font-size: 0.92rem;
  font-style: normal;
}

@keyframes typing-dot {
  0%, 80%, 100% {
    transform: translateY(0);
    opacity: 0.42;
  }

  40% {
    transform: translateY(-3px);
    opacity: 1;
  }
}

/* 沉浸式书写台 */
.composer-desk {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  flex-shrink: 0;
  padding: 1.15rem 1.25rem;
  border-radius: 26px;
  background: #ffffff;
  box-shadow:
    0 18px 50px rgba(42, 54, 46, 0.07),
    inset 0 0 0 1px rgba(42, 54, 46, 0.045);
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
  background: #fbfcfc;
  padding: 1rem 1.05rem;
  border-radius: 18px;
  font-family: 'Noto Serif SC', serif;
  font-size: 1.1rem;
  line-height: 1.8;
  color: #1e2821;
  resize: none;
  outline: none;
  box-shadow: inset 0 0 0 1px rgba(42, 54, 46, 0.045);
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.sleek-textarea::placeholder {
  color: #b5c2b9;
  font-style: italic;
}

.sleek-textarea:focus {
  background: #ffffff;
  box-shadow:
    0 16px 40px rgba(42, 54, 46, 0.06),
    inset 0 0 0 1px rgba(92, 107, 96, 0.18);
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
  background: #2a362e;
  border: none;
  color: #ffffff;
  padding: 0.8rem 1.8rem;
  border-radius: 100px;
  font-family: 'Noto Serif SC', serif;
  font-size: 0.95rem;
  font-weight: 600;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 0.6rem;
  box-shadow: 0 12px 30px rgba(42, 54, 46, 0.16);
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.action-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  color: #ffffff;
  box-shadow: 0 18px 42px rgba(42, 54, 46, 0.2);
}

.action-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

/* 状态提示 */
.error-banner {
  background: rgba(140, 74, 74, 0.06);
  color: #8c4a4a;
  padding: 1.5rem;
  border-radius: 18px;
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
  transition: transform 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.ghost-link:hover .arrow {
  transform: translateX(-4px);
}

.action-btn:hover:not(:disabled) .arrow {
  transform: translateX(4px);
}

/* AI 形象设置弹窗 */
:deep(.persona-dialog .el-dialog) {
  border-radius: 30px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 36px 90px rgba(42, 54, 46, 0.14);
  overflow: hidden;
}

:deep(.persona-dialog .el-dialog__header) {
  padding: 1.5rem 1.6rem 0.6rem;
  margin: 0;
}

:deep(.persona-dialog .el-dialog__title) {
  font-family: 'Noto Serif SC', serif;
  font-size: 1.25rem;
  font-weight: 600;
  color: #1e2821;
}

:deep(.persona-dialog .el-dialog__body) {
  padding: 1rem 1.6rem 1.4rem;
}

:deep(.persona-dialog .el-dialog__footer) {
  padding: 0 1.6rem 1.5rem;
}

.persona-dialog-body {
  display: grid;
  gap: 1.35rem;
}

.persona-preview {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1rem;
  border-radius: 24px;
  background:
    radial-gradient(circle at 15% 20%, rgba(226, 242, 235, 0.95), transparent 8rem),
    linear-gradient(135deg, #ffffff, #f8faf8);
  box-shadow: inset 0 0 0 1px rgba(42, 54, 46, 0.04);
}

.preview-avatar {
  width: 3.8rem;
  height: 3.8rem;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #ffffff;
  box-shadow: 0 18px 40px rgba(42, 54, 46, 0.08);
  font-size: 1.8rem;
}

.preview-kicker {
  margin: 0 0 0.3rem;
  font-family: 'Manrope', sans-serif;
  font-size: 0.72rem;
  font-weight: 800;
  letter-spacing: 0.16em;
  color: #8a9c90;
  text-transform: uppercase;
}

.persona-preview h2 {
  margin: 0;
  font-family: 'Noto Serif SC', serif;
  font-size: 1.45rem;
  color: #1e2821;
}

.persona-field {
  display: grid;
  gap: 0.75rem;
}

.persona-field span {
  font-family: 'Noto Serif SC', serif;
  font-size: 0.92rem;
  font-weight: 600;
  color: #5c6b60;
}

.persona-field input {
  width: 100%;
  border: none;
  border-radius: 18px;
  background: #fbfcfc;
  color: #1e2821;
  padding: 0.9rem 1rem;
  font-family: 'Noto Serif SC', serif;
  font-size: 1rem;
  outline: none;
  box-shadow: inset 0 0 0 1px rgba(42, 54, 46, 0.06);
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.persona-field input:focus {
  background: #ffffff;
  box-shadow:
    0 14px 34px rgba(42, 54, 46, 0.06),
    inset 0 0 0 1px rgba(92, 107, 96, 0.18);
}

.avatar-picker {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 0.65rem;
}

.avatar-option {
  border: none;
  width: 100%;
  aspect-ratio: 1;
  border-radius: 18px;
  background: #f8faf9;
  font-size: 1.35rem;
  cursor: pointer;
  box-shadow: inset 0 0 0 1px rgba(42, 54, 46, 0.045);
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.avatar-option:hover {
  transform: translateY(-3px);
  background: #ffffff;
  box-shadow: 0 12px 28px rgba(42, 54, 46, 0.08);
}

.avatar-option.is-selected {
  background: #eef7f1;
  box-shadow:
    0 12px 30px rgba(92, 140, 107, 0.12),
    inset 0 0 0 1px rgba(92, 140, 107, 0.18);
}

.persona-dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.8rem;
}

.dialog-btn {
  border: none;
  border-radius: 999px;
  padding: 0.75rem 1.35rem;
  background: #f5f7f6;
  color: #5c6b60;
  font-family: 'Noto Serif SC', serif;
  font-size: 0.92rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.dialog-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 26px rgba(42, 54, 46, 0.08);
}

.dialog-btn--primary {
  background: #2a362e;
  color: #ffffff;
  box-shadow: 0 14px 30px rgba(42, 54, 46, 0.16);
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
    height: 72vh;
    min-height: 560px;
  }
}

@media (max-width: 600px) {
  .split-editorial-page {
    padding: 2rem 1rem;
  }

  .connection-status {
    align-items: flex-start;
  }

  .transcript-row {
    grid-template-columns: 1fr;
    gap: 0.8rem;
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

  .actor-avatar {
    width: 2.1rem;
    height: 2.1rem;
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

  .avatar-picker {
    grid-template-columns: repeat(3, 1fr);
  }
}
</style>
