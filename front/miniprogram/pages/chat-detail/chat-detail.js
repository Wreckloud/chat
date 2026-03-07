const request = require('../../utils/request')
const auth = require('../../utils/auth')
const time = require('../../utils/time')
const ws = require('../../utils/ws')
const { normalizeUser, openUserProfile } = require('../../utils/user')
const { toastError, toastSuccess } = require('../../utils/ui')
const { applyPageTheme } = require('../../utils/page-theme')

Page({
  data: {
    conversationId: null,
    messages: [],
    messageBlocks: [],
    inputMessage: '',
    loading: false,
    sending: false,
    hasMore: true,
    currentPage: 1,
    pageSize: 20,
    scrollTop: 0,
    targetUser: {},
    themeClass: 'theme-retro-blue'
  },

  SEND_TIMEOUT_MS: 8000,

  onLoad(options) {
    if (!auth.requireLogin()) return

    const conversationId = Number(options.conversationId)
    if (!conversationId) {
      toastError('会话ID不能为空')
      setTimeout(() => {
        wx.navigateBack()
      }, 1500)
      return
    }

    const userInfo = auth.getUserInfo()
    this.currentUserId = userInfo ? userInfo.userId : null
    this.currentUser = normalizeUser(userInfo) || {}

    this.setData({
      conversationId
    })

    this.loadConversation()
    this.loadMessages()
    this.initSocket()
  },

  onShow() {
    if (!auth.requireLogin()) return
    this.applyTheme()
  },

  onUnload() {
    this.clearPendingTimer()
    this.teardownSocket()
  },

  initSocket() {
    if (this.wsHandler) return
    ws.connect()
    this.wsHandler = (payload) => {
      this.handleWsMessage(payload)
    }
    ws.onMessage(this.wsHandler)
  },

  teardownSocket() {
    if (!this.wsHandler) return
    ws.offMessage(this.wsHandler)
    this.wsHandler = null
  },

  loadConversation() {
    request.get('/conversations')
      .then(res => {
        const conversations = res.data || []
        const conversation = conversations.find(
          c => Number(c.conversationId) === Number(this.data.conversationId)
        )
        if (!conversation) return

        const normalized = normalizeUser({
          userId: conversation.targetUserId,
          nickname: conversation.targetNickname,
          wolfNo: conversation.targetWolfNo,
          avatar: conversation.targetAvatar
        }) || {}

        this.setData({
          targetUser: normalized,
          messageBlocks: this.buildMessageBlocks(this.data.messages)
        })

        wx.setNavigationBarTitle({
          title: normalized.nickname || normalized.wolfNo || '聊天'
        })

        const needResolveTargetProfile = normalized.userId
          && !normalized.nickname
          && !normalized.wolfNo
        if (needResolveTargetProfile) {
          this.loadTargetUserProfile(normalized.userId)
        }
      })
      .catch(err => {
        toastError(err, '加载失败')
      })
  },

  loadTargetUserProfile(userId) {
    request.get(`/users/${userId}`)
      .then(res => {
        if (!res || !res.data) return
        const user = normalizeUser(res.data) || {}
        this.setData({
          targetUser: user,
          messageBlocks: this.buildMessageBlocks(this.data.messages)
        })
        wx.setNavigationBarTitle({
          title: user.nickname || user.wolfNo || '聊天'
        })
      })
      .catch(() => {})
  },

  loadMessages() {
    if (this.data.loading || !this.data.hasMore) return

    this.setData({ loading: true })

    const page = this.data.currentPage
    request.get(`/conversations/${this.data.conversationId}/messages`, {
      page,
      size: this.data.pageSize
    })
      .then(res => {
        const records = res.data.records || []
        const ordered = records.slice().reverse()
        const messages = page === 1
          ? ordered
          : [...ordered, ...this.data.messages]

        this.setData({
          messages,
          messageBlocks: this.buildMessageBlocks(messages),
          loading: false,
          hasMore: records.length >= this.data.pageSize,
          currentPage: page + 1
        })

        if (page === 1) {
          this.scrollToBottom()
        }
      })
      .catch(err => {
        toastError(err, '加载失败')
        this.setData({ loading: false })
      })
  },

  onMessageInput(e) {
    this.setData({
      inputMessage: e.detail.value
    })
  },

  sendMessage() {
    const content = this.data.inputMessage.trim()
    if (!content) {
      toastError('消息内容不能为空')
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

  handleWsMessage(payload) {
    if (!payload || !payload.type) return

    if (payload.type === 'ERROR') {
      if (payload.clientMsgId && payload.clientMsgId === this.pendingClientMsgId) {
        this.clearPendingState(payload.clientMsgId)
      } else if (!payload.clientMsgId && this.data.sending) {
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

  appendMessage(message) {
    if (!message || message.conversationId !== Number(this.data.conversationId)) {
      return
    }
    if (this.data.messages.some(item => item.messageId === message.messageId)) {
      return
    }

    const messages = [...this.data.messages, message]
    this.setData({
      messages,
      messageBlocks: this.buildMessageBlocks(messages)
    })
    this.scrollToBottom()
  },

  goUserProfile() {
    const user = this.data.targetUser
    if (!user || !user.userId) return
    openUserProfile(user)
  },

  onTapUserElement(e) {
    const userId = e && e.currentTarget && e.currentTarget.dataset
      ? Number(e.currentTarget.dataset.userId)
      : 0
    if (!userId) return
    openUserProfile({ userId })
  },

  buildMessageBlocks(messages) {
    if (!Array.isArray(messages) || messages.length === 0) {
      return []
    }

    const blocks = []
    let prevDateLabel = ''
    let currentGroup = null

    for (let index = 0; index < messages.length; index++) {
      const message = messages[index]
      const currentDateLabel = time.formatDateLabel(message.createTime)
      const showDivider = !prevDateLabel || currentDateLabel !== prevDateLabel
      if (showDivider) {
        blocks.push({
          type: 'divider',
          key: `d_${message.messageId || index}`,
          timeText: currentDateLabel
        })
        currentGroup = null
      }

      const isSelf = Number(message.senderId) === Number(this.currentUserId)
      const sender = isSelf ? (this.currentUser || {}) : (this.data.targetUser || {})
      const senderName = sender.nickname || sender.wolfNo || (isSelf ? '我' : `行者${message.senderId}`)
      const senderInitial = senderName ? senderName.charAt(0) : (isSelf ? '我' : '行')
      const senderAvatar = sender.avatar || ''

      if (!currentGroup || Number(currentGroup.senderId) !== Number(message.senderId)) {
        currentGroup = {
          type: 'group',
          key: `g_${message.messageId || index}`,
          senderId: message.senderId,
          isSelf,
          senderName,
          senderInitial,
          senderAvatar,
          headerTimeText: time.formatMessageMetaTime(message.createTime),
          rows: []
        }
        blocks.push(currentGroup)
      }

      currentGroup.rows.push({
        key: `m_${message.messageId || index}`,
        messageId: message.messageId,
        content: message.content
      })
      prevDateLabel = currentDateLabel
    }
    return blocks
  },

  applyTheme() {
    applyPageTheme(this)
  },

  onClickMore() {
    wx.showActionSheet({
      itemList: ['从相册选择图片', '从相册选择视频'],
      success: (res) => {
        if (res.tapIndex === 0) {
          this.chooseMediaFromAlbum('image', 9, (count) => `已选择 ${count} 张图片，发送功能待实现`)
          return
        }
        if (res.tapIndex === 1) {
          this.chooseMediaFromAlbum('video', 1, () => '已选择视频，发送功能待实现')
        }
      }
    })
  },

  chooseMediaFromAlbum(mediaType, count, messageBuilder) {
    wx.chooseMedia({
      count,
      mediaType: [mediaType],
      sourceType: ['album'],
      success: (res) => {
        const fileCount = Array.isArray(res.tempFiles) ? res.tempFiles.length : 0
        if (fileCount > 0) {
          toastSuccess(messageBuilder(fileCount))
        }
      },
      fail: (err) => {
        if (err && err.errMsg && err.errMsg.includes('cancel')) {
          return
        }
        toastError('打开相册失败')
      }
    })
  },

  scrollToBottom() {
    this.setData({
      scrollTop: 999999
    })
  },

  onMessageListLower() {
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
