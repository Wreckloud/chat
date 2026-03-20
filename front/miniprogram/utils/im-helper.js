const time = require('./time')
const imLayoutHelper = require('./im-layout-helper')
const imRequestHelper = require('./im-request-helper')

const DEFAULT_MESSAGE_MERGE_GAP_MS = 5 * 60 * 1000
const IMAGE_MAX_WIDTH_RPX = 320
const IMAGE_MAX_HEIGHT_RPX = 420
const IMAGE_MIN_WIDTH_RPX = 120
const IMAGE_MIN_HEIGHT_RPX = 96
const IMAGE_FALLBACK_WIDTH_RPX = 280
const IMAGE_FALLBACK_HEIGHT_RPX = 210
const VIDEO_MAX_WIDTH_RPX = 420
const VIDEO_MAX_HEIGHT_RPX = 520
const VIDEO_MIN_WIDTH_RPX = 220
const VIDEO_MIN_HEIGHT_RPX = 180
const FILE_NAME_MAX_LENGTH = 120
const REPLY_RECALLED_TEXT = '原消息已被撤回'
const DELIVERY_STATUS_FAILED = 2

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

function buildImageRenderStyle(mediaWidth, mediaHeight) {
  const width = Number(mediaWidth)
  const height = Number(mediaHeight)
  if (!Number.isFinite(width) || !Number.isFinite(height) || width <= 0 || height <= 0) {
    return `width:${IMAGE_FALLBACK_WIDTH_RPX}rpx;height:${IMAGE_FALLBACK_HEIGHT_RPX}rpx;`
  }

  const ratio = width / height
  if (ratio >= 3) {
    const renderHeight = Math.max(
      IMAGE_MIN_HEIGHT_RPX,
      Math.min(IMAGE_MAX_HEIGHT_RPX, Math.round(IMAGE_MAX_WIDTH_RPX / ratio))
    )
    return `width:${IMAGE_MAX_WIDTH_RPX}rpx;height:${renderHeight}rpx;`
  }
  if (ratio <= 1 / 3) {
    const renderWidth = Math.max(
      IMAGE_MIN_WIDTH_RPX,
      Math.min(IMAGE_MAX_WIDTH_RPX, Math.round(IMAGE_MAX_HEIGHT_RPX * ratio))
    )
    return `width:${renderWidth}rpx;height:${IMAGE_MAX_HEIGHT_RPX}rpx;`
  }

  let renderWidth = IMAGE_MAX_WIDTH_RPX
  let renderHeight = renderWidth / ratio
  if (renderHeight > IMAGE_MAX_HEIGHT_RPX) {
    renderHeight = IMAGE_MAX_HEIGHT_RPX
    renderWidth = renderHeight * ratio
  }
  if (renderWidth < IMAGE_MIN_WIDTH_RPX) {
    renderWidth = IMAGE_MIN_WIDTH_RPX
    renderHeight = renderWidth / ratio
  }
  if (renderHeight < IMAGE_MIN_HEIGHT_RPX) {
    renderHeight = IMAGE_MIN_HEIGHT_RPX
    renderWidth = renderHeight * ratio
  }
  if (renderWidth > IMAGE_MAX_WIDTH_RPX) {
    renderWidth = IMAGE_MAX_WIDTH_RPX
    renderHeight = renderWidth / ratio
  }
  if (renderHeight > IMAGE_MAX_HEIGHT_RPX) {
    renderHeight = IMAGE_MAX_HEIGHT_RPX
    renderWidth = renderHeight * ratio
  }

  const roundedWidth = Math.max(1, Math.round(renderWidth))
  const roundedHeight = Math.max(1, Math.round(renderHeight))
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

function buildMessageLookup(messages) {
  const lookup = new Map()
  if (!Array.isArray(messages) || messages.length === 0) {
    return lookup
  }
  for (let index = 0; index < messages.length; index++) {
    const item = messages[index]
    const messageId = Number(item && item.messageId)
    if (!messageId) {
      continue
    }
    lookup.set(messageId, item)
  }
  return lookup
}

function resolveReplyQuoteState(message, messageLookup) {
  const replyToMessageId = Number(message.replyToMessageId) || 0
  if (!replyToMessageId) {
    return {
      replyToMessageId: 0,
      replyToPreview: '',
      replyTargetDisabled: false
    }
  }

  let replyToPreview = String(message.replyToPreview || '').trim()
  let replyTargetDisabled = false
  if (messageLookup instanceof Map) {
    const targetMessage = messageLookup.get(replyToMessageId)
    if (targetMessage && String(targetMessage.msgType || '').toUpperCase() === 'RECALL') {
      replyToPreview = REPLY_RECALLED_TEXT
      replyTargetDisabled = true
    }
  }

  return {
    replyToMessageId,
    replyToPreview,
    replyTargetDisabled
  }
}

function createMessageRow(message, indexToken, currentUserId = 0, messageLookup = null) {
  const msgType = message.msgType || 'TEXT'
  const deliveryStatus = Number(message.deliveryStatus)
  const uploadStatus = String(message.uploadStatus || '').toUpperCase()
  const uploadProgress = Number.isFinite(Number(message.uploadProgress))
    ? Math.max(0, Math.min(100, Math.round(Number(message.uploadProgress))))
    : 0
  const uploadFailed = uploadStatus === 'FAILED'
  const uploading = uploadStatus === 'UPLOADING' || uploadStatus === 'SENDING'
  const isUploadPlaceholder = msgType === 'VIDEO' && (uploading || uploadFailed)
  const deliveryFailed = uploadFailed || (Number.isFinite(deliveryStatus) && deliveryStatus === DELIVERY_STATUS_FAILED)
  const textContent = typeof message.content === 'string' ? message.content : ''
  const mediaUrl = message.mediaUrl || ''
  const linkUrl = extractStandaloneLink(textContent)
  let copyContent = textContent
  if (msgType === 'IMAGE' || msgType === 'VIDEO' || msgType === 'FILE') {
    copyContent = mediaUrl || textContent
  } else if (linkUrl) {
    copyContent = linkUrl
  }
  const replyQuoteState = resolveReplyQuoteState(message, messageLookup)
  const replyToSenderId = Number(message.replyToSenderId) || 0
  return {
    key: `m_${message.messageId || indexToken}`,
    messageId: message.messageId,
    createTime: message.createTime || '',
    msgType,
    recalled: msgType === 'RECALL',
    content: textContent,
    linkUrl,
    copyContent: String(copyContent || ''),
    mediaUrl,
    mediaPosterUrl: message.mediaPosterUrl || '',
    mediaWidth: message.mediaWidth || 0,
    mediaHeight: message.mediaHeight || 0,
    mediaSize: Number(message.mediaSize) || 0,
    mediaMimeType: message.mediaMimeType || '',
    uploadProgress,
    uploadStatus,
    isUploadPlaceholder,
    uploadFailed,
    deliveryStatus: Number.isFinite(deliveryStatus) ? deliveryStatus : 0,
    deliveryFailed,
    imageRenderStyle: buildImageRenderStyle(message.mediaWidth, message.mediaHeight),
    replyToMessageId: replyQuoteState.replyToMessageId,
    replyToSenderId,
    repliedToMe: replyToSenderId > 0 && Number(currentUserId) > 0 && replyToSenderId === Number(currentUserId),
    replyToPreview: replyQuoteState.replyToPreview,
    replyTargetDisabled: replyQuoteState.replyTargetDisabled,
    videoRenderStyle: buildVideoRenderStyle(message.mediaWidth, message.mediaHeight),
    fileLabel: buildFileLabel(message.content || '', message.mediaSize)
  }
}

function isSystemNoticeMessage(message) {
  const msgType = String(message && message.msgType ? message.msgType : '').toUpperCase()
  return msgType === 'SYSTEM'
}

function createSystemNoticeBlock(message, indexToken) {
  const text = String(message && message.content ? message.content : '').trim()
  if (!text) {
    return null
  }
  return {
    type: 'system-notice',
    key: `s_${message.messageId || indexToken}`,
    text,
    createTime: message.createTime || ''
  }
}

function createClusterBlock(message, indexToken, sender, isSelf, currentUserId = 0, messageLookup = null) {
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
    rows: [createMessageRow(message, indexToken, currentUserId, messageLookup)]
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
  const messageLookup = context.messageLookup instanceof Map
    ? context.messageLookup
    : buildMessageLookup([message])

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

  if (isSystemNoticeMessage(message)) {
    const systemNoticeBlock = createSystemNoticeBlock(message, indexToken)
    if (systemNoticeBlock) {
      blocks.push(systemNoticeBlock)
    }
    return blocks
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
      rows: [...(lastBlock.rows || []), createMessageRow(message, indexToken, currentUserId, messageLookup)]
    }
    blocks[blocks.length - 1] = mergedCluster
    return blocks
  }

  blocks.push(createClusterBlock(message, indexToken, sender, isSelf, currentUserId, messageLookup))
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
  const messageLookup = buildMessageLookup(messages)

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

    if (isSystemNoticeMessage(message)) {
      const systemNoticeBlock = createSystemNoticeBlock(message, index)
      if (systemNoticeBlock) {
        blocks.push(systemNoticeBlock)
      }
      previousDateLabel = currentDateLabel
      previousMessage = message
      currentCluster = null
      continue
    }

    const isSelf = Number(message.senderId) === currentUserId
    const sender = resolveSenderProfile(message.senderId, isSelf)
    const senderChanged = !currentCluster || Number(currentCluster.senderId) !== Number(message.senderId)
    const splitByGap = isMessageMergeGapExceeded(previousMessage, message, gapMs)
    if (!currentCluster || senderChanged || splitByGap) {
      currentCluster = createClusterBlock(message, index, sender, isSelf, currentUserId, messageLookup)
      blocks.push(currentCluster)
    } else {
      currentCluster.rows.push(createMessageRow(message, index, currentUserId, messageLookup))
    }

    previousDateLabel = currentDateLabel
    previousMessage = message
  }

  return blocks
}

module.exports = {
  DEFAULT_MESSAGE_MERGE_GAP_MS,
  clearPendingTimer: imRequestHelper.clearPendingTimer,
  resolvePendingRequest: imRequestHelper.resolvePendingRequest,
  rejectPendingRequest: imRequestHelper.rejectPendingRequest,
  resolvePendingByEchoMessage: imRequestHelper.resolvePendingByEchoMessage,
  sendWsMessageWithAck: imRequestHelper.sendWsMessageWithAck,
  scrollToBottom: imLayoutHelper.scrollToBottom,
  clearScrollToBottomTimer: imLayoutHelper.clearScrollToBottomTimer,
  clearRefocusComposerTimer: imLayoutHelper.clearRefocusComposerTimer,
  resetKeyboardHeight: imLayoutHelper.resetKeyboardHeight,
  handleKeyboardHeightChange: imLayoutHelper.handleKeyboardHeightChange,
  handleComposerFocus: imLayoutHelper.handleComposerFocus,
  handleComposerBlur: imLayoutHelper.handleComposerBlur,
  measureDockHeight: imLayoutHelper.measureDockHeight,
  shouldKeepComposerAfterSend: imLayoutHelper.shouldKeepComposerAfterSend,
  refocusComposerInput: imLayoutHelper.refocusComposerInput,
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
