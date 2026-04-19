<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  createAdminResourceApi,
  fetchAdminResourceCategoriesApi,
  fetchAdminResourceTagsApi,
  fetchAdminResourcesApi,
  offlineAdminResourceApi,
  publishAdminResourceApi,
  updateAdminResourceApi,
  uploadAdminResourceAssetApi
} from '@/api/admin-resource'
import type {
  AdminResourceCategory,
  AdminResourceListItem,
  AdminResourceTag,
  CreateOrUpdateResourceRequest
} from '@/api/types'
import { toErrorMessage, toNumberParam } from '@/views/shared/page-logic'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const saving = ref(false)
const switchingStatus = ref(false)
const uploadingContent = ref(false)
const uploadingCover = ref(false)
const errorMessage = ref('')

const resourceId = computed(() => toNumberParam(route.params.resourceId))
const resourceDetail = ref<AdminResourceListItem | null>(null)
const categories = ref<AdminResourceCategory[]>([])
const tags = ref<AdminResourceTag[]>([])

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

function syncForm(data: AdminResourceListItem): void {
  form.title = data.title
  form.summaryText = data.summaryText
  form.resourceType = data.resourceType
  form.contentUrl = data.contentUrl
  form.coverUrl = data.coverUrl ?? ''
  form.categoryId = data.categoryId
  form.tagIds = data.tags.map((tag) => tag.tagId)
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

function resolveStatusText(status?: string): string {
  if (status === 'PUBLISHED') return '已发布'
  if (status === 'OFFLINE') return '已下线'
  if (status === 'DRAFT') return '草稿'
  return status || '未标记'
}

async function loadMeta(): Promise<void> {
  const [categoryList, tagList] = await Promise.all([
    fetchAdminResourceCategoriesApi(),
    fetchAdminResourceTagsApi()
  ])
  categories.value = categoryList
  tags.value = tagList
  if (!resourceId.value && !form.categoryId && categoryList.length) {
    form.categoryId = categoryList[0].categoryId
  }
}

async function loadDetail(): Promise<void> {
  loading.value = true
  errorMessage.value = ''

  try {
    await loadMeta()
    if (!resourceId.value) {
      resourceDetail.value = null
      resetForm()
      return
    }

    const resources = await fetchAdminResourcesApi({})
    const matched = resources.find((item) => item.resourceId === resourceId.value) ?? null
    resourceDetail.value = matched
    if (matched) {
      syncForm(matched)
    }
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    loading.value = false
  }
}

function toggleTag(tagId: number): void {
  if (form.tagIds.includes(tagId)) {
    form.tagIds = form.tagIds.filter((item) => item !== tagId)
    return
  }
  form.tagIds = [...form.tagIds, tagId]
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
  if (!form.resourceType) {
    errorMessage.value = '请选择资源类型'
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
      coverUrl: form.coverUrl.trim(),
      categoryId: form.categoryId,
      tagIds: form.tagIds
    }

    const saved = resourceId.value
      ? await updateAdminResourceApi(resourceId.value, payload)
      : await createAdminResourceApi(payload)

    resourceDetail.value = saved
    syncForm(saved)

    if (!resourceId.value) {
      await router.replace({ name: 'admin-resource-detail', params: { resourceId: saved.resourceId } })
    }
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    saving.value = false
  }
}

async function toggleResourceStatus(): Promise<void> {
  if (!resourceId.value || !resourceDetail.value) {
    errorMessage.value = '当前资源尚未保存，无法切换状态'
    return
  }

  switchingStatus.value = true
  errorMessage.value = ''

  try {
    resourceDetail.value = resourceDetail.value.status === 'PUBLISHED'
      ? await offlineAdminResourceApi(resourceId.value)
      : await publishAdminResourceApi(resourceId.value)
    syncForm(resourceDetail.value)
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    switchingStatus.value = false
  }
}

watch(() => route.params.resourceId, () => {
  void loadDetail()
})

onMounted(() => {
  void loadDetail()
})
</script>

<template>
  <section class="admin-editorial-page">
    <div class="admin-editorial-shell">
      <header class="admin-editorial-hero">
        <div class="admin-editorial-copy">
          <p class="admin-editorial-eyebrow">资源详情</p>
          <h1 class="admin-editorial-title">{{ resourceDetail ? '编辑资源' : '新增资源' }}</h1>
          <p class="admin-editorial-lead">这里支持直接选择资源类型、上传资源文件与封面图片，并用点击选中的方式管理标签。</p>
        </div>
        <div class="admin-editorial-hero-side">
          <article class="admin-editorial-stat">
            <p class="admin-editorial-label">当前状态</p>
            <strong>{{ resolveStatusText(resourceDetail?.status) }}</strong>
            <p class="admin-editorial-lead">{{ resourceDetail?.title || '新资源尚未保存' }}</p>
          </article>
        </div>
      </header>

      <p v-if="errorMessage" class="admin-editorial-alert">{{ errorMessage }}</p>

      <div class="admin-editorial-grid">
        <section class="admin-editorial-panel admin-editorial-panel--mesh">
          <div class="admin-editorial-section">
            <p class="admin-editorial-kicker">资源表单</p>
            <h2>编辑基础信息与文件</h2>
          </div>

          <div class="admin-editorial-form">
            <label class="admin-editorial-field wide">
              <span>资源标题</span>
              <input v-model="form.title" type="text" placeholder="请输入资源标题">
            </label>

            <label class="admin-editorial-field wide">
              <span>资源摘要</span>
              <textarea v-model="form.summaryText" rows="4" placeholder="请输入资源摘要" />
            </label>

            <label class="admin-editorial-field">
              <span>资源类型</span>
              <select v-model="form.resourceType">
                <option v-for="item in resourceTypeOptions" :key="item.value" :value="item.value">
                  {{ item.label }}
                </option>
              </select>
            </label>

            <label class="admin-editorial-field">
              <span>资源分类</span>
              <select v-model.number="form.categoryId">
                <option v-for="category in categories" :key="category.categoryId" :value="category.categoryId">
                  {{ category.name }}
                </option>
              </select>
            </label>

            <div class="admin-editorial-field wide">
              <span>资源文件</span>
              <div v-if="form.resourceType === 'LINK'" class="upload-block">
                <input v-model="form.contentUrl" type="url" placeholder="请输入外部链接地址">
              </div>
              <div v-else class="upload-block">
                <button class="admin-editorial-ghost" type="button" :disabled="uploadingContent" @click="openContentPicker">
                  {{ uploadingContent ? '上传中...' : '上传资源文件' }}
                </button>
                <div class="upload-meta">
                  <span>{{ form.contentUrl || '尚未上传资源文件' }}</span>
                </div>
                <input
                  ref="contentFileInput"
                  class="hidden-file-input"
                  type="file"
                  :accept="contentFileAccept"
                  @change="uploadContentFile"
                >
              </div>
            </div>

            <div class="admin-editorial-field wide">
              <span>封面图片</span>
              <div class="upload-block">
                <button class="admin-editorial-ghost" type="button" :disabled="uploadingCover" @click="openCoverPicker">
                  {{ uploadingCover ? '上传中...' : '上传封面图片' }}
                </button>
                <div class="upload-meta">
                  <span>{{ form.coverUrl || '尚未上传封面图片' }}</span>
                </div>
                <input
                  ref="coverFileInput"
                  class="hidden-file-input"
                  type="file"
                  accept="image/*,.jpg,.jpeg,.png,.webp,.gif"
                  @change="uploadCoverFile"
                >
              </div>
            </div>

            <div class="admin-editorial-field wide">
              <span>标签选择</span>
              <div class="tag-selector">
                <button
                  v-for="tag in tags"
                  :key="tag.tagId"
                  class="tag-pill"
                  :class="{ 'is-active': form.tagIds.includes(tag.tagId) }"
                  type="button"
                  @click="toggleTag(tag.tagId)"
                >
                  {{ tag.name }}
                </button>
              </div>
            </div>
          </div>

          <div class="admin-editorial-actions" style="margin-top: 1rem;">
            <button class="admin-editorial-button" type="button" :disabled="saving" @click="saveResource">
              {{ saving ? '保存中...' : '保存资源' }}
            </button>
            <button
              v-if="resourceDetail"
              class="admin-editorial-ghost"
              type="button"
              :disabled="switchingStatus"
              @click="toggleResourceStatus"
            >
              {{ resourceDetail.status === 'PUBLISHED' ? '下线资源' : '发布资源' }}
            </button>
          </div>
        </section>

        <section class="admin-editorial-panel">
          <div class="admin-editorial-section">
            <p class="admin-editorial-kicker">辅助参照</p>
            <h2>当前已选分类与标签</h2>
          </div>

          <div v-if="loading" class="admin-editorial-empty">正在同步资源元数据...</div>
          <div v-else class="admin-editorial-board">
            <article class="admin-editorial-card">
              <p class="admin-editorial-code">当前分类</p>
              <h3>{{ categories.find((item) => item.categoryId === form.categoryId)?.name || '未选择分类' }}</h3>
              <p>{{ categories.find((item) => item.categoryId === form.categoryId)?.description || '请选择当前资源所属分类。' }}</p>
            </article>
            <article class="admin-editorial-card">
              <p class="admin-editorial-code">已选标签</p>
              <div class="admin-editorial-chip-list">
                <span
                  v-for="tag in tags.filter((item) => form.tagIds.includes(item.tagId))"
                  :key="tag.tagId"
                  class="admin-editorial-badge"
                >
                  {{ tag.name }}
                </span>
                <span v-if="!form.tagIds.length" class="admin-editorial-empty-inline">当前还未选择标签</span>
              </div>
            </article>
          </div>
        </section>
      </div>
    </div>
  </section>
</template>

<style scoped>
@import './admin-editorial.css';

.upload-block {
  display: grid;
  gap: 0.8rem;
}

.upload-meta {
  min-height: 3rem;
  padding: 0.9rem 1rem;
  border-radius: 18px;
  border: 1px solid rgba(42, 54, 46, 0.08);
  background: rgba(255, 255, 255, 0.68);
  color: rgba(42, 54, 46, 0.72);
  font: 500 0.92rem/1.6 'Manrope', sans-serif;
  word-break: break-all;
}

.hidden-file-input {
  display: none;
}

.tag-selector {
  display: flex;
  flex-wrap: wrap;
  gap: 0.8rem;
}

.tag-pill {
  border: 1px solid rgba(42, 54, 46, 0.08);
  background: rgba(255, 255, 255, 0.66);
  color: #2a362e;
  padding: 0.85rem 1rem;
  border-radius: 999px;
  cursor: pointer;
  font: 600 0.88rem/1 'Manrope', sans-serif;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.tag-pill:hover {
  transform: translateY(-3px);
  box-shadow: 0 20px 44px rgba(54, 66, 58, 0.08);
}

.tag-pill.is-active {
  background: linear-gradient(135deg, #2d3b31 0%, #617a69 100%);
  color: #fffdf8;
  border-color: transparent;
}

.admin-editorial-empty-inline {
  color: rgba(42, 54, 46, 0.58);
  font: 500 0.92rem/1.6 'Manrope', sans-serif;
}
</style>
