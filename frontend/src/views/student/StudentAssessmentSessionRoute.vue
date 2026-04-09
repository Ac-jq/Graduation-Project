<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchScaleSessionQuestionsApi, saveScaleAnswersApi, submitScaleSessionApi } from '@/api/assessment'
import type { AnswerSaveRequest, ScaleQuestionPage, SubmitScaleResponse } from '@/api/types'
import { toErrorMessage, toNumberParam } from '@/views/shared/page-logic'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const submitting = ref(false)
const errorMessage = ref('')
const pageData = ref<ScaleQuestionPage | null>(null)
const submitResult = ref<SubmitScaleResponse | null>(null)
const paging = reactive({
  pageNum: 1,
  pageSize: 10
})
const answerDraft = ref<Record<number, number>>({})

const sessionId = computed(() => toNumberParam(route.params.sessionId))
const totalPages = computed(() => {
  if (!pageData.value) {
    return 1
  }

  return Math.max(1, Math.ceil(pageData.value.totalQuestions / pageData.value.pageSize))
})
const currentPageAnsweredCount = computed(() =>
  (pageData.value?.records ?? []).filter((question) => answerDraft.value[question.questionId] != null).length
)
const savedPageAnsweredCount = computed(() =>
  (pageData.value?.records ?? []).filter((question) => question.selectedOptionId != null).length
)
const estimatedAnsweredCount = computed(() => {
  if (!pageData.value) {
    return 0
  }

  return Math.max(
    0,
    Math.min(
      pageData.value.totalQuestions,
      pageData.value.answeredCount - savedPageAnsweredCount.value + currentPageAnsweredCount.value
    )
  )
})
const answeredRatio = computed(() => {
  if (!pageData.value || pageData.value.totalQuestions === 0) {
    return 0
  }

  return Math.round((estimatedAnsweredCount.value / pageData.value.totalQuestions) * 100)
})
const currentRangeText = computed(() => {
  if (!pageData.value || pageData.value.records.length === 0) {
    return '当前没有题目'
  }

  const start = (pageData.value.pageNum - 1) * pageData.value.pageSize + 1
  const end = start + pageData.value.records.length - 1
  return `${start}-${end} / ${pageData.value.totalQuestions}`
})
const pageNumbers = computed(() => Array.from({ length: totalPages.value }, (_, index) => index + 1))
const canSubmit = computed(() => pageData.value != null && estimatedAnsweredCount.value >= pageData.value.totalQuestions)

function syncAnswersFromPage(): void {
  const nextDraft: Record<number, number> = {}
  for (const question of pageData.value?.records ?? []) {
    if (question.selectedOptionId != null) {
      nextDraft[question.questionId] = question.selectedOptionId
    }
  }
  answerDraft.value = nextDraft
}

async function loadQuestionPage(): Promise<void> {
  if (!sessionId.value) {
    errorMessage.value = '无效的测评会话。'
    pageData.value = null
    return
  }

  loading.value = true
  errorMessage.value = ''

  try {
    const response = await fetchScaleSessionQuestionsApi(sessionId.value, { ...paging })
    pageData.value = response
    paging.pageNum = response.pageNum
    paging.pageSize = response.pageSize
    syncAnswersFromPage()
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    loading.value = false
  }
}

function buildAnswerPayload(): AnswerSaveRequest {
  return {
    answers: Object.entries(answerDraft.value).map(([questionId, optionId]) => ({
      questionId: Number(questionId),
      optionId
    }))
  }
}

function selectOption(questionId: number, optionId: number): void {
  answerDraft.value = {
    ...answerDraft.value,
    [questionId]: optionId
  }
}

async function persistAnswers(reloadAfterSave: boolean): Promise<void> {
  if (!sessionId.value) {
    errorMessage.value = '无效的测评会话。'
    return
  }

  saving.value = true
  errorMessage.value = ''

  try {
    await saveScaleAnswersApi(sessionId.value, buildAnswerPayload())
    if (reloadAfterSave) {
      await loadQuestionPage()
    }
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
    throw error
  } finally {
    saving.value = false
  }
}

async function saveAnswers(): Promise<void> {
  await persistAnswers(true)
}

async function changePage(nextPageNum: number): Promise<void> {
  if (!pageData.value || !sessionId.value) {
    return
  }

  if (nextPageNum < 1 || nextPageNum > totalPages.value || nextPageNum === paging.pageNum) {
    return
  }

  try {
    await persistAnswers(false)
    paging.pageNum = nextPageNum
    await loadQuestionPage()
  } catch {
    // persistAnswers already maps the backend error into errorMessage.
  }
}

async function submitSession(): Promise<void> {
  if (!sessionId.value) {
    errorMessage.value = '无效的测评会话。'
    return
  }

  submitting.value = true
  errorMessage.value = ''

  try {
    await saveScaleAnswersApi(sessionId.value, buildAnswerPayload())
    const result = await submitScaleSessionApi(sessionId.value)
    submitResult.value = result
    await router.push({ name: 'student-report-detail', params: { reportId: result.reportId } })
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    submitting.value = false
  }
}

watch(
  () => route.params.sessionId,
  () => {
    paging.pageNum = 1
    submitResult.value = null
    void loadQuestionPage()
  }
)

onMounted(() => {
  void loadQuestionPage()
})
</script>

<template>
  <main class="assessment-session-page">
    <section class="assessment-session-page__masthead">
      <div class="assessment-session-page__heading">
        <p class="assessment-session-page__eyebrow">测评作答</p>
        <h1 class="assessment-session-page__title">心理测评作答</h1>
        <p class="assessment-session-page__summary">
          当前页面按测评会话逐页保存。你可以先完成本页，再切换到下一页，系统会持续记录你的作答进度。
        </p>
      </div>

      <aside class="assessment-session-page__session-card">
        <p class="assessment-session-page__label">Session</p>
        <strong>#{{ sessionId ?? '—' }}</strong>
        <dl class="assessment-session-page__stats">
          <div>
            <dt>Progress</dt>
            <dd>{{ answeredRatio }}%</dd>
          </div>
          <div>
            <dt>Answered</dt>
            <dd>{{ estimatedAnsweredCount }}/{{ pageData?.totalQuestions ?? 0 }}</dd>
          </div>
          <div>
            <dt>Current Page</dt>
            <dd>{{ pageData?.pageNum ?? paging.pageNum }}/{{ totalPages }}</dd>
          </div>
        </dl>
      </aside>
    </section>

    <section class="assessment-session-page__toolbar">
      <div class="assessment-session-page__progress-block">
        <div class="assessment-session-page__progress-track">
          <span class="assessment-session-page__progress-fill" :style="{ width: `${answeredRatio}%` }" />
        </div>
        <div class="assessment-session-page__progress-meta">
          <p>{{ currentRangeText }}</p>
          <p>本页已完成 {{ currentPageAnsweredCount }} / {{ pageData?.records.length ?? 0 }} 题</p>
        </div>
      </div>

      <div class="assessment-session-page__actions">
        <button
          class="assessment-session-page__ghost"
          type="button"
          :disabled="loading || saving || paging.pageNum <= 1"
          @click="changePage(paging.pageNum - 1)"
        >
          上一页
        </button>
        <button class="assessment-session-page__ghost" type="button" :disabled="loading || saving" @click="saveAnswers">
          {{ saving ? '保存中...' : '保存当前页' }}
        </button>
        <button
          class="assessment-session-page__ghost"
          type="button"
          :disabled="loading || saving || paging.pageNum >= totalPages"
          @click="changePage(paging.pageNum + 1)"
        >
          下一页
        </button>
        <button
          class="assessment-session-page__primary"
          type="button"
          :disabled="loading || saving || submitting || !canSubmit"
          @click="submitSession"
        >
          {{ submitting ? '提交中...' : '提交并查看报告' }}
        </button>
      </div>
    </section>

    <section class="assessment-session-page__pager" v-if="pageNumbers.length > 1">
      <button
        v-for="pageNum in pageNumbers"
        :key="pageNum"
        type="button"
        class="assessment-session-page__pager-button"
        :class="{ 'assessment-session-page__pager-button--active': pageNum === paging.pageNum }"
        :disabled="loading || saving || pageNum === paging.pageNum"
        @click="changePage(pageNum)"
      >
        {{ pageNum }}
      </button>
    </section>

    <p v-if="errorMessage" class="assessment-session-page__alert">
      {{ errorMessage }}
    </p>

    <section v-if="loading" class="assessment-session-page__status-panel">
      <p>正在加载当前页题目...</p>
    </section>

    <section v-else-if="pageData?.records.length" class="assessment-session-page__question-grid">
      <article
        v-for="question in pageData.records"
        :key="question.questionId"
        class="assessment-question"
      >
        <header class="assessment-question__header">
          <span class="assessment-question__index">Q{{ String(question.questionNo).padStart(2, '0') }}</span>
          <p class="assessment-question__state">
            {{ answerDraft[question.questionId] != null ? '已选择答案' : '待作答' }}
          </p>
        </header>

        <h2 class="assessment-question__title">{{ question.content }}</h2>

        <div class="assessment-question__options">
          <button
            v-for="option in question.options"
            :key="option.id"
            type="button"
            class="assessment-option"
            :class="{ 'assessment-option--selected': answerDraft[question.questionId] === option.id }"
            @click="selectOption(question.questionId, option.id)"
          >
            <span class="assessment-option__meta">
              <strong>{{ option.optionCode }}</strong>
              <small>Score {{ option.score }}</small>
            </span>
            <span class="assessment-option__content">{{ option.content }}</span>
          </button>
        </div>
      </article>
    </section>

    <section v-else class="assessment-session-page__status-panel">
      <p>当前页没有可作答题目。</p>
    </section>

    <section v-if="submitResult" class="assessment-session-page__submit-result">
      <p class="assessment-session-page__label">最新结果</p>
      <h2>报告已生成</h2>
      <dl>
        <div>
          <dt>报告状态</dt>
          <dd>#{{ submitResult.reportId }}</dd>
        </div>
        <div>
          <dt>Score</dt>
          <dd>{{ submitResult.totalScore }}</dd>
        </div>
        <div>
          <dt>Level</dt>
          <dd>{{ submitResult.levelCode }}</dd>
        </div>
      </dl>
      <p>{{ submitResult.summaryText }}</p>
    </section>
  </main>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700&family=Noto+Serif+SC:wght@400;500;600;700&display=swap');

.assessment-session-page {
  --paper: #f4eee3;
  --ink: #201c17;
  --muted: #706860;
  --line: rgba(32, 28, 23, 0.12);
  --glass: rgba(255, 251, 245, 0.68);
  --accent: #5d7763;
  --accent-soft: rgba(93, 119, 99, 0.14);
  --danger: #8d4747;
  min-height: 100vh;
  padding: 2rem;
  background:
    radial-gradient(circle at top right, rgba(118, 139, 123, 0.18), transparent 28%),
    radial-gradient(circle at left center, rgba(196, 184, 165, 0.22), transparent 32%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.14), transparent 40%),
    var(--paper);
  color: var(--ink);
}

.assessment-session-page__masthead {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(290px, 0.85fr);
  gap: 1.5rem;
  align-items: end;
  padding-bottom: 1.45rem;
  border-bottom: 1px solid var(--line);
}

.assessment-session-page__eyebrow,
.assessment-session-page__label,
.assessment-session-page__stats dt,
.assessment-question__index,
.assessment-question__state,
.assessment-option__meta small,
.assessment-session-page__pager-button {
  margin: 0;
  font: 600 0.72rem/1.4 'Manrope', sans-serif;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--muted);
}

.assessment-session-page__title {
  margin: 0.95rem 0 0;
  font: 600 clamp(2.8rem, 5vw, 5.2rem)/0.98 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.assessment-session-page__summary {
  max-width: 46rem;
  margin: 1rem 0 0;
  color: var(--muted);
  font: 400 1rem/1.92 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.assessment-session-page__session-card,
.assessment-session-page__toolbar,
.assessment-question,
.assessment-session-page__status-panel,
.assessment-session-page__submit-result,
.assessment-session-page__pager {
  border: 1px solid var(--line);
  background: var(--glass);
  backdrop-filter: blur(18px);
  box-shadow: 0 22px 48px rgba(80, 70, 58, 0.08);
}

.assessment-session-page__session-card {
  padding: 1.25rem;
}

.assessment-session-page__session-card strong {
  display: block;
  margin-top: 0.4rem;
  font: 600 2rem/1.05 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.assessment-session-page__stats {
  display: grid;
  gap: 0.95rem;
  margin: 1.2rem 0 0;
}

.assessment-session-page__stats dd {
  margin: 0.38rem 0 0;
  font: 600 1.04rem/1.4 'Manrope', sans-serif;
}

.assessment-session-page__toolbar {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 1rem;
  align-items: center;
  margin-top: 1.5rem;
  padding: 1rem 1.15rem;
}

.assessment-session-page__progress-block {
  display: grid;
  gap: 0.65rem;
}

.assessment-session-page__progress-track {
  position: relative;
  overflow: hidden;
  width: 100%;
  height: 10px;
  background: rgba(32, 28, 23, 0.08);
}

.assessment-session-page__progress-fill {
  display: block;
  height: 100%;
  background: linear-gradient(90deg, #7f9785, #4e6555);
}

.assessment-session-page__progress-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  justify-content: space-between;
}

.assessment-session-page__progress-meta p {
  margin: 0;
  color: var(--muted);
  font: 500 0.92rem/1.5 'Manrope', sans-serif;
}

.assessment-session-page__actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 0.8rem;
}

.assessment-session-page__ghost,
.assessment-session-page__primary,
.assessment-option,
.assessment-session-page__pager-button {
  transition: transform 180ms ease, border-color 180ms ease, box-shadow 180ms ease, background 180ms ease;
}

.assessment-session-page__ghost,
.assessment-session-page__primary {
  min-height: 3rem;
  padding: 0 1.15rem;
  border: 1px solid transparent;
  font: 600 0.84rem/1 'Manrope', sans-serif;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  cursor: pointer;
}

.assessment-session-page__ghost {
  border-color: var(--line);
  background: rgba(255, 255, 255, 0.48);
  color: var(--ink);
}

.assessment-session-page__primary {
  background: linear-gradient(135deg, #67816d, #4f6556);
  color: #fbf7f1;
  box-shadow: 0 18px 36px rgba(79, 101, 86, 0.24);
}

.assessment-session-page__ghost:hover:not(:disabled),
.assessment-session-page__primary:hover:not(:disabled),
.assessment-option:hover,
.assessment-session-page__pager-button:hover:not(:disabled) {
  transform: translateY(-2px);
}

.assessment-session-page__ghost:disabled,
.assessment-session-page__primary:disabled,
.assessment-session-page__pager-button:disabled {
  opacity: 0.55;
  cursor: not-allowed;
  transform: none;
}

.assessment-session-page__pager {
  display: flex;
  flex-wrap: wrap;
  gap: 0.65rem;
  margin-top: 1rem;
  padding: 0.9rem 1rem;
}

.assessment-session-page__pager-button {
  min-width: 2.8rem;
  min-height: 2.8rem;
  padding: 0 0.8rem;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.5);
  cursor: pointer;
}

.assessment-session-page__pager-button--active {
  border-color: rgba(93, 119, 99, 0.4);
  background: var(--accent-soft);
  color: var(--ink);
}

.assessment-session-page__alert {
  margin: 1.25rem 0 0;
  color: var(--danger);
  font: 600 0.9rem/1.6 'Manrope', sans-serif;
}

.assessment-session-page__question-grid {
  display: grid;
  gap: 1.3rem;
  margin-top: 1.5rem;
}

.assessment-question {
  display: grid;
  gap: 1.1rem;
  padding: 1.4rem;
}

.assessment-question__header {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 0.75rem;
  align-items: center;
  padding-bottom: 0.9rem;
  border-bottom: 1px solid var(--line);
}

.assessment-question__title {
  margin: 0;
  font: 600 1.45rem/1.52 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.assessment-question__options {
  display: grid;
  gap: 0.85rem;
}

.assessment-option {
  display: grid;
  gap: 0.8rem;
  width: 100%;
  padding: 1rem;
  border: 1px solid rgba(32, 28, 23, 0.08);
  text-align: left;
  background: rgba(255, 255, 255, 0.42);
  cursor: pointer;
}

.assessment-option__meta {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: baseline;
}

.assessment-option__meta strong {
  font: 700 0.88rem/1 'Manrope', sans-serif;
  letter-spacing: 0.1em;
  color: var(--ink);
}

.assessment-option__content {
  color: var(--muted);
  font: 400 0.96rem/1.85 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.assessment-option--selected {
  border-color: rgba(93, 119, 99, 0.45);
  background: linear-gradient(180deg, rgba(93, 119, 99, 0.16), rgba(255, 255, 255, 0.54));
  box-shadow: 0 18px 28px rgba(93, 119, 99, 0.12);
}

.assessment-session-page__status-panel,
.assessment-session-page__submit-result {
  margin-top: 1.5rem;
  padding: 1.4rem;
}

.assessment-session-page__status-panel p,
.assessment-session-page__submit-result p {
  margin: 0;
  color: var(--muted);
  font: 400 0.98rem/1.9 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.assessment-session-page__submit-result h2 {
  margin: 0.75rem 0 0;
  font: 600 1.82rem/1.26 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.assessment-session-page__submit-result dl {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 1rem;
  margin: 1rem 0;
  padding-top: 1rem;
  border-top: 1px solid var(--line);
}

.assessment-session-page__submit-result dt {
  margin: 0;
  font: 600 0.72rem/1.4 'Manrope', sans-serif;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--muted);
}

.assessment-session-page__submit-result dd {
  margin: 0.35rem 0 0;
  font: 600 1rem/1.4 'Noto Serif SC', 'Source Han Serif SC', serif;
}

@media (max-width: 980px) {
  .assessment-session-page {
    padding: 1rem;
  }

  .assessment-session-page__masthead,
  .assessment-session-page__toolbar {
    grid-template-columns: 1fr;
  }

  .assessment-session-page__actions {
    justify-content: stretch;
  }

  .assessment-session-page__ghost,
  .assessment-session-page__primary {
    width: 100%;
  }

  .assessment-session-page__submit-result dl {
    grid-template-columns: 1fr;
  }
}
</style>

