/**
 * 行者帖子列表页
 */
const auth = require('../../utils/auth')
const request = require('../../utils/request')
const { toastError } = require('../../utils/ui')
const { applyPageTheme } = require('../../utils/page-theme')
const pageLifecycleHelper = require('../../utils/page-lifecycle-helper')
const forumViewHelper = require('../../utils/forum-view-helper')
const time = require('../../utils/time')
const { normalizeUser } = require('../../utils/user')

Page({
  data: {
    userId: 0,
    currentUserId: 0,
    isSelf: false,
    nickname: '行者',
    threads: [],
    stickyLoadingMap: {},
    page: 1,
    size: 20,
    hasMore: true,
    loading: false,
    loadingMore: false,
    themeClass: 'theme-retro-blue'
  },

  onLoad(options) {
    pageLifecycleHelper.handleProtectedPageLoad(auth, {
      beforeInit: () => {
        const userId = Number(options && options.userId)
        if (!userId) {
          toastError('参数错误')
          return false
        }
        const nickname = options && options.nickname ? decodeURIComponent(options.nickname) : '行者'
        const userInfo = auth.getUserInfo()
        const currentUserId = Number(userInfo && userInfo.userId) || 0
        this.setData({
          userId,
          currentUserId,
          isSelf: currentUserId > 0 && currentUserId === userId,
          nickname
        })
        wx.setNavigationBarTitle({
          title: `${nickname} 的帖子`
        })
        return true
      },
      afterInit: () => {
        this.loadThreads({ reset: true })
      }
    })
  },

  onShow() {
    pageLifecycleHelper.handleProtectedPageShow(auth, {
      afterShow: () => {
        this.applyTheme()
      }
    })
  },

  async loadThreads(options = {}) {
    const reset = options.reset === true
    if (reset) {
      this.setData({
        loading: true,
        page: 1,
        hasMore: true
      })
    } else {
      if (this.data.loadingMore || !this.data.hasMore) return
      this.setData({ loadingMore: true })
    }

    const page = reset ? 1 : this.data.page
    try {
      const res = await request.get(`/users/${this.data.userId}/threads`, {
        page,
        size: this.data.size
      })
      const list = forumViewHelper.mapThreadList(res.data.list || [], normalizeUser, time)
      const merged = forumViewHelper.mergePagedList(this.data.threads, list, reset)
      const total = Number(res.data.total) || 0
      const hasMore = forumViewHelper.resolveHasMoreByTotal(merged.length, total)
      this.setData({
        threads: merged,
        hasMore,
        page: hasMore ? page + 1 : page
      })
    } catch (error) {
      toastError(error, '加载帖子列表失败')
    } finally {
      this.setData({
        loading: false,
        loadingMore: false
      })
    }
  },

  onTapThread(e) {
    const threadId = Number(e.currentTarget.dataset.threadId)
    if (!threadId) return
    wx.navigateTo({
      url: `/pages/post-detail/post-detail?threadId=${threadId}`
    })
  },

  async onToggleSticky(e) {
    if (!this.data.isSelf) {
      return
    }
    const threadId = Number(e.currentTarget.dataset.threadId)
    if (!threadId) {
      return
    }
    const sticky = String(e.currentTarget.dataset.sticky || '') === 'true'
    if (this.data.stickyLoadingMap[threadId]) {
      return
    }

    this.setData({
      [`stickyLoadingMap.${threadId}`]: true
    })
    try {
      await request.put(`/forum/threads/${threadId}/sticky`, {
        sticky: !sticky
      })
      await this.loadThreads({ reset: true })
    } catch (error) {
      toastError(error, sticky ? '取消置顶失败' : '置顶到主页失败')
    } finally {
      this.setData({
        [`stickyLoadingMap.${threadId}`]: false
      })
    }
  },

  onReachBottom() {
    this.loadThreads({ reset: false })
  },

  applyTheme() {
    applyPageTheme(this)
  }
})
