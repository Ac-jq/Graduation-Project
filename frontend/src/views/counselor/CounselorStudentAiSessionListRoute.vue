<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchCounselorStudentAiSessionsApi } from '@/api/ai-chat'
import type { AiChatSession } from '@/api/types'
import { toErrorMessage, toNumberParam } from '@/views/shared/page-logic'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const errorMessage = ref('')
const sessions = ref<AiChatSession[]>([])
const studentUserId = computed(() => toNumberParam(route.params.studentUserId))

async function loadSessions(): Promise<void> {
  if (!studentUserId.value) {
    errorMessage.value = 'Invalid studentUserId'
    sessions.value = []
    return
  }

  loading.value = true
  errorMessage.value = ''

  try {
    sessions.value = await fetchCounselorStudentAiSessionsApi(studentUserId.value)
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    loading.value = false
  }
}

async function openSession(sessionId: number): Promise<void> {
  if (!studentUserId.value) {
    return
  }

  await router.push({ name: 'counselor-student-ai-session-detail', params: { studentUserId: studentUserId.value, sessionId } })
}

watch(() => route.params.studentUserId, () => {
  void loadSessions()
})

onMounted(() => {
  void loadSessions()
})
</script>

<template>
  <section class="c-ai-list-page">
    <div class="page-shell">
      <header class="page-hero">
        <div class="hero-copy">
          <p class="eyebrow">Student AI Sessions</p>
          <h1>浏览学生与 AI 导师的历史会话，快速定位Level与对话脉络。</h1>
          <p class="lead">当前查看学生 #{{ studentUserId || '-' }} 的 AI 会话档案。</p>
        </div>
        <div class="hero-metric">
          <span>会话Total</span>
          <strong>{{ sessions.length }}</strong>
        </div>
      </header>

      <p v-if="loading" class="state-text">正在同步 AI 会话列表...</p>
      <p v-else-if="errorMessage" class="error-text">{{ errorMessage }}</p>
      <p v-else-if="!sessions.length" class="state-text">当前学生暂无 AI 会话。</p>

      <div v-else class="session-stack">
        <article v-for="session in sessions" :key="session.sessionId" class="session-card" @click="openSession(session.sessionId)">
          <div class="session-topline">
            <div>
              <p class="session-code">Session #{{ session.sessionId }}</p>
              <h2>{{ session.title || `未命名会话 #${session.sessionId}` }}</h2>
            </div>
            <span class="risk-pill" :class="{ 'risk-pill--alert': Boolean(session.riskFlag) }">{{ session.riskLevel || '常规' }}</span>
          </div>
          <p class="session-summary">{{ session.summaryText || '暂无摘要。' }}</p>
          <div class="session-meta">
            <span>{{ session.status }}</span>
            <span>{{ new Date(session.createdAt).toLocaleString('zh-CN') }}</span>
            <span v-if="session.lastActiveAt">活跃于 {{ new Date(session.lastActiveAt).toLocaleString('zh-CN') }}</span>
          </div>
        </article>
      </div>
    </div>
  </section>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700&family=Noto+Serif+SC:wght@400;500;600;700&display=swap');

.c-ai-list-page{min-height:100vh;padding:44px 28px 72px;color:#283128;background:linear-gradient(180deg,#f5f0e5 0%,#f8f4ed 100%)}
.page-shell{max-width:1240px;margin:0 auto}.page-hero{display:grid;grid-template-columns:minmax(0,1.35fr) 220px;gap:28px;align-items:end;margin-bottom:30px}.hero-copy{border-top:1px solid rgba(59,69,59,.16);padding-top:18px}.eyebrow,.session-code{margin:0 0 10px;font:700 .76rem/1 'Manrope',sans-serif;letter-spacing:.22em;text-transform:uppercase;color:#7b6857}.hero-copy h1,.session-card h2{margin:0;font-family:'Noto Serif SC',serif;font-weight:600}.hero-copy h1{font-size:clamp(2rem,3vw,3.2rem);line-height:1.16}.lead,.session-summary,.session-meta,.state-text,.error-text{font-family:'Manrope',sans-serif}.lead{margin:18px 0 0;line-height:1.84;color:rgba(40,49,40,.72)}.hero-metric,.session-card{border:1px solid rgba(77,86,77,.14);background:rgba(255,252,247,.76);box-shadow:0 24px 70px rgba(91,80,66,.08);backdrop-filter:blur(16px)}.hero-metric{padding:18px 20px}.hero-metric span{display:block;margin-bottom:8px;font:700 .78rem/1 'Manrope',sans-serif;letter-spacing:.16em;text-transform:uppercase;color:rgba(68,74,66,.56)}.hero-metric strong{font:600 1.6rem/1 'Noto Serif SC',serif}.session-stack{display:grid;gap:18px}.session-card{padding:22px;cursor:pointer;transition:transform .28s ease, box-shadow .28s ease}.session-card:hover{transform:translateY(-3px);box-shadow:0 28px 54px rgba(86,106,92,.12)}.session-topline{display:flex;justify-content:space-between;gap:16px;align-items:start}.session-card h2{font-size:1.34rem;line-height:1.35}.risk-pill{border:1px solid rgba(97,111,98,.15);background:rgba(242,244,237,.94);padding:8px 12px;font:700 .74rem/1 'Manrope',sans-serif;letter-spacing:.12em;text-transform:uppercase;color:#66735f}.risk-pill--alert{color:#8c4f37;background:rgba(239,225,217,.95);border-color:rgba(140,79,55,.22)}.session-summary{margin:14px 0 0;font-size:.96rem;line-height:1.86;color:rgba(40,49,40,.7)}.session-meta{display:flex;flex-wrap:wrap;gap:10px 18px;margin-top:14px;font-size:.84rem;color:rgba(40,49,40,.58)}.error-text{color:#a44f46}
@media (max-width:900px){.c-ai-list-page{padding:28px 16px 46px}.page-hero{grid-template-columns:1fr}.session-topline{flex-direction:column;align-items:start}}
</style>

