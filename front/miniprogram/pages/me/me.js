/**
 * 我的页面
 */
const request = require('../../utils/request')
const auth = require('../../utils/auth')
const { toastError, toastSuccess } = require('../../utils/ui')
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

  handleDeactivateAccount() {
    wx.showModal({
      title: '注销账号',
      content: '注销后将立即退出登录，且当前账号不可恢复。确定继续吗？',
      confirmText: '继续',
      success: (firstConfirmRes) => {
        if (!firstConfirmRes.confirm) {
          return
        }
        wx.showModal({
          title: '再次确认',
          content: '该操作用于测试环境，是否立即注销当前账号？',
          confirmColor: '#b4474f',
          confirmText: '立即注销',
          success: (secondConfirmRes) => {
            if (!secondConfirmRes.confirm) {
              return
            }
            this.doDeactivateAccount()
          }
        })
      }
    })
  },

  async doDeactivateAccount() {
    try {
      const res = await request.del('/users/me')
      if (res.code !== 0) {
        return
      }
      auth.logout()
      toastSuccess('账号已注销')
      setTimeout(() => {
        wx.reLaunch({ url: '/pages/login/login' })
      }, 300)
    } catch (error) {
      toastError(error, '注销失败')
    }
  },

  goFollow() {
    wx.navigateTo({
      url: '/pages/follow/follow'
    })
  },

  goEmailManage() {
    wx.navigateTo({
      url: '/pages/email/email'
    })
  },

  goChangePassword() {
    wx.navigateTo({
      url: '/pages/password/password'
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
