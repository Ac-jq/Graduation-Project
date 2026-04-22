import { get, post } from './http'
import type { ChangePasswordRequest, CurrentUserResponse, LoginRequest, LoginSuccessResponse, RegisterRequest } from './types'

export function loginApi(payload: LoginRequest): Promise<LoginSuccessResponse> {
  return post<LoginSuccessResponse>('/auth/login', payload)
}

export function registerApi(payload: RegisterRequest): Promise<CurrentUserResponse> {
  return post<CurrentUserResponse>('/auth/register', payload)
}

export function logoutApi(): Promise<void> {
  return post<void>('/auth/logout')
}

export function changePasswordApi(payload: ChangePasswordRequest): Promise<void> {
  return post<void>('/auth/change-password', payload)
}

export function fetchCurrentUserApi(): Promise<CurrentUserResponse> {
  return get<CurrentUserResponse>('/auth/current-user')
}
