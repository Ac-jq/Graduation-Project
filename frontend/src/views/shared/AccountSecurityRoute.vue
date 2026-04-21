<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { changePassword } from '@/core/auth-service'
import { useAuthStore } from '@/stores/auth'
import { fetchCounselorProfileApi, updateCounselorProfileApi } from '@/api/user'
import type { CounselorProfile } from '@/api/types'
import { fetchSystemBusinessErrorApi, fetchSystemPingApi } from '@/api/system'
import { resolveRoleHome } from '@/core/session'
import { toErrorMessage } from '@/views/shared/page-logic'

const router = useRouter()
const authStore = useAuthStore()
const saving = ref(false)
const probing = ref(false)
const loadingCounselorProfile = ref(false)
const savingCounselorAvatar = ref(false)
const errorMessage = ref('')
const pingResult = ref('')
const assetOrigin = `${window.location.protocol}//${window.location.hostname}:8080`
const counselorAvatarEventName = 'jqpro:counselor-avatar-updated'
const avatarPresets = Array.from({ length: 10 }, (_, index) => ({
  id: index + 1,
  label: `预设头像 ${String(index + 1).padStart(2, '0')}`,
  url: `${assetOrigin}/assets/avatars/presets/avatar-${String(index + 1).padStart(2, '0')}.jpg`
}))
const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})
const counselorAvatarForm = reactive({
  avatarUrl: ''
})
const counselorProfile = ref<CounselorProfile | null>(null)

const homePath = computed(() => {
  const roleCode = authStore.currentUser?.roleCode
  return roleCode ? resolveRoleHome(roleCode) : '/login'
})
const isCounselorAccount = computed(() => authStore.currentUser?.roleCode === 'COUNSELOR')
const currentCounselorAvatarUrl = computed(() =>
  counselorAvatarForm.avatarUrl || authStore.currentUser?.avatarUrl || avatarPresets[0]?.url || ''
)
const currentPresetId = computed(() =>
  avatarPresets.find((preset) => preset.url === currentCounselorAvatarUrl.value)?.id ?? null
)

async function loadCounselorProfile(): Promise<void> {
  if (!isCounselorAccount.value) {
    return
  }

  loadingCounselorProfile.value = true
  errorMessage.value = ''

  try {
    const profile = await fetchCounselorProfileApi()
    counselorProfile.value = profile
    counselorAvatarForm.avatarUrl = profile.avatarUrl || authStore.currentUser?.avatarUrl || avatarPresets[0].url
    authStore.updateCurrentUser({
      realName: profile.realName,
      displayName: profile.displayName,
      counselorNo: profile.counselorNo,
      avatarUrl: profile.avatarUrl
    })
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    loadingCounselorProfile.value = false
  }
}

function chooseCounselorAvatar(url: string): void {
  counselorAvatarForm.avatarUrl = url
}

async function saveCounselorAvatar(): Promise<void> {
  if (!isCounselorAccount.value) {
    return
  }

  savingCounselorAvatar.value = true
  errorMessage.value = ''

  try {
    const profile = await updateCounselorProfileApi({
      avatarUrl: counselorAvatarForm.avatarUrl || null
    })
    counselorProfile.value = profile
    counselorAvatarForm.avatarUrl = profile.avatarUrl || avatarPresets[0].url
    authStore.updateCurrentUser({
      realName: profile.realName,
      displayName: profile.displayName,
      counselorNo: profile.counselorNo,
      avatarUrl: profile.avatarUrl
    })
    window.dispatchEvent(new Event(counselorAvatarEventName))
    ElMessage.success('头像已更新，学生端将同步显示新的咨询师形象。')
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    savingCounselorAvatar.value = false
  }
}

function resetForm(): void {
  form.oldPassword = ''
  form.newPassword = ''
  form.confirmPassword = ''
}

async function submitChangePassword(): Promise<void> {
  if (!form.oldPassword || !form.newPassword || !form.confirmPassword) {
    errorMessage.value = '请完整填写旧密码、新密码和确认密码。'
    return
  }

  saving.value = true
  errorMessage.value = ''

  try {
    await changePassword(form.oldPassword, form.newPassword, form.confirmPassword)
    ElMessage.success('密码修改成功，请妥善保管新密码。')
    resetForm()
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    saving.value = false
  }
}

async function runPing(): Promise<void> {
  probing.value = true
  errorMessage.value = ''

  try {
    pingResult.value = await fetchSystemPingApi()
    ElMessage.success(`系统探针返回：${pingResult.value}`)
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    probing.value = false
  }
}

async function runBusinessErrorProbe(): Promise<void> {
  probing.value = true
  errorMessage.value = ''

  try {
    await fetchSystemBusinessErrorApi()
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    probing.value = false
  }
}

async function goHome(): Promise<void> {
  await router.push(homePath.value)
}

onMounted(() => {
  void loadCounselorProfile()
})
</script>

<template>
  <main class="security-dossier-page">
    <div class="page-container">

      <nav class="dossier-nav">
        <button class="nav-ghost-btn" @click="goHome">
          <span class="arrow">←</span> 返回角色首页
        </button>
      </nav>

      <header class="dossier-header">
        <span class="header-tag">Security Protocol</span>
        <h1 class="huge-title">账户与安全</h1>
        <p class="header-lead">
          在此管理您的访问凭证与系统连接状态。您的密码经过不可逆加密存储，系统无法直接查看，请妥善保管新设定的安全密钥。
        </p>
      </header>

      <div v-if="errorMessage" class="error-banner">{{ errorMessage }}</div>

      <section class="dossier-grid">

        <aside class="session-specs">
          <section v-if="isCounselorAccount" class="portrait-card">
            <div class="portrait-circle">
              <img :src="currentCounselorAvatarUrl" alt="咨询师头像" class="portrait-image">
            </div>

            <div class="portrait-copy">
              <span class="portrait-kicker">Counselor Avatar</span>
              <h3>选择你的咨询师形象</h3>
              <p>
                使用学生端同款本地预设头像。保存后会写入后端资料，并同步到工作台、预约列表与聊天室。
              </p>
            </div>

            <div class="preset-grid" aria-label="咨询师预设头像">
              <button
                v-for="preset in avatarPresets"
                :key="preset.id"
                type="button"
                class="preset-item"
                :class="{ 'is-selected': currentPresetId === preset.id }"
                :aria-label="preset.label"
                :disabled="loadingCounselorProfile || savingCounselorAvatar"
                @click="chooseCounselorAvatar(preset.url)"
              >
                <img :src="preset.url" :alt="preset.label">
              </button>
            </div>

            <button
              class="portrait-save-btn"
              type="button"
              :disabled="loadingCounselorProfile || savingCounselorAvatar"
              @click="saveCounselorAvatar"
            >
              {{ savingCounselorAvatar ? '正在保存...' : '保存头像' }}
            </button>
          </section>

          <h3 class="specs-heading">当前会话标识</h3>

          <div class="spec-row">
            <span class="spec-label">当前用户</span>
            <span class="spec-value">{{ authStore.currentUser?.displayName || '未登录' }}</span>
          </div>

          <div class="spec-row">
            <span class="spec-label">角色权限</span>
            <span class="spec-value">{{ authStore.currentUser?.roleCode || 'Unknown' }}</span>
          </div>

          <div class="spec-row">
            <span class="spec-label">探针回显</span>
            <span class="spec-value">{{ pingResult || '尚未探测' }}</span>
          </div>

          <div class="spec-note">
            * 会话标识用于验证您的当前访问身份。如果发现信息不符，请立即更改密码或联系系统管理员。
          </div>
        </aside>

        <section class="action-fields">

          <article class="form-section">
            <div class="thick-accent-line"></div>
            <div class="fields-header">
              <h3 class="fields-heading">凭证轮换</h3>
              <span class="fields-note">修改密码</span>
            </div>

            <div class="form-stack">
              <label class="sleek-field">
                <span class="field-label">当前密码</span>
                <input v-model="form.oldPassword" type="password" class="sleek-input" autocomplete="current-password" placeholder="请输入原密码进行验证">
              </label>

              <label class="sleek-field">
                <span class="field-label">设定新密码</span>
                <input v-model="form.newPassword" type="password" class="sleek-input" autocomplete="new-password" placeholder="输入新的安全密钥">
              </label>

              <label class="sleek-field">
                <span class="field-label">确认新密码</span>
                <input v-model="form.confirmPassword" type="password" class="sleek-input" autocomplete="new-password" placeholder="再次输入以确认无误">
              </label>
            </div>

            <div class="form-actions">
              <button class="ghost-btn" type="button" @click="resetForm" :disabled="saving">
                清空重置
              </button>
              <button class="primary-btn" type="button" :disabled="saving" @click="submitChangePassword">
                {{ saving ? '正在更新凭证...' : '提交修改' }} <span class="arrow">→</span>
              </button>
            </div>
          </article>

          <article class="form-section diagnostic-section">
            <div class="thick-accent-line"></div>
            <div class="fields-header">
              <h3 class="fields-heading">系统诊断探针</h3>
              <span class="fields-note">开发者或维护选项</span>
            </div>

            <p class="diagnostic-text">
              此功能用于验证当前环境与后端 `api/system` 服务的连通性及全局异常拦截机制是否正常运转。
            </p>

            <div class="diagnostic-actions">
              <button class="wire-btn" type="button" :disabled="probing" @click="runPing">
                执行 Ping 探针
              </button>
              <button class="wire-btn wire-btn--danger" type="button" :disabled="probing" @click="runBusinessErrorProbe">
                触发业务异常探针
              </button>
            </div>
          </article>

        </section>

      </section>
    </div>
  </main>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700;800&family=Noto+Serif+SC:wght@500;600;700&display=swap');

/* 全局极简白纸底色 */
.security-dossier-page {
  min-height: 100vh;
  background: #fcfbf9;
  color: #1e2821;
  font-family: 'Manrope', 'Noto Serif SC', sans-serif;
  padding: 2rem 2vw 8rem;
  box-sizing: border-box;
}

.page-container {
  max-width: 1060px;
  margin: 0 auto;
}

/* 顶部导航 */
.dossier-nav {
  margin-bottom: 3rem;
}

.nav-ghost-btn {
  background: transparent;
  border: none;
  font-family: 'Noto Serif SC', serif;
  font-size: 1rem;
  font-weight: 600;
  color: #5c6b60;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0;
  transition: color 0.3s ease;
}

.nav-ghost-btn:hover {
  color: #1e2821;
}

/* 头部排版 */
.dossier-header {
  margin-bottom: 4rem;
  padding-bottom: 2.5rem;
  border-bottom: 1px solid rgba(42, 54, 46, 0.1);
}

.header-tag {
  display: block;
  font-family: 'Manrope', sans-serif;
  font-size: 0.85rem;
  font-weight: 700;
  letter-spacing: 0.15em;
  color: #8a9c90;
  text-transform: uppercase;
  margin-bottom: 1rem;
}

.huge-title {
  font-family: 'Noto Serif SC', serif;
  font-size: clamp(2.8rem, 5vw, 4.2rem);
  font-weight: 600;
  color: #1e2821;
  margin: 0 0 1.2rem 0;
  line-height: 1.1;
  letter-spacing: 0.02em;
}

.header-lead {
  font-size: 1.05rem;
  color: #5c6b60;
  line-height: 1.8;
  margin: 0;
  max-width: 680px;
}

/* 状态样式 */
.error-banner {
  background: rgba(140, 74, 74, 0.08);
  color: #8c4a4a;
  padding: 1.5rem;
  border-radius: 12px;
  text-align: center;
  font-family: 'Noto Serif SC', serif;
  margin-bottom: 3rem;
}

/* 核心双栏网格 */
.dossier-grid {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 5rem;
  align-items: start;
}

/* ================= 左侧：会话标识 ================= */
.session-specs {
  display: flex;
  flex-direction: column;
}

.portrait-card {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  margin-bottom: 2.4rem;
  padding: 1.25rem;
  border-radius: 28px;
  background:
    radial-gradient(circle at 20% 12%, rgba(206, 219, 203, 0.55), transparent 32%),
    linear-gradient(145deg, rgba(255, 255, 255, 0.86), rgba(247, 244, 239, 0.72));
  box-shadow: 0 28px 56px rgba(52, 65, 55, 0.08);
}

.portrait-circle {
  width: 92px;
  height: 92px;
  border-radius: 30px;
  overflow: hidden;
  background: linear-gradient(135deg, #dfe8dc, #f3e7dc);
  box-shadow: 0 18px 36px rgba(52, 65, 55, 0.12);
}

.portrait-image {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
}

.portrait-copy {
  display: grid;
  gap: 0.45rem;
}

.portrait-kicker {
  color: #8a9c90;
  font: 700 0.72rem/1 'Manrope', sans-serif;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.portrait-copy h3 {
  margin: 0;
  color: #1e2821;
  font: 600 1.25rem/1.3 'Noto Serif SC', serif;
}

.portrait-copy p {
  margin: 0;
  color: #6a7c70;
  font: 500 0.88rem/1.7 'Manrope', sans-serif;
}

.preset-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 0.6rem;
}

.preset-item {
  width: 100%;
  aspect-ratio: 1;
  padding: 0.16rem;
  border: none;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.68);
  cursor: pointer;
  overflow: hidden;
  box-shadow: inset 0 0 0 1px rgba(42, 54, 46, 0.06);
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.preset-item img {
  width: 100%;
  height: 100%;
  display: block;
  border-radius: 15px;
  object-fit: cover;
}

.preset-item:hover:not(:disabled) {
  transform: translateY(-3px);
  box-shadow: 0 14px 26px rgba(52, 65, 55, 0.1);
}

.preset-item.is-selected {
  background: #2a362e;
  box-shadow: 0 18px 32px rgba(42, 54, 46, 0.18);
}

.portrait-save-btn {
  width: 100%;
  min-height: 2.9rem;
  border: none;
  border-radius: 999px;
  background: #2a362e;
  color: #ffffff;
  cursor: pointer;
  font: 600 0.95rem/1 'Noto Serif SC', serif;
  box-shadow: 0 18px 32px rgba(42, 54, 46, 0.16);
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.portrait-save-btn:hover:not(:disabled) {
  transform: translateY(-3px);
  box-shadow: 0 22px 38px rgba(42, 54, 46, 0.22);
}

.portrait-save-btn:disabled,
.preset-item:disabled {
  cursor: not-allowed;
  opacity: 0.62;
}

.specs-heading {
  font-family: 'Noto Serif SC', serif;
  font-size: 1.25rem;
  font-weight: 600;
  color: #1e2821;
  margin: 0 0 1.5rem 0;
}

.spec-row {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
  padding: 1.2rem 0;
  border-bottom: 1px solid rgba(42, 54, 46, 0.08);
}

.spec-row:first-of-type {
  border-top: 1px solid rgba(42, 54, 46, 0.08);
}

.spec-label {
  font-family: 'Manrope', sans-serif;
  font-size: 0.8rem;
  font-weight: 700;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: #8a9c90;
}

.spec-value {
  font-family: 'Manrope', sans-serif;
  font-size: 1.15rem;
  color: #2a362e;
  font-weight: 500;
}

.spec-note {
  margin-top: 1.5rem;
  font-size: 0.85rem;
  color: #a3b0a7;
  line-height: 1.6;
  font-style: italic;
}

/* ================= 右侧：操作表单 ================= */
.action-fields {
  display: flex;
  flex-direction: column;
  gap: 5rem;
}

.form-section {
  display: flex;
  flex-direction: column;
}

.thick-accent-line {
  width: 100%;
  height: 4px;
  background: #2a362e;
  margin-bottom: 1.5rem;
}

.fields-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 3rem;
}

.fields-heading {
  font-family: 'Noto Serif SC', serif;
  font-size: 1.6rem;
  font-weight: 600;
  color: #1e2821;
  margin: 0;
}

.fields-note {
  font-size: 0.9rem;
  color: #8a9c90;
}

/* 沉浸式表单 */
.form-stack {
  display: flex;
  flex-direction: column;
  gap: 2.5rem;
}

.sleek-field {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.field-label {
  font-family: 'Manrope', sans-serif;
  font-size: 0.85rem;
  font-weight: 700;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: #5c6b60;
}

.sleek-input {
  width: 100%;
  border: none;
  border-bottom: 1px solid rgba(42, 54, 46, 0.2);
  background: transparent;
  padding: 0.8rem 0;
  font-family: 'Manrope', sans-serif;
  font-size: 1.2rem;
  color: #1e2821;
  outline: none;
  transition: border-color 0.3s ease;
  letter-spacing: 0.1em; /* 密码点间距稍大 */
}

.sleek-input::placeholder {
  color: #b5c2b9;
  font-weight: 400;
  letter-spacing: normal; /* placeholder 恢复正常间距 */
}

.sleek-input:focus {
  border-bottom-color: #2a362e;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 1.5rem;
  margin-top: 3rem;
  padding-top: 2rem;
  border-top: 1px solid rgba(42, 54, 46, 0.08);
}

.primary-btn, .ghost-btn {
  padding: 1.2rem 2.5rem;
  border-radius: 100px;
  font-family: 'Noto Serif SC', serif;
  font-size: 1.05rem;
  font-weight: 600;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 0.6rem;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.primary-btn {
  background: #2a362e;
  border: none;
  color: #ffffff;
  box-shadow: 0 12px 24px rgba(42, 54, 46, 0.15);
}

.primary-btn:hover:not(:disabled) {
  background: #1c2620;
  transform: translateY(-2px);
  box-shadow: 0 16px 32px rgba(42, 54, 46, 0.25);
}

.primary-btn:disabled {
  background: #8a9c90;
  box-shadow: none;
  cursor: not-allowed;
}

.ghost-btn {
  background: transparent;
  border: none;
  color: #7b8c80;
  padding: 1.2rem 1.5rem;
}

.ghost-btn:hover:not(:disabled) {
  color: #2a362e;
}

/* ================= 诊断面板 ================= */
.diagnostic-text {
  font-size: 1.05rem;
  color: #6a7c70;
  line-height: 1.8;
  margin: 0 0 2rem 0;
}

.diagnostic-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
}

.wire-btn {
  background: transparent;
  border: 1px solid rgba(42, 54, 46, 0.3);
  color: #4a5c51;
  padding: 0.8rem 1.5rem;
  border-radius: 8px;
  font-family: 'Noto Serif SC', serif;
  font-size: 0.95rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.wire-btn:hover:not(:disabled) {
  background: rgba(42, 54, 46, 0.05);
  border-color: #2a362e;
  color: #1e2821;
}

.wire-btn--danger:hover:not(:disabled) {
  border-color: #8c4a4a;
  color: #8c4a4a;
  background: rgba(140, 74, 74, 0.05);
}

.wire-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 动画交互 */
.arrow {
  font-family: 'Manrope', sans-serif;
  transition: transform 0.3s ease;
}

.primary-btn:hover:not(:disabled) .arrow {
  transform: translateX(4px);
}

.nav-ghost-btn:hover .arrow {
  transform: translateX(-4px);
}

/* 响应式 */
@media (max-width: 900px) {
  .dossier-grid {
    grid-template-columns: 1fr;
    gap: 4rem;
  }

  .fields-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.5rem;
  }

  .form-actions {
    flex-direction: column-reverse;
    align-items: stretch;
  }

  .primary-btn, .ghost-btn {
    width: 100%;
    justify-content: center;
  }

  .diagnostic-actions {
    flex-direction: column;
  }

  .wire-btn {
    width: 100%;
  }
}
</style>
