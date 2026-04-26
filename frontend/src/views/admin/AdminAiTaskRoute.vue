<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElButton, ElDialog, ElMessage, ElMessageBox } from 'element-plus'
import {
  cancelAdminAiTaskApi,
  confirmAdminAiTaskApi,
  fetchAdminAiTaskDetailApi,
  fetchAdminAiTasksApi,
  parseAdminAiTaskApi
} from '@/api/admin-ai-task'
import { deleteAdminUsersApi, disableUserApi, enableUserApi, updateAdminUserApi } from '@/api/user'
import type {
  AdminAiTaskDetail,
  AdminAiTaskItem,
  AdminAiTaskSummary,
  UpdateAdminUserRequest
} from '@/api/types'
import { toErrorMessage } from '@/views/shared/page-logic'

type WorkflowStatus =
  | 'NEED_CLARIFICATION'
  | 'QUERY_RESULT'
  | 'PENDING_DELETE'
  | 'PENDING_UPDATE'
  | 'SUCCESS'
  | 'CANCELED'
  | 'NOT_FOUND'
  | ''

interface QuickEditOption {
  value: string
  label: string
}

const loading = ref(false)
const processing = ref(false)
const errorMessage = ref('')

const tasks = ref<AdminAiTaskSummary[]>([])
const currentTask = ref<AdminAiTaskDetail | null>(null)
const detailDialogVisible = ref(false)
const selectedItemIds = ref<number[]>([])
const selectedQueryUserIds = ref<number[]>([])
const currentPage = ref(1)
const pageSize = 10

const quickEditDialogVisible = ref(false)
const quickEditTarget = ref<AdminAiTaskItem | null>(null)

const form = reactive({
  instruction: ''
})

const quickEditForm = reactive({
  fieldName: '',
  newValue: ''
})

const filterDraft = reactive({
  keyword: '',
  taskType: '',
  status: '',
  startDate: '',
  endDate: ''
})

const activeFilters = reactive({
  keyword: '',
  taskType: '',
  status: '',
  startDate: '',
  endDate: ''
})

const examples = [
  '查询姓名为庄文轩的学生信息',
  '查询人工智能学院 2026 级学生',
  '删除学号 20269999 的学生',
  '把学号 20269999 的学生年级改成 2025'
]

const taskTypeOptions = [
  { value: 'USER_CRUD', label: '用户操作' },
  { value: 'ACCOUNT_STATUS', label: '账号状态' },
  { value: 'COUNSELOR_CREATE', label: '咨询师创建' },
  { value: 'RESOURCE_STATUS', label: '资源状态' }
]

const statusOptions = [
  { value: 'NEED_CLARIFICATION', label: '待补充' },
  { value: 'QUERY_RESULT', label: '查询结果' },
  { value: 'NOT_FOUND', label: '未查找到' },
  { value: 'PENDING_DELETE', label: '待确认删除' },
  { value: 'PENDING_UPDATE', label: '待确认修改' },
  { value: 'SUCCESS', label: '已执行' },
  { value: 'CANCELED', label: '已取消' }
]

const editableFieldCatalog: QuickEditOption[] = [
  { value: 'displayName', label: '显示名' },
  { value: 'realName', label: '真实姓名' },
  { value: 'account', label: '账号' },
  { value: 'studentNo', label: '学号' },
  { value: 'counselorNo', label: '工号' },
  { value: 'college', label: '学院' },
  { value: 'grade', label: '年级' },
  { value: 'status', label: '状态' }
]

const currentWorkflowStatus = computed<WorkflowStatus>(() => {
  const task = currentTask.value
  if (!task) {
    return ''
  }
  if (task.agentStatus === 'CANCELED') {
    return 'CANCELED'
  }
  return (task.workflowStatus as WorkflowStatus) || ''
})

const isQueryResultTask = computed(() => currentWorkflowStatus.value === 'QUERY_RESULT' || currentWorkflowStatus.value === 'NOT_FOUND')
const isPendingDeleteTask = computed(() => currentWorkflowStatus.value === 'PENDING_DELETE')
const isPendingUpdateTask = computed(() => currentWorkflowStatus.value === 'PENDING_UPDATE')
const isNeedClarificationTask = computed(() => currentWorkflowStatus.value === 'NEED_CLARIFICATION')
const isSuccessTask = computed(() => currentWorkflowStatus.value === 'SUCCESS')

const queryResultItems = computed(() => (isQueryResultTask.value ? currentTask.value?.items ?? [] : []))
const deletePreviewItems = computed(() => (isPendingDeleteTask.value ? currentTask.value?.items ?? [] : []))
const updatePreviewItems = computed(() => (isPendingUpdateTask.value ? currentTask.value?.items ?? [] : []))

const actionableItemIds = computed(() => {
  const task = currentTask.value
  if (!task) {
    return []
  }
  return task.items
    .filter((item) => item.executeStatus !== 'EXECUTED' && item.executeStatus !== 'CANCELED')
    .map((item) => item.itemId)
})

const allSelected = computed(() => {
  return actionableItemIds.value.length > 0
    && actionableItemIds.value.every((itemId) => selectedItemIds.value.includes(itemId))
})

const quickEditFieldOptions = computed<QuickEditOption[]>(() => {
  const item = quickEditTarget.value
  if (!item) {
    return []
  }
  return editableFieldCatalog.filter((option) => {
    if (option.value === 'studentNo' || option.value === 'college' || option.value === 'grade') {
      return Boolean(item.studentNo)
    }
    if (option.value === 'counselorNo') {
      return Boolean(item.counselorNo)
    }
    return true
  })
})

const allQuerySelected = computed(() => {
  const targetIds = queryResultItems.value
    .map((item) => item.targetId)
    .filter((value): value is number => typeof value === 'number')
  return targetIds.length > 0 && targetIds.every((id) => selectedQueryUserIds.value.includes(id))
})

const pendingUpdateSummary = computed(() => {
  if (!updatePreviewItems.value.length) {
    return '请确认以下字段变更后再执行。'
  }
  if (updatePreviewItems.value.length === 1) {
    const item = updatePreviewItems.value[0]
    return `确认将 ${targetName(item)} 的 ${resolveFieldLabel(item.fieldName)} 从 ${formatValue(item.oldValue)} 修改为 ${formatValue(item.newValue)} 吗？`
  }
  return `本次将修改 ${updatePreviewItems.value.length} 项字段，请逐项复核后确认执行。`
})

const filteredTasks = computed(() => {
  return tasks.value.filter((task) => {
    const keyword = activeFilters.keyword.trim().toLowerCase()
    const createdAt = task.createdAt ? new Date(task.createdAt) : null
    const startDate = activeFilters.startDate ? new Date(`${activeFilters.startDate}T00:00:00`) : null
    const endDate = activeFilters.endDate ? new Date(`${activeFilters.endDate}T23:59:59`) : null

    const summaryText = (task.summaryText ?? '').toLowerCase()
    const pendingPrompt = (task.pendingPrompt ?? '').toLowerCase()
    const workflowStatus = resolveFilterStatus(task)

    const matchesKeyword = !keyword
      || String(task.taskId).includes(keyword)
      || task.instructionText.toLowerCase().includes(keyword)
      || summaryText.includes(keyword)
      || pendingPrompt.includes(keyword)

    const matchesType = !activeFilters.taskType || task.taskType === activeFilters.taskType
    const matchesStatus = !activeFilters.status || workflowStatus === activeFilters.status
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

function resolveFilterStatus(task: AdminAiTaskSummary | AdminAiTaskDetail): WorkflowStatus {
  if (task.agentStatus === 'CANCELED') {
    return 'CANCELED'
  }
  return (task.workflowStatus as WorkflowStatus) || ''
}

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

function formatValue(value: string | null | undefined): string {
  if (!value || !value.trim()) {
    return '--'
  }
  const upperValue = value.toUpperCase()
  if (upperValue === 'ACTIVE') {
    return '启用'
  }
  if (upperValue === 'DISABLED') {
    return '禁用'
  }
  return value
}

function normalizeStatusInput(value: string): 'ACTIVE' | 'DISABLED' {
  const normalized = value.trim().toUpperCase()
  if (normalized === 'ACTIVE' || value.trim() === '启用' || value.trim() === '正常') {
    return 'ACTIVE'
  }
  return 'DISABLED'
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
  const workflowStatus = resolveFilterStatus(task)
  switch (workflowStatus) {
    case 'NEED_CLARIFICATION':
      return '待补充'
    case 'QUERY_RESULT':
      return '查询结果'
    case 'NOT_FOUND':
      return '未查找到'
    case 'PENDING_DELETE':
      return '待确认删除'
    case 'PENDING_UPDATE':
      return '待确认修改'
    case 'SUCCESS':
      return '已执行'
    case 'CANCELED':
      return '已取消'
    default:
      return '处理中'
  }
}

function resolveStatusClass(task: AdminAiTaskSummary | AdminAiTaskDetail): string {
  const workflowStatus = resolveFilterStatus(task)
  switch (workflowStatus) {
    case 'NEED_CLARIFICATION':
      return 'is-info'
    case 'QUERY_RESULT':
      return 'is-neutral'
    case 'NOT_FOUND':
      return 'is-muted'
    case 'PENDING_DELETE':
      return 'is-danger'
    case 'PENDING_UPDATE':
      return 'is-warning'
    case 'SUCCESS':
      return 'is-success'
    case 'CANCELED':
      return 'is-muted'
    default:
      return 'is-muted'
  }
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

function resolveFieldLabel(fieldName: string | null | undefined): string {
  switch (fieldName) {
    case 'account':
      return '账号'
    case 'displayName':
      return '显示名'
    case 'realName':
      return '真实姓名'
    case 'studentNo':
      return '学号'
    case 'counselorNo':
      return '工号'
    case 'status':
      return '状态'
    case 'college':
      return '学院'
    case 'grade':
      return '年级'
    case 'snapshot':
      return '快照'
    default:
      return fieldName || '字段'
  }
}

function resolveItemExecuteStatus(status: string | null | undefined): string {
  switch (status) {
    case 'EXECUTED':
      return '已执行'
    case 'CANCELED':
      return '已取消'
    case 'FAILED':
      return '执行失败'
    default:
      return '待执行'
  }
}

function resolveOpenActionLabel(task: AdminAiTaskSummary | AdminAiTaskDetail): string {
  if (resolveFilterStatus(task) === 'NEED_CLARIFICATION') {
    return '查看提示'
  }
  if (resolveFilterStatus(task) === 'PENDING_DELETE' || resolveFilterStatus(task) === 'PENDING_UPDATE') {
    return '查看并确认'
  }
  return '查看详情'
}

function canConfirm(task: AdminAiTaskSummary | AdminAiTaskDetail): boolean {
  const workflowStatus = resolveFilterStatus(task)
  return task.confirmStatus === 'PENDING'
    && (workflowStatus === 'PENDING_DELETE' || workflowStatus === 'PENDING_UPDATE')
}

function confirmButtonText(): string {
  if (isPendingDeleteTask.value) {
    return '确认删除勾选项'
  }
  if (isPendingUpdateTask.value) {
    return '确认执行修改'
  }
  return '确认执行'
}

function targetAccount(item: AdminAiTaskItem): string {
  return item.account || item.targetLabel?.split('/')[0]?.trim() || `#${item.targetId ?? item.itemId}`
}

function targetName(item: AdminAiTaskItem): string {
  return item.realName
    || item.displayName
    || item.targetLabel?.split('/')[1]?.trim()
    || item.targetLabel
    || '未命名目标'
}

function targetExtra(item: AdminAiTaskItem): string {
  const parts = [
    item.studentNo ? `学号 ${item.studentNo}` : '',
    item.counselorNo ? `工号 ${item.counselorNo}` : '',
    item.college || '',
    item.grade || ''
  ].filter(Boolean)
  return parts.join(' / ')
}

function buildIdentityText(item: AdminAiTaskItem): string {
  if (item.studentNo) {
    return `学号 ${item.studentNo}`
  }
  if (item.counselorNo) {
    return `工号 ${item.counselorNo}`
  }
  if (item.account) {
    return `账号 ${item.account}`
  }
  return `编号 ${item.targetId ?? item.itemId}`
}

function syncSelection(): void {
  selectedQueryUserIds.value = []
  if (!currentTask.value) {
    selectedItemIds.value = []
    return
  }
  if (isPendingDeleteTask.value || isPendingUpdateTask.value) {
    selectedItemIds.value = [...actionableItemIds.value]
    return
  }
  selectedItemIds.value = []
}

function toggleAll(): void {
  selectedItemIds.value = allSelected.value ? [] : [...actionableItemIds.value]
}

function toggleItem(itemId: number): void {
  selectedItemIds.value = selectedItemIds.value.includes(itemId)
    ? selectedItemIds.value.filter((id) => id !== itemId)
    : [...selectedItemIds.value, itemId]
}

function toggleAllQueryRows(): void {
  if (allQuerySelected.value) {
    selectedQueryUserIds.value = []
    return
  }
  selectedQueryUserIds.value = queryResultItems.value
    .map((item) => item.targetId)
    .filter((value): value is number => typeof value === 'number')
}

function toggleQueryRow(targetId: number): void {
  selectedQueryUserIds.value = selectedQueryUserIds.value.includes(targetId)
    ? selectedQueryUserIds.value.filter((id) => id !== targetId)
    : [...selectedQueryUserIds.value, targetId]
}

function buildDirectUpdatePayload(item: AdminAiTaskItem, fieldName: string, newValue: string): UpdateAdminUserRequest {
  const payload: UpdateAdminUserRequest = {
    account: item.account || targetAccount(item),
    displayName: item.displayName || item.realName || targetName(item),
    realName: item.realName || null,
    studentNo: item.studentNo || null,
    counselorNo: item.counselorNo || null,
    college: item.college || null,
    grade: item.grade || null,
    phone: null,
    password: null
  }

  switch (fieldName) {
    case 'account':
      payload.account = newValue
      break
    case 'displayName':
      payload.displayName = newValue
      break
    case 'realName':
      payload.realName = newValue
      break
    case 'studentNo':
      payload.studentNo = newValue
      break
    case 'counselorNo':
      payload.counselorNo = newValue
      break
    case 'college':
      payload.college = newValue
      break
    case 'grade':
      payload.grade = newValue
      break
    default:
      break
  }

  return payload
}

function applyFilters(): void {
  activeFilters.keyword = filterDraft.keyword
  activeFilters.taskType = filterDraft.taskType
  activeFilters.status = filterDraft.status
  activeFilters.startDate = filterDraft.startDate
  activeFilters.endDate = filterDraft.endDate
  currentPage.value = 1
}

function resetFilters(): void {
  filterDraft.keyword = ''
  filterDraft.taskType = ''
  filterDraft.status = ''
  filterDraft.startDate = ''
  filterDraft.endDate = ''
  activeFilters.keyword = ''
  activeFilters.taskType = ''
  activeFilters.status = ''
  activeFilters.startDate = ''
  activeFilters.endDate = ''
  currentPage.value = 1
  void loadTasks()
}

function useExample(example: string): void {
  form.instruction = example
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

async function parseInstruction(): Promise<void> {
  const instruction = form.instruction.trim()
  if (!instruction) {
    ElMessage.warning('请输入自然语言指令')
    return
  }
  processing.value = true
  errorMessage.value = ''
  try {
    const result = await parseAdminAiTaskApi({ instruction })
    await loadTasks()

    if (!result.ready || result.task.workflowStatus === 'NEED_CLARIFICATION') {
      currentTask.value = null
      detailDialogVisible.value = false
      await ElMessageBox.alert(result.message, '缺少参数', {
        confirmButtonText: '继续补充'
      })
      return
    }

    currentTask.value = result.task
    detailDialogVisible.value = true
    syncSelection()
    form.instruction = ''
    ElMessage.success(result.message)
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
    syncSelection()
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

  const deleteMode = isPendingDeleteTask.value
  const updateMode = isPendingUpdateTask.value
  const selectedIds = (deleteMode || updateMode)
    ? [...selectedItemIds.value]
    : (selectedItemIds.value.length ? selectedItemIds.value : [...actionableItemIds.value])

  if ((deleteMode || updateMode) && selectedIds.length === 0) {
    ElMessage.warning('请至少勾选一项后再执行')
    return
  }

  processing.value = true
  errorMessage.value = ''
  try {
    if (deleteMode) {
      const deleteIds = deletePreviewItems.value
        .filter((item) => selectedIds.includes(item.itemId))
        .map((item) => item.targetId)
        .filter((value): value is number => typeof value === 'number')

      if (!deleteIds.length) {
        ElMessage.warning('当前没有可删除的用户')
        return
      }

      await deleteAdminUsersApi(deleteIds)
      currentTask.value = {
        ...currentTask.value,
        workflowStatus: 'SUCCESS',
        confirmStatus: 'CONFIRMED',
        executeStatus: 'EXECUTED',
        items: currentTask.value.items.map((item) =>
          selectedIds.includes(item.itemId) ? { ...item, executeStatus: 'EXECUTED' } : item
        )
      }
    } else if (updateMode) {
      const updateItems = updatePreviewItems.value.filter((item) => selectedIds.includes(item.itemId))
      for (const item of updateItems) {
        if (typeof item.targetId !== 'number' || !item.fieldName) {
          continue
        }
        if (item.fieldName === 'status') {
          const statusValue = normalizeStatusInput(String(item.newValue ?? ''))
          if (statusValue === 'ACTIVE') {
            await enableUserApi(item.targetId)
          } else {
            await disableUserApi(item.targetId)
          }
        } else {
          const payload = buildDirectUpdatePayload(item, item.fieldName, String(item.newValue ?? ''))
          await updateAdminUserApi(item.targetId, payload)
        }
      }

      currentTask.value = {
        ...currentTask.value,
        workflowStatus: 'SUCCESS',
        confirmStatus: 'CONFIRMED',
        executeStatus: 'EXECUTED',
        items: currentTask.value.items.map((item) =>
          selectedIds.includes(item.itemId) ? { ...item, executeStatus: 'EXECUTED' } : item
        )
      }
    } else {
      currentTask.value = await confirmAdminAiTaskApi(currentTask.value.taskId, {
        selectedItemIds: selectedIds
      })
    }

    await loadTasks()
    syncSelection()
    ElMessage.success(deleteMode ? '已完成删除' : '已完成修改')
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
    await loadTasks()
    syncSelection()
    ElMessage.success('任务已取消')
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    processing.value = false
  }
}

async function startDeleteFromQuery(item: AdminAiTaskItem): Promise<void> {
  if (typeof item.targetId !== 'number') {
    return
  }
  if (!window.confirm(`确认删除 ${targetName(item)} 吗？`)) {
    return
  }
  processing.value = true
  errorMessage.value = ''
  try {
    await deleteAdminUsersApi([item.targetId])
    if (currentTask.value) {
      currentTask.value = {
        ...currentTask.value,
        items: currentTask.value.items.filter((entry) => entry.itemId !== item.itemId)
      }
    }
    syncSelection()
    await loadTasks()
    ElMessage.success('删除成功')
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    processing.value = false
  }
}

function openQuickEdit(item: AdminAiTaskItem): void {
  quickEditTarget.value = item
  quickEditDialogVisible.value = true
  quickEditForm.fieldName = quickEditFieldOptions.value[0]?.value ?? ''
  quickEditForm.newValue = ''
}

function closeQuickEdit(): void {
  quickEditDialogVisible.value = false
  quickEditTarget.value = null
  quickEditForm.fieldName = ''
  quickEditForm.newValue = ''
}

async function submitQuickEdit(): Promise<void> {
  if (!quickEditTarget.value) {
    return
  }

  const fieldName = quickEditForm.fieldName.trim()
  const newValue = quickEditForm.newValue.trim()
  if (!fieldName || !newValue) {
    ElMessage.warning('请选择字段并填写新的值')
    return
  }

  processing.value = true
  errorMessage.value = ''
  try {
    if (typeof quickEditTarget.value.targetId !== 'number') {
      return
    }
    if (fieldName === 'status') {
      const statusValue = normalizeStatusInput(newValue)
      if (statusValue === 'ACTIVE') {
        await enableUserApi(quickEditTarget.value.targetId)
      } else {
        await disableUserApi(quickEditTarget.value.targetId)
      }
    } else {
      const payload = buildDirectUpdatePayload(quickEditTarget.value, fieldName, newValue)
      await updateAdminUserApi(quickEditTarget.value.targetId, payload)
    }

    closeQuickEdit()
    await loadTasks()
    ElMessage.success('保存成功')
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    processing.value = false
  }
}

async function deleteSelectedQueryUsers(): Promise<void> {
  if (!selectedQueryUserIds.value.length) {
    ElMessage.warning('请先勾选要删除的用户')
    return
  }
  if (!window.confirm(`确认删除已勾选的 ${selectedQueryUserIds.value.length} 名用户吗？`)) {
    return
  }

  processing.value = true
  errorMessage.value = ''
  try {
    await deleteAdminUsersApi(selectedQueryUserIds.value)
    if (currentTask.value) {
      currentTask.value = {
        ...currentTask.value,
        items: currentTask.value.items.filter(
          (item) => typeof item.targetId !== 'number' || !selectedQueryUserIds.value.includes(item.targetId)
        )
      }
    }
    selectedQueryUserIds.value = []
    await loadTasks()
    ElMessage.success('批量删除成功')
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
        <div class="header-copy">
          <span class="eyebrow">Admin Operations</span>
          <h1>AI 运维助手</h1>
          <p>当前页面已回归单次解析模式。管理员一次性输入完整指令，系统只做当前这一句的解析；查询结果、删除预览和修改预览统一在弹窗中展示，后续编辑与删除直接走后台接口，不再依赖多轮上下文。</p>
        </div>
      </header>

      <p v-if="errorMessage" class="admin-ai-alert">{{ errorMessage }}</p>

      <section class="command-panel">
        <label class="command-field">
          <span>自然语言指令</span>
          <textarea
            v-model="form.instruction"
            rows="3"
            placeholder="例如：查询姓名为庄文轩的学生信息；删除学号 20269999 的学生；把学号 20269999 的学生年级改成 2025"
            @keydown.enter.ctrl.prevent="parseInstruction"
          />
          <div class="example-list" aria-label="常用中文指令示例">
            <button
              v-for="example in examples"
              :key="example"
              class="example-btn"
              type="button"
              @click="useExample(example)"
            >
              {{ example }}
            </button>
          </div>
        </label>

        <button class="primary-btn" type="button" :disabled="processing" @click="parseInstruction">
          {{ processing ? '解析中...' : '生成执行结果' }}
        </button>
      </section>

      <section class="filter-panel">
        <label class="filter-field filter-field--keyword">
          <span>内容关键词</span>
          <input
            v-model="filterDraft.keyword"
            type="text"
            placeholder="任务编号 / 指令内容 / 摘要 / 补充提示"
            @keyup.enter="applyFilters"
          >
        </label>

        <label class="filter-field">
          <span>任务类型</span>
          <select v-model="filterDraft.taskType">
            <option value="">全部类型</option>
            <option v-for="option in taskTypeOptions" :key="option.value" :value="option.value">
              {{ option.label }}
            </option>
          </select>
        </label>

        <label class="filter-field">
          <span>任务状态</span>
          <select v-model="filterDraft.status">
            <option value="">全部状态</option>
            <option v-for="option in statusOptions" :key="option.value" :value="option.value">
              {{ option.label }}
            </option>
          </select>
        </label>

        <label class="filter-field">
          <span>开始日期</span>
          <input v-model="filterDraft.startDate" type="date">
        </label>

        <label class="filter-field">
          <span>结束日期</span>
          <input v-model="filterDraft.endDate" type="date">
        </label>

        <div class="filter-actions">
          <button class="filter-btn filter-btn--primary" type="button" @click="applyFilters">查询</button>
          <button class="filter-btn" type="button" @click="resetFilters">重置</button>
        </div>
      </section>

      <section class="table-panel">
        <div class="table-summary">
          <span>当前共 {{ filteredTasks.length }} 条任务<span v-if="filteredTasks.length !== tasks.length"> / 全部 {{ tasks.length }} 条</span></span>
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
                  <strong>{{ task.summaryText || task.pendingPrompt || '待补充摘要' }}</strong>
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
                    {{ resolveOpenActionLabel(task) }}
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
        title="任务详情"
        width="1120px"
        class="ai-task-dialog"
        destroy-on-close
      >
        <template v-if="currentTask">
          <section class="detail-state-card">
            <span class="status-tag" :class="resolveStatusClass(currentTask)">
              {{ resolveTaskStatus(currentTask) }}
            </span>
            <div class="detail-state-copy">
              <h3>{{ currentTask.summaryText || currentTask.pendingPrompt || currentTask.failureReason || '任务详情' }}</h3>
              <p>{{ currentTask.instructionText }}</p>
            </div>
          </section>

          <section v-if="isNeedClarificationTask" class="workflow-panel">
            <div class="panel-heading">
              <h3>缺少参数</h3>
              <p>{{ currentTask.pendingPrompt || currentTask.failureReason || '请回到顶部输入框补充完整条件后重新提交。系统不会清空你原来的输入内容。' }}</p>
            </div>
          </section>

          <section v-else-if="isQueryResultTask" class="workflow-panel">
            <div class="panel-heading panel-heading--split">
              <div>
                <h3>{{ queryResultItems.length ? '查询结果' : '未查找到匹配用户' }}</h3>
                <p>{{ queryResultItems.length ? '以下是本次命中的学生结果。你可以直接在这里编辑或删除。' : '数据库中未查找到符合该条件的学生。' }}</p>
              </div>
              <button class="filter-btn" type="button" :disabled="processing || !selectedQueryUserIds.length" @click="deleteSelectedQueryUsers">
                批量删除
              </button>
            </div>
            <div class="detail-table-wrap">
              <table class="detail-table">
                <thead>
                  <tr>
                    <th class="check-col">
                      <label class="check-label">
                        <input type="checkbox" :checked="allQuerySelected" @change="toggleAllQueryRows">
                        <span>全选</span>
                      </label>
                    </th>
                    <th>账号</th>
                    <th>姓名</th>
                    <th>补充信息</th>
                    <th>结果摘要</th>
                    <th class="operation-col">操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-if="!queryResultItems.length">
                    <td colspan="6" class="empty-cell">数据库中未查找到符合该条件的学生</td>
                  </tr>
                  <tr v-for="item in queryResultItems" :key="item.itemId">
                    <td>
                      <input
                        v-if="typeof item.targetId === 'number'"
                        type="checkbox"
                        :checked="selectedQueryUserIds.includes(item.targetId)"
                        @change="toggleQueryRow(item.targetId)"
                      >
                    </td>
                    <td>{{ targetAccount(item) }}</td>
                    <td>{{ targetName(item) }}</td>
                    <td>{{ targetExtra(item) || '--' }}</td>
                    <td>{{ formatValue(item.newValue) }}</td>
                    <td class="operation-cell">
                      <button class="text-btn" type="button" :disabled="processing" @click="openQuickEdit(item)">编辑</button>
                      <button class="text-btn text-btn--danger" type="button" :disabled="processing" @click="startDeleteFromQuery(item)">删除</button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </section>

          <section v-else-if="isPendingDeleteTask" class="workflow-panel">
            <div class="panel-heading panel-heading--split">
              <div>
                <h3>删除预览</h3>
                <p>系统已先完成命中查询。只有勾选后的记录才会真正执行删除。</p>
              </div>
              <button class="filter-btn" type="button" @click="toggleAll">
                {{ allSelected ? '取消全选' : '全选' }}
              </button>
            </div>
            <div class="detail-table-wrap">
              <table class="detail-table">
                <thead>
                  <tr>
                    <th class="check-col">
                      <label class="check-label">
                        <input
                          type="checkbox"
                          :checked="allSelected"
                          :disabled="!canConfirm(currentTask)"
                          @change="toggleAll"
                        >
                        <span>全选</span>
                      </label>
                    </th>
                    <th>账号</th>
                    <th>姓名</th>
                    <th>补充信息</th>
                    <th>操作类型</th>
                    <th>快照</th>
                    <th>状态</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="item in deletePreviewItems" :key="item.itemId">
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
                    <td>{{ targetExtra(item) || '--' }}</td>
                    <td>{{ resolveOperationLabel(item) }}</td>
                    <td class="snapshot-cell">{{ formatValue(item.oldValue) }}</td>
                    <td>{{ resolveItemExecuteStatus(item.executeStatus) }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </section>

          <section v-else-if="isPendingUpdateTask" class="workflow-panel">
            <div class="panel-heading">
              <h3>修改确认</h3>
              <p>{{ pendingUpdateSummary }}</p>
            </div>
            <div class="update-highlight">
              <span v-for="item in updatePreviewItems" :key="item.itemId" class="update-chip">
                {{ targetName(item) }} / {{ resolveFieldLabel(item.fieldName) }}：{{ formatValue(item.oldValue) }} → {{ formatValue(item.newValue) }}
              </span>
            </div>
            <div class="detail-table-wrap">
              <table class="detail-table">
                <thead>
                  <tr>
                    <th>账号</th>
                    <th>姓名</th>
                    <th>补充信息</th>
                    <th>字段</th>
                    <th>旧值</th>
                    <th>新值</th>
                    <th>状态</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="item in updatePreviewItems" :key="item.itemId">
                    <td>{{ targetAccount(item) }}</td>
                    <td>{{ targetName(item) }}</td>
                    <td>{{ targetExtra(item) || '--' }}</td>
                    <td>{{ resolveFieldLabel(item.fieldName) }}</td>
                    <td>{{ formatValue(item.oldValue) }}</td>
                    <td>{{ formatValue(item.newValue) }}</td>
                    <td>{{ resolveItemExecuteStatus(item.executeStatus) }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </section>

          <section v-else-if="isSuccessTask" class="workflow-panel">
            <div class="panel-heading">
              <h3>执行结果</h3>
              <p>当前任务已经执行完成，以下是落库后的结果明细。</p>
            </div>
            <div class="detail-table-wrap">
              <table class="detail-table">
                <thead>
                  <tr>
                    <th>账号</th>
                    <th>姓名</th>
                    <th>补充信息</th>
                    <th>操作</th>
                    <th>字段</th>
                    <th>新值</th>
                    <th>状态</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="item in currentTask.items" :key="item.itemId">
                    <td>{{ targetAccount(item) }}</td>
                    <td>{{ targetName(item) }}</td>
                    <td>{{ targetExtra(item) || '--' }}</td>
                    <td>{{ resolveOperationLabel(item) }}</td>
                    <td>{{ resolveFieldLabel(item.fieldName) }}</td>
                    <td>{{ formatValue(item.newValue) }}</td>
                    <td>{{ resolveItemExecuteStatus(item.executeStatus) }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </section>
        </template>

        <template #footer>
          <div class="dialog-footer">
            <span v-if="isPendingDeleteTask">当前勾选 {{ selectedItemIds.length }} 项</span>
            <span v-else-if="isPendingUpdateTask">当前将执行 {{ actionableItemIds.length }} 项修改</span>
            <span v-else-if="isNeedClarificationTask">当前任务缺少必要参数</span>
            <span v-else></span>

            <div class="dialog-actions">
              <ElButton @click="detailDialogVisible = false">关闭</ElButton>
              <ElButton
                v-if="currentTask && currentTask.confirmStatus === 'PENDING'"
                :disabled="processing"
                @click="cancelCurrentTask"
              >
                取消任务
              </ElButton>
              <ElButton
                v-if="currentTask && canConfirm(currentTask)"
                type="primary"
                :loading="processing"
                @click="confirmCurrentTask"
              >
                {{ confirmButtonText() }}
              </ElButton>
            </div>
          </div>
        </template>
      </el-dialog>

      <el-dialog
        v-model="quickEditDialogVisible"
        title="编辑用户"
        width="460px"
        destroy-on-close
        @closed="closeQuickEdit"
      >
        <div v-if="quickEditTarget" class="quick-edit-panel">
          <p class="quick-edit-target">
            正在编辑：{{ targetName(quickEditTarget) }}（{{ buildIdentityText(quickEditTarget) }}）
          </p>

          <label class="filter-field">
            <span>修改字段</span>
            <select v-model="quickEditForm.fieldName">
              <option value="">请选择字段</option>
              <option v-for="option in quickEditFieldOptions" :key="option.value" :value="option.value">
                {{ option.label }}
              </option>
            </select>
          </label>

          <label class="filter-field">
            <span>新值</span>
            <input
              v-model="quickEditForm.newValue"
              type="text"
              :placeholder="quickEditForm.fieldName === 'status' ? '例如：启用 / 禁用' : '请输入新的字段值'"
              @keyup.enter="submitQuickEdit"
            >
          </label>
        </div>

        <template #footer>
          <div class="dialog-actions">
            <ElButton @click="closeQuickEdit">取消</ElButton>
            <ElButton type="primary" :loading="processing" @click="submitQuickEdit">保存修改</ElButton>
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
  padding: 28px;
  background:
    radial-gradient(circle at top left, rgba(207, 223, 212, 0.32), transparent 36%),
    linear-gradient(180deg, #f8faf8 0%, #f4f6f4 100%);
  color: #243029;
  font-family: 'Manrope', sans-serif;
}

.admin-ai-shell {
  display: grid;
  gap: 18px;
  max-width: 1320px;
  margin: 0 auto;
}

.admin-ai-header,
.command-panel,
.filter-panel,
.table-panel {
  border: 1px solid rgba(47, 76, 58, 0.08);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 18px 48px rgba(36, 48, 41, 0.06);
  backdrop-filter: blur(18px);
}

.admin-ai-header {
  padding: 28px 30px;
}

.header-copy {
  max-width: 760px;
}

.eyebrow {
  display: inline-flex;
  margin-bottom: 10px;
  color: #6d7b72;
  font-size: 11px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.admin-ai-header h1 {
  margin: 0;
  color: #1e2821;
  font: 600 clamp(2rem, 2.4vw, 3rem) / 1.05 'Noto Serif SC', serif;
}

.admin-ai-header p {
  margin: 14px 0 0;
  max-width: 680px;
  color: #66736a;
  font-size: 14px;
  line-height: 1.8;
}

.admin-ai-alert {
  margin: 0;
  padding: 14px 16px;
  border-radius: 18px;
  background: rgba(255, 240, 236, 0.92);
  color: #a5453d;
  font-size: 13px;
}

.command-panel {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 18px;
  align-items: end;
  padding: 22px;
}

.command-field,
.followup-field {
  display: grid;
  gap: 10px;
}

.command-field span,
.followup-field span,
.filter-field span {
  color: #536158;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.02em;
}

.command-field textarea,
.followup-field textarea,
.filter-field input,
.filter-field select {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid rgba(47, 76, 58, 0.12);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.96);
  color: #243029;
  font: 500 14px/1.7 'Manrope', sans-serif;
  outline: none;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.command-field textarea,
.followup-field textarea {
  min-height: 104px;
  resize: vertical;
  padding: 14px 16px;
}

.filter-field input,
.filter-field select {
  height: 46px;
  padding: 0 14px;
}

.command-field textarea:focus,
.followup-field textarea:focus,
.filter-field input:focus,
.filter-field select:focus {
  border-color: rgba(47, 76, 58, 0.25);
  box-shadow: 0 0 0 4px rgba(110, 132, 118, 0.12);
}

.example-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.example-btn,
.filter-btn,
.primary-btn,
.page-btn,
.text-btn {
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.example-btn {
  border: 1px solid rgba(47, 76, 58, 0.12);
  border-radius: 999px;
  background: rgba(245, 247, 245, 0.96);
  color: #385142;
  cursor: pointer;
  font: 700 12px/1 'Manrope', sans-serif;
  padding: 10px 14px;
}

.example-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 20px rgba(36, 48, 41, 0.08);
}

.filter-panel {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  align-items: flex-end;
  padding: 22px;
}

.filter-field {
  display: grid;
  gap: 8px;
  min-width: 168px;
}

.filter-field--keyword {
  min-width: 300px;
  flex: 1;
}

.filter-actions {
  display: flex;
  gap: 10px;
}

.filter-btn,
.primary-btn,
.page-btn {
  height: 46px;
  border-radius: 16px;
  font: 700 13px/1 'Manrope', sans-serif;
  cursor: pointer;
}

.filter-btn {
  padding: 0 18px;
  border: 1px solid rgba(47, 76, 58, 0.12);
  background: rgba(255, 255, 255, 0.96);
  color: #2f4c3a;
}

.filter-btn--primary,
.primary-btn {
  border: none;
  background: linear-gradient(135deg, #294437 0%, #3f5c4d 100%);
  color: #ffffff;
}

.filter-btn:hover,
.page-btn:hover:not(:disabled),
.primary-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 14px 24px rgba(36, 48, 41, 0.1);
}

.primary-btn {
  min-width: 170px;
  padding: 0 24px;
}

.primary-btn:disabled,
.page-btn:disabled,
.filter-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.table-panel {
  padding: 22px;
}

.table-summary {
  display: flex;
  justify-content: space-between;
  margin-bottom: 14px;
  color: #66736a;
  font-size: 13px;
}

.table-wrap,
.detail-table-wrap {
  overflow: auto;
  border: 1px solid rgba(47, 76, 58, 0.08);
  border-radius: 18px;
  background: #ffffff;
}

.task-table,
.detail-table {
  width: 100%;
  min-width: 980px;
  border-collapse: collapse;
}

.detail-table {
  min-width: 1040px;
}

.task-table th,
.task-table td,
.detail-table th,
.detail-table td {
  padding: 14px 12px;
  border-bottom: 1px solid rgba(47, 76, 58, 0.06);
  text-align: left;
  font-size: 13px;
  vertical-align: middle;
}

.task-table th,
.detail-table th {
  position: sticky;
  top: 0;
  background: rgba(248, 250, 248, 0.98);
  color: #526058;
  font-weight: 700;
  white-space: nowrap;
}

.task-table tbody tr:hover,
.detail-table tbody tr:hover {
  background: rgba(248, 250, 248, 0.72);
}

.col-id {
  width: 108px;
}

.col-type {
  width: 128px;
}

.col-status {
  width: 128px;
}

.col-time {
  width: 180px;
}

.col-action,
.operation-col {
  width: 168px;
}

.instruction-cell {
  display: grid;
  gap: 6px;
}

.instruction-cell strong {
  color: #243029;
}

.instruction-cell span {
  color: #68766d;
  line-height: 1.6;
}

.status-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.status-tag.is-success {
  background: rgba(232, 245, 235, 0.98);
  color: #26734d;
}

.status-tag.is-warning {
  background: rgba(255, 244, 226, 0.98);
  color: #9a6a1d;
}

.status-tag.is-info {
  background: rgba(237, 244, 247, 0.98);
  color: #3d6675;
}

.status-tag.is-danger {
  background: rgba(255, 238, 236, 0.98);
  color: #ad4c3f;
}

.status-tag.is-neutral {
  background: rgba(241, 243, 240, 0.98);
  color: #526058;
}

.status-tag.is-muted {
  background: rgba(240, 242, 241, 0.98);
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

.text-btn:hover {
  transform: translateY(-1px);
}

.text-btn--danger {
  color: #b04e42;
}

.empty-cell {
  padding: 40px 12px !important;
  color: #829087;
  text-align: center !important;
}

.pagination-nav {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 14px;
  margin-top: 18px;
}

.pagination-nav span {
  color: #66736a;
  font-size: 13px;
}

.page-btn {
  padding: 0 18px;
  border: 1px solid rgba(47, 76, 58, 0.12);
  background: rgba(255, 255, 255, 0.94);
  color: #2f4c3a;
}

.detail-state-card,
.conversation-panel,
.followup-panel,
.workflow-panel {
  margin-bottom: 16px;
  border: 1px solid rgba(47, 76, 58, 0.08);
  border-radius: 20px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98) 0%, rgba(249, 250, 249, 0.98) 100%);
  box-shadow: 0 16px 32px rgba(36, 48, 41, 0.05);
}

.detail-state-card {
  display: flex;
  gap: 14px;
  align-items: flex-start;
  padding: 18px 20px;
}

.detail-state-copy h3 {
  margin: 2px 0 8px;
  color: #1f2a22;
  font: 600 20px/1.35 'Noto Serif SC', serif;
}

.detail-state-copy p {
  margin: 0;
  color: #58665d;
  line-height: 1.8;
}

.panel-heading {
  display: grid;
  gap: 6px;
  padding: 18px 20px 12px;
}

.panel-heading--split {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.panel-heading h3 {
  margin: 0;
  color: #1f2a22;
  font: 600 18px/1.3 'Noto Serif SC', serif;
}

.panel-heading p {
  margin: 0;
  color: #66736a;
  font-size: 13px;
  line-height: 1.8;
}

.conversation-list {
  display: grid;
  gap: 10px;
  max-height: 280px;
  overflow-y: auto;
  padding: 0 20px 20px;
}

.conversation-item {
  padding: 14px 16px;
  border-radius: 18px;
}

.conversation-item.is-assistant {
  background: rgba(243, 248, 244, 0.96);
}

.conversation-item.is-user {
  background: rgba(248, 248, 248, 0.96);
}

.conversation-meta {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 6px;
  color: #66736a;
  font-size: 12px;
}

.conversation-item p {
  margin: 0;
  color: #243029;
  line-height: 1.8;
  white-space: pre-wrap;
}

.followup-panel {
  padding: 0 20px 20px;
}

.followup-actions,
.dialog-footer,
.dialog-actions,
.operation-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.followup-actions {
  justify-content: space-between;
  margin-top: 14px;
}

.followup-actions span {
  color: #66736a;
  font-size: 13px;
  line-height: 1.7;
}

.update-highlight {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  padding: 0 20px 18px;
}

.update-chip {
  display: inline-flex;
  align-items: center;
  min-height: 34px;
  padding: 0 14px;
  border-radius: 999px;
  background: rgba(241, 244, 241, 0.96);
  color: #32453a;
  font-size: 12px;
  line-height: 1.5;
}

.check-col {
  width: 88px;
}

.check-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.snapshot-cell {
  max-width: 360px;
  color: #66736a;
  line-height: 1.7;
}

.dialog-footer {
  justify-content: space-between;
  width: 100%;
}

.dialog-footer > span {
  color: #66736a;
  font-size: 13px;
}

.dialog-actions {
  justify-content: flex-end;
}

.quick-edit-panel {
  display: grid;
  gap: 14px;
}

.quick-edit-target {
  margin: 0;
  color: #556259;
  line-height: 1.7;
}

@media (max-width: 1024px) {
  .admin-ai-page {
    padding: 18px;
  }

  .command-panel {
    grid-template-columns: 1fr;
  }

  .panel-heading--split,
  .followup-actions,
  .dialog-footer {
    flex-direction: column;
    align-items: stretch;
  }

  .dialog-actions {
    justify-content: stretch;
  }

  .dialog-actions :deep(.el-button) {
    width: 100%;
  }
}

@media (max-width: 640px) {
  .admin-ai-page {
    padding: 14px;
  }

  .admin-ai-header,
  .command-panel,
  .filter-panel,
  .table-panel {
    padding: 18px;
  }

  .filter-field,
  .filter-field--keyword {
    min-width: 100%;
  }
}
</style>
