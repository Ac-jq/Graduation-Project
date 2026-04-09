import type { CurrentUser, RoleCode } from '@/types/common'
import { get, post } from '@/api/http'
import { clearSession, setCurrentUserCache, setToken } from '@/core/session'
import type { useAuthStore } from '@/stores/auth'

function buildDebugUser(roleCode: RoleCode): CurrentUser {
  return {
    userId: roleCode === 'ADMIN' ? 3 : roleCode === 'COUNSELOR' ? 2 : 1,
    account: roleCode === 'ADMIN' ? 'admin' : roleCode === 'COUNSELOR' ? 'teacher01' : '20230001',
    roleCode,
    displayName: `${roleCode.toLowerCase()}-debug`,
    roles: [roleCode]
  }
}

export function installDebugBridge(authStore: ReturnType<typeof useAuthStore>): void {
  if (!import.meta.env.DEV) {
    return
  }

  window.__JQPRO_DEBUG__ = {
    async login(account: string, password: string) {
      return authStore.signIn(account, password)
    },
    async logout() {
      await authStore.signOut(false)
    },
    seedSession(roleCode: RoleCode = 'STUDENT') {
      setToken(`debug-${roleCode.toLowerCase()}-token`)
      setCurrentUserCache(buildDebugUser(roleCode))
      authStore.syncFromStorage()
    },
    clearSession() {
      clearSession()
      authStore.syncFromStorage()
    },
    async trigger401() {
      this.seedSession('STUDENT')
      setToken('invalid-debug-token')
      authStore.syncFromStorage()
      return get('/auth/current-user')
    },
    async trigger600() {
      clearSession()
      authStore.syncFromStorage()
      return post('/auth/login', {
        account: 'debug-invalid-account',
        password: 'debug-invalid-password'
      })
    }
  }
}
