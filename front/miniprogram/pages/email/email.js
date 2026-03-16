/**
 * 邮箱绑定管理页面
 */
const request = require('../../utils/request')
const auth = require('../../utils/auth')
const { toastError, toastSuccess } = require('../../utils/ui')
const { applyPageTheme } = require('../../utils/page-theme')
const { EMAIL_BIND_PAGE_COPY } = require('../../constants/copy')
const { normalizeEmail, isValidEmail } = require('../../utils/account')
const pageLifecycleHelper = require('../../utils/page-lifecycle-helper')

Page({
  data: {
    userInfo: null,
    emailInput: '',
    statusText: EMAIL_BIND_PAGE_COPY.status.unbound,
    statusType: 'unbound',
    statusEmail: '未设置',
    statusHint: EMAIL_BIND_PAGE_COPY.status.unboundHint,
    submitText: EMAIL_BIND_PAGE_COPY.actions.sendLink,
    emailInputDisabled: false,
    canSendLink: true,
    submitting: false,
    themeClass: 'theme-retro-blue',
    copy: EMAIL_BIND_PAGE_COPY
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

  async loadUserInfo() {
    try {
      const res = await request.get('/users/me')
      const userInfo = res.data || null
      auth.setUserInfo(userInfo)
      this.setData(
        {
          userInfo,
          emailInput: userInfo && userInfo.email ? userInfo.email : ''
        },
        () => this.syncViewState()
      )
    } catch (error) {
      toastError(error, EMAIL_BIND_PAGE_COPY.toast.loadFail)
    }
  },

  onEmailInput(e) {
    this.setData(
      {
        emailInput: e.detail.value || ''
      },
      () => this.syncViewState()
    )
  },

  async handleSendVerifyLink() {
    if (this.data.submitting) return
    if (!this.data.canSendLink) return

    const copy = this.data.copy
    const normalizedEmail = normalizeEmail(this.data.emailInput)
    if (!normalizedEmail) {
      toastError(copy.validation.emailRequired)
      return
    }
    if (!isValidEmail(normalizedEmail)) {
      toastError(copy.validation.invalidEmail)
      return
    }

    this.setData({ submitting: true })
    try {
      await request.post('/users/email-link/send', {
        email: normalizedEmail
      })
      toastSuccess(copy.toast.sendSuccess)
    } catch (error) {
      toastError(error, copy.toast.sendFail)
    } finally {
      this.setData({ submitting: false })
    }
  },

  syncViewState() {
    const meta = this.getStatusMeta()
    const hasBoundEmail = !!(this.data.userInfo && this.data.userInfo.email)
    const canSendLink = !(this.data.userInfo && this.data.userInfo.emailVerified)
    this.setData({
      statusText: meta.text,
      statusType: meta.type,
      statusEmail: meta.email,
      statusHint: meta.hint,
      submitText: this.resolveSubmitText(),
      emailInputDisabled: hasBoundEmail,
      canSendLink
    })
  },

  getStatusMeta() {
    const userInfo = this.data.userInfo
    const statusCopy = EMAIL_BIND_PAGE_COPY.status

    if (!userInfo || !userInfo.email) {
      return {
        text: statusCopy.unbound,
        type: 'unbound',
        email: '未设置',
        hint: statusCopy.unboundHint
      }
    }

    if (userInfo.emailVerified) {
      return {
        text: statusCopy.verified,
        type: 'verified',
        email: userInfo.email,
        hint: statusCopy.verifiedHint
      }
    }

    return {
      text: statusCopy.pending,
      type: 'pending',
      email: userInfo.email,
      hint: statusCopy.pendingHint
    }
  },

  resolveSubmitText() {
    const userInfo = this.data.userInfo
    const normalizedInput = (this.data.emailInput || '').trim().toLowerCase()

    if (!userInfo || !userInfo.email) {
      return EMAIL_BIND_PAGE_COPY.actions.sendLink
    }

    if (userInfo.emailVerified) {
      return EMAIL_BIND_PAGE_COPY.actions.verifiedLocked
    }

    if (normalizedInput && normalizedInput === userInfo.email.toLowerCase()) {
      return EMAIL_BIND_PAGE_COPY.actions.resendLink
    }

    return EMAIL_BIND_PAGE_COPY.actions.sendLink
  },

  applyTheme() {
    applyPageTheme(this)
  }
})
