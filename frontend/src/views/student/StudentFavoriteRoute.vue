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

onMounted(() => {
  void loadFavorites()
})
</script>

<template>
  <section class="favorite-page">
    <div class="page-shell">
      <header class="page-hero">
        <div class="hero-copy">
          <p class="eyebrow">收藏清单</p>
          <h1>把真正对你有帮助的内容沉淀下来，形成自己的恢复资料夹。</h1>
          <p class="lead">
            收藏列表直接读取后端真实资源数据，包含分类、标签、浏览量与收藏量，你可以继续进入详情或取消收藏。
          </p>
        </div>
        <div class="hero-aside">
          <div class="metric-card">
            <span>收藏总数</span>
            <strong>{{ favorites.length }}</strong>
          </div>
        </div>
      </header>

      <section class="favorite-panel">
        <div class="section-head section-head-inline">
          <div>
            <p class="section-kicker">收藏归档</p>
            <h2>我的资源收藏</h2>
          </div>
          <span class="status-chip">{{ loading ? '读取中' : `${favorites.length} 条收藏` }}</span>
        </div>

        <p v-if="loading" class="state-text">正在读取收藏资源...</p>
        <p v-else-if="!favorites.length" class="state-text">当前还没有收藏内容，你可以先去资源库挑选对自己有帮助的条目。</p>

        <div v-else class="favorite-grid">
          <article v-for="resource in favorites" :key="resource.resourceId" class="favorite-card">
            <div class="resource-topline">
              <div>
                <p class="resource-category">{{ resource.categoryName }}</p>
                <h3>{{ resource.title }}</h3>
              </div>
              <span class="resource-type">{{ resource.resourceType }}</span>
            </div>
            <p class="resource-summary">{{ resource.summaryText }}</p>
            <div class="tag-list">
              <span v-for="tag in resource.tags" :key="tag.tagId" class="tag-pill">{{ tag.name }}</span>
            </div>
            <div class="resource-meta">
              <span>浏览 {{ resource.viewCount }}</span>
              <span>收藏 {{ resource.favoriteCount }}</span>
              <span v-if="resource.publishedAt">发布于 {{ new Date(resource.publishedAt).toLocaleDateString('zh-CN') }}</span>
            </div>
            <div class="action-row">
              <button class="ghost-button" type="button" @click="openResource(resource.resourceId)">查看详情</button>
              <button class="danger-button" type="button" :disabled="removing" @click="removeFavorite(resource.resourceId)">
                {{ removing ? '处理中...' : '取消收藏' }}
              </button>
            </div>
          </article>
        </div>

        <p v-if="errorMessage" class="error-text">{{ errorMessage }}</p>
      </section>
    </div>
  </section>
</template>
<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700&family=Noto+Serif+SC:wght@400;500;600;700&display=swap');

:global(body) {
  background:
    radial-gradient(circle at 12% 16%, rgba(205, 221, 208, 0.28), transparent 24%),
    radial-gradient(circle at 86% 18%, rgba(228, 217, 203, 0.3), transparent 24%),
    linear-gradient(180deg, #f5f0e6 0%, #f8f5ee 100%);
}

.favorite-page {
  min-height: 100vh;
  padding: 44px 28px 72px;
  color: #2b3029;
}

.page-shell {
  max-width: 1320px;
  margin: 0 auto;
}

.page-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(220px, 0.55fr);
  gap: 28px;
  align-items: end;
  margin-bottom: 30px;
}

.hero-copy {
  border-top: 1px solid rgba(64, 72, 63, 0.16);
  padding-top: 18px;
}

.eyebrow,
.section-kicker {
  margin: 0 0 10px;
  font: 700 0.76rem/1 'Manrope', sans-serif;
  letter-spacing: 0.22em;
  text-transform: uppercase;
  color: #7f6a57;
}

.hero-copy h1,
.section-head h2,
.favorite-card h3 {
  margin: 0;
  font-family: 'Noto Serif SC', serif;
  font-weight: 600;
  letter-spacing: -0.02em;
}

.hero-copy h1 {
  font-size: clamp(1.96rem, 3vw, 3.25rem);
  line-height: 1.16;
}

.lead {
  max-width: 720px;
  margin: 18px 0 0;
  font: 400 1rem/1.84 'Manrope', sans-serif;
  color: rgba(43, 48, 41, 0.74);
}

.metric-card,
.favorite-panel,
.favorite-card {
  border: 1px solid rgba(78, 86, 77, 0.14);
  background: rgba(255, 252, 247, 0.74);
  box-shadow: 0 24px 70px rgba(91, 80, 66, 0.08);
  backdrop-filter: blur(16px);
}

.metric-card {
  padding: 18px 20px;
}

.metric-card span,
.resource-summary,
.resource-meta,
.state-text,
.error-text {
  font-family: 'Manrope', sans-serif;
}

.metric-card span {
  display: block;
  margin-bottom: 8px;
  font-size: 0.78rem;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: rgba(68, 74, 66, 0.56);
}

.metric-card strong {
  font-family: 'Noto Serif SC', serif;
  font-size: 1.6rem;
  font-weight: 600;
}

.favorite-panel {
  padding: 24px;
}

.section-head {
  margin-bottom: 18px;
}

.section-head-inline {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: end;
}

.status-chip {
  border: 1px solid rgba(88, 93, 84, 0.14);
  background: rgba(255, 250, 240, 0.82);
  padding: 9px 14px;
  font: 700 0.76rem/1 'Manrope', sans-serif;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #696152;
}

.favorite-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.favorite-card {
  padding: 22px;
}

.resource-topline {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: start;
  margin-bottom: 12px;
}

.resource-category {
  margin: 0 0 6px;
  font: 700 0.78rem/1 'Manrope', sans-serif;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: #7f6957;
}

.favorite-card h3 {
  font-size: 1.28rem;
  line-height: 1.35;
}

.resource-type {
  flex-shrink: 0;
  border: 1px solid rgba(98, 112, 99, 0.16);
  background: rgba(242, 244, 237, 0.94);
  padding: 8px 12px;
  font: 700 0.74rem/1 'Manrope', sans-serif;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #66735f;
}

.resource-summary {
  margin: 0 0 14px;
  font-size: 0.96rem;
  line-height: 1.84;
  color: rgba(43, 48, 41, 0.72);
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 14px;
}

.tag-pill {
  border: 1px solid rgba(125, 130, 119, 0.18);
  background: rgba(255, 255, 255, 0.62);
  padding: 6px 10px;
  font: 600 0.78rem/1 'Manrope', sans-serif;
  color: #5b6158;
}

.resource-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 18px;
  font-size: 0.82rem;
  color: rgba(68, 74, 66, 0.58);
}

.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 16px;
}

.ghost-button,
.danger-button {
  border: 1px solid rgba(54, 65, 56, 0.2);
  background: rgba(255, 255, 255, 0.58);
  color: #2b3029;
  padding: 12px 16px;
  font: 700 0.82rem/1 'Manrope', sans-serif;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  cursor: pointer;
  transition: transform 0.28s ease, box-shadow 0.28s ease, border-color 0.28s ease, opacity 0.28s ease;
}

.danger-button {
  color: #8f4d3b;
}

.ghost-button:hover,
.danger-button:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 16px 30px rgba(55, 67, 57, 0.1);
}

.danger-button:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.state-text,
.error-text {
  margin: 14px 0 0;
  font-size: 0.96rem;
  line-height: 1.8;
}

.error-text {
  font-weight: 600;
  color: #a64939;
}

@media (max-width: 980px) {
  .page-hero,
  .favorite-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .favorite-page {
    padding: 28px 16px 46px;
  }

  .hero-copy h1,
  .section-head h2 {
    font-size: 1.82rem;
  }

  .resource-topline,
  .section-head-inline {
    flex-direction: column;
    align-items: start;
  }
}
</style>

