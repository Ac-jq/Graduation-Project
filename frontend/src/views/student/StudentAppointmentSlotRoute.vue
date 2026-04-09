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
    errorMessage.value = '请选择预约时段'
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

onMounted(() => {
  void loadSlots()
})
</script>

<template>
  <section class="appointment-page">
    <div class="page-shell">
      <header class="page-hero">
        <div class="hero-copy">
          <p class="eyebrow">咨询预约</p>
          <h1>在可用时段里预约一场更安静、更具体的人际支持。</h1>
          <p class="lead">
            页面展示的时段、咨询师信息与创建结果均来自后端接口。提交后会即时生成真实预约记录。
          </p>
        </div>
        <div class="hero-aside">
          <div class="metric-card">
            <span>可预约时段</span>
            <strong>{{ slots.length }}</strong>
          </div>
          <div class="metric-card">
            <span>当前选择</span>
            <strong>{{ createForm.slotId ? `#${createForm.slotId}` : '未选择' }}</strong>
          </div>
        </div>
      </header>

      <div class="editorial-grid">
        <section class="slot-panel">
          <div class="section-head section-head-inline">
            <div>
              <p class="section-kicker">可用时段</p>
              <h2>选择预约时段</h2>
            </div>
            <span class="status-chip">{{ loading ? '同步中' : `${slots.length} 个时段` }}</span>
          </div>

          <p v-if="loading" class="state-text">正在读取可预约时段...</p>
          <p v-else-if="!slots.length" class="state-text">当前没有可预约时段，请稍后再试。</p>

          <div v-else class="slot-stack">
            <label
              v-for="slot in slots"
              :key="slot.slotId"
              class="slot-card"
              :class="{ 'slot-card--active': createForm.slotId === slot.slotId }"
            >
              <input
                class="slot-radio"
                type="radio"
                name="appointment-slot"
                :checked="createForm.slotId === slot.slotId"
                @change="createForm.slotId = slot.slotId"
              />
              <div class="slot-body">
                <div class="slot-topline">
                  <p class="slot-name">咨询师 {{ slot.counselorName || `#${slot.counselorUserId}` }}</p>
                  <span class="slot-status">{{ slot.status }}</span>
                </div>
                <p class="slot-time">
                  {{ new Date(slot.startTime).toLocaleString('zh-CN') }} - {{ new Date(slot.endTime).toLocaleString('zh-CN') }}
                </p>
              </div>
            </label>
          </div>
        </section>

        <aside class="booking-panel glass-panel">
          <div class="section-head">
            <p class="section-kicker">问题摘要</p>
            <h2>描述你的来访议题</h2>
          </div>
          <textarea
            v-model="createForm.issueSummary"
            class="summary-textarea"
            rows="10"
            maxlength="1000"
            placeholder="你可以写下希望咨询中优先讨论的主题、最近的感受或卡住的情境。"
          />
          <button class="primary-button" type="button" :disabled="submitting" @click="createAppointment">
            {{ submitting ? '正在提交预约...' : '确认发起预约' }}
          </button>
          <p v-if="errorMessage" class="error-text">{{ errorMessage }}</p>

          <div v-if="createdAppointment" class="result-card">
            <p class="result-kicker">预约结果</p>
            <h3>预约已提交</h3>
            <p class="result-line">预约编号：#{{ createdAppointment.appointmentId }}</p>
            <p class="result-line">匿名称呼：{{ createdAppointment.anonymousName }}</p>
            <p class="result-line">状态：{{ createdAppointment.status }}</p>
            <p class="result-line">
              时间：{{ new Date(createdAppointment.startTime).toLocaleString('zh-CN') }} -
              {{ new Date(createdAppointment.endTime).toLocaleString('zh-CN') }}
            </p>
          </div>
        </aside>
      </div>
    </div>
  </section>
</template>
<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700&family=Noto+Serif+SC:wght@400;500;600;700&display=swap');

:global(body) {
  background:
    radial-gradient(circle at top left, rgba(206, 222, 210, 0.3), transparent 26%),
    radial-gradient(circle at 84% 18%, rgba(225, 214, 198, 0.32), transparent 22%),
    linear-gradient(180deg, #f5f0e6 0%, #f8f5ee 100%);
}

.appointment-page {
  min-height: 100vh;
  padding: 44px 28px 72px;
  color: #2b3029;
}

.page-shell {
  max-width: 1360px;
  margin: 0 auto;
}

.page-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(250px, 0.6fr);
  gap: 28px;
  align-items: end;
  margin-bottom: 34px;
}

.hero-copy {
  border-top: 1px solid rgba(64, 72, 62, 0.16);
  padding-top: 18px;
}

.eyebrow,
.section-kicker,
.result-kicker {
  margin: 0 0 10px;
  font: 700 0.76rem/1 'Manrope', sans-serif;
  letter-spacing: 0.22em;
  text-transform: uppercase;
  color: #7e6955;
}

.hero-copy h1,
.section-head h2,
.result-card h3 {
  margin: 0;
  font-family: 'Noto Serif SC', serif;
  font-weight: 600;
  letter-spacing: -0.02em;
}

.hero-copy h1 {
  font-size: clamp(2rem, 3vw, 3.4rem);
  line-height: 1.16;
}

.lead {
  max-width: 700px;
  margin: 18px 0 0;
  font: 400 1rem/1.84 'Manrope', sans-serif;
  color: rgba(43, 48, 41, 0.74);
}

.hero-aside {
  display: grid;
  gap: 14px;
}

.metric-card,
.glass-panel,
.slot-card {
  border: 1px solid rgba(78, 86, 77, 0.14);
  background: rgba(255, 252, 247, 0.74);
  box-shadow: 0 24px 70px rgba(91, 80, 66, 0.08);
  backdrop-filter: blur(16px);
}

.metric-card {
  padding: 18px 20px;
}

.metric-card span,
.state-text,
.slot-time,
.result-line,
.error-text {
  font-family: 'Manrope', sans-serif;
}

.metric-card span {
  display: block;
  margin-bottom: 8px;
  font-size: 0.78rem;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: rgba(68, 74, 66, 0.56);
}

.metric-card strong {
  font-family: 'Noto Serif SC', serif;
  font-size: 1.6rem;
  font-weight: 600;
}

.editorial-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.12fr) minmax(330px, 0.88fr);
  gap: 28px;
}

.section-head {
  margin-bottom: 18px;
}

.section-head-inline {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: end;
}

.status-chip {
  border: 1px solid rgba(88, 93, 84, 0.14);
  background: rgba(255, 250, 240, 0.82);
  padding: 9px 14px;
  font: 700 0.76rem/1 'Manrope', sans-serif;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #696152;
}

.slot-stack {
  display: grid;
  gap: 16px;
}

.slot-card {
  display: block;
  cursor: pointer;
  transition: transform 0.28s ease, border-color 0.28s ease, box-shadow 0.28s ease;
}

.slot-card:hover {
  transform: translateY(-3px);
}

.slot-card--active {
  border-color: rgba(78, 101, 90, 0.34);
  box-shadow: 0 28px 54px rgba(86, 106, 92, 0.12);
}

.slot-radio {
  position: absolute;
  opacity: 0;
  pointer-events: none;
}

.slot-body {
  padding: 20px 22px;
}

.slot-topline {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: start;
  margin-bottom: 10px;
}

.slot-name {
  margin: 0;
  font-family: 'Noto Serif SC', serif;
  font-size: 1.24rem;
  line-height: 1.35;
}

.slot-status {
  flex-shrink: 0;
  border: 1px solid rgba(97, 111, 98, 0.15);
  background: rgba(242, 244, 237, 0.94);
  padding: 7px 11px;
  font: 700 0.74rem/1 'Manrope', sans-serif;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #66735f;
}

.slot-time {
  margin: 0;
  font-size: 0.96rem;
  line-height: 1.8;
  color: rgba(43, 48, 41, 0.7);
}

.booking-panel {
  padding: 24px;
}

.summary-textarea {
  width: 100%;
  resize: vertical;
  border: 1px solid rgba(80, 88, 79, 0.16);
  background: rgba(255, 255, 255, 0.74);
  padding: 16px 18px;
  font: 400 0.98rem/1.8 'Manrope', sans-serif;
  color: #2b3029;
  outline: none;
  transition: border-color 0.28s ease, box-shadow 0.28s ease, transform 0.28s ease;
}

.summary-textarea:focus {
  border-color: rgba(96, 113, 102, 0.5);
  box-shadow: 0 18px 38px rgba(80, 97, 85, 0.12);
  transform: translateY(-1px);
}

.primary-button {
  margin-top: 18px;
  border: none;
  background: linear-gradient(135deg, #253128 0%, #47564b 100%);
  color: #f8f5ef;
  padding: 15px 20px;
  font: 700 0.92rem/1 'Manrope', sans-serif;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  cursor: pointer;
  transition: transform 0.28s ease, box-shadow 0.28s ease, opacity 0.28s ease;
}

.primary-button:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 22px 38px rgba(37, 49, 40, 0.18);
}

.primary-button:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.error-text,
.state-text {
  margin: 16px 0 0;
  font-size: 0.95rem;
  line-height: 1.8;
}

.error-text {
  font-weight: 600;
  color: #a64a39;
}

.result-card {
  margin-top: 22px;
  border-top: 1px solid rgba(88, 94, 84, 0.16);
  padding-top: 18px;
}

.result-card h3 {
  font-size: 1.42rem;
  line-height: 1.3;
}

.result-line {
  margin: 10px 0 0;
  font-size: 0.95rem;
  line-height: 1.8;
  color: rgba(43, 48, 41, 0.72);
}

@media (max-width: 980px) {
  .page-hero,
  .editorial-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .appointment-page {
    padding: 28px 16px 46px;
  }

  .hero-copy h1,
  .section-head h2 {
    font-size: 1.84rem;
  }

  .slot-topline,
  .section-head-inline {
    flex-direction: column;
    align-items: start;
  }
}
</style>

