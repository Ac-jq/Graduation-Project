<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
  createAdminResourceCategoryApi,
  createAdminResourceTagApi,
  deleteAdminResourceCategoryApi,
  deleteAdminResourceTagApi,
  fetchAdminResourceCategoriesApi,
  fetchAdminResourceTagsApi,
  updateAdminResourceCategoryApi,
  updateAdminResourceTagApi
} from '@/api/admin-resource'
import type {
  AdminResourceCategory,
  AdminResourceTag,
  CreateOrUpdateResourceCategoryRequest,
  CreateOrUpdateResourceTagRequest,
  CreateResourceTagRequest
} from '@/api/types'
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

const categoryEditorVisible = ref(false)
const tagEditorVisible = ref(false)
const deleteDialogVisible = ref(false)
const deleteTargetType = ref<'category' | 'tag'>('category')
const deleteTargetId = ref<number | null>(null)
const deleteTargetName = ref('')
const editingCategoryId = ref<number | null>(null)
const editingTagId = ref<number | null>(null)

const categoryEditForm = reactive<CreateOrUpdateResourceCategoryRequest>({
  name: '',
  description: '',
  sortNo: 0,
  status: 'ACTIVE'
})

const tagEditForm = reactive<CreateOrUpdateResourceTagRequest>({
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
    categoryForm.name = ''
    categoryForm.description = ''
    categoryForm.sortNo = 0
    categoryForm.status = 'ACTIVE'
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
    tagForm.name = ''
    tagForm.description = ''
    await loadMeta()
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    processing.value = false
  }
}

function openCategoryEditor(category: AdminResourceCategory): void {
  editingCategoryId.value = category.categoryId
  categoryEditForm.name = category.name
  categoryEditForm.description = category.description ?? ''
  categoryEditForm.sortNo = category.sortNo ?? 0
  categoryEditForm.status = category.status ?? 'ACTIVE'
  categoryEditorVisible.value = true
}

function openTagEditor(tag: AdminResourceTag): void {
  editingTagId.value = tag.tagId
  tagEditForm.name = tag.name
  tagEditForm.description = tag.description ?? ''
  tagEditorVisible.value = true
}

function openDeleteDialog(type: 'category' | 'tag', id: number, name: string): void {
  deleteTargetType.value = type
  deleteTargetId.value = id
  deleteTargetName.value = name
  deleteDialogVisible.value = true
}

async function confirmCategoryEdit(): Promise<void> {
  if (!editingCategoryId.value) {
    return
  }
  processing.value = true
  errorMessage.value = ''
  try {
    await updateAdminResourceCategoryApi(editingCategoryId.value, categoryEditForm)
    categoryEditorVisible.value = false
    await loadMeta()
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    processing.value = false
  }
}

async function confirmTagEdit(): Promise<void> {
  if (!editingTagId.value) {
    return
  }
  processing.value = true
  errorMessage.value = ''
  try {
    await updateAdminResourceTagApi(editingTagId.value, tagEditForm)
    tagEditorVisible.value = false
    await loadMeta()
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    processing.value = false
  }
}

async function confirmDelete(): Promise<void> {
  if (!deleteTargetId.value) {
    return
  }
  processing.value = true
  errorMessage.value = ''
  try {
    if (deleteTargetType.value === 'category') {
      await deleteAdminResourceCategoryApi(deleteTargetId.value)
    } else {
      await deleteAdminResourceTagApi(deleteTargetId.value)
    }
    deleteDialogVisible.value = false
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
  }
}

function nextCategoryPage(): void {
  if (currentCategoryPage.value < totalCategoryPages.value) {
    currentCategoryPage.value++
  }
}

function prevTagPage(): void {
  if (currentTagPage.value > 1) {
    currentTagPage.value--
  }
}

function nextTagPage(): void {
  if (currentTagPage.value < totalTagPages.value) {
    currentTagPage.value++
  }
}

onMounted(() => {
  void loadMeta()
})
</script>

<template>
  <main class="meta-dashboard">
    <div class="dashboard-container">
      <header class="glass-header">
        <div class="header-info">
          <div class="title-wrap">
            <div class="icon-box">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M4 6h16M4 12h16M4 18h7" />
              </svg>
            </div>
            <h1>资源元数据中心</h1>
          </div>
          <p class="subtitle">统一维护资源分类与标签字典，保证资源检索与归档结构一致。</p>
        </div>

        <div class="stats-group">
          <div class="stat-card">
            <span class="stat-icon category-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
                <rect x="3" y="3" width="7" height="7" />
                <rect x="14" y="3" width="7" height="7" />
                <rect x="14" y="14" width="7" height="7" />
                <rect x="3" y="14" width="7" height="7" />
              </svg>
            </span>
            <div class="stat-data">
              <span class="stat-value">{{ categories.length }}</span>
              <span class="stat-label">全部分类</span>
            </div>
          </div>
          <div class="divider"></div>
          <div class="stat-card">
            <span class="stat-icon tag-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
                <path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z" />
                <line x1="7" y1="7" x2="7.01" y2="7" />
              </svg>
            </span>
            <div class="stat-data">
              <span class="stat-value">{{ tags.length }}</span>
              <span class="stat-label">全部标签</span>
            </div>
          </div>
        </div>
      </header>

      <transition name="fade">
        <div v-if="errorMessage" class="error-toast">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
            <circle cx="12" cy="12" r="10" />
            <line x1="12" y1="8" x2="12" y2="12" />
            <line x1="12" y1="16" x2="12.01" y2="16" />
          </svg>
          {{ errorMessage }}
        </div>
      </transition>

      <div class="dashboard-grid">
        <section class="module-panel">
          <div class="panel-header">
            <h2>分类管理</h2>
            <span class="badge">Categories</span>
          </div>

          <div class="control-console">
            <div class="input-row">
              <div class="input-group">
                <label>分类名称</label>
                <input v-model="categoryForm.name" type="text" placeholder="如：心理科普" />
              </div>
              <div class="input-group">
                <label>排序值</label>
                <input v-model.number="categoryForm.sortNo" type="number" placeholder="数值越小越靠前" />
              </div>
              <div class="input-group">
                <label>状态</label>
                <div class="select-wrapper">
                  <select v-model="categoryForm.status">
                    <option value="ACTIVE">启用</option>
                    <option value="DISABLED">停用</option>
                  </select>
                </div>
              </div>
            </div>
            <div class="input-group full-width">
              <label>分类描述</label>
              <div class="action-input">
                <input v-model="categoryForm.description" type="text" placeholder="一句话描述该分类的用途" />
                <button class="btn-primary" type="button" :disabled="processing" @click="createCategory">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
                    <line x1="12" y1="5" x2="12" y2="19" />
                    <line x1="5" y1="12" x2="19" y2="12" />
                  </svg>
                  新增分类
                </button>
              </div>
            </div>
          </div>

          <div class="data-list-container">
            <transition-group name="list" tag="div" class="data-list">
              <div v-for="category in pagedCategories" :key="category.categoryId" class="data-row">
                <div class="row-main">
                  <div class="row-title">
                    <span class="id-hash">#{{ category.categoryId }}</span>
                    <strong class="name">{{ category.name }}</strong>
                    <span class="status-dot" :class="category.status === 'ACTIVE' ? 'active' : 'disabled'"></span>
                  </div>
                  <p class="desc">{{ category.description || '暂无描述信息' }}</p>
                </div>
                <div class="row-actions">
                  <div class="sort-badge">Sort: {{ category.sortNo }}</div>
                  <button class="btn-icon" type="button" :disabled="processing" @click="openCategoryEditor(category)">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
                      <path d="M12 20h9" />
                      <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z" />
                    </svg>
                    编辑
                  </button>
                  <button class="btn-icon btn-icon--danger" type="button" :disabled="processing" @click="openDeleteDialog('category', category.categoryId, category.name)">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
                      <path d="M3 6h18" />
                      <path d="M8 6V4h8v2" />
                      <path d="M19 6l-1 14H6L5 6" />
                      <path d="M10 11v6" />
                      <path d="M14 11v6" />
                    </svg>
                    删除
                  </button>
                </div>
              </div>
            </transition-group>
          </div>

          <div class="pagination" v-if="totalCategoryPages > 1">
            <button class="page-nav" :disabled="currentCategoryPage <= 1" @click="prevCategoryPage">上一页</button>
            <div class="page-dots">
              <span class="current">{{ currentCategoryPage }}</span> / <span class="total">{{ totalCategoryPages }}</span>
            </div>
            <button class="page-nav" :disabled="currentCategoryPage >= totalCategoryPages" @click="nextCategoryPage">下一页</button>
          </div>
        </section>

        <section class="module-panel">
          <div class="panel-header">
            <h2>标签管理</h2>
            <span class="badge">Tags</span>
          </div>

          <div class="control-console">
            <div class="input-group full-width">
              <label>标签名称</label>
              <input v-model="tagForm.name" type="text" placeholder="如：焦虑缓解" />
            </div>
            <div class="input-group full-width">
              <label>标签描述</label>
              <div class="action-input">
                <input v-model="tagForm.description" type="text" placeholder="描述该标签的具体含义" />
                <button class="btn-primary" type="button" :disabled="processing" @click="createTag">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
                    <line x1="12" y1="5" x2="12" y2="19" />
                    <line x1="5" y1="12" x2="19" y2="12" />
                  </svg>
                  新增标签
                </button>
              </div>
            </div>
          </div>

          <div class="data-list-container">
            <div v-if="loading" class="loading-placeholder">正在同步元数据...</div>
            <transition-group v-else name="list" tag="div" class="data-list">
              <div v-for="tag in pagedTags" :key="tag.tagId" class="data-row">
                <div class="row-main">
                  <div class="row-title">
                    <span class="id-hash">#{{ tag.tagId }}</span>
                    <strong class="name">{{ tag.name }}</strong>
                  </div>
                  <p class="desc">{{ tag.description || '暂无描述信息' }}</p>
                </div>
                <div class="row-actions">
                  <button class="btn-icon" type="button" :disabled="processing" @click="openTagEditor(tag)">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
                      <path d="M12 20h9" />
                      <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z" />
                    </svg>
                    编辑
                  </button>
                  <button class="btn-icon btn-icon--danger" type="button" :disabled="processing" @click="openDeleteDialog('tag', tag.tagId, tag.name)">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
                      <path d="M3 6h18" />
                      <path d="M8 6V4h8v2" />
                      <path d="M19 6l-1 14H6L5 6" />
                      <path d="M10 11v6" />
                      <path d="M14 11v6" />
                    </svg>
                    删除
                  </button>
                </div>
              </div>
            </transition-group>
          </div>

          <div class="pagination" v-if="totalTagPages > 1">
            <button class="page-nav" :disabled="currentTagPage <= 1" @click="prevTagPage">上一页</button>
            <div class="page-dots">
              <span class="current">{{ currentTagPage }}</span> / <span class="total">{{ totalTagPages }}</span>
            </div>
            <button class="page-nav" :disabled="currentTagPage >= totalTagPages" @click="nextTagPage">下一页</button>
          </div>
        </section>
      </div>
    </div>

    <div v-if="categoryEditorVisible" class="modal-mask" @click.self="categoryEditorVisible = false">
      <div class="modal-card">
        <div class="modal-header">
          <h3>编辑分类</h3>
          <button class="modal-close" type="button" @click="categoryEditorVisible = false">×</button>
        </div>
        <div class="modal-body">
          <div class="input-group">
            <label>分类名称</label>
            <input v-model="categoryEditForm.name" type="text" />
          </div>
          <div class="input-row">
            <div class="input-group">
              <label>排序值</label>
              <input v-model.number="categoryEditForm.sortNo" type="number" />
            </div>
            <div class="input-group">
              <label>状态</label>
              <div class="select-wrapper">
                <select v-model="categoryEditForm.status">
                  <option value="ACTIVE">启用</option>
                  <option value="DISABLED">停用</option>
                </select>
              </div>
            </div>
          </div>
          <div class="input-group">
            <label>分类描述</label>
            <input v-model="categoryEditForm.description" type="text" />
          </div>
        </div>
        <div class="modal-actions">
          <button class="page-nav" type="button" @click="categoryEditorVisible = false">取消</button>
          <button class="btn-primary" type="button" :disabled="processing" @click="confirmCategoryEdit">确认修改</button>
        </div>
      </div>
    </div>

    <div v-if="tagEditorVisible" class="modal-mask" @click.self="tagEditorVisible = false">
      <div class="modal-card">
        <div class="modal-header">
          <h3>编辑标签</h3>
          <button class="modal-close" type="button" @click="tagEditorVisible = false">×</button>
        </div>
        <div class="modal-body">
          <div class="input-group">
            <label>标签名称</label>
            <input v-model="tagEditForm.name" type="text" />
          </div>
          <div class="input-group">
            <label>标签描述</label>
            <input v-model="tagEditForm.description" type="text" />
          </div>
        </div>
        <div class="modal-actions">
          <button class="page-nav" type="button" @click="tagEditorVisible = false">取消</button>
          <button class="btn-primary" type="button" :disabled="processing" @click="confirmTagEdit">确认修改</button>
        </div>
      </div>
    </div>

    <div v-if="deleteDialogVisible" class="modal-mask" @click.self="deleteDialogVisible = false">
      <div class="modal-card modal-card--confirm">
        <div class="modal-header">
          <h3>确认删除</h3>
          <button class="modal-close" type="button" @click="deleteDialogVisible = false">×</button>
        </div>
        <div class="modal-body">
          <p class="confirm-text">确认删除“{{ deleteTargetName }}”吗？</p>
        </div>
        <div class="modal-actions">
          <button class="page-nav" type="button" @click="deleteDialogVisible = false">取消</button>
          <button class="btn-primary btn-primary--danger" type="button" :disabled="processing" @click="confirmDelete">确认删除</button>
        </div>
      </div>
    </div>
  </main>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=Noto+Sans+SC:wght@400;500;700&display=swap');

.meta-dashboard {
  --bg-body: #f4f7f9;
  --bg-card: #ffffff;
  --text-main: #1e293b;
  --text-sub: #64748b;
  --primary: #0ea5e9;
  --primary-hover: #0284c7;
  --border-light: #e2e8f0;
  --border-focus: #bae6fd;
  --success: #10b981;
  --danger: #ef4444;

  min-height: 100vh;
  background-color: var(--bg-body);
  font-family: 'Inter', 'Noto Sans SC', sans-serif;
  color: var(--text-main);
  padding: 2rem;
  box-sizing: border-box;
}

.dashboard-container {
  max-width: 1440px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.glass-header {
  background: var(--bg-card);
  border-radius: 20px;
  padding: 2rem 2.5rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.8);
}

.header-info .title-wrap {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.icon-box {
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, #e0f2fe, #bae6fd);
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--primary);
}

.icon-box svg {
  width: 24px;
  height: 24px;
}

.title-wrap h1 {
  margin: 0;
  font-size: 1.75rem;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.subtitle {
  margin: 0.5rem 0 0 4rem;
  color: var(--text-sub);
  font-size: 0.95rem;
}

.stats-group {
  display: flex;
  align-items: center;
  gap: 2.5rem;
  background: #f8fafc;
  padding: 1.25rem 2rem;
  border-radius: 16px;
  border: 1px solid var(--border-light);
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.stat-icon {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-icon svg { width: 20px; height: 20px; }

.category-icon { background: #dcfce7; color: #16a34a; }
.tag-icon { background: #f3e8ff; color: #9333ea; }

.stat-data {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 1.5rem;
  font-weight: 700;
  line-height: 1;
}

.stat-label {
  font-size: 0.8rem;
  color: var(--text-sub);
  margin-top: 0.25rem;
  font-weight: 500;
}

.divider {
  width: 1px;
  height: 40px;
  background: var(--border-light);
}

.error-toast {
  background: #fef2f2;
  border: 1px solid #fecaca;
  color: var(--danger);
  padding: 1rem 1.5rem;
  border-radius: 12px;
  display: flex;
  align-items: center;
  gap: 0.75rem;
  font-weight: 500;
  box-shadow: 0 4px 12px rgba(239, 68, 68, 0.1);
}

.error-toast svg { width: 20px; height: 20px; }

.dashboard-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 2rem;
  align-items: start;
}

.module-panel {
  background: var(--bg-card);
  border-radius: 20px;
  padding: 2rem;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.02);
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.panel-header {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.panel-header h2 {
  margin: 0;
  font-size: 1.25rem;
  font-weight: 700;
}

.badge {
  background: #f1f5f9;
  color: var(--text-sub);
  padding: 0.25rem 0.75rem;
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 700;
  letter-spacing: 0.05em;
  text-transform: uppercase;
}

.control-console {
  background: #f8fafc;
  border: 1px solid var(--border-light);
  border-radius: 16px;
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.input-row {
  display: grid;
  grid-template-columns: 2fr 1fr 1fr;
  gap: 1rem;
}

.input-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.input-group label {
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--text-sub);
}

.input-group input,
.input-group select {
  width: 100%;
  height: 42px;
  padding: 0 1rem;
  background: #ffffff;
  border: 1px solid var(--border-light);
  border-radius: 10px;
  font-family: inherit;
  font-size: 0.9rem;
  color: var(--text-main);
  box-sizing: border-box;
  transition: all 0.2s ease;
  outline: none;
}

.input-group input:focus,
.input-group select:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px var(--border-focus);
}

.select-wrapper {
  position: relative;
}

.select-wrapper::after {
  content: "▾";
  font-size: 0.6rem;
  color: var(--text-sub);
  position: absolute;
  right: 1rem;
  top: 50%;
  transform: translateY(-50%);
  pointer-events: none;
}

.select-wrapper select {
  appearance: none;
  padding-right: 2rem;
}

.action-input {
  display: flex;
  gap: 0.75rem;
}

.action-input input {
  flex: 1;
}

.btn-primary {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  height: 42px;
  padding: 0 1.25rem;
  background: var(--primary);
  color: #ffffff;
  border: none;
  border-radius: 10px;
  font-family: inherit;
  font-weight: 600;
  font-size: 0.9rem;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}

.btn-primary svg { width: 18px; height: 18px; }

.btn-primary:hover:not(:disabled) {
  background: var(--primary-hover);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(14, 165, 233, 0.25);
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-primary--danger {
  background: var(--danger);
}

.btn-primary--danger:hover:not(:disabled) {
  background: #dc2626;
  box-shadow: 0 4px 12px rgba(239, 68, 68, 0.25);
}

.data-list-container {
  max-height: 500px;
  overflow-y: auto;
  padding-right: 0.5rem;
}

.data-list-container::-webkit-scrollbar { width: 6px; }
.data-list-container::-webkit-scrollbar-thumb { background: #cbd5e1; border-radius: 10px; }

.data-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.data-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.25rem;
  background: #ffffff;
  border: 1px solid var(--border-light);
  border-radius: 14px;
  transition: all 0.2s ease;
}

.data-row:hover {
  border-color: #cbd5e1;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.03);
  transform: translateY(-2px);
}

.row-main {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.row-title {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.id-hash {
  font-family: monospace;
  font-size: 0.8rem;
  color: #94a3b8;
  background: #f1f5f9;
  padding: 0.2rem 0.5rem;
  border-radius: 6px;
}

.name {
  font-size: 1.05rem;
  font-weight: 600;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.status-dot.active { background: var(--success); box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.15); }
.status-dot.disabled { background: var(--danger); }

.desc {
  margin: 0;
  font-size: 0.85rem;
  color: var(--text-sub);
  max-width: 350px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.row-actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.sort-badge {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--text-sub);
  background: #f8fafc;
  padding: 0.25rem 0.75rem;
  border-radius: 999px;
  border: 1px solid var(--border-light);
}

.btn-icon {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  background: transparent;
  border: 1px solid var(--border-light);
  color: var(--text-main);
  padding: 0.4rem 0.8rem;
  border-radius: 8px;
  font-size: 0.85rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-icon svg { width: 14px; height: 14px; }

.btn-icon:hover:not(:disabled) {
  background: #f1f5f9;
  color: var(--primary);
  border-color: #cbd5e1;
}

.btn-icon--danger:hover:not(:disabled) {
  color: var(--danger);
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 1.5rem;
  margin-top: 1rem;
  padding-top: 1.5rem;
  border-top: 1px dashed var(--border-light);
}

.page-nav {
  background: #ffffff;
  border: 1px solid var(--border-light);
  padding: 0.5rem 1rem;
  border-radius: 8px;
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--text-sub);
  cursor: pointer;
  transition: all 0.2s;
}

.page-nav:hover:not(:disabled) {
  color: var(--primary);
  border-color: var(--primary);
}

.page-nav:disabled {
  opacity: 0.4;
  cursor: not-allowed;
  background: #f8fafc;
}

.page-dots {
  font-size: 0.9rem;
  font-weight: 500;
  color: var(--text-sub);
}

.page-dots .current {
  color: var(--text-main);
  font-weight: 700;
}

.loading-placeholder {
  text-align: center;
  padding: 3rem 0;
  color: var(--text-sub);
  font-size: 0.9rem;
}

.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.28);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1.5rem;
  z-index: 50;
}

.modal-card {
  width: min(520px, 100%);
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: 18px;
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.12);
  overflow: hidden;
}

.modal-card--confirm {
  width: min(420px, 100%);
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1.25rem 1.5rem;
  border-bottom: 1px solid var(--border-light);
}

.modal-header h3 {
  margin: 0;
  font-size: 1.05rem;
  font-weight: 700;
}

.modal-close {
  border: none;
  background: transparent;
  color: var(--text-sub);
  font-size: 1.5rem;
  line-height: 1;
  cursor: pointer;
}

.modal-body {
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.modal-actions {
  padding: 0 1.5rem 1.5rem;
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
}

.confirm-text {
  margin: 0;
  color: var(--text-main);
  font-size: 0.95rem;
}

.list-enter-active, .list-leave-active { transition: all 0.3s ease; }
.list-enter-from, .list-leave-to { opacity: 0; transform: translateX(20px); }

.fade-enter-active, .fade-leave-active { transition: opacity 0.3s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

@media (max-width: 1200px) {
  .dashboard-grid { grid-template-columns: 1fr; }
  .input-row { grid-template-columns: 1fr; }
}
</style>
