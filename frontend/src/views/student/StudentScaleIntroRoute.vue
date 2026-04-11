<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { createScaleDraftSessionApi, fetchScaleDetailApi } from '@/api/assessment'
import type { ScaleDetail, ScaleSession } from '@/api/types'
import { toErrorMessage, toNumberParam } from '@/views/shared/page-logic'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const creating = ref(false)
const errorMessage = ref('')
const scaleDetail = ref<ScaleDetail | null>(null)
const draftSession = ref<ScaleSession | null>(null)

async function loadScaleDetail(scaleId: number): Promise<void> {
  loading.value = true
  errorMessage.value = ''

  try {
    scaleDetail.value = await fetchScaleDetailApi(scaleId)
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    loading.value = false
  }
}

async function startAssessment(): Promise<void> {
  const scaleId = toNumberParam(route.params.scaleId)
  if (!scaleId) {
    errorMessage.value = '无效的量表编号'
    return
  }

  creating.value = true
  errorMessage.value = ''

  try {
    const session = await createScaleDraftSessionApi(scaleId)
    draftSession.value = session
    await router.push({ name: 'student-assessment-session', params: { sessionId: session.sessionId } })
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    creating.value = false
  }
}

async function syncByRoute(): Promise<void> {
  const scaleId = toNumberParam(route.params.scaleId)
  if (!scaleId) {
    errorMessage.value = '无效的量表编号'
    scaleDetail.value = null
    return
  }

  await loadScaleDetail(scaleId)
}

watch(() => route.params.scaleId, () => {
  void syncByRoute()
})

onMounted(() => {
  void syncByRoute()
})
</script>

<template>
  <div class="healing-viewport">
    <div class="ambient-background"></div>
    <div class="noise-texture"></div>

    <main class="intro-container">

      <nav class="top-nav">
        <button class="glass-btn back-btn" @click="router.push({ name: 'student-scales' })">
          <span class="arrow">←</span>
          <span>返回目录</span>
        </button>
      </nav>

      <div v-if="loading" class="status-card">
        <div class="loader-pulse"></div>
        <p>正在为您准备测评空间...</p>
      </div>

      <div v-else-if="errorMessage" class="status-card error">
        <p>{{ errorMessage }}</p>
      </div>

      <article v-else class="glass-card main-content">

        <header class="scale-header">
          <div class="header-tags">
            <span class="tag">心理测评</span>
            <span class="tag code">NO. {{ scaleDetail?.code || '...' }}</span>
          </div>
          <h1 class="scale-title">{{ scaleDetail?.name || '载入中...' }}</h1>
          <p class="scale-summary">{{ scaleDetail?.description || '一份帮助你了解当下内心状态的心理学量表。' }}</p>

          <div class="metrics-row">
            <div class="metric-item">
              <span class="val">{{ scaleDetail?.totalQuestions || '--' }}</span>
              <span class="lbl">总题数</span>
            </div>
            <div class="metric-divider"></div>
            <div class="metric-item">
              <span class="val">{{ scaleDetail?.pageSize || '--' }}</span>
              <span class="lbl">每页题数</span>
            </div>
          </div>
        </header>

        <div class="scale-body">
          <section class="info-section">
            <h2 class="section-title">导言与说明</h2>
            <div class="text-content">
              <p v-if="scaleDetail?.introduction">{{ scaleDetail.introduction }}</p>
              <p v-else class="placeholder-text">深呼吸，找一个安静的角落。请根据你最近一周的真实感受进行选择，答案没有对错之分。</p>
            </div>
          </section>

          <section class="info-section">
            <h2 class="section-title">作答流程</h2>
            <div class="steps-grid">
              <div class="step-card">
                <span class="step-num">01</span>
                <p>如实作答，遵从第一直觉</p>
              </div>
              <div class="step-card">
                <span class="step-num">02</span>
                <p>系统将自动保存您的草稿</p>
              </div>
              <div class="step-card">
                <span class="step-num">03</span>
                <p>完成后获取专业解读报告</p>
              </div>
            </div>
          </section>
        </div>

        <footer class="scale-footer">
          <div v-if="draftSession" class="draft-alert">
            <span class="icon">📝</span>
            <span>您有一个未完成的会话，点击继续作答。</span>
          </div>

          <button
              class="primary-action-btn"
              :class="{ 'is-loading': creating }"
              :disabled="creating"
              @click="startAssessment"
          >
            <span class="btn-text">{{ creating ? '正在开启...' : '开始测评' }}</span>
            <div class="btn-arrow-circle">→</div>
          </button>
        </footer>

      </article>

    </main>
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600&family=Noto+Serif+SC:wght@400;500;600&display=swap');

/* =========================================
   底层环境：莫兰迪色系与柔和渐变
========================================= */
.healing-viewport {
  --color-sage: #8DA393;     /* 莫兰迪绿 */
  --color-sand: #E8E5DF;     /* 温柔米白 */
  --color-ink: #2C352D;      /* 深灰绿（代替纯黑文字） */
  --color-muted: #7A857B;    /* 浅灰绿（次级文字） */
  --glass-bg: rgba(255, 255, 255, 0.65);
  --glass-border: rgba(255, 255, 255, 0.8);
  --shadow-soft: 0 24px 48px rgba(141, 163, 147, 0.15);

  position: relative;
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 4rem 2rem;
  font-family: 'Manrope', 'Noto Serif SC', sans-serif;
  color: var(--color-ink);
  overflow-x: hidden;
}

.ambient-background {
  position: absolute;
  inset: 0;
  background:
      radial-gradient(circle at 15% 0%, rgba(206, 214, 201, 0.8) 0%, transparent 40%),
      radial-gradient(circle at 85% 100%, rgba(224, 216, 203, 0.8) 0%, transparent 40%),
      var(--color-sand);
  z-index: 0;
}

.noise-texture {
  position: absolute;
  inset: 0;
  opacity: 0.03;
  pointer-events: none;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noiseFilter'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.85' numOctaves='3' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noiseFilter)'/%3E%3C/svg%3E");
  z-index: 1;
}

/* =========================================
   主容器布局
========================================= */
.intro-container {
  position: relative;
  z-index: 10;
  width: 100%;
  max-width: 860px; /* 限制阅读宽度，增加留白 */
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  animation: float-up 0.8s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes float-up {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

/* =========================================
   通用毛玻璃组件 (Glassmorphism)
========================================= */
.glass-btn {
  background: var(--glass-bg);
  border: 1px solid var(--glass-border);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  padding: 0.6rem 1.2rem;
  border-radius: 12px; /* 柔和圆角 */
  color: var(--color-ink);
  font-size: 0.9rem;
  font-weight: 500;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(0,0,0,0.02);
}

.glass-btn:hover {
  transform: translateY(-2px);
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 8px 16px rgba(141, 163, 147, 0.1);
}

.glass-card {
  background: var(--glass-bg);
  border: 1px solid var(--glass-border);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border-radius: 24px; /* 更大的圆角体现包裹感 */
  box-shadow: var(--shadow-soft), inset 0 0 0 1px rgba(255,255,255,0.5); /* 内发光增强质感 */
  padding: 4rem;
}

/* =========================================
   卡片内部：头部区
========================================= */
.scale-header {
  text-align: center;
  border-bottom: 1px solid rgba(122, 133, 123, 0.15);
  padding-bottom: 3rem;
  margin-bottom: 3rem;
}

.header-tags {
  display: flex;
  justify-content: center;
  gap: 0.8rem;
  margin-bottom: 1.5rem;
}

.tag {
  background: rgba(141, 163, 147, 0.15);
  color: var(--color-sage);
  padding: 0.4rem 1rem;
  border-radius: 20px;
  font-size: 0.8rem;
  font-weight: 600;
  letter-spacing: 0.05em;
}

.tag.code {
  font-family: 'Manrope', sans-serif;
  background: transparent;
  border: 1px solid rgba(141, 163, 147, 0.3);
}

.scale-title {
  font-family: 'Noto Serif SC', serif;
  font-size: clamp(2rem, 4vw, 2.8rem);
  font-weight: 500;
  color: var(--color-ink);
  margin: 0 0 1rem 0;
  line-height: 1.2;
}

.scale-summary {
  font-size: 1.05rem;
  color: var(--color-muted);
  line-height: 1.8;
  max-width: 80%;
  margin: 0 auto 2.5rem auto;
}

.metrics-row {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 3rem;
}

.metric-item {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
}

.metric-item .val {
  font-size: 1.8rem;
  font-weight: 600;
  font-family: 'Manrope', sans-serif;
  color: var(--color-ink);
}

.metric-item .lbl {
  font-size: 0.75rem;
  color: var(--color-muted);
  text-transform: uppercase;
  letter-spacing: 0.1em;
}

.metric-divider {
  width: 1px;
  height: 30px;
  background: rgba(122, 133, 123, 0.2);
}

/* =========================================
   卡片内部：正文区
========================================= */
.scale-body {
  display: flex;
  flex-direction: column;
  gap: 3rem;
}

.section-title {
  font-family: 'Noto Serif SC', serif;
  font-size: 1.3rem;
  font-weight: 600;
  margin: 0 0 1.5rem 0;
  color: var(--color-ink);
  display: flex;
  align-items: center;
  gap: 0.8rem;
}

.section-title::before {
  content: '';
  display: block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--color-sage);
}

.text-content p {
  font-size: 1rem;
  line-height: 1.9;
  color: var(--color-ink);
  opacity: 0.85;
  margin: 0;
}

.placeholder-text {
  font-style: italic;
  color: var(--color-muted) !important;
}

/* 流程卡片网格 */
.steps-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1.5rem;
}

.step-card {
  background: rgba(255, 255, 255, 0.4);
  border: 1px solid rgba(255, 255, 255, 0.6);
  padding: 1.5rem;
  border-radius: 16px; /* 统一 16px 圆角 */
  transition: transform 0.3s ease, background 0.3s ease;
}

.step-card:hover {
  transform: translateY(-4px);
  background: rgba(255, 255, 255, 0.8);
}

.step-num {
  font-family: 'Manrope', sans-serif;
  font-size: 1.2rem;
  font-weight: 600;
  color: var(--color-sage);
  display: block;
  margin-bottom: 0.5rem;
}

.step-card p {
  margin: 0;
  font-size: 0.9rem;
  line-height: 1.5;
  color: var(--color-muted);
}

/* =========================================
   卡片内部：底部操作区
========================================= */
.scale-footer {
  margin-top: 4rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1.5rem;
}

.draft-alert {
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid #E8D3C5;
  color: #B57B59;
  padding: 0.8rem 1.5rem;
  border-radius: 12px;
  font-size: 0.9rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

/* 核心高亮按钮 */
.primary-action-btn {
  background: linear-gradient(135deg, #8DA393 0%, #6B8071 100%); /* 莫兰迪绿渐变 */
  color: #FFFFFF;
  border: none;
  padding: 0.6rem 0.6rem 0.6rem 2.5rem;
  border-radius: 100px; /* 胶囊型圆角，极具亲和力 */
  display: flex;
  align-items: center;
  gap: 1.5rem;
  cursor: pointer;
  box-shadow: 0 12px 24px rgba(107, 128, 113, 0.25);
  transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1);
}

.primary-action-btn:hover:not(:disabled) {
  transform: translateY(-4px);
  box-shadow: 0 16px 32px rgba(107, 128, 113, 0.35);
  background: linear-gradient(135deg, #9CB3A3 0%, #768C7D 100%);
}

.primary-action-btn:active:not(:disabled) {
  transform: scale(0.98);
}

.primary-action-btn.is-loading {
  opacity: 0.7;
  cursor: wait;
}

.btn-text {
  font-size: 1.1rem;
  font-weight: 500;
  letter-spacing: 0.05em;
}

.btn-arrow-circle {
  width: 44px;
  height: 44px;
  background: #FFFFFF;
  color: #6B8071;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.2rem;
  transition: transform 0.3s ease;
}

.primary-action-btn:hover:not(:disabled) .btn-arrow-circle {
  transform: translateX(4px);
}

/* =========================================
   状态加载
========================================= */
.status-card {
  background: var(--glass-bg);
  backdrop-filter: blur(12px);
  border-radius: 24px;
  padding: 4rem;
  text-align: center;
  color: var(--color-muted);
}

.loader-pulse {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--color-sage);
  margin: 0 auto 1.5rem auto;
  animation: pulse-glow 1.5s infinite;
}

@keyframes pulse-glow {
  0% { transform: scale(0.8); opacity: 0.5; }
  50% { transform: scale(1.2); opacity: 0.2; }
  100% { transform: scale(0.8); opacity: 0.5; }
}

/* =========================================
   响应式调整
========================================= */
@media (max-width: 768px) {
  .healing-viewport {
    padding: 1rem;
  }

  .glass-card {
    padding: 2.5rem 1.5rem;
    border-radius: 20px;
  }

  .metrics-row {
    gap: 1.5rem;
  }

  .steps-grid {
    grid-template-columns: 1fr;
  }
}
</style>