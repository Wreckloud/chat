/**
 * 我的页面
 */
const request = require('../../utils/request')
const auth = require('../../utils/auth')

Page({
  data: {
    userInfo: null,
    loading: false
  },

  onLoad() {
    // tabBar 页面，检查登录状态
    if (!auth.isLoggedIn()) {
      wx.redirectTo({
        url: '/pages/login/login'
      })
      return
    }

    // 加载用户信息
    this.loadUserInfo()
  },

  onShow() {
    // 每次显示时刷新用户信息
    if (auth.isLoggedIn()) {
      this.loadUserInfo()
    }
  },

  /**
   * 加载用户信息
   */
  async loadUserInfo() {
    // 先从本地获取
    const localUserInfo = auth.getUserInfo()
    if (localUserInfo) {
      this.setData({
        userInfo: localUserInfo
      })
    }

    this.setData({ loading: true })

    try {
      const res = await request.get('/users/me')
      
      if (res.code === 0 && res.data) {
        // 更新本地存储（会自动补默认头像/昵称）
        auth.setUserInfo(res.data)
        this.setData({
          userInfo: auth.getUserInfo()
        })
      }
    } catch (error) {
      wx.showToast({
        title: error.message || '加载失败',
        icon: 'none'
      })
    } finally {
      this.setData({ loading: false })
    }
  },

  /**
   * 退出登录
   */
  handleLogout() {
    wx.showModal({
      title: '提示',
      content: '确定要退出登录吗？',
      success: (res) => {
        if (res.confirm) {
          auth.logout()
          wx.redirectTo({
            url: '/pages/login/login'
          })
        }
      }
    })
  },
  /**
   * 进入关注列表
   */
  goFollow() {
    wx.navigateTo({
      url: '/pages/follow/follow'
    })
  }
})





