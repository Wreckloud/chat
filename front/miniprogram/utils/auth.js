/**
 * token 本地存储工具
 */
const TOKEN_KEY = 'wolfchat_token'
const USER_INFO_KEY = 'wolfchat_user_info'

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
    wx.setStorageSync(USER_INFO_KEY, userInfo)
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
   * 退出登录
   */
  logout() {
    this.removeToken()
    this.removeUserInfo()
  }
}

module.exports = auth





