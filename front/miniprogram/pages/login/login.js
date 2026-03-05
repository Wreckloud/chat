/**
 * 登录/注册页面
 */
const request = require('../../utils/request')
const auth = require('../../utils/auth')
const { toastError, toastSuccess } = require('../../utils/ui')
const { applyPageTheme } = require('../../utils/page-theme')
const { LOGIN_PAGE_COPY } = require('../../constants/copy')
const {
  evaluatePasswordStrength,
  getPasswordStrengthInlineText
} = require('../../utils/password')

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

Page({
  data: {
    // 登录模式：'register' 注册，'login' 登录
    mode: 'login',

    // 注册表单
    nickname: '',
    password: '',
    confirmPassword: '',
    email: '',
    passwordStrengthLevel: '',
    passwordStrengthInlineText: '',

    // 登录表单
    account: '',
    loginKey: '',

    // 注册结果
    showResult: false,
    registeredWolfNo: '',
    loading: false,
    themeClass: 'theme-retro-blue',
    copy: LOGIN_PAGE_COPY
  },

  onLoad() {
    // 检查是否已登录
    if (auth.isLoggedIn()) {
      wx.switchTab({
        url: '/pages/chat/chat'
      })
    }
  },

  onShow() {
    this.applyTheme()
  },

  /**
   * 切换到登录模式
   */
  switchToLogin() {
    this.setData({
      mode: 'login',
      account: '',
      loginKey: ''
    })
  },

  /**
   * 切换到注册模式
   */
  switchToRegister() {
    this.setData({
      mode: 'register',
      nickname: '',
      password: '',
      confirmPassword: '',
      email: '',
      passwordStrengthLevel: '',
      passwordStrengthInlineText: '',
      showResult: false,
      registeredWolfNo: ''
    })
  },

  /**
   * 输入行者名
   */
  onNicknameInput(e) {
    this.setData({
      nickname: e.detail.value || ''
    })
  },

  /**
   * 输入密码（注册）
   */
  onPasswordInput(e) {
    const password = e.detail.value || ''
    const strength = evaluatePasswordStrength(password)
    this.setData({
      password: password,
      passwordStrengthLevel: strength.level,
      passwordStrengthInlineText: getPasswordStrengthInlineText(strength.level)
    })
  },

  onConfirmPasswordInput(e) {
    this.setData({
      confirmPassword: e.detail.value || ''
    })
  },

  /**
   * 输入邮箱（注册）
   */
  onEmailInput(e) {
    this.setData({
      email: e.detail.value || ''
    })
  },

  /**
   * 输入账号（登录）
   */
  onAccountInput(e) {
    this.setData({
      account: e.detail.value || ''
    })
  },

  /**
   * 输入密码（登录）
   */
  onLoginKeyInput(e) {
    this.setData({
      loginKey: e.detail.value || ''
    })
  },

  /**
   * 注册
   */
  async handleRegister() {
    if (this.data.loading) return

    const { nickname, password, confirmPassword, email } = this.data
    const copy = LOGIN_PAGE_COPY
    const normalizedNickname = (nickname || '').trim()
    const normalizedPassword = (password || '').trim()
    const normalizedConfirmPassword = (confirmPassword || '').trim()
    const normalizedEmail = (email || '').trim()

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

    if (normalizedEmail && !this.isValidEmail(normalizedEmail)) {
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

      if (res.code === 0 && res.data) {
        // 保存 token 和用户信息
        auth.setToken(res.data.token)
        auth.setUserInfo(res.data.userInfo)

        // 显示注册结果（狼藉号）
        this.setData({
          showResult: true,
          registeredWolfNo: res.data.userInfo.wolfNo
        })

        toastSuccess(copy.toast.registerSuccess)
      }
    } catch (error) {
      toastError(error, copy.toast.registerFail)
    } finally {
      this.setData({ loading: false })
    }
  },

  /**
   * 登录
   */
  async handleLogin() {
    if (this.data.loading) return

    const { account, loginKey } = this.data
    const copy = LOGIN_PAGE_COPY
    const normalizedAccount = (account || '').trim()
    const normalizedLoginKey = (loginKey || '').trim()

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

      if (res.code === 0 && res.data) {
        // 保存 token 和用户信息
        auth.setToken(res.data.token)
        auth.setUserInfo(res.data.userInfo)

        toastSuccess(copy.toast.loginSuccess)

        // 跳转到聊天首页
        setTimeout(() => {
          wx.switchTab({
            url: '/pages/chat/chat'
          })
        }, 1000)
      }
    } catch (error) {
      toastError(error, copy.toast.loginFail)
    } finally {
      this.setData({ loading: false })
    }
  },

  /**
   * 确认注册结果，进入应用
   */
  handleConfirmRegister() {
    wx.switchTab({
      url: '/pages/chat/chat'
    })
  },

  applyTheme() {
    applyPageTheme(this)
  },

  isValidEmail(email) {
    return EMAIL_REGEX.test(email)
  }
})
