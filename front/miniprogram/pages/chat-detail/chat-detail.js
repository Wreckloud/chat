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
const chatPolicyHelper = require('../../utils/chat-policy-helper')
const { resolveDisplayName, attachDisplayTitle } = require('../../utils/title')
const { refreshChatUnreadBadge } = require('../../utils/tab-badge')
const buildCommonImPageMethods = require('../../utils/im-page-methods')

const commonImPageMethods = buildCommonImPageMethods({
  imPageHelper,
  imSendHelper,
  imHelper,
  toastError,
  uploadImage: uploadChatImage,
  uploadVideo: uploadChatVideo,
  uploadFile: uploadChatFile
})

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
    canSendText: false,
    targetUser: {},
    themeClass: 'theme-retro-blue',
    morePanelVisible: false,
    moreActions: imSendHelper.DEFAULT_MORE_ACTIONS,
    messagePolicy: null,
    systemNoticeBlocks: [],
    replyDraft: null
  },

  SEND_TIMEOUT_MS: imConfig.SEND_TIMEOUT_MS,
  WS_READY_TIMEOUT_MS: imConfig.WS_READY_TIMEOUT_MS,
  CLIENT_MSG_ID_PREFIX: 'c',
  MESSAGE_MERGE_GAP_MS: imHelper.DEFAULT_MESSAGE_MERGE_GAP_MS,
  MARK_READ_MIN_INTERVAL_MS: 1500,
  IM_SEND_TYPE: 'SEND',

  onLoad(options) {
    imPageHelper.handlePageLoad(this, auth, {
      beforeInit: () => {
        const conversationId = Number(options.conversationId)
        if (!conversationId) {
          toastError('会话ID不能为空')
          setTimeout(() => {
            wx.navigateBack()
          }, 1500)
          return false
        }
        this.setData({ conversationId })
        return true
      },
      afterInit: () => {
        imUserHelper.initCurrentUserContext(this, auth, normalizeUser, {
          enableLoadingUserGuard: true
        })
        this.applyMessagePolicy(null)
        this.markConversationRead(true)
        this.loadCurrentUserProfile()
        this.loadConversation()
        this.loadMessagePolicy()
        this.loadMessages()
        this.initSocket()
      }
    })
  },

  onReady() {
    imPageHelper.handlePageReady(this)
  },

  onShow() {
    imPageHelper.handlePageShow(this, auth, {
      beforeShow: () => this.applyTheme(),
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
          avatar: conversation.targetAvatar,
          equippedTitleName: conversation.targetEquippedTitleName,
          equippedTitleColor: conversation.targetEquippedTitleColor
        }) || {}
        const targetUser = this.attachUserDisplayFields(normalized)
        this.cacheUserProfile(targetUser)

        this.setData({
          targetUser,
          messageBlocks: this.buildMessageBlocks(this.data.messages)
        })

        wx.setNavigationBarTitle({
          title: resolveDisplayName(targetUser)
        })

        this.ensureUserProfileById(targetUser.userId)
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
      cacheUserProfile: (user) => this.cacheUserProfile(user)
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

  loadMessagePolicy() {
    if (!this.data.conversationId) {
      return Promise.resolve()
    }
    return request.get(`/conversations/${this.data.conversationId}/messages/policy`)
      .then(res => {
        this.applyMessagePolicy(res.data || null)
      })
      .catch(() => {})
  },

  applyMessagePolicy(policy) {
    const systemNoticeBlocks = this.buildSystemNoticeBlocks(policy)
    const messageBlocks = this.buildMessageBlocks(this.data.messages, systemNoticeBlocks)
    this.setData({
      messagePolicy: policy,
      systemNoticeBlocks,
      messageBlocks
    })
  },

  ...commonImPageMethods,

  async sendMessage() {
    return imPageHelper.sendComposerTextMessage(this, imSendHelper, {
      buildExtraPayload: () => this.buildReplyPayload(),
      onError: (error) => {
        toastError(error, '发送失败')
      },
      onSuccess: () => {
        this.clearReplyDraft()
        if (!this.data.messagePolicy || !this.data.messagePolicy.canSendFreely) {
          this.loadMessagePolicy()
        }
      }
    })
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
      imHelper.resolvePendingByEchoMessage(this, payload.data)
      this.appendMessage(payload.data)
      if (payload.data && Number(payload.data.senderId) !== Number(this.currentUserId)) {
        this.markConversationRead()
        if (!this.data.messagePolicy || !this.data.messagePolicy.canSendFreely) {
          this.loadMessagePolicy()
        }
      }
    }
  },

  appendMessage(message) {
    imMessageHelper.appendMessage(this, message, {
      isValidMessage: (current) => Boolean(current) && Number(current.conversationId) === Number(this.data.conversationId),
      isDuplicate: (prev, current) => Number(prev.messageId) === Number(current.messageId),
      appendMessageBlock: (payload) => this.appendMessageBlock(payload),
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

  handleLongPressUserMeta(e) {
    const dataset = e && e.currentTarget ? (e.currentTarget.dataset || {}) : {}
    const isSelf = Number(dataset.isSelf) === 1
    if (isSelf) {
      return
    }
    const userName = String(dataset.userName || '').trim()
    if (!userName) {
      return
    }
    const origin = String(this.data.inputMessage || '')
    const separator = origin && !/\s$/.test(origin) ? ' ' : ''
    const nextInput = `${origin}${separator}@${userName} `
    this.setData({
      inputMessage: nextInput,
      canSendText: nextInput.trim().length > 0,
      composerFocused: true
    })
    if (this.data.morePanelVisible) {
      this.setMorePanelVisible(false)
    }
    imHelper.refocusComposerInput(this)
  },

  handleLongPressMessageRow(e) {
    const dataset = e && e.currentTarget ? (e.currentTarget.dataset || {}) : {}
    const messageId = Number(dataset.messageId)
    if (!messageId) {
      imPageHelper.onLongPressMessage(this, e)
      return
    }

    const copyContent = String(dataset.copy || '').trim()
    const actionList = copyContent
      ? ['回复该消息', '复制内容']
      : ['回复该消息']

    wx.showActionSheet({
      itemList: actionList,
      success: ({ tapIndex }) => {
        if (tapIndex === 0) {
          this.startReplyDraft(dataset)
          return
        }
        if (tapIndex === 1 && copyContent) {
          wx.setClipboardData({ data: copyContent })
        }
      }
    })
  },

  startReplyDraft(dataset) {
    const replyDraft = chatPolicyHelper.buildReplyDraft(dataset)
    if (!replyDraft) {
      return
    }
    this.setData({
      replyDraft,
      composerFocused: true
    })
    if (this.data.morePanelVisible) {
      this.setMorePanelVisible(false)
    }
    imHelper.refocusComposerInput(this)
  },

  clearReplyDraft() {
    if (!this.data.replyDraft) {
      return
    }
    this.setData({ replyDraft: null })
  },

  buildReplyPayload() {
    return chatPolicyHelper.buildReplyPayload(this.data.replyDraft)
  },

  buildMessageBlocks(messages, systemNoticeBlocks = this.data.systemNoticeBlocks) {
    const messageBlocks = imHelper.buildMessageBlocks(messages, {
      currentUserId: this.currentUserId,
      messageMergeGapMs: this.MESSAGE_MERGE_GAP_MS,
      cacheUserProfile: (user) => this.cacheUserProfile(user),
      resolveSenderProfile: (senderId, isSelf) => this.resolveSenderProfile(senderId, isSelf)
    })
    return this.prependSystemNoticeBlocks(messageBlocks, systemNoticeBlocks)
  },

  appendMessageBlock({ messageBlocks, message, previousMessage, messageIndex }) {
    const pureBlocks = this.stripSystemNoticeBlocks(messageBlocks)
    const nextBlocks = imHelper.appendMessageBlock(pureBlocks, message, {
      currentUserId: this.currentUserId,
      messageMergeGapMs: this.MESSAGE_MERGE_GAP_MS,
      cacheUserProfile: (user) => this.cacheUserProfile(user),
      resolveSenderProfile: (senderId, isSelf) => this.resolveSenderProfile(senderId, isSelf),
      previousMessage,
      messageIndex
    })
    return this.prependSystemNoticeBlocks(nextBlocks)
  },

  buildSystemNoticeBlocks(policy) {
    return chatPolicyHelper.buildSystemNoticeBlocks(policy)
  },

  prependSystemNoticeBlocks(messageBlocks, systemNoticeBlocks = this.data.systemNoticeBlocks) {
    return chatPolicyHelper.prependSystemNoticeBlocks(systemNoticeBlocks, messageBlocks)
  },

  stripSystemNoticeBlocks(messageBlocks) {
    return chatPolicyHelper.stripSystemNoticeBlocks(messageBlocks)
  },

  resolveSenderProfile(senderId, isSelf) {
    return imUserHelper.resolveSenderProfile(this, senderId, isSelf, {
      targetUser: this.data.targetUser
    })
  },

  cacheUserProfile(user) {
    imUserHelper.cacheUserProfile(this, normalizeUser, this.attachUserDisplayFields(user))
  },

  ensureSenderProfiles(messages) {
    imUserHelper.ensureSenderProfiles(this, request, normalizeUser, messages, {
      fetchMissing: true,
      onLoaded: (normalized) => this.applyLoadedUserProfile(normalized)
    })
  },

  ensureUserProfileById(userId) {
    return imUserHelper.ensureUserProfileById(
      this,
      request,
      normalizeUser,
      userId,
      {
        onLoaded: (normalized) => this.applyLoadedUserProfile(normalized)
      }
    )
  },

  applyLoadedUserProfile(normalized) {
    if (!normalized || !normalized.userId) {
      return
    }
    const normalizedUser = this.attachUserDisplayFields(normalized)

    const updates = {}
    if (Number(this.currentUserId) === Number(normalizedUser.userId)) {
      this.currentUser = normalizedUser
    }

    if (this.data.targetUser && Number(this.data.targetUser.userId) === Number(normalizedUser.userId)) {
      updates.targetUser = normalizedUser
      wx.setNavigationBarTitle({
        title: resolveDisplayName(normalizedUser)
      })
    }

    if (this.data.messages.length > 0) {
      updates.messageBlocks = this.buildMessageBlocks(this.data.messages)
    }
    if (Object.keys(updates).length > 0) {
      this.setData(updates)
    }
  },

  attachUserDisplayFields(user) {
    const normalized = normalizeUser(user) || {}
    return attachDisplayTitle(
      normalized,
      normalized.equippedTitleName,
      normalized.equippedTitleColor
    )
  },

  applyTheme() {
    applyPageTheme(this)
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
      .then(() => refreshChatUnreadBadge())
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


