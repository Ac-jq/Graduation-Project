import type { CurrentUser, LoginResponse, RoleCode } from '@/types/common'

const TOKEN_KEY = 'jqpro.token'
const USER_KEY = 'jqpro.current-user'

let currentUserCache: CurrentUser | null = null

function emitSessionChanged(): void {
  window.dispatchEvent(new Event('jqpro:session-changed'))
}

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token)
  emitSessionChanged()
}

export function clearToken(): void {
  localStorage.removeItem(TOKEN_KEY)
  emitSessionChanged()
}

export function getCurrentUserCache(): CurrentUser | null {
  if (currentUserCache) {
    return currentUserCache
  }

  const raw = localStorage.getItem(USER_KEY)
  if (!raw) {
    return null
  }

  try {
    currentUserCache = JSON.parse(raw) as CurrentUser
    return currentUserCache
  } catch {
    clearCurrentUserCache()
    return null
  }
}

export function setCurrentUserCache(user: CurrentUser): void {
  currentUserCache = user
  localStorage.setItem(USER_KEY, JSON.stringify(user))
  emitSessionChanged()
}

export function clearCurrentUserCache(): void {
  currentUserCache = null
  localStorage.removeItem(USER_KEY)
  emitSessionChanged()
}

export function clearSession(): void {
  localStorage.removeItem(TOKEN_KEY)
  currentUserCache = null
  localStorage.removeItem(USER_KEY)
  emitSessionChanged()
}

export function hydrateFromLogin(payload: LoginResponse): CurrentUser {
  const user: CurrentUser = {
    userId: payload.userId,
    account: payload.account,
    roleCode: payload.roleCode,
    displayName: payload.displayName,
    avatarUrl: payload.avatarUrl ?? null,
    roles: payload.roles
  }
  localStorage.setItem(TOKEN_KEY, payload.token)
  currentUserCache = user
  localStorage.setItem(USER_KEY, JSON.stringify(user))
  emitSessionChanged()
  return user
}

export function resolveRoleHome(roleCode: RoleCode): string {
  if (roleCode === 'STUDENT') {
    return '/student'
  }

  if (roleCode === 'COUNSELOR') {
    return '/counselor'
  }

  return '/admin'
}
