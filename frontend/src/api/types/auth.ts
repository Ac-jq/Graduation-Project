import type { CurrentUser, LoginResponse } from '@/types/common'

export interface LoginRequest {
  account: string
  password: string
}

export interface ChangePasswordRequest {
  oldPassword: string
  newPassword: string
  confirmPassword: string
}

export type CurrentUserResponse = CurrentUser
export type LoginSuccessResponse = LoginResponse