/**
 * 修改密码页面
 */
const request = require('../../utils/request')
const auth = require('../../utils/auth')
const { toastError, toastSuccess } = require('../../utils/ui')
const { applyPageTheme } = require('../../utils/page-theme')
const { PASSWORD_PAGE_COPY } = require('../../constants/copy')
const { normalizeText } = require('../../utils/account')
const pageLifecycleHelper = require('../../utils/page-lifecycle-helper')
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
    pageLifecycleHelper.handleProtectedPageLoad(auth)
  },

  onShow() {
    pageLifecycleHelper.handleProtectedPageShow(auth, {
      afterShow: () => {
        this.applyTheme()
      }
    })
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

    const oldLoginKey = normalizeText(this.data.oldLoginKey)
    const newLoginKey = normalizeText(this.data.newLoginKey)
    const confirmLoginKey = normalizeText(this.data.confirmLoginKey)
    const copy = this.data.copy

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
      await request.put('/users/password', {
        oldLoginKey,
        newLoginKey,
        confirmLoginKey
      })
      toastSuccess(copy.toast.success)
      auth.logout()
      setTimeout(() => {
        wx.reLaunch({
          url: '/pages/login/login'
        })
      }, 1000)
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
