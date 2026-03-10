/**
 * 聊天首页
 */
const auth = require('../../utils/auth')
const request = require('../../utils/request')
const time = require('../../utils/time')
const ws = require('../../utils/ws')
const { DEFAULT_AVATAR } = require('../../utils/user')
const { toastError, toastSuccess } = require('../../utils/ui')
const { getSwipeActionStyles } = require('../../utils/theme')
const { applyPageTheme } = require('../../utils/page-theme')

Page({
  data: {
    conversationList: [],
    conversationSections: [],
    loading: false,
    themeClass: 'theme-retro-blue'
  },

  onLoad() {
    if (!auth.requireLogin()) {
      return
    }
    const userInfo = auth.getUserInfo()
    this.currentUserId = userInfo ? Number(userInfo.userId) : 0
  },

  onShow() {
    if (!auth.requireLogin()) return
    this.applyTheme()
    this.syncPinnedConversationIds()
    this.initSocket()
    this.loadConversations()
  },

  onHide() {
    this.teardownSocket()
  },

  onUnload() {
    this.teardownSocket()
  },

  /**
   * 加载会话列表
   */
  loadConversations() {
    if (this.data.loading) return Promise.resolve()

    this.setData({ loading: true })

    return request.get('/conversations')
      .then(res => {
        const list = (res.data || []).map(item => this.formatConversationItem(item))
        this.setData({
          ...this.buildConversationViewData(list),
          loading: false
        })
      })
      .catch(err => {
        toastError(err, '加载失败')
        this.setData({ loading: false })
      })
  },

  /**
   * 初始化 WebSocket 监听
   */
  initSocket() {
    if (this.wsHandler) return
    ws.connect()
    this.wsHandler = (payload) => {
      this.handleWsMessage(payload)
    }
    ws.onMessage(this.wsHandler)
  },

  /**
   * 取消 WebSocket 监听
   */
  teardownSocket() {
    if (!this.wsHandler) return
    ws.offMessage(this.wsHandler)
    this.wsHandler = null
  },

  /**
   * 处理 WebSocket 消息
   */
  handleWsMessage(payload) {
    if (!payload || !payload.type || !payload.data) return

    if (payload.type === 'PRESENCE') {
      this.handlePresenceMessage(payload.data)
      return
    }

    if (payload.type !== 'MESSAGE') return

    const message = payload.data
    const list = this.data.conversationList.slice()
    const idx = list.findIndex(item => Number(item.conversationId) === Number(message.conversationId))

    if (idx < 0) {
      this.loadConversations()
      return
    }

    const item = list[idx]
    item.lastMessage = this.buildConversationPreview(message)
    item.lastMessageTime = message.createTime
    item.isOnline = true
    item.lastSeenAt = ''
    if (Number(message.senderId) !== Number(this.currentUserId)) {
      item.unreadCount = (Number(item.unreadCount) || 0) + 1
    }
    item.formattedTime = time.formatConversationTime(message.createTime)
    item.presenceText = this.buildPresenceText(item.isOnline, item.lastSeenAt)
    item.swipeActions = this.buildSwipeActions(Boolean(item.pinned), Number(item.unreadCount) || 0)
    list[idx] = item

    this.setData(this.buildConversationViewData(list))
  },

  buildConversationPreview(message) {
    if (!message) {
      return ''
    }
    if (message.msgType === 'IMAGE') {
      return '[图片]'
    }
    if (message.msgType === 'VIDEO') {
      return '[视频]'
    }
    if (message.msgType === 'FILE') {
      return '[文件]'
    }
    return message.content || ''
  },

  handlePresenceMessage(presence) {
    const targetUserId = Number(presence.userId)
    if (!targetUserId) return

    const list = this.data.conversationList.slice()
    let changed = false
    for (let index = 0; index < list.length; index++) {
      const item = list[index]
      if (Number(item.targetUserId) !== targetUserId) {
        continue
      }
      const isOnline = presence.online === true
      const nextLastSeenAt = isOnline ? '' : (presence.lastSeenAt || item.lastSeenAt || '')
      list[index] = {
        ...item,
        isOnline,
        lastSeenAt: nextLastSeenAt,
        presenceText: this.buildPresenceText(isOnline, nextLastSeenAt)
      }
      changed = true
    }
    if (!changed) return
    this.setData(this.buildConversationViewData(list))
  },

  /**
   * 右滑菜单操作
   */
  onSwipeActionClick(e) {
    const conversationId = Number(e.currentTarget.dataset.id)
    if (!conversationId) return

    const action = e.detail || {}
    if (action.actionType === 'PIN') {
      this.togglePinConversation(conversationId)
      return
    }

    if (action.actionType === 'TOGGLE_READ') {
      this.toggleConversationReadState(conversationId)
      return
    }

    if (action.actionType === 'MORE') {
      this.openConversationMoreMenu(conversationId)
    }
  },

  openConversationMoreMenu(conversationId) {
    const conversation = this.findConversationById(conversationId)
    if (!conversation) return

    wx.showActionSheet({
      itemList: ['查看个人资料', '删除好友（待实现）', '拉聊天室（待实现）'],
      success: ({ tapIndex }) => {
        if (tapIndex === 0) {
          const targetUserId = Number(conversation.targetUserId)
          if (!targetUserId) {
            toastError('用户信息缺失')
            return
          }
          wx.navigateTo({
            url: `/pages/user-detail/user-detail?userId=${targetUserId}`
          })
          return
        }
        toastError('功能待实现')
      }
    })
  },

  findConversationById(conversationId) {
    return (this.data.conversationList || []).find(
      item => Number(item.conversationId) === Number(conversationId)
    ) || null
  },

  toggleConversationReadState(conversationId) {
    const list = this.data.conversationList || []
    const current = list.find(item => Number(item.conversationId) === Number(conversationId))
    if (!current) return
    const currentUnread = Number(current.unreadCount) || 0
    const markRead = currentUnread > 0
    const url = markRead
      ? `/conversations/${conversationId}/read`
      : `/conversations/${conversationId}/unread`

    request.put(url)
      .then(() => {
        const nextUnread = markRead ? 0 : 1
        const nextList = list.map(item => {
          if (Number(item.conversationId) !== Number(conversationId)) {
            return item
          }
          return {
            ...item,
            unreadCount: nextUnread,
            swipeActions: this.buildSwipeActions(Boolean(item.pinned), nextUnread)
          }
        })
        this.setData(this.buildConversationViewData(nextList))
        toastSuccess(markRead ? '已标为已读' : '已标为未读')
      })
      .catch(err => {
        toastError(err, '操作失败')
      })
  },

  /**
   * 切换置顶状态（前端本地实现）
   */
  togglePinConversation(conversationId) {
    const pinnedSet = new Set(this.pinnedConversationIds || [])
    const nextPinned = !pinnedSet.has(conversationId)

    if (nextPinned) {
      pinnedSet.add(conversationId)
    } else {
      pinnedSet.delete(conversationId)
    }

    const pinnedIds = Array.from(pinnedSet)
    this.pinnedConversationIds = pinnedIds
    this.savePinnedConversationIds(pinnedIds)

    const list = (this.data.conversationList || []).map(item => {
      if (Number(item.conversationId) !== conversationId) {
        return item
      }
      return {
        ...item,
        pinned: nextPinned,
        swipeActions: this.buildSwipeActions(nextPinned, Number(item.unreadCount) || 0)
      }
    })

    this.setData(this.buildConversationViewData(list))
    toastSuccess(nextPinned ? '置顶成功' : '已取消置顶')
  },

  formatConversationItem(item) {
    const conversationId = Number(item.conversationId)
    const pinned = (this.pinnedConversationIds || []).includes(conversationId)
    const isOnline = this.resolveOnlineStatus(item)
    const lastMessageTime = item.lastMessageTime || ''
    const lastSeenAt = item.lastSeenAt || ''
    const unreadCount = Number(item.unreadCount) || 0
    return {
      ...item,
      pinned,
      isOnline,
      lastSeenAt,
      unreadCount,
      targetAvatar: item.targetAvatar || DEFAULT_AVATAR,
      formattedTime: lastMessageTime ? time.formatConversationTime(lastMessageTime) : '',
      presenceText: this.buildPresenceText(isOnline, lastSeenAt),
      swipeActions: this.buildSwipeActions(pinned, unreadCount)
    }
  },

  resolveOnlineStatus(item) {
    if (item.isOnline === true || item.online === true) {
      return true
    }
    if (typeof item.onlineStatus === 'string' && item.onlineStatus.toUpperCase() === 'ONLINE') {
      return true
    }
    return false
  },

  buildPresenceText(isOnline, lastSeenAt) {
    if (isOnline) {
      return '在线'
    }
    if (lastSeenAt) {
      return time.formatLastSeenText(lastSeenAt)
    }
    return '上次在线 未知'
  },

  buildConversationViewData(sourceList) {
    const conversationList = this.sortConversationList(sourceList)
    return {
      conversationList,
      conversationSections: this.buildConversationSections(conversationList)
    }
  },

  buildConversationSections(conversationList) {
    const onlineConversations = conversationList.filter(item => item.isOnline)
    const offlineConversations = conversationList.filter(item => !item.isOnline)
    return [
      {
        key: 'online',
        title: `在线好友 (${onlineConversations.length})`,
        rowClass: '',
        showEmpty: true,
        emptyText: '暂无在线好友',
        list: onlineConversations
      },
      {
        key: 'offline',
        title: `离线 (${offlineConversations.length})`,
        rowClass: 'session-row-offline',
        showEmpty: false,
        emptyText: '',
        list: offlineConversations
      }
    ]
  },

  buildSwipeActions(pinned, unreadCount) {
    const styles = this.swipeActionStyles || getSwipeActionStyles()
    return [
      {
        text: pinned ? '取消置顶' : '置顶',
        actionType: 'PIN',
        style: styles.pin
      },
      {
        text: unreadCount > 0 ? '标为已读' : '标为未读',
        actionType: 'TOGGLE_READ',
        style: styles.unread
      },
      {
        text: '更多',
        actionType: 'MORE',
        style: styles.more
      }
    ]
  },

  applyTheme() {
    applyPageTheme(this, {
      tabBar: true,
      extraData: (themeContext) => {
        this.swipeActionStyles = getSwipeActionStyles(themeContext.themeName)
        const list = (this.data.conversationList || []).map(item => ({
          ...item,
          swipeActions: this.buildSwipeActions(Boolean(item.pinned), Number(item.unreadCount) || 0)
        }))
        return this.buildConversationViewData(list)
      }
    })
  },

  sortConversationList(sourceList) {
    return (sourceList || []).slice().sort((a, b) => {
      const pinDiff = Number(Boolean(b.pinned)) - Number(Boolean(a.pinned))
      if (pinDiff !== 0) return pinDiff

      const aTime = a.lastMessageTime || ''
      const bTime = b.lastMessageTime || ''
      if (aTime !== bTime) return bTime.localeCompare(aTime)
      return Number(b.conversationId) - Number(a.conversationId)
    })
  },

  syncPinnedConversationIds() {
    const key = this.getPinnedStorageKey()
    const saved = wx.getStorageSync(key)
    this.pinnedConversationIds = Array.isArray(saved)
      ? saved.map(id => Number(id)).filter(id => Number.isFinite(id))
      : []
  },

  savePinnedConversationIds(pinnedIds) {
    wx.setStorageSync(this.getPinnedStorageKey(), pinnedIds)
  },

  getPinnedStorageKey() {
    const userInfo = auth.getUserInfo()
    const userId = userInfo && userInfo.userId ? userInfo.userId : 0
    return `wolfchat_chat_pins_${userId}`
  },

  /**
   * 进入聊天详情
   */
  goChatDetail(e) {
    const conversationId = e.currentTarget.dataset.id
    wx.navigateTo({
      url: `/pages/chat-detail/chat-detail?conversationId=${conversationId}`
    })
  },

  /**
   * 下拉刷新
   */
  onPullDownRefresh() {
    this.loadConversations().then(() => {
      wx.stopPullDownRefresh()
    })
  },

  /**
   * 去互关列表找行者
   */
  goToMutual() {
    wx.switchTab({
      url: '/pages/follow/follow'
    })
  }
})
