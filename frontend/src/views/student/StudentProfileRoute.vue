<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { fetchStudentProfileApi, updateStudentProfileApi } from '@/api/user'
import type { StudentProfile, UpdateStudentProfileRequest } from '@/api/types'
import { toErrorMessage } from '@/views/shared/page-logic'

const loading = ref(false)
const saving = ref(false)
const errorMessage = ref('')
const profile = ref<StudentProfile | null>(null)
const form = reactive<UpdateStudentProfileRequest>({
  avatarUrl: null,
  college: null,
  grade: null,
  gender: null,
  phone: null,
  emergencyContact: null,
  emergencyPhone: null
})

function syncForm(data: StudentProfile): void {
  form.avatarUrl = data.avatarUrl
  form.college = data.college
  form.grade = data.grade
  form.gender = data.gender
  form.phone = data.phone
  form.emergencyContact = data.emergencyContact
  form.emergencyPhone = data.emergencyPhone
}

async function loadProfile(): Promise<void> {
  loading.value = true
  errorMessage.value = ''

  try {
    const data = await fetchStudentProfileApi()
    profile.value = data
    syncForm(data)
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    loading.value = false
  }
}

async function saveProfile(): Promise<void> {
  saving.value = true
  errorMessage.value = ''

  try {
    const data = await updateStudentProfileApi({ ...form })
    profile.value = data
    syncForm(data)
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  void loadProfile()
})
</script>

<template>
  <main class="profile-page">
    <div class="profile-page__texture" />

    <section class="profile-page__hero">
      <div class="profile-page__identity">
        <p class="profile-page__eyebrow">学生档案</p>
        <h1 class="profile-page__name">
          {{ profile?.displayName || '个人档案' }}
        </h1>
        <p class="profile-page__lead">
          {{ profile?.realName || '在此维护你的基础信息与紧急联系资料，便于测评、预约与后续支持流程保持连贯。' }}
        </p>
      </div>

      <aside class="profile-page__stamp">
        <div class="profile-page__avatar-shell">
          <img
            v-if="form.avatarUrl"
            :src="form.avatarUrl"
            alt="avatar"
            class="profile-page__avatar"
          >
          <span v-else>{{ (profile?.displayName || 'P').slice(0, 1) }}</span>
        </div>
        <dl class="profile-page__meta">
          <div>
            <dt>账号</dt>
            <dd>{{ profile?.account || '—' }}</dd>
          </div>
          <div>
            <dt>学号</dt>
            <dd>{{ profile?.studentNo || '—' }}</dd>
          </div>
          <div>
            <dt>咨询师编号</dt>
            <dd>{{ profile?.counselorUserId || '—' }}</dd>
          </div>
        </dl>
      </aside>
    </section>

    <section class="profile-page__layout">
      <article class="profile-page__panel profile-page__panel--form">
        <header class="profile-page__panel-head">
          <p class="profile-page__panel-kicker">可编辑信息</p>
          <p class="profile-page__panel-copy">更新会直接影响当前账号在预约、通知与辅导流程中的可识别信息。</p>
        </header>

        <div v-if="errorMessage" class="profile-page__alert profile-page__alert--error">
          {{ errorMessage }}
        </div>

        <div class="profile-page__form-grid">
          <label class="profile-field">
            <span>头像地址</span>
            <input v-model="form.avatarUrl" type="url" placeholder="https://example.com/avatar.png">
          </label>

          <label class="profile-field">
            <span>学院</span>
            <input v-model="form.college" type="text" placeholder="软件学院">
          </label>

          <label class="profile-field">
            <span>年级</span>
            <input v-model="form.grade" type="text" placeholder="2023级">
          </label>

          <label class="profile-field">
            <span>性别</span>
            <input v-model="form.gender" type="text" placeholder="男 / 女 / 其他">
          </label>

          <label class="profile-field">
            <span>手机号</span>
            <input v-model="form.phone" type="text" placeholder="13800000000">
          </label>

          <label class="profile-field">
            <span>紧急联系人</span>
            <input v-model="form.emergencyContact" type="text" placeholder="联系人姓名">
          </label>

          <label class="profile-field profile-field--wide">
            <span>紧急联系电话</span>
            <input v-model="form.emergencyPhone" type="text" placeholder="紧急联系电话">
          </label>
        </div>

        <footer class="profile-page__form-footer">
          <button class="profile-page__action" type="button" :disabled="saving" @click="saveProfile">
            {{ saving ? '保存中...' : '保存档案' }}
          </button>
        </footer>
      </article>

      <article class="profile-page__panel profile-page__panel--summary">
        <header class="profile-page__panel-head">
          <p class="profile-page__panel-kicker">只读身份信息</p>
          <p class="profile-page__panel-copy">以下字段由系统认证与学籍侧维护，当前页面不提供编辑。</p>
        </header>

        <div v-if="loading" class="profile-page__status">正在同步个人档案...</div>

        <dl v-else class="profile-page__readonly-grid">
          <div>
            <dt>显示名称</dt>
            <dd>{{ profile?.displayName || '—' }}</dd>
          </div>
          <div>
            <dt>真实姓名</dt>
            <dd>{{ profile?.realName || '—' }}</dd>
          </div>
          <div>
            <dt>学院</dt>
            <dd>{{ profile?.college || '—' }}</dd>
          </div>
          <div>
            <dt>年级</dt>
            <dd>{{ profile?.grade || '—' }}</dd>
          </div>
          <div>
            <dt>手机号</dt>
            <dd>{{ profile?.phone || '—' }}</dd>
          </div>
          <div>
            <dt>紧急联系人</dt>
            <dd>{{ profile?.emergencyContact || '—' }}</dd>
          </div>
        </dl>
      </article>
    </section>
  </main>
</template>
<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700&family=Noto+Serif+SC:wght@400;500;600;700&display=swap');

.profile-page {
  --paper: #f7f2ea;
  --ink: #1f1b17;
  --muted: #6e665d;
  --line: rgba(31, 27, 23, 0.11);
  --accent: #6f8170;
  position: relative;
  min-height: 100vh;
  padding: 2rem 2rem 3rem;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.2), transparent 38%),
    radial-gradient(circle at 10% 0%, rgba(173, 164, 138, 0.18), transparent 28%),
    var(--paper);
  color: var(--ink);
  overflow: hidden;
}

.profile-page__texture {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(90deg, transparent 0, transparent 58%, rgba(31, 27, 23, 0.04) 58%, rgba(31, 27, 23, 0.04) 58.2%, transparent 58.2%),
    radial-gradient(rgba(31, 27, 23, 0.06) 0.7px, transparent 0.7px);
  background-size: auto, 15px 15px;
  opacity: 0.2;
  pointer-events: none;
}

.profile-page__hero,
.profile-page__layout {
  position: relative;
  z-index: 1;
}

.profile-page__hero {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(280px, 0.8fr);
  gap: 2rem;
  align-items: end;
  padding-bottom: 1.8rem;
  border-bottom: 1px solid var(--line);
}

.profile-page__eyebrow,
.profile-page__panel-kicker,
.profile-field span,
.profile-page__readonly-grid dt,
.profile-page__meta dt {
  margin: 0;
  font: 600 0.74rem/1.4 'Manrope', sans-serif;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--muted);
}

.profile-page__name {
  margin: 0.9rem 0 0;
  font: 600 clamp(2.4rem, 4.4vw, 4.9rem)/1 'Noto Serif SC', 'Source Han Serif SC', serif;
  letter-spacing: 0.02em;
}

.profile-page__lead {
  max-width: 38rem;
  margin: 1.3rem 0 0;
  font: 400 1.02rem/1.95 'Noto Serif SC', 'Source Han Serif SC', serif;
  color: var(--muted);
}

.profile-page__stamp {
  display: grid;
  gap: 1rem;
  align-content: start;
  padding: 1.2rem;
  border: 1px solid var(--line);
  background: rgba(255, 251, 245, 0.6);
  backdrop-filter: blur(18px);
}

.profile-page__avatar-shell {
  width: 5.8rem;
  height: 5.8rem;
  display: grid;
  place-items: center;
  border-radius: 50%;
  border: 1px solid rgba(31, 27, 23, 0.12);
  background: linear-gradient(135deg, rgba(111, 129, 112, 0.16), rgba(255, 255, 255, 0.88));
  font: 600 1.7rem/1 'Noto Serif SC', 'Source Han Serif SC', serif;
  color: var(--ink);
  overflow: hidden;
}

.profile-page__avatar {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.profile-page__meta {
  display: grid;
  gap: 0.8rem;
  margin: 0;
}

.profile-page__meta dd {
  margin: 0.45rem 0 0;
  font: 500 0.96rem/1.7 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.profile-page__layout {
  margin-top: 1.8rem;
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) minmax(320px, 0.85fr);
  gap: 1.6rem;
}

.profile-page__panel {
  padding: 1.4rem;
  border: 1px solid var(--line);
  background: rgba(255, 252, 246, 0.68);
  backdrop-filter: blur(20px);
}

.profile-page__panel-head {
  padding-bottom: 1rem;
  border-bottom: 1px solid var(--line);
}

.profile-page__panel-copy {
  margin: 0.8rem 0 0;
  font: 400 0.95rem/1.8 'Noto Serif SC', 'Source Han Serif SC', serif;
  color: var(--muted);
}

.profile-page__alert {
  margin-top: 1.2rem;
  padding: 0.95rem 1rem;
  border: 1px solid rgba(138, 73, 73, 0.2);
  background: rgba(138, 73, 73, 0.07);
  color: #7f4040;
  font: 500 0.92rem/1.6 'Manrope', sans-serif;
}

.profile-page__form-grid,
.profile-page__readonly-grid {
  margin-top: 1.35rem;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem 1.15rem;
}

.profile-field {
  display: grid;
  gap: 0.55rem;
}

.profile-field--wide {
  grid-column: 1 / -1;
}

.profile-field input {
  width: 100%;
  box-sizing: border-box;
  padding: 0.9rem 0.95rem;
  border: 1px solid rgba(31, 27, 23, 0.14);
  background: rgba(255, 255, 255, 0.74);
  font: 500 0.95rem/1.4 'Manrope', sans-serif;
  color: var(--ink);
}

.profile-field input:focus {
  outline: none;
  border-color: var(--accent);
  background: rgba(255, 255, 255, 0.96);
}

.profile-page__form-footer {
  margin-top: 1.4rem;
  display: flex;
  justify-content: flex-end;
}

.profile-page__action {
  min-width: 10rem;
  min-height: 3.15rem;
  border: none;
  background: linear-gradient(135deg, #6f8170, #536355);
  color: #f8f3eb;
  font: 600 0.92rem/1 'Manrope', sans-serif;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  cursor: pointer;
  transition: transform 180ms ease, box-shadow 180ms ease;
  box-shadow: 0 18px 30px rgba(83, 99, 85, 0.2);
}

.profile-page__action:hover:not(:disabled) {
  transform: translateY(-2px);
}

.profile-page__action:disabled {
  cursor: wait;
  opacity: 0.72;
}

.profile-page__status {
  margin-top: 1.2rem;
  color: var(--muted);
  font: 500 0.95rem/1.7 'Manrope', sans-serif;
}

.profile-page__readonly-grid dd {
  margin: 0.45rem 0 0;
  font: 500 1rem/1.8 'Noto Serif SC', 'Source Han Serif SC', serif;
}

@media (max-width: 980px) {
  .profile-page {
    padding: 1rem;
  }

  .profile-page__hero,
  .profile-page__layout,
  .profile-page__form-grid,
  .profile-page__readonly-grid {
    grid-template-columns: 1fr;
  }
}
</style>

