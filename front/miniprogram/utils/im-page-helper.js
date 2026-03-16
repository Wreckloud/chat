const imHelper = require('./im-helper')

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

function handlePageShow(page, auth, options = {}) {
  if (!page || !auth || typeof auth.requireLogin !== 'function') {
    return false
  }
  if (!auth.requireLogin()) {
    return false
  }
  if (typeof options.applyTheme === 'function') {
    options.applyTheme()
  }
  imHelper.measureDockHeight(page)
  if (typeof options.afterShow === 'function') {
    options.afterShow()
  }
  return true
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
  handlePageReady,
  handlePageShow,
  onMessageInput,
  onKeyboardHeightChange,
  onComposerFocus,
  onComposerBlur,
  setMorePanelVisible,
  toggleMorePanel,
  getSendDeps,
  onSendButtonTap,
  onSendStatusTap,
  setSendStatus,
  sendComposerText,
  previewImage,
  resolveConnectionTip
}
