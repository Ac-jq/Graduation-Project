import { get } from './http'
import type { AuditLogItem, AuditLogQuery } from './types'

export function fetchAdminAuditLogsApi(query: AuditLogQuery = {}): Promise<AuditLogItem[]> {
  return get<AuditLogItem[]>('/admin/audit-logs', { params: query })
}