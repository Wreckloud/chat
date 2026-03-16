const request = require('../../utils/request')
const auth = require('../../utils/auth')
const time = require('../../utils/time')
const { toastError } = require('../../utils/ui')
const { applyPageTheme } = require('../../utils/page-theme')
const pageLifecycleHelper = require('../../utils/page-lifecycle-helper')
const { refreshNoticeUnreadBadge } = require('../../utils/tab-badge')

const DEFAULT_PAGE = 1
const DEFAULT_SIZE = 20

Page({
  data: {
    list: [],
    loading: false,
    page: DEFAULT_PAGE,
    size: DEFAULT_SIZE,
    hasMore: true,
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
      this.setData({
        list: mergedList,
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
        this.setData({ list })
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
      this.setData({ list })
      refreshNoticeUnreadBadge()
    } catch (error) {
      toastError(error, '操作失败')
    }
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
