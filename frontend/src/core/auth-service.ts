import type { CurrentUser, LoginResponse } from '@/types/common'
import { changePasswordApi, fetchCurrentUserApi, loginApi, logoutApi } from '@/api/auth'
import { clearSession, getCurrentUserCache, hydrateFromLogin, setCurrentUserCache } from './session'

export async function fetchCurrentUser(): Promise<CurrentUser | null> {
  try {
    const user = await fetchCurrentUserApi()
    setCurrentUserCache(user)
    return user
  } catch {
    clearSession()
    return null
  }
}

export function getBootstrappedUser(): CurrentUser | null {
  return getCurrentUserCache()
}

export async function login(account: string, password: string): Promise<LoginResponse> {
  return loginApi({ account, password })
}

export async function logout(): Promise<void> {
  await logoutApi()
}

export async function changePassword(oldPassword: string, newPassword: string, confirmPassword: string): Promise<void> {
  await changePasswordApi({ oldPassword, newPassword, confirmPassword })
}

export function applyLoginSession(payload: LoginResponse): CurrentUser {
  return hydrateFromLogin(payload)
}