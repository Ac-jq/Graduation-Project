<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  activateAdminScaleApi,
  createAdminScaleApi,
  deactivateAdminScaleApi,
  fetchAdminScaleDetailApi,
  fetchAdminScalesApi,
  updateAdminScaleApi
} from '@/api/admin-scale'
import type {
  AdminScale,
  UpsertAdminScaleOptionRequest,
  UpsertAdminScaleQuestionRequest,
  UpsertAdminScaleRequest
} from '@/api/types'
import { toErrorMessage } from '@/views/shared/page-logic'

const loading = ref(false)
const dialogLoading = ref(false)
const saving = ref(false)
const switchingStatus = ref(false)
const errorMessage = ref('')
const scales = ref<AdminScale[]>([])
const currentPage = ref(1)
const pageSize = 10
const dialogVisible = ref(false)
const editingScaleId = ref<number | null>(null)
const currentQuestionIndex = ref(0)

const filters = reactive({
  keyword: '',
  status: ''
})

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

const filteredScales = computed(() => {
  const keyword = filters.keyword.trim().toLowerCase()
  return scales.value.filter((scale) => {
    const matchStatus = !filters.status || scale.status === filters.status
    const matchKeyword = !keyword
      || scale.name.toLowerCase().includes(keyword)
      || scale.code.toLowerCase().includes(keyword)
      || (scale.description ?? '').toLowerCase().includes(keyword)
    return matchStatus && matchKeyword
  })
})

const totalPages = computed(() => Math.max(1, Math.ceil(filteredScales.value.length / pageSize)))
const pagedScales = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return filteredScales.value.slice(start, start + pageSize)
})

const totalQuestions = computed(() => form.questions.length)
const currentQuestion = computed(() => form.questions[currentQuestionIndex.value] ?? null)
const isCreateMode = computed(() => editingScaleId.value == null)

function rowIndex(index: number): number {
  return (currentPage.value - 1) * pageSize + index + 1
}

function resolveStatusText(status?: string): string {
  if (status === 'ACTIVE') return '启用中'
  if (status === 'INACTIVE') return '已停用'
  return status || '未标记'
}

function resetFilters(): void {
  filters.keyword = ''
  filters.status = ''
  currentPage.value = 1
}

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
  form.questions[questionIndex].options.push(createDefaultOption(form.questions[questionIndex].options.length))
  reindexQuestions()
}

function removeOption(questionIndex: number, optionIndex: number): void {
  const question = form.questions[questionIndex]
  if (question.options.length <= 2) {
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
    errorMessage.value = '请填写量表编码和量表名称'
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

async function loadScales(): Promise<void> {
  loading.value = true
  errorMessage.value = ''
  try {
    scales.value = await fetchAdminScalesApi()
    currentPage.value = 1
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    loading.value = false
  }
}

function openCreateDialog(): void {
  errorMessage.value = ''
  editingScaleId.value = null
  resetForm()
  dialogVisible.value = true
}

async function openEditDialog(scaleId: number): Promise<void> {
  dialogVisible.value = true
  dialogLoading.value = true
  errorMessage.value = ''
  editingScaleId.value = scaleId
  try {
    const detail = await fetchAdminScaleDetailApi(scaleId)
    syncForm(detail)
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
    dialogVisible.value = false
  } finally {
    dialogLoading.value = false
  }
}

function closeDialog(): void {
  dialogVisible.value = false
  editingScaleId.value = null
  currentQuestionIndex.value = 0
}

async function saveScale(): Promise<void> {
  errorMessage.value = ''
  if (!validateForm()) {
    return
  }

  saving.value = true
  try {
    const payload = buildPayload()
    if (editingScaleId.value) {
      await updateAdminScaleApi(editingScaleId.value, payload)
      ElMessage.success('量表已更新')
    } else {
      await createAdminScaleApi(payload)
      ElMessage.success('量表已创建')
    }
    closeDialog()
    await loadScales()
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    saving.value = false
  }
}

async function toggleScaleStatus(scale: AdminScale): Promise<void> {
  switchingStatus.value = true
  errorMessage.value = ''
  try {
    if (scale.status === 'ACTIVE') {
      await deactivateAdminScaleApi(scale.scaleId)
      ElMessage.success('量表已停用')
    } else {
      await activateAdminScaleApi(scale.scaleId)
      ElMessage.success('量表已启用')
    }
    await loadScales()
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    switchingStatus.value = false
  }
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

onMounted(() => {
  resetForm()
  void loadScales()
})
</script>

<template>
  <section class="admin-table-page">
    <div class="admin-table-shell">
      <header class="admin-table-header">
        <div>
          <h1>量表管理</h1>
          <p>统一查看量表状态、题目规模与风险阈值，新增和编辑都在弹窗内完成。</p>
        </div>
      </header>

      <p v-if="errorMessage" class="admin-table-alert">{{ errorMessage }}</p>

      <section class="admin-table-toolbar">
        <div class="admin-table-filters">
          <label class="admin-table-field admin-table-field--keyword">
            <span>关键词</span>
            <input v-model="filters.keyword" type="text" placeholder="量表名称 / 编码 / 描述" @keyup.enter="currentPage = 1">
          </label>
          <label class="admin-table-field">
            <span>状态</span>
            <select v-model="filters.status">
              <option value="">全部状态</option>
              <option value="ACTIVE">启用中</option>
              <option value="INACTIVE">已停用</option>
            </select>
          </label>
        </div>
        <div class="admin-table-actions">
          <button class="admin-table-button--secondary" type="button" @click="resetFilters">重置</button>
          <button class="admin-table-button--secondary" type="button" @click="loadScales">刷新</button>
          <button class="admin-table-button" type="button" @click="openCreateDialog">新增量表</button>
        </div>
      </section>

      <section class="admin-table-panel">
        <div class="admin-table-panel-header">
          <div>
            <h2 class="admin-table-panel-title">量表列表</h2>
            <p class="admin-table-panel-note">共 {{ filteredScales.length }} 条记录</p>
          </div>
        </div>

        <div class="admin-table-wrap">
          <table class="admin-table">
            <thead>
              <tr>
                <th>#</th>
                <th>量表编码</th>
                <th>量表名称</th>
                <th>状态</th>
                <th>题目数</th>
                <th>每页题数</th>
                <th>风险阈值</th>
                <th>更新时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(scale, index) in pagedScales" :key="scale.scaleId">
                <td>{{ rowIndex(index) }}</td>
                <td>{{ scale.code }}</td>
                <td>{{ scale.name }}</td>
                <td>
                  <span class="admin-table-status" :class="scale.status === 'ACTIVE' ? 'is-success' : 'is-warning'">
                    {{ resolveStatusText(scale.status) }}
                  </span>
                </td>
                <td>{{ scale.totalQuestions }}</td>
                <td>{{ scale.pageSize }}</td>
                <td>{{ scale.lowThreshold }} / {{ scale.mediumThreshold }} / {{ scale.highThreshold }}</td>
                <td>{{ scale.updatedAt ? new Date(scale.updatedAt).toLocaleString('zh-CN') : '--' }}</td>
                <td>
                  <div class="admin-table-ops">
                    <button class="admin-table-inline-btn" type="button" @click="openEditDialog(scale.scaleId)">编辑</button>
                    <button class="admin-table-inline-btn" type="button" :disabled="switchingStatus" @click="toggleScaleStatus(scale)">
                      {{ scale.status === 'ACTIVE' ? '停用' : '启用' }}
                    </button>
                  </div>
                </td>
              </tr>
              <tr v-if="!pagedScales.length">
                <td colspan="9" class="admin-table-empty">{{ loading ? '正在加载量表列表...' : '暂无量表数据' }}</td>
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

      <el-dialog v-model="dialogVisible" :title="isCreateMode ? '新增量表' : '编辑量表'" width="1040px" destroy-on-close>
        <div v-if="dialogLoading" class="admin-table-empty">正在加载量表详情...</div>
        <div v-else class="admin-table-dialog-body">
          <div class="admin-table-dialog-grid">
            <label class="admin-table-dialog-label">
              量表编码
              <input v-model="form.code" type="text" placeholder="例如：STRESS-CAMPUS-12">
            </label>
            <label class="admin-table-dialog-label">
              量表名称
              <input v-model="form.name" type="text" placeholder="请输入量表名称">
            </label>
            <label class="admin-table-dialog-label is-wide">
              量表简介
              <textarea v-model="form.description" rows="3" placeholder="用于描述量表用途"></textarea>
            </label>
            <label class="admin-table-dialog-label is-wide">
              作答引导语
              <textarea v-model="form.introduction" rows="3" placeholder="请输入作答说明"></textarea>
            </label>
            <label class="admin-table-dialog-label">
              每页题数
              <input v-model.number="form.pageSize" type="number" min="1">
            </label>
            <label class="admin-table-dialog-label">
              低风险阈值
              <input v-model.number="form.lowThreshold" type="number">
            </label>
            <label class="admin-table-dialog-label">
              中风险阈值
              <input v-model.number="form.mediumThreshold" type="number">
            </label>
            <label class="admin-table-dialog-label">
              高风险阈值
              <input v-model.number="form.highThreshold" type="number">
            </label>
          </div>

          <section class="admin-table-questions">
            <div class="admin-table-question-toolbar">
              <div>
                <strong>题目编辑</strong>
                <p class="admin-table-muted">当前第 {{ currentQuestionIndex + 1 }} / {{ totalQuestions }} 题</p>
              </div>
              <div class="admin-table-question-actions">
                <button class="admin-table-button--secondary" type="button" @click="addQuestion">新增题目</button>
                <button class="admin-table-button--secondary" type="button" :disabled="currentQuestionIndex <= 0" @click="goPrevQuestion">上一题</button>
                <button class="admin-table-button--secondary" type="button" :disabled="currentQuestionIndex >= totalQuestions - 1" @click="goNextQuestion">下一题</button>
              </div>
            </div>

            <div v-if="currentQuestion" class="admin-table-dialog-body">
              <div class="admin-table-panel-header">
                <div>
                  <h3 class="admin-table-panel-title">题目 {{ currentQuestion.questionNo }}</h3>
                  <p class="admin-table-panel-note">编辑题干、必答状态和选项分值。</p>
                </div>
                <button class="admin-table-button--danger" type="button" @click="removeQuestion(currentQuestionIndex)">删除题目</button>
              </div>

              <div class="admin-table-dialog-grid">
                <label class="admin-table-dialog-label is-wide">
                  题干
                  <textarea v-model="currentQuestion.content" rows="3" placeholder="请输入题目内容"></textarea>
                </label>
                <label class="admin-table-dialog-label">
                  是否必答
                  <select v-model.number="currentQuestion.requiredFlag">
                    <option :value="1">必答</option>
                    <option :value="0">选答</option>
                  </select>
                </label>
              </div>

              <div v-for="(option, optionIndex) in currentQuestion.options" :key="`${currentQuestion.questionNo}-${optionIndex}`" class="admin-table-option-row">
                <label class="admin-table-dialog-label">
                  选项编号
                  <input v-model="option.optionCode" type="text">
                </label>
                <label class="admin-table-dialog-label">
                  选项内容
                  <input v-model="option.content" type="text" placeholder="请输入选项文案">
                </label>
                <label class="admin-table-dialog-label">
                  分值
                  <input v-model.number="option.score" type="number">
                </label>
                <button class="admin-table-button--danger" type="button" @click="removeOption(currentQuestionIndex, optionIndex)">删除选项</button>
              </div>

              <div class="admin-table-actions">
                <button class="admin-table-button--secondary" type="button" @click="addOption(currentQuestionIndex)">新增选项</button>
              </div>
            </div>
          </section>
        </div>

        <template #footer>
          <div class="admin-table-dialog-footer">
            <button class="admin-table-button--secondary" type="button" @click="closeDialog">取消</button>
            <button class="admin-table-button" type="button" :disabled="saving" @click="saveScale">
              {{ saving ? '保存中...' : '保存量表' }}
            </button>
          </div>
        </template>
      </el-dialog>
    </div>
  </section>
</template>

<style scoped>
@import './admin-table.css';
</style>
