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

const guideSteps = [
  {
    title: '按最近两周作答',
    description: '请围绕最近两周的真实状态选择答案，不需要追求“理想答案”。'
  },
  {
    title: '系统自动保存',
    description: '切页或暂存都会保留当前进度，之后可以继续完成。'
  },
  {
    title: '提交后生成报告',
    description: '系统将输出分数、风险等级、辅助解释与后续建议。'
  }
]

function resolvePositioning(detail: ScaleDetail | null): string {
  return detail?.productPositioning || '标准化心理状态辅助评估'
}

function resolveNotice(detail: ScaleDetail | null): string {
  return (
    detail?.noticeText ||
    '本结果仅用于心理状态辅助评估，不作为医学诊断依据。如有持续困扰，请联系专业老师或医疗机构。'
  )
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
  <main class="scale-intro-page">
    <section class="scale-intro-page__hero">
      <div class="scale-intro-page__copy">
        <p class="scale-intro-page__eyebrow">Assessment Brief</p>
        <h1>{{ scaleDetail?.name || '心理测评说明' }}</h1>
        <p class="scale-intro-page__lead">
          {{ scaleDetail?.description || '本测评基于标准量表，用于帮助你理解当下的情绪状态与压力体验。' }}
        </p>
      </div>

      <aside class="scale-intro-page__summary-card">
        <p class="scale-intro-page__meta-label">量表参数</p>
        <dl>
          <div>
            <dt>量表编码</dt>
            <dd>{{ scaleDetail?.code || '--' }}</dd>
          </div>
          <div>
            <dt>总题数</dt>
            <dd>{{ scaleDetail?.totalQuestions || '--' }}</dd>
          </div>
          <div>
            <dt>每页题数</dt>
            <dd>{{ scaleDetail?.pageSize || '--' }}</dd>
          </div>
          <div>
            <dt>产品定位</dt>
            <dd>{{ resolvePositioning(scaleDetail) }}</dd>
          </div>
        </dl>
      </aside>
    </section>

    <p v-if="errorMessage" class="scale-intro-page__alert">{{ errorMessage }}</p>

    <section v-if="loading" class="scale-intro-page__status-panel">
      <p>正在加载量表说明...</p>
    </section>

    <template v-else-if="scaleDetail">
      <section class="scale-intro-page__content-grid">
        <article class="scale-intro-page__panel">
          <p class="scale-intro-page__meta-label">作答导语</p>
          <h2>开始前请先确认这三件事</h2>
          <p class="scale-intro-page__body-text">
            {{
              scaleDetail.introduction ||
              '请在相对安静的环境中完成作答，尽量基于最近两周的真实感受来选择答案，不需要反复纠结。'
            }}
          </p>
          <ul class="scale-intro-page__guide-list">
            <li v-for="step in guideSteps" :key="step.title">
              <strong>{{ step.title }}</strong>
              <span>{{ step.description }}</span>
            </li>
          </ul>
        </article>

        <article class="scale-intro-page__panel">
          <p class="scale-intro-page__meta-label">评分说明</p>
          <h2>系统会如何解释你的结果</h2>
          <ul class="scale-intro-page__rule-list">
            <li v-for="rule in scaleDetail.scoringRules || []" :key="rule">{{ rule }}</li>
          </ul>
          <p v-if="!(scaleDetail.scoringRules || []).length" class="scale-intro-page__body-text">
            提交后系统会根据标准规则自动计算总分与等级，并生成结构化报告。
          </p>
        </article>
      </section>

      <section class="scale-intro-page__notice-panel">
        <div>
          <p class="scale-intro-page__meta-label">重要声明</p>
          <h2>本模块只提供辅助评估，不提供诊断结论</h2>
        </div>
        <p>{{ resolveNotice(scaleDetail) }}</p>
      </section>

      <section class="scale-intro-page__actions">
        <button class="scale-intro-page__ghost" type="button" @click="router.push({ name: 'student-scales' })">
          返回量表目录
        </button>
        <button
          class="scale-intro-page__primary"
          type="button"
          :disabled="creating"
          @click="startAssessment"
        >
          {{ creating ? '正在创建测评会话...' : '开始本次测评' }}
        </button>
      </section>
    </template>
  </main>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700&family=Noto+Serif+SC:wght@400;500;600;700&display=swap');

.scale-intro-page {
  --ink: #212720;
  --muted: #6d675f;
  --line: rgba(33, 39, 32, 0.1);
  --glass: rgba(255, 251, 246, 0.74);
  min-height: 100%;
  color: var(--ink);
}

.scale-intro-page__hero,
.scale-intro-page__content-grid {
  display: grid;
  gap: 1.4rem;
}

.scale-intro-page__hero {
  grid-template-columns: minmax(0, 1.35fr) minmax(280px, 0.8fr);
  align-items: end;
}

.scale-intro-page__eyebrow,
.scale-intro-page__meta-label,
.scale-intro-page__summary-card dt {
  margin: 0;
  font: 700 0.74rem/1 'Manrope', sans-serif;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: #72695e;
}

.scale-intro-page h1,
.scale-intro-page h2 {
  margin: 0;
  font-family: 'Noto Serif SC', serif;
  font-weight: 600;
}

.scale-intro-page h1 {
  margin-top: 0.9rem;
  font-size: clamp(2.4rem, 5vw, 4.6rem);
  line-height: 1.04;
}

.scale-intro-page__lead,
.scale-intro-page__body-text,
.scale-intro-page__notice-panel p,
.scale-intro-page__guide-list span,
.scale-intro-page__rule-list li,
.scale-intro-page__summary-card dd,
.scale-intro-page__status-panel p {
  font-family: 'Manrope', sans-serif;
  line-height: 1.85;
}

.scale-intro-page__lead {
  max-width: 42rem;
  margin: 1rem 0 0;
  color: var(--muted);
}

.scale-intro-page__summary-card,
.scale-intro-page__panel,
.scale-intro-page__notice-panel,
.scale-intro-page__status-panel {
  border: 1px solid var(--line);
  background: var(--glass);
  backdrop-filter: blur(18px);
  box-shadow: 0 24px 56px rgba(74, 61, 48, 0.08);
}

.scale-intro-page__summary-card,
.scale-intro-page__panel,
.scale-intro-page__notice-panel {
  padding: 1.35rem;
}

.scale-intro-page__summary-card dl {
  display: grid;
  gap: 0.95rem;
  margin: 1rem 0 0;
}

.scale-intro-page__summary-card dd {
  margin: 0.35rem 0 0;
}

.scale-intro-page__content-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-top: 1.5rem;
}

.scale-intro-page__panel h2,
.scale-intro-page__notice-panel h2 {
  margin-top: 0.8rem;
  font-size: 1.65rem;
  line-height: 1.32;
}

.scale-intro-page__body-text {
  margin: 0.9rem 0 0;
  color: var(--muted);
}

.scale-intro-page__guide-list,
.scale-intro-page__rule-list {
  display: grid;
  gap: 0.9rem;
  margin: 1.25rem 0 0;
  padding: 0;
  list-style: none;
}

.scale-intro-page__guide-list li,
.scale-intro-page__rule-list li {
  padding: 0.95rem 1rem;
  border: 1px solid rgba(33, 39, 32, 0.08);
  background: rgba(255, 255, 255, 0.5);
}

.scale-intro-page__guide-list strong {
  display: block;
  margin-bottom: 0.4rem;
  font: 600 1rem/1.5 'Noto Serif SC', serif;
}

.scale-intro-page__notice-panel {
  display: grid;
  gap: 0.8rem;
  margin-top: 1.5rem;
}

.scale-intro-page__notice-panel p {
  margin: 0;
  color: #7c574a;
}

.scale-intro-page__alert {
  margin-top: 1rem;
  color: #9f4d4d;
  font-weight: 600;
}

.scale-intro-page__status-panel {
  margin-top: 1.5rem;
  padding: 1.2rem;
}

.scale-intro-page__actions {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  margin-top: 1.5rem;
}

.scale-intro-page__ghost,
.scale-intro-page__primary {
  min-height: 3.2rem;
  padding: 0 1.2rem;
  border: none;
  font: 700 0.8rem/1 'Manrope', sans-serif;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  cursor: pointer;
}

.scale-intro-page__ghost {
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.56);
  color: var(--ink);
}

.scale-intro-page__primary {
  background: linear-gradient(135deg, #607968, #4d6454);
  color: #fffaf4;
  box-shadow: 0 18px 34px rgba(77, 100, 84, 0.24);
}

@media (max-width: 980px) {
  .scale-intro-page__hero,
  .scale-intro-page__content-grid {
    grid-template-columns: 1fr;
  }

  .scale-intro-page__actions {
    flex-direction: column;
  }
}
</style>
