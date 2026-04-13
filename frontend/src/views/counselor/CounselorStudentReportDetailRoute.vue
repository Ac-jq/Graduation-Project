<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchCounselorStudentReportDetailApi } from '@/api/assessment'
import type { ReportDetail } from '@/api/types'
import { toErrorMessage, toNumberParam } from '@/views/shared/page-logic'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const errorMessage = ref('')
const reportDetail = ref<ReportDetail | null>(null)

const studentUserId = computed(() => toNumberParam(route.params.studentUserId))
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
  <section class="counselor-report-detail-page">
    <p v-if="loading" class="counselor-report-detail-page__status-panel">正在读取报告详情...</p>
    <p v-else-if="errorMessage" class="counselor-report-detail-page__alert">{{ errorMessage }}</p>

    <template v-else-if="reportDetail">
      <header class="counselor-report-detail-page__hero">
        <div class="counselor-report-detail-page__copy">
          <p class="counselor-report-detail-page__eyebrow">Counselor Report</p>
          <h1>{{ reportDetail.scaleName }}</h1>
          <p class="counselor-report-detail-page__lead">
            当前正在查看学生 {{ reportDetail.studentName || `#${reportDetail.studentUserId}` }} 的测评报告。页面保留原始评分结果、辅助解释和推荐内容，方便咨询前预读。
          </p>
        </div>
        <div class="counselor-report-detail-page__metrics">
          <div class="metric-card">
            <span>等级</span>
            <strong>{{ resolveLevelLabel(reportDetail.levelCode) }}</strong>
          </div>
          <div class="metric-card">
            <span>总分</span>
            <strong>{{ reportDetail.totalScore }}</strong>
          </div>
        </div>
      </header>

      <div class="counselor-report-detail-page__grid">
        <article class="glass-panel">
          <div class="section-head">
            <p class="section-kicker">报告摘要</p>
            <h2>核心结果</h2>
          </div>
          <p class="body-text">{{ reportDetail.summaryText }}</p>
          <dl class="meta-grid">
            <div>
              <dt>报告编号</dt>
              <dd>#{{ reportDetail.reportId }}</dd>
            </div>
            <div>
              <dt>学号</dt>
              <dd>{{ reportDetail.studentNo || '-' }}</dd>
            </div>
            <div>
              <dt>生成时间</dt>
              <dd>{{ new Date(reportDetail.createdAt).toLocaleString('zh-CN') }}</dd>
            </div>
            <div>
              <dt>建议预约</dt>
              <dd>{{ reportDetail.recommendAppointment ? '是' : '否' }}</dd>
            </div>
          </dl>
        </article>

        <article class="glass-panel">
          <div class="section-head">
            <p class="section-kicker">AI 辅助解释</p>
            <h2>解释与建议</h2>
          </div>
          <p class="body-text">{{ reportDetail.aiInterpretation || '当前报告没有额外 AI 辅助解释。' }}</p>
          <p class="body-text counselor-report-detail-page__recommend">
            {{ reportDetail.recommendationNote || '当前报告没有附加建议说明。' }}
          </p>
        </article>
      </div>

      <section class="glass-panel counselor-report-detail-page__notice-panel">
        <p class="section-kicker">声明</p>
        <h2>报告仅用于辅助评估</h2>
        <p class="body-text">
          {{
            reportDetail.noticeText ||
            '本结果仅用于心理状态辅助评估，不作为医学诊断依据。如有持续困扰，请联系专业老师或医疗机构。'
          }}
        </p>
      </section>

      <section class="glass-panel counselor-report-detail-page__resource-panel">
        <div class="section-head counselor-report-detail-page__inline-head">
          <div>
            <p class="section-kicker">推荐资源</p>
            <h2>配套内容</h2>
          </div>
          <span class="status-chip">{{ reportDetail.recommendedResources.length }} 项</span>
        </div>
        <div v-if="reportDetail.recommendedResources.length" class="resource-grid">
          <article v-for="resource in reportDetail.recommendedResources" :key="resource.resourceId" class="resource-card">
            <div class="resource-card__topline">
              <h3>{{ resource.title }}</h3>
              <span>{{ resource.categoryName }}</span>
            </div>
            <p class="body-text">{{ resource.summaryText }}</p>
          </article>
        </div>
        <p v-else class="body-text">当前没有推荐资源。</p>
      </section>

      <section class="counselor-report-detail-page__actions">
        <button
          class="ghost-button"
          type="button"
          @click="router.push({ name: 'counselor-student-reports', params: { studentUserId: reportDetail.studentUserId } })"
        >
          返回学生报告列表
        </button>
      </section>
    </template>
  </section>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700&family=Noto+Serif+SC:wght@400;500;600;700&display=swap');

.counselor-report-detail-page {
  min-height: 100%;
  color: #283128;
}

.counselor-report-detail-page__hero {
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) minmax(240px, 0.7fr);
  gap: 1.5rem;
  align-items: end;
}

.counselor-report-detail-page__copy {
  border-top: 1px solid rgba(59, 69, 59, 0.16);
  padding-top: 18px;
}

.counselor-report-detail-page__eyebrow,
.section-kicker,
.meta-grid dt,
.resource-card__topline span {
  margin: 0 0 10px;
  font: 700 0.76rem/1 'Manrope', sans-serif;
  letter-spacing: 0.22em;
  text-transform: uppercase;
  color: #7b6857;
}

.counselor-report-detail-page h1,
.section-head h2,
.resource-card h3 {
  margin: 0;
  font-family: 'Noto Serif SC', serif;
  font-weight: 600;
}

.counselor-report-detail-page h1 {
  font-size: clamp(2rem, 3vw, 3.3rem);
  line-height: 1.16;
}

.counselor-report-detail-page__lead,
.body-text,
.meta-grid dd,
.counselor-report-detail-page__status-panel,
.counselor-report-detail-page__alert {
  font-family: 'Manrope', sans-serif;
}

.counselor-report-detail-page__lead {
  margin: 18px 0 0;
  line-height: 1.84;
  color: rgba(40, 49, 40, 0.72);
}

.counselor-report-detail-page__metrics,
.counselor-report-detail-page__grid,
.resource-grid {
  display: grid;
  gap: 18px;
}

.metric-card,
.glass-panel,
.resource-card {
  border: 1px solid rgba(77, 86, 77, 0.14);
  background: rgba(255, 252, 247, 0.76);
  box-shadow: 0 24px 70px rgba(91, 80, 66, 0.08);
  backdrop-filter: blur(16px);
}

.metric-card {
  padding: 18px 20px;
}

.metric-card span,
.status-chip {
  display: block;
  margin-bottom: 8px;
  font: 700 0.78rem/1 'Manrope', sans-serif;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: rgba(68, 74, 66, 0.56);
}

.metric-card strong {
  font: 600 1.6rem/1 'Noto Serif SC', serif;
}

.counselor-report-detail-page__grid {
  grid-template-columns: minmax(0, 0.95fr) minmax(0, 1.05fr);
  margin-top: 1.5rem;
}

.glass-panel {
  padding: 24px;
}

.section-head {
  margin-bottom: 18px;
}

.body-text {
  margin: 0;
  font-size: 0.98rem;
  line-height: 1.9;
  color: rgba(40, 49, 40, 0.74);
}

.counselor-report-detail-page__recommend {
  margin-top: 14px;
  color: #846152;
}

.meta-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px 18px;
  margin: 18px 0 0;
}

.meta-grid dd {
  margin: 6px 0 0;
  font-size: 0.96rem;
  line-height: 1.8;
  color: rgba(40, 49, 40, 0.74);
}

.counselor-report-detail-page__notice-panel,
.counselor-report-detail-page__resource-panel,
.counselor-report-detail-page__actions {
  margin-top: 1.5rem;
}

.counselor-report-detail-page__inline-head {
  display: flex;
  justify-content: space-between;
  align-items: end;
  gap: 16px;
}

.resource-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-top: 18px;
}

.resource-card {
  padding: 18px;
}

.resource-card__topline {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: start;
  margin-bottom: 10px;
}

.resource-card h3 {
  font-size: 1.1rem;
  line-height: 1.45;
}

.ghost-button {
  border: 1px solid rgba(54, 65, 56, 0.2);
  background: rgba(255, 255, 255, 0.58);
  color: #283128;
  padding: 12px 16px;
  font: 700 0.82rem/1 'Manrope', sans-serif;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  cursor: pointer;
}

.counselor-report-detail-page__alert {
  color: #a44f46;
}

@media (max-width: 980px) {
  .counselor-report-detail-page__hero,
  .counselor-report-detail-page__grid,
  .resource-grid {
    grid-template-columns: 1fr;
  }

  .counselor-report-detail-page__inline-head,
  .resource-card__topline {
    flex-direction: column;
    align-items: start;
  }
}
</style>
