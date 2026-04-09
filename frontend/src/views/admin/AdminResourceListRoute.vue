<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { fetchAdminResourcesApi } from '@/api/admin-resource'
import type { AdminResourceListItem, AdminResourceQuery } from '@/api/types'
import { toErrorMessage } from '@/views/shared/page-logic'

const router = useRouter()
const loading = ref(false)
const errorMessage = ref('')
const resources = ref<AdminResourceListItem[]>([])
const filters = reactive<AdminResourceQuery>({
  status: undefined,
  keyword: ''
})

async function loadResources(): Promise<void> {
  loading.value = true
  errorMessage.value = ''

  try {
    resources.value = await fetchAdminResourcesApi(filters)
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

onMounted(() => {
  void loadResources()
})
</script>

<template>
  <section class="admin-resource-page">
    <div class="page-shell">
      <header class="page-hero">
        <div class="hero-copy">
          <p class="eyebrow">资源治理</p>
          <h1>查看资源上下线状态、浏览热度与分类归属，并进入单条资源编辑页。</h1>
        </div>
        <div class="hero-actions">
          <button class="ghost-button" type="button" @click="openMetaCenter">分类标签中心</button>
          <button class="primary-button" type="button" @click="openResourceDetail()">新增资源</button>
        </div>
      </header>

      <p v-if="errorMessage" class="error-text">{{ errorMessage }}</p>

      <section class="glass-panel panel">
        <div class="filter-grid">
          <label><span>状态</span><input v-model="filters.status" type="text" placeholder="已发布 / 草稿 / 已下线"></label>
          <label class="filter-wide"><span>关键词</span><input v-model="filters.keyword" type="text" placeholder="标题或摘要关键词"></label>
        </div>
        <button class="ghost-button" type="button" @click="loadResources">刷新资源列表</button>

        <p v-if="loading" class="state-text">正在同步资源列表...</p>
        <div v-else class="resource-stack">
          <article v-for="resource in resources" :key="resource.resourceId" class="resource-card" @click="openResourceDetail(resource.resourceId)">
            <div class="resource-topline">
              <div>
                <p class="resource-code">资源 #{{ resource.resourceId }}</p>
                <h3>{{ resource.title }}</h3>
              </div>
              <span class="status-pill">{{ resource.status }}</span>
            </div>
            <p class="resource-summary">{{ resource.summaryText }}</p>
            <div class="resource-meta">
              <span>{{ resource.categoryName }}</span>
              <span>{{ resource.resourceType }}</span>
              <span>浏览 {{ resource.viewCount }}</span>
              <span>收藏 {{ resource.favoriteCount }}</span>
            </div>
          </article>
        </div>
      </section>
    </div>
  </section>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700&family=Noto+Serif+SC:wght@400;500;600;700&display=swap');
.admin-resource-page{min-height:100vh;padding:44px 28px 72px;color:#272f27;background:linear-gradient(180deg,#f4efe6 0%,#f8f4ed 100%)}.page-shell{max-width:1320px;margin:0 auto}.page-hero{display:flex;justify-content:space-between;align-items:end;gap:18px;margin-bottom:28px}.hero-copy{border-top:1px solid rgba(59,69,59,.16);padding-top:18px;flex:1}.eyebrow,.resource-code,.filter-grid span{margin:0 0 10px;font:700 .76rem/1 'Manrope',sans-serif;letter-spacing:.22em;text-transform:uppercase;color:#7b6857}.hero-copy h1,.resource-card h3{margin:0;font-family:'Noto Serif SC',serif;font-weight:600}.hero-copy h1{font-size:clamp(2rem,3vw,3.2rem);line-height:1.16}.hero-actions{display:flex;gap:12px;flex-wrap:wrap}.primary-button,.ghost-button{padding:12px 16px;font:700 .82rem/1 'Manrope',sans-serif;letter-spacing:.08em;text-transform:uppercase;cursor:pointer}.primary-button{border:none;background:linear-gradient(135deg,#253128 0%,#47564b 100%);color:#f8f5ef}.ghost-button{border:1px solid rgba(54,65,56,.2);background:rgba(255,255,255,.58);color:#272f27}.panel,.resource-card{border:1px solid rgba(77,86,77,.14);background:rgba(255,252,247,.76);box-shadow:0 24px 70px rgba(91,80,66,.08);backdrop-filter:blur(16px)}.panel{padding:24px}.filter-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:14px;margin-bottom:16px}.filter-grid label{display:grid;gap:8px}.filter-wide{grid-column:1/-1}input{width:100%;box-sizing:border-box;border:1px solid rgba(80,88,79,.16);background:rgba(255,255,255,.74);padding:14px 16px;font:500 .95rem/1.4 'Manrope',sans-serif;color:#272f27;outline:none}.resource-stack{display:grid;gap:16px;margin-top:16px}.resource-card{padding:18px;cursor:pointer;transition:transform .28s ease,box-shadow .28s ease}.resource-card:hover{transform:translateY(-3px);box-shadow:0 28px 54px rgba(86,106,92,.12)}.resource-topline{display:flex;justify-content:space-between;gap:16px;align-items:start}.status-pill{border:1px solid rgba(97,111,98,.15);background:rgba(242,244,237,.94);padding:8px 12px;font:700 .74rem/1 'Manrope',sans-serif;letter-spacing:.12em;text-transform:uppercase;color:#66735f}.resource-summary,.resource-meta,.state-text,.error-text{font-family:'Manrope',sans-serif}.resource-summary{margin:14px 0 0;font-size:.96rem;line-height:1.84;color:rgba(39,47,39,.72)}.resource-meta{display:flex;flex-wrap:wrap;gap:10px 18px;margin-top:14px;font-size:.84rem;color:rgba(39,47,39,.6)}.error-text{margin-bottom:16px;color:#a44f46}
@media (max-width:980px){.admin-resource-page{padding:28px 16px 46px}.page-hero,.filter-grid{display:grid}.page-hero{grid-template-columns:1fr}}
</style>

