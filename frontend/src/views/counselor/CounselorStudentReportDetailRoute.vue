<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { fetchCounselorStudentReportDetailApi } from '@/api/assessment'
import type { ReportDetail } from '@/api/types'
import { toErrorMessage, toNumberParam } from '@/views/shared/page-logic'

const route = useRoute()
const loading = ref(false)
const errorMessage = ref('')
const reportDetail = ref<ReportDetail | null>(null)
const studentUserId = computed(() => toNumberParam(route.params.studentUserId))
const reportId = computed(() => toNumberParam(route.params.reportId))

async function loadReportDetail(): Promise<void> {
  if (!studentUserId.value || !reportId.value) {
    errorMessage.value = 'Invalid route params'
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

watch(() => [route.params.studentUserId, route.params.reportId], () => {
  void loadReportDetail()
})

onMounted(() => {
  void loadReportDetail()
})
</script>

<template>
  <section class="c-report-detail-page">
    <div class="page-shell">
      <p v-if="loading" class="state-text">正在读取报告详情...</p>
      <p v-else-if="errorMessage" class="error-text">{{ errorMessage }}</p>

      <template v-else-if="reportDetail">
        <header class="detail-hero">
          <div class="hero-copy">
            <p class="eyebrow">报告详情</p>
            <h1>{{ reportDetail.scaleName }}</h1>
            <p class="lead">学生 {{ reportDetail.studentName || `#${reportDetail.studentUserId}` }} 的单份报告详情，包含 AI 解读与推荐内容。</p>
          </div>
          <div class="hero-metrics">
            <div class="metric-card">
              <span>等级</span>
              <strong>{{ reportDetail.levelCode }}</strong>
            </div>
            <div class="metric-card">
              <span>Score</span>
              <strong>{{ reportDetail.totalScore }}</strong>
            </div>
          </div>
        </header>

        <div class="detail-grid">
          <article class="summary-panel glass-panel">
            <div class="section-head">
              <p class="section-kicker">Summary</p>
              <h2>报告摘要</h2>
            </div>
            <p class="summary-text">{{ reportDetail.summaryText }}</p>
            <dl class="meta-grid">
              <div><dt>报告编号</dt><dd>#{{ reportDetail.reportId }}</dd></div>
              <div><dt>学生学号</dt><dd>{{ reportDetail.studentNo || '-' }}</dd></div>
              <div><dt>创建时间</dt><dd>{{ new Date(reportDetail.createdAt).toLocaleString('zh-CN') }}</dd></div>
              <div><dt>建议预约</dt><dd>{{ reportDetail.recommendAppointment ? '是' : '否' }}</dd></div>
            </dl>
          </article>

          <article class="interpret-panel glass-panel">
            <div class="section-head">
              <p class="section-kicker">AI 解读</p>
              <h2>AI 解读</h2>
            </div>
            <p class="interpret-text">{{ reportDetail.aiInterpretation || '当前报告没有额外 AI 解读。' }}</p>
            <p class="recommend-note">{{ reportDetail.recommendationNote || '当前报告没有附加建议说明。' }}</p>
          </article>
        </div>

        <section class="resource-panel glass-panel">
          <div class="section-head section-head-inline">
            <div>
              <p class="section-kicker">推荐资源</p>
              <h2>推荐资源</h2>
            </div>
            <span class="status-chip">{{ reportDetail.recommendedResources.length }} 条</span>
          </div>
          <div v-if="reportDetail.recommendedResources.length" class="resource-grid">
            <article v-for="resource in reportDetail.recommendedResources" :key="resource.resourceId" class="resource-card">
              <div class="resource-topline">
                <h3>{{ resource.title }}</h3>
                <span>{{ resource.categoryName }}</span>
              </div>
              <p>{{ resource.summaryText }}</p>
            </article>
          </div>
          <p v-else class="state-text">当前没有推荐资源。</p>
        </section>
      </template>
    </div>
  </section>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700&family=Noto+Serif+SC:wght@400;500;600;700&display=swap');

.c-report-detail-page{min-height:100vh;padding:44px 28px 72px;color:#283128;background:linear-gradient(180deg,#f5f0e5 0%,#f8f4ed 100%)}
.page-shell{max-width:1320px;margin:0 auto}.detail-hero{display:grid;grid-template-columns:minmax(0,1.3fr) minmax(240px,.7fr);gap:28px;align-items:end;margin-bottom:30px}.hero-copy{border-top:1px solid rgba(59,69,59,.16);padding-top:18px}.eyebrow,.section-kicker,.meta-grid dt,.resource-topline span{margin:0 0 10px;font:700 .76rem/1 'Manrope',sans-serif;letter-spacing:.22em;text-transform:uppercase;color:#7b6857}.hero-copy h1,.section-head h2,.resource-card h3{margin:0;font-family:'Noto Serif SC',serif;font-weight:600}.hero-copy h1{font-size:clamp(2rem,3vw,3.3rem);line-height:1.16}.lead,.summary-text,.interpret-text,.recommend-note,.state-text,.error-text,.meta-grid dd,.resource-card p{font-family:'Manrope',sans-serif}.lead{margin:18px 0 0;line-height:1.84;color:rgba(40,49,40,.72)}
.hero-metrics,.detail-grid,.resource-grid{display:grid;gap:18px}.metric-card,.glass-panel,.resource-card{border:1px solid rgba(77,86,77,.14);background:rgba(255,252,247,.76);box-shadow:0 24px 70px rgba(91,80,66,.08);backdrop-filter:blur(16px)}.metric-card{padding:18px 20px}.metric-card span,.status-chip{display:block;margin-bottom:8px;font:700 .78rem/1 'Manrope',sans-serif;letter-spacing:.16em;text-transform:uppercase;color:rgba(68,74,66,.56)}.metric-card strong{font:600 1.6rem/1 'Noto Serif SC',serif}
.detail-grid{grid-template-columns:minmax(0,.9fr) minmax(0,1.1fr);margin-bottom:24px}.summary-panel,.interpret-panel,.resource-panel{padding:24px}.section-head{margin-bottom:18px}.section-head-inline{display:flex;justify-content:space-between;align-items:end;gap:16px}.summary-text,.interpret-text,.recommend-note,.resource-card p{margin:0;font-size:.98rem;line-height:1.9;color:rgba(40,49,40,.74)}.recommend-note{margin-top:14px;color:#846152}.meta-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:14px 18px;margin:18px 0 0}.meta-grid dd{margin:6px 0 0;font-size:.96rem;line-height:1.8;color:rgba(40,49,40,.74)}.status-chip{border:1px solid rgba(88,93,84,.14);background:rgba(255,250,240,.82);padding:9px 14px;color:#696152}.resource-grid{grid-template-columns:repeat(2,minmax(0,1fr))}.resource-card{padding:18px}.resource-topline{display:flex;justify-content:space-between;gap:12px;align-items:start;margin-bottom:10px}.resource-card h3{font-size:1.1rem;line-height:1.45}.error-text{color:#a44f46}
@media (max-width:980px){.c-report-detail-page{padding:28px 16px 46px}.detail-hero,.detail-grid,.resource-grid{grid-template-columns:1fr}.resource-topline,.section-head-inline{flex-direction:column;align-items:start}}
</style>

