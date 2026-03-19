const imHelper = require('./im-helper')
const imMessageHelper = require('./im-message-helper')
const pageLifecycleHelper = require('./page-lifecycle-helper')

function initSocket(page, ws, onMessage) {
  if (page.wsHandler) {
    return
  }
  ws.connect()
  page.wsHandler = (payload) => {
    onMessage(payload)
  }
  ws.onMessage(page.wsHandler)
}

function teardownSocket(page, ws) {
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
  page.keepComposerOpenUntilSendFinish = false
  page.messageIdSet = null
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
  if (!page || !page.data) {
    return
  }

  const nextCanSendText = String(value || '').trim().length > 0
  if (page.data.inputMessage === value
    && (!Object.prototype.hasOwnProperty.call(page.data, 'canSendText')
      || page.data.canSendText === nextCanSendText)) {
    return
  }

  const nextData = { inputMessage: value }
  if (Object.prototype.hasOwnProperty.call(page.data, 'canSendText')) {
    nextData.canSendText = nextCanSendText
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

function getMediaSendDeps(page, uploaders) {
  return getSendDeps(page, () => ({
    uploadImage: uploaders.uploadImage,
    uploadVideo: uploaders.uploadVideo,
    uploadFile: uploaders.uploadFile
  }))
}

async function onDefaultMoreActionTap(page, event, imSendHelper, uploaders) {
  const deps = getMediaSendDeps(page, uploaders)
  await imSendHelper.onMoreActionTap(page, event, {
    image: () => imSendHelper.chooseImageFromAlbum(page, deps),
    video: () => imSendHelper.chooseVideoFromAlbum(page, deps),
    file: () => imSendHelper.chooseFileForShare(page, deps),
    link: () => imSendHelper.shareLinkAsText(page)
  })
}

function onSendButtonTap(page, sendFn) {
  page.keepComposerOpenAfterSend = imHelper.shouldKeepComposerAfterSend(page)
  if (page.keepComposerOpenAfterSend) {
    imHelper.refocusComposerInput(page)
  }
  if (typeof sendFn === 'function') {
    sendFn()
  }
}

async function sendComposerText(page, sendTextFn) {
  const content = String(page.data.inputMessage || '').trim()
  if (!content) {
    page.keepComposerOpenAfterSend = false
    return
  }

  if (page.data.sending) {
    page.keepComposerOpenAfterSend = false
    return
  }

  const keepComposerOpen = page.keepComposerOpenAfterSend || imHelper.shouldKeepComposerAfterSend(page)
  page.keepComposerOpenAfterSend = false
  page.keepComposerOpenUntilSendFinish = keepComposerOpen
  page.setData({ sending: true })
  if (keepComposerOpen) {
    imHelper.refocusComposerInput(page)
  }
  try {
    await sendTextFn(content)
  } catch (error) {
    if (page.pageUnloaded) {
      return
    }
  } finally {
    page.keepComposerOpenUntilSendFinish = false
    page.setData({ sending: false })
    if (keepComposerOpen) {
      imHelper.refocusComposerInput(page)
    }
  }
}

async function sendComposerTextMessage(page, imSendHelper, options = {}) {
  const buildExtraPayload = typeof options.buildExtraPayload === 'function'
    ? options.buildExtraPayload
    : null
  const onSuccess = typeof options.onSuccess === 'function'
    ? options.onSuccess
    : null
  return sendComposerText(
    page,
    async (content) => {
      const extraPayload = buildExtraPayload ? (buildExtraPayload() || {}) : {}
      await imSendHelper.sendTextMessage(page, content, {
        clearInputOnSuccess: true,
        extraPayload
      })
      if (onSuccess) {
        onSuccess(content)
      }
    }
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

function onLongPressMessage(page, event) {
  if (!page || !event || !event.currentTarget || !event.currentTarget.dataset) {
    return
  }
  const content = String(event.currentTarget.dataset.copy || '').trim()
  if (!content) {
    return
  }
  wx.setClipboardData({
    data: content
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
  sendComposerTextMessage,
  onLongPressMessage,
  onMessageListUpper,
  previewImage
}
