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
const currentPage = ref(1)
const pageSize = 6

const totalQuestions = computed(() =>
    scales.value.reduce((sum, item) => sum + item.totalQuestions, 0)
)
const totalPages = computed(() => Math.max(1, Math.ceil(scales.value.length / pageSize)))
const pagedScales = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return scales.value.slice(start, start + pageSize)
})

const averageQuestions = computed(() => {
  if (!scales.value.length) return 0
  return Math.round(totalQuestions.value / scales.value.length)
})

function resolveScaleTone(code: string): ScaleTone {
  return code === 'GAD7' || code === 'STRESS8' ? 'amber' : 'sage'
}

function resolveScaleLabel(code: string): string {
  switch (code) {
    case 'PHQ9': return '情绪状态'
    case 'GAD7': return '焦虑筛查'
    case 'SLEEP6': return '睡眠状态'
    case 'STRESS8': return '压力观察'
    default: return '标准量表'
  }
}

async function loadScales(): Promise<void> {
  loading.value = true
  errorMessage.value = ''

  try {
    const response = await fetchScaleListApi()
    scales.value = response
    currentPage.value = 1
    assessmentStore.setScales(scales.value)
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    loading.value = false
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

async function openScale(scaleId: number): Promise<void> {
  await router.push({ name: 'student-scale-detail', params: { scaleId } })
}

onMounted(() => {
  void loadScales()
})
</script>

<template>
  <main class="editorial-scale-page">
    <div class="page-container">

      <header class="editorial-hero">
        <div class="hero-meta">
          <span class="hero-tag">量表目录</span>
        </div>
        <h1 class="hero-title">自我察觉的刻度</h1>
        <p class="hero-lead">
          选择一份量表，开启一段平静的向内探索。<br>
          不需要追求“标准答案”，只需如实记录此刻的感受，所有的梳理都会被妥善保存。
        </p>

        <div class="hero-stats">
          <div class="stat-item">
            <span class="stat-label">可用量表</span>
            <strong class="stat-value">{{ loading ? '-' : scales.length }}</strong>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <span class="stat-label">总计题量</span>
            <strong class="stat-value">{{ loading ? '-' : totalQuestions }}</strong>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <span class="stat-label">体验保障</span>
            <strong class="stat-text">支持分段作答与自动保存</strong>
          </div>
        </div>
      </header>

      <div v-if="errorMessage" class="error-banner">{{ errorMessage }}</div>

      <div v-if="loading" class="loading-state">
        <div class="spinner"></div>
        <p>正在展卷...</p>
      </div>

      <section v-else class="scale-journal">
        <article
            v-for="scale in pagedScales"
            :key="scale.id"
            class="scale-row"
            :class="`scale-row--${resolveScaleTone(scale.code)}`"
            @click="openScale(scale.id)"
        >
          <div class="row-watermark" aria-hidden="true">{{ scale.code }}</div>

          <div class="row-content">
            <div class="row-left">
              <span class="scale-label">{{ resolveScaleLabel(scale.code) }}</span>
              <h2 class="scale-title">{{ scale.name }}</h2>
              <div class="scale-tags">
                <span class="minimal-tag">{{ scale.totalQuestions }} 题</span>
                <span class="minimal-tag">每页 {{ scale.pageSize }} 题</span>
              </div>
            </div>

            <div class="row-center">
              <p class="scale-desc">
                {{ scale.description || '用于了解最近两周心理状态的标准量表。' }}
              </p>
              <div class="scale-notes">
                <p><strong>适用场景：</strong>{{ scale.productPositioning || '心理状态辅助评估' }}</p>
                <p><strong>填写提醒：</strong>{{ scale.noticeText || '建议按当下真实感受作答。' }}</p>
              </div>
            </div>

            <div class="row-right">
              <button class="start-btn">
                开始梳理 <span class="arrow">→</span>
              </button>
            </div>
          </div>
        </article>
      </section>

      <nav class="pagination-nav" v-if="totalPages > 1">
        <button
            class="page-btn"
            :disabled="currentPage <= 1"
            @click="prevPage"
        >
          <span class="arrow">←</span> 上一页
        </button>

        <div class="page-indicator">
          <span>{{ currentPage }}</span> / <span>{{ totalPages }}</span>
        </div>

        <button
            class="page-btn"
            :disabled="currentPage >= totalPages"
            @click="nextPage"
        >
          下一页 <span class="arrow">→</span>
        </button>
      </nav>

    </div>
  </main>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;800&family=Noto+Serif+SC:wght@500;600;700&display=swap');

/* 全局纸张质感背景 */
.editorial-scale-page {
  min-height: 100vh;
  background: #f4f6f4;
  color: #1e2821;
  font-family: 'Manrope', 'Noto Serif SC', sans-serif;
  padding: 4rem 2rem 8rem;
}

.page-container {
  max-width: 1100px;
  margin: 0 auto;
}

/* 头部排版 */
.editorial-hero {
  margin-bottom: 5rem;
  padding-bottom: 3rem;
  border-bottom: 1px solid rgba(42, 54, 46, 0.1);
}

.hero-meta {
  margin-bottom: 1.5rem;
}

.hero-tag {
  display: inline-block;
  font-family: 'Manrope', sans-serif;
  font-size: 0.85rem;
  font-weight: 600;
  letter-spacing: 0.15em;
  color: #8a9c90;
  text-transform: uppercase;
}

.hero-title {
  font-family: 'Noto Serif SC', serif;
  font-size: clamp(2.8rem, 5vw, 4.5rem);
  font-weight: 600;
  color: #1e2821;
  margin: 0 0 1.5rem 0;
  letter-spacing: 0.02em;
}

.hero-lead {
  font-family: 'Noto Serif SC', serif;
  font-size: 1.15rem;
  color: #5c6b60;
  line-height: 1.8;
  max-width: 600px;
  margin: 0 0 3rem 0;
}

/* 融入背景的轻量级数据展示 */
.hero-stats {
  display: flex;
  align-items: center;
  gap: 2rem;
  flex-wrap: wrap;
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.stat-label {
  font-family: 'Noto Serif SC', serif;
  font-size: 0.85rem;
  color: #8a9c90;
}

.stat-value {
  font-family: 'Manrope', sans-serif;
  font-size: 2rem;
  font-weight: 600;
  color: #2a362e;
  line-height: 1;
}

.stat-text {
  font-family: 'Noto Serif SC', serif;
  font-size: 1.1rem;
  font-weight: 600;
  color: #2a362e;
  line-height: 1;
  padding-top: 0.5rem;
}

.stat-divider {
  width: 1px;
  height: 2.5rem;
  background: rgba(42, 54, 46, 0.15);
}

/* 核心列表区（打破方框） */
.scale-journal {
  display: flex;
  flex-direction: column;
}

/* 每一行都是开放的，仅用底边线分隔 */
.scale-row {
  position: relative;
  padding: 4rem 0;
  border-bottom: 1px solid rgba(42, 54, 46, 0.08);
  cursor: pointer;
  transition: background 0.4s ease;
  overflow: hidden;
}

.scale-row:hover {
  background: rgba(255, 255, 255, 0.4);
}

.scale-row:first-child {
  padding-top: 1rem;
}

/* 巨大的背景水印，营造画报感 */
.row-watermark {
  position: absolute;
  top: 1rem;
  left: -1rem;
  font-family: 'Manrope', sans-serif;
  font-size: 12rem;
  font-weight: 800;
  color: rgba(130, 150, 138, 0.04);
  line-height: 1;
  pointer-events: none;
  z-index: 0;
  transition: color 0.4s ease, transform 0.4s ease;
}

.scale-row--amber:hover .row-watermark {
  color: rgba(213, 176, 115, 0.08);
  transform: translateX(20px);
}

.scale-row--sage:hover .row-watermark {
  color: rgba(125, 154, 126, 0.08);
  transform: translateX(20px);
}

/* 内容网格排版 */
.row-content {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: 1.2fr 2fr auto;
  gap: 4rem;
  align-items: center;
}

/* 左侧标题区 */
.row-left {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.scale-label {
  font-family: 'Noto Serif SC', serif;
  font-size: 0.9rem;
  color: #7b8c80;
  font-weight: 600;
}

.scale-title {
  font-family: 'Noto Serif SC', serif;
  font-size: 2.2rem;
  font-weight: 600;
  color: #1e2821;
  margin: 0;
  line-height: 1.2;
}

.scale-tags {
  display: flex;
  gap: 0.8rem;
}

.minimal-tag {
  font-family: 'Manrope', 'Noto Serif SC', sans-serif;
  font-size: 0.8rem;
  color: #6a7c70;
  padding: 0.3rem 0;
}

/* 中间描述区 */
.row-center {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.scale-desc {
  font-family: 'Noto Serif SC', serif;
  font-size: 1.1rem;
  color: #4a5c51;
  line-height: 1.8;
  margin: 0;
}

.scale-notes {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  padding-left: 1rem;
  border-left: 2px solid rgba(130, 150, 138, 0.2);
}

.scale-notes p {
  font-family: 'Noto Serif SC', serif;
  font-size: 0.9rem;
  color: #7b8c80;
  margin: 0;
}

.scale-notes strong {
  color: #5c6b60;
  font-weight: 600;
}

/* 右侧操作区 */
.start-btn {
  background: transparent;
  border: 1px solid rgba(42, 54, 46, 0.2);
  color: #2a362e;
  padding: 1.2rem 2.5rem;
  border-radius: 100px;
  font-family: 'Noto Serif SC', serif;
  font-size: 1.05rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1);
  display: flex;
  align-items: center;
  gap: 0.8rem;
}

.scale-row:hover .start-btn {
  background: #2a362e;
  border-color: #2a362e;
  color: #ffffff;
  box-shadow: 0 12px 24px rgba(42, 54, 46, 0.15);
}

.arrow {
  font-family: 'Manrope', sans-serif;
  transition: transform 0.3s ease;
}

.scale-row:hover .arrow {
  transform: translateX(4px);
}

/* 状态提示 */
.error-banner {
  background: rgba(140, 74, 74, 0.08);
  color: #8c4a4a;
  padding: 1.5rem;
  border-radius: 12px;
  text-align: center;
  font-family: 'Noto Serif SC', serif;
  margin-bottom: 2rem;
}

.loading-state {
  text-align: center;
  padding: 8rem 0;
  color: #7b8c80;
  font-family: 'Noto Serif SC', serif;
}

.spinner {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: 2px solid rgba(130, 150, 138, 0.2);
  border-top-color: #2a362e;
  animation: spin 0.8s linear infinite;
  margin: 0 auto 1.5rem;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 响应式适配 */
@media (max-width: 1024px) {
  .row-content {
    grid-template-columns: 1fr;
    gap: 2rem;
  }

  .row-watermark {
    font-size: 8rem;
    top: 0;
  }

  .scale-row {
    padding: 3rem 1.5rem;
  }

  .scale-row:first-child {
    padding-top: 3rem;
  }

  .start-btn {
    width: 100%;
    justify-content: center;
  }
}

@media (max-width: 768px) {
  .editorial-scale-page {
    padding: 2rem 1rem 4rem;
  }

  .hero-stats {
    gap: 1rem;
    flex-direction: column;
    align-items: flex-start;
  }

  .stat-divider {
    display: none;
  }
}

/* 优雅的分页器 */
.pagination-nav {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 2rem;
  margin-top: 4rem;
  padding-top: 2rem;
  border-top: 1px solid rgba(42, 54, 46, 0.08);
}

.page-btn {
  background: transparent;
  border: none;
  font-family: 'Noto Serif SC', serif;
  font-size: 1.05rem;
  font-weight: 600;
  color: #2a362e;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  transition: all 0.3s ease;
}

.page-btn:hover:not(:disabled) {
  color: #5c6b60;
}

.page-btn:disabled {
  color: #cbd5cf;
  cursor: not-allowed;
}

.page-indicator {
  font-family: 'Manrope', sans-serif;
  font-size: 1rem;
  color: #8a9c90;
  letter-spacing: 0.1em;
}

.page-indicator span {
  color: #2a362e;
  font-weight: 600;
}

.page-btn:hover:not(:disabled) .arrow:last-child {
  transform: translateX(4px);
}

.page-btn:hover:not(:disabled) .arrow:first-child {
  transform: translateX(-4px);
}
</style>
