export interface PageQuery {
  current?: number
  size?: number
}

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

export function createEmptyPage<T>(): PageResult<T> {
  return {
    records: [],
    total: 0,
    size: 10,
    current: 1,
    pages: 0
  }
}
