import { formatDateTime, formatRelative } from '@/utils/time'

function normalizeImageUrls(value) {
  if (!Array.isArray(value)) {
    return []
  }
  return value
    .map((item) => String(item || '').trim())
    .filter(Boolean)
}

function attachDisplayTitle(user) {
  if (!user || typeof user !== 'object') {
    return {}
  }
  return {
    ...user,
    displayName: String(user.nickname || user.wolfNo || `行者${user.userId || ''}`).trim(),
    displayTitleName: String(user.equippedTitleName || '').trim(),
    displayTitleColor: String(user.equippedTitleColor || '').trim()
  }
}

export function mapThread(rawThread) {
  const imageUrls = normalizeImageUrls(rawThread?.imageUrls)
  const videoPosterUrl = String(rawThread?.videoPosterUrl || '').trim()
  const videoUrl = String(rawThread?.videoUrl || '').trim()
  const hasVideo = Boolean(videoUrl)
  const hasSingleImagePreview = !hasVideo && imageUrls.length === 1
  const singleImagePreviewUrl = hasSingleImagePreview ? imageUrls[0] : ''
  const previewImageUrls = hasSingleImagePreview ? imageUrls : imageUrls.slice(0, hasVideo ? 2 : 3)
  const hasEditTime = Boolean(rawThread?.editTime)
  return {
    ...rawThread,
    title: String(rawThread?.title || '').trim(),
    contentPreview: String(rawThread?.contentPreview || '').trim(),
    imageUrls,
    previewImageUrls,
    hasMoreImages: imageUrls.length > previewImageUrls.length,
    hasSingleImagePreview,
    singleImagePreviewUrl,
    hasVideo,
    videoPosterUrl,
    author: attachDisplayTitle(rawThread?.author || {}),
    timePrefix: hasEditTime ? '编辑于' : '发布于',
    timeRelativeText: formatRelative(hasEditTime ? rawThread?.editTime : rawThread?.createTime),
    timeText: formatDateTime(hasEditTime ? rawThread?.editTime : rawThread?.createTime),
    hasEditTime
  }
}

export function mapThreadList(rawList) {
  if (!Array.isArray(rawList)) {
    return []
  }
  return rawList.map((item) => mapThread(item))
}

export function mergePageList(previousList, nextList, reset = false) {
  if (reset) {
    return nextList
  }
  return (Array.isArray(previousList) ? previousList : []).concat(Array.isArray(nextList) ? nextList : [])
}

export function resolveHasMore(total, currentSize) {
  const safeTotal = Number(total || 0)
  const safeCurrentSize = Number(currentSize || 0)
  return safeCurrentSize < safeTotal
}

export function mapReply(rawReply, options = {}) {
  const currentUserId = Number(options.currentUserId || 0)
  const canManageThread = options.canManageThread === true
  const author = attachDisplayTitle(rawReply?.author || {})
  return {
    ...rawReply,
    author,
    quoteAuthor: attachDisplayTitle(rawReply?.quoteAuthor || {}),
    likeCount: Number(rawReply?.likeCount || 0),
    likedByCurrentUser: rawReply?.likedByCurrentUser === true,
    floorNo: Number(rawReply?.floorNo || 0),
    canDelete: currentUserId > 0 && (
      Number(author.userId || 0) === currentUserId || canManageThread
    )
  }
}
