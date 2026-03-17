function normalizeComparableValue(value) {
  if (value == null) {
    return ''
  }
  return String(value).trim()
}

function isSameUserProfile(left, right) {
  const leftUserId = Number(left && left.userId)
  const rightUserId = Number(right && right.userId)
  if (!Number.isFinite(leftUserId) || !Number.isFinite(rightUserId) || leftUserId <= 0 || rightUserId <= 0) {
    return false
  }

  return leftUserId === rightUserId
    && normalizeComparableValue(left && left.wolfNo) === normalizeComparableValue(right && right.wolfNo)
    && normalizeComparableValue(left && left.nickname) === normalizeComparableValue(right && right.nickname)
    && normalizeComparableValue(left && left.avatar) === normalizeComparableValue(right && right.avatar)
    && normalizeComparableValue(left && left.equippedTitleName) === normalizeComparableValue(right && right.equippedTitleName)
    && normalizeComparableValue(left && left.equippedTitleColor) === normalizeComparableValue(right && right.equippedTitleColor)
}

function cacheUserProfile(page, normalizeUser, user) {
  if (!page || !user || !user.userId) {
    return false
  }
  if (!page.userProfileMap) {
    page.userProfileMap = {}
  }

  const userId = Number(user.userId)
  if (!Number.isFinite(userId) || userId <= 0) {
    return false
  }

  const existing = page.userProfileMap[userId] || {}
  const hasTitleName = Object.prototype.hasOwnProperty.call(user, 'equippedTitleName')
  const hasTitleColor = Object.prototype.hasOwnProperty.call(user, 'equippedTitleColor')
  // 仅补齐缺失字段，避免后续不完整数据覆盖已缓存资料。
  const merged = {
    userId,
    wolfNo: user.wolfNo || existing.wolfNo,
    nickname: user.nickname || existing.nickname,
    avatar: user.avatar || existing.avatar,
    equippedTitleName: hasTitleName ? user.equippedTitleName : existing.equippedTitleName,
    equippedTitleColor: hasTitleColor ? user.equippedTitleColor : existing.equippedTitleColor
  }
  const normalized = normalizeUser(merged) || {}
  if (isSameUserProfile(existing, normalized)) {
    return false
  }
  page.userProfileMap[userId] = normalized
  return true
}

function initCurrentUserContext(page, auth, normalizeUser, options = {}) {
  const userInfo = auth.getUserInfo()
  // 页面进入时重建当前用户上下文，避免复用上一页残留状态。
  page.pageUnloaded = false
  page.userProfileMap = {}
  if (options.enableLoadingUserGuard) {
    page.loadingUserIds = new Set()
  }
  page.currentUserId = userInfo ? Number(userInfo.userId) : 0
  page.currentUser = normalizeUser(userInfo) || {}
  cacheUserProfile(page, normalizeUser, page.currentUser)
  return page.currentUser
}

function resolveSenderProfile(page, senderId, isSelf, options = {}) {
  const numericSenderId = Number(senderId)
  if (!Number.isFinite(numericSenderId) || numericSenderId <= 0) {
    return {}
  }

  if (isSelf && page.currentUser && Number(page.currentUser.userId) === numericSenderId) {
    return page.currentUser
  }

  const targetUser = options.targetUser || null
  if (targetUser && Number(targetUser.userId) === numericSenderId) {
    return targetUser
  }

  return page.userProfileMap && page.userProfileMap[numericSenderId]
    ? page.userProfileMap[numericSenderId]
    : {}
}

function collectUniqueSenderIds(messages) {
  if (!Array.isArray(messages) || messages.length === 0) {
    return []
  }
  return Array.from(new Set(
    messages
      .map(item => Number(item.senderId))
      .filter(id => Number.isFinite(id) && id > 0)
  ))
}

function cacheSenderProfilesFromMessages(messages, cacheFn) {
  if (!Array.isArray(messages) || messages.length === 0 || typeof cacheFn !== 'function') {
    return
  }

  const cachedSenderIdMap = {}
  for (let index = 0; index < messages.length; index++) {
    const message = messages[index]
    const senderId = Number(message.senderId)
    if (!Number.isFinite(senderId) || senderId <= 0 || cachedSenderIdMap[senderId]) {
      continue
    }

    cachedSenderIdMap[senderId] = true
    const senderProfile = {
      userId: message.senderId,
      wolfNo: message.senderWolfNo,
      nickname: message.senderNickname,
      avatar: message.senderAvatar
    }
    if (Object.prototype.hasOwnProperty.call(message, 'senderEquippedTitleName')) {
      senderProfile.equippedTitleName = message.senderEquippedTitleName
    }
    if (Object.prototype.hasOwnProperty.call(message, 'senderEquippedTitleColor')) {
      senderProfile.equippedTitleColor = message.senderEquippedTitleColor
    }
    cacheFn(senderProfile)
  }
}

function ensureSenderProfiles(page, request, normalizeUser, messages, options = {}) {
  if (!Array.isArray(messages) || messages.length === 0) {
    return
  }

  cacheSenderProfilesFromMessages(messages, (user) => {
    cacheUserProfile(page, normalizeUser, user)
  })

  if (!options.fetchMissing || !request || typeof request.get !== 'function') {
    return
  }

  const senderIds = collectUniqueSenderIds(messages)
  for (let index = 0; index < senderIds.length; index++) {
    ensureUserProfileById(page, request, normalizeUser, senderIds[index], {
      onLoaded: options.onLoaded
    })
  }
}

function loadCurrentUserProfile(page, request, normalizeUser, onLoaded) {
  return request.get('/users/me')
    .then(res => {
      if (!res || !res.data) {
        return
      }
      const user = normalizeUser(res.data) || {}
      if (!user.userId) {
        return
      }
      const previousCurrentUser = page.currentUser || {}
      const currentUserId = Number(user.userId)
      const cacheUpdated = cacheUserProfile(page, normalizeUser, user)
      const nextCurrentUser = page.userProfileMap && page.userProfileMap[currentUserId]
        ? page.userProfileMap[currentUserId]
        : user
      page.currentUserId = currentUserId
      page.currentUser = nextCurrentUser

      if (typeof onLoaded === 'function' && (!isSameUserProfile(previousCurrentUser, nextCurrentUser) || cacheUpdated)) {
        onLoaded(nextCurrentUser)
      }
    })
    .catch(() => {})
}

function ensureUserProfileById(page, request, normalizeUser, userId, options = {}) {
  const numericUserId = Number(userId)
  if (!Number.isFinite(numericUserId) || numericUserId <= 0) {
    return Promise.resolve()
  }

  const cached = page.userProfileMap && page.userProfileMap[numericUserId]
  if (cached && (cached.nickname || cached.wolfNo)) {
    return Promise.resolve(cached)
  }

  if (!page.loadingUserIds) {
    page.loadingUserIds = new Set()
  }
  // 同一用户资料只允许一个在途请求，避免频繁重算消息块。
  if (page.loadingUserIds.has(numericUserId)) {
    return Promise.resolve()
  }

  page.loadingUserIds.add(numericUserId)
  const req = Number(page.currentUserId) === numericUserId
    ? request.get('/users/me')
    : request.get(`/users/${numericUserId}`)

  return req
    .then(res => {
      if (!res || !res.data) {
        return
      }
      const normalized = normalizeUser(res.data) || {}
      if (!normalized.userId) {
        return
      }

      const cacheUpdated = cacheUserProfile(page, normalizeUser, normalized)
      const nextProfile = page.userProfileMap && page.userProfileMap[numericUserId]
        ? page.userProfileMap[numericUserId]
        : normalized
      if (cacheUpdated && typeof options.onLoaded === 'function') {
        options.onLoaded(nextProfile, numericUserId)
      }
      return nextProfile
    })
    .catch(() => {})
    .finally(() => {
      if (page.loadingUserIds) {
        page.loadingUserIds.delete(numericUserId)
      }
    })
}

function getUserIdFromTapEvent(event) {
  const dataset = event && event.currentTarget ? event.currentTarget.dataset || {} : {}
  const userId = Number(dataset.userId)
  if (!Number.isFinite(userId) || userId <= 0) {
    return 0
  }
  return userId
}

function openUserProfileByTapEvent(event, openUserProfile) {
  if (typeof openUserProfile !== 'function') {
    return
  }
  const userId = getUserIdFromTapEvent(event)
  if (!userId) {
    return
  }
  openUserProfile({ userId })
}

module.exports = {
  initCurrentUserContext,
  cacheUserProfile,
  resolveSenderProfile,
  collectUniqueSenderIds,
  cacheSenderProfilesFromMessages,
  ensureSenderProfiles,
  loadCurrentUserProfile,
  ensureUserProfileById,
  getUserIdFromTapEvent,
  openUserProfileByTapEvent
}
