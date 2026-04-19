<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  activateAdminScaleApi,
  createAdminScaleApi,
  deactivateAdminScaleApi,
  fetchAdminScaleDetailApi,
  updateAdminScaleApi
} from '@/api/admin-scale'
import type {
  AdminScale,
  UpsertAdminScaleOptionRequest,
  UpsertAdminScaleQuestionRequest,
  UpsertAdminScaleRequest
} from '@/api/types'
import { toErrorMessage, toNumberParam } from '@/views/shared/page-logic'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const saving = ref(false)
const switchingStatus = ref(false)
const errorMessage = ref('')
const scaleDetail = ref<AdminScale | null>(null)
const scaleId = computed(() => toNumberParam(route.params.scaleId))
const currentQuestionIndex = ref(0)

const form = reactive<UpsertAdminScaleRequest>({
  code: '',
  name: '',
  description: '',
  introduction: '',
  pageSize: 10,
  lowThreshold: 0,
  mediumThreshold: 10,
  highThreshold: 20,
  questions: []
})

function createDefaultOption(index: number): UpsertAdminScaleOptionRequest {
  const code = String.fromCharCode(65 + index)
  return {
    optionCode: code,
    content: '',
    score: index,
    sortNo: index + 1
  }
}

function createDefaultQuestion(index: number): UpsertAdminScaleQuestionRequest {
  return {
    questionNo: index + 1,
    content: '',
    requiredFlag: 1,
    options: [0, 1, 2, 3].map((item) => createDefaultOption(item))
  }
}

function resetForm(): void {
  form.code = ''
  form.name = ''
  form.description = ''
  form.introduction = ''
  form.pageSize = 10
  form.lowThreshold = 0
  form.mediumThreshold = 10
  form.highThreshold = 20
  form.questions = [createDefaultQuestion(0)]
  currentQuestionIndex.value = 0
}

function syncForm(data: AdminScale): void {
  form.code = data.code
  form.name = data.name
  form.description = data.description ?? ''
  form.introduction = data.introduction ?? ''
  form.pageSize = data.pageSize
  form.lowThreshold = data.lowThreshold
  form.mediumThreshold = data.mediumThreshold
  form.highThreshold = data.highThreshold
  form.questions = data.questions.map((question) => ({
    questionNo: question.questionNo,
    content: question.content,
    requiredFlag: question.requiredFlag,
    options: question.options.map((option) => ({
      optionCode: option.optionCode,
      content: option.content,
      score: option.score,
      sortNo: option.sortNo
    }))
  }))

  if (!form.questions.length) {
    form.questions = [createDefaultQuestion(0)]
  }
  currentQuestionIndex.value = 0
}

const totalQuestions = computed(() => form.questions.length)
const currentQuestion = computed(() => form.questions[currentQuestionIndex.value] ?? null)

function reindexQuestions(): void {
  form.questions.forEach((question, questionIndex) => {
    question.questionNo = questionIndex + 1
    question.options.forEach((option, optionIndex) => {
      option.sortNo = optionIndex + 1
      if (!option.optionCode?.trim()) {
        option.optionCode = String.fromCharCode(65 + optionIndex)
      }
    })
  })
}

function addQuestion(): void {
  form.questions.push(createDefaultQuestion(form.questions.length))
  reindexQuestions()
  currentQuestionIndex.value = form.questions.length - 1
}

function removeQuestion(questionIndex: number): void {
  if (form.questions.length === 1) {
    errorMessage.value = '量表至少需要保留 1 道题目'
    return
  }
  form.questions.splice(questionIndex, 1)
  reindexQuestions()
  if (currentQuestionIndex.value >= form.questions.length) {
    currentQuestionIndex.value = form.questions.length - 1
  }
}

function addOption(questionIndex: number): void {
  const question = form.questions[questionIndex]
  question.options.push(createDefaultOption(question.options.length))
  reindexQuestions()
}

function removeOption(questionIndex: number, optionIndex: number): void {
  const question = form.questions[questionIndex]
  if (question.options.length === 2) {
    errorMessage.value = '每道题至少需要保留 2 个选项'
    return
  }
  question.options.splice(optionIndex, 1)
  reindexQuestions()
}

function goPrevQuestion(): void {
  if (currentQuestionIndex.value > 0) {
    currentQuestionIndex.value--
  }
}

function goNextQuestion(): void {
  if (currentQuestionIndex.value < form.questions.length - 1) {
    currentQuestionIndex.value++
  }
}

function buildPayload(): UpsertAdminScaleRequest {
  reindexQuestions()
  return {
    code: form.code.trim(),
    name: form.name.trim(),
    description: form.description?.trim() || '',
    introduction: form.introduction?.trim() || '',
    pageSize: Number(form.pageSize),
    lowThreshold: Number(form.lowThreshold),
    mediumThreshold: Number(form.mediumThreshold),
    highThreshold: Number(form.highThreshold),
    questions: form.questions.map((question, questionIndex) => ({
      questionNo: questionIndex + 1,
      content: question.content.trim(),
      requiredFlag: question.requiredFlag ? 1 : 0,
      options: question.options.map((option, optionIndex) => ({
        optionCode: (option.optionCode?.trim() || String.fromCharCode(65 + optionIndex)).toUpperCase(),
        content: option.content.trim(),
        score: Number(option.score),
        sortNo: optionIndex + 1
      }))
    }))
  }
}

function validateForm(): boolean {
  if (!form.code.trim() || !form.name.trim()) {
    errorMessage.value = '请先填写量表编码和量表名称'
    return false
  }

  if (!form.questions.length) {
    errorMessage.value = '请至少添加 1 道题目'
    return false
  }

  for (const question of form.questions) {
    if (!question.content.trim()) {
      errorMessage.value = `题目 ${question.questionNo} 的题干不能为空`
      return false
    }

    if (question.options.length < 2) {
      errorMessage.value = `题目 ${question.questionNo} 至少需要 2 个选项`
      return false
    }

    for (const option of question.options) {
      if (!option.content.trim()) {
        errorMessage.value = `题目 ${question.questionNo} 存在空白选项`
        return false
      }
    }
  }

  if (Number(form.highThreshold) < Number(form.mediumThreshold) || Number(form.mediumThreshold) < Number(form.lowThreshold)) {
    errorMessage.value = '风险阈值必须满足：低阈值 <= 中阈值 <= 高阈值'
    return false
  }

  return true
}

function resolveStatusText(status?: string): string {
  return status === 'ACTIVE' ? '启用中' : status === 'INACTIVE' ? '已停用' : status || '未标记'
}

async function loadScaleDetail(): Promise<void> {
  if (!scaleId.value) {
    scaleDetail.value = null
    resetForm()
    return
  }

  loading.value = true
  errorMessage.value = ''

  try {
    const data = await fetchAdminScaleDetailApi(scaleId.value)
    scaleDetail.value = data
    syncForm(data)
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    loading.value = false
  }
}

async function saveScale(): Promise<void> {
  errorMessage.value = ''
  if (!validateForm()) {
    return
  }

  saving.value = true

  try {
    const payload = buildPayload()
    const saved = scaleId.value
      ? await updateAdminScaleApi(scaleId.value, payload)
      : await createAdminScaleApi(payload)

    scaleDetail.value = saved
    syncForm(saved)

    if (!scaleId.value) {
      await router.replace({ name: 'admin-scale-detail', params: { scaleId: saved.scaleId } })
    }
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    saving.value = false
  }
}

async function toggleScaleStatus(): Promise<void> {
  if (!scaleId.value || !scaleDetail.value) {
    errorMessage.value = '当前量表还未保存，不能切换状态'
    return
  }

  switchingStatus.value = true
  errorMessage.value = ''

  try {
    scaleDetail.value = scaleDetail.value.status === 'ACTIVE'
      ? await deactivateAdminScaleApi(scaleId.value)
      : await activateAdminScaleApi(scaleId.value)
    syncForm(scaleDetail.value)
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    switchingStatus.value = false
  }
}

watch(() => route.params.scaleId, () => {
  void loadScaleDetail()
})

onMounted(() => {
  void loadScaleDetail()
})
</script>

<template>
  <section class="admin-editorial-page">
    <div class="admin-editorial-shell">
      <header class="admin-editorial-hero">
        <div class="admin-editorial-copy">
          <p class="admin-editorial-eyebrow">量表详情</p>
          <h1 class="admin-editorial-title">{{ scaleDetail ? '编辑量表' : '新增量表' }}</h1>
          <p class="admin-editorial-lead">在这里直接配置量表基础信息、题目、选项与分值。保存后，学生端就可以按这张测试卷进行作答。</p>
        </div>
        <div class="admin-editorial-hero-side">
          <article class="admin-editorial-stat">
            <p class="admin-editorial-label">当前状态</p>
            <strong>{{ resolveStatusText(scaleDetail?.status) }}</strong>
            <p class="admin-editorial-lead">{{ scaleDetail?.name || '新量表尚未保存' }}</p>
          </article>
        </div>
      </header>

      <p v-if="errorMessage" class="admin-editorial-alert">{{ errorMessage }}</p>

      <div class="admin-editorial-grid scale-editor-grid">
        <section class="admin-editorial-panel admin-editorial-panel--mesh">
          <div class="admin-editorial-section">
            <p class="admin-editorial-kicker">基础配置</p>
            <h2>编辑量表元信息与阈值</h2>
          </div>

          <div class="admin-editorial-form">
            <label class="admin-editorial-field">
              <span>量表编码</span>
              <input v-model="form.code" type="text" placeholder="例如：STRESS-CAMPUS-12">
            </label>
            <label class="admin-editorial-field">
              <span>量表名称</span>
              <input v-model="form.name" type="text" placeholder="例如：校园压力状态量表">
            </label>
            <label class="admin-editorial-field wide">
              <span>量表简介</span>
              <input v-model="form.description" type="text" placeholder="用于说明量表适用场景和目标">
            </label>
            <label class="admin-editorial-field wide">
              <span>作答引导语</span>
              <textarea v-model="form.introduction" rows="4" placeholder="例如：请根据过去两周的真实状态进行选择"></textarea>
            </label>
            <label class="admin-editorial-field">
              <span>每页题数</span>
              <input v-model.number="form.pageSize" type="number" min="1">
            </label>
            <label class="admin-editorial-field">
              <span>低风险阈值</span>
              <input v-model.number="form.lowThreshold" type="number">
            </label>
            <label class="admin-editorial-field">
              <span>中风险阈值</span>
              <input v-model.number="form.mediumThreshold" type="number">
            </label>
            <label class="admin-editorial-field">
              <span>高风险阈值</span>
              <input v-model.number="form.highThreshold" type="number">
            </label>
          </div>

          <div class="admin-editorial-actions" style="margin-top: 1rem;">
            <button class="admin-editorial-button" type="button" :disabled="saving" @click="saveScale">
              {{ saving ? '保存中...' : '保存量表' }}
            </button>
            <button
              v-if="scaleDetail"
              class="admin-editorial-ghost"
              type="button"
              :disabled="switchingStatus"
              @click="toggleScaleStatus"
            >
              {{ scaleDetail.status === 'ACTIVE' ? '停用量表' : '启用量表' }}
            </button>
          </div>
        </section>

        <section class="admin-editorial-panel">
          <div class="admin-editorial-section admin-editorial-section--inline">
            <div>
              <p class="admin-editorial-kicker">题目编辑</p>
              <h2>自定义题目与选项</h2>
            </div>
            <button class="admin-editorial-ghost question-add-btn" type="button" @click="addQuestion">新增题目</button>
          </div>

          <div v-if="loading" class="admin-editorial-empty">正在读取量表详情...</div>

          <div v-else class="question-editor-stack">
            <div class="question-editor-toolbar">
              <div class="question-editor-progress">
                <p class="admin-editorial-code">当前题目</p>
                <strong>{{ currentQuestionIndex + 1 }} / {{ totalQuestions }}</strong>
              </div>
              <div class="question-editor-actions">
                <button class="admin-editorial-ghost question-nav-btn" type="button" :disabled="currentQuestionIndex <= 0" @click="goPrevQuestion">上一题</button>
                <button class="admin-editorial-ghost question-nav-btn" type="button" :disabled="currentQuestionIndex >= totalQuestions - 1" @click="goNextQuestion">下一题</button>
              </div>
            </div>

            <article v-if="currentQuestion" :key="`${currentQuestion.questionNo}-${currentQuestionIndex}`" class="question-editor-card">
              <div class="question-editor-head">
                <div>
                  <p class="admin-editorial-code">题目 {{ currentQuestion.questionNo }}</p>
                  <h3 class="question-editor-title">配置题干与作答项</h3>
                </div>
                <button class="inline-danger-btn" type="button" @click="removeQuestion(currentQuestionIndex)">删除题目</button>
              </div>

              <div class="question-editor-grid">
                <label class="admin-editorial-field wide">
                  <span>题干</span>
                  <textarea v-model="currentQuestion.content" rows="3" placeholder="请输入学生要看到的题目内容"></textarea>
                </label>

                <label class="admin-editorial-field compact-field">
                  <span>是否必答</span>
                  <select v-model.number="currentQuestion.requiredFlag">
                    <option :value="1">必答</option>
                    <option :value="0">可选</option>
                  </select>
                </label>
              </div>

              <div class="option-editor">
                <div class="option-editor-head">
                  <p class="admin-editorial-kicker">选项配置</p>
                  <button class="admin-editorial-chip-button" type="button" @click="addOption(currentQuestionIndex)">新增选项</button>
                </div>

                <div class="option-editor-list">
                  <div
                    v-for="(option, optionIndex) in currentQuestion.options"
                    :key="`${currentQuestion.questionNo}-${option.sortNo}-${optionIndex}`"
                    class="option-editor-row"
                  >
                    <label class="admin-editorial-field option-code-field">
                      <span>编号</span>
                      <input v-model="option.optionCode" type="text" maxlength="4">
                    </label>
                    <label class="admin-editorial-field option-content-field">
                      <span>选项文案</span>
                      <input v-model="option.content" type="text" placeholder="例如：几乎没有 / 有时如此">
                    </label>
                    <label class="admin-editorial-field option-score-field">
                      <span>分值</span>
                      <input v-model.number="option.score" type="number">
                    </label>
                    <button class="inline-danger-btn option-remove-btn" type="button" @click="removeOption(currentQuestionIndex, optionIndex)">删除</button>
                  </div>
                </div>
              </div>
            </article>
          </div>
        </section>
      </div>
    </div>
  </section>
</template>

<style scoped>
@import './admin-editorial.css';

.scale-editor-grid {
  grid-template-columns: minmax(320px, 0.82fr) minmax(0, 1.18fr);
}

.question-add-btn {
  justify-self: end;
}

.question-editor-stack {
  display: grid;
  gap: 1rem;
}

.question-editor-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  padding: 0.25rem 0;
}

.question-editor-progress {
  display: grid;
  gap: 0.3rem;
}

.question-editor-progress strong {
  color: #1e2821;
  font-family: 'Noto Serif SC', serif;
  font-size: 1.1rem;
  font-weight: 600;
}

.question-editor-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.question-nav-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.question-editor-card {
  border-radius: 24px;
  border: 1px solid rgba(255, 255, 255, 0.82);
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.76), rgba(248, 246, 242, 0.88));
  box-shadow: 0 32px 72px rgba(54, 66, 58, 0.06);
  padding: 1.25rem;
}

.question-editor-head,
.option-editor-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
}

.question-editor-title {
  margin-top: 0.35rem;
  color: #1e2821;
  font-family: 'Noto Serif SC', serif;
  font-size: 1.15rem;
  font-weight: 600;
}

.question-editor-grid {
  display: grid;
  gap: 1rem;
  margin-top: 1rem;
}

.compact-field {
  max-width: 220px;
}

.option-editor {
  margin-top: 1.2rem;
  padding-top: 1rem;
  border-top: 1px solid rgba(42, 54, 46, 0.08);
}

.option-editor-list {
  display: grid;
  gap: 0.9rem;
  margin-top: 1rem;
}

.option-editor-row {
  display: grid;
  grid-template-columns: 100px minmax(0, 1fr) 120px auto;
  gap: 0.9rem;
  align-items: end;
}

.option-code-field,
.option-score-field {
  min-width: 0;
}

.option-content-field {
  min-width: 0;
}

.inline-danger-btn {
  min-height: 2.8rem;
  padding: 0.8rem 1rem;
  border-radius: 999px;
  border: 1px solid rgba(155, 88, 80, 0.14);
  background: rgba(155, 88, 80, 0.08);
  color: #9b5850;
  font: 700 0.78rem/1 'Manrope', sans-serif;
  letter-spacing: 0.08em;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.inline-danger-btn:hover {
  transform: translateY(-3px);
  box-shadow: 0 20px 44px rgba(155, 88, 80, 0.12);
}

.option-remove-btn {
  align-self: stretch;
}

@media (max-width: 1120px) {
  .scale-editor-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 820px) {
  .question-editor-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .question-editor-actions {
    width: 100%;
  }

  .option-editor-row {
    grid-template-columns: 1fr;
  }

  .compact-field {
    max-width: none;
  }
}
</style>
