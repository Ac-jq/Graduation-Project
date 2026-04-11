import { defineStore } from 'pinia'

export const useResourceStore = defineStore('resource', {
  state: () => ({
    categories: [] as Array<Record<string, unknown>>,
    tags: [] as Array<Record<string, unknown>>,
    resources: [] as Array<Record<string, unknown>>
  })
})
