const time = require('./time')

const DEFAULT_MESSAGE_MERGE_GAP_MS = 5 * 60 * 1000
const VIDEO_MAX_WIDTH_RPX = 420
const VIDEO_MAX_HEIGHT_RPX = 520
const VIDEO_MIN_WIDTH_RPX = 220
const VIDEO_MIN_HEIGHT_RPX = 180
const FILE_NAME_MAX_LENGTH = 120
const BLUR_RESET_DELAY_MS = 90
const REFOCUS_DELAY_MS = 24
const ECHO_MATCH_TOLERANCE_MS = 2 * 60 * 1000

function normalizeSharedLink(value) {
  const raw = String(value || '').trim()
  if (!raw) {
    return ''
  }
  const normalized = /^https?:\/\//i.test(raw)
    ? raw
    : `https://${raw}`
  const isValid = /^https?:\/\/[^\s]+$/i.test(normalized)
  return isValid ? normalized : ''
}

function extractStandaloneLink(value) {
  const raw = String(value || '').trim()
  if (!raw) {
    return ''
  }
  return /^https?:\/\/[^\s]+$/i.test(raw) ? raw : ''
}

function formatMediaSize(size) {
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
}

function buildFileLabel(fileName, size) {
  const normalizedFileName = String(fileName || '').trim() || '未命名文件'
  const sizeText = formatMediaSize(size)
  if (!sizeText) {
    return `[文件] ${normalizedFileName}`
  }
  return `[文件] ${normalizedFileName} (${sizeText})`
}

function buildVideoRenderStyle(mediaWidth, mediaHeight) {
  const width = Number(mediaWidth)
  const height = Number(mediaHeight)
  const fallbackWidth = VIDEO_MAX_WIDTH_RPX
  const fallbackHeight = Math.round((fallbackWidth * 9) / 16)
  if (!Number.isFinite(width) || !Number.isFinite(height) || width <= 0 || height <= 0) {
    return `width:${fallbackWidth}rpx;height:${fallbackHeight}rpx;`
  }

  const ratio = width / height
  let renderWidth = VIDEO_MAX_WIDTH_RPX
  let renderHeight = renderWidth / ratio

  if (renderHeight > VIDEO_MAX_HEIGHT_RPX) {
    renderHeight = VIDEO_MAX_HEIGHT_RPX
    renderWidth = renderHeight * ratio
  }
  if (renderHeight < VIDEO_MIN_HEIGHT_RPX) {
    renderHeight = VIDEO_MIN_HEIGHT_RPX
    renderWidth = renderHeight * ratio
  }
  if (renderWidth < VIDEO_MIN_WIDTH_RPX) {
    renderWidth = VIDEO_MIN_WIDTH_RPX
    renderHeight = renderWidth / ratio
  }
  if (renderWidth > VIDEO_MAX_WIDTH_RPX) {
    renderWidth = VIDEO_MAX_WIDTH_RPX
    renderHeight = renderWidth / ratio
  }
  if (renderHeight > VIDEO_MAX_HEIGHT_RPX) {
    renderHeight = VIDEO_MAX_HEIGHT_RPX
    renderWidth = renderHeight * ratio
  }

  const roundedWidth = Math.round(renderWidth)
  const roundedHeight = Math.round(renderHeight)
  return `width:${roundedWidth}rpx;height:${roundedHeight}rpx;`
}

function isUserCancelError(error) {
  const message = error && error.errMsg ? String(error.errMsg) : ''
  return message.includes('cancel')
}

function getFileTypeByName(fileName) {
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
}

function normalizeFileNameForMessage(fileName) {
  const raw = String(fileName || '').trim() || '未命名文件'
  if (raw.length <= FILE_NAME_MAX_LENGTH) {
    return raw
  }
  return raw.slice(0, FILE_NAME_MAX_LENGTH)
}

function resolveKeyboardHeight(event) {
  const rawHeight = event && event.detail ? Number(event.detail.height) : 0
  if (!Number.isFinite(rawHeight) || rawHeight <= 0) {
    return 0
  }
  return Math.floor(rawHeight)
}

function isMessageMergeGapExceeded(previousMessage, currentMessage, gapMs = DEFAULT_MESSAGE_MERGE_GAP_MS) {
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

  const diff = currentTime.getTime() - previousTime.getTime()
  if (diff < 0) {
    return true
  }
  return diff >= gapMs
}

function createClientMsgId(prefix) {
  const idPrefix = String(prefix || 'm').trim() || 'm'
  return `${idPrefix}_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`
}

function buildEchoMatcher(page, payload) {
  const msgType = String(payload && payload.msgType ? payload.msgType : '').toUpperCase()
  const conversationId = payload && payload.conversationId != null
    ? Number(payload.conversationId)
    : 0
  return {
    senderId: Number(page && page.currentUserId ? page.currentUserId : 0),
    conversationId: Number.isFinite(conversationId) && conversationId > 0 ? conversationId : 0,
    msgType,
    content: String(payload && payload.content ? payload.content : '').trim(),
    sentAt: Date.now()
  }
}

function isEchoMessageMatched(pendingRequest, message) {
  if (!pendingRequest || !pendingRequest.echoMatcher || !message) {
    return false
  }
  const matcher = pendingRequest.echoMatcher
  const senderId = Number(message.senderId)
  if (!senderId || senderId !== matcher.senderId) {
    return false
  }

  const incomingType = String(message.msgType || '').toUpperCase()
  if (matcher.msgType && matcher.msgType !== incomingType) {
    return false
  }

  if (matcher.conversationId > 0) {
    const incomingConversationId = Number(message.conversationId)
    if (!incomingConversationId || incomingConversationId !== matcher.conversationId) {
      return false
    }
  }

  if (matcher.msgType === 'TEXT' || matcher.msgType === 'FILE') {
    const incomingContent = String(message.content || '').trim()
    if (matcher.content !== incomingContent) {
      return false
    }
  }

  const messageTime = time.parseDateTime(message.createTime)
  if (messageTime) {
    const diff = Math.abs(messageTime.getTime() - matcher.sentAt)
    if (diff > ECHO_MATCH_TOLERANCE_MS) {
      return false
    }
  }

  return true
}

function clearPendingTimer(page) {
  if (!page || !page.pendingTimer) {
    return
  }
  clearTimeout(page.pendingTimer)
  page.pendingTimer = null
}

function resolvePendingRequest(page, clientMsgId) {
  const pending = page ? page.pendingRequest : null
  if (!pending) {
    return false
  }
  const expectedClientMsgId = pending.clientMsgId ? String(pending.clientMsgId) : ''
  const actualClientMsgId = clientMsgId ? String(clientMsgId) : ''
  if (actualClientMsgId && expectedClientMsgId !== actualClientMsgId) {
    return false
  }

  clearPendingTimer(page)
  page.pendingRequest = null
  if (pending.clearInputOnSuccess) {
    const nextData = {
      inputMessage: ''
    }
    if (page.data && Object.prototype.hasOwnProperty.call(page.data, 'canSendText')) {
      nextData.canSendText = false
    }
    page.setData(nextData)
  }
  pending.resolve()
  return true
}

function rejectPendingRequest(page, error, clientMsgId) {
  const pending = page ? page.pendingRequest : null
  if (!pending) {
    return false
  }
  const expectedClientMsgId = pending.clientMsgId ? String(pending.clientMsgId) : ''
  const actualClientMsgId = clientMsgId ? String(clientMsgId) : ''
  if (actualClientMsgId && expectedClientMsgId !== actualClientMsgId) {
    return false
  }

  clearPendingTimer(page)
  page.pendingRequest = null
  pending.reject(error || new Error('发送失败'))
  return true
}

function resolvePendingByEchoMessage(page, message) {
  const pending = page ? page.pendingRequest : null
  if (!pending || !isEchoMessageMatched(pending, message)) {
    return false
  }
  return resolvePendingRequest(page, '')
}

function startPendingTimer(page, clientMsgId, timeoutMs) {
  clearPendingTimer(page)
  page.pendingTimer = setTimeout(() => {
    rejectPendingRequest(page, new Error('消息发送超时，请重试'), clientMsgId)
  }, timeoutMs)
}

async function sendWsMessageWithAck(page, ws, payload, options = {}) {
  if (page.pendingRequest) {
    throw new Error('存在未完成的发送请求，请稍后重试')
  }

  await ws.waitUntilReady(page.WS_READY_TIMEOUT_MS)

  const clientMsgId = createClientMsgId(page.CLIENT_MSG_ID_PREFIX)
  const messagePayload = {
    ...payload,
    clientMsgId
  }

  return new Promise((resolve, reject) => {
    page.pendingRequest = {
      clientMsgId,
      resolve,
      reject,
      clearInputOnSuccess: Boolean(options.clearInputOnSuccess),
      echoMatcher: buildEchoMatcher(page, messagePayload)
    }
    startPendingTimer(page, clientMsgId, page.SEND_TIMEOUT_MS)
    ws.send(messagePayload)
  })
}

function clearScrollToBottomTimer(page) {
  if (!page || !page.scrollToBottomTimer) {
    return
  }
  clearTimeout(page.scrollToBottomTimer)
  page.scrollToBottomTimer = null
}

function scrollToBottom(page) {
  clearScrollToBottomTimer(page)
  const scrollToAnchor = () => {
    page.setData({ scrollIntoView: 'im-bottom-anchor' })
    page.scrollToBottomTimer = setTimeout(() => {
      if (page.data.scrollIntoView) {
        page.setData({ scrollIntoView: '' })
      }
      page.scrollToBottomTimer = null
    }, 120)
  }

  if (page.data.scrollIntoView) {
    page.setData({ scrollIntoView: '' }, scrollToAnchor)
    return
  }
  scrollToAnchor()
}

function clearRefocusComposerTimer(page) {
  if (!page || !page.refocusComposerTimer) {
    return
  }
  clearTimeout(page.refocusComposerTimer)
  page.refocusComposerTimer = null
}

function updateKeyboardLayout(page, keyboardHeight, options = {}) {
  if (!page || !page.data) {
    return false
  }
  const prevHeight = Number(page.data.keyboardHeightPx) || 0
  const nextHeight = Number.isFinite(keyboardHeight) && keyboardHeight > 0
    ? Math.floor(keyboardHeight)
    : 0
  const dockHeight = Number(page.data.dockHeightPx) || 0
  const nextBottom = dockHeight + nextHeight
  if (nextHeight === prevHeight && nextBottom === page.data.messageListBottomPx) {
    return false
  }

  page.setData({
    keyboardHeightPx: nextHeight,
    messageListBottomPx: nextBottom
  }, () => {
    if (options.scrollOnKeyboardOpen && prevHeight === 0 && nextHeight > 0) {
      scrollToBottom(page)
    }
  })
  return true
}

function resetKeyboardHeight(page) {
  if (!page || !page.data) {
    return
  }
  updateKeyboardLayout(page, 0)
}

function handleKeyboardHeightChange(page, event, closeMorePanel) {
  if (!page || !page.data) {
    return
  }
  const nextHeight = resolveKeyboardHeight(event)
  if (nextHeight > 0) {
    page.lastKeyboardOpenedAt = Date.now()
    page.lastKeyboardHeightPx = nextHeight
  }
  if (nextHeight > 0 && page.data.morePanelVisible && typeof closeMorePanel === 'function') {
    closeMorePanel()
  }

  // 发送按钮点击会触发短暂 blur，忽略这一帧的 0 高度避免输入栏下坠。
  if (nextHeight === 0 && page.keepComposerOpenUntilSendFinish) {
    return
  }

  updateKeyboardLayout(page, nextHeight, {
    scrollOnKeyboardOpen: true
  })
}

function handleComposerFocus(page, event, closeMorePanel) {
  if (!page || !page.data) {
    return
  }
  if (!page.data.composerFocused) {
    page.setData({ composerFocused: true })
  }
  const focusHeight = resolveKeyboardHeight(event) || Number(page.lastKeyboardHeightPx || 0)
  if (focusHeight > 0) {
    page.lastKeyboardOpenedAt = Date.now()
    const changed = updateKeyboardLayout(page, focusHeight)
    if (!changed) {
      scrollToBottom(page)
    }
  }

  if (!page.data.morePanelVisible || typeof closeMorePanel !== 'function') {
    return
  }
  closeMorePanel()
}

function handleComposerBlur(page) {
  if (!page) {
    return
  }
  if (page.keepComposerOpenUntilSendFinish) {
    refocusComposerInput(page)
    return
  }
  page.setData({ composerFocused: false })
  setTimeout(() => {
    if (page.pageUnloaded || !page.data || page.data.composerFocused) {
      return
    }
    if (page.data.keyboardHeightPx > 0) {
      resetKeyboardHeight(page)
    }
  }, BLUR_RESET_DELAY_MS)
}

function measureDockHeight(page) {
  if (!page || typeof wx === 'undefined') {
    return
  }
  wx.nextTick(() => {
    const query = wx.createSelectorQuery().in(page)
    query.select('#im-dock').boundingClientRect(rect => {
      if (!rect || !rect.height || !page.data) {
        return
      }
      const nextDockHeight = Math.ceil(rect.height)
      const nextBottom = nextDockHeight + page.data.keyboardHeightPx
      if (nextDockHeight === page.data.dockHeightPx && nextBottom === page.data.messageListBottomPx) {
        return
      }
      page.setData({
        dockHeightPx: nextDockHeight,
        messageListBottomPx: nextBottom
      })
    }).exec()
  })
}

function shouldKeepComposerAfterSend(page) {
  if (!page || !page.data) {
    return false
  }
  if (page.data.morePanelVisible) {
    return false
  }
  if (page.data.keyboardHeightPx > 0 || page.data.composerFocused) {
    return true
  }

  const lastKeyboardOpenedAt = Number(page.lastKeyboardOpenedAt || 0)
  if (lastKeyboardOpenedAt <= 0) {
    return false
  }
  return Date.now() - lastKeyboardOpenedAt <= 1200
}

function refocusComposerInput(page) {
  if (!page || page.pageUnloaded || !page.data || page.data.morePanelVisible) {
    return
  }
  if (page.data.composerFocused && page.data.keyboardHeightPx > 0) {
    return
  }

  clearRefocusComposerTimer(page)
  page.setData({ composerFocused: false }, () => {
    page.refocusComposerTimer = setTimeout(() => {
      page.refocusComposerTimer = null
      if (page.pageUnloaded || !page.data || page.data.morePanelVisible) {
        return
      }
      page.setData({ composerFocused: true })
    }, REFOCUS_DELAY_MS)
  })
}

function chooseMedia(options) {
  return new Promise((resolve, reject) => {
    wx.chooseMedia({
      ...options,
      success: resolve,
      fail: reject
    })
  })
}

function chooseMessageFile(options) {
  return new Promise((resolve, reject) => {
    wx.chooseMessageFile({
      ...options,
      success: resolve,
      fail: reject
    })
  })
}

function showEditableModal(options) {
  return new Promise((resolve) => {
    wx.showModal({
      editable: true,
      ...options,
      success: resolve,
      fail: () => resolve({ confirm: false, content: '' })
    })
  })
}

function downloadTempFile(url) {
  return new Promise((resolve, reject) => {
    wx.downloadFile({
      url,
      success: resolve,
      fail: reject
    })
  })
}

function openDocument(filePath, fileName) {
  return new Promise((resolve, reject) => {
    wx.openDocument({
      filePath,
      fileType: getFileTypeByName(fileName),
      showMenu: true,
      success: resolve,
      fail: reject
    })
  })
}

function buildSenderProfileFromMessage(message) {
  const senderProfile = {
    userId: message.senderId,
    wolfNo: message.senderWolfNo,
    nickname: message.senderNickname,
    avatar: message.senderAvatar
  }
  if (Object.prototype.hasOwnProperty.call(message, 'senderEquippedTitleName')) {
    senderProfile.equippedTitleName = message.senderEquippedTitleName
  }
  if (Object.prototype.hasOwnProperty.call(message, 'senderEquippedTitleColor')) {
    senderProfile.equippedTitleColor = message.senderEquippedTitleColor
  }
  return senderProfile
}

function getLastDividerLabel(blocks) {
  if (!Array.isArray(blocks) || blocks.length === 0) {
    return ''
  }
  for (let index = blocks.length - 1; index >= 0; index--) {
    const block = blocks[index]
    if (block && block.type === 'divider') {
      return String(block.timeText || '')
    }
  }
  return ''
}

function createMessageRow(message, indexToken) {
  return {
    key: `m_${message.messageId || indexToken}`,
    messageId: message.messageId,
    msgType: message.msgType || 'TEXT',
    content: message.content || '',
    linkUrl: extractStandaloneLink(message.content || ''),
    mediaUrl: message.mediaUrl || '',
    mediaPosterUrl: message.mediaPosterUrl || '',
    mediaWidth: message.mediaWidth || 0,
    mediaHeight: message.mediaHeight || 0,
    mediaSize: Number(message.mediaSize) || 0,
    mediaMimeType: message.mediaMimeType || '',
    videoRenderStyle: buildVideoRenderStyle(message.mediaWidth, message.mediaHeight),
    fileLabel: buildFileLabel(message.content || '', message.mediaSize)
  }
}

function createClusterBlock(message, indexToken, sender, isSelf) {
  const senderName = sender.nickname || sender.wolfNo || '未知用户'
  const senderTitleName = String(sender.equippedTitleName || '').trim()
  const senderTitleColor = String(sender.equippedTitleColor || '').trim()
  return {
    type: 'cluster',
    key: `g_${message.messageId || indexToken}`,
    senderId: message.senderId,
    isSelf,
    senderName,
    senderInitial: senderName ? senderName.charAt(0) : '行',
    senderAvatar: sender.avatar || '',
    senderTitleName,
    senderTitleColor,
    headerTimeText: time.formatMessageMetaTime(message.createTime),
    rows: [createMessageRow(message, indexToken)]
  }
}

function appendMessageBlock(messageBlocks, message, context = {}) {
  if (!message) {
    return Array.isArray(messageBlocks) ? messageBlocks.slice() : []
  }

  const indexToken = Number.isFinite(context.messageIndex) ? context.messageIndex : Date.now()
  const blocks = Array.isArray(messageBlocks) ? messageBlocks.slice() : []
  const cacheUserProfile = typeof context.cacheUserProfile === 'function'
    ? context.cacheUserProfile
    : () => {}
  const resolveSenderProfile = typeof context.resolveSenderProfile === 'function'
    ? context.resolveSenderProfile
    : () => ({})
  const currentUserId = Number(context.currentUserId)
  const gapMs = Number(context.messageMergeGapMs) || DEFAULT_MESSAGE_MERGE_GAP_MS

  cacheUserProfile(buildSenderProfileFromMessage(message))

  const currentDateLabel = time.formatMessageDividerDate(message.createTime)
  const previousDividerLabel = getLastDividerLabel(blocks)
  if (!previousDividerLabel || previousDividerLabel !== currentDateLabel) {
    blocks.push({
      type: 'divider',
      key: `d_${message.messageId || indexToken}`,
      timeText: currentDateLabel
    })
  }

  const isSelf = Number(message.senderId) === currentUserId
  const sender = resolveSenderProfile(message.senderId, isSelf)
  const lastBlock = blocks.length > 0 ? blocks[blocks.length - 1] : null
  const canMergeLastCluster = lastBlock
    && lastBlock.type === 'cluster'
    && Number(lastBlock.senderId) === Number(message.senderId)
    && !isMessageMergeGapExceeded(context.previousMessage, message, gapMs)

  if (canMergeLastCluster) {
    const mergedCluster = {
      ...lastBlock,
      rows: [...(lastBlock.rows || []), createMessageRow(message, indexToken)]
    }
    blocks[blocks.length - 1] = mergedCluster
    return blocks
  }

  blocks.push(createClusterBlock(message, indexToken, sender, isSelf))
  return blocks
}

function buildMessageBlocks(messages, context) {
  if (!Array.isArray(messages) || messages.length === 0) {
    return []
  }
  const blocks = []
  let previousDateLabel = ''
  let currentCluster = null
  let previousMessage = null
  const cacheUserProfile = typeof context.cacheUserProfile === 'function'
    ? context.cacheUserProfile
    : () => {}
  const resolveSenderProfile = typeof context.resolveSenderProfile === 'function'
    ? context.resolveSenderProfile
    : () => ({})
  const currentUserId = Number(context.currentUserId)
  const gapMs = Number(context.messageMergeGapMs) || DEFAULT_MESSAGE_MERGE_GAP_MS

  for (let index = 0; index < messages.length; index++) {
    const message = messages[index]
    cacheUserProfile(buildSenderProfileFromMessage(message))

    const currentDateLabel = time.formatMessageDividerDate(message.createTime)
    if (!previousDateLabel || previousDateLabel !== currentDateLabel) {
      blocks.push({
        type: 'divider',
        key: `d_${message.messageId || index}`,
        timeText: currentDateLabel
      })
      currentCluster = null
    }

    const isSelf = Number(message.senderId) === currentUserId
    const sender = resolveSenderProfile(message.senderId, isSelf)
    const senderChanged = !currentCluster || Number(currentCluster.senderId) !== Number(message.senderId)
    const splitByGap = isMessageMergeGapExceeded(previousMessage, message, gapMs)
    if (!currentCluster || senderChanged || splitByGap) {
      currentCluster = createClusterBlock(message, index, sender, isSelf)
      blocks.push(currentCluster)
    } else {
      currentCluster.rows.push(createMessageRow(message, index))
    }

    previousDateLabel = currentDateLabel
    previousMessage = message
  }

  return blocks
}

module.exports = {
  DEFAULT_MESSAGE_MERGE_GAP_MS,
  clearPendingTimer,
  resolvePendingRequest,
  rejectPendingRequest,
  resolvePendingByEchoMessage,
  sendWsMessageWithAck,
  scrollToBottom,
  clearScrollToBottomTimer,
  clearRefocusComposerTimer,
  resetKeyboardHeight,
  handleKeyboardHeightChange,
  handleComposerFocus,
  handleComposerBlur,
  measureDockHeight,
  shouldKeepComposerAfterSend,
  refocusComposerInput,
  chooseMedia,
  chooseMessageFile,
  showEditableModal,
  downloadTempFile,
  openDocument,
  normalizeSharedLink,
  isUserCancelError,
  normalizeFileNameForMessage,
  appendMessageBlock,
  buildMessageBlocks
}
