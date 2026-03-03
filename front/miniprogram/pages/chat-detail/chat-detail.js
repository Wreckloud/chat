/**
 * 聊天详情页面
 */
const request = require('../../utils/request')
const auth = require('../../utils/auth')
const time = require('../../utils/time')
const ws = require('../../utils/ws')
const { normalizeUser, openUserProfile } = require('../../utils/user')
const { toastError } = require('../../utils/ui')

Page({
  data: {
    conversationId: null,
    messages: [],
    inputMessage: '',
    loading: false,
    sending: false,
    hasMore: true,
    currentPage: 1,
    pageSize: 20,
    currentUserId: null,
    scrollTop: 0,
    targetUser: null, // 对方用户信息
    currentUser: null // 当前用户信息
  },

  SEND_TIMEOUT_MS: 8000,

  onLoad(options) {
    const conversationId = options.conversationId
    if (!conversationId) {
      toastError('会话ID不能为空', '会话ID不能为空')
      setTimeout(() => {
        wx.navigateBack()
      }, 1500)
      return
    }

    const numericConversationId = Number(conversationId)

    // 从本地存储获取当前用户信息
    // 本地用户信息用于区分消息归属
    const userInfo = auth.getUserInfo()
    const currentUserId = userInfo ? userInfo.userId : null

    this.setData({
      conversationId: numericConversationId,
      currentUserId: currentUserId,
      currentUser: normalizeUser(userInfo)
    })

    // 加载会话信息（获取对方用户信息）
    // 获取对方信息并设置标题
    this.loadConversation()
    this.loadMessages()
    this.initSocket()
  },

  onUnload() {
    this.clearPendingTimer()
    if (this.wsHandler) {
      ws.offMessage(this.wsHandler)
      this.wsHandler = null
    }
  },

  /**
   * 初始化 WebSocket
   */
  initSocket() {
    ws.connect()
    this.wsHandler = (payload) => {
      this.handleWsMessage(payload)
    }
    ws.onMessage(this.wsHandler)
  },

  /**
   * 加载会话信息
   */
  loadConversation() {
    // 先从会话列表中获取对方信息
    // 复用会话列表数据获取对方资料
    request.get('/conversations')
      .then(res => {
        const conversations = res.data || []
        const conversation = conversations.find(c => c.conversationId === Number(this.data.conversationId))
        
        if (conversation) {
          const targetUser = {
            userId: conversation.targetUserId,
            nickname: conversation.targetNickname,
            wolfNo: conversation.targetWolfNo,
            avatar: conversation.targetAvatar
          }
          // 对方头像兜底，避免空白头像
          const normalized = normalizeUser(targetUser)
          this.setData({ targetUser: normalized })
          
          // 设置页面标题为对方昵称
          wx.setNavigationBarTitle({
            title: normalized.nickname || normalized.wolfNo || '聊天'
          })
        }
      })
      .catch(err => {
        console.error('加载会话信息失败:', err)
        toastError(err, '加载失败')
      })
  },

  /**
   * 加载消息列表
   */
  loadMessages() {
    if (this.data.loading || !this.data.hasMore) return

    this.setData({ loading: true })

    const page = this.data.currentPage
    request.get(`/conversations/${this.data.conversationId}/messages`, {
      page: page,
      size: this.data.pageSize
    })
      .then(res => {
        const records = res.data.records || []
        const ordered = records.slice().reverse() // 转成时间正序
        const messages = page === 1
          ? ordered
          : [...ordered, ...this.data.messages]

        // 处理消息时间显示
        this.processMessageTimes(messages)

        this.setData({
          messages,
          loading: false,
          hasMore: records.length >= this.data.pageSize,
          currentPage: page + 1
        })

        // 首次加载后滚动到底部
        if (page === 1) {
          this.scrollToBottom()
        }
      })
      .catch(err => {
        console.error('加载消息失败:', err)
        toastError(err, '加载失败')
        this.setData({ loading: false })
      })
  },

  /**
   * 输入框内容改变
   */
  onMessageInput(e) {
    this.setData({
      inputMessage: e.detail.value
    })
  },

  /**
   * 发送消息
   */
  sendMessage() {
    const content = this.data.inputMessage.trim()

    if (!content) {
      toastError('消息内容不能为空', '消息内容不能为空')
      return
    }

    if (this.data.sending) {
      return
    }

    this.setData({ sending: true })

    const clientMsgId = `c_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`
    this.pendingClientMsgId = clientMsgId
    this.startPendingTimer(clientMsgId)

    ws.send({
      type: 'SEND',
      clientMsgId: clientMsgId,
      conversationId: Number(this.data.conversationId),
      content: content
    })
  },

  /**
   * 处理 WebSocket 消息
   */
  handleWsMessage(payload) {
    if (!payload || !payload.type) return

    if (payload.type === 'ERROR') {
      if (payload.clientMsgId && payload.clientMsgId === this.pendingClientMsgId) {
        this.clearPendingState(payload.clientMsgId)
      } else if (!payload.clientMsgId && this.data.sending) {
        // 通用错误也要收敛发送态，避免页面卡住
        this.clearPendingState()
      }
      toastError(payload.message || '发送失败', '发送失败')
      return
    }

    if (payload.type === 'ACK') {
      if (payload.clientMsgId && payload.clientMsgId === this.pendingClientMsgId) {
        this.clearPendingTimer()
        this.pendingClientMsgId = null
        this.setData({
          inputMessage: '',
          sending: false
        })
      }
      this.appendMessage(payload.data)
      return
    }

    if (payload.type === 'MESSAGE') {
      this.appendMessage(payload.data)
    }
  },

  /**
   * 追加消息并刷新时间显示
   */
  appendMessage(message) {
    if (!message || message.conversationId !== Number(this.data.conversationId)) {
      return
    }
    if (this.data.messages.some(item => item.messageId === message.messageId)) {
      return
    }

    const messages = [...this.data.messages, message]
    this.processMessageTimes(messages)
    this.setData({ messages })
    this.scrollToBottom()
  },

  goUserProfile() {
    const user = this.data.targetUser
    if (!user || !user.userId) return
    openUserProfile(user)
  },

  /**
   * 处理消息时间显示
   * 相邻消息间隔>5分钟才显示时间
   */
  processMessageTimes(messages) {
    for (let i = 0; i < messages.length; i++) {
      const current = messages[i]
      const prev = i > 0 ? messages[i - 1] : null
      
      current.showTime = time.shouldShowTime(current.createTime, prev ? prev.createTime : null)
      if (current.showTime) {
        current.timeText = time.formatTime(current.createTime)
      }
    }
  },

  /**
   * 滚动到底部
   */
  scrollToBottom() {
    this.setData({
      scrollTop: 999999
    })
  },

  /**
   * 上拉加载更多
   */
  onReachBottom() {
    this.loadMessages()
  },

  startPendingTimer(clientMsgId) {
    this.clearPendingTimer()
    this.pendingTimer = setTimeout(() => {
      if (this.pendingClientMsgId === clientMsgId) {
        this.clearPendingState(clientMsgId)
        toastError('消息发送超时，请重试', '发送超时')
      }
    }, this.SEND_TIMEOUT_MS)
  },

  clearPendingTimer() {
    if (this.pendingTimer) {
      clearTimeout(this.pendingTimer)
      this.pendingTimer = null
    }
  },

  clearPendingState(clientMsgId) {
    if (clientMsgId && this.pendingClientMsgId !== clientMsgId) {
      return
    }
    this.clearPendingTimer()
    this.pendingClientMsgId = null
    this.setData({ sending: false })
  }
})
