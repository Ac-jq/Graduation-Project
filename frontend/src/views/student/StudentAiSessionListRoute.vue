<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
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

const totalSessions = computed(() => sessions.value.length)
const activeSessions = computed(() => sessions.value.filter((session) => session.status === 'ACTIVE').length)
const flaggedSessions = computed(() => sessions.value.filter((session) => Boolean(session.riskFlag)).length)
const latestSession = computed(() => sessions.value[0] ?? null)
const featuredSessions = computed(() => sessions.value.slice(0, 6))

function formatDateTime(value: string | null | undefined): string {
  if (!value) {
    return '刚刚开始'
  }
  return new Date(value).toLocaleString('zh-CN', { hour12: false })
}

function resolveRiskLabel(session: AiChatSession): string {
  if (session.riskFlag) {
    return '需要更多支持'
  }
  if (session.riskLevel === 'HIGH') {
    return '高关注'
  }
  if (session.riskLevel === 'MEDIUM') {
    return '中等波动'
  }
  return '平稳对话'
}

function resolveRiskClass(session: AiChatSession): string {
  if (session.riskFlag || session.riskLevel === 'HIGH') {
    return 'risk-pill risk-pill--high'
  }
  if (session.riskLevel === 'MEDIUM') {
    return 'risk-pill risk-pill--medium'
  }
  return 'risk-pill risk-pill--low'
}

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
    createForm.title = ''
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
      <header class="hero-board">
        <div class="hero-copy">
          <p class="eyebrow">AI 倾诉会话库</p>
          <h1>把今天没说完的话，留在一个安静又高级的空间里。</h1>
          <p class="lead">
            这里不是普通列表页，而是你与 AI 导师每一次对话的归档画板。你可以重新回到某条线索，也可以开启一段新的倾诉，
            让复杂情绪在更有秩序的界面里被慢慢整理出来。
          </p>

          <div class="hero-actions">
            <button class="hero-action hero-action--dark" type="button" @click="createSession" :disabled="creating">
              {{ creating ? '正在开启会话…' : '立刻发起新的倾诉' }}
            </button>
            <span class="hero-note">支持继续旧会话，也支持用一个新标题重新开始。</span>
          </div>
        </div>

        <div class="hero-side">
          <article class="featured-orb">
            <p class="card-kicker">最新会话</p>
            <h2>{{ latestSession?.title || '还没有任何会话' }}</h2>
            <p>
              {{ latestSession?.summaryText || '第一条会话会出现在这里，成为整个页面的视觉焦点。' }}
            </p>
            <div class="featured-meta">
              <span>{{ latestSession ? formatDateTime(latestSession.lastActiveAt || latestSession.createdAt) : '等待开始' }}</span>
              <button
                v-if="latestSession"
                class="featured-link"
                type="button"
                @click="openSession(latestSession.sessionId)"
              >
                继续这一段 →
              </button>
            </div>
          </article>
        </div>
      </header>

      <section class="studio-grid">
        <div class="studio-column studio-column--left">
          <article class="metric-panel glass-panel">
            <div class="metric-head">
              <p class="card-kicker">会话概览</p>
              <span class="metric-caption">同步你与 AI 导师的全部轨迹</span>
            </div>
            <div class="metric-grid">
              <div class="metric-card">
                <span class="metric-label">全部会话</span>
                <strong>{{ totalSessions }}</strong>
              </div>
              <div class="metric-card">
                <span class="metric-label">活跃会话</span>
                <strong>{{ activeSessions }}</strong>
              </div>
              <div class="metric-card metric-card--accent">
                <span class="metric-label">重点关注</span>
                <strong>{{ flaggedSessions }}</strong>
              </div>
            </div>
          </article>

          <article class="compose-panel glass-panel">
            <div class="compose-copy">
              <p class="card-kicker">新建会话</p>
              <h2>给这一段情绪，一个恰到好处的题目。</h2>
              <p>
                你可以写“最近的学业疲惫”“晚上总是睡不沉”，也可以什么都不填，让系统帮你留出一个新的入口。
              </p>
            </div>

            <label class="field-label" for="session-title">会话标题</label>
            <input
              id="session-title"
              v-model="createForm.title"
              class="text-input"
              type="text"
              maxlength="100"
              placeholder="例如：这周的心口发紧、对未来的担心、难以入睡的夜晚"
            />

            <div class="compose-footer">
              <p class="helper-text">留空也可以，系统会用“新的倾诉会话”作为默认标题。</p>
              <button class="create-button" type="button" :disabled="creating" @click="createSession">
                {{ creating ? '创建中…' : '创建并进入' }}
              </button>
            </div>
          </article>

          <article class="ritual-panel">
            <p class="card-kicker">倾诉建议</p>
            <ul>
              <li>先写感受，不必急着把整件事解释清楚。</li>
              <li>如果你只想说一句话，也可以直接开始。</li>
              <li>当你愿意时，再把它扩展成一段更完整的对话。</li>
            </ul>
          </article>
        </div>

        <div class="studio-column studio-column--right">
          <div class="section-head">
            <div>
              <p class="card-kicker">历史会话</p>
              <h2>把每一次波动，都整理成有分寸的留白与层次。</h2>
            </div>
            <span class="section-badge">{{ loading ? '正在同步' : `${totalSessions} 条记录` }}</span>
          </div>

          <div v-if="loading" class="state-panel">正在加载你的 AI 会话档案…</div>
          <div v-else-if="errorMessage" class="state-panel state-panel--error">{{ errorMessage }}</div>
          <div v-else-if="!sessions.length" class="state-panel">
            还没有任何会话。左侧已经为你预留了新建入口，从一句最真实的话开始就够了。
          </div>

          <div v-else class="session-gallery">
            <article
              v-for="(session, index) in featuredSessions"
              :key="session.sessionId"
              class="session-card"
              :class="{ 'session-card--featured': index === 0 }"
              @click="openSession(session.sessionId)"
            >
              <div class="session-topline">
                <span class="session-order">会话 {{ String(index + 1).padStart(2, '0') }}</span>
                <span :class="resolveRiskClass(session)">{{ resolveRiskLabel(session) }}</span>
              </div>

              <h3>{{ session.title || `未命名会话 #${session.sessionId}` }}</h3>
              <p class="session-summary">
                {{ session.summaryText || '还没有摘要。进入会话后，新的对话会自动在这里留下痕迹。' }}
              </p>

              <div class="session-footer">
                <div class="session-meta">
                  <span>{{ session.status }}</span>
                  <span>创建于 {{ formatDateTime(session.createdAt) }}</span>
                  <span v-if="session.lastActiveAt">最近活跃 {{ formatDateTime(session.lastActiveAt) }}</span>
                </div>
                <span class="session-link">进入对话 →</span>
              </div>
            </article>
          </div>
        </div>
      </section>
    </div>
  </section>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700;800&family=Noto+Serif+SC:wght@400;500;600;700&display=swap');

.ai-list-page {
  min-height: 100%;
  padding: 22px 0 40px;
  color: #283129;
}

.page-shell {
  max-width: 1520px;
  margin: 0 auto;
  padding: 0 18px;
}

.hero-board {
  display: grid;
  grid-template-columns: minmax(0, 1.18fr) minmax(320px, 0.82fr);
  gap: 24px;
  margin-bottom: 28px;
}

.hero-copy,
.glass-panel,
.ritual-panel,
.featured-orb,
.session-card {
  border-radius: 30px;
  background: rgba(255, 252, 247, 0.88);
  box-shadow: 0 24px 64px rgba(67, 55, 39, 0.08);
  backdrop-filter: blur(18px);
}

.hero-copy {
  padding: 34px 36px 36px;
  background:
    radial-gradient(circle at 14% 18%, rgba(202, 217, 206, 0.66), transparent 26%),
    radial-gradient(circle at 86% 16%, rgba(235, 208, 181, 0.78), transparent 24%),
    linear-gradient(140deg, rgba(255, 250, 244, 0.94), rgba(251, 247, 241, 0.92));
}

.eyebrow,
.card-kicker,
.field-label,
.metric-label,
.session-order {
  margin: 0;
  font: 700 0.76rem/1 'Manrope', sans-serif;
  letter-spacing: 0.22em;
  text-transform: uppercase;
  color: #8a7661;
}

.hero-copy h1,
.compose-copy h2,
.section-head h2,
.featured-orb h2,
.session-card h3 {
  margin: 0;
  font-family: 'Noto Serif SC', serif;
  font-weight: 600;
  letter-spacing: -0.02em;
}

.hero-copy h1 {
  max-width: 10.5em;
  margin-top: 14px;
  font-size: clamp(2.8rem, 4vw, 5rem);
  line-height: 1.02;
}

.lead,
.compose-copy p,
.helper-text,
.featured-orb p,
.metric-caption,
.session-summary,
.session-meta,
.state-panel,
.ritual-panel ul {
  font-family: 'Manrope', sans-serif;
}

.lead {
  max-width: 760px;
  margin: 20px 0 0;
  font-size: 1rem;
  line-height: 1.95;
  color: rgba(40, 49, 41, 0.68);
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  align-items: center;
  margin-top: 30px;
}

.hero-action {
  border: none;
  border-radius: 999px;
  padding: 16px 24px;
  font: 700 0.84rem/1 'Manrope', sans-serif;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  cursor: pointer;
  transition: transform 0.28s ease, box-shadow 0.28s ease, opacity 0.28s ease;
}

.hero-action--dark {
  background: linear-gradient(135deg, #253129, #485c4d);
  color: #fffdf8;
  box-shadow: 0 18px 34px rgba(37, 49, 41, 0.18);
}

.hero-action:hover:not(:disabled),
.create-button:hover:not(:disabled),
.session-card:hover,
.featured-link:hover {
  transform: translateY(-2px);
}

.hero-action:disabled,
.create-button:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.hero-note {
  font: 600 0.92rem/1.7 'Manrope', sans-serif;
  color: rgba(40, 49, 41, 0.58);
}

.hero-side {
  display: flex;
}

.featured-orb {
  position: relative;
  flex: 1;
  overflow: hidden;
  padding: 28px;
  background:
    radial-gradient(circle at 18% 18%, rgba(178, 202, 185, 0.8), transparent 26%),
    radial-gradient(circle at 82% 24%, rgba(235, 199, 169, 0.9), transparent 22%),
    radial-gradient(circle at 56% 86%, rgba(248, 240, 229, 0.88), transparent 40%),
    linear-gradient(145deg, rgba(252, 248, 242, 0.96), rgba(255, 253, 248, 0.94));
}

.featured-orb::after {
  content: '';
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.16) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.16) 1px, transparent 1px);
  background-size: 38px 38px;
  opacity: 0.22;
  pointer-events: none;
}

.featured-orb h2 {
  margin-top: 14px;
  font-size: clamp(1.8rem, 2.4vw, 2.8rem);
  line-height: 1.12;
}

.featured-orb p {
  position: relative;
  z-index: 1;
  margin: 16px 0 0;
  max-width: 28rem;
  color: rgba(40, 49, 41, 0.72);
  line-height: 1.85;
}

.featured-meta {
  position: relative;
  z-index: 1;
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  margin-top: 28px;
  font: 700 0.8rem/1 'Manrope', sans-serif;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: rgba(40, 49, 41, 0.56);
}

.featured-link {
  border: none;
  background: rgba(255, 255, 255, 0.76);
  padding: 12px 16px;
  border-radius: 999px;
  font: 700 0.8rem/1 'Manrope', sans-serif;
  color: #3f5347;
  cursor: pointer;
  transition: transform 0.28s ease, box-shadow 0.28s ease;
}

.studio-grid {
  display: grid;
  grid-template-columns: minmax(320px, 0.78fr) minmax(0, 1.22fr);
  gap: 24px;
  align-items: start;
}

.studio-column {
  display: grid;
  gap: 20px;
}

.glass-panel {
  padding: 26px;
}

.metric-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: baseline;
  margin-bottom: 20px;
}

.metric-caption {
  color: rgba(40, 49, 41, 0.54);
  font-size: 0.88rem;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.metric-card {
  padding: 18px;
  border-radius: 22px;
  background: rgba(250, 245, 238, 0.82);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.5);
}

.metric-card--accent {
  background: linear-gradient(135deg, rgba(242, 231, 221, 0.94), rgba(250, 243, 235, 0.94));
}

.metric-card strong {
  display: block;
  margin-top: 10px;
  font-family: 'Noto Serif SC', serif;
  font-size: 2rem;
  font-weight: 600;
}

.compose-panel {
  background:
    linear-gradient(180deg, rgba(255, 252, 247, 0.92), rgba(252, 248, 241, 0.92));
}

.compose-copy h2 {
  margin-top: 12px;
  font-size: 2rem;
  line-height: 1.15;
}

.compose-copy p {
  margin: 16px 0 0;
  color: rgba(40, 49, 41, 0.68);
  line-height: 1.84;
}

.field-label {
  display: block;
  margin-top: 24px;
  margin-bottom: 10px;
}

.text-input {
  width: 100%;
  border: none;
  outline: none;
  border-radius: 22px;
  background: #f5efe7;
  padding: 18px 20px;
  font: 500 0.98rem/1.5 'Manrope', sans-serif;
  color: #283129;
  transition: box-shadow 0.28s ease, transform 0.28s ease;
}

.text-input:focus {
  transform: translateY(-1px);
  box-shadow: 0 18px 36px rgba(79, 95, 83, 0.12);
}

.compose-footer {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  margin-top: 18px;
}

.helper-text {
  margin: 0;
  color: rgba(40, 49, 41, 0.56);
  line-height: 1.72;
}

.create-button {
  flex-shrink: 0;
  border: none;
  border-radius: 18px;
  padding: 14px 18px;
  background: linear-gradient(135deg, #2c382f, #55695b);
  color: #fffdf9;
  font: 700 0.82rem/1 'Manrope', sans-serif;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  cursor: pointer;
  transition: transform 0.28s ease, box-shadow 0.28s ease, opacity 0.28s ease;
}

.create-button:hover:not(:disabled) {
  box-shadow: 0 16px 28px rgba(44, 56, 47, 0.18);
}

.ritual-panel {
  padding: 24px 26px;
  background: linear-gradient(180deg, rgba(244, 238, 230, 0.82), rgba(255, 252, 246, 0.84));
}

.ritual-panel ul {
  margin: 16px 0 0;
  padding-left: 1.2rem;
  color: rgba(40, 49, 41, 0.72);
  line-height: 1.9;
}

.section-head {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: end;
}

.section-head h2 {
  margin-top: 12px;
  max-width: 12em;
  font-size: clamp(2rem, 2.8vw, 3.2rem);
  line-height: 1.08;
}

.section-badge {
  display: inline-flex;
  align-items: center;
  padding: 12px 16px;
  border-radius: 999px;
  background: rgba(255, 252, 247, 0.9);
  box-shadow: 0 14px 32px rgba(66, 55, 39, 0.06);
  font: 700 0.78rem/1 'Manrope', sans-serif;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: #6f7d70;
}

.state-panel {
  min-height: 260px;
  display: grid;
  place-items: center;
  padding: 28px;
  border-radius: 30px;
  background: rgba(255, 252, 247, 0.78);
  color: rgba(40, 49, 41, 0.62);
  text-align: center;
  line-height: 1.9;
}

.state-panel--error {
  color: #a14f42;
  background: rgba(247, 233, 229, 0.88);
}

.session-gallery {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.session-card {
  padding: 22px;
  cursor: pointer;
  transition: transform 0.32s ease, box-shadow 0.32s ease;
}

.session-card:hover {
  box-shadow: 0 30px 72px rgba(67, 55, 39, 0.11);
}

.session-card--featured {
  grid-column: span 2;
  background:
    radial-gradient(circle at 15% 18%, rgba(201, 217, 205, 0.54), transparent 24%),
    radial-gradient(circle at 86% 14%, rgba(236, 205, 177, 0.56), transparent 20%),
    linear-gradient(145deg, rgba(255, 251, 245, 0.96), rgba(252, 247, 240, 0.96));
}

.session-topline {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
}

.session-card h3 {
  margin-top: 18px;
  font-size: 1.7rem;
  line-height: 1.14;
}

.session-card--featured h3 {
  font-size: 2.05rem;
}

.session-summary {
  margin: 14px 0 0;
  color: rgba(40, 49, 41, 0.68);
  line-height: 1.86;
}

.session-footer {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: end;
  margin-top: 22px;
}

.session-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 16px;
  font-size: 0.84rem;
  color: rgba(40, 49, 41, 0.56);
}

.session-link {
  flex-shrink: 0;
  font: 700 0.8rem/1 'Manrope', sans-serif;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #55675c;
}

.risk-pill {
  display: inline-flex;
  align-items: center;
  padding: 9px 12px;
  border-radius: 999px;
  font: 700 0.74rem/1 'Manrope', sans-serif;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.risk-pill--low {
  background: rgba(235, 242, 235, 0.92);
  color: #5f7767;
}

.risk-pill--medium {
  background: rgba(244, 233, 220, 0.94);
  color: #9b6e49;
}

.risk-pill--high {
  background: rgba(244, 228, 225, 0.94);
  color: #9e5043;
}

@media (max-width: 1200px) {
  .hero-board,
  .studio-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 840px) {
  .page-shell {
    padding: 0 8px;
  }

  .metric-grid,
  .session-gallery {
    grid-template-columns: 1fr;
  }

  .session-card--featured {
    grid-column: span 1;
  }

  .compose-footer,
  .section-head,
  .session-footer,
  .featured-meta,
  .metric-head {
    flex-direction: column;
    align-items: start;
  }
}

@media (max-width: 640px) {
  .ai-list-page {
    padding: 8px 0 24px;
  }

  .hero-copy,
  .glass-panel,
  .ritual-panel,
  .featured-orb,
  .session-card {
    border-radius: 24px;
  }

  .hero-copy {
    padding: 26px 22px 28px;
  }

  .hero-copy h1,
  .section-head h2,
  .compose-copy h2,
  .featured-orb h2 {
    font-size: 1.95rem;
  }
}
</style>
