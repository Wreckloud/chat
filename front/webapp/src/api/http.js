import axios from 'axios'
import { useAuthStore } from '@/stores/auth'

const AUTH_ERROR_CODES = new Set([2001, 2002, 2003])

const http = axios.create({
  baseURL: '/api',
  timeout: 10000
})

http.interceptors.request.use((config) => {
  const authStore = useAuthStore()
  const nextConfig = { ...config }
  nextConfig.headers = nextConfig.headers || {}
  nextConfig.headers['X-Client-Type'] = 'WEB'
  if (authStore.token) {
    nextConfig.headers.Authorization = `Bearer ${authStore.token}`
  }
  return nextConfig
})

http.interceptors.response.use(
  (response) => {
    const result = response.data || {}
    if (result.code === 0) {
      return result.data
    }
    const error = new Error(result.message || '请求失败')
    error.code = result.code
    error.kind = 'business'
    if (AUTH_ERROR_CODES.has(result.code)) {
      const authStore = useAuthStore()
      authStore.clearAuth()
      window.location.hash = '#/login'
    }
    throw error
  },
  (error) => {
    if (error.response) {
      const nextError = new Error(`请求失败(${error.response.status})`)
      nextError.kind = 'http'
      nextError.status = error.response.status
      throw nextError
    }
    const nextError = new Error('网络异常，请稍后重试')
    nextError.kind = 'network'
    throw nextError
  }
)

export default http

