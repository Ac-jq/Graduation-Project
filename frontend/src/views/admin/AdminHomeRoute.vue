<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { fetchAdminResourcesApi } from '@/api/admin-resource'
import { fetchAdminScalesApi } from '@/api/admin-scale'
import { fetchAppointmentStatisticsApi, fetchAssessmentStatisticsApi, fetchOverviewStatisticsApi, fetchResourceStatisticsApi } from '@/api/admin-statistics'
import { fetchAdminUsersApi } from '@/api/user'
import type { AdminResourceListItem, AdminScale, AdminUserSummary, AppointmentStatistics, AssessmentStatistics, OverviewStatistics, ResourceStatistics } from '@/api/types'
import { toErrorMessage } from '@/views/shared/page-logic'

const router = useRouter()
const loading = ref(false)
const errorMessage = ref('')
const overview = ref<OverviewStatistics | null>(null)
const assessments = ref<AssessmentStatistics | null>(null)
const resourcesStat = ref<ResourceStatistics | null>(null)
const appointmentsStat = ref<AppointmentStatistics | null>(null)
const users = ref<AdminUserSummary[]>([])
const resources = ref<AdminResourceListItem[]>([])
const scales = ref<AdminScale[]>([])

const latestUser = computed(() => users.value[0] ?? null)
const latestResource = computed(() => resources.value[0] ?? null)
const latestScale = computed(() => scales.value[0] ?? null)

async function loadDashboard(): Promise<void> {
  loading.value = true
  errorMessage.value = ''

  try {
    const [overviewData, assessmentData, resourceData, appointmentData, userData, resourceList, scaleList] = await Promise.all([
      fetchOverviewStatisticsApi(),
      fetchAssessmentStatisticsApi(),
      fetchResourceStatisticsApi(),
      fetchAppointmentStatisticsApi(),
      fetchAdminUsersApi(),
      fetchAdminResourcesApi(),
      fetchAdminScalesApi()
    ])
    overview.value = overviewData
    assessments.value = assessmentData
    resourcesStat.value = resourceData
    appointmentsStat.value = appointmentData
    users.value = userData
    resources.value = resourceList
    scales.value = scaleList
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    loading.value = false
  }
}

function resolveScaleStatus(status?: string): string {
  return status === 'ACTIVE' ? '启用中' : status === 'INACTIVE' ? '已停用' : status || '未标记'
}

onMounted(() => {
  void loadDashboard()
})
</script>

<template>
  <section class="admin-editorial-page">
    <div class="admin-editorial-shell">
      <header class="admin-editorial-hero">
        <div class="admin-editorial-copy">
          <p class="admin-editorial-eyebrow">管理员工作台</p>
          <h1 class="admin-editorial-title">把用户、资源、量表与流量数据收束到同一张治理画布。</h1>
          <p class="admin-editorial-lead">
            这里保留了原有真实统计接口与治理入口，只把管理员端的阅读节奏、留白和卡片层级统一到学生端的视觉体系。
          </p>
        </div>

        <div class="admin-editorial-hero-side">
          <article class="admin-editorial-stat">
            <p class="admin-editorial-label">治理快照</p>
            <strong>{{ loading ? '-' : `${overview?.studentCount ?? 0} / ${overview?.counselorCount ?? 0}` }}</strong>
            <p class="admin-editorial-lead">学生与咨询师规模一眼可见，便于快速判断平台活跃结构。</p>
          </article>
          <article class="admin-editorial-stat">
            <p class="admin-editorial-label">资源与会话</p>
            <strong>{{ loading ? '-' : `${overview?.resourceCount ?? 0} / ${overview?.aiSessionCount ?? 0}` }}</strong>
            <p class="admin-editorial-lead">资源沉淀与 AI 会话量共同反映平台陪伴密度。</p>
          </article>
        </div>
      </header>

      <p v-if="errorMessage" class="admin-editorial-alert">{{ errorMessage }}</p>

      <div class="admin-editorial-grid">
        <section class="admin-editorial-panel admin-editorial-panel--mesh">
          <div class="admin-editorial-section">
            <p class="admin-editorial-kicker">治理入口</p>
            <h2>从这里进入各个管理模块</h2>
          </div>

          <div class="admin-editorial-actions">
            <button class="admin-editorial-card" type="button" @click="router.push({ name: 'admin-statistics' })">
              <p class="admin-editorial-code">01</p>
              <h3>统计分析</h3>
              <p>查看总览、资源、测评与预约的实时切面。</p>
            </button>
            <button class="admin-editorial-card" type="button" @click="router.push({ name: 'admin-users' })">
              <p class="admin-editorial-code">02</p>
              <h3>用户管理</h3>
              <p>创建咨询师、启停账号、重置密码与筛选用户。</p>
            </button>
            <button class="admin-editorial-card" type="button" @click="router.push({ name: 'admin-resources' })">
              <p class="admin-editorial-code">03</p>
              <h3>资源治理</h3>
              <p>管理资源上下线、分类、标签与资源详情。</p>
            </button>
            <button class="admin-editorial-card" type="button" @click="router.push({ name: 'admin-scales' })">
              <p class="admin-editorial-code">04</p>
              <h3>量表治理</h3>
              <p>维护量表状态、阈值、题量与结构说明。</p>
            </button>
            <button class="admin-editorial-card" type="button" @click="router.push({ name: 'admin-ai-tasks' })">
              <p class="admin-editorial-code">05</p>
              <h3>AI 运维</h3>
              <p>解析自然语言指令，先预览计划，再人工确认执行。</p>
            </button>
            <button class="admin-editorial-card" type="button" @click="router.push({ name: 'admin-audit-logs' })">
              <p class="admin-editorial-code">06</p>
              <h3>审计日志</h3>
              <p>回溯关键动作、异常处理与治理轨迹。</p>
            </button>
          </div>
        </section>

        <section class="admin-editorial-panel">
          <div class="admin-editorial-section admin-editorial-section--inline">
            <div>
              <p class="admin-editorial-kicker">平台切面</p>
              <h2>运行概览</h2>
            </div>
            <span class="admin-editorial-badge">{{ loading ? '同步中' : '已更新' }}</span>
          </div>

          <div class="admin-editorial-metrics">
            <article class="admin-editorial-metric">
              <p class="admin-editorial-label">学生数</p>
              <strong>{{ overview?.studentCount ?? '-' }}</strong>
            </article>
            <article class="admin-editorial-metric">
              <p class="admin-editorial-label">咨询师数</p>
              <strong>{{ overview?.counselorCount ?? '-' }}</strong>
            </article>
            <article class="admin-editorial-metric">
              <p class="admin-editorial-label">资源总量</p>
              <strong>{{ overview?.resourceCount ?? '-' }}</strong>
            </article>
            <article class="admin-editorial-metric">
              <p class="admin-editorial-label">报告总数</p>
              <strong>{{ assessments?.totalReports ?? 0 }}</strong>
            </article>
            <article class="admin-editorial-metric">
              <p class="admin-editorial-label">预约总量</p>
              <strong>{{ appointmentsStat?.totalCount ?? 0 }}</strong>
            </article>
            <article class="admin-editorial-metric">
              <p class="admin-editorial-label">浏览总量</p>
              <strong>{{ resourcesStat?.totalViews ?? 0 }}</strong>
            </article>
          </div>

          <div class="admin-editorial-stack" style="margin-top: 1rem;">
            <article class="admin-editorial-card">
              <div class="admin-editorial-card__topline">
                <div>
                  <p class="admin-editorial-code">最新用户</p>
                  <h3>{{ latestUser?.displayName || '暂无' }}</h3>
                </div>
                <span class="admin-editorial-status">{{ latestUser?.roleCode || '无记录' }}</span>
              </div>
              <p>{{ latestUser ? `${latestUser.account} · ${latestUser.status}` : '尚无用户数据。' }}</p>
            </article>

            <article class="admin-editorial-card">
              <div class="admin-editorial-card__topline">
                <div>
                  <p class="admin-editorial-code">最新资源</p>
                  <h3>{{ latestResource?.title || '暂无' }}</h3>
                </div>
                <span class="admin-editorial-status">{{ latestResource?.status || '无记录' }}</span>
              </div>
              <p>{{ latestResource ? `${latestResource.categoryName} · 收藏 ${latestResource.favoriteCount} · 浏览 ${latestResource.viewCount}` : '尚无资源数据。' }}</p>
            </article>

            <article class="admin-editorial-card">
              <div class="admin-editorial-card__topline">
                <div>
                  <p class="admin-editorial-code">最新量表</p>
                  <h3>{{ latestScale?.name || '暂无' }}</h3>
                </div>
                <span class="admin-editorial-status">{{ resolveScaleStatus(latestScale?.status) }}</span>
              </div>
              <p>{{ latestScale ? `${latestScale.code} · 题目 ${latestScale.totalQuestions} · 每页 ${latestScale.pageSize}` : '尚无量表数据。' }}</p>
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
