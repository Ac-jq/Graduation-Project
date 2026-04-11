import axios, { AxiosError, type AxiosInstance, type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiError, ApiResult } from '@/types/common'
import { navigateTo } from '@/core/navigation'
import { clearSession, getToken } from '@/core/session'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '/api'
const REQUEST_TIMEOUT = Number(import.meta.env.VITE_API_TIMEOUT ?? 10000)
const SUCCESS_CODES = new Set([0, 200])

let unauthorizedRedirecting = false

function createApiError(message: string, options?: Partial<ApiError>): ApiError {
  const error = new Error(message) as ApiError
  error.isApiError = true
  error.status = options?.status
  error.code = options?.code
  error.details = options?.details
  return error
}

function handleApiError(error: ApiError): void {
  if (error.status === 401 || error.code === 401) {
    clearSession()
    ElMessage.error('登录已失效，请重新登录')
    if (!unauthorizedRedirecting) {
      unauthorizedRedirecting = true
      void navigateTo('/login').finally(() => {
        unauthorizedRedirecting = false
      })
    }
    return
  }

  if (error.status === 403 || error.code === 403) {
    ElMessage.error('无权限访问当前资源')
    void navigateTo('/forbidden')
    return
  }

  if (error.code === 600) {
    ElMessage.error(error.message || '业务处理失败')
    return
  }

  if ((error.status ?? 0) >= 500) {
    ElMessage.error('服务暂时不可用，请稍后重试')
  }
}

function normalizeAxiosError(error: AxiosError<ApiResult<unknown>>): ApiError {
  const status = error.response?.status
  const body = error.response?.data
  return createApiError(body?.message || error.message || '请求失败', {
    status,
    code: body?.code,
    details: body ?? error.toJSON()
  })
}

const httpClient: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  timeout: REQUEST_TIMEOUT
})

httpClient.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers = config.headers ?? {}
    config.headers.Authorization = token
  }
  return config
})

httpClient.interceptors.response.use(
  (response) => {
    const body = response.data as ApiResult<unknown>

    if (body && typeof body === 'object' && typeof body.code === 'number') {
      if (SUCCESS_CODES.has(body.code)) {
        return body.data
      }

      const apiError = createApiError(body.message || '请求失败', {
        status: response.status,
        code: body.code,
        details: body
      })
      handleApiError(apiError)
      return Promise.reject(apiError)
    }

    return response.data
  },
  (error: AxiosError<ApiResult<unknown>>) => {
    const apiError = normalizeAxiosError(error)
    handleApiError(apiError)
    return Promise.reject(apiError)
  }
)

export function request<T>(config: AxiosRequestConfig): Promise<T> {
  return httpClient.request<unknown, T>(config)
}

export function get<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
  return httpClient.get<unknown, T>(url, config)
}

export function post<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
  return httpClient.post<unknown, T>(url, data, config)
}

export function put<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
  return httpClient.put<unknown, T>(url, data, config)
}

export function del<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
  return httpClient.delete<unknown, T>(url, config)
}

export { httpClient }
