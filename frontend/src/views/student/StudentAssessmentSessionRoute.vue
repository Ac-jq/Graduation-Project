<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  fetchScaleSessionQuestionsApi,
  saveScaleAnswersApi,
  submitScaleSessionApi
} from '@/api/assessment'
import type { AnswerSaveRequest, Question, ScaleQuestionPage, SubmitScaleResponse } from '@/api/types'
import { useAssessmentStore } from '@/stores/assessment'
import { toErrorMessage, toNumberParam } from '@/views/shared/page-logic'

const route = useRoute()
const router = useRouter()
const assessmentStore = useAssessmentStore()

const loading = ref(false)
const saving = ref(false)
const submitting = ref(false)
const errorMessage = ref('')
const pageData = ref<ScaleQuestionPage | null>(null)

const paging = reactive({
  pageNum: 1,
  pageSize: 3
})

const sessionId = computed(() => toNumberParam(route.params.sessionId))
const questions = computed<Question[]>(() => pageData.value?.records ?? [])
const totalPages = computed(() => {
  if (!pageData.value) {
    return 1
  }
  return Math.max(1, Math.ceil(pageData.value.totalQuestions / pageData.value.pageSize))
})
const answeredCount = computed(() => {
  if (!pageData.value) {
    return 0
  }

  const originalAnswered = questions.value.filter((item) => item.selectedOptionId != null).length
  const currentAnswered = questions.value.filter((item) => assessmentStore.draftAnswers[item.questionId] != null).length
  return pageData.value.answeredCount - originalAnswered + currentAnswered
})
const progressPercent = computed(() => {
  if (!pageData.value || pageData.value.totalQuestions === 0) {
    return 0
  }
  return Math.min(100, Math.round((answeredCount.value / pageData.value.totalQuestions) * 100))
})

function buildAnswerPayload(): AnswerSaveRequest {
  return {
    answers: Object.entries(assessmentStore.draftAnswers).map(([questionId, optionId]) => ({
      questionId: Number(questionId),
      optionId
    }))
  }
}

function optionSelected(questionId: number, optionId: number): boolean {
  return assessmentStore.draftAnswers[questionId] === optionId
}

function selectOption(questionId: number, optionId: number): void {
  assessmentStore.setDraftAnswer(questionId, optionId)
}

async function loadQuestionPage(): Promise<void> {
  if (!sessionId.value) {
    errorMessage.value = '测评会话编号无效'
    return
  }

  loading.value = true
  errorMessage.value = ''

  try {
    const response = await fetchScaleSessionQuestionsApi(sessionId.value, { ...paging })
    pageData.value = response
    paging.pageNum = response.pageNum
    paging.pageSize = response.pageSize
    assessmentStore.setCurrentQuestionPage(response)
    assessmentStore.syncDraftAnswersFromPage()
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    loading.value = false
  }
}

async function persistAnswers(showReload = false): Promise<void> {
  if (!sessionId.value) {
    return
  }

  saving.value = true

  try {
    await saveScaleAnswersApi(sessionId.value, buildAnswerPayload())
    if (showReload) {
      await loadQuestionPage()
    }
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
    throw error
  } finally {
    saving.value = false
  }
}

async function changePage(nextPage: number): Promise<void> {
  if (nextPage < 1 || nextPage > totalPages.value || nextPage === paging.pageNum) {
    return
  }

  try {
    await persistAnswers(false)
    paging.pageNum = nextPage
    await loadQuestionPage()
  } catch {
    return
  }
}

async function submitSession(): Promise<void> {
  if (!sessionId.value) {
    return
  }

  submitting.value = true
  errorMessage.value = ''

  try {
    await saveScaleAnswersApi(sessionId.value, buildAnswerPayload())
    const result: SubmitScaleResponse = await submitScaleSessionApi(sessionId.value)
    assessmentStore.setLatestSubmit(result)
    await router.push({
      name: 'student-assessment-result',
      params: { reportId: result.reportId }
    })
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    submitting.value = false
  }
}

watch(
    () => route.params.sessionId,
    () => {
      assessmentStore.resetSessionState()
      void loadQuestionPage()
    }
)

onMounted(() => {
  void loadQuestionPage()
})
</script>

<template>
  <main class="premium-session-page">
    <div class="premium-session-card">

      <header class="session-header">
        <div class="header-top">
          <h1 class="session-title">倾听内心的声音</h1>
          <div class="progress-text">
            <span>当前进度</span>
            <strong>{{ progressPercent }}%</strong>
          </div>
        </div>
        <div class="progress-track">
          <div class="progress-fill" :style="{ width: `${progressPercent}%` }"></div>
        </div>
      </header>

      <div v-if="errorMessage" class="state-container">
        <h2 class="error-text">{{ errorMessage }}</h2>
      </div>

      <div v-else-if="loading" class="state-container">
        <div class="loading-orb"></div>
        <p class="meta-text">正在翻开新的一页...</p>
      </div>

      <section v-else class="question-list">
        <article v-for="question in questions" :key="question.questionId" class="question-item">
          <h2 class="question-content">
            <span class="question-number">第 {{ String(question.questionNo).padStart(2, '0') }} 题</span>
            {{ question.content }}
          </h2>

          <div class="options-grid">
            <button
                v-for="option in question.options"
                :key="option.id"
                class="option-btn"
                :class="{ 'option-btn--active': optionSelected(question.questionId, option.id) }"
                type="button"
                @click="selectOption(question.questionId, option.id)"
            >
              <span class="option-label">{{ option.content }}</span>
            </button>
          </div>
        </article>
      </section>

      <footer class="session-footer">
        <div class="footer-meta">
          第 {{ paging.pageNum }} / {{ totalPages }} 页
        </div>

        <div class="footer-actions">
          <button
              class="ghost-action-btn"
              type="button"
              :disabled="paging.pageNum <= 1 || saving || submitting"
              @click="changePage(paging.pageNum - 1)"
          >
            上一页
          </button>
          <button
              class="ghost-action-btn"
              type="button"
              :disabled="saving || submitting"
              @click="persistAnswers(true)"
          >
            {{ saving ? '暂存中...' : '暂存进度' }}
          </button>
          <button
              v-if="paging.pageNum < totalPages"
              class="primary-action-btn"
              type="button"
              :disabled="saving || submitting"
              @click="changePage(paging.pageNum + 1)"
          >
            下一页
          </button>
          <button
              v-else
              class="primary-action-btn submit-btn"
              type="button"
              :disabled="saving || submitting"
              @click="submitSession"
          >
            {{ submitting ? '生成报告中...' : '提交并生成报告' }}
          </button>
        </div>
      </footer>

    </div>
  </main>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600&family=Noto+Serif+SC:wght@500;600;700&display=swap');

/* 全局沉浸式背景 */
.premium-session-page {
  min-height: 100vh;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  background: #f4f6f4;
  padding: 5vw 2rem;
  box-sizing: border-box;
}

/* 核心画板容器 */
.premium-session-card {
  width: 100%;
  max-width: 820px;
  background: linear-gradient(
      145deg,
      rgba(255, 255, 255, 0.75) 0%,
      rgba(248, 246, 242, 0.85) 100%
  );
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border: 1px solid rgba(255, 255, 255, 0.9);
  border-radius: 36px;
  padding: 3.5rem 4rem;
  box-sizing: border-box;
  box-shadow:
      0 30px 60px rgba(54, 66, 58, 0.05),
      inset 0 2px 0 rgba(255, 255, 255, 0.8);
  display: flex;
  flex-direction: column;
  gap: 3rem;
}

/* 头部进度条区 */
.session-header {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.header-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
}

.session-title {
  font-family: 'Noto Serif SC', serif;
  font-size: clamp(1.8rem, 3vw, 2.2rem);
  font-weight: 600;
  color: #1e2821;
  margin: 0;
  letter-spacing: 0.02em;
}

.progress-text {
  text-align: right;
}

.progress-text span {
  display: block;
  font-family: 'Noto Serif SC', serif;
  font-size: 0.85rem;
  color: #7b8c80;
  margin-bottom: 0.2rem;
}

.progress-text strong {
  font-family: 'Manrope', sans-serif;
  font-size: 1.4rem;
  font-weight: 600;
  color: #2a362e;
}

.progress-track {
  height: 4px;
  background: rgba(42, 54, 46, 0.06);
  border-radius: 4px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: #2a362e;
  border-radius: 4px;
  transition: width 0.6s cubic-bezier(0.16, 1, 0.3, 1);
}

/* 题目列表区 */
.question-list {
  display: flex;
  flex-direction: column;
  gap: 3.5rem;
}

.question-item {
  display: flex;
  flex-direction: column;
  gap: 1.8rem;
}

.question-content {
  font-family: 'Noto Serif SC', serif;
  font-size: 1.45rem;
  font-weight: 600;
  color: #2a362e;
  line-height: 1.6;
  margin: 0;
}

.question-number {
  display: block;
  font-size: 0.95rem;
  color: #7b8c80;
  margin-bottom: 0.8rem;
  font-weight: 500;
  letter-spacing: 0.05em;
}

/* 高级选项交互 */
.options-grid {
  display: grid;
  gap: 0.8rem;
}

.option-btn {
  width: 100%;
  text-align: left;
  padding: 1.4rem 1.8rem;
  background: rgba(255, 255, 255, 0.4);
  border: 1px solid rgba(130, 150, 138, 0.15);
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  font-family: 'Noto Serif SC', serif;
  font-size: 1.1rem;
  color: #4a5c51;
}

.option-btn:hover {
  background: rgba(255, 255, 255, 0.8);
  border-color: rgba(130, 150, 138, 0.4);
  transform: translateX(4px);
}

.option-btn--active {
  background: #2a362e;
  border-color: #2a362e;
  color: #ffffff;
  box-shadow: 0 12px 24px rgba(42, 54, 46, 0.15);
  transform: translateX(8px);
}

.option-btn--active:hover {
  background: #2a362e;
  transform: translateX(8px);
}

/* 底部操作区 */
.session-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 1rem;
  padding-top: 2rem;
  border-top: 1px solid rgba(42, 54, 46, 0.08);
}

.footer-meta {
  font-family: 'Noto Serif SC', serif;
  color: #7b8c80;
  font-size: 0.95rem;
}

.footer-actions {
  display: flex;
  gap: 1rem;
}

.ghost-action-btn,
.primary-action-btn {
  padding: 0 1.8rem;
  height: 3.2rem;
  border-radius: 100px;
  font-family: 'Noto Serif SC', serif;
  font-size: 1rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.ghost-action-btn {
  background: transparent;
  border: 1px solid rgba(130, 150, 138, 0.3);
  color: #5c6b60;
}

.ghost-action-btn:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.6);
  border-color: #5c6b60;
  color: #2a362e;
}

.primary-action-btn {
  background: #2a362e;
  border: none;
  color: #ffffff;
  box-shadow: 0 8px 16px rgba(42, 54, 46, 0.15);
}

.primary-action-btn:hover:not(:disabled) {
  background: #1c2620;
  transform: translateY(-2px);
  box-shadow: 0 12px 24px rgba(42, 54, 46, 0.25);
}

.ghost-action-btn:disabled,
.primary-action-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
  transform: none;
}

/* 加载与错误状态 */
.state-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 4rem 0;
}

.loading-orb {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: 2px solid rgba(130, 150, 138, 0.2);
  border-top-color: #2a362e;
  animation: spin 0.8s linear infinite;
  margin-bottom: 1rem;
}

.meta-text {
  color: #7b8c80;
  font-family: 'Noto Serif SC', serif;
}

.error-text {
  color: #8c4a4a;
  font-family: 'Noto Serif SC', serif;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 响应式调整 */
@media (max-width: 768px) {
  .premium-session-card {
    padding: 2.5rem 1.5rem;
    border-radius: 28px;
    gap: 2rem;
  }

  .session-footer {
    flex-direction: column;
    gap: 1.5rem;
  }

  .footer-actions {
    width: 100%;
    flex-direction: column;
  }

  .ghost-action-btn,
  .primary-action-btn {
    width: 100%;
  }

  .option-btn:hover {
    transform: translateX(2px);
  }

  .option-btn--active {
    transform: translateX(4px);
  }

  .option-btn--active:hover {
    transform: translateX(4px);
  }
}
</style>