function buildConversationPreview(message) {
  if (!message) {
    return ''
  }
  if (message.msgType === 'IMAGE') {
    return '[图片]'
  }
  if (message.msgType === 'VIDEO') {
    return '[视频]'
  }
  if (message.msgType === 'FILE') {
    return '[文件]'
  }
  return message.content || ''
}

function resolveOnlineStatus(item) {
  if (item.isOnline === true || item.online === true) {
    return true
  }
  if (typeof item.onlineStatus === 'string' && item.onlineStatus.toUpperCase() === 'ONLINE') {
    return true
  }
  return false
}

function buildPresenceText(isOnline, lastSeenAt, time) {
  if (isOnline) {
    return '在线'
  }
  if (lastSeenAt) {
    return time.formatLastSeenText(lastSeenAt)
  }
  return '上次在线 未知'
}

function sortConversationList(sourceList) {
  return (sourceList || []).slice().sort((a, b) => {
    const pinDiff = Number(Boolean(b.pinned)) - Number(Boolean(a.pinned))
    if (pinDiff !== 0) return pinDiff

    const aTime = a.lastMessageTime || ''
    const bTime = b.lastMessageTime || ''
    if (aTime !== bTime) return bTime.localeCompare(aTime)
    return Number(b.conversationId) - Number(a.conversationId)
  })
}

function buildConversationSections(conversationList) {
  const onlineConversations = conversationList.filter(item => item.isOnline)
  const offlineConversations = conversationList.filter(item => !item.isOnline)
  return [
    {
      key: 'online',
      title: `在线好友 (${onlineConversations.length})`,
      rowClass: '',
      showEmpty: true,
      emptyText: '暂无在线好友',
      list: onlineConversations
    },
    {
      key: 'offline',
      title: `离线 (${offlineConversations.length})`,
      rowClass: 'session-row-offline',
      showEmpty: false,
      emptyText: '',
      list: offlineConversations
    }
  ]
}

module.exports = {
  buildConversationPreview,
  resolveOnlineStatus,
  buildPresenceText,
  sortConversationList,
  buildConversationSections
}

