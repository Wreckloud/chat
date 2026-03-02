/**
 * 聊天首页
 */
const auth = require('../../utils/auth')
const request = require('../../utils/request')
const time = require('../../utils/time')
const ws = require('../../utils/ws')
const { DEFAULT_AVATAR, openUserProfile } = require('../../utils/user')
const { toastError } = require('../../utils/ui')

Page({
  data: {
    // 会话列表数据
    conversationList: [],
    loading: false
  },

  onLoad() {
    // 检查登录状态
    if (!auth.isLoggedIn()) {
      wx.redirectTo({
        url: '/pages/login/login'
      })
      return
    }
    
    // 加载会话列表
    this.loadConversations()
  },

  onShow() {
    // 每次显示时刷新会话列表
    if (auth.isLoggedIn()) {
      this.loadConversations()
      this.initSocket()
    }
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
    if (this.data.loading) return

    this.setData({ loading: true })

    request.get('/conversations')
      .then(res => {
        const list = (res.data || []).map(item => ({
          ...item,
          targetAvatar: item.targetAvatar || DEFAULT_AVATAR,
          formattedTime: item.lastMessageTime ? time.formatTime(item.lastMessageTime) : ''
        }))
        
        this.setData({
          conversationList: list,
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
    const idx = list.findIndex(item => item.conversationId === message.conversationId)

    if (idx < 0) {
      this.loadConversations()
      return
    }

    const item = list[idx]
    item.lastMessage = message.content || item.lastMessage
    item.lastMessageTime = message.createTime
    item.formattedTime = time.formatTime(message.createTime)

    list.splice(idx, 1)
    list.unshift(item)

    this.setData({ conversationList: list })
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
    this.loadConversations()
    wx.stopPullDownRefresh()
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

