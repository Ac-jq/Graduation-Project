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
  <section class="admin-scale-detail-page">
    <div class="page-shell">
      <header class="hero-copy">
        <p class="eyebrow">量表详情</p>
        <h1>{{ scaleDetail ? '编辑量表' : '新增量表' }}</h1>
      </header>

      <p v-if="errorMessage" class="error-text">{{ errorMessage }}</p>

      <section class="glass-panel panel">
        <div class="form-grid">
          <label><span>编码</span><input v-model="form.code" type="text"></label>
          <label><span>名称</span><input v-model="form.name" type="text"></label>
          <label class="wide"><span>描述</span><input v-model="form.description" type="text"></label>
          <label class="wide"><span>引导语</span><textarea v-model="form.introduction" rows="4" /></label>
          <label><span>分页大小</span><input v-model.number="form.pageSize" type="number"></label>
          <label><span>低阈值</span><input v-model.number="form.lowThreshold" type="number"></label>
          <label><span>中阈值</span><input v-model.number="form.mediumThreshold" type="number"></label>
          <label><span>高阈值</span><input v-model.number="form.highThreshold" type="number"></label>
        </div>

        <article class="question-panel">
          <h3>题目结构概览</h3>
          <div v-if="form.questions.length" class="question-stack">
            <div v-for="question in form.questions" :key="question.questionNo" class="question-card">
              <p>Q{{ question.questionNo }} · {{ question.content }}</p>
              <small>选项 {{ question.options.length }} · requiredFlag {{ question.requiredFlag }}</small>
            </div>
          </div>
          <p v-else class="state-text">当前量表没有题目数据。</p>
        </article>

        <div class="action-row">
          <button class="primary-button" type="button" :disabled="saving" @click="saveScale">保存量表</button>
          <button v-if="scaleDetail" class="ghost-button" type="button" :disabled="switchingStatus" @click="toggleScaleStatus">
            {{ scaleDetail.status === 'ACTIVE' ? 'Deactivate scale' : 'Activate scale' }}
          </button>
        </div>
      </section>
    </div>
  </section>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700&family=Noto+Serif+SC:wght@400;500;600;700&display=swap');
.admin-scale-detail-page{min-height:100vh;padding:44px 28px 72px;color:#272f27;background:linear-gradient(180deg,#f4efe6 0%,#f8f4ed 100%)}.page-shell{max-width:1320px;margin:0 auto}.hero-copy{border-top:1px solid rgba(59,69,59,.16);padding-top:18px;margin-bottom:28px}.eyebrow,.form-grid span{margin:0 0 10px;font:700 .76rem/1 'Manrope',sans-serif;letter-spacing:.22em;text-transform:uppercase;color:#7b6857}.hero-copy h1,.question-panel h3{margin:0;font-family:'Noto Serif SC',serif;font-weight:600}.hero-copy h1{font-size:clamp(2rem,3vw,3.2rem);line-height:1.16}.panel,.question-card{border:1px solid rgba(77,86,77,.14);background:rgba(255,252,247,.76);box-shadow:0 24px 70px rgba(91,80,66,.08);backdrop-filter:blur(16px)}.panel{padding:24px}.form-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:14px}.form-grid label{display:grid;gap:8px}.wide{grid-column:1/-1}input,textarea{width:100%;box-sizing:border-box;border:1px solid rgba(80,88,79,.16);background:rgba(255,255,255,.74);padding:14px 16px;font:500 .95rem/1.6 'Manrope',sans-serif;color:#272f27;outline:none;resize:vertical}.question-panel{margin-top:18px}.question-stack{display:grid;gap:12px;margin-top:12px}.question-card{padding:14px}.question-card p,.question-card small,.state-text,.error-text{font-family:'Manrope',sans-serif}.question-card p{margin:0;color:#272f27}.question-card small,.state-text{display:block;margin-top:8px;color:rgba(39,47,39,.62)}.action-row{display:flex;flex-wrap:wrap;gap:12px;margin-top:18px}.primary-button,.ghost-button{padding:12px 16px;font:700 .82rem/1 'Manrope',sans-serif;letter-spacing:.08em;text-transform:uppercase;cursor:pointer}.primary-button{border:none;background:linear-gradient(135deg,#253128 0%,#47564b 100%);color:#f8f5ef}.ghost-button{border:1px solid rgba(54,65,56,.2);background:rgba(255,255,255,.58);color:#272f27}.error-text{margin-bottom:16px;color:#a44f46}
@media (max-width:980px){.admin-scale-detail-page{padding:28px 16px 46px}.form-grid{grid-template-columns:1fr}}
</style>

