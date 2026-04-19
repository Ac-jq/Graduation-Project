<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { fetchAdminResourcesApi } from '@/api/admin-resource'
import { fetchAdminScalesApi } from '@/api/admin-scale'
import {
  fetchAppointmentStatisticsApi,
  fetchAssessmentStatisticsApi,
  fetchOverviewStatisticsApi,
  fetchResourceStatisticsApi
} from '@/api/admin-statistics'
import { fetchAdminUsersApi } from '@/api/user'
import { useAuthStore } from '@/stores/auth'
import type {
  AdminResourceListItem,
  AdminScale,
  AdminUserSummary,
  AppointmentStatistics,
  AssessmentStatistics,
  OverviewStatistics,
  ResourceStatistics
} from '@/api/types'
import { toErrorMessage } from '@/views/shared/page-logic'

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const errorMessage = ref('')
const showDropdown = ref(false)
const overview = ref<OverviewStatistics | null>(null)
const assessments = ref<AssessmentStatistics | null>(null)
const resourcesStat = ref<ResourceStatistics | null>(null)
const appointmentsStat = ref<AppointmentStatistics | null>(null)
const users = ref<AdminUserSummary[]>([])
const resources = ref<AdminResourceListItem[]>([])
const scales = ref<AdminScale[]>([])

const currentUser = computed(() => authStore.currentUser)
const roleAvatarUrl = computed(() => `${window.location.protocol}//${window.location.hostname}:8080/assets/avatars/roles/admin-default.jpg`)
const latestUsers = computed(() => users.value.slice(0, 8))
const latestResources = computed(() => resources.value.slice(0, 8))
const latestScales = computed(() => scales.value.slice(0, 8))

function formatDate(value?: string | null): string {
  if (!value) {
    return '--'
  }

  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value))
}

function resolveScaleStatus(status?: string): string {
  if (status === 'ACTIVE') return '启用中'
  if (status === 'INACTIVE') return '已停用'
  return status || '未标记'
}

function resolveResourceStatus(status?: string): string {
  if (status === 'PUBLISHED') return '已发布'
  if (status === 'OFFLINE') return '已下线'
  if (status === 'DRAFT') return '草稿'
  return status || '未标记'
}

function resolveUserStatus(status?: string): string {
  if (status === 'ACTIVE') return '正常'
  if (status === 'DISABLED') return '禁用'
  return status || '未标记'
}

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

async function handleDropdownClick(action: 'role' | 'home' | 'security' | 'logout'): Promise<void> {
  showDropdown.value = false

  if (action === 'role' || action === 'security') {
    await router.push('/admin/account')
    return
  }

  if (action === 'home') {
    await router.push('/admin')
    return
  }

  await authStore.signOut(true)
  await router.push('/login')
}

onMounted(() => {
  void loadDashboard()
})
</script>

<template>
  <section class="admin-table-page">
    <div class="admin-table-shell">
      <header class="admin-table-header">
        <div>
          <h1>管理首页</h1>
          <p>统一查看平台核心数据、最近变动和后台治理入口。</p>
        </div>

        <div class="admin-home-profile" @mouseleave="showDropdown = false">
          <button class="admin-home-profile-btn" type="button" @click="showDropdown = !showDropdown">
            <img :src="roleAvatarUrl" alt="管理员头像">
            <div>
              <strong>{{ currentUser?.displayName || currentUser?.realName || '管理员' }}</strong>
              <span>管理员工作台</span>
            </div>
          </button>

          <ul v-if="showDropdown" class="admin-home-profile-menu">
            <li @click="handleDropdownClick('role')">角色信息</li>
            <li @click="handleDropdownClick('home')">首页</li>
            <li @click="handleDropdownClick('security')">账户安全</li>
            <li class="is-danger" @click="handleDropdownClick('logout')">退出登录</li>
          </ul>
        </div>
      </header>

      <p v-if="errorMessage" class="admin-table-alert">{{ errorMessage }}</p>

      <section class="admin-table-toolbar">
        <div class="admin-table-actions">
          <button class="admin-table-button" type="button" @click="loadDashboard">刷新数据</button>
          <button class="admin-table-button--secondary" type="button" @click="router.push({ name: 'admin-users' })">用户管理</button>
          <button class="admin-table-button--secondary" type="button" @click="router.push({ name: 'admin-scales' })">量表管理</button>
          <button class="admin-table-button--secondary" type="button" @click="router.push({ name: 'admin-resources' })">资源管理</button>
          <button class="admin-table-button--secondary" type="button" @click="router.push({ name: 'admin-statistics' })">统计分析</button>
          <button class="admin-table-button--secondary" type="button" @click="router.push({ name: 'admin-audit-logs' })">审计日志</button>
        </div>
      </section>

      <section class="admin-table-summary">
        <article class="admin-table-summary-item">
          <p>学生总数</p>
          <strong>{{ overview?.studentCount ?? 0 }}</strong>
        </article>
        <article class="admin-table-summary-item">
          <p>咨询师总数</p>
          <strong>{{ overview?.counselorCount ?? 0 }}</strong>
        </article>
        <article class="admin-table-summary-item">
          <p>测评报告</p>
          <strong>{{ assessments?.totalReports ?? 0 }}</strong>
        </article>
        <article class="admin-table-summary-item">
          <p>资源总量</p>
          <strong>{{ overview?.resourceCount ?? 0 }}</strong>
        </article>
        <article class="admin-table-summary-item">
          <p>预约总量</p>
          <strong>{{ appointmentsStat?.totalCount ?? 0 }}</strong>
        </article>
        <article class="admin-table-summary-item">
          <p>资源浏览</p>
          <strong>{{ resourcesStat?.totalViews ?? 0 }}</strong>
        </article>
      </section>

      <div class="admin-table-section-grid">
        <section class="admin-table-panel">
          <div class="admin-table-panel-header">
            <div>
              <h2 class="admin-table-panel-title">最近用户</h2>
              <p class="admin-table-panel-note">显示最近加载到的用户记录。</p>
            </div>
          </div>
          <div class="admin-table-wrap">
            <table class="admin-table">
              <thead>
                <tr>
                  <th>账号</th>
                  <th>姓名</th>
                  <th>角色</th>
                  <th>状态</th>
                  <th>创建时间</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="user in latestUsers" :key="user.userId">
                  <td>{{ user.account }}</td>
                  <td>{{ user.displayName }}</td>
                  <td>{{ user.roleCode }}</td>
                  <td>{{ resolveUserStatus(user.status) }}</td>
                  <td>{{ formatDate(user.createdAt) }}</td>
                </tr>
                <tr v-if="!latestUsers.length">
                  <td colspan="5" class="admin-table-empty">{{ loading ? '加载中...' : '暂无数据' }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        <section class="admin-table-panel">
          <div class="admin-table-panel-header">
            <div>
              <h2 class="admin-table-panel-title">最近资源</h2>
              <p class="admin-table-panel-note">快速查看资源状态和热度。</p>
            </div>
          </div>
          <div class="admin-table-wrap">
            <table class="admin-table">
              <thead>
                <tr>
                  <th>标题</th>
                  <th>分类</th>
                  <th>类型</th>
                  <th>状态</th>
                  <th>浏览 / 收藏</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="resource in latestResources" :key="resource.resourceId">
                  <td>{{ resource.title }}</td>
                  <td>{{ resource.categoryName }}</td>
                  <td>{{ resource.resourceType }}</td>
                  <td>{{ resolveResourceStatus(resource.status) }}</td>
                  <td>{{ resource.viewCount }} / {{ resource.favoriteCount }}</td>
                </tr>
                <tr v-if="!latestResources.length">
                  <td colspan="5" class="admin-table-empty">{{ loading ? '加载中...' : '暂无数据' }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>
      </div>

      <section class="admin-table-panel">
        <div class="admin-table-panel-header">
          <div>
            <h2 class="admin-table-panel-title">最近量表</h2>
            <p class="admin-table-panel-note">查看最新量表的状态、题量和阈值配置。</p>
          </div>
        </div>
        <div class="admin-table-wrap">
          <table class="admin-table">
            <thead>
              <tr>
                <th>量表编码</th>
                <th>量表名称</th>
                <th>状态</th>
                <th>题目数</th>
                <th>分页数</th>
                <th>阈值</th>
                <th>更新时间</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="scale in latestScales" :key="scale.scaleId">
                <td>{{ scale.code }}</td>
                <td>{{ scale.name }}</td>
                <td>{{ resolveScaleStatus(scale.status) }}</td>
                <td>{{ scale.totalQuestions }}</td>
                <td>{{ scale.pageSize }}</td>
                <td>{{ scale.lowThreshold }} / {{ scale.mediumThreshold }} / {{ scale.highThreshold }}</td>
                <td>{{ formatDate(scale.updatedAt) }}</td>
              </tr>
              <tr v-if="!latestScales.length">
                <td colspan="7" class="admin-table-empty">{{ loading ? '加载中...' : '暂无数据' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </div>
  </section>
</template>

<style scoped>
@import './admin-table.css';

.admin-home-profile {
  position: relative;
}

.admin-home-profile-btn {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  border: 1px solid #d1d5db;
  border-radius: 10px;
  background: #fff;
  cursor: pointer;
}

.admin-home-profile-btn img {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
}

.admin-home-profile-btn strong,
.admin-home-profile-btn span {
  display: block;
  text-align: left;
}

.admin-home-profile-btn strong {
  font-size: 14px;
  color: #111827;
}

.admin-home-profile-btn span {
  font-size: 12px;
  color: #6b7280;
}

.admin-home-profile-menu {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  width: 160px;
  margin: 0;
  padding: 8px 0;
  list-style: none;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.12);
  z-index: 20;
}

.admin-home-profile-menu li {
  padding: 10px 14px;
  font-size: 14px;
  color: #374151;
  cursor: pointer;
}

.admin-home-profile-menu li:hover {
  background: #f3f4f6;
}

.admin-home-profile-menu li.is-danger {
  color: #dc2626;
}
</style>
