const { attachDisplayTitle } = require('./title')

function mapThread(rawThread, normalizeUser, time) {
  const viewCount = Number(rawThread.viewCount) || 0
  const replyCount = Number(rawThread.replyCount) || 0
  const likeCount = Number(rawThread.likeCount) || 0
  const imageUrls = Array.isArray(rawThread.imageUrls)
    ? rawThread.imageUrls.filter(item => typeof item === 'string' && item.trim())
    : []
  const videoUrl = typeof rawThread.videoUrl === 'string' ? rawThread.videoUrl : ''
  const videoPosterUrl = typeof rawThread.videoPosterUrl === 'string' ? rawThread.videoPosterUrl : ''
  return {
    ...rawThread,
    viewCount,
    replyCount,
    likeCount,
    imageUrls,
    videoUrl,
    videoPosterUrl,
    likedByCurrentUser: rawThread.likedByCurrentUser === true,
    author: attachDisplayTitle(
      normalizeUser(rawThread.author) || {},
      rawThread.author && rawThread.author.equippedTitleName,
      rawThread.author && rawThread.author.equippedTitleColor
    ),
    lastReplyUser: attachDisplayTitle(
      normalizeUser(rawThread.lastReplyUser) || {},
      rawThread.lastReplyUser && rawThread.lastReplyUser.equippedTitleName,
      rawThread.lastReplyUser && rawThread.lastReplyUser.equippedTitleColor
    ),
    createTimeText: time.formatPostTime(rawThread.createTime),
    lastReplyTimeText: time.formatPostTime(rawThread.lastReplyTime)
  }
}

function mapThreadList(rawList, normalizeUser, time) {
  if (!Array.isArray(rawList) || rawList.length === 0) {
    return []
  }
  return rawList.map(item => mapThread(item, normalizeUser, time))
}

function mergePagedList(previousList, incomingList, reset) {
  if (reset) {
    return incomingList
  }
  return [...previousList, ...incomingList]
}

function resolveHasMoreByTotal(currentSize, total) {
  return currentSize < (Number(total) || 0)
}

function mapReply(rawReply, normalizeUser, time, options = {}) {
  const author = attachDisplayTitle(
    normalizeUser(rawReply.author) || {},
    rawReply.author && rawReply.author.equippedTitleName,
    rawReply.author && rawReply.author.equippedTitleColor
  )
  const currentUserId = Number(options.currentUserId) || 0
  const canManageThread = options.canManageThread === true
  const likeCount = Number(rawReply.likeCount) || 0
  return {
    ...rawReply,
    likeCount,
    imageUrl: typeof rawReply.imageUrl === 'string' ? rawReply.imageUrl : '',
    likedByCurrentUser: rawReply.likedByCurrentUser === true,
    author,
    quoteAuthor: attachDisplayTitle(
      normalizeUser(rawReply.quoteAuthor) || {},
      rawReply.quoteAuthor && rawReply.quoteAuthor.equippedTitleName,
      rawReply.quoteAuthor && rawReply.quoteAuthor.equippedTitleColor
    ),
    timeText: time.formatPostTime(rawReply.createTime),
    canDelete: currentUserId > 0 && (currentUserId === Number(author.userId) || canManageThread)
  }
}

module.exports = {
  mapThread,
  mapThreadList,
  mergePagedList,
  resolveHasMoreByTotal,
  mapReply
}
