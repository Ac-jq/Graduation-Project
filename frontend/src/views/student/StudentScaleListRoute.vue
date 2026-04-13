<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { fetchScaleListApi } from '@/api/assessment'
import type { ScaleSummary } from '@/api/types'
import { useAssessmentStore } from '@/stores/assessment'
import { toErrorMessage } from '@/views/shared/page-logic'

type ScaleTone = 'sage' | 'amber'

const router = useRouter()
const assessmentStore = useAssessmentStore()

const loading = ref(false)
const errorMessage = ref('')
const scales = ref<ScaleSummary[]>([])

const totalQuestions = computed(() =>
  scales.value.reduce((sum, item) => sum + item.totalQuestions, 0)
)

const averageQuestions = computed(() => {
  if (!scales.value.length) {
    return 0
  }

  return Math.round(totalQuestions.value / scales.value.length)
})

function resolveScaleTone(code: string): ScaleTone {
  return code === 'GAD7' ? 'amber' : 'sage'
}

function resolveScaleLabel(code: string): string {
  switch (code) {
    case 'PHQ9':
      return '情绪状态'
    case 'GAD7':
      return '焦虑筛查'
    default:
      return '标准量表'
  }
}

function resolveScaleIcon(code: string): string[] {
  switch (code) {
    case 'GAD7':
      return [
        'M12 3.75c3.38 0 6.34 1.82 7.92 4.53',
        'M20.25 12A8.25 8.25 0 1 1 12 3.75',
        'M12 8.25v4.1l2.75 2.4'
      ]
    default:
      return [
        'M12 20.25s-6.75-4.02-6.75-9.28A3.97 3.97 0 0 1 12 7.89a3.97 3.97 0 0 1 6.75 3.08c0 5.26-6.75 9.28-6.75 9.28Z'
      ]
  }
}

async function loadScales(): Promise<void> {
  loading.value = true
  errorMessage.value = ''

  try {
    const response = await fetchScaleListApi()
    scales.value = response.filter((item) => item.code === 'PHQ9' || item.code === 'GAD7')
    assessmentStore.setScales(scales.value)
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    loading.value = false
  }
}

async function openScale(scaleId: number): Promise<void> {
  await router.push({ name: 'student-scale-detail', params: { scaleId } })
}

onMounted(() => {
  void loadScales()
})
</script>

<template>
  <main class="scale-list-page">
    <section class="scale-list-page__hero">
      <div class="scale-list-page__hero-copy">
        <div class="scale-list-page__eyebrow-row">
          <span class="scale-list-page__eyebrow">Assessment Studio</span>
          <span class="scale-list-page__hero-pill">
            <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
              <path d="M12 4.75v14.5" />
              <path d="M4.75 12h14.5" />
            </svg>
            温和完成本轮自评
          </span>
        </div>
        <h1>选择一份量表，开始一次更有节奏的心理状态整理。</h1>
        <p class="scale-list-page__lead">
          标准量表会帮助你在几分钟内记录最近两周的感受变化。提交后系统会生成结构化报告，
          便于后续查看趋势、风险等级与支持建议。
        </p>

        <div class="scale-list-page__hero-metrics">
          <article class="metric-card">
            <span>可用量表</span>
            <strong>{{ scales.length }}</strong>
            <p>精选学生端标准自评量表</p>
          </article>
          <article class="metric-card">
            <span>平均题量</span>
            <strong>{{ averageQuestions }}</strong>
            <p>单次作答负担轻，适合阶段性复盘</p>
          </article>
          <article class="metric-card metric-card--soft">
            <span>作答体验</span>
            <strong>自动保存</strong>
            <p>支持分页继续填写，减少中途打断焦虑</p>
          </article>
        </div>
      </div>

      <aside class="scale-list-page__hero-panel">
        <div class="scale-list-page__hero-panel-top">
          <span class="scale-list-page__panel-label">本轮测评速览</span>
          <div class="scale-list-page__panel-icon">
            <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
              <path d="M12 4.5 19.5 8.7v6.6L12 19.5 4.5 15.3V8.7L12 4.5Z" />
              <path d="M12 10.2v5.4" />
              <path d="M9.4 12.75h5.2" />
            </svg>
          </div>
        </div>
        <strong>{{ totalQuestions }} 道问题</strong>
        <p>
          每份量表都使用更柔和的分步体验帮助你完成自检，结果仅用于心理支持和辅助评估，
          不作为医学诊断依据。
        </p>
        <div class="scale-list-page__panel-note">
          <span>建议</span>
          <p>选择一个安静时段，按直觉作答，比追求“标准答案”更有参考价值。</p>
        </div>
      </aside>
    </section>

    <p v-if="errorMessage" class="scale-list-page__alert">{{ errorMessage }}</p>

    <section v-if="loading" class="scale-list-page__status-panel">
      <div class="scale-list-page__status-icon">
        <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
          <path d="M12 6.5v5.5l3.25 2" />
          <path d="M20 12a8 8 0 1 1-8-8" />
        </svg>
      </div>
      <div>
        <h2>正在整理量表列表</h2>
        <p>马上就好，系统正在同步可用测评内容。</p>
      </div>
    </section>

    <section v-else class="scale-list-page__grid">
      <article
        v-for="scale in scales"
        :key="scale.id"
        class="scale-card"
        :class="`scale-card--${resolveScaleTone(scale.code)}`"
        @click="openScale(scale.id)"
      >
        <div class="scale-card__glow"></div>

        <div class="scale-card__topline">
          <div class="scale-card__identity">
            <span class="scale-card__icon" aria-hidden="true">
              <svg viewBox="0 0 24 24" fill="none">
                <path
                  v-for="segment in resolveScaleIcon(scale.code)"
                  :key="segment"
                  :d="segment"
                />
              </svg>
            </span>
            <div>
              <p class="scale-card__code">{{ scale.code }}</p>
              <h2>{{ scale.name }}</h2>
            </div>
          </div>
          <span class="scale-card__badge">{{ resolveScaleLabel(scale.code) }}</span>
        </div>

        <p class="scale-card__desc">
          {{ scale.description || '用于了解最近两周心理状态的标准量表。' }}
        </p>

        <div class="scale-card__chips">
          <span class="info-chip">{{ scale.totalQuestions }} 题</span>
          <span class="info-chip">每页 {{ scale.pageSize }} 题</span>
          <span class="info-chip">自动存档</span>
        </div>

        <dl class="scale-card__meta">
          <div>
            <dt>适用场景</dt>
            <dd>{{ scale.productPositioning || '心理状态辅助评估' }}</dd>
          </div>
          <div>
            <dt>填写提醒</dt>
            <dd>{{ scale.noticeText || '建议按当下真实感受作答。' }}</dd>
          </div>
        </dl>

        <div class="scale-card__footer">
          <div class="scale-card__footnote">
            <span>完成后生成正式报告与 AI 辅助解释</span>
          </div>
          <button type="button">
            开始测评
            <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
              <path d="M5 12h14" />
              <path d="m13 6 6 6-6 6" />
            </svg>
          </button>
        </div>
      </article>
    </section>
  </main>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700;800&family=Noto+Serif+SC:wght@500;600;700&display=swap');

.scale-list-page {
  --ink: #222720;
  --muted: #6d675d;
  --line: rgba(44, 50, 40, 0.08);
  --card-shadow: 0 8px 24px rgba(0, 0, 0, 0.04);
  min-height: 100%;
  padding: 0.4rem 0 2.4rem;
  color: var(--ink);
}

.scale-list-page__hero {
  display: grid;
  grid-template-columns: minmax(0, 1.28fr) minmax(300px, 0.72fr);
  gap: 1.6rem;
  align-items: stretch;
}

.scale-list-page__hero-copy,
.scale-list-page__hero-panel,
.metric-card,
.scale-card,
.scale-list-page__status-panel {
  border: 1px solid rgba(33, 39, 31, 0.05);
  border-radius: 24px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.92) 0%, rgba(247, 243, 236, 0.84) 100%);
  box-shadow: var(--card-shadow);
  backdrop-filter: blur(16px);
}

.scale-list-page__hero-copy {
  padding: 1.8rem;
}

.scale-list-page__eyebrow-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0.9rem;
  align-items: center;
}

.scale-list-page__eyebrow,
.scale-card__code,
.scale-card__meta dt,
.scale-list-page__panel-label,
.metric-card span {
  margin: 0;
  font: 800 0.72rem/1 'Manrope', sans-serif;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.scale-list-page__eyebrow,
.scale-card__code,
.scale-list-page__panel-label {
  color: #7e7264;
}

.scale-list-page__hero-pill {
  display: inline-flex;
  gap: 0.45rem;
  align-items: center;
  padding: 0.52rem 0.78rem;
  border-radius: 999px;
  background: rgba(97, 122, 105, 0.12);
  color: #5a7362;
  font: 700 0.75rem/1 'Manrope', sans-serif;
}

.scale-list-page__hero-pill svg,
.scale-list-page__panel-icon svg,
.scale-list-page__status-icon svg,
.scale-card__icon svg,
.scale-card__footer button svg {
  width: 18px;
  height: 18px;
  stroke: currentColor;
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.scale-list-page h1,
.scale-card h2,
.scale-list-page__status-panel h2 {
  margin: 0;
  font-family: 'Noto Serif SC', serif;
  font-weight: 600;
}

.scale-list-page h1 {
  margin-top: 1rem;
  font-size: clamp(2.4rem, 4.6vw, 4.3rem);
  line-height: 1.08;
}

.scale-list-page__lead,
.metric-card p,
.scale-list-page__hero-panel p,
.scale-card__desc,
.scale-card__meta dd,
.scale-list-page__status-panel p {
  font-family: 'Manrope', sans-serif;
  line-height: 1.8;
}

.scale-list-page__lead {
  max-width: 44rem;
  margin: 1rem 0 0;
  color: var(--muted);
}

.scale-list-page__hero-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 1rem;
  margin-top: 1.6rem;
}

.metric-card {
  padding: 1.25rem 1.3rem;
}

.metric-card--soft {
  background:
    linear-gradient(180deg, rgba(245, 248, 243, 0.98) 0%, rgba(252, 247, 241, 0.88) 100%);
}

.metric-card span {
  color: #7d7466;
}

.metric-card strong,
.scale-list-page__hero-panel strong {
  display: block;
  margin-top: 0.65rem;
  font: 600 1.85rem/1.08 'Noto Serif SC', serif;
  color: #2b332a;
}

.metric-card p {
  margin: 0.7rem 0 0;
  font-size: 0.88rem;
  color: var(--muted);
}

.scale-list-page__hero-panel {
  position: relative;
  overflow: hidden;
  padding: 1.6rem;
  background:
    radial-gradient(circle at top right, rgba(97, 122, 105, 0.18), transparent 34%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.95) 0%, rgba(240, 246, 238, 0.88) 100%);
}

.scale-list-page__hero-panel-top {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
}

.scale-list-page__panel-icon {
  width: 3rem;
  height: 3rem;
  border-radius: 18px;
  display: grid;
  place-items: center;
  background: rgba(97, 122, 105, 0.12);
  color: #5f7664;
}

.scale-list-page__hero-panel p {
  margin: 0.85rem 0 0;
  color: #666156;
}

.scale-list-page__panel-note {
  margin-top: 1.35rem;
  padding: 1rem 1.05rem;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.68);
  border: 1px solid rgba(44, 50, 40, 0.05);
}

.scale-list-page__panel-note span {
  display: inline-flex;
  margin-bottom: 0.45rem;
  font: 800 0.7rem/1 'Manrope', sans-serif;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: #6f886f;
}

.scale-list-page__panel-note p {
  margin: 0;
  font-size: 0.85rem;
}

.scale-list-page__alert {
  margin-top: 1rem;
  padding: 1rem 1.1rem;
  border-radius: 18px;
  background: rgba(169, 84, 74, 0.08);
  color: #994b43;
  font-weight: 700;
}

.scale-list-page__status-panel {
  display: grid;
  grid-template-columns: 54px minmax(0, 1fr);
  gap: 1rem;
  align-items: center;
  margin-top: 1.6rem;
  padding: 1.4rem 1.5rem;
}

.scale-list-page__status-icon {
  width: 54px;
  height: 54px;
  border-radius: 18px;
  display: grid;
  place-items: center;
  background: rgba(97, 122, 105, 0.11);
  color: #5d7564;
}

.scale-list-page__status-panel h2 {
  font-size: 1.3rem;
}

.scale-list-page__status-panel p {
  margin: 0.3rem 0 0;
  color: var(--muted);
}

.scale-list-page__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1.4rem;
  margin-top: 1.7rem;
}

.scale-card {
  position: relative;
  overflow: hidden;
  padding: 1.6rem;
  cursor: pointer;
  transition: all 0.3s ease;
}

.scale-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 18px 36px rgba(70, 60, 50, 0.1);
}

.scale-card__glow {
  position: absolute;
  inset: auto -2rem -3rem auto;
  width: 8rem;
  height: 8rem;
  border-radius: 999px;
  opacity: 0.55;
  pointer-events: none;
}

.scale-card--sage .scale-card__glow {
  background: radial-gradient(circle, rgba(125, 154, 126, 0.24), transparent 72%);
}

.scale-card--amber .scale-card__glow {
  background: radial-gradient(circle, rgba(213, 176, 115, 0.24), transparent 72%);
}

.scale-card__topline {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: start;
}

.scale-card__identity {
  display: flex;
  gap: 0.95rem;
  align-items: center;
}

.scale-card__icon {
  width: 3.4rem;
  height: 3.4rem;
  border-radius: 18px;
  display: grid;
  place-items: center;
  flex-shrink: 0;
}

.scale-card--sage .scale-card__icon {
  background: rgba(97, 122, 105, 0.13);
  color: #5d7765;
}

.scale-card--amber .scale-card__icon {
  background: rgba(193, 150, 83, 0.14);
  color: #b07c2e;
}

.scale-card__badge,
.info-chip {
  display: inline-flex;
  align-items: center;
  padding: 0.5rem 0.8rem;
  border-radius: 999px;
  font: 800 0.72rem/1 'Manrope', sans-serif;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.scale-card__badge {
  background: rgba(255, 255, 255, 0.9);
  color: #63745b;
  border: 1px solid rgba(33, 39, 31, 0.05);
}

.scale-card h2 {
  margin-top: 0.32rem;
  font-size: 1.72rem;
  line-height: 1.2;
}

.scale-card__desc {
  margin: 1rem 0 0;
  color: #666055;
}

.scale-card__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 0.7rem;
  margin-top: 1rem;
}

.info-chip {
  background: rgba(247, 244, 238, 0.92);
  color: #6e695f;
}

.scale-card__meta {
  display: grid;
  gap: 0.95rem;
  margin: 1.2rem 0 0;
}

.scale-card__meta div {
  padding: 1rem 1.05rem;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.64);
}

.scale-card__meta dt {
  color: #7b7266;
}

.scale-card__meta dd {
  margin: 0.45rem 0 0;
  color: #2a2f28;
}

.scale-card__footer {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
  margin-top: 1.25rem;
}

.scale-card__footnote span {
  display: block;
  font: 600 0.85rem/1.65 'Manrope', sans-serif;
  color: #70695f;
}

.scale-card__footer button {
  display: inline-flex;
  gap: 0.45rem;
  align-items: center;
  min-height: 3rem;
  padding: 0 1.1rem;
  border: 1px solid rgba(44, 50, 40, 0.06);
  border-radius: 999px;
  background: linear-gradient(135deg, #6c8473, #526a58);
  color: #fffdf8;
  font: 800 0.76rem/1 'Manrope', sans-serif;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  cursor: pointer;
  transition: all 0.3s ease;
}

.scale-card__footer button:hover {
  transform: translateY(-2px);
  box-shadow: 0 14px 24px rgba(82, 106, 88, 0.22);
}

@media (max-width: 1100px) {
  .scale-list-page__hero,
  .scale-list-page__grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .scale-list-page__hero-copy,
  .scale-list-page__hero-panel,
  .scale-card {
    padding: 1.35rem;
  }

  .scale-list-page__hero-metrics {
    grid-template-columns: 1fr;
  }

  .scale-card__topline,
  .scale-card__footer {
    flex-direction: column;
    align-items: start;
  }
}
</style>
