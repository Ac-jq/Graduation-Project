<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import {
  createStudentAppointmentApi,
  fetchStudentAppointmentCounselorsApi,
  fetchStudentAppointmentSlotsApi
} from '@/api/appointment'
import type { Appointment, AppointmentCounselorOption, AppointmentSlot } from '@/api/types'
import { toErrorMessage } from '@/views/shared/page-logic'

const loadingCounselors = ref(false)
const loadingSlots = ref(false)
const submitting = ref(false)
const errorMessage = ref('')
const counselorOptions = ref<AppointmentCounselorOption[]>([])
const slots = ref<AppointmentSlot[]>([])
const createdAppointment = ref<Appointment | null>(null)

const form = reactive({
  counselorUserId: null as number | null,
  date: buildDefaultDate(),
  slotId: null as number | null,
  issueSummary: ''
})

const selectedCounselor = computed(() =>
  counselorOptions.value.find((item) => item.counselorUserId === form.counselorUserId) ?? null
)
const selectableSlots = computed(() => slots.value.filter((slot) => slot.isSelectable))
const canSubmit = computed(() => !!form.slotId && !loadingSlots.value && !submitting.value)

function buildDefaultDate(): string {
  const today = new Date()
  const month = String(today.getMonth() + 1).padStart(2, '0')
  const date = String(today.getDate()).padStart(2, '0')
  return `${today.getFullYear()}-${month}-${date}`
}

function formatDateLabel(value: string): string {
  const date = new Date(`${value}T00:00:00`)
  const month = date.getMonth() + 1
  const day = date.getDate()
  const weekDays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  return `${month} 月 ${day} 日 · ${weekDays[date.getDay()]}`
}

async function loadCounselors(): Promise<void> {
  loadingCounselors.value = true
  errorMessage.value = ''

  try {
    counselorOptions.value = await fetchStudentAppointmentCounselorsApi()
    if (!form.counselorUserId && counselorOptions.value.length > 0) {
      form.counselorUserId = counselorOptions.value[0].counselorUserId
    }
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    loadingCounselors.value = false
  }
}

async function loadSlots(): Promise<void> {
  if (!form.counselorUserId || !form.date) {
    slots.value = []
    form.slotId = null
    return
  }

  loadingSlots.value = true
  errorMessage.value = ''

  try {
    slots.value = await fetchStudentAppointmentSlotsApi(form.counselorUserId, form.date)
    form.slotId = null
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
    slots.value = []
    form.slotId = null
  } finally {
    loadingSlots.value = false
  }
}

function chooseSlot(slot: AppointmentSlot): void {
  if (!slot.isSelectable || !slot.slotId) {
    return
  }
  form.slotId = slot.slotId
}

async function createAppointment(): Promise<void> {
  if (!form.slotId) {
    errorMessage.value = '请先选择一个空闲时段'
    return
  }

  submitting.value = true
  errorMessage.value = ''

  try {
    createdAppointment.value = await createStudentAppointmentApi({
      slotId: form.slotId,
      issueSummary: form.issueSummary
    })
    await loadSlots()
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    submitting.value = false
  }
}

function formatRange(start: string | Date, end: string | Date): string {
  const begin = new Date(start)
  const finish = new Date(end)
  const startHour = String(begin.getHours()).padStart(2, '0')
  const startMinute = String(begin.getMinutes()).padStart(2, '0')
  const endHour = String(finish.getHours()).padStart(2, '0')
  const endMinute = String(finish.getMinutes()).padStart(2, '0')
  return `${startHour}:${startMinute} - ${endHour}:${endMinute}`
}

onMounted(async () => {
  await loadCounselors()
  await loadSlots()
})

watch(
  () => [form.counselorUserId, form.date],
  async () => {
    await loadSlots()
  }
)
</script>

<template>
  <main class="appointment-page">
    <section class="appointment-shell">
      <header class="hero">
        <div class="hero-copy">
          <span class="hero-tag">咨询预约</span>
          <h1 class="hero-title">先选咨询师，再挑一个合适的时段</h1>
          <p class="hero-lead">
            系统会根据你选中的咨询师和日期，实时显示当天固定 5 个时段的预约状态。
          </p>
        </div>
      </header>

      <div v-if="errorMessage" class="message-banner message-banner--error">{{ errorMessage }}</div>

      <div v-if="createdAppointment" class="success-panel">
        <div class="success-badge">预约成功</div>
        <h2 class="success-title">你的预约已经提交</h2>
        <p class="success-text">
          {{ createdAppointment.counselorName || '咨询师' }} 将在约定时段查看并处理这条预约。
        </p>

        <dl class="success-facts">
          <div>
            <dt>预约编号</dt>
            <dd>#{{ createdAppointment.appointmentId }}</dd>
          </div>
          <div>
            <dt>匿名称呼</dt>
            <dd>{{ createdAppointment.anonymousName }}</dd>
          </div>
          <div>
            <dt>预约时间</dt>
            <dd>{{ formatRange(createdAppointment.startTime, createdAppointment.endTime) }}</dd>
          </div>
        </dl>
      </div>

      <div v-else class="layout-grid">
        <section class="panel panel--soft">
          <div class="panel-header">
            <span class="panel-tag">第一步</span>
            <h2 class="panel-title">选择咨询师</h2>
          </div>

          <div v-if="loadingCounselors" class="inline-state">正在加载咨询师列表...</div>
          <div v-else-if="!counselorOptions.length" class="inline-state">当前没有可预约的咨询师。</div>
          <div v-else class="counselor-list">
            <button
              v-for="counselor in counselorOptions"
              :key="counselor.counselorUserId"
              type="button"
              class="counselor-card"
              :class="{ 'counselor-card--active': form.counselorUserId === counselor.counselorUserId }"
              @click="form.counselorUserId = counselor.counselorUserId"
            >
              <div class="counselor-main">
                <span class="counselor-name">{{ counselor.counselorName }}</span>
                <span class="counselor-sub">工号 {{ counselor.counselorNo || '未设置' }}</span>
              </div>
              <span class="counselor-state">
                {{ form.counselorUserId === counselor.counselorUserId ? '已选中' : '可预约' }}
              </span>
            </button>
          </div>

          <div class="panel-header panel-header--tight">
            <span class="panel-tag">第二步</span>
            <h2 class="panel-title">选择日期</h2>
          </div>

          <label class="date-field">
            <span class="date-label">预约日期</span>
            <input v-model="form.date" class="date-input" type="date" :min="buildDefaultDate()" />
          </label>

          <div class="selection-note">
            <span class="selection-note__label">当前选择</span>
            <strong>{{ selectedCounselor?.counselorName || '未选择咨询师' }}</strong>
            <em>{{ formatDateLabel(form.date) }}</em>
          </div>
        </section>

        <section class="panel panel--focus">
          <div class="panel-header">
            <span class="panel-tag">第三步</span>
            <h2 class="panel-title">选择时段</h2>
          </div>

          <div v-if="loadingSlots" class="inline-state">正在刷新时段状态...</div>
          <div v-else class="slot-list">
            <button
              v-for="slot in slots"
              :key="`${slot.counselorUserId}-${slot.timeLabel}`"
              type="button"
              class="slot-row"
              :class="{
                'slot-row--active': form.slotId === slot.slotId,
                'slot-row--disabled': !slot.isSelectable
              }"
              :disabled="!slot.isSelectable"
              @click="chooseSlot(slot)"
            >
              <div class="slot-main">
                <strong class="slot-time">{{ slot.timeLabel || formatRange(slot.startTime, slot.endTime) }}</strong>
                <span class="slot-desc">{{ slot.counselorName }} · {{ formatDateLabel(form.date) }}</span>
              </div>
              <span class="slot-status" :class="slot.isBooked ? 'slot-status--booked' : 'slot-status--free'">
                {{ slot.isBooked ? '已预约' : '空闲' }}
              </span>
            </button>
          </div>

          <div v-if="!loadingSlots && !selectableSlots.length" class="empty-tip">
            这一天暂时没有可选时段，换一个日期或换一位咨询师即可继续查看。
          </div>

          <div class="panel-header panel-header--tight">
            <span class="panel-tag">第四步</span>
            <h2 class="panel-title">填写沟通主题</h2>
          </div>

          <textarea
            v-model="form.issueSummary"
            class="issue-textarea"
            rows="5"
            maxlength="1000"
            placeholder="写下你希望在咨询中优先讨论的问题，例如近期压力、睡眠困扰、人际关系或学习节奏。"
          />

          <button class="submit-button" type="button" :disabled="!canSubmit" @click="createAppointment">
            {{ submitting ? '正在提交预约...' : '确认发起预约' }}
          </button>
        </section>
      </div>
    </section>
  </main>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700&family=Noto+Serif+SC:wght@500;600;700&display=swap');

.appointment-page {
  min-height: 100vh;
  padding: 3rem 1.5rem 5rem;
  background:
    radial-gradient(circle at top left, rgba(201, 214, 205, 0.28), transparent 28rem),
    radial-gradient(circle at bottom right, rgba(229, 212, 197, 0.24), transparent 30rem),
    #fcfbfa;
  color: #1e2821;
}

.appointment-shell {
  width: min(1120px, 100%);
  margin: 0 auto;
}

.hero {
  margin-bottom: 2rem;
}

.hero-copy {
  max-width: 720px;
}

.hero-tag,
.panel-tag {
  display: inline-flex;
  align-items: center;
  padding: 0.4rem 0.9rem;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.72);
  color: #6a7c70;
  font: 600 0.8rem/1 'Manrope', sans-serif;
  letter-spacing: 0.08em;
}

.hero-title,
.panel-title,
.success-title {
  margin: 0;
  font-family: 'Noto Serif SC', serif;
  font-weight: 600;
  color: #1e2821;
}

.hero-title {
  margin-top: 1rem;
  font-size: clamp(2rem, 4vw, 3.2rem);
  line-height: 1.18;
}

.hero-lead,
.success-text,
.inline-state,
.empty-tip {
  margin: 1rem 0 0;
  color: #5f7065;
  font: 500 1rem/1.8 'Manrope', sans-serif;
}

.layout-grid {
  display: grid;
  grid-template-columns: minmax(320px, 0.95fr) minmax(0, 1.25fr);
  gap: 1.75rem;
}

.panel {
  padding: 2rem;
  border-radius: 28px;
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.76), rgba(248, 246, 242, 0.88));
  backdrop-filter: blur(24px);
  box-shadow: 0 40px 80px rgba(54, 66, 58, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.8);
}

.panel--focus {
  display: flex;
  flex-direction: column;
  gap: 1.4rem;
}

.panel-header {
  display: flex;
  flex-direction: column;
  gap: 0.9rem;
}

.panel-header--tight {
  margin-top: 0.4rem;
}

.counselor-list,
.slot-list {
  display: flex;
  flex-direction: column;
  gap: 0.9rem;
  margin-top: 1.2rem;
}

.counselor-card,
.slot-row,
.submit-button {
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.counselor-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  width: 100%;
  padding: 1rem 1.15rem;
  border: none;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.7);
  cursor: pointer;
  text-align: left;
}

.counselor-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 24px 40px rgba(54, 66, 58, 0.08);
}

.counselor-card--active {
  background: linear-gradient(145deg, rgba(42, 54, 46, 0.95), rgba(53, 68, 58, 0.92));
  box-shadow: 0 28px 44px rgba(42, 54, 46, 0.18);
}

.counselor-main {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.counselor-name {
  font: 600 1.05rem/1.4 'Noto Serif SC', serif;
  color: #223027;
}

.counselor-sub,
.slot-desc {
  color: #74857a;
  font: 500 0.9rem/1.6 'Manrope', sans-serif;
}

.counselor-card--active .counselor-name,
.counselor-card--active .counselor-sub,
.counselor-card--active .counselor-state {
  color: #fff;
}

.counselor-state {
  color: #5f7065;
  font: 600 0.85rem/1 'Manrope', sans-serif;
}

.date-field {
  display: flex;
  flex-direction: column;
  gap: 0.7rem;
  margin-top: 1.25rem;
}

.date-label {
  font: 600 0.92rem/1 'Manrope', sans-serif;
  color: #5f7065;
}

.date-input,
.issue-textarea {
  width: 100%;
  border: none;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.76);
  color: #223027;
  box-sizing: border-box;
}

.date-input {
  min-height: 3.4rem;
  padding: 0 1rem;
  font: 500 0.98rem/1 'Manrope', sans-serif;
}

.selection-note {
  display: grid;
  gap: 0.35rem;
  margin-top: 1.25rem;
  padding: 1rem 1.1rem;
  border-radius: 20px;
  background: rgba(243, 244, 240, 0.76);
}

.selection-note__label {
  font: 600 0.82rem/1 'Manrope', sans-serif;
  color: #8a9c90;
  letter-spacing: 0.08em;
}

.selection-note strong {
  font: 600 1rem/1.5 'Noto Serif SC', serif;
}

.selection-note em {
  font: 500 0.92rem/1.5 'Manrope', sans-serif;
  color: #6b7d72;
  font-style: normal;
}

.slot-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  width: 100%;
  padding: 1.1rem 1.2rem;
  border: none;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.76);
  cursor: pointer;
}

.slot-row:hover:not(:disabled) {
  transform: translateY(-4px);
  box-shadow: 0 24px 40px rgba(54, 66, 58, 0.08);
}

.slot-row--active {
  background: linear-gradient(145deg, rgba(42, 54, 46, 0.95), rgba(53, 68, 58, 0.92));
  box-shadow: 0 28px 44px rgba(42, 54, 46, 0.18);
}

.slot-row--disabled {
  cursor: not-allowed;
  opacity: 0.78;
  background: rgba(244, 242, 240, 0.86);
}

.slot-main {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
  text-align: left;
}

.slot-time {
  font: 600 1.12rem/1.4 'Noto Serif SC', serif;
  color: #223027;
}

.slot-row--active .slot-time,
.slot-row--active .slot-desc {
  color: #fff;
}

.slot-status {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 4.6rem;
  padding: 0.45rem 0.8rem;
  border-radius: 999px;
  font: 600 0.84rem/1 'Manrope', sans-serif;
}

.slot-status--free {
  background: rgba(127, 170, 135, 0.16);
  color: #3f7b4d;
}

.slot-status--booked {
  background: rgba(211, 120, 120, 0.14);
  color: #ad5858;
}

.issue-textarea {
  min-height: 10rem;
  padding: 1rem 1.1rem;
  resize: vertical;
  font: 500 0.98rem/1.8 'Manrope', sans-serif;
}

.submit-button {
  min-height: 3.8rem;
  border: none;
  border-radius: 999px;
  background: #2a362e;
  color: #fff;
  font: 600 1rem/1 'Noto Serif SC', serif;
  cursor: pointer;
}

.submit-button:hover:not(:disabled) {
  transform: translateY(-4px);
  box-shadow: 0 24px 40px rgba(42, 54, 46, 0.2);
}

.submit-button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
  box-shadow: none;
}

.message-banner {
  margin-bottom: 1.25rem;
  padding: 1rem 1.15rem;
  border-radius: 18px;
  font: 500 0.95rem/1.7 'Manrope', sans-serif;
}

.message-banner--error {
  background: rgba(202, 111, 111, 0.12);
  color: #9f5757;
}

.success-panel {
  padding: 2rem;
  border-radius: 28px;
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.78), rgba(246, 244, 240, 0.88));
  backdrop-filter: blur(24px);
  box-shadow: 0 40px 80px rgba(54, 66, 58, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.8);
}

.success-badge {
  display: inline-flex;
  align-items: center;
  padding: 0.45rem 0.9rem;
  border-radius: 999px;
  background: rgba(127, 170, 135, 0.16);
  color: #3f7b4d;
  font: 700 0.8rem/1 'Manrope', sans-serif;
  letter-spacing: 0.08em;
}

.success-facts {
  display: grid;
  gap: 1rem;
  margin: 1.5rem 0 0;
}

.success-facts div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 1rem 1.1rem;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.74);
}

.success-facts dt {
  color: #77887d;
  font: 500 0.92rem/1 'Manrope', sans-serif;
}

.success-facts dd {
  margin: 0;
  color: #223027;
  font: 600 0.95rem/1.5 'Noto Serif SC', serif;
}

@media (max-width: 900px) {
  .layout-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .appointment-page {
    padding-inline: 1rem;
  }

  .panel,
  .success-panel {
    padding: 1.4rem;
    border-radius: 24px;
  }

  .slot-row,
  .counselor-card {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
