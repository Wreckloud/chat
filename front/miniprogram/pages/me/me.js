/**
 * 我的页面
 */
const request = require('../../utils/request')
const auth = require('../../utils/auth')
const { toastError } = require('../../utils/ui')
const {
  setThemeName,
  setDarkModeEnabled,
  listThemes
} = require('../../utils/theme')
const { applyPageTheme } = require('../../utils/page-theme')
const pageLifecycleHelper = require('../../utils/page-lifecycle-helper')
const { setNoticeBadge } = require('../../utils/tab-badge')

const QUICK_ACTIONS = [
  {
    key: 'follow',
    title: '关注列表',
    desc: '管理互关关系',
    type: 'nav',
    url: '/pages/follow/follow'
  },
  {
    key: 'achievement',
    title: '成就头衔',
    desc: '查看成就并管理佩戴',
    type: 'nav',
    url: '/pages/achievement/achievement'
  },
  {
    key: 'notice',
    title: '系统通知',
    desc: '查看系统提醒',
    type: 'nav',
    url: '/pages/notice/notice'
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

Page({
  data: {
    userInfo: null,
    loading: false,
    themeName: 'retro_blue',
    themeClass: 'theme-retro-blue',
    darkModeEnabled: false,
    noticeUnreadCount: 0,
    themeOptions: [],
    quickActions: QUICK_ACTIONS,
    accountActions: ACCOUNT_ACTIONS
  },

  onLoad() {
    pageLifecycleHelper.handleProtectedPageLoad(auth, {
      afterInit: () => {
        this.setData({
          themeOptions: listThemes({
            darkModeEnabled: this.data.darkModeEnabled
          })
        })
      }
    })
  },

  onShow() {
    pageLifecycleHelper.handleProtectedPageShow(auth, {
      afterShow: () => {
        this.applyTheme()
        this.loadUserInfo()
        this.loadNoticeUnreadCount()
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
      toastError(error, '加载个人信息失败')
    } finally {
      this.setData({ loading: false })
    }
  },

  async loadNoticeUnreadCount() {
    try {
      const res = await request.get('/notices/unread-count')
      const unreadCount = Number(res && res.data) || 0
      const normalizedUnreadCount = unreadCount > 0 ? unreadCount : 0
      this.setData({ noticeUnreadCount: normalizedUnreadCount })
      setNoticeBadge(normalizedUnreadCount)
    } catch (error) {
      this.setData({ noticeUnreadCount: 0 })
      setNoticeBadge(0)
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
      wx.reLaunch({ url: '/pages/login/login' })
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

  onToggleDarkMode() {
    const enabled = !this.data.darkModeEnabled
    setDarkModeEnabled(enabled)
    this.applyTheme(this.data.themeName)
  },

  applyTheme(themeName) {
    applyPageTheme(this, {
      themeName,
      tabBar: true,
      extraData: (themeContext) => ({
        themeName: themeContext.themeName,
        darkModeEnabled: themeContext.darkModeEnabled,
        themeOptions: listThemes({
          darkModeEnabled: themeContext.darkModeEnabled
        })
      })
    })
  },

  applyUserInfo(userInfo) {
    if (!userInfo) {
      this.setData({
        userInfo: null
      })
      return
    }

    this.setData({
      userInfo
    })
  }
})
