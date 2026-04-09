import { defineStore } from 'pinia'
import { createEmptyPage } from '@/types/pagination'

export const useUserStore = defineStore('user', {
  state: () => ({
    profile: null as Record<string, unknown> | null,
    boundStudents: [] as Array<Record<string, unknown>>,
    userPage: createEmptyPage<Record<string, unknown>>()
  })
})
