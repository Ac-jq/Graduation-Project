<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { fetchStudentFavoritesApi, removeStudentFavoriteApi } from '@/api/resource'
import type { ResourceSummary } from '@/api/types'
import { toErrorMessage } from '@/views/shared/page-logic'

const router = useRouter()
const loading = ref(false)
const removing = ref(false)
const errorMessage = ref('')
const favorites = ref<ResourceSummary[]>([])

async function loadFavorites(): Promise<void> {
  loading.value = true
  errorMessage.value = ''

  try {
    favorites.value = await fetchStudentFavoritesApi()
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    loading.value = false
  }
}

async function removeFavorite(resourceId: number): Promise<void> {
  removing.value = true
  errorMessage.value = ''

  try {
    await removeStudentFavoriteApi(resourceId)
    await loadFavorites()
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    removing.value = false
  }
}

async function openResource(resourceId: number): Promise<void> {
  await router.push({ name: 'student-resource-detail', params: { resourceId } })
}

function formatDate(dateString?: string): string {
  if (!dateString) return ''
  const date = new Date(dateString)
  return `${date.getFullYear()}/${date.getMonth() + 1}/${date.getDate()}`
}

onMounted(() => {
  void loadFavorites()
})
</script>

<template>
  <main class="premium-gallery-page">
    <div class="premium-gallery-card">

      <header class="gallery-header">
        <div class="header-content">
          <span class="premium-tag">恢复资料夹</span>
          <h1 class="main-title">沉淀对你有帮助的片段</h1>
          <p class="sub-title">在这里，随时重温那些给你带来平静与启发的影音与图文。</p>
        </div>
        <div class="header-meta">
          <div class="meta-block">
            <span>共收藏</span>
            <strong>{{ loading ? '-' : favorites.length }}</strong>
          </div>
        </div>
      </header>

      <div v-if="errorMessage" class="error-banner">{{ errorMessage }}</div>

      <div v-if="loading" class="state-container">
        <div class="loading-orb"></div>
        <p class="state-text">正在整理您的资料夹...</p>
      </div>

      <div v-else-if="!favorites.length" class="state-container empty-state">
        <div class="empty-icon"></div>
        <h2 class="empty-title">这里还是空的</h2>
        <p class="state-text">去资源库逛逛，把有共鸣的内容留在这里。</p>
      </div>

      <section v-else class="media-grid">
        <article v-for="resource in favorites" :key="resource.resourceId" class="media-card">

          <div class="media-cover" @click="openResource(resource.resourceId)">
            <img
                :src="(resource as any).coverUrl || 'https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?q=80&w=600&auto=format&fit=crop'"
                class="cover-image"
                alt="资源封面"
            />
            <span class="media-type-badge">{{ resource.resourceType }}</span>
            <div class="play-overlay">
              <span class="play-text">查看内容</span>
            </div>
          </div>

          <div class="media-info">
            <div class="info-top">
              <span class="media-category">{{ resource.categoryName }}</span>
              <h3 class="media-title" @click="openResource(resource.resourceId)">{{ resource.title }}</h3>
              <p class="media-summary">{{ resource.summaryText }}</p>
            </div>

            <div class="info-bottom">
              <div class="media-stats">
                <span>浏览 {{ resource.viewCount }}</span>
                <span class="dot-divider">·</span>
                <span>收藏 {{ resource.favoriteCount }}</span>
                <template v-if="resource.publishedAt">
                  <span class="dot-divider">·</span>
                  <span>发布于 {{ formatDate(resource.publishedAt) }}</span>
                </template>
              </div>

              <div class="action-footer">
                <div class="tags-row">
                  <span v-for="tag in resource.tags?.slice(0, 2)" :key="tag.tagId" class="minimal-tag">
                    {{ tag.name }}
                  </span>
                </div>

                <button
                    class="ghost-remove-btn"
                    :disabled="removing"
                    @click="removeFavorite(resource.resourceId)"
                    title="取消收藏"
                >
                  取消收藏
                </button>
              </div>
            </div>
          </div>

        </article>
      </section>

    </div>
  </main>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700&family=Noto+Serif+SC:wght@500;600;700&display=swap');

/* 全局背景 */
.premium-gallery-page {
  min-height: 100vh;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  background: #f4f6f4;
  padding: 5vw 2rem;
  box-sizing: border-box;
}

/* 巨大的悬浮画板容器 */
.premium-gallery-card {
  width: 100%;
  max-width: 1100px;
  background: linear-gradient(
      145deg,
      rgba(255, 255, 255, 0.75) 0%,
      rgba(248, 246, 242, 0.85) 100%
  );
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border: 1px solid rgba(255, 255, 255, 0.9);
  border-radius: 40px;
  padding: 4.5rem;
  box-sizing: border-box;
  box-shadow:
      0 40px 80px rgba(54, 66, 58, 0.06),
      inset 0 2px 0 rgba(255, 255, 255, 0.8);
  display: flex;
  flex-direction: column;
}

/* 头部排版 */
.gallery-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 4rem;
  border-bottom: 1px solid rgba(42, 54, 46, 0.08);
  padding-bottom: 3rem;
}

.header-content {
  max-width: 60%;
}

.premium-tag {
  display: inline-block;
  font-family: 'Noto Serif SC', serif;
  font-size: 0.85rem;
  letter-spacing: 0.1em;
  color: #4a5c51;
  background: rgba(255, 255, 255, 0.6);
  padding: 0.5rem 1.2rem;
  border-radius: 100px;
  font-weight: 600;
  margin-bottom: 1.2rem;
}

.main-title {
  font-family: 'Noto Serif SC', serif;
  font-size: clamp(2.2rem, 4vw, 2.8rem);
  font-weight: 600;
  color: #1e2821;
  margin: 0 0 1rem 0;
  line-height: 1.2;
}

.sub-title {
  font-family: 'Noto Serif SC', serif;
  font-size: 1.1rem;
  color: #6a7c70;
  margin: 0;
  line-height: 1.8;
}

.meta-block {
  text-align: right;
  background: rgba(255, 255, 255, 0.5);
  padding: 1.2rem 2rem;
  border-radius: 24px;
  border: 1px solid rgba(130, 150, 138, 0.15);
}

.meta-block span {
  display: block;
  font-family: 'Noto Serif SC', serif;
  font-size: 0.85rem;
  color: #7b8c80;
  margin-bottom: 0.2rem;
}

.meta-block strong {
  font-family: 'Manrope', sans-serif;
  font-size: 2.2rem;
  font-weight: 600;
  color: #2a362e;
  line-height: 1;
}

/* 媒体网格 (画廊模式) */
.media-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 2.8rem;
}

/* 独立的媒体卡片 */
.media-card {
  display: flex;
  flex-direction: column;
  background: transparent;
  border-radius: 24px;
  overflow: hidden;
  transition: transform 0.4s cubic-bezier(0.16, 1, 0.3, 1);
}

.media-card:hover {
  transform: translateY(-6px);
}

/* 图片封面区域 */
.media-cover {
  position: relative;
  width: 100%;
  aspect-ratio: 16 / 9;
  border-radius: 24px;
  overflow: hidden;
  cursor: pointer;
  background: #e9ecea; /* 兜底背景色 */
  border: 1px solid rgba(255, 255, 255, 0.6);
}

.cover-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.6s ease;
}

.media-cover:hover .cover-image {
  transform: scale(1.05);
}

.media-type-badge {
  position: absolute;
  top: 1rem;
  left: 1rem;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  color: #2a362e;
  padding: 0.4rem 0.8rem;
  border-radius: 12px;
  font-family: 'Noto Serif SC', serif;
  font-size: 0.75rem;
  font-weight: 600;
  z-index: 2;
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
}

/* 悬停时的播放遮罩 */
.play-overlay {
  position: absolute;
  inset: 0;
  background: rgba(42, 54, 46, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.media-cover:hover .play-overlay {
  opacity: 1;
}

.play-text {
  color: white;
  font-family: 'Noto Serif SC', serif;
  font-weight: 600;
  font-size: 0.95rem;
  letter-spacing: 0.1em;
  padding: 0.8rem 1.5rem;
  border: 1px solid rgba(255, 255, 255, 0.8);
  border-radius: 100px;
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(8px);
  transform: translateY(10px);
  transition: transform 0.3s ease;
}

.media-cover:hover .play-text {
  transform: translateY(0);
}

/* 信息区 */
.media-info {
  padding: 1.5rem 0.5rem 0;
  display: flex;
  flex-direction: column;
  flex: 1;
  justify-content: space-between;
}

.info-top {
  margin-bottom: 1.2rem;
}

.media-category {
  display: block;
  font-family: 'Noto Serif SC', serif;
  font-size: 0.8rem;
  color: #8a9c90;
  margin-bottom: 0.5rem;
}

.media-title {
  font-family: 'Noto Serif SC', serif;
  font-size: 1.25rem;
  font-weight: 600;
  color: #1e2821;
  margin: 0 0 0.8rem 0;
  line-height: 1.4;
  cursor: pointer;
  transition: color 0.3s ease;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.media-title:hover {
  color: #5c6b60;
}

.media-summary {
  font-family: 'Noto Serif SC', serif;
  font-size: 0.9rem;
  color: #7b8c80;
  margin: 0;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 底部数据与操作 */
.info-bottom {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.media-stats {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  font-family: 'Manrope', 'Noto Serif SC', sans-serif;
  font-size: 0.8rem;
  color: #8fa094;
}

.dot-divider {
  margin: 0 0.5rem;
  color: #b5c2b9;
}

.action-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.tags-row {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.minimal-tag {
  font-family: 'Noto Serif SC', serif;
  font-size: 0.75rem;
  color: #6a7c70;
  background: rgba(130, 150, 138, 0.1);
  padding: 0.3rem 0.8rem;
  border-radius: 8px;
}

.ghost-remove-btn {
  background: transparent;
  border: none;
  font-family: 'Noto Serif SC', serif;
  font-size: 0.85rem;
  color: #b08e8e;
  cursor: pointer;
  padding: 0.4rem 0;
  transition: color 0.3s ease;
}

.ghost-remove-btn:hover:not(:disabled) {
  color: #8c4a4a;
}

.ghost-remove-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 状态提示 */
.error-banner {
  background: rgba(140, 74, 74, 0.08);
  border: 1px solid rgba(140, 74, 74, 0.2);
  color: #8c4a4a;
  padding: 1rem 1.5rem;
  border-radius: 16px;
  font-family: 'Noto Serif SC', serif;
  text-align: center;
  margin-bottom: 2rem;
}

.state-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 6rem 0;
}

.loading-orb {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  border: 2px solid rgba(130, 150, 138, 0.2);
  border-top-color: #2a362e;
  animation: spin 0.8s linear infinite;
  margin-bottom: 1.5rem;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.state-text {
  color: #7b8c80;
  font-family: 'Noto Serif SC', serif;
  font-size: 1.05rem;
}

.empty-state {
  padding: 8rem 0;
}

.empty-title {
  font-family: 'Noto Serif SC', serif;
  font-size: 1.8rem;
  color: #2a362e;
  margin: 0 0 1rem 0;
}

@media (max-width: 900px) {
  .gallery-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 2rem;
  }
  .header-content {
    max-width: 100%;
  }
}

@media (max-width: 600px) {
  .premium-gallery-card {
    padding: 2.5rem 1.5rem;
    border-radius: 32px;
  }
  .media-grid {
    grid-template-columns: 1fr;
  }
}
</style>