<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
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
const passwordVisible = ref(false)

const passwordInputType = computed(() => (passwordVisible.value ? 'text' : 'password'))

function togglePasswordVisible(): void {
  passwordVisible.value = !passwordVisible.value
}

async function submitLogin(): Promise<void> {
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
  <div class="login-viewport">
    <div class="noise-texture"></div>

    <main class="login-frame">

      <section class="visual-panel">
        <div class="brand-header">
          <div class="brand-logo"></div>
          <span class="brand-eyebrow">JQPro Platform</span>
        </div>

        <div class="hero-content">
          <h1 class="brand-headline">
            构筑内心的<br />
            <span>安全岛屿。</span>
          </h1>
          <p class="brand-lead">
            以测评、倾诉与结构化支持为核心，为学生、咨询师和管理员提供统一的协作空间。
          </p>
        </div>

        <div class="decorative-graphic">
          <div class="shape shape-circle"></div>
          <div class="shape shape-arch"></div>
        </div>
      </section>

      <section class="interaction-panel">
        <div class="form-wrapper">
          <div class="form-header">
            <h2>系统准入</h2>
            <p>请输入您的身份凭证进入工作台</p>
          </div>

          <form class="login-form" @submit.prevent="submitLogin">
            <div class="input-group">
              <label for="account">识别码</label>
              <input
                  id="account"
                  v-model="form.account"
                  type="text"
                  autocomplete="username"
                  placeholder="学号 / 工号 / Admin"
                  :disabled="submitting"
              >
            </div>

            <div class="input-group">
              <label for="password">安全密钥</label>
              <div class="password-field">
                <input
                    id="password"
                    v-model="form.password"
                    :type="passwordInputType"
                    autocomplete="current-password"
                    placeholder="请输入登录密码"
                    :disabled="submitting"
                >
                <button
                    class="password-toggle"
                    type="button"
                    :disabled="submitting"
                    @click="togglePasswordVisible"
                    aria-label="切换密码显示状态"
                >
                  <svg v-if="passwordVisible" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                    <circle cx="12" cy="12" r="3"></circle>
                  </svg>
                  <svg v-else xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path>
                    <line x1="1" y1="1" x2="23" y2="23"></line>
                  </svg>
                </button>
              </div>
            </div>

            <div v-if="submitError" class="error-banner" role="alert">
              <span class="error-icon">!</span>
              {{ submitError }}
            </div>

            <button class="submit-btn" type="submit" :disabled="submitting">
              <span>{{ submitting ? '身份验证中...' : '进入系统' }}</span>
              <span class="btn-arrow">→</span>
            </button>
          </form>

          <div class="form-footer">
            <p>遇到登录问题？请联系系统管理员获取帮助。</p>
            <button type="button" class="register-link" @click="router.push('/register')">
              创建学生账号 <span>→</span>
            </button>
          </div>
        </div>
      </section>

    </main>
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600&family=Noto+Serif+SC:wght@400;500;600;700&display=swap');

/* 全局视图背景 */
.login-viewport {
  --bg-viewport: #E8E5DF;
  --bg-frame: #F4F1EA;
  --text-main: #2C302B;
  --text-muted: #7A7D75;
  --border-color: rgba(44, 48, 43, 0.15);
  --accent: #6A7A6B;

  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding: 4vmin;
  background-color: var(--bg-viewport);
  color: var(--text-main);
  font-family: 'Manrope', sans-serif;
  position: relative;
}

.noise-texture {
  position: absolute;
  inset: 0;
  pointer-events: none;
  opacity: 0.3;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noiseFilter'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.85' numOctaves='3' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noiseFilter)'/%3E%3C/svg%3E");
  mix-blend-mode: multiply;
}

/* 核心：带边框的画板容器 */
.login-frame {
  display: flex;
  width: 100%;
  max-width: 1200px;
  height: min(800px, 90vh);
  background-color: var(--bg-frame);
  border: 1px solid var(--border-color);
  border-radius: 24px;
  overflow: hidden;
  position: relative;
  z-index: 1;
  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.04);
}

/* 左侧陈述区 */
.visual-panel {
  flex: 1.2;
  padding: 4rem;
  display: flex;
  flex-direction: column;
  position: relative;
}

.brand-header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.brand-logo {
  width: 20px;
  height: 20px;
  background-color: var(--accent);
  border-radius: 50%;
}

.brand-eyebrow {
  font-size: 0.8rem;
  letter-spacing: 0.15em;
  text-transform: uppercase;
  color: var(--text-muted);
  font-weight: 600;
}

.hero-content {
  margin-top: auto;
  margin-bottom: auto;
  position: relative;
  z-index: 2;
}

.brand-headline {
  font-family: 'Noto Serif SC', serif;
  font-size: clamp(2.5rem, 4vw, 4rem);
  font-weight: 500;
  line-height: 1.2;
  margin: 0 0 1.5rem 0;
  color: var(--text-main);
}

.brand-headline span {
  color: var(--accent);
}

.brand-lead {
  font-family: 'Noto Serif SC', serif;
  font-size: 1.1rem;
  line-height: 1.8;
  color: var(--text-muted);
  max-width: 32ch;
  margin: 0;
}

/* 用 CSS 绘制极简的有机图形装饰 */
.decorative-graphic {
  position: absolute;
  right: 4rem;
  bottom: 4rem;
  display: flex;
  align-items: flex-end;
  gap: 1rem;
  opacity: 0.6;
}

.shape {
  border: 1px solid var(--border-color);
}

.shape-circle {
  width: 120px;
  height: 120px;
  border-radius: 50%;
}

.shape-arch {
  width: 100px;
  height: 160px;
  border-radius: 100px 100px 0 0;
  background-color: rgba(106, 122, 107, 0.05);
}

/* 右侧表单区：通过左边框与视觉区明确分割 */
.interaction-panel {
  flex: 1;
  border-left: 1px solid var(--border-color);
  background-color: #FFFFFF;
  padding: 4rem;
  display: flex;
  align-items: center;
  justify-content: center;
}

.form-wrapper {
  width: 100%;
  max-width: 340px;
}

.form-header {
  margin-bottom: 3rem;
}

.form-header h2 {
  font-family: 'Noto Serif SC', serif;
  font-size: 1.8rem;
  font-weight: 500;
  margin: 0 0 0.5rem 0;
}

.form-header p {
  font-size: 0.9rem;
  color: var(--text-muted);
  margin: 0;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.input-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.input-group label {
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--text-main);
  padding-left: 0.2rem;
}

/* 带边框的输入框 */
.input-group input {
  background: var(--bg-frame);
  border: 1px solid var(--border-color);
  padding: 1rem 1.2rem;
  border-radius: 12px;
  font-family: 'Manrope', sans-serif;
  font-size: 1rem;
  color: var(--text-main);
  transition: all 0.3s ease;
}

.input-group input:focus {
  outline: none;
  border-color: var(--accent);
  background: #FFFFFF;
  box-shadow: 0 0 0 4px rgba(106, 122, 107, 0.1);
}

.input-group input::placeholder {
  color: rgba(122, 125, 117, 0.5);
}

.password-field {
  position: relative;
}

/* 修改：因为去掉了汉字只保留图标，右侧内边距相应减小，确保账号和密码输入框等长 */
.password-field input {
  width: 100%;
  padding-right: 3rem;
  box-sizing: border-box;
}

/* 隐藏浏览器自带的密码显示/清除图标（主要针对 Edge） */
.password-field input::-ms-reveal,
.password-field input::-ms-clear {
  display: none;
}

/* 修改：移除多余字体属性，采用 flex 居中图标 */
.password-toggle {
  position: absolute;
  top: 50%;
  right: 1rem;
  transform: translateY(-50%);
  border: none;
  background: transparent;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-muted);
  cursor: pointer;
  transition: opacity 0.3s ease, color 0.3s ease;
}

.password-toggle:hover:not(:disabled) {
  color: var(--text-main);
}

.password-toggle:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.error-banner {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.85rem;
  color: #8C4A4A;
  background: #FCF4F4;
  border: 1px solid rgba(140, 74, 74, 0.2);
  padding: 0.75rem 1rem;
  border-radius: 8px;
}

.error-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  background: #8C4A4A;
  color: white;
  border-radius: 50%;
  font-size: 0.7rem;
  font-weight: bold;
}

/* 带边框的圆角按钮 */
.submit-btn {
  background: var(--text-main);
  color: #FFFFFF;
  border: 1px solid var(--text-main);
  padding: 1.1rem;
  border-radius: 12px;
  font-family: 'Manrope', sans-serif;
  font-size: 0.95rem;
  font-weight: 500;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 0.5rem;
  transition: all 0.3s ease;
}

.submit-btn:hover:not(:disabled) {
  background: var(--accent);
  border-color: var(--accent);
}

.submit-btn:active:not(:disabled) {
  transform: scale(0.98);
}

.submit-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-arrow {
  transition: transform 0.3s ease;
}

.submit-btn:hover:not(:disabled) .btn-arrow {
  transform: translateX(4px);
}

.form-footer {
  margin-top: 2rem;
  text-align: center;
  font-size: 0.8rem;
  color: var(--text-muted);
}

.register-link {
  margin-top: 0.8rem;
  border: none;
  background: transparent;
  color: var(--text-main);
  cursor: pointer;
  font-family: 'Noto Serif SC', serif;
  font-size: 0.9rem;
  font-weight: 600;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.register-link:hover {
  color: var(--accent);
  transform: translateY(-2px);
}

.register-link span {
  display: inline-block;
  transition: transform 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.register-link:hover span {
  transform: translateX(4px);
}

/* 响应式调整 */
@media (max-width: 900px) {
  .login-viewport {
    padding: 0;
  }

  .login-frame {
    flex-direction: column;
    border-radius: 0;
    border: none;
    height: 100vh;
  }

  .visual-panel {
    padding: 2rem;
    flex: 0 0 auto;
    border-bottom: 1px solid var(--border-color);
  }

  .decorative-graphic {
    display: none;
  }

  .interaction-panel {
    border-left: none;
    padding: 2rem;
    align-items: flex-start;
  }
}
</style>
