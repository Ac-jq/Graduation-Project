<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { fetchScaleListApi } from '@/api/assessment'
import type { ScaleSummary } from '@/api/types'
import { toErrorMessage } from '@/views/shared/page-logic'

const router = useRouter()
const loading = ref(false)
const errorMessage = ref('')
const scales = ref<ScaleSummary[]>([])

async function loadScales(): Promise<void> {
  loading.value = true
  errorMessage.value = ''

  try {
    scales.value = await fetchScaleListApi()
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    loading.value = false
  }
}

async function openScale(scaleId: number): Promise<void> {
  await router.push({ name: 'student-scale-detail', params: { scaleId } })
}

onMounted(() => {
  void loadScales()
})
</script>

<template>
  <main class="scale-list-page">
    <section class="scale-list-page__masthead">
      <div>
        <p class="scale-list-page__eyebrow">测评图谱</p>
        <h1 class="scale-list-page__title">心理测评目录</h1>
      </div>
      <div class="scale-list-page__aside">
        <p>当前可用量表</p>
        <strong>{{ scales.length }}</strong>
      </div>
    </section>

    <section class="scale-list-page__narrative">
      <p>
        这里集中展示学生端当前可参与的全部心理测评。进入任意量表后，你可以查看介绍、开始作答，并在提交后获得对应报告。
      </p>
    </section>

    <p v-if="errorMessage" class="scale-list-page__alert">
      {{ errorMessage }}
    </p>

    <p v-if="loading" class="scale-list-page__status">
      正在加载测评目录...
    </p>

    <section v-else class="scale-list-page__grid">
      <article
        v-for="(scale, index) in scales"
        :key="scale.id"
        class="scale-card"
        @click="openScale(scale.id)"
      >
        <div class="scale-card__index">{{ String(index + 1).padStart(2, '0') }}</div>
        <div class="scale-card__body">
          <p class="scale-card__code">{{ scale.code }}</p>
          <h2 class="scale-card__name">{{ scale.name }}</h2>
          <p class="scale-card__desc">{{ scale.description || '暂无补充说明。' }}</p>
        </div>
        <dl class="scale-card__metrics">
          <div>
            <dt>题目数</dt>
            <dd>{{ scale.totalQuestions }}</dd>
          </div>
          <div>
            <dt>每页题数</dt>
            <dd>{{ scale.pageSize }}</dd>
          </div>
        </dl>
      </article>
    </section>
  </main>
</template>
<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700&family=Noto+Serif+SC:wght@400;500;600;700&display=swap');

.scale-list-page {
  --paper: #f6f1e8;
  --ink: #211d19;
  --muted: #736b62;
  --line: rgba(33, 29, 25, 0.12);
  --accent: #7f8c80;
  min-height: 100vh;
  padding: 2rem;
  background:
    radial-gradient(circle at top left, rgba(188, 179, 158, 0.18), transparent 26%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.24), transparent 42%),
    var(--paper);
  color: var(--ink);
}

.scale-list-page__masthead {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 220px;
  gap: 1.5rem;
  align-items: end;
  padding-bottom: 1.4rem;
  border-bottom: 1px solid var(--line);
}

.scale-list-page__eyebrow,
.scale-card__code,
.scale-card__metrics dt,
.scale-list-page__aside p {
  margin: 0;
  font: 600 0.72rem/1.4 'Manrope', sans-serif;
  letter-spacing: 0.17em;
  text-transform: uppercase;
  color: var(--muted);
}

.scale-list-page__title {
  margin: 0.95rem 0 0;
  font: 600 clamp(2.5rem, 4.6vw, 4.8rem)/1 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.scale-list-page__aside {
  display: grid;
  gap: 0.55rem;
  align-content: end;
  padding: 1rem 1.1rem;
  border: 1px solid var(--line);
  background: rgba(255, 251, 245, 0.66);
  backdrop-filter: blur(18px);
}

.scale-list-page__aside strong {
  font: 600 2.3rem/1 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.scale-list-page__narrative {
  max-width: 48rem;
  margin: 1.4rem 0 0;
  color: var(--muted);
  font: 400 1rem/1.95 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.scale-list-page__alert,
.scale-list-page__status {
  margin: 1.25rem 0 0;
  font: 500 0.95rem/1.7 'Manrope', sans-serif;
}

.scale-list-page__alert {
  color: #8a4747;
}

.scale-list-page__status {
  color: var(--muted);
}

.scale-list-page__grid {
  margin-top: 2rem;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1.4rem;
}

.scale-card {
  display: grid;
  grid-template-columns: 70px minmax(0, 1fr);
  gap: 1rem;
  padding: 1.35rem;
  border: 1px solid var(--line);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.74), rgba(255, 255, 255, 0.38));
  backdrop-filter: blur(18px);
  cursor: pointer;
  transition: transform 180ms ease, border-color 180ms ease, box-shadow 180ms ease;
}

.scale-card:hover {
  transform: translateY(-4px);
  border-color: rgba(127, 140, 128, 0.32);
  box-shadow: 0 28px 46px rgba(83, 75, 66, 0.12);
}

.scale-card__index {
  font: 600 2.2rem/1 'Noto Serif SC', 'Source Han Serif SC', serif;
  color: rgba(33, 29, 25, 0.46);
}

.scale-card__name {
  margin: 0.55rem 0 0;
  font: 600 1.55rem/1.3 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.scale-card__desc {
  margin: 0.95rem 0 0;
  color: var(--muted);
  font: 400 0.98rem/1.85 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.scale-card__metrics {
  grid-column: 1 / -1;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
  margin: 0.5rem 0 0;
  padding-top: 1rem;
  border-top: 1px solid var(--line);
}

.scale-card__metrics dd {
  margin: 0.45rem 0 0;
  font: 600 1rem/1.5 'Manrope', sans-serif;
}

@media (max-width: 900px) {
  .scale-list-page {
    padding: 1rem;
  }

  .scale-list-page__masthead,
  .scale-list-page__grid {
    grid-template-columns: 1fr;
  }
}
</style>

