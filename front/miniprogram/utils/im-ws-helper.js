const imHelper = require('./im-helper')

function getPayloadType(payload) {
  if (!payload || !payload.type) {
    return ''
  }
  return String(payload.type).toUpperCase()
}

function notifyWsError(toastError, errorMessage, fallbackMessage) {
  if (typeof toastError !== 'function') {
    return
  }
  toastError(errorMessage, fallbackMessage)
}

function handleWsError(page, payload, toastError) {
  const pageData = page && page.data ? page.data : {}
  const errorMessage = payload && payload.message ? payload.message : '发送失败'
  const clientMsgId = payload && payload.clientMsgId ? String(payload.clientMsgId) : ''

  // 仅 clientMsgId 对应的 ERROR 参与发送链路兜底。
  if (clientMsgId) {
    const resolved = imHelper.rejectPendingRequest(page, new Error(errorMessage), clientMsgId)
    if (!resolved && !pageData.sending) {
      notifyWsError(toastError, errorMessage, '请求异常')
    }
    return true
  }

  if (!pageData.sending) {
    notifyWsError(toastError, errorMessage, '请求异常')
    return true
  }

  // 服务端未带 clientMsgId 时，结束当前发送态，避免前端误报超时。
  imHelper.rejectPendingRequest(page, new Error(errorMessage), '')
  return true
}

function handleWsAck(page, payload, options = {}) {
  const clientMsgId = payload && payload.clientMsgId ? String(payload.clientMsgId) : ''
  const resolved = imHelper.resolvePendingRequest(page, clientMsgId)
  const consumeWhenResolved = options.consumeWhenResolved !== false
  const canConsume = consumeWhenResolved ? resolved : true

  if (canConsume && payload && payload.data && typeof options.onMessage === 'function') {
    options.onMessage(payload.data)
  }

  if (typeof options.onAfterAck === 'function') {
    options.onAfterAck({ resolved, payload })
  }
  return resolved
}

function handleCommonPayload(page, payload, options = {}) {
  const payloadType = getPayloadType(payload)
  if (!payloadType) {
    return {
      handled: true,
      payloadType
    }
  }

  if (payloadType === 'ERROR') {
    handleWsError(page, payload, options.toastError)
    return {
      handled: true,
      payloadType
    }
  }

  if (payloadType === 'ACK') {
    handleWsAck(page, payload, {
      consumeWhenResolved: options.consumeWhenResolved,
      onMessage: options.onAckMessage,
      onAfterAck: options.onAfterAck
    })
    return {
      handled: true,
      payloadType
    }
  }

  return {
    handled: false,
    payloadType
  }
}

module.exports = {
  getPayloadType,
  handleWsError,
  handleWsAck,
  handleCommonPayload
}
