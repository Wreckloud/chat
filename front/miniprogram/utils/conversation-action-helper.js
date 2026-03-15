const SWIPE_ACTION_TYPES = {
  PIN: 'PIN',
  TOGGLE_READ: 'TOGGLE_READ',
  MORE: 'MORE'
}

function resolveSwipeActionType(detail) {
  const actionType = detail && detail.actionType ? String(detail.actionType).toUpperCase() : ''
  if (actionType === SWIPE_ACTION_TYPES.PIN
    || actionType === SWIPE_ACTION_TYPES.TOGGLE_READ
    || actionType === SWIPE_ACTION_TYPES.MORE) {
    return actionType
  }
  return ''
}

function findConversationById(list, conversationId) {
  return (list || []).find(
    item => Number(item.conversationId) === Number(conversationId)
  ) || null
}

function mapConversationById(list, conversationId, mapFn) {
  const numericConversationId = Number(conversationId)
  return (list || []).map(item => {
    if (Number(item.conversationId) !== numericConversationId) {
      return item
    }
    return mapFn(item)
  })
}

function buildReadTogglePlan(conversationId, unreadCount) {
  const currentUnread = Number(unreadCount) || 0
  const markRead = currentUnread > 0
  return {
    url: markRead
      ? `/conversations/${conversationId}/read`
      : `/conversations/${conversationId}/unread`,
    markRead,
    nextUnread: markRead ? 0 : 1
  }
}

function togglePinnedConversationIds(sourcePinnedIds, conversationId) {
  const pinnedSet = new Set(sourcePinnedIds || [])
  const numericConversationId = Number(conversationId)
  const nextPinned = !pinnedSet.has(numericConversationId)
  if (nextPinned) {
    pinnedSet.add(numericConversationId)
  } else {
    pinnedSet.delete(numericConversationId)
  }
  return {
    nextPinned,
    pinnedIds: Array.from(pinnedSet)
  }
}

function buildPinnedStorageKey(userId) {
  return `wolfchat_chat_pins_${Number(userId) || 0}`
}

function normalizePinnedConversationIds(savedValue) {
  if (!Array.isArray(savedValue)) {
    return []
  }
  return savedValue
    .map(id => Number(id))
    .filter(id => Number.isFinite(id))
}

module.exports = {
  SWIPE_ACTION_TYPES,
  resolveSwipeActionType,
  findConversationById,
  mapConversationById,
  buildReadTogglePlan,
  togglePinnedConversationIds,
  buildPinnedStorageKey,
  normalizePinnedConversationIds
}

