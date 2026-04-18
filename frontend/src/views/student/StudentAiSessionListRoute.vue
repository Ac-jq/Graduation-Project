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
const currentPage = ref(1)
const pageSize = 6

const createForm = reactive({
  title: ''
})

const totalSessions = computed(() => sessions.value.length)
const totalPages = computed(() => Math.max(1, Math.ceil(sessions.value.length / pageSize)))
const pagedSessions = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return sessions.value.slice(start, start + pageSize)
})

function formatDateTime(value: string | null | undefined): string {
  if (!value) return '刚刚'
  const d = new Date(value)
  return `${d.getFullYear()}/${String(d.getMonth() + 1).padStart(2, '0')}/${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

function resolveSessionStatusText(status: string | null | undefined): string {
  switch (status) {
    case 'ACTIVE': return '进行中'
    case 'ARCHIVED': return '已封存'
    case 'CLOSED': return '已结束'
    default: return '进行中'
  }
}

async function loadSessions(): Promise<void> {
  loading.value = true
  errorMessage.value = ''

  try {
    sessions.value = await fetchStudentAiSessionsApi()
    currentPage.value = 1
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    loading.value = false
  }
}

function prevPage(): void {
  if (currentPage.value > 1) {
    currentPage.value--
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
}

function nextPage(): void {
  if (currentPage.value < totalPages.value) {
    currentPage.value++
    window.scrollTo({ top: 0, behavior: 'smooth' })
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
  <main class="split-editorial-page">
    <div class="page-container">

      <aside class="editorial-sidebar">
        <div class="sidebar-sticky">

          <header class="side-header">
            <span class="eyebrow">Mindful Journal</span>
            <h1 class="side-title">倾诉手札</h1>
            <p class="side-lead">
              把平时难以消化的情绪和没有说出口的话，留在这个安静的画板里。你可以随时回顾过去的记录，或者开启一段新的倾诉。
            </p>
          </header>

          <div class="thick-accent-line"></div>

          <div class="compose-module">
            <h3 class="module-title">开启新的对话</h3>
            <p class="module-text">
              给这次的心情起个名字。留空也可以，系统会自动为你准备好空间。
            </p>

            <input
                v-model="createForm.title"
                class="sleek-input"
                type="text"
                maxlength="100"
                placeholder="例如：这周的学业焦虑、失眠的夜晚..."
            />

            <button class="action-btn" type="button" :disabled="creating" @click="createSession">
              {{ creating ? '准备房间中...' : '进入安静房间' }} <span class="arrow">→</span>
            </button>
          </div>

          <div class="side-tips">
            <h3 class="tips-title">倾诉建议</h3>
            <ul class="tips-list">
              <li>先写感受，不必急着把整件事解释清楚。</li>
              <li>如果你只想说一句话，也可以直接开始。</li>
              <li>当你愿意时，AI 导师会陪你把它扩展成一段更完整的对话。</li>
            </ul>
          </div>

        </div>
      </aside>

      <section class="editorial-stream-area">

        <div class="stream-toolbar">
          <span class="toolbar-status">你的记忆抽屉：共 {{ totalSessions }} 段档案</span>
        </div>

        <div v-if="errorMessage" class="error-banner">{{ errorMessage }}</div>

        <div class="archive-wrapper">

          <div v-if="loading" class="loading-state">
            <div class="spinner"></div>
            <p>正在同步你的过往记录...</p>
          </div>

          <div v-else-if="!sessions.length" class="empty-state">
            <p class="empty-desc">当前还没有记录。<br>在左侧输入你此刻的想法，开启你的第一条手札。</p>
          </div>

          <div v-else class="archive-stream">
            <article
                v-for="(session, index) in pagedSessions"
                :key="session.sessionId"
                class="archive-row"
                @click="openSession(session.sessionId)"
            >
              <div class="row-left">
                <span class="session-id">#{{ session.sessionId }}</span>
                <span class="session-date">{{ formatDateTime(session.createdAt) }}</span>
                <span class="session-status">{{ resolveSessionStatusText(session.status) }}</span>
              </div>

              <div class="row-center">
                <h3 class="session-title">{{ session.title || '未命名会话' }}</h3>
                <p class="session-summary">{{ session.summaryText || '暂无对话内容。进入房间后，你写下的话会作为摘要展示在这里。' }}</p>
              </div>

              <div class="row-right">
                <span class="action-link">继续对话 <span class="arrow">→</span></span>
              </div>
            </article>
          </div>
        </div>

        <nav class="pagination-nav" v-if="totalPages > 1">
          <button class="page-btn" :disabled="currentPage <= 1" @click="prevPage">
            <span class="arrow">←</span> 上一页
          </button>

          <div class="page-indicator">
            <span>{{ currentPage }}</span> / <span>{{ totalPages }}</span>
          </div>

          <button class="page-btn" :disabled="currentPage >= totalPages" @click="nextPage">
            下一页 <span class="arrow">→</span>
          </button>
        </nav>

      </section>

    </div>
  </main>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700;800&family=Noto+Serif+SC:wght@500;600;700&display=swap');

/* 全局极简纸张底色 */
.split-editorial-page {
  min-height: 100vh;
  background: #fcfbf9;
  color: #1e2821;
  font-family: 'Manrope', 'Noto Serif SC', sans-serif;
  padding: 4rem 2vw 8rem;
  box-sizing: border-box;
}

.page-container {
  max-width: 1100px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: 340px minmax(0, 1fr);
  gap: 6rem;
  align-items: start;
}

/* ================= 左侧控制台 ================= */
.editorial-sidebar {
  position: relative;
}

.sidebar-sticky {
  position: sticky;
  top: 4rem;
  display: flex;
  flex-direction: column;
  gap: 3rem;
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

.side-title {
  font-family: 'Noto Serif SC', serif;
  font-size: 2.6rem;
  font-weight: 600;
  color: #1e2821;
  margin: 0;
  line-height: 1.2;
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
  height: 4px;
  background: #2a362e;
}

/* 新建模块 */
.compose-module {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.module-title {
  font-family: 'Noto Serif SC', serif;
  font-size: 1.3rem;
  font-weight: 600;
  color: #1e2821;
  margin: 0;
}

.module-text {
  font-size: 0.95rem;
  line-height: 1.8;
  color: #5c6b60;
  margin: 0;
}

.sleek-input {
  width: 100%;
  border: none;
  border-bottom: 1px dashed rgba(42, 54, 46, 0.25);
  background: transparent;
  padding: 0.8rem 0;
  font-family: 'Noto Serif SC', serif;
  font-size: 1.05rem;
  color: #1e2821;
  outline: none;
  transition: border-color 0.3s ease;
  margin-bottom: 0.5rem;
}

.sleek-input::placeholder {
  color: #b5c2b9;
  font-style: italic;
}

.sleek-input:focus {
  border-bottom-color: #2a362e;
  border-bottom-style: solid;
}

.action-btn {
  align-self: flex-start;
  background: #2a362e;
  border: none;
  color: #ffffff;
  padding: 1.2rem 2.2rem;
  border-radius: 100px;
  font-family: 'Noto Serif SC', serif;
  font-size: 1.05rem;
  font-weight: 600;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 0.6rem;
  transition: all 0.3s ease;
  box-shadow: 0 12px 24px rgba(42, 54, 46, 0.15);
}

.action-btn:hover:not(:disabled) {
  background: #1c2620;
  transform: translateY(-2px);
  box-shadow: 0 16px 32px rgba(42, 54, 46, 0.25);
}

.action-btn:disabled {
  background: #8a9c90;
  box-shadow: none;
  cursor: not-allowed;
}

/* 提示建议区 */
.tips-title {
  font-family: 'Noto Serif SC', serif;
  font-size: 1.15rem;
  font-weight: 600;
  color: #1e2821;
  margin: 0 0 1rem 0;
}

.tips-list {
  margin: 0;
  padding-left: 1.2rem;
  color: #5c6b60;
  font-size: 0.95rem;
  line-height: 1.8;
}

.tips-list li {
  margin-bottom: 0.5rem;
}

/* ================= 右侧手札档案流 ================= */
.editorial-stream-area {
  display: flex;
  flex-direction: column;
}

.stream-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 1.5rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid rgba(42, 54, 46, 0.15);
}

.toolbar-status {
  font-family: 'Noto Serif SC', serif;
  font-size: 0.95rem;
  font-weight: 600;
  color: #8a9c90;
}

.archive-wrapper {
  display: flex;
  flex-direction: column;
}

.archive-stream {
  display: flex;
  flex-direction: column;
}

.archive-row {
  display: grid;
  grid-template-columns: 100px minmax(0, 1fr) auto;
  gap: 2.5rem;
  padding: 2.5rem 0;
  border-bottom: 1px solid rgba(42, 54, 46, 0.08);
  cursor: pointer;
  transition: background 0.4s ease;
  align-items: start;
}

.archive-row:hover {
  background: rgba(255, 255, 255, 0.5);
}

/* 极简信息戳 */
.row-left {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
  padding-top: 0.2rem;
}

.session-id {
  font-family: 'Manrope', sans-serif;
  font-size: 0.85rem;
  font-weight: 800;
  letter-spacing: 0.1em;
  color: #8a9c90;
}

.session-date {
  font-family: 'Manrope', sans-serif;
  font-size: 1rem;
  font-weight: 600;
  color: #2a362e;
}

.session-status {
  font-family: 'Noto Serif SC', serif;
  font-size: 0.85rem;
  color: #5c6b60;
  margin-top: 0.5rem;
}

/* 正文标题与摘要 */
.row-center {
  display: flex;
  flex-direction: column;
  gap: 0.8rem;
}

.session-title {
  font-family: 'Noto Serif SC', serif;
  font-size: 1.4rem;
  font-weight: 600;
  color: #1e2821;
  margin: 0;
}

.session-summary {
  font-family: 'Noto Serif SC', serif;
  font-size: 1.05rem;
  color: #7b8c80;
  line-height: 1.8;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 右侧链接 */
.row-right {
  display: flex;
  align-items: flex-start;
  padding-top: 0.2rem;
}

.action-link {
  background: transparent;
  font-family: 'Noto Serif SC', serif;
  font-size: 1.05rem;
  font-weight: 600;
  color: #b5c2b9;
  transition: color 0.3s ease;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.archive-row:hover .action-link {
  color: #2a362e;
}

/* 状态样式 */
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

.empty-desc {
  line-height: 1.8;
}

/* 分页器 */
.pagination-nav {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 2rem;
  margin-top: 4rem;
  padding-top: 2rem;
  border-top: 1px solid rgba(42, 54, 46, 0.08);
}

.page-btn {
  background: transparent;
  border: none;
  font-family: 'Noto Serif SC', serif;
  font-size: 1.05rem;
  font-weight: 600;
  color: #2a362e;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  transition: all 0.3s ease;
}

.page-btn:hover:not(:disabled) {
  color: #5c6b60;
}

.page-btn:disabled {
  color: #cbd5cf;
  cursor: not-allowed;
}

.page-indicator {
  font-family: 'Manrope', sans-serif;
  font-size: 1rem;
  color: #8a9c90;
  letter-spacing: 0.1em;
}

.page-indicator span {
  color: #2a362e;
  font-weight: 600;
}

/* 动画 */
.arrow {
  font-family: 'Manrope', sans-serif;
  transition: transform 0.3s ease;
}

.action-btn:hover:not(:disabled) .arrow,
.archive-row:hover .action-link .arrow,
.page-btn:hover:not(:disabled) .arrow:last-child {
  transform: translateX(4px);
}
.page-btn:hover:not(:disabled) .arrow:first-child {
  transform: translateX(-4px);
}

/* 响应式 */
@media (max-width: 900px) {
  .page-container {
    grid-template-columns: 1fr;
    gap: 4rem;
  }

  .sidebar-sticky {
    position: relative;
    top: 0;
  }

  .archive-row {
    grid-template-columns: 1fr;
    gap: 1rem;
    padding: 2rem 0;
  }

  .row-left {
    flex-direction: row;
    align-items: center;
    gap: 1rem;
  }

  .row-right {
    margin-top: 1rem;
  }

  .action-link {
    color: #2a362e;
  }
}
</style>