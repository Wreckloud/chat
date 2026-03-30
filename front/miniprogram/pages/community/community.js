/**
 * 社区首页（推荐/热议/好友/最新）
 */
const request = require('../../utils/request')
const auth = require('../../utils/auth')
const { normalizeUser } = require('../../utils/user')
const { toastError } = require('../../utils/ui')
const time = require('../../utils/time')
const { applyPageTheme } = require('../../utils/page-theme')
const forumViewHelper = require('../../utils/forum-view-helper')
const pullRefreshHelper = require('../../utils/pull-refresh-helper')

const FEED_TABS = [
  { value: 'recommend', label: '推荐' },
  { value: 'hot', label: '热议' },
  { value: 'friends', label: '好友' },
  { value: 'latest', label: '最新' }
]

function ensureLoginForAction(authInstance, actionText) {
  return new Promise(resolve => {
    if (authInstance && typeof authInstance.isLoggedIn === 'function' && authInstance.isLoggedIn()) {
      resolve(true)
      return
    }
    wx.showModal({
      title: '请先登录',
      content: actionText ? `${actionText}需要登录，是否前往登录？` : '该操作需要登录，是否前往登录？',
      confirmText: '去登录',
      cancelText: '继续浏览',
      success(res) {
        if (!res.confirm) {
          resolve(false)
          return
        }
        if (authInstance && typeof authInstance.requireLogin === 'function') {
          resolve(authInstance.requireLogin())
          return
        }
        resolve(false)
      },
      fail() {
        resolve(false)
      }
    })
  })
}

Page({
  data: {
    threads: [],
    tabs: FEED_TABS,
    activeTab: 'recommend',
    searchKeyword: '',
    noticeUnreadCount: 0,
    page: 1,
    size: 20,
    total: 0,
    hasMore: true,
    loading: false,
    themeClass: 'theme-retro-blue'
  },

  onShow() {
    this.applyTheme()
    this.loadThreads(true)
    this.loadNoticeUnreadCount()
  },

  async loadThreads(reset = false) {
    if (this.data.loading) {
      return
    }
    if (!reset && !this.data.hasMore) {
      return
    }

    const nextPage = reset ? 1 : this.data.page
    this.setData({ loading: true })
    try {
      const keyword = String(this.data.searchKeyword || '').trim()
      const searching = !!keyword
      const endpoint = searching ? '/forum/search' : '/forum/feed'
      const params = searching
        ? { keyword, page: nextPage, size: this.data.size }
        : { tab: this.data.activeTab, page: nextPage, size: this.data.size }
      const res = await request.get(endpoint, params)
      const payload = res.data || {}
      const list = forumViewHelper.mapThreadList(payload.list || [], normalizeUser, time)
      const mergedThreads = forumViewHelper.mergePagedList(this.data.threads, list, reset)
      const total = Number(payload.total) || 0
      const hasMore = forumViewHelper.resolveHasMoreByTotal(mergedThreads.length, total)

      this.setData({
        threads: mergedThreads,
        total,
        hasMore,
        page: hasMore ? nextPage + 1 : nextPage
      })
    } catch (error) {
      toastError(error, '加载帖子失败')
    } finally {
      this.setData({ loading: false })
    }
  },

  selectTab(e) {
    const tab = String(e.currentTarget.dataset.tab || '')
    if (!tab || tab === this.data.activeTab) {
      return
    }
    this.setData({
      activeTab: tab,
      searchKeyword: '',
      page: 1,
      total: 0,
      hasMore: true,
      threads: []
    })
    this.loadThreads(true)
  },

  loadMoreThreads() {
    this.loadThreads(false)
  },

  onSearchInput(e) {
    this.setData({
      searchKeyword: String(e.detail.value || '')
    })
  },

  onSearchConfirm() {
    this.loadThreads(true)
  },

  onSearchTap() {
    this.loadThreads(true)
  },

  onReachBottom() {
    this.loadMoreThreads()
  },

  onPullDownRefresh() {
    pullRefreshHelper.runPullDownRefresh(this, async () => {
      await this.loadThreads(true)
      await this.loadNoticeUnreadCount()
    })
  },

  async goCreatePost() {
    const allowed = await ensureLoginForAction(auth, '发帖')
    if (!allowed) {
      return
    }
    wx.navigateTo({
      url: '/pages/post-create/post-create'
    })
  },

  async goNotice() {
    const allowed = await ensureLoginForAction(auth, '查看系统通知')
    if (!allowed) {
      return
    }
    wx.navigateTo({
      url: '/pages/notice/notice'
    })
  },

  async loadNoticeUnreadCount() {
    if (!auth.isLoggedIn()) {
      this.setData({
        noticeUnreadCount: 0
      })
      return
    }
    try {
      const res = await request.get('/notices/unread-count')
      const unreadCount = Number(res && res.data) || 0
      this.setData({
        noticeUnreadCount: unreadCount > 0 ? unreadCount : 0
      })
    } catch (error) {
      this.setData({
        noticeUnreadCount: 0
      })
    }
  },

  goPostDetail(e) {
    const threadId = Number(e.currentTarget.dataset.id)
    if (!threadId) {
      return
    }
    wx.navigateTo({
      url: `/pages/post-detail/post-detail?threadId=${threadId}`
    })
  },

  applyTheme() {
    applyPageTheme(this, { tabBar: true })
  }
})
