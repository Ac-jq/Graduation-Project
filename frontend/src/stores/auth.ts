import { defineStore } from 'pinia'
import type { CurrentUser } from '@/types/common'
import { applyLoginSession, fetchCurrentUser, login, logout } from '@/core/auth-service'
import { clearSession, getCurrentUserCache, getToken } from '@/core/session'

interface AuthState {
  token: string | null
  currentUser: CurrentUser | null
  sessionRestored: boolean
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    token: getToken(),
    currentUser: getCurrentUserCache(),
    sessionRestored: false
  }),
  getters: {
    isAuthenticated: (state): boolean => Boolean(state.token && state.currentUser),
    roleCode: (state) => state.currentUser?.roleCode ?? null
  },
  actions: {
    syncFromStorage(): void {
      this.token = getToken()
      this.currentUser = getCurrentUserCache()
    },
    resetSession(): void {
      clearSession()
      this.syncFromStorage()
      this.sessionRestored = true
    },
    async restoreSession(force = false): Promise<CurrentUser | null> {
      if (this.sessionRestored && !force) {
        return this.currentUser
      }

      this.syncFromStorage()
      if (!this.token) {
        this.currentUser = null
        this.sessionRestored = true
        return null
      }

      const currentUser = await fetchCurrentUser()
      this.syncFromStorage()
      this.currentUser = currentUser
      this.sessionRestored = true
      return currentUser
    },
    async signIn(account: string, password: string): Promise<CurrentUser> {
      const payload = await login(account, password)
      const user = applyLoginSession(payload)
      this.syncFromStorage()
      this.currentUser = user
      this.sessionRestored = true
      return user
    },
    async signOut(callBackend = false): Promise<void> {
      if (callBackend && this.token) {
        try {
          await logout()
        } catch {
          this.resetSession()
          return
        }
      }

      this.resetSession()
    }
  }
})
