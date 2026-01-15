/**
 * 登录/注册页面
 */
const request = require('../../utils/request')
const auth = require('../../utils/auth')

Page({
  data: {
    // 登录模式：'register' 注册，'login' 登录
    mode: 'login',
    // 注册表单
    nickname: '',
    password: '',
    // 登录表单
    wolfNo: '',
    loginKey: '',
    // 注册结果
    showResult: false,
    registeredWolfNo: '',
    loading: false
  },

  onLoad() {
    // 检查是否已登录
    if (auth.isLoggedIn()) {
      wx.redirectTo({
        url: '/pages/me/me'
      })
    }
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
    this.setData({
      password: e.detail.value || ''
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
      wx.showToast({
        title: '请输入行者名',
        icon: 'none'
      })
      return
    }

    if (!password || password.length < 6) {
      wx.showToast({
        title: '密码至少6位',
        icon: 'none'
      })
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

        wx.showToast({
          title: '注册成功',
          icon: 'success'
        })
      }
    } catch (error) {
      wx.showToast({
        title: error.message || '注册失败',
        icon: 'none'
      })
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
      wx.showToast({
        title: '请输入狼藉号',
        icon: 'none'
      })
      return
    }

    if (!loginKey) {
      wx.showToast({
        title: '请输入密码',
        icon: 'none'
      })
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

        wx.showToast({
          title: '登录成功',
          icon: 'success'
        })

        // 跳转到我的页面
        setTimeout(() => {
          wx.redirectTo({
            url: '/pages/me/me'
          })
        }, 1000)
      }
    } catch (error) {
      wx.showToast({
        title: error.message || '登录失败',
        icon: 'none'
      })
    } finally {
      this.setData({ loading: false })
    }
  },

  /**
   * 确认注册结果，进入应用
   */
  handleConfirmRegister() {
    wx.redirectTo({
      url: '/pages/me/me'
    })
  }
})

