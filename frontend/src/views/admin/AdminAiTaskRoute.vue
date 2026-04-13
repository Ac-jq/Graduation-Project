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
  <main class="admin-ai-ops-page">
    <section class="admin-ai-ops-page__hero">
      <div>
        <p class="admin-ai-ops-page__eyebrow">Admin AI Ops</p>
        <h1>自然语言运维确认台</h1>
        <p class="admin-ai-ops-page__lead">
          管理员先输入指令，AI 只负责生成待执行计划。真正的数据变更必须经过人工复核并点击确认执行后才会落库。
        </p>
      </div>
      <div class="admin-ai-ops-page__stats">
        <article>
          <span>待确认</span>
          <strong>{{ pendingTasks.length }}</strong>
        </article>
        <article>
          <span>已完成</span>
          <strong>{{ finishedTasks.length }}</strong>
        </article>
      </div>
    </section>

    <section class="admin-ai-ops-page__composer">
      <div class="admin-ai-ops-page__composer-copy">
        <p class="admin-ai-ops-page__section-label">输入指令</p>
        <h2>先生成执行计划，再人工确认</h2>
        <p>建议用清晰的对象、状态和编号描述指令，解析结果会拆成可审查的字段级清单。</p>
      </div>

      <div class="admin-ai-ops-page__example-list">
        <button v-for="example in examples" :key="example" type="button" class="example-chip" @click="useExample(example)">
          {{ example }}
        </button>
      </div>

      <textarea
        v-model="form.instruction"
        class="admin-ai-ops-page__textarea"
        placeholder="例如：Disable student accounts inactive for 3 months"
      />

      <div class="admin-ai-ops-page__composer-actions">
        <button class="primary-button" type="button" :disabled="processing" @click="parseInstruction">
          {{ processing ? '正在解析...' : '生成待执行计划' }}
        </button>
        <p v-if="parseResult" class="admin-ai-ops-page__result-copy">
          {{ parseResult.message }}
        </p>
      </div>
    </section>

    <p v-if="errorMessage" class="admin-ai-ops-page__alert">{{ errorMessage }}</p>

    <section class="admin-ai-ops-page__grid">
      <article class="panel">
        <div class="panel__head">
          <div>
            <p class="admin-ai-ops-page__section-label">任务队列</p>
            <h2>最近解析记录</h2>
          </div>
          <span>{{ tasks.length }} 条</span>
        </div>

        <div v-if="loading" class="panel__empty">正在加载任务队列...</div>
        <div v-else-if="tasks.length" class="task-list">
          <button
            v-for="task in tasks"
            :key="task.taskId"
            type="button"
            class="task-card"
            :class="{ 'task-card--active': currentTask?.taskId === task.taskId }"
            @click="loadTaskDetail(task.taskId)"
          >
            <div class="task-card__topline">
              <span>{{ resolveTaskType(task.taskType) }}</span>
              <strong>{{ resolveTaskState(task) }}</strong>
            </div>
            <h3>#{{ task.taskId }} {{ task.summaryText || '待补充解析信息' }}</h3>
            <p>{{ task.instructionText }}</p>
            <small>{{ formatDate(task.createdAt) }}</small>
          </button>
        </div>
        <div v-else class="panel__empty">当前还没有管理员 AI 运维任务。</div>
      </article>

      <article class="panel panel--detail">
        <div class="panel__head">
          <div>
            <p class="admin-ai-ops-page__section-label">执行清单</p>
            <h2>人工复核后才能落库</h2>
          </div>
          <span>{{ currentTask ? `#${currentTask.taskId}` : '未选择' }}</span>
        </div>

        <div v-if="currentTask" class="detail">
          <section class="detail__summary">
            <div class="summary-badge">{{ resolveTaskState(currentTask) }}</div>
            <h3>{{ resolveTaskType(currentTask.taskType) }}</h3>
            <p>{{ currentTask.summaryText || currentTask.failureReason || '当前任务还没有可执行摘要。' }}</p>
            <dl>
              <div>
                <dt>原始指令</dt>
                <dd>{{ currentTask.instructionText }}</dd>
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
          </section>

          <section class="detail__items">
            <div class="detail__items-head">
              <p class="admin-ai-ops-page__section-label">待执行操作清单</p>
              <span>{{ currentTask.items.length }} 项</span>
            </div>

            <div v-if="currentTask.items.length" class="detail__item-list">
              <article v-for="item in currentTask.items" :key="item.itemId" class="review-item">
                <header>
                  <div>
                    <strong>{{ resolveItemAction(item) }}</strong>
                    <span>{{ item.targetLabel || item.targetId || '未命名目标' }}</span>
                  </div>
                  <em>{{ item.executeStatus || 'WAITING' }}</em>
                </header>
                <div class="review-item__compare">
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
            <div v-else class="panel__empty">当前任务没有拆解出可执行明细。</div>
          </section>

          <section class="detail__actions">
            <button class="primary-button" type="button" :disabled="processing || !canReviewCurrent" @click="confirmTask(currentTask.taskId)">
              确认执行
            </button>
            <button class="ghost-button" type="button" :disabled="processing || currentTask.confirmStatus !== 'PENDING'" @click="cancelTask(currentTask.taskId)">
              取消任务
            </button>
          </section>
        </div>
        <div v-else class="panel__empty">请先从左侧选择一个解析任务。</div>
      </article>
    </section>
  </main>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700;800&family=Noto+Serif+SC:wght@500;600;700&display=swap');

.admin-ai-ops-page {
  --bg: #131820;
  --panel: rgba(20, 28, 37, 0.78);
  --panel-soft: rgba(255, 255, 255, 0.04);
  --line: rgba(255, 255, 255, 0.08);
  --text: #edf2f7;
  --muted: #8d99a8;
  --accent: #e2a84b;
  --accent-soft: rgba(226, 168, 75, 0.14);
  min-height: 100vh;
  padding: 2rem;
  color: var(--text);
  background:
    radial-gradient(circle at top right, rgba(226, 168, 75, 0.15), transparent 18%),
    radial-gradient(circle at left center, rgba(76, 112, 146, 0.16), transparent 24%),
    linear-gradient(180deg, #121820 0%, #181f28 100%);
  font-family: 'Manrope', sans-serif;
}

.admin-ai-ops-page__hero,
.admin-ai-ops-page__composer,
.panel {
  border: 1px solid var(--line);
  border-radius: 24px;
  background: var(--panel);
  box-shadow: 0 30px 60px rgba(0, 0, 0, 0.2);
  backdrop-filter: blur(20px);
}

.admin-ai-ops-page__hero,
.admin-ai-ops-page__composer {
  display: grid;
  gap: 1rem;
  padding: 1.5rem;
}

.admin-ai-ops-page__hero {
  grid-template-columns: minmax(0, 1.4fr) minmax(260px, 0.6fr);
  align-items: end;
}

.admin-ai-ops-page__eyebrow,
.admin-ai-ops-page__section-label,
.summary-badge,
.task-card__topline span,
.review-item header em,
.detail__items-head span,
.detail__summary dt {
  margin: 0;
  color: var(--muted);
  font-size: 0.74rem;
  font-weight: 700;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.admin-ai-ops-page h1,
.admin-ai-ops-page h2,
.admin-ai-ops-page h3 {
  margin: 0;
  font-family: 'Noto Serif SC', serif;
}

.admin-ai-ops-page h1 {
  font-size: clamp(2.5rem, 4vw, 4rem);
  line-height: 1;
}

.admin-ai-ops-page__lead,
.admin-ai-ops-page__composer-copy p:last-child,
.task-card p,
.detail__summary p,
.admin-ai-ops-page__result-copy,
.panel__empty {
  margin: 0;
  color: var(--muted);
  line-height: 1.8;
}

.admin-ai-ops-page__stats {
  display: grid;
  gap: 0.9rem;
}

.admin-ai-ops-page__stats article,
.review-item,
.task-card {
  border: 1px solid var(--line);
  border-radius: 18px;
  background: var(--panel-soft);
}

.admin-ai-ops-page__stats article {
  padding: 1rem 1.1rem;
}

.admin-ai-ops-page__stats strong {
  display: block;
  margin-top: 0.35rem;
  font-size: 2rem;
}

.admin-ai-ops-page__example-list {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.example-chip,
.primary-button,
.ghost-button,
.task-card {
  transition: transform 180ms ease, border-color 180ms ease, background 180ms ease, box-shadow 180ms ease;
}

.example-chip {
  padding: 0.65rem 0.85rem;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.03);
  color: var(--text);
  cursor: pointer;
}

.example-chip:hover,
.task-card:hover,
.primary-button:hover:not(:disabled),
.ghost-button:hover:not(:disabled) {
  transform: translateY(-1px);
}

.admin-ai-ops-page__textarea {
  min-height: 9rem;
  padding: 1rem 1.05rem;
  border: 1px solid var(--line);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.03);
  color: var(--text);
  resize: vertical;
}

.admin-ai-ops-page__composer-actions,
.detail__actions,
.panel__head,
.task-card__topline,
.review-item header,
.detail__items-head {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
}

.primary-button,
.ghost-button {
  min-height: 3rem;
  padding: 0 1.1rem;
  border-radius: 16px;
  font-weight: 800;
  letter-spacing: 0.08em;
  cursor: pointer;
}

.primary-button {
  border: none;
  background: linear-gradient(135deg, #e2a84b, #b57f2a);
  color: #1e1710;
}

.ghost-button {
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.03);
  color: var(--text);
}

.primary-button:disabled,
.ghost-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.admin-ai-ops-page__alert {
  margin: 1rem 0 0;
  color: #ffb4b4;
}

.admin-ai-ops-page__grid {
  display: grid;
  grid-template-columns: minmax(0, 0.9fr) minmax(0, 1.1fr);
  gap: 1rem;
  margin-top: 1.25rem;
}

.panel {
  display: grid;
  gap: 1rem;
  padding: 1.25rem;
}

.task-list,
.detail__item-list,
.detail {
  display: grid;
  gap: 0.9rem;
}

.task-card {
  padding: 1rem;
  text-align: left;
  color: inherit;
  cursor: pointer;
}

.task-card--active {
  border-color: rgba(226, 168, 75, 0.45);
  background: linear-gradient(180deg, rgba(226, 168, 75, 0.14), rgba(255, 255, 255, 0.03));
  box-shadow: 0 18px 30px rgba(226, 168, 75, 0.12);
}

.task-card__topline strong {
  color: var(--accent);
  font-size: 0.82rem;
}

.task-card h3 {
  font-size: 1.06rem;
  line-height: 1.45;
}

.task-card small {
  color: var(--muted);
}

.panel--detail {
  align-self: start;
}

.detail__summary,
.detail__items {
  padding: 1rem;
  border: 1px solid var(--line);
  border-radius: 18px;
  background: var(--panel-soft);
}

.summary-badge {
  display: inline-flex;
  width: fit-content;
  padding: 0.35rem 0.7rem;
  border-radius: 999px;
  background: var(--accent-soft);
  color: var(--accent);
}

.detail__summary dl {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.85rem 1rem;
  margin: 0;
  padding-top: 1rem;
}

.detail__summary dd {
  margin: 0.35rem 0 0;
  line-height: 1.6;
}

.review-item {
  display: grid;
  gap: 0.85rem;
  padding: 1rem;
}

.review-item header strong {
  display: block;
  margin-bottom: 0.2rem;
}

.review-item header span {
  color: var(--muted);
  font-size: 0.92rem;
}

.review-item__compare {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.8rem;
}

.review-item__compare label {
  display: block;
  margin-bottom: 0.35rem;
  color: var(--muted);
  font-size: 0.74rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.review-item__compare p {
  margin: 0;
  line-height: 1.6;
}

@media (max-width: 1080px) {
  .admin-ai-ops-page {
    padding: 1rem;
  }

  .admin-ai-ops-page__hero,
  .admin-ai-ops-page__grid,
  .detail__summary dl,
  .review-item__compare {
    grid-template-columns: 1fr;
  }

  .panel__head,
  .detail__actions,
  .admin-ai-ops-page__composer-actions,
  .detail__items-head,
  .review-item header,
  .task-card__topline {
    flex-direction: column;
    align-items: flex-start;
  }

  .primary-button,
  .ghost-button {
    width: 100%;
  }
}
</style>
