<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { resolveRoleHome } from '@/core/session'
import { toErrorMessage } from '@/views/shared/page-logic'

const router = useRouter()
const authStore = useAuthStore()

const form = reactive({
  account: '',
  password: ''
})
const submitting = ref(false)
const submitError = ref('')

async function submit登录(): Promise<void> {
  submitting.value = true
  submitError.value = ''

  try {
    const currentUser = await authStore.signIn(form.account, form.password)
    await router.replace(resolveRoleHome(currentUser.roleCode))
  } catch (error) {
    submitError.value = toErrorMessage(error)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <main class="editorial-login">
    <div class="editorial-login__grain" />
    <section class="editorial-login__hero">
      <p class="editorial-login__eyebrow">高校心理自助平台</p>
      <h1 class="editorial-login__title">
        高校心理自助服务平台
      </h1>
      <p class="editorial-login__lead">
        以测评、倾诉、预约与资源支持为核心，为学生、咨询师和管理员提供统一的心理服务协作入口。
      </p>
      <dl class="editorial-login__facts">
        <div>
          <dt>系统</dt>
          <dd>心理服务协作中枢</dd>
        </div>
        <div>
          <dt>聚焦</dt>
          <dd>测评、反思与结构化支持流程</dd>
        </div>
        <div>
          <dt>模式</dt>
          <dd>基于角色的安全访问控制</dd>
        </div>
      </dl>
    </section>

    <section class="editorial-login__panel">
      <div class="editorial-login__panel-head">
        <p class="editorial-login__panel-kicker">登录</p>
        <p class="editorial-login__panel-copy">
          输入账号与密码后进入对应工作台。学生、咨询师与管理员将自动跳转到各自首页。
        </p>
      </div>

      <form class="editorial-login__form" @submit.prevent="submit登录">
        <label class="field">
          <span class="field__label">账号</span>
          <input
            v-model="form.account"
            class="field__control"
            type="text"
            autocomplete="username"
            placeholder="20230001 / teacher01 / admin"
          >
        </label>

        <label class="field">
          <span class="field__label">密码</span>
          <input
            v-model="form.password"
            class="field__control"
            type="password"
            autocomplete="current-password"
            placeholder="请输入登录密码"
          >
        </label>

        <p v-if="submitError" class="editorial-login__error">
          {{ submitError }}
        </p>

        <button class="editorial-login__submit" type="submit" :disabled="submitting">
          <span>{{ submitting ? '登录中...' : '进入系统' }}</span>
        </button>
      </form>

      <div class="editorial-login__footer">
        <p>登录失败时请先确认账号密码是否正确，或联系管理员检查当前账号状态。</p>
      </div>
    </section>
  </main>
</template>
<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700&family=Noto+Serif+SC:wght@400;500;600;700&display=swap');

:global(body) {
  margin: 0;
  background:
    radial-gradient(circle at top left, rgba(185, 168, 140, 0.18), transparent 32%),
    radial-gradient(circle at bottom right, rgba(107, 127, 114, 0.16), transparent 28%),
    #f5f0e8;
  color: #1d1b18;
}

.editorial-login {
  --paper: rgba(252, 248, 241, 0.84);
  --ink: #201d19;
  --muted: #71695e;
  --line: rgba(32, 29, 25, 0.12);
  --accent: #6a7a6b;
  --accent-soft: rgba(106, 122, 107, 0.12);
  position: relative;
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(360px, 520px);
  overflow: hidden;
}

.editorial-login__grain {
  position: absolute;
  inset: 0;
  pointer-events: none;
  opacity: 0.18;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.35), rgba(255, 255, 255, 0.1)),
    radial-gradient(rgba(38, 33, 28, 0.08) 0.7px, transparent 0.7px);
  background-size: auto, 14px 14px;
  mix-blend-mode: multiply;
}

.editorial-login__hero,
.editorial-login__panel {
  position: relative;
  z-index: 1;
}

.editorial-login__hero {
  padding: 5.5rem 5rem 4rem 6vw;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.editorial-login__eyebrow,
.editorial-login__panel-kicker,
.field__label,
.editorial-login__facts dt {
  margin: 0;
  font: 600 0.72rem/1.4 'Manrope', sans-serif;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: var(--muted);
}

.editorial-login__title {
  max-width: 7em;
  margin: 1.1rem 0 0;
  font: 600 clamp(2.8rem, 5.6vw, 5.8rem)/0.98 'Noto Serif SC', 'Source Han Serif SC', serif;
  letter-spacing: 0.02em;
  color: var(--ink);
}

.editorial-login__lead {
  max-width: 34rem;
  margin: 1.8rem 0 0;
  font: 400 1.05rem/1.95 'Noto Serif SC', 'Source Han Serif SC', serif;
  color: var(--muted);
}

.editorial-login__facts {
  margin: 4rem 0 0;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 1.4rem;
  padding-top: 1.4rem;
  border-top: 1px solid var(--line);
}

.editorial-login__facts div {
  padding-right: 1rem;
}

.editorial-login__facts dd {
  margin: 0.65rem 0 0;
  font: 500 0.96rem/1.8 'Noto Serif SC', 'Source Han Serif SC', serif;
  color: var(--ink);
}

.editorial-login__panel {
  margin: 2rem 2rem 2rem 0;
  padding: 2rem;
  align-self: stretch;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  border-left: 1px solid var(--line);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.62), rgba(255, 255, 255, 0.32)),
    var(--paper);
  backdrop-filter: blur(22px);
  box-shadow: -24px 0 60px rgba(55, 48, 42, 0.08);
}

.editorial-login__panel-head {
  padding-bottom: 1.4rem;
  border-bottom: 1px solid var(--line);
}

.editorial-login__panel-copy {
  margin: 1rem 0 0;
  font: 400 0.98rem/1.85 'Noto Serif SC', 'Source Han Serif SC', serif;
  color: var(--muted);
}

.editorial-login__form {
  display: grid;
  gap: 1.35rem;
  margin-top: 2rem;
}

.field {
  display: grid;
  gap: 0.65rem;
}

.field__control {
  width: 100%;
  box-sizing: border-box;
  padding: 1rem 1.05rem;
  border: 1px solid rgba(32, 29, 25, 0.16);
  background: rgba(255, 252, 248, 0.88);
  color: var(--ink);
  font: 500 0.98rem/1.4 'Manrope', sans-serif;
  transition: border-color 180ms ease, transform 180ms ease, background-color 180ms ease;
}

.field__control:focus {
  outline: none;
  border-color: var(--accent);
  background: rgba(255, 255, 255, 0.96);
  transform: translateY(-1px);
}

.field__control::placeholder {
  color: rgba(113, 105, 94, 0.7);
}

.editorial-login__error {
  margin: 0;
  padding: 0.85rem 1rem;
  border: 1px solid rgba(143, 72, 72, 0.2);
  background: rgba(143, 72, 72, 0.08);
  color: #7a3f3f;
  font: 500 0.92rem/1.6 'Manrope', sans-serif;
}

.editorial-login__submit {
  margin-top: 0.25rem;
  min-height: 3.5rem;
  border: 1px solid transparent;
  background:
    linear-gradient(135deg, rgba(106, 122, 107, 1), rgba(77, 92, 84, 1));
  color: #f7f3ed;
  font: 600 0.96rem/1 'Manrope', sans-serif;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  cursor: pointer;
  transition: transform 180ms ease, box-shadow 180ms ease, opacity 180ms ease;
  box-shadow: 0 18px 30px rgba(68, 83, 75, 0.2);
}

.editorial-login__submit:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 24px 34px rgba(68, 83, 75, 0.24);
}

.editorial-login__submit:disabled {
  cursor: wait;
  opacity: 0.74;
}

.editorial-login__footer {
  margin-top: 2rem;
  padding-top: 1.4rem;
  border-top: 1px solid var(--line);
  color: var(--muted);
  font: 400 0.9rem/1.8 'Noto Serif SC', 'Source Han Serif SC', serif;
}

@media (max-width: 1100px) {
  .editorial-login {
    grid-template-columns: 1fr;
  }

  .editorial-login__hero {
    padding: 4rem 1.5rem 2rem;
  }

  .editorial-login__facts {
    grid-template-columns: 1fr;
  }

  .editorial-login__panel {
    margin: 0 1rem 1rem;
    border-left: none;
    border-top: 1px solid var(--line);
  }
}
</style>

