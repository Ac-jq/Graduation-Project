<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { registerApi } from '@/api/auth'
import type { RegisterRequest } from '@/api/types'
import { toErrorMessage } from '@/views/shared/page-logic'

type WizardStep = 1 | 2 | 3

const router = useRouter()
const currentStep = ref<WizardStep>(1)
const formError = ref('')
const submitting = ref(false)

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

const gradeOptions = ['2022', '2023', '2025', '2026']
const genderOptions = ['男', '女']

const registerForm = reactive<RegisterRequest>({
  account: '',
  password: '',
  realName: '',
  displayName: '',
  studentNo: '',
  gender: '',
  grade: '',
  college: ''
})

const stepMeta = [
  { step: 1, label: '账号与安全', caption: '访问凭证' },
  { step: 2, label: '身份认证', caption: '学生身份' },
  { step: 3, label: '个人档案', caption: '基础画像' }
]

const activeStepTitle = computed(() => stepMeta.find((item) => item.step === currentStep.value)?.label ?? '')
const canGoBack = computed(() => currentStep.value > 1 && !submitting.value)
const isFinalStep = computed(() => currentStep.value === 3)

function validateCurrentStep(): boolean {
  formError.value = ''

  if (currentStep.value === 1) {
    if (!registerForm.account.trim() || !registerForm.password.trim()) {
      formError.value = '请先填写账号和密码。'
      return false
    }
    if (registerForm.password.length < 6) {
      formError.value = '密码至少需要 6 位。'
      return false
    }
  }

  if (currentStep.value === 2) {
    if (!registerForm.realName.trim() || !registerForm.displayName.trim() || !registerForm.studentNo.trim()) {
      formError.value = '请完整填写真实姓名、展示昵称和学号。'
      return false
    }
  }

  if (currentStep.value === 3) {
    if (!registerForm.gender || !registerForm.grade || !registerForm.college) {
      formError.value = '请选择性别、年级和学院。'
      return false
    }
  }

  return true
}

async function nextStep(): Promise<void> {
  if (submitting.value || !validateCurrentStep()) {
    return
  }

  if (currentStep.value < 3) {
    currentStep.value = (currentStep.value + 1) as WizardStep
    return
  }

  await submitRegister()
}

function prevStep(): void {
  formError.value = ''
  if (canGoBack.value) {
    currentStep.value = (currentStep.value - 1) as WizardStep
  }
}

function chooseOption(field: 'gender' | 'grade', value: string): void {
  if (!submitting.value) {
    registerForm[field] = value
  }
}

async function submitRegister(): Promise<void> {
  submitting.value = true
  formError.value = ''

  try {
    await registerApi({
      account: registerForm.account.trim(),
      password: registerForm.password,
      realName: registerForm.realName.trim(),
      displayName: registerForm.displayName.trim(),
      studentNo: registerForm.studentNo.trim(),
      gender: registerForm.gender,
      grade: registerForm.grade,
      college: registerForm.college
    })
    ElMessage.success('注册成功，请使用新账号登录。')
    await router.replace('/login')
  } catch (error) {
    formError.value = toErrorMessage(error)
  } finally {
    submitting.value = false
  }
}

async function goLogin(): Promise<void> {
  if (!submitting.value) {
    await router.push('/login')
  }
}
</script>

<template>
  <main class="register-viewport">
    <div class="noise-texture"></div>

    <section class="register-frame">
      <aside class="visual-panel">
        <div class="visual-orbit" aria-hidden="true">
          <span class="aurora aurora--sage"></span>
          <span class="aurora aurora--sand"></span>
          <span class="glass-shape glass-shape--arch"></span>
          <span class="glass-shape glass-shape--circle"></span>
          <span class="glass-shape glass-shape--pill"></span>
          <svg class="orbit-line" viewBox="0 0 420 420" fill="none">
            <path d="M78 244C50 163 110 76 204 72C310 68 377 156 356 251C337 335 246 384 159 346C112 326 92 286 78 244Z" />
            <path d="M132 118C202 61 315 106 330 198C348 310 210 386 122 314C56 260 64 174 132 118Z" />
          </svg>
        </div>

        <div class="step-rail" aria-label="注册步骤">
          <button
            v-for="item in stepMeta"
            :key="item.step"
            type="button"
            class="step-dot"
            :class="{ 'is-active': currentStep === item.step, 'is-done': currentStep > item.step }"
            :aria-current="currentStep === item.step ? 'step' : undefined"
          >
            <span>{{ item.step }}</span>
            <strong>{{ item.label }}</strong>
            <em>{{ item.caption }}</em>
          </button>
        </div>
      </aside>

      <section class="interaction-panel">
        <div class="form-wrapper">
          <div class="form-header">
            <span class="wizard-count">Step {{ currentStep }} / 3</span>
            <h1>{{ activeStepTitle }}</h1>
            <p>创建学生账号，开启你的心理支持空间。</p>
          </div>

          <transition name="slide-x" mode="out-in">
            <form v-if="currentStep === 1" key="step-account" class="wizard-form" @submit.prevent="nextStep">
              <label class="field-line">
                <span>账号 / 用户名</span>
                <input
                  v-model="registerForm.account"
                  type="text"
                  autocomplete="username"
                  placeholder="例如 20220353"
                  :disabled="submitting"
                >
              </label>

              <label class="field-line">
                <span>密码</span>
                <input
                  v-model="registerForm.password"
                  type="password"
                  autocomplete="new-password"
                  placeholder="至少 6 位安全密码"
                  :disabled="submitting"
                >
              </label>
            </form>

            <form v-else-if="currentStep === 2" key="step-identity" class="wizard-form" @submit.prevent="nextStep">
              <label class="field-line">
                <span>真实姓名</span>
                <input v-model="registerForm.realName" type="text" autocomplete="name" placeholder="用于校内身份核验" :disabled="submitting">
              </label>

              <label class="field-line">
                <span>展示昵称</span>
                <input v-model="registerForm.displayName" type="text" placeholder="例如 向日葵同学" :disabled="submitting">
              </label>

              <label class="field-line">
                <span>学号</span>
                <input v-model="registerForm.studentNo" type="text" inputmode="numeric" placeholder="请输入你的学号" :disabled="submitting">
              </label>
            </form>

            <form v-else key="step-profile" class="wizard-form" @submit.prevent="nextStep">
              <div class="choice-block">
                <span class="field-caption">性别</span>
                <div class="choice-grid choice-grid--two">
                  <button
                    v-for="gender in genderOptions"
                    :key="gender"
                    type="button"
                    class="choice-pill"
                    :class="{ 'is-selected': registerForm.gender === gender }"
                    :disabled="submitting"
                    @click="chooseOption('gender', gender)"
                  >
                    {{ gender }}
                  </button>
                </div>
              </div>

              <div class="choice-block">
                <span class="field-caption">年级</span>
                <div class="choice-grid">
                  <button
                    v-for="grade in gradeOptions"
                    :key="grade"
                    type="button"
                    class="choice-pill"
                    :class="{ 'is-selected': registerForm.grade === grade }"
                    :disabled="submitting"
                    @click="chooseOption('grade', grade)"
                  >
                    {{ grade }}
                  </button>
                </div>
              </div>

              <label class="field-line select-line">
                <span>学院</span>
                <select v-model="registerForm.college" :disabled="submitting">
                  <option disabled value="">请选择学院</option>
                  <option v-for="college in collegeOptions" :key="college" :value="college">
                    {{ college }}
                  </option>
                </select>
              </label>
            </form>
          </transition>

          <p v-if="formError" class="form-error" role="alert">{{ formError }}</p>

          <div class="wizard-actions">
            <button class="ghost-action" type="button" :disabled="!canGoBack" @click="prevStep">上一步</button>
            <button class="primary-action" type="button" :disabled="submitting" @click="nextStep">
              {{ submitting ? '正在创建...' : (isFinalStep ? '完成注册' : '下一步') }}
              <span>→</span>
            </button>
          </div>

          <button class="login-link" type="button" :disabled="submitting" @click="goLogin">已有账号，返回登录</button>
        </div>
      </section>
    </section>
  </main>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700&family=Noto+Serif+SC:wght@500;600;700&display=swap');

.register-viewport {
  --bg-viewport: #e8e5df;
  --bg-frame: #f4f1ea;
  --text-main: #2c302b;
  --text-muted: #7a7d75;
  --border-color: rgba(44, 48, 43, 0.15);
  --accent: #6a7a6b;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding: 4vmin;
  background-color: var(--bg-viewport);
  color: var(--text-main);
  font-family: 'Manrope', sans-serif;
  box-sizing: border-box;
}

.noise-texture {
  position: absolute;
  inset: 0;
  pointer-events: none;
  opacity: 0.24;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noiseFilter'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.85' numOctaves='3' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noiseFilter)'/%3E%3C/svg%3E");
  mix-blend-mode: multiply;
}

.register-frame {
  position: relative;
  z-index: 1;
  display: flex;
  width: 100%;
  max-width: 1200px;
  height: min(800px, 90vh);
  overflow: hidden;
  border: 1px solid var(--border-color);
  border-radius: 24px;
  background-color: var(--bg-frame);
  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.04);
}

.visual-panel {
  flex: 1.2;
  position: relative;
  display: flex;
  align-items: flex-end;
  padding: 4rem;
  overflow: hidden;
}

.visual-panel::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 18% 24%, rgba(176, 198, 178, 0.42), transparent 32%),
    radial-gradient(circle at 82% 72%, rgba(231, 208, 183, 0.38), transparent 30%),
    linear-gradient(145deg, rgba(255, 255, 255, 0.22), rgba(106, 122, 107, 0.04));
  animation: auroraDrift 10s ease-in-out infinite alternate;
}

.visual-orbit {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
}

.aurora,
.glass-shape,
.orbit-line {
  position: absolute;
  pointer-events: none;
}

.aurora {
  width: 24rem;
  height: 24rem;
  border-radius: 999px;
  filter: blur(24px);
  opacity: 0.6;
}

.aurora--sage {
  top: 7%;
  left: 10%;
  background: radial-gradient(circle, rgba(154, 180, 160, 0.45), transparent 68%);
  animation: floatSlow 8s ease-in-out infinite;
}

.aurora--sand {
  right: 5%;
  bottom: 8%;
  background: radial-gradient(circle, rgba(231, 205, 174, 0.52), transparent 68%);
  animation: floatSlow 9s ease-in-out infinite reverse;
}

.glass-shape {
  border: 1px solid rgba(255, 255, 255, 0.56);
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.5), rgba(255, 255, 255, 0.08));
  box-shadow: 0 28px 80px rgba(54, 66, 58, 0.08);
  backdrop-filter: blur(18px);
}

.glass-shape--arch {
  width: 12rem;
  height: 17rem;
  left: 12%;
  top: 16%;
  border-radius: 999px 999px 28px 28px;
  animation: breathe 5.5s ease-in-out infinite;
}

.glass-shape--circle {
  width: 9rem;
  height: 9rem;
  right: 17%;
  top: 19%;
  border-radius: 999px;
  animation: floatSlow 7s ease-in-out infinite;
}

.glass-shape--pill {
  width: 17rem;
  height: 6rem;
  right: 10%;
  bottom: 21%;
  border-radius: 999px;
  transform: rotate(-14deg);
  animation: breathe 6.5s ease-in-out infinite reverse;
}

.orbit-line {
  width: min(72%, 30rem);
  height: auto;
  color: rgba(44, 48, 43, 0.18);
  stroke: currentColor;
  stroke-width: 1.2;
  transform: translateY(-1rem);
  animation: orbitRotate 18s linear infinite;
}

.step-rail {
  position: relative;
  z-index: 2;
  display: grid;
  gap: 0.85rem;
  width: min(100%, 24rem);
}

.step-dot {
  display: grid;
  grid-template-columns: 2.7rem minmax(0, 1fr);
  column-gap: 0.9rem;
  align-items: center;
  width: 100%;
  border: none;
  border-radius: 22px;
  padding: 0.95rem;
  background: rgba(255, 255, 255, 0.6);
  color: var(--text-muted);
  text-align: left;
  cursor: default;
  box-shadow: 0 18px 44px rgba(54, 66, 58, 0.04);
  backdrop-filter: blur(18px);
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.step-dot span {
  grid-row: span 2;
  width: 2.7rem;
  height: 2.7rem;
  display: grid;
  place-items: center;
  border-radius: 16px;
  background: rgba(106, 122, 107, 0.12);
  color: var(--accent);
  font-weight: 800;
}

.step-dot strong {
  color: var(--text-main);
  font: 600 0.98rem/1.3 'Noto Serif SC', serif;
}

.step-dot em {
  font-style: normal;
  font-size: 0.78rem;
}

.step-dot.is-active,
.step-dot.is-done {
  background: rgba(255, 255, 255, 0.9);
  transform: translateX(6px);
}

.step-dot.is-active span,
.step-dot.is-done span {
  background: var(--text-main);
  color: #fff;
}

.interaction-panel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  border-left: 1px solid var(--border-color);
  background: #fff;
  padding: 4rem;
}

.form-wrapper {
  width: 100%;
  max-width: 360px;
}

.form-header {
  margin-bottom: 2.6rem;
}

.wizard-count,
.field-caption {
  color: #8a9c90;
  font: 700 0.76rem/1 'Manrope', sans-serif;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.form-header h1 {
  margin: 0.75rem 0 0.55rem;
  color: var(--text-main);
  font: 600 2.2rem/1.08 'Noto Serif SC', serif;
}

.form-header p {
  margin: 0;
  color: var(--text-muted);
  font-size: 0.92rem;
}

.wizard-form {
  display: grid;
  gap: 1.45rem;
  min-height: 320px;
}

.field-line {
  display: grid;
  gap: 0.55rem;
}

.field-line span {
  color: var(--text-main);
  font: 700 0.82rem/1 'Manrope', sans-serif;
  letter-spacing: 0.08em;
}

.field-line input,
.field-line select {
  width: 100%;
  height: 44px;
  border: 1px solid rgba(44, 48, 43, 0.1);
  border-radius: 12px;
  background: var(--bg-frame);
  color: var(--text-main);
  box-sizing: border-box;
  padding: 0 0.95rem;
  font: 600 0.92rem/1 'Manrope', sans-serif;
  outline: none;
  box-shadow: 0 14px 30px rgba(54, 66, 58, 0.035);
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.field-line input:focus,
.field-line select:focus {
  border-color: var(--accent);
  background: #fff;
  box-shadow: 0 0 0 4px rgba(106, 122, 107, 0.1);
  transform: translateY(-2px);
}

.field-line input:disabled,
.field-line select:disabled {
  cursor: not-allowed;
  opacity: 0.62;
}

.field-line input::placeholder {
  color: rgba(122, 125, 117, 0.52);
  font-weight: 500;
}

.choice-block {
  display: grid;
  gap: 0.8rem;
}

.choice-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0.7rem;
}

.choice-grid--two {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.choice-pill {
  min-height: 3.15rem;
  border: 1px solid rgba(44, 48, 43, 0.1);
  border-radius: 14px;
  background: var(--bg-frame);
  color: var(--text-muted);
  cursor: pointer;
  font: 700 0.9rem/1 'Manrope', sans-serif;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.choice-pill:hover:not(:disabled) {
  transform: translateY(-3px);
  box-shadow: 0 16px 30px rgba(54, 66, 58, 0.08);
}

.choice-pill.is-selected {
  background: var(--text-main);
  border-color: var(--text-main);
  color: #fff;
  box-shadow: 0 18px 34px rgba(44, 48, 43, 0.18);
}

.choice-pill:disabled {
  cursor: not-allowed;
  opacity: 0.62;
}

.form-error {
  margin: 1rem 0 0;
  padding: 0.8rem 0.95rem;
  border-radius: 12px;
  background: #fcf4f4;
  color: #8c4a4a;
  font: 600 0.85rem/1.5 'Manrope', sans-serif;
}

.wizard-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  margin-top: 1.8rem;
}

.ghost-action,
.primary-action,
.login-link {
  border: none;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.ghost-action {
  background: transparent;
  color: var(--text-muted);
  font: 700 0.95rem/1 'Noto Serif SC', serif;
}

.ghost-action:hover:not(:disabled) {
  color: var(--text-main);
  transform: translateX(-4px);
}

.ghost-action:disabled {
  cursor: not-allowed;
  opacity: 0.35;
}

.primary-action {
  min-height: 3.3rem;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.55rem;
  padding: 0 1.45rem;
  border-radius: 14px;
  background: var(--text-main);
  color: #fff;
  font: 700 0.96rem/1 'Noto Serif SC', serif;
  box-shadow: 0 18px 34px rgba(44, 48, 43, 0.16);
}

.primary-action:hover:not(:disabled) {
  background: var(--accent);
  transform: translateY(-3px);
  box-shadow: 0 24px 42px rgba(44, 48, 43, 0.18);
}

.primary-action:hover:not(:disabled) span {
  transform: translateX(4px);
}

.primary-action span {
  transition: transform 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.primary-action:disabled {
  cursor: not-allowed;
  opacity: 0.62;
}

.login-link {
  display: block;
  margin: 1.6rem auto 0;
  background: transparent;
  color: var(--text-muted);
  font: 700 0.82rem/1 'Manrope', sans-serif;
}

.login-link:hover:not(:disabled) {
  color: var(--text-main);
  transform: translateY(-2px);
}

.login-link:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.slide-x-enter-active,
.slide-x-leave-active {
  transition: opacity 0.32s ease, transform 0.32s cubic-bezier(0.16, 1, 0.3, 1);
}

.slide-x-enter-from {
  opacity: 0;
  transform: translateX(28px);
}

.slide-x-leave-to {
  opacity: 0;
  transform: translateX(-28px);
}

@keyframes breathe {
  0%, 100% {
    transform: scale(0.96);
    opacity: 0.72;
  }

  50% {
    transform: scale(1.04);
    opacity: 1;
  }
}

@keyframes floatSlow {
  0%, 100% {
    transform: translate3d(0, 0, 0);
  }

  50% {
    transform: translate3d(1rem, -1rem, 0);
  }
}

@keyframes orbitRotate {
  to {
    transform: translateY(-1rem) rotate(360deg);
  }
}

@keyframes auroraDrift {
  from {
    transform: scale(1);
  }

  to {
    transform: scale(1.06) translate3d(-1.2rem, 0.8rem, 0);
  }
}

@media (max-width: 900px) {
  .register-viewport {
    padding: 0;
  }

  .register-frame {
    flex-direction: column;
    min-height: 100vh;
    height: auto;
    border: none;
    border-radius: 0;
  }

  .visual-panel {
    flex: 0 0 auto;
    min-height: 18rem;
    padding: 2rem;
    border-bottom: 1px solid var(--border-color);
  }

  .interaction-panel {
    border-left: none;
    padding: 2rem;
    align-items: flex-start;
  }

  .form-wrapper {
    max-width: none;
  }

  .choice-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 560px) {
  .visual-panel {
    min-height: 16rem;
  }

  .step-dot {
    padding: 0.75rem;
  }

  .wizard-actions {
    flex-direction: column-reverse;
    align-items: stretch;
  }

  .primary-action {
    width: 100%;
  }
}
</style>
