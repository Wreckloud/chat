/**
 * WebSocket 连接管理
 */
const config = require('./config')
const auth = require('./auth')

let socketOpen = false
let connecting = false
let inited = false
const messageQueue = []
const listeners = new Set()

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
    console.info('[WS] 连接已建立')
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
    listeners.forEach((handler) => handler(payload))
  })

  wx.onSocketClose(() => {
    socketOpen = false
    connecting = false
    messageQueue.length = 0
    console.warn('[WS] 连接已关闭')
  })

  wx.onSocketError((err) => {
    socketOpen = false
    connecting = false
    console.error('WS 连接错误:', err)
  })
}

function connect() {
  if (socketOpen || connecting) return
  initEvents()
  connecting = true
  console.debug('[WS] 尝试连接')
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
    wx.sendSocketMessage({ data })
  }
}

function send(data) {
  const payload = JSON.stringify(data)
  if (socketOpen) {
    wx.sendSocketMessage({ data: payload })
  } else {
    messageQueue.push(payload)
    connect()
  }
}

function close() {
  if (socketOpen || connecting) {
    wx.closeSocket()
  }
  socketOpen = false
  connecting = false
  messageQueue.length = 0
}

function onMessage(handler) {
  listeners.add(handler)
}

function offMessage(handler) {
  listeners.delete(handler)
}

module.exports = {
  connect,
  send,
  close,
  onMessage,
  offMessage
}
