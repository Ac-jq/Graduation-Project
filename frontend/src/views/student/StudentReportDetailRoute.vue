<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
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

function formatDate(value: string): string {
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value))
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
  <main class="student-report-detail-page">
    <section class="student-report-detail-page__hero">
      <div class="student-report-detail-page__copy">
        <p class="student-report-detail-page__eyebrow">Assessment Report</p>
        <h1>报告详情</h1>
        <p class="student-report-detail-page__lead">
          这里展示量表得分、风险等级、辅助解读与推荐资源。内容用于帮助你理解当前状态，不作为医学诊断依据。
        </p>
      </div>

      <aside v-if="reportDetail" class="student-report-detail-page__overview">
        <p class="student-report-detail-page__meta-label">报告快照</p>
        <dl>
          <div>
            <dt>报告编号</dt>
            <dd>#{{ reportDetail.reportId }}</dd>
          </div>
          <div>
            <dt>总分</dt>
            <dd>{{ reportDetail.totalScore }}</dd>
          </div>
          <div>
            <dt>等级</dt>
            <dd>{{ resolveLevelLabel(reportDetail.levelCode) }}</dd>
          </div>
          <div>
            <dt>生成时间</dt>
            <dd>{{ formatDate(reportDetail.createdAt) }}</dd>
          </div>
        </dl>
      </aside>
    </section>

    <p v-if="errorMessage" class="student-report-detail-page__alert">{{ errorMessage }}</p>

    <section v-if="loading" class="student-report-detail-page__status-panel">
      <p>正在加载报告详情...</p>
    </section>

    <template v-else-if="reportDetail">
      <section class="student-report-detail-page__headline-card">
        <div>
          <p class="student-report-detail-page__meta-label">量表信息</p>
          <h2>{{ reportDetail.scaleName }}</h2>
          <p>{{ reportDetail.summaryText }}</p>
        </div>
        <div class="student-report-detail-page__headline-meta">
          <div>
            <span>学生姓名</span>
            <strong>{{ reportDetail.studentName || '匿名学生' }}</strong>
          </div>
          <div>
            <span>学号</span>
            <strong>{{ reportDetail.studentNo || '未提供' }}</strong>
          </div>
          <div>
            <span>咨询建议</span>
            <strong>{{ reportDetail.recommendAppointment ? '建议预约咨询师' : '可先自助观察与调节' }}</strong>
          </div>
        </div>
      </section>

      <section class="student-report-detail-page__body-grid">
        <article class="student-report-detail-page__panel">
          <p class="student-report-detail-page__meta-label">AI 辅助解释</p>
          <h3>如何理解本次结果</h3>
          <p>{{ reportDetail.aiInterpretation || '当前报告暂无 AI 辅助解释。' }}</p>
        </article>

        <article class="student-report-detail-page__panel">
          <p class="student-report-detail-page__meta-label">后续建议</p>
          <h3>建议行动</h3>
          <p>{{ reportDetail.recommendationNote || '当前报告暂无额外建议。' }}</p>
          <button
            v-if="reportDetail.recommendAppointment"
            class="student-report-detail-page__primary"
            type="button"
            @click="router.push({ name: 'student-appointment-slots' })"
          >
            预约人工咨询
          </button>
        </article>
      </section>

      <section class="student-report-detail-page__notice-panel">
        <p class="student-report-detail-page__meta-label">重要声明</p>
        <h3>结果仅用于辅助评估</h3>
        <p>
          {{
            reportDetail.noticeText ||
            '本结果仅用于心理状态辅助评估，不作为医学诊断依据。如有持续困扰，请联系专业老师或医疗机构。'
          }}
        </p>
      </section>

      <section class="student-report-detail-page__resource-section">
        <div class="student-report-detail-page__section-head">
          <div>
            <p class="student-report-detail-page__meta-label">推荐资源</p>
            <h3>与你当前状态更相关的内容</h3>
          </div>
          <strong>{{ reportDetail.recommendedResources.length }} 项</strong>
        </div>

        <div v-if="reportDetail.recommendedResources.length" class="student-report-detail-page__resource-grid">
          <article
            v-for="resource in reportDetail.recommendedResources"
            :key="resource.resourceId"
            class="resource-card"
            @click="router.push({ name: 'student-resource-detail', params: { resourceId: resource.resourceId } })"
          >
            <div class="resource-card__header">
              <p class="resource-card__type">{{ resource.resourceType }}</p>
              <p class="resource-card__metric">{{ resource.viewCount }} 次浏览</p>
            </div>
            <h4>{{ resource.title }}</h4>
            <p>{{ resource.summaryText }}</p>
            <div class="resource-card__footer">
              <span>{{ resource.categoryName }}</span>
              <span>{{ resource.favoriteCount }} 次收藏</span>
            </div>
          </article>
        </div>

        <div v-else class="student-report-detail-page__status-panel">
          <p>当前报告暂未匹配到推荐资源。</p>
        </div>
      </section>

      <section class="student-report-detail-page__footer-actions">
        <button class="student-report-detail-page__ghost" type="button" @click="router.push({ name: 'student-reports' })">
          返回报告列表
        </button>
        <button class="student-report-detail-page__ghost" type="button" @click="router.push({ name: 'student-scales' })">
          返回量表目录
        </button>
      </section>
    </template>

    <section v-else class="student-report-detail-page__status-panel">
      <p>没有找到对应报告，请返回报告列表重新选择。</p>
      <button class="student-report-detail-page__ghost" type="button" @click="router.push({ name: 'student-reports' })">
        返回报告列表
      </button>
    </section>
  </main>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700&family=Noto+Serif+SC:wght@400;500;600;700&display=swap');

.student-report-detail-page {
  --ink: #201c18;
  --muted: #6e665f;
  --line: rgba(32, 28, 24, 0.12);
  --glass: rgba(255, 251, 245, 0.72);
  min-height: 100%;
  color: var(--ink);
}

.student-report-detail-page__hero {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(300px, 0.85fr);
  gap: 1.5rem;
  align-items: end;
}

.student-report-detail-page__eyebrow,
.student-report-detail-page__meta-label,
.student-report-detail-page__overview dt,
.student-report-detail-page__headline-meta span,
.resource-card__type,
.resource-card__metric,
.resource-card__footer span {
  margin: 0;
  font: 700 0.72rem/1.4 'Manrope', sans-serif;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--muted);
}

.student-report-detail-page h1,
.student-report-detail-page h2,
.student-report-detail-page h3,
.resource-card h4 {
  margin: 0;
  font-family: 'Noto Serif SC', serif;
  font-weight: 600;
}

.student-report-detail-page h1 {
  margin-top: 0.95rem;
  font-size: clamp(2.6rem, 5vw, 4.8rem);
  line-height: 1.02;
}

.student-report-detail-page__lead,
.student-report-detail-page__overview dd,
.student-report-detail-page__headline-card p:last-child,
.student-report-detail-page__panel p:last-child,
.student-report-detail-page__notice-panel p,
.resource-card p,
.student-report-detail-page__status-panel p {
  font-family: 'Manrope', sans-serif;
  line-height: 1.88;
}

.student-report-detail-page__lead {
  max-width: 46rem;
  margin: 1rem 0 0;
  color: var(--muted);
}

.student-report-detail-page__overview,
.student-report-detail-page__headline-card,
.student-report-detail-page__panel,
.resource-card,
.student-report-detail-page__notice-panel,
.student-report-detail-page__status-panel,
.student-report-detail-page__footer-actions {
  border: 1px solid var(--line);
  background: var(--glass);
  backdrop-filter: blur(18px);
  box-shadow: 0 22px 48px rgba(80, 70, 58, 0.08);
}

.student-report-detail-page__overview,
.student-report-detail-page__headline-card,
.student-report-detail-page__panel,
.student-report-detail-page__notice-panel,
.student-report-detail-page__status-panel,
.student-report-detail-page__footer-actions {
  padding: 1.35rem;
}

.student-report-detail-page__overview dl {
  display: grid;
  gap: 0.95rem;
  margin: 1rem 0 0;
}

.student-report-detail-page__overview dd {
  margin: 0.35rem 0 0;
}

.student-report-detail-page__alert {
  margin-top: 1rem;
  color: #8d4747;
  font-weight: 600;
}

.student-report-detail-page__headline-card {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(260px, 0.7fr);
  gap: 1.4rem;
  margin-top: 1.5rem;
}

.student-report-detail-page__headline-card h2,
.student-report-detail-page__section-head h3,
.student-report-detail-page__panel h3,
.student-report-detail-page__notice-panel h3 {
  margin-top: 0.75rem;
  font-size: 1.9rem;
  line-height: 1.24;
}

.student-report-detail-page__headline-card p:last-child,
.student-report-detail-page__panel p:last-child {
  margin: 0.9rem 0 0;
  color: var(--muted);
}

.student-report-detail-page__headline-meta {
  display: grid;
  gap: 1rem;
}

.student-report-detail-page__headline-meta strong,
.student-report-detail-page__section-head strong {
  display: block;
  margin-top: 0.35rem;
  font: 600 1.04rem/1.45 'Noto Serif SC', serif;
}

.student-report-detail-page__body-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1.2rem;
  margin-top: 1.5rem;
}

.student-report-detail-page__panel {
  display: grid;
  gap: 0.9rem;
}

.student-report-detail-page__primary,
.student-report-detail-page__ghost {
  min-height: 3rem;
  padding: 0 1.15rem;
  font: 700 0.8rem/1 'Manrope', sans-serif;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  cursor: pointer;
}

.student-report-detail-page__primary {
  justify-self: start;
  border: none;
  background: linear-gradient(135deg, #6b8473, #4f6656);
  color: #faf6f0;
}

.student-report-detail-page__ghost {
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.5);
  color: var(--ink);
}

.student-report-detail-page__notice-panel {
  display: grid;
  gap: 0.8rem;
  margin-top: 1.5rem;
}

.student-report-detail-page__notice-panel p:last-child {
  margin: 0;
  color: #7b5648;
}

.student-report-detail-page__resource-section {
  margin-top: 1.5rem;
}

.student-report-detail-page__section-head {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: end;
  padding-bottom: 1rem;
  border-bottom: 1px solid var(--line);
}

.student-report-detail-page__resource-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 1rem;
  margin-top: 1.2rem;
}

.resource-card {
  display: grid;
  gap: 0.85rem;
  padding: 1.15rem;
  cursor: pointer;
}

.resource-card__header,
.resource-card__footer,
.student-report-detail-page__footer-actions {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
}

.resource-card h4 {
  font-size: 1.28rem;
  line-height: 1.35;
}

.resource-card p {
  margin: 0;
  color: var(--muted);
}

.student-report-detail-page__status-panel,
.student-report-detail-page__footer-actions {
  margin-top: 1.5rem;
}

@media (max-width: 980px) {
  .student-report-detail-page__hero,
  .student-report-detail-page__headline-card,
  .student-report-detail-page__body-grid,
  .student-report-detail-page__resource-grid {
    grid-template-columns: 1fr;
  }

  .student-report-detail-page__section-head,
  .resource-card__header,
  .resource-card__footer,
  .student-report-detail-page__footer-actions {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
