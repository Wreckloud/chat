// 统一默认头像资源
const DEFAULT_AVATAR = '/images/default-avatar.png'

function withDefaultAvatar(user) {
  if (!user) return null
  // 仅兜底缺失头像，避免覆盖有效头像
  const normalized = { ...user }
  if (!normalized.avatar) {
    normalized.avatar = DEFAULT_AVATAR
  }
  return normalized
}

function withDefaultAvatarList(list) {
  if (!Array.isArray(list)) return []
  // 批量兜底头像
  return list.map(withDefaultAvatar)
}

module.exports = {
  DEFAULT_AVATAR,
  withDefaultAvatar,
  withDefaultAvatarList
}
