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

async function start测评(): Promise<void> {
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
  <main class="scale-intro-page">
    <section class="scale-intro-page__hero">
      <div class="scale-intro-page__heading">
        <p class="scale-intro-page__eyebrow">量表详情</p>
        <h1 class="scale-intro-page__title">
          {{ scaleDetail?.name || '测评详情' }}
        </h1>
        <p class="scale-intro-page__summary">
          {{ scaleDetail?.description || '这里展示量表的简介、作答方式和开始测评的入口。' }}
        </p>
      </div>

      <aside class="scale-intro-page__data-ribbon">
        <div>
          <span>编号</span>
          <strong>{{ scaleDetail?.code || '—' }}</strong>
        </div>
        <div>
          <span>题目数</span>
          <strong>{{ scaleDetail?.totalQuestions || 0 }}</strong>
        </div>
        <div>
          <span>每页题数</span>
          <strong>{{ scaleDetail?.pageSize || 0 }}</strong>
        </div>
      </aside>
    </section>

    <section class="scale-intro-page__body">
      <article class="scale-intro-page__panel scale-intro-page__panel--intro">
        <p class="scale-intro-page__panel-kicker">量表说明</p>
        <p v-if="loading" class="scale-intro-page__status">正在载入量表说明...</p>
        <p v-else class="scale-intro-page__intro-text">
          {{ scaleDetail?.introduction || '暂无补充说明。' }}
        </p>
      </article>

      <article class="scale-intro-page__panel scale-intro-page__panel--action">
        <p class="scale-intro-page__panel-kicker">测评流程</p>
        <div class="scale-intro-page__flow">
          <div>
            <span>01</span>
            <p>阅读量表说明，确认自己当前适合进入作答流程。</p>
          </div>
          <div>
            <span>02</span>
            <p>逐页完成题目选择，系统会实时保存你的草稿状态。</p>
          </div>
          <div>
            <span>03</span>
            <p>提交后生成正式报告，并同步展示风险等级与解读建议。</p>
          </div>
        </div>

        <p v-if="errorMessage" class="scale-intro-page__alert">
          {{ errorMessage }}
        </p>

        <p v-if="draftSession" class="scale-intro-page__draft">
          当前草稿会话：#{{ draftSession.sessionId }} / {{ draftSession.status }}
        </p>

        <button class="scale-intro-page__action" type="button" :disabled="creating" @click="start测评">
          {{ creating ? '准备中...' : '开始测评' }}
        </button>
      </article>
    </section>
  </main>
</template>
<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700&family=Noto+Serif+SC:wght@400;500;600;700&display=swap');

.scale-intro-page {
  --paper: #f7f2e9;
  --ink: #211d18;
  --muted: #71695f;
  --line: rgba(33, 29, 24, 0.11);
  --accent: #70846f;
  min-height: 100vh;
  padding: 2rem;
  background:
    radial-gradient(circle at right top, rgba(167, 180, 167, 0.16), transparent 26%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.18), transparent 35%),
    var(--paper);
  color: var(--ink);
}

.scale-intro-page__hero {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) minmax(260px, 0.7fr);
  gap: 1.6rem;
  padding-bottom: 1.35rem;
  border-bottom: 1px solid var(--line);
  align-items: end;
}

.scale-intro-page__eyebrow,
.scale-intro-page__panel-kicker,
.scale-intro-page__data-ribbon span,
.scale-intro-page__flow span {
  margin: 0;
  font: 600 0.72rem/1.4 'Manrope', sans-serif;
  letter-spacing: 0.17em;
  text-transform: uppercase;
  color: var(--muted);
}

.scale-intro-page__title {
  margin: 0.95rem 0 0;
  font: 600 clamp(2.5rem, 4.8vw, 5rem)/1.02 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.scale-intro-page__summary {
  max-width: 40rem;
  margin: 1.1rem 0 0;
  color: var(--muted);
  font: 400 1rem/1.9 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.scale-intro-page__data-ribbon {
  display: grid;
  gap: 1rem;
  padding: 1.15rem;
  border: 1px solid var(--line);
  background: rgba(255, 251, 245, 0.62);
  backdrop-filter: blur(18px);
}

.scale-intro-page__data-ribbon strong {
  display: block;
  margin-top: 0.45rem;
  font: 600 1.5rem/1.2 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.scale-intro-page__body {
  margin-top: 1.6rem;
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(320px, 0.95fr);
  gap: 1.4rem;
}

.scale-intro-page__panel {
  padding: 1.4rem;
  border: 1px solid var(--line);
  background: rgba(255, 252, 247, 0.68);
  backdrop-filter: blur(18px);
}

.scale-intro-page__status,
.scale-intro-page__intro-text,
.scale-intro-page__flow p,
.scale-intro-page__draft {
  color: var(--muted);
  font: 400 0.98rem/1.9 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.scale-intro-page__intro-text,
.scale-intro-page__status {
  margin: 1.1rem 0 0;
}

.scale-intro-page__flow {
  margin-top: 1.2rem;
  display: grid;
  gap: 1rem;
}

.scale-intro-page__flow div {
  padding: 0.95rem 0;
  border-top: 1px solid var(--line);
}

.scale-intro-page__flow div:first-child {
  border-top: none;
  padding-top: 0;
}

.scale-intro-page__flow p {
  margin: 0.55rem 0 0;
}

.scale-intro-page__alert {
  margin: 1rem 0 0;
  color: #8c4949;
  font: 500 0.92rem/1.6 'Manrope', sans-serif;
}

.scale-intro-page__draft {
  margin: 1rem 0 0;
}

.scale-intro-page__action {
  margin-top: 1.4rem;
  min-width: 11rem;
  min-height: 3.25rem;
  border: none;
  background: linear-gradient(135deg, #70846f, #4f6653);
  color: #f7f3ed;
  font: 600 0.92rem/1 'Manrope', sans-serif;
  letter-spacing: 0.13em;
  text-transform: uppercase;
  cursor: pointer;
  transition: transform 180ms ease, box-shadow 180ms ease;
  box-shadow: 0 18px 30px rgba(79, 102, 83, 0.22);
}

.scale-intro-page__action:hover:not(:disabled) {
  transform: translateY(-2px);
}

.scale-intro-page__action:disabled {
  cursor: wait;
  opacity: 0.72;
}

@media (max-width: 960px) {
  .scale-intro-page {
    padding: 1rem;
  }

  .scale-intro-page__hero,
  .scale-intro-page__body {
    grid-template-columns: 1fr;
  }
}
</style>

