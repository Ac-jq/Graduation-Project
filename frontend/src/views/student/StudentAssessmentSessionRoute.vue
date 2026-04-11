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
const answeredRatio = computed(() => {
  if (!pageData.value || pageData.value.totalQuestions === 0) return 0
  return Math.round(( (pageData.value.answeredCount - (pageData.value?.records ?? []).filter(q => q.selectedOptionId != null).length + currentPageAnsweredCount.value) / pageData.value.totalQuestions) * 100)
})

const scrollArea = ref<HTMLElement | null>(null)

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
  if (!sessionId.value) return
  loading.value = true
  errorMessage.value = ''
  try {
    const response = await fetchScaleSessionQuestionsApi(sessionId.value, { ...paging })
    pageData.value = response
    paging.pageNum = response.pageNum
    syncAnswersFromPage()
    // 换页时让中间区域回滚到顶
    if (scrollArea.value) scrollArea.value.scrollTo({ top: 0, behavior: 'smooth' })
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
  answerDraft.value = { ...answerDraft.value, [questionId]: optionId }
}

async function persistAnswers(reload: boolean): Promise<void> {
  if (!sessionId.value) return
  saving.value = true
  try {
    await saveScaleAnswersApi(sessionId.value, buildAnswerPayload())
    if (reload) await loadQuestionPage()
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
    throw error
  } finally {
    saving.value = false
  }
}

async function changePage(next: number): Promise<void> {
  if (next < 1 || next > totalPages.value || next === paging.pageNum) return
  try {
    await persistAnswers(false)
    paging.pageNum = next
    await loadQuestionPage()
  } catch {}
}

async function submitSession(): Promise<void> {
  submitting.value = true
  try {
    await saveScaleAnswersApi(sessionId.value, buildAnswerPayload())
    const result = await submitScaleSessionApi(sessionId.value!)
    await router.push({ name: 'student-report-detail', params: { reportId: result.reportId } })
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    submitting.value = false
  }
}

onMounted(() => { void loadQuestionPage() })
</script>

<template>
  <div class="healing-viewport">
    <header class="progress-header">
      <div class="header-inner">
        <div class="info">
          <span class="session-tag">SESSION #{{ sessionId }}</span>
          <span class="ratio">{{ answeredRatio }}%</span>
        </div>
        <div class="bar-container">
          <div class="bar-fill" :style="{ width: `${answeredRatio}%` }"></div>
        </div>
      </div>
    </header>

    <main class="assessment-scroll-area" ref="scrollArea">
      <div class="content-limit">
        <div v-if="loading" class="status-box">正在布置作答空间...</div>

        <div v-else class="questions-list">
          <article
              v-for="(question, index) in pageData?.records"
              :key="question.questionId"
              class="question-card"
          >
            <div class="q-head">
              <span class="q-num">Q{{ String((paging.pageNum - 1) * paging.pageSize + index + 1).padStart(2, '0') }}</span>
              <span class="q-check" v-if="answerDraft[question.questionId]">已就绪</span>
            </div>
            <h2 class="q-text">{{ question.content }}</h2>
            <div class="options-group">
              <button
                  v-for="opt in question.options"
                  :key="opt.id"
                  class="opt-item"
                  :class="{ 'is-selected': answerDraft[question.questionId] === opt.id }"
                  @click="selectOption(question.questionId, opt.id)"
              >
                <span class="opt-label">{{ opt.optionCode }}</span>
                <span class="opt-val">{{ opt.content }}</span>
              </button>
            </div>
          </article>
        </div>
      </div>
    </main>

    <footer class="controls-footer">
      <div class="footer-inner">
        <div class="page-info">
          <strong>Page {{ paging.pageNum }}</strong> / {{ totalPages }}
        </div>
        <div class="btn-group">
          <button class="btn btn--ghost" :disabled="paging.pageNum <= 1" @click="changePage(paging.pageNum - 1)">上一页</button>
          <button class="btn btn--ghost" @click="persistAnswers(true)">{{ saving ? '正在存入...' : '暂存' }}</button>
          <button v-if="paging.pageNum < totalPages" class="btn btn--primary" @click="changePage(paging.pageNum + 1)">下一页</button>
          <button v-else class="btn btn--submit" :disabled="submitting" @click="submitSession">{{ submitting ? '分析中...' : '提交结果' }}</button>
        </div>
      </div>
    </footer>
  </div>
</template>

<style scoped>
/* =========================================
   核心：视口锁死布局
========================================= */
.healing-viewport {
  --c-sage: #8DA393;
  --c-sand: #F4F1EA;
  --c-ink: #2C352D;
  --c-white: #FFFFFF;

  height: 100dvh;
  display: flex;
  flex-direction: column;
  overflow: hidden; /* 禁止浏览器级别滚动 */
  background: var(--c-sand);
  color: var(--c-ink);
  font-family: 'Manrope', sans-serif;
}

/* =========================================
   1. 固定顶部 (Header)
========================================= */
.progress-header {
  flex: 0 0 auto;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(20px);
  padding: 1.2rem 2rem;
  border-bottom: 1px solid rgba(0,0,0,0.05);
}

.header-inner { max-width: 800px; margin: 0 auto; }
.info { display: flex; justify-content: space-between; margin-bottom: 0.6rem; font-size: 0.8rem; font-weight: 700; color: #7A857B; }
.bar-container { height: 6px; background: rgba(0,0,0,0.05); border-radius: 10px; overflow: hidden; }
.bar-fill { height: 100%; background: var(--c-sage); transition: width 0.6s ease; }

/* =========================================
   2. 中间滚动区 (Main)
========================================= */
.assessment-scroll-area {
  flex: 1 1 auto;
  overflow-y: auto; /* 仅此处允许滚动 */
  padding: 3rem 1.5rem;
  scrollbar-width: thin;
  scrollbar-color: var(--c-sage) transparent;
}

.content-limit { max-width: 720px; margin: 0 auto; }

/* 题卡部分换成白色 */
.question-card {
  background: var(--c-white);
  border-radius: 20px;
  padding: 2.5rem;
  margin-bottom: 2rem;
  box-shadow: 0 10px 30px rgba(0,0,0,0.03);
}

.q-head { display: flex; justify-content: space-between; margin-bottom: 1rem; }
.q-num { font-size: 1.8rem; font-family: serif; color: rgba(0,0,0,0.1); font-weight: 700; }
.q-check { font-size: 0.75rem; color: var(--c-sage); font-weight: 700; }
.q-text { font-size: 1.25rem; font-weight: 500; margin: 0 0 2rem 0; line-height: 1.6; }

.options-group { display: flex; flex-direction: column; gap: 0.8rem; }
.opt-item {
  display: flex; align-items: center; gap: 1rem; padding: 1.1rem 1.5rem;
  border-radius: 14px; border: 1px solid #F0F0F0; background: #FAFAFA;
  cursor: pointer; transition: all 0.2s ease; text-align: left;
}
.opt-item:hover { background: #F5F5F5; transform: translateX(4px); }
.opt-label { font-weight: 800; color: #AAA; width: 20px; }
.opt-val { font-size: 1rem; }

/* 选中状态 */
.opt-item.is-selected {
  background: #E8F0E9; border-color: var(--c-sage);
}
.opt-item.is-selected .opt-label { color: var(--c-sage); }

/* =========================================
   3. 固定底部 (Footer)
========================================= */
.controls-footer {
  flex: 0 0 auto;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(20px);
  padding: 1.2rem 2rem;
  box-shadow: 0 -10px 30px rgba(0,0,0,0.03);
}

.footer-inner { max-width: 800px; margin: 0 auto; display: flex; justify-content: space-between; align-items: center; }
.page-info { font-size: 0.9rem; color: #999; }
.page-info strong { color: var(--c-ink); }

.btn-group { display: flex; gap: 0.8rem; }
.btn { padding: 0.8rem 1.5rem; border-radius: 12px; font-size: 0.9rem; font-weight: 600; cursor: pointer; transition: all 0.2s; border: none; }
.btn:disabled { opacity: 0.4; cursor: not-allowed; }

.btn--ghost { background: #F0F0F0; color: var(--c-ink); }
.btn--ghost:hover:not(:disabled) { background: #E5E5E5; }

.btn--primary { background: var(--c-ink); color: white; }
.btn--primary:hover:not(:disabled) { background: #000; }

.btn--submit { background: var(--c-sage); color: white; }
.btn--submit:hover:not(:disabled) { transform: scale(1.05); }

@media (max-width: 600px) {
  .btn-group { width: 100%; }
  .btn { flex: 1; padding: 0.8rem 0.5rem; font-size: 0.8rem; }
  .page-info { display: none; }
}
</style>