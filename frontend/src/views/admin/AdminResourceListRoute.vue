<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import {
  createAdminResourceApi,
  fetchAdminResourceCategoriesApi,
  fetchAdminResourcesApi,
  fetchAdminResourceTagsApi,
  offlineAdminResourceApi,
  publishAdminResourceApi,
  updateAdminResourceApi,
  uploadAdminResourceAssetApi
} from '@/api/admin-resource'
import type {
  AdminResourceCategory,
  AdminResourceListItem,
  AdminResourceQuery,
  AdminResourceTag,
  CreateOrUpdateResourceRequest
} from '@/api/types'
import { toErrorMessage } from '@/views/shared/page-logic'

const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const switchingStatus = ref(false)
const uploadingContent = ref(false)
const uploadingCover = ref(false)
const errorMessage = ref('')
const resources = ref<AdminResourceListItem[]>([])
const categories = ref<AdminResourceCategory[]>([])
const tags = ref<AdminResourceTag[]>([])
const currentPage = ref(1)
const pageSize = 10
const dialogVisible = ref(false)
const editingResourceId = ref<number | null>(null)

const filters = reactive<AdminResourceQuery>({
  status: undefined,
  keyword: ''
})

const form = reactive<CreateOrUpdateResourceRequest>({
  title: '',
  summaryText: '',
  resourceType: 'ARTICLE',
  contentUrl: '',
  coverUrl: '',
  categoryId: 0,
  tagIds: []
})

const contentFileInput = ref<HTMLInputElement | null>(null)
const coverFileInput = ref<HTMLInputElement | null>(null)

const totalPages = computed(() => Math.max(1, Math.ceil(resources.value.length / pageSize)))
const pagedResources = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return resources.value.slice(start, start + pageSize)
})

const isCreateMode = computed(() => editingResourceId.value == null)

const resourceTypeOptions = [
  { label: '图文文章', value: 'ARTICLE' },
  { label: '视频资源', value: 'VIDEO' },
  { label: '音频资源', value: 'AUDIO' },
  { label: '图像内容', value: 'IMAGE' },
  { label: '外部链接', value: 'LINK' }
]

const contentFileAccept = computed(() => {
  if (form.resourceType === 'LINK') {
    return '*/*'
  }
  return [
    'image/*',
    'video/*',
    'audio/*',
    '.jpg',
    '.jpeg',
    '.png',
    '.webp',
    '.gif',
    '.mp4',
    '.webm',
    '.mov',
    '.m4v',
    '.mp3',
    '.wav',
    '.ogg',
    '.m4a',
    '.pdf',
    '.txt',
    '.md',
    '.html',
    '.htm',
    '.doc',
    '.docx',
    'text/plain',
    'text/html',
    'application/pdf'
  ].join(',')
})

function rowIndex(index: number): number {
  return (currentPage.value - 1) * pageSize + index + 1
}

function resolveStatusText(status?: string): string {
  if (status === 'PUBLISHED') return '已发布'
  if (status === 'OFFLINE') return '已下线'
  if (status === 'DRAFT') return '草稿'
  return status || '未标记'
}

function resetForm(): void {
  form.title = ''
  form.summaryText = ''
  form.resourceType = 'ARTICLE'
  form.contentUrl = ''
  form.coverUrl = ''
  form.categoryId = categories.value[0]?.categoryId ?? 0
  form.tagIds = []
}

function syncForm(data: AdminResourceListItem): void {
  form.title = data.title
  form.summaryText = data.summaryText
  form.resourceType = data.resourceType
  form.contentUrl = data.contentUrl
  form.coverUrl = data.coverUrl ?? ''
  form.categoryId = data.categoryId
  form.tagIds = data.tags.map((tag) => tag.tagId)
}

async function loadMeta(): Promise<void> {
  const [categoryList, tagList] = await Promise.all([
    fetchAdminResourceCategoriesApi(),
    fetchAdminResourceTagsApi()
  ])
  categories.value = categoryList
  tags.value = tagList
  if (!form.categoryId && categories.value.length) {
    form.categoryId = categories.value[0].categoryId
  }
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

function resetFilters(): void {
  filters.status = undefined
  filters.keyword = ''
}

function openCreateDialog(): void {
  errorMessage.value = ''
  editingResourceId.value = null
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(resource: AdminResourceListItem): void {
  errorMessage.value = ''
  editingResourceId.value = resource.resourceId
  syncForm(resource)
  dialogVisible.value = true
}

function closeDialog(): void {
  dialogVisible.value = false
  editingResourceId.value = null
}

function toggleTag(tagId: number): void {
  if (form.tagIds?.includes(tagId)) {
    form.tagIds = form.tagIds.filter((item) => item !== tagId)
    return
  }
  form.tagIds = [...(form.tagIds ?? []), tagId]
}

function openContentPicker(): void {
  contentFileInput.value?.click()
}

function openCoverPicker(): void {
  coverFileInput.value?.click()
}

async function uploadContentFile(event: Event): Promise<void> {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  uploadingContent.value = true
  errorMessage.value = ''
  try {
    const uploaded = await uploadAdminResourceAssetApi(file, false)
    form.contentUrl = uploaded.assetUrl
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    uploadingContent.value = false
    input.value = ''
  }
}

async function uploadCoverFile(event: Event): Promise<void> {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  uploadingCover.value = true
  errorMessage.value = ''
  try {
    const uploaded = await uploadAdminResourceAssetApi(file, true)
    form.coverUrl = uploaded.assetUrl
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    uploadingCover.value = false
    input.value = ''
  }
}

function validateForm(): boolean {
  if (!form.title.trim()) {
    errorMessage.value = '请填写资源标题'
    return false
  }
  if (!form.summaryText.trim()) {
    errorMessage.value = '请填写资源摘要'
    return false
  }
  if (!form.categoryId) {
    errorMessage.value = '请选择资源分类'
    return false
  }
  if (!form.contentUrl.trim()) {
    errorMessage.value = form.resourceType === 'LINK' ? '请填写外部链接地址' : '请先上传资源文件'
    return false
  }
  return true
}

async function saveResource(): Promise<void> {
  if (!validateForm()) {
    return
  }

  saving.value = true
  errorMessage.value = ''
  try {
    const payload: CreateOrUpdateResourceRequest = {
      title: form.title.trim(),
      summaryText: form.summaryText.trim(),
      resourceType: form.resourceType,
      contentUrl: form.contentUrl.trim(),
      coverUrl: form.coverUrl?.trim() || '',
      categoryId: form.categoryId,
      tagIds: form.tagIds ?? []
    }

    if (editingResourceId.value) {
      await updateAdminResourceApi(editingResourceId.value, payload)
      ElMessage.success('资源已更新')
    } else {
      await createAdminResourceApi(payload)
      ElMessage.success('资源已创建')
    }
    closeDialog()
    await loadResources()
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    saving.value = false
  }
}

async function toggleResourceStatus(resource: AdminResourceListItem): Promise<void> {
  switchingStatus.value = true
  errorMessage.value = ''
  try {
    if (resource.status === 'PUBLISHED') {
      await offlineAdminResourceApi(resource.resourceId)
      ElMessage.success('资源已下线')
    } else {
      await publishAdminResourceApi(resource.resourceId)
      ElMessage.success('资源已发布')
    }
    await loadResources()
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    switchingStatus.value = false
  }
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
  void Promise.all([loadMeta(), loadResources()])
})
</script>

<template>
  <section class="admin-table-page">
    <div class="admin-table-shell">
      <header class="admin-table-header">
        <div>
          <h1>资源管理</h1>
          <p>统一筛选资源、查看状态与热度，新增和编辑都在弹窗内完成。</p>
        </div>
      </header>

      <p v-if="errorMessage" class="admin-table-alert">{{ errorMessage }}</p>

      <section class="admin-table-toolbar">
        <div class="admin-table-filters">
          <label class="admin-table-field admin-table-field--keyword">
            <span>关键词</span>
            <input v-model="filters.keyword" type="text" placeholder="标题 / 摘要" @keyup.enter="loadResources">
          </label>
          <label class="admin-table-field">
            <span>状态</span>
            <select v-model="filters.status">
              <option :value="undefined">全部状态</option>
              <option value="PUBLISHED">已发布</option>
              <option value="DRAFT">草稿</option>
              <option value="OFFLINE">已下线</option>
            </select>
          </label>
        </div>
        <div class="admin-table-actions">
          <button class="admin-table-button--secondary" type="button" @click="resetFilters">重置</button>
          <button class="admin-table-button--secondary" type="button" @click="loadResources">查询</button>
          <button class="admin-table-button--secondary" type="button" @click="router.push({ name: 'admin-resource-meta' })">分类标签</button>
          <button class="admin-table-button" type="button" @click="openCreateDialog">新增资源</button>
        </div>
      </section>

      <section class="admin-table-panel">
        <div class="admin-table-panel-header">
          <div>
            <h2 class="admin-table-panel-title">资源列表</h2>
            <p class="admin-table-panel-note">共 {{ resources.length }} 条记录</p>
          </div>
        </div>

        <div class="admin-table-wrap">
          <table class="admin-table">
            <thead>
              <tr>
                <th>#</th>
                <th>资源标题</th>
                <th>分类</th>
                <th>类型</th>
                <th>状态</th>
                <th>标签</th>
                <th>浏览 / 收藏</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(resource, index) in pagedResources" :key="resource.resourceId">
                <td>{{ rowIndex(index) }}</td>
                <td>
                  <div>{{ resource.title }}</div>
                  <div class="admin-table-muted">{{ resource.summaryText }}</div>
                </td>
                <td>{{ resource.categoryName }}</td>
                <td>{{ resource.resourceType }}</td>
                <td>
                  <span class="admin-table-status" :class="resource.status === 'PUBLISHED' ? 'is-success' : resource.status === 'OFFLINE' ? 'is-warning' : ''">
                    {{ resolveStatusText(resource.status) }}
                  </span>
                </td>
                <td>{{ resource.tags.map((tag) => tag.name).join('、') || '--' }}</td>
                <td>{{ resource.viewCount }} / {{ resource.favoriteCount }}</td>
                <td>
                  <div class="admin-table-ops">
                    <button class="admin-table-inline-btn" type="button" @click="openEditDialog(resource)">编辑</button>
                    <button class="admin-table-inline-btn" type="button" :disabled="switchingStatus" @click="toggleResourceStatus(resource)">
                      {{ resource.status === 'PUBLISHED' ? '下线' : '发布' }}
                    </button>
                  </div>
                </td>
              </tr>
              <tr v-if="!pagedResources.length">
                <td colspan="8" class="admin-table-empty">{{ loading ? '正在加载资源列表...' : '暂无资源数据' }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="admin-table-pagination" v-if="totalPages > 1">
          <span>第 {{ currentPage }} / {{ totalPages }} 页</span>
          <div class="admin-table-pagination-actions">
            <button class="admin-table-button--secondary" type="button" :disabled="currentPage <= 1" @click="prevPage">上一页</button>
            <button class="admin-table-button--secondary" type="button" :disabled="currentPage >= totalPages" @click="nextPage">下一页</button>
          </div>
        </div>
      </section>

      <el-dialog v-model="dialogVisible" :title="isCreateMode ? '新增资源' : '编辑资源'" width="920px" destroy-on-close>
        <div class="admin-table-dialog-body">
          <div class="admin-table-dialog-grid">
            <label class="admin-table-dialog-label is-wide">
              资源标题
              <input v-model="form.title" type="text" placeholder="请输入资源标题">
            </label>
            <label class="admin-table-dialog-label is-wide">
              资源摘要
              <textarea v-model="form.summaryText" rows="4" placeholder="请输入资源摘要"></textarea>
            </label>
            <label class="admin-table-dialog-label">
              资源类型
              <select v-model="form.resourceType">
                <option v-for="item in resourceTypeOptions" :key="item.value" :value="item.value">
                  {{ item.label }}
                </option>
              </select>
            </label>
            <label class="admin-table-dialog-label">
              资源分类
              <select v-model.number="form.categoryId">
                <option v-for="category in categories" :key="category.categoryId" :value="category.categoryId">
                  {{ category.name }}
                </option>
              </select>
            </label>
            <label v-if="form.resourceType === 'LINK'" class="admin-table-dialog-label is-wide">
              外部链接
              <input v-model="form.contentUrl" type="url" placeholder="请输入外部链接地址">
            </label>
            <div v-else class="is-wide admin-table-upload">
              <label class="admin-table-dialog-label">资源文件</label>
              <button class="admin-table-button--secondary" type="button" :disabled="uploadingContent" @click="openContentPicker">
                {{ uploadingContent ? '上传中...' : '上传资源文件' }}
              </button>
              <span class="admin-table-muted">{{ form.contentUrl || '尚未上传资源文件' }}</span>
              <input ref="contentFileInput" class="admin-table-file-input" type="file" :accept="contentFileAccept" @change="uploadContentFile">
            </div>
            <div class="is-wide admin-table-upload">
              <label class="admin-table-dialog-label">封面图片</label>
              <button class="admin-table-button--secondary" type="button" :disabled="uploadingCover" @click="openCoverPicker">
                {{ uploadingCover ? '上传中...' : '上传封面图片' }}
              </button>
              <span class="admin-table-muted">{{ form.coverUrl || '尚未上传封面图片' }}</span>
              <input ref="coverFileInput" class="admin-table-file-input" type="file" accept="image/*,.jpg,.jpeg,.png,.webp,.gif" @change="uploadCoverFile">
            </div>
            <div class="is-wide">
              <label class="admin-table-dialog-label">标签选择</label>
              <div class="admin-table-tag-picker">
                <button
                  v-for="tag in tags"
                  :key="tag.tagId"
                  class="admin-table-tag"
                  :class="{ 'is-active': form.tagIds?.includes(tag.tagId) }"
                  type="button"
                  @click="toggleTag(tag.tagId)"
                >
                  {{ tag.name }}
                </button>
              </div>
            </div>
          </div>
        </div>
        <template #footer>
          <div class="admin-table-dialog-footer">
            <button class="admin-table-button--secondary" type="button" @click="closeDialog">取消</button>
            <button class="admin-table-button" type="button" :disabled="saving" @click="saveResource">
              {{ saving ? '保存中...' : '保存资源' }}
            </button>
          </div>
        </template>
      </el-dialog>
    </div>
  </section>
</template>

<style scoped>
@import './admin-table.css';
</style>
