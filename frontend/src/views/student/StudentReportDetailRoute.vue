<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchStudentReportDetailApi } from '@/api/assessment'
import type { ReportDetail } from '@/api/types'
import { toErrorMessage, toNumberParam } from '@/views/shared/page-logic'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const errorMessage = ref('')
const reportDetail = ref<ReportDetail | null>(null)
const reportId = computed(() => toNumberParam(route.params.reportId))

const recommendedResourceCount = computed(() => reportDetail.value?.recommendedResources.length ?? 0)
const riskToneLabel = computed(() => {
  switch (reportDetail.value?.levelCode) {
    case 'LOW':
      return '低风险'
    case 'MEDIUM':
      return '中风险'
    case 'HIGH':
      return '高风险'
    default:
      return reportDetail.value?.levelCode ?? '未评定'
  }
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

async function loadReportDetail(): Promise<void> {
  if (!reportId.value) {
    errorMessage.value = '无效的报告编号'
    reportDetail.value = null
    return
  }

  loading.value = true
  errorMessage.value = ''

  try {
    reportDetail.value = await fetchStudentReportDetailApi(reportId.value)
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    loading.value = false
  }
}

async function jumpToRecommendedResource(resourceId: number): Promise<void> {
  await router.push({ name: 'student-resource-detail', params: { resourceId } })
}

async function jumpToAppointment(): Promise<void> {
  await router.push({ name: 'student-appointment-slots' })
}

async function jumpToReportArchive(): Promise<void> {
  await router.push({ name: 'student-reports' })
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
  <main class="report-detail-page">
    <section class="report-detail-page__masthead">
      <div class="report-detail-page__heading">
        <p class="report-detail-page__eyebrow">测评解读</p>
        <h1 class="report-detail-page__title">报告详情</h1>
        <p class="report-detail-page__summary">
          这里汇总本次测评的分数、风险等级、AI 解读与后续建议，帮助你决定下一步是继续自助梳理还是进入人工支持。
        </p>
      </div>

      <aside class="report-detail-page__overview" v-if="reportDetail">
        <p class="report-detail-page__label">报告快照</p>
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
            <dt>风险等级</dt>
            <dd>{{ riskToneLabel }}</dd>
          </div>
          <div>
            <dt>生成时间</dt>
            <dd>{{ formatDate(reportDetail.createdAt) }}</dd>
          </div>
        </dl>
      </aside>
    </section>

    <p v-if="errorMessage" class="report-detail-page__alert">{{ errorMessage }}</p>

    <section v-if="loading" class="report-detail-page__status-panel">
      <p>正在加载报告详情...</p>
    </section>

    <template v-else-if="reportDetail">
      <section class="report-detail-page__headline-card">
        <div>
          <p class="report-detail-page__label">量表</p>
          <h2>{{ reportDetail.scaleName }}</h2>
          <p>{{ reportDetail.summaryText }}</p>
        </div>

        <div class="report-detail-page__headline-meta">
          <div>
            <span>学生姓名</span>
            <strong>{{ reportDetail.studentName || '匿名学生' }}</strong>
          </div>
          <div>
            <span>学号</span>
            <strong>{{ reportDetail.studentNo || '未提供' }}</strong>
          </div>
          <div>
            <span>建议</span>
            <strong>{{ reportDetail.recommendAppointment ? '建议预约咨询师' : '先继续自助观察' }}</strong>
          </div>
        </div>
      </section>

      <section class="report-detail-page__body-grid">
        <article class="report-detail-page__panel">
          <p class="report-detail-page__label">AI 解读</p>
          <h3>AI 解读摘要</h3>
          <p>{{ reportDetail.aiInterpretation || '当前报告暂未生成更详细的 AI 解读。' }}</p>
        </article>

        <article class="report-detail-page__panel">
          <p class="report-detail-page__label">后续建议</p>
          <h3>建议行动</h3>
          <p>{{ reportDetail.recommendationNote || '当前报告暂无额外建议。' }}</p>
          <button
            v-if="reportDetail.recommendAppointment"
            class="report-detail-page__primary"
            type="button"
            @click="jumpToAppointment"
          >
            立即预约人工咨询
          </button>
        </article>
      </section>

      <section class="report-detail-page__resource-section">
        <div class="report-detail-page__section-head">
          <div>
            <p class="report-detail-page__label">推荐资源</p>
            <h3>匹配资源</h3>
          </div>
          <strong>{{ recommendedResourceCount }} 项</strong>
        </div>

        <div v-if="reportDetail.recommendedResources.length" class="report-detail-page__resource-grid">
          <article
            v-for="resource in reportDetail.recommendedResources"
            :key="resource.resourceId"
            class="resource-card"
            @click="jumpToRecommendedResource(resource.resourceId)"
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

        <div v-else class="report-detail-page__status-panel">
          <p>当前报告暂未匹配到推荐资源。</p>
        </div>
      </section>

      <section class="report-detail-page__footer-actions">
        <button class="report-detail-page__ghost" type="button" @click="jumpToReportArchive">返回报告列表</button>
        <button class="report-detail-page__ghost" type="button" @click="jumpToAppointment">前往预约时段</button>
      </section>
    </template>

    <section v-else class="report-detail-page__status-panel">
      <p>未找到对应报告，请返回报告列表重新选择。</p>
      <button class="report-detail-page__ghost" type="button" @click="jumpToReportArchive">返回报告列表</button>
    </section>
  </main>
</template>
<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700&family=Noto+Serif+SC:wght@400;500;600;700&display=swap');

.report-detail-page {
  --paper: #f5efe5;
  --ink: #201c18;
  --muted: #6e665f;
  --line: rgba(32, 28, 24, 0.12);
  --glass: rgba(255, 251, 245, 0.7);
  --accent: #667f6f;
  --danger: #8d4747;
  min-height: 100vh;
  padding: 2rem;
  color: var(--ink);
  background:
    radial-gradient(circle at right top, rgba(114, 136, 121, 0.18), transparent 26%),
    radial-gradient(circle at left center, rgba(197, 187, 169, 0.2), transparent 32%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.16), transparent 38%),
    var(--paper);
}

.report-detail-page__masthead {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(300px, 0.85fr);
  gap: 1.5rem;
  align-items: end;
  padding-bottom: 1.4rem;
  border-bottom: 1px solid var(--line);
}

.report-detail-page__eyebrow,
.report-detail-page__label,
.report-detail-page__overview dt,
.report-detail-page__headline-meta span,
.resource-card__type,
.resource-card__metric,
.resource-card__footer span {
  margin: 0;
  font: 600 0.72rem/1.4 'Manrope', sans-serif;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--muted);
}

.report-detail-page__title {
  margin: 0.95rem 0 0;
  font: 600 clamp(2.8rem, 5vw, 5.1rem)/0.98 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.report-detail-page__summary {
  max-width: 46rem;
  margin: 1rem 0 0;
  color: var(--muted);
  font: 400 1rem/1.9 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.report-detail-page__overview,
.report-detail-page__headline-card,
.report-detail-page__panel,
.resource-card,
.report-detail-page__status-panel,
.report-detail-page__footer-actions {
  border: 1px solid var(--line);
  background: var(--glass);
  backdrop-filter: blur(18px);
  box-shadow: 0 22px 48px rgba(80, 70, 58, 0.08);
}

.report-detail-page__overview {
  padding: 1.2rem;
}

.report-detail-page__overview dl {
  display: grid;
  gap: 0.95rem;
  margin: 1rem 0 0;
}

.report-detail-page__overview dd {
  margin: 0.35rem 0 0;
  font: 600 1.06rem/1.45 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.report-detail-page__alert {
  margin: 1.25rem 0 0;
  color: var(--danger);
  font: 600 0.9rem/1.6 'Manrope', sans-serif;
}

.report-detail-page__headline-card {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(260px, 0.7fr);
  gap: 1.4rem;
  margin-top: 1.5rem;
  padding: 1.4rem;
}

.report-detail-page__headline-card h2,
.report-detail-page__section-head h3,
.report-detail-page__panel h3 {
  margin: 0.75rem 0 0;
  font: 600 1.9rem/1.24 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.report-detail-page__headline-card p:last-child,
.report-detail-page__panel p:last-child {
  margin: 0.9rem 0 0;
  color: var(--muted);
  font: 400 0.98rem/1.9 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.report-detail-page__headline-meta {
  display: grid;
  gap: 1rem;
}

.report-detail-page__headline-meta strong,
.report-detail-page__section-head strong {
  display: block;
  margin-top: 0.35rem;
  font: 600 1.04rem/1.45 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.report-detail-page__body-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1.2rem;
  margin-top: 1.5rem;
}

.report-detail-page__panel {
  display: grid;
  gap: 0.9rem;
  padding: 1.35rem;
}

.report-detail-page__primary,
.report-detail-page__ghost {
  min-height: 3rem;
  padding: 0 1.15rem;
  font: 600 0.84rem/1 'Manrope', sans-serif;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  cursor: pointer;
  transition: transform 180ms ease, box-shadow 180ms ease, border-color 180ms ease;
}

.report-detail-page__primary {
  justify-self: start;
  border: none;
  background: linear-gradient(135deg, #6b8473, #4f6656);
  color: #faf6f0;
  box-shadow: 0 18px 36px rgba(79, 102, 86, 0.24);
}

.report-detail-page__ghost {
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.5);
  color: var(--ink);
}

.report-detail-page__primary:hover,
.report-detail-page__ghost:hover,
.resource-card:hover {
  transform: translateY(-2px);
}

.report-detail-page__resource-section {
  margin-top: 1.5rem;
}

.report-detail-page__section-head {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: end;
  padding-bottom: 1rem;
  border-bottom: 1px solid var(--line);
}

.report-detail-page__resource-grid {
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
  transition: transform 180ms ease, border-color 180ms ease, box-shadow 180ms ease;
}

.resource-card:hover {
  border-color: rgba(102, 127, 111, 0.38);
  box-shadow: 0 24px 44px rgba(80, 70, 58, 0.12);
}

.resource-card__header,
.resource-card__footer,
.report-detail-page__footer-actions {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
}

.resource-card h4 {
  margin: 0;
  font: 600 1.28rem/1.35 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.resource-card p {
  margin: 0;
  color: var(--muted);
  font: 400 0.95rem/1.82 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.report-detail-page__status-panel,
.report-detail-page__footer-actions {
  margin-top: 1.5rem;
  padding: 1.3rem;
}

.report-detail-page__status-panel p {
  margin: 0;
  color: var(--muted);
  font: 400 0.98rem/1.9 'Noto Serif SC', 'Source Han Serif SC', serif;
}

@media (max-width: 980px) {
  .report-detail-page,
  .report-list-page {
    padding: 1rem;
  }

  .report-detail-page__masthead,
  .report-detail-page__headline-card,
  .report-detail-page__body-grid,
  .report-detail-page__resource-grid,
  .report-detail-page__footer-actions {
    grid-template-columns: 1fr;
    flex-direction: column;
    align-items: stretch;
  }

  .report-detail-page__section-head,
  .resource-card__header,
  .resource-card__footer {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>

