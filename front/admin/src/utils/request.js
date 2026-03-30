import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { readToken, clearToken, clearAdminInfo } from '@/utils/auth-storage'

const service = axios.create({
  baseURL: '/api',
  timeout: 10000
})

service.interceptors.request.use(
  (config) => {
    config.headers['X-Client-Type'] = 'ADMIN_CONSOLE'
    config.headers['X-Client-Version'] = 'web-admin-v1'
    const token = readToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

service.interceptors.response.use(
  (response) => {
    const body = response.data || {}
    if (body.code === 0) {
      return body.data
    }

    if (body.code === 2001 || body.code === 2002) {
      clearToken()
      clearAdminInfo()
      if (router.currentRoute.value.path !== '/login') {
        router.replace('/login')
      }
      return Promise.reject(new Error('登录状态已失效'))
    }

    return Promise.reject(new Error(body.message || '请求失败'))
  },
  (error) => {
    const status = error?.response?.status
    if (status === 401) {
      clearToken()
      clearAdminInfo()
      if (router.currentRoute.value.path !== '/login') {
        router.replace('/login')
      }
      return Promise.reject(new Error('登录状态已失效'))
    }

    const message = error?.message || '网络异常，请稍后重试'
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export default service
