export interface CreateAiChatSessionRequest {
  title?: string | null
}

export interface AiChatSession {
  sessionId: number
  studentUserId: number
  studentName: string | null
  title: string | null
  status: string
  summaryText: string | null
  riskFlag: boolean | null
  riskLevel: string | null
  lastActiveAt: string | null
  createdAt: string
}

export interface AiChatMessage {
  messageId: number
  sessionId: number
  senderType: string
  content: string
  riskLevel: string | null
  hitKeywords: string | null
  createdAt: string
}

export interface SendAiChatMessageRequest {
  content: string
}

export interface SendAiChatMessageResponse {
  studentMessage: AiChatMessage
  aiMessage: AiChatMessage
  riskFlag: boolean | null
  riskLevel: string | null
}