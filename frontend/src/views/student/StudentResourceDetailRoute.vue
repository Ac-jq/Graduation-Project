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

const favoriteButtonText = computed(() => {
  if (togglingFavorite.value) {
    return '处理中...'
  }

  return resourceDetail.value?.favorite ? '取消收藏' : '加入收藏'
})

function formatDate(value: string | null): string {
  if (!value) {
    return '未发布'
  }

  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
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

async function loadResourceDetail(): Promise<void> {
  if (!resourceId.value) {
    errorMessage.value = '无效的资源编号。'
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
  if (!resourceId.value || !resourceDetail.value) {
    errorMessage.value = '无效的资源编号。'
    return
  }

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

async function jumpTo收藏数(): Promise<void> {
  await router.push({ name: 'student-favorites' })
}

async function jumpToResourceArchive(): Promise<void> {
  await router.push({ name: 'student-resources' })
}

function openContent(): void {
  if (resourceDetail.value?.contentUrl) {
    window.open(resourceDetail.value.contentUrl, '_blank', 'noopener,noreferrer')
  }
}

watch(
  () => route.params.resourceId,
  () => {
    void loadResourceDetail()
  }
)

onMounted(() => {
  void loadResourceDetail()
})
</script>

<template>
  <main class="resource-detail-page">
    <section class="resource-detail-page__masthead">
      <div class="resource-detail-page__heading">
        <p class="resource-detail-page__eyebrow">资源详情</p>
        <h1 class="resource-detail-page__title">资源详情</h1>
        <p class="resource-detail-page__summary">
          在这里查看单条资源的内容说明、标签与收藏状态，并决定是否把它纳入你的长期支持清单。
        </p>
      </div>

      <aside class="resource-detail-page__snapshot" v-if="resourceDetail">
        <p class="resource-detail-page__label">Archive Entry</p>
        <dl>
          <div>
            <dt>Type</dt>
            <dd>{{ resolveResourceType(resourceDetail.resourceType) }}</dd>
          </div>
          <div>
            <dt>Category</dt>
            <dd>{{ resourceDetail.categoryName }}</dd>
          </div>
          <div>
            <dt>发布时间</dt>
            <dd>{{ formatDate(resourceDetail.publishedAt) }}</dd>
          </div>
          <div>
            <dt>收藏状态</dt>
            <dd>{{ resourceDetail.favorite ? '已收藏' : '未收藏' }}</dd>
          </div>
        </dl>
      </aside>
    </section>

    <p v-if="errorMessage" class="resource-detail-page__alert">{{ errorMessage }}</p>

    <section v-if="loading" class="resource-detail-page__status-panel">
      <p>正在加载资源详情...</p>
    </section>

    <template v-else-if="resourceDetail">
      <section class="resource-detail-page__hero">
        <div class="resource-detail-page__hero-copy">
          <p class="resource-detail-page__label">{{ resolveResourceType(resourceDetail.resourceType) }}</p>
          <h2>{{ resourceDetail.title }}</h2>
          <p>{{ resourceDetail.summaryText }}</p>
        </div>

        <div class="resource-detail-page__hero-meta">
          <div>
            <span>Views</span>
            <strong>{{ resourceDetail.viewCount }}</strong>
          </div>
          <div>
            <span>收藏数</span>
            <strong>{{ resourceDetail.favoriteCount }}</strong>
          </div>
          <div>
            <span>Updated</span>
            <strong>{{ formatDate(resourceDetail.updatedAt) }}</strong>
          </div>
        </div>
      </section>

      <section class="resource-detail-page__body-grid">
        <article class="resource-detail-page__panel">
          <p class="resource-detail-page__label">Reading Notes</p>
          <h3>内容说明</h3>
          <p>{{ resourceDetail.summaryText }}</p>
          <div class="resource-detail-page__tag-row">
            <span class="resource-detail-page__tag">{{ resourceDetail.categoryName }}</span>
            <span v-for="tag in resourceDetail.tags" :key="tag.tagId" class="resource-detail-page__tag">{{ tag.name }}</span>
          </div>
        </article>

        <article class="resource-detail-page__panel">
          <p class="resource-detail-page__label">Actions</p>
          <h3>收藏与跳转</h3>
          <p>你可以把这条资源加入收藏，或直接跳转到原始内容地址继续阅读、观看或收听。</p>
          <div class="resource-detail-page__actions">
            <button class="resource-detail-page__primary" type="button" :disabled="togglingFavorite" @click="toggleFavorite">
              {{ favoriteButtonText }}
            </button>
            <button class="resource-detail-page__ghost" type="button" @click="openContent">打开原始内容</button>
            <button class="resource-detail-page__ghost" type="button" @click="jumpTo收藏数">查看我的收藏</button>
          </div>
        </article>
      </section>

      <section class="resource-detail-page__footer-actions">
        <button class="resource-detail-page__ghost" type="button" @click="jumpToResourceArchive">返回资源目录</button>
        <button class="resource-detail-page__ghost" type="button" @click="jumpTo收藏数">前往收藏列表</button>
      </section>
    </template>

    <section v-else class="resource-detail-page__status-panel">
      <p>未找到对应资源，请返回资源目录重新选择。</p>
      <button class="resource-detail-page__ghost" type="button" @click="jumpToResourceArchive">返回资源目录</button>
    </section>
  </main>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700&family=Noto+Serif+SC:wght@400;500;600;700&display=swap');

.resource-detail-page {
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

.resource-detail-page__masthead {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(300px, 0.85fr);
  gap: 1.5rem;
  align-items: end;
  padding-bottom: 1.4rem;
  border-bottom: 1px solid var(--line);
}

.resource-detail-page__eyebrow,
.resource-detail-page__label,
.resource-detail-page__snapshot dt,
.resource-detail-page__hero-meta span,
.resource-detail-page__tag {
  margin: 0;
  font: 600 0.72rem/1.4 'Manrope', sans-serif;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--muted);
}

.resource-detail-page__title {
  margin: 0.95rem 0 0;
  font: 600 clamp(2.8rem, 5vw, 5.1rem)/0.98 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.resource-detail-page__summary {
  max-width: 46rem;
  margin: 1rem 0 0;
  color: var(--muted);
  font: 400 1rem/1.9 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.resource-detail-page__snapshot,
.resource-detail-page__hero,
.resource-detail-page__panel,
.resource-detail-page__status-panel,
.resource-detail-page__footer-actions {
  border: 1px solid var(--line);
  background: var(--glass);
  backdrop-filter: blur(18px);
  box-shadow: 0 22px 48px rgba(80, 70, 58, 0.08);
}

.resource-detail-page__snapshot {
  padding: 1.2rem;
}

.resource-detail-page__snapshot dl {
  display: grid;
  gap: 0.9rem;
  margin: 1rem 0 0;
}

.resource-detail-page__snapshot dd {
  margin: 0.35rem 0 0;
  font: 600 1.04rem/1.45 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.resource-detail-page__alert {
  margin: 1.25rem 0 0;
  color: #8d4747;
  font: 600 0.9rem/1.6 'Manrope', sans-serif;
}

.resource-detail-page__hero {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(240px, 0.7fr);
  gap: 1.3rem;
  margin-top: 1.5rem;
  padding: 1.4rem;
}

.resource-detail-page__hero-copy h2,
.resource-detail-page__panel h3 {
  margin: 0.7rem 0 0;
  font: 600 1.95rem/1.24 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.resource-detail-page__hero-copy p:last-child,
.resource-detail-page__panel p:last-of-type {
  margin: 0.9rem 0 0;
  color: var(--muted);
  font: 400 0.98rem/1.9 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.resource-detail-page__hero-meta {
  display: grid;
  gap: 0.9rem;
}

.resource-detail-page__hero-meta strong {
  display: block;
  margin-top: 0.35rem;
  font: 600 1.05rem/1.45 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.resource-detail-page__body-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
  margin-top: 1.5rem;
}

.resource-detail-page__panel {
  display: grid;
  gap: 0.95rem;
  padding: 1.35rem;
}

.resource-detail-page__tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
}

.resource-detail-page__tag {
  padding: 0.45rem 0.7rem;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.42);
}

.resource-detail-page__actions,
.resource-detail-page__footer-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.8rem;
}

.resource-detail-page__primary,
.resource-detail-page__ghost {
  min-height: 3rem;
  padding: 0 1.15rem;
  font: 600 0.84rem/1 'Manrope', sans-serif;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  cursor: pointer;
  transition: transform 180ms ease, border-color 180ms ease, box-shadow 180ms ease;
}

.resource-detail-page__primary {
  border: none;
  background: linear-gradient(135deg, #6b8473, #4f6656);
  color: #faf6f0;
  box-shadow: 0 18px 36px rgba(79, 102, 86, 0.24);
}

.resource-detail-page__ghost {
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.48);
  color: var(--ink);
}

.resource-detail-page__primary:hover,
.resource-detail-page__ghost:hover {
  transform: translateY(-2px);
}

.resource-detail-page__status-panel,
.resource-detail-page__footer-actions {
  margin-top: 1.5rem;
  padding: 1.35rem;
}

.resource-detail-page__status-panel p {
  margin: 0;
  color: var(--muted);
  font: 400 0.98rem/1.9 'Noto Serif SC', 'Source Han Serif SC', serif;
}

@media (max-width: 980px) {
  .resource-detail-page,
  .resource-list-page {
    padding: 1rem;
  }

  .resource-detail-page__masthead,
  .resource-detail-page__hero,
  .resource-detail-page__body-grid {
    grid-template-columns: 1fr;
  }

  .resource-detail-page__actions,
  .resource-detail-page__footer-actions {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>

