/**
 * 我的页面
 */
const request = require('../../utils/request')
const auth = require('../../utils/auth')
const { toastError } = require('../../utils/ui')
const { setThemeName, listThemes } = require('../../utils/theme')
const { applyPageTheme } = require('../../utils/page-theme')

Page({
  data: {
    userInfo: null,
    loading: false,
    themeName: 'retro_blue',
    themeClass: 'theme-retro-blue',
    themeOptions: []
  },

  onLoad() {
    if (!auth.requireLogin()) {
      return
    }
    this.setData({
      themeOptions: listThemes()
    })
  },

  onShow() {
    if (!auth.requireLogin()) return
    this.applyTheme()
    this.loadUserInfo()
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
  },

  onSelectTheme(e) {
    const themeName = e.currentTarget.dataset.theme
    if (!themeName || themeName === this.data.themeName) {
      return
    }
    setThemeName(themeName)
    this.applyTheme(themeName)
    wx.showToast({
      title: '主题已切换',
      icon: 'success'
    })
  },

  applyTheme(themeName) {
    applyPageTheme(this, {
      themeName,
      tabBar: true,
      extraData: (themeContext) => ({
        themeName: themeContext.themeName
      })
    })
  }
})
