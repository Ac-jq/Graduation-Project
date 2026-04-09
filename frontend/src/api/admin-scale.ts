import { get, post, put } from './http'
import type { AdminScale, UpsertAdminScaleRequest } from './types'

export function fetchAdminScalesApi(): Promise<AdminScale[]> {
  return get<AdminScale[]>('/admin/scales')
}

export function fetchAdminScaleDetailApi(scaleId: number): Promise<AdminScale> {
  return get<AdminScale>(`/admin/scales/${scaleId}`)
}

export function createAdminScaleApi(payload: UpsertAdminScaleRequest): Promise<AdminScale> {
  return post<AdminScale>('/admin/scales', payload)
}

export function updateAdminScaleApi(scaleId: number, payload: UpsertAdminScaleRequest): Promise<AdminScale> {
  return put<AdminScale>(`/admin/scales/${scaleId}`, payload)
}

export function activateAdminScaleApi(scaleId: number): Promise<AdminScale> {
  return post<AdminScale>(`/admin/scales/${scaleId}/activate`)
}

export function deactivateAdminScaleApi(scaleId: number): Promise<AdminScale> {
  return post<AdminScale>(`/admin/scales/${scaleId}/deactivate`)
}