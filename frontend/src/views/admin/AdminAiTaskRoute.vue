<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
  cancelAdminAiTaskApi,
  confirmAdminAiTaskApi,
  fetchAdminAiTaskDetailApi,
  fetchAdminAiTasksApi,
  parseAdminAiTaskApi
} from '@/api/admin-ai-task'
import type { AdminAiTaskDetail, AdminAiTaskItem, AdminAiTaskSummary, ParseAdminAiTaskResponse } from '@/api/types'
import { toErrorMessage } from '@/views/shared/page-logic'

const loading = ref(false)
const processing = ref(false)
const errorMessage = ref('')
const tasks = ref<AdminAiTaskSummary[]>([])
const currentTask = ref<AdminAiTaskDetail | null>(null)
const parseResult = ref<ParseAdminAiTaskResponse | null>(null)
const form = reactive({
  instruction: ''
})

const examples = [
  'Disable student accounts inactive for 3 months',
  'Create a counselor named Zhang San with counselorNo T009',
  'Take resource id 23 offline',
  'Enable account: 20209998'
]

const pendingTasks = computed(() =>
  tasks.value.filter((task) => task.confirmStatus === 'PENDING' && task.executeStatus === 'WAITING')
)

const finishedTasks = computed(() =>
  tasks.value.filter((task) => task.confirmStatus !== 'PENDING' || task.executeStatus !== 'WAITING')
)

const canReviewCurrent = computed(() => {
  if (!currentTask.value) {
    return false
  }
  return currentTask.value.parseStatus === 'READY' && currentTask.value.confirmStatus === 'PENDING'
})

function formatDate(value: string | null): string {
  if (!value) {
    return '未记录'
  }
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value))
}

function resolveTaskState(task: AdminAiTaskSummary | AdminAiTaskDetail): string {
  if (task.executeStatus === 'EXECUTED') {
    return '已执行'
  }
  if (task.confirmStatus === 'CANCELED') {
    return '已取消'
  }
  if (task.parseStatus === 'READY') {
    return '待确认'
  }
  return '需补充信息'
}

function resolveTaskType(taskType: string | null | undefined): string {
  switch (taskType) {
    case 'ACCOUNT_STATUS':
      return '账号状态变更'
    case 'COUNSELOR_CREATE':
      return '咨询师创建'
    case 'RESOURCE_STATUS':
      return '资源上下架'
    default:
      return '待识别任务'
  }
}

function resolveItemAction(item: AdminAiTaskItem): string {
  if (item.operationType === 'CREATE') {
    return '创建字段'
  }
  if (item.operationType === 'OFFLINE') {
    return '下架资源'
  }
  if (item.operationType === 'PUBLISH') {
    return '上架资源'
  }
  return '更新字段'
}

function formatValue(value: string | null | undefined): string {
  return value && value.trim() ? value : '空'
}

function useExample(example: string): void {
  form.instruction = example
}

async function loadTasks(): Promise<void> {
  loading.value = true
  errorMessage.value = ''
  try {
    tasks.value = await fetchAdminAiTasksApi()
    if (!currentTask.value && tasks.value.length > 0) {
      await loadTaskDetail(tasks.value[0].taskId)
    }
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    loading.value = false
  }
}

async function loadTaskDetail(taskId: number): Promise<void> {
  processing.value = true
  errorMessage.value = ''
  try {
    currentTask.value = await fetchAdminAiTaskDetailApi(taskId)
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    processing.value = false
  }
}

async function parseInstruction(): Promise<void> {
  if (!form.instruction.trim()) {
    errorMessage.value = '请输入管理员指令'
    return
  }

  processing.value = true
  errorMessage.value = ''
  try {
    parseResult.value = await parseAdminAiTaskApi({ instruction: form.instruction.trim() })
    currentTask.value = parseResult.value.task
    await loadTasks()
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    processing.value = false
  }
}

async function confirmTask(taskId: number): Promise<void> {
  processing.value = true
  errorMessage.value = ''
  try {
    currentTask.value = await confirmAdminAiTaskApi(taskId)
    await loadTasks()
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    processing.value = false
  }
}

async function cancelTask(taskId: number): Promise<void> {
  processing.value = true
  errorMessage.value = ''
  try {
    currentTask.value = await cancelAdminAiTaskApi(taskId)
    await loadTasks()
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    processing.value = false
  }
}

onMounted(() => {
  void loadTasks()
})
</script>

<template>
  <section class="admin-editorial-page">
    <div class="admin-editorial-shell">
      <header class="admin-editorial-hero">
        <div class="admin-editorial-copy">
          <p class="admin-editorial-eyebrow">AI 运维确认台</p>
          <h1 class="admin-editorial-title">先生成执行计划，再由管理员逐项复核，最后才允许真正落库。</h1>
          <p class="admin-editorial-lead">自然语言输入、解析、确认执行与取消任务仍然使用原有接口，这里只改变视觉结构，不改变任何业务边界。</p>
        </div>
        <div class="admin-editorial-hero-side">
          <article class="admin-editorial-stat">
            <p class="admin-editorial-label">待确认</p>
            <strong>{{ pendingTasks.length }}</strong>
          </article>
          <article class="admin-editorial-stat">
            <p class="admin-editorial-label">已完成</p>
            <strong>{{ finishedTasks.length }}</strong>
          </article>
        </div>
      </header>

      <p v-if="errorMessage" class="admin-editorial-alert">{{ errorMessage }}</p>

      <section class="admin-editorial-panel admin-editorial-panel--mesh">
        <div class="admin-editorial-section">
          <p class="admin-editorial-kicker">输入指令</p>
          <h2>用自然语言生成待执行清单</h2>
        </div>

        <div class="admin-editorial-chip-list">
          <button v-for="example in examples" :key="example" type="button" class="admin-editorial-chip-button" @click="useExample(example)">
            {{ example }}
          </button>
        </div>

        <div class="admin-editorial-form" style="margin-top: 1rem;">
          <label class="admin-editorial-field wide">
            <span>管理员指令</span>
            <textarea
              v-model="form.instruction"
              rows="4"
              placeholder="例如：Disable student accounts inactive for 3 months"
            />
          </label>
        </div>

        <div class="admin-editorial-actions" style="margin-top: 1rem;">
          <button class="admin-editorial-button" type="button" :disabled="processing" @click="parseInstruction">
            {{ processing ? '正在解析' : '生成待执行计划' }}
          </button>
        </div>

        <article v-if="parseResult" class="admin-editorial-card" style="margin-top: 1rem;">
          <p class="admin-editorial-code">解析结果</p>
          <p>{{ parseResult.message }}</p>
        </article>
      </section>

      <div class="admin-editorial-grid" style="margin-top: 1.5rem;">
        <section class="admin-editorial-panel">
          <div class="admin-editorial-section admin-editorial-section--inline">
            <div>
              <p class="admin-editorial-kicker">任务队列</p>
              <h2>最近解析记录</h2>
            </div>
            <span class="admin-editorial-badge">{{ tasks.length }} 条</span>
          </div>

          <div v-if="loading" class="admin-editorial-empty">正在加载任务队列…</div>
          <div v-else-if="tasks.length" class="admin-editorial-board">
            <button
              v-for="task in tasks"
              :key="task.taskId"
              type="button"
              class="admin-editorial-card"
              :class="{ 'is-active': currentTask?.taskId === task.taskId }"
              @click="loadTaskDetail(task.taskId)"
            >
              <div class="admin-editorial-card__topline">
                <div>
                  <p class="admin-editorial-code">{{ resolveTaskType(task.taskType) }}</p>
                  <h3>#{{ task.taskId }} {{ task.summaryText || '待补充解析信息' }}</h3>
                </div>
                <span class="admin-editorial-status">{{ resolveTaskState(task) }}</span>
              </div>
              <p>{{ task.instructionText }}</p>
              <div class="admin-editorial-card__footer">
                <span class="admin-editorial-note">{{ formatDate(task.createdAt) }}</span>
              </div>
            </button>
          </div>
          <div v-else class="admin-editorial-empty">当前还没有管理员 AI 运维任务。</div>
        </section>

        <section class="admin-editorial-panel">
          <div class="admin-editorial-section admin-editorial-section--inline">
            <div>
              <p class="admin-editorial-kicker">执行清单</p>
              <h2>人工复核后才能落库</h2>
            </div>
            <span class="admin-editorial-badge">{{ currentTask ? `#${currentTask.taskId}` : '未选择' }}</span>
          </div>

          <div v-if="currentTask" class="admin-editorial-board">
            <article class="admin-editorial-card">
              <div class="admin-editorial-card__topline">
                <div>
                  <p class="admin-editorial-code">任务摘要</p>
                  <h3>{{ resolveTaskType(currentTask.taskType) }}</h3>
                </div>
                <span class="admin-editorial-status">{{ resolveTaskState(currentTask) }}</span>
              </div>
              <p>{{ currentTask.summaryText || currentTask.failureReason || '当前任务还没有可执行摘要。' }}</p>
              <div class="admin-editorial-meta">
                <span>原始指令：{{ currentTask.instructionText }}</span>
                <span>创建时间：{{ formatDate(currentTask.createdAt) }}</span>
                <span>确认时间：{{ formatDate(currentTask.confirmedAt) }}</span>
                <span>执行时间：{{ formatDate(currentTask.executedAt) }}</span>
              </div>
            </article>

            <article class="admin-editorial-card">
              <div class="admin-editorial-card__topline">
                <div>
                  <p class="admin-editorial-code">待执行操作清单</p>
                  <h3>{{ currentTask.items.length }} 项</h3>
                </div>
              </div>

              <div v-if="currentTask.items.length" class="admin-editorial-board" style="margin-top: 1rem;">
                <article v-for="item in currentTask.items" :key="item.itemId" class="admin-editorial-card">
                  <div class="admin-editorial-card__topline">
                    <div>
                      <p class="admin-editorial-code">{{ resolveItemAction(item) }}</p>
                      <h3>{{ item.targetLabel || item.targetId || '未命名目标' }}</h3>
                    </div>
                    <span class="admin-editorial-status">{{ item.executeStatus || 'WAITING' }}</span>
                  </div>
                  <div class="admin-editorial-compare">
                    <div>
                      <label>字段</label>
                      <p>{{ item.fieldName || '未指定' }}</p>
                    </div>
                    <div>
                      <label>旧值</label>
                      <p>{{ formatValue(item.oldValue) }}</p>
                    </div>
                    <div>
                      <label>新值</label>
                      <p>{{ formatValue(item.newValue) }}</p>
                    </div>
                  </div>
                </article>
              </div>
              <div v-else class="admin-editorial-empty">当前任务没有拆解出可执行明细。</div>
            </article>

            <div class="admin-editorial-actions">
              <button class="admin-editorial-button" type="button" :disabled="processing || !canReviewCurrent" @click="confirmTask(currentTask.taskId)">
                确认执行
              </button>
              <button class="admin-editorial-ghost" type="button" :disabled="processing || currentTask.confirmStatus !== 'PENDING'" @click="cancelTask(currentTask.taskId)">
                取消任务
              </button>
            </div>
          </div>

          <div v-else class="admin-editorial-empty">请先从左侧选择一个解析任务。</div>
        </section>
      </div>
    </div>
  </section>
</template>

<style scoped>
@import './admin-editorial.css';

.is-active {
  box-shadow: 0 0 0 1px rgba(97, 122, 105, 0.18), 0 48px 88px rgba(54, 66, 58, 0.09);
}
</style>
