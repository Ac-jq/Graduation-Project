import type { ResourceCategory, ResourceSummary, ResourceTag, UpsertResourceCategoryRequest, UpsertResourceRequest, UpsertResourceTagRequest } from './resource'

export type AdminResourceListItem = ResourceSummary
export type AdminResourceCategory = ResourceCategory
export type AdminResourceTag = ResourceTag
export type CreateOrUpdateResourceRequest = UpsertResourceRequest
export type CreateOrUpdateResourceCategoryRequest = UpsertResourceCategoryRequest
export type CreateResourceTagRequest = UpsertResourceTagRequest

export interface ResourceAssetUploadResponse {
  fileName: string
  assetUrl: string
  contentType: string
  size: number
}
