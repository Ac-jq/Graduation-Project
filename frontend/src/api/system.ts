import { get } from './http'
import type { SystemPingResponse } from './types'

export function fetchSystemPingApi(): Promise<SystemPingResponse> {
  return get<SystemPingResponse>('/system/ping')
}

export function fetchSystemBusinessErrorApi(): Promise<never> {
  return get<never>('/system/business-error')
}
