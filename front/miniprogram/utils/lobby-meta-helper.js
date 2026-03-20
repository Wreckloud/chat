const { formatNameWithTitle, normalizeTitleName } = require('./title')

function buildLobbyActiveText(latestMessageAt, time) {
  if (!latestMessageAt) {
    return '最近活跃 --'
  }
  return `最近活跃 ${time.formatConversationTime(latestMessageAt)}`
}

function buildRecentUsersText(recentUsers, normalizeUser, time) {
  if (!Array.isArray(recentUsers) || recentUsers.length === 0) {
    return '最近在线 --'
  }

  const text = recentUsers
    .slice(0, 6)
    .map(item => {
      const user = normalizeUser(item) || {}
      const name = formatNameWithTitle(
        user,
        normalizeTitleName(item.equippedTitleName)
      )
      if (item.online === true) {
        return `${name}(在线)`
      }
      if (item.lastActiveAt) {
        return `${name}(${time.formatConversationTime(item.lastActiveAt)})`
      }
      return `${name}(离线)`
    })
    .join(' · ')
  return text || '最近在线 --'
}

function buildLatestMessageText(latestMessageSenderName, latestMessagePreview) {
  const senderName = String(latestMessageSenderName || '').trim()
  const text = String(latestMessagePreview || '').trim()
  if (!text) {
    return '暂无消息'
  }
  if (!senderName) {
    return text
  }
  return `${senderName}: ${text}`
}

function loadLobbyMeta(page, request, onMeta) {
  return request.get('/lobby/meta')
    .then(res => {
      const meta = res && res.data ? res.data : {}
      if (typeof onMeta === 'function') {
        onMeta(meta)
      }
      return meta
    })
    .catch(() => {})
}

function scheduleLobbyMetaRefresh(page, refreshFn, delayMs = 600) {
  if (page.lobbyMetaTimer) {
    return
  }
  page.lobbyMetaTimer = setTimeout(() => {
    page.lobbyMetaTimer = null
    if (typeof refreshFn === 'function') {
      refreshFn()
    }
  }, delayMs)
}

function clearLobbyMetaTimer(page) {
  if (!page.lobbyMetaTimer) {
    return
  }
  clearTimeout(page.lobbyMetaTimer)
  page.lobbyMetaTimer = null
}

function startLobbyMetaPolling(page, refreshFn, intervalMs = 10000) {
  stopLobbyMetaPolling(page)
  if (typeof refreshFn !== 'function' || intervalMs <= 0) {
    return
  }
  page.lobbyMetaPollTimer = setInterval(() => {
    refreshFn()
  }, intervalMs)
}

function stopLobbyMetaPolling(page) {
  if (!page.lobbyMetaPollTimer) {
    return
  }
  clearInterval(page.lobbyMetaPollTimer)
  page.lobbyMetaPollTimer = null
}

module.exports = {
  buildLobbyActiveText,
  buildRecentUsersText,
  buildLatestMessageText,
  loadLobbyMeta,
  scheduleLobbyMetaRefresh,
  clearLobbyMetaTimer,
  startLobbyMetaPolling,
  stopLobbyMetaPolling
}
