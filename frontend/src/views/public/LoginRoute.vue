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
              <input
                  id="password"
                  v-model="form.password"
                  type="password"
                  autocomplete="current-password"
                  placeholder="请输入登录密码"
                  :disabled="submitting"
              >
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
  --bg-viewport: #E8E5DF; /* 更深一点的外层底色，突显画板 */
  --bg-frame: #F4F1EA;    /* 画板内部底色，呼应学生端主题 */
  --text-main: #2C302B;
  --text-muted: #7A7D75;
  --border-color: rgba(44, 48, 43, 0.15); /* 明确的边框颜色 */
  --accent: #6A7A6B;      /* 鼠尾草绿 */

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
  border-radius: 24px; /* 呼应学生端的圆角 */
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
  border-left: 1px solid var(--border-color); /* 明确的中轴线边框 */
  background-color: #FFFFFF; /* 右侧微微提亮 */
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
  border-radius: 12px; /* 柔和的边框内角 */
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

/* 响应式调整 */
@media (max-width: 900px) {
  .login-viewport {
    padding: 0; /* 移动端去掉外围边距 */
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
    display: none; /* 移动端隐藏装饰图形节省空间 */
  }

  .interaction-panel {
    border-left: none;
    padding: 2rem;
    align-items: flex-start;
  }
}
</style>