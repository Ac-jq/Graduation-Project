export interface AuditLogQuery {
  actionCode?: string
  keyword?: string
}

export interface AuditLogItem {
  logId: number
  userId: number | null
  userDisplayName: string | null
  actionCode: string
  actionName: string
  detailText: string
  ipAddress: string | null
  createdAt: string
}