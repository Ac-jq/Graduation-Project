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

const uniqueActionCodes = computed(() => Array.from(new Set(logs.value.map((log) => log.actionCode))))
const latestLog = computed(() => logs.value[0] ?? null)

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
  <section class="admin-editorial-page">
    <div class="admin-editorial-shell">
      <header class="admin-editorial-hero">
        <div class="admin-editorial-copy">
          <p class="admin-editorial-eyebrow">审计日志</p>
          <h1 class="admin-editorial-title">在一条更清晰的日志流里回看管理员治理动作与系统关键事件。</h1>
          <p class="admin-editorial-lead">筛选、检索和路由同步逻辑保持不变，只把原先偏后台表单式的页面整理成更接近学生端的阅读结构。</p>
        </div>
        <div class="admin-editorial-hero-side">
          <article class="admin-editorial-stat">
            <p class="admin-editorial-label">日志总数</p>
            <strong>{{ logs.length }}</strong>
          </article>
          <article class="admin-editorial-stat">
            <p class="admin-editorial-label">动作类型数</p>
            <strong>{{ uniqueActionCodes.length }}</strong>
          </article>
        </div>
      </header>

      <p v-if="errorMessage" class="admin-editorial-alert">{{ errorMessage }}</p>

      <section class="admin-editorial-panel admin-editorial-panel--mesh">
        <div class="admin-editorial-section admin-editorial-section--inline">
          <div>
            <p class="admin-editorial-kicker">筛选条件</p>
            <h2>按动作和关键词回溯治理过程</h2>
          </div>
          <span class="admin-editorial-badge">{{ latestLog ? formatDate(latestLog.createdAt) : '暂无日志' }}</span>
        </div>

        <div class="admin-editorial-form">
          <label class="admin-editorial-field">
            <span>动作类型</span>
            <select v-model="filters.actionCode">
              <option :value="undefined">全部动作</option>
              <option v-for="option in actionCodeOptions" :key="option.value" :value="option.value">
                {{ option.label }} / {{ option.value }}
              </option>
            </select>
          </label>
          <label class="admin-editorial-field">
            <span>关键词</span>
            <input v-model="filters.keyword" type="text" placeholder="输入关键词检索详情" @keyup.enter="applyFilters">
          </label>
        </div>

        <div class="admin-editorial-actions" style="margin-top: 1rem;">
          <button class="admin-editorial-button" type="button" @click="applyFilters">应用筛选</button>
          <button class="admin-editorial-ghost" type="button" @click="resetFilters">重置</button>
        </div>
      </section>

      <section v-if="loading" class="admin-editorial-panel" style="margin-top: 1.5rem;">
        <div class="admin-editorial-empty">正在加载审计日志…</div>
      </section>

      <section v-else-if="logs.length" class="admin-editorial-board" style="margin-top: 1.5rem;">
        <article v-for="log in logs" :key="log.logId" class="admin-editorial-card">
          <div class="admin-editorial-card__topline">
            <div>
              <p class="admin-editorial-code">日志 #{{ log.logId }}</p>
              <h3>{{ log.actionName }}</h3>
            </div>
            <span class="admin-editorial-status">{{ log.actionCode }}</span>
          </div>
          <p>{{ log.detailText }}</p>
          <div class="admin-editorial-card__footer">
            <div class="admin-editorial-meta">
              <span>操作人：{{ log.userDisplayName || `用户 ${log.userId ?? '未知'}` }}</span>
              <span>IP：{{ log.ipAddress || '未记录' }}</span>
            </div>
            <span class="admin-editorial-note">{{ formatDate(log.createdAt) }}</span>
          </div>
        </article>
      </section>

      <section v-else class="admin-editorial-panel" style="margin-top: 1.5rem;">
        <div class="admin-editorial-empty">当前筛选条件下没有日志记录。</div>
      </section>
    </div>
  </section>
</template>

<style scoped>
@import './admin-editorial.css';
</style>
