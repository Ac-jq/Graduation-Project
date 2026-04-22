<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  cancelAdminAiTaskApi,
  confirmAdminAiTaskApi,
  fetchAdminAiTaskDetailApi,
  fetchAdminAiTasksApi,
  parseAdminAiTaskApi
} from '@/api/admin-ai-task'
import type { AdminAiTaskDetail, AdminAiTaskItem, AdminAiTaskSummary } from '@/api/types'
import { toErrorMessage } from '@/views/shared/page-logic'

const loading = ref(false)
const processing = ref(false)
const errorMessage = ref('')
const tasks = ref<AdminAiTaskSummary[]>([])
const currentTask = ref<AdminAiTaskDetail | null>(null)
const detailDialogVisible = ref(false)
const selectedItemIds = ref<number[]>([])
const currentPage = ref(1)
const pageSize = 10

const form = reactive({
  instruction: ''
})

const filters = reactive({
  keyword: '',
  taskType: '',
  status: '',
  startDate: '',
  endDate: ''
})

const taskTypeOptions = [
  { value: 'USER_CRUD', label: '用户操作' },
  { value: 'ACCOUNT_STATUS', label: '账号状态' },
  { value: 'COUNSELOR_CREATE', label: '咨询师创建' },
  { value: 'RESOURCE_STATUS', label: '资源状态' }
]

const statusOptions = [
  { value: 'PENDING', label: '待确认' },
  { value: 'EXECUTED', label: '已执行' },
  { value: 'CANCELED', label: '已取消' },
  { value: 'NEED_MORE_INFO', label: '待补充' }
]

const filteredTasks = computed(() => {
  return tasks.value.filter((task) => {
    const keyword = filters.keyword.trim().toLowerCase()
    const createdAt = task.createdAt ? new Date(task.createdAt) : null
    const startDate = filters.startDate ? new Date(`${filters.startDate}T00:00:00`) : null
    const endDate = filters.endDate ? new Date(`${filters.endDate}T23:59:59`) : null

    const matchesKeyword = !keyword
      || String(task.taskId).includes(keyword)
      || task.instructionText.toLowerCase().includes(keyword)
      || (task.summaryText ?? '').toLowerCase().includes(keyword)

    const matchesType = !filters.taskType || task.taskType === filters.taskType
    const matchesStatus = !filters.status || resolveTaskStatusKey(task) === filters.status
    const matchesStart = !startDate || (createdAt !== null && createdAt >= startDate)
    const matchesEnd = !endDate || (createdAt !== null && createdAt <= endDate)

    return matchesKeyword && matchesType && matchesStatus && matchesStart && matchesEnd
  })
})

const pagedTasks = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return filteredTasks.value.slice(start, start + pageSize)
})

const totalPages = computed(() => Math.max(1, Math.ceil(filteredTasks.value.length / pageSize)))
const executableItems = computed(() => currentTask.value?.items.filter((item) => item.executeStatus !== 'EXECUTED') ?? [])
const executableItemIds = computed(() => executableItems.value.map((item) => item.itemId))
const allSelected = computed(() =>
  executableItemIds.value.length > 0 && executableItemIds.value.every((id) => selectedItemIds.value.includes(id))
)

function formatDate(value: string | null | undefined): string {
  if (!value) {
    return '--'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return '--'
  }
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(date)
}

function resolveTaskType(type: string | null | undefined): string {
  switch (type) {
    case 'ACCOUNT_STATUS':
      return '账号状态'
    case 'COUNSELOR_CREATE':
      return '咨询师创建'
    case 'RESOURCE_STATUS':
      return '资源状态'
    case 'USER_CRUD':
      return '用户操作'
    default:
      return '待识别'
  }
}

function resolveTaskStatus(task: AdminAiTaskSummary | AdminAiTaskDetail): string {
  switch (resolveTaskStatusKey(task)) {
    case 'EXECUTED':
      return '已执行'
    case 'CANCELED':
      return '已取消'
    case 'PENDING':
      return '待确认'
    default:
      return '待补充'
  }
}

function resolveTaskStatusKey(task: AdminAiTaskSummary | AdminAiTaskDetail): string {
  if (task.executeStatus === 'EXECUTED') {
    return 'EXECUTED'
  }
  if (task.confirmStatus === 'CANCELED' || task.executeStatus === 'CANCELED') {
    return 'CANCELED'
  }
  if (task.parseStatus === 'READY') {
    return 'PENDING'
  }
  return 'NEED_MORE_INFO'
}

function resolveStatusClass(task: AdminAiTaskSummary | AdminAiTaskDetail): string {
  if (task.executeStatus === 'EXECUTED') {
    return 'is-success'
  }
  if (task.confirmStatus === 'CANCELED' || task.executeStatus === 'CANCELED') {
    return 'is-muted'
  }
  if (task.parseStatus === 'READY') {
    return 'is-warning'
  }
  return 'is-info'
}

function resolveOperationLabel(item: AdminAiTaskItem): string {
  switch (item.operationType) {
    case 'CREATE':
      return '新增'
    case 'DELETE':
      return '删除'
    case 'QUERY':
      return '查询'
    case 'PUBLISH':
      return '上架'
    case 'OFFLINE':
      return '下架'
    default:
      return '修改'
  }
}

function formatValue(value: string | null | undefined): string {
  return value && value.trim() ? value : '空'
}

function targetAccount(item: AdminAiTaskItem): string {
  return item.targetLabel?.split('/')[0]?.trim() || `#${item.targetId ?? item.itemId}`
}

function targetName(item: AdminAiTaskItem): string {
  return item.targetLabel?.split('/')[1]?.trim() || item.targetLabel || '未命名目标'
}

function canConfirm(task: AdminAiTaskSummary | AdminAiTaskDetail): boolean {
  return task.parseStatus === 'READY' && task.confirmStatus === 'PENDING'
}

function resetSelection(): void {
  selectedItemIds.value = executableItemIds.value
}

function toggleAll(): void {
  selectedItemIds.value = allSelected.value ? [] : [...executableItemIds.value]
}

function toggleItem(itemId: number): void {
  selectedItemIds.value = selectedItemIds.value.includes(itemId)
    ? selectedItemIds.value.filter((id) => id !== itemId)
    : [...selectedItemIds.value, itemId]
}

async function loadTasks(): Promise<void> {
  loading.value = true
  errorMessage.value = ''
  try {
    tasks.value = await fetchAdminAiTasksApi()
    currentPage.value = 1
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    loading.value = false
  }
}

function applyFilters(): void {
  currentPage.value = 1
}

function resetFilters(): void {
  filters.keyword = ''
  filters.taskType = ''
  filters.status = ''
  filters.startDate = ''
  filters.endDate = ''
  currentPage.value = 1
}

async function parseInstruction(): Promise<void> {
  const instruction = form.instruction.trim()
  if (!instruction) {
    ElMessage.warning('请输入管理员指令')
    return
  }
  processing.value = true
  errorMessage.value = ''
  try {
    const result = await parseAdminAiTaskApi({ instruction })
    form.instruction = ''
    currentTask.value = result.task
    detailDialogVisible.value = true
    resetSelection()
    await loadTasks()
    ElMessage.success(result.ready ? '已生成执行清单，请复核后确认' : result.message)
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    processing.value = false
  }
}

async function openTaskDetail(taskId: number): Promise<void> {
  processing.value = true
  errorMessage.value = ''
  try {
    currentTask.value = await fetchAdminAiTaskDetailApi(taskId)
    detailDialogVisible.value = true
    resetSelection()
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    processing.value = false
  }
}

async function confirmCurrentTask(): Promise<void> {
  if (!currentTask.value) {
    return
  }
  if (selectedItemIds.value.length === 0) {
    ElMessage.warning('请至少选择一条明细')
    return
  }
  processing.value = true
  errorMessage.value = ''
  try {
    currentTask.value = await confirmAdminAiTaskApi(currentTask.value.taskId, {
      selectedItemIds: selectedItemIds.value
    })
    detailDialogVisible.value = false
    await loadTasks()
    ElMessage.success('任务已按勾选范围执行')
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    processing.value = false
  }
}

async function cancelCurrentTask(): Promise<void> {
  if (!currentTask.value) {
    return
  }
  processing.value = true
  errorMessage.value = ''
  try {
    currentTask.value = await cancelAdminAiTaskApi(currentTask.value.taskId)
    detailDialogVisible.value = false
    await loadTasks()
    ElMessage.success('任务已取消')
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    processing.value = false
  }
}

function prevPage(): void {
  if (currentPage.value > 1) {
    currentPage.value--
  }
}

function nextPage(): void {
  if (currentPage.value < totalPages.value) {
    currentPage.value++
  }
}

onMounted(() => {
  void loadTasks()
})
</script>

<template>
  <section class="admin-ai-page">
    <div class="admin-ai-shell">
      <header class="admin-ai-header">
        <div>
          <h1>AI 运维任务</h1>
          <p>输入自然语言生成执行清单，管理员复核明细后再确认落库。</p>
        </div>
      </header>

      <p v-if="errorMessage" class="admin-ai-alert">{{ errorMessage }}</p>

      <section class="command-panel">
        <label class="command-field">
          <span>自然语言指令</span>
          <textarea
            v-model="form.instruction"
            rows="3"
            placeholder="例如：禁用三个月未登录的学生账号；或：查询学生账号 account: 20220353"
            @keydown.enter.ctrl.prevent="parseInstruction"
          />
        </label>
        <button class="primary-btn" type="button" :disabled="processing" @click="parseInstruction">
          {{ processing ? '生成中...' : '生成执行清单' }}
        </button>
      </section>

      <section class="filter-panel">
        <label class="filter-field filter-field--keyword">
          <span>内容关键词</span>
          <input
            v-model="filters.keyword"
            type="text"
            placeholder="任务编号 / 指令内容 / 摘要"
            @keyup.enter="applyFilters"
          >
        </label>

        <label class="filter-field">
          <span>任务类型</span>
          <select v-model="filters.taskType" @change="applyFilters">
            <option value="">全部类型</option>
            <option v-for="option in taskTypeOptions" :key="option.value" :value="option.value">
              {{ option.label }}
            </option>
          </select>
        </label>

        <label class="filter-field">
          <span>执行状态</span>
          <select v-model="filters.status" @change="applyFilters">
            <option value="">全部状态</option>
            <option v-for="option in statusOptions" :key="option.value" :value="option.value">
              {{ option.label }}
            </option>
          </select>
        </label>

        <label class="filter-field">
          <span>开始日期</span>
          <input v-model="filters.startDate" type="date" @change="applyFilters">
        </label>

        <label class="filter-field">
          <span>结束日期</span>
          <input v-model="filters.endDate" type="date" @change="applyFilters">
        </label>

        <div class="filter-actions">
          <button class="filter-btn filter-btn--primary" type="button" @click="applyFilters">筛选</button>
          <button class="filter-btn" type="button" @click="resetFilters">重置</button>
        </div>
      </section>

      <section class="table-panel">
        <div class="table-summary">
          <span>共 {{ filteredTasks.length }} 条任务<span v-if="filteredTasks.length !== tasks.length"> / 全部 {{ tasks.length }} 条</span></span>
          <span v-if="loading">正在加载...</span>
        </div>

        <div class="table-wrap">
          <table class="task-table">
            <thead>
              <tr>
                <th class="col-id">任务编号</th>
                <th>指令内容</th>
                <th class="col-type">任务类型</th>
                <th class="col-status">状态</th>
                <th class="col-time">创建时间</th>
                <th class="col-action">操作</th>
              </tr>
            </thead>
            <tbody v-if="!loading && pagedTasks.length">
              <tr v-for="task in pagedTasks" :key="task.taskId">
                <td>#{{ task.taskId }}</td>
                <td class="instruction-cell">
                  <strong>{{ task.summaryText || '待补充任务摘要' }}</strong>
                  <span>{{ task.instructionText }}</span>
                </td>
                <td>{{ resolveTaskType(task.taskType) }}</td>
                <td>
                  <span class="status-tag" :class="resolveStatusClass(task)">
                    {{ resolveTaskStatus(task) }}
                  </span>
                </td>
                <td>{{ formatDate(task.createdAt) }}</td>
                <td>
                  <button class="text-btn" type="button" @click="openTaskDetail(task.taskId)">
                    {{ canConfirm(task) ? '查看明细并确认' : '查看明细' }}
                  </button>
                </td>
              </tr>
            </tbody>
            <tbody v-else>
              <tr>
                <td colspan="6" class="empty-cell">
                  {{ loading ? '正在同步 AI 运维任务...' : '暂无 AI 运维任务' }}
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <nav class="pagination-nav" v-if="totalPages > 1">
          <button class="page-btn" type="button" :disabled="currentPage <= 1" @click="prevPage">上一页</button>
          <span>{{ currentPage }} / {{ totalPages }}</span>
          <button class="page-btn" type="button" :disabled="currentPage >= totalPages" @click="nextPage">下一页</button>
        </nav>
      </section>

      <el-dialog
        v-model="detailDialogVisible"
        title="执行清单明细"
        width="920px"
        class="ai-task-dialog"
        destroy-on-close
      >
        <template v-if="currentTask">
          <div class="detail-summary">
            <span class="status-tag" :class="resolveStatusClass(currentTask)">
              {{ resolveTaskStatus(currentTask) }}
            </span>
            <p>{{ currentTask.summaryText || currentTask.failureReason || currentTask.instructionText }}</p>
          </div>

          <div class="detail-table-wrap">
            <table class="detail-table">
              <thead>
                <tr>
                  <th class="check-col">
                    <label class="check-label">
                      <input type="checkbox" :checked="allSelected" :disabled="!canConfirm(currentTask)" @change="toggleAll">
                      <span>全选</span>
                    </label>
                  </th>
                  <th>目标账号</th>
                  <th>名称</th>
                  <th>操作</th>
                  <th>字段</th>
                  <th>旧值</th>
                  <th>新值</th>
                  <th>状态</th>
                </tr>
              </thead>
              <tbody v-if="currentTask.items.length">
                <tr v-for="item in currentTask.items" :key="item.itemId">
                  <td>
                    <input
                      type="checkbox"
                      :checked="selectedItemIds.includes(item.itemId)"
                      :disabled="!canConfirm(currentTask) || item.executeStatus === 'EXECUTED'"
                      @change="toggleItem(item.itemId)"
                    >
                  </td>
                  <td>{{ targetAccount(item) }}</td>
                  <td>{{ targetName(item) }}</td>
                  <td>{{ resolveOperationLabel(item) }}</td>
                  <td>{{ item.fieldName || '快照' }}</td>
                  <td>{{ formatValue(item.oldValue) }}</td>
                  <td>{{ formatValue(item.newValue) }}</td>
                  <td>{{ item.executeStatus || 'WAITING' }}</td>
                </tr>
              </tbody>
              <tbody v-else>
                <tr>
                  <td colspan="8" class="empty-cell">暂无明细。该任务可能需要补充信息后重新生成。</td>
                </tr>
              </tbody>
            </table>
          </div>
        </template>

        <template #footer>
          <div class="dialog-footer">
            <span v-if="currentTask && canConfirm(currentTask)">已选择 {{ selectedItemIds.length }} 条</span>
            <span v-else></span>
            <div class="dialog-actions">
              <el-button @click="detailDialogVisible = false">关闭</el-button>
              <el-button
                v-if="currentTask && currentTask.confirmStatus === 'PENDING'"
                :disabled="processing"
                @click="cancelCurrentTask"
              >
                取消任务
              </el-button>
              <el-button
                v-if="currentTask && canConfirm(currentTask)"
                type="primary"
                :loading="processing"
                @click="confirmCurrentTask"
              >
                确认执行
              </el-button>
            </div>
          </div>
        </template>
      </el-dialog>
    </div>
  </section>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700&family=Noto+Serif+SC:wght@500;600;700&display=swap');

.admin-ai-page {
  min-height: 100%;
  padding: 24px;
  background: #f7f8f7;
  color: #243029;
  font-family: 'Manrope', sans-serif;
}

.admin-ai-shell {
  display: grid;
  gap: 16px;
  max-width: 1280px;
  margin: 0 auto;
}

.admin-ai-header h1 {
  margin: 0;
  color: #1e2821;
  font: 700 26px/1.2 'Noto Serif SC', serif;
}

.admin-ai-header p {
  margin: 8px 0 0;
  color: #66736a;
  font-size: 14px;
}

.admin-ai-alert {
  margin: 0;
  padding: 12px 14px;
  border-radius: 8px;
  background: #fff3f1;
  color: #a5453d;
  font-size: 13px;
}

.command-panel,
.filter-panel,
.table-panel {
  border: 1px solid #e2e7e3;
  border-radius: 10px;
  background: #ffffff;
}

.command-panel {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 14px;
  align-items: end;
  padding: 16px;
}

.command-field {
  display: grid;
  gap: 8px;
}

.command-field span {
  color: #536158;
  font-size: 13px;
  font-weight: 700;
}

.command-field textarea {
  width: 100%;
  resize: vertical;
  min-height: 82px;
  box-sizing: border-box;
  border: 1px solid #d6ded8;
  border-radius: 8px;
  padding: 10px 12px;
  color: #243029;
  font: 500 14px/1.6 'Manrope', sans-serif;
  outline: none;
}

.command-field textarea:focus {
  border-color: #6b7f70;
  box-shadow: 0 0 0 3px rgba(107, 127, 112, 0.12);
}

.filter-panel {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: flex-end;
  padding: 16px;
}

.filter-field {
  display: grid;
  gap: 6px;
  min-width: 150px;
}

.filter-field--keyword {
  min-width: 280px;
  flex: 1;
}

.filter-field span {
  color: #536158;
  font-size: 12px;
  font-weight: 700;
}

.filter-field input,
.filter-field select {
  height: 36px;
  box-sizing: border-box;
  border: 1px solid #d6ded8;
  border-radius: 6px;
  background: #ffffff;
  color: #243029;
  padding: 0 10px;
  font: 500 13px/1 'Manrope', sans-serif;
  outline: none;
}

.filter-field input:focus,
.filter-field select:focus {
  border-color: #6b7f70;
  box-shadow: 0 0 0 3px rgba(107, 127, 112, 0.1);
}

.filter-actions {
  display: flex;
  gap: 8px;
}

.filter-btn {
  height: 36px;
  padding: 0 14px;
  border: 1px solid #d6ded8;
  border-radius: 6px;
  background: #ffffff;
  color: #2f4c3a;
  font-weight: 700;
  cursor: pointer;
}

.filter-btn--primary {
  border-color: #2f4c3a;
  background: #2f4c3a;
  color: #ffffff;
}

.primary-btn,
.page-btn {
  height: 38px;
  border: 1px solid #2f4c3a;
  border-radius: 6px;
  background: #2f4c3a;
  color: #ffffff;
  font-weight: 700;
  cursor: pointer;
}

.primary-btn {
  padding: 0 18px;
}

.primary-btn:disabled,
.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.table-panel {
  padding: 16px;
}

.table-summary {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
  color: #66736a;
  font-size: 13px;
}

.table-wrap,
.detail-table-wrap {
  overflow: auto;
  border: 1px solid #e2e7e3;
  border-radius: 8px;
}

.task-table,
.detail-table {
  width: 100%;
  border-collapse: collapse;
  background: #ffffff;
}

.task-table {
  min-width: 980px;
}

.detail-table {
  min-width: 860px;
}

.task-table th,
.task-table td,
.detail-table th,
.detail-table td {
  padding: 12px 10px;
  border-bottom: 1px solid #edf1ee;
  text-align: left;
  font-size: 13px;
  vertical-align: middle;
}

.task-table th,
.detail-table th {
  background: #fafbf9;
  color: #526058;
  font-weight: 700;
  white-space: nowrap;
}

.task-table tr:hover {
  background: #fbfcfb;
}

.col-id {
  width: 100px;
}

.col-type {
  width: 120px;
}

.col-status {
  width: 100px;
}

.col-time {
  width: 170px;
}

.col-action {
  width: 150px;
}

.instruction-cell {
  display: grid;
  gap: 4px;
}

.instruction-cell strong {
  color: #243029;
}

.instruction-cell span {
  color: #68766d;
}

.status-tag {
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 8px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.status-tag.is-success {
  background: #edf7f0;
  color: #26734d;
}

.status-tag.is-warning {
  background: #fff7e8;
  color: #9a6a1d;
}

.status-tag.is-info {
  background: #eef4f7;
  color: #3d6675;
}

.status-tag.is-muted {
  background: #f0f2f1;
  color: #7a827d;
}

.text-btn {
  border: none;
  background: transparent;
  color: #2f4c3a;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}

.empty-cell {
  padding: 34px 12px !important;
  color: #829087;
  text-align: center !important;
}

.pagination-nav {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 14px;
  margin-top: 16px;
}

.pagination-nav span {
  color: #66736a;
  font-size: 13px;
}

.page-btn {
  padding: 0 14px;
  background: #ffffff;
  color: #2f4c3a;
}

.detail-summary {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  margin-bottom: 14px;
}

.detail-summary p {
  margin: 0;
  color: #536158;
  line-height: 1.7;
}

.check-col {
  width: 88px;
}

.check-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.dialog-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.dialog-footer > span {
  color: #66736a;
  font-size: 13px;
}

.dialog-actions {
  display: flex;
  gap: 10px;
}

@media (max-width: 820px) {
  .admin-ai-page {
    padding: 12px;
  }

  .command-panel {
    grid-template-columns: 1fr;
  }
}
</style>
