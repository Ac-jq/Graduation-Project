import { del, get, post } from './http'
import type { ResourceCategory, ResourceDetail, ResourceQuery, ResourceSummary, ResourceTag } from './types'

export function fetchResourceCategoriesApi(): Promise<ResourceCategory[]> {
  return get<ResourceCategory[]>('/resources/categories')
}

export function fetchResourceTagsApi(): Promise<ResourceTag[]> {
  return get<ResourceTag[]>('/resources/tags')
}

export function fetchResourcesApi(query: ResourceQuery = {}): Promise<ResourceSummary[]> {
  return get<ResourceSummary[]>('/resources', { params: query })
}

export function fetchResourceDetailApi(resourceId: number): Promise<ResourceDetail> {
  return get<ResourceDetail>(`/resources/${resourceId}`)
}

export function fetchStudentFavoritesApi(): Promise<ResourceSummary[]> {
  return get<ResourceSummary[]>('/student/favorites')
}

export function addStudentFavoriteApi(resourceId: number): Promise<void> {
  return post<void>(`/student/favorites/${resourceId}`)
}

export function removeStudentFavoriteApi(resourceId: number): Promise<void> {
  return del<void>(`/student/favorites/${resourceId}`)
}