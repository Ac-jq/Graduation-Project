<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { createStudentAppointmentApi, fetchStudentAppointmentSlotsApi } from '@/api/appointment'
import type { Appointment, AppointmentSlot } from '@/api/types'
import { toErrorMessage } from '@/views/shared/page-logic'

const loading = ref(false)
const submitting = ref(false)
const errorMessage = ref('')
const slots = ref<AppointmentSlot[]>([])
const createdAppointment = ref<Appointment | null>(null)
const createForm = reactive({
  slotId: null as number | null,
  issueSummary: ''
})

async function loadSlots(): Promise<void> {
  loading.value = true
  errorMessage.value = ''

  try {
    slots.value = await fetchStudentAppointmentSlotsApi()
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    loading.value = false
  }
}

async function createAppointment(): Promise<void> {
  if (!createForm.slotId) {
    errorMessage.value = '请先选择一个适合您的时间'
    return
  }

  submitting.value = true
  errorMessage.value = ''

  try {
    createdAppointment.value = await createStudentAppointmentApi({
      slotId: createForm.slotId,
      issueSummary: createForm.issueSummary
    })
    await loadSlots()
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    submitting.value = false
  }
}

// 优雅化时间显示
function formatTime(start: string | Date, end: string | Date): string {
  const d1 = new Date(start)
  const d2 = new Date(end)
  const month = d1.getMonth() + 1
  const date = d1.getDate()
  const h1 = String(d1.getHours()).padStart(2, '0')
  const m1 = String(d1.getMinutes()).padStart(2, '0')
  const h2 = String(d2.getHours()).padStart(2, '0')
  const m2 = String(d2.getMinutes()).padStart(2, '0')
  return `${month}月${date}日 ${h1}:${m1} - ${h2}:${m2}`
}

onMounted(() => {
  void loadSlots()
})
</script>

<template>
  <main class="premium-appointment-page">
    <div class="premium-card">

      <div v-if="createdAppointment" class="success-container">
        <div class="success-icon"></div>
        <h2 class="main-title">预约已确认</h2>
        <p class="sub-title">我们会在约定的时间等您。</p>

        <div class="receipt-box">
          <p><strong>预约编号</strong> <span>#{{ createdAppointment.appointmentId }}</span></p>
          <p><strong>匿名称呼</strong> <span>{{ createdAppointment.anonymousName }}</span></p>
          <p><strong>预约时间</strong>
            <span>{{ formatTime(createdAppointment.startTime, createdAppointment.endTime) }}</span>
          </p>
        </div>
      </div>

      <div v-else class="form-container">
        <header class="card-header">
          <span class="premium-tag">咨询预约</span>
          <h1 class="main-title">预约一次交谈</h1>
          <p class="sub-title">在这些安静的时段里，挑选一个属于你的时间。</p>
        </header>

        <div v-if="errorMessage" class="error-banner">{{ errorMessage }}</div>

        <section class="form-section">
          <div class="section-title-row">
            <h2 class="section-title">选择时段</h2>
            <span v-if="loading" class="status-text">正在读取...</span>
            <span v-else class="status-text">共 {{ slots.length }} 个可用时段</span>
          </div>

          <div v-if="!loading && slots.length === 0" class="empty-state">
            当前没有可预约时段，请稍后再试。
          </div>

          <div v-else class="slots-grid">
            <label
                v-for="slot in slots"
                :key="slot.slotId"
                class="slot-block"
                :class="{ 'slot-block--active': createForm.slotId === slot.slotId }"
            >
              <input
                  type="radio"
                  name="appointment-slot"
                  class="hidden-radio"
                  :checked="createForm.slotId === slot.slotId"
                  @change="createForm.slotId = slot.slotId"
              />
              <div class="slot-content">
                <span class="slot-counselor">咨询师 {{ slot.counselorName || `#${slot.counselorUserId}` }}</span>
                <span class="slot-time">{{ formatTime(slot.startTime, slot.endTime) }}</span>
              </div>
            </label>
          </div>
        </section>

        <section class="form-section">
          <h2 class="section-title">想聊些什么？</h2>
          <textarea
              v-model="createForm.issueSummary"
              class="premium-textarea"
              rows="5"
              maxlength="1000"
              placeholder="写下希望在咨询中优先讨论的主题，或此刻卡住的情境（选填）。"
          ></textarea>
        </section>

        <footer class="card-footer">
          <button
              class="submit-action-btn"
              :disabled="submitting || loading"
              @click="createAppointment"
          >
            {{ submitting ? '正在提交预约...' : '确认发起预约' }}
          </button>
        </footer>
      </div>

    </div>
  </main>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600&family=Noto+Serif+SC:wght@500;600;700&display=swap');

/* 全局背景与居中布局 */
.premium-appointment-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f4f6f4;
  padding: 5vw 2rem;
  box-sizing: border-box;
}

/* 高级感毛玻璃卡片 */
.premium-card {
  width: 100%;
  max-width: 680px;
  background: linear-gradient(
      145deg,
      rgba(219, 230, 222, 0.65) 0%,
      rgba(238, 228, 218, 0.55) 100%
  );
  backdrop-filter: blur(32px);
  -webkit-backdrop-filter: blur(32px);
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 40px;
  padding: 4rem;
  box-sizing: border-box;
  box-shadow:
      0 40px 80px rgba(54, 66, 58, 0.08),
      inset 0 2px 0 rgba(255, 255, 255, 0.6);
  display: flex;
  flex-direction: column;
}

/* 头部排版 */
.card-header {
  text-align: center;
  margin-bottom: 3.5rem;
}

.premium-tag {
  display: inline-block;
  font-family: 'Noto Serif SC', serif;
  font-size: 0.85rem;
  letter-spacing: 0.1em;
  color: #4a5c51;
  background: rgba(255, 255, 255, 0.5);
  padding: 0.5rem 1.2rem;
  border-radius: 100px;
  font-weight: 600;
  margin-bottom: 1.5rem;
}

.main-title {
  font-family: 'Noto Serif SC', serif;
  font-size: clamp(2.2rem, 4vw, 3rem);
  font-weight: 600;
  color: #1e2821;
  margin: 0 0 1rem 0;
  letter-spacing: 0.05em;
}

.sub-title {
  font-family: 'Noto Serif SC', serif;
  font-size: 1.1rem;
  color: #5c6b60;
  margin: 0;
}

/* 内部区块布局 */
.form-container {
  display: flex;
  flex-direction: column;
  gap: 3rem;
}

.form-section {
  display: flex;
  flex-direction: column;
  gap: 1.2rem;
}

.section-title-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
}

.section-title {
  font-family: 'Noto Serif SC', serif;
  font-size: 1.25rem;
  font-weight: 600;
  color: #2a362e;
  margin: 0;
}

.status-text {
  font-family: 'Noto Serif SC', serif;
  font-size: 0.9rem;
  color: #7b8c80;
}

/* 时段选择网格 */
.slots-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 1rem;
  max-height: 280px;
  overflow-y: auto;
  padding-right: 0.5rem;
}

/* 自定义滚动条 */
.slots-grid::-webkit-scrollbar {
  width: 6px;
}
.slots-grid::-webkit-scrollbar-track {
  background: transparent;
}
.slots-grid::-webkit-scrollbar-thumb {
  background: rgba(130, 150, 138, 0.3);
  border-radius: 10px;
}

.hidden-radio {
  position: absolute;
  opacity: 0;
  pointer-events: none;
}

.slot-block {
  display: block;
  background: rgba(255, 255, 255, 0.4);
  border: 1px solid rgba(130, 150, 138, 0.15);
  border-radius: 20px;
  padding: 1.2rem 1.5rem;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.slot-block:hover {
  background: rgba(255, 255, 255, 0.8);
  border-color: rgba(130, 150, 138, 0.4);
  transform: translateY(-2px);
}

.slot-block--active {
  background: #2a362e;
  border-color: #2a362e;
  box-shadow: 0 12px 24px rgba(42, 54, 46, 0.15);
  transform: translateY(-2px);
}

.slot-content {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.slot-counselor {
  font-family: 'Noto Serif SC', serif;
  font-size: 1.05rem;
  font-weight: 600;
  color: #4a5c51;
  transition: color 0.3s ease;
}

.slot-time {
  font-family: 'Manrope', sans-serif;
  font-size: 0.9rem;
  color: #6a7c70;
  transition: color 0.3s ease;
}

.slot-block--active .slot-counselor,
.slot-block--active .slot-time {
  color: #ffffff;
}

/* 文本域 */
.premium-textarea {
  width: 100%;
  box-sizing: border-box;
  resize: none;
  background: rgba(255, 255, 255, 0.4);
  border: 1px solid rgba(130, 150, 138, 0.2);
  border-radius: 20px;
  padding: 1.5rem;
  font-family: 'Noto Serif SC', serif;
  font-size: 1rem;
  line-height: 1.8;
  color: #2a362e;
  outline: none;
  transition: all 0.3s ease;
}

.premium-textarea::placeholder {
  color: #8a9c90;
}

.premium-textarea:focus {
  background: rgba(255, 255, 255, 0.85);
  border-color: rgba(130, 150, 138, 0.5);
  box-shadow: 0 8px 24px rgba(42, 54, 46, 0.05);
}

/* 底部操作按钮 */
.card-footer {
  display: flex;
  justify-content: center;
  margin-top: 1rem;
}

.submit-action-btn {
  width: 100%;
  height: 4.2rem;
  border-radius: 100px;
  border: none;
  background: #2a362e;
  color: #ffffff;
  font-family: 'Noto Serif SC', serif;
  font-size: 1.1rem;
  font-weight: 600;
  letter-spacing: 0.1em;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1);
  box-shadow: 0 12px 24px rgba(42, 54, 46, 0.2);
}

.submit-action-btn:hover:not(:disabled) {
  transform: translateY(-4px) scale(1.01);
  box-shadow: 0 20px 40px rgba(42, 54, 46, 0.3);
  background: #1c2620;
}

.submit-action-btn:disabled {
  background: #7a8c80;
  cursor: not-allowed;
  box-shadow: none;
}

/* 状态提示 */
.error-banner {
  background: rgba(140, 74, 74, 0.1);
  border: 1px solid rgba(140, 74, 74, 0.3);
  color: #8c4a4a;
  padding: 1rem 1.5rem;
  border-radius: 16px;
  font-family: 'Noto Serif SC', serif;
  text-align: center;
}

.empty-state {
  text-align: center;
  padding: 3rem 0;
  color: #7b8c80;
  font-family: 'Noto Serif SC', serif;
}

/* 成功状态 */
.success-container {
  text-align: center;
  padding: 2rem 0;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.success-icon {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: #2a362e;
  margin-bottom: 2rem;
  position: relative;
}

.success-icon::after {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  width: 14px;
  height: 24px;
  border: solid #ffffff;
  border-width: 0 3px 3px 0;
  transform: translate(-50%, -60%) rotate(45deg);
}

.receipt-box {
  margin-top: 3rem;
  width: 100%;
  background: rgba(255, 255, 255, 0.5);
  border-radius: 24px;
  padding: 2rem;
  display: flex;
  flex-direction: column;
  gap: 1.2rem;
  text-align: left;
}

.receipt-box p {
  margin: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-family: 'Noto Serif SC', serif;
  border-bottom: 1px solid rgba(130, 150, 138, 0.15);
  padding-bottom: 0.8rem;
}

.receipt-box p:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.receipt-box strong {
  color: #7b8c80;
  font-weight: 500;
}

.receipt-box span {
  color: #2a362e;
  font-weight: 600;
  font-family: 'Manrope', 'Noto Serif SC', sans-serif;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .premium-card {
    padding: 2.5rem 1.5rem;
    border-radius: 32px;
  }
  .slots-grid {
    grid-template-columns: 1fr;
  }
}
</style>