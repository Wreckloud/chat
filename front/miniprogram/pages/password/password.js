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

const SEND_CODE_INTERVAL_SECONDS = 60

Page({
  data: {
    userInfo: null,
    emailDisplay: '未绑定邮箱',
    hasVerifiedEmail: false,
    oldLoginKey: '',
    newLoginKey: '',
    confirmLoginKey: '',
    emailCode: '',
    passwordStrengthLevel: '',
    passwordStrengthInlineText: '',
    codeSending: false,
    codeCooldown: 0,
    sendCodeButtonText: PASSWORD_PAGE_COPY.actions.sendCode,
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
        this.loadUserInfo()
      }
    })
  },

  onHide() {
    this.clearCodeCooldownTimer()
  },

  onUnload() {
    this.clearCodeCooldownTimer()
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

  onEmailCodeInput(e) {
    this.setData({
      emailCode: e.detail.value || ''
    })
  },

  async loadUserInfo() {
    const copy = this.data.copy
    try {
      const res = await request.get('/users/me')
      const userInfo = res.data || null
      auth.setUserInfo(userInfo)
      this.setData({
        userInfo
      })
      this.syncEmailMeta()
    } catch (error) {
      toastError(error, copy.toast.loadFail)
    }
  },

  syncEmailMeta() {
    const userInfo = this.data.userInfo || null
    const email = normalizeText(userInfo && userInfo.email)
    const hasVerifiedEmail = !!(email && userInfo && userInfo.emailVerified)
    this.setData({
      hasVerifiedEmail,
      emailDisplay: hasVerifiedEmail ? this.maskEmail(email) : '未绑定或未认证邮箱'
    })
  },

  maskEmail(email) {
    const safeEmail = normalizeText(email)
    const atIndex = safeEmail.indexOf('@')
    if (atIndex <= 1) {
      return safeEmail
    }
    const localPart = safeEmail.slice(0, atIndex)
    const domainPart = safeEmail.slice(atIndex)
    const first = localPart.slice(0, 1)
    const last = localPart.slice(-1)
    return `${first}***${last}${domainPart}`
  },

  async handleSendCode() {
    if (this.data.codeSending || this.data.codeCooldown > 0) {
      return
    }
    const copy = this.data.copy
    if (!this.data.hasVerifiedEmail) {
      toastError(copy.validation.emailUnavailable)
      return
    }

    this.setData({ codeSending: true })
    try {
      await request.post('/users/password/change-code/send')
      toastSuccess(copy.toast.sendCodeSuccess)
      this.startCodeCooldown(SEND_CODE_INTERVAL_SECONDS)
    } catch (error) {
      toastError(error, copy.toast.sendCodeFail)
    } finally {
      this.setData({ codeSending: false })
    }
  },

  async handleSubmit() {
    if (this.data.loading) return

    const oldLoginKey = normalizeText(this.data.oldLoginKey)
    const newLoginKey = normalizeText(this.data.newLoginKey)
    const confirmLoginKey = normalizeText(this.data.confirmLoginKey)
    const emailCode = normalizeText(this.data.emailCode)
    const copy = this.data.copy

    if (!this.data.hasVerifiedEmail) {
      toastError(copy.validation.emailUnavailable)
      return
    }

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

    if (!emailCode) {
      toastError(copy.validation.codeRequired)
      return
    }
    if (!/^\d{6}$/.test(emailCode)) {
      toastError(copy.validation.codeInvalid)
      return
    }

    this.setData({ loading: true })

    try {
      await request.put('/users/password', {
        oldLoginKey,
        newLoginKey,
        confirmLoginKey,
        emailCode
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

  startCodeCooldown(seconds) {
    this.clearCodeCooldownTimer()
    this.setData({
      codeCooldown: seconds,
      sendCodeButtonText: this.formatCodeCooldownText(seconds)
    })
    this.codeCooldownTimer = setInterval(() => {
      const next = this.data.codeCooldown - 1
      if (next <= 0) {
        this.clearCodeCooldownTimer()
        this.setData({
          codeCooldown: 0,
          sendCodeButtonText: this.data.copy.actions.sendCode
        })
        return
      }
      this.setData({
        codeCooldown: next,
        sendCodeButtonText: this.formatCodeCooldownText(next)
      })
    }, 1000)
  },

  clearCodeCooldownTimer() {
    if (this.codeCooldownTimer) {
      clearInterval(this.codeCooldownTimer)
      this.codeCooldownTimer = null
    }
  },

  formatCodeCooldownText(seconds) {
    return `${seconds}s`
  },

  applyTheme() {
    applyPageTheme(this)
  }
})
