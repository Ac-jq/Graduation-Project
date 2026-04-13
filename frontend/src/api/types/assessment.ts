import type { ResourceSummary } from './resource'

export interface ScaleSummary {
  id: number
  code: string
  name: string
  description: string | null
  totalQuestions: number
  pageSize: number
  productPositioning?: string | null
  noticeText?: string | null
}

export interface ScaleDetail extends ScaleSummary {
  introduction: string | null
  scoringRules?: string[]
}

export interface ScaleSession {
  sessionId: number
  scaleId: number
  answeredCount: number
  totalQuestions: number
  status: string
}

export interface QuestionOption {
  id: number
  optionCode: string
  content: string
  score: number
}

export interface Question {
  questionId: number
  questionNo: number
  content: string
  selectedOptionId: number | null
  options: QuestionOption[]
}

export interface ScaleQuestionPageQuery {
  pageNum?: number
  pageSize?: number
}

export interface ScaleQuestionPage {
  sessionId: number
  pageNum: number
  pageSize: number
  total: number
  answeredCount: number
  totalQuestions: number
  records: Question[]
}

export interface AnswerItem {
  questionId: number
  optionId: number
}

export interface AnswerSaveRequest {
  answers: AnswerItem[]
}

export interface SubmitScaleResponse {
  sessionId: number
  reportId: number
  totalScore: number
  levelCode: string
  summaryText: string
  noticeText?: string | null
}

export interface ReportSummary {
  reportId: number
  scaleId: number
  scaleName: string
  totalScore: number
  levelCode: string
  summaryText: string
  createdAt: string
}

export interface ReportDetail {
  reportId: number
  sessionId: number
  scaleId: number
  scaleName: string
  studentUserId: number
  studentName: string | null
  studentNo: string | null
  totalScore: number
  levelCode: string
  summaryText: string
  aiInterpretation: string | null
  recommendationNote: string | null
  recommendAppointment: boolean
  recommendedResources: ResourceSummary[]
  noticeText?: string | null
  createdAt: string
}
