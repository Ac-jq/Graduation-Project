export interface NotificationItem {
  notificationId: number
  title: string
  contentText: string
  read: boolean
  readAt: string | null
  createdAt: string
}