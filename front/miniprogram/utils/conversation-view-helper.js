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
  if (message.msgType === 'RECALL') {
    return '[消息已撤回]'
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

module.exports = {
  buildConversationPreview,
  resolveOnlineStatus,
  buildPresenceText
}
