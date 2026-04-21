<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { fetchCounselorStudentsApi } from '@/api/user'
import type { CounselorStudentSummary } from '@/api/types'
import { toErrorMessage } from '@/views/shared/page-logic'
import SaaSBackground from '@/components/SaaSBackground.vue'

const router = useRouter()
const loading = ref(false)
const errorMessage = ref('')
const students = ref<CounselorStudentSummary[]>([])
const searchKeyword = ref('')

// 前端切片分页状态
const currentPage = ref(1)
const pageSize = 8

const filteredStudents = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase()
  if (!keyword) {
    return students.value
  }

  return students.value.filter((student) => {
    const fields = [
      student.studentName,
      student.studentNo,
      student.college,
      student.grade,
      student.gender
    ]
    return fields.some((field) => (field ?? '').toLowerCase().includes(keyword))
  })
})

const totalPages = computed(() => Math.max(1, Math.ceil(filteredStudents.value.length / pageSize)))
const pagedStudents = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return filteredStudents.value.slice(start, start + pageSize)
})

async function loadStudents(): Promise<void> {
  loading.value = true
  errorMessage.value = ''

  try {
    students.value = await fetchCounselorStudentsApi()
    currentPage.value = 1 // 重新加载时回到第一页
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    loading.value = false
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

async function openReports(studentUserId: number): Promise<void> {
  await router.push({ name: 'counselor-student-reports', params: { studentUserId } })
}

async function openAiSessions(studentUserId: number): Promise<void> {
  await router.push({ name: 'counselor-student-ai-sessions', params: { studentUserId } })
}

function resolveInitial(name: string | null | undefined): string {
  if (!name) return '匿'
  return name.charAt(0).toUpperCase()
}

onMounted(() => {
  void loadStudents()
})

watch(searchKeyword, () => {
  currentPage.value = 1
})
</script>

<template>
  <main class="editorial-roster-page">
    <SaaSBackground />
    <div class="page-container">

      <header class="roster-header">
        <div class="header-main">
          <span class="header-tag">Client Roster</span>
          <h1 class="huge-title">来访者名册</h1>
          <p class="header-lead">
            这里收录了与您建立咨询关系的所有学生档案。您可以直接查阅他们的情绪量表报告或 AI 访谈记录，为后续的干预与支持做好准备。
          </p>
        </div>

        <div class="header-stats">
          <div class="stat-item">
            <span class="stat-label">已绑定总数</span>
            <span class="stat-value">{{ loading ? '-' : students.length }}</span>
          </div>
        </div>
      </header>

      <div v-if="errorMessage" class="error-banner">{{ errorMessage }}</div>

      <div v-if="loading" class="loading-state">
        <div class="spinner"></div>
        <p>正在展卷名册...</p>
      </div>

      <div v-else-if="!students.length" class="empty-state">
        <h2 class="empty-title">名册尚为空白</h2>
        <p class="empty-desc">当前还没有学生绑定您为专属咨询师。</p>
      </div>

      <section v-else class="roster-section">

        <div class="list-toolbar">
          <input
              v-model.trim="searchKeyword"
              class="toolbar-search"
              type="search"
              placeholder="搜索姓名、学号、学院或年级"
          >
          <span class="toolbar-status">当前显示第 {{ currentPage }} 页，共 {{ totalPages }} 页</span>
        </div>

        <div class="roster-list">
          <article
              v-for="student in pagedStudents"
              :key="student.studentUserId"
              class="roster-row"
          >
            <div class="row-stamp" aria-hidden="true">
              {{ resolveInitial(student.studentName) }}
            </div>

            <div class="row-identity">
              <h3 class="student-name">{{ student.studentName || '匿名学生' }}</h3>
              <span class="student-code">ID: {{ student.studentUserId }} <span class="dot">·</span> 学号: {{ student.studentNo || '未提供' }}</span>
            </div>

            <div class="row-demographics">
              <span class="demo-tag">{{ student.gender || '性别未知' }}</span>
              <span class="demo-text">{{ student.college || '未填写学院' }}</span>
              <span class="demo-text">{{ student.grade || '未填写年级' }}</span>
            </div>

            <div class="row-actions">
              <button class="action-link" type="button" @click="openReports(student.studentUserId)">
                查阅量表报告 <span class="arrow">→</span>
              </button>
              <button class="action-link action-link--secondary" type="button" @click="openAiSessions(student.studentUserId)">
                调阅 AI 会话 <span class="arrow">→</span>
              </button>
            </div>
          </article>
        </div>

        <nav class="pagination-nav" v-if="totalPages > 1">
          <button class="page-btn" :disabled="currentPage <= 1" @click="prevPage">
            <span class="arrow">←</span> 往前翻
          </button>

          <div class="page-indicator">
            <span>{{ currentPage }}</span> / <span>{{ totalPages }}</span>
          </div>

          <button class="page-btn" :disabled="currentPage >= totalPages" @click="nextPage">
            往后翻 <span class="arrow">→</span>
          </button>
        </nav>

      </section>

    </div>
  </main>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700;800&family=Noto+Serif+SC:wght@500;600;700&display=swap');

/* 全局极简白纸底色 */
.editorial-roster-page {
  min-height: 100vh;
  position: relative;
  isolation: isolate;
  background: #fcfbf9;
  color: #1e2821;
  font-family: 'Manrope', 'Noto Serif SC', sans-serif;
  padding: 4rem 2vw 8rem;
  box-sizing: border-box;
  overflow-x: hidden;
}

.page-container {
  max-width: 1100px;
  margin: 0 auto;
  position: relative;
  z-index: 1;
}

/* 头部排版 */
.roster-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  padding-bottom: 3rem;
  margin-bottom: 2rem;
  border-bottom: 1px solid rgba(42, 54, 46, 0.12);
  gap: 4rem;
}

.header-main {
  max-width: 600px;
}

.header-tag {
  display: block;
  font-family: 'Manrope', sans-serif;
  font-size: 0.85rem;
  font-weight: 700;
  letter-spacing: 0.15em;
  text-transform: uppercase;
  color: #8a9c90;
  margin-bottom: 1rem;
}

.huge-title {
  font-family: 'Noto Serif SC', serif;
  font-size: 2.8rem;
  font-weight: 600;
  color: #1e2821;
  margin: 0 0 1.2rem 0;
  letter-spacing: 0.05em;
}

.header-lead {
  font-size: 1.05rem;
  color: #5c6b60;
  line-height: 1.8;
  margin: 0;
}

.header-stats {
  display: flex;
  gap: 3rem;
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.stat-label {
  font-family: 'Noto Serif SC', serif;
  font-size: 0.85rem;
  color: #8a9c90;
}

.stat-value {
  font-family: 'Manrope', sans-serif;
  font-size: 2.5rem;
  font-weight: 600;
  color: #2a362e;
  line-height: 1;
}

/* 列表控制栏 */
.list-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
  padding: 0 0.5rem;
  gap: 1rem;
  flex-wrap: wrap;
}

.toolbar-status {
  font-family: 'Noto Serif SC', serif;
  font-size: 0.95rem;
  color: #8a9c90;
}

.toolbar-search {
  min-width: 280px;
  padding: 0.9rem 1.1rem;
  border-radius: 999px;
  border: 1px solid rgba(42, 54, 46, 0.08);
  background: rgba(255, 255, 255, 0.82);
  color: #2a362e;
  font: 500 0.95rem/1 'Manrope', sans-serif;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.toolbar-search:focus {
  outline: none;
  border-color: rgba(92, 107, 96, 0.18);
  box-shadow: 0 16px 32px rgba(54, 66, 58, 0.08);
  transform: translateY(-1px);
}

/* 无边框名册列表 */
.roster-list {
  display: flex;
  flex-direction: column;
}

.roster-row {
  display: grid;
  grid-template-columns: 64px 1.5fr 1fr auto;
  gap: 2.5rem;
  align-items: center;
  padding: 2rem 1.5rem;
  border-bottom: 1px solid rgba(42, 54, 46, 0.08);
  transition: background 0.4s ease;
}

.roster-row:hover {
  background: rgba(255, 255, 255, 0.6);
}

/* 印章式头像 */
.row-stamp {
  width: 64px;
  height: 64px;
  border-radius: 20px;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, rgba(200, 214, 205, 0.5) 0%, rgba(225, 218, 208, 0.5) 100%);
  color: #2a362e;
  font-family: 'Noto Serif SC', serif;
  font-size: 1.6rem;
  font-weight: 600;
  box-shadow: inset 0 2px 0 rgba(255, 255, 255, 0.6);
}

/* 身份标识区 */
.row-identity {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  min-width: 0;
}

.student-name {
  font-family: 'Noto Serif SC', serif;
  font-size: 1.35rem;
  font-weight: 600;
  color: #1e2821;
  margin: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.student-code {
  font-family: 'Manrope', sans-serif;
  font-size: 0.95rem;
  color: #8a9c90;
}

.dot {
  margin: 0 0.4rem;
  color: #cbd5cf;
}

/* 人口统计学标签 */
.row-demographics {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 1.2rem;
}

.demo-tag {
  font-family: 'Noto Serif SC', serif;
  font-size: 0.85rem;
  font-weight: 600;
  color: #5c6b60;
  background: rgba(130, 150, 138, 0.15);
  padding: 0.3rem 0.8rem;
  border-radius: 100px;
}

.demo-text {
  font-family: 'Noto Serif SC', serif;
  font-size: 0.95rem;
  color: #6a7c70;
}

/* 操作链接 */
.row-actions {
  display: flex;
  gap: 2rem;
  align-items: center;
}

.action-link {
  background: transparent;
  border: none;
  font-family: 'Noto Serif SC', serif;
  font-size: 1.05rem;
  font-weight: 600;
  color: #2a362e;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0;
  transition: color 0.3s ease;
}

.action-link--secondary {
  color: #7b8c80;
}

.action-link:hover {
  color: #5c6b60;
}

/* 状态样式 */
.error-banner {
  background: rgba(140, 74, 74, 0.08);
  color: #8c4a4a;
  padding: 1.5rem;
  border-radius: 12px;
  text-align: center;
  font-family: 'Noto Serif SC', serif;
  margin-bottom: 2rem;
}

.loading-state,
.empty-state {
  text-align: center;
  padding: 8rem 0;
  color: #7b8c80;
  font-family: 'Noto Serif SC', serif;
}

.spinner {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: 2px solid rgba(130, 150, 138, 0.2);
  border-top-color: #2a362e;
  animation: spin 0.8s linear infinite;
  margin: 0 auto 1.5rem;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.empty-title {
  font-family: 'Noto Serif SC', serif;
  font-size: 1.6rem;
  color: #2a362e;
  margin: 0 0 1rem 0;
}

/* 分页器 */
.pagination-nav {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 2rem;
  margin-top: 4rem;
  padding-top: 2rem;
  border-top: 1px solid rgba(42, 54, 46, 0.08);
}

.page-btn {
  background: transparent;
  border: none;
  font-family: 'Noto Serif SC', serif;
  font-size: 1.05rem;
  font-weight: 600;
  color: #2a362e;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  transition: all 0.3s ease;
}

.page-btn:hover:not(:disabled) {
  color: #5c6b60;
}

.page-btn:disabled {
  color: #cbd5cf;
  cursor: not-allowed;
}

.page-indicator {
  font-family: 'Manrope', sans-serif;
  font-size: 1rem;
  color: #8a9c90;
  letter-spacing: 0.1em;
}

.page-indicator span {
  color: #2a362e;
  font-weight: 600;
}

/* 交互动画 */
.arrow {
  font-family: 'Manrope', sans-serif;
  transition: transform 0.3s ease;
}

.action-link:hover .arrow,
.page-btn:hover:not(:disabled) .arrow:last-child {
  transform: translateX(4px);
}

.page-btn:hover:not(:disabled) .arrow:first-child {
  transform: translateX(-4px);
}

/* 响应式 */
@media (max-width: 1024px) {
  .roster-row {
    grid-template-columns: 64px 1fr auto;
    gap: 1.5rem;
  }

  .row-demographics {
    display: none; /* 屏幕较窄时隐藏次要统计信息 */
  }
}

@media (max-width: 768px) {
  .roster-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 2rem;
  }

  .roster-row {
    grid-template-columns: 1fr;
    gap: 1.5rem;
    padding: 2rem 1rem;
  }

  .row-stamp {
    display: none; /* 移动端隐藏头像印章节省空间 */
  }

  .row-identity {
    gap: 0.8rem;
  }

  .row-actions {
    flex-direction: column;
    align-items: flex-start;
    gap: 1.2rem;
    margin-top: 0.5rem;
  }
}
</style>
