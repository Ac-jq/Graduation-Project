import { get, post } from './http'
import type { AdminAiTaskDetail, AdminAiTaskSummary, ParseAdminAiTaskRequest, ParseAdminAiTaskResponse } from './types'

export function parseAdminAiTaskApi(payload: ParseAdminAiTaskRequest): Promise<ParseAdminAiTaskResponse> {
  return post<ParseAdminAiTaskResponse>('/admin/ai-tasks/parse', payload)
}

export function fetchAdminAiTasksApi(): Promise<AdminAiTaskSummary[]> {
  return get<AdminAiTaskSummary[]>('/admin/ai-tasks')
}

export function fetchAdminAiTaskDetailApi(taskId: number): Promise<AdminAiTaskDetail> {
  return get<AdminAiTaskDetail>(`/admin/ai-tasks/${taskId}`)
}

export function confirmAdminAiTaskApi(taskId: number): Promise<AdminAiTaskDetail> {
  return post<AdminAiTaskDetail>(`/admin/ai-tasks/${taskId}/confirm`)
}

export function cancelAdminAiTaskApi(taskId: number): Promise<AdminAiTaskDetail> {
  return post<AdminAiTaskDetail>(`/admin/ai-tasks/${taskId}/cancel`)
}