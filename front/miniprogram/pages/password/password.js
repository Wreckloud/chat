/**
 * 修改密码页面
 */
const request = require('../../utils/request')
const auth = require('../../utils/auth')
const { toastError, toastSuccess } = require('../../utils/ui')
const { applyPageTheme } = require('../../utils/page-theme')
const { PASSWORD_PAGE_COPY } = require('../../constants/copy')
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
    themeClass: 'theme-retro-blue',
    copy: PASSWORD_PAGE_COPY
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
    const copy = PASSWORD_PAGE_COPY

    if (!oldLoginKey) {
      toastError(copy.validation.oldRequired)
      return
    }

    if (!newLoginKey) {
      toastError(copy.validation.newRequired)
      return
    }

    if (newLoginKey.length < 6) {
      toastError(copy.validation.newMinLength)
      return
    }

    if (!confirmLoginKey) {
      toastError(copy.validation.confirmRequired)
      return
    }

    if (newLoginKey !== confirmLoginKey) {
      toastError(copy.validation.newMismatch)
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
        toastSuccess(copy.toast.success)
        auth.logout()
        setTimeout(() => {
          wx.reLaunch({
            url: '/pages/login/login'
          })
        }, 1000)
      }
    } catch (error) {
      toastError(error, copy.toast.fail)
    } finally {
      this.setData({ loading: false })
    }
  },

  applyTheme() {
    applyPageTheme(this)
  }
})
