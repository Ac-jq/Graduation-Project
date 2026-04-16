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
const overview = ref<OverviewStatistics | null>(null)
const assessments = ref<AssessmentStatistics | null>(null)
const resources = ref<ResourceStatistics | null>(null)
const appointments = ref<AppointmentStatistics | null>(null)
const engagements = ref<UserEngagementStatistics | null>(null)

const compareSummary = computed(() => assessments.value?.compareSummary ?? null)
const topEngagementItems = computed(() => (engagements.value?.items ?? []).slice(0, 10))

function formatDateTime(value?: string | null): string {
  if (!value) {
    return '暂无记录'
  }
  return new Date(value).toLocaleString('zh-CN', { hour12: false })
}

function resolveDeltaText(delta?: number): string {
  if (delta == null) {
    return '暂无对比'
  }
  if (delta < 0) {
    return `平均下降 ${Math.abs(delta).toFixed(2)} 分`
  }
  if (delta > 0) {
    return `平均上升 ${delta.toFixed(2)} 分`
  }
  return '前后测平均无变化'
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

onMounted(() => {
  void loadStatistics()
})
</script>

<template>
  <section class="admin-editorial-page">
    <div class="admin-editorial-shell">
      <header class="admin-editorial-hero">
        <div class="admin-editorial-copy">
          <p class="admin-editorial-eyebrow">统计分析</p>
          <h1 class="admin-editorial-title">围绕真实用户逐行导出前后测干预效果评估报表。</h1>
          <p class="admin-editorial-lead">
            这版导出不再输出平台级总览表，而是针对每个学生计算前测、后测、风险变化与干预效果，适合论文附表和答辩展示。
          </p>
        </div>
        <div class="admin-editorial-hero-side">
          <article class="admin-editorial-stat">
            <p class="admin-editorial-label">导出状态</p>
            <strong>{{ exporting ? '导出中' : '可导出' }}</strong>
            <p class="admin-editorial-lead">输出格式为后端生成的 Excel 文件。</p>
          </article>
        </div>
      </header>

      <p v-if="errorMessage" class="admin-editorial-alert">{{ errorMessage }}</p>

      <section class="admin-editorial-panel admin-editorial-panel--mesh">
        <div class="admin-editorial-section admin-editorial-section--inline">
          <div>
            <p class="admin-editorial-kicker">报表导出</p>
            <h2>用户个体心理健康干预效果评估</h2>
          </div>
          <span class="admin-editorial-badge">{{ loading ? '同步中' : '已就绪' }}</span>
        </div>
        <p class="admin-editorial-lead">
          导出字段包含基础信息、系统参与度、前测基线、后测现状、得分差值、状态转化和自动评估结果。
        </p>
        <div class="admin-editorial-actions">
          <button class="admin-editorial-button" type="button" @click="exportInterventionReport">
            {{ exporting ? '正在生成 Excel...' : '导出干预效果评估 Excel' }}
          </button>
        </div>
      </section>

      <div class="admin-editorial-grid admin-editorial-grid--equal" style="margin-top: 1.5rem;">
        <section class="admin-editorial-panel">
          <div class="admin-editorial-section">
            <p class="admin-editorial-kicker">总览指标</p>
            <h2>先看平台总体运行截面</h2>
          </div>
          <div class="admin-editorial-metrics">
            <article class="admin-editorial-metric"><p class="admin-editorial-label">学生</p><strong>{{ overview?.studentCount ?? '-' }}</strong></article>
            <article class="admin-editorial-metric"><p class="admin-editorial-label">咨询师</p><strong>{{ overview?.counselorCount ?? '-' }}</strong></article>
            <article class="admin-editorial-metric"><p class="admin-editorial-label">测评报告</p><strong>{{ overview?.scaleReportCount ?? '-' }}</strong></article>
            <article class="admin-editorial-metric"><p class="admin-editorial-label">AI 会话</p><strong>{{ overview?.aiSessionCount ?? '-' }}</strong></article>
            <article class="admin-editorial-metric"><p class="admin-editorial-label">预约</p><strong>{{ overview?.appointmentCount ?? '-' }}</strong></article>
            <article class="admin-editorial-metric"><p class="admin-editorial-label">资源浏览</p><strong>{{ overview?.resourceViewCount ?? '-' }}</strong></article>
          </div>
        </section>

        <section class="admin-editorial-panel">
          <div class="admin-editorial-section">
            <p class="admin-editorial-kicker">效果概览</p>
            <h2>先看整体前后测趋势</h2>
          </div>
          <div class="admin-editorial-metrics">
            <article class="admin-editorial-metric"><p class="admin-editorial-label">对比样本数</p><strong>{{ compareSummary?.sampleCount ?? 0 }}</strong></article>
            <article class="admin-editorial-metric"><p class="admin-editorial-label">平均分差</p><strong>{{ resolveDeltaText(compareSummary?.averageDelta) }}</strong></article>
            <article class="admin-editorial-metric"><p class="admin-editorial-label">改善人数</p><strong>{{ compareSummary?.improvedCount ?? 0 }}</strong></article>
            <article class="admin-editorial-metric"><p class="admin-editorial-label">稳定人数</p><strong>{{ compareSummary?.stableCount ?? 0 }}</strong></article>
            <article class="admin-editorial-metric"><p class="admin-editorial-label">波动人数</p><strong>{{ compareSummary?.worsenedCount ?? 0 }}</strong></article>
            <article class="admin-editorial-metric"><p class="admin-editorial-label">样本提示</p><strong>{{ compareSummary?.smallSampleWarning ? '样本偏少' : '样本可参考' }}</strong></article>
          </div>
        </section>
      </div>

      <div class="admin-editorial-grid admin-editorial-grid--equal" style="margin-top: 1.5rem;">
        <section class="admin-editorial-panel admin-editorial-panel--mesh">
          <div class="admin-editorial-section">
            <p class="admin-editorial-kicker">用户参与情况</p>
            <h2>高参与用户可直接作为干预样本观察入口</h2>
          </div>
          <p class="admin-editorial-lead">
            总学生 {{ engagements?.totalStudents ?? 0 }} / 有参与记录 {{ engagements?.activeStudents ?? 0 }} / 高参与用户 {{ engagements?.highlyEngagedStudents ?? 0 }}
          </p>
          <div class="admin-editorial-board" style="margin-top: 1rem;">
            <article v-for="item in topEngagementItems" :key="item.userId" class="admin-editorial-card">
              <div class="admin-editorial-card__topline">
                <div>
                  <p class="admin-editorial-code">{{ item.studentNo || `用户 ${item.userId}` }}</p>
                  <h3>{{ item.displayName }}</h3>
                </div>
                <span class="admin-editorial-status">热度 {{ item.engagementScore }}</span>
              </div>
              <div class="admin-editorial-meta">
                <span>{{ item.college || '未填写学院' }}</span>
                <span>{{ item.grade || '未填写年级' }}</span>
                <span>测评 {{ item.assessmentCount }}</span>
                <span>AI 会话 {{ item.aiSessionCount }}</span>
                <span>预约 {{ item.appointmentCount }}</span>
                <span>资源浏览 {{ item.resourceViewCount }}</span>
              </div>
              <div class="admin-editorial-card__footer">
                <span class="admin-editorial-note">最近活跃：{{ formatDateTime(item.latestActivityAt) }}</span>
              </div>
            </article>
          </div>
        </section>

        <section class="admin-editorial-panel">
          <div class="admin-editorial-section">
            <p class="admin-editorial-kicker">量表与资源</p>
            <h2>同步检查测评和资源使用面</h2>
          </div>
          <div class="admin-editorial-board">
            <article v-for="scale in assessments?.scales ?? []" :key="scale.scaleId" class="admin-editorial-card">
              <div class="admin-editorial-card__topline">
                <div>
                  <p class="admin-editorial-code">测评量表</p>
                  <h3>{{ scale.scaleName }}</h3>
                </div>
                <span class="admin-editorial-status">报告 {{ scale.reportCount }}</span>
              </div>
              <div class="admin-editorial-meta">
                <span>测试人数 {{ scale.participantCount }}</span>
                <span>最低 {{ scale.minScore }}</span>
                <span>最高 {{ scale.maxScore }}</span>
                <span>平均 {{ Number(scale.averageScore.toFixed(2)) }}</span>
              </div>
            </article>

            <article v-for="category in resources?.categories ?? []" :key="`category-${category.categoryId}`" class="admin-editorial-card">
              <div class="admin-editorial-card__topline">
                <div>
                  <p class="admin-editorial-code">资源分类</p>
                  <h3>{{ category.categoryName }}</h3>
                </div>
                <span class="admin-editorial-status">资源 {{ category.resourceCount }}</span>
              </div>
              <div class="admin-editorial-meta">
                <span>已发布 {{ category.publishedCount }}</span>
                <span>浏览 {{ category.viewCount }}</span>
                <span>收藏 {{ category.favoriteCount }}</span>
              </div>
            </article>
          </div>
        </section>
      </div>
    </div>
  </section>
</template>

<style scoped>
@import './admin-editorial.css';
</style>
