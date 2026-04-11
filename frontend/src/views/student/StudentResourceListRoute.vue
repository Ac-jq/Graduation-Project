<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { fetchResourceCategoriesApi, fetchResourcesApi, fetchResourceTagsApi } from '@/api/resource'
import type { ResourceCategory, ResourceQuery, ResourceSummary, ResourceTag } from '@/api/types'
import { toErrorMessage } from '@/views/shared/page-logic'

const router = useRouter()
const loading = ref(false)
const errorMessage = ref('')
const categories = ref<ResourceCategory[]>([])
const tags = ref<ResourceTag[]>([])
const resources = ref<ResourceSummary[]>([])
const filters = reactive<ResourceQuery>({
  categoryId: undefined,
  tagId: undefined,
  keyword: ''
})

const favoriteCount = computed(() => resources.value.filter((resource) => resource.favorite).length)
const selectedCategoryName = computed(() =>
  categories.value.find((category) => category.categoryId === filters.categoryId)?.name ?? '全部分类'
)
const selectedTagName = computed(() =>
  tags.value.find((tag) => tag.tagId === filters.tagId)?.name ?? '全部标签'
)

function formatDate(value: string | null): string {
  if (!value) {
    return '未发布'
  }

  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  }).format(new Date(value))
}

function resolveResourceType(type: string): string {
  switch (type) {
    case 'ARTICLE':
      return '文章'
    case 'VIDEO':
      return '视频'
    case 'AUDIO':
      return '音频'
    case 'LINK':
      return '外部链接'
    default:
      return type
  }
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
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    loading.value = false
  }
}

async function applyFilters(): Promise<void> {
  await loadResources()
}

async function resetFilters(): Promise<void> {
  filters.categoryId = undefined
  filters.tagId = undefined
  filters.keyword = ''
  await loadResources()
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
</script>

<template>
  <main class="resource-list-page">
    <section class="resource-list-page__masthead">
      <div class="resource-list-page__heading">
        <p class="resource-list-page__eyebrow">Curated Wellbeing Library</p>
        <h1 class="resource-list-page__title">心理资源目录</h1>
        <p class="resource-list-page__summary">
          你可以按分类、标签与关键词筛选自助资源，在阅读、观看与收藏之间建立自己的长期支持清单。
        </p>
      </div>

      <aside class="resource-list-page__snapshot">
        <p class="resource-list-page__label">Current Slice</p>
        <dl>
          <div>
            <dt>资源数量</dt>
            <dd>{{ resources.length }}</dd>
          </div>
          <div>
            <dt>收藏数</dt>
            <dd>{{ favoriteCount }}</dd>
          </div>
          <div>
            <dt>Category</dt>
            <dd>{{ selectedCategoryName }}</dd>
          </div>
          <div>
            <dt>Tag</dt>
            <dd>{{ selectedTagName }}</dd>
          </div>
        </dl>
      </aside>
    </section>

    <p v-if="errorMessage" class="resource-list-page__alert">{{ errorMessage }}</p>

    <section class="resource-list-page__filter-grid">
      <article class="resource-filter-panel">
        <div class="resource-filter-panel__head">
          <p class="resource-list-page__label">关键词</p>
          <button class="resource-list-page__ghost" type="button" @click="resetFilters">重置筛选</button>
        </div>
        <label class="resource-filter-panel__field">
          <span>搜索内容</span>
          <input v-model="filters.keyword" type="text" placeholder="输入标题、摘要或关键字" @keyup.enter="applyFilters" />
        </label>
        <button class="resource-list-page__primary" type="button" :disabled="loading" @click="applyFilters">
          {{ loading ? '筛选中...' : '应用筛选' }}
        </button>
      </article>

      <article class="resource-filter-panel">
        <p class="resource-list-page__label">Categories</p>
        <div class="resource-filter-panel__chips">
          <button
            type="button"
            class="resource-chip"
            :class="{ 'resource-chip--active': filters.categoryId == null }"
            @click="toggleCategory(undefined)"
          >
            全部分类
          </button>
          <button
            v-for="category in categories"
            :key="category.categoryId"
            type="button"
            class="resource-chip"
            :class="{ 'resource-chip--active': filters.categoryId === category.categoryId }"
            @click="toggleCategory(category.categoryId)"
          >
            {{ category.name }}
          </button>
        </div>
      </article>

      <article class="resource-filter-panel">
        <p class="resource-list-page__label">Tags</p>
        <div class="resource-filter-panel__chips">
          <button
            type="button"
            class="resource-chip"
            :class="{ 'resource-chip--active': filters.tagId == null }"
            @click="toggleTag(undefined)"
          >
            全部标签
          </button>
          <button
            v-for="tag in tags"
            :key="tag.tagId"
            type="button"
            class="resource-chip"
            :class="{ 'resource-chip--active': filters.tagId === tag.tagId }"
            @click="toggleTag(tag.tagId)"
          >
            {{ tag.name }}
          </button>
        </div>
      </article>
    </section>

    <section v-if="loading" class="resource-list-page__status-panel">
      <p>正在加载资源目录...</p>
    </section>

    <section v-else-if="resources.length" class="resource-list-page__grid">
      <article v-for="resource in resources" :key="resource.resourceId" class="resource-card" @click="openResource(resource.resourceId)">
        <div class="resource-card__header">
          <p class="resource-card__type">{{ resolveResourceType(resource.resourceType) }}</p>
          <p class="resource-card__published">{{ formatDate(resource.publishedAt) }}</p>
        </div>

        <div class="resource-card__body">
          <h2>{{ resource.title }}</h2>
          <p>{{ resource.summaryText }}</p>
        </div>

        <div class="resource-card__tags">
          <span>{{ resource.categoryName }}</span>
          <span v-for="tag in resource.tags" :key="tag.tagId">{{ tag.name }}</span>
        </div>

        <dl class="resource-card__stats">
          <div>
            <dt>Views</dt>
            <dd>{{ resource.viewCount }}</dd>
          </div>
          <div>
            <dt>收藏数</dt>
            <dd>{{ resource.favoriteCount }}</dd>
          </div>
          <div>
            <dt>收藏状态</dt>
            <dd>{{ resource.favorite ? '已收藏' : '未收藏' }}</dd>
          </div>
        </dl>
      </article>
    </section>

    <section v-else class="resource-list-page__status-panel">
      <p>当前筛选条件下没有资源，尝试放宽关键词或切换分类与标签。</p>
      <button class="resource-list-page__ghost" type="button" @click="resetFilters">清空筛选条件</button>
    </section>
  </main>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700&family=Noto+Serif+SC:wght@400;500;600;700&display=swap');

.resource-list-page {
  --paper: #f4efe5;
  --ink: #201c18;
  --muted: #6e665f;
  --line: rgba(32, 28, 24, 0.12);
  --glass: rgba(255, 251, 245, 0.68);
  --accent: #647d6d;
  min-height: 100vh;
  padding: 2rem;
  color: var(--ink);
  background:
    radial-gradient(circle at top right, rgba(114, 136, 121, 0.18), transparent 26%),
    radial-gradient(circle at left center, rgba(198, 186, 168, 0.22), transparent 30%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.14), transparent 38%),
    var(--paper);
}

.resource-list-page__masthead {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(300px, 0.85fr);
  gap: 1.5rem;
  align-items: end;
  padding-bottom: 1.4rem;
  border-bottom: 1px solid var(--line);
}

.resource-list-page__eyebrow,
.resource-list-page__label,
.resource-list-page__snapshot dt,
.resource-card__type,
.resource-card__published,
.resource-card__stats dt,
.resource-card__tags span,
.resource-filter-panel__field span {
  margin: 0;
  font: 600 0.72rem/1.4 'Manrope', sans-serif;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--muted);
}

.resource-list-page__title {
  margin: 0.95rem 0 0;
  font: 600 clamp(2.8rem, 5vw, 5.1rem)/0.98 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.resource-list-page__summary {
  max-width: 46rem;
  margin: 1rem 0 0;
  color: var(--muted);
  font: 400 1rem/1.9 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.resource-list-page__snapshot,
.resource-filter-panel,
.resource-card,
.resource-list-page__status-panel {
  border: 1px solid var(--line);
  background: var(--glass);
  backdrop-filter: blur(18px);
  box-shadow: 0 22px 48px rgba(80, 70, 58, 0.08);
}

.resource-list-page__snapshot {
  padding: 1.2rem;
}

.resource-list-page__snapshot dl {
  display: grid;
  gap: 0.9rem;
  margin: 1rem 0 0;
}

.resource-list-page__snapshot dd {
  margin: 0.35rem 0 0;
  font: 600 1.04rem/1.45 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.resource-list-page__alert {
  margin: 1.25rem 0 0;
  color: #8d4747;
  font: 600 0.9rem/1.6 'Manrope', sans-serif;
}

.resource-list-page__filter-grid {
  display: grid;
  grid-template-columns: minmax(0, 0.9fr) minmax(0, 1fr) minmax(0, 1fr);
  gap: 1rem;
  margin-top: 1.5rem;
}

.resource-filter-panel {
  display: grid;
  gap: 1rem;
  padding: 1.2rem;
}

.resource-filter-panel__head {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
}

.resource-filter-panel__field {
  display: grid;
  gap: 0.7rem;
}

.resource-filter-panel__field input {
  min-height: 3rem;
  padding: 0 0.95rem;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.54);
  color: var(--ink);
  font: 500 0.95rem/1.5 'Manrope', sans-serif;
}

.resource-filter-panel__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 0.7rem;
}

.resource-chip,
.resource-list-page__primary,
.resource-list-page__ghost {
  min-height: 2.9rem;
  padding: 0 1rem;
  font: 600 0.82rem/1 'Manrope', sans-serif;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  cursor: pointer;
  transition: transform 180ms ease, border-color 180ms ease, box-shadow 180ms ease, background 180ms ease;
}

.resource-chip,
.resource-list-page__ghost {
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.48);
  color: var(--ink);
}

.resource-chip--active {
  border-color: rgba(100, 125, 109, 0.42);
  background: rgba(100, 125, 109, 0.12);
}

.resource-list-page__primary {
  border: none;
  background: linear-gradient(135deg, #6b8473, #4f6656);
  color: #faf6f0;
  box-shadow: 0 18px 36px rgba(79, 102, 86, 0.24);
}

.resource-chip:hover,
.resource-list-page__primary:hover,
.resource-list-page__ghost:hover,
.resource-card:hover {
  transform: translateY(-2px);
}

.resource-list-page__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
  margin-top: 1.5rem;
}

.resource-card {
  display: grid;
  gap: 1rem;
  padding: 1.25rem;
  cursor: pointer;
  transition: transform 180ms ease, border-color 180ms ease, box-shadow 180ms ease;
}

.resource-card:hover {
  border-color: rgba(100, 125, 109, 0.38);
  box-shadow: 0 26px 46px rgba(80, 70, 58, 0.12);
}

.resource-card__header,
.resource-card__stats {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
}

.resource-card__body h2 {
  margin: 0;
  font: 600 1.45rem/1.34 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.resource-card__body p {
  margin: 0.85rem 0 0;
  color: var(--muted);
  font: 400 0.96rem/1.85 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.resource-card__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
}

.resource-card__tags span {
  padding: 0.45rem 0.7rem;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.42);
}

.resource-card__stats {
  padding-top: 1rem;
  border-top: 1px solid var(--line);
}

.resource-card__stats dd {
  margin: 0.35rem 0 0;
  font: 600 0.98rem/1.45 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.resource-list-page__status-panel {
  display: grid;
  gap: 1rem;
  margin-top: 1.5rem;
  padding: 1.35rem;
}

.resource-list-page__status-panel p {
  margin: 0;
  color: var(--muted);
  font: 400 0.98rem/1.9 'Noto Serif SC', 'Source Han Serif SC', serif;
}

@media (max-width: 980px) {
  .resource-list-page {
    padding: 1rem;
  }

  .resource-list-page__masthead,
  .resource-list-page__filter-grid,
  .resource-list-page__grid {
    grid-template-columns: 1fr;
  }

  .resource-filter-panel__head,
  .resource-card__header,
  .resource-card__stats {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>

