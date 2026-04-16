<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchCounselorStudentReportDetailApi } from '@/api/assessment'
import type { ReportDetail } from '@/api/types'
import { toErrorMessage, toNumberParam } from '@/views/shared/page-logic'

type LevelTone = 'low' | 'medium' | 'high'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const errorMessage = ref('')
const reportDetail = ref<ReportDetail | null>(null)

const studentUserId = computed(() => toNumberParam(route.params.studentUserId))
const reportId = computed(() => toNumberParam(route.params.reportId))

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

function formatDate(value: string): string {
  const date = new Date(value)
  return `${date.getFullYear()}/${String(date.getMonth() + 1).padStart(2, '0')}/${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

async function loadReportDetail(): Promise<void> {
  if (!studentUserId.value || !reportId.value) {
    errorMessage.value = '路由参数无效'
    reportDetail.value = null
    return
  }

  loading.value = true
  errorMessage.value = ''

  try {
    reportDetail.value = await fetchCounselorStudentReportDetailApi(studentUserId.value, reportId.value)
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    loading.value = false
  }
}

function goBackToList(): void {
  if (studentUserId.value) {
    router.push({ name: 'counselor-student-reports', params: { studentUserId: studentUserId.value } })
  } else {
    router.push({ name: 'counselor-students' })
  }
}

watch(
    () => [route.params.studentUserId, route.params.reportId],
    () => {
      void loadReportDetail()
    }
)

onMounted(() => {
  void loadReportDetail()
})
</script>

<template>
  <main class="dossier-detail-page">
    <div class="dossier-container">

      <nav class="dossier-nav">
        <button class="nav-ghost-btn" @click="goBackToList">
          <span class="arrow">←</span> 返回该学生的历史案卷
        </button>
      </nav>

      <div v-if="errorMessage" class="error-banner">{{ errorMessage }}</div>

      <div v-if="loading" class="loading-state">
        <div class="spinner"></div>
        <p>正在调阅详情案卷...</p>
      </div>

      <article v-else-if="reportDetail" class="dossier-paper">

        <header class="dossier-header">
          <div class="header-thick-line"></div>
          <div class="dossier-meta">
            <span class="meta-item">卷宗编号 <strong>#{{ reportDetail.reportId }}</strong></span>
            <span class="meta-item">生成日期 <strong>{{ formatDate(reportDetail.createdAt) }}</strong></span>
            <span class="meta-item">来访者 <strong>{{ reportDetail.studentName || `#${reportDetail.studentUserId}` }}</strong></span>
            <span class="meta-item">学号 <strong>{{ reportDetail.studentNo || '未记录' }}</strong></span>
          </div>

          <h1 class="dossier-title">{{ reportDetail.scaleName }}</h1>
          <p class="dossier-lead">{{ reportDetail.summaryText }}</p>

          <div class="dossier-dashboard">
            <div class="dashboard-item">
              <span class="dash-label">评估得分</span>
              <strong class="dash-value">{{ reportDetail.totalScore }}</strong>
            </div>
            <div class="dashboard-item">
              <span class="dash-label">状态判定</span>
              <span class="level-pill" :class="`level-pill--${resolveLevelTone(reportDetail.levelCode)}`">
                {{ resolveLevelLabel(reportDetail.levelCode) }}
              </span>
            </div>
            <div class="dashboard-item">
              <span class="dash-label">系统干预建议</span>
              <strong class="dash-text">{{ reportDetail.recommendAppointment ? '建议发起人工面谈' : '暂无强制干预要求' }}</strong>
            </div>
          </div>
        </header>

        <section class="editorial-columns">
          <div class="column-block">
            <h3 class="column-heading">AI 深度解读</h3>
            <p class="column-text">
              {{ reportDetail.aiInterpretation || '本次评估显示数据较为稳定，系统未生成额外的异常预警解释。' }}
            </p>
          </div>

          <div class="column-block">
            <h3 class="column-heading">辅助行动建议</h3>
            <p class="column-text">
              {{ reportDetail.recommendationNote || '建议在下次沟通中，适当关注该生近期的作息与基础情绪变化。' }}
            </p>
          </div>
        </section>

        <section class="resource-appendix" v-if="reportDetail.recommendedResources.length">
          <div class="appendix-head">
            <h3>系统为该生匹配的配套内容</h3>
            <span>附录 {{ reportDetail.recommendedResources.length }} 项</span>
          </div>

          <div class="appendix-list">
            <div
                v-for="resource in reportDetail.recommendedResources"
                :key="resource.resourceId"
                class="appendix-item"
            >
              <div class="item-meta">
                <span class="item-type">{{ resource.resourceType === 'ARTICLE' ? '图文' : resource.resourceType === 'VIDEO' ? '影像' : resource.resourceType }}</span>
                <span class="item-category">{{ resource.categoryName }}</span>
              </div>
              <div class="item-main">
                <h4 class="item-title">{{ resource.title }}</h4>
                <p class="item-summary">{{ resource.summaryText }}</p>
              </div>
            </div>
          </div>
        </section>

        <footer class="dossier-footer">
          <div class="footer-thin-line"></div>
          <p class="disclaimer">
            <strong>* 郑重声明：</strong>
            {{ reportDetail.noticeText || '本报告生成的所有数据、分析及建议仅供咨询师作为线下沟通的辅助参考视角，绝不具备最终医学诊断效力。如该生遭遇严重情绪危机，请立即启动危机干预预案。' }}
          </p>
          <div class="footer-actions">
            <button class="ghost-btn" @click="goBackToList">
              阅毕，返回目录
            </button>
          </div>
        </footer>

      </article>
    </div>
  </main>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700&family=Noto+Serif+SC:wght@500;600;700&display=swap');

/* 全局极简白纸底色 */
.dossier-detail-page {
  min-height: 100vh;
  background: #fcfbf9;
  color: #1e2821;
  font-family: 'Manrope', 'Noto Serif SC', sans-serif;
  padding: 2rem 2vw 8rem;
  box-sizing: border-box;
}

.dossier-container {
  max-width: 960px;
  margin: 0 auto;
}

/* 顶部极简导航 */
.dossier-nav {
  margin-bottom: 2.5rem;
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

/* 卷宗排版区 */
.dossier-paper {
  background: transparent; /* 去除卡片底色 */
  display: flex;
  flex-direction: column;
  gap: 4rem;
}

/* 卷宗头部（公文风） */
.dossier-header {
  display: flex;
  flex-direction: column;
}

.header-thick-line {
  width: 100%;
  height: 6px;
  background: #2a362e;
  margin-bottom: 1.5rem;
}

.dossier-meta {
  display: flex;
  gap: 2rem;
  flex-wrap: wrap;
  font-family: 'Manrope', 'Noto Serif SC', sans-serif;
  font-size: 0.9rem;
  color: #8a9c90;
  margin-bottom: 3rem;
}

.meta-item strong {
  color: #2a362e;
  margin-left: 0.4rem;
}

.dossier-title {
  font-family: 'Noto Serif SC', serif;
  font-size: clamp(2.2rem, 4vw, 3.2rem);
  font-weight: 600;
  color: #1e2821;
  margin: 0 0 1.5rem 0;
  line-height: 1.2;
  letter-spacing: 0.02em;
}

.dossier-lead {
  font-family: 'Noto Serif SC', serif;
  font-size: 1.15rem;
  color: #5c6b60;
  line-height: 1.8;
  margin: 0 0 3.5rem 0;
  max-width: 90%;
}

/* 数据仪表盘（融入排版，不使用框） */
.dossier-dashboard {
  display: flex;
  gap: 4rem;
  flex-wrap: wrap;
  padding-bottom: 3rem;
  border-bottom: 1px solid rgba(42, 54, 46, 0.15);
}

.dashboard-item {
  display: flex;
  flex-direction: column;
  gap: 0.8rem;
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
  font-size: 2.8rem;
  font-weight: 600;
  color: #2a362e;
  line-height: 1;
}

.dash-text {
  font-family: 'Noto Serif SC', serif;
  font-size: 1.3rem;
  font-weight: 600;
  color: #2a362e;
  margin-top: 0.5rem;
}

/* 状态胶囊 */
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

/* 杂志分栏排版 */
.editorial-columns {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 5rem;
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
  margin: 0 0 1.5rem 0;
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
  margin: 0 0 2rem 0;
}

/* 附录推荐资源（极简目录排版） */
.resource-appendix {
  display: flex;
  flex-direction: column;
}

.appendix-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  padding-bottom: 1rem;
  border-bottom: 1px solid rgba(42, 54, 46, 0.15);
  margin-bottom: 1rem;
}

.appendix-head h3 {
  font-family: 'Noto Serif SC', serif;
  font-size: 1.25rem;
  font-weight: 600;
  color: #1e2821;
  margin: 0;
}

.appendix-head span {
  font-family: 'Noto Serif SC', serif;
  font-size: 0.9rem;
  color: #8a9c90;
}

.appendix-list {
  display: flex;
  flex-direction: column;
}

.appendix-item {
  display: grid;
  grid-template-columns: 100px minmax(0, 1fr);
  gap: 2rem;
  align-items: center;
  padding: 1.8rem 0;
  border-bottom: 1px solid rgba(42, 54, 46, 0.06);
}

.item-meta {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.item-type {
  font-family: 'Noto Serif SC', serif;
  font-size: 0.8rem;
  font-weight: 600;
  color: #5c6b60;
  background: rgba(130, 150, 138, 0.12);
  padding: 0.2rem 0.6rem;
  border-radius: 6px;
  align-self: flex-start;
}

.item-category {
  font-family: 'Noto Serif SC', serif;
  font-size: 0.85rem;
  color: #8a9c90;
  padding-left: 0.2rem;
}

.item-main {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.item-title {
  font-family: 'Noto Serif SC', serif;
  font-size: 1.2rem;
  font-weight: 600;
  color: #2a362e;
  margin: 0;
}

.item-summary {
  font-family: 'Noto Serif SC', serif;
  font-size: 0.95rem;
  color: #7b8c80;
  margin: 0;
  line-height: 1.6;
}

/* 底部声明与动作 */
.dossier-footer {
  display: flex;
  flex-direction: column;
  gap: 2rem;
  margin-top: 2rem;
}

.footer-thin-line {
  width: 100%;
  height: 1px;
  background: rgba(42, 54, 46, 0.15);
}

.disclaimer {
  font-family: 'Noto Serif SC', serif;
  font-size: 0.9rem;
  color: #8a9c90;
  line-height: 1.8;
  margin: 0;
}

.disclaimer strong {
  color: #5c6b60;
}

.footer-actions {
  display: flex;
  justify-content: center;
  padding-top: 1rem;
}

.ghost-btn {
  border: 1px solid rgba(42, 54, 46, 0.3);
  padding: 0.8rem 2rem;
  border-radius: 100px;
  font-family: 'Noto Serif SC', serif;
  font-size: 1rem;
  font-weight: 600;
  color: #5c6b60;
  background: transparent;
  cursor: pointer;
  transition: all 0.3s ease;
}

.ghost-btn:hover {
  background: rgba(42, 54, 46, 0.05);
  color: #2a362e;
}

/* 状态样式 */
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

/* 交互动效 */
.arrow {
  font-family: 'Manrope', sans-serif;
  transition: transform 0.3s ease;
}

.nav-ghost-btn:hover .arrow {
  transform: translateX(-4px);
}

/* 响应式 */
@media (max-width: 900px) {
  .dossier-dashboard {
    gap: 2rem;
  }

  .editorial-columns {
    grid-template-columns: 1fr;
    gap: 3.5rem;
  }

  .appendix-item {
    grid-template-columns: 1fr;
    gap: 1rem;
    padding: 1.5rem 0;
  }

  .item-meta {
    flex-direction: row;
    align-items: center;
  }
}
</style>