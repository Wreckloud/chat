const request = require('../../utils/request')
const auth = require('../../utils/auth')
const ws = require('../../utils/ws')
const { uploadChatImage, uploadChatVideo, uploadChatFile } = require('../../utils/oss')
const { normalizeUser, openUserProfile } = require('../../utils/user')
const { toastError } = require('../../utils/ui')
const { applyPageTheme } = require('../../utils/page-theme')
const imHelper = require('../../utils/im-helper')
const imSendHelper = require('../../utils/im-send-helper')
const imPageHelper = require('../../utils/im-page-helper')
const imUserHelper = require('../../utils/im-user-helper')
const imWsHelper = require('../../utils/im-ws-helper')
const imMessageHelper = require('../../utils/im-message-helper')
const imConfig = require('../../utils/im-config')

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
    scrollIntoView: '',
    composerFocused: false,
    keyboardHeightPx: 0,
    dockHeightPx: 88,
    messageListBottomPx: 88,
    connectionTip: '',
    sendStatusText: '',
    targetUser: {},
    themeClass: 'theme-retro-blue',
    morePanelVisible: false,
    moreActions: imSendHelper.DEFAULT_MORE_ACTIONS
  },

  SEND_TIMEOUT_MS: imConfig.SEND_TIMEOUT_MS,
  WS_READY_TIMEOUT_MS: imConfig.WS_READY_TIMEOUT_MS,
  CLIENT_MSG_ID_PREFIX: 'c',
  MESSAGE_MERGE_GAP_MS: imHelper.DEFAULT_MESSAGE_MERGE_GAP_MS,
  MARK_READ_MIN_INTERVAL_MS: 1500,
  IM_SEND_TYPE: 'SEND',

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

    imUserHelper.initCurrentUserContext(this, auth, normalizeUser, {
      enableLoadingUserGuard: true
    })

    this.setData({
      conversationId
    })

    this.markConversationRead(true)
    this.loadCurrentUserProfile()
    this.loadConversation()
    this.loadMessages()
    this.initSocket()
  },

  onReady() {
    imPageHelper.handlePageReady(this)
  },

  onShow() {
    imPageHelper.handlePageShow(this, auth, {
      applyTheme: () => this.applyTheme(),
      afterShow: () => this.markConversationRead(true)
    })
  },

  onUnload() {
    imPageHelper.cleanupPage(this, ws, {
      beforeTeardown: () => {
        if (this.markReadThrottleTimer) {
          clearTimeout(this.markReadThrottleTimer)
          this.markReadThrottleTimer = null
        }
      }
    })
  },

  initSocket() {
    imPageHelper.initSocket(this, ws, payload => this.handleWsMessage(payload))
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
        this.cacheUserProfile(normalized)

        this.setData({
          targetUser: normalized,
          messageBlocks: this.buildMessageBlocks(this.data.messages)
        })

        wx.setNavigationBarTitle({
          title: normalized.nickname || normalized.wolfNo || '聊天'
        })

        this.ensureUserProfileById(normalized.userId)
      })
      .catch(err => {
        toastError(err, '加载失败')
      })
  },

  loadCurrentUserProfile() {
    return imPageHelper.loadCurrentUserProfile(this, {
      loadFn: (onLoaded) => imUserHelper.loadCurrentUserProfile(
        this,
        request,
        normalizeUser,
        onLoaded
      ),
      cacheUserProfile: (user) => this.cacheUserProfile(user),
      buildMessageBlocks: (messages) => this.buildMessageBlocks(messages)
    })
  },

  loadMessages() {
    return imPageHelper.loadMessages(this, request, {
      url: `/conversations/${this.data.conversationId}/messages`,
      onLoaded: ({ records }) => {
        if (records.length > 0) {
          this.markConversationRead()
        }
      },
      onError: (err) => {
        toastError(err, '加载失败')
      }
    })
  },

  onMessageInput(e) {
    imPageHelper.onMessageInput(this, e)
  },

  onKeyboardHeightChange(e) {
    imPageHelper.onKeyboardHeightChange(this, e)
  },

  onComposerFocus(e) {
    imPageHelper.onComposerFocus(this, e)
  },

  onComposerBlur() {
    imPageHelper.onComposerBlur(this)
  },

  onSendButtonTap() {
    imPageHelper.onSendButtonTap(this, () => this.sendMessage())
  },

  onSendStatusTap() {
    imPageHelper.onSendStatusTap(this, () => this.sendMessage())
  },

  async sendMessage() {
    return imPageHelper.sendComposerTextMessage(this, imSendHelper, toastError)
  },

  handleWsMessage(payload) {
    const commonHandled = imWsHelper.handleCommonPayload(this, payload, {
      toastError,
      consumeWhenResolved: false,
      onAckMessage: message => this.appendMessage(message)
    })
    if (commonHandled.handled) {
      return
    }

    if (commonHandled.payloadType === 'MESSAGE') {
      this.appendMessage(payload.data)
      if (payload.data && Number(payload.data.senderId) !== Number(this.currentUserId)) {
        this.markConversationRead()
      }
    }
  },

  appendMessage(message) {
    imMessageHelper.appendMessage(this, message, {
      isValidMessage: (current) => Boolean(current) && Number(current.conversationId) === Number(this.data.conversationId),
      isDuplicate: (prev, current) => Number(prev.messageId) === Number(current.messageId),
      buildMessageBlocks: (messages) => this.buildMessageBlocks(messages),
      afterAppend: (current) => {
        this.ensureUserProfileById(current.senderId)
        this.scrollToBottom()
      }
    })
  },

  goUserProfile() {
    const user = this.data.targetUser
    if (!user || !user.userId) return
    openUserProfile(user)
  },

  onTapUserElement(e) {
    imUserHelper.openUserProfileByTapEvent(e, openUserProfile)
  },

  buildMessageBlocks(messages) {
    return imHelper.buildMessageBlocks(messages, {
      currentUserId: this.currentUserId,
      messageMergeGapMs: this.MESSAGE_MERGE_GAP_MS,
      cacheUserProfile: (user) => this.cacheUserProfile(user),
      resolveSenderProfile: (senderId, isSelf) => this.resolveSenderProfile(senderId, isSelf)
    })
  },

  resolveSenderProfile(senderId, isSelf) {
    return imUserHelper.resolveSenderProfile(this, senderId, isSelf, {
      targetUser: this.data.targetUser
    })
  },

  cacheUserProfile(user) {
    imUserHelper.cacheUserProfile(this, normalizeUser, user)
  },

  ensureSenderProfiles(messages) {
    imUserHelper.ensureSenderProfiles(this, request, normalizeUser, messages, {
      fetchMissing: true,
      onLoaded: (normalized) => {
        const updates = {}
        if (Number(this.currentUserId) === Number(normalized.userId)) {
          this.currentUser = normalized
        }

        if (this.data.targetUser && Number(this.data.targetUser.userId) === Number(normalized.userId)) {
          updates.targetUser = normalized
          wx.setNavigationBarTitle({
            title: normalized.nickname || normalized.wolfNo || '聊天'
          })
        }

        if (this.data.messages.length > 0) {
          updates.messageBlocks = this.buildMessageBlocks(this.data.messages)
        }
        if (Object.keys(updates).length > 0) {
          this.setData(updates)
        }
      }
    })
  },

  ensureUserProfileById(userId) {
    return imUserHelper.ensureUserProfileById(
      this,
      request,
      normalizeUser,
      userId,
      {
        onLoaded: (normalized) => {
          const updates = {}
          if (Number(this.currentUserId) === Number(normalized.userId)) {
            this.currentUser = normalized
          }

          if (this.data.targetUser && Number(this.data.targetUser.userId) === Number(normalized.userId)) {
            updates.targetUser = normalized
            wx.setNavigationBarTitle({
              title: normalized.nickname || normalized.wolfNo || '聊天'
            })
          }

          if (this.data.messages.length > 0) {
            updates.messageBlocks = this.buildMessageBlocks(this.data.messages)
          }
          if (Object.keys(updates).length > 0) {
            this.setData(updates)
          }
        }
      }
    )
  },

  applyTheme() {
    applyPageTheme(this)
  },

  setMorePanelVisible(visible) {
    imPageHelper.setMorePanelVisible(this, visible)
  },

  onClickMore() {
    imPageHelper.toggleMorePanel(this)
  },

  async onMoreActionTap(e) {
    await imPageHelper.onDefaultMoreActionTap(this, e, imSendHelper, {
      uploadImage: uploadChatImage,
      uploadVideo: uploadChatVideo,
      uploadFile: uploadChatFile
    }, toastError)
  },

  onTapLink(e) {
    imSendHelper.onTapLink(e)
  },

  onTapVideo(e) {
    imSendHelper.onTapVideo(this, e, toastError)
  },

  onTapFile(e) {
    imSendHelper.onTapFile(this, e, toastError)
  },

  scrollToBottom() {
    imHelper.scrollToBottom(this)
  },

  onMessageListUpper() {
    this.loadMessages()
  },

  previewImage(e) {
    imPageHelper.previewImage(this, e)
  },

  markConversationRead(force = false) {
    if (!this.data.conversationId) {
      return Promise.resolve()
    }
    const now = Date.now()
    const elapsed = now - (this.lastMarkReadAt || 0)

    if (!force && elapsed < this.MARK_READ_MIN_INTERVAL_MS) {
      this.markReadPending = true
      if (!this.markReadThrottleTimer) {
        const delay = this.MARK_READ_MIN_INTERVAL_MS - elapsed
        this.markReadThrottleTimer = setTimeout(() => {
          this.markReadThrottleTimer = null
          if (this.markReadPending) {
            this.markReadPending = false
            this.markConversationRead(true)
          }
        }, delay)
      }
      return Promise.resolve()
    }

    // 合并短时间重复已读请求，避免连续拉取/收消息时放大请求量。
    if (this.markReadLoading) {
      this.markReadPending = true
      return Promise.resolve()
    }

    this.lastMarkReadAt = now
    this.markReadLoading = true
    return request.put(`/conversations/${this.data.conversationId}/read`)
      .catch(() => {})
      .finally(() => {
        this.markReadLoading = false
        if (this.markReadPending) {
          this.markReadPending = false
          this.markConversationRead(true)
        }
      })
  },

  async sendWsMessageWithAck(payload, options = {}) {
    return imHelper.sendWsMessageWithAck(this, ws, payload, options)
  }

})


