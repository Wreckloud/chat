/**
 * 登录/注册页面
 */
const request = require('../../utils/request')
const auth = require('../../utils/auth')
const { toastError, toastSuccess } = require('../../utils/ui')
const { applyPageTheme } = require('../../utils/page-theme')

Page({
  data: {
    // 登录模式：'register' 注册，'login' 登录
    mode: 'login',
    // 注册表单
    nickname: '',
    password: '',
    passwordStrengthLevel: '',
    passwordStrengthText: '',
    // 登录表单
    wolfNo: '',
    loginKey: '',
    // 注册结果
    showResult: false,
    registeredWolfNo: '',
    loading: false,
    themeClass: 'theme-retro-blue'
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
      wolfNo: '',
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
      passwordStrengthLevel: '',
      passwordStrengthText: '',
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
    const strength = this.evaluatePasswordStrength(password)
    this.setData({
      password: password,
      passwordStrengthLevel: strength.level,
      passwordStrengthText: strength.text
    })
  },

  /**
   * 输入狼藉号（登录）
   */
  onWolfNoInput(e) {
    this.setData({
      wolfNo: e.detail.value || ''
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

    const { nickname, password } = this.data

    if (!nickname || !nickname.trim()) {
      toastError('请输入行者名')
      return
    }

    if (!password || password.length < 6) {
      toastError('密码至少6位')
      return
    }

    this.setData({ loading: true })

    try {
      const res = await request.post('/auth/register', {
        nickname: nickname.trim(),
        password: password.trim()
      })
      
      if (res.code === 0 && res.data) {
        // 保存 token 和用户信息
        auth.setToken(res.data.token)
        auth.setUserInfo(res.data.userInfo)

        // 显示注册结果（狼藉号）
        this.setData({
          showResult: true,
          registeredWolfNo: res.data.userInfo.wolfNo
        })

        toastSuccess('注册成功')
      }
    } catch (error) {
      toastError(error, '注册失败')
    } finally {
      this.setData({ loading: false })
    }
  },

  /**
   * 登录
   */
  async handleLogin() {
    if (this.data.loading) return

    const { wolfNo, loginKey } = this.data

    if (!wolfNo) {
      toastError('请输入狼藉号')
      return
    }

    if (!loginKey) {
      toastError('请输入密码')
      return
    }

    this.setData({ loading: true })

    try {
      const res = await request.post('/auth/login', {
        wolfNo: wolfNo.trim(),
        loginKey: loginKey.trim()
      })

      if (res.code === 0 && res.data) {
        // 保存 token 和用户信息
        auth.setToken(res.data.token)
        auth.setUserInfo(res.data.userInfo)

        toastSuccess('登录成功')

        // 跳转到聊天首页
        setTimeout(() => {
          wx.switchTab({
            url: '/pages/chat/chat'
          })
        }, 1000)
      }
    } catch (error) {
      toastError(error, '登录失败')
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

  /**
   * 应用当前主题到页面
   */
  applyTheme() {
    applyPageTheme(this)
  },

  /**
   * 评估密码强度（仅提示，不阻止提交）
   * 规则：长度<6或字符类型过少为弱；2类为中；3类及以上为强
   */
  evaluatePasswordStrength(password) {
    const source = password || ''
    if (!source) {
      return {
        level: '',
        text: ''
      }
    }

    let score = 0
    if (/[0-9]/.test(source)) score += 1
    if (/[a-z]/.test(source)) score += 1
    if (/[A-Z]/.test(source)) score += 1
    if (/[^a-zA-Z0-9]/.test(source)) score += 1

    if (source.length < 6 || score <= 1) {
      return {
        level: 'weak',
        text: '强度偏弱，建议至少 8 位并混合字母和数字'
      }
    }

    if (score <= 2) {
      return {
        level: 'medium',
        text: '强度中等，可加入大写字母或符号提高安全性'
      }
    }

    return {
      level: 'strong',
      text: '强度较高，当前密码可用'
    }
  }
})

