<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { activateAdminScaleApi, createAdminScaleApi, deactivateAdminScaleApi, fetchAdminScaleDetailApi, updateAdminScaleApi } from '@/api/admin-scale'
import type { AdminScale, UpsertAdminScaleRequest } from '@/api/types'
import { toErrorMessage, toNumberParam } from '@/views/shared/page-logic'

const route = useRoute()
const loading = ref(false)
const saving = ref(false)
const switchingStatus = ref(false)
const errorMessage = ref('')
const scaleDetail = ref<AdminScale | null>(null)
const scaleId = computed(() => toNumberParam(route.params.scaleId))
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

function syncForm(data: AdminScale): void {
  form.code = data.code
  form.name = data.name
  form.description = data.description
  form.introduction = data.introduction
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
}

function resolveStatusText(status?: string): string {
  return status === 'ACTIVE' ? '启用中' : status === 'INACTIVE' ? '已停用' : status || '未标记'
}

async function loadScaleDetail(): Promise<void> {
  if (!scaleId.value) {
    scaleDetail.value = null
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
  saving.value = true
  errorMessage.value = ''

  try {
    scaleDetail.value = scaleId.value
      ? await updateAdminScaleApi(scaleId.value, form)
      : await createAdminScaleApi(form)

    if (scaleDetail.value) {
      syncForm(scaleDetail.value)
    }
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    saving.value = false
  }
}

async function toggleScaleStatus(): Promise<void> {
  if (!scaleId.value || !scaleDetail.value) {
    errorMessage.value = 'Invalid scaleId'
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
          <p class="admin-editorial-lead">表单字段、保存动作和启停逻辑保持不变，界面只改成更克制、更接近学生端的编辑体验。</p>
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

      <div class="admin-editorial-grid">
        <section class="admin-editorial-panel admin-editorial-panel--mesh">
          <div class="admin-editorial-section">
            <p class="admin-editorial-kicker">基础配置</p>
            <h2>编辑量表元信息与阈值</h2>
          </div>

          <div class="admin-editorial-form">
            <label class="admin-editorial-field">
              <span>编码</span>
              <input v-model="form.code" type="text">
            </label>
            <label class="admin-editorial-field">
              <span>名称</span>
              <input v-model="form.name" type="text">
            </label>
            <label class="admin-editorial-field wide">
              <span>描述</span>
              <input v-model="form.description" type="text">
            </label>
            <label class="admin-editorial-field wide">
              <span>引导语</span>
              <textarea v-model="form.introduction" rows="4" />
            </label>
            <label class="admin-editorial-field">
              <span>分页大小</span>
              <input v-model.number="form.pageSize" type="number">
            </label>
            <label class="admin-editorial-field">
              <span>低阈值</span>
              <input v-model.number="form.lowThreshold" type="number">
            </label>
            <label class="admin-editorial-field">
              <span>中阈值</span>
              <input v-model.number="form.mediumThreshold" type="number">
            </label>
            <label class="admin-editorial-field">
              <span>高阈值</span>
              <input v-model.number="form.highThreshold" type="number">
            </label>
          </div>

          <div class="admin-editorial-actions" style="margin-top: 1rem;">
            <button class="admin-editorial-button" type="button" :disabled="saving" @click="saveScale">保存量表</button>
            <button v-if="scaleDetail" class="admin-editorial-ghost" type="button" :disabled="switchingStatus" @click="toggleScaleStatus">
              {{ scaleDetail.status === 'ACTIVE' ? '停用量表' : '启用量表' }}
            </button>
          </div>
        </section>

        <section class="admin-editorial-panel">
          <div class="admin-editorial-section">
            <p class="admin-editorial-kicker">结构概览</p>
            <h2>题目与选项数量快速预览</h2>
          </div>

          <div v-if="loading" class="admin-editorial-empty">正在读取量表详情…</div>
          <div v-else-if="form.questions.length" class="admin-editorial-board">
            <article v-for="question in form.questions" :key="question.questionNo" class="admin-editorial-card">
              <p class="admin-editorial-code">题目 {{ question.questionNo }}</p>
              <h3>{{ question.content }}</h3>
              <div class="admin-editorial-meta">
                <span>选项 {{ question.options.length }}</span>
                <span>requiredFlag {{ question.requiredFlag }}</span>
              </div>
            </article>
          </div>
          <div v-else class="admin-editorial-empty">当前量表没有题目数据。</div>
        </section>
      </div>
    </div>
  </section>
</template>

<style scoped>
@import './admin-editorial.css';
</style>
