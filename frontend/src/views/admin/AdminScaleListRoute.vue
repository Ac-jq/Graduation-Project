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

function resolveStatusText(status?: string): string {
  return status === 'ACTIVE' ? '启用中' : status === 'INACTIVE' ? '已停用' : status || '未标记'
}

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
  <section class="admin-editorial-page">
    <div class="admin-editorial-shell">
      <header class="admin-editorial-hero">
        <div class="admin-editorial-copy">
          <p class="admin-editorial-eyebrow">量表治理</p>
          <h1 class="admin-editorial-title">集中查看量表启停状态、阈值结构与题量规模，再进入单量表页做精细维护。</h1>
        </div>
        <div class="admin-editorial-hero-side">
          <article class="admin-editorial-stat">
            <p class="admin-editorial-label">量表总数</p>
            <strong>{{ scales.length }}</strong>
          </article>
          <div class="admin-editorial-actions">
            <button class="admin-editorial-button" type="button" @click="openScaleDetail()">新增量表</button>
          </div>
        </div>
      </header>

      <p v-if="errorMessage" class="admin-editorial-alert">{{ errorMessage }}</p>
      <div v-if="loading" class="admin-editorial-empty">正在同步量表列表…</div>

      <div v-else class="admin-editorial-board">
        <article
          v-for="scale in scales"
          :key="scale.scaleId"
          class="admin-editorial-card"
          style="cursor: pointer;"
          @click="openScaleDetail(scale.scaleId)"
        >
          <div class="admin-editorial-card__topline">
            <div>
              <p class="admin-editorial-code">{{ scale.code }}</p>
              <h3>{{ scale.name }}</h3>
            </div>
            <span class="admin-editorial-status">{{ resolveStatusText(scale.status) }}</span>
          </div>
          <p>{{ scale.description || '无量表描述' }}</p>
          <div class="admin-editorial-meta">
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
@import './admin-editorial.css';
</style>
