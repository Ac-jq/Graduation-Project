import type { NavigationGuardNext, RouteLocationNormalized } from 'vue-router'
import { pinia } from '@/stores'
import { useAuthStore } from '@/stores/auth'
import { resolveRoleHome } from '@/core/session'
import type { AppRouteMeta } from './route-meta'

function getRouteMeta(to: RouteLocationNormalized): AppRouteMeta {
  return (to.meta ?? {}) as AppRouteMeta
}

export async function applyRouteGuards(
  to: RouteLocationNormalized,
  _from: RouteLocationNormalized,
  next: NavigationGuardNext
): Promise<void> {
  const authStore = useAuthStore(pinia)
  const meta = getRouteMeta(to)

  await authStore.restoreSession()

  if (meta.guestOnly && authStore.isAuthenticated && authStore.roleCode) {
    next(resolveRoleHome(authStore.roleCode))
    return
  }

  if (!meta.requiresAuth) {
    next()
    return
  }

  if (!authStore.isAuthenticated || !authStore.currentUser) {
    next('/login')
    return
  }

  if (meta.roles && !meta.roles.includes(authStore.currentUser.roleCode)) {
    next('/forbidden')
    return
  }

  next()
}
