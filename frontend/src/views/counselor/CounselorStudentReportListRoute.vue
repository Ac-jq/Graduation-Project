<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchCounselorStudentReportsApi } from '@/api/assessment'
import type { ReportSummary } from '@/api/types'
import { toErrorMessage, toNumberParam } from '@/views/shared/page-logic'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const errorMessage = ref('')
const reports = ref<ReportSummary[]>([])
const studentUserId = computed(() => toNumberParam(route.params.studentUserId))

async function loadReports(): Promise<void> {
  if (!studentUserId.value) {
    errorMessage.value = 'Invalid studentUserId'
    reports.value = []
    return
  }

  loading.value = true
  errorMessage.value = ''

  try {
    reports.value = await fetchCounselorStudentReportsApi(studentUserId.value)
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    loading.value = false
  }
}

async function openReport(reportId: number): Promise<void> {
  if (!studentUserId.value) {
    return
  }

  await router.push({ name: 'counselor-student-report-detail', params: { studentUserId: studentUserId.value, reportId } })
}

watch(() => route.params.studentUserId, () => {
  void loadReports()
})

onMounted(() => {
  void loadReports()
})
</script>

<template>
  <section class="c-report-list-page">
    <div class="page-shell">
      <header class="page-hero">
        <div class="hero-copy">
          <p class="eyebrow">学生报告</p>
          <h1>围绕单个学生阅读全部测评报告，用更连贯的方式判断风险与支持建议。</h1>
          <p class="lead">当前查看的是学生 #{{ studentUserId || '-' }} 的历史报告列表。</p>
        </div>
        <div class="hero-metric">
          <span>报告总数</span>
          <strong>{{ reports.length }}</strong>
        </div>
      </header>

      <p v-if="loading" class="state-text">正在同步学生报告...</p>
      <p v-else-if="errorMessage" class="error-text">{{ errorMessage }}</p>
      <p v-else-if="!reports.length" class="state-text">当前学生暂无测评报告。</p>

      <div v-else class="report-stack">
        <article v-for="report in reports" :key="report.reportId" class="report-card" @click="openReport(report.reportId)">
          <div class="report-card__topline">
            <div>
              <p class="report-code">报告 #{{ report.reportId }}</p>
              <h2>{{ report.scaleName }}</h2>
            </div>
            <span class="level-pill">{{ report.levelCode }}</span>
          </div>
          <p class="report-summary">{{ report.summaryText }}</p>
          <div class="report-meta">
            <span>总分 {{ report.totalScore }}</span>
            <span>{{ new Date(report.createdAt).toLocaleString('zh-CN') }}</span>
          </div>
        </article>
      </div>
    </div>
  </section>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700&family=Noto+Serif+SC:wght@400;500;600;700&display=swap');

.c-report-list-page{min-height:100vh;padding:44px 28px 72px;color:#283128;background:linear-gradient(180deg,#f5f0e5 0%,#f8f4ed 100%)}
.page-shell{max-width:1240px;margin:0 auto}
.page-hero{display:grid;grid-template-columns:minmax(0,1.35fr) 220px;gap:28px;align-items:end;margin-bottom:30px}.hero-copy{border-top:1px solid rgba(59,69,59,.16);padding-top:18px}
.eyebrow,.report-code{margin:0 0 10px;font:700 .76rem/1 'Manrope',sans-serif;letter-spacing:.22em;text-transform:uppercase;color:#7b6857}
.hero-copy h1,.report-card h2{margin:0;font-family:'Noto Serif SC',serif;font-weight:600}.hero-copy h1{font-size:clamp(2rem,3vw,3.2rem);line-height:1.16}.lead,.report-summary,.report-meta,.state-text,.error-text{font-family:'Manrope',sans-serif}.lead{margin:18px 0 0;line-height:1.84;color:rgba(40,49,40,.72)}
.hero-metric,.report-card{border:1px solid rgba(77,86,77,.14);background:rgba(255,252,247,.76);box-shadow:0 24px 70px rgba(91,80,66,.08);backdrop-filter:blur(16px)}.hero-metric{padding:18px 20px}.hero-metric span{display:block;margin-bottom:8px;font:700 .78rem/1 'Manrope',sans-serif;letter-spacing:.16em;text-transform:uppercase;color:rgba(68,74,66,.56)}.hero-metric strong{font:600 1.6rem/1 'Noto Serif SC',serif}
.report-stack{display:grid;gap:18px}.report-card{padding:22px;cursor:pointer;transition:transform .28s ease, box-shadow .28s ease}.report-card:hover{transform:translateY(-3px);box-shadow:0 28px 54px rgba(86,106,92,.12)}
.report-card__topline{display:flex;justify-content:space-between;gap:16px;align-items:start}.report-card h2{font-size:1.34rem;line-height:1.35}.level-pill{border:1px solid rgba(97,111,98,.15);background:rgba(242,244,237,.94);padding:8px 12px;font:700 .74rem/1 'Manrope',sans-serif;letter-spacing:.12em;text-transform:uppercase;color:#66735f}.report-summary{margin:14px 0 0;font-size:.96rem;line-height:1.86;color:rgba(40,49,40,.7)}.report-meta{display:flex;flex-wrap:wrap;gap:10px 18px;margin-top:14px;font-size:.84rem;color:rgba(40,49,40,.58)}.error-text{color:#a44f46}
@media (max-width:900px){.c-report-list-page{padding:28px 16px 46px}.page-hero{grid-template-columns:1fr}.report-card__topline{flex-direction:column;align-items:start}}
</style>

