<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { fetchAdminScalesApi } from '@/api/admin-scale'
import type { AdminScale } from '@/api/types'
import { toErrorMessage } from '@/views/shared/page-logic'

const router = useRouter()
const loading = ref(false)
const errorMessage = ref('')
const scales = ref<AdminScale[]>([])

async function loadScales(): Promise<void> {
  loading.value = true
  errorMessage.value = ''

  try {
    scales.value = await fetchAdminScalesApi()
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    loading.value = false
  }
}

async function openScaleDetail(scaleId?: number): Promise<void> {
  if (scaleId) {
    await router.push({ name: 'admin-scale-detail', params: { scaleId } })
    return
  }

  await router.push({ name: 'admin-scale-new' })
}

onMounted(() => {
  void loadScales()
})
</script>

<template>
  <section class="admin-scale-page">
    <div class="page-shell">
      <header class="page-hero">
        <div class="hero-copy">
          <p class="eyebrow">Scale Governance</p>
          <h1>管理量表启停状态、阈值与题量结构，进入单量表页可继续编辑细节。</h1>
        </div>
        <button class="primary-button" type="button" @click="openScaleDetail()">新增量表</button>
      </header>

      <p v-if="errorMessage" class="error-text">{{ errorMessage }}</p>
      <p v-if="loading" class="state-text">正在同步量表列表...</p>

      <div v-else class="scale-stack">
        <article v-for="scale in scales" :key="scale.scaleId" class="scale-card" @click="openScaleDetail(scale.scaleId)">
          <div class="scale-topline">
            <div>
              <p class="scale-code">{{ scale.code }}</p>
              <h2>{{ scale.name }}</h2>
            </div>
            <span class="status-pill">{{ scale.status }}</span>
          </div>
          <p class="scale-description">{{ scale.description || '无量表描述' }}</p>
          <div class="scale-meta">
            <span>题目 {{ scale.totalQuestions }}</span>
            <span>分页 {{ scale.pageSize }}</span>
            <span>阈值 {{ scale.lowThreshold }} / {{ scale.mediumThreshold }} / {{ scale.highThreshold }}</span>
          </div>
        </article>
      </div>
    </div>
  </section>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700&family=Noto+Serif+SC:wght@400;500;600;700&display=swap');
.admin-scale-page{min-height:100vh;padding:44px 28px 72px;color:#272f27;background:linear-gradient(180deg,#f4efe6 0%,#f8f4ed 100%)}.page-shell{max-width:1240px;margin:0 auto}.page-hero{display:flex;justify-content:space-between;align-items:end;gap:16px;margin-bottom:28px}.hero-copy{border-top:1px solid rgba(59,69,59,.16);padding-top:18px;flex:1}.eyebrow,.scale-code{margin:0 0 10px;font:700 .76rem/1 'Manrope',sans-serif;letter-spacing:.22em;text-transform:uppercase;color:#7b6857}.hero-copy h1,.scale-card h2{margin:0;font-family:'Noto Serif SC',serif;font-weight:600}.hero-copy h1{font-size:clamp(2rem,3vw,3.2rem);line-height:1.16}.primary-button{border:none;background:linear-gradient(135deg,#253128 0%,#47564b 100%);color:#f8f5ef;padding:12px 16px;font:700 .82rem/1 'Manrope',sans-serif;letter-spacing:.08em;text-transform:uppercase;cursor:pointer}.scale-stack{display:grid;gap:16px}.scale-card{border:1px solid rgba(77,86,77,.14);background:rgba(255,252,247,.76);box-shadow:0 24px 70px rgba(91,80,66,.08);backdrop-filter:blur(16px);padding:18px;cursor:pointer;transition:transform .28s ease,box-shadow .28s ease}.scale-card:hover{transform:translateY(-3px);box-shadow:0 28px 54px rgba(86,106,92,.12)}.scale-topline{display:flex;justify-content:space-between;gap:16px;align-items:start}.status-pill{border:1px solid rgba(97,111,98,.15);background:rgba(242,244,237,.94);padding:8px 12px;font:700 .74rem/1 'Manrope',sans-serif;letter-spacing:.12em;text-transform:uppercase;color:#66735f}.scale-description,.scale-meta,.error-text,.state-text{font-family:'Manrope',sans-serif}.scale-description{margin:14px 0 0;font-size:.96rem;line-height:1.84;color:rgba(39,47,39,.72)}.scale-meta{display:flex;flex-wrap:wrap;gap:10px 18px;margin-top:14px;font-size:.84rem;color:rgba(39,47,39,.6)}.error-text{margin-bottom:16px;color:#a44f46}
@media (max-width:900px){.admin-scale-page{padding:28px 16px 46px}.page-hero{flex-direction:column;align-items:start}.scale-topline{flex-direction:column;align-items:start}}
</style>

