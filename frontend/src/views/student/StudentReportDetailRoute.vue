<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
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

function resolveResourceType(type: string): string {
  switch (type) {
    case 'ARTICLE': return '图文阅览'
    case 'VIDEO': return '视频影像'
    case 'AUDIO': return '声音片段'
    case 'IMAGE': return '图像内容'
    case 'LINK': return '外部指引'
    default: return type
  }
}

function formatDate(value: string): string {
  const date = new Date(value)
  return `${date.getFullYear()}/${String(date.getMonth() + 1).padStart(2, '0')}/${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

async function loadReportDetail(): Promise<void> {
  if (!reportId.value) {
    errorMessage.value = '报告编号无效'
    reportDetail.value = null
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

watch(
    () => route.params.reportId,
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
        <button class="ghost-btn" @click="router.push({ name: 'student-reports' })">
          <span class="arrow">←</span> 归档目录
        </button>
      </nav>

      <div v-if="errorMessage" class="error-banner">{{ errorMessage }}</div>

      <div v-if="loading" class="loading-state">
        <div class="spinner"></div>
        <p>正在解开卷宗...</p>
      </div>

      <article v-else-if="reportDetail" class="dossier-paper">

        <header class="dossier-header">
          <div class="header-thick-line"></div>
          <div class="dossier-meta">
            <span class="meta-item">卷宗编号 <strong>#{{ reportDetail.reportId }}</strong></span>
            <span class="meta-item">生成日期 <strong>{{ formatDate(reportDetail.createdAt) }}</strong></span>
            <span class="meta-item">记录主体 <strong>{{ reportDetail.studentName || '匿名化' }}</strong></span>
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
              <span class="dash-label">人工干预建议</span>
              <strong class="dash-text">{{ reportDetail.recommendAppointment ? '建议预约' : '无需强制干预' }}</strong>
            </div>
          </div>
        </header>

        <section class="editorial-stack">
          <div class="column-block column-block--primary">
            <h3 class="column-heading">深度解读</h3>
            <div class="column-text interpretation-content">
              {{ reportDetail.aiInterpretation || '本次评估显示数据较为稳定，系统未生成额外的异常预警解释。请结合自身实际感受进行判断。' }}
            </div>
          </div>

          <div class="column-block column-block--secondary">
            <div class="secondary-header">
              <h4 class="secondary-heading">后续建议</h4>
              <div class="heading-line"></div>
            </div>
            <div class="secondary-content">
              <p class="secondary-text">
                {{ reportDetail.recommendationNote || '保持当前的作息与规律，适度关注自我情绪变化即可。' }}
              </p>
              <button
                  v-if="reportDetail.recommendAppointment"
                  class="action-btn action-btn--primary"
                  type="button"
                  @click="router.push({ name: 'student-appointment-slots' })"
              >
                前往预约心理咨询 <span class="arrow">→</span>
              </button>
            </div>
          </div>
        </section>

        <section class="resource-appendix" v-if="reportDetail.recommendedResources.length">
          <div class="appendix-head">
            <h3>相关资料与支持</h3>
            <span>附录 {{ reportDetail.recommendedResources.length }} 项</span>
          </div>

          <div class="appendix-list">
            <div
                v-for="resource in reportDetail.recommendedResources"
                :key="resource.resourceId"
                class="appendix-item"
                @click="router.push({ name: 'student-resource-detail', params: { resourceId: resource.resourceId } })"
            >
              <div class="item-meta">
                <span class="item-type">{{ resolveResourceType(resource.resourceType) }}</span>
                <span class="item-category">{{ resource.categoryName }}</span>
              </div>
              <div class="item-main">
                <h4 class="item-title">{{ resource.title }}</h4>
                <p class="item-summary">{{ resource.summaryText }}</p>
              </div>
              <div class="item-action">
                <span class="arrow">→</span>
              </div>
            </div>
          </div>
        </section>

        <footer class="dossier-footer">
          <div class="footer-thin-line"></div>
          <p class="disclaimer">
            <strong>* 郑重声明：</strong>
            {{ reportDetail.noticeText || '本报告生成的所有数据、分析及建议仅限于提供情绪觉察的参考视角，绝不具备医学诊断效力。如遭遇严重的情绪危机或持续困扰，请务必寻求线下专业医疗机构或心理干预中心的帮助。' }}
          </p>
          <div class="footer-actions">
            <button class="ghost-btn" @click="router.push({ name: 'student-scales' })">
              发起一次新测评
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
  margin-bottom: 2rem;
}

.ghost-btn {
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

.ghost-btn:hover {
  color: #1e2821;
}

/* 卷宗排版区 */
.dossier-paper {
  background: transparent;
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
  font-size: 0.85rem;
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

/* 数据仪表盘 */
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

/* ★ 杂志排版布局优化：单列堆叠布局 ★ */
.editorial-stack {
  display: flex;
  flex-direction: column;
  gap: 5rem;
}

.column-block {
  display: flex;
  flex-direction: column;
}

/* 深度解读：突出显示 */
.column-block--primary .column-heading {
  font-size: 1.8rem;
  margin-bottom: 2rem;
}

.column-block--primary .interpretation-content {
  font-size: 1.2rem;
  line-height: 2;
  color: #2a362e;
  text-align: justify;
  background: rgba(130, 150, 138, 0.03);
  padding: 2.5rem;
  border-radius: 12px;
  border-left: 4px solid #2a362e;
}

/* 后续建议：弱化展示 */
.column-block--secondary {
  background: #f6f5f0;
  padding: 2.5rem;
  border-radius: 12px;
  margin-top: -1rem;
}

.secondary-header {
  display: flex;
  align-items: center;
  gap: 1.5rem;
  margin-bottom: 1.5rem;
}

.secondary-heading {
  font-family: 'Noto Serif SC', serif;
  font-size: 1.1rem;
  font-weight: 700;
  color: #8a9c90;
  white-space: nowrap;
  margin: 0;
}

.heading-line {
  flex-grow: 1;
  height: 1px;
  background: rgba(138, 156, 144, 0.2);
}

.secondary-content {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.secondary-text {
  font-size: 1rem;
  color: #5c6b60;
  margin: 0;
}

.column-heading {
  font-family: 'Noto Serif SC', serif;
  font-size: 1.35rem;
  font-weight: 600;
  color: #1e2821;
  margin: 0 0 1.5rem 0;
  position: relative;
}

/* 附录推荐资源 */
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
  grid-template-columns: 140px minmax(0, 1fr) 40px;
  gap: 2rem;
  align-items: center;
  padding: 1.8rem 0;
  border-bottom: 1px solid rgba(42, 54, 46, 0.06);
  cursor: pointer;
  transition: background 0.3s ease;
}

.appendix-item:hover {
  background: rgba(255, 255, 255, 0.6);
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
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.item-action {
  color: #b5c2b9;
  font-size: 1.2rem;
  text-align: right;
  transition: color 0.3s ease;
}

.appendix-item:hover .item-action {
  color: #2a362e;
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

.footer-actions .ghost-btn {
  border: 1px solid rgba(42, 54, 46, 0.3);
  padding: 0.8rem 2rem;
  border-radius: 100px;
}

.action-btn {
  align-self: flex-start;
  padding: 0.9rem 1.8rem;
  border-radius: 100px;
  font-family: 'Noto Serif SC', serif;
  font-size: 0.95rem;
  font-weight: 600;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  transition: all 0.3s ease;
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

.action-btn:hover .arrow,
.appendix-item:hover .item-action .arrow {
  transform: translateX(4px);
}

/* 响应式 */
@media (max-width: 900px) {
  .editorial-stack {
    gap: 3.5rem;
  }

  .column-block--primary .interpretation-content {
    padding: 1.5rem;
    font-size: 1.1rem;
  }

  .appendix-item {
    grid-template-columns: 100px minmax(0, 1fr) 20px;
    gap: 1rem;
  }
}
</style>