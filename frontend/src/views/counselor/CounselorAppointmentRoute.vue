<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { acceptAppointmentApi, fetchCounselorAppointmentsApi, rejectAppointmentApi } from '@/api/appointment'
import type { Appointment, AppointmentActionRequest } from '@/api/types'
import { toErrorMessage } from '@/views/shared/page-logic'

const router = useRouter()
const loading = ref(false)
const processing = ref(false)
const errorMessage = ref('')
const appointments = ref<Appointment[]>([])
const actionForm = reactive<Record<number, AppointmentActionRequest>>({})

async function loadAppointments(): Promise<void> {
  loading.value = true
  errorMessage.value = ''

  try {
    appointments.value = await fetchCounselorAppointmentsApi()
    for (const appointment of appointments.value) {
      actionForm[appointment.appointmentId] ??= { resultMessage: appointment.resultMessage ?? '' }
    }
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    loading.value = false
  }
}

function resolveActionPayload(appointmentId: number): AppointmentActionRequest {
  return actionForm[appointmentId] ?? { resultMessage: '' }
}

function ensureActionModel(appointmentId: number): AppointmentActionRequest {
  actionForm[appointmentId] ??= { resultMessage: '' }
  return actionForm[appointmentId]
}

async function acceptAppointment(appointmentId: number): Promise<void> {
  processing.value = true
  errorMessage.value = ''

  try {
    await acceptAppointmentApi(appointmentId, resolveActionPayload(appointmentId))
    await loadAppointments()
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    processing.value = false
  }
}

async function rejectAppointment(appointmentId: number): Promise<void> {
  processing.value = true
  errorMessage.value = ''

  try {
    await rejectAppointmentApi(appointmentId, resolveActionPayload(appointmentId))
    await loadAppointments()
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    processing.value = false
  }
}

async function openChat(appointmentId: number): Promise<void> {
  await router.push({ name: 'counselor-chat', params: { appointmentId } })
}

onMounted(() => {
  void loadAppointments()
})
</script>

<template>
  <section class="c-appointment-page">
    <div class="page-shell">
      <header class="page-hero">
        <div class="hero-copy">
          <p class="eyebrow">预约处理台</p>
          <h1>处理学生的匿名预约请求，并为后续沟通写下明确的流转说明。</h1>
          <p class="lead">列表中的预约、匿名名与时间段均来自真实接口返回。</p>
        </div>
        <div class="hero-metric">
          <span>预约总数</span>
          <strong>{{ appointments.length }}</strong>
        </div>
      </header>

      <p v-if="loading" class="state-text">正在同步预约池...</p>
      <p v-else-if="errorMessage" class="error-text">{{ errorMessage }}</p>
      <p v-else-if="!appointments.length" class="state-text">当前没有待处理或历史预约。</p>

      <div v-else class="appointment-stack">
        <article v-for="appointment in appointments" :key="appointment.appointmentId" class="appointment-card">
          <div class="appointment-topline">
            <div>
              <p class="appointment-code">预约 #{{ appointment.appointmentId }}</p>
              <h2>{{ appointment.anonymousName }}</h2>
            </div>
            <span class="status-pill">{{ appointment.status }}</span>
          </div>
          <p class="issue-summary">{{ appointment.issueSummary }}</p>
          <div class="appointment-meta">
            <span>{{ new Date(appointment.startTime).toLocaleString('zh-CN') }} - {{ new Date(appointment.endTime).toLocaleString('zh-CN') }}</span>
            <span>咨询师 {{ appointment.counselorName || `#${appointment.counselorUserId || '-'}` }}</span>
          </div>
          <textarea v-model="ensureActionModel(appointment.appointmentId).resultMessage" class="result-input" rows="3" placeholder="写给学生的处理说明或后续建议。" />
          <div class="action-row">
            <button
              v-if="['ACCEPTED', 'IN_PROGRESS', 'COMPLETED'].includes(appointment.status)"
              class="ghost-button"
              type="button"
              @click="openChat(appointment.appointmentId)"
            >
              进入私密聊天室
            </button>
            <button class="ghost-button" type="button" :disabled="processing" @click="rejectAppointment(appointment.appointmentId)">拒绝</button>
            <button class="primary-button" type="button" :disabled="processing" @click="acceptAppointment(appointment.appointmentId)">接单</button>
          </div>
        </article>
      </div>
    </div>
  </section>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700&family=Noto+Serif+SC:wght@400;500;600;700&display=swap');

.c-appointment-page{min-height:100vh;padding:44px 28px 72px;color:#283128;background:linear-gradient(180deg,#f5f0e5 0%,#f8f4ed 100%)}
.page-shell{max-width:1240px;margin:0 auto}.page-hero{display:grid;grid-template-columns:minmax(0,1.35fr) 220px;gap:28px;align-items:end;margin-bottom:30px}.hero-copy{border-top:1px solid rgba(59,69,59,.16);padding-top:18px}.eyebrow,.appointment-code{margin:0 0 10px;font:700 .76rem/1 'Manrope',sans-serif;letter-spacing:.22em;text-transform:uppercase;color:#7b6857}.hero-copy h1,.appointment-card h2{margin:0;font-family:'Noto Serif SC',serif;font-weight:600}.hero-copy h1{font-size:clamp(2rem,3vw,3.2rem);line-height:1.16}.lead,.issue-summary,.appointment-meta,.result-input,.state-text,.error-text{font-family:'Manrope',sans-serif}.lead{margin:18px 0 0;line-height:1.84;color:rgba(40,49,40,.72)}.hero-metric,.appointment-card{border:1px solid rgba(77,86,77,.14);background:rgba(255,252,247,.76);box-shadow:0 24px 70px rgba(91,80,66,.08);backdrop-filter:blur(16px)}.hero-metric{padding:18px 20px}.hero-metric span{display:block;margin-bottom:8px;font:700 .78rem/1 'Manrope',sans-serif;letter-spacing:.16em;text-transform:uppercase;color:rgba(68,74,66,.56)}.hero-metric strong{font:600 1.6rem/1 'Noto Serif SC',serif}.appointment-stack{display:grid;gap:18px}.appointment-card{padding:22px}.appointment-topline{display:flex;justify-content:space-between;gap:16px;align-items:start}.appointment-card h2{font-size:1.34rem;line-height:1.35}.status-pill{border:1px solid rgba(97,111,98,.15);background:rgba(242,244,237,.94);padding:8px 12px;font:700 .74rem/1 'Manrope',sans-serif;letter-spacing:.12em;text-transform:uppercase;color:#66735f}.issue-summary{margin:14px 0 0;font-size:.96rem;line-height:1.86;color:rgba(40,49,40,.7)}.appointment-meta{display:flex;flex-wrap:wrap;gap:10px 18px;margin-top:14px;font-size:.84rem;color:rgba(40,49,40,.58)}.result-input{width:100%;margin-top:16px;border:1px solid rgba(80,88,79,.16);background:rgba(255,255,255,.74);padding:14px 16px;resize:vertical;color:#283128;outline:none}.action-row{display:flex;flex-wrap:wrap;gap:12px;margin-top:16px}.ghost-button,.primary-button{padding:12px 16px;font:700 .82rem/1 'Manrope',sans-serif;letter-spacing:.08em;text-transform:uppercase;cursor:pointer}.ghost-button{border:1px solid rgba(54,65,56,.2);background:rgba(255,255,255,.58);color:#283128}.primary-button{border:none;background:linear-gradient(135deg,#253128 0%,#47564b 100%);color:#f8f5ef}.error-text{color:#a44f46}
@media (max-width:900px){.c-appointment-page{padding:28px 16px 46px}.page-hero{grid-template-columns:1fr}.appointment-topline,.action-row{flex-direction:column;align-items:start}}
</style>

