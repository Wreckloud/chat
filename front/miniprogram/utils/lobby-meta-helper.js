function buildLobbyActiveText(latestActiveAt, time) {
  if (!latestActiveAt) {
    return '最近活跃 --'
  }
  return `最近活跃 ${time.formatConversationTime(latestActiveAt)}`
}

function buildRecentUsersText(recentUsers, normalizeUser, time) {
  if (!Array.isArray(recentUsers) || recentUsers.length === 0) {
    return '最近在线 --'
  }

  const text = recentUsers
    .slice(0, 6)
    .map(item => {
      const user = normalizeUser(item) || {}
      const name = user.nickname || user.wolfNo || '行者'
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

module.exports = {
  buildLobbyActiveText,
  buildRecentUsersText,
  loadLobbyMeta,
  scheduleLobbyMetaRefresh,
  clearLobbyMetaTimer
}

