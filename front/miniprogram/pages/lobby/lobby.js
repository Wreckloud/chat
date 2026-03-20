const request = require('../../utils/request')
const auth = require('../../utils/auth')
const time = require('../../utils/time')
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
const lobbyMetaHelper = require('../../utils/lobby-meta-helper')
const imConfig = require('../../utils/im-config')
const chatPolicyHelper = require('../../utils/chat-policy-helper')
const replyMentionHelper = require('../../utils/reply-mention-helper')
const buildCommonImPageMethods = require('../../utils/im-page-methods')
const { COMMON_KAOMOJI_LIST } = require('../../utils/kaomoji')

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
    themeClass: 'theme-retro-blue',
    morePanelVisible: false,
    emojiPanelVisible: false,
    emojiList: COMMON_KAOMOJI_LIST,
    moreActions: imSendHelper.DEFAULT_MORE_ACTIONS,
    replyDraft: null,
    highlightMessageId: 0,
    lobbyOnlineCount: 0,
    lobbyActiveText: '最近活跃 --',
    recentUsersText: '最近在线 --'
  },

  SEND_TIMEOUT_MS: imConfig.SEND_TIMEOUT_MS,
  WS_READY_TIMEOUT_MS: imConfig.WS_READY_TIMEOUT_MS,
  CLIENT_MSG_ID_PREFIX: 'l',
  MESSAGE_MERGE_GAP_MS: imHelper.DEFAULT_MESSAGE_MERGE_GAP_MS,
  RECALL_WINDOW_MS: 5 * 60 * 1000,
  MESSAGE_HIGHLIGHT_MS: 1600,
  IM_SEND_TYPE: 'LOBBY_SEND',

  onLoad() {
    imPageHelper.handlePageLoad(this, auth, {
      afterInit: () => {
        imUserHelper.initCurrentUserContext(this, auth, normalizeUser)
        this.loadCurrentUserProfile()
        this.loadLobbyMeta()
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
      afterShow: () => {
        this.loadLobbyMeta()
        this.startLobbyMetaPolling()
      }
    })
  },

  onHide() {
    this.stopLobbyMetaPolling()
    this.clearLobbyMetaTimer()
  },

  onUnload() {
    imPageHelper.cleanupPage(this, ws, {
      beforeTeardown: () => {
        this.stopLobbyMetaPolling()
        this.clearLobbyMetaTimer()
        if (this.highlightTimer) {
          clearTimeout(this.highlightTimer)
          this.highlightTimer = null
        }
      }
    })
  },

  initSocket() {
    imPageHelper.initSocket(this, ws, payload => this.handleWsMessage(payload))
  },

  loadLobbyMeta() {
    return lobbyMetaHelper.loadLobbyMeta(this, request, (meta) => {
        const onlineCount = Number(meta.onlineCount) || 0
        const latestMessageAt = meta.latestMessageAt || ''
        const recentUsers = Array.isArray(meta.recentUsers) ? meta.recentUsers : []
        recentUsers.forEach(item => this.cacheUserProfile(item))
        const lobbyActiveText = this.buildLobbyActiveText(latestMessageAt)
        const recentUsersText = this.buildRecentUsersText(recentUsers)

        if (onlineCount === this.data.lobbyOnlineCount
          && lobbyActiveText === this.data.lobbyActiveText
          && recentUsersText === this.data.recentUsersText) {
          return
        }

        this.setData({
          lobbyOnlineCount: onlineCount,
          lobbyActiveText,
          recentUsersText
        })
      })
  },

  buildLobbyActiveText(latestMessageAt) {
    return lobbyMetaHelper.buildLobbyActiveText(latestMessageAt, time)
  },

  buildRecentUsersText(recentUsers) {
    return lobbyMetaHelper.buildRecentUsersText(recentUsers, normalizeUser, time)
  },

  scheduleLobbyMetaRefresh() {
    // Presence 推送可能很密，短暂防抖后再拉一次 meta。
    lobbyMetaHelper.scheduleLobbyMetaRefresh(this, () => this.loadLobbyMeta(), 600)
  },

  startLobbyMetaPolling() {
    lobbyMetaHelper.startLobbyMetaPolling(this, () => this.loadLobbyMeta(), 10000)
  },

  stopLobbyMetaPolling() {
    lobbyMetaHelper.stopLobbyMetaPolling(this)
  },

  clearLobbyMetaTimer() {
    lobbyMetaHelper.clearLobbyMetaTimer(this)
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
      url: '/lobby/messages',
      onError: (err) => {
        toastError(err, '加载大厅消息失败')
      }
    })
  },

  ...commonImPageMethods,

  async sendMessage() {
    return imPageHelper.sendComposerTextMessage(this, imSendHelper, {
      buildExtraPayload: () => this.buildReplyPayload(),
      onError: (error) => {
        toastError(error, '发送消息失败')
      },
      onSuccess: () => {
        this.clearReplyDraft()
      }
    })
  },

  handleWsMessage(payload) {
    const commonHandled = imWsHelper.handleCommonPayload(this, payload, {
      toastError,
      onAckMessage: message => this.appendMessage(message),
      onAfterAck: () => this.scheduleLobbyMetaRefresh()
    })
    if (commonHandled.handled) {
      return
    }

    if (commonHandled.payloadType === 'PRESENCE') {
      this.scheduleLobbyMetaRefresh()
      return
    }

    if (commonHandled.payloadType === 'LOBBY_MESSAGE') {
      imHelper.resolvePendingByEchoMessage(this, payload.data)
      this.appendMessage(payload.data)
      this.scheduleLobbyMetaRefresh()
      return
    }

    if (commonHandled.payloadType === 'UPLOAD_PROGRESS') {
      imSendHelper.applyUploadProgressPayload(this, payload.data)
    }
  },

  appendMessage(message) {
    const normalizedMessage = { ...(message || {}) }
    if (Number(normalizedMessage.messageId) > 0) {
      normalizedMessage.uploadStatus = ''
      normalizedMessage.uploadProgress = 100
    }
    imMessageHelper.appendMessage(this, normalizedMessage, {
      isValidMessage: (current) => {
        if (!current) {
          return false
        }
        const messageId = Number(current.messageId)
        const clientMsgId = String(current.clientMsgId || '').trim()
        return messageId > 0 || Boolean(clientMsgId)
      },
      isDuplicate: (prev, current) => {
        const prevMessageId = Number(prev.messageId)
        const currentMessageId = Number(current.messageId)
        if (prevMessageId > 0 && currentMessageId > 0 && prevMessageId === currentMessageId) {
          return true
        }
        const prevClientMsgId = String(prev.clientMsgId || '').trim()
        const currentClientMsgId = String(current.clientMsgId || '').trim()
        return Boolean(prevClientMsgId) && prevClientMsgId === currentClientMsgId
      },
      upsertOnDuplicate: true,
      buildMessageBlocks: (messages) => this.buildMessageBlocks(messages),
      appendMessageBlock: (payload) => this.appendMessageBlock(payload),
      beforeAppend: (current) => {
        const senderProfile = {
          userId: current.senderId,
          wolfNo: current.senderWolfNo,
          nickname: current.senderNickname,
          avatar: current.senderAvatar
        }
        if (Object.prototype.hasOwnProperty.call(current, 'senderEquippedTitleName')) {
          senderProfile.equippedTitleName = current.senderEquippedTitleName
        }
        if (Object.prototype.hasOwnProperty.call(current, 'senderEquippedTitleColor')) {
          senderProfile.equippedTitleColor = current.senderEquippedTitleColor
        }
        this.cacheUserProfile(senderProfile)
      },
      afterAppend: (current, messages, meta) => {
        if (!meta || meta.updated !== true) {
          this.scrollToBottom()
        }
      }
    })
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
    const actionList = [{ key: 'reply', label: '回复该消息' }]
    if (copyContent) {
      actionList.push({ key: 'copy-all', label: '复制全部' })
    }
    if (this.canRecallMessage(dataset)) {
      actionList.push({ key: 'recall', label: '撤回消息' })
    }

    wx.showActionSheet({
      itemList: actionList.map(item => item.label),
      success: ({ tapIndex }) => {
        const action = actionList[tapIndex]
        if (!action) {
          return
        }
        if (action.key === 'reply') {
          this.startReplyDraft(dataset)
          return
        }
        if (action.key === 'copy-all' && copyContent) {
          wx.setClipboardData({ data: copyContent })
          return
        }
        if (action.key === 'recall') {
          this.recallMessage(dataset)
        }
      }
    })
  },

  startReplyDraft(dataset) {
    const replyDraft = chatPolicyHelper.buildReplyDraft(dataset)
    if (!replyDraft) {
      return
    }
    const nextInput = replyMentionHelper.prependMention(this.data.inputMessage, replyDraft.mentionName)
    this.setData({
      replyDraft,
      inputMessage: nextInput,
      canSendText: nextInput.trim().length > 0,
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
    const nextInput = replyMentionHelper.removeLeadingMention(
      this.data.inputMessage,
      this.data.replyDraft.mentionName
    )
    this.setData({
      replyDraft: null,
      inputMessage: nextInput,
      canSendText: nextInput.trim().length > 0
    })
  },

  buildReplyPayload() {
    return chatPolicyHelper.buildReplyPayload(this.data.replyDraft)
  },

  handleTapReplyQuote(e) {
    const dataset = e && e.currentTarget ? (e.currentTarget.dataset || {}) : {}
    const targetDisabled = Number(dataset.replyTargetDisabled) === 1
    if (targetDisabled) {
      return
    }
    const targetMessageId = Number(dataset.replyTargetId)
    if (!targetMessageId) {
      return
    }
    this.jumpToMessage(targetMessageId)
  },

  hasMessage(messageId) {
    if (!messageId) {
      return false
    }
    return (this.data.messages || []).some(item => Number(item.messageId) === Number(messageId))
  },

  findMessageById(messageId) {
    if (!messageId) {
      return null
    }
    return (this.data.messages || []).find(item => Number(item.messageId) === Number(messageId)) || null
  },

  isMessageRecalled(messageId) {
    const target = this.findMessageById(messageId)
    if (!target) {
      return false
    }
    return String(target.msgType || '').toUpperCase() === 'RECALL'
  },

  async jumpToMessage(messageId) {
    if (!messageId) {
      return
    }
    let loaded = this.hasMessage(messageId)
    let loopCount = 0
    while (!loaded && this.data.hasMore && loopCount < 20) {
      await this.loadMessages()
      loaded = this.hasMessage(messageId)
      loopCount += 1
    }
    if (!loaded) {
      return
    }
    if (this.isMessageRecalled(messageId)) {
      return
    }
    this.scrollToMessageAnchor(messageId)
  },

  scrollToMessageAnchor(messageId) {
    const anchorId = `im-msg-${messageId}`
    this.setData({ scrollIntoView: '' }, () => {
      this.setData({
        scrollIntoView: anchorId,
        highlightMessageId: Number(messageId)
      })
      if (this.highlightTimer) {
        clearTimeout(this.highlightTimer)
      }
      this.highlightTimer = setTimeout(() => {
        this.highlightTimer = null
        if (Number(this.data.highlightMessageId) !== Number(messageId)) {
          return
        }
        this.setData({ highlightMessageId: 0 })
      }, this.MESSAGE_HIGHLIGHT_MS)
    })
  },

  canRecallMessage(dataset) {
    const data = dataset || {}
    if (!Number(data.messageId) || Number(data.isSelf) !== 1 || Number(data.recalled) === 1) {
      return false
    }
    const createTime = time.parseDateTime(data.createTime)
    if (!createTime) {
      return false
    }
    const elapsed = Date.now() - createTime.getTime()
    if (!Number.isFinite(elapsed)) {
      return false
    }
    return elapsed <= this.RECALL_WINDOW_MS
  },

  async recallMessage(dataset) {
    const messageId = Number(dataset && dataset.messageId)
    if (!messageId || this.data.sending) {
      return
    }
    this.setData({ sending: true })
    try {
      await this.sendWsMessageWithAck({
        type: 'LOBBY_RECALL',
        messageId
      })
    } catch (error) {
      toastError(error, '撤回消息失败')
    } finally {
      if (!this.pageUnloaded) {
        this.setData({ sending: false })
      }
    }
  },

  buildMessageBlocks(messages) {
    return imHelper.buildMessageBlocks(messages, {
      currentUserId: this.currentUserId,
      messageMergeGapMs: this.MESSAGE_MERGE_GAP_MS,
      cacheUserProfile: (user) => this.cacheUserProfile(user),
      resolveSenderProfile: (senderId, isSelf) => this.resolveSenderProfile(senderId, isSelf)
    })
  },

  appendMessageBlock({ messageBlocks, message, previousMessage, messageIndex }) {
    return imHelper.appendMessageBlock(messageBlocks, message, {
      currentUserId: this.currentUserId,
      messageMergeGapMs: this.MESSAGE_MERGE_GAP_MS,
      cacheUserProfile: (user) => this.cacheUserProfile(user),
      resolveSenderProfile: (senderId, isSelf) => this.resolveSenderProfile(senderId, isSelf),
      previousMessage,
      messageIndex
    })
  },

  resolveSenderProfile(senderId, isSelf) {
    return imUserHelper.resolveSenderProfile(this, senderId, isSelf)
  },

  cacheUserProfile(user) {
    imUserHelper.cacheUserProfile(this, normalizeUser, user)
  },

  ensureSenderProfiles(messages) {
    imUserHelper.ensureSenderProfiles(this, request, normalizeUser, messages)
  },

  applyTheme() {
    applyPageTheme(this)
  },

  async sendWsMessageWithAck(payload, options = {}) {
    return imHelper.sendWsMessageWithAck(this, ws, payload, options)
  },

  sendWsUploadProgress(payload) {
    const clientMsgId = String(payload && payload.clientMsgId ? payload.clientMsgId : '').trim()
    if (!clientMsgId) {
      return
    }
    ws.send({
      type: 'UPLOAD_PROGRESS',
      sendType: String(this.IM_SEND_TYPE || 'LOBBY_SEND').toUpperCase(),
      clientMsgId,
      msgType: payload.msgType,
      uploadProgress: payload.uploadProgress,
      uploadStatus: payload.uploadStatus
    })
  }

})



