const imHelper = require('./im-helper')
const imMessageHelper = require('./im-message-helper')
const pageLifecycleHelper = require('./page-lifecycle-helper')

const CONNECTION_TIP_MAP = {
  CONNECTING: '连接中...',
  AUTHING: '认证中...',
  RECONNECTING: '网络波动，重连中...',
  DISCONNECTED: '连接已断开',
  READY: ''
}

function resolveConnectionTip(state) {
  const key = String(state || '').toUpperCase()
  return CONNECTION_TIP_MAP[key] || ''
}

function clearSendStatusTimer(page) {
  if (!page || !page.sendStatusTimer) {
    return
  }
  clearTimeout(page.sendStatusTimer)
  page.sendStatusTimer = null
}

function setSendStatus(page, text, autoClearMs = 0) {
  if (!page || !page.data || !Object.prototype.hasOwnProperty.call(page.data, 'sendStatusText')) {
    return
  }
  clearSendStatusTimer(page)
  if (page.data.sendStatusText !== text) {
    page.setData({ sendStatusText: text })
  }
  if (autoClearMs > 0) {
    page.sendStatusTimer = setTimeout(() => {
      page.sendStatusTimer = null
      if (page.pageUnloaded || !page.data) {
        return
      }
      if (page.data.sendStatusText) {
        page.setData({ sendStatusText: '' })
      }
    }, autoClearMs)
  }
}

function initSocket(page, ws, onMessage) {
  if (page.wsHandler) {
    return
  }
  ws.connect()
  page.wsHandler = (payload) => {
    onMessage(payload)
  }
  ws.onMessage(page.wsHandler)

  if (Object.prototype.hasOwnProperty.call(page.data || {}, 'connectionTip')) {
    page.wsStateHandler = (state) => {
      const nextTip = resolveConnectionTip(state)
      if (nextTip !== page.data.connectionTip) {
        page.setData({ connectionTip: nextTip })
      }
    }
    ws.onStateChange(page.wsStateHandler)
    page.wsStateHandler(ws.getState())
  }
}

function teardownSocket(page, ws) {
  if (page.wsStateHandler) {
    ws.offStateChange(page.wsStateHandler)
    page.wsStateHandler = null
  }
  clearSendStatusTimer(page)
  if (!page.wsHandler) {
    return
  }
  ws.offMessage(page.wsHandler)
  page.wsHandler = null
}

function cleanupPage(page, ws, options = {}) {
  if (!page) {
    return
  }
  page.pageUnloaded = true
  imHelper.resetKeyboardHeight(page)
  imHelper.clearScrollToBottomTimer(page)
  imHelper.clearRefocusComposerTimer(page)
  imHelper.rejectPendingRequest(page, new Error('页面已关闭'))
  imHelper.clearPendingTimer(page)
  if (typeof options.beforeTeardown === 'function') {
    options.beforeTeardown()
  }
  teardownSocket(page, ws)
}

function handlePageReady(page) {
  if (!page) {
    return
  }
  imHelper.measureDockHeight(page)
}

function handlePageLoad(page, auth, options = {}) {
  if (!page) {
    return false
  }
  return pageLifecycleHelper.handleProtectedPageLoad(auth, {
    beforeInit: options.beforeInit,
    afterInit: options.afterInit
  })
}

function handlePageShow(page, auth, options = {}) {
  if (!page) {
    return false
  }
  return pageLifecycleHelper.handleProtectedPageShow(auth, {
    beforeShow: options.beforeShow,
    afterShow: () => {
      imHelper.measureDockHeight(page)
      if (typeof options.afterShow === 'function') {
        options.afterShow()
      }
    }
  })
}

function loadCurrentUserProfile(page, options = {}) {
  if (!page) {
    return Promise.resolve()
  }
  const loadFn = typeof options.loadFn === 'function' ? options.loadFn : null
  const cacheUserProfile = typeof options.cacheUserProfile === 'function'
    ? options.cacheUserProfile
    : null
  const buildMessageBlocks = typeof options.buildMessageBlocks === 'function'
    ? options.buildMessageBlocks
    : (typeof page.buildMessageBlocks === 'function'
        ? (messages) => page.buildMessageBlocks(messages)
        : null)

  if (!loadFn) {
    return Promise.resolve()
  }

  return loadFn((user) => {
    if (cacheUserProfile) {
      cacheUserProfile(user)
    }
    const messages = Array.isArray(page.data && page.data.messages)
      ? page.data.messages
      : []
    if (messages.length > 0 && buildMessageBlocks) {
      page.setData({
        messageBlocks: buildMessageBlocks(messages)
      })
    }
    if (typeof options.onLoaded === 'function') {
      options.onLoaded(user)
    }
  })
}

function loadMessages(page, request, options = {}) {
  if (!page || !request || typeof request.get !== 'function' || !options.url) {
    return Promise.resolve()
  }
  return imMessageHelper.loadPagedMessages(page, request, {
    url: options.url,
    parseRecords: typeof options.parseRecords === 'function'
      ? options.parseRecords
      : (res) => (res && res.data && res.data.records) || [],
    buildMessageBlocks: (messages) => page.buildMessageBlocks(messages),
    onLoaded: (payload) => {
      if (typeof page.ensureSenderProfiles === 'function') {
        page.ensureSenderProfiles(payload.messages)
      }
      if (typeof options.onLoaded === 'function') {
        options.onLoaded(payload)
      }
    },
    onFirstPageLoaded: (messages) => {
      if (typeof page.scrollToBottom === 'function') {
        page.scrollToBottom()
      }
      if (typeof options.onFirstPageLoaded === 'function') {
        options.onFirstPageLoaded(messages)
      }
    },
    onError: (err) => {
      if (typeof options.onError === 'function') {
        options.onError(err)
      }
    }
  })
}

function onMessageInput(page, event) {
  const value = event && event.detail ? event.detail.value : ''
  const nextData = {
    inputMessage: value
  }
  if (page.data.sendStatusText) {
    nextData.sendStatusText = ''
  }
  page.setData(nextData)
}

function onKeyboardHeightChange(page, event) {
  imHelper.handleKeyboardHeightChange(page, event, () => setMorePanelVisible(page, false))
}

function onComposerFocus(page, event) {
  imHelper.handleComposerFocus(page, event, () => setMorePanelVisible(page, false))
}

function onComposerBlur(page) {
  imHelper.handleComposerBlur(page)
}

function setMorePanelVisible(page, visible) {
  const nextVisible = Boolean(visible)
  if (nextVisible === page.data.morePanelVisible) {
    return
  }
  page.setData({
    morePanelVisible: nextVisible
  }, () => {
    imHelper.measureDockHeight(page)
  })
}

function toggleMorePanel(page) {
  if (page.data.sending) {
    return
  }
  const nextVisible = !page.data.morePanelVisible
  if (nextVisible) {
    wx.hideKeyboard()
    imHelper.resetKeyboardHeight(page)
  }
  setMorePanelVisible(page, nextVisible)
}

function getSendDeps(page, depsFactory) {
  if (page.imSendDeps) {
    return page.imSendDeps
  }
  page.imSendDeps = depsFactory()
  return page.imSendDeps
}

function getMediaSendDeps(page, uploaders, toastError) {
  return getSendDeps(page, () => ({
    uploadImage: uploaders.uploadImage,
    uploadVideo: uploaders.uploadVideo,
    uploadFile: uploaders.uploadFile,
    toastError
  }))
}

async function onDefaultMoreActionTap(page, event, imSendHelper, uploaders, toastError) {
  const deps = getMediaSendDeps(page, uploaders, toastError)
  await imSendHelper.onMoreActionTap(page, event, {
    image: () => imSendHelper.chooseImageFromAlbum(page, deps),
    video: () => imSendHelper.chooseVideoFromAlbum(page, deps),
    file: () => imSendHelper.chooseFileForShare(page, deps),
    link: () => imSendHelper.shareLinkAsText(page, deps)
  })
}

function onSendButtonTap(page, sendFn) {
  page.keepComposerOpenAfterSend = imHelper.shouldKeepComposerAfterSend(page)
  if (typeof sendFn === 'function') {
    sendFn()
  }
}

async function sendComposerText(page, sendTextFn, toastError) {
  const content = String(page.data.inputMessage || '').trim()
  if (!content) {
    page.keepComposerOpenAfterSend = false
    toastError('消息内容不能为空')
    return
  }

  if (page.data.sending) {
    page.keepComposerOpenAfterSend = false
    return
  }

  const keepComposerOpen = page.keepComposerOpenAfterSend || imHelper.shouldKeepComposerAfterSend(page)
  page.keepComposerOpenAfterSend = false
  setSendStatus(page, '发送中...')
  page.setData({ sending: true })
  try {
    await sendTextFn(content)
    page.lastFailedInputContent = ''
    setSendStatus(page, '')
  } catch (error) {
    if (page.pageUnloaded) {
      return
    }
    page.lastFailedInputContent = content
    setSendStatus(page, '发送失败，点击重试')
    toastError(error, '发送失败')
  } finally {
    page.setData({ sending: false })
    if (keepComposerOpen) {
      imHelper.refocusComposerInput(page)
    }
  }
}

async function sendComposerTextMessage(page, imSendHelper, toastError) {
  return sendComposerText(
    page,
    (content) => imSendHelper.sendTextMessage(page, content, {
      clearInputOnSuccess: true
    }),
    toastError
  )
}

function previewImage(page, event) {
  const current = event && event.currentTarget && event.currentTarget.dataset
    ? event.currentTarget.dataset.url
    : ''
  if (!current) {
    return
  }

  const urls = (page.data.messages || [])
    .filter(item => item.msgType === 'IMAGE' && item.mediaUrl)
    .map(item => item.mediaUrl)

  wx.previewImage({
    current,
    urls: urls.length > 0 ? urls : [current]
  })
}

function onMessageListUpper(page, loadMessagesFn) {
  if (!page || !page.data || page.data.loading || !page.data.hasMore) {
    return
  }
  if (typeof loadMessagesFn === 'function') {
    loadMessagesFn()
  }
}

function onSendStatusTap(page, sendFn) {
  if (!page || page.data.sending) {
    return
  }
  if (!String(page.data.sendStatusText || '').includes('点击重试')) {
    return
  }
  if (!String(page.data.inputMessage || '').trim() && page.lastFailedInputContent) {
    page.setData({
      inputMessage: page.lastFailedInputContent
    })
  }
  if (typeof sendFn === 'function') {
    sendFn()
  }
}

module.exports = {
  initSocket,
  teardownSocket,
  cleanupPage,
  handlePageLoad,
  handlePageReady,
  handlePageShow,
  loadCurrentUserProfile,
  loadMessages,
  onMessageInput,
  onKeyboardHeightChange,
  onComposerFocus,
  onComposerBlur,
  setMorePanelVisible,
  toggleMorePanel,
  onDefaultMoreActionTap,
  onSendButtonTap,
  onSendStatusTap,
  setSendStatus,
  sendComposerTextMessage,
  onMessageListUpper,
  previewImage
}
