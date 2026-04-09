<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { cancelAdminAiTaskApi, confirmAdminAiTaskApi, fetchAdminAiTaskDetailApi, fetchAdminAiTasksApi, parseAdminAiTaskApi } from '@/api/admin-ai-task'
import type { AdminAiTaskDetail, AdminAiTaskSummary, ParseAdminAiTaskResponse } from '@/api/types'
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

const pendingTasks = computed(() => tasks.value.filter((task) => task.confirmStatus !== 'CONFIRMED' && task.executeStatus !== 'EXECUTED'))
const executedTasks = computed(() => tasks.value.filter((task) => task.executeStatus === 'EXECUTED'))

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
  if (task.confirmStatus === 'CONFIRMED') {
    return '已确认待执行'
  }
  if (task.confirmStatus === 'CANCELLED') {
    return '已取消'
  }
  if (task.parseStatus === 'READY') {
    return '待确认'
  }
  return task.parseStatus
}

async function loadTasks(): Promise<void> {
  loading.value = true
  errorMessage.value = ''

  try {
    tasks.value = await fetchAdminAiTasksApi()
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
    errorMessage.value = '请输入管理员指令。'
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
  <main class="admin-ai-page">
    <section class="admin-ai-page__masthead">
      <div class="admin-ai-page__heading">
        <p class="admin-ai-page__eyebrow">管理员 AI 运维台</p>
        <h1 class="admin-ai-page__title">管理员 AI 运维</h1>
        <p class="admin-ai-page__summary">
          在这里输入自然语言指令，让系统先解析为待确认任务，再决定执行或取消。所有动作都保留审计轨迹。
        </p>
      </div>

      <aside class="admin-ai-page__snapshot">
        <p class="admin-ai-page__label">任务快照</p>
        <dl>
          <div>
            <dt>总数</dt>
            <dd>{{ tasks.length }}</dd>
          </div>
          <div>
            <dt>待处理</dt>
            <dd>{{ pendingTasks.length }}</dd>
          </div>
          <div>
            <dt>已执行</dt>
            <dd>{{ executedTasks.length }}</dd>
          </div>
        </dl>
      </aside>
    </section>

    <p v-if="errorMessage" class="admin-ai-page__alert">{{ errorMessage }}</p>

    <section class="admin-ai-page__composer">
      <div class="admin-ai-page__composer-copy">
        <p class="admin-ai-page__label">指令解析</p>
        <h2>输入管理指令</h2>
        <p>例如：将资源 20 下线，或把量表 8 设为启用状态。</p>
      </div>
      <textarea v-model="form.instruction" placeholder="输入管理员自然语言指令" />
      <button class="admin-ai-page__primary" type="button" :disabled="processing" @click="parseInstruction">
        {{ processing ? '解析中...' : '解析指令' }}
      </button>
    </section>

    <section v-if="parseResult" class="admin-ai-page__parse-result">
      <p class="admin-ai-page__label">最近一次解析结果</p>
      <h2>{{ parseResult.ready ? '任务已准备就绪' : '任务未就绪' }}</h2>
      <p>{{ parseResult.message }}</p>
    </section>

    <section v-if="loading" class="admin-ai-page__status-panel">
      <p>正在加载任务列表...</p>
    </section>

    <section v-else class="admin-ai-page__grid">
      <article class="admin-ai-page__panel admin-ai-page__panel--queue">
        <div class="admin-ai-page__panel-head">
          <div>
            <p class="admin-ai-page__label">任务队列</p>
            <h2>待处理任务</h2>
          </div>
          <span>{{ tasks.length }} 项</span>
        </div>

        <div v-if="tasks.length" class="admin-ai-page__task-list admin-ai-page__task-list--scroll">
          <button
            v-for="task in tasks"
            :key="task.taskId"
            type="button"
            class="admin-task-card"
            :class="{ 'admin-task-card--active': currentTask?.taskId === task.taskId }"
            @click="loadTaskDetail(task.taskId)"
          >
            <div class="admin-task-card__header">
              <p class="admin-task-card__serial">任务 #{{ task.taskId }}</p>
              <span>{{ resolveTaskState(task) }}</span>
            </div>
            <h3>{{ task.taskType }}</h3>
            <p>{{ task.instructionText }}</p>
            <footer class="admin-task-card__footer">
              <span>{{ formatDate(task.createdAt) }}</span>
              <strong>{{ task.summaryText || '待生成摘要' }}</strong>
            </footer>
          </button>
        </div>

        <p v-else class="admin-ai-page__empty">当前没有 AI 运维任务。</p>
      </article>

      <article class="admin-ai-page__panel admin-ai-page__panel--detail">
        <div class="admin-ai-page__panel-head">
          <div>
            <p class="admin-ai-page__label">任务详情</p>
            <h2>任务详情</h2>
          </div>
          <span>{{ currentTask ? `#${currentTask.taskId}` : '未选择' }}</span>
        </div>

        <div v-if="currentTask" class="admin-ai-page__detail-body">
          <section class="admin-ai-page__detail-summary">
            <div>
              <p class="admin-ai-page__label">指令内容</p>
              <h3>{{ currentTask.instructionText }}</h3>
            </div>
            <dl>
              <div>
                <dt>状态</dt>
                <dd>{{ resolveTaskState(currentTask) }}</dd>
              </div>
              <div>
                <dt>创建时间</dt>
                <dd>{{ formatDate(currentTask.createdAt) }}</dd>
              </div>
              <div>
                <dt>确认时间</dt>
                <dd>{{ formatDate(currentTask.confirmedAt) }}</dd>
              </div>
              <div>
                <dt>执行时间</dt>
                <dd>{{ formatDate(currentTask.executedAt) }}</dd>
              </div>
            </dl>
            <p class="admin-ai-page__detail-note">{{ currentTask.summaryText || currentTask.failureReason || '当前任务暂无额外摘要。' }}</p>
          </section>

          <section class="admin-ai-page__detail-items">
            <p class="admin-ai-page__label">执行项</p>
            <div v-if="currentTask.items.length" class="admin-ai-page__item-list">
              <article v-for="item in currentTask.items" :key="item.itemId" class="admin-task-item">
                <header>
                  <strong>{{ item.operationType }}</strong>
                  <span>{{ item.targetType }} / {{ item.targetLabel || item.targetId || '未命名目标' }}</span>
                </header>
                <p>字段：{{ item.fieldName || '无' }}</p>
                <p>旧值：{{ item.oldValue || '无' }}</p>
                <p>新值：{{ item.newValue || '无' }}</p>
                <p>执行状态：{{ item.executeStatus || '待执行' }}</p>
              </article>
            </div>
            <p v-else class="admin-ai-page__empty">当前任务没有拆解项。</p>
          </section>

          <section class="admin-ai-page__detail-actions">
            <button
              class="admin-ai-page__primary"
              type="button"
              :disabled="processing || currentTask.executeStatus === 'EXECUTED' || currentTask.confirmStatus === 'CANCELLED'"
              @click="confirmTask(currentTask.taskId)"
            >
              确认执行
            </button>
            <button
              class="admin-ai-page__ghost"
              type="button"
              :disabled="processing || currentTask.executeStatus === 'EXECUTED' || currentTask.confirmStatus === 'CANCELLED'"
              @click="cancelTask(currentTask.taskId)"
            >
              取消任务
            </button>
          </section>
        </div>

        <p v-else class="admin-ai-page__empty">请选择左侧任务查看详情。</p>
      </article>
    </section>
  </main>
</template>
<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700&family=Noto+Serif+SC:wght@400;500;600;700&display=swap');

.admin-ai-page {
  --paper: #f2ede4;
  --ink: #201b17;
  --muted: #6b645d;
  --line: rgba(32, 27, 23, 0.12);
  --glass: rgba(255, 251, 245, 0.72);
  min-height: 100vh;
  padding: 2rem;
  color: var(--ink);
  background:
    radial-gradient(circle at top left, rgba(110, 128, 118, 0.18), transparent 24%),
    radial-gradient(circle at right center, rgba(198, 186, 166, 0.2), transparent 30%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.14), transparent 38%),
    var(--paper);
}

.admin-ai-page__masthead {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(300px, 0.85fr);
  gap: 1.5rem;
  align-items: end;
  padding-bottom: 1.4rem;
  border-bottom: 1px solid var(--line);
}

.admin-ai-page__eyebrow,
.admin-ai-page__label,
.admin-ai-page__snapshot dt,
.admin-task-card__serial,
.admin-task-card__header span,
.admin-task-item header span,
.admin-ai-page__detail-summary dt {
  margin: 0;
  font: 600 0.72rem/1.4 'Manrope', sans-serif;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--muted);
}

.admin-ai-page__title {
  margin: 0.95rem 0 0;
  font: 600 clamp(2.7rem, 4.8vw, 5rem)/0.98 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.admin-ai-page__summary,
.admin-ai-page__composer-copy p:last-child,
.admin-task-card p,
.admin-ai-page__detail-note,
.admin-task-item p,
.admin-ai-page__empty,
.admin-ai-page__status-panel p,
.admin-ai-page__parse-result p {
  margin: 0;
  color: var(--muted);
  font: 400 0.98rem/1.85 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.admin-ai-page__snapshot,
.admin-ai-page__composer,
.admin-ai-page__parse-result,
.admin-ai-page__panel,
.admin-ai-page__status-panel,
.admin-task-card,
.admin-task-item {
  border: 1px solid var(--line);
  background: var(--glass);
  backdrop-filter: blur(18px);
  box-shadow: 0 22px 48px rgba(80, 70, 58, 0.08);
}

.admin-ai-page__snapshot,
.admin-ai-page__composer,
.admin-ai-page__parse-result,
.admin-ai-page__status-panel {
  margin-top: 1.5rem;
  padding: 1.2rem;
}

.admin-ai-page__snapshot dl {
  display: grid;
  gap: 0.9rem;
  margin: 1rem 0 0;
}

.admin-ai-page__snapshot dd,
.admin-ai-page__detail-summary dd {
  margin: 0.35rem 0 0;
  font: 600 1.04rem/1.45 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.admin-ai-page__alert {
  margin: 1.25rem 0 0;
  color: #8d4747;
  font: 600 0.9rem/1.6 'Manrope', sans-serif;
}

.admin-ai-page__composer {
  display: grid;
  gap: 1rem;
}

.admin-ai-page__composer h2,
.admin-ai-page__panel-head h2,
.admin-ai-page__parse-result h2,
.admin-ai-page__detail-summary h3 {
  margin: 0.7rem 0 0;
  font: 600 1.72rem/1.28 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.admin-ai-page__composer textarea {
  min-height: 8.5rem;
  resize: vertical;
  padding: 1rem;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.52);
  color: var(--ink);
  font: 400 1rem/1.72 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.admin-ai-page__primary,
.admin-ai-page__ghost {
  min-height: 3rem;
  padding: 0 1.15rem;
  font: 600 0.84rem/1 'Manrope', sans-serif;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  cursor: pointer;
  transition: transform 180ms ease, border-color 180ms ease, box-shadow 180ms ease;
}

.admin-ai-page__primary {
  justify-self: start;
  border: none;
  background: linear-gradient(135deg, #64806e, #4d6657);
  color: #faf6f0;
  box-shadow: 0 18px 36px rgba(77, 102, 87, 0.24);
}

.admin-ai-page__ghost {
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.5);
  color: var(--ink);
}

.admin-ai-page__primary:hover:not(:disabled),
.admin-ai-page__ghost:hover:not(:disabled),
.admin-task-card:hover {
  transform: translateY(-2px);
}

.admin-ai-page__primary:disabled,
.admin-ai-page__ghost:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.admin-ai-page__grid {
  display: grid;
  grid-template-columns: minmax(0, 0.95fr) minmax(0, 1.15fr);
  gap: 1rem;
  margin-top: 1.5rem;
}

.admin-ai-page__panel {
  display: grid;
  gap: 1rem;
  padding: 1.2rem;
}

.admin-ai-page__panel--queue {
  align-self: start;
}

.admin-ai-page__panel--detail {
  position: sticky;
  top: 1rem;
  align-self: start;
}

.admin-ai-page__panel-head {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: end;
  padding-bottom: 1rem;
  border-bottom: 1px solid var(--line);
}

.admin-ai-page__task-list,
.admin-ai-page__item-list {
  display: grid;
  gap: 0.9rem;
}

.admin-ai-page__task-list--scroll {
  max-height: calc(100vh - 14rem);
  overflow-y: auto;
  padding-right: 0.4rem;
}

.admin-task-card,
.admin-task-item {
  display: grid;
  gap: 0.75rem;
  padding: 1rem;
  text-align: left;
}

.admin-task-card--active {
  border-color: rgba(100, 128, 110, 0.42);
  background: linear-gradient(180deg, rgba(100, 128, 110, 0.12), rgba(255, 251, 245, 0.74));
}

.admin-task-card__header,
.admin-task-card__footer,
.admin-task-item header,
.admin-ai-page__detail-actions {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
}

.admin-task-card h3 {
  margin: 0;
  font: 600 1.28rem/1.3 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.admin-task-card__footer strong {
  font: 600 0.92rem/1.6 'Noto Serif SC', 'Source Han Serif SC', serif;
  color: var(--ink);
}

.admin-ai-page__detail-body,
.admin-ai-page__detail-summary {
  display: grid;
  gap: 1rem;
}

.admin-ai-page__detail-summary dl {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.9rem 1rem;
  padding-top: 1rem;
  border-top: 1px solid var(--line);
}

.admin-task-item header strong {
  font: 700 0.92rem/1.4 'Manrope', sans-serif;
  color: var(--ink);
}

@media (max-width: 980px) {
  .admin-ai-page {
    padding: 1rem;
  }

  .admin-ai-page__masthead,
  .admin-ai-page__grid,
  .admin-ai-page__detail-summary dl {
    grid-template-columns: 1fr;
  }

  .admin-ai-page__panel--detail {
    position: static;
  }

  .admin-ai-page__task-list--scroll {
    max-height: none;
    overflow-y: visible;
    padding-right: 0;
  }

  .admin-ai-page__panel-head,
  .admin-task-card__header,
  .admin-task-card__footer,
  .admin-task-item header,
  .admin-ai-page__detail-actions {
    flex-direction: column;
    align-items: flex-start;
  }

  .admin-ai-page__primary {
    width: 100%;
    justify-self: stretch;
  }
}
</style>

