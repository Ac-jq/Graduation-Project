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

function resolveRoleLabel(roleCode?: string): string {
  switch (roleCode) {
    case 'STUDENT':
      return '学生'
    case 'COUNSELOR':
      return '咨询师'
    case 'ADMIN':
      return '管理员'
    default:
      return roleCode || '未知角色'
  }
}

function resolveStatusLabel(status?: string): string {
  return status === 'ACTIVE' ? '启用中' : status === 'DISABLED' ? '已停用' : status || '未标记'
}

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
  <section class="admin-editorial-page">
    <div class="admin-editorial-shell">
      <header class="admin-editorial-hero">
        <div class="admin-editorial-copy">
          <p class="admin-editorial-eyebrow">用户治理</p>
          <h1 class="admin-editorial-title">维护学生、咨询师与管理员账号，让身份与状态始终清晰可控。</h1>
          <p class="admin-editorial-lead">
            页面逻辑保持不变，仍然使用原有用户接口完成筛选、创建、启停和密码重置，这里只统一阅读体验和视觉层次。
          </p>
        </div>

        <div class="admin-editorial-hero-side">
          <article class="admin-editorial-stat">
            <p class="admin-editorial-label">当前结果</p>
            <strong>{{ users.length }}</strong>
            <p class="admin-editorial-lead">已载入符合筛选条件的账号数量。</p>
          </article>
        </div>
      </header>

      <p v-if="errorMessage" class="admin-editorial-alert">{{ errorMessage }}</p>

      <div class="admin-editorial-grid">
        <section class="admin-editorial-panel admin-editorial-panel--mesh">
          <div class="admin-editorial-section">
            <p class="admin-editorial-kicker">新增咨询师</p>
            <h2>先完善基础信息，再直接创建账号</h2>
          </div>

          <div class="admin-editorial-form">
            <label class="admin-editorial-field">
              <span>账号</span>
              <input v-model="createForm.account" type="text">
            </label>
            <label class="admin-editorial-field">
              <span>显示名</span>
              <input v-model="createForm.displayName" type="text">
            </label>
            <label class="admin-editorial-field">
              <span>真实姓名</span>
              <input v-model="createForm.realName" type="text">
            </label>
            <label class="admin-editorial-field">
              <span>工号</span>
              <input v-model="createForm.counselorNo" type="text">
            </label>
          </div>

          <div class="admin-editorial-card" style="margin-top: 1rem;">
            <p class="admin-editorial-code">创建提醒</p>
            <p>账号、显示名、真实姓名和工号会直接提交到原有创建咨询师接口，提交成功后下方列表会立即刷新。</p>
          </div>

          <div class="admin-editorial-actions" style="margin-top: 1rem;">
            <button class="admin-editorial-button" type="button" :disabled="processing" @click="createCounselor">创建咨询师</button>
          </div>
        </section>

        <section class="admin-editorial-panel">
          <div class="admin-editorial-section">
            <p class="admin-editorial-kicker">用户筛选</p>
            <h2>按角色、状态和关键词收窄列表</h2>
          </div>

          <div class="admin-editorial-form">
            <label class="admin-editorial-field">
              <span>角色</span>
              <input v-model="filters.roleCode" type="text" placeholder="STUDENT / COUNSELOR / ADMIN">
            </label>
            <label class="admin-editorial-field">
              <span>状态</span>
              <input v-model="filters.status" type="text" placeholder="ACTIVE / DISABLED">
            </label>
            <label class="admin-editorial-field wide">
              <span>关键词</span>
              <input v-model="filters.keyword" type="text" placeholder="账号、显示名、学号或工号">
            </label>
          </div>

          <div class="admin-editorial-actions" style="margin-top: 1rem;">
            <button class="admin-editorial-ghost" type="button" @click="loadUsers">刷新用户列表</button>
          </div>

          <div v-if="loading" class="admin-editorial-empty">正在同步用户列表…</div>
          <div v-else class="admin-editorial-stack" style="margin-top: 1rem;">
            <article v-for="user in users" :key="user.userId" class="admin-editorial-card">
              <div class="admin-editorial-card__topline">
                <div>
                  <p class="admin-editorial-code">用户 #{{ user.userId }}</p>
                  <h3>{{ user.displayName }}</h3>
                </div>
                <span class="admin-editorial-status">{{ resolveStatusLabel(user.status) }}</span>
              </div>

              <div class="admin-editorial-meta">
                <span>{{ resolveRoleLabel(user.roleCode) }}</span>
                <span>{{ user.account }}</span>
                <span v-if="user.studentNo">学号 {{ user.studentNo }}</span>
                <span v-if="user.counselorNo">工号 {{ user.counselorNo }}</span>
              </div>

              <div class="admin-editorial-actions" style="margin-top: 1rem;">
                <button class="admin-editorial-ghost" type="button" :disabled="processing" @click="resetPassword(user.userId)">重置密码</button>
                <button
                  v-if="user.status !== 'ACTIVE'"
                  class="admin-editorial-button"
                  type="button"
                  :disabled="processing"
                  @click="enableUser(user.userId)"
                >
                  启用账号
                </button>
                <button
                  v-else
                  class="admin-editorial-danger"
                  type="button"
                  :disabled="processing"
                  @click="disableUser(user.userId)"
                >
                  停用账号
                </button>
              </div>
            </article>
          </div>
        </section>
      </div>
    </div>
  </section>
</template>

<style scoped>
@import './admin-editorial.css';
</style>
