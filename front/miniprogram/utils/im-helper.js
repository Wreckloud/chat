const time = require('./time')
const imLayoutHelper = require('./im-layout-helper')
const imRequestHelper = require('./im-request-helper')

const DEFAULT_MESSAGE_MERGE_GAP_MS = 5 * 60 * 1000
const VIDEO_MAX_WIDTH_RPX = 420
const VIDEO_MAX_HEIGHT_RPX = 520
const VIDEO_MIN_WIDTH_RPX = 220
const VIDEO_MIN_HEIGHT_RPX = 180
const FILE_NAME_MAX_LENGTH = 120

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
    replyToMessageId: Number(message.replyToMessageId) || 0,
    replyToSenderId: Number(message.replyToSenderId) || 0,
    replyToPreview: String(message.replyToPreview || '').trim(),
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
