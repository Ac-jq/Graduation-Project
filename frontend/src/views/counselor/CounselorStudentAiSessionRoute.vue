<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { fetchCounselorStudentAiSessionMessagesApi } from '@/api/ai-chat'
import type { AiChatMessage } from '@/api/types'
import { toErrorMessage, toNumberParam } from '@/views/shared/page-logic'

const route = useRoute()
const loading = ref(false)
const errorMessage = ref('')
const messages = ref<AiChatMessage[]>([])
const studentUserId = computed(() => toNumberParam(route.params.studentUserId))
const sessionId = computed(() => toNumberParam(route.params.sessionId))

async function loadMessages(): Promise<void> {
  if (!studentUserId.value || !sessionId.value) {
    errorMessage.value = 'Invalid route params'
    messages.value = []
    return
  }

  loading.value = true
  errorMessage.value = ''

  try {
    messages.value = await fetchCounselorStudentAiSessionMessagesApi(studentUserId.value, sessionId.value)
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    loading.value = false
  }
}

watch(() => [route.params.studentUserId, route.params.sessionId], () => {
  void loadMessages()
})

onMounted(() => {
  void loadMessages()
})
</script>

<template>
  <section class="c-ai-detail-page">
    <div class="page-shell">
      <header class="page-hero">
        <div class="hero-copy">
          <p class="eyebrow">AI Session Detail</p>
          <h1>以只读方式回看学生与 AI 的对话节奏，辅助你理解情绪线索的演化。</h1>
          <p class="lead">当前查看学生 #{{ studentUserId || '-' }} 的会话 #{{ sessionId || '-' }}。</p>
        </div>
        <div class="hero-metric">
          <span>Messages量</span>
          <strong>{{ messages.length }}</strong>
        </div>
      </header>

      <p v-if="loading" class="state-text">正在同步消息记录...</p>
      <p v-else-if="errorMessage" class="error-text">{{ errorMessage }}</p>
      <p v-else-if="!messages.length" class="state-text">当前会话没有消息。</p>

      <div v-else class="message-stack">
        <article v-for="message in messages" :key="message.messageId" class="message-card" :class="{ 'message-card--student': message.senderType === 'STUDENT' }">
          <div class="message-meta">
            <span class="message-role">{{ message.senderType === 'STUDENT' ? '学生' : 'AI 导师' }}</span>
            <span>{{ new Date(message.createdAt).toLocaleString('zh-CN') }}</span>
            <span v-if="message.riskLevel">Level {{ message.riskLevel }}</span>
          </div>
          <p class="message-content">{{ message.content }}</p>
          <p v-if="message.hitKeywords" class="message-tip">命中关键词：{{ message.hitKeywords }}</p>
        </article>
      </div>
    </div>
  </section>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700&family=Noto+Serif+SC:wght@400;500;600;700&display=swap');

.c-ai-detail-page{min-height:100vh;padding:44px 28px 72px;color:#283128;background:linear-gradient(180deg,#f5f0e5 0%,#f8f4ed 100%)}
.page-shell{max-width:1240px;margin:0 auto}.page-hero{display:grid;grid-template-columns:minmax(0,1.35fr) 220px;gap:28px;align-items:end;margin-bottom:30px}.hero-copy{border-top:1px solid rgba(59,69,59,.16);padding-top:18px}.eyebrow,.message-role{margin:0;font:700 .76rem/1 'Manrope',sans-serif;letter-spacing:.22em;text-transform:uppercase;color:#7b6857}.hero-copy h1{margin:0;font:600 clamp(2rem,3vw,3.2rem)/1.16 'Noto Serif SC',serif}.lead,.message-meta,.message-content,.message-tip,.state-text,.error-text{font-family:'Manrope',sans-serif}.lead{margin:18px 0 0;line-height:1.84;color:rgba(40,49,40,.72)}.hero-metric,.message-card{border:1px solid rgba(77,86,77,.14);background:rgba(255,252,247,.76);box-shadow:0 24px 70px rgba(91,80,66,.08);backdrop-filter:blur(16px)}.hero-metric{padding:18px 20px}.hero-metric span{display:block;margin-bottom:8px;font:700 .78rem/1 'Manrope',sans-serif;letter-spacing:.16em;text-transform:uppercase;color:rgba(68,74,66,.56)}.hero-metric strong{font:600 1.6rem/1 'Noto Serif SC',serif}.message-stack{display:grid;gap:16px}.message-card{padding:20px}.message-card--student{margin-left:64px}.message-meta{display:flex;flex-wrap:wrap;gap:10px 14px;font-size:.82rem;color:rgba(40,49,40,.58);margin-bottom:10px}.message-content{margin:0;white-space:pre-wrap;font-size:.98rem;line-height:1.92;color:#283128}.message-tip{margin:10px 0 0;font-size:.84rem;line-height:1.6;color:#8a533d}.error-text{color:#a44f46}
@media (max-width:900px){.c-ai-detail-page{padding:28px 16px 46px}.page-hero{grid-template-columns:1fr}.message-card--student{margin-left:0}}
</style>

