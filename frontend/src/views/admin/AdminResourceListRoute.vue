<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { fetchAdminResourcesApi } from '@/api/admin-resource'
import type { AdminResourceListItem, AdminResourceQuery } from '@/api/types'
import { toErrorMessage } from '@/views/shared/page-logic'

const router = useRouter()
const loading = ref(false)
const errorMessage = ref('')
const resources = ref<AdminResourceListItem[]>([])
const currentPage = ref(1)
const pageSize = 10
const filters = reactive<AdminResourceQuery>({
  status: undefined,
  keyword: ''
})

const totalPages = computed(() => Math.max(1, Math.ceil(resources.value.length / pageSize)))
const pagedResources = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return resources.value.slice(start, start + pageSize)
})

function resolveStatusText(status?: string): string {
  if (status === 'PUBLISHED') return '已发布'
  if (status === 'OFFLINE') return '已下线'
  if (status === 'DRAFT') return '草稿'
  return status || '未标记'
}

async function loadResources(): Promise<void> {
  loading.value = true
  errorMessage.value = ''

  try {
    resources.value = await fetchAdminResourcesApi(filters)
    currentPage.value = 1
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    loading.value = false
  }
}

async function openResourceDetail(resourceId?: number): Promise<void> {
  if (resourceId) {
    await router.push({ name: 'admin-resource-detail', params: { resourceId } })
    return
  }

  await router.push({ name: 'admin-resource-new' })
}

async function openMetaCenter(): Promise<void> {
  await router.push({ name: 'admin-resource-meta' })
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

onMounted(() => {
  void loadResources()
})
</script>

<template>
  <section class="admin-editorial-page">
    <div class="admin-editorial-shell">
      <header class="admin-editorial-hero">
        <div class="admin-editorial-copy">
          <p class="admin-editorial-eyebrow">资源治理</p>
          <h1 class="admin-editorial-title">统一查看资源状态、分类归属与热度变化，再进入单条资源页细化维护。</h1>
          <p class="admin-editorial-lead">保留原有查询与跳转逻辑，只重构可读性、层级和留白，让管理员端与学生端维持一致的阅读气质。</p>
        </div>
        <div class="admin-editorial-hero-side">
          <article class="admin-editorial-stat">
            <p class="admin-editorial-label">当前条目</p>
            <strong>{{ resources.length }}</strong>
            <p class="admin-editorial-lead">显示符合筛选条件的资源总量。</p>
          </article>
          <div class="admin-editorial-actions">
            <button class="admin-editorial-ghost" type="button" @click="openMetaCenter">分类标签中心</button>
            <button class="admin-editorial-button" type="button" @click="openResourceDetail()">新增资源</button>
          </div>
        </div>
      </header>

      <p v-if="errorMessage" class="admin-editorial-alert">{{ errorMessage }}</p>

      <section class="admin-editorial-panel">
        <div class="admin-editorial-section">
          <p class="admin-editorial-kicker">筛选条件</p>
          <h2>按状态或关键词聚焦资源集合</h2>
        </div>

        <div class="admin-editorial-form">
          <label class="admin-editorial-field">
            <span>状态</span>
            <input v-model="filters.status" type="text" placeholder="PUBLISHED / DRAFT / OFFLINE">
          </label>
          <label class="admin-editorial-field wide">
            <span>关键词</span>
            <input v-model="filters.keyword" type="text" placeholder="标题或摘要关键词">
          </label>
        </div>

        <div class="admin-editorial-actions" style="margin-top: 1rem;">
          <button class="admin-editorial-ghost" type="button" @click="loadResources">刷新资源列表</button>
        </div>

        <div v-if="loading" class="admin-editorial-empty">正在同步资源列表…</div>
        <div v-else class="admin-editorial-board" style="margin-top: 1rem;">
          <article
            v-for="resource in pagedResources"
            :key="resource.resourceId"
            class="admin-editorial-card"
            style="cursor: pointer;"
            @click="openResourceDetail(resource.resourceId)"
          >
            <div class="admin-editorial-card__topline">
              <div>
                <p class="admin-editorial-code">资源 #{{ resource.resourceId }}</p>
                <h3>{{ resource.title }}</h3>
              </div>
              <span class="admin-editorial-status">{{ resolveStatusText(resource.status) }}</span>
            </div>
            <p>{{ resource.summaryText }}</p>
            <div class="admin-editorial-meta">
              <span>{{ resource.categoryName }}</span>
              <span>{{ resource.resourceType }}</span>
              <span>浏览 {{ resource.viewCount }}</span>
              <span>收藏 {{ resource.favoriteCount }}</span>
            </div>
          </article>
        </div>
        <nav class="pagination-nav" v-if="totalPages > 1">
          <button class="page-btn" :disabled="currentPage <= 1" @click="prevPage">
            <span class="arrow">←</span> 往前翻
          </button>
          <div class="page-indicator">
            <span>{{ currentPage }}</span> / <span>{{ totalPages }}</span>
          </div>
          <button class="page-btn" :disabled="currentPage >= totalPages" @click="nextPage">
            往后翻 <span class="arrow">→</span>
          </button>
        </nav>
      </section>
    </div>
  </section>
</template>

<style scoped>
@import './admin-editorial.css';

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

.arrow {
  font-family: 'Manrope', sans-serif;
  transition: transform 0.3s ease;
}

.page-btn:hover:not(:disabled) .arrow:last-child {
  transform: translateX(4px);
}

.page-btn:hover:not(:disabled) .arrow:first-child {
  transform: translateX(-4px);
}
</style>
