const request = require('../../utils/request')
const auth = require('../../utils/auth')
const time = require('../../utils/time')
const { toastError } = require('../../utils/ui')
const { applyPageTheme } = require('../../utils/page-theme')
const pageLifecycleHelper = require('../../utils/page-lifecycle-helper')
const { refreshNoticeUnreadBadge } = require('../../utils/tab-badge')

const DEFAULT_PAGE = 1
const DEFAULT_SIZE = 20
const NOTICE_FILTER_KEY_ALL = 'ALL'
const NOTICE_FILTER_KEY_INTERACTION = 'INTERACTION'
const NOTICE_FILTER_KEY_FOLLOW = 'FOLLOW'
const NOTICE_FILTER_KEY_ACHIEVEMENT = 'ACHIEVEMENT'
const NOTICE_FILTER_OPTIONS = [
  { key: NOTICE_FILTER_KEY_ALL, label: '全部' },
  { key: NOTICE_FILTER_KEY_INTERACTION, label: '互动' },
  { key: NOTICE_FILTER_KEY_FOLLOW, label: '关注' },
  { key: NOTICE_FILTER_KEY_ACHIEVEMENT, label: '成就' }
]
const INTERACTION_NOTICE_TYPES = new Set(['THREAD_LIKED', 'THREAD_REPLIED', 'REPLY_LIKED'])

Page({
  data: {
    list: [],
    filteredList: [],
    loading: false,
    page: DEFAULT_PAGE,
    size: DEFAULT_SIZE,
    hasMore: true,
    activeFilterKey: NOTICE_FILTER_KEY_ALL,
    filterOptions: NOTICE_FILTER_OPTIONS,
    themeClass: 'theme-retro-blue'
  },

  onLoad() {
    pageLifecycleHelper.handleProtectedPageLoad(auth)
  },

  onShow() {
    pageLifecycleHelper.handleProtectedPageShow(auth, {
      afterShow: () => {
        this.applyTheme()
        this.loadNotices(true)
      }
    })
  },

  async loadNotices(reset) {
    if (this.data.loading) {
      return
    }

    const nextPage = reset ? DEFAULT_PAGE : this.data.page
    this.setData({ loading: true })

    try {
      const res = await request.get('/notices', {
        page: nextPage,
        size: this.data.size
      })
      const pageData = res.data || {}
      const fetchedList = (pageData.list || []).map(item => this.normalizeNotice(item))
      const mergedList = reset ? fetchedList : this.data.list.concat(fetchedList)
      const total = Number(pageData.total) || mergedList.length
      const current = Number(pageData.page) || nextPage
      const hasMore = mergedList.length < total
      const filteredList = this.buildFilteredList(mergedList, this.data.activeFilterKey)
      this.setData({
        list: mergedList,
        filteredList,
        page: current + 1,
        hasMore
      })
      refreshNoticeUnreadBadge()
    } catch (error) {
      toastError(error, '加载失败')
    } finally {
      this.setData({ loading: false })
    }
  },

  normalizeNotice(item) {
    return {
      noticeId: item.noticeId,
      noticeType: item.noticeType || '',
      content: item.content || '',
      typeText: item.typeLabel || '通知',
      actionUrl: item.actionUrl || '',
      navigable: item.navigable === true,
      read: item.read === true,
      readTime: item.readTime || '',
      createTime: item.createTime || '',
      timeText: time.formatPostTime(item.createTime)
    }
  },

  async onTapNotice(e) {
    const noticeId = Number(e.currentTarget.dataset.id)
    if (!noticeId) {
      return
    }
    const notice = this.data.list.find(item => Number(item.noticeId) === noticeId)
    if (!notice) {
      return
    }

    if (!notice.read) {
      try {
        await request.put(`/notices/${noticeId}/read`)
        const list = this.data.list.map(item => {
          if (Number(item.noticeId) !== noticeId) {
            return item
          }
          return {
            ...item,
            read: true
          }
        })
        this.setData({
          list,
          filteredList: this.buildFilteredList(list, this.data.activeFilterKey)
        })
        refreshNoticeUnreadBadge()
      } catch (error) {
        toastError(error, '操作失败')
        return
      }
    }

    this.navigateByNotice(notice)
  },

  async onMarkAllRead() {
    const hasUnread = this.data.list.some(item => item.read !== true)
    if (!hasUnread) {
      return
    }

    try {
      await request.put('/notices/read-all')
      const list = this.data.list.map(item => ({
        ...item,
        read: true
      }))
      this.setData({
        list,
        filteredList: this.buildFilteredList(list, this.data.activeFilterKey)
      })
      refreshNoticeUnreadBadge()
    } catch (error) {
      toastError(error, '操作失败')
    }
  },

  onTapFilter(e) {
    const filterKey = String(e.currentTarget.dataset.key || '')
    if (!filterKey || filterKey === this.data.activeFilterKey) {
      return
    }
    this.setData({
      activeFilterKey: filterKey,
      filteredList: this.buildFilteredList(this.data.list, filterKey)
    })
  },

  buildFilteredList(sourceList, filterKey) {
    const list = Array.isArray(sourceList) ? sourceList : []
    if (filterKey === NOTICE_FILTER_KEY_ALL) {
      return list
    }
    return list.filter(item => this.matchFilter(item, filterKey))
  },

  matchFilter(item, filterKey) {
    if (!item || !item.noticeType) {
      return false
    }
    if (filterKey === NOTICE_FILTER_KEY_ACHIEVEMENT) {
      return item.noticeType === 'ACHIEVEMENT_UNLOCK'
    }
    if (filterKey === NOTICE_FILTER_KEY_FOLLOW) {
      return item.noticeType === 'FOLLOW_RECEIVED'
    }
    if (filterKey === NOTICE_FILTER_KEY_INTERACTION) {
      return INTERACTION_NOTICE_TYPES.has(item.noticeType)
    }
    return true
  },

  navigateByNotice(notice) {
    if (!notice || notice.navigable !== true) {
      return
    }
    const actionUrl = String(notice.actionUrl || '').trim()
    if (!actionUrl) {
      return
    }
    wx.navigateTo({ url: actionUrl })
  },

  onReachBottom() {
    if (!this.data.hasMore || this.data.loading) {
      return
    }
    this.loadNotices(false)
  },

  onPullDownRefresh() {
    this.loadNotices(true).finally(() => {
      wx.stopPullDownRefresh()
    })
  },

  applyTheme() {
    applyPageTheme(this)
  }
})
