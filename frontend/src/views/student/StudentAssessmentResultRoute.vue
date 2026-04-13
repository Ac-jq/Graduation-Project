<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchStudentReportDetailApi } from '@/api/assessment'
import type { ReportDetail } from '@/api/types'
import { useAssessmentStore } from '@/stores/assessment'
import { toErrorMessage, toNumberParam } from '@/views/shared/page-logic'

const route = useRoute()
const router = useRouter()
const assessmentStore = useAssessmentStore()

const loading = ref(false)
const errorMessage = ref('')
const reportDetail = ref<ReportDetail | null>(null)

const reportId = computed(() => toNumberParam(route.params.reportId))

function resolveLevelLabel(levelCode?: string | null): string {
  switch (levelCode) {
    case 'LOW':
      return '低风险'
    case 'MEDIUM':
      return '中风险'
    case 'HIGH':
      return '高风险'
    default:
      return '待评估'
  }
}

async function loadReport(): Promise<void> {
  if (!reportId.value) {
    errorMessage.value = '报告编号无效'
    return
  }

  loading.value = true
  errorMessage.value = ''

  try {
    const detail = await fetchStudentReportDetailApi(reportId.value)
    reportDetail.value = detail
    assessmentStore.setCurrentReport(detail)
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void loadReport()
})
</script>

<template>
  <main class="assessment-result-page">
    <section class="assessment-result-page__hero">
      <div>
        <p class="assessment-result-page__eyebrow">Result Snapshot</p>
        <h1>本次测评已完成</h1>
        <p class="assessment-result-page__lead">
          系统已经基于标准量表完成评分，并生成了辅助评估报告。你可以先看总分和等级，再进入完整报告页查看详细解释。
        </p>
      </div>
      <aside class="assessment-result-page__summary-card" v-if="assessmentStore.latestSubmit || reportDetail">
        <p class="assessment-result-page__meta-label">本次结果</p>
        <dl>
          <div>
            <dt>总分</dt>
            <dd>{{ assessmentStore.latestSubmit?.totalScore ?? reportDetail?.totalScore ?? '--' }}</dd>
          </div>
          <div>
            <dt>等级</dt>
            <dd>{{ resolveLevelLabel(assessmentStore.latestSubmit?.levelCode || reportDetail?.levelCode) }}</dd>
          </div>
          <div>
            <dt>报告编号</dt>
            <dd>#{{ assessmentStore.latestSubmit?.reportId ?? reportDetail?.reportId ?? '--' }}</dd>
          </div>
        </dl>
      </aside>
    </section>

    <p v-if="errorMessage" class="assessment-result-page__alert">{{ errorMessage }}</p>

    <section v-if="loading" class="assessment-result-page__status-panel">
      <p>正在加载报告摘要...</p>
    </section>

    <template v-else-if="reportDetail">
      <section class="assessment-result-page__content-grid">
        <article class="assessment-result-page__panel">
          <p class="assessment-result-page__meta-label">结果摘要</p>
          <h2>{{ reportDetail.scaleName }}</h2>
          <p class="assessment-result-page__body-text">{{ reportDetail.summaryText }}</p>
        </article>

        <article class="assessment-result-page__panel">
          <p class="assessment-result-page__meta-label">AI 辅助解释</p>
          <h2>先看方向，再看细节</h2>
          <p class="assessment-result-page__body-text">
            {{ reportDetail.aiInterpretation || '系统已生成结构化报告，你可以继续查看完整报告页中的分数解释与建议。' }}
          </p>
        </article>
      </section>

      <section class="assessment-result-page__notice-panel">
        <p class="assessment-result-page__meta-label">重要声明</p>
        <h2>结果仅用于辅助评估</h2>
        <p>
          {{
            reportDetail.noticeText ||
            '本结果仅用于心理状态辅助评估，不作为医学诊断依据。如有持续困扰，请联系专业老师或医疗机构。'
          }}
        </p>
      </section>

      <section class="assessment-result-page__actions">
        <button class="assessment-result-page__ghost" type="button" @click="router.push({ name: 'student-scales' })">
          再做一次测评
        </button>
        <button
          class="assessment-result-page__primary"
          type="button"
          @click="router.push({ name: 'student-report-detail', params: { reportId: reportDetail.reportId } })"
        >
          查看完整报告
        </button>
      </section>
    </template>
  </main>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700&family=Noto+Serif+SC:wght@400;500;600;700&display=swap');

.assessment-result-page {
  --ink: #212720;
  --muted: #6b655d;
  --line: rgba(33, 39, 32, 0.1);
  --glass: rgba(255, 251, 246, 0.78);
  min-height: 100%;
  color: var(--ink);
}

.assessment-result-page__hero,
.assessment-result-page__content-grid {
  display: grid;
  gap: 1.4rem;
}

.assessment-result-page__hero {
  grid-template-columns: minmax(0, 1.35fr) minmax(280px, 0.8fr);
  align-items: end;
}

.assessment-result-page__eyebrow,
.assessment-result-page__meta-label,
.assessment-result-page__summary-card dt {
  margin: 0;
  font: 700 0.74rem/1 'Manrope', sans-serif;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: #72695e;
}

.assessment-result-page h1,
.assessment-result-page h2 {
  margin: 0;
  font-family: 'Noto Serif SC', serif;
  font-weight: 600;
}

.assessment-result-page h1 {
  margin-top: 0.9rem;
  font-size: clamp(2.4rem, 5vw, 4.6rem);
  line-height: 1.04;
}

.assessment-result-page__lead,
.assessment-result-page__body-text,
.assessment-result-page__summary-card dd,
.assessment-result-page__notice-panel p,
.assessment-result-page__status-panel p {
  font-family: 'Manrope', sans-serif;
  line-height: 1.85;
}

.assessment-result-page__lead {
  max-width: 42rem;
  margin: 1rem 0 0;
  color: var(--muted);
}

.assessment-result-page__summary-card,
.assessment-result-page__panel,
.assessment-result-page__notice-panel,
.assessment-result-page__status-panel {
  border: 1px solid var(--line);
  background: var(--glass);
  backdrop-filter: blur(18px);
  box-shadow: 0 24px 56px rgba(74, 61, 48, 0.08);
}

.assessment-result-page__summary-card,
.assessment-result-page__panel,
.assessment-result-page__notice-panel {
  padding: 1.35rem;
}

.assessment-result-page__summary-card dl {
  display: grid;
  gap: 0.95rem;
  margin: 1rem 0 0;
}

.assessment-result-page__summary-card dd {
  margin: 0.35rem 0 0;
}

.assessment-result-page__content-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-top: 1.5rem;
}

.assessment-result-page__panel h2,
.assessment-result-page__notice-panel h2 {
  margin-top: 0.8rem;
  font-size: 1.7rem;
  line-height: 1.32;
}

.assessment-result-page__body-text {
  margin: 0.9rem 0 0;
  color: var(--muted);
}

.assessment-result-page__notice-panel {
  display: grid;
  gap: 0.8rem;
  margin-top: 1.5rem;
}

.assessment-result-page__notice-panel p:last-child {
  margin: 0;
  color: #7b5648;
}

.assessment-result-page__alert {
  margin-top: 1rem;
  color: #9f4d4d;
  font-weight: 600;
}

.assessment-result-page__status-panel {
  margin-top: 1.5rem;
  padding: 1.2rem;
}

.assessment-result-page__actions {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  margin-top: 1.5rem;
}

.assessment-result-page__ghost,
.assessment-result-page__primary {
  min-height: 3.1rem;
  padding: 0 1.2rem;
  border: none;
  font: 700 0.8rem/1 'Manrope', sans-serif;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  cursor: pointer;
}

.assessment-result-page__ghost {
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.56);
  color: var(--ink);
}

.assessment-result-page__primary {
  background: linear-gradient(135deg, #607968, #4d6454);
  color: #fffaf4;
}

@media (max-width: 980px) {
  .assessment-result-page__hero,
  .assessment-result-page__content-grid {
    grid-template-columns: 1fr;
  }

  .assessment-result-page__actions {
    flex-direction: column;
  }
}
</style>
