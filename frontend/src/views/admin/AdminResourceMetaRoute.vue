<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { createAdminResourceCategoryApi, createAdminResourceTagApi, fetchAdminResourceCategoriesApi, fetchAdminResourceTagsApi, updateAdminResourceCategoryApi } from '@/api/admin-resource'
import type { AdminResourceCategory, AdminResourceTag, CreateOrUpdateResourceCategoryRequest, CreateResourceTagRequest } from '@/api/types'
import { toErrorMessage } from '@/views/shared/page-logic'

const loading = ref(false)
const processing = ref(false)
const errorMessage = ref('')
const categories = ref<AdminResourceCategory[]>([])
const tags = ref<AdminResourceTag[]>([])
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

onMounted(() => {
  void loadMeta()
})
</script>

<template>
  <section class="admin-meta-page">
    <div class="page-shell">
      <header class="hero-copy">
        <p class="eyebrow">资源元数据中心</p>
        <h1>集中维护资源分类与标签体系，让内容治理保持统一口径。</h1>
      </header>

      <p v-if="errorMessage" class="error-text">{{ errorMessage }}</p>

      <div class="meta-grid">
        <section class="glass-panel panel">
          <div class="section-head"><p class="section-kicker">Categories</p><h2>分类管理</h2></div>
          <div class="form-grid">
            <label><span>名称</span><input v-model="categoryForm.name" type="text"></label>
            <label><span>排序</span><input v-model.number="categoryForm.sortNo" type="number"></label>
            <label><span>状态</span><input v-model="categoryForm.status" type="text"></label>
            <label class="wide"><span>描述</span><input v-model="categoryForm.description" type="text"></label>
          </div>
          <div class="action-row">
            <button class="primary-button" type="button" :disabled="processing" @click="createCategory">新增分类</button>
          </div>
          <div class="list-stack">
            <article v-for="category in categories" :key="category.categoryId" class="list-card">
              <div class="list-topline">
                <div>
                  <p class="list-code">Category #{{ category.categoryId }}</p>
                  <h3>{{ category.name }}</h3>
                </div>
                <span class="status-pill">{{ category.status || 'ACTIVE' }}</span>
              </div>
              <p>{{ category.description || '无描述' }}</p>
              <button class="ghost-button" type="button" :disabled="processing" @click="updateCategory(category.categoryId)">用当前表单更新此分类</button>
            </article>
          </div>
        </section>

        <section class="glass-panel panel">
          <div class="section-head"><p class="section-kicker">Tags</p><h2>标签管理</h2></div>
          <div class="form-grid">
            <label><span>名称</span><input v-model="tagForm.name" type="text"></label>
            <label class="wide"><span>描述</span><input v-model="tagForm.description" type="text"></label>
          </div>
          <div class="action-row">
            <button class="primary-button" type="button" :disabled="processing" @click="createTag">新增标签</button>
          </div>
          <div class="list-stack">
            <article v-for="tag in tags" :key="tag.tagId" class="list-card">
              <p class="list-code">Tag #{{ tag.tagId }}</p>
              <h3>{{ tag.name }}</h3>
              <p>{{ tag.description || '无描述' }}</p>
            </article>
          </div>
        </section>
      </div>
    </div>
  </section>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700&family=Noto+Serif+SC:wght@400;500;600;700&display=swap');
.admin-meta-page{min-height:100vh;padding:44px 28px 72px;color:#272f27;background:linear-gradient(180deg,#f4efe6 0%,#f8f4ed 100%)}.page-shell{max-width:1320px;margin:0 auto}.hero-copy{border-top:1px solid rgba(59,69,59,.16);padding-top:18px;margin-bottom:28px}.eyebrow,.section-kicker,.list-code,.form-grid span{margin:0 0 10px;font:700 .76rem/1 'Manrope',sans-serif;letter-spacing:.22em;text-transform:uppercase;color:#7b6857}.hero-copy h1,.section-head h2,.list-card h3{margin:0;font-family:'Noto Serif SC',serif;font-weight:600}.hero-copy h1{font-size:clamp(2rem,3vw,3.2rem);line-height:1.16}.meta-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:28px}.glass-panel,.list-card{border:1px solid rgba(77,86,77,.14);background:rgba(255,252,247,.76);box-shadow:0 24px 70px rgba(91,80,66,.08);backdrop-filter:blur(16px)}.panel{padding:24px}.section-head{margin-bottom:18px}.form-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:14px}.form-grid label{display:grid;gap:8px}.wide{grid-column:1/-1}input{width:100%;box-sizing:border-box;border:1px solid rgba(80,88,79,.16);background:rgba(255,255,255,.74);padding:14px 16px;font:500 .95rem/1.4 'Manrope',sans-serif;color:#272f27;outline:none}.action-row{display:flex;gap:12px;margin:16px 0}.primary-button,.ghost-button{padding:12px 16px;font:700 .82rem/1 'Manrope',sans-serif;letter-spacing:.08em;text-transform:uppercase;cursor:pointer}.primary-button{border:none;background:linear-gradient(135deg,#253128 0%,#47564b 100%);color:#f8f5ef}.ghost-button{border:1px solid rgba(54,65,56,.2);background:rgba(255,255,255,.58);color:#272f27}.list-stack{display:grid;gap:14px}.list-card{padding:16px}.list-topline{display:flex;justify-content:space-between;gap:16px;align-items:start}.status-pill{border:1px solid rgba(97,111,98,.15);background:rgba(242,244,237,.94);padding:8px 12px;font:700 .74rem/1 'Manrope',sans-serif;letter-spacing:.12em;text-transform:uppercase;color:#66735f}.list-card p,.error-text{font:400 .92rem/1.8 'Manrope',sans-serif;color:rgba(39,47,39,.68)}.error-text{margin-bottom:16px;color:#a44f46}
@media (max-width:980px){.admin-meta-page{padding:28px 16px 46px}.meta-grid,.form-grid{grid-template-columns:1fr}}
</style>

