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
  <section class="admin-resource-detail-page">
    <div class="page-shell">
      <header class="hero-copy">
        <p class="eyebrow">资源详情</p>
        <h1>{{ resourceDetail ? '编辑资源' : '新增资源' }}</h1>
      </header>

      <p v-if="errorMessage" class="error-text">{{ errorMessage }}</p>

      <section class="glass-panel panel">
        <div class="form-grid">
          <label class="wide"><span>标题</span><input v-model="form.title" type="text"></label>
          <label class="wide"><span>摘要</span><textarea v-model="form.summaryText" rows="4" /></label>
          <label><span>资源类型</span><input v-model="form.resourceType" type="text"></label>
          <label><span>分类 ID</span><input v-model.number="form.categoryId" type="number"></label>
          <label class="wide"><span>内容链接</span><input v-model="form.contentUrl" type="url"></label>
          <label class="wide"><span>封面链接</span><input v-model="form.coverUrl" type="url"></label>
          <label class="wide"><span>标签 ID 列表（逗号分隔，仅展示当前值）</span><input :value="(form.tagIds || []).join(', ')" @input="form.tagIds = String(($event.target as HTMLInputElement).value).split(',').map((item) => Number(item.trim())).filter((item) => Number.isFinite(item) && item > 0)"></label>
        </div>

        <div class="meta-grid">
          <article class="meta-card"><h3>可选分类</h3><p v-for="category in categories" :key="category.categoryId">#{{ category.categoryId }} {{ category.name }}</p></article>
          <article class="meta-card"><h3>可选标签</h3><p v-for="tag in tags" :key="tag.tagId">#{{ tag.tagId }} {{ tag.name }}</p></article>
        </div>

        <div class="action-row">
          <button class="primary-button" type="button" :disabled="saving" @click="saveResource">保存资源</button>
          <button v-if="resourceDetail" class="ghost-button" type="button" :disabled="switchingStatus" @click="toggleResourceStatus">
            {{ resourceDetail.status === 'PUBLISHED' ? '下线资源' : '发布资源' }}
          </button>
        </div>
      </section>
    </div>
  </section>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700&family=Noto+Serif+SC:wght@400;500;600;700&display=swap');
.admin-resource-detail-page{min-height:100vh;padding:44px 28px 72px;color:#272f27;background:linear-gradient(180deg,#f4efe6 0%,#f8f4ed 100%)}.page-shell{max-width:1320px;margin:0 auto}.hero-copy{border-top:1px solid rgba(59,69,59,.16);padding-top:18px;margin-bottom:28px}.eyebrow,.form-grid span{margin:0 0 10px;font:700 .76rem/1 'Manrope',sans-serif;letter-spacing:.22em;text-transform:uppercase;color:#7b6857}.hero-copy h1,.meta-card h3{margin:0;font-family:'Noto Serif SC',serif;font-weight:600}.hero-copy h1{font-size:clamp(2rem,3vw,3.2rem);line-height:1.16}.panel,.meta-card{border:1px solid rgba(77,86,77,.14);background:rgba(255,252,247,.76);box-shadow:0 24px 70px rgba(91,80,66,.08);backdrop-filter:blur(16px)}.panel{padding:24px}.form-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:14px}.form-grid label{display:grid;gap:8px}.wide{grid-column:1/-1}input,textarea{width:100%;box-sizing:border-box;border:1px solid rgba(80,88,79,.16);background:rgba(255,255,255,.74);padding:14px 16px;font:500 .95rem/1.6 'Manrope',sans-serif;color:#272f27;outline:none;resize:vertical}.meta-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:16px;margin-top:18px}.meta-card{padding:16px}.meta-card p,.error-text{font:400 .92rem/1.8 'Manrope',sans-serif;color:rgba(39,47,39,.68)}.action-row{display:flex;flex-wrap:wrap;gap:12px;margin-top:18px}.primary-button,.ghost-button{padding:12px 16px;font:700 .82rem/1 'Manrope',sans-serif;letter-spacing:.08em;text-transform:uppercase;cursor:pointer}.primary-button{border:none;background:linear-gradient(135deg,#253128 0%,#47564b 100%);color:#f8f5ef}.ghost-button{border:1px solid rgba(54,65,56,.2);background:rgba(255,255,255,.58);color:#272f27}.error-text{margin-bottom:16px;color:#a44f46}
@media (max-width:980px){.admin-resource-detail-page{padding:28px 16px 46px}.form-grid,.meta-grid{grid-template-columns:1fr}}
</style>

