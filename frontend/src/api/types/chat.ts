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
  senderDisplayName?: string | null
  senderAvatarUrl?: string | null
  content: string
  createdAt: string
}

export interface ConsultChatSendPayload {
  content: string
}

export interface ConsultChatSocketPayload {
  type: 'CONNECTED' | 'MESSAGE' | 'ERROR' | 'SYSTEM'
  action?: 'USER_JOINED' | 'WAITING_PEER' | 'USER_LEFT' | 'CHAT_CLOSED'
  message?: ConsultChatMessage
  session?: ConsultChatSession
  tip?: string
  onlineCount?: number
}
