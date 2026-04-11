<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { fetchScaleListApi } from '@/api/assessment'
import type { ScaleSummary } from '@/api/types'
import { toErrorMessage } from '@/views/shared/page-logic'

const router = useRouter()
const loading = ref(false)
const errorMessage = ref('')
const scales = ref<ScaleSummary[]>([])

async function loadScales(): Promise<void> {
  loading.value = true
  errorMessage.value = ''

  try {
    scales.value = await fetchScaleListApi()
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
  <div class="gallery-layout">

    <header class="gallery-header">
      <div class="header-content">
        <span class="eyebrow">ASSESSMENT GALLERY</span>
        <h1 class="display-title">心理测评</h1>
        <p class="narrative">
          这里不是考场，而是认识自我的画廊。<br>
          选择一份量表，开启一段向内探索的旅程。
        </p>
      </div>
      <div class="header-stats">
        <div class="stat-circle">
          <span class="stat-num">{{ String(scales.length).padStart(2, '0') }}</span>
          <span class="stat-label">Scales</span>
        </div>
      </div>
    </header>

    <main class="white-canvas">
      <div v-if="loading" class="state-text">正在布置画廊...</div>
      <div v-else-if="errorMessage" class="state-text error">{{ errorMessage }}</div>

      <div v-else class="list-wrapper">
        <article
            v-for="(scale, index) in scales"
            :key="scale.id"
            class="list-item"
            @click="openScale(scale.id)"
        >
          <div class="item-visual">
            <span class="item-index">{{ String(index + 1).padStart(2, '0') }}.</span>
            <span class="item-code">{{ scale.code }}</span>
          </div>

          <div class="item-body">
            <h2 class="item-name">{{ scale.name }}</h2>
            <p class="item-desc">{{ scale.description || '探索内在状态，获取专属的结构化反馈。' }}</p>
          </div>

          <div class="item-tail">
            <div class="tail-meta">
              <div class="meta-item">
                <span class="meta-val">{{ scale.totalQuestions }}</span>
                <span class="meta-lbl">题数</span>
              </div>
              <div class="meta-divider"></div>
              <div class="meta-item">
                <span class="meta-val">{{ scale.pageSize }}</span>
                <span class="meta-lbl">单页</span>
              </div>
            </div>
            <div class="action-btn">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path stroke-linecap="round" stroke-linejoin="round" d="M17 8l4 4m0 0l-4 4m4-4H3" />
              </svg>
            </div>
          </div>
        </article>
      </div>
    </main>
  </div>
</template>

<style scoped>
/* =========================================
   全局布局
========================================= */
.gallery-layout {
  width: 100%;
  animation: fade-in 0.8s cubic-bezier(0.16, 1, 0.3, 1);
  /* 顶部不设 padding，让画布从下往上铺 */
}

@keyframes fade-in {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

/* =========================================
   环境色头部 (Environment Header)
========================================= */
.gallery-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  padding: 4rem 4rem 6rem 4rem; /* 底部留足空间，与白色画布衔接 */
}

.eyebrow {
  font-family: var(--font-mono);
  font-size: 0.85rem;
  letter-spacing: 0.2em;
  color: var(--text-secondary);
  text-transform: uppercase;
  display: block;
  margin-bottom: 1.5rem;
}

.display-title {
  font-family: var(--font-serif);
  font-size: clamp(3rem, 6vw, 5rem);
  font-weight: 500;
  line-height: 1;
  color: var(--text-primary);
  margin: 0 0 1.5rem 0;
  letter-spacing: 0.02em;
}

.narrative {
  font-size: 1.05rem;
  line-height: 1.8;
  color: var(--text-secondary);
  margin: 0;
}

.stat-circle {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  border: 1px solid rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: transparent;
  backdrop-filter: blur(4px);
}

.stat-num {
  font-family: var(--font-serif);
  font-size: 2.5rem;
  line-height: 1;
  color: var(--text-primary);
}

.stat-label {
  font-family: var(--font-mono);
  font-size: 0.7rem;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: var(--text-secondary);
  margin-top: 0.3rem;
}

/* =========================================
   纯白画布 (Pure White Canvas) - 核心突破点
========================================= */
.white-canvas {
  background-color: #FFFFFF; /* 强制纯白 */
  border-radius: 48px 48px 0 0; /* 巨大的顶部圆角 */
  padding: 6rem 4rem;
  min-height: 60vh;
  box-shadow: 0 -20px 60px rgba(0, 0, 0, 0.03); /* 顶部的淡淡悬浮阴影 */
  position: relative;
  z-index: 10;
}

/* =========================================
   列表样式 (List items on white background)
========================================= */
.list-wrapper {
  display: flex;
  flex-direction: column;
  max-width: 1200px;
  margin: 0 auto;
}

.list-item {
  display: grid;
  grid-template-columns: 140px 1fr auto;
  gap: 2rem;
  padding: 3rem 0;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06); /* 在白底上使用极浅的黑线 */
  cursor: pointer;
  align-items: center;
  transition: padding-left 0.4s ease, padding-right 0.4s ease, background-color 0.4s ease;
}

/* Hover 时的吸附反馈 */
.list-item:hover {
  background-color: #FAFAFA; /* 在白底上微微加深一层浅灰 */
  padding-left: 2rem;
  padding-right: 2rem;
  border-bottom-color: transparent;
  border-radius: 16px;
}

.item-visual {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.item-index {
  font-family: var(--font-serif);
  font-size: 1.8rem;
  color: #111111;
  transition: color 0.3s;
}

.item-code {
  font-family: var(--font-mono);
  font-size: 0.75rem;
  color: #888888;
  letter-spacing: 0.05em;
}

.list-item:hover .item-index {
  color: var(--accent-color);
}

.item-body {
  padding-right: 2rem;
}

.item-name {
  font-family: var(--font-serif);
  font-size: 1.6rem;
  font-weight: 500;
  color: #111111;
  margin: 0 0 0.5rem 0;
}

.item-desc {
  font-size: 0.95rem;
  color: #666666;
  margin: 0;
  line-height: 1.6;
}

.item-tail {
  display: flex;
  align-items: center;
  gap: 4rem;
}

.tail-meta {
  display: flex;
  align-items: center;
  gap: 1.5rem;
}

.meta-item {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 0.2rem;
}

.meta-val {
  font-family: var(--font-mono);
  font-size: 1.1rem;
  color: #111111;
}

.meta-lbl {
  font-size: 0.7rem;
  color: #888888;
  text-transform: uppercase;
}

.meta-divider {
  width: 1px;
  height: 24px;
  background-color: rgba(0, 0, 0, 0.1);
}

.action-btn {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  border: 1px solid rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #111111;
  transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1);
}

.action-btn svg {
  width: 20px;
  height: 20px;
  transition: transform 0.4s ease;
}

.list-item:hover .action-btn {
  background-color: #111111;
  color: #FFFFFF;
  border-color: #111111;
  transform: scale(1.05);
}

.list-item:hover .action-btn svg {
  transform: translateX(4px);
}

/* =========================================
   状态文本
========================================= */
.state-text {
  text-align: center;
  padding: 4rem 0;
  color: #888888;
  font-size: 0.95rem;
}

.state-text.error {
  color: #D9534F;
}

/* =========================================
   响应式
========================================= */
@media (max-width: 1024px) {
  .gallery-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 2rem;
    padding: 3rem 2rem 4rem 2rem;
  }

  .white-canvas {
    padding: 4rem 2rem;
    border-radius: 32px 32px 0 0;
  }

  .list-item {
    grid-template-columns: 80px 1fr;
    gap: 1.5rem;
    padding: 2rem 0;
  }

  .item-tail {
    display: none; /* 移动端隐藏过多的元数据和按钮，保持纯粹 */
  }

  .list-item:hover {
    padding-left: 1rem;
    padding-right: 1rem;
  }
}
</style>