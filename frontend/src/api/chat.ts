import { get } from './http'
import type { ConsultChatMessage, ConsultChatSession } from './types'

const WS_BASE_URL = import.meta.env.VITE_WS_BASE_URL ?? 'ws://127.0.0.1:8080/ws'

export function fetchConsultChatSessionApi(appointmentId: number): Promise<ConsultChatSession> {
  return get<ConsultChatSession>(`/chat/appointments/${appointmentId}/session`)
}

export function fetchConsultChatMessagesApi(appointmentId: number): Promise<ConsultChatMessage[]> {
  return get<ConsultChatMessage[]>(`/chat/appointments/${appointmentId}/messages`)
}

export function buildConsultChatWebSocketUrl(appointmentId: number, token: string): string {
  const encodedToken = encodeURIComponent(token)
  return `${WS_BASE_URL}/consult-chat?appointmentId=${appointmentId}&token=${encodedToken}`
}