function resolveActiveBoardId(boards, currentBoardId) {
  if (!Array.isArray(boards) || boards.length === 0) {
    return null
  }
  if (!currentBoardId) {
    return boards[0].boardId
  }
  const exists = boards.some(item => Number(item.boardId) === Number(currentBoardId))
  return exists ? currentBoardId : boards[0].boardId
}

function mapThread(rawThread, normalizeUser, time) {
  return {
    ...rawThread,
    author: normalizeUser(rawThread.author) || {},
    lastReplyUser: normalizeUser(rawThread.lastReplyUser) || {},
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
  const author = normalizeUser(rawReply.author) || {}
  const currentUserId = Number(options.currentUserId) || 0
  const canManageThread = options.canManageThread === true
  return {
    ...rawReply,
    author,
    quoteAuthor: normalizeUser(rawReply.quoteAuthor) || {},
    timeText: time.formatPostTime(rawReply.createTime),
    canDelete: currentUserId > 0 && (currentUserId === Number(author.userId) || canManageThread)
  }
}

module.exports = {
  resolveActiveBoardId,
  mapThread,
  mapThreadList,
  mergePagedList,
  resolveHasMoreByTotal,
  mapReply
}
