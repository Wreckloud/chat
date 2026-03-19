const time = require('./time')

const ECHO_MATCH_TOLERANCE_MS = 2 * 60 * 1000

function createClientError(message) {
  const error = new Error(message)
  error.expose = true
  return error
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
  pending.reject(error || createClientError('发送失败'))
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
    rejectPendingRequest(page, createClientError('消息发送超时，请重试'), clientMsgId)
  }, timeoutMs)
}

async function sendWsMessageWithAck(page, ws, payload, options = {}) {
  if (page.pendingRequest) {
    throw createClientError('存在未完成的发送请求，请稍后重试')
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

module.exports = {
  clearPendingTimer,
  resolvePendingRequest,
  rejectPendingRequest,
  resolvePendingByEchoMessage,
  sendWsMessageWithAck
}
