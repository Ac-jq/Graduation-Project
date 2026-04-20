import { get, post, put } from './http'
import type {
  AnswerSaveRequest,
  ReportDetail,
  ReportSummary,
  ScaleDetail,
  ScaleQuestionPage,
  ScaleQuestionPageQuery,
  ScaleSession,
  ScaleSummary,
  SubmitScaleResponse
} from './types'

export function fetchScaleListApi(): Promise<ScaleSummary[]> {
  return get<ScaleSummary[]>('/scales')
}

export function fetchScaleDetailApi(scaleId: number): Promise<ScaleDetail> {
  return get<ScaleDetail>(`/scales/${scaleId}`)
}

export function createScaleDraftSessionApi(scaleId: number): Promise<ScaleSession> {
  return post<ScaleSession>(`/scales/${scaleId}/sessions/draft`)
}

export function fetchScaleSessionQuestionsApi(sessionId: number, query: ScaleQuestionPageQuery = {}): Promise<ScaleQuestionPage> {
  return get<ScaleQuestionPage>(`/scales/sessions/${sessionId}/questions`, { params: query })
}

export function saveScaleAnswersApi(sessionId: number, payload: AnswerSaveRequest): Promise<void> {
  return put<void>(`/scales/sessions/${sessionId}/answers`, payload)
}

export function submitScaleSessionApi(sessionId: number): Promise<SubmitScaleResponse> {
  return post<SubmitScaleResponse>(`/scales/sessions/${sessionId}/submit`, undefined, {
    timeout: 90000
  })
}

export function fetchStudentReportsApi(): Promise<ReportSummary[]> {
  return get<ReportSummary[]>('/student/reports')
}

export function fetchStudentReportDetailApi(reportId: number): Promise<ReportDetail> {
  return get<ReportDetail>(`/student/reports/${reportId}`)
}

export function fetchCounselorStudentReportsApi(studentUserId: number): Promise<ReportSummary[]> {
  return get<ReportSummary[]>(`/counselor/students/${studentUserId}/reports`)
}

export function fetchCounselorStudentReportDetailApi(studentUserId: number, reportId: number): Promise<ReportDetail> {
  return get<ReportDetail>(`/counselor/students/${studentUserId}/reports/${reportId}`)
}
