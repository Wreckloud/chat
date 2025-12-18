/**
 * WolfChat 网络请求封装
 * @author Wreckloud
 * @date 2024-12-18
 */

import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { useAuthStore } from '@/store/auth'
import logger from './logger'

// 创建 axios 实例
const service = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8'
  }
})

// 请求拦截器
service.interceptors.request.use(
  (config) => {
    const authStore = useAuthStore()
    
    // 记录请求日志
    logger.request(config.method.toUpperCase(), config.url, config.data || config.params)
    
    // 添加 Token
    if (authStore.token) {
      config.headers.Authorization = `Bearer ${authStore.token}`
    }
    
    return config
  },
  (error) => {
    logger.error('Request', '请求拦截器错误', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  (response) => {
    const res = response.data
    const config = response.config
    
    // 记录响应日志
    logger.response(
      config.method.toUpperCase(),
      config.url,
      res.code,
      res
    )
    
    // 判断业务状态码
    if (res.code === 0) {
      // 成功
      return res.data
    } else if (res.code === -1001 || res.code === -1002 || res.code === -1003) {
      // Token 相关错误，需要重新登录
      logger.warn('Response', 'Token过期，需要重新登录', res)
      
      ElMessage.error('登录已过期，请重新登录')
      
      const authStore = useAuthStore()
      authStore.logout()
      
      router.push('/login')
      
      return Promise.reject(res)
    } else {
      // 业务错误
      logger.error('Response', '业务错误', res)
      
      ElMessage.error(res.message || '请求失败')
      
      return Promise.reject(res)
    }
  },
  (error) => {
    logger.error('Response', '网络请求失败', error)
    
    if (error.response) {
      // 服务器返回了错误状态码
      const status = error.response.status
      switch (status) {
        case 401:
          ElMessage.error('未授权，请重新登录')
          router.push('/login')
          break
        case 403:
          ElMessage.error('拒绝访问')
          break
        case 404:
          ElMessage.error('请求的资源不存在')
          break
        case 500:
          ElMessage.error('服务器内部错误')
          break
        default:
          ElMessage.error(`请求失败: ${status}`)
      }
    } else if (error.request) {
      // 请求已发出，但没有收到响应
      ElMessage.error('网络连接失败，请检查网络')
    } else {
      // 发生了触发请求错误的问题
      ElMessage.error('请求配置错误')
    }
    
    return Promise.reject(error)
  }
)

export default service

