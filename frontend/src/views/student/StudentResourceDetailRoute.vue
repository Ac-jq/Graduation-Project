<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { addStudentFavoriteApi, fetchResourceDetailApi, removeStudentFavoriteApi } from '@/api/resource'
import type { ResourceDetail } from '@/api/types'
import { useRoute, useRouter } from 'vue-router'
import { toErrorMessage, toNumberParam } from '@/views/shared/page-logic'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const togglingFavorite = ref(false)
const errorMessage = ref('')
const resourceDetail = ref<ResourceDetail | null>(null)
const resourceId = computed(() => toNumberParam(route.params.resourceId))

const isFavorited = computed(() => resourceDetail.value?.favorite)

const favoriteButtonText = computed(() => {
  if (togglingFavorite.value) return '处理中...'
  return isFavorited.value ? '已收录至资料夹' : '收录至资料夹'
})

const previewMode = computed<'video' | 'image' | 'audio' | 'article' | 'external'>(() => {
  const url = resourceDetail.value?.contentUrl?.toLowerCase() ?? ''
  if (!url) return 'external'
  if (url.endsWith('.mp4') || url.endsWith('.webm') || resourceDetail.value?.resourceType === 'VIDEO') return 'video'
  if (url.endsWith('.mp3') || url.endsWith('.wav') || url.endsWith('.ogg') || resourceDetail.value?.resourceType === 'AUDIO') return 'audio'
  if (url.endsWith('.jpg') || url.endsWith('.jpeg') || url.endsWith('.png') || url.endsWith('.webp')) return 'image'
  if (url.endsWith('.html') || resourceDetail.value?.resourceType === 'ARTICLE') return 'article'
  return 'external'
})

function formatDate(value: string | null): string {
  if (!value) return '未发布'
  const date = new Date(value)
  return `${date.getFullYear()}/${date.getMonth() + 1}/${date.getDate()}`
}

function resolveResourceType(type: string): string {
  if (type === 'IMAGE') return '图像内容'
  switch (type) {
    case 'ARTICLE': return '图文阅览'
    case 'VIDEO': return '视频影像'
    case 'AUDIO': return '声音片段'
    case 'LINK': return '外部指引'
    default: return type
  }
}

async function loadResourceDetail(): Promise<void> {
  if (!resourceId.value) {
    errorMessage.value = '无法定位到该片段。'
    resourceDetail.value = null
    return
  }

  loading.value = true
  errorMessage.value = ''

  try {
    resourceDetail.value = await fetchResourceDetailApi(resourceId.value)
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    loading.value = false
  }
}

async function toggleFavorite(): Promise<void> {
  if (!resourceId.value || !resourceDetail.value) return

  togglingFavorite.value = true
  errorMessage.value = ''

  try {
    if (resourceDetail.value.favorite) {
      await removeStudentFavoriteApi(resourceId.value)
    } else {
      await addStudentFavoriteApi(resourceId.value)
    }
    await loadResourceDetail()
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    togglingFavorite.value = false
  }
}

function openContent(): void {
  if (resourceDetail.value?.contentUrl) {
    window.open(resourceDetail.value.contentUrl, '_blank', 'noopener,noreferrer')
  }
}

function goBack(): void {
  router.back()
}

watch(() => route.params.resourceId, () => void loadResourceDetail())
onMounted(() => void loadResourceDetail())
</script>

<template>
  <main class="editorial-detail-page">

    <nav class="editorial-nav">
      <button class="nav-back-btn" @click="goBack">
        <span class="arrow">←</span> 返回探索
      </button>
    </nav>

    <div class="editorial-container">

      <div v-if="errorMessage" class="error-text">{{ errorMessage }}</div>

      <div v-if="loading" class="loading-state">
        <div class="spinner"></div>
        <p>正在展卷...</p>
      </div>

      <article v-else-if="resourceDetail" class="editorial-article">

        <header class="article-header">
          <div class="meta-top">
            <span class="category-badge">{{ resourceDetail.categoryName }}</span>
            <span class="type-text">{{ resolveResourceType(resourceDetail.resourceType) }}</span>
          </div>

          <h1 class="article-title">{{ resourceDetail.title }}</h1>

          <div class="meta-bottom">
            <span>浏览 {{ resourceDetail.viewCount }}</span>
            <span class="dot">·</span>
            <span>收藏 {{ resourceDetail.favoriteCount }}</span>
            <span class="dot">·</span>
            <span>{{ formatDate(resourceDetail.publishedAt) }}</span>
          </div>
        </header>

        <section class="intro-spread">
          <div class="intro-text">
            <h3 class="intro-heading">内容导读</h3>
            <p class="summary-text">{{ resourceDetail.summaryText }}</p>

            <div class="tags-row">
              <span v-for="tag in resourceDetail.tags" :key="tag.tagId" class="minimal-tag">
                # {{ tag.name }}
              </span>
            </div>

            <p class="disclaimer">
              * 本片段用于辅助觉察与情绪整理，不替代专业医学诊断。
            </p>
          </div>

          <div class="intro-cover" v-if="(resourceDetail as any).coverUrl">
            <img :src="(resourceDetail as any).coverUrl" :alt="resourceDetail.title" class="cover-image" />
          </div>
        </section>

        <section class="media-stage">

          <video
              v-if="previewMode === 'video'"
              class="media-player"
              :src="resourceDetail.contentUrl"
              controls
              playsinline
          />

          <img
              v-else-if="previewMode === 'image'"
              class="media-image"
              :src="resourceDetail.contentUrl"
              :alt="resourceDetail.title"
          />

          <div v-else-if="previewMode === 'audio'" class="audio-stage">
            <img
                v-if="resourceDetail.coverUrl"
                class="audio-cover"
                :src="resourceDetail.coverUrl"
                :alt="resourceDetail.title"
            />
            <audio
                class="media-audio"
                :src="resourceDetail.contentUrl"
                controls
                preload="metadata"
            />
          </div>

          <iframe
              v-else-if="previewMode === 'article'"
              class="media-iframe"
              :src="resourceDetail.contentUrl"
              title="内容阅览"
          />

          <div v-else class="external-prompt">
            <div class="external-icon"></div>
            <h3>这份内容需要在新视窗中展开</h3>
            <p>为保证最佳的阅读体验，系统已将内容剥离。你可以点击下方按钮直接前往原始内容所在地。</p>
          </div>

        </section>

        <footer class="article-footer">
          <div class="footer-copy">
            <h3>收录与共鸣</h3>
            <p>如果这份内容给了你些许力量，不妨把它留在你的资料夹里，方便日后随时重温。</p>
          </div>

          <div class="action-buttons">
            <button
                class="btn-favorite"
                :class="{ 'is-active': isFavorited }"
                :disabled="togglingFavorite"
                @click="toggleFavorite"
            >
              {{ favoriteButtonText }}
            </button>
            <button class="btn-ghost" @click="openContent" v-if="resourceDetail.contentUrl">
              在新视窗中打开
            </button>
          </div>
        </footer>

      </article>

    </div>
  </main>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600&family=Noto+Serif+SC:wght@500;600;700&display=swap');

/* 全局沉浸底色 */
.editorial-detail-page {
  min-height: 100vh;
  background: #fcfbf9; /* 极度干净的纸张白/暖灰色 */
  color: #1e2821;
  font-family: 'Manrope', 'Noto Serif SC', sans-serif;
  padding-bottom: 8rem;
}

/* 顶部极简导航 */
.editorial-nav {
  position: sticky;
  top: 0;
  z-index: 10;
  padding: 1.5rem 4vw;
  background: linear-gradient(180deg, rgba(252, 251, 249, 0.95) 0%, rgba(252, 251, 249, 0) 100%);
}

.nav-back-btn {
  background: transparent;
  border: none;
  font-family: 'Noto Serif SC', serif;
  font-size: 1rem;
  color: #5c6b60;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  transition: color 0.3s ease;
}

.nav-back-btn:hover {
  color: #1e2821;
}

.arrow {
  font-family: 'Manrope', sans-serif;
  font-size: 1.2rem;
  transition: transform 0.3s ease;
}

.nav-back-btn:hover .arrow {
  transform: translateX(-4px);
}

/* 画报级容器 */
.editorial-container {
  max-width: 1000px; /* 控制最佳阅读行长 */
  margin: 0 auto;
  padding: 0 4vw;
}

/* 标题区：极具分量感的排版 */
.article-header {
  margin-top: 2rem;
  margin-bottom: 4rem;
  text-align: center;
}

.meta-top {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.category-badge {
  background: rgba(130, 150, 138, 0.15);
  color: #4a5c51;
  padding: 0.4rem 1rem;
  border-radius: 100px;
  font-family: 'Noto Serif SC', serif;
  font-size: 0.85rem;
  font-weight: 600;
}

.type-text {
  font-family: 'Noto Serif SC', serif;
  font-size: 0.9rem;
  color: #8a9c90;
  letter-spacing: 0.1em;
}

.article-title {
  font-family: 'Noto Serif SC', serif;
  font-size: clamp(2.5rem, 5vw, 4.2rem);
  font-weight: 700;
  color: #1e2821;
  line-height: 1.15;
  margin: 0 0 1.5rem 0;
  letter-spacing: 0.02em;
}

.meta-bottom {
  font-family: 'Manrope', 'Noto Serif SC', sans-serif;
  font-size: 0.95rem;
  color: #8a9c90;
  display: flex;
  justify-content: center;
  align-items: center;
}

.dot {
  margin: 0 0.8rem;
  color: #cbd5cf;
}

/* 导读区：杂志分栏结构 */
.intro-spread {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 4rem;
  margin-bottom: 4rem;
  align-items: center;
}

.intro-heading {
  font-family: 'Noto Serif SC', serif;
  font-size: 1.2rem;
  color: #5c6b60;
  margin: 0 0 1rem 0;
  position: relative;
  padding-left: 1rem;
}

.intro-heading::before {
  content: '';
  position: absolute;
  left: 0;
  top: 10%;
  height: 80%;
  width: 3px;
  background: #2a362e;
}

.summary-text {
  font-size: 1.1rem;
  color: #4a5c51;
  line-height: 1.8;
  margin: 0 0 2rem 0;
}

.tags-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0.8rem;
  margin-bottom: 2rem;
}

.minimal-tag {
  font-family: 'Manrope', 'Noto Serif SC', sans-serif;
  font-size: 0.85rem;
  color: #7b8c80;
}

.disclaimer {
  font-family: 'Noto Serif SC', serif;
  font-size: 0.85rem;
  color: #a3b0a7;
  font-style: italic;
  margin: 0;
}

.intro-cover {
  width: 100%;
  aspect-ratio: 4 / 3;
  border-radius: 2px; /* 故意使用极小圆角，增强海报感 */
  overflow: hidden;
  box-shadow: 0 20px 40px rgba(42, 54, 46, 0.08);
}

.cover-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* 沉浸式媒体区 */
.media-stage {
  width: 100%;
  margin-bottom: 5rem;
  border-radius: 16px;
  overflow: hidden;
  background: #f0f2f0;
  box-shadow: 0 30px 60px rgba(0, 0, 0, 0.05);
}

.media-player,
.media-image,
.media-iframe {
  width: 100%;
  display: block;
  border: none;
}

.audio-stage {
  display: grid;
  gap: 1.5rem;
  padding: 2rem;
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.72), rgba(244, 240, 233, 0.88));
}

.audio-cover {
  width: 100%;
  max-height: 360px;
  object-fit: cover;
  border-radius: 24px;
}

.media-audio {
  width: 100%;
}

.media-player {
  aspect-ratio: 16 / 9;
  background: #000;
}

.media-iframe {
  height: 70vh; /* 给长文章足够的阅读高度 */
  min-height: 600px;
}

.external-prompt {
  padding: 6rem 2rem;
  text-align: center;
  background: linear-gradient(135deg, rgba(200, 214, 205, 0.2) 0%, rgba(225, 218, 208, 0.2) 100%);
}

.external-prompt h3 {
  font-family: 'Noto Serif SC', serif;
  font-size: 1.5rem;
  color: #2a362e;
  margin: 0 0 1rem 0;
}

.external-prompt p {
  color: #6a7c70;
  max-width: 400px;
  margin: 0 auto;
  line-height: 1.6;
}

/* 底部操作区 */
.article-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 3rem;
  border-top: 1px solid rgba(42, 54, 46, 0.1);
}

.footer-copy h3 {
  font-family: 'Noto Serif SC', serif;
  font-size: 1.3rem;
  color: #1e2821;
  margin: 0 0 0.5rem 0;
}

.footer-copy p {
  font-size: 0.95rem;
  color: #7b8c80;
  margin: 0;
  max-width: 380px;
  line-height: 1.6;
}

.action-buttons {
  display: flex;
  gap: 1rem;
}

.btn-favorite,
.btn-ghost {
  padding: 1.2rem 2rem;
  border-radius: 100px;
  font-family: 'Noto Serif SC', serif;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.btn-favorite {
  background: #1e2821;
  border: 1px solid #1e2821;
  color: #ffffff;
  box-shadow: 0 10px 20px rgba(30, 40, 33, 0.2);
}

.btn-favorite:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 15px 30px rgba(30, 40, 33, 0.3);
}

.btn-favorite.is-active {
  background: #f0f2f0;
  color: #2a362e;
  border-color: #cbd5cf;
  box-shadow: none;
}

.btn-favorite:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-ghost {
  background: transparent;
  border: 1px solid rgba(130, 150, 138, 0.4);
  color: #5c6b60;
}

.btn-ghost:hover {
  background: rgba(255, 255, 255, 0.8);
  border-color: #2a362e;
  color: #1e2821;
}

/* 状态 */
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

.error-text {
  background: rgba(140, 74, 74, 0.08);
  color: #8c4a4a;
  padding: 1.5rem;
  border-radius: 12px;
  text-align: center;
  font-family: 'Noto Serif SC', serif;
  margin-top: 4rem;
}

/* 响应式适配 */
@media (max-width: 900px) {
  .intro-spread {
    grid-template-columns: 1fr;
    gap: 3rem;
  }

  .intro-cover {
    order: -1; /* 移动端封面图置顶 */
  }

  .article-footer {
    flex-direction: column;
    align-items: stretch;
    gap: 2rem;
  }

  .footer-copy {
    text-align: center;
  }

  .footer-copy p {
    max-width: 100%;
  }

  .action-buttons {
    flex-direction: column;
  }

  .btn-favorite,
  .btn-ghost {
    width: 100%;
    text-align: center;
  }
}
</style>

