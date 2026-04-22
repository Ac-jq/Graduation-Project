<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  createAdminUserApi,
  deleteAdminUserApi,
  disableUserApi,
  enableUserApi,
  fetchAdminUsersApi,
  resetUserPasswordApi,
  updateAdminUserApi
} from '@/api/user'
import type { AdminUserQuery, AdminUserSummary, CreateAdminUserRequest, UpdateAdminUserRequest } from '@/api/types'
import { toErrorMessage } from '@/views/shared/page-logic'

interface AdminUserEditForm {
  userId: number
  account: string
  roleCode: string
  realName: string
  displayName: string
  studentNo: string
  counselorNo: string
  status: string
  college: string
  grade: string
  phone: string
  password: string
  createdAt: string
}

const loading = ref(false)
const processing = ref(false)
const errorMessage = ref('')
const users = ref<AdminUserSummary[]>([])
const currentPage = ref(1)
const pageSize = 10
const showCreatePanel = ref(false)
const editDialogVisible = ref(false)

const filters = reactive<AdminUserQuery>({
  roleCode: undefined,
  status: undefined,
  keyword: '',
  grade: undefined,
  college: undefined
})

const gradeOptions = ['2022', '2023', '2025', '2026']
const collegeOptions = [
  '计算机科学与技术学院',
  '软件学院',
  '人工智能学院',
  '医学院',
  '法学院',
  '经济管理学院',
  '外国语学院',
  '文学院',
  '理学院',
  '工学院',
  '艺术学院',
  '建筑与城规学院',
  '机械工程学院',
  '电子信息工程学院'
]

const createForm = reactive<CreateAdminUserRequest>({
  account: '',
  roleCode: 'STUDENT',
  displayName: '',
  realName: '',
  studentNo: '',
  counselorNo: '',
  college: '',
  grade: '',
  phone: '',
  password: ''
})

const editForm = reactive<AdminUserEditForm>({
  userId: 0,
  account: '',
  roleCode: '',
  realName: '',
  displayName: '',
  studentNo: '',
  counselorNo: '',
  status: '',
  college: '',
  grade: '',
  phone: '',
  password: '',
  createdAt: ''
})

const totalPages = computed(() => Math.max(1, Math.ceil(users.value.length / pageSize)))
const pagedUsers = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return users.value.slice(start, start + pageSize)
})

const editIdentityLabel = computed(() => {
  if (editForm.roleCode === 'COUNSELOR') {
    return '工号'
  }
  if (editForm.roleCode === 'STUDENT') {
    return '学号'
  }
  return '编号'
})

const editIdentityValue = computed({
  get: () => (editForm.roleCode === 'COUNSELOR' ? editForm.counselorNo : editForm.studentNo),
  set: (value: string) => {
    if (editForm.roleCode === 'COUNSELOR') {
      editForm.counselorNo = value
      return
    }
    editForm.studentNo = value
  }
})

const isStudentEditing = computed(() => editForm.roleCode === 'STUDENT')
const isCreatingStudent = computed(() => createForm.roleCode === 'STUDENT')
const isCreatingCounselor = computed(() => createForm.roleCode === 'COUNSELOR')

function rowIndex(index: number): number {
  return (currentPage.value - 1) * pageSize + index + 1
}

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
  return status === 'ACTIVE' ? '正常' : status === 'DISABLED' ? '禁用' : status || '未标记'
}

function formatDate(value?: string): string {
  if (!value) {
    return '--'
  }

  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value))
}

function resetCreateForm(): void {
  createForm.account = ''
  createForm.roleCode = 'STUDENT'
  createForm.displayName = ''
  createForm.realName = ''
  createForm.studentNo = ''
  createForm.counselorNo = ''
  createForm.college = ''
  createForm.grade = ''
  createForm.phone = ''
  createForm.password = ''
}

function fillEditForm(user: AdminUserSummary): void {
  const snapshot = JSON.parse(JSON.stringify(user)) as AdminUserSummary
  editForm.userId = snapshot.userId
  editForm.account = snapshot.account || ''
  editForm.roleCode = snapshot.roleCode || ''
  editForm.realName = snapshot.realName || ''
  editForm.displayName = snapshot.displayName || ''
  editForm.studentNo = snapshot.studentNo || ''
  editForm.counselorNo = snapshot.counselorNo || ''
  editForm.status = snapshot.status || ''
  editForm.college = snapshot.college || ''
  editForm.grade = snapshot.grade || ''
  editForm.phone = snapshot.phone || ''
  editForm.password = ''
  editForm.createdAt = snapshot.createdAt || ''
}

function buildEditPayload(): UpdateAdminUserRequest {
  return {
    account: editForm.account.trim(),
    displayName: editForm.displayName.trim(),
    realName: editForm.realName.trim() || null,
    studentNo: editForm.roleCode === 'STUDENT' ? editForm.studentNo.trim() || null : null,
    counselorNo: editForm.roleCode === 'COUNSELOR' ? editForm.counselorNo.trim() || null : null,
    college: editForm.roleCode === 'STUDENT' ? editForm.college.trim() || null : null,
    grade: editForm.roleCode === 'STUDENT' ? editForm.grade.trim() || null : null,
    phone: editForm.roleCode === 'STUDENT' ? editForm.phone.trim() || null : null,
    password: editForm.password.trim() || null
  }
}

async function loadUsers(): Promise<void> {
  loading.value = true
  errorMessage.value = ''

  try {
    users.value = await fetchAdminUsersApi(filters)
    currentPage.value = 1
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    loading.value = false
  }
}

function buildCreatePayload(): CreateAdminUserRequest {
  return {
    account: createForm.account.trim(),
    roleCode: createForm.roleCode,
    displayName: createForm.displayName.trim(),
    realName: createForm.realName?.trim() || null,
    studentNo: createForm.roleCode === 'STUDENT' ? createForm.studentNo?.trim() || null : null,
    counselorNo: createForm.roleCode === 'COUNSELOR' ? createForm.counselorNo?.trim() || null : null,
    college: createForm.roleCode === 'STUDENT' ? createForm.college?.trim() || null : null,
    grade: createForm.roleCode === 'STUDENT' ? createForm.grade?.trim() || null : null,
    phone: createForm.roleCode === 'STUDENT' ? createForm.phone?.trim() || null : null,
    password: createForm.password?.trim() || null
  }
}

async function createUser(): Promise<void> {
  if (!createForm.account.trim()) {
    ElMessage.warning('账号不能为空')
    return
  }

  if (!createForm.displayName.trim()) {
    ElMessage.warning('显示名不能为空')
    return
  }

  if (isCreatingStudent.value) {
    if (!createForm.studentNo?.trim()) {
      ElMessage.warning('学生学号不能为空')
      return
    }
    if (!createForm.college?.trim()) {
      ElMessage.warning('学生学院不能为空')
      return
    }
    if (!createForm.grade?.trim()) {
      ElMessage.warning('学生年级不能为空')
      return
    }
  }

  if (isCreatingCounselor.value && !createForm.counselorNo?.trim()) {
    ElMessage.warning('咨询师工号不能为空')
    return
  }

  processing.value = true
  errorMessage.value = ''

  try {
    await createAdminUserApi(buildCreatePayload())
    resetCreateForm()
    showCreatePanel.value = false
    await loadUsers()
    ElMessage.success('用户已创建')
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
    ElMessage.success('用户已启用')
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
    ElMessage.success('用户已禁用')
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
    ElMessage.success('密码已重置为默认密码')
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    processing.value = false
  }
}

function startCreateUser(): void {
  resetCreateForm()
  showCreatePanel.value = !showCreatePanel.value
}

function startEdit(user: AdminUserSummary): void {
  errorMessage.value = ''
  fillEditForm(user)
  editDialogVisible.value = true
}

async function removeUser(user: AdminUserSummary): Promise<void> {
  const confirmed = window.confirm(`确认删除用户“${user.displayName}”吗？删除后不可恢复。若该账号已有测评、预约或聊天等业务数据，系统会阻止删除并提示改为禁用。`)
  if (!confirmed) {
    return
  }

  processing.value = true
  errorMessage.value = ''

  try {
    await deleteAdminUserApi(user.userId)
    await loadUsers()
    ElMessage.success('用户已删除')
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    processing.value = false
  }
}

function closeEditDialog(): void {
  editDialogVisible.value = false
  editForm.password = ''
}

async function saveEdit(): Promise<void> {
  const account = editForm.account.trim()
  const displayName = editForm.displayName.trim()

  if (!account) {
    ElMessage.warning('账号不能为空')
    return
  }

  if (!displayName) {
    ElMessage.warning('显示名不能为空')
    return
  }

  if (editForm.roleCode === 'STUDENT' && !editForm.studentNo.trim()) {
    ElMessage.warning('学号不能为空')
    return
  }

  if (editForm.roleCode === 'COUNSELOR' && !editForm.counselorNo.trim()) {
    ElMessage.warning('工号不能为空')
    return
  }

  processing.value = true
  errorMessage.value = ''

  try {
    const updatedUser = await updateAdminUserApi(editForm.userId, buildEditPayload())
    const targetIndex = users.value.findIndex((user) => user.userId === updatedUser.userId)

    if (targetIndex >= 0) {
      users.value[targetIndex] = updatedUser
    } else {
      await loadUsers()
    }

    editDialogVisible.value = false
    editForm.password = ''
    ElMessage.success('用户信息已更新')
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    processing.value = false
  }
}

function resetFilters(): void {
  filters.roleCode = undefined
  filters.status = undefined
  filters.keyword = ''
  filters.grade = undefined
  filters.college = undefined
  void loadUsers()
}

function exportUsers(): void {
  const header = ['账号', '姓名', '角色', '状态', '学号', '工号', '学院', '年级', '创建时间']
  const rows = users.value.map((user) => [
    user.account,
    user.displayName,
    resolveRoleLabel(user.roleCode),
    resolveStatusLabel(user.status),
    user.studentNo || '',
    user.counselorNo || '',
    user.college || '',
    user.grade || '',
    formatDate(user.createdAt)
  ])

  const csv = [header, ...rows]
    .map((row) => row.map((cell) => `"${String(cell).replace(/"/g, '""')}"`).join(','))
    .join('\n')

  const blob = new Blob([`\uFEFF${csv}`], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = 'admin-users.csv'
  link.click()
  URL.revokeObjectURL(url)
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

onMounted(() => {
  void loadUsers()
})
</script>

<template>
  <section class="admin-user-page">
    <div class="admin-user-shell">
      <header class="admin-user-header">
        <div>
          <h1>用户管理</h1>
          <p>查看账号状态、筛选用户并执行常用后台操作。</p>
        </div>
      </header>

      <p v-if="errorMessage" class="admin-user-alert">{{ errorMessage }}</p>

      <section class="admin-user-toolbar">
        <div class="toolbar-filters">
          <label class="toolbar-field toolbar-field--keyword">
            <span>搜索</span>
            <input
              v-model="filters.keyword"
              type="text"
              placeholder="账号 / 姓名 / 学号 / 工号"
              @keyup.enter="loadUsers"
            >
          </label>

          <label class="toolbar-field">
            <span>角色</span>
            <select v-model="filters.roleCode">
              <option :value="undefined">全部</option>
              <option value="STUDENT">学生</option>
              <option value="COUNSELOR">咨询师</option>
              <option value="ADMIN">管理员</option>
            </select>
          </label>

          <label class="toolbar-field">
            <span>状态</span>
            <select v-model="filters.status">
              <option :value="undefined">全部</option>
              <option value="ACTIVE">正常</option>
              <option value="DISABLED">禁用</option>
            </select>
          </label>

          <label class="toolbar-field">
            <span>年级</span>
            <select v-model="filters.grade">
              <option :value="undefined">全部</option>
              <option v-for="grade in gradeOptions" :key="grade" :value="grade">{{ grade }}</option>
            </select>
          </label>

          <label class="toolbar-field toolbar-field--college">
            <span>学院</span>
            <select v-model="filters.college">
              <option :value="undefined">全部</option>
              <option v-for="college in collegeOptions" :key="college" :value="college">{{ college }}</option>
            </select>
          </label>

          <button class="toolbar-btn toolbar-btn--primary" type="button" @click="loadUsers">
            查询
          </button>
          <button class="toolbar-btn" type="button" @click="resetFilters">
            重置
          </button>
        </div>

        <div class="toolbar-actions">
          <button class="toolbar-btn toolbar-btn--primary" type="button" @click="startCreateUser">
            + 新增用户
          </button>
          <button class="toolbar-btn" type="button" @click="exportUsers">
            批量导出
          </button>
        </div>
      </section>

      <section v-if="showCreatePanel" class="admin-user-create-panel">
        <div class="create-panel-header">
          <div>
            <h2>新增用户</h2>
            <p>根据角色填写必要身份信息，保存后账号默认启用。</p>
          </div>
          <button class="toolbar-btn" type="button" @click="showCreatePanel = false">收起</button>
        </div>

        <div class="create-panel-form">
          <label class="toolbar-field">
            <span>用户角色</span>
            <select v-model="createForm.roleCode">
              <option value="STUDENT">学生</option>
              <option value="COUNSELOR">咨询师</option>
              <option value="ADMIN">系统管理员</option>
            </select>
          </label>
          <label class="toolbar-field">
            <span>账号</span>
            <input v-model="createForm.account" type="text">
          </label>
          <label class="toolbar-field">
            <span>显示名</span>
            <input v-model="createForm.displayName" type="text">
          </label>
          <label class="toolbar-field">
            <span>真实姓名</span>
            <input v-model="createForm.realName" type="text">
          </label>
          <label class="toolbar-field">
            <span>登录密码</span>
            <input v-model="createForm.password" type="password" placeholder="留空使用默认密码">
          </label>

          <template v-if="isCreatingStudent">
            <label class="toolbar-field">
              <span>学号</span>
              <input v-model="createForm.studentNo" type="text">
            </label>
            <label class="toolbar-field toolbar-field--college">
              <span>学院</span>
              <select v-model="createForm.college">
                <option value="">请选择学院</option>
                <option v-for="college in collegeOptions" :key="college" :value="college">{{ college }}</option>
              </select>
            </label>
            <label class="toolbar-field">
              <span>年级</span>
              <select v-model="createForm.grade">
                <option value="">请选择年级</option>
                <option v-for="grade in gradeOptions" :key="grade" :value="grade">{{ grade }}</option>
              </select>
            </label>
            <label class="toolbar-field">
              <span>手机号</span>
              <input v-model="createForm.phone" type="text">
            </label>
          </template>

          <label v-if="isCreatingCounselor" class="toolbar-field">
            <span>工号</span>
            <input v-model="createForm.counselorNo" type="text">
          </label>
        </div>

        <div class="create-panel-actions">
          <button
            class="toolbar-btn toolbar-btn--primary"
            type="button"
            :disabled="processing"
            @click="createUser"
          >
            {{ processing ? '提交中...' : '保存' }}
          </button>
        </div>
      </section>

      <el-dialog
        v-model="editDialogVisible"
        title="编辑用户信息"
        width="640px"
        destroy-on-close
        class="admin-user-edit-dialog"
      >
        <el-form label-position="top" class="edit-dialog-form">
          <div class="edit-dialog-grid">
            <el-form-item label="账号">
              <el-input v-model="editForm.account" />
            </el-form-item>

            <el-form-item label="角色信息">
              <el-input :model-value="resolveRoleLabel(editForm.roleCode)" disabled />
            </el-form-item>

            <el-form-item label="显示名">
              <el-input v-model="editForm.displayName" />
            </el-form-item>

            <el-form-item label="真实姓名">
              <el-input v-model="editForm.realName" />
            </el-form-item>

            <el-form-item :label="editIdentityLabel" v-if="editForm.roleCode !== 'ADMIN'">
              <el-input v-model="editIdentityValue" />
            </el-form-item>

            <el-form-item label="登录密码">
              <el-input
                v-model="editForm.password"
                type="password"
                show-password
                placeholder="留空则不修改密码"
              />
            </el-form-item>

            <template v-if="isStudentEditing">
              <el-form-item label="学院">
                <el-input v-model="editForm.college" />
              </el-form-item>

              <el-form-item label="年级">
                <el-input v-model="editForm.grade" />
              </el-form-item>

              <el-form-item label="手机号" class="is-wide">
                <el-input v-model="editForm.phone" />
              </el-form-item>
            </template>
          </div>
        </el-form>

        <template #footer>
          <div class="edit-dialog-footer">
            <el-button @click="closeEditDialog">取消</el-button>
            <el-button type="primary" :loading="processing" @click="saveEdit">保存</el-button>
          </div>
        </template>
      </el-dialog>

      <section class="admin-user-table-panel">
        <div class="table-summary">
          <span>共 {{ users.length }} 条记录</span>
          <span v-if="loading">正在加载...</span>
        </div>

        <div class="table-wrap">
          <table class="admin-user-table">
            <thead>
              <tr>
                <th class="col-index">序号</th>
                <th>账号</th>
                <th>姓名</th>
                <th>真实姓名</th>
                <th>角色</th>
                <th>年级</th>
                <th>学院</th>
                <th>状态</th>
                <th>创建时间</th>
                <th class="col-action sticky-col">操作</th>
              </tr>
            </thead>
            <tbody v-if="!loading && pagedUsers.length">
              <tr v-for="(user, index) in pagedUsers" :key="user.userId">
                <td class="col-index">{{ rowIndex(index) }}</td>
                <td>{{ user.account }}</td>
                <td>{{ user.displayName }}</td>
                <td>{{ user.realName || '--' }}</td>
                <td>{{ resolveRoleLabel(user.roleCode) }}</td>
                <td>{{ user.grade || '--' }}</td>
                <td>{{ user.college || '--' }}</td>
                <td>
                  <span class="status-tag" :class="user.status === 'ACTIVE' ? 'is-active' : 'is-disabled'">
                    {{ resolveStatusLabel(user.status) }}
                  </span>
                </td>
                <td>{{ formatDate(user.createdAt) }}</td>
                <td class="sticky-col">
                  <div class="row-actions">
                    <button class="text-btn" type="button" @click="startEdit(user)">编辑</button>
                    <button class="text-btn" type="button" :disabled="processing" @click="resetPassword(user.userId)">
                      重置密码
                    </button>
                    <button
                      v-if="user.status === 'ACTIVE'"
                      class="text-btn"
                      type="button"
                      :disabled="processing"
                      @click="disableUser(user.userId)"
                    >
                      禁用
                    </button>
                    <button
                      v-else
                      class="text-btn"
                      type="button"
                      :disabled="processing"
                      @click="enableUser(user.userId)"
                    >
                      启用
                    </button>
                    <button class="text-btn text-btn--danger" type="button" @click="removeUser(user)">
                      删除
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
            <tbody v-else>
              <tr>
                <td colspan="10" class="table-empty">
                  {{ loading ? '正在同步用户列表...' : '暂无符合条件的用户数据。' }}
                </td>
              </tr>
            </tbody>
          </table>
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
  </section>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700&family=Noto+Serif+SC:wght@500;600;700&display=swap');

.admin-user-page {
  min-height: 100%;
  padding: 24px;
  background: #f5f7fa;
  color: #1f2933;
  font-family: 'Manrope', sans-serif;
}

.admin-user-shell {
  display: grid;
  gap: 16px;
}

.admin-user-header h1 {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
}

.admin-user-header p {
  margin: 6px 0 0;
  font-size: 14px;
  color: #66727f;
}

.admin-user-alert {
  margin: 0;
  padding: 12px 14px;
  border: 1px solid #f3c5c0;
  background: #fff4f2;
  color: #b5473e;
  font-size: 13px;
  border-radius: 6px;
}

.admin-user-toolbar,
.admin-user-create-panel,
.admin-user-table-panel {
  background: #ffffff;
  border: 1px solid #dfe5eb;
  border-radius: 8px;
}

.admin-user-toolbar {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-end;
  padding: 16px;
}

.toolbar-filters,
.toolbar-actions,
.create-panel-form,
.create-panel-actions {
  display: flex;
  gap: 12px;
  align-items: flex-end;
  flex-wrap: wrap;
}

.toolbar-filters {
  flex: 1;
}

.toolbar-field {
  display: grid;
  gap: 6px;
  min-width: 140px;
}

.toolbar-field--keyword {
  min-width: 280px;
}

.toolbar-field--college {
  min-width: 220px;
}

.toolbar-field span {
  font-size: 12px;
  color: #607080;
}

.toolbar-field input,
.toolbar-field select {
  height: 34px;
  padding: 0 10px;
  border: 1px solid #cfd7df;
  border-radius: 4px;
  background: #fff;
  font-size: 13px;
  color: #1f2933;
  outline: none;
}

.toolbar-field input:focus,
.toolbar-field select:focus {
  border-color: #7c8d7a;
}

.toolbar-btn {
  height: 34px;
  padding: 0 14px;
  border: 1px solid #cfd7df;
  border-radius: 4px;
  background: #fff;
  color: #1f2933;
  font-size: 13px;
  cursor: pointer;
}

.toolbar-btn--primary {
  border-color: #2f4c3a;
  background: #2f4c3a;
  color: #fff;
}

.admin-user-create-panel {
  padding: 16px;
}

.create-panel-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 16px;
}

.create-panel-header h2 {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
}

.create-panel-header p {
  margin: 6px 0 0;
  font-size: 12px;
  color: #66727f;
}

.admin-user-table-panel {
  padding: 16px;
}

.table-summary {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
  font-size: 13px;
  color: #607080;
}

.table-wrap {
  overflow: auto;
  border: 1px solid #dfe5eb;
  border-radius: 6px;
}

.admin-user-table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
  min-width: 1260px;
  background: #fff;
}

.admin-user-table th,
.admin-user-table td {
  padding: 12px 10px;
  border-bottom: 1px solid #e7edf3;
  text-align: left;
  font-size: 13px;
  vertical-align: middle;
}

.admin-user-table th {
  background: #f8fafc;
  color: #4d5b68;
  font-weight: 600;
  white-space: nowrap;
}

.admin-user-table tbody tr:hover {
  background: #fafcfd;
}

.col-index {
  width: 72px;
}

.col-action {
  width: 280px;
}

.sticky-col {
  position: sticky;
  right: 0;
  z-index: 1;
  background: #fff;
  box-shadow: -1px 0 0 #e7edf3;
}

.admin-user-table th.sticky-col {
  z-index: 2;
  background: #f8fafc;
}

.status-tag {
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}

.status-tag.is-active {
  background: #edf7f0;
  color: #26734d;
}

.status-tag.is-disabled {
  background: #fff3f1;
  color: #c0564d;
}

.row-actions {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}

.text-btn {
  padding: 0;
  border: none;
  background: transparent;
  color: #2f4c3a;
  font-size: 13px;
  cursor: pointer;
}

.text-btn:disabled {
  color: #9aa7b4;
  cursor: not-allowed;
}

.text-btn--danger {
  color: #c0483f;
}

.table-empty {
  padding: 32px 12px !important;
  text-align: center !important;
  color: #7a8794;
}

.pagination-nav {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 2rem;
  margin-top: 24px;
  padding-top: 16px;
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

.arrow {
  font-family: 'Manrope', sans-serif;
  transition: transform 0.3s ease;
}

.page-btn:hover:not(:disabled) .arrow:last-child {
  transform: translateX(4px);
}

.page-btn:hover:not(:disabled) .arrow:first-child {
  transform: translateX(-4px);
}

@media (max-width: 1200px) {
  .admin-user-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .toolbar-actions {
    justify-content: flex-start;
  }
}

:deep(.admin-user-edit-dialog .el-dialog) {
  border-radius: 10px;
}

:deep(.admin-user-edit-dialog .el-dialog__header) {
  margin-right: 0;
  padding: 18px 20px 12px;
  border-bottom: 1px solid #edf1f5;
}

:deep(.admin-user-edit-dialog .el-dialog__body) {
  padding: 20px;
}

:deep(.admin-user-edit-dialog .el-dialog__footer) {
  padding: 12px 20px 20px;
  border-top: 1px solid #edf1f5;
}

.edit-dialog-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.edit-dialog-grid .is-wide {
  grid-column: 1 / -1;
}

.edit-dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
