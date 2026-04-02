const imHelper = require('./im-helper')
const imBatchSendHelper = require('./im-batch-send-helper')
const { toastError } = require('./ui')

const DEFAULT_MORE_ACTIONS = [
  { key: 'image', label: '多图', icon: '图', subLabel: '相册' },
  { key: 'video', label: '视频', icon: '视', subLabel: '相册' },
  { key: 'file', label: '文件', icon: '档', subLabel: '多选' },
  { key: 'link', label: '链接', icon: '链', subLabel: '发送' }
]
const MEDIA_UPLOAD_STEP_PERCENT = 10
const MEDIA_UPLOAD_STATUS_UPLOADING = 'UPLOADING'
const MEDIA_UPLOAD_STATUS_SENDING = 'SENDING'
const MEDIA_UPLOAD_STATUS_FAILED = 'FAILED'

function buildBaseSendPayload(page, msgType, extra = {}) {
  const sendType = String(page && page.IM_SEND_TYPE ? page.IM_SEND_TYPE : 'SEND').toUpperCase()
  const payload = {
    type: sendType,
    msgType,
    ...extra
  }
  if (sendType === 'SEND') {
    payload.conversationId = Number(page.data.conversationId)
  }
  return payload
}

function createUploadClientMsgId(prefix = 'u') {
  return `${prefix}_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`
}

function clampProgress(progress) {
  const value = Number(progress)
  if (!Number.isFinite(value)) {
    return 0
  }
  return Math.max(0, Math.min(100, Math.round(value)))
}

function resolveUploadFailureMessage(error, fallback = '视频上传失败') {
  if (typeof error === 'string' && error.trim()) {
    return error.trim()
  }
  if (error && typeof error.message === 'string' && error.message.trim()) {
    return error.message.trim()
  }
  return fallback
}

function resolveUploadStatusText(uploadStatus, uploadProgress, uploadErrorMessage) {
  if (uploadStatus === MEDIA_UPLOAD_STATUS_FAILED) {
    return resolveUploadFailureMessage(uploadErrorMessage, '视频上传失败')
  }
  if (uploadStatus === MEDIA_UPLOAD_STATUS_SENDING) {
    return '视频上传完成，正在发送'
  }
  return `视频上传中 ${uploadProgress}%`
}

function ensureUploadPlaceholderDraft(page, clientMsgId, tempFile = {}) {
  if (!page.uploadPlaceholderDraftMap) {
    page.uploadPlaceholderDraftMap = {}
  }
  const currentUser = page.currentUser || {}
  const existing = page.uploadPlaceholderDraftMap[clientMsgId] || {}
  const nextDraft = {
    createTime: existing.createTime || new Date(),
    senderId: Number(existing.senderId || page.currentUserId) || 0,
    senderWolfNo: String(existing.senderWolfNo || currentUser.wolfNo || ''),
    senderNickname: String(existing.senderNickname || currentUser.nickname || ''),
    senderAvatar: String(existing.senderAvatar || currentUser.avatar || ''),
    senderEquippedTitleName: String(existing.senderEquippedTitleName || currentUser.equippedTitleName || ''),
    senderEquippedTitleColor: String(existing.senderEquippedTitleColor || currentUser.equippedTitleColor || ''),
    mediaWidth: Number(existing.mediaWidth || tempFile.width) || 0,
    mediaHeight: Number(existing.mediaHeight || tempFile.height) || 0,
    mediaSize: Number(existing.mediaSize || tempFile.size) || 0,
    mediaDuration: Number(existing.mediaDuration || tempFile.duration) || 0,
    tempFilePath: String(existing.tempFilePath || tempFile.tempFilePath || ''),
    thumbTempFilePath: String(existing.thumbTempFilePath || tempFile.thumbTempFilePath || '')
  }
  page.uploadPlaceholderDraftMap[clientMsgId] = nextDraft
  return nextDraft
}

function clearUploadPlaceholderDraft(page, clientMsgId) {
  if (!page || !page.uploadPlaceholderDraftMap) {
    return
  }
  delete page.uploadPlaceholderDraftMap[clientMsgId]
}

function upsertVideoUploadPlaceholder(page, options = {}) {
  if (!page || typeof page.appendMessage !== 'function') {
    return
  }
  const clientMsgId = String(options.clientMsgId || '').trim()
  if (!clientMsgId) {
    return
  }
  const draft = ensureUploadPlaceholderDraft(page, clientMsgId, options.tempFile || {})
  const uploadStatus = String(options.uploadStatus || MEDIA_UPLOAD_STATUS_UPLOADING).toUpperCase()
  const uploadProgress = clampProgress(options.uploadProgress)
  const uploadErrorMessage = resolveUploadFailureMessage(options.uploadErrorMessage, '')
  const sendType = String(page.IM_SEND_TYPE || 'SEND').toUpperCase()
  const conversationId = sendType === 'SEND'
    ? (Number(page.data && page.data.conversationId) || 0)
    : 0

  page.appendMessage({
    clientMsgId,
    conversationId,
    senderId: draft.senderId,
    senderWolfNo: draft.senderWolfNo,
    senderNickname: draft.senderNickname,
    senderAvatar: draft.senderAvatar,
    senderEquippedTitleName: draft.senderEquippedTitleName,
    senderEquippedTitleColor: draft.senderEquippedTitleColor,
    msgType: 'VIDEO',
    content: resolveUploadStatusText(uploadStatus, uploadProgress, uploadErrorMessage),
    mediaWidth: draft.mediaWidth,
    mediaHeight: draft.mediaHeight,
    mediaSize: draft.mediaSize,
    mediaMimeType: 'video/mp4',
    uploadProgress,
    uploadStatus,
    uploadErrorMessage,
    deliveryStatus: uploadStatus === MEDIA_UPLOAD_STATUS_FAILED ? 2 : 0,
    createTime: draft.createTime
  })
}

function emitUploadProgress(page, options = {}) {
  if (!page || typeof page.sendWsUploadProgress !== 'function') {
    return
  }
  const clientMsgId = String(options.clientMsgId || '').trim()
  if (!clientMsgId) {
    return
  }
  page.sendWsUploadProgress({
    clientMsgId,
    msgType: 'VIDEO',
    uploadProgress: clampProgress(options.uploadProgress),
    uploadStatus: String(options.uploadStatus || MEDIA_UPLOAD_STATUS_UPLOADING).toUpperCase()
  })
}

function getProgressSendBucket(progress) {
  return Math.floor(clampProgress(progress) / MEDIA_UPLOAD_STEP_PERCENT)
}

async function waitForPendingSendDrain(page, timeoutMs = 5000) {
  if (!page) {
    return
  }
  const deadline = Date.now() + Math.max(200, Number(timeoutMs) || 5000)
  while (page.pendingRequest && Date.now() < deadline) {
    // 文本消息 ACK 通常在几十毫秒内返回，这里仅做短暂等待避免与媒体发送竞争。
    // eslint-disable-next-line no-await-in-loop
    await new Promise(resolve => setTimeout(resolve, 60))
  }
}

function applyUploadProgressPayload(page, payload) {
  if (!page || !payload || typeof page.appendMessage !== 'function') {
    return false
  }
  const msgType = String(payload.msgType || '').toUpperCase()
  if (msgType !== 'VIDEO') {
    return false
  }
  const clientMsgId = String(payload.clientMsgId || '').trim()
  if (!clientMsgId) {
    return false
  }
  const sendType = String(payload.sendType || '').toUpperCase()
  const pageSendType = String(page.IM_SEND_TYPE || 'SEND').toUpperCase()
  if (!sendType || sendType !== pageSendType) {
    return false
  }
  if (sendType === 'SEND') {
    const pageConversationId = Number(page.data && page.data.conversationId) || 0
    const payloadConversationId = Number(payload.conversationId) || 0
    if (!pageConversationId || payloadConversationId !== pageConversationId) {
      return false
    }
  }
  const senderId = Number(payload.senderId) || 0
  if (senderId > 0 && Number(page.currentUserId) === senderId) {
    return false
  }

  const uploadStatus = String(payload.uploadStatus || MEDIA_UPLOAD_STATUS_UPLOADING).toUpperCase()
  const uploadProgress = clampProgress(payload.uploadProgress)
  const uploadErrorMessage = resolveUploadFailureMessage(payload.uploadErrorMessage, '')
  const conversationId = sendType === 'SEND' ? (Number(payload.conversationId) || 0) : 0
  if (!page.remoteUploadPlaceholderTimeMap) {
    page.remoteUploadPlaceholderTimeMap = {}
  }
  if (!page.remoteUploadPlaceholderTimeMap[clientMsgId]) {
    page.remoteUploadPlaceholderTimeMap[clientMsgId] = payload.createTime || new Date()
  }
  const createTime = page.remoteUploadPlaceholderTimeMap[clientMsgId]
  page.appendMessage({
    clientMsgId,
    conversationId,
    senderId,
    senderWolfNo: String(payload.senderWolfNo || ''),
    senderNickname: String(payload.senderNickname || ''),
    senderAvatar: String(payload.senderAvatar || ''),
    senderEquippedTitleName: String(payload.senderEquippedTitleName || ''),
    senderEquippedTitleColor: String(payload.senderEquippedTitleColor || ''),
    msgType: 'VIDEO',
    content: resolveUploadStatusText(uploadStatus, uploadProgress, uploadErrorMessage),
    uploadProgress,
    uploadStatus,
    uploadErrorMessage,
    deliveryStatus: uploadStatus === MEDIA_UPLOAD_STATUS_FAILED ? 2 : 0,
    createTime
  })
  if (uploadStatus === MEDIA_UPLOAD_STATUS_FAILED || uploadStatus === MEDIA_UPLOAD_STATUS_SENDING) {
    delete page.remoteUploadPlaceholderTimeMap[clientMsgId]
  }
  return true
}

function beginSending(page) {
  if (!page || !page.data || page.data.sending) {
    return false
  }
  page.setData({ sending: true })
  return true
}

function endSending(page) {
  if (!page || page.pageUnloaded || !page.data || !page.data.sending) {
    return
  }
  page.setData({ sending: false })
}

async function sendTextMessage(page, content, options = {}) {
  const extraPayload = options && options.extraPayload ? options.extraPayload : {}
  return page.sendWsMessageWithAck(
    buildBaseSendPayload(page, 'TEXT', { content, ...extraPayload }),
    options
  )
}

async function chooseImageFromAlbum(page, deps) {
  if (!page || !page.data || page.data.sending) {
    return
  }

  let didBeginSending = false
  try {
    const chooseRes = await imHelper.chooseMedia({
      count: 9,
      mediaType: ['image'],
      sourceType: ['album']
    })
    const tempFiles = Array.isArray(chooseRes.tempFiles) ? chooseRes.tempFiles : []
    if (tempFiles.length === 0) {
      return
    }

    if (!beginSending(page)) {
      return
    }
    didBeginSending = true
    const batchResult = await imBatchSendHelper.sendUploadedMediaBatch(page, tempFiles, {
      upload: deps.uploadImage,
      buildPayload: (media) => buildBaseSendPayload(page, 'IMAGE', {
        mediaKey: media.mediaKey,
        mediaWidth: media.mediaWidth,
        mediaHeight: media.mediaHeight,
        mediaSize: media.mediaSize,
        mediaMimeType: media.mediaMimeType
      })
    })
    if (!page.pageUnloaded) {
      imBatchSendHelper.showBatchFailureToast(batchResult.failCount, batchResult.totalCount)
    }
  } catch (error) {
    if (page.pageUnloaded) {
      return
    }
    if (imHelper.isUserCancelError(error)) {
      return
    }
    imBatchSendHelper.showBatchFailureToast(1, 1)
  } finally {
    if (didBeginSending) {
      endSending(page)
    }
  }
}

async function chooseVideoFromAlbum(page, deps) {
  if (!page || !page.data || page.data.sending) {
    return
  }

  try {
    const chooseRes = await imHelper.chooseMedia({
      count: 1,
      mediaType: ['video'],
      sourceType: ['album']
    })
    const tempFile = Array.isArray(chooseRes.tempFiles) ? chooseRes.tempFiles[0] : null
    if (!tempFile || !tempFile.tempFilePath) {
      return
    }
    startVideoUploadTask(page, deps, tempFile, { clientMsgId: createUploadClientMsgId('v') })
  } catch (error) {
    if (page.pageUnloaded) {
      return
    }
    if (imHelper.isUserCancelError(error)) {
      return
    }
    toastError(error, '视频上传失败')
  }
}

function startVideoUploadTask(page, deps, tempFile, options = {}) {
  if (!page || !deps || typeof deps.uploadVideo !== 'function' || !tempFile || !tempFile.tempFilePath) {
    return ''
  }
  const clientMsgId = String(options.clientMsgId || '').trim() || createUploadClientMsgId('v')
  upsertVideoUploadPlaceholder(page, {
    clientMsgId,
    tempFile,
    uploadProgress: 0,
    uploadStatus: MEDIA_UPLOAD_STATUS_UPLOADING
  })
  emitUploadProgress(page, {
    clientMsgId,
    uploadProgress: 0,
    uploadStatus: MEDIA_UPLOAD_STATUS_UPLOADING
  })

  // 视频上传改为异步进行，避免阻塞输入与继续编辑。
  let lastBucket = getProgressSendBucket(0)
  deps.uploadVideo(tempFile, {
    onProgress: (progress) => {
      if (page.pageUnloaded) {
        return
      }
      const currentProgress = clampProgress(progress)
      upsertVideoUploadPlaceholder(page, {
        clientMsgId,
        tempFile,
        uploadProgress: currentProgress,
        uploadStatus: MEDIA_UPLOAD_STATUS_UPLOADING
      })
      const currentBucket = getProgressSendBucket(currentProgress)
      if (currentBucket === lastBucket && currentProgress < 100) {
        return
      }
      lastBucket = currentBucket
      emitUploadProgress(page, {
        clientMsgId,
        uploadProgress: currentProgress,
        uploadStatus: MEDIA_UPLOAD_STATUS_UPLOADING
      })
    }
  })
    .then(async (media) => {
      if (page.pageUnloaded) {
        return
      }
      upsertVideoUploadPlaceholder(page, {
        clientMsgId,
        tempFile,
        uploadProgress: 100,
        uploadStatus: MEDIA_UPLOAD_STATUS_SENDING
      })
      emitUploadProgress(page, {
        clientMsgId,
        uploadProgress: 100,
        uploadStatus: MEDIA_UPLOAD_STATUS_SENDING
      })
      await waitForPendingSendDrain(page)
      await page.sendWsMessageWithAck(
        buildBaseSendPayload(page, 'VIDEO', {
          mediaKey: media.mediaKey,
          mediaPosterKey: media.mediaPosterKey,
          mediaWidth: media.mediaWidth,
          mediaHeight: media.mediaHeight,
          mediaSize: media.mediaSize,
          mediaMimeType: media.mediaMimeType
        }),
        { clientMsgId }
      )
      clearUploadPlaceholderDraft(page, clientMsgId)
    })
    .catch((error) => {
      if (page.pageUnloaded) {
        return
      }
      const failureMessage = resolveUploadFailureMessage(error, '视频上传失败')
      upsertVideoUploadPlaceholder(page, {
        clientMsgId,
        tempFile,
        uploadProgress: 100,
        uploadStatus: MEDIA_UPLOAD_STATUS_FAILED,
        uploadErrorMessage: failureMessage
      })
      emitUploadProgress(page, {
        clientMsgId,
        uploadProgress: 100,
        uploadStatus: MEDIA_UPLOAD_STATUS_FAILED
      })
      toastError(error, failureMessage)
    })

  return clientMsgId
}

async function retryFailedVideoUpload(page, deps, clientMsgId) {
  const normalizedClientMsgId = String(clientMsgId || '').trim()
  if (!normalizedClientMsgId) {
    throw new Error('无可重发的视频记录')
  }
  const draftMap = page && page.uploadPlaceholderDraftMap ? page.uploadPlaceholderDraftMap : null
  const draft = draftMap ? draftMap[normalizedClientMsgId] : null
  const tempFilePath = draft ? String(draft.tempFilePath || '').trim() : ''
  if (!tempFilePath) {
    throw new Error('原视频文件已失效，请重新选择')
  }
  const tempFile = {
    tempFilePath,
    width: Number(draft.mediaWidth) || 0,
    height: Number(draft.mediaHeight) || 0,
    size: Number(draft.mediaSize) || 0,
    duration: Number(draft.mediaDuration) || 0,
    thumbTempFilePath: String(draft.thumbTempFilePath || '')
  }
  startVideoUploadTask(page, deps, tempFile, { clientMsgId: normalizedClientMsgId })
}

async function chooseFileForShare(page, deps) {
  if (!page || !page.data || page.data.sending) {
    return
  }

  let didBeginSending = false
  try {
    const chooseRes = await imHelper.chooseMessageFile({
      count: 9,
      type: 'file'
    })
    const selectedFiles = Array.isArray(chooseRes.tempFiles)
      ? chooseRes.tempFiles.filter(Boolean)
      : []
    if (selectedFiles.length === 0) {
      return
    }

    if (!beginSending(page)) {
      return
    }
    didBeginSending = true
    const batchResult = await imBatchSendHelper.sendUploadedMediaBatch(page, selectedFiles, {
      upload: deps.uploadFile,
      buildPayload: (media) => buildBaseSendPayload(page, 'FILE', {
        content: imHelper.normalizeFileNameForMessage(media.fileName),
        mediaKey: media.mediaKey,
        mediaSize: media.mediaSize,
        mediaMimeType: media.mediaMimeType
      })
    })
    if (!page.pageUnloaded) {
      imBatchSendHelper.showBatchFailureToast(batchResult.failCount, batchResult.totalCount)
    }
  } catch (error) {
    if (page.pageUnloaded) {
      return
    }
    if (imHelper.isUserCancelError(error)) {
      return
    }
    imBatchSendHelper.showBatchFailureToast(1, 1)
  } finally {
    if (didBeginSending) {
      endSending(page)
    }
  }
}

async function shareLinkAsText(page) {
  const modalRes = await imHelper.showEditableModal({
    title: '分享链接',
    placeholderText: '请输入链接地址'
  })
  if (!modalRes.confirm) {
    return
  }

  const link = imHelper.normalizeSharedLink(modalRes.content)
  if (!link) {
    toastError('链接格式不正确')
    return
  }

  if (!page || !page.data || page.data.sending) {
    return
  }

  if (!beginSending(page)) {
    return
  }
  let didBeginSending = true
  try {
    await sendTextMessage(page, link)
  } catch (error) {
    if (page.pageUnloaded) {
      return
    }
    imBatchSendHelper.showBatchFailureToast(1, 1)
  } finally {
    if (didBeginSending) {
      endSending(page)
    }
  }
}

function onTapLink(e) {
  const url = e && e.currentTarget && e.currentTarget.dataset
    ? String(e.currentTarget.dataset.url || '')
    : ''
  if (!url) {
    return
  }
  wx.setClipboardData({
    data: url
  })
}

function onTapFile(page, e, toastError) {
  const dataset = e && e.currentTarget ? e.currentTarget.dataset || {} : {}
  const url = String(dataset.url || '')
  if (!url) {
    toastError('文件地址无效')
    return
  }
  const fileName = String(dataset.name || '').trim() || 'file'

  wx.showLoading({ title: '打开中...', mask: true })
  imHelper.downloadTempFile(url)
    .then(downloadRes => {
      if (downloadRes.statusCode < 200 || downloadRes.statusCode >= 300 || !downloadRes.tempFilePath) {
        throw new Error(`文件下载失败(${downloadRes.statusCode || 0})`)
      }
      return imHelper.openDocument(downloadRes.tempFilePath, fileName)
    })
    .catch(error => {
      if (page.pageUnloaded) {
        return
      }
      toastError(error, '文件打开失败')
    })
    .finally(() => {
      wx.hideLoading()
    })
}

async function onMoreActionTap(page, e, handlers) {
  const key = e && e.currentTarget && e.currentTarget.dataset
    ? String(e.currentTarget.dataset.key || '')
    : ''
  if (!key) {
    return
  }
  page.setMorePanelVisible(false)

  if (key === 'image') {
    await handlers.image()
    return
  }
  if (key === 'video') {
    await handlers.video()
    return
  }
  if (key === 'file') {
    await handlers.file()
    return
  }
  if (key === 'link') {
    await handlers.link()
  }
}

module.exports = {
  DEFAULT_MORE_ACTIONS,
  sendTextMessage,
  chooseImageFromAlbum,
  chooseVideoFromAlbum,
  chooseFileForShare,
  shareLinkAsText,
  onTapLink,
  onTapFile,
  onMoreActionTap,
  applyUploadProgressPayload,
  retryFailedVideoUpload
}
