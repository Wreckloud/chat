const AUTH_ERROR_CODES = new Set([2001, 2002, 2003])
const DEFAULT_ACK_TIMEOUT_MS = 10000
const READY_TIMEOUT_MS = 12000
const HEARTBEAT_INTERVAL_MS = 30000

const WS_STATE = {
  DISCONNECTED: 'DISCONNECTED',
  CONNECTING: 'CONNECTING',
  AUTHING: 'AUTHING',
  READY: 'READY',
  RECONNECTING: 'RECONNECTING'
}

let socket = null
let state = WS_STATE.DISCONNECTED
let socketOpen = false
let authed = false
let reconnectTimer = null
let heartbeatTimer = null
let sendQueue = []
const listeners = new Set()
const stateListeners = new Set()
const readyWaiters = []
const pendingAckMap = new Map()

let tokenProvider = () => ''
let authExpiredHandler = () => {}

function setState(nextState) {
  if (state === nextState) {
    return
  }
  state = nextState
  stateListeners.forEach((handler) => {
    try {
      handler(state)
    } catch (error) {
      // 状态监听异常不影响主流程
    }
  })
}

function emitPayload(payload) {
  listeners.forEach((handler) => {
    try {
      handler(payload)
    } catch (error) {
      // 监听异常不影响主流程
    }
  })
}

function resolveReadyWaiters() {
  if (readyWaiters.length === 0) {
    return
  }
  const pending = readyWaiters.splice(0, readyWaiters.length)
  pending.forEach((item) => item.resolve())
}

function rejectReadyWaiters(error) {
  if (readyWaiters.length === 0) {
    return
  }
  const pending = readyWaiters.splice(0, readyWaiters.length)
  pending.forEach((item) => item.reject(error))
}

function buildWsUrl() {
  const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  const pathname = window.location.pathname || '/'
  const segments = pathname.split('/').filter(Boolean)
  const contextPath = segments.length > 0 ? `/${segments[0]}` : ''
  return `${wsProtocol}//${window.location.host}${contextPath}/ws/chat`
}

function clearHeartbeat() {
  if (!heartbeatTimer) {
    return
  }
  clearInterval(heartbeatTimer)
  heartbeatTimer = null
}

function startHeartbeat() {
  clearHeartbeat()
  heartbeatTimer = setInterval(() => {
    if (!socketOpen) {
      return
    }
    sendRaw({ type: 'PING' }, false)
  }, HEARTBEAT_INTERVAL_MS)
}

function clearReconnectTimer() {
  if (!reconnectTimer) {
    return
  }
  clearTimeout(reconnectTimer)
  reconnectTimer = null
}

function scheduleReconnect() {
  if (reconnectTimer || listeners.size === 0) {
    return
  }
  setState(WS_STATE.RECONNECTING)
  reconnectTimer = setTimeout(() => {
    reconnectTimer = null
    connect()
  }, 2500)
}

function resetSocketState() {
  socketOpen = false
  authed = false
  clearHeartbeat()
  setState(WS_STATE.DISCONNECTED)
}

function cleanupPendingAcks(error) {
  if (pendingAckMap.size === 0) {
    return
  }
  pendingAckMap.forEach((item) => {
    clearTimeout(item.timer)
    item.reject(error)
  })
  pendingAckMap.clear()
}

function handleAuthExpired(payload) {
  const code = Number(payload && payload.code)
  if (!AUTH_ERROR_CODES.has(code)) {
    return false
  }
  const error = new Error(payload?.message || '登录状态失效')
  rejectReadyWaiters(error)
  cleanupPendingAcks(error)
  authExpiredHandler()
  disconnect()
  return true
}

function handleIncoming(raw) {
  let payload = null
  try {
    payload = JSON.parse(raw)
  } catch (error) {
    return
  }
  const type = String(payload?.type || '').toUpperCase()
  if (!type) {
    return
  }

  if (type === 'AUTH_OK') {
    authed = true
    setState(WS_STATE.READY)
    resolveReadyWaiters()
    flushQueue()
    emitPayload(payload)
    return
  }

  if (type === 'ERROR') {
    const handled = handleAuthExpired(payload)
    if (handled) {
      return
    }
    const clientMsgId = String(payload?.clientMsgId || '')
    if (clientMsgId && pendingAckMap.has(clientMsgId)) {
      const pending = pendingAckMap.get(clientMsgId)
      clearTimeout(pending.timer)
      pendingAckMap.delete(clientMsgId)
      const error = new Error(payload?.message || '发送失败')
      error.code = Number(payload?.code || 0)
      pending.reject(error)
    }
    emitPayload(payload)
    return
  }

  if (type === 'ACK') {
    const clientMsgId = String(payload?.clientMsgId || '')
    if (clientMsgId && pendingAckMap.has(clientMsgId)) {
      const pending = pendingAckMap.get(clientMsgId)
      clearTimeout(pending.timer)
      pendingAckMap.delete(clientMsgId)
      pending.resolve(payload?.data)
    }
    emitPayload(payload)
    return
  }

  emitPayload(payload)
}

function sendSocketText(text) {
  if (!socket || socket.readyState !== WebSocket.OPEN) {
    throw new Error('连接不可用')
  }
  socket.send(text)
}

function flushQueue() {
  if (!socketOpen || !authed || sendQueue.length === 0) {
    return
  }
  const pending = sendQueue
  sendQueue = []
  pending.forEach((item) => {
    try {
      sendSocketText(item.text)
    } catch (error) {
      sendQueue.unshift(item)
      resetSocketState()
      scheduleReconnect()
    }
  })
}

function sendRaw(payload, requireAuth = true) {
  const text = JSON.stringify(payload || {})
  if (!socketOpen || (requireAuth && !authed)) {
    sendQueue.push({ text, requireAuth })
    connect()
    return
  }
  sendSocketText(text)
}

function connect() {
  const token = String(tokenProvider() || '').trim()
  if (!token || socketOpen || (socket && socket.readyState === WebSocket.CONNECTING)) {
    return
  }
  clearReconnectTimer()
  setState(WS_STATE.CONNECTING)
  socket = new WebSocket(buildWsUrl())

  socket.onopen = () => {
    socketOpen = true
    authed = false
    setState(WS_STATE.AUTHING)
    startHeartbeat()
    sendRaw({ type: 'AUTH', token: `Bearer ${token}` }, false)
  }

  socket.onmessage = (event) => {
    handleIncoming(event.data)
  }

  socket.onclose = () => {
    resetSocketState()
    scheduleReconnect()
  }

  socket.onerror = () => {
    resetSocketState()
    scheduleReconnect()
  }
}

function waitUntilReady(timeoutMs = READY_TIMEOUT_MS) {
  if (socketOpen && authed) {
    return Promise.resolve()
  }
  connect()
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => {
      reject(new Error('连接未就绪，请稍后重试'))
    }, Math.max(1200, Number(timeoutMs) || READY_TIMEOUT_MS))
    readyWaiters.push({
      resolve: () => {
        clearTimeout(timer)
        resolve()
      },
      reject: (error) => {
        clearTimeout(timer)
        reject(error)
      }
    })
  })
}

function createClientMsgId(prefix = 'w') {
  return `${prefix}_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`
}

async function sendWithAck(payload, options = {}) {
  await waitUntilReady()
  const nextPayload = { ...(payload || {}) }
  const clientMsgId = String(options.clientMsgId || nextPayload.clientMsgId || createClientMsgId('c')).trim()
  nextPayload.clientMsgId = clientMsgId
  const timeoutMs = Math.max(1500, Number(options.timeoutMs) || DEFAULT_ACK_TIMEOUT_MS)

  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => {
      pendingAckMap.delete(clientMsgId)
      reject(new Error('消息发送超时，请重试'))
    }, timeoutMs)
    pendingAckMap.set(clientMsgId, { resolve, reject, timer })
    try {
      sendRaw(nextPayload, true)
    } catch (error) {
      clearTimeout(timer)
      pendingAckMap.delete(clientMsgId)
      reject(error)
    }
  })
}

function subscribe(handler) {
  if (typeof handler !== 'function') {
    return () => {}
  }
  listeners.add(handler)
  connect()
  return () => listeners.delete(handler)
}

function onStateChange(handler) {
  if (typeof handler !== 'function') {
    return () => {}
  }
  stateListeners.add(handler)
  handler(state)
  return () => stateListeners.delete(handler)
}

function configure(options = {}) {
  if (typeof options.tokenProvider === 'function') {
    tokenProvider = options.tokenProvider
  }
  if (typeof options.onAuthExpired === 'function') {
    authExpiredHandler = options.onAuthExpired
  }
}

function disconnect() {
  clearReconnectTimer()
  clearHeartbeat()
  rejectReadyWaiters(new Error('连接已关闭'))
  cleanupPendingAcks(new Error('连接已断开'))
  sendQueue = []
  if (socket) {
    try {
      socket.close()
    } catch (error) {
      // 忽略关闭异常
    }
  }
  socket = null
  resetSocketState()
}

function getState() {
  return state
}

export {
  WS_STATE,
  configure,
  connect,
  disconnect,
  subscribe,
  onStateChange,
  waitUntilReady,
  sendRaw,
  sendWithAck,
  getState
}

