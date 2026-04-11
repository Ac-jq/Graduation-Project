import { get, post } from './http'
import type {
  AiChatMessage,
  AiChatSession,
  CreateAiChatSessionRequest,
  SendAiChatMessageRequest,
  SendAiChatMessageResponse
} from './types'

export function createAiChatSessionApi(payload: CreateAiChatSessionRequest): Promise<AiChatSession> {
  return post<AiChatSession>('/student/ai-sessions', payload)
}

export function fetchStudentAiSessionsApi(): Promise<AiChatSession[]> {
  return get<AiChatSession[]>('/student/ai-sessions')
}

export function fetchStudentAiSessionMessagesApi(sessionId: number): Promise<AiChatMessage[]> {
  return get<AiChatMessage[]>(`/student/ai-sessions/${sessionId}/messages`)
}

export function sendStudentAiChatMessageApi(sessionId: number, payload: SendAiChatMessageRequest): Promise<SendAiChatMessageResponse> {
  return post<SendAiChatMessageResponse>(`/student/ai-sessions/${sessionId}/messages`, payload)
}

export function fetchCounselorStudentAiSessionsApi(studentUserId: number): Promise<AiChatSession[]> {
  return get<AiChatSession[]>(`/counselor/students/${studentUserId}/ai-sessions`)
}

export function fetchCounselorStudentAiSessionMessagesApi(studentUserId: number, sessionId: number): Promise<AiChatMessage[]> {
  return get<AiChatMessage[]>(`/counselor/students/${studentUserId}/ai-sessions/${sessionId}/messages`)
}