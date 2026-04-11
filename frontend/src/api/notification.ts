import { get, post } from './http'
import type { NotificationItem } from './types'

export function fetchNotificationsApi(): Promise<NotificationItem[]> {
  return get<NotificationItem[]>('/notifications')
}

export function markNotificationReadApi(notificationId: number): Promise<void> {
  return post<void>(`/notifications/${notificationId}/read`)
}

export function markAllNotificationsReadApi(): Promise<void> {
  return post<void>('/notifications/read-all')
}