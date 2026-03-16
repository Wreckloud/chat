/**
 * token 本地存储工具
 */
const TOKEN_KEY = 'wolfchat_token'
const USER_INFO_KEY = 'wolfchat_user_info'
const LOGIN_PAGE_URL = '/pages/login/login'
const { DEFAULT_AVATAR } = require('./user')
const TAB_INDEX_CHAT = 0
const TAB_INDEX_ME = 2
let authRedirecting = false

const auth = {
  /**
   * 保存 token
   */
  setToken(token) {
    wx.setStorageSync(TOKEN_KEY, token)
  },

  /**
   * 获取 token
   */
  getToken() {
    return wx.getStorageSync(TOKEN_KEY) || ''
  },

  /**
   * 清除 token
   */
  removeToken() {
    wx.removeStorageSync(TOKEN_KEY)
  },

  /**
   * 保存用户信息
   */
  setUserInfo(userInfo) {
    if (!userInfo) {
      wx.removeStorageSync(USER_INFO_KEY)
      return
    }

    const normalized = { ...userInfo }
    if (!normalized.avatar) {
      normalized.avatar = DEFAULT_AVATAR
    }
    if (!normalized.nickname && normalized.wolfNo) {
      normalized.nickname = normalized.wolfNo
    }
    wx.setStorageSync(USER_INFO_KEY, normalized)
  },

  /**
   * 获取用户信息
   */
  getUserInfo() {
    return wx.getStorageSync(USER_INFO_KEY) || null
  },

  /**
   * 清除用户信息
   */
  removeUserInfo() {
    wx.removeStorageSync(USER_INFO_KEY)
  },

  /**
   * 检查是否已登录
   */
  isLoggedIn() {
    return !!this.getToken()
  },

  /**
   * 检查登录态，不满足时跳转登录页
   */
  requireLogin() {
    if (this.isLoggedIn()) {
      return true
    }
    wx.redirectTo({ url: LOGIN_PAGE_URL })
    return false
  },

  /**
   * 退出登录
   */
  logout() {
    this.removeToken()
    this.removeUserInfo()
    wx.removeTabBarBadge({ index: TAB_INDEX_CHAT })
    wx.removeTabBarBadge({ index: TAB_INDEX_ME })
  },

  /**
   * 登录态失效处理（清理本地并跳转登录）
   */
  handleAuthExpired() {
    this.logout()
    if (authRedirecting) {
      return
    }
    const pages = getCurrentPages()
    const currentPage = pages[pages.length - 1]
    if (currentPage && currentPage.route === 'pages/login/login') {
      return
    }
    authRedirecting = true
    wx.reLaunch({
      url: LOGIN_PAGE_URL,
      complete() {
        authRedirecting = false
      }
    })
  }
}

module.exports = auth

