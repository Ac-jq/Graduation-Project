<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchCounselorStudentReportsApi } from '@/api/assessment'
import type { ReportSummary } from '@/api/types'
import { toErrorMessage, toNumberParam } from '@/views/shared/page-logic'

type LevelTone = 'low' | 'medium' | 'high'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const errorMessage = ref('')
const reports = ref<ReportSummary[]>([])

const studentUserId = computed(() => toNumberParam(route.params.studentUserId))

// 前端切片分页状态
const currentPage = ref(1)
const pageSize = 6

const totalPages = computed(() => Math.max(1, Math.ceil(reports.value.length / pageSize)))
const pagedReports = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return reports.value.slice(start, start + pageSize)
})

const highRiskCount = computed(() => reports.value.filter((item) => item.levelCode === 'HIGH').length)

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
  if (!studentUserId.value) {
    errorMessage.value = '学生编号无效'
    reports.value = []
    return
  }

  loading.value = true
  errorMessage.value = ''

  try {
    reports.value = await fetchCounselorStudentReportsApi(studentUserId.value)
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

async function openReport(reportId: number): Promise<void> {
  if (!studentUserId.value) return

  await router.push({
    name: 'counselor-student-report-detail',
    params: { studentUserId: studentUserId.value, reportId }
  })
}

function goBack(): void {
  router.push({ name: 'counselor-students' })
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
  <main class="editorial-archive-page">
    <div class="archive-container">

      <nav class="dossier-nav">
        <button class="nav-ghost-btn" @click="goBack">
          <span class="arrow">←</span> 返回来访者名册
        </button>
      </nav>

      <header class="archive-header">
        <div class="header-context">
          <span class="header-tag">Student Assessment Archive</span>
          <h1 class="header-title">测评报告案卷</h1>
          <p class="header-desc">
            当前正在调阅学生 <strong>#{{ studentUserId || '--' }}</strong> 的历史测评归档。列表按时间倒序排列，便于您快速锚定风险波动与情绪趋势。
          </p>
        </div>

        <div class="header-stats">
          <div class="stat-block">
            <span class="stat-label">历史归档</span>
            <span class="stat-value">{{ loading ? '-' : reports.length }}</span>
          </div>
          <div class="stat-block">
            <span class="stat-label">高关注提醒</span>
            <span class="stat-value highlight">{{ loading ? '-' : highRiskCount }}</span>
          </div>
        </div>
      </header>

      <div v-if="errorMessage" class="error-text">{{ errorMessage }}</div>

      <div v-if="loading" class="loading-state">
        <div class="spinner"></div>
        <p>正在展开案卷...</p>
      </div>

      <div v-else-if="!reports.length" class="empty-state">
        <h2 class="empty-title">卷宗尚为空白</h2>
        <p class="empty-desc">该学生暂未留下任何正式的量表测评记录。</p>
      </div>

      <section v-else class="history-list">
        <div class="list-toolbar">
          <span class="toolbar-status">
            当前显示第 {{ currentPage }} 页，共 {{ totalPages }} 页
          </span>
        </div>

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
            <span class="arrow">←</span> 往前翻
          </button>

          <div class="page-indicator">
            <span>{{ currentPage }}</span> / <span>{{ totalPages }}</span>
          </div>

          <button
              class="page-btn"
              :disabled="currentPage >= totalPages"
              @click="nextPage"
          >
            往后翻 <span class="arrow">→</span>
          </button>
        </nav>

      </section>

    </div>
  </main>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700&family=Noto+Serif+SC:wght@500;600;700&display=swap');

/* 全局基调与宽度控制，彻底解决左右滑动 */
.editorial-archive-page {
  min-height: 100vh;
  background: #fcfbf9;
  color: #1e2821;
  font-family: 'Manrope', 'Noto Serif SC', sans-serif;
  padding: 2rem 2vw 8rem;
  box-sizing: border-box;
  overflow-x: hidden;
  width: 100%;
}

.archive-container {
  max-width: 1000px;
  margin: 0 auto;
  box-sizing: border-box;
}

/* 顶部导航 */
.dossier-nav {
  margin-bottom: 3rem;
}

.nav-ghost-btn {
  background: transparent;
  border: none;
  font-family: 'Noto Serif SC', serif;
  font-size: 1rem;
  font-weight: 600;
  color: #5c6b60;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0;
  transition: color 0.3s ease;
}

.nav-ghost-btn:hover {
  color: #1e2821;
}

/* 卷宗头部：紧凑的秩序感 */
.archive-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  padding-bottom: 2.5rem;
  margin-bottom: 2rem;
  border-bottom: 1px solid rgba(42, 54, 46, 0.15);
  gap: 4rem;
}

.header-context {
  max-width: 560px;
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
  font-size: 2.6rem;
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

.header-desc strong {
  color: #2a362e;
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

.stat-value.highlight {
  color: #8c4a4a;
}

/* 列表控制栏 */
.list-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
  padding: 0 0.5rem;
}

.toolbar-status {
  font-family: 'Noto Serif SC', serif;
  font-size: 0.95rem;
  color: #8a9c90;
}

/* 往期列表：对齐与单行截断 */
.history-list {
  display: flex;
  flex-direction: column;
}

.history-row {
  display: grid;
  grid-template-columns: 140px minmax(0, 1fr) 180px 40px;
  gap: 2rem;
  align-items: flex-start;
  padding: 2.2rem 0;
  border-bottom: 1px solid rgba(42, 54, 46, 0.08);
  cursor: pointer;
  transition: background 0.3s ease;
  box-sizing: border-box;
}

.history-row:hover {
  background: rgba(255, 255, 255, 0.6);
}

.row-date {
  font-family: 'Manrope', sans-serif;
  font-size: 1.05rem;
  color: #8a9c90;
  margin-top: 0.2rem;
}

.row-main {
  display: flex;
  flex-direction: column;
  gap: 0.8rem;
  min-width: 0;
}

.row-title {
  font-family: 'Noto Serif SC', serif;
  font-size: 1.35rem;
  font-weight: 600;
  color: #2a362e;
  margin: 0;
}

.row-summary {
  font-family: 'Noto Serif SC', serif;
  font-size: 1.05rem;
  color: #7b8c80;
  margin: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  width: 100%;
}

.row-stats {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
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
  font-size: 1.3rem;
  color: #1e2821;
}

.minimal-pill {
  font-family: 'Noto Serif SC', serif;
  font-size: 0.95rem;
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

/* 交互动画 */
.arrow {
  font-family: 'Manrope', sans-serif;
  transition: transform 0.3s ease;
}

.nav-ghost-btn:hover .arrow {
  transform: translateX(-4px);
}

.history-row:hover .row-action .arrow,
.page-btn:hover:not(:disabled) .arrow:last-child {
  transform: translateX(4px);
}

.page-btn:hover:not(:disabled) .arrow:first-child {
  transform: translateX(-4px);
}

/* 状态样式 */
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
}
</style>