<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { fetchStudentReportsApi } from '@/api/assessment'
import type { ReportSummary } from '@/api/types'
import { toErrorMessage } from '@/views/shared/page-logic'

type LevelTone = 'low' | 'medium' | 'high'

const router = useRouter()

const loading = ref(false)
const errorMessage = ref('')
const reports = ref<ReportSummary[]>([])

const latestReport = computed(() => reports.value[0] ?? null)
const highRiskCount = computed(() => reports.value.filter((item) => item.levelCode === 'HIGH').length)
const averageScore = computed(() => {
  if (!reports.value.length) {
    return 0
  }

  return Math.round((reports.value.reduce((sum, item) => sum + item.totalScore, 0) / reports.value.length) * 10) / 10
})

const latestSummary = computed(() => {
  if (!latestReport.value) {
    return '完成测评后，系统会在这里保留正式历史报告。'
  }

  return latestReport.value.summaryText
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
      return '高风险'
    default:
      return levelCode
  }
}

function resolveLevelTone(levelCode: string): LevelTone {
  switch (levelCode) {
    case 'HIGH':
      return 'high'
    case 'MEDIUM':
      return 'medium'
    default:
      return 'low'
  }
}

function resolveReportInitials(scaleName: string): string {
  const sanitized = scaleName.replace(/[^A-Za-z0-9\u4e00-\u9fa5]/g, '')
  return sanitized.slice(0, 2).toUpperCase() || '报告'
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

onMounted(() => {
  void loadReports()
})
</script>

<template>
  <main class="student-report-list-page">
    <section class="student-report-list-page__hero">
      <div class="student-report-list-page__copy-card">
        <div class="student-report-list-page__eyebrow-row">
          <p class="student-report-list-page__eyebrow">Report Archive</p>
          <span class="student-report-list-page__hero-badge">
            <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
              <path d="M6 4.75h9l3 3v11.5A1.75 1.75 0 0 1 16.25 21h-10.5A1.75 1.75 0 0 1 4 19.25V6.5A1.75 1.75 0 0 1 5.75 4.75Z" />
              <path d="M9 12h6" />
              <path d="M9 15.5h4.5" />
            </svg>
            结构化成长留档
          </span>
        </div>
        <h1>历史报告总览</h1>
        <p class="student-report-list-page__lead">
          每一次正式测评都会在这里留下可回看的记录，方便你重新理解阶段性的情绪波动、
          分值变化和系统生成的风险判断。
        </p>

        <div class="student-report-list-page__metrics">
          <article class="metric-card">
            <span>报告数量</span>
            <strong>{{ reports.length }}</strong>
            <p>已完成并归档的正式报告</p>
          </article>
          <article class="metric-card">
            <span>高风险次数</span>
            <strong>{{ highRiskCount }}</strong>
            <p>系统记录的高关注区间</p>
          </article>
          <article class="metric-card metric-card--warm">
            <span>平均分</span>
            <strong>{{ averageScore }}</strong>
            <p>用于感受趋势，不代表医学诊断</p>
          </article>
        </div>
      </div>

      <aside class="student-report-list-page__snapshot">
        <div class="student-report-list-page__snapshot-top">
          <div class="student-report-list-page__snapshot-avatar">
            {{ latestReport ? resolveReportInitials(latestReport.scaleName) : '记录' }}
          </div>
          <div>
            <p class="student-report-list-page__meta-label">最近一次归档</p>
            <strong>{{ latestReport?.scaleName || '等待生成第一份报告' }}</strong>
          </div>
        </div>
        <p class="student-report-list-page__snapshot-summary">{{ latestSummary }}</p>
        <div v-if="latestReport" class="student-report-list-page__snapshot-tags">
          <span class="info-chip">{{ formatDate(latestReport.createdAt) }}</span>
          <span class="info-chip">总分 {{ latestReport.totalScore }}</span>
          <span class="level-pill" :class="`level-pill--${resolveLevelTone(latestReport.levelCode)}`">
            {{ resolveLevelLabel(latestReport.levelCode) }}
          </span>
        </div>
      </aside>
    </section>

    <p v-if="errorMessage" class="student-report-list-page__alert">{{ errorMessage }}</p>

    <section v-if="latestReport && !loading" class="student-report-list-page__featured-card">
      <div class="student-report-list-page__featured-copy">
        <div class="student-report-list-page__featured-kicker">
          <span class="student-report-list-page__meta-label">重点查看</span>
          <span class="level-pill" :class="`level-pill--${resolveLevelTone(latestReport.levelCode)}`">
            {{ resolveLevelLabel(latestReport.levelCode) }}
          </span>
        </div>
        <h2>{{ latestReport.scaleName }}</h2>
        <p>{{ latestReport.summaryText }}</p>
      </div>

      <dl class="student-report-list-page__featured-meta">
        <div>
          <dt>总分</dt>
          <dd>{{ latestReport.totalScore }}</dd>
        </div>
        <div>
          <dt>生成时间</dt>
          <dd>{{ formatDate(latestReport.createdAt) }}</dd>
        </div>
      </dl>

      <button class="student-report-list-page__primary" type="button" @click="openReport(latestReport.reportId)">
        查看报告详情
        <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
          <path d="M5 12h14" />
          <path d="m13 6 6 6-6 6" />
        </svg>
      </button>
    </section>

    <section v-if="loading" class="student-report-list-page__status-panel">
      <div class="student-report-list-page__status-icon">
        <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
          <path d="M12 6.5v5.5l3.25 2" />
          <path d="M20 12a8 8 0 1 1-8-8" />
        </svg>
      </div>
      <div>
        <h2>正在加载历史报告</h2>
        <p>系统正在同步你已生成的测评档案。</p>
      </div>
    </section>

    <section v-else-if="reports.length" class="student-report-list-page__timeline">
      <article
        v-for="(report, index) in reports"
        :key="report.reportId"
        class="report-card"
        @click="openReport(report.reportId)"
      >
        <div class="report-card__index">{{ String(index + 1).padStart(2, '0') }}</div>
        <div class="report-card__avatar">{{ resolveReportInitials(report.scaleName) }}</div>

        <div class="report-card__content">
          <div class="report-card__header">
            <div>
              <p class="report-card__code">报告 #{{ report.reportId }}</p>
              <h2>{{ report.scaleName }}</h2>
            </div>
            <span class="level-pill" :class="`level-pill--${resolveLevelTone(report.levelCode)}`">
              {{ resolveLevelLabel(report.levelCode) }}
            </span>
          </div>

          <div class="report-card__chips">
            <span class="info-chip">{{ formatDate(report.createdAt) }}</span>
            <span class="info-chip">总分 {{ report.totalScore }}</span>
          </div>

          <p>{{ report.summaryText }}</p>
        </div>

        <div class="report-card__cta">
          <span>查看详情</span>
          <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
            <path d="M5 12h14" />
            <path d="m13 6 6 6-6 6" />
          </svg>
        </div>
      </article>
    </section>

    <section v-else class="student-report-list-page__status-panel">
      <div class="student-report-list-page__status-icon">
        <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
          <path d="M12 4.75v14.5" />
          <path d="M4.75 12h14.5" />
        </svg>
      </div>
      <div>
        <h2>还没有正式测评报告</h2>
        <p>先完成一次量表作答，系统才会生成可供回看的历史结果。</p>
      </div>
      <button class="student-report-list-page__primary" type="button" @click="router.push({ name: 'student-scales' })">
        前往量表测评
        <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
          <path d="M5 12h14" />
          <path d="m13 6 6 6-6 6" />
        </svg>
      </button>
    </section>
  </main>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700;800&family=Noto+Serif+SC:wght@500;600;700&display=swap');

.student-report-list-page {
  --ink: #201c18;
  --muted: #6e665f;
  --line: rgba(32, 28, 24, 0.08);
  min-height: 100%;
  padding: 0.4rem 0 2.4rem;
  color: var(--ink);
}

.student-report-list-page__hero {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(300px, 0.7fr);
  gap: 1.5rem;
  align-items: stretch;
}

.student-report-list-page__copy-card,
.student-report-list-page__snapshot,
.student-report-list-page__featured-card,
.report-card,
.student-report-list-page__status-panel,
.metric-card {
  border: 1px solid rgba(33, 28, 24, 0.05);
  border-radius: 24px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.94) 0%, rgba(248, 244, 238, 0.86) 100%);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.04);
  backdrop-filter: blur(18px);
}

.student-report-list-page__copy-card {
  padding: 1.8rem;
}

.student-report-list-page__eyebrow-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0.9rem;
  align-items: center;
}

.student-report-list-page__eyebrow,
.student-report-list-page__meta-label,
.report-card__code,
.metric-card span {
  margin: 0;
  font: 800 0.72rem/1 'Manrope', sans-serif;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: #7a7167;
}

.student-report-list-page__hero-badge,
.info-chip,
.level-pill {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  padding: 0.52rem 0.82rem;
  border-radius: 999px;
  font: 800 0.72rem/1 'Manrope', sans-serif;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.student-report-list-page__hero-badge {
  background: rgba(93, 120, 101, 0.12);
  color: #5e7465;
}

.student-report-list-page__hero-badge svg,
.student-report-list-page__status-icon svg,
.student-report-list-page__primary svg,
.report-card__cta svg {
  width: 18px;
  height: 18px;
  stroke: currentColor;
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.student-report-list-page h1,
.student-report-list-page h2,
.report-card h2,
.student-report-list-page__status-panel h2 {
  margin: 0;
  font-family: 'Noto Serif SC', serif;
  font-weight: 600;
}

.student-report-list-page h1 {
  margin-top: 0.95rem;
  font-size: clamp(2.5rem, 4.6vw, 4.4rem);
  line-height: 1.05;
}

.student-report-list-page__lead,
.student-report-list-page__snapshot-summary,
.metric-card p,
.student-report-list-page__featured-copy p,
.student-report-list-page__status-panel p,
.report-card__content p {
  font-family: 'Manrope', sans-serif;
  line-height: 1.82;
}

.student-report-list-page__lead {
  max-width: 44rem;
  margin: 1rem 0 0;
  color: var(--muted);
}

.student-report-list-page__metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 1rem;
  margin-top: 1.55rem;
}

.metric-card {
  padding: 1.2rem 1.25rem;
}

.metric-card--warm {
  background:
    linear-gradient(180deg, rgba(255, 249, 241, 0.95) 0%, rgba(248, 243, 235, 0.88) 100%);
}

.metric-card strong,
.student-report-list-page__snapshot strong {
  display: block;
  margin-top: 0.7rem;
  font: 600 1.75rem/1.08 'Noto Serif SC', serif;
  color: #2b2621;
}

.metric-card p {
  margin: 0.7rem 0 0;
  font-size: 0.88rem;
  color: var(--muted);
}

.student-report-list-page__snapshot {
  padding: 1.55rem;
  background:
    radial-gradient(circle at top right, rgba(97, 122, 105, 0.16), transparent 35%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.96) 0%, rgba(241, 246, 240, 0.86) 100%);
}

.student-report-list-page__snapshot-top {
  display: flex;
  gap: 1rem;
  align-items: center;
}

.student-report-list-page__snapshot-avatar,
.report-card__avatar,
.student-report-list-page__status-icon {
  display: grid;
  place-items: center;
  flex-shrink: 0;
}

.student-report-list-page__snapshot-avatar {
  width: 3.6rem;
  height: 3.6rem;
  border-radius: 20px;
  background: linear-gradient(135deg, #6d8573, #c6d1c0);
  color: #fffdf8;
  font: 700 0.95rem/1 'Manrope', sans-serif;
}

.student-report-list-page__snapshot-summary {
  margin: 1rem 0 0;
  color: #656056;
}

.student-report-list-page__snapshot-tags,
.report-card__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 0.7rem;
  margin-top: 1rem;
}

.info-chip {
  background: rgba(255, 255, 255, 0.78);
  color: #736a5f;
}

.level-pill--low {
  background: rgba(104, 148, 117, 0.14);
  color: #4f7556;
}

.level-pill--medium {
  background: rgba(215, 175, 107, 0.16);
  color: #9b6e27;
}

.level-pill--high {
  background: rgba(178, 88, 76, 0.14);
  color: #a04c42;
}

.student-report-list-page__alert {
  margin-top: 1rem;
  padding: 1rem 1.1rem;
  border-radius: 18px;
  background: rgba(168, 76, 67, 0.08);
  color: #9a473f;
  font-weight: 700;
}

.student-report-list-page__featured-card {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(220px, 0.7fr) auto;
  gap: 1.2rem;
  align-items: center;
  margin-top: 1.55rem;
  padding: 1.45rem;
}

.student-report-list-page__featured-copy {
  min-width: 0;
}

.student-report-list-page__featured-kicker {
  display: flex;
  flex-wrap: wrap;
  gap: 0.8rem;
  align-items: center;
}

.student-report-list-page__featured-card h2 {
  margin-top: 0.7rem;
  font-size: 2rem;
  line-height: 1.18;
}

.student-report-list-page__featured-copy p {
  margin: 0.8rem 0 0;
  color: var(--muted);
}

.student-report-list-page__featured-meta {
  display: grid;
  gap: 0.9rem;
}

.student-report-list-page__featured-meta dt {
  margin: 0;
  font: 800 0.72rem/1 'Manrope', sans-serif;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: #7d7468;
}

.student-report-list-page__featured-meta dd {
  margin: 0.4rem 0 0;
  font: 600 1.02rem/1.45 'Noto Serif SC', serif;
}

.student-report-list-page__primary {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  min-height: 3rem;
  padding: 0 1.15rem;
  border: none;
  border-radius: 999px;
  background: linear-gradient(135deg, #6b8473, #4f6656);
  color: #faf6f0;
  font: 800 0.76rem/1 'Manrope', sans-serif;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  cursor: pointer;
  transition: all 0.3s ease;
}

.student-report-list-page__primary:hover,
.report-card:hover {
  transform: translateY(-2px);
}

.student-report-list-page__primary:hover {
  box-shadow: 0 16px 28px rgba(79, 102, 86, 0.2);
}

.student-report-list-page__status-panel {
  display: grid;
  grid-template-columns: 54px minmax(0, 1fr) auto;
  gap: 1rem;
  align-items: center;
  margin-top: 1.5rem;
  padding: 1.4rem;
}

.student-report-list-page__status-icon {
  width: 54px;
  height: 54px;
  border-radius: 18px;
  background: rgba(97, 122, 105, 0.12);
  color: #5e7564;
}

.student-report-list-page__status-panel h2 {
  font-size: 1.24rem;
}

.student-report-list-page__status-panel p {
  margin: 0.35rem 0 0;
  color: var(--muted);
}

.student-report-list-page__timeline {
  display: grid;
  gap: 1.2rem;
  margin-top: 1.55rem;
}

.report-card {
  display: grid;
  grid-template-columns: 48px 64px minmax(0, 1fr) auto;
  gap: 1rem;
  align-items: center;
  padding: 1.35rem;
  cursor: pointer;
  transition: all 0.3s ease;
}

.report-card:hover {
  box-shadow: 0 18px 34px rgba(80, 70, 58, 0.1);
}

.report-card__index {
  font: 600 1.6rem/1 'Noto Serif SC', serif;
  color: rgba(32, 28, 24, 0.34);
}

.report-card__avatar {
  width: 64px;
  height: 64px;
  border-radius: 20px;
  background: linear-gradient(135deg, rgba(107, 132, 115, 0.95), rgba(212, 220, 204, 0.88));
  color: #fffdf8;
  font: 700 0.96rem/1 'Manrope', sans-serif;
}

.report-card__content {
  min-width: 0;
}

.report-card__header {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: start;
}

.report-card h2 {
  margin-top: 0.45rem;
  font-size: 1.5rem;
  line-height: 1.25;
}

.report-card__content p {
  margin: 0.95rem 0 0;
  color: var(--muted);
}

.report-card__cta {
  display: inline-flex;
  gap: 0.45rem;
  align-items: center;
  color: #596f60;
  font: 800 0.76rem/1 'Manrope', sans-serif;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

@media (max-width: 1080px) {
  .student-report-list-page__hero,
  .student-report-list-page__featured-card,
  .report-card {
    grid-template-columns: 1fr;
  }

  .student-report-list-page__metrics {
    grid-template-columns: 1fr;
  }

  .student-report-list-page__status-panel {
    grid-template-columns: 54px minmax(0, 1fr);
  }
}

@media (max-width: 760px) {
  .student-report-list-page__copy-card,
  .student-report-list-page__snapshot,
  .student-report-list-page__featured-card,
  .report-card,
  .student-report-list-page__status-panel {
    padding: 1.25rem;
  }

  .report-card__header {
    flex-direction: column;
    align-items: start;
  }
}
</style>
