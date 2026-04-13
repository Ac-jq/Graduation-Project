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
  <main class="assessment-session-page">
    <header class="assessment-session-page__header">
      <div>
        <p class="assessment-session-page__eyebrow">Assessment Session</p>
        <h1>按真实状态完成本次作答</h1>
      </div>
      <div class="assessment-session-page__progress-card">
        <span>完成进度</span>
        <strong>{{ progressPercent }}%</strong>
        <p>{{ answeredCount }} / {{ pageData?.totalQuestions || '--' }} 题已作答</p>
      </div>
    </header>

    <section class="assessment-session-page__bar-panel">
      <div class="assessment-session-page__bar-track">
        <div class="assessment-session-page__bar-fill" :style="{ width: `${progressPercent}%` }"></div>
      </div>
      <p>{{ pageData?.totalQuestions || '--' }} 题中，当前为第 {{ paging.pageNum }} / {{ totalPages }} 页。</p>
    </section>

    <p v-if="errorMessage" class="assessment-session-page__alert">{{ errorMessage }}</p>

    <section v-if="loading" class="assessment-session-page__status-panel">
      <p>正在加载当前页题目...</p>
    </section>

    <section v-else class="assessment-session-page__question-list">
      <article v-for="question in questions" :key="question.questionId" class="question-card">
        <div class="question-card__head">
          <p class="question-card__number">Q{{ String(question.questionNo).padStart(2, '0') }}</p>
          <span v-if="assessmentStore.draftAnswers[question.questionId] != null" class="question-card__status">
            已选择
          </span>
        </div>
        <h2>{{ question.content }}</h2>
        <div class="question-card__options">
          <button
            v-for="option in question.options"
            :key="option.id"
            class="question-card__option"
            :class="{ 'question-card__option--active': optionSelected(question.questionId, option.id) }"
            type="button"
            @click="selectOption(question.questionId, option.id)"
          >
            <span>{{ option.optionCode }}</span>
            <strong>{{ option.content }}</strong>
            <small>{{ option.score }} 分</small>
          </button>
        </div>
      </article>
    </section>

    <footer class="assessment-session-page__footer">
      <div class="assessment-session-page__page-info">
        第 {{ paging.pageNum }} 页，共 {{ totalPages }} 页
      </div>

      <div class="assessment-session-page__actions">
        <button
          class="assessment-session-page__ghost"
          type="button"
          :disabled="paging.pageNum <= 1 || saving || submitting"
          @click="changePage(paging.pageNum - 1)"
        >
          上一页
        </button>
        <button
          class="assessment-session-page__ghost"
          type="button"
          :disabled="saving || submitting"
          @click="persistAnswers(true)"
        >
          {{ saving ? '正在暂存...' : '暂存进度' }}
        </button>
        <button
          v-if="paging.pageNum < totalPages"
          class="assessment-session-page__primary"
          type="button"
          :disabled="saving || submitting"
          @click="changePage(paging.pageNum + 1)"
        >
          下一页
        </button>
        <button
          v-else
          class="assessment-session-page__submit"
          type="button"
          :disabled="saving || submitting"
          @click="submitSession"
        >
          {{ submitting ? '正在提交并生成报告...' : '提交并生成报告' }}
        </button>
      </div>
    </footer>
  </main>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700&family=Noto+Serif+SC:wght@400;500;600;700&display=swap');

.assessment-session-page {
  --ink: #1f2620;
  --muted: #6c665d;
  --line: rgba(31, 38, 32, 0.1);
  --card: rgba(255, 252, 247, 0.82);
  min-height: 100%;
  color: var(--ink);
}

.assessment-session-page__header,
.assessment-session-page__footer {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: end;
}

.assessment-session-page__eyebrow,
.question-card__number {
  margin: 0;
  font: 700 0.74rem/1 'Manrope', sans-serif;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: #756c60;
}

.assessment-session-page h1,
.question-card h2 {
  margin: 0;
  font-family: 'Noto Serif SC', serif;
  font-weight: 600;
}

.assessment-session-page h1 {
  margin-top: 0.85rem;
  font-size: clamp(2.1rem, 4vw, 3.7rem);
  line-height: 1.08;
}

.assessment-session-page__progress-card,
.assessment-session-page__bar-panel,
.question-card,
.assessment-session-page__status-panel {
  border: 1px solid var(--line);
  background: var(--card);
  backdrop-filter: blur(16px);
  box-shadow: 0 22px 52px rgba(76, 62, 46, 0.08);
}

.assessment-session-page__progress-card {
  min-width: 220px;
  padding: 1.15rem 1.2rem;
}

.assessment-session-page__progress-card span,
.assessment-session-page__page-info,
.question-card__status {
  font: 700 0.78rem/1.4 'Manrope', sans-serif;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #687466;
}

.assessment-session-page__progress-card strong {
  display: block;
  margin-top: 0.55rem;
  font: 600 2rem/1 'Noto Serif SC', serif;
}

.assessment-session-page__progress-card p,
.assessment-session-page__bar-panel p,
.assessment-session-page__status-panel p {
  margin: 0.7rem 0 0;
  color: var(--muted);
  font: 400 0.92rem/1.7 'Manrope', sans-serif;
}

.assessment-session-page__bar-panel {
  margin-top: 1.2rem;
  padding: 1rem 1.1rem;
}

.assessment-session-page__bar-track {
  height: 10px;
  overflow: hidden;
  background: rgba(31, 38, 32, 0.08);
}

.assessment-session-page__bar-fill {
  height: 100%;
  background: linear-gradient(90deg, #718a79, #43584a);
  transition: width 220ms ease;
}

.assessment-session-page__alert {
  margin-top: 1rem;
  color: #a24d4d;
  font-weight: 600;
}

.assessment-session-page__status-panel {
  margin-top: 1.2rem;
  padding: 1.2rem;
}

.assessment-session-page__question-list {
  display: grid;
  gap: 1rem;
  margin-top: 1.2rem;
}

.question-card {
  padding: 1.35rem;
}

.question-card__head {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
}

.question-card h2 {
  margin-top: 0.8rem;
  font-size: 1.4rem;
  line-height: 1.55;
}

.question-card__options {
  display: grid;
  gap: 0.75rem;
  margin-top: 1rem;
}

.question-card__option {
  display: grid;
  grid-template-columns: 56px minmax(0, 1fr) auto;
  gap: 0.9rem;
  align-items: center;
  padding: 1rem;
  border: 1px solid rgba(31, 38, 32, 0.08);
  background: rgba(255, 255, 255, 0.48);
  cursor: pointer;
  text-align: left;
}

.question-card__option span,
.question-card__option small {
  font-family: 'Manrope', sans-serif;
}

.question-card__option span {
  font-weight: 700;
  color: #7b7368;
}

.question-card__option strong {
  font: 600 1rem/1.6 'Noto Serif SC', serif;
}

.question-card__option small {
  color: var(--muted);
}

.question-card__option--active {
  border-color: rgba(97, 121, 105, 0.45);
  background: linear-gradient(135deg, rgba(236, 244, 237, 0.95), rgba(246, 250, 246, 0.95));
}

.assessment-session-page__footer {
  margin-top: 1.25rem;
  padding-bottom: 0.5rem;
}

.assessment-session-page__actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.8rem;
  flex-wrap: wrap;
}

.assessment-session-page__ghost,
.assessment-session-page__primary,
.assessment-session-page__submit {
  min-height: 3rem;
  padding: 0 1.15rem;
  border: none;
  font: 700 0.8rem/1 'Manrope', sans-serif;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  cursor: pointer;
}

.assessment-session-page__ghost {
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.52);
  color: var(--ink);
}

.assessment-session-page__primary,
.assessment-session-page__submit {
  color: #fffaf4;
  background: linear-gradient(135deg, #627b69, #4d6454);
  box-shadow: 0 18px 34px rgba(77, 100, 84, 0.24);
}

.assessment-session-page__ghost:disabled,
.assessment-session-page__primary:disabled,
.assessment-session-page__submit:disabled {
  opacity: 0.56;
  cursor: not-allowed;
}

@media (max-width: 980px) {
  .assessment-session-page__header,
  .assessment-session-page__footer {
    flex-direction: column;
    align-items: stretch;
  }

  .question-card__option {
    grid-template-columns: 1fr;
  }

  .assessment-session-page__actions button {
    flex: 1;
  }
}
</style>
