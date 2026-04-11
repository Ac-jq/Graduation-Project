export interface AdminScaleOption {
  optionId: number
  optionCode: string
  content: string
  score: number
  sortNo: number
}

export interface AdminScaleQuestion {
  questionId: number
  questionNo: number
  content: string
  requiredFlag: number
  options: AdminScaleOption[]
}

export interface AdminScale {
  scaleId: number
  code: string
  name: string
  description: string | null
  introduction: string | null
  totalQuestions: number
  pageSize: number
  lowThreshold: number
  mediumThreshold: number
  highThreshold: number
  status: string
  inUse: boolean
  createdAt: string
  updatedAt: string
  questions: AdminScaleQuestion[]
}

export interface UpsertAdminScaleOptionRequest {
  optionCode: string
  content: string
  score: number
  sortNo: number
}

export interface UpsertAdminScaleQuestionRequest {
  questionNo: number
  content: string
  requiredFlag: number
  options: UpsertAdminScaleOptionRequest[]
}

export interface UpsertAdminScaleRequest {
  code: string
  name: string
  description?: string | null
  introduction?: string | null
  pageSize: number
  lowThreshold: number
  mediumThreshold: number
  highThreshold: number
  questions: UpsertAdminScaleQuestionRequest[]
}