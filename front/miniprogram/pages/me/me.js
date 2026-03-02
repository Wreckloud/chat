/**
 * 我的页面
 */
const request = require('../../utils/request')
const auth = require('../../utils/auth')
const { toastError } = require('../../utils/ui')

Page({
  data: {
    userInfo: null,
    loading: false
  },

  onLoad() {
    if (!auth.isLoggedIn()) {
      wx.redirectTo({ url: '/pages/login/login' })
      return
    }
    this.loadUserInfo()
  },

  onShow() {
    if (auth.isLoggedIn()) {
      this.loadUserInfo()
    }
  },

  async loadUserInfo() {
    const localUserInfo = auth.getUserInfo()
    if (localUserInfo) {
      this.setData({ userInfo: localUserInfo })
    }

    this.setData({ loading: true })
    try {
      const res = await request.get('/users/me')
      if (res.code === 0 && res.data) {
        auth.setUserInfo(res.data)
        this.setData({ userInfo: auth.getUserInfo() })
      }
    } catch (error) {
      toastError(error, '加载失败')
    } finally {
      this.setData({ loading: false })
    }
  },

  handleLogout() {
    wx.showModal({
      title: '提示',
      content: '确定要退出登录吗？',
      success: (res) => {
        if (res.confirm) {
          auth.logout()
          wx.redirectTo({ url: '/pages/login/login' })
        }
      }
    })
  },

  goFollow() {
    wx.navigateTo({
      url: '/pages/follow/follow'
    })
  }
})
