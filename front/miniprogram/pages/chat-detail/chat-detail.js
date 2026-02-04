/**
 * 聊天详情页面
 */
const request = require('../../utils/request')
const auth = require('../../utils/auth')
const time = require('../../utils/time')

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

  onLoad(options) {
    const conversationId = options.conversationId
    if (!conversationId) {
      wx.showToast({
        title: '会话ID不能为空',
        icon: 'none'
      })
      setTimeout(() => {
        wx.navigateBack()
      }, 1500)
      return
    }

    // 从本地存储获取当前用户信息
    const userInfo = auth.getUserInfo()
    const currentUserId = userInfo ? userInfo.userId : null

    this.setData({
      conversationId: conversationId,
      currentUserId: currentUserId,
      currentUser: userInfo
    })

    // 加载会话信息（获取对方用户信息）
    this.loadConversation()
    this.loadMessages()
  },

  /**
   * 加载会话信息
   */
  loadConversation() {
    // 先从会话列表中获取对方信息
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
          
          this.setData({ targetUser })
          
          // 设置页面标题为对方昵称
          wx.setNavigationBarTitle({
            title: targetUser.nickname || targetUser.wolfNo || '聊天'
          })
        }
      })
      .catch(err => {
        console.error('加载会话信息失败:', err)
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
        wx.showToast({
          title: err.message || '加载失败',
          icon: 'none',
          duration: 2000
        })
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
      wx.showToast({
        title: '消息内容不能为空',
        icon: 'none'
      })
      return
    }

    if (this.data.sending) {
      return
    }

    this.setData({ sending: true })

    request.post(`/conversations/${this.data.conversationId}/messages`, {
      content: content
    })
      .then(res => {
        const newMessage = res.data
        const messages = [...this.data.messages, newMessage]
        
        // 处理消息时间显示
        this.processMessageTimes(messages)
        
        this.setData({
          messages,
          inputMessage: '',
          sending: false
        })

        this.scrollToBottom()
      })
      .catch(err => {
        console.error('发送消息失败:', err)
        wx.showToast({
          title: err.message || '发送失败',
          icon: 'none',
          duration: 2000
        })
        this.setData({ sending: false })
      })
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
        current.timeText = time.formatMessageTime(current.createTime)
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
  }
})