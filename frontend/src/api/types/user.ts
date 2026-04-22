export interface StudentProfile {
  userId: number
  account: string
  realName: string | null
  displayName: string
  studentNo: string | null
  avatarUrl: string | null
  college: string | null
  grade: string | null
  gender: string | null
  phone: string | null
  emergencyContact: string | null
  emergencyPhone: string | null
  counselorUserId: number | null
}

export interface UpdateStudentProfileRequest {
  avatarUrl?: string | null
  college?: string | null
  grade?: string | null
  gender?: string | null
  phone?: string | null
  emergencyContact?: string | null
  emergencyPhone?: string | null
}

export interface CounselorProfile {
  userId: number
  account: string
  realName: string | null
  displayName: string
  counselorNo: string | null
  roleCode: string
  avatarUrl: string | null
}

export interface UpdateCounselorProfileRequest {
  avatarUrl?: string | null
}

export interface AvatarUploadResponse {
  avatarUrl: string
}

export interface CounselorStudentSummary {
  studentUserId: number
  studentName: string
  studentNo: string | null
  college: string | null
  grade: string | null
  gender: string | null
}

export interface AdminUserSummary {
  userId: number
  account: string
  roleCode: string
  realName: string | null
  displayName: string
  studentNo: string | null
  counselorNo: string | null
  status: string
  college: string | null
  grade: string | null
  phone: string | null
  createdAt: string
}

export interface AdminUserQuery {
  roleCode?: string
  status?: string
  keyword?: string
  grade?: string
  college?: string
}

export interface CreateCounselorRequest {
  account: string
  displayName: string
  realName?: string | null
  counselorNo: string
}

export interface CreateAdminUserRequest {
  account: string
  roleCode: string
  displayName: string
  realName?: string | null
  studentNo?: string | null
  counselorNo?: string | null
  college?: string | null
  grade?: string | null
  phone?: string | null
  password?: string | null
}

export interface UpdateAdminUserRequest {
  account: string
  displayName: string
  realName?: string | null
  studentNo?: string | null
  counselorNo?: string | null
  college?: string | null
  grade?: string | null
  phone?: string | null
  password?: string | null
}
