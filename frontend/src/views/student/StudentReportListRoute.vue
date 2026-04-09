<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { fetchStudentReportsApi } from '@/api/assessment'
import type { ReportSummary } from '@/api/types'
import { toErrorMessage } from '@/views/shared/page-logic'

const router = useRouter()
const loading = ref(false)
const errorMessage = ref('')
const reports = ref<ReportSummary[]>([])

const latestReport = computed(() => reports.value[0] ?? null)
const highRiskCount = computed(() => reports.value.filter((report) => report.levelCode === 'HIGH').length)
const averageScore = computed(() => {
  if (reports.value.length === 0) {
    return 0
  }

  const totalScore = reports.value.reduce((sum, report) => sum + report.totalScore, 0)
  return Math.round((totalScore / reports.value.length) * 10) / 10
})

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
      return 'High Risk'
    default:
      return levelCode
  }
}

async function loadReports(): Promise<void> {
  loading.value = true
  errorMessage.value = ''

  try {
    reports.value = await fetchStudentReportsApi()
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    loading.value = false
  }
}

async function openReport(reportId: number): Promise<void> {
  await router.push({ name: 'student-report-detail', params: { reportId } })
}

async function jumpTo测评Hub(): Promise<void> {
  await router.push({ name: 'student-scales' })
}

onMounted(() => {
  void loadReports()
})
</script>

<template>
  <main class="report-list-page">
    <section class="report-list-page__masthead">
      <div class="report-list-page__heading">
        <p class="report-list-page__eyebrow">心理报告归档</p>
        <h1 class="report-list-page__title">历史报告总览</h1>
        <p class="report-list-page__summary">
          这里保留你每一次正式测评后的摘要、评分与Level，便于你持续回看自己的情绪波动轨迹。
        </p>
        <div class="report-list-page__heading-actions">
          <button class="report-list-page__primary" type="button" @click="jumpTo测评Hub">
            开始新测评
          </button>
        </div>
      </div>

      <aside class="report-list-page__snapshot">
        <p class="report-list-page__label">Archive Snapshot</p>
        <dl>
          <div>
            <dt>报告总数</dt>
            <dd>{{ reports.length }}</dd>
          </div>
          <div>
            <dt>High Risk</dt>
            <dd>{{ highRiskCount }}</dd>
          </div>
          <div>
            <dt>Avg Score</dt>
            <dd>{{ averageScore }}</dd>
          </div>
        </dl>
      </aside>
    </section>

    <p v-if="errorMessage" class="report-list-page__alert">{{ errorMessage }}</p>

    <section class="report-list-page__hero" v-if="latestReport && !loading">
      <div class="report-list-page__hero-copy">
        <p class="report-list-page__label">最新报告</p>
        <h2>{{ latestReport.scaleName }}</h2>
        <p>{{ latestReport.summaryText }}</p>
      </div>
      <div class="report-list-page__hero-meta">
        <div>
          <span>Score</span>
          <strong>{{ latestReport.totalScore }}</strong>
        </div>
        <div>
          <span>Level</span>
          <strong>{{ resolveLevelLabel(latestReport.levelCode) }}</strong>
        </div>
        <div>
          <span>Date</span>
          <strong>{{ formatDate(latestReport.createdAt) }}</strong>
        </div>
      </div>
      <button class="report-list-page__primary" type="button" @click="openReport(latestReport.reportId)">
        查看最新报告
      </button>
    </section>

    <section v-if="loading" class="report-list-page__status-panel">
      <p>正在加载历史报告...</p>
    </section>

    <section v-else-if="reports.length" class="report-list-page__timeline">
      <article
        v-for="(report, index) in reports"
        :key="report.reportId"
        class="report-card"
        @click="openReport(report.reportId)"
      >
        <div class="report-card__index">{{ String(index + 1).padStart(2, '0') }}</div>
        <div class="report-card__content">
          <div class="report-card__header">
            <p class="report-card__code">报告 #{{ report.reportId }}</p>
            <p class="report-card__date">{{ formatDate(report.createdAt) }}</p>
          </div>
          <h2>{{ report.scaleName }}</h2>
          <p>{{ report.summaryText }}</p>
        </div>
        <dl class="report-card__stats">
          <div>
            <dt>Score</dt>
            <dd>{{ report.totalScore }}</dd>
          </div>
          <div>
            <dt>Level</dt>
            <dd>{{ resolveLevelLabel(report.levelCode) }}</dd>
          </div>
        </dl>
      </article>
    </section>

    <section v-else class="report-list-page__status-panel">
      <p>你还没有正式报告，可以先前往心理测评模块完成一次测评。</p>
      <button class="report-list-page__primary" type="button" @click="jumpTo测评Hub">前往心理测评</button>
    </section>
  </main>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700&family=Noto+Serif+SC:wght@400;500;600;700&display=swap');

.report-list-page {
  --paper: #f5efe5;
  --ink: #201c18;
  --muted: #6e665f;
  --line: rgba(32, 28, 24, 0.12);
  --glass: rgba(255, 251, 245, 0.68);
  --accent: #627c6c;
  min-height: 100vh;
  padding: 2rem;
  color: var(--ink);
  background:
    radial-gradient(circle at right top, rgba(112, 132, 119, 0.18), transparent 26%),
    radial-gradient(circle at left center, rgba(198, 186, 168, 0.22), transparent 30%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.14), transparent 38%),
    var(--paper);
}

.report-list-page__masthead {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(290px, 0.85fr);
  gap: 1.5rem;
  align-items: end;
  padding-bottom: 1.4rem;
  border-bottom: 1px solid var(--line);
}

.report-list-page__eyebrow,
.report-list-page__label,
.report-card__code,
.report-card__stats dt,
.report-list-page__snapshot dt {
  margin: 0;
  font: 600 0.72rem/1.4 'Manrope', sans-serif;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--muted);
}

.report-list-page__title {
  margin: 0.95rem 0 0;
  font: 600 clamp(2.8rem, 5vw, 5.1rem)/0.98 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.report-list-page__summary {
  max-width: 44rem;
  margin: 1rem 0 0;
  color: var(--muted);
  font: 400 1rem/1.9 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.report-list-page__heading-actions {
  margin-top: 1.25rem;
}

.report-list-page__snapshot,
.report-list-page__hero,
.report-card,
.report-list-page__status-panel {
  border: 1px solid var(--line);
  background: var(--glass);
  backdrop-filter: blur(18px);
  box-shadow: 0 22px 48px rgba(80, 70, 58, 0.08);
}

.report-list-page__snapshot {
  padding: 1.2rem;
}

.report-list-page__snapshot dl {
  display: grid;
  gap: 0.95rem;
  margin: 1rem 0 0;
}

.report-list-page__snapshot dd {
  margin: 0.35rem 0 0;
  font: 600 1.1rem/1.4 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.report-list-page__alert {
  margin: 1.25rem 0 0;
  color: #8d4747;
  font: 600 0.9rem/1.6 'Manrope', sans-serif;
}

.report-list-page__hero {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(240px, 0.7fr) auto;
  gap: 1.2rem;
  align-items: end;
  margin-top: 1.5rem;
  padding: 1.4rem;
}

.report-list-page__hero-copy h2 {
  margin: 0.7rem 0 0;
  font: 600 2rem/1.2 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.report-list-page__hero-copy p:last-child {
  margin: 0.9rem 0 0;
  color: var(--muted);
  font: 400 0.98rem/1.9 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.report-list-page__hero-meta {
  display: grid;
  gap: 0.85rem;
}

.report-list-page__hero-meta span,
.report-card__date {
  color: var(--muted);
  font: 500 0.78rem/1.5 'Manrope', sans-serif;
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

.report-list-page__hero-meta strong {
  display: block;
  margin-top: 0.35rem;
  font: 600 1.08rem/1.45 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.report-list-page__primary {
  min-height: 3.2rem;
  padding: 0 1.2rem;
  border: none;
  background: linear-gradient(135deg, #6b8473, #4f6656);
  color: #faf6f0;
  font: 600 0.84rem/1 'Manrope', sans-serif;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  cursor: pointer;
  box-shadow: 0 18px 36px rgba(79, 102, 86, 0.24);
  transition: transform 180ms ease, box-shadow 180ms ease;
}

.report-list-page__primary:hover {
  transform: translateY(-2px);
}

.report-list-page__status-panel {
  display: grid;
  gap: 1rem;
  margin-top: 1.5rem;
  padding: 1.4rem;
}

.report-list-page__status-panel p {
  margin: 0;
  color: var(--muted);
  font: 400 1rem/1.9 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.report-list-page__timeline {
  display: grid;
  gap: 1.2rem;
  margin-top: 1.5rem;
}

.report-card {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr) minmax(180px, 240px);
  gap: 1rem;
  padding: 1.3rem;
  cursor: pointer;
  transition: transform 180ms ease, border-color 180ms ease, box-shadow 180ms ease;
}

.report-card:hover {
  transform: translateY(-3px);
  border-color: rgba(98, 124, 108, 0.36);
  box-shadow: 0 26px 46px rgba(80, 70, 58, 0.12);
}

.report-card__index {
  font: 600 2.2rem/1 'Noto Serif SC', 'Source Han Serif SC', serif;
  color: rgba(32, 28, 24, 0.42);
}

.report-card__header {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
}

.report-card__content h2 {
  margin: 0.6rem 0 0;
  font: 600 1.5rem/1.32 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.report-card__content p:last-child {
  margin: 0.9rem 0 0;
  color: var(--muted);
  font: 400 0.96rem/1.85 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.report-card__stats {
  display: grid;
  align-content: center;
  gap: 1rem;
  padding-left: 1rem;
  border-left: 1px solid var(--line);
}

.report-card__stats dd {
  margin: 0.35rem 0 0;
  font: 600 1.02rem/1.45 'Noto Serif SC', 'Source Han Serif SC', serif;
}

@media (max-width: 980px) {
  .report-list-page {
    padding: 1rem;
  }

  .report-list-page__masthead,
  .report-list-page__hero,
  .report-card {
    grid-template-columns: 1fr;
  }

  .report-list-page__heading-actions .report-list-page__primary {
    width: 100%;
  }

  .report-card__stats {
    padding-left: 0;
    border-left: none;
    border-top: 1px solid var(--line);
    padding-top: 1rem;
  }
}
</style>

