<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { createScaleDraftSessionApi, fetchScaleDetailApi } from '@/api/assessment'
import type { ScaleDetail, ScaleSession } from '@/api/types'
import { useAssessmentStore } from '@/stores/assessment'
import { toErrorMessage, toNumberParam } from '@/views/shared/page-logic'

const route = useRoute()
const router = useRouter()
const assessmentStore = useAssessmentStore()

const loading = ref(false)
const creating = ref(false)
const errorMessage = ref('')
const scaleDetail = ref<ScaleDetail | null>(null)
const draftSession = ref<ScaleSession | null>(null)

const scaleId = computed(() => toNumberParam(route.params.scaleId))

const estimatedMinutes = computed(() => {
  const totalQuestions = scaleDetail.value?.totalQuestions ?? 0
  if (!totalQuestions) return '--'
  return Math.max(3, Math.ceil(totalQuestions / 4))
})

function resolveScaleAccent(code?: string | null): string {
  switch (code) {
    case 'GAD7':
      return '焦虑感受梳理'
    case 'PHQ9':
      return '情绪状态梳理'
    default:
      return '温和自我觉察'
  }
}

async function loadScaleDetail(): Promise<void> {
  if (!scaleId.value) {
    errorMessage.value = '量表编号无效'
    scaleDetail.value = null
    return
  }
  loading.value = true
  errorMessage.value = ''
  try {
    const detail = await fetchScaleDetailApi(scaleId.value)
    scaleDetail.value = detail
    assessmentStore.setCurrentScale(detail)
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    loading.value = false
  }
}

async function startAssessment(): Promise<void> {
  if (!scaleId.value) {
    errorMessage.value = '量表编号无效'
    return
  }
  creating.value = true
  errorMessage.value = ''
  try {
    const session = await createScaleDraftSessionApi(scaleId.value)
    draftSession.value = session
    assessmentStore.resetSessionState()
    assessmentStore.setCurrentScale(scaleDetail.value)
    assessmentStore.setCurrentSession(session)
    await router.push({
      name: 'student-assessment-session',
      params: { sessionId: session.sessionId }
    })
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    creating.value = false
  }
}

watch(
    () => route.params.scaleId,
    () => {
      void loadScaleDetail()
    }
)

onMounted(() => {
  void loadScaleDetail()
})
</script>

<template>
  <main class="premium-intro-page">
    <div class="premium-card">

      <div v-if="errorMessage" class="state-container">
        <h2 class="error-text">{{ errorMessage }}</h2>
        <button class="ghost-btn" @click="router.push({ name: 'student-scales' })">返回目录</button>
      </div>

      <div v-else-if="loading" class="state-container">
        <div class="loading-orb"></div>
        <p class="meta-text">正在准备情绪画卷...</p>
      </div>

      <div v-else-if="scaleDetail" class="content-container">

        <div class="top-section">
          <div class="tags-row">
            <span class="premium-tag">{{ resolveScaleAccent(scaleDetail?.code) }}</span>
            <span class="premium-tag tag-light">预计 {{ estimatedMinutes }} 分钟</span>
          </div>
        </div>

        <div class="middle-section">
          <h1 class="main-title">{{ scaleDetail?.name || '探索内在的平静' }}</h1>
          <p class="sub-title">
            先让心绪落地，再开始回答。<br>
            不需要追求完美的答案，只需如实记录此刻的感受。
          </p>
        </div>

        <div class="bottom-section">
          <button
              class="start-action-btn"
              :disabled="creating"
              @click="startAssessment"
          >
            {{ creating ? '正在进入...' : '开始本次测评' }}
          </button>
          <button class="ghost-link" @click="router.push({ name: 'student-scales' })">
            暂不开始，返回目录
          </button>
        </div>

      </div>
    </div>
  </main>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600&family=Noto+Serif+SC:wght@500;600;700&display=swap');

/* 全屏背景，营造大空间的呼吸感 */
.premium-intro-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  /* 页面底色：极度干净的冷调米色/浅灰绿 */
  background: #f4f6f4;
  padding: 5vw 2rem;
  box-sizing: border-box;
}

/* 居中的高级感卡片 */
.premium-card {
  width: 100%;
  max-width: 680px;
  min-height: 560px;
  /* 背景色：提取之前的氛围色调，加透明度 */
  background: linear-gradient(
      145deg,
      rgba(219, 230, 222, 0.65) 0%,
      rgba(238, 228, 218, 0.55) 100%
  );
  backdrop-filter: blur(32px);
  -webkit-backdrop-filter: blur(32px);
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 40px;
  padding: 4rem;
  box-sizing: border-box;
  /* 柔和深邃的弥散阴影 */
  box-shadow:
      0 40px 80px rgba(54, 66, 58, 0.08),
      inset 0 2px 0 rgba(255, 255, 255, 0.6);
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
}

/* 上下结构容器 */
.content-container {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  flex: 1;
  height: 100%;
}

.top-section {
  display: flex;
  justify-content: center;
}

.tags-row {
  display: flex;
  gap: 1rem;
  align-items: center;
}

.premium-tag {
  font-family: 'Manrope', 'Noto Serif SC', sans-serif;
  font-size: 0.85rem;
  letter-spacing: 0.1em;
  color: #4a5c51;
  background: rgba(255, 255, 255, 0.6);
  padding: 0.6rem 1.2rem;
  border-radius: 100px;
  font-weight: 600;
}

.tag-light {
  background: transparent;
  border: 1px solid rgba(130, 150, 138, 0.3);
  color: #6a7c70;
}

.middle-section {
  text-align: center;
  margin: 4rem 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex: 1;
}

.main-title {
  font-family: 'Noto Serif SC', serif;
  font-size: clamp(2.5rem, 5vw, 3.8rem);
  font-weight: 600;
  color: #1e2821;
  line-height: 1.2;
  margin: 0 0 1.5rem 0;
  letter-spacing: 0.05em;
}

.sub-title {
  font-family: 'Noto Serif SC', serif;
  font-size: 1.15rem;
  color: #5c6b60;
  line-height: 2;
  max-width: 80%;
  margin: 0;
}

.bottom-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1.5rem;
}

/* 夸张而精致的按钮 */
.start-action-btn {
  width: 100%;
  max-width: 320px;
  height: 4.2rem;
  border-radius: 100px;
  border: none;
  background: #2a362e;
  color: #ffffff;
  font-family: 'Noto Serif SC', serif;
  font-size: 1.1rem;
  font-weight: 600;
  letter-spacing: 0.2em;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1);
  box-shadow: 0 12px 24px rgba(42, 54, 46, 0.2);
}

.start-action-btn:hover:not(:disabled) {
  transform: translateY(-4px) scale(1.02);
  box-shadow: 0 20px 40px rgba(42, 54, 46, 0.3);
  background: #1c2620;
}

.start-action-btn:active:not(:disabled) {
  transform: translateY(2px) scale(0.98);
}

.start-action-btn:disabled {
  background: #7a8c80;
  cursor: not-allowed;
  box-shadow: none;
}

.ghost-link {
  background: none;
  border: none;
  color: #7b8c80;
  font-family: 'Manrope', 'Noto Serif SC', sans-serif;
  font-size: 0.9rem;
  cursor: pointer;
  transition: color 0.3s ease;
  padding: 0.5rem;
}

.ghost-link:hover {
  color: #2a362e;
}

/* 状态样式 */
.state-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  text-align: center;
  flex: 1;
}

.error-text {
  color: #8c4a4a;
  font-family: 'Noto Serif SC', serif;
  margin-bottom: 2rem;
}

.ghost-btn {
  padding: 0.8rem 2rem;
  border-radius: 100px;
  border: 1px solid #8c4a4a;
  background: transparent;
  color: #8c4a4a;
  cursor: pointer;
}

.loading-orb {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: rgba(130, 150, 138, 0.2);
  border: 2px solid rgba(130, 150, 138, 0.8);
  border-top-color: transparent;
  animation: spin 1s linear infinite;
  margin-bottom: 1.5rem;
}

.meta-text {
  color: #6a7c70;
  font-family: 'Noto Serif SC', serif;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 响应式调整 */
@media (max-width: 768px) {
  .premium-card {
    padding: 2.5rem 1.5rem;
    min-height: 80vh;
    border-radius: 32px;
  }
  .main-title {
    font-size: 2.2rem;
  }
  .sub-title {
    max-width: 100%;
    font-size: 1rem;
  }
}
</style>