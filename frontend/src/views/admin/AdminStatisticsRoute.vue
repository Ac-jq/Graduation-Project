<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import {
  exportInterventionEffectReportApi,
  fetchAppointmentStatisticsApi,
  fetchAssessmentStatisticsApi,
  fetchOverviewStatisticsApi,
  fetchResourceStatisticsApi,
  fetchUserEngagementStatisticsApi
} from '@/api/admin-statistics'
import type {
  AppointmentStatistics,
  AssessmentStatistics,
  OverviewStatistics,
  ResourceStatistics,
  UserEngagementStatistics
} from '@/api/types'
import { toErrorMessage } from '@/views/shared/page-logic'

const loading = ref(false)
const exporting = ref(false)
const errorMessage = ref('')
const keyword = ref('')
const overview = ref<OverviewStatistics | null>(null)
const assessments = ref<AssessmentStatistics | null>(null)
const resources = ref<ResourceStatistics | null>(null)
const appointments = ref<AppointmentStatistics | null>(null)
const engagements = ref<UserEngagementStatistics | null>(null)

const scalePage = ref(1)
const resourcePage = ref(1)
const counselorPage = ref(1)
const engagementPage = ref(1)

const pageSize = 8

const filteredScales = computed(() => {
  const text = keyword.value.trim().toLowerCase()
  return (assessments.value?.scales ?? []).filter((item) => !text || item.scaleName.toLowerCase().includes(text))
})

const filteredCategories = computed(() => {
  const text = keyword.value.trim().toLowerCase()
  return (resources.value?.categories ?? []).filter((item) => !text || item.categoryName.toLowerCase().includes(text))
})

const filteredCounselors = computed(() => {
  const text = keyword.value.trim().toLowerCase()
  return (appointments.value?.counselorLoads ?? []).filter((item) => !text || item.counselorName.toLowerCase().includes(text))
})

const filteredEngagements = computed(() => {
  const text = keyword.value.trim().toLowerCase()
  return (engagements.value?.items ?? []).filter((item) => {
    if (!text) return true
    return item.displayName.toLowerCase().includes(text)
      || (item.studentNo ?? '').toLowerCase().includes(text)
      || (item.college ?? '').toLowerCase().includes(text)
  })
})

function slicePage<T>(items: T[], page: number) {
  const start = (page - 1) * pageSize
  return items.slice(start, start + pageSize)
}

const pagedScales = computed(() => slicePage(filteredScales.value, scalePage.value))
const pagedCategories = computed(() => slicePage(filteredCategories.value, resourcePage.value))
const pagedCounselors = computed(() => slicePage(filteredCounselors.value, counselorPage.value))
const pagedEngagements = computed(() => slicePage(filteredEngagements.value, engagementPage.value))

function calcTotalPages(total: number): number {
  return Math.max(1, Math.ceil(total / pageSize))
}

function formatDateTime(value?: string | null): string {
  if (!value) {
    return '--'
  }
  return new Date(value).toLocaleString('zh-CN', { hour12: false })
}

function resetPaging(): void {
  scalePage.value = 1
  resourcePage.value = 1
  counselorPage.value = 1
  engagementPage.value = 1
}

async function loadStatistics(): Promise<void> {
  loading.value = true
  errorMessage.value = ''

  try {
    const [overviewData, assessmentData, resourceData, appointmentData, engagementData] = await Promise.all([
      fetchOverviewStatisticsApi(),
      fetchAssessmentStatisticsApi(),
      fetchResourceStatisticsApi(),
      fetchAppointmentStatisticsApi(),
      fetchUserEngagementStatisticsApi()
    ])
    overview.value = overviewData
    assessments.value = assessmentData
    resources.value = resourceData
    appointments.value = appointmentData
    engagements.value = engagementData
    resetPaging()
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    loading.value = false
  }
}

async function exportInterventionReport(): Promise<void> {
  exporting.value = true
  errorMessage.value = ''

  try {
    const blob = await exportInterventionEffectReportApi()
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `用户个体心理健康干预效果评估报表_${Date.now()}.xlsx`
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    exporting.value = false
  }
}

function prevScalePage(): void {
  if (scalePage.value > 1) scalePage.value--
}

function nextScalePage(): void {
  if (scalePage.value < calcTotalPages(filteredScales.value.length)) scalePage.value++
}

function prevResourcePage(): void {
  if (resourcePage.value > 1) resourcePage.value--
}

function nextResourcePage(): void {
  if (resourcePage.value < calcTotalPages(filteredCategories.value.length)) resourcePage.value++
}

function prevCounselorPage(): void {
  if (counselorPage.value > 1) counselorPage.value--
}

function nextCounselorPage(): void {
  if (counselorPage.value < calcTotalPages(filteredCounselors.value.length)) counselorPage.value++
}

function prevEngagementPage(): void {
  if (engagementPage.value > 1) engagementPage.value--
}

function nextEngagementPage(): void {
  if (engagementPage.value < calcTotalPages(filteredEngagements.value.length)) engagementPage.value++
}

onMounted(() => {
  void loadStatistics()
})
</script>

<template>
  <section class="admin-table-page">
    <div class="admin-table-shell">
      <header class="admin-table-header">
        <div>
          <h1>统计分析</h1>
          <p>按后台运营视角查看测评、资源、预约和干预效果报表。</p>
        </div>
      </header>

      <p v-if="errorMessage" class="admin-table-alert">{{ errorMessage }}</p>

      <section class="admin-table-toolbar">
        <div class="admin-table-filters">
          <label class="admin-table-field admin-table-field--keyword">
            <span>检索关键词</span>
            <input v-model="keyword" type="text" placeholder="量表名 / 分类名 / 咨询师 / 学生">
          </label>
        </div>
        <div class="admin-table-actions">
          <button class="admin-table-button--secondary" type="button" @click="loadStatistics">刷新</button>
          <button class="admin-table-button" type="button" @click="exportInterventionReport">
            {{ exporting ? '导出中...' : '导出干预效果 Excel' }}
          </button>
        </div>
      </section>

      <section class="admin-table-summary">
        <article class="admin-table-summary-item"><p>学生总数</p><strong>{{ overview?.studentCount ?? 0 }}</strong></article>
        <article class="admin-table-summary-item"><p>咨询师总数</p><strong>{{ overview?.counselorCount ?? 0 }}</strong></article>
        <article class="admin-table-summary-item"><p>测评报告</p><strong>{{ assessments?.totalReports ?? 0 }}</strong></article>
        <article class="admin-table-summary-item"><p>AI 会话</p><strong>{{ overview?.aiSessionCount ?? 0 }}</strong></article>
        <article class="admin-table-summary-item"><p>预约总量</p><strong>{{ appointments?.totalCount ?? 0 }}</strong></article>
        <article class="admin-table-summary-item"><p>资源浏览</p><strong>{{ resources?.totalViews ?? 0 }}</strong></article>
      </section>

      <div class="admin-table-section-grid">
        <section class="admin-table-panel">
          <div class="admin-table-panel-header">
            <div>
              <h2 class="admin-table-panel-title">测评统计</h2>
              <p class="admin-table-panel-note">量表维度的参与、报告与分数分布。</p>
            </div>
          </div>
          <div class="admin-table-wrap">
            <table class="admin-table">
              <thead>
                <tr>
                  <th>量表名称</th>
                  <th>测试人数</th>
                  <th>报告数量</th>
                  <th>最低分</th>
                  <th>最高分</th>
                  <th>平均分</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="scale in pagedScales" :key="scale.scaleId">
                  <td>{{ scale.scaleName }}</td>
                  <td>{{ scale.participantCount }}</td>
                  <td>{{ scale.reportCount }}</td>
                  <td>{{ scale.minScore }}</td>
                  <td>{{ scale.maxScore }}</td>
                  <td>{{ Number(scale.averageScore.toFixed(2)) }}</td>
                </tr>
                <tr v-if="!pagedScales.length">
                  <td colspan="6" class="admin-table-empty">{{ loading ? '加载中...' : '暂无数据' }}</td>
                </tr>
              </tbody>
            </table>
          </div>
          <div class="admin-table-pagination" v-if="calcTotalPages(filteredScales.length) > 1">
            <span>第 {{ scalePage }} / {{ calcTotalPages(filteredScales.length) }} 页</span>
            <div class="admin-table-pagination-actions">
              <button class="admin-table-button--secondary" type="button" :disabled="scalePage <= 1" @click="prevScalePage">上一页</button>
              <button class="admin-table-button--secondary" type="button" :disabled="scalePage >= calcTotalPages(filteredScales.length)" @click="nextScalePage">下一页</button>
            </div>
          </div>
        </section>

        <section class="admin-table-panel">
          <div class="admin-table-panel-header">
            <div>
              <h2 class="admin-table-panel-title">效果评估摘要</h2>
              <p class="admin-table-panel-note">前后测对比的整体趋势。</p>
            </div>
          </div>
          <div class="admin-table-wrap">
            <table class="admin-table">
              <thead>
                <tr>
                  <th>对比样本数</th>
                  <th>平均分差</th>
                  <th>显著改善</th>
                  <th>无明显变化</th>
                  <th>风险加剧</th>
                  <th>样本提示</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td>{{ assessments?.compareSummary.sampleCount ?? 0 }}</td>
                  <td>{{ assessments?.compareSummary.averageDelta ?? 0 }}</td>
                  <td>{{ assessments?.compareSummary.improvedCount ?? 0 }}</td>
                  <td>{{ assessments?.compareSummary.stableCount ?? 0 }}</td>
                  <td>{{ assessments?.compareSummary.worsenedCount ?? 0 }}</td>
                  <td>{{ assessments?.compareSummary.smallSampleWarning ? '样本偏少' : '样本可参考' }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>
      </div>

      <div class="admin-table-section-grid">
        <section class="admin-table-panel">
          <div class="admin-table-panel-header">
            <div>
              <h2 class="admin-table-panel-title">资源分类统计</h2>
              <p class="admin-table-panel-note">查看分类维度的资源沉淀和使用情况。</p>
            </div>
          </div>
          <div class="admin-table-wrap">
            <table class="admin-table">
              <thead>
                <tr>
                  <th>分类名称</th>
                  <th>资源数</th>
                  <th>已发布</th>
                  <th>浏览量</th>
                  <th>收藏量</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="category in pagedCategories" :key="category.categoryId">
                  <td>{{ category.categoryName }}</td>
                  <td>{{ category.resourceCount }}</td>
                  <td>{{ category.publishedCount }}</td>
                  <td>{{ category.viewCount }}</td>
                  <td>{{ category.favoriteCount }}</td>
                </tr>
                <tr v-if="!pagedCategories.length">
                  <td colspan="5" class="admin-table-empty">{{ loading ? '加载中...' : '暂无数据' }}</td>
                </tr>
              </tbody>
            </table>
          </div>
          <div class="admin-table-pagination" v-if="calcTotalPages(filteredCategories.length) > 1">
            <span>第 {{ resourcePage }} / {{ calcTotalPages(filteredCategories.length) }} 页</span>
            <div class="admin-table-pagination-actions">
              <button class="admin-table-button--secondary" type="button" :disabled="resourcePage <= 1" @click="prevResourcePage">上一页</button>
              <button class="admin-table-button--secondary" type="button" :disabled="resourcePage >= calcTotalPages(filteredCategories.length)" @click="nextResourcePage">下一页</button>
            </div>
          </div>
        </section>

        <section class="admin-table-panel">
          <div class="admin-table-panel-header">
            <div>
              <h2 class="admin-table-panel-title">高参与用户</h2>
              <p class="admin-table-panel-note">便于快速定位高频使用样本。</p>
            </div>
          </div>
          <div class="admin-table-wrap">
            <table class="admin-table">
              <thead>
                <tr>
                  <th>姓名</th>
                  <th>学号</th>
                  <th>学院</th>
                  <th>测评次数</th>
                  <th>AI 会话</th>
                  <th>预约次数</th>
                  <th>资源浏览</th>
                  <th>最近活跃</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in pagedEngagements" :key="item.userId">
                  <td>{{ item.displayName }}</td>
                  <td>{{ item.studentNo || '--' }}</td>
                  <td>{{ item.college || '--' }}</td>
                  <td>{{ item.assessmentCount }}</td>
                  <td>{{ item.aiSessionCount }}</td>
                  <td>{{ item.appointmentCount }}</td>
                  <td>{{ item.resourceViewCount }}</td>
                  <td>{{ formatDateTime(item.latestActivityAt) }}</td>
                </tr>
                <tr v-if="!pagedEngagements.length">
                  <td colspan="8" class="admin-table-empty">{{ loading ? '加载中...' : '暂无数据' }}</td>
                </tr>
              </tbody>
            </table>
          </div>
          <div class="admin-table-pagination" v-if="calcTotalPages(filteredEngagements.length) > 1">
            <span>第 {{ engagementPage }} / {{ calcTotalPages(filteredEngagements.length) }} 页</span>
            <div class="admin-table-pagination-actions">
              <button class="admin-table-button--secondary" type="button" :disabled="engagementPage <= 1" @click="prevEngagementPage">上一页</button>
              <button class="admin-table-button--secondary" type="button" :disabled="engagementPage >= calcTotalPages(filteredEngagements.length)" @click="nextEngagementPage">下一页</button>
            </div>
          </div>
        </section>
      </div>

      <section class="admin-table-panel">
        <div class="admin-table-panel-header">
          <div>
            <h2 class="admin-table-panel-title">咨询师预约负载</h2>
            <p class="admin-table-panel-note">查看各咨询师处理预约的分布。</p>
          </div>
        </div>
        <div class="admin-table-wrap">
          <table class="admin-table">
            <thead>
              <tr>
                <th>咨询师</th>
                <th>总预约数</th>
                <th>已接单</th>
                <th>已拒绝</th>
                <th>待处理</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in pagedCounselors" :key="item.counselorUserId">
                <td>{{ item.counselorName }}</td>
                <td>{{ item.totalCount }}</td>
                <td>{{ item.acceptedCount }}</td>
                <td>{{ item.rejectedCount }}</td>
                <td>{{ item.pendingCount }}</td>
              </tr>
              <tr v-if="!pagedCounselors.length">
                <td colspan="5" class="admin-table-empty">{{ loading ? '加载中...' : '暂无数据' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="admin-table-pagination" v-if="calcTotalPages(filteredCounselors.length) > 1">
          <span>第 {{ counselorPage }} / {{ calcTotalPages(filteredCounselors.length) }} 页</span>
          <div class="admin-table-pagination-actions">
            <button class="admin-table-button--secondary" type="button" :disabled="counselorPage <= 1" @click="prevCounselorPage">上一页</button>
            <button class="admin-table-button--secondary" type="button" :disabled="counselorPage >= calcTotalPages(filteredCounselors.length)" @click="nextCounselorPage">下一页</button>
          </div>
        </div>
      </section>
    </div>
  </section>
</template>

<style scoped>
@import './admin-table.css';
</style>
