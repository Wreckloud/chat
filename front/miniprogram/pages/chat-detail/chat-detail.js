const request = require('../../utils/request')
const auth = require('../../utils/auth')
const time = require('../../utils/time')
const ws = require('../../utils/ws')
const { uploadChatImage, uploadChatVideo, uploadChatFile } = require('../../utils/oss')
const { normalizeUser, openUserProfile } = require('../../utils/user')
const { toastError } = require('../../utils/ui')
const { applyPageTheme } = require('../../utils/page-theme')

const MORE_ACTIONS = [
  { key: 'image', label: '多图', icon: '图', subLabel: '相册' },
  { key: 'video', label: '视频', icon: '视', subLabel: '相册' },
  { key: 'file', label: '文件', icon: '档', subLabel: '本地' },
  { key: 'link', label: '链接', icon: '链', subLabel: '发送' }
]

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
    keyboardHeightPx: 0,
    dockHeightPx: 88,
    messageListBottomPx: 88,
    targetUser: {},
    themeClass: 'theme-retro-blue',
    morePanelVisible: false,
    moreActions: MORE_ACTIONS
  },

  SEND_TIMEOUT_MS: 12000,
  WS_READY_TIMEOUT_MS: 6000,
  MESSAGE_GROUP_GAP_MS: 5 * 60 * 1000,

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
    this.pageUnloaded = false
    this.userProfileMap = {}
    this.loadingUserIds = new Set()
    this.currentUserId = userInfo ? userInfo.userId : null
    this.currentUser = normalizeUser(userInfo) || {}
    this.cacheUserProfile(this.currentUser)

    this.setData({
      conversationId
    })

    this.markConversationRead()
    this.loadCurrentUserProfile()
    this.loadConversation()
    this.loadMessages()
    this.initSocket()
  },

  onReady() {
    this.measureDockHeight()
  },

  onShow() {
    if (!auth.requireLogin()) return
    this.applyTheme()
    this.markConversationRead()
    this.measureDockHeight()
  },

  onUnload() {
    this.pageUnloaded = true
    this.resetKeyboardHeight()
    this.clearScrollToBottomTimer()
    this.rejectPendingRequest(new Error('页面已关闭'))
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
    request.get('/users/me')
      .then(res => {
        if (!res || !res.data) return
        const user = normalizeUser(res.data) || {}
        if (!user.userId) return
        this.currentUserId = user.userId
        this.currentUser = user
        this.cacheUserProfile(user)
        if (this.data.messages.length > 0) {
          this.setData({
            messageBlocks: this.buildMessageBlocks(this.data.messages)
          })
        }
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
        this.ensureSenderProfiles(messages)
        if (messages.length > 0) {
          this.markConversationRead()
        }

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

  onKeyboardHeightChange(e) {
    const prevHeight = this.data.keyboardHeightPx
    const nextHeight = this.resolveKeyboardHeight(e)
    if (nextHeight > 0 && this.data.morePanelVisible) {
      this.setMorePanelVisible(false)
    }
    const nextBottom = this.data.dockHeightPx + nextHeight
    this.setData({
      keyboardHeightPx: nextHeight,
      messageListBottomPx: nextBottom
    }, () => {
      if (prevHeight === 0 && nextHeight > 0) {
        this.scrollToBottom()
      }
    })
  },

  onComposerFocus() {
    if (!this.data.morePanelVisible) {
      return
    }
    this.setMorePanelVisible(false)
  },

  onComposerBlur() {
    this.resetKeyboardHeight()
  },

  async sendMessage() {
    const content = this.data.inputMessage.trim()
    if (!content) {
      toastError('消息内容不能为空')
      return
    }

    if (this.data.sending) {
      return
    }

    this.setData({ sending: true })
    try {
      await this.sendWsMessageWithAck({
        type: 'SEND',
        conversationId: Number(this.data.conversationId),
        msgType: 'TEXT',
        content
      }, {
        clearInputOnSuccess: true
      })
    } catch (error) {
      if (this.pageUnloaded) {
        return
      }
      toastError(error, '发送失败')
    } finally {
      this.setData({ sending: false })
    }
  },

  handleWsMessage(payload) {
    if (!payload || !payload.type) return

    if (payload.type === 'ERROR') {
      const errorMessage = payload.message || '发送失败'
      const clientMsgId = payload.clientMsgId ? String(payload.clientMsgId) : ''

      // 只有携带 clientMsgId 的 ERROR 才属于发送链路，避免把无关 ERROR 误判为发送失败
      if (clientMsgId) {
        const resolved = this.rejectPendingRequest(new Error(errorMessage), clientMsgId)
        if (!resolved && !this.data.sending) {
          toastError(errorMessage, '请求异常')
        }
        return
      }

      if (!this.data.sending) {
        toastError(errorMessage, '请求异常')
      }
      return
    }

    if (payload.type === 'ACK') {
      this.resolvePendingRequest(payload.clientMsgId)
      this.appendMessage(payload.data)
      return
    }

    if (payload.type === 'MESSAGE') {
      this.appendMessage(payload.data)
      if (payload.data && Number(payload.data.senderId) !== Number(this.currentUserId)) {
        this.markConversationRead()
      }
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
    this.ensureUserProfileById(message.senderId)
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
    let previousMessage = null

    for (let index = 0; index < messages.length; index++) {
      const message = messages[index]
      this.cacheUserProfile({
        userId: message.senderId,
        wolfNo: message.senderWolfNo,
        nickname: message.senderNickname,
        avatar: message.senderAvatar
      })
      const currentDateLabel = time.formatMessageDividerDate(message.createTime)
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
      const sender = this.resolveSenderProfile(message.senderId, isSelf)
      const senderName = sender.nickname || sender.wolfNo || '未知用户'
      const senderInitial = senderName ? senderName.charAt(0) : '行'
      const senderAvatar = sender.avatar || ''
      const senderChanged = !currentGroup || Number(currentGroup.senderId) !== Number(message.senderId)
      const splitByGap = this.isMessageGroupGapExceeded(previousMessage, message)

      if (!currentGroup || senderChanged || splitByGap) {
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
        msgType: message.msgType || 'TEXT',
        content: message.content || '',
        linkUrl: this.extractStandaloneLink(message.content || ''),
        mediaUrl: message.mediaUrl || '',
        mediaWidth: message.mediaWidth || 0,
        mediaHeight: message.mediaHeight || 0,
        mediaSize: Number(message.mediaSize) || 0,
        mediaMimeType: message.mediaMimeType || '',
        fileLabel: this.buildFileLabel(message.content || '', message.mediaSize)
      })
      prevDateLabel = currentDateLabel
      previousMessage = message
    }
    return blocks
  },

  isMessageGroupGapExceeded(previousMessage, currentMessage) {
    if (!previousMessage || !currentMessage) {
      return false
    }

    const previousSenderId = Number(previousMessage.senderId)
    const currentSenderId = Number(currentMessage.senderId)
    if (previousSenderId !== currentSenderId) {
      return false
    }

    const previousTime = time.parseDateTime(previousMessage.createTime)
    const currentTime = time.parseDateTime(currentMessage.createTime)
    if (!previousTime || !currentTime) {
      return true
    }

    const gapMs = currentTime.getTime() - previousTime.getTime()
    if (gapMs < 0) {
      return true
    }
    return gapMs >= this.MESSAGE_GROUP_GAP_MS
  },

  resolveSenderProfile(senderId, isSelf) {
    const numericSenderId = Number(senderId)
    if (isSelf && this.currentUser && Number(this.currentUser.userId) === numericSenderId) {
      return this.currentUser
    }

    if (this.data.targetUser && Number(this.data.targetUser.userId) === numericSenderId) {
      return this.data.targetUser
    }

    return this.userProfileMap && this.userProfileMap[numericSenderId]
      ? this.userProfileMap[numericSenderId]
      : {}
  },

  cacheUserProfile(user) {
    if (!user || !user.userId) return
    if (!this.userProfileMap) {
      this.userProfileMap = {}
    }
    const userId = Number(user.userId)
    const existing = this.userProfileMap[userId] || {}
    const merged = {
      userId
    }

    merged.wolfNo = user.wolfNo || existing.wolfNo
    merged.nickname = user.nickname || existing.nickname
    merged.avatar = user.avatar || existing.avatar

    this.userProfileMap[userId] = normalizeUser(merged)
  },

  ensureSenderProfiles(messages) {
    if (!Array.isArray(messages) || messages.length === 0) {
      return
    }
    const senderIds = Array.from(new Set(
      messages
        .map(item => Number(item.senderId))
        .filter(id => Number.isFinite(id) && id > 0)
    ))

    for (let index = 0; index < senderIds.length; index++) {
      this.ensureUserProfileById(senderIds[index])
    }
  },

  ensureUserProfileById(userId) {
    const numericUserId = Number(userId)
    if (!Number.isFinite(numericUserId) || numericUserId <= 0) {
      return Promise.resolve()
    }

    const cached = this.userProfileMap && this.userProfileMap[numericUserId]
    if (cached && (cached.nickname || cached.wolfNo)) {
      return Promise.resolve()
    }

    if (this.loadingUserIds && this.loadingUserIds.has(numericUserId)) {
      return Promise.resolve()
    }

    this.loadingUserIds.add(numericUserId)
    const req = Number(this.currentUserId) === numericUserId
      ? request.get('/users/me')
      : request.get(`/users/${numericUserId}`)

    return req
      .then(res => {
        if (!res || !res.data) return
        const normalized = normalizeUser(res.data) || {}
        if (!normalized.userId) return

        this.cacheUserProfile(normalized)

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

        updates.messageBlocks = this.buildMessageBlocks(this.data.messages)
        this.setData(updates)
      })
      .catch(() => {})
      .finally(() => {
        if (this.loadingUserIds) {
          this.loadingUserIds.delete(numericUserId)
        }
      })
  },

  applyTheme() {
    applyPageTheme(this)
  },

  setMorePanelVisible(visible) {
    const nextVisible = Boolean(visible)
    if (nextVisible === this.data.morePanelVisible) {
      return
    }
    this.setData({
      morePanelVisible: nextVisible
    }, () => {
      this.measureDockHeight()
    })
  },

  onClickMore() {
    if (this.data.sending) {
      return
    }
    const nextVisible = !this.data.morePanelVisible
    if (nextVisible) {
      wx.hideKeyboard()
      this.resetKeyboardHeight()
    }
    this.setMorePanelVisible(nextVisible)
  },

  async onMoreActionTap(e) {
    const key = e && e.currentTarget && e.currentTarget.dataset
      ? String(e.currentTarget.dataset.key || '')
      : ''
    if (!key) {
      return
    }
    this.setMorePanelVisible(false)

    if (key === 'image') {
      await this.chooseImageFromAlbum()
      return
    }
    if (key === 'video') {
      await this.chooseVideoFromAlbum()
      return
    }
    if (key === 'file') {
      await this.chooseFileForShare()
      return
    }
    if (key === 'link') {
      await this.shareLinkAsText()
    }
  },

  async chooseImageFromAlbum() {
    if (this.data.sending) {
      return
    }

    try {
      const chooseRes = await this.chooseMedia({
        count: 9,
        mediaType: ['image'],
        sourceType: ['album']
      })
      const tempFiles = Array.isArray(chooseRes.tempFiles) ? chooseRes.tempFiles : []
      if (tempFiles.length === 0) {
        return
      }

      this.setData({ sending: true })
      for (let index = 0; index < tempFiles.length; index++) {
        const media = await uploadChatImage(tempFiles[index])
        await this.sendImageMessage(media)
      }
    } catch (error) {
      if (this.pageUnloaded) {
        return
      }
      if (this.isUserCancelError(error)) {
        return
      }
      toastError(error, '图片发送失败')
    } finally {
      this.setData({ sending: false })
    }
  },

  chooseMedia(options) {
    return new Promise((resolve, reject) => {
      wx.chooseMedia({
        ...options,
        success: resolve,
        fail: reject
      })
    })
  },

  chooseMessageFile(options) {
    return new Promise((resolve, reject) => {
      wx.chooseMessageFile({
        ...options,
        success: resolve,
        fail: reject
      })
    })
  },

  async chooseVideoFromAlbum() {
    if (this.data.sending) {
      return
    }

    try {
      const chooseRes = await this.chooseMedia({
        count: 1,
        mediaType: ['video'],
        sourceType: ['album']
      })
      const tempFile = Array.isArray(chooseRes.tempFiles) ? chooseRes.tempFiles[0] : null
      if (!tempFile || !tempFile.tempFilePath) {
        return
      }
      this.setData({ sending: true })
      const media = await uploadChatVideo(tempFile)
      await this.sendVideoMessage(media)
    } catch (error) {
      if (this.pageUnloaded) {
        return
      }
      if (this.isUserCancelError(error)) {
        return
      }
      toastError(error, '视频发送失败')
    } finally {
      this.setData({ sending: false })
    }
  },

  async chooseFileForShare() {
    if (this.data.sending) {
      return
    }

    try {
      const chooseRes = await this.chooseMessageFile({
        count: 1,
        type: 'file'
      })
      const file = Array.isArray(chooseRes.tempFiles) ? chooseRes.tempFiles[0] : null
      if (!file) {
        return
      }
      this.setData({ sending: true })
      const media = await uploadChatFile(file)
      await this.sendFileMessage(media)
    } catch (error) {
      if (this.pageUnloaded) {
        return
      }
      if (this.isUserCancelError(error)) {
        return
      }
      toastError(error, '文件发送失败')
    } finally {
      this.setData({ sending: false })
    }
  },

  async shareLinkAsText() {
    const modalRes = await this.showEditableModal({
      title: '分享链接',
      placeholderText: '请输入链接地址'
    })
    if (!modalRes.confirm) {
      return
    }

    const link = this.normalizeSharedLink(modalRes.content)
    if (!link) {
      toastError('链接格式不正确')
      return
    }

    if (this.data.sending) {
      return
    }

    this.setData({ sending: true })
    try {
      await this.sendWsMessageWithAck({
        type: 'SEND',
        conversationId: Number(this.data.conversationId),
        msgType: 'TEXT',
        content: link
      })
    } catch (error) {
      if (this.pageUnloaded) {
        return
      }
      toastError(error, '链接发送失败')
    } finally {
      this.setData({ sending: false })
    }
  },

  showEditableModal(options) {
    return new Promise((resolve) => {
      wx.showModal({
        editable: true,
        ...options,
        success: resolve,
        fail: () => resolve({ confirm: false, content: '' })
      })
    })
  },

  normalizeSharedLink(value) {
    const raw = String(value || '').trim()
    if (!raw) {
      return ''
    }
    const normalized = /^https?:\/\//i.test(raw)
      ? raw
      : `https://${raw}`
    const isValid = /^https?:\/\/[^\s]+$/i.test(normalized)
    return isValid ? normalized : ''
  },

  extractStandaloneLink(value) {
    const raw = String(value || '').trim()
    if (!raw) {
      return ''
    }
    return /^https?:\/\/[^\s]+$/i.test(raw) ? raw : ''
  },

  buildFileLabel(fileName, size) {
    const normalizedFileName = String(fileName || '').trim() || '未命名文件'
    const sizeText = this.formatMediaSize(size)
    if (!sizeText) {
      return `[文件] ${normalizedFileName}`
    }
    return `[文件] ${normalizedFileName} (${sizeText})`
  },

  formatMediaSize(size) {
    const bytes = Number(size)
    if (!Number.isFinite(bytes) || bytes <= 0) {
      return ''
    }
    if (bytes < 1024) {
      return `${bytes}B`
    }
    if (bytes < 1024 * 1024) {
      return `${(bytes / 1024).toFixed(1)}KB`
    }
    if (bytes < 1024 * 1024 * 1024) {
      return `${(bytes / (1024 * 1024)).toFixed(1)}MB`
    }
    return `${(bytes / (1024 * 1024 * 1024)).toFixed(1)}GB`
  },

  isUserCancelError(error) {
    const message = error && error.errMsg ? String(error.errMsg) : ''
    return message.includes('cancel')
  },

  onTapLink(e) {
    const url = e && e.currentTarget && e.currentTarget.dataset
      ? String(e.currentTarget.dataset.url || '')
      : ''
    if (!url) {
      return
    }
    wx.setClipboardData({
      data: url
    })
  },

  onTapFile(e) {
    const dataset = e && e.currentTarget ? e.currentTarget.dataset || {} : {}
    const url = String(dataset.url || '')
    if (!url) {
      toastError('文件地址无效')
      return
    }
    const fileName = String(dataset.name || '').trim() || 'file'

    wx.showLoading({ title: '打开中...', mask: true })
    this.downloadTempFile(url)
      .then(downloadRes => {
        if (downloadRes.statusCode < 200 || downloadRes.statusCode >= 300 || !downloadRes.tempFilePath) {
          throw new Error(`文件下载失败(${downloadRes.statusCode || 0})`)
        }
        return this.openDocument(downloadRes.tempFilePath, fileName)
      })
      .catch(error => {
        if (this.pageUnloaded) {
          return
        }
        toastError(error, '文件打开失败')
      })
      .finally(() => {
        wx.hideLoading()
      })
  },

  downloadTempFile(url) {
    return new Promise((resolve, reject) => {
      wx.downloadFile({
        url,
        success: resolve,
        fail: reject
      })
    })
  },

  openDocument(filePath, fileName) {
    return new Promise((resolve, reject) => {
      wx.openDocument({
        filePath,
        fileType: this.getFileTypeByName(fileName),
        showMenu: true,
        success: resolve,
        fail: reject
      })
    })
  },

  getFileTypeByName(fileName) {
    const raw = String(fileName || '')
    const dotIndex = raw.lastIndexOf('.')
    if (dotIndex < 0 || dotIndex === raw.length - 1) {
      return ''
    }
    const extension = raw.slice(dotIndex + 1).toLowerCase()
    if (!['doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx', 'pdf', 'txt'].includes(extension)) {
      return ''
    }
    return extension
  },

  sendImageMessage(media) {
    return this.sendWsMessageWithAck({
      type: 'SEND',
      conversationId: Number(this.data.conversationId),
      msgType: 'IMAGE',
      mediaKey: media.mediaKey,
      mediaWidth: media.mediaWidth,
      mediaHeight: media.mediaHeight,
      mediaSize: media.mediaSize,
      mediaMimeType: media.mediaMimeType
    })
  },

  sendVideoMessage(media) {
    return this.sendWsMessageWithAck({
      type: 'SEND',
      conversationId: Number(this.data.conversationId),
      msgType: 'VIDEO',
      mediaKey: media.mediaKey,
      mediaWidth: media.mediaWidth,
      mediaHeight: media.mediaHeight,
      mediaSize: media.mediaSize,
      mediaMimeType: media.mediaMimeType
    })
  },

  sendFileMessage(media) {
    return this.sendWsMessageWithAck({
      type: 'SEND',
      conversationId: Number(this.data.conversationId),
      msgType: 'FILE',
      content: this.normalizeFileNameForMessage(media.fileName),
      mediaKey: media.mediaKey,
      mediaSize: media.mediaSize,
      mediaMimeType: media.mediaMimeType
    })
  },

  normalizeFileNameForMessage(fileName) {
    const raw = String(fileName || '').trim() || '未命名文件'
    if (raw.length <= 120) {
      return raw
    }
    return raw.slice(0, 120)
  },

  scrollToBottom() {
    this.clearScrollToBottomTimer()
    const scrollToAnchor = () => {
      this.setData({ scrollIntoView: 'im-bottom-anchor' })
      this.scrollToBottomTimer = setTimeout(() => {
        if (this.data.scrollIntoView) {
          this.setData({ scrollIntoView: '' })
        }
        this.scrollToBottomTimer = null
      }, 120)
    }

    if (this.data.scrollIntoView) {
      this.setData({ scrollIntoView: '' }, scrollToAnchor)
      return
    }
    scrollToAnchor()
  },

  onMessageListUpper() {
    this.loadMessages()
  },

  previewImage(e) {
    const current = e.currentTarget.dataset.url
    if (!current) {
      return
    }

    const urls = this.data.messages
      .filter(item => item.msgType === 'IMAGE' && item.mediaUrl)
      .map(item => item.mediaUrl)

    wx.previewImage({
      current,
      urls: urls.length > 0 ? urls : [current]
    })
  },

  markConversationRead() {
    if (!this.data.conversationId) {
      return Promise.resolve()
    }
    if (this.markReadLoading) {
      this.markReadPending = true
      return Promise.resolve()
    }

    this.markReadLoading = true
    return request.put(`/conversations/${this.data.conversationId}/read`)
      .catch(() => {})
      .finally(() => {
        this.markReadLoading = false
        if (this.markReadPending) {
          this.markReadPending = false
          this.markConversationRead()
        }
      })
  },

  generateClientMsgId() {
    return `c_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`
  },

  async sendWsMessageWithAck(payload, options = {}) {
    if (this.pendingRequest) {
      throw new Error('存在未完成的发送请求，请稍后重试')
    }

    await ws.waitUntilReady(this.WS_READY_TIMEOUT_MS)

    const clientMsgId = this.generateClientMsgId()
    const messagePayload = {
      ...payload,
      clientMsgId
    }

    return new Promise((resolve, reject) => {
      this.pendingRequest = {
        clientMsgId,
        resolve,
        reject,
        clearInputOnSuccess: Boolean(options.clearInputOnSuccess)
      }
      this.startPendingTimer(clientMsgId)
      ws.send(messagePayload)
    })
  },

  startPendingTimer(clientMsgId) {
    this.clearPendingTimer()
    this.pendingTimer = setTimeout(() => {
      this.rejectPendingRequest(new Error('消息发送超时，请重试'), clientMsgId)
    }, this.SEND_TIMEOUT_MS)
  },

  clearPendingTimer() {
    if (this.pendingTimer) {
      clearTimeout(this.pendingTimer)
      this.pendingTimer = null
    }
  },

  resolvePendingRequest(clientMsgId) {
    const pending = this.pendingRequest
    if (!pending) {
      return false
    }
    if (clientMsgId && pending.clientMsgId !== clientMsgId) {
      return false
    }

    this.clearPendingTimer()
    this.pendingRequest = null
    if (pending.clearInputOnSuccess) {
      this.setData({
        inputMessage: ''
      })
    }
    pending.resolve()
    return true
  },

  rejectPendingRequest(error, clientMsgId) {
    const pending = this.pendingRequest
    if (!pending) {
      return false
    }
    if (clientMsgId && pending.clientMsgId !== clientMsgId) {
      return false
    }

    this.clearPendingTimer()
    this.pendingRequest = null
    pending.reject(error || new Error('发送失败'))
    return true
  },

  resetKeyboardHeight() {
    if (this.data.keyboardHeightPx === 0
      && this.data.messageListBottomPx === this.data.dockHeightPx) {
      return
    }
    this.setData({
      keyboardHeightPx: 0,
      messageListBottomPx: this.data.dockHeightPx
    })
  },

  resolveKeyboardHeight(event) {
    const rawHeight = event && event.detail ? Number(event.detail.height) : 0
    if (!Number.isFinite(rawHeight) || rawHeight <= 0) {
      return 0
    }
    return Math.floor(rawHeight)
  },

  measureDockHeight() {
    wx.nextTick(() => {
      const query = wx.createSelectorQuery().in(this)
      query.select('#im-dock').boundingClientRect(rect => {
        if (!rect || !rect.height) {
          return
        }
        const nextDockHeight = Math.ceil(rect.height)
        const nextBottom = nextDockHeight + this.data.keyboardHeightPx
        if (nextDockHeight === this.data.dockHeightPx && nextBottom === this.data.messageListBottomPx) {
          return
        }
        this.setData({
          dockHeightPx: nextDockHeight,
          messageListBottomPx: nextBottom
        })
      }).exec()
    })
  },

  clearScrollToBottomTimer() {
    if (!this.scrollToBottomTimer) {
      return
    }
    clearTimeout(this.scrollToBottomTimer)
    this.scrollToBottomTimer = null
  }
})
