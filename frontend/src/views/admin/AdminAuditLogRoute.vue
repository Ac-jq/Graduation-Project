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
  <main class="admin-audit-page">
    <section class="admin-audit-page__masthead">
      <div class="admin-audit-page__heading">
        <p class="admin-audit-page__eyebrow">操作审计台账</p>
        <h1 class="admin-audit-page__title">审计日志</h1>
        <p class="admin-audit-page__summary">
          这里按动作与关键词回溯系统治理过程。所有管理员操作、AI 任务执行和资源治理都会沉淀到这条日志流里。
        </p>
      </div>

      <aside class="admin-audit-page__snapshot">
        <p class="admin-audit-page__label">日志快照</p>
        <dl>
          <div>
            <dt>总数</dt>
            <dd>{{ logs.length }}</dd>
          </div>
          <div>
            <dt>动作类型数</dt>
            <dd>{{ uniqueActionCodes.length }}</dd>
          </div>
          <div>
            <dt>最新时间</dt>
            <dd>{{ latestLog ? formatDate(latestLog.createdAt) : '暂无日志' }}</dd>
          </div>
        </dl>
      </aside>
    </section>

    <p v-if="errorMessage" class="admin-audit-page__alert">{{ errorMessage }}</p>

    <section class="admin-audit-page__filters">
      <label class="admin-audit-page__field">
        <span>动作类型</span>
        <select v-model="filters.actionCode">
          <option :value="undefined">全部动作</option>
          <option v-for="option in actionCodeOptions" :key="option.value" :value="option.value">
            {{ option.label }} / {{ option.value }}
          </option>
        </select>
      </label>
      <label class="admin-audit-page__field">
        <span>关键词</span>
        <input v-model="filters.keyword" type="text" placeholder="输入关键词检索详情" @keyup.enter="applyFilters" />
      </label>
      <div class="admin-audit-page__filter-actions">
        <button class="admin-audit-page__primary" type="button" @click="applyFilters">应用筛选</button>
        <button class="admin-audit-page__ghost" type="button" @click="resetFilters">重置</button>
      </div>
    </section>

    <section v-if="loading" class="admin-audit-page__status-panel">
      <p>正在加载审计日志...</p>
    </section>

    <section v-else-if="logs.length" class="admin-audit-page__list">
      <article v-for="log in logs" :key="log.logId" class="admin-audit-card">
        <header class="admin-audit-card__header">
          <div>
            <p class="admin-audit-card__serial">日志 #{{ log.logId }}</p>
            <h2>{{ log.actionName }}</h2>
          </div>
          <span>{{ log.actionCode }}</span>
        </header>
        <p class="admin-audit-card__detail">{{ log.detailText }}</p>
        <footer class="admin-audit-card__footer">
          <div>
            <p>操作人：{{ log.userDisplayName || `用户 ${log.userId ?? '未知'}` }}</p>
            <p>IP：{{ log.ipAddress || '未记录' }}</p>
          </div>
          <time>{{ formatDate(log.createdAt) }}</time>
        </footer>
      </article>
    </section>

    <section v-else class="admin-audit-page__status-panel">
      <p>当前筛选条件下没有日志记录。</p>
    </section>
  </main>
</template>
<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700&family=Noto+Serif+SC:wght@400;500;600;700&display=swap');

.admin-audit-page {
  --paper: #f2ede4;
  --ink: #201b17;
  --muted: #6b645d;
  --line: rgba(32, 27, 23, 0.12);
  --glass: rgba(255, 251, 245, 0.72);
  min-height: 100vh;
  padding: 2rem;
  color: var(--ink);
  background:
    radial-gradient(circle at top right, rgba(110, 128, 118, 0.18), transparent 24%),
    radial-gradient(circle at left center, rgba(198, 186, 166, 0.2), transparent 30%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.14), transparent 38%),
    var(--paper);
}

.admin-audit-page__masthead {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(300px, 0.85fr);
  gap: 1.5rem;
  align-items: end;
  padding-bottom: 1.4rem;
  border-bottom: 1px solid var(--line);
}

.admin-audit-page__eyebrow,
.admin-audit-page__label,
.admin-audit-page__snapshot dt,
.admin-audit-card__serial,
.admin-audit-card__header span,
.admin-audit-page__field span,
.admin-audit-card__footer time {
  margin: 0;
  font: 600 0.72rem/1.4 'Manrope', sans-serif;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--muted);
}

.admin-audit-page__title {
  margin: 0.95rem 0 0;
  font: 600 clamp(2.7rem, 4.8vw, 5rem)/0.98 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.admin-audit-page__summary,
.admin-audit-card__detail,
.admin-audit-card__footer p,
.admin-audit-page__status-panel p {
  margin: 0;
  color: var(--muted);
  font: 400 0.98rem/1.85 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.admin-audit-page__snapshot,
.admin-audit-page__filters,
.admin-audit-card,
.admin-audit-page__status-panel {
  border: 1px solid var(--line);
  background: var(--glass);
  backdrop-filter: blur(18px);
  box-shadow: 0 22px 48px rgba(80, 70, 58, 0.08);
}

.admin-audit-page__snapshot {
  padding: 1.2rem;
}

.admin-audit-page__snapshot dl {
  display: grid;
  gap: 0.9rem;
  margin: 1rem 0 0;
}

.admin-audit-page__snapshot dd {
  margin: 0.35rem 0 0;
  font: 600 1.04rem/1.45 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.admin-audit-page__alert {
  margin: 1.25rem 0 0;
  color: #8d4747;
  font: 600 0.9rem/1.6 'Manrope', sans-serif;
}

.admin-audit-page__filters {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr)) auto;
  gap: 1rem;
  margin-top: 1.5rem;
  padding: 1.2rem;
}

.admin-audit-page__field {
  display: grid;
  gap: 0.7rem;
}

.admin-audit-page__field input,
.admin-audit-page__field select {
  min-height: 3rem;
  padding: 0 0.95rem;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.52);
  color: var(--ink);
  font: 500 0.95rem/1.5 'Manrope', sans-serif;
}

.admin-audit-page__field select {
  appearance: none;
}

.admin-audit-page__filter-actions {
  display: flex;
  gap: 0.8rem;
  align-items: end;
}

.admin-audit-page__primary,
.admin-audit-page__ghost {
  min-height: 3rem;
  padding: 0 1.15rem;
  font: 600 0.84rem/1 'Manrope', sans-serif;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  cursor: pointer;
  transition: transform 180ms ease, border-color 180ms ease, box-shadow 180ms ease;
}

.admin-audit-page__primary {
  border: none;
  background: linear-gradient(135deg, #64806e, #4d6657);
  color: #faf6f0;
  box-shadow: 0 18px 36px rgba(77, 102, 87, 0.24);
}

.admin-audit-page__ghost {
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.5);
  color: var(--ink);
}

.admin-audit-page__primary:hover,
.admin-audit-page__ghost:hover,
.admin-audit-card:hover {
  transform: translateY(-2px);
}

.admin-audit-page__list {
  display: grid;
  gap: 1rem;
  margin-top: 1.5rem;
}

.admin-audit-card {
  display: grid;
  gap: 1rem;
  padding: 1.2rem;
  transition: transform 180ms ease, border-color 180ms ease, box-shadow 180ms ease;
}

.admin-audit-card__header,
.admin-audit-card__footer {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
}

.admin-audit-card__header h2 {
  margin: 0.65rem 0 0;
  font: 600 1.42rem/1.32 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.admin-audit-card__header span {
  padding: 0.4rem 0.65rem;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.5);
}

.admin-audit-card__footer {
  padding-top: 1rem;
  border-top: 1px solid var(--line);
}

.admin-audit-page__status-panel {
  margin-top: 1.5rem;
  padding: 1.35rem;
}

@media (max-width: 980px) {
  .admin-audit-page,
  .admin-ai-page {
    padding: 1rem;
  }

  .admin-audit-page__masthead,
  .admin-audit-page__filters {
    grid-template-columns: 1fr;
  }

  .admin-audit-page__filter-actions,
  .admin-audit-card__header,
  .admin-audit-card__footer {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>

