const request = require('./request')
const auth = require('./auth')

const CHAT_TAB_INDEX = 0
const ME_TAB_INDEX = 2
const MAX_BADGE = 99
const TAB_BASE_TEXT = {
  0: '聊天',
  1: '社区',
  2: '我的'
}
const lastTabTextMap = {}
let chatBadgeLoadingPromise = null
let noticeBadgeLoadingPromise = null

function normalizeCount(value) {
  const count = Number(value)
  if (!Number.isFinite(count) || count <= 0) {
    return 0
  }
  return Math.floor(count)
}

function toBadgeText(count) {
  if (count > MAX_BADGE) {
    return `${MAX_BADGE}+`
  }
  return String(count)
}

function buildTabText(index, count) {
  const normalized = normalizeCount(count)
  const baseText = TAB_BASE_TEXT[index] || ''
  const suffix = normalized > 0 ? `[${toBadgeText(normalized)}]` : ''
  return `${baseText}${suffix}`
}

function setBadge(index, count) {
  return new Promise(resolve => {
    const normalized = normalizeCount(count)
    const nextText = buildTabText(index, normalized)
    if (lastTabTextMap[index] === nextText) {
      resolve(normalized)
      return
    }

    wx.setTabBarItem({
      index,
      text: nextText,
      success: () => {
        lastTabTextMap[index] = nextText
      },
      complete: () => {
        resolve(normalized)
      }
    })
  })
}

async function refreshChatUnreadBadge() {
  if (!auth.isLoggedIn()) {
    return setBadge(CHAT_TAB_INDEX, 0)
  }
  if (chatBadgeLoadingPromise) {
    return chatBadgeLoadingPromise
  }
  chatBadgeLoadingPromise = request.get('/conversations/unread-count')
    .then(res => setBadge(CHAT_TAB_INDEX, res.data))
    .catch(() => setBadge(CHAT_TAB_INDEX, 0))
    .finally(() => {
      chatBadgeLoadingPromise = null
    })
  return chatBadgeLoadingPromise
}

async function refreshNoticeUnreadBadge() {
  if (!auth.isLoggedIn()) {
    return setBadge(ME_TAB_INDEX, 0)
  }
  if (noticeBadgeLoadingPromise) {
    return noticeBadgeLoadingPromise
  }
  noticeBadgeLoadingPromise = request.get('/notices/unread-count')
    .then(res => setBadge(ME_TAB_INDEX, res.data))
    .catch(() => setBadge(ME_TAB_INDEX, 0))
    .finally(() => {
      noticeBadgeLoadingPromise = null
    })
  return noticeBadgeLoadingPromise
}

function setChatBadge(count) {
  return setBadge(CHAT_TAB_INDEX, count)
}

function setNoticeBadge(count) {
  return setBadge(ME_TAB_INDEX, count)
}

async function refreshAllTabBadges() {
  await Promise.all([refreshChatUnreadBadge(), refreshNoticeUnreadBadge()])
}

module.exports = {
  refreshChatUnreadBadge,
  refreshNoticeUnreadBadge,
  refreshAllTabBadges,
  setChatBadge,
  setNoticeBadge
}
