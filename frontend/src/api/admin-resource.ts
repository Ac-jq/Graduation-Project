import { get, post, put } from './http'
import type {
  AdminResourceCategory,
  AdminResourceListItem,
  AdminResourceQuery,
  AdminResourceTag,
  CreateOrUpdateResourceCategoryRequest,
  CreateOrUpdateResourceRequest,
  CreateResourceTagRequest
} from './types'

export function fetchAdminResourcesApi(query: AdminResourceQuery = {}): Promise<AdminResourceListItem[]> {
  return get<AdminResourceListItem[]>('/admin/resources', { params: query })
}

export function createAdminResourceApi(payload: CreateOrUpdateResourceRequest): Promise<AdminResourceListItem> {
  return post<AdminResourceListItem>('/admin/resources', payload)
}

export function updateAdminResourceApi(resourceId: number, payload: CreateOrUpdateResourceRequest): Promise<AdminResourceListItem> {
  return put<AdminResourceListItem>(`/admin/resources/${resourceId}`, payload)
}

export function publishAdminResourceApi(resourceId: number): Promise<AdminResourceListItem> {
  return post<AdminResourceListItem>(`/admin/resources/${resourceId}/publish`)
}

export function offlineAdminResourceApi(resourceId: number): Promise<AdminResourceListItem> {
  return post<AdminResourceListItem>(`/admin/resources/${resourceId}/offline`)
}

export function fetchAdminResourceCategoriesApi(): Promise<AdminResourceCategory[]> {
  return get<AdminResourceCategory[]>('/admin/resource-categories')
}

export function createAdminResourceCategoryApi(payload: CreateOrUpdateResourceCategoryRequest): Promise<AdminResourceCategory> {
  return post<AdminResourceCategory>('/admin/resource-categories', payload)
}

export function updateAdminResourceCategoryApi(categoryId: number, payload: CreateOrUpdateResourceCategoryRequest): Promise<AdminResourceCategory> {
  return put<AdminResourceCategory>(`/admin/resource-categories/${categoryId}`, payload)
}

export function fetchAdminResourceTagsApi(): Promise<AdminResourceTag[]> {
  return get<AdminResourceTag[]>('/admin/resource-tags')
}

export function createAdminResourceTagApi(payload: CreateResourceTagRequest): Promise<AdminResourceTag> {
  return post<AdminResourceTag>('/admin/resource-tags', payload)
}