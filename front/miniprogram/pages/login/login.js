/**
 * 登录/注册页面
 */
const request = require('../../utils/request')
const auth = require('../../utils/auth')
const { toastError } = require('../../utils/ui')
const { applyPageTheme } = require('../../utils/page-theme')
const { LOGIN_PAGE_COPY } = require('../../constants/copy')
const { normalizeText, normalizeEmail, isValidEmail } = require('../../utils/account')
const { refreshAllTabBadges } = require('../../utils/tab-badge')
const {
  evaluatePasswordStrength,
  getPasswordStrengthInlineText
} = require('../../utils/password')

const NICKNAME_MAX_LEN = 12
const EMAIL_ALREADY_USED_CODE = 2010

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
    canRegisterSubmit: false,
    canLoginSubmit: false,
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
      this.setData(nextData, () => {
        this.syncLoginSubmitState()
      })
      return
    }
    this.syncLoginSubmitState()
  },

  onShow() {
    this.applyTheme()
  },

  switchToLogin() {
    this.setData({
      mode: 'login',
      ...EMPTY_LOGIN_FORM,
      showResult: false,
      registeredWolfNo: '',
      canLoginSubmit: false
    })
  },

  switchToRegister() {
    this.setData({
      mode: 'register',
      ...EMPTY_REGISTER_FORM,
      showResult: false,
      registeredWolfNo: '',
      canRegisterSubmit: false
    })
  },

  onNicknameInput(e) {
    this.setData({
      nickname: e.detail.value || ''
    }, () => {
      this.syncRegisterSubmitState()
    })
  },

  onPasswordInput(e) {
    const password = e.detail.value || ''
    const strength = evaluatePasswordStrength(password)
    this.setData({
      password,
      passwordStrengthLevel: strength.level,
      passwordStrengthInlineText: getPasswordStrengthInlineText(strength.level)
    }, () => {
      this.syncRegisterSubmitState()
    })
  },

  onConfirmPasswordInput(e) {
    this.setData({
      confirmPassword: e.detail.value || ''
    }, () => {
      this.syncRegisterSubmitState()
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
    }, () => {
      this.syncLoginSubmitState()
    })
  },

  onLoginKeyInput(e) {
    this.setData({
      loginKey: e.detail.value || ''
    }, () => {
      this.syncLoginSubmitState()
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
    if (this.registerSubmitting || this.data.loading || !this.data.canRegisterSubmit) return

    const copy = this.data.copy
    const normalizedNickname = normalizeText(this.data.nickname)
    const normalizedPassword = normalizeText(this.data.password)
    const normalizedConfirmPassword = normalizeText(this.data.confirmPassword)
    const normalizedEmail = normalizeEmail(this.data.email)

    if (normalizedNickname.length > NICKNAME_MAX_LEN) {
      toastError(`称谓最多 ${NICKNAME_MAX_LEN} 个字符`)
      return
    }

    if (normalizedPassword.length < 6) {
      toastError(copy.validation.passwordMinLength)
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

    this.registerSubmitting = true
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
    } catch (error) {
      // 并发双击时，可能出现“一个请求已注册成功，另一个请求返回邮箱占用”。
      // 若当前已拿到登录态，则按注册成功处理，避免用户感知到无意义失败提示。
      if (Number(error && error.code) === EMAIL_ALREADY_USED_CODE && auth.isLoggedIn()) {
        const userInfo = auth.getUserInfo() || {}
        this.setData({
          showResult: true,
          registeredWolfNo: userInfo.wolfNo || this.data.registeredWolfNo || ''
        })
        return
      }
      toastError(error, copy.toast.registerFail)
    } finally {
      this.registerSubmitting = false
      this.setData({ loading: false })
    }
  },

  async handleLogin() {
    if (this.loginSubmitting || this.data.loading || !this.data.canLoginSubmit) return

    const copy = this.data.copy
    const normalizedAccount = normalizeText(this.data.account)
    const normalizedLoginKey = normalizeText(this.data.loginKey)

    this.loginSubmitting = true
    this.setData({ loading: true })

    try {
      const res = await request.post('/auth/login', {
        account: normalizedAccount,
        loginKey: normalizedLoginKey
      })
      this.saveAuthSession(res.data)
      setTimeout(() => {
        wx.switchTab({
          url: '/pages/chat/chat'
        })
      }, 800)
    } catch (error) {
      toastError(error, copy.toast.loginFail, { scene: 'login' })
    } finally {
      this.loginSubmitting = false
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
  },

  syncRegisterSubmitState() {
    const canRegisterSubmit = Boolean(
      normalizeText(this.data.nickname)
      && normalizeText(this.data.password)
      && normalizeText(this.data.confirmPassword)
    )
    if (canRegisterSubmit === this.data.canRegisterSubmit) {
      return
    }
    this.setData({ canRegisterSubmit })
  },

  syncLoginSubmitState() {
    const canLoginSubmit = Boolean(
      normalizeText(this.data.account)
      && normalizeText(this.data.loginKey)
    )
    if (canLoginSubmit === this.data.canLoginSubmit) {
      return
    }
    this.setData({ canLoginSubmit })
  }
})
