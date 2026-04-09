import { defineStore } from 'pinia'

export const useAssessmentStore = defineStore('assessment', {
  state: () => ({
    scales: [] as Array<Record<string, unknown>>,
    currentScale: null as Record<string, unknown> | null,
    currentReport: null as Record<string, unknown> | null
  })
})
