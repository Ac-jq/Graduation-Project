import type { RoleCode } from './common'

export interface AppRouteMeta {
  requiresAuth?: boolean
  roles?: readonly RoleCode[]
  guestOnly?: boolean
  preload?: boolean
  description?: string
}
