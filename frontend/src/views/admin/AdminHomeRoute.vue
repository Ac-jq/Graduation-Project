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

onMounted(() => {
  void loadDashboard()
})
</script>

<template>
  <section class="admin-home">
    <div class="page-shell">
      <header class="hero">
        <div class="hero-copy">
          <p class="eyebrow">治理中枢</p>
          <h1>把平台的用户、量表、资源与流量指标收束到一个有秩序的治理大盘。</h1>
          <p class="lead">这里聚合管理员最常用的统计指标与治理入口，所有数字均来自真实后端统计接口。</p>
        </div>
        <div class="hero-metrics">
          <div class="metric-card"><span>学生数</span><strong>{{ overview?.studentCount ?? '-' }}</strong></div>
          <div class="metric-card"><span>咨询师数</span><strong>{{ overview?.counselorCount ?? '-' }}</strong></div>
          <div class="metric-card"><span>资源总量</span><strong>{{ overview?.resourceCount ?? '-' }}</strong></div>
        </div>
      </header>

      <p v-if="errorMessage" class="error-text">{{ errorMessage }}</p>

      <div class="dashboard-grid">
        <section class="launch-panel glass-panel">
          <div class="section-head">
            <p class="section-kicker">控制台</p>
            <h2>治理入口</h2>
          </div>
          <div class="action-stack">
            <button class="action-card" type="button" @click="router.push({ name: 'admin-statistics' })"><span>数据统计</span><small>查看总览、资源、测评与预约分布</small></button>
            <button class="action-card" type="button" @click="router.push({ name: 'admin-users' })"><span>用户管理</span><small>创建咨询师、启停账号、重置密码</small></button>
            <button class="action-card" type="button" @click="router.push({ name: 'admin-resources' })"><span>资源治理</span><small>查看资源上下线状态与分类标签体系</small></button>
            <button class="action-card" type="button" @click="router.push({ name: 'admin-scales' })"><span>量表治理</span><small>查看量表启停状态与题目结构</small></button>
            <button class="action-card" type="button" @click="router.push({ name: 'admin-ai-tasks' })"><span>AI 运维</span><small>解析管理员指令、确认执行步骤并跟踪任务状态</small></button>
            <button class="action-card" type="button" @click="router.push({ name: 'admin-audit-logs' })"><span>审计日志</span><small>回溯关键治理动作、异常处理与管理员操作轨迹</small></button>
          </div>
        </section>

        <section class="insight-panel glass-panel">
          <div class="section-head section-head-inline">
            <div>
              <p class="section-kicker">平台切面</p>
              <h2>运行概览</h2>
            </div>
            <span class="status-chip">{{ loading ? '同步中' : '已更新' }}</span>
          </div>
          <div class="insight-grid">
            <article class="insight-card">
              <p class="insight-label">测评概况</p>
              <h3>{{ assessments?.totalReports ?? 0 }} 份报告</h3>
              <p>{{ assessments ? `参与人数 ${assessments.participantCount} · 平均分 ${assessments.averageScore}` : '正在读取测评统计。' }}</p>
            </article>
            <article class="insight-card">
              <p class="insight-label">预约概况</p>
              <h3>{{ appointmentsStat?.totalCount ?? 0 }} 条预约</h3>
              <p>{{ appointmentsStat ? `待处理 ${appointmentsStat.pendingCount} · 已接单 ${appointmentsStat.acceptedCount}` : '正在读取预约统计。' }}</p>
            </article>
            <article class="insight-card">
              <p class="insight-label">最新用户</p>
              <h3>{{ latestUser?.displayName || '暂无' }}</h3>
              <p>{{ latestUser ? `${latestUser.roleCode} · ${latestUser.account}` : '尚无用户数据。' }}</p>
            </article>
            <article class="insight-card">
              <p class="insight-label">最新资源</p>
              <h3>{{ latestResource?.title || '暂无' }}</h3>
              <p>{{ latestResource ? `${latestResource.categoryName} · ${latestResource.status}` : '尚无资源数据。' }}</p>
            </article>
            <article class="insight-card">
              <p class="insight-label">最新量表</p>
              <h3>{{ latestScale?.name || '暂无' }}</h3>
              <p>{{ latestScale ? `${latestScale.code} · ${latestScale.status}` : '尚无量表数据。' }}</p>
            </article>
            <article class="insight-card">
              <p class="insight-label">资源热度</p>
              <h3>{{ resourcesStat?.totalViews ?? 0 }} 次浏览</h3>
              <p>{{ resourcesStat ? `总收藏 ${resourcesStat.totalFavorites} · 已发布 ${resourcesStat.publishedCount}` : '正在读取资源统计。' }}</p>
            </article>
          </div>
        </section>
      </div>
    </div>
  </section>
</template>
<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700&family=Noto+Serif+SC:wght@400;500;600;700&display=swap');
.admin-home{min-height:100vh;padding:44px 28px 72px;color:#272f27;background:linear-gradient(180deg,#f4efe6 0%,#f8f4ed 100%)}.page-shell{max-width:1320px;margin:0 auto}.hero{display:grid;grid-template-columns:minmax(0,1.35fr) minmax(260px,.65fr);gap:28px;align-items:end;margin-bottom:32px}.hero-copy{border-top:1px solid rgba(59,69,59,.16);padding-top:18px}.eyebrow,.section-kicker,.insight-label{margin:0 0 10px;font:700 .76rem/1 'Manrope',sans-serif;letter-spacing:.22em;text-transform:uppercase;color:#7b6857}.hero-copy h1,.section-head h2,.insight-card h3{margin:0;font-family:'Noto Serif SC',serif;font-weight:600}.hero-copy h1{font-size:clamp(2rem,3vw,3.45rem);line-height:1.16}.lead,.error-text,.insight-card p{font-family:'Manrope',sans-serif}.lead{margin:18px 0 0;max-width:720px;line-height:1.84;color:rgba(39,47,39,.72)}.hero-metrics{display:grid;gap:14px}.metric-card,.glass-panel,.insight-card{border:1px solid rgba(77,86,77,.14);background:rgba(255,252,247,.76);box-shadow:0 24px 70px rgba(91,80,66,.08);backdrop-filter:blur(16px)}.metric-card{padding:18px 20px}.metric-card span,.status-chip{display:block;margin-bottom:8px;font:700 .78rem/1 'Manrope',sans-serif;letter-spacing:.16em;text-transform:uppercase;color:rgba(68,74,66,.56)}.metric-card strong{font:600 1.6rem/1 'Noto Serif SC',serif}.dashboard-grid{display:grid;grid-template-columns:minmax(320px,.82fr) minmax(0,1.18fr);gap:28px}.launch-panel,.insight-panel{padding:24px}.section-head{margin-bottom:18px}.section-head-inline{display:flex;justify-content:space-between;align-items:end;gap:16px}.status-chip{border:1px solid rgba(88,93,84,.14);background:rgba(255,250,240,.82);padding:9px 14px;color:#696152}.action-stack,.insight-grid{display:grid;gap:16px}.action-card{border:1px solid rgba(79,88,79,.12);background:rgba(255,255,255,.58);padding:18px 18px 16px;text-align:left;cursor:pointer;transition:transform .28s ease,box-shadow .28s ease}.action-card:hover{transform:translateY(-2px);box-shadow:0 18px 32px rgba(61,73,63,.1)}.action-card span{font:700 1rem/1.4 'Noto Serif SC',serif;color:#272f27}.action-card small{display:block;margin-top:8px;font:400 .9rem/1.7 'Manrope',sans-serif;color:rgba(39,47,39,.66)}.insight-grid{grid-template-columns:repeat(2,minmax(0,1fr))}.insight-card{padding:18px}.insight-card h3{font-size:1.24rem;line-height:1.35}.insight-card p{margin:10px 0 0;font-size:.92rem;line-height:1.8;color:rgba(39,47,39,.7)}.error-text{margin:0 0 16px;color:#a44f46}
@media (max-width:980px){.admin-home{padding:28px 16px 46px}.hero,.dashboard-grid,.insight-grid{grid-template-columns:1fr}}
</style>

