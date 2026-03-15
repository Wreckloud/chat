const imHelper = require('./im-helper')

function getPayloadType(payload) {
  if (!payload || !payload.type) {
    return ''
  }
  return String(payload.type).toUpperCase()
}

function handleWsError(page, payload, toastError) {
  const errorMessage = payload && payload.message ? payload.message : '发送失败'
  const clientMsgId = payload && payload.clientMsgId ? String(payload.clientMsgId) : ''

  // 仅 clientMsgId 对应的 ERROR 参与发送链路兜底。
  if (clientMsgId) {
    const resolved = imHelper.rejectPendingRequest(page, new Error(errorMessage), clientMsgId)
    if (!resolved && !page.data.sending) {
      toastError(errorMessage, '请求异常')
    }
    return true
  }

  if (!page.data.sending) {
    toastError(errorMessage, '请求异常')
  }
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

module.exports = {
  getPayloadType,
  handleWsError,
  handleWsAck
}

