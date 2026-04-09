<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { changePassword } from '@/core/auth-service'
import { useAuthStore } from '@/stores/auth'
import { fetchSystemBusinessErrorApi, fetchSystemPingApi } from '@/api/system'
import { resolveRoleHome } from '@/core/session'
import { toErrorMessage } from '@/views/shared/page-logic'

const router = useRouter()
const authStore = useAuthStore()
const saving = ref(false)
const probing = ref(false)
const errorMessage = ref('')
const pingResult = ref('')
const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const homePath = computed(() => {
  const roleCode = authStore.currentUser?.roleCode
  return roleCode ? resolveRoleHome(roleCode) : '/login'
})

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
</script>

<template>
  <main class="account-page">
    <section class="account-page__masthead">
      <div class="account-page__heading">
        <p class="account-page__eyebrow">账户安全</p>
        <h1 class="account-page__title">账户安全</h1>
        <p class="account-page__summary">
          这里负责密码修改与基础系统探针。密码修改将调用真实鉴权接口，系统探针用于验证公共接口和全局异常处理。
        </p>
      </div>

      <aside class="account-page__snapshot">
        <p class="account-page__label">Current Session</p>
        <dl>
          <div>
            <dt>User</dt>
            <dd>{{ authStore.currentUser?.displayName || '未登录' }}</dd>
          </div>
          <div>
            <dt>Role</dt>
            <dd>{{ authStore.currentUser?.roleCode || 'Unknown' }}</dd>
          </div>
          <div>
            <dt>Ping</dt>
            <dd>{{ pingResult || '尚未探测' }}</dd>
          </div>
        </dl>
      </aside>
    </section>

    <p v-if="errorMessage" class="account-page__alert">{{ errorMessage }}</p>

    <section class="account-page__grid">
      <article class="account-page__panel account-page__panel--form">
        <div>
          <p class="account-page__label">密码轮换</p>
          <h2>修改密码</h2>
        </div>
        <label class="account-page__field">
          <span>旧密码</span>
          <input v-model="form.oldPassword" type="password" autocomplete="current-password" />
        </label>
        <label class="account-page__field">
          <span>新密码</span>
          <input v-model="form.newPassword" type="password" autocomplete="new-password" />
        </label>
        <label class="account-page__field">
          <span>确认新密码</span>
          <input v-model="form.confirmPassword" type="password" autocomplete="new-password" />
        </label>
        <div class="account-page__actions">
          <button class="account-page__primary" type="button" :disabled="saving" @click="submitChangePassword">
            {{ saving ? '提交中...' : '提交修改' }}
          </button>
          <button class="account-page__ghost" type="button" @click="resetForm">清空表单</button>
        </div>
      </article>

      <article class="account-page__panel">
        <div>
          <p class="account-page__label">系统探针</p>
          <h2>系统接口探针</h2>
        </div>
        <p class="account-page__note">用于验证 `GET /api/system/ping` 和 `GET /api/system/business-error` 的真实行为。</p>
        <div class="account-page__actions">
          <button class="account-page__primary" type="button" :disabled="probing" @click="runPing">
            {{ probing ? '探测中...' : '执行 Ping 探针' }}
          </button>
          <button class="account-page__ghost" type="button" :disabled="probing" @click="runBusinessErrorProbe">
            触发业务异常探针
          </button>
        </div>
        <button class="account-page__ghost account-page__ghost--inline" type="button" @click="goHome">返回角色首页</button>
      </article>
    </section>
  </main>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700&family=Noto+Serif+SC:wght@400;500;600;700&display=swap');

.account-page {
  --paper: #f3eee4;
  --ink: #201b17;
  --muted: #6c645d;
  --line: rgba(32, 27, 23, 0.12);
  --glass: rgba(255, 251, 245, 0.72);
  min-height: 100vh;
  padding: 2rem;
  color: var(--ink);
  background:
    radial-gradient(circle at top left, rgba(110, 128, 118, 0.18), transparent 24%),
    radial-gradient(circle at right center, rgba(198, 186, 166, 0.2), transparent 30%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.14), transparent 38%),
    var(--paper);
}

.account-page__masthead {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(300px, 0.85fr);
  gap: 1.5rem;
  align-items: end;
  padding-bottom: 1.4rem;
  border-bottom: 1px solid var(--line);
}

.account-page__eyebrow,
.account-page__label,
.account-page__snapshot dt,
.account-page__field span {
  margin: 0;
  font: 600 0.72rem/1.4 'Manrope', sans-serif;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--muted);
}

.account-page__title {
  margin: 0.95rem 0 0;
  font: 600 clamp(2.7rem, 4.8vw, 5rem)/0.98 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.account-page__summary,
.account-page__note {
  margin: 1rem 0 0;
  color: var(--muted);
  font: 400 0.98rem/1.85 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.account-page__snapshot,
.account-page__panel {
  border: 1px solid var(--line);
  background: var(--glass);
  backdrop-filter: blur(18px);
  box-shadow: 0 22px 48px rgba(80, 70, 58, 0.08);
}

.account-page__snapshot,
.account-page__panel {
  padding: 1.2rem;
}

.account-page__snapshot dl {
  display: grid;
  gap: 0.9rem;
  margin: 1rem 0 0;
}

.account-page__snapshot dd {
  margin: 0.35rem 0 0;
  font: 600 1.04rem/1.45 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.account-page__alert {
  margin: 1.25rem 0 0;
  color: #8d4747;
  font: 600 0.9rem/1.6 'Manrope', sans-serif;
}

.account-page__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
  margin-top: 1.5rem;
}

.account-page__panel {
  display: grid;
  gap: 1rem;
}

.account-page__panel h2 {
  margin: 0.65rem 0 0;
  font: 600 1.72rem/1.28 'Noto Serif SC', 'Source Han Serif SC', serif;
}

.account-page__field {
  display: grid;
  gap: 0.7rem;
}

.account-page__field input {
  min-height: 3rem;
  padding: 0 0.95rem;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.52);
  color: var(--ink);
  font: 500 0.95rem/1.5 'Manrope', sans-serif;
}

.account-page__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.8rem;
}

.account-page__primary,
.account-page__ghost {
  min-height: 3rem;
  padding: 0 1.15rem;
  font: 600 0.84rem/1 'Manrope', sans-serif;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  cursor: pointer;
  transition: transform 180ms ease, border-color 180ms ease, box-shadow 180ms ease;
}

.account-page__primary {
  border: none;
  background: linear-gradient(135deg, #64806e, #4d6657);
  color: #faf6f0;
  box-shadow: 0 18px 36px rgba(77, 102, 87, 0.24);
}

.account-page__ghost {
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.5);
  color: var(--ink);
}

.account-page__ghost--inline {
  justify-self: start;
}

.account-page__primary:hover:not(:disabled),
.account-page__ghost:hover:not(:disabled) {
  transform: translateY(-2px);
}

.account-page__primary:disabled,
.account-page__ghost:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

@media (max-width: 980px) {
  .account-page {
    padding: 1rem;
  }

  .account-page__masthead,
  .account-page__grid {
    grid-template-columns: 1fr;
  }
}
</style>

