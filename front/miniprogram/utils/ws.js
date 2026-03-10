/**
 * WebSocket 连接管理
 */
const config = require('./config')
const auth = require('./auth')
const AUTH_ERROR_CODES = [2001, 2002, 2003]
const READY_WAIT_TIMEOUT_MS = 6000

let socketOpen = false
let connecting = false
let authenticated = false
let inited = false
let reconnectTimer = null
let heartbeatTimer = null
const messageQueue = []
const listeners = new Set()
const readyWaiters = []
const RECONNECT_DELAY = 3000
const HEARTBEAT_INTERVAL = 30000

function getWsUrl() {
  // 统一把 http/https 转为 ws/wss
  let base = config.baseURL || ''
  if (base.endsWith('/')) {
    base = base.slice(0, -1)
  }
  return base.replace(/^http/, 'ws') + '/ws/chat'
}

function initEvents() {
  if (inited) return
  inited = true

  wx.onSocketOpen(() => {
    socketOpen = true
    connecting = false
    authenticated = false
    sendAuth()
    startHeartbeat()
    flushQueue()
  })

  wx.onSocketMessage((res) => {
    let payload = null
    try {
      payload = JSON.parse(res.data)
    } catch (e) {
      console.error('WS 消息解析失败:', e)
      return
    }

    if (payload && payload.type === 'AUTH_OK') {
      authenticated = true
      flushQueue()
      resolveReadyWaiters()
    }

    if (payload && payload.type === 'ERROR' && AUTH_ERROR_CODES.includes(payload.code)) {
      authenticated = false
      rejectReadyWaiters('登录状态失效，请重新登录')
      auth.handleAuthExpired()
      return
    }
    listeners.forEach((handler) => handler(payload))
  })

  wx.onSocketClose(() => {
    markSocketBroken()
    scheduleReconnect()
  })

  wx.onSocketError((err) => {
    markSocketBroken()
    console.error('WS 连接错误:', err)
    scheduleReconnect()
  })
}

function markSocketBroken() {
  socketOpen = false
  connecting = false
  authenticated = false
  stopHeartbeat()
}

function connect() {
  if (socketOpen || connecting) return
  if (!auth.getToken()) return
  initEvents()
  connecting = true
  wx.connectSocket({
    url: getWsUrl()
  })
}

function sendAuth() {
  const token = auth.getToken()
  if (!token) return
  send({
    type: 'AUTH',
    token: `Bearer ${token}`
  })
}

function requiresAuth(data) {
  if (!data || !data.type) {
    return true
  }
  const type = String(data.type).toUpperCase()
  return type !== 'AUTH' && type !== 'PING'
}

function canSendNow(requireAuth) {
  if (!socketOpen) {
    return false
  }
  if (!requireAuth) {
    return true
  }
  return authenticated
}

function flushQueue() {
  while (messageQueue.length > 0) {
    const item = messageQueue[0]
    if (!canSendNow(item.requireAuth)) {
      break
    }
    messageQueue.shift()
    wx.sendSocketMessage({
      data: item.payload,
      fail: () => {
        // 发送失败回退到队列头，等待重连后补发
        messageQueue.unshift(item)
        markSocketBroken()
        scheduleReconnect()
      }
    })
  }
}

function send(data) {
  const payload = JSON.stringify(data || {})
  const item = {
    payload,
    requireAuth: requiresAuth(data)
  }
  if (canSendNow(item.requireAuth)) {
    wx.sendSocketMessage({
      data: payload,
      fail: () => {
        messageQueue.unshift(item)
        markSocketBroken()
        scheduleReconnect()
      }
    })
  } else {
    messageQueue.push(item)
    connect()
  }
}

function removeReadyWaiter(waiter) {
  const idx = readyWaiters.indexOf(waiter)
  if (idx >= 0) {
    readyWaiters.splice(idx, 1)
  }
}

function resolveReadyWaiters() {
  if (readyWaiters.length === 0) {
    return
  }
  const pending = readyWaiters.slice()
  readyWaiters.length = 0
  pending.forEach(waiter => waiter.resolve())
}

function rejectReadyWaiters(message) {
  if (readyWaiters.length === 0) {
    return
  }
  const pending = readyWaiters.slice()
  readyWaiters.length = 0
  pending.forEach(waiter => waiter.reject(new Error(message || '连接不可用')))
}

function waitUntilReady(timeoutMs = READY_WAIT_TIMEOUT_MS) {
  if (socketOpen && authenticated) {
    return Promise.resolve()
  }

  connect()

  return new Promise((resolve, reject) => {
    const waiter = {
      resolve: () => {
        clearTimeout(timer)
        resolve()
      },
      reject: (error) => {
        clearTimeout(timer)
        reject(error)
      }
    }
    const timer = setTimeout(() => {
      removeReadyWaiter(waiter)
      reject(new Error('连接未就绪，请稍后重试'))
    }, Math.max(500, Number(timeoutMs) || READY_WAIT_TIMEOUT_MS))

    readyWaiters.push(waiter)
  })
}

function onMessage(handler) {
  listeners.add(handler)
}

function offMessage(handler) {
  listeners.delete(handler)
  if (listeners.size === 0) {
    rejectReadyWaiters('连接已关闭')
    stopHeartbeat()
    if (socketOpen || connecting) {
      wx.closeSocket()
      markSocketBroken()
    }
  }
  if (listeners.size === 0 && reconnectTimer) {
    clearTimeout(reconnectTimer)
    reconnectTimer = null
  }
}

function scheduleReconnect() {
  if (reconnectTimer || socketOpen || connecting || listeners.size === 0) return
  reconnectTimer = setTimeout(() => {
    reconnectTimer = null
    connect()
  }, RECONNECT_DELAY)
}

function startHeartbeat() {
  if (heartbeatTimer) return
  heartbeatTimer = setInterval(() => {
    if (!socketOpen) return
    send({ type: 'PING' })
  }, HEARTBEAT_INTERVAL)
}

function stopHeartbeat() {
  if (!heartbeatTimer) return
  clearInterval(heartbeatTimer)
  heartbeatTimer = null
}

module.exports = {
  connect,
  send,
  waitUntilReady,
  onMessage,
  offMessage
}
