/**
 * 聊天首页
 */
const auth = require('../../utils/auth')
const request = require('../../utils/request')
const time = require('../../utils/time')
const ws = require('../../utils/ws')
const { DEFAULT_AVATAR } = require('../../utils/user')
const { toastError } = require('../../utils/ui')
const { getSwipeActionStyles } = require('../../utils/theme')
const { applyPageTheme } = require('../../utils/page-theme')
const imPageHelper = require('../../utils/im-page-helper')
const imWsHelper = require('../../utils/im-ws-helper')
const lobbyMetaHelper = require('../../utils/lobby-meta-helper')
const conversationViewHelper = require('../../utils/conversation-view-helper')
const conversationActionHelper = require('../../utils/conversation-action-helper')
const { attachDisplayTitle } = require('../../utils/title')
const { setChatBadge, refreshChatUnreadBadge } = require('../../utils/tab-badge')
const pullRefreshHelper = require('../../utils/pull-refresh-helper')
const PRESENCE_FLUSH_DELAY_MS = 80
const CONVERSATION_RELOAD_DELAY_MS = 300
const CHAT_FILTER_ITEMS = [
  { value: 'all', label: '全部', mode: 'base' },
  { value: 'friend', label: '好友', mode: 'base' },
  { value: 'unread', label: '未读', mode: 'toggle' },
  { value: 'online', label: '在线', mode: 'toggle' }
]

Page({
  data: {
    conversationList: [],
    displayConversationList: [],
    filterItems: CHAT_FILTER_ITEMS.map(item => ({
      ...item,
      active: item.value === 'all'
    })),
    baseFilter: 'all',
    unreadOnly: false,
    onlineOnly: false,
    searchKeyword: '',
    noticeUnreadCount: 0,
    lobbyOnlineCount: 0,
    lobbyActiveText: '最近活跃 --',
    lobbyLatestMessageText: '暂无消息',
    displayEmptyDescription: '没有匹配会话',
    loading: false,
    themeClass: 'theme-retro-blue'
  },

  onLoad() {
    imPageHelper.handlePageLoad(this, auth, {
      afterInit: () => {
        const userInfo = auth.getUserInfo()
        this.currentUserId = userInfo ? Number(userInfo.userId) : 0
      }
    })
  },

  onShow() {
    imPageHelper.handlePageShow(this, auth, {
      beforeShow: () => this.applyTheme(),
      afterShow: () => {
        this.syncPinnedConversationIds()
        this.loadFriendUserIdSet()
        this.initSocket()
        this.loadLobbyMeta()
        this.startLobbyMetaPolling()
        this.loadConversations()
        this.loadNoticeUnreadCount()
      }
    })
  },

  onHide() {
    this.stopLobbyMetaPolling()
    this.cleanupPageState({ clearLobbyMeta: true })
  },

  onUnload() {
    this.stopLobbyMetaPolling()
    this.cleanupPageState({ clearLobbyMeta: true })
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
        this.setData(this.buildConversationViewData(list))
        this.syncChatUnreadBadge(list)
      })
      .catch(err => {
        toastError(err, '加载会话失败')
        refreshChatUnreadBadge()
      })
      .finally(() => {
        if (!this.data.loading) {
          return
        }
        this.setData({ loading: false })
      })
  },

  refreshDisplayConversationList() {
    const displayConversationList = this.buildDisplayConversationList(this.data.conversationList || [])
    this.setData({
      displayConversationList,
      displayEmptyDescription: this.buildDisplayEmptyDescription(displayConversationList)
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

  cleanupPageState(options = {}) {
    this.clearPerformanceTimers()
    if (options.clearLobbyMeta) {
      this.clearLobbyMetaTimer()
    }
    this.teardownSocket()
  },

  /**
   * 处理 WebSocket 消息
   */
  handleWsMessage(payload) {
    const payloadType = imWsHelper.getPayloadType(payload)
    if (!payloadType) return

    if (payloadType === 'PRESENCE') {
      if (payload.data) {
        this.queuePresenceMessage(payload.data)
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
      this.scheduleConversationReload()
      return
    }

    const item = list[idx]
    item.lastMessage = conversationViewHelper.buildConversationPreview(message)
    item.lastMessageTime = message.createTime
    item.isOnline = true
    item.lastSeenAt = ''
    const isRecallMessage = String(message.msgType || '').toUpperCase() === 'RECALL'
    if (!isRecallMessage && Number(message.senderId) !== Number(this.currentUserId)) {
      item.unreadCount = (Number(item.unreadCount) || 0) + 1
    }
    item.formattedTime = time.formatConversationTime(message.createTime)
    item.presenceText = conversationViewHelper.buildPresenceText(item.isOnline, item.lastSeenAt, time)
    item.swipeActions = this.buildSwipeActionsByItem(item)
    list[idx] = item

    this.setData(this.buildConversationViewData(list))
    this.syncChatUnreadBadge(list)
  },

  loadLobbyMeta() {
    return lobbyMetaHelper.loadLobbyMeta(this, request, (meta) => {
      const onlineCount = Number(meta.onlineCount) || 0
      const latestMessageAt = meta.latestMessageAt || ''
      const activeText = this.buildLobbyActiveText(latestMessageAt)
      const latestMessageText = this.buildLatestMessageText(
        meta.latestMessageSenderName,
        meta.latestMessagePreview
      )
      if (onlineCount === this.data.lobbyOnlineCount
        && activeText === this.data.lobbyActiveText
        && latestMessageText === this.data.lobbyLatestMessageText) {
        return
      }
      this.setData({
        lobbyOnlineCount: onlineCount,
        lobbyActiveText: activeText,
        lobbyLatestMessageText: latestMessageText
      })
    })
  },

  scheduleLobbyMetaRefresh() {
    lobbyMetaHelper.scheduleLobbyMetaRefresh(this, () => this.loadLobbyMeta(), 600)
  },

  clearLobbyMetaTimer() {
    lobbyMetaHelper.clearLobbyMetaTimer(this)
  },

  buildLobbyActiveText(latestMessageAt) {
    return lobbyMetaHelper.buildLobbyActiveText(latestMessageAt, time)
  },

  buildLatestMessageText(latestMessageSenderName, latestMessagePreview) {
    return lobbyMetaHelper.buildLatestMessageText(latestMessageSenderName, latestMessagePreview)
  },

  startLobbyMetaPolling() {
    lobbyMetaHelper.startLobbyMetaPolling(this, () => this.loadLobbyMeta(), 10000)
  },

  stopLobbyMetaPolling() {
    lobbyMetaHelper.stopLobbyMetaPolling(this)
  },

  queuePresenceMessage(presence) {
    const targetUserId = Number(presence.userId)
    if (!targetUserId) return

    if (!this.pendingPresenceMap) {
      this.pendingPresenceMap = {}
    }
    this.pendingPresenceMap[targetUserId] = {
      online: presence.online === true,
      lastSeenAt: presence.lastSeenAt || ''
    }
    if (this.presenceFlushTimer) return

    this.presenceFlushTimer = setTimeout(() => {
      this.presenceFlushTimer = null
      this.flushPresenceMessages()
    }, PRESENCE_FLUSH_DELAY_MS)
  },

  flushPresenceMessages() {
    const pending = this.pendingPresenceMap
    this.pendingPresenceMap = null
    if (!pending) return

    const list = this.data.conversationList.slice()
    let changed = false
    for (let index = 0; index < list.length; index++) {
      const item = list[index]
      const targetUserId = Number(item.targetUserId)
      const nextPresence = pending[targetUserId]
      if (!nextPresence) continue

      const nextLastSeenAt = nextPresence.online
        ? ''
        : (nextPresence.lastSeenAt || item.lastSeenAt || '')
      if (item.isOnline === nextPresence.online && (item.lastSeenAt || '') === nextLastSeenAt) {
        continue
      }

      list[index] = {
        ...item,
        isOnline: nextPresence.online,
        lastSeenAt: nextLastSeenAt,
        presenceText: conversationViewHelper.buildPresenceText(nextPresence.online, nextLastSeenAt, time)
      }
      changed = true
    }
    if (!changed) return
    this.setData(this.buildConversationViewData(list))
  },

  scheduleConversationReload() {
    if (this.conversationReloadTimer) {
      return
    }
    this.conversationReloadTimer = setTimeout(() => {
      this.conversationReloadTimer = null
      if (!this.data.loading) {
        this.loadConversations()
      }
    }, CONVERSATION_RELOAD_DELAY_MS)
  },

  clearPerformanceTimers() {
    if (this.presenceFlushTimer) {
      clearTimeout(this.presenceFlushTimer)
      this.presenceFlushTimer = null
    }
    if (this.conversationReloadTimer) {
      clearTimeout(this.conversationReloadTimer)
      this.conversationReloadTimer = null
    }
    this.pendingPresenceMap = null
  },

  selectTab(e) {
    const tab = String(e.currentTarget.dataset.tab || '')
    if (!tab) {
      return
    }

    const nextState = {
      baseFilter: this.data.baseFilter,
      unreadOnly: this.data.unreadOnly,
      onlineOnly: this.data.onlineOnly
    }

    if (tab === 'all' || tab === 'friend') {
      if (tab === nextState.baseFilter) {
        return
      }
      nextState.baseFilter = tab
    } else if (tab === 'unread') {
      nextState.unreadOnly = !nextState.unreadOnly
    } else if (tab === 'online') {
      nextState.onlineOnly = !nextState.onlineOnly
    } else {
      return
    }

    this.setData({
      ...nextState,
      filterItems: this.buildFilterItemsByState(nextState)
    }, () => {
      this.refreshDisplayConversationList()
    })
  },

  onSearchInput(e) {
    this.setData({
      searchKeyword: String(e.detail.value || '')
    }, () => {
      this.refreshDisplayConversationList()
    })
  },

  onSearchConfirm() {
    this.refreshDisplayConversationList()
  },

  onSearchTap() {
    this.refreshDisplayConversationList()
  },

  async loadNoticeUnreadCount() {
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

  async loadFriendUserIdSet() {
    try {
      const res = await request.get('/follow/mutual')
      const list = Array.isArray(res && res.data) ? res.data : []
      this.friendUserIdSet = new Set(
        list.map(item => Number(item && item.userId))
          .filter(id => Number.isFinite(id) && id > 0)
      )
    } catch (error) {
      this.friendUserIdSet = new Set()
    } finally {
      this.refreshDisplayConversationList()
    }
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
        this.syncChatUnreadBadge(nextList)
      })
      .catch(err => {
        toastError(err, '更新未读状态失败')
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
  },

  formatConversationItem(item) {
    const withDisplayTitle = attachDisplayTitle(
      item,
      item.targetEquippedTitleName,
      item.targetEquippedTitleColor
    )
    const conversationId = Number(item.conversationId)
    const pinned = (this.pinnedConversationIds || []).includes(conversationId)
    const isOnline = this.resolveOnlineStatus(withDisplayTitle)
    const lastMessageTime = withDisplayTitle.lastMessageTime || ''
    const lastSeenAt = withDisplayTitle.lastSeenAt || ''
    const unreadCount = Number(withDisplayTitle.unreadCount) || 0
    return {
      ...withDisplayTitle,
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
    const conversationList = Array.isArray(sourceList) ? sourceList.slice() : []
    const displayConversationList = this.buildDisplayConversationList(conversationList)
    const displayEmptyDescription = this.buildDisplayEmptyDescription(displayConversationList)
    return {
      conversationList,
      displayConversationList,
      displayEmptyDescription,
      filterItems: this.buildFilterItemsByState({
        baseFilter: this.data.baseFilter,
        unreadOnly: this.data.unreadOnly,
        onlineOnly: this.data.onlineOnly
      })
    }
  },

  buildDisplayEmptyDescription(displayConversationList) {
    if ((displayConversationList || []).length > 0) {
      return ''
    }
    if (String(this.data.searchKeyword || '').trim()) {
      return '没有匹配会话'
    }
    if (this.data.unreadOnly && this.data.onlineOnly) {
      if (this.data.baseFilter === 'friend') {
        return '当前没有符合筛选的好友会话'
      }
      return '当前没有符合筛选的会话'
    }
    if (this.data.baseFilter === 'friend' && this.data.onlineOnly) {
      return '当前没有好友在线'
    }
    if (this.data.baseFilter === 'friend' && this.data.unreadOnly) {
      return '当前没有好友未读会话'
    }
    if (this.data.baseFilter === 'friend') {
      return '当前没有好友会话'
    }
    if (this.data.onlineOnly) {
      return '当前没有在线会话'
    }
    if (this.data.unreadOnly) {
      return '当前没有未读会话'
    }
    return '没有匹配会话'
  },

  buildDisplayConversationList(conversationList) {
    const keyword = String(this.data.searchKeyword || '').trim().toLowerCase()
    let list = this.sortConversationList(conversationList)

    if (this.data.baseFilter === 'friend') {
      list = list.filter(item => this.isFriendConversation(item))
    }
    if (this.data.unreadOnly) {
      list = list.filter(item => (Number(item.unreadCount) || 0) > 0)
    }
    if (this.data.onlineOnly) {
      list = list.filter(item => item.isOnline)
    }
    if (!keyword) {
      return list
    }
    return list.filter(item => this.matchConversationByKeyword(item, keyword))
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

  isFriendConversation(item) {
    const targetUserId = Number(item && item.targetUserId)
    if (!Number.isFinite(targetUserId) || targetUserId <= 0) {
      return false
    }
    return Boolean(this.friendUserIdSet && this.friendUserIdSet.has(targetUserId))
  },

  buildFilterItemsByState(state) {
    return CHAT_FILTER_ITEMS.map(item => {
      if (item.mode === 'base') {
        return {
          ...item,
          active: state.baseFilter === item.value
        }
      }
      return {
        ...item,
        active: item.value === 'unread' ? Boolean(state.unreadOnly) : Boolean(state.onlineOnly)
      }
    })
  },

  matchConversationByKeyword(item, keyword) {
    const fields = [
      item.displayName,
      item.targetNickname,
      item.targetWolfNo,
      item.lastMessage,
      item.presenceText
    ]
    return fields.some(field => String(field || '').toLowerCase().includes(keyword))
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

  syncChatUnreadBadge(list) {
    const conversationList = Array.isArray(list) ? list : (this.data.conversationList || [])
    const unreadTotal = conversationList.reduce((sum, item) => {
      const count = Number(item.unreadCount)
      if (!Number.isFinite(count) || count <= 0) {
        return sum
      }
      return sum + Math.floor(count)
    }, 0)
    setChatBadge(unreadTotal)
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
    pullRefreshHelper.runPullDownRefresh(this, async () => {
      await this.loadConversations()
      await this.loadNoticeUnreadCount()
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

  goNotice() {
    wx.navigateTo({
      url: '/pages/notice/notice'
    })
  },

  openLobby() {
    wx.navigateTo({
      url: '/pages/lobby/lobby'
    })
  }
})
