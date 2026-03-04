/**
 * 聊天首页
 */
const auth = require('../../utils/auth')
const request = require('../../utils/request')
const time = require('../../utils/time')
const ws = require('../../utils/ws')
const { DEFAULT_AVATAR, openUserProfile } = require('../../utils/user')
const { toastError, toastSuccess } = require('../../utils/ui')
const { getSwipeActionStyles } = require('../../utils/theme')
const { applyPageTheme } = require('../../utils/page-theme')

Page({
  data: {
    // 会话列表数据
    conversationList: [],
    loading: false,
    themeClass: 'theme-retro-blue'
  },

  onLoad() {
    if (!auth.requireLogin()) {
      return
    }
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
          conversationList: this.sortConversationList(list),
          loading: false
        })
      })
      .catch(err => {
        console.error('加载会话列表失败:', err)
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
    if (!payload || payload.type !== 'MESSAGE' || !payload.data) return

    const message = payload.data
    const list = this.data.conversationList.slice()
    const idx = list.findIndex(item => Number(item.conversationId) === Number(message.conversationId))

    if (idx < 0) {
      this.loadConversations()
      return
    }

    const item = list[idx]
    item.lastMessage = message.content || item.lastMessage
    item.lastMessageTime = message.createTime
    item.formattedTime = time.formatTime(message.createTime)
    item.swipeActions = this.buildSwipeActions(Boolean(item.pinned))
    list[idx] = item

    this.setData({
      conversationList: this.sortConversationList(list)
    })
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

    if (action.actionType === 'UNREAD') {
      toastError('功能待实现')
      return
    }

    if (action.actionType === 'DELETE') {
      toastError('功能待实现')
    }
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
        swipeActions: this.buildSwipeActions(nextPinned)
      }
    })

    this.setData({
      conversationList: this.sortConversationList(list)
    })
    toastSuccess(nextPinned ? '置顶成功' : '已取消置顶')
  },

  formatConversationItem(item) {
    const conversationId = Number(item.conversationId)
    const pinned = (this.pinnedConversationIds || []).includes(conversationId)
    return {
      ...item,
      pinned: pinned,
      targetAvatar: item.targetAvatar || DEFAULT_AVATAR,
      formattedTime: item.lastMessageTime ? time.formatTime(item.lastMessageTime) : '',
      swipeActions: this.buildSwipeActions(pinned)
    }
  },

  buildSwipeActions(pinned) {
    const styles = this.swipeActionStyles || getSwipeActionStyles()
    return [
      {
        text: pinned ? '取消置顶' : '置顶',
        actionType: 'PIN',
        style: styles.pin
      },
      {
        text: '标为未读',
        actionType: 'UNREAD',
        style: styles.unread
      },
      {
        text: '删除',
        actionType: 'DELETE',
        style: styles.delete
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
          swipeActions: this.buildSwipeActions(Boolean(item.pinned))
        }))
        return {
          conversationList: list
        }
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

  goUserProfile(e) {
    const item = e.currentTarget.dataset.item
    if (!item || !item.targetUserId) return
    const user = {
      userId: item.targetUserId,
      nickname: item.targetNickname,
      wolfNo: item.targetWolfNo,
      avatar: item.targetAvatar
    }
    openUserProfile(user)
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
