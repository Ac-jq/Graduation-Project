export interface ResourceTag {
  tagId: number
  name: string
  description: string | null
}

export interface ResourceCategory {
  categoryId: number
  name: string
  description: string | null
  sortNo: number | null
  status: string | null
}

export interface ResourceQuery {
  categoryId?: number
  tagId?: number
  keyword?: string
}

export interface AdminResourceQuery {
  status?: string
  keyword?: string
}

export interface ResourceSummary {
  resourceId: number
  title: string
  summaryText: string
  resourceType: string
  contentUrl: string
  coverUrl: string | null
  status: string
  publishedAt: string | null
  categoryId: number
  categoryName: string
  tags: ResourceTag[]
  favorite: boolean
  favoriteCount: number
  viewCount: number
}

export interface ResourceDetail extends ResourceSummary {
  createdAt: string
  updatedAt: string
}

export interface UpsertResourceRequest {
  title: string
  summaryText: string
  resourceType: string
  contentUrl: string
  coverUrl?: string | null
  categoryId: number
  tagIds?: number[]
}

export interface UpsertResourceCategoryRequest {
  name: string
  description?: string | null
  sortNo?: number | null
  status?: string | null
}

export interface UpsertResourceTagRequest {
  name: string
  description?: string | null
}