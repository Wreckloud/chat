/**
 * 社区首页（版块 + 主题）
 */
const request = require('../../utils/request')
const auth = require('../../utils/auth')
const { normalizeUser } = require('../../utils/user')
const { toastError } = require('../../utils/ui')
const time = require('../../utils/time')
const { applyPageTheme } = require('../../utils/page-theme')

const THREAD_TABS = [
  { value: 'all', label: '全部' },
  { value: 'sticky', label: '置顶' },
  { value: 'essence', label: '精华' }
]

Page({
  data: {
    boards: [],
    threads: [],
    tabs: THREAD_TABS,
    activeBoardId: null,
    activeTab: 'all',
    page: 1,
    size: 20,
    total: 0,
    hasMore: true,
    loading: false,
    themeClass: 'theme-retro-blue'
  },

  onLoad() {
    auth.requireLogin()
  },

  onShow() {
    if (!auth.requireLogin()) return
    this.applyTheme()
    this.loadBoardsAndThreads()
  },

  async loadBoardsAndThreads() {
    await this.loadBoards()
    if (this.data.activeBoardId) {
      await this.loadThreads(true)
    }
  },

  async loadBoards() {
    try {
      const res = await request.get('/forum/boards')
      const boards = res.data || []
      let activeBoardId = this.data.activeBoardId
      if (!activeBoardId && boards.length > 0) {
        activeBoardId = boards[0].boardId
      } else if (activeBoardId && !boards.some(item => item.boardId === activeBoardId)) {
        activeBoardId = boards.length > 0 ? boards[0].boardId : null
      }
      this.setData({ boards, activeBoardId })
    } catch (error) {
      toastError(error, '加载版块失败')
    }
  },

  async loadThreads(reset = false) {
    if (!this.data.activeBoardId || this.data.loading) return
    if (!reset && !this.data.hasMore) return

    const page = reset ? 1 : this.data.page
    this.setData({ loading: true })
    try {
      const res = await request.get(`/forum/boards/${this.data.activeBoardId}/threads`, {
        page,
        size: this.data.size,
        tab: this.data.activeTab
      })
      const payload = res.data || {}
      const list = (payload.list || []).map(item => ({
        ...item,
        author: normalizeUser(item.author) || {},
        lastReplyUser: normalizeUser(item.lastReplyUser) || {},
        createTimeText: time.formatPostTime(item.createTime),
        lastReplyTimeText: time.formatPostTime(item.lastReplyTime)
      }))
      const mergedThreads = reset ? list : [...this.data.threads, ...list]
      const total = Number(payload.total) || 0
      const hasMore = mergedThreads.length < total
      this.setData({
        threads: mergedThreads,
        total,
        hasMore,
        page: hasMore ? page + 1 : page
      })
    } catch (error) {
      toastError(error, '加载主题失败')
    } finally {
      this.setData({ loading: false })
    }
  },

  selectBoard(e) {
    const boardId = Number(e.currentTarget.dataset.id)
    if (!boardId || boardId === this.data.activeBoardId) {
      return
    }
    this.setData({
      activeBoardId: boardId,
      page: 1,
      total: 0,
      hasMore: true,
      threads: []
    })
    this.loadThreads(true)
  },

  selectTab(e) {
    const tab = e.currentTarget.dataset.tab
    if (!tab || tab === this.data.activeTab) {
      return
    }
    this.setData({
      activeTab: tab,
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

  onReachBottom() {
    this.loadMoreThreads()
  },

  goCreatePost() {
    const boardId = this.data.activeBoardId
    const url = boardId
      ? `/pages/post-create/post-create?boardId=${boardId}`
      : '/pages/post-create/post-create'
    wx.navigateTo({ url })
  },

  goPostDetail(e) {
    const threadId = Number(e.currentTarget.dataset.id)
    if (!threadId) return
    wx.navigateTo({
      url: `/pages/post-detail/post-detail?threadId=${threadId}`
    })
  },

  applyTheme() {
    applyPageTheme(this, { tabBar: true })
  }
})
