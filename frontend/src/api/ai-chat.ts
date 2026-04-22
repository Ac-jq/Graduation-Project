import { del, get, post, put } from './http'
import type {
  AiChatMessage,
  AiChatSession,
  AiPersonaSetting,
  CreateAiChatSessionRequest,
  SendAiChatMessageRequest,
  SendAiChatMessageResponse,
  UpdateAiPersonaSettingRequest
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

export function archiveStudentAiSessionApi(sessionId: number): Promise<AiChatSession> {
  return post<AiChatSession>(`/student/ai-sessions/${sessionId}/archive`)
}

export function deleteEmptyStudentAiSessionApi(sessionId: number): Promise<void> {
  return del<void>(`/student/ai-sessions/${sessionId}`)
}

export function fetchStudentAiPersonaApi(): Promise<AiPersonaSetting> {
  return get<AiPersonaSetting>('/student/ai-persona')
}

export function updateStudentAiPersonaApi(payload: UpdateAiPersonaSettingRequest): Promise<AiPersonaSetting> {
  return put<AiPersonaSetting>('/student/ai-persona', payload)
}

export function fetchCounselorStudentAiSessionsApi(studentUserId: number): Promise<AiChatSession[]> {
  return get<AiChatSession[]>(`/counselor/students/${studentUserId}/ai-sessions`)
}

export function fetchCounselorStudentAiSessionMessagesApi(studentUserId: number, sessionId: number): Promise<AiChatMessage[]> {
  return get<AiChatMessage[]>(`/counselor/students/${studentUserId}/ai-sessions/${sessionId}/messages`)
}
