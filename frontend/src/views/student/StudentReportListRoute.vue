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

// 分页相关状态
const currentPage = ref(1)
const pageSize = 5 // 每页显示 5 条历史记录

const latestReport = computed(() => reports.value[0] ?? null)
const highRiskCount = computed(() => reports.value.filter((item) => item.levelCode === 'HIGH').length)
const averageScore = computed(() => {
  if (!reports.value.length) return 0
  return Math.round((reports.value.reduce((sum, item) => sum + item.totalScore, 0) / reports.value.length) * 10) / 10
})

// 剥离出第一条（最新）后，剩下的作为历史记录
const pastReports = computed(() => reports.value.slice(1))

// 前端分页计算
const totalPages = computed(() => Math.max(1, Math.ceil(pastReports.value.length / pageSize)))
const pagedReports = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return pastReports.value.slice(start, start + pageSize)
})

function prevPage() {
  if (currentPage.value > 1) {
    currentPage.value--
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
}

function nextPage() {
  if (currentPage.value < totalPages.value) {
    currentPage.value++
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
}

function formatDate(value: string): string {
  const date = new Date(value)
  return `${date.getFullYear()}/${String(date.getMonth() + 1).padStart(2, '0')}/${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

function resolveLevelLabel(levelCode: string): string {
  switch (levelCode) {
    case 'LOW': return '状态平稳'
    case 'MEDIUM': return '需适度关注'
    case 'HIGH': return '建议重点关注'
    default: return levelCode
  }
}

function resolveLevelTone(levelCode: string): LevelTone {
  switch (levelCode) {
    case 'HIGH': return 'high'
    case 'MEDIUM': return 'medium'
    default: return 'low'
  }
}

async function loadReports(): Promise<void> {
  loading.value = true
  errorMessage.value = ''

  try {
    reports.value = await fetchStudentReportsApi()
    currentPage.value = 1 // 重新加载时回到第一页
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
  <main class="archive-list-page">
    <div class="archive-container">

      <header class="archive-header">
        <div class="header-context">
          <span class="header-tag">Report Archive</span>
          <h1 class="header-title">评估卷宗</h1>
          <p class="header-desc">
            你所有的情绪切片与梳理记录都已妥善归档。这些分数仅代表过去的某个切面，请将它们作为了解自己的线索，而非绝对的结论。
          </p>
        </div>

        <div class="header-stats">
          <div class="stat-block">
            <span class="stat-label">归档总数</span>
            <span class="stat-value">{{ loading ? '-' : reports.length }}</span>
          </div>
          <div class="stat-block">
            <span class="stat-label">高关注提醒</span>
            <span class="stat-value">{{ loading ? '-' : highRiskCount }}</span>
          </div>
          <div class="stat-block">
            <span class="stat-label">平均分波动</span>
            <span class="stat-value">{{ loading ? '-' : averageScore }}</span>
          </div>
        </div>
      </header>

      <div v-if="errorMessage" class="error-text">{{ errorMessage }}</div>

      <div v-if="loading" class="loading-state">
        <div class="spinner"></div>
        <p>正在展卷...</p>
      </div>

      <div v-else-if="!reports.length" class="empty-state">
        <h2 class="empty-title">卷宗尚为空白</h2>
        <p class="empty-desc">完成你的第一次量表梳理后，系统会在此生成详细的解读报告。</p>
        <button class="ghost-btn" @click="router.push({ name: 'student-scales' })">
          前往量表室 <span class="arrow">→</span>
        </button>
      </div>

      <template v-else>
        <section class="latest-entry" v-if="latestReport && currentPage === 1" @click="openReport(latestReport.reportId)">
          <div class="latest-accent-line"></div>
          <div class="latest-content">
            <div class="latest-meta">
              <span class="meta-label">最新归档</span>
              <span class="meta-date">{{ formatDate(latestReport.createdAt) }}</span>
            </div>

            <h2 class="latest-title">{{ latestReport.scaleName }}</h2>
            <p class="latest-summary">{{ latestReport.summaryText }}</p>

            <div class="latest-bottom">
              <div class="latest-indicators">
                <span class="indicator-score">总分 <strong>{{ latestReport.totalScore }}</strong></span>
                <span class="indicator-pill" :class="`indicator-pill--${resolveLevelTone(latestReport.levelCode)}`">
                  {{ resolveLevelLabel(latestReport.levelCode) }}
                </span>
              </div>
              <button class="action-read-btn">
                查阅详阅 <span class="arrow">→</span>
              </button>
            </div>
          </div>
        </section>

        <section class="history-list" v-if="pastReports.length > 0">
          <h3 class="list-heading" v-if="currentPage === 1">往期留档</h3>
          <h3 class="list-heading" v-else>第 {{ currentPage }} 页的留档记录</h3>

          <div class="list-container">
            <article
                v-for="report in pagedReports"
                :key="report.reportId"
                class="history-row"
                @click="openReport(report.reportId)"
            >
              <div class="row-date">
                {{ formatDate(report.createdAt) }}
              </div>

              <div class="row-main">
                <h4 class="row-title">{{ report.scaleName }}</h4>
                <p class="row-summary">{{ report.summaryText }}</p>
              </div>

              <div class="row-stats">
                <div class="stat-score">
                  <span>总分</span>
                  <strong>{{ report.totalScore }}</strong>
                </div>
                <div class="stat-advice">
                  <span class="minimal-pill" :class="`minimal-pill--${resolveLevelTone(report.levelCode)}`">
                    {{ resolveLevelLabel(report.levelCode) }}
                  </span>
                </div>
              </div>

              <div class="row-action">
                <span class="arrow">→</span>
              </div>
            </article>
          </div>

          <nav class="pagination-nav" v-if="totalPages > 1">
            <button
                class="page-btn"
                :disabled="currentPage <= 1"
                @click="prevPage"
            >
              <span class="arrow">←</span> 上一卷
            </button>

            <div class="page-indicator">
              <span>{{ currentPage }}</span> / <span>{{ totalPages }}</span>
            </div>

            <button
                class="page-btn"
                :disabled="currentPage >= totalPages"
                @click="nextPage"
            >
              下一卷 <span class="arrow">→</span>
            </button>
          </nav>
        </section>
      </template>

    </div>
  </main>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700&family=Noto+Serif+SC:wght@500;600;700&display=swap');

/* 全局基调与宽度控制，彻底解决左右滑动 */
.archive-list-page {
  min-height: 100vh;
  background: #fcfbf9;
  color: #1e2821;
  font-family: 'Manrope', 'Noto Serif SC', sans-serif;
  padding: 4rem 2vw 8rem;
  box-sizing: border-box;
  overflow-x: hidden;
  width: 100%;
}

.archive-container {
  max-width: 1000px; /* 适当放宽以容纳更大的字号 */
  margin: 0 auto;
  box-sizing: border-box;
}

/* 卷宗头部：紧凑的秩序感 */
.archive-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  padding-bottom: 2.5rem;
  margin-bottom: 4rem;
  border-bottom: 1px solid rgba(42, 54, 46, 0.15);
  gap: 4rem;
}

.header-context {
  max-width: 520px;
}

.header-tag {
  display: block;
  font-family: 'Manrope', sans-serif;
  font-size: 0.9rem;
  font-weight: 700;
  letter-spacing: 0.15em;
  color: #8a9c90;
  text-transform: uppercase;
  margin-bottom: 1rem;
}

.header-title {
  font-family: 'Noto Serif SC', serif;
  font-size: 2.2rem;
  font-weight: 600;
  color: #1e2821;
  margin: 0 0 1.2rem 0;
  letter-spacing: 0.05em;
}

.header-desc {
  font-size: 1.05rem;
  color: #6a7c70;
  line-height: 1.8;
  margin: 0;
}

.header-stats {
  display: flex;
  gap: 3rem;
}

.stat-block {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.stat-label {
  font-family: 'Noto Serif SC', serif;
  font-size: 0.9rem;
  color: #8a9c90;
}

.stat-value {
  font-family: 'Manrope', sans-serif;
  font-size: 2.2rem;
  font-weight: 600;
  color: #2a362e;
  line-height: 1;
}

/* 最新归档 */
.latest-entry {
  position: relative;
  display: flex;
  gap: 2rem;
  margin-bottom: 5rem;
  padding: 2.5rem;
  background: rgba(255, 255, 255, 0.5);
  border-radius: 16px;
  cursor: pointer;
  transition: background 0.3s ease;
}

.latest-entry:hover {
  background: rgba(255, 255, 255, 0.9);
}

.latest-accent-line {
  width: 4px;
  background: #2a362e;
  border-radius: 4px;
  flex-shrink: 0;
}

.latest-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.latest-meta {
  display: flex;
  gap: 1rem;
  align-items: center;
  margin-bottom: 1.2rem;
}

.meta-label {
  font-family: 'Noto Serif SC', serif;
  font-size: 0.9rem;
  font-weight: 600;
  color: #1e2821;
  background: rgba(42, 54, 46, 0.08);
  padding: 0.3rem 0.8rem;
  border-radius: 100px;
}

.meta-date {
  font-family: 'Manrope', sans-serif;
  font-size: 1rem;
  color: #8a9c90;
}

.latest-title {
  font-family: 'Noto Serif SC', serif;
  font-size: 1.8rem;
  font-weight: 600;
  color: #1e2821;
  margin: 0 0 1rem 0;
}

.latest-summary {
  font-size: 1.1rem;
  color: #5c6b60;
  line-height: 1.8;
  margin: 0 0 2rem 0;
  max-width: 90%;
}

.latest-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: auto;
}

.latest-indicators {
  display: flex;
  align-items: center;
  gap: 1.5rem;
}

.indicator-score {
  font-family: 'Noto Serif SC', serif;
  font-size: 1rem;
  color: #5c6b60;
}

.indicator-score strong {
  font-family: 'Manrope', sans-serif;
  font-size: 1.3rem;
  color: #1e2821;
}

/* 莫兰迪色调状态胶囊 */
.indicator-pill {
  font-family: 'Noto Serif SC', serif;
  font-size: 0.95rem;
  padding: 0.4rem 1rem;
  border-radius: 100px;
  font-weight: 600;
}

.indicator-pill--low {
  background: rgba(130, 150, 138, 0.15);
  color: #4a5c51;
}
.indicator-pill--medium {
  background: rgba(193, 150, 83, 0.15);
  color: #9e7330;
}
.indicator-pill--high {
  background: rgba(176, 115, 115, 0.15);
  color: #8c4a4a;
}

.action-read-btn {
  background: transparent;
  border: none;
  font-family: 'Noto Serif SC', serif;
  font-size: 1.1rem;
  font-weight: 600;
  color: #2a362e;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0;
}

/* 往期列表：对齐与截断 */
.history-list {
  display: flex;
  flex-direction: column;
}

.list-heading {
  font-family: 'Noto Serif SC', serif;
  font-size: 1.1rem;
  font-weight: 600;
  color: #8a9c90;
  margin: 0 0 1rem 0;
  padding-bottom: 1rem;
  border-bottom: 1px solid rgba(42, 54, 46, 0.15);
}

.history-row {
  display: grid;
  /* 调整网格宽度分配，彻底解决挤压与溢出 */
  grid-template-columns: 140px minmax(0, 1fr) 180px 40px;
  gap: 2rem;
  align-items: flex-start; /* 统一顶端对齐 */
  padding: 2rem 0;
  border-bottom: 1px solid rgba(42, 54, 46, 0.08);
  cursor: pointer;
  transition: background 0.3s ease;
  box-sizing: border-box;
}

.history-row:hover {
  background: rgba(255, 255, 255, 0.5);
}

.row-date {
  font-family: 'Manrope', sans-serif;
  font-size: 1rem;
  color: #8a9c90;
  margin-top: 0.2rem;
}

.row-main {
  display: flex;
  flex-direction: column;
  gap: 0.8rem;
  min-width: 0; /* 允许文本截断生效 */
}

.row-title {
  font-family: 'Noto Serif SC', serif;
  font-size: 1.25rem;
  font-weight: 600;
  color: #2a362e;
  margin: 0;
}

.row-summary {
  font-family: 'Noto Serif SC', serif;
  font-size: 1rem;
  color: #7b8c80;
  margin: 0;
  /* 强制单行截断，防止撑开页面 */
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  width: 100%;
}

.row-stats {
  display: flex;
  flex-direction: column;
  align-items: flex-end; /* 右对齐 */
  gap: 0.8rem;
}

.stat-score {
  font-family: 'Noto Serif SC', serif;
  font-size: 0.95rem;
  color: #5c6b60;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.stat-score strong {
  font-family: 'Manrope', sans-serif;
  font-size: 1.2rem;
  color: #1e2821;
}

.minimal-pill {
  font-family: 'Noto Serif SC', serif;
  font-size: 0.9rem;
  font-weight: 600;
  text-align: right;
}
.minimal-pill--low { color: #5c6b60; }
.minimal-pill--medium { color: #9e7330; }
.minimal-pill--high { color: #8c4a4a; }

.row-action {
  color: #b5c2b9;
  font-size: 1.3rem;
  text-align: right;
  transition: color 0.3s ease;
  margin-top: 0.1rem;
}

.history-row:hover .row-action {
  color: #2a362e;
}

/* 优雅的分页器 */
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

/* 通用动画 */
.arrow {
  font-family: 'Manrope', sans-serif;
  transition: transform 0.3s ease;
}

.latest-entry:hover .action-read-btn .arrow,
.history-row:hover .row-action .arrow,
.ghost-btn:hover .arrow {
  transform: translateX(4px);
}
.page-btn:hover:not(:disabled) .arrow:last-child {
  transform: translateX(4px);
}
.page-btn:hover:not(:disabled) .arrow:first-child {
  transform: translateX(-4px);
}

/* 状态提示 */
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

.error-text {
  background: rgba(140, 74, 74, 0.08);
  color: #8c4a4a;
  padding: 1.5rem;
  border-radius: 12px;
  text-align: center;
  font-family: 'Noto Serif SC', serif;
  margin-bottom: 2rem;
}

.empty-title {
  font-family: 'Noto Serif SC', serif;
  font-size: 1.6rem;
  color: #2a362e;
  margin: 0 0 1rem 0;
}

.ghost-btn {
  background: transparent;
  border: 1px solid rgba(42, 54, 46, 0.3);
  color: #2a362e;
  padding: 1rem 2.2rem;
  border-radius: 100px;
  font-family: 'Noto Serif SC', serif;
  font-size: 1.05rem;
  font-weight: 600;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  transition: all 0.3s ease;
}

.ghost-btn:hover {
  background: rgba(42, 54, 46, 0.05);
  border-color: #2a362e;
}

/* 响应式 */
@media (max-width: 900px) {
  .archive-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 2rem;
  }

  .history-row {
    grid-template-columns: 1fr auto 20px;
    gap: 1rem;
    padding: 1.5rem 1rem;
  }

  .row-date {
    display: none;
  }

  .row-summary {
    display: none;
  }

  .row-stats {
    align-items: flex-end;
  }

  .latest-entry {
    flex-direction: column;
    gap: 1rem;
    padding: 1.5rem;
  }

  .latest-accent-line {
    width: 100%;
    height: 4px;
  }

  .latest-bottom {
    flex-direction: column;
    align-items: flex-start;
    gap: 1.5rem;
    margin-top: 1.5rem;
  }
}
</style>