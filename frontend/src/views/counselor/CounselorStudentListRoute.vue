<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { fetchCounselorStudentsApi } from '@/api/user'
import type { CounselorStudentSummary } from '@/api/types'
import { toErrorMessage } from '@/views/shared/page-logic'

const router = useRouter()
const loading = ref(false)
const errorMessage = ref('')
const students = ref<CounselorStudentSummary[]>([])

async function loadStudents(): Promise<void> {
  loading.value = true
  errorMessage.value = ''

  try {
    students.value = await fetchCounselorStudentsApi()
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    loading.value = false
  }
}

async function openReports(studentUserId: number): Promise<void> {
  await router.push({ name: 'counselor-student-reports', params: { studentUserId } })
}

async function openAiSessions(studentUserId: number): Promise<void> {
  await router.push({ name: 'counselor-student-ai-sessions', params: { studentUserId } })
}

onMounted(() => {
  void loadStudents()
})
</script>

<template>
  <section class="c-students-page">
    <div class="page-shell">
      <header class="page-hero">
        <div class="hero-copy">
          <p class="eyebrow">Bound Students</p>
          <h1>在一个沉静的名单里查看已绑定学生，并继续深入到报告与 AI 会话。</h1>
          <p class="lead">页面直接展示后端返回的学生身份、学院、年级与学号信息。</p>
        </div>
        <div class="hero-metric">
          <span>绑定学生</span>
          <strong>{{ students.length }}</strong>
        </div>
      </header>

      <p v-if="loading" class="state-text">正在同步绑定学生列表...</p>
      <p v-else-if="errorMessage" class="error-text">{{ errorMessage }}</p>
      <p v-else-if="!students.length" class="state-text">当前没有已绑定学生。</p>

      <div v-else class="student-grid">
        <article v-for="student in students" :key="student.studentUserId" class="student-card">
          <div class="student-card__topline">
            <div>
              <p class="student-code">Student #{{ student.studentUserId }}</p>
              <h2>{{ student.studentName }}</h2>
            </div>
            <span class="student-gender">{{ student.gender || '未填写' }}</span>
          </div>
          <div class="student-meta">
            <span>学号 {{ student.studentNo || '-' }}</span>
            <span>{{ student.college || '学院未填' }}</span>
            <span>{{ student.grade || '年级未填' }}</span>
          </div>
          <div class="student-actions">
            <button class="ghost-button" type="button" @click="openReports(student.studentUserId)">查看报告</button>
            <button class="primary-button" type="button" @click="openAiSessions(student.studentUserId)">查看 AI 会话</button>
          </div>
        </article>
      </div>
    </div>
  </section>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700&family=Noto+Serif+SC:wght@400;500;600;700&display=swap');

.c-students-page {
  min-height: 100vh;
  padding: 44px 28px 72px;
  color: #283128;
  background: linear-gradient(180deg, #f5f0e5 0%, #f8f4ed 100%);
}

.page-shell {
  max-width: 1320px;
  margin: 0 auto;
}

.page-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) 220px;
  gap: 28px;
  align-items: end;
  margin-bottom: 30px;
}

.hero-copy { border-top: 1px solid rgba(59,69,59,.16); padding-top: 18px; }
.eyebrow, .student-code { margin: 0 0 10px; font: 700 .76rem/1 'Manrope', sans-serif; letter-spacing: .22em; text-transform: uppercase; color: #7b6857; }
.hero-copy h1, .student-card h2 { margin: 0; font-family: 'Noto Serif SC', serif; font-weight: 600; }
.hero-copy h1 { font-size: clamp(2rem, 3vw, 3.2rem); line-height: 1.16; }
.lead, .student-meta, .state-text, .error-text { font-family: 'Manrope', sans-serif; }
.lead { margin: 18px 0 0; max-width: 720px; line-height: 1.84; color: rgba(40,49,40,.72); }
.hero-metric, .student-card { border: 1px solid rgba(77,86,77,.14); background: rgba(255,252,247,.76); box-shadow: 0 24px 70px rgba(91,80,66,.08); backdrop-filter: blur(16px); }
.hero-metric { padding: 18px 20px; }
.hero-metric span { display:block; margin-bottom:8px; font:700 .78rem/1 'Manrope',sans-serif; letter-spacing:.16em; text-transform:uppercase; color:rgba(68,74,66,.56); }
.hero-metric strong { font: 600 1.6rem/1 'Noto Serif SC', serif; }
.student-grid { display:grid; grid-template-columns: repeat(2, minmax(0,1fr)); gap:18px; }
.student-card { padding: 22px; }
.student-card__topline { display:flex; justify-content:space-between; gap:16px; align-items:start; }
.student-card h2 { font-size: 1.34rem; line-height: 1.35; }
.student-gender { border:1px solid rgba(97,111,98,.15); background: rgba(242,244,237,.94); padding:8px 12px; font:700 .74rem/1 'Manrope',sans-serif; letter-spacing:.12em; text-transform:uppercase; color:#66735f; }
.student-meta { display:flex; flex-wrap:wrap; gap:10px 18px; margin-top:14px; font-size:.88rem; color:rgba(40,49,40,.62); }
.student-actions { display:flex; gap:12px; margin-top:18px; }
.ghost-button, .primary-button { border:1px solid rgba(54,65,56,.2); padding:12px 16px; font:700 .82rem/1 'Manrope',sans-serif; letter-spacing:.08em; text-transform:uppercase; cursor:pointer; }
.ghost-button { background: rgba(255,255,255,.58); color:#283128; }
.primary-button { border:none; background: linear-gradient(135deg,#253128 0%,#47564b 100%); color:#f8f5ef; }
.state-text, .error-text { font-size:.96rem; line-height:1.8; }
.error-text { color:#a44f46; }
@media (max-width: 900px) { .c-students-page{padding:28px 16px 46px;} .page-hero,.student-grid{grid-template-columns:1fr;} .student-card__topline,.student-actions{flex-direction:column; align-items:start;} }
</style>

