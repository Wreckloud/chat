/**
 * WolfChat 认证状态管理
 * @author Wreckloud
 * @date 2024-12-18
 */

import { defineStore } from 'pinia'
import { ref } from 'vue'
import router from '@/router'
import logger from '@/utils/logger'

export const useAuthStore = defineStore('auth', () => {
  // 状态
  const token = ref(localStorage.getItem('wolf-admin-token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('wolf-admin-user') || '{}'))
  
  /**
   * 是否已登录
   */
  const isLogin = () => {
    return !!token.value
  }
  
  /**
   * 设置 Token
   */
  const setToken = (newToken) => {
    token.value = newToken
    localStorage.setItem('wolf-admin-token', newToken)
    logger.info('AuthStore', 'Token已设置')
  }
  
  /**
   * 设置用户信息
   */
  const setUserInfo = (user) => {
    userInfo.value = user
    localStorage.setItem('wolf-admin-user', JSON.stringify(user))
    logger.info('AuthStore', '用户信息已设置', { userId: user.userId })
  }
  
  /**
   * 登录
   */
  const login = (loginData) => {
    setToken(loginData.token)
    setUserInfo({
      userId: loginData.userId,
      username: loginData.username,
      wfNo: loginData.wfNo,
      avatar: loginData.avatar
    })
    logger.info('AuthStore', '登录成功', { userId: loginData.userId })
  }
  
  /**
   * 退出登录
   */
  const logout = () => {
    logger.info('AuthStore', '退出登录', { userId: userInfo.value.userId })
    
    token.value = ''
    userInfo.value = {}
    localStorage.removeItem('wolf-admin-token')
    localStorage.removeItem('wolf-admin-user')
    
    router.push('/login')
  }
  
  return {
    token,
    userInfo,
    isLogin,
    setToken,
    setUserInfo,
    login,
    logout
  }
})

