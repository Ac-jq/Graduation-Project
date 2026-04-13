<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchCounselorStudentReportsApi } from '@/api/assessment'
import type { ReportSummary } from '@/api/types'
import { toErrorMessage, toNumberParam } from '@/views/shared/page-logic'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const errorMessage = ref('')
const reports = ref<ReportSummary[]>([])

const studentUserId = computed(() => toNumberParam(route.params.studentUserId))

function formatDate(value: string): string {
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value))
}

function resolveLevelLabel(levelCode: string): string {
  switch (levelCode) {
    case 'LOW':
      return '低风险'
    case 'MEDIUM':
      return '中风险'
    case 'HIGH':
      return '高风险'
    default:
      return levelCode
  }
}

async function loadReports(): Promise<void> {
  if (!studentUserId.value) {
    errorMessage.value = '学生编号无效'
    reports.value = []
    return
  }

  loading.value = true
  errorMessage.value = ''

  try {
    reports.value = await fetchCounselorStudentReportsApi(studentUserId.value)
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    loading.value = false
  }
}

async function openReport(reportId: number): Promise<void> {
  if (!studentUserId.value) {
    return
  }

  await router.push({
    name: 'counselor-student-report-detail',
    params: { studentUserId: studentUserId.value, reportId }
  })
}

watch(
  () => route.params.studentUserId,
  () => {
    void loadReports()
  }
)

onMounted(() => {
  void loadReports()
})
</script>

<template>
  <section class="counselor-report-list-page">
    <header class="counselor-report-list-page__hero">
      <div class="counselor-report-list-page__copy">
        <p class="counselor-report-list-page__eyebrow">Counselor View</p>
        <h1>学生测评报告列表</h1>
        <p class="counselor-report-list-page__lead">
          当前正在查看学生 #{{ studentUserId || '--' }} 的历史测评报告。列表按时间倒序排列，便于快速回看风险变化与量表类型。
        </p>
      </div>
      <aside class="counselor-report-list-page__metric-card">
        <span>报告数量</span>
        <strong>{{ reports.length }}</strong>
      </aside>
    </header>

    <p v-if="errorMessage" class="counselor-report-list-page__alert">{{ errorMessage }}</p>

    <section v-if="loading" class="counselor-report-list-page__status-panel">
      <p>正在同步学生报告列表...</p>
    </section>

    <section v-else-if="reports.length" class="counselor-report-list-page__stack">
      <article v-for="report in reports" :key="report.reportId" class="report-card" @click="openReport(report.reportId)">
        <div class="report-card__topline">
          <div>
            <p class="report-card__code">报告 #{{ report.reportId }}</p>
            <h2>{{ report.scaleName }}</h2>
          </div>
          <span class="report-card__badge">{{ resolveLevelLabel(report.levelCode) }}</span>
        </div>
        <p class="report-card__summary">{{ report.summaryText }}</p>
        <div class="report-card__meta">
          <span>总分 {{ report.totalScore }}</span>
          <span>{{ formatDate(report.createdAt) }}</span>
        </div>
      </article>
    </section>

    <section v-else class="counselor-report-list-page__status-panel">
      <p>当前学生还没有测评报告。</p>
    </section>
  </section>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700&family=Noto+Serif+SC:wght@400;500;600;700&display=swap');

.counselor-report-list-page {
  min-height: 100%;
  color: #283128;
}

.counselor-report-list-page__hero {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) 220px;
  gap: 1.5rem;
  align-items: end;
}

.counselor-report-list-page__copy {
  border-top: 1px solid rgba(59, 69, 59, 0.16);
  padding-top: 18px;
}

.counselor-report-list-page__eyebrow,
.report-card__code {
  margin: 0 0 10px;
  font: 700 0.76rem/1 'Manrope', sans-serif;
  letter-spacing: 0.22em;
  text-transform: uppercase;
  color: #7b6857;
}

.counselor-report-list-page h1,
.report-card h2 {
  margin: 0;
  font-family: 'Noto Serif SC', serif;
  font-weight: 600;
}

.counselor-report-list-page h1 {
  font-size: clamp(2rem, 3vw, 3.2rem);
  line-height: 1.16;
}

.counselor-report-list-page__lead,
.report-card__summary,
.report-card__meta,
.counselor-report-list-page__status-panel p {
  font-family: 'Manrope', sans-serif;
}

.counselor-report-list-page__lead {
  margin: 18px 0 0;
  line-height: 1.84;
  color: rgba(40, 49, 40, 0.72);
}

.counselor-report-list-page__metric-card,
.report-card,
.counselor-report-list-page__status-panel {
  border: 1px solid rgba(77, 86, 77, 0.14);
  background: rgba(255, 252, 247, 0.76);
  box-shadow: 0 24px 70px rgba(91, 80, 66, 0.08);
  backdrop-filter: blur(16px);
}

.counselor-report-list-page__metric-card {
  padding: 18px 20px;
}

.counselor-report-list-page__metric-card span {
  display: block;
  margin-bottom: 8px;
  font: 700 0.78rem/1 'Manrope', sans-serif;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: rgba(68, 74, 66, 0.56);
}

.counselor-report-list-page__metric-card strong {
  font: 600 1.6rem/1 'Noto Serif SC', serif;
}

.counselor-report-list-page__alert {
  margin-top: 1rem;
  color: #a44f46;
  font-weight: 600;
}

.counselor-report-list-page__status-panel {
  margin-top: 1.5rem;
  padding: 1.3rem;
}

.counselor-report-list-page__stack {
  display: grid;
  gap: 1rem;
  margin-top: 1.5rem;
}

.report-card {
  padding: 1.25rem;
  cursor: pointer;
}

.report-card__topline {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: start;
}

.report-card h2 {
  font-size: 1.32rem;
  line-height: 1.35;
}

.report-card__badge {
  border: 1px solid rgba(97, 111, 98, 0.15);
  background: rgba(242, 244, 237, 0.94);
  padding: 8px 12px;
  font: 700 0.74rem/1 'Manrope', sans-serif;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #66735f;
}

.report-card__summary {
  margin: 14px 0 0;
  font-size: 0.96rem;
  line-height: 1.86;
  color: rgba(40, 49, 40, 0.7);
}

.report-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 18px;
  margin-top: 14px;
  font-size: 0.84rem;
  color: rgba(40, 49, 40, 0.58);
}

@media (max-width: 900px) {
  .counselor-report-list-page__hero {
    grid-template-columns: 1fr;
  }

  .report-card__topline {
    flex-direction: column;
    align-items: start;
  }
}
</style>
