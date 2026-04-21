import { del, get, post, put } from './http'
import type {
  AdminUserQuery,
  AdminUserSummary,
  AvatarUploadResponse,
  CounselorProfile,
  CounselorStudentSummary,
  CreateCounselorRequest,
  StudentProfile,
  UpdateAdminUserRequest,
  UpdateCounselorProfileRequest,
  UpdateStudentProfileRequest
} from './types'

export function fetchStudentProfileApi(): Promise<StudentProfile> {
  return get<StudentProfile>('/student/profile/me')
}

export function updateStudentProfileApi(payload: UpdateStudentProfileRequest): Promise<StudentProfile> {
  return put<StudentProfile>('/student/profile/me', payload)
}

export function uploadStudentAvatarApi(file: File): Promise<AvatarUploadResponse> {
  const formData = new FormData()
  formData.append('file', file)
  return post<AvatarUploadResponse>('/student/profile/avatar', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export function fetchCounselorProfileApi(): Promise<CounselorProfile> {
  return get<CounselorProfile>('/counselor/profile')
}

export function updateCounselorProfileApi(payload: UpdateCounselorProfileRequest): Promise<CounselorProfile> {
  return put<CounselorProfile>('/counselor/profile', payload)
}

export function fetchCounselorStudentsApi(): Promise<CounselorStudentSummary[]> {
  return get<CounselorStudentSummary[]>('/counselor/students')
}

export function fetchAdminUsersApi(query: AdminUserQuery = {}): Promise<AdminUserSummary[]> {
  return get<AdminUserSummary[]>('/admin/users', { params: query })
}

export function createCounselorApi(payload: CreateCounselorRequest): Promise<AdminUserSummary> {
  return post<AdminUserSummary>('/admin/users/counselors', payload)
}

export function updateAdminUserApi(userId: number, payload: UpdateAdminUserRequest): Promise<AdminUserSummary> {
  return put<AdminUserSummary>(`/admin/users/${userId}`, payload)
}

export function deleteAdminUserApi(userId: number): Promise<void> {
  return del<void>(`/admin/users/${userId}`)
}

export function enableUserApi(userId: number): Promise<void> {
  return post<void>(`/admin/users/${userId}/enable`)
}

export function disableUserApi(userId: number): Promise<void> {
  return post<void>(`/admin/users/${userId}/disable`)
}

export function resetUserPasswordApi(userId: number): Promise<void> {
  return post<void>(`/admin/users/${userId}/reset-password`)
}
