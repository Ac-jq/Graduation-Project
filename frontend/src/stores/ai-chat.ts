import { defineStore } from 'pinia'

export const useAiChatStore = defineStore('ai-chat', {
  state: () => ({
    sessions: [] as Array<Record<string, unknown>>,
    messages: [] as Array<Record<string, unknown>>,
    activeSessionId: null as number | null
  })
})
