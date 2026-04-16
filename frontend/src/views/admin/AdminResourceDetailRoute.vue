<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { createAdminResourceApi, fetchAdminResourceCategoriesApi, fetchAdminResourceTagsApi, fetchAdminResourcesApi, offlineAdminResourceApi, publishAdminResourceApi, updateAdminResourceApi } from '@/api/admin-resource'
import type { AdminResourceCategory, AdminResourceTag, AdminResourceListItem, CreateOrUpdateResourceRequest } from '@/api/types'
import { toErrorMessage, toNumberParam } from '@/views/shared/page-logic'

const route = useRoute()
const loading = ref(false)
const saving = ref(false)
const switchingStatus = ref(false)
const errorMessage = ref('')
const resourceId = computed(() => toNumberParam(route.params.resourceId))
const resourceDetail = ref<AdminResourceListItem | null>(null)
const categories = ref<AdminResourceCategory[]>([])
const tags = ref<AdminResourceTag[]>([])
const form = reactive<CreateOrUpdateResourceRequest>({
  title: '',
  summaryText: '',
  resourceType: '',
  contentUrl: '',
  coverUrl: '',
  categoryId: 0,
  tagIds: []
})

function syncForm(data: AdminResourceListItem): void {
  form.title = data.title
  form.summaryText = data.summaryText
  form.resourceType = data.resourceType
  form.contentUrl = data.contentUrl
  form.coverUrl = data.coverUrl
  form.categoryId = data.categoryId
  form.tagIds = data.tags.map((tag) => tag.tagId)
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
}

async function loadDetail(): Promise<void> {
  loading.value = true
  errorMessage.value = ''

  try {
    await loadMeta()
    if (!resourceId.value) {
      resourceDetail.value = null
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

async function saveResource(): Promise<void> {
  saving.value = true
  errorMessage.value = ''

  try {
    resourceDetail.value = resourceId.value
      ? await updateAdminResourceApi(resourceId.value, form)
      : await createAdminResourceApi(form)

    if (resourceDetail.value) {
      syncForm(resourceDetail.value)
    }
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    saving.value = false
  }
}

async function toggleResourceStatus(): Promise<void> {
  if (!resourceId.value || !resourceDetail.value) {
    errorMessage.value = 'Invalid resourceId'
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
          <p class="admin-editorial-lead">资源保存、发布和下线逻辑保持不变，界面只改成更克制、更接近学生端的编辑体验。</p>
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
            <h2>编辑基础信息与链接</h2>
          </div>

          <div class="admin-editorial-form">
            <label class="admin-editorial-field wide">
              <span>标题</span>
              <input v-model="form.title" type="text">
            </label>
            <label class="admin-editorial-field wide">
              <span>摘要</span>
              <textarea v-model="form.summaryText" rows="4" />
            </label>
            <label class="admin-editorial-field">
              <span>资源类型</span>
              <input v-model="form.resourceType" type="text">
            </label>
            <label class="admin-editorial-field">
              <span>分类 ID</span>
              <input v-model.number="form.categoryId" type="number">
            </label>
            <label class="admin-editorial-field wide">
              <span>内容链接</span>
              <input v-model="form.contentUrl" type="url">
            </label>
            <label class="admin-editorial-field wide">
              <span>封面链接</span>
              <input v-model="form.coverUrl" type="url">
            </label>
            <label class="admin-editorial-field wide">
              <span>标签 ID 列表</span>
              <input
                :value="(form.tagIds || []).join(', ')"
                @input="form.tagIds = String(($event.target as HTMLInputElement).value).split(',').map((item) => Number(item.trim())).filter((item) => Number.isFinite(item) && item > 0)"
              >
            </label>
          </div>

          <div class="admin-editorial-actions" style="margin-top: 1rem;">
            <button class="admin-editorial-button" type="button" :disabled="saving" @click="saveResource">保存资源</button>
            <button v-if="resourceDetail" class="admin-editorial-ghost" type="button" :disabled="switchingStatus" @click="toggleResourceStatus">
              {{ resourceDetail.status === 'PUBLISHED' ? '下线资源' : '发布资源' }}
            </button>
          </div>
        </section>

        <section class="admin-editorial-panel">
          <div class="admin-editorial-section">
            <p class="admin-editorial-kicker">辅助参照</p>
            <h2>当前可选分类与标签</h2>
          </div>

          <div v-if="loading" class="admin-editorial-empty">正在同步资源元数据…</div>
          <div v-else class="admin-editorial-board">
            <article class="admin-editorial-card">
              <p class="admin-editorial-code">可选分类</p>
              <div class="admin-editorial-stack">
                <div v-for="category in categories" :key="category.categoryId" class="admin-editorial-meta">
                  <span>#{{ category.categoryId }}</span>
                  <span>{{ category.name }}</span>
                </div>
              </div>
            </article>
            <article class="admin-editorial-card">
              <p class="admin-editorial-code">可选标签</p>
              <div class="admin-editorial-stack">
                <div v-for="tag in tags" :key="tag.tagId" class="admin-editorial-meta">
                  <span>#{{ tag.tagId }}</span>
                  <span>{{ tag.name }}</span>
                </div>
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
</style>
