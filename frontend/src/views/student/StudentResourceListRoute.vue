<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchResourceCategoriesApi, fetchResourcesApi, fetchResourceTagsApi } from '@/api/resource'
import type { ResourceCategory, ResourceQuery, ResourceSummary, ResourceTag } from '@/api/types'
import { toErrorMessage } from '@/views/shared/page-logic'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const errorMessage = ref('')
const categories = ref<ResourceCategory[]>([])
const tags = ref<ResourceTag[]>([])
const resources = ref<ResourceSummary[]>([])
const currentPage = ref(1)
const pageSize = 6
const filters = reactive<ResourceQuery>({
  categoryId: undefined,
  tagId: undefined,
  keyword: ''
})
const totalPages = computed(() => Math.max(1, Math.ceil(resources.value.length / pageSize)))
const pagedResources = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return resources.value.slice(start, start + pageSize)
})

const selectedCategoryName = computed(() =>
    categories.value.find((category) => category.categoryId === filters.categoryId)?.name ?? '所有分类'
)
const selectedTagName = computed(() =>
    tags.value.find((tag) => tag.tagId === filters.tagId)?.name ?? '所有标签'
)

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

function buildResourceMemory(resource: ResourceSummary): string {
  if (resource.resourceType === 'VIDEO') return '适合在高压与走神之间做短暂停顿。'
  if (resource.resourceType === 'ARTICLE') return '适合保存后反复回看，做成支持卡片。'
  return '适合放进长期自助清单。'
}

async function loadResourceMeta(): Promise<void> {
  const [categoryList, tagList] = await Promise.all([fetchResourceCategoriesApi(), fetchResourceTagsApi()])
  categories.value = categoryList
  tags.value = tagList
}

async function loadResources(): Promise<void> {
  loading.value = true
  errorMessage.value = ''
  try {
    await loadResourceMeta()
    resources.value = await fetchResourcesApi({
      ...filters,
      keyword: filters.keyword?.trim() || undefined
    })
    currentPage.value = resolvePageFromQuery()
    clampCurrentPage()
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    loading.value = false
  }
}

function resolvePageFromQuery(): number {
  const rawPage = Array.isArray(route.query.page) ? route.query.page[0] : route.query.page
  const parsedPage = Number.parseInt(rawPage ?? '1', 10)
  return Number.isFinite(parsedPage) && parsedPage > 0 ? parsedPage : 1
}

function clampCurrentPage(): void {
  currentPage.value = Math.min(Math.max(currentPage.value, 1), totalPages.value)
}

async function syncPageQuery(page: number): Promise<void> {
  await router.replace({
    name: 'student-resources',
    query: {
      ...route.query,
      page: page > 1 ? String(page) : undefined
    }
  })
}

function prevPage(): void {
  if (currentPage.value > 1) {
    currentPage.value--
    void syncPageQuery(currentPage.value)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
}

function nextPage(): void {
  if (currentPage.value < totalPages.value) {
    currentPage.value++
    void syncPageQuery(currentPage.value)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
}

async function applyFilters(): Promise<void> {
  await loadResources()
  await syncPageQuery(1)
}

async function resetFilters(): Promise<void> {
  filters.categoryId = undefined
  filters.tagId = undefined
  filters.keyword = ''
  await loadResources()
  await syncPageQuery(1)
}

async function openResource(resourceId: number): Promise<void> {
  await router.push({ name: 'student-resource-detail', params: { resourceId } })
}

function toggleCategory(categoryId?: number): void {
  filters.categoryId = filters.categoryId === categoryId ? undefined : categoryId
}

function toggleTag(tagId?: number): void {
  filters.tagId = filters.tagId === tagId ? undefined : tagId
}

onMounted(() => {
  void loadResources()
})

watch(() => route.query.page, () => {
  currentPage.value = resolvePageFromQuery()
  clampCurrentPage()
})
</script>

<template>
  <main class="magazine-library-page">

    <header class="library-hero">
      <div class="hero-content">
        <h1 class="hero-title">寻找安定的力量</h1>
        <p class="hero-summary">
          这里收录了可供随时查看的图文与影像。你可以自由探索，把有共鸣的片段沉淀为自己的支持清单。
        </p>
      </div>
    </header>

    <div class="library-layout">

      <aside class="sidebar-filter">
        <div class="sidebar-sticky">

          <div class="filter-group">
            <h3 class="filter-title">关键词探索</h3>
            <input
                v-model="filters.keyword"
                type="text"
                class="sleek-input"
                placeholder="如：睡前、考试周、呼吸..."
                @keyup.enter="applyFilters"
            />
          </div>

          <div class="filter-group">
            <h3 class="filter-title">内容领域</h3>
            <ul class="category-list">
              <li
                  class="category-item"
                  :class="{ 'is-active': filters.categoryId == null }"
                  @click="toggleCategory(undefined)"
              >
                所有分类
              </li>
              <li
                  v-for="category in categories"
                  :key="category.categoryId"
                  class="category-item"
                  :class="{ 'is-active': filters.categoryId === category.categoryId }"
                  @click="toggleCategory(category.categoryId)"
              >
                {{ category.name }}
              </li>
            </ul>
          </div>

          <div class="filter-group">
            <h3 class="filter-title">情绪标签</h3>
            <div class="tags-cloud">
              <button
                  class="minimal-tag"
                  :class="{ 'is-active': filters.tagId == null }"
                  @click="toggleTag(undefined)"
              >
                全部
              </button>
              <button
                  v-for="tag in tags"
                  :key="tag.tagId"
                  class="minimal-tag"
                  :class="{ 'is-active': filters.tagId === tag.tagId }"
                  @click="toggleTag(tag.tagId)"
              >
                {{ tag.name }}
              </button>
            </div>
          </div>

          <div class="filter-actions">
            <button class="btn-apply" :disabled="loading" @click="applyFilters">
              {{ loading ? '检索中...' : '开始探索' }}
            </button>
            <button class="btn-reset" @click="resetFilters">清空条件</button>
          </div>

        </div>
      </aside>

      <section class="main-content">

        <div v-if="errorMessage" class="error-text">{{ errorMessage }}</div>

        <div class="content-header">
          <span class="view-status">
            当前：{{ selectedCategoryName }} <span class="dot">·</span> {{ selectedTagName }}
          </span>
          <span class="result-count">共 {{ resources.length }} 份内容</span>
        </div>

        <div v-if="loading" class="loading-state">
          <div class="spinner"></div>
          <p>正在展卷...</p>
        </div>

        <div v-else-if="!resources.length" class="empty-state">
          <h2>未找到相关内容</h2>
          <p>尝试放宽关键词，或在左侧切换其他领域看看。</p>
        </div>

        <div v-else class="resource-grid">
          <article
              v-for="resource in pagedResources"
              :key="resource.resourceId"
              class="media-block"
              @click="openResource(resource.resourceId)"
          >
            <div class="media-cover">
              <img
                  :src="(resource as any).coverUrl || 'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?q=80&w=600&auto=format&fit=crop'"
                  class="cover-img"
                  alt="cover"
              />
              <span class="type-badge">{{ resolveResourceType(resource.resourceType) }}</span>
              <div v-if="resource.favorite" class="favorite-mark">已收藏</div>
            </div>

            <div class="media-text">
              <div class="meta-line">
                <span class="category-name">{{ resource.categoryName }}</span>
                <span class="publish-date">{{ formatDate(resource.publishedAt) }}</span>
              </div>

              <h2 class="media-title">{{ resource.title }}</h2>
              <p class="media-summary">{{ resource.summaryText }}</p>

              <blockquote class="media-memory">
                “{{ buildResourceMemory(resource) }}”
              </blockquote>

              <div class="stats-line">
                <span>浏览 {{ resource.viewCount }}</span>
                <span>收藏 {{ resource.favoriteCount }}</span>
              </div>
            </div>
          </article>
        </div>

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

      </section>
    </div>
  </main>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600&family=Noto+Serif+SC:wght@500;600;700&display=swap');

/* 页面基调：大面积呼吸感 */
.magazine-library-page {
  min-height: 100vh;
  background: #f8f9f8;
  color: #1e2821;
  font-family: 'Manrope', 'Noto Serif SC', sans-serif;
  padding-bottom: 6rem;
}

/* 顶部导语区 */
.library-hero {
  padding: 6vw 4vw 4vw;
  background: linear-gradient(180deg, #edf0ee 0%, #f8f9f8 100%);
  border-bottom: 1px solid rgba(42, 54, 46, 0.06);
}

.hero-content {
  max-width: 1400px;
  margin: 0 auto;
}

.hero-title {
  font-family: 'Noto Serif SC', serif;
  font-size: clamp(2.5rem, 4vw, 3.8rem);
  font-weight: 600;
  color: #1e2821;
  margin: 0 0 1.2rem 0;
  letter-spacing: 0.02em;
}

.hero-summary {
  font-size: 1.15rem;
  color: #5c6b60;
  max-width: 600px;
  line-height: 1.8;
  margin: 0;
}

/* 杂志分栏布局 */
.library-layout {
  max-width: 1400px;
  margin: 0 auto;
  padding: 4rem 4vw;
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 5rem;
  align-items: start;
}

/* 侧边栏与吸顶交互 */
.sidebar-filter {
  position: relative;
}

.sidebar-sticky {
  position: sticky;
  top: 2rem; /* UI/UX 视差跟随 */
  display: flex;
  flex-direction: column;
  gap: 2.5rem;
}

.filter-group {
  display: flex;
  flex-direction: column;
  gap: 1.2rem;
}

.filter-title {
  font-family: 'Noto Serif SC', serif;
  font-size: 0.95rem;
  font-weight: 600;
  color: #8a9c90;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  margin: 0;
}

/* 考究的输入框 */
.sleek-input {
  width: 100%;
  border: none;
  border-bottom: 1px solid rgba(42, 54, 46, 0.2);
  background: transparent;
  padding: 0.5rem 0;
  font-family: 'Noto Serif SC', serif;
  font-size: 1.05rem;
  color: #1e2821;
  outline: none;
  transition: border-color 0.3s ease;
}

.sleek-input:focus {
  border-bottom-color: #2a362e;
}

.sleek-input::placeholder {
  color: #b5c2b9;
}

/* 分类列表 */
.category-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 0.8rem;
}

.category-item {
  font-family: 'Noto Serif SC', serif;
  font-size: 1.05rem;
  color: #5c6b60;
  cursor: pointer;
  transition: all 0.2s ease;
  padding-left: 0.5rem;
  border-left: 2px solid transparent;
}

.category-item:hover {
  color: #2a362e;
}

.category-item.is-active {
  color: #1e2821;
  font-weight: 600;
  border-left-color: #2a362e;
}

/* 标签云 */
.tags-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
}

.minimal-tag {
  background: transparent;
  border: 1px solid rgba(130, 150, 138, 0.3);
  padding: 0.4rem 0.9rem;
  border-radius: 100px;
  font-family: 'Noto Serif SC', serif;
  font-size: 0.85rem;
  color: #5c6b60;
  cursor: pointer;
  transition: all 0.2s ease;
}

.minimal-tag:hover {
  border-color: #5c6b60;
  color: #2a362e;
}

.minimal-tag.is-active {
  background: #2a362e;
  border-color: #2a362e;
  color: #ffffff;
}

/* 操作按钮 */
.filter-actions {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  margin-top: 1rem;
}

.btn-apply {
  background: #2a362e;
  color: #ffffff;
  border: none;
  padding: 1rem;
  border-radius: 100px;
  font-family: 'Noto Serif SC', serif;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.btn-apply:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 10px 20px rgba(42, 54, 46, 0.15);
}

.btn-apply:disabled {
  background: #8a9c90;
  cursor: not-allowed;
}

.btn-reset {
  background: transparent;
  border: none;
  color: #8a9c90;
  font-family: 'Noto Serif SC', serif;
  font-size: 0.9rem;
  cursor: pointer;
}

.btn-reset:hover {
  color: #5c6b60;
}

/* 右侧内容区 */
.content-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 3rem;
  padding-bottom: 1.5rem;
  border-bottom: 1px solid rgba(42, 54, 46, 0.08);
}

.view-status {
  font-family: 'Noto Serif SC', serif;
  font-size: 1.1rem;
  color: #2a362e;
  font-weight: 500;
}

.dot {
  color: #b5c2b9;
  margin: 0 0.5rem;
}

.result-count {
  font-family: 'Manrope', serif;
  font-size: 0.9rem;
  color: #8a9c90;
}

/* 资源网格 (杂志风) */
.resource-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 4rem 2.5rem; /* 垂直间距极大，增强阅读感 */
}

/* 无边框区块 */
.media-block {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  cursor: pointer;
  group: hover;
}

.media-cover {
  position: relative;
  width: 100%;
  aspect-ratio: 16 / 10;
  border-radius: 16px;
  overflow: hidden;
  background: #e9ecea;
}

.cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.7s cubic-bezier(0.16, 1, 0.3, 1);
}

.media-block:hover .cover-img {
  transform: scale(1.04);
}

.type-badge {
  position: absolute;
  top: 1rem;
  left: 1rem;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(8px);
  color: #1e2821;
  padding: 0.3rem 0.8rem;
  border-radius: 8px;
  font-family: 'Noto Serif SC', serif;
  font-size: 0.75rem;
  font-weight: 600;
}

.favorite-mark {
  position: absolute;
  top: 1rem;
  right: 1rem;
  background: rgba(42, 54, 46, 0.8);
  color: #ffffff;
  padding: 0.3rem 0.8rem;
  border-radius: 8px;
  font-family: 'Noto Serif SC', serif;
  font-size: 0.75rem;
}

/* 文本排版区 */
.media-text {
  display: flex;
  flex-direction: column;
}

.meta-line {
  display: flex;
  justify-content: space-between;
  font-family: 'Noto Serif SC', serif;
  font-size: 0.85rem;
  color: #8a9c90;
  margin-bottom: 0.8rem;
}

.category-name {
  color: #5c6b60;
  font-weight: 600;
}

.media-title {
  font-family: 'Noto Serif SC', serif;
  font-size: 1.35rem;
  font-weight: 600;
  color: #1e2821;
  margin: 0 0 0.8rem 0;
  line-height: 1.4;
  transition: color 0.3s ease;
}

.media-block:hover .media-title {
  color: #5c6b60;
}

.media-summary {
  font-size: 0.95rem;
  color: #6a7c70;
  line-height: 1.7;
  margin: 0 0 1rem 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.media-memory {
  margin: 0 0 1.2rem 0;
  padding-left: 1rem;
  border-left: 2px solid rgba(130, 150, 138, 0.3);
  font-family: 'Noto Serif SC', serif;
  font-size: 0.9rem;
  font-style: italic;
  color: #8a9c90;
}

.stats-line {
  display: flex;
  gap: 1.5rem;
  font-family: 'Manrope', 'Noto Serif SC', sans-serif;
  font-size: 0.8rem;
  color: #a3b0a7;
  padding-top: 1rem;
  border-top: 1px solid rgba(42, 54, 46, 0.06);
}

/* 状态展示 */
.loading-state,
.empty-state {
  padding: 6rem 0;
  text-align: center;
  color: #7b8c80;
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

.empty-state h2 {
  font-family: 'Noto Serif SC', serif;
  color: #2a362e;
  margin-bottom: 0.5rem;
}

.error-text {
  background: rgba(140, 74, 74, 0.08);
  color: #8c4a4a;
  padding: 1rem;
  border-radius: 12px;
  margin-bottom: 2rem;
  font-family: 'Noto Serif SC', serif;
}

/* 响应式适配 */
@media (max-width: 1024px) {
  .library-layout {
    grid-template-columns: 240px 1fr;
    gap: 3rem;
  }
}

@media (max-width: 768px) {
  .library-layout {
    grid-template-columns: 1fr; /* 侧边栏移至顶部 */
    gap: 2rem;
    padding: 2rem 4vw;
  }

  .sidebar-sticky {
    position: relative;
    top: 0;
  }

  .category-list {
    flex-direction: row;
    flex-wrap: wrap;
    gap: 1rem;
  }

  .category-item {
    border-left: none;
    border-bottom: 2px solid transparent;
    padding-left: 0;
    padding-bottom: 0.3rem;
  }

  .category-item.is-active {
    border-bottom-color: #2a362e;
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
