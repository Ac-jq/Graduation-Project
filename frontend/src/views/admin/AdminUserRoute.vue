<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { createCounselorApi, disableUserApi, enableUserApi, fetchAdminUsersApi, resetUserPasswordApi } from '@/api/user'
import type { AdminUserQuery, AdminUserSummary, CreateCounselorRequest } from '@/api/types'
import { toErrorMessage } from '@/views/shared/page-logic'

const loading = ref(false)
const processing = ref(false)
const errorMessage = ref('')
const users = ref<AdminUserSummary[]>([])
const filters = reactive<AdminUserQuery>({
  roleCode: undefined,
  status: undefined,
  keyword: ''
})
const createForm = reactive<CreateCounselorRequest>({
  account: '',
  displayName: '',
  realName: '',
  counselorNo: ''
})

async function loadUsers(): Promise<void> {
  loading.value = true
  errorMessage.value = ''

  try {
    users.value = await fetchAdminUsersApi(filters)
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    loading.value = false
  }
}

async function createCounselor(): Promise<void> {
  processing.value = true
  errorMessage.value = ''

  try {
    await createCounselorApi(createForm)
    await loadUsers()
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    processing.value = false
  }
}

async function enableUser(userId: number): Promise<void> {
  processing.value = true
  errorMessage.value = ''

  try {
    await enableUserApi(userId)
    await loadUsers()
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    processing.value = false
  }
}

async function disableUser(userId: number): Promise<void> {
  processing.value = true
  errorMessage.value = ''

  try {
    await disableUserApi(userId)
    await loadUsers()
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    processing.value = false
  }
}

async function resetPassword(userId: number): Promise<void> {
  processing.value = true
  errorMessage.value = ''

  try {
    await resetUserPasswordApi(userId)
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    processing.value = false
  }
}

onMounted(() => {
  void loadUsers()
})
</script>

<template>
  <section class="admin-user-page">
    <div class="page-shell">
      <header class="page-hero">
        <div class="hero-copy">
          <p class="eyebrow">User Governance</p>
          <h1>统一维护学生、咨询师与管理员账号状态，并快速创建新的咨询师账号。</h1>
          <p class="lead">当前页面会直接调用管理端用户接口，支持真实筛选、启停与密码重置动作。</p>
        </div>
      </header>

      <p v-if="errorMessage" class="error-text">{{ errorMessage }}</p>

      <div class="page-grid">
        <section class="create-panel glass-panel">
          <div class="section-head"><p class="section-kicker">Create Counselor</p><h2>新增咨询师</h2></div>
          <div class="form-grid">
            <label><span>账号</span><input v-model="createForm.account" type="text"></label>
            <label><span>显示名</span><input v-model="createForm.displayName" type="text"></label>
            <label><span>真实姓名</span><input v-model="createForm.realName" type="text"></label>
            <label><span>工号</span><input v-model="createForm.counselorNo" type="text"></label>
          </div>
          <button class="primary-button" type="button" :disabled="processing" @click="createCounselor">Create counselor</button>
        </section>

        <section class="list-panel glass-panel">
          <div class="section-head"><p class="section-kicker">Filters</p><h2>用户筛选</h2></div>
          <div class="filter-grid">
            <label><span>角色</span><input v-model="filters.roleCode" type="text" placeholder="STUDENT / COUNSELOR / ADMIN"></label>
            <label><span>状态</span><input v-model="filters.status" type="text" placeholder="启用 / 停用"></label>
            <label class="filter-wide"><span>关键词</span><input v-model="filters.keyword" type="text" placeholder="账号、显示名、学号"></label>
          </div>
          <button class="ghost-button" type="button" @click="loadUsers">刷新用户列表</button>

          <p v-if="loading" class="state-text">正在同步用户列表...</p>
          <div v-else class="user-stack">
            <article v-for="user in users" :key="user.userId" class="user-card">
              <div class="user-topline">
                <div>
                  <p class="user-code">User #{{ user.userId }}</p>
                  <h3>{{ user.displayName }}</h3>
                </div>
                <span class="status-pill">{{ user.status }}</span>
              </div>
              <div class="user-meta">
                <span>{{ user.roleCode }}</span>
                <span>{{ user.account }}</span>
                <span v-if="user.studentNo">学号 {{ user.studentNo }}</span>
                <span v-if="user.counselorNo">工号 {{ user.counselorNo }}</span>
              </div>
              <div class="action-row">
                <button class="ghost-button" type="button" :disabled="processing" @click="resetPassword(user.userId)">Reset password</button>
                <button v-if="user.status !== 'ACTIVE'" class="ghost-button" type="button" :disabled="processing" @click="enableUser(user.userId)">启用</button>
                <button v-else class="danger-button" type="button" :disabled="processing" @click="disableUser(user.userId)">停用</button>
              </div>
            </article>
          </div>
        </section>
      </div>
    </div>
  </section>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700&family=Noto+Serif+SC:wght@400;500;600;700&display=swap');
.admin-user-page{min-height:100vh;padding:44px 28px 72px;color:#272f27;background:linear-gradient(180deg,#f4efe6 0%,#f8f4ed 100%)}.page-shell{max-width:1320px;margin:0 auto}.hero-copy{border-top:1px solid rgba(59,69,59,.16);padding-top:18px;margin-bottom:28px}.eyebrow,.section-kicker,.user-code,.form-grid span,.filter-grid span{margin:0 0 10px;font:700 .76rem/1 'Manrope',sans-serif;letter-spacing:.22em;text-transform:uppercase;color:#7b6857}.hero-copy h1,.section-head h2,.user-card h3{margin:0;font-family:'Noto Serif SC',serif;font-weight:600}.hero-copy h1{font-size:clamp(2rem,3vw,3.3rem);line-height:1.16}.lead,.error-text,.user-meta,.state-text,input{font-family:'Manrope',sans-serif}.lead{margin:18px 0 0;max-width:760px;line-height:1.84;color:rgba(39,47,39,.72)}.error-text{margin-bottom:16px;color:#a44f46}.page-grid{display:grid;grid-template-columns:minmax(320px,.78fr) minmax(0,1.22fr);gap:28px}.glass-panel,.user-card{border:1px solid rgba(77,86,77,.14);background:rgba(255,252,247,.76);box-shadow:0 24px 70px rgba(91,80,66,.08);backdrop-filter:blur(16px)}.create-panel,.list-panel{padding:24px}.section-head{margin-bottom:18px}.form-grid,.filter-grid{display:grid;gap:14px}.form-grid label,.filter-grid label{display:grid;gap:8px}.filter-grid{grid-template-columns:repeat(2,minmax(0,1fr));margin-bottom:16px}.filter-wide{grid-column:1/-1}input{width:100%;box-sizing:border-box;border:1px solid rgba(80,88,79,.16);background:rgba(255,255,255,.74);padding:14px 16px;color:#272f27;outline:none}.primary-button,.ghost-button,.danger-button{padding:12px 16px;font:700 .82rem/1 'Manrope',sans-serif;letter-spacing:.08em;text-transform:uppercase;cursor:pointer}.primary-button{margin-top:16px;border:none;background:linear-gradient(135deg,#253128 0%,#47564b 100%);color:#f8f5ef}.ghost-button{border:1px solid rgba(54,65,56,.2);background:rgba(255,255,255,.58);color:#272f27}.danger-button{border:1px solid rgba(164,79,70,.18);background:rgba(164,79,70,.08);color:#a44f46}.user-stack{display:grid;gap:16px}.user-card{padding:18px}.user-topline{display:flex;justify-content:space-between;gap:16px;align-items:start}.status-pill{border:1px solid rgba(97,111,98,.15);background:rgba(242,244,237,.94);padding:8px 12px;font:700 .74rem/1 'Manrope',sans-serif;letter-spacing:.12em;text-transform:uppercase;color:#66735f}.user-meta{display:flex;flex-wrap:wrap;gap:10px 18px;margin-top:12px;font-size:.86rem;color:rgba(39,47,39,.64)}.action-row{display:flex;flex-wrap:wrap;gap:12px;margin-top:16px}
@media (max-width:980px){.admin-user-page{padding:28px 16px 46px}.page-grid,.filter-grid{grid-template-columns:1fr}}
</style>

