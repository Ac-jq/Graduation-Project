export interface ParseAdminAiTaskRequest {
  instruction: string
}

export interface ConfirmAdminAiTaskRequest {
  selectedItemIds?: number[]
}

export interface AdminAiTaskItem {
  itemId: number
  targetType: string
  targetId: number | null
  targetLabel: string | null
  operationType: string
  fieldName: string | null
  oldValue: string | null
  newValue: string | null
  sortNo: number | null
  executeStatus: string | null
}

export interface AdminAiTaskSummary {
  taskId: number
  instructionText: string
  taskType: string
  parseStatus: string
  confirmStatus: string
  executeStatus: string
  summaryText: string | null
  createdAt: string
}

export interface AdminAiTaskDetail extends AdminAiTaskSummary {
  adminUserId: number
  failureReason: string | null
  confirmedAt: string | null
  executedAt: string | null
  items: AdminAiTaskItem[]
}

export interface ParseAdminAiTaskResponse {
  ready: boolean
  message: string
  task: AdminAiTaskDetail
}
