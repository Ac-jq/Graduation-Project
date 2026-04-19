<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { fetchAdminAuditLogsApi } from '@/api/admin-audit'
import type { AuditLogItem, AuditLogQuery } from '@/api/types'
import type { ApiError } from '@/types/common'
import { toErrorMessage } from '@/views/shared/page-logic'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const errorMessage = ref('')
const logs = ref<AuditLogItem[]>([])
const currentPage = ref(1)
const pageSize = 12

const filters = reactive<AuditLogQuery>({
  actionCode: undefined,
  keyword: undefined
})

const actionCodeOptions = [
  { value: 'LOGIN', label: '用户登录' },
  { value: 'LOGOUT', label: '用户退出' },
  { value: 'CHANGE_PASSWORD', label: '修改密码' },
  { value: 'PROFILE_UPDATE', label: '更新学生档案' },
  { value: 'ASSESSMENT_SUBMIT', label: '提交测评' },
  { value: 'APPOINTMENT_CREATE', label: '发起匿名预约' },
  { value: 'APPOINTMENT_ACCEPT', label: '咨询师接单' },
  { value: 'APPOINTMENT_REJECT', label: '咨询师拒绝预约' },
  { value: 'RESOURCE_FAVORITE_ADD', label: '收藏资源' },
  { value: 'RESOURCE_FAVORITE_REMOVE', label: '取消收藏' },
  { value: 'ADMIN_RESOURCE_CREATE', label: '创建资源' },
  { value: 'ADMIN_RESOURCE_UPDATE', label: '编辑资源' },
  { value: 'ADMIN_RESOURCE_PUBLISH', label: '发布资源' },
  { value: 'ADMIN_RESOURCE_OFFLINE', label: '下线资源' },
  { value: 'ADMIN_RESOURCE_CATEGORY_CREATE', label: '创建资源分类' },
  { value: 'ADMIN_RESOURCE_CATEGORY_UPDATE', label: '编辑资源分类' },
  { value: 'ADMIN_RESOURCE_TAG_CREATE', label: '创建资源标签' },
  { value: 'ADMIN_SCALE_CREATE', label: '创建量表' },
  { value: 'ADMIN_SCALE_UPDATE', label: '编辑量表' },
  { value: 'ADMIN_SCALE_ACTIVATE', label: '启用量表' },
  { value: 'ADMIN_SCALE_DEACTIVATE', label: '停用量表' },
  { value: 'ADMIN_USER_CREATE_COUNSELOR', label: '创建咨询师账号' },
  { value: 'ADMIN_USER_ENABLE', label: '启用用户' },
  { value: 'ADMIN_USER_DISABLE', label: '停用用户' },
  { value: 'ADMIN_USER_RESET_PASSWORD', label: '重置密码' },
  { value: 'ADMIN_AI_PARSE', label: 'AI 解析任务' },
  { value: 'ADMIN_AI_CONFIRM', label: 'AI 确认任务' },
  { value: 'ADMIN_AI_CANCEL', label: 'AI 取消任务' },
  { value: 'ADMIN_AI_USER_STATUS', label: 'AI 更新用户状态' },
  { value: 'ADMIN_AI_RESOURCE_STATUS', label: 'AI 更新资源状态' },
  { value: 'ADMIN_AI_COUNSELOR_CREATE', label: 'AI 创建咨询师' }
]

const totalPages = computed(() => Math.max(1, Math.ceil(logs.value.length / pageSize)))
const pagedLogs = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return logs.value.slice(start, start + pageSize)
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

function syncFiltersFromRoute(): void {
  const actionCode = typeof route.query.actionCode === 'string' ? route.query.actionCode : undefined
  const keyword = typeof route.query.keyword === 'string' ? route.query.keyword : undefined
  filters.actionCode = actionCode
  filters.keyword = keyword
}

async function loadLogs(): Promise<void> {
  loading.value = true
  errorMessage.value = ''

  try {
    logs.value = await fetchAdminAuditLogsApi(filters)
    currentPage.value = 1
    if (logs.value.length === 0 && (filters.actionCode || filters.keyword)) {
      ElMessage.closeAll()
      ElMessage.warning('没有检索到匹配日志，请调整动作类型或关键词后再试')
    }
  } catch (error) {
    const apiError = error as ApiError
    if ((apiError.status ?? 0) >= 500) {
      errorMessage.value = '日志检索暂时不可用，请稍后再试'
      ElMessage.closeAll()
      ElMessage.error('日志检索暂时不可用，请稍后再试')
    } else {
      errorMessage.value = toErrorMessage(error)
    }
  } finally {
    loading.value = false
  }
}

async function syncRouteQuery(): Promise<void> {
  await router.replace({
    query: {
      actionCode: filters.actionCode || undefined,
      keyword: filters.keyword || undefined
    }
  })
}

async function applyFilters(): Promise<void> {
  await syncRouteQuery()
}

async function resetFilters(): Promise<void> {
  filters.actionCode = undefined
  filters.keyword = undefined
  await syncRouteQuery()
}

function prevPage(): void {
  if (currentPage.value > 1) {
    currentPage.value--
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
}

function nextPage(): void {
  if (currentPage.value < totalPages.value) {
    currentPage.value++
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
}

watch(
  () => route.query,
  () => {
    syncFiltersFromRoute()
    void loadLogs()
  }
)

onMounted(() => {
  syncFiltersFromRoute()
  void loadLogs()
})
</script>

<template>
  <section class="admin-table-page">
    <div class="admin-table-shell">
      <header class="admin-table-header">
        <div>
          <h1>审计日志</h1>
          <p>按动作类型和关键词回溯管理员治理行为与系统关键事件。</p>
        </div>
      </header>

      <p v-if="errorMessage" class="admin-table-alert">{{ errorMessage }}</p>

      <section class="admin-table-toolbar">
        <div class="admin-table-filters">
          <label class="admin-table-field">
            <span>动作类型</span>
            <select v-model="filters.actionCode">
              <option :value="undefined">全部动作</option>
              <option v-for="option in actionCodeOptions" :key="option.value" :value="option.value">
                {{ option.label }}
              </option>
            </select>
          </label>
          <label class="admin-table-field admin-table-field--keyword">
            <span>关键词</span>
            <input v-model="filters.keyword" type="text" placeholder="输入关键词检索详情" @keyup.enter="applyFilters">
          </label>
        </div>
        <div class="admin-table-actions">
          <button class="admin-table-button--secondary" type="button" @click="resetFilters">重置</button>
          <button class="admin-table-button" type="button" @click="applyFilters">查询</button>
        </div>
      </section>

      <section class="admin-table-panel">
        <div class="admin-table-panel-header">
          <div>
            <h2 class="admin-table-panel-title">日志列表</h2>
            <p class="admin-table-panel-note">共 {{ logs.length }} 条记录</p>
          </div>
        </div>
        <div class="admin-table-wrap">
          <table class="admin-table">
            <thead>
              <tr>
                <th>#</th>
                <th>动作编码</th>
                <th>动作名称</th>
                <th>操作人</th>
                <th>详情</th>
                <th>IP 地址</th>
                <th>时间</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(log, index) in pagedLogs" :key="log.logId">
                <td>{{ (currentPage - 1) * pageSize + index + 1 }}</td>
                <td>{{ log.actionCode }}</td>
                <td>{{ log.actionName }}</td>
                <td>{{ log.userDisplayName || `用户 ${log.userId ?? '未知'}` }}</td>
                <td>{{ log.detailText }}</td>
                <td>{{ log.ipAddress || '--' }}</td>
                <td>{{ formatDate(log.createdAt) }}</td>
              </tr>
              <tr v-if="!pagedLogs.length">
                <td colspan="7" class="admin-table-empty">{{ loading ? '正在加载审计日志...' : '当前条件下暂无数据' }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="admin-table-pagination" v-if="totalPages > 1">
          <span>第 {{ currentPage }} / {{ totalPages }} 页</span>
          <div class="admin-table-pagination-actions">
            <button class="admin-table-button--secondary" type="button" :disabled="currentPage <= 1" @click="prevPage">上一页</button>
            <button class="admin-table-button--secondary" type="button" :disabled="currentPage >= totalPages" @click="nextPage">下一页</button>
          </div>
        </div>
      </section>
    </div>
  </section>
</template>

<style scoped>
@import './admin-table.css';
</style>
