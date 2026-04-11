<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { createAiChatSessionApi, fetchStudentAiSessionsApi } from '@/api/ai-chat'
import type { AiChatSession } from '@/api/types'
import { toErrorMessage } from '@/views/shared/page-logic'

const router = useRouter()
const loading = ref(false)
const creating = ref(false)
const errorMessage = ref('')
const sessions = ref<AiChatSession[]>([])
const createForm = reactive({
  title: ''
})

async function loadSessions(): Promise<void> {
  loading.value = true
  errorMessage.value = ''

  try {
    sessions.value = await fetchStudentAiSessionsApi()
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    loading.value = false
  }
}

async function createSession(): Promise<void> {
  creating.value = true
  errorMessage.value = ''

  try {
    const session = await createAiChatSessionApi({ title: createForm.title || undefined })
    await loadSessions()
    await router.push({ name: 'student-ai-session-detail', params: { sessionId: session.sessionId } })
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    creating.value = false
  }
}

async function openSession(sessionId: number): Promise<void> {
  await router.push({ name: 'student-ai-session-detail', params: { sessionId } })
}

onMounted(() => {
  void loadSessions()
})
</script>

<template>
  <section class="ai-list-page">
    <div class="page-shell">
      <header class="page-hero">
        <div class="hero-copy">
          <p class="eyebrow">Student AI Studio</p>
          <h1>以私密对话梳理当下情绪，把未说出口的内容先交给系统接住。</h1>
          <p class="lead">
            这里保留你与 AI 导师的历次倾诉记录。你可以开启新的会话主题，也可以回到既有线索，
            继续向下追问与整理。
          </p>
        </div>
        <div class="hero-stats">
          <div class="stat-card">
            <span class="stat-label">会话Total</span>
            <strong>{{ sessions.length }}</strong>
          </div>
          <div class="stat-card">
            <span class="stat-label">最近风险标记</span>
            <strong>{{ sessions.some((session) => Boolean(session.riskFlag)) ? '存在' : '无' }}</strong>
          </div>
        </div>
      </header>

      <div class="editorial-grid">
        <section class="compose-panel glass-panel">
          <div class="section-head">
            <p class="section-kicker">New Session</p>
            <h2>发起新的倾诉主题</h2>
          </div>
          <label class="field-label" for="session-title">会话题目</label>
          <input
            id="session-title"
            v-model="createForm.title"
            class="text-input"
            type="text"
            maxlength="100"
            placeholder="例如：最近的学业压力、睡眠波动、关系困扰"
          />
          <p class="helper-text">题目可留空，系统会按时间生成默认主题。</p>
          <button class="primary-button" type="button" :disabled="creating" @click="createSession">
            {{ creating ? '正在创建会话...' : '开启新的 AI 对话' }}
          </button>
          <p v-if="errorMessage" class="error-text">{{ errorMessage }}</p>
        </section>

        <section class="session-panel">
          <div class="section-head section-head-inline">
            <div>
              <p class="section-kicker">Archive</p>
              <h2>历史会话</h2>
            </div>
            <span class="status-chip">{{ loading ? '加载中' : `${sessions.length} 条记录` }}</span>
          </div>

          <p v-if="loading" class="state-text">正在读取你的 AI 会话档案...</p>
          <p v-else-if="!sessions.length" class="state-text">
            当前还没有历史会话，先创建一条新的倾诉主题。
          </p>

          <div v-else class="session-stack">
            <article
              v-for="session in sessions"
              :key="session.sessionId"
              class="session-card"
              @click="openSession(session.sessionId)"
            >
              <div class="session-topline">
                <p class="session-title">{{ session.title || `未命名会话 #${session.sessionId}` }}</p>
                <span class="risk-pill" :class="{ 'risk-pill--active': Boolean(session.riskFlag) }">
                  {{ session.riskLevel || '常规' }}
                </span>
              </div>
              <p class="session-summary">
                {{ session.summaryText || '尚无摘要，进入会话后可查看完整对话内容。' }}
              </p>
              <div class="session-meta">
                <span>{{ session.status }}</span>
                <span>{{ new Date(session.createdAt).toLocaleString('zh-CN') }}</span>
                <span v-if="session.lastActiveAt">活跃于 {{ new Date(session.lastActiveAt).toLocaleString('zh-CN') }}</span>
              </div>
            </article>
          </div>
        </section>
      </div>
    </div>
  </section>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700&family=Noto+Serif+SC:wght@400;500;600;700&display=swap');

:global(body) {
  background:
    radial-gradient(circle at top left, rgba(200, 216, 205, 0.34), transparent 28%),
    radial-gradient(circle at 80% 20%, rgba(216, 205, 191, 0.26), transparent 24%),
    linear-gradient(180deg, #f4efe4 0%, #f8f5ee 42%, #f0ebe0 100%);
}

.ai-list-page {
  min-height: 100vh;
  padding: 48px 28px 72px;
  color: #2d312b;
}

.page-shell {
  max-width: 1320px;
  margin: 0 auto;
}

.page-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(260px, 0.65fr);
  gap: 28px;
  align-items: end;
  margin-bottom: 34px;
}

.hero-copy {
  border-top: 1px solid rgba(58, 68, 58, 0.18);
  padding-top: 18px;
}

.eyebrow,
.section-kicker {
  margin: 0 0 10px;
  font: 700 0.76rem/1 'Manrope', sans-serif;
  letter-spacing: 0.24em;
  text-transform: uppercase;
  color: #7f6956;
}

.hero-copy h1,
.section-head h2 {
  margin: 0;
  font-family: 'Noto Serif SC', serif;
  font-size: clamp(2rem, 3vw, 3.6rem);
  line-height: 1.15;
  font-weight: 600;
  letter-spacing: -0.02em;
}

.lead {
  max-width: 720px;
  margin: 18px 0 0;
  font: 400 1.02rem/1.85 'Manrope', sans-serif;
  color: rgba(45, 49, 43, 0.76);
}

.hero-stats {
  display: grid;
  gap: 14px;
}

.stat-card,
.glass-panel,
.session-card {
  border: 1px solid rgba(78, 87, 77, 0.14);
  background: rgba(255, 252, 246, 0.72);
  box-shadow: 0 24px 70px rgba(87, 79, 66, 0.08);
  backdrop-filter: blur(16px);
}

.stat-card {
  padding: 18px 20px;
}

.stat-label,
.helper-text,
.session-meta,
.state-text {
  font-family: 'Manrope', sans-serif;
}

.stat-label {
  display: block;
  margin-bottom: 8px;
  font-size: 0.8rem;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: rgba(68, 74, 64, 0.56);
}

.stat-card strong {
  font-family: 'Noto Serif SC', serif;
  font-size: 1.7rem;
  font-weight: 600;
}

.editorial-grid {
  display: grid;
  grid-template-columns: minmax(310px, 0.86fr) minmax(0, 1.14fr);
  gap: 28px;
}

.compose-panel,
.session-panel {
  min-height: 100%;
}

.compose-panel {
  padding: 26px 24px 28px;
}

.section-head {
  margin-bottom: 18px;
}

.section-head-inline {
  display: flex;
  justify-content: space-between;
  align-items: end;
  gap: 18px;
}

.field-label {
  display: block;
  margin-bottom: 10px;
  font: 700 0.8rem/1 'Manrope', sans-serif;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #615547;
}

.text-input {
  width: 100%;
  border: 1px solid rgba(78, 87, 77, 0.16);
  background: rgba(255, 255, 255, 0.68);
  padding: 16px 18px;
  font: 500 0.98rem/1.4 'Manrope', sans-serif;
  color: #2d312b;
  outline: none;
  transition: border-color 0.28s ease, box-shadow 0.28s ease, transform 0.28s ease;
}

.text-input:focus {
  border-color: rgba(96, 114, 102, 0.52);
  box-shadow: 0 18px 38px rgba(74, 96, 80, 0.12);
  transform: translateY(-1px);
}

.helper-text {
  margin: 12px 0 22px;
  font-size: 0.92rem;
  line-height: 1.8;
  color: rgba(45, 49, 43, 0.62);
}

.primary-button {
  border: none;
  background: linear-gradient(135deg, #23312a 0%, #445549 100%);
  color: #f8f5ee;
  padding: 15px 22px;
  font: 700 0.92rem/1 'Manrope', sans-serif;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  cursor: pointer;
  transition: transform 0.28s ease, box-shadow 0.28s ease, opacity 0.28s ease;
}

.primary-button:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 22px 36px rgba(35, 49, 42, 0.18);
}

.primary-button:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.error-text {
  margin: 16px 0 0;
  font: 600 0.95rem/1.7 'Manrope', sans-serif;
  color: #a44835;
}

.session-panel {
  padding: 4px 0 0;
}

.status-chip {
  border: 1px solid rgba(88, 93, 84, 0.14);
  background: rgba(255, 250, 240, 0.82);
  padding: 9px 14px;
  font: 700 0.76rem/1 'Manrope', sans-serif;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #696152;
}

.state-text {
  padding: 20px 0;
  font-size: 0.96rem;
  color: rgba(45, 49, 43, 0.62);
}

.session-stack {
  display: grid;
  gap: 18px;
}

.session-card {
  padding: 22px 22px 20px;
  cursor: pointer;
  transition: transform 0.28s ease, box-shadow 0.28s ease, border-color 0.28s ease;
}

.session-card:hover {
  transform: translateY(-4px);
  border-color: rgba(72, 92, 80, 0.24);
  box-shadow: 0 28px 52px rgba(89, 81, 68, 0.12);
}

.session-topline {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: start;
  margin-bottom: 14px;
}

.session-title {
  margin: 0;
  font-family: 'Noto Serif SC', serif;
  font-size: 1.34rem;
  line-height: 1.35;
}

.risk-pill {
  flex-shrink: 0;
  border: 1px solid rgba(97, 111, 98, 0.15);
  padding: 8px 12px;
  font: 700 0.74rem/1 'Manrope', sans-serif;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #66735f;
  background: rgba(242, 244, 237, 0.9);
}

.risk-pill--active {
  color: #8c4f37;
  background: rgba(239, 225, 217, 0.95);
  border-color: rgba(140, 79, 55, 0.22);
}

.session-summary {
  margin: 0 0 16px;
  font: 400 0.98rem/1.8 'Manrope', sans-serif;
  color: rgba(45, 49, 43, 0.7);
}

.session-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px 18px;
  font-size: 0.84rem;
  letter-spacing: 0.05em;
  color: rgba(70, 74, 66, 0.56);
}

@media (max-width: 980px) {
  .page-hero,
  .editorial-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .ai-list-page {
    padding: 28px 16px 48px;
  }

  .hero-copy h1,
  .section-head h2 {
    font-size: 1.86rem;
  }

  .session-topline,
  .section-head-inline {
    flex-direction: column;
    align-items: start;
  }
}
</style>

