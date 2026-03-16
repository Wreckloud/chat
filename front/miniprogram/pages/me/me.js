/**
 * 我的页面
 */
const request = require('../../utils/request')
const auth = require('../../utils/auth')
const { toastError, toastSuccess } = require('../../utils/ui')
const { setThemeName, listThemes } = require('../../utils/theme')
const { applyPageTheme } = require('../../utils/page-theme')
const pageLifecycleHelper = require('../../utils/page-lifecycle-helper')

const QUICK_ACTIONS = [
  {
    key: 'follow',
    title: '关注列表',
    desc: '管理互关关系',
    type: 'nav',
    url: '/pages/follow/follow'
  },
  {
    key: 'my_posts',
    title: '我的帖子',
    desc: '查看我发布的全部主题',
    type: 'action'
  }
]

const ACCOUNT_ACTIONS = [
  {
    key: 'email',
    title: '邮箱管理',
    desc: '绑定与认证邮箱',
    url: '/pages/email/email'
  },
  {
    key: 'password',
    title: '修改密码',
    desc: '更新登录凭证',
    url: '/pages/password/password'
  }
]

const ONBOARDING_STATUS_TEXT = {
  PENDING: '引导进行中',
  COMPLETED: '引导已完成',
  SKIPPED: '引导已跳过'
}

function confirmAction(options) {
  return new Promise(resolve => {
    wx.showModal({
      ...options,
      success(res) {
        resolve(!!res.confirm)
      },
      fail() {
        resolve(false)
      }
    })
  })
}

function resolveEmailStatusText(userInfo) {
  if (!userInfo || !userInfo.email) {
    return '邮箱未绑定'
  }
  return userInfo.emailVerified ? '邮箱已认证' : '邮箱待认证'
}

function resolveOnboardingStatusText(userInfo) {
  const status = userInfo && userInfo.onboardingStatus ? String(userInfo.onboardingStatus) : ''
  return ONBOARDING_STATUS_TEXT[status] || '引导未设置'
}

Page({
  data: {
    userInfo: null,
    emailStatusText: '',
    onboardingStatusText: '',
    loading: false,
    themeName: 'retro_blue',
    themeClass: 'theme-retro-blue',
    themeOptions: [],
    quickActions: QUICK_ACTIONS,
    accountActions: ACCOUNT_ACTIONS
  },

  onLoad() {
    pageLifecycleHelper.handleProtectedPageLoad(auth, {
      afterInit: () => {
        this.setData({
          themeOptions: listThemes()
        })
      }
    })
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
    const localUserInfo = auth.getUserInfo()
    if (localUserInfo) {
      this.applyUserInfo(localUserInfo)
    }

    this.setData({ loading: true })
    try {
      const userRes = await request.get('/users/me')
      auth.setUserInfo(userRes.data)
      this.applyUserInfo(auth.getUserInfo())
    } catch (error) {
      toastError(error, '加载失败')
    } finally {
      this.setData({ loading: false })
    }
  },

  handleLogout() {
    confirmAction({
      title: '退出登录',
      content: '确定要退出当前账号吗？',
      confirmText: '退出'
    }).then(confirmed => {
      if (!confirmed) {
        return
      }
      auth.logout()
      wx.redirectTo({ url: '/pages/login/login' })
    })
  },

  async handleDeactivateAccount() {
    const firstConfirmed = await confirmAction({
      title: '注销账号',
      content: '注销后将立即退出登录，且当前账号不可恢复。确定继续吗？',
      confirmText: '继续'
    })
    if (!firstConfirmed) return

    const secondConfirmed = await confirmAction({
      title: '再次确认',
      content: '该操作用于测试环境，是否立即注销当前账号？',
      confirmColor: '#b4474f',
      confirmText: '立即注销'
    })
    if (!secondConfirmed) return

    this.doDeactivateAccount()
  },

  async doDeactivateAccount() {
    try {
      await request.del('/users/me')
      auth.logout()
      toastSuccess('账号已注销')
      setTimeout(() => {
        wx.reLaunch({ url: '/pages/login/login' })
      }, 300)
    } catch (error) {
      toastError(error, '注销失败')
    }
  },

  onTapQuickAction(e) {
    const key = e.currentTarget.dataset.key
    const action = this.data.quickActions.find(item => item.key === key)
    if (!action) {
      return
    }

    if (action.type === 'action' && action.key === 'my_posts') {
      this.onTapMyPosts()
      return
    }

    if (!action.url) {
      return
    }
    wx.navigateTo({ url: action.url })
  },

  onTapProfileCard() {
    wx.navigateTo({ url: '/pages/profile/profile' })
  },

  onTapAccountAction(e) {
    const key = e.currentTarget.dataset.key
    const action = this.data.accountActions.find(item => item.key === key)
    if (!action || !action.url) {
      return
    }
    wx.navigateTo({ url: action.url })
  },

  onTapMyPosts() {
    const userInfo = this.data.userInfo
    if (!userInfo || !userInfo.userId) {
      return
    }
    const nickname = encodeURIComponent(userInfo.nickname || userInfo.wolfNo || '行者')
    wx.navigateTo({
      url: `/pages/user-posts/user-posts?userId=${userInfo.userId}&nickname=${nickname}`
    })
  },

  onTapUserHome() {
    const userId = this.data.userInfo && this.data.userInfo.userId
    if (!userId) {
      return
    }
    wx.navigateTo({
      url: `/pages/user-detail/user-detail?userId=${userId}`
    })
  },

  onSelectTheme(e) {
    const themeName = e.currentTarget.dataset.theme
    if (!themeName || themeName === this.data.themeName) {
      return
    }
    setThemeName(themeName)
    this.applyTheme(themeName)
  },

  applyTheme(themeName) {
    applyPageTheme(this, {
      themeName,
      tabBar: true,
      extraData: (themeContext) => ({
        themeName: themeContext.themeName
      })
    })
  },

  applyUserInfo(userInfo) {
    if (!userInfo) {
      this.setData({
        userInfo: null,
        emailStatusText: '',
        onboardingStatusText: ''
      })
      return
    }

    this.setData({
      userInfo,
      emailStatusText: resolveEmailStatusText(userInfo),
      onboardingStatusText: resolveOnboardingStatusText(userInfo)
    })
  }
})
