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
    connectionTip: '',
    sendStatusText: '',
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
    if (!auth.requireLogin()) return

    imUserHelper.initCurrentUserContext(this, auth, normalizeUser)

    this.loadCurrentUserProfile()
    this.loadLobbyMeta()
    this.loadMessages()
    this.initSocket()
  },

  onReady() {
    imPageHelper.handlePageReady(this)
  },

  onShow() {
    imPageHelper.handlePageShow(this, auth, {
      applyTheme: () => this.applyTheme(),
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

  teardownSocket() {
    imPageHelper.teardownSocket(this, ws)
  },

  loadLobbyMeta() {
    return lobbyMetaHelper.loadLobbyMeta(this, request, (meta) => {
        const onlineCount = Number(meta.onlineCount) || 0
        const latestActiveAt = meta.latestActiveAt || ''
        const recentUsers = Array.isArray(meta.recentUsers) ? meta.recentUsers : []
        recentUsers.forEach(item => this.cacheUserProfile(item))

        this.setData({
          lobbyOnlineCount: onlineCount,
          lobbyActiveText: this.buildLobbyActiveText(latestActiveAt),
          recentUsersText: this.buildRecentUsersText(recentUsers)
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
    return imUserHelper.loadCurrentUserProfile(
      this,
      request,
      normalizeUser,
      user => {
        this.cacheUserProfile(user)
        if (this.data.messages.length > 0) {
          this.setData({
            messageBlocks: this.buildMessageBlocks(this.data.messages)
          })
        }
      }
    )
  },

  loadMessages() {
    return imMessageHelper.loadPagedMessages(this, request, {
      url: '/lobby/messages',
      parseRecords: (res) => {
        const pageData = res && res.data ? res.data : {}
        return Array.isArray(pageData.records) ? pageData.records : []
      },
      buildMessageBlocks: (messages) => this.buildMessageBlocks(messages),
      onLoaded: ({ messages }) => {
        this.ensureSenderProfiles(messages)
      },
      onFirstPageLoaded: () => {
        this.scrollToBottom()
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
    return imPageHelper.sendComposerText(
      this,
      (content) => imSendHelper.sendTextMessage(this, content, {
        clearInputOnSuccess: true
      }),
      toastError
    )
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
      this.appendMessage(payload.data)
      this.scheduleLobbyMetaRefresh()
    }
  },

  appendMessage(message) {
    imMessageHelper.appendMessage(this, message, {
      isValidMessage: (current) => Boolean(current && current.messageId),
      isDuplicate: (prev, current) => Number(prev.messageId) === Number(current.messageId),
      beforeAppend: (current) => this.cacheUserProfile({
        userId: current.senderId,
        wolfNo: current.senderWolfNo,
        nickname: current.senderNickname,
        avatar: current.senderAvatar
      }),
      buildMessageBlocks: (messages) => this.buildMessageBlocks(messages),
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

  resolveSenderProfile(senderId, isSelf) {
    return imUserHelper.resolveSenderProfile(this, senderId, isSelf)
  },

  cacheUserProfile(user) {
    imUserHelper.cacheUserProfile(this, normalizeUser, user)
  },

  ensureSenderProfiles(messages) {
    imUserHelper.cacheSenderProfilesFromMessages(messages, user => this.cacheUserProfile(user))
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
    await imSendHelper.onMoreActionTap(this, e, {
      image: () => imSendHelper.chooseImageFromAlbum(this, this.getImSendDeps()),
      video: () => imSendHelper.chooseVideoFromAlbum(this, this.getImSendDeps()),
      file: () => imSendHelper.chooseFileForShare(this, this.getImSendDeps()),
      link: () => imSendHelper.shareLinkAsText(this, this.getImSendDeps())
    })
  },

  getImSendDeps() {
    return imPageHelper.getSendDeps(this, () => ({
      uploadImage: uploadChatImage,
      uploadVideo: uploadChatVideo,
      uploadFile: uploadChatFile,
      toastError
    }))
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

  async sendWsMessageWithAck(payload, options = {}) {
    return imHelper.sendWsMessageWithAck(this, ws, payload, options)
  }

})


