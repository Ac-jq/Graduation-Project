import type { CurrentUser, LoginResponse } from '@/types/common'

export interface LoginRequest {
  account: string
  password: string
}

export interface RegisterRequest {
  account: string
  password: string
  realName: string
  displayName: string
  studentNo: string
  gender: string
  grade: string
  college: string
}

export interface ChangePasswordRequest {
  oldPassword: string
  newPassword: string
  confirmPassword: string
}

export type CurrentUserResponse = CurrentUser
export type LoginSuccessResponse = LoginResponse
