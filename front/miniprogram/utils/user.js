// 统一默认头像资源
const DEFAULT_AVATAR = '/images/default-avatar.png'

function trimTextField(value) {
  return typeof value === 'string' ? value.trim() : value
}

function withDefaultAvatar(user) {
  if (!user) return null
  // 仅做前端展示数据规整，避免昵称尾空格拉开头衔布局。
  const normalized = {
    ...user,
    nickname: trimTextField(user.nickname),
    targetNickname: trimTextField(user.targetNickname),
    displayName: trimTextField(user.displayName),
    wolfNo: trimTextField(user.wolfNo),
    targetWolfNo: trimTextField(user.targetWolfNo)
  }
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

function normalizeUser(user) {
  return withDefaultAvatar(user)
}

function normalizeUserList(list) {
  return withDefaultAvatarList(list)
}

function openUserProfile(user) {
  if (!user || !user.userId) return
  const url = `/pages/user-detail/user-detail?userId=${user.userId}`
  wx.navigateTo({ url })
}

module.exports = {
  DEFAULT_AVATAR,
  normalizeUser,
  normalizeUserList,
  openUserProfile
}
