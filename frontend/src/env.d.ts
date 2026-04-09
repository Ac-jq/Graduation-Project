/// <reference types="vite/client" />

import type { CurrentUser, RoleCode } from '@/types/common'

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL?: string
  readonly VITE_API_TIMEOUT?: string
  readonly VITE_WS_BASE_URL?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}

declare global {
  interface Window {
    __JQPRO_DEBUG__?: {
      login(account: string, password: string): Promise<CurrentUser>
      logout(): Promise<void>
      seedSession(roleCode?: RoleCode): void
      clearSession(): void
      trigger401(): Promise<unknown>
      trigger600(): Promise<unknown>
    }
  }
}