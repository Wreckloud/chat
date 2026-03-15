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
const imPageHelper = require('../../utils/im-page-helper')
const imWsHelper = require('../../utils/im-ws-helper')
const lobbyMetaHelper = require('../../utils/lobby-meta-helper')
const conversationViewHelper = require('../../utils/conversation-view-helper')
const conversationActionHelper = require('../../utils/conversation-action-helper')

Page({
  data: {
    conversationList: [],
    conversationSections: [],
    lobbyOnlineCount: 0,
    lobbyActiveText: '最近活跃 --',
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
    this.loadLobbyMeta()
    this.loadConversations()
  },

  onHide() {
    this.teardownSocket()
  },

  onUnload() {
    this.clearLobbyMetaTimer()
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
    imPageHelper.initSocket(this, ws, payload => this.handleWsMessage(payload))
  },

  /**
   * 取消 WebSocket 监听
   */
  teardownSocket() {
    imPageHelper.teardownSocket(this, ws)
  },

  /**
   * 处理 WebSocket 消息
   */
  handleWsMessage(payload) {
    const payloadType = imWsHelper.getPayloadType(payload)
    if (!payloadType) return

    if (payloadType === 'PRESENCE') {
      if (payload.data) {
        this.handlePresenceMessage(payload.data)
      }
      this.scheduleLobbyMetaRefresh()
      return
    }

    if (payloadType === 'LOBBY_MESSAGE') {
      this.scheduleLobbyMetaRefresh()
      return
    }

    if (payloadType !== 'MESSAGE') return
    if (!payload.data) return

    const message = payload.data
    const list = this.data.conversationList.slice()
    const idx = list.findIndex(item => Number(item.conversationId) === Number(message.conversationId))

    if (idx < 0) {
      this.loadConversations()
      return
    }

    const item = list[idx]
    item.lastMessage = conversationViewHelper.buildConversationPreview(message)
    item.lastMessageTime = message.createTime
    item.isOnline = true
    item.lastSeenAt = ''
    if (Number(message.senderId) !== Number(this.currentUserId)) {
      item.unreadCount = (Number(item.unreadCount) || 0) + 1
    }
    item.formattedTime = time.formatConversationTime(message.createTime)
    item.presenceText = conversationViewHelper.buildPresenceText(item.isOnline, item.lastSeenAt, time)
    item.swipeActions = this.buildSwipeActionsByItem(item)
    list[idx] = item

    this.setData(this.buildConversationViewData(list))
  },

  loadLobbyMeta() {
    return lobbyMetaHelper.loadLobbyMeta(this, request, (meta) => {
      const onlineCount = Number(meta.onlineCount) || 0
      const latestActiveAt = meta.latestActiveAt || ''
      this.setData({
        lobbyOnlineCount: onlineCount,
        lobbyActiveText: this.buildLobbyActiveText(latestActiveAt)
      })
    })
  },

  scheduleLobbyMetaRefresh() {
    lobbyMetaHelper.scheduleLobbyMetaRefresh(this, () => this.loadLobbyMeta(), 600)
  },

  clearLobbyMetaTimer() {
    lobbyMetaHelper.clearLobbyMetaTimer(this)
  },

  buildLobbyActiveText(latestActiveAt) {
    return lobbyMetaHelper.buildLobbyActiveText(latestActiveAt, time)
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
        presenceText: conversationViewHelper.buildPresenceText(isOnline, nextLastSeenAt, time)
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

    const actionType = conversationActionHelper.resolveSwipeActionType(e.detail || {})
    if (actionType === conversationActionHelper.SWIPE_ACTION_TYPES.PIN) {
      this.togglePinConversation(conversationId)
      return
    }

    if (actionType === conversationActionHelper.SWIPE_ACTION_TYPES.TOGGLE_READ) {
      this.toggleConversationReadState(conversationId)
      return
    }

    if (actionType === conversationActionHelper.SWIPE_ACTION_TYPES.MORE) {
      this.openConversationMoreMenu(conversationId)
    }
  },

  openConversationMoreMenu(conversationId) {
    const conversation = conversationActionHelper.findConversationById(this.data.conversationList, conversationId)
    if (!conversation) return

    wx.showActionSheet({
      itemList: ['查看个人资料'],
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
        }
      }
      })
  },

  toggleConversationReadState(conversationId) {
    const list = this.data.conversationList || []
    const current = conversationActionHelper.findConversationById(list, conversationId)
    if (!current) return
    const readTogglePlan = conversationActionHelper.buildReadTogglePlan(conversationId, current.unreadCount)

    request.put(readTogglePlan.url)
      .then(() => {
        const nextList = conversationActionHelper.mapConversationById(list, conversationId, (item) => {
          const nextItem = {
            ...item,
            unreadCount: readTogglePlan.nextUnread
          }
          return this.withConversationSwipeActions(nextItem)
        })
        this.setData(this.buildConversationViewData(nextList))
        toastSuccess(readTogglePlan.markRead ? '已标为已读' : '已标为未读')
      })
      .catch(err => {
        toastError(err, '操作失败')
      })
  },

  /**
   * 切换置顶状态（前端本地实现）
   */
  togglePinConversation(conversationId) {
    const toggleResult = conversationActionHelper.togglePinnedConversationIds(this.pinnedConversationIds, conversationId)
    this.pinnedConversationIds = toggleResult.pinnedIds
    this.savePinnedConversationIds(toggleResult.pinnedIds)

    const list = conversationActionHelper.mapConversationById(this.data.conversationList || [], conversationId, (item) => {
      const nextItem = {
        ...item,
        pinned: toggleResult.nextPinned
      }
      return this.withConversationSwipeActions(nextItem)
    })

    this.setData(this.buildConversationViewData(list))
    toastSuccess(toggleResult.nextPinned ? '置顶成功' : '已取消置顶')
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
      presenceText: conversationViewHelper.buildPresenceText(isOnline, lastSeenAt, time),
      swipeActions: this.buildSwipeActions(pinned, unreadCount)
    }
  },

  resolveOnlineStatus(item) {
    return conversationViewHelper.resolveOnlineStatus(item)
  },

  buildConversationViewData(sourceList) {
    const conversationList = this.sortConversationList(sourceList)
    return {
      conversationList,
      conversationSections: this.buildConversationSections(conversationList)
    }
  },

  buildConversationSections(conversationList) {
    return conversationViewHelper.buildConversationSections(conversationList)
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
        const list = (this.data.conversationList || []).map(item => this.withConversationSwipeActions(item))
        return this.buildConversationViewData(list)
      }
    })
  },

  withConversationSwipeActions(item) {
    return {
      ...item,
      swipeActions: this.buildSwipeActionsByItem(item)
    }
  },

  buildSwipeActionsByItem(item) {
    return this.buildSwipeActions(Boolean(item.pinned), Number(item.unreadCount) || 0)
  },

  sortConversationList(sourceList) {
    return conversationViewHelper.sortConversationList(sourceList)
  },

  syncPinnedConversationIds() {
    const storageKey = conversationActionHelper.buildPinnedStorageKey(this.currentUserId)
    const saved = wx.getStorageSync(storageKey)
    this.pinnedConversationIds = conversationActionHelper.normalizePinnedConversationIds(saved)
  },

  savePinnedConversationIds(pinnedIds) {
    wx.setStorageSync(conversationActionHelper.buildPinnedStorageKey(this.currentUserId), pinnedIds)
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
  },

  openLobby() {
    wx.navigateTo({
      url: '/pages/lobby/lobby'
    })
  }
})
