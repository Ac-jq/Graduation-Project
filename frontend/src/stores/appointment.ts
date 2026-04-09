import { defineStore } from 'pinia'

export const useAppointmentStore = defineStore('appointment', {
  state: () => ({
    slots: [] as Array<Record<string, unknown>>,
    appointments: [] as Array<Record<string, unknown>>,
    activeAppointmentId: null as number | null
  })
})
