<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { createAdminResourceCategoryApi, createAdminResourceTagApi, fetchAdminResourceCategoriesApi, fetchAdminResourceTagsApi, updateAdminResourceCategoryApi } from '@/api/admin-resource'
import type { AdminResourceCategory, AdminResourceTag, CreateOrUpdateResourceCategoryRequest, CreateResourceTagRequest } from '@/api/types'
import { toErrorMessage } from '@/views/shared/page-logic'

const loading = ref(false)
const processing = ref(false)
const errorMessage = ref('')
const categories = ref<AdminResourceCategory[]>([])
const tags = ref<AdminResourceTag[]>([])
const currentCategoryPage = ref(1)
const currentTagPage = ref(1)
const categoryPageSize = 8
const tagPageSize = 10
const categoryForm = reactive<CreateOrUpdateResourceCategoryRequest>({
  name: '',
  description: '',
  sortNo: 0,
  status: 'ACTIVE'
})
const tagForm = reactive<CreateResourceTagRequest>({
  name: '',
  description: ''
})

const totalCategoryPages = computed(() => Math.max(1, Math.ceil(categories.value.length / categoryPageSize)))
const pagedCategories = computed(() => {
  const start = (currentCategoryPage.value - 1) * categoryPageSize
  return categories.value.slice(start, start + categoryPageSize)
})

const totalTagPages = computed(() => Math.max(1, Math.ceil(tags.value.length / tagPageSize)))
const pagedTags = computed(() => {
  const start = (currentTagPage.value - 1) * tagPageSize
  return tags.value.slice(start, start + tagPageSize)
})

async function loadMeta(): Promise<void> {
  loading.value = true
  errorMessage.value = ''

  try {
    const [categoryList, tagList] = await Promise.all([
      fetchAdminResourceCategoriesApi(),
      fetchAdminResourceTagsApi()
    ])
    categories.value = categoryList
    tags.value = tagList
    currentCategoryPage.value = 1
    currentTagPage.value = 1
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    loading.value = false
  }
}

async function createCategory(): Promise<void> {
  processing.value = true
  errorMessage.value = ''

  try {
    await createAdminResourceCategoryApi(categoryForm)
    await loadMeta()
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    processing.value = false
  }
}

async function updateCategory(categoryId: number): Promise<void> {
  processing.value = true
  errorMessage.value = ''

  try {
    await updateAdminResourceCategoryApi(categoryId, categoryForm)
    await loadMeta()
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    processing.value = false
  }
}

async function createTag(): Promise<void> {
  processing.value = true
  errorMessage.value = ''

  try {
    await createAdminResourceTagApi(tagForm)
    await loadMeta()
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    processing.value = false
  }
}

function prevCategoryPage(): void {
  if (currentCategoryPage.value > 1) {
    currentCategoryPage.value--
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
}

function nextCategoryPage(): void {
  if (currentCategoryPage.value < totalCategoryPages.value) {
    currentCategoryPage.value++
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
}

function prevTagPage(): void {
  if (currentTagPage.value > 1) {
    currentTagPage.value--
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
}

function nextTagPage(): void {
  if (currentTagPage.value < totalTagPages.value) {
    currentTagPage.value++
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
}

onMounted(() => {
  void loadMeta()
})
</script>

<template>
  <section class="admin-editorial-page">
    <div class="admin-editorial-shell">
      <header class="admin-editorial-hero">
        <div class="admin-editorial-copy">
          <p class="admin-editorial-eyebrow">资源元数据中心</p>
          <h1 class="admin-editorial-title">统一维护分类与标签，让资源治理口径保持稳定一致。</h1>
          <p class="admin-editorial-lead">所有创建与更新动作仍然调用原有元数据接口，这里只把信息编排和卡片结构统一为学生端同源的视觉语言。</p>
        </div>
        <div class="admin-editorial-hero-side">
          <article class="admin-editorial-stat">
            <p class="admin-editorial-label">分类数量</p>
            <strong>{{ categories.length }}</strong>
          </article>
          <article class="admin-editorial-stat">
            <p class="admin-editorial-label">标签数量</p>
            <strong>{{ tags.length }}</strong>
          </article>
        </div>
      </header>

      <p v-if="errorMessage" class="admin-editorial-alert">{{ errorMessage }}</p>

      <div class="admin-editorial-grid admin-editorial-grid--equal">
        <section class="admin-editorial-panel admin-editorial-panel--mesh">
          <div class="admin-editorial-section">
            <p class="admin-editorial-kicker">分类管理</p>
            <h2>新增或更新分类</h2>
          </div>

          <div class="admin-editorial-form">
            <label class="admin-editorial-field">
              <span>名称</span>
              <input v-model="categoryForm.name" type="text">
            </label>
            <label class="admin-editorial-field">
              <span>排序</span>
              <input v-model.number="categoryForm.sortNo" type="number">
            </label>
            <label class="admin-editorial-field">
              <span>状态</span>
              <input v-model="categoryForm.status" type="text">
            </label>
            <label class="admin-editorial-field wide">
              <span>描述</span>
              <input v-model="categoryForm.description" type="text">
            </label>
          </div>

          <div class="admin-editorial-actions" style="margin-top: 1rem;">
            <button class="admin-editorial-button" type="button" :disabled="processing" @click="createCategory">新增分类</button>
          </div>

          <div class="admin-editorial-board" style="margin-top: 1rem;">
            <article v-for="category in pagedCategories" :key="category.categoryId" class="admin-editorial-card">
              <div class="admin-editorial-card__topline">
                <div>
                  <p class="admin-editorial-code">分类 #{{ category.categoryId }}</p>
                  <h3>{{ category.name }}</h3>
                </div>
                <span class="admin-editorial-status">{{ category.status || 'ACTIVE' }}</span>
              </div>
              <p>{{ category.description || '无描述' }}</p>
              <div class="admin-editorial-card__footer">
                <span class="admin-editorial-note">排序 {{ category.sortNo }}</span>
                <button class="admin-editorial-ghost" type="button" :disabled="processing" @click="updateCategory(category.categoryId)">用当前表单更新</button>
              </div>
            </article>
          </div>
          <nav class="pagination-nav" v-if="totalCategoryPages > 1">
            <button class="page-btn" :disabled="currentCategoryPage <= 1" @click="prevCategoryPage">
              <span class="arrow">←</span> 往前翻
            </button>
            <div class="page-indicator">
              <span>{{ currentCategoryPage }}</span> / <span>{{ totalCategoryPages }}</span>
            </div>
            <button class="page-btn" :disabled="currentCategoryPage >= totalCategoryPages" @click="nextCategoryPage">
              往后翻 <span class="arrow">→</span>
            </button>
          </nav>
        </section>

        <section class="admin-editorial-panel">
          <div class="admin-editorial-section">
            <p class="admin-editorial-kicker">标签管理</p>
            <h2>新增标签并查看现有体系</h2>
          </div>

          <div class="admin-editorial-form">
            <label class="admin-editorial-field">
              <span>名称</span>
              <input v-model="tagForm.name" type="text">
            </label>
            <label class="admin-editorial-field wide">
              <span>描述</span>
              <input v-model="tagForm.description" type="text">
            </label>
          </div>

          <div class="admin-editorial-actions" style="margin-top: 1rem;">
            <button class="admin-editorial-button" type="button" :disabled="processing" @click="createTag">新增标签</button>
          </div>

          <div v-if="loading" class="admin-editorial-empty">正在同步分类与标签…</div>
          <div v-else class="admin-editorial-board" style="margin-top: 1rem;">
            <article v-for="tag in pagedTags" :key="tag.tagId" class="admin-editorial-card">
              <p class="admin-editorial-code">标签 #{{ tag.tagId }}</p>
              <h3>{{ tag.name }}</h3>
              <p>{{ tag.description || '无描述' }}</p>
            </article>
          </div>
          <nav class="pagination-nav" v-if="totalTagPages > 1">
            <button class="page-btn" :disabled="currentTagPage <= 1" @click="prevTagPage">
              <span class="arrow">←</span> 往前翻
            </button>
            <div class="page-indicator">
              <span>{{ currentTagPage }}</span> / <span>{{ totalTagPages }}</span>
            </div>
            <button class="page-btn" :disabled="currentTagPage >= totalTagPages" @click="nextTagPage">
              往后翻 <span class="arrow">→</span>
            </button>
          </nav>
        </section>
      </div>
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
