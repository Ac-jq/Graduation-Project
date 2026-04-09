import { defineStore } from 'pinia'

export const useNotificationStore = defineStore('notification', {
  state: () => ({
    notifications: [] as Array<Record<string, unknown>>,
    unreadCount: 0
  })
})
