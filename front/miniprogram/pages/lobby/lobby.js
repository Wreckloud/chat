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
    moreActions: imSendHelper.DEFAULT_MORE_ACTIONS,
    lobbyOnlineCount: 0,
    lobbyActiveText: '最近活跃 --',
    recentUsersText: '最近在线 --'
  },

  SEND_TIMEOUT_MS: imConfig.SEND_TIMEOUT_MS,
  WS_READY_TIMEOUT_MS: imConfig.WS_READY_TIMEOUT_MS,
  CLIENT_MSG_ID_PREFIX: 'l',
  MESSAGE_MERGE_GAP_MS: imHelper.DEFAULT_MESSAGE_MERGE_GAP_MS,
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
      afterShow: () => this.loadLobbyMeta()
    })
  },

  onUnload() {
    imPageHelper.cleanupPage(this, ws, {
      beforeTeardown: () => {
        this.clearLobbyMetaTimer()
      }
    })
  },

  initSocket() {
    imPageHelper.initSocket(this, ws, payload => this.handleWsMessage(payload))
  },

  loadLobbyMeta() {
    return lobbyMetaHelper.loadLobbyMeta(this, request, (meta) => {
        const onlineCount = Number(meta.onlineCount) || 0
        const latestActiveAt = meta.latestActiveAt || ''
        const recentUsers = Array.isArray(meta.recentUsers) ? meta.recentUsers : []
        recentUsers.forEach(item => this.cacheUserProfile(item))
        const lobbyActiveText = this.buildLobbyActiveText(latestActiveAt)
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

  buildLobbyActiveText(latestActiveAt) {
    return lobbyMetaHelper.buildLobbyActiveText(latestActiveAt, time)
  },

  buildRecentUsersText(recentUsers) {
    return lobbyMetaHelper.buildRecentUsersText(recentUsers, normalizeUser, time)
  },

  scheduleLobbyMetaRefresh() {
    // Presence 推送可能很密，短暂防抖后再拉一次 meta。
    lobbyMetaHelper.scheduleLobbyMetaRefresh(this, () => this.loadLobbyMeta(), 600)
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
        toastError(err, '加载失败')
      }
    })
  },

  ...commonImPageMethods,

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
    }
  },

  appendMessage(message) {
    imMessageHelper.appendMessage(this, message, {
      isValidMessage: (current) => Boolean(current && current.messageId),
      isDuplicate: (prev, current) => Number(prev.messageId) === Number(current.messageId),
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
      afterAppend: () => this.scrollToBottom()
    })
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
  }

})


