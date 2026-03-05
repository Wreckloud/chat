/**
 * 修改密码页面
 */
const request = require('../../utils/request')
const auth = require('../../utils/auth')
const { toastError, toastSuccess } = require('../../utils/ui')
const { applyPageTheme } = require('../../utils/page-theme')
const {
  evaluatePasswordStrength,
  getPasswordStrengthInlineText
} = require('../../utils/password')

Page({
  data: {
    oldLoginKey: '',
    newLoginKey: '',
    confirmLoginKey: '',
    passwordStrengthLevel: '',
    passwordStrengthInlineText: '',
    loading: false,
    themeClass: 'theme-retro-blue'
  },

  onLoad() {
    if (!auth.requireLogin()) {
      return
    }
  },

  onShow() {
    if (!auth.requireLogin()) return
    this.applyTheme()
  },

  onOldPasswordInput(e) {
    this.setData({
      oldLoginKey: e.detail.value || ''
    })
  },

  onNewPasswordInput(e) {
    const newLoginKey = e.detail.value || ''
    const strength = evaluatePasswordStrength(newLoginKey)
    this.setData({
      newLoginKey,
      passwordStrengthLevel: strength.level,
      passwordStrengthInlineText: getPasswordStrengthInlineText(strength.level)
    })
  },

  onConfirmPasswordInput(e) {
    this.setData({
      confirmLoginKey: e.detail.value || ''
    })
  },

  async handleSubmit() {
    if (this.data.loading) return

    const oldLoginKey = this.data.oldLoginKey.trim()
    const newLoginKey = this.data.newLoginKey.trim()
    const confirmLoginKey = this.data.confirmLoginKey.trim()

    if (!oldLoginKey) {
      toastError('请输入原密码')
      return
    }

    if (!newLoginKey) {
      toastError('请输入新密码')
      return
    }

    if (newLoginKey.length < 6) {
      toastError('新密码至少6位')
      return
    }

    if (!confirmLoginKey) {
      toastError('请确认新密码')
      return
    }

    if (newLoginKey !== confirmLoginKey) {
      toastError('两次输入的新密码不一致')
      return
    }

    this.setData({ loading: true })

    try {
      const res = await request.put('/users/password', {
        oldLoginKey,
        newLoginKey,
        confirmLoginKey
      })
      if (res.code === 0) {
        toastSuccess('密码已修改，请重新登录')
        auth.logout()
        setTimeout(() => {
          wx.reLaunch({
            url: '/pages/login/login'
          })
        }, 1000)
      }
    } catch (error) {
      toastError(error, '修改失败')
    } finally {
      this.setData({ loading: false })
    }
  },

  applyTheme() {
    applyPageTheme(this)
  }
})
