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
const INTERACTION_NOTICE_TYPES = new Set(['THREAD_LIKED', 'THREAD_REPLIED', 'REPLY_LIKED'])
const ONE_DAY_MS = 24 * 60 * 60 * 1000

function normalizeUnreadCount(value) {
  const count = Number(value)
  if (!Number.isFinite(count) || count <= 0) {
    return 0
  }
  return Math.floor(count)
}

function buildFilterLabel(baseLabel, count) {
  if (count <= 0) {
    return baseLabel
  }
  return `${baseLabel} ${count}`
}

function buildFilterOptions(summary) {
  const totalUnread = normalizeUnreadCount(summary && summary.totalUnread)
  const interactionUnread = normalizeUnreadCount(summary && summary.interactionUnread)
  const followUnread = normalizeUnreadCount(summary && summary.followUnread)
  const achievementUnread = normalizeUnreadCount(summary && summary.achievementUnread)
  return [
    { key: NOTICE_FILTER_KEY_ALL, label: buildFilterLabel('全部', totalUnread), count: totalUnread },
    { key: NOTICE_FILTER_KEY_INTERACTION, label: buildFilterLabel('互动', interactionUnread), count: interactionUnread },
    { key: NOTICE_FILTER_KEY_FOLLOW, label: buildFilterLabel('关注', followUnread), count: followUnread },
    { key: NOTICE_FILTER_KEY_ACHIEVEMENT, label: buildFilterLabel('成就', achievementUnread), count: achievementUnread }
  ]
}

function pad2(value) {
  return String(value).padStart(2, '0')
}

function formatDateLabel(date) {
  return `${date.getFullYear()}-${pad2(date.getMonth() + 1)}-${pad2(date.getDate())}`
}

function getDayDiff(date, now) {
  const targetStart = new Date(date.getFullYear(), date.getMonth(), date.getDate()).getTime()
  const nowStart = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime()
  return Math.floor((nowStart - targetStart) / ONE_DAY_MS)
}

Page({
  data: {
    list: [],
    filteredList: [],
    groupedList: [],
    loading: false,
    page: DEFAULT_PAGE,
    size: DEFAULT_SIZE,
    hasMore: true,
    activeFilterKey: NOTICE_FILTER_KEY_ALL,
    filterOptions: buildFilterOptions(null),
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
      const groupedList = this.buildGroupedList(filteredList)
      this.setData({
        list: mergedList,
        filteredList,
        groupedList,
        page: current + 1,
        hasMore
      })
      refreshNoticeUnreadBadge()
      this.loadUnreadSummary()
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
        const filteredList = this.buildFilteredList(list, this.data.activeFilterKey)
        this.setData({
          list,
          filteredList,
          groupedList: this.buildGroupedList(filteredList)
        })
        refreshNoticeUnreadBadge()
        this.loadUnreadSummary()
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
      const filteredList = this.buildFilteredList(list, this.data.activeFilterKey)
      this.setData({
        list,
        filteredList,
        groupedList: this.buildGroupedList(filteredList)
      })
      refreshNoticeUnreadBadge()
      this.loadUnreadSummary()
    } catch (error) {
      toastError(error, '操作失败')
    }
  },

  async loadUnreadSummary() {
    try {
      const res = await request.get('/notices/unread-summary')
      this.setData({
        filterOptions: buildFilterOptions(res && res.data)
      })
    } catch (error) {
      this.setData({
        filterOptions: buildFilterOptions(null)
      })
    }
  },

  onTapFilter(e) {
    const filterKey = String(e.currentTarget.dataset.key || '')
    if (!filterKey || filterKey === this.data.activeFilterKey) {
      return
    }
    const filteredList = this.buildFilteredList(this.data.list, filterKey)
    this.setData({
      activeFilterKey: filterKey,
      filteredList,
      groupedList: this.buildGroupedList(filteredList)
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

  buildGroupedList(filteredList) {
    const source = Array.isArray(filteredList) ? filteredList : []
    if (source.length === 0) {
      return []
    }

    const groups = []
    const groupMap = Object.create(null)
    source.forEach(item => {
      const groupMeta = this.resolveGroupMeta(item.createTime)
      if (!groupMap[groupMeta.key]) {
        groupMap[groupMeta.key] = {
          groupKey: groupMeta.key,
          groupTitle: groupMeta.title,
          items: []
        }
        groups.push(groupMap[groupMeta.key])
      }
      groupMap[groupMeta.key].items.push(item)
    })
    return groups
  },

  resolveGroupMeta(createTime) {
    const parsedTime = time.parseDateTime(createTime)
    if (!parsedTime) {
      return {
        key: 'OLDER',
        title: '更早'
      }
    }

    const now = new Date()
    const dayDiff = getDayDiff(parsedTime, now)
    if (dayDiff === 0) {
      return {
        key: 'TODAY',
        title: '今天'
      }
    }
    if (dayDiff === 1) {
      return {
        key: 'YESTERDAY',
        title: '昨天'
      }
    }
    const dateLabel = formatDateLabel(parsedTime)
    return {
      key: `DATE_${dateLabel}`,
      title: dateLabel
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
