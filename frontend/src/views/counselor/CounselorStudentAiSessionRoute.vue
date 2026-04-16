<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchCounselorStudentAiSessionMessagesApi } from '@/api/ai-chat'
import type { AiChatMessage } from '@/api/types'
import { toErrorMessage, toNumberParam } from '@/views/shared/page-logic'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const errorMessage = ref('')
const messages = ref<AiChatMessage[]>([])

const studentUserId = computed(() => toNumberParam(route.params.studentUserId))
const sessionId = computed(() => toNumberParam(route.params.sessionId))

const alertCount = computed(() => messages.value.filter(m => m.riskLevel || m.hitKeywords).length)

function formatTime(value: string | Date): string {
  const d = new Date(value)
  return `${String(d.getMonth() + 1).padStart(2, '0')}/${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

async function loadMessages(): Promise<void> {
  if (!studentUserId.value || !sessionId.value) {
    errorMessage.value = '无法定位到有效的会话参数'
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

function goBack(): void {
  if (studentUserId.value) {
    router.push({ name: 'counselor-student-ai-sessions', params: { studentUserId: studentUserId.value } })
  } else {
    router.push({ name: 'counselor-students' })
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
  <main class="editorial-transcript-page">
    <div class="page-container">

      <nav class="dossier-nav">
        <button class="nav-ghost-btn" @click="goBack">
          <span class="arrow">←</span> 返回 AI 访谈案卷
        </button>
      </nav>

      <header class="transcript-header">
        <div class="header-main">
          <span class="header-tag">Session Transcript</span>
          <h1 class="huge-title">会话实录</h1>
          <p class="header-lead">
            当前正在调阅学生 <strong>#{{ studentUserId || '-' }}</strong> 的第 <strong>#{{ sessionId || '-' }}</strong> 次 AI 会谈。以下为未经删减的原始对话记录，请留意附带高亮的情绪风险词汇。
          </p>
        </div>

        <div class="header-stats">
          <div class="stat-item">
            <span class="stat-label">发言总数</span>
            <span class="stat-value">{{ loading ? '-' : messages.length }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">触发关注</span>
            <span class="stat-value" :class="{ 'highlight': alertCount > 0 }">{{ loading ? '-' : alertCount }}</span>
          </div>
        </div>
      </header>

      <div v-if="errorMessage" class="error-banner">{{ errorMessage }}</div>

      <div v-if="loading" class="loading-state">
        <div class="spinner"></div>
        <p>正在解密并同步对话实录...</p>
      </div>

      <div v-else-if="!messages.length" class="empty-state">
        <h2 class="empty-title">实录空白</h2>
        <p class="empty-desc">该会话中尚未产生任何发言记录。</p>
      </div>

      <section v-else class="transcript-stream-section">
        <div class="thick-accent-line"></div>
        <div class="stream-toolbar">
          <span class="toolbar-status">BEGIN TRANSCRIPT</span>
        </div>

        <div class="transcript-stream">
          <article
              v-for="message in messages"
              :key="message.messageId"
              class="transcript-row"
              :class="{ 'is-student': message.senderType === 'STUDENT' }"
          >
            <div class="row-actor-col">
              <span class="actor-name">{{ message.senderType === 'STUDENT' ? '来访学生' : 'AI 导师' }}</span>
              <span class="actor-time">{{ formatTime(message.createdAt) }}</span>
            </div>

            <div class="row-content-col">
              <p class="message-content">{{ message.content }}</p>

              <div v-if="message.riskLevel || message.hitKeywords" class="message-annotations">
                <span v-if="message.riskLevel" class="annotation-pill risk-pill">
                  Level {{ message.riskLevel }}
                </span>
                <span v-if="message.hitKeywords" class="annotation-pill keyword-pill">
                  触发关键词: {{ message.hitKeywords }}
                </span>
              </div>
            </div>
          </article>
        </div>

        <div class="stream-footer">
          <span class="toolbar-status">END OF TRANSCRIPT</span>
        </div>
      </section>

    </div>
  </main>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700;800&family=Noto+Serif+SC:wght@500;600;700&display=swap');

/* 全局极简白纸底色 */
.editorial-transcript-page {
  min-height: 100vh;
  background: #fcfbf9;
  color: #1e2821;
  font-family: 'Manrope', 'Noto Serif SC', sans-serif;
  padding: 2rem 2vw 8rem;
  box-sizing: border-box;
}

.page-container {
  max-width: 960px; /* 剧本阅读的最佳行宽较小，因此收紧容器 */
  margin: 0 auto;
}

/* 顶部导航 */
.dossier-nav {
  margin-bottom: 3rem;
}

.nav-ghost-btn {
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
  transition: color 0.3s ease;
}

.nav-ghost-btn:hover {
  color: #1e2821;
}

/* 头部排版 */
.transcript-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  padding-bottom: 3rem;
  margin-bottom: 3rem;
  border-bottom: 1px solid rgba(42, 54, 46, 0.12);
  gap: 4rem;
}

.header-main {
  max-width: 600px;
}

.header-tag {
  display: block;
  font-family: 'Manrope', sans-serif;
  font-size: 0.85rem;
  font-weight: 700;
  letter-spacing: 0.15em;
  color: #8a9c90;
  text-transform: uppercase;
  margin-bottom: 1rem;
}

.huge-title {
  font-family: 'Noto Serif SC', serif;
  font-size: 2.8rem;
  font-weight: 600;
  color: #1e2821;
  margin: 0 0 1.2rem 0;
  letter-spacing: 0.05em;
}

.header-lead {
  font-size: 1.05rem;
  color: #5c6b60;
  line-height: 1.8;
  margin: 0;
}

.header-lead strong {
  color: #2a362e;
}

.header-stats {
  display: flex;
  gap: 3rem;
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.stat-label {
  font-family: 'Noto Serif SC', serif;
  font-size: 0.85rem;
  color: #8a9c90;
}

.stat-value {
  font-family: 'Manrope', sans-serif;
  font-size: 2.4rem;
  font-weight: 600;
  color: #2a362e;
  line-height: 1;
}

.stat-value.highlight {
  color: #8c4a4a;
}

/* 实录流区域 */
.transcript-stream-section {
  display: flex;
  flex-direction: column;
}

.thick-accent-line {
  width: 100%;
  height: 4px;
  background: #2a362e;
  margin-bottom: 1rem;
}

.stream-toolbar,
.stream-footer {
  display: flex;
  justify-content: center;
  padding: 1.5rem 0;
}

.toolbar-status {
  font-family: 'Manrope', sans-serif;
  font-size: 0.85rem;
  font-weight: 800;
  letter-spacing: 0.2em;
  color: #b5c2b9;
}

.transcript-stream {
  display: flex;
  flex-direction: column;
}

/* 剧本分栏排版 */
.transcript-row {
  display: grid;
  grid-template-columns: 140px minmax(0, 1fr);
  gap: 3.5rem;
  padding: 2.5rem 0;
  border-bottom: 1px solid rgba(42, 54, 46, 0.08);
  align-items: start;
}

/* 左侧角色栏 */
.row-actor-col {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
  text-align: right;
  padding-top: 0.3rem; /* 对齐右侧首行中文字体 */
}

.actor-name {
  font-family: 'Noto Serif SC', serif;
  font-size: 1rem;
  font-weight: 600;
  letter-spacing: 0.05em;
  color: #5c7062; /* AI 导师的莫兰迪绿 */
}

.is-student .actor-name {
  color: #8c6a5c; /* 来访学生的莫兰迪锈红 */
}

.actor-time {
  font-family: 'Manrope', sans-serif;
  font-size: 0.85rem;
  color: #8a9c90;
}

/* 右侧正文栏 */
.row-content-col {
  display: flex;
  flex-direction: column;
  gap: 1.2rem;
}

.message-content {
  font-family: 'Noto Serif SC', serif;
  font-size: 1.15rem;
  line-height: 1.85;
  color: #1e2821;
  margin: 0;
  white-space: pre-wrap; /* 保持后端返回的换行格式 */
}

/* 风险批注（Annotations） */
.message-annotations {
  display: flex;
  flex-wrap: wrap;
  gap: 0.8rem;
  padding-top: 0.5rem;
}

.annotation-pill {
  font-family: 'Noto Serif SC', serif;
  font-size: 0.85rem;
  font-weight: 600;
  padding: 0.4rem 0.8rem;
  border-radius: 6px;
  display: inline-flex;
  align-items: center;
}

.risk-pill {
  background: rgba(140, 74, 74, 0.1);
  color: #8c4a4a;
  border: 1px solid rgba(140, 74, 74, 0.2);
}

.keyword-pill {
  background: rgba(193, 150, 83, 0.1);
  color: #9e7330;
  border: 1px solid rgba(193, 150, 83, 0.2);
}

/* 状态提示 */
.error-banner {
  background: rgba(140, 74, 74, 0.08);
  color: #8c4a4a;
  padding: 1.5rem;
  border-radius: 12px;
  text-align: center;
  font-family: 'Noto Serif SC', serif;
  margin-bottom: 2rem;
}

.loading-state,
.empty-state {
  text-align: center;
  padding: 8rem 0;
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

.empty-title {
  font-family: 'Noto Serif SC', serif;
  font-size: 1.6rem;
  color: #2a362e;
  margin: 0 0 1rem 0;
}

/* 交互动画 */
.arrow {
  font-family: 'Manrope', sans-serif;
  transition: transform 0.3s ease;
}

.nav-ghost-btn:hover .arrow {
  transform: translateX(-4px);
}

/* 响应式 */
@media (max-width: 900px) {
  .transcript-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 2rem;
  }

  .header-stats {
    flex-wrap: wrap;
    gap: 2rem;
  }
}

@media (max-width: 600px) {
  .transcript-row {
    grid-template-columns: 1fr; /* 移动端变为上下结构 */
    gap: 1rem;
    padding: 2rem 0;
  }

  .row-actor-col {
    flex-direction: row;
    align-items: baseline;
    justify-content: flex-start;
    gap: 0.8rem;
    text-align: left;
    padding-bottom: 0.5rem;
  }
}
</style>