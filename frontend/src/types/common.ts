export type RoleCode = 'STUDENT' | 'COUNSELOR' | 'ADMIN'

export interface ApiResult<T> {
  code: number
  message: string
  data: T
  timestamp: string
}

export interface CurrentUser {
  userId: number
  account: string
  roleCode: RoleCode
  realName?: string | null
  displayName: string
  studentNo?: string | null
  counselorNo?: string | null
  roles: string[]
}

export interface LoginResponse {
  token: string
  userId: number
  account: string
  roleCode: RoleCode
  displayName: string
  roles: string[]
}

export interface ApiError extends Error {
  isApiError: true
  status?: number
  code?: number
  details?: unknown
}
