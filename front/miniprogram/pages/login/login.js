/**
 * 登录/注册页面
 */
const request = require('../../utils/request')
const auth = require('../../utils/auth')
const { toastError, toastSuccess } = require('../../utils/ui')
const { applyPageTheme } = require('../../utils/page-theme')
const { LOGIN_PAGE_COPY } = require('../../constants/copy')
const { normalizeText, normalizeEmail, isValidEmail } = require('../../utils/account')
const { refreshAllTabBadges } = require('../../utils/tab-badge')
const {
  evaluatePasswordStrength,
  getPasswordStrengthInlineText
} = require('../../utils/password')

const EMPTY_REGISTER_FORM = {
  nickname: '',
  password: '',
  confirmPassword: '',
  email: '',
  passwordStrengthLevel: '',
  passwordStrengthInlineText: ''
}

const EMPTY_LOGIN_FORM = {
  account: '',
  loginKey: ''
}

Page({
  data: {
    mode: 'login',
    nickname: '',
    password: '',
    confirmPassword: '',
    email: '',
    passwordStrengthLevel: '',
    passwordStrengthInlineText: '',
    account: '',
    loginKey: '',
    showResult: false,
    registeredWolfNo: '',
    loading: false,
    themeClass: 'theme-retro-blue',
    copy: LOGIN_PAGE_COPY
  },

  onLoad(options) {
    if (auth.isLoggedIn()) {
      wx.switchTab({
        url: '/pages/chat/chat'
      })
      return
    }

    const nextData = {}
    if (options && (options.mode === 'login' || options.mode === 'register')) {
      nextData.mode = options.mode
    }
    if (options && options.account) {
      nextData.account = decodeURIComponent(options.account)
    }
    if (Object.keys(nextData).length > 0) {
      this.setData(nextData)
    }
  },

  onShow() {
    this.applyTheme()
  },

  switchToLogin() {
    this.setData({
      mode: 'login',
      ...EMPTY_LOGIN_FORM,
      showResult: false,
      registeredWolfNo: ''
    })
  },

  switchToRegister() {
    this.setData({
      mode: 'register',
      ...EMPTY_REGISTER_FORM,
      showResult: false,
      registeredWolfNo: ''
    })
  },

  onNicknameInput(e) {
    this.setData({
      nickname: e.detail.value || ''
    })
  },

  onPasswordInput(e) {
    const password = e.detail.value || ''
    const strength = evaluatePasswordStrength(password)
    this.setData({
      password,
      passwordStrengthLevel: strength.level,
      passwordStrengthInlineText: getPasswordStrengthInlineText(strength.level)
    })
  },

  onConfirmPasswordInput(e) {
    this.setData({
      confirmPassword: e.detail.value || ''
    })
  },

  onEmailInput(e) {
    this.setData({
      email: e.detail.value || ''
    })
  },

  onAccountInput(e) {
    this.setData({
      account: e.detail.value || ''
    })
  },

  onLoginKeyInput(e) {
    this.setData({
      loginKey: e.detail.value || ''
    })
  },

  goResetPassword() {
    const account = normalizeText(this.data.account)
    const maybeEmail = isValidEmail(account)
    const query = maybeEmail ? `?email=${encodeURIComponent(account)}` : ''
    wx.navigateTo({
      url: `/pages/reset-password/reset-password${query}`
    })
  },

  async handleRegister() {
    if (this.data.loading) return

    const copy = this.data.copy
    const normalizedNickname = normalizeText(this.data.nickname)
    const normalizedPassword = normalizeText(this.data.password)
    const normalizedConfirmPassword = normalizeText(this.data.confirmPassword)
    const normalizedEmail = normalizeEmail(this.data.email)

    if (!normalizedNickname) {
      toastError(copy.validation.nicknameRequired)
      return
    }

    if (!normalizedPassword || normalizedPassword.length < 6) {
      toastError(copy.validation.passwordMinLength)
      return
    }

    if (!normalizedConfirmPassword) {
      toastError(copy.validation.confirmPasswordRequired)
      return
    }

    if (normalizedPassword !== normalizedConfirmPassword) {
      toastError(copy.validation.registerPasswordMismatch)
      return
    }

    if (normalizedEmail && !isValidEmail(normalizedEmail)) {
      toastError(copy.validation.invalidEmail)
      return
    }

    this.setData({ loading: true })

    try {
      const payload = {
        nickname: normalizedNickname,
        password: normalizedPassword
      }
      if (normalizedEmail) {
        payload.email = normalizedEmail
      }
      const res = await request.post('/auth/register', payload)
      this.saveAuthSession(res.data)
      this.setData({
        showResult: true,
        registeredWolfNo: res.data.userInfo.wolfNo || ''
      })
      toastSuccess(copy.toast.registerSuccess)
    } catch (error) {
      toastError(error, copy.toast.registerFail)
    } finally {
      this.setData({ loading: false })
    }
  },

  async handleLogin() {
    if (this.data.loading) return

    const copy = this.data.copy
    const normalizedAccount = normalizeText(this.data.account)
    const normalizedLoginKey = normalizeText(this.data.loginKey)

    if (!normalizedAccount) {
      toastError(copy.validation.accountRequired)
      return
    }

    if (!normalizedLoginKey) {
      toastError(copy.validation.loginPasswordRequired)
      return
    }

    this.setData({ loading: true })

    try {
      const res = await request.post('/auth/login', {
        account: normalizedAccount,
        loginKey: normalizedLoginKey
      })
      this.saveAuthSession(res.data)
      toastSuccess(copy.toast.loginSuccess)
      setTimeout(() => {
        wx.switchTab({
          url: '/pages/chat/chat'
        })
      }, 800)
    } catch (error) {
      toastError(error, copy.toast.loginFail)
    } finally {
      this.setData({ loading: false })
    }
  },

  handleConfirmRegister() {
    wx.switchTab({
      url: '/pages/chat/chat'
    })
  },

  saveAuthSession(authPayload) {
    if (!authPayload || !authPayload.token || !authPayload.userInfo) {
      throw new Error('登录态返回异常')
    }
    auth.setToken(authPayload.token)
    auth.setUserInfo(authPayload.userInfo)
    refreshAllTabBadges()
  },

  applyTheme() {
    applyPageTheme(this)
  }
})
