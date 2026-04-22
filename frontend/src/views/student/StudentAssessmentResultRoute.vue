<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchStudentReportDetailApi } from '@/api/assessment'
import type { ReportDetail } from '@/api/types'
import { useAssessmentStore } from '@/stores/assessment'
import { toErrorMessage, toNumberParam } from '@/views/shared/page-logic'

type LevelTone = 'low' | 'medium' | 'high'

const route = useRoute()
const router = useRouter()
const assessmentStore = useAssessmentStore()

const loading = ref(false)
const errorMessage = ref('')
const reportDetail = ref<ReportDetail | null>(null)

const reportId = computed(() => toNumberParam(route.params.reportId))

const displayScore = computed(() => assessmentStore.latestSubmit?.totalScore ?? reportDetail.value?.totalScore ?? '--')
const displayLevel = computed(() => assessmentStore.latestSubmit?.levelCode || reportDetail.value?.levelCode)
const displayReportId = computed(() => assessmentStore.latestSubmit?.reportId ?? reportDetail.value?.reportId ?? '--')

function resolveLevelLabel(levelCode?: string | null): string {
  switch (levelCode) {
    case 'LOW': return '状态平稳'
    case 'MEDIUM': return '需适度关注'
    case 'HIGH': return '建议重点关注'
    default: return '待评估'
  }
}

function resolveLevelTone(levelCode?: string | null): LevelTone {
  switch (levelCode) {
    case 'HIGH': return 'high'
    case 'MEDIUM': return 'medium'
    default: return 'low'
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
  <main class="editorial-result-page">
    <div class="page-container">

      <header class="result-hero">
        <div class="hero-copy">
          <span class="hero-tag">Assessment Result</span>
          <h1 class="huge-title">本次梳理已完成。</h1>
          <p class="hero-lead">
            系统已经基于标准量表完成了初步的计分。你可以先在此处查看方向性的结论，稍后在完整卷宗里查阅匹配的支持资源。
          </p>
        </div>

        <div class="result-dashboard" v-if="assessmentStore.latestSubmit || reportDetail">
          <div class="dashboard-item">
            <span class="dash-label">梳理卷宗编号</span>
            <strong class="dash-text">#{{ displayReportId }}</strong>
          </div>
          <div class="dash-divider"></div>
          <div class="dashboard-item">
            <span class="dash-label">评估刻度</span>
            <strong class="dash-value">{{ displayScore }}</strong>
          </div>
          <div class="dash-divider"></div>
          <div class="dashboard-item">
            <span class="dash-label">系统反馈</span>
            <span class="level-pill" :class="`level-pill--${resolveLevelTone(displayLevel)}`">
              {{ resolveLevelLabel(displayLevel) }}
            </span>
          </div>
        </div>
      </header>

      <div v-if="errorMessage" class="error-banner">{{ errorMessage }}</div>

      <div v-if="loading" class="loading-state">
        <div class="spinner"></div>
        <p>正在生成本次的反馈附言...</p>
      </div>

      <template v-else-if="reportDetail">

        <section class="editorial-columns">
          <article class="column-block">
            <h3 class="column-heading">结果摘要</h3>
            <p class="column-text">
              {{ reportDetail.summaryText || '系统暂无摘要输出。' }}
            </p>
          </article>

          <article class="column-block">
            <h3 class="column-heading">补充视角与解释</h3>
            <p class="column-text column-text--scrollable">
              {{ reportDetail.aiInterpretation || '系统已生成结构化卷宗，你可以继续前往查阅包含建议的完整报告。' }}
            </p>
          </article>
        </section>

        <section class="disclaimer-section">
          <div class="thin-accent-line"></div>
          <h3 class="disclaimer-title">客观参考声明</h3>
          <p class="disclaimer-text">
            {{
              reportDetail.noticeText ||
              '以上提示仅代表基于本次量表数据生成的客观刻度，不作为也无法替代任何形式的医学诊断。如觉不适，请联系身边的支持网络或专业机构。'
            }}
          </p>
        </section>

        <nav class="action-nav">
          <button class="ghost-btn" type="button" @click="router.push({ name: 'student-scales' })">
            重新挑选其他量表
          </button>
          <button
              class="action-btn action-btn--primary"
              type="button"
              @click="router.push({ name: 'student-report-detail', params: { reportId: reportDetail.reportId } })"
          >
            打开完整卷宗 <span class="arrow">→</span>
          </button>
        </nav>

      </template>

    </div>
  </main>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700;800&family=Noto+Serif+SC:wght@500;600;700&display=swap');

/* 全局纸张底色 */
.editorial-result-page {
  min-height: 100vh;
  background: #fcfbf9;
  color: #1e2821;
  font-family: 'Manrope', 'Noto Serif SC', sans-serif;
  padding: 4rem 2vw 8rem;
  box-sizing: border-box;
}

.page-container {
  max-width: 900px;
  margin: 0 auto;
}

/* ================= 头部排版 ================= */
.result-hero {
  margin-bottom: 4rem;
}

.hero-copy {
  margin-bottom: 3.5rem;
  padding-bottom: 2.5rem;
  border-bottom: 1px solid rgba(42, 54, 46, 0.1);
}

.hero-tag {
  display: block;
  font-family: 'Manrope', sans-serif;
  font-size: 0.85rem;
  font-weight: 700;
  letter-spacing: 0.15em;
  color: #8a9c90;
  text-transform: uppercase;
  margin-bottom: 1rem;
}

.huge-title {
  font-family: 'Noto Serif SC', serif;
  font-size: clamp(2.6rem, 5vw, 4rem);
  font-weight: 600;
  color: #1e2821;
  margin: 0 0 1.2rem 0;
  line-height: 1.1;
  letter-spacing: 0.02em;
}

.hero-lead {
  font-size: 1.1rem;
  color: #5c6b60;
  line-height: 1.85;
  margin: 0;
  max-width: 680px;
}

/* ================= 无框数据控制台 ================= */
.result-dashboard {
  display: flex;
  align-items: center;
  gap: 3rem;
  flex-wrap: wrap;
}

.dashboard-item {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  align-items: flex-start;
}

.dash-label {
  font-family: 'Noto Serif SC', serif;
  font-size: 0.85rem;
  color: #8a9c90;
  text-transform: uppercase;
  letter-spacing: 0.1em;
}

.dash-value {
  font-family: 'Manrope', sans-serif;
  font-size: 2.6rem;
  font-weight: 600;
  color: #2a362e;
  line-height: 1;
}

.dash-text {
  font-family: 'Manrope', sans-serif;
  font-size: 1.4rem;
  font-weight: 600;
  color: #5c6b60;
  margin-top: 0.5rem;
}

.dash-divider {
  width: 1px;
  height: 2.5rem;
  background: rgba(42, 54, 46, 0.15);
}

/* ================= 莫兰迪状态胶囊 ================= */
.level-pill {
  font-family: 'Noto Serif SC', serif;
  font-size: 1rem;
  font-weight: 600;
  padding: 0.4rem 1.2rem;
  border-radius: 100px;
  margin-top: 0.5rem;
}
.level-pill--low { background: rgba(130, 150, 138, 0.15); color: #4a5c51; }
.level-pill--medium { background: rgba(193, 150, 83, 0.15); color: #9e7330; }
.level-pill--high { background: rgba(176, 115, 115, 0.15); color: #8c4a4a; }

/* ================= 杂志分栏解读 ================= */
.editorial-columns {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 4rem;
  margin-bottom: 4rem;
}

.column-block {
  display: flex;
  flex-direction: column;
}

.column-heading {
  font-family: 'Noto Serif SC', serif;
  font-size: 1.35rem;
  font-weight: 600;
  color: #1e2821;
  margin: 0 0 1.2rem 0;
  position: relative;
  padding-left: 1rem;
}

.column-heading::before {
  content: '';
  position: absolute;
  left: 0;
  top: 10%;
  height: 80%;
  width: 3px;
  background: #2a362e;
}

.column-text {
  font-size: 1.05rem;
  line-height: 1.85;
  color: #4a5c51;
  margin: 0;
}

.column-text--scrollable {
  max-height: 400px;
  overflow-y: auto;
  padding-right: 0.75rem;
}

.column-text--scrollable::-webkit-scrollbar {
  width: 5px;
}

.column-text--scrollable::-webkit-scrollbar-track {
  background: transparent;
}

.column-text--scrollable::-webkit-scrollbar-thumb {
  background: rgba(138, 156, 144, 0.28);
  border-radius: 999px;
}

/* ================= 极简声明 ================= */
.disclaimer-section {
  display: flex;
  flex-direction: column;
  gap: 0.8rem;
  margin-bottom: 4rem;
}

.thin-accent-line {
  width: 100%;
  height: 1px;
  background: rgba(42, 54, 46, 0.15);
  margin-bottom: 1rem;
}

.disclaimer-title {
  font-family: 'Noto Serif SC', serif;
  font-size: 1.1rem;
  font-weight: 600;
  color: #1e2821;
  margin: 0;
}

.disclaimer-text {
  font-size: 0.95rem;
  line-height: 1.8;
  color: #8a9c90;
  margin: 0;
}

/* ================= 底部操作 ================= */
.action-nav {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1.5rem;
  flex-wrap: wrap;
}

.ghost-btn {
  background: transparent;
  border: 1px solid rgba(42, 54, 46, 0.3);
  color: #5c6b60;
  padding: 1.2rem 2.2rem;
  border-radius: 100px;
  font-family: 'Noto Serif SC', serif;
  font-size: 1.05rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.ghost-btn:hover {
  background: rgba(42, 54, 46, 0.05);
  color: #1e2821;
}

.action-btn {
  padding: 1.2rem 2.2rem;
  border-radius: 100px;
  font-family: 'Noto Serif SC', serif;
  font-size: 1.05rem;
  font-weight: 600;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 0.6rem;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.action-btn--primary {
  background: #2a362e;
  border: none;
  color: #ffffff;
  box-shadow: 0 12px 24px rgba(42, 54, 46, 0.15);
}

.action-btn--primary:hover {
  background: #1c2620;
  transform: translateY(-2px);
  box-shadow: 0 16px 32px rgba(42, 54, 46, 0.25);
}

.arrow {
  font-family: 'Manrope', sans-serif;
  transition: transform 0.3s ease;
}

.action-btn:hover .arrow {
  transform: translateX(4px);
}

/* ================= 状态 ================= */
.error-banner {
  background: rgba(140, 74, 74, 0.08);
  color: #8c4a4a;
  padding: 1.5rem;
  border-radius: 12px;
  text-align: center;
  font-family: 'Noto Serif SC', serif;
  margin-bottom: 2rem;
}

.loading-state {
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

/* ================= 响应式 ================= */
@media (max-width: 900px) {
  .result-dashboard {
    gap: 2rem;
  }

  .dash-divider {
    display: none;
  }

  .editorial-columns {
    grid-template-columns: 1fr;
    gap: 3rem;
  }

  .action-nav {
    flex-direction: column-reverse;
    align-items: stretch;
  }

  .action-btn, .ghost-btn {
    width: 100%;
    justify-content: center;
  }
}
</style>
