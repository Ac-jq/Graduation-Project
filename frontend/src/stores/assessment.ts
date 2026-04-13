import { defineStore } from 'pinia'
import type {
  ReportDetail,
  ScaleDetail,
  ScaleQuestionPage,
  ScaleSession,
  ScaleSummary,
  SubmitScaleResponse
} from '@/api/types'

interface AssessmentState {
  scales: ScaleSummary[]
  currentScale: ScaleDetail | null
  currentSession: ScaleSession | null
  currentQuestionPage: ScaleQuestionPage | null
  draftAnswers: Record<number, number>
  latestSubmit: SubmitScaleResponse | null
  currentReport: ReportDetail | null
}

export const useAssessmentStore = defineStore('assessment', {
  state: (): AssessmentState => ({
    scales: [],
    currentScale: null,
    currentSession: null,
    currentQuestionPage: null,
    draftAnswers: {},
    latestSubmit: null,
    currentReport: null
  }),
  getters: {
    answeredCount(state): number {
      return state.currentSession?.answeredCount ?? state.currentQuestionPage?.answeredCount ?? 0
    }
  },
  actions: {
    setScales(scales: ScaleSummary[]): void {
      this.scales = scales
    },
    setCurrentScale(scale: ScaleDetail | null): void {
      this.currentScale = scale
    },
    setCurrentSession(session: ScaleSession | null): void {
      this.currentSession = session
    },
    setCurrentQuestionPage(page: ScaleQuestionPage | null): void {
      this.currentQuestionPage = page
    },
    setDraftAnswer(questionId: number, optionId: number): void {
      this.draftAnswers = { ...this.draftAnswers, [questionId]: optionId }
    },
    syncDraftAnswersFromPage(): void {
      const draftAnswers: Record<number, number> = {}
      for (const question of this.currentQuestionPage?.records ?? []) {
        if (question.selectedOptionId != null) {
          draftAnswers[question.questionId] = question.selectedOptionId
        }
      }
      this.draftAnswers = draftAnswers
    },
    setLatestSubmit(submit: SubmitScaleResponse | null): void {
      this.latestSubmit = submit
    },
    setCurrentReport(report: ReportDetail | null): void {
      this.currentReport = report
    },
    resetSessionState(): void {
      this.currentSession = null
      this.currentQuestionPage = null
      this.draftAnswers = {}
      this.latestSubmit = null
    }
  }
})
