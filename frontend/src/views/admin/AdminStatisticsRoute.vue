<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { exportStatisticsApi, fetchAppointmentStatisticsApi, fetchAssessmentStatisticsApi, fetchOverviewStatisticsApi, fetchResourceStatisticsApi } from '@/api/admin-statistics'
import type { AppointmentStatistics, AssessmentStatistics, OverviewStatistics, ResourceStatistics, StatisticsExportQuery, StatisticsExportRow } from '@/api/types'
import { toErrorMessage } from '@/views/shared/page-logic'

const loading = ref(false)
const exporting = ref(false)
const errorMessage = ref('')
const overview = ref<OverviewStatistics | null>(null)
const assessments = ref<AssessmentStatistics | null>(null)
const resources = ref<ResourceStatistics | null>(null)
const appointments = ref<AppointmentStatistics | null>(null)
const exportRows = ref<StatisticsExportRow[]>([])

async function loadStatistics(): Promise<void> {
  loading.value = true
  errorMessage.value = ''

  try {
    const [overviewData, assessmentData, resourceData, appointmentData] = await Promise.all([
      fetchOverviewStatisticsApi(),
      fetchAssessmentStatisticsApi(),
      fetchResourceStatisticsApi(),
      fetchAppointmentStatisticsApi()
    ])
    overview.value = overviewData
    assessments.value = assessmentData
    resources.value = resourceData
    appointments.value = appointmentData
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    loading.value = false
  }
}

async function exportStatistics(query: StatisticsExportQuery): Promise<void> {
  exporting.value = true
  errorMessage.value = ''

  try {
    exportRows.value = await exportStatisticsApi(query)
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    exporting.value = false
  }
}

onMounted(() => {
  void loadStatistics()
})
</script>

<template>
  <section class="admin-stats-page">
    <div class="page-shell">
      <header class="page-hero">
        <div class="hero-copy">
          <p class="eyebrow">统计看板</p>
          <h1>在一张治理视图里观察学生、测评、预约与资源的总体运行状态。</h1>
          <p class="lead">总览、测评、资源和预约统计均为真实接口返回，导出结果会直接展示在页面底部。</p>
        </div>
      </header>

      <p v-if="errorMessage" class="error-text">{{ errorMessage }}</p>

      <div class="metric-grid">
        <article class="metric-card"><span>学生</span><strong>{{ overview?.studentCount ?? '-' }}</strong></article>
        <article class="metric-card"><span>咨询师</span><strong>{{ overview?.counselorCount ?? '-' }}</strong></article>
        <article class="metric-card"><span>报告</span><strong>{{ overview?.scaleReportCount ?? '-' }}</strong></article>
        <article class="metric-card"><span>AI 会话</span><strong>{{ overview?.aiSessionCount ?? '-' }}</strong></article>
        <article class="metric-card"><span>预约</span><strong>{{ overview?.appointmentCount ?? '-' }}</strong></article>
        <article class="metric-card"><span>资源</span><strong>{{ overview?.resourceCount ?? '-' }}</strong></article>
      </div>

      <div class="panel-grid">
        <section class="panel glass-panel">
          <div class="section-head"><p class="section-kicker">测评</p><h2>测评统计</h2></div>
          <p class="panel-copy">总报告 {{ assessments?.totalReports ?? 0 }} · 参与人数 {{ assessments?.participantCount ?? 0 }} · 平均分 {{ assessments?.averageScore ?? 0 }}</p>
          <div class="list-stack">
            <article v-for="scale in assessments?.scales ?? []" :key="scale.scaleId" class="list-card">
              <h3>{{ scale.scaleName }}</h3>
              <p>参与 {{ scale.participantCount }} · 报告 {{ scale.reportCount }} · 平均分 {{ scale.averageScore }}</p>
            </article>
          </div>
        </section>

        <section class="panel glass-panel">
          <div class="section-head"><p class="section-kicker">资源</p><h2>资源统计</h2></div>
          <p class="panel-copy">总量 {{ resources?.resourceCount ?? 0 }} · 已发布 {{ resources?.publishedCount ?? 0 }} · 浏览 {{ resources?.totalViews ?? 0 }}</p>
          <div class="list-stack">
            <article v-for="category in resources?.categories ?? []" :key="category.categoryId" class="list-card">
              <h3>{{ category.categoryName }}</h3>
              <p>资源 {{ category.resourceCount }} · 发布 {{ category.publishedCount }} · 浏览 {{ category.viewCount }}</p>
            </article>
          </div>
        </section>
      </div>

      <div class="panel-grid panel-grid--second">
        <section class="panel glass-panel">
          <div class="section-head"><p class="section-kicker">预约</p><h2>预约统计</h2></div>
          <p class="panel-copy">总量 {{ appointments?.totalCount ?? 0 }} · 待处理 {{ appointments?.pendingCount ?? 0 }} · 已接单 {{ appointments?.acceptedCount ?? 0 }}</p>
          <div class="list-stack">
            <article v-for="load in appointments?.counselorLoads ?? []" :key="load.counselorUserId" class="list-card">
              <h3>{{ load.counselorName }}</h3>
              <p>总量 {{ load.totalCount }} · 待处理 {{ load.pendingCount }} · 已接单 {{ load.acceptedCount }}</p>
            </article>
          </div>
        </section>

        <section class="panel glass-panel">
          <div class="section-head section-head-inline">
            <div><p class="section-kicker">导出</p><h2>导出视图</h2></div>
            <span class="status-chip">{{ exporting ? '导出中' : `${exportRows.length} 行` }}</span>
          </div>
          <div class="action-row">
            <button class="ghost-button" type="button" @click="exportStatistics({ dimension: 'college' })">导出学院维度</button>
            <button class="ghost-button" type="button" @click="exportStatistics({ dimension: 'grade' })">导出年级维度</button>
            <button class="ghost-button" type="button" @click="exportStatistics({ dimension: 'gender' })">导出性别维度</button>
          </div>
          <div class="list-stack">
            <article v-for="row in exportRows" :key="`${row.dimension}-${row.dimensionValue}`" class="list-card">
              <h3>{{ row.dimension }} / {{ row.dimensionValue }}</h3>
              <p>学生 {{ row.studentCount }} · 报告 {{ row.reportCount }} · AI 会话 {{ row.aiSessionCount }} · 预约 {{ row.appointmentCount }}</p>
            </article>
          </div>
        </section>
      </div>
    </div>
  </section>
</template>
<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700&family=Noto+Serif+SC:wght@400;500;600;700&display=swap');
.admin-stats-page{min-height:100vh;padding:44px 28px 72px;color:#272f27;background:linear-gradient(180deg,#f4efe6 0%,#f8f4ed 100%)}.page-shell{max-width:1320px;margin:0 auto}.page-hero{margin-bottom:28px}.hero-copy{border-top:1px solid rgba(59,69,59,.16);padding-top:18px}.eyebrow,.section-kicker{margin:0 0 10px;font:700 .76rem/1 'Manrope',sans-serif;letter-spacing:.22em;text-transform:uppercase;color:#7b6857}.hero-copy h1,.section-head h2,.list-card h3{margin:0;font-family:'Noto Serif SC',serif;font-weight:600}.hero-copy h1{font-size:clamp(2rem,3vw,3.3rem);line-height:1.16}.lead,.panel-copy,.list-card p,.error-text{font-family:'Manrope',sans-serif}.lead{margin:18px 0 0;max-width:760px;line-height:1.84;color:rgba(39,47,39,.72)}.error-text{margin-bottom:16px;color:#a44f46}.metric-grid,.panel-grid{display:grid;gap:18px}.metric-grid{grid-template-columns:repeat(3,minmax(0,1fr));margin-bottom:18px}.metric-card,.glass-panel,.list-card{border:1px solid rgba(77,86,77,.14);background:rgba(255,252,247,.76);box-shadow:0 24px 70px rgba(91,80,66,.08);backdrop-filter:blur(16px)}.metric-card{padding:18px 20px}.metric-card span,.status-chip{display:block;margin-bottom:8px;font:700 .78rem/1 'Manrope',sans-serif;letter-spacing:.16em;text-transform:uppercase;color:rgba(68,74,66,.56)}.metric-card strong{font:600 1.6rem/1 'Noto Serif SC',serif}.panel-grid{grid-template-columns:repeat(2,minmax(0,1fr))}.panel{padding:24px}.section-head{margin-bottom:18px}.section-head-inline{display:flex;justify-content:space-between;align-items:end;gap:16px}.panel-copy{margin:0 0 16px;font-size:.95rem;line-height:1.8;color:rgba(39,47,39,.7)}.list-stack{display:grid;gap:14px}.list-card{padding:16px}.list-card h3{font-size:1.08rem;line-height:1.45}.list-card p{margin:8px 0 0;font-size:.9rem;line-height:1.75;color:rgba(39,47,39,.68)}.action-row{display:flex;flex-wrap:wrap;gap:12px;margin-bottom:16px}.ghost-button{border:1px solid rgba(54,65,56,.2);background:rgba(255,255,255,.58);padding:12px 16px;font:700 .82rem/1 'Manrope',sans-serif;letter-spacing:.08em;text-transform:uppercase;cursor:pointer}.status-chip{border:1px solid rgba(88,93,84,.14);background:rgba(255,250,240,.82);padding:9px 14px;color:#696152}
@media (max-width:980px){.admin-stats-page{padding:28px 16px 46px}.metric-grid,.panel-grid{grid-template-columns:1fr}}
</style>

