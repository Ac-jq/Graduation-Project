export interface ConsultChatSession {
  chatSessionId: number
  appointmentId: number
  status: string
  sealed: boolean
  openTime: string | null
  closeTime: string | null
}

export interface ConsultChatMessage {
  messageId: number
  chatSessionId: number
  senderUserId: number
  senderType: string
  content: string
  createdAt: string
}

export interface ConsultChatSendPayload {
  content: string
}

export interface ConsultChatSocketPayload {
  type: 'CONNECTED' | 'MESSAGE' | 'ERROR'
  message?: ConsultChatMessage
  tip?: string
}