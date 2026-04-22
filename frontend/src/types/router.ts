import type { RoleCode } from './common'

export interface AppRouteMeta {
  requiresAuth?: boolean
  roles?: readonly RoleCode[]
  guestOnly?: boolean
  isAuthPage?: boolean
  useAuthTransition?: boolean
  preload?: boolean
  description?: string
}
