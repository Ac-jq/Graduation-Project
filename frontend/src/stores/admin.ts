import { defineStore } from 'pinia'
import { createEmptyPage } from '@/types/pagination'

export const useAdminStore = defineStore('admin', {
  state: () => ({
    statistics: null as Record<string, unknown> | null,
    auditLogPage: createEmptyPage<Record<string, unknown>>(),
    pendingTasks: [] as Array<Record<string, unknown>>
  })
})
