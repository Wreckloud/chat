/**
 * 找回密码页面（发送重置链接）
 */
const request = require('../../utils/request')
const { toastError, toastSuccess } = require('../../utils/ui')
const { applyPageTheme } = require('../../utils/page-theme')
const { RESET_PASSWORD_PAGE_COPY } = require('../../constants/copy')

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const SEND_LINK_INTERVAL_SECONDS = 60

Page({
  data: {
    email: '',
    sending: false,
    cooldown: 0,
    sendButtonText: RESET_PASSWORD_PAGE_COPY.actions.sendLink,
    themeClass: 'theme-retro-blue',
    copy: RESET_PASSWORD_PAGE_COPY
  },

  onLoad(options) {
    if (options && options.email) {
      this.setData({
        email: decodeURIComponent(options.email)
      })
    }
  },

  onShow() {
    this.applyTheme()
  },

  onUnload() {
    this.clearCountdownTimer()
  },

  onEmailInput(e) {
    this.setData({
      email: e.detail.value || ''
    })
  },

  async handleSendLink() {
    if (this.data.sending || this.data.cooldown > 0) {
      return
    }

    const normalizedEmail = (this.data.email || '').trim().toLowerCase()
    if (!normalizedEmail) {
      toastError(RESET_PASSWORD_PAGE_COPY.validation.emailRequired)
      return
    }
    if (!EMAIL_REGEX.test(normalizedEmail)) {
      toastError(RESET_PASSWORD_PAGE_COPY.validation.invalidEmail)
      return
    }

    this.setData({ sending: true })
    try {
      const res = await request.post('/auth/password/reset-link/send', {
        email: normalizedEmail
      })
      if (res.code === 0) {
        toastSuccess(RESET_PASSWORD_PAGE_COPY.toast.sendSuccess)
        this.startCooldown(SEND_LINK_INTERVAL_SECONDS)
      }
    } catch (error) {
      toastError(error, RESET_PASSWORD_PAGE_COPY.toast.sendFail)
    } finally {
      this.setData({ sending: false })
    }
  },

  goBackToLogin() {
    const normalizedEmail = (this.data.email || '').trim()
    const query = normalizedEmail ? `?mode=login&account=${encodeURIComponent(normalizedEmail)}` : '?mode=login'
    wx.redirectTo({
      url: `/pages/login/login${query}`
    })
  },

  startCooldown(seconds) {
    this.clearCountdownTimer()
    this.setData({
      cooldown: seconds,
      sendButtonText: this.formatCooldownText(seconds)
    })

    this.cooldownTimer = setInterval(() => {
      const next = this.data.cooldown - 1
      if (next <= 0) {
        this.clearCountdownTimer()
        this.setData({
          cooldown: 0,
          sendButtonText: RESET_PASSWORD_PAGE_COPY.actions.sendLink
        })
        return
      }
      this.setData({
        cooldown: next,
        sendButtonText: this.formatCooldownText(next)
      })
    }, 1000)
  },

  clearCountdownTimer() {
    if (this.cooldownTimer) {
      clearInterval(this.cooldownTimer)
      this.cooldownTimer = null
    }
  },

  formatCooldownText(seconds) {
    return `${seconds}s`
  },

  applyTheme() {
    applyPageTheme(this)
  }
})
