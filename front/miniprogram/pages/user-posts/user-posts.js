/**
 * 行者帖子列表页（我的/草稿/垃圾站）
 */
const auth = require('../../utils/auth')
const request = require('../../utils/request')
const { toastError, confirmAction } = require('../../utils/ui')
const { applyPageTheme } = require('../../utils/page-theme')
const pageLifecycleHelper = require('../../utils/page-lifecycle-helper')
const forumViewHelper = require('../../utils/forum-view-helper')
const time = require('../../utils/time')
const { normalizeUser } = require('../../utils/user')

const SELF_TABS = [
  { value: 'mine', label: '我的' },
  { value: 'draft', label: '草稿' },
  { value: 'trash', label: '垃圾站' }
]

Page({
  data: {
    userId: 0,
    currentUserId: 0,
    isSelf: false,
    nickname: '行者',

    tabs: SELF_TABS,
    activeTab: 'mine',
    searchKeyword: '',
    stickyLoadingMap: {},
    deletingMap: {},

    threads: [],
    page: 1,
    size: 20,
    hasMore: true,
    total: 0,
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
        const isSelf = currentUserId > 0 && currentUserId === userId
        this.setData({
          userId,
          currentUserId,
          isSelf,
          nickname,
          activeTab: isSelf ? 'mine' : 'mine'
        })
        wx.setNavigationBarTitle({
          title: isSelf ? '我的帖子' : `${nickname} 的帖子`
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
      const payload = this.data.isSelf
        ? await this.loadSelfThreads(page)
        : await this.loadPublicThreads(page)

      const list = forumViewHelper.mapThreadList(payload.list || [], normalizeUser, time)
      const merged = forumViewHelper.mergePagedList(this.data.threads, list, reset)
      const total = Number(payload.total) || 0
      const hasMore = forumViewHelper.resolveHasMoreByTotal(merged.length, total)
      this.setData({
        threads: merged,
        total,
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

  async loadSelfThreads(page) {
    const keyword = String(this.data.searchKeyword || '').trim()
    const params = {
      tab: this.data.activeTab,
      page,
      size: this.data.size
    }
    if (keyword) {
      params.keyword = keyword
    }
    const res = await request.get('/users/me/threads', params)
    return res.data || {}
  },

  async loadPublicThreads(page) {
    const res = await request.get(`/users/${this.data.userId}/threads`, {
      page,
      size: this.data.size
    })
    return res.data || {}
  },

  onSelectTab(e) {
    if (!this.data.isSelf) {
      return
    }
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
    this.loadThreads({ reset: true })
  },

  onSearchInput(e) {
    if (!this.data.isSelf) {
      return
    }
    this.setData({
      searchKeyword: String(e.detail.value || '')
    })
  },

  onSearchConfirm() {
    if (!this.data.isSelf) {
      return
    }
    this.loadThreads({ reset: true })
  },

  onSearchTap() {
    if (!this.data.isSelf) {
      return
    }
    this.loadThreads({ reset: true })
  },

  onTapThread(e) {
    const threadId = Number(e.currentTarget.dataset.threadId)
    if (!threadId) {
      return
    }
    if (this.data.isSelf && this.data.activeTab === 'draft') {
      wx.navigateTo({
        url: `/pages/post-create/post-create?mode=draft&threadId=${threadId}`
      })
      return
    }
    if (this.data.isSelf && this.data.activeTab === 'trash') {
      return
    }
    wx.navigateTo({
      url: `/pages/post-detail/post-detail?threadId=${threadId}`
    })
  },

  async onToggleSticky(e) {
    if (!this.data.isSelf || this.data.activeTab !== 'mine') {
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

    this.setData({ [`stickyLoadingMap.${threadId}`]: true })
    try {
      await request.put(`/forum/threads/${threadId}/sticky`, {
        sticky: !sticky
      })
      await this.loadThreads({ reset: true })
    } catch (error) {
      toastError(error, sticky ? '取消置顶失败' : '置顶到主页失败')
    } finally {
      this.setData({ [`stickyLoadingMap.${threadId}`]: false })
    }
  },

  onEditThread(e) {
    if (!this.data.isSelf || this.data.activeTab !== 'mine') {
      return
    }
    const threadId = Number(e.currentTarget.dataset.threadId)
    if (!threadId) {
      return
    }
    wx.navigateTo({
      url: `/pages/post-create/post-create?mode=edit&threadId=${threadId}`
    })
  },

  onRestoreThread(e) {
    if (!this.data.isSelf || this.data.activeTab !== 'trash') {
      return
    }
    const threadId = Number(e.currentTarget.dataset.threadId)
    if (!threadId) {
      return
    }
    wx.navigateTo({
      url: `/pages/post-create/post-create?mode=restore&threadId=${threadId}`
    })
  },

  async onDeleteThread(e) {
    if (!this.data.isSelf || this.data.activeTab !== 'mine') {
      return
    }
    const threadId = Number(e.currentTarget.dataset.threadId)
    if (!threadId || this.data.deletingMap[threadId]) {
      return
    }
    const confirmed = await confirmAction({
      title: '删除主题',
      content: '删除后将进入垃圾站，确认删除吗？',
      confirmText: '删除',
      confirmColor: '#b4474f'
    })
    if (!confirmed) {
      return
    }

    this.setData({ [`deletingMap.${threadId}`]: true })
    try {
      await request.del(`/forum/threads/${threadId}`)
      await this.loadThreads({ reset: true })
    } catch (error) {
      toastError(error, '删除主题失败')
    } finally {
      this.setData({ [`deletingMap.${threadId}`]: false })
    }
  },

  async onPurgeThread(e) {
    if (!this.data.isSelf || this.data.activeTab !== 'trash') {
      return
    }
    const threadId = Number(e.currentTarget.dataset.threadId)
    if (!threadId || this.data.deletingMap[threadId]) {
      return
    }
    const confirmed = await confirmAction({
      title: '彻底删除',
      content: '彻底删除后该主题不会再出现在你的列表中，确认继续吗？',
      confirmText: '彻底删除',
      confirmColor: '#b4474f'
    })
    if (!confirmed) {
      return
    }
    this.setData({ [`deletingMap.${threadId}`]: true })
    try {
      await request.del(`/forum/threads/${threadId}/purge`)
      await this.loadThreads({ reset: true })
    } catch (error) {
      toastError(error, '彻底删除失败')
    } finally {
      this.setData({ [`deletingMap.${threadId}`]: false })
    }
  },

  onReachBottom() {
    this.loadThreads({ reset: false })
  },

  applyTheme() {
    applyPageTheme(this)
  }
})
