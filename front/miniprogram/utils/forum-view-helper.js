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
  const hasVideo = !!videoUrl
  const contentPreview = typeof rawThread.contentPreview === 'string' ? rawThread.contentPreview.trim() : ''
  const hasSingleImagePreview = !hasVideo && imageUrls.length === 1
  const singleImagePreviewUrl = hasSingleImagePreview ? imageUrls[0] : ''
  const previewImageUrls = imageUrls.slice(0, hasVideo ? 2 : 3)
  const editTimeText = time.formatPostTime(rawThread.editTime)
  const editTimeRelativeText = time.formatRelativeTime(rawThread.editTime)
  const hasEditTime = !!editTimeText
  const timePrefix = hasEditTime ? '编辑于' : '发布于'
  const timeText = hasEditTime ? editTimeText : time.formatPostTime(rawThread.createTime)
  const timeRelativeText = hasEditTime ? editTimeRelativeText : time.formatRelativeTime(rawThread.createTime)
  return {
    ...rawThread,
    viewCount,
    replyCount,
    likeCount,
    imageUrls,
    hasSingleImagePreview,
    singleImagePreviewUrl,
    previewImageUrls,
    hasMoreImages: imageUrls.length > previewImageUrls.length,
    hasVideo,
    videoUrl,
    videoPosterUrl,
    contentPreview,
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
    createTimeRelativeText: time.formatRelativeTime(rawThread.createTime),
    createTimeText: time.formatPostTime(rawThread.createTime),
    editTimeRelativeText,
    editTimeText,
    hasEditTime,
    timePrefix,
    timeText,
    timeRelativeText,
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
  const rawReplyId = Number(rawReply && rawReply.replyId)
  const replyId = Number.isFinite(rawReplyId) && rawReplyId > 0 ? rawReplyId : 0
  const rawQuoteReplyId = Number(rawReply && rawReply.quoteReplyId)
  const quoteReplyId = Number.isFinite(rawQuoteReplyId) && rawQuoteReplyId > 0 ? rawQuoteReplyId : 0
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
    replyId,
    likeCount,
    imageUrl: typeof rawReply.imageUrl === 'string' ? rawReply.imageUrl : '',
    likedByCurrentUser: rawReply.likedByCurrentUser === true,
    quoteReplyId,
    author,
    timeText: time.formatPostTime(rawReply.createTime),
    relativeTimeText: time.formatRelativeTime(rawReply.createTime),
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
