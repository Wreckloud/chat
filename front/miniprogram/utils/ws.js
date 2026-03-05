/**
 * WebSocket 连接管理
 */
const config = require('./config')
const auth = require('./auth')
const AUTH_ERROR_CODES = [2001, 2002, 2003]

let socketOpen = false
let connecting = false
let inited = false
let reconnectTimer = null
const messageQueue = []
const listeners = new Set()
const RECONNECT_DELAY = 3000

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
    sendAuth()
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

    if (payload && payload.type === 'ERROR' && AUTH_ERROR_CODES.includes(payload.code)) {
      auth.handleAuthExpired()
      return
    }
    listeners.forEach((handler) => handler(payload))
  })

  wx.onSocketClose(() => {
    socketOpen = false
    connecting = false
    scheduleReconnect()
  })

  wx.onSocketError((err) => {
    socketOpen = false
    connecting = false
    console.error('WS 连接错误:', err)
    scheduleReconnect()
  })
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

function flushQueue() {
  while (socketOpen && messageQueue.length > 0) {
    const data = messageQueue.shift()
    wx.sendSocketMessage({
      data,
      fail: () => {
        // 发送失败回退到队列头，等待重连后补发
        messageQueue.unshift(data)
        socketOpen = false
        connecting = false
        scheduleReconnect()
      }
    })
  }
}

function send(data) {
  const payload = JSON.stringify(data)
  if (socketOpen) {
    wx.sendSocketMessage({
      data: payload,
      fail: () => {
        messageQueue.unshift(payload)
        socketOpen = false
        connecting = false
        scheduleReconnect()
      }
    })
  } else {
    messageQueue.push(payload)
    connect()
  }
}

function onMessage(handler) {
  listeners.add(handler)
}

function offMessage(handler) {
  listeners.delete(handler)
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

module.exports = {
  connect,
  send,
  onMessage,
  offMessage
}
