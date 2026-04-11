import type { Router } from 'vue-router'

let routerRef: Router | null = null

export function bindRouter(router: Router): void {
  routerRef = router
}

export async function navigateTo(path: string): Promise<void> {
  if (routerRef) {
    await routerRef.replace(path)
    return
  }

  window.location.assign(path)
}
