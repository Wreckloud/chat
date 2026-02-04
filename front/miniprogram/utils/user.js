const DEFAULT_AVATAR = '/images/default-avatar.png'

function withDefaultAvatar(user) {
  if (!user) return null
  const normalized = { ...user }
  if (!normalized.avatar) {
    normalized.avatar = DEFAULT_AVATAR
  }
  return normalized
}

function withDefaultAvatarList(list) {
  if (!Array.isArray(list)) return []
  return list.map(withDefaultAvatar)
}

module.exports = {
  DEFAULT_AVATAR,
  withDefaultAvatar,
  withDefaultAvatarList
}
