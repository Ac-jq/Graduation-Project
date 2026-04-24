export interface ParseAdminAiTaskRequest {
  instruction: string
  taskId?: number | null
}

export interface ConfirmAdminAiTaskRequest {
  selectedItemIds?: number[]
}

export interface AdminAiTaskItem {
  itemId: number
  targetType: string
  targetId: number | null
  targetLabel: string | null
  account?: string | null
  displayName?: string | null
  realName?: string | null
  studentNo?: string | null
  counselorNo?: string | null
  college?: string | null
  grade?: string | null
  operationType: string
  fieldName: string | null
  oldValue: string | null
  newValue: string | null
  sortNo: number | null
  executeStatus: string | null
}

export interface AdminAiConversationMessage {
  role: string
  content: string
  createdAt: string
}

export interface AdminAiTaskSummary {
  taskId: number
  instructionText: string
  taskType: string
  parseStatus: string
  workflowStatus: string
  agentStatus: string
  confirmStatus: string
  executeStatus: string
  summaryText: string | null
  pendingPrompt: string | null
  createdAt: string
}

export interface AdminAiTaskDetail extends AdminAiTaskSummary {
  adminUserId: number
  failureReason: string | null
  pendingPrompt: string | null
  confirmedAt: string | null
  executedAt: string | null
  conversation: AdminAiConversationMessage[]
  items: AdminAiTaskItem[]
}

export interface ParseAdminAiTaskResponse {
  ready: boolean
  message: string
  task: AdminAiTaskDetail
}
