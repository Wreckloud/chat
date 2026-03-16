function resolveDisplayName(user) {
  if (!user) {
    return '行者'
  }
  const nickname = String(user.nickname || '').trim()
  if (nickname) {
    return nickname
  }
  const wolfNo = String(user.wolfNo || '').trim()
  if (wolfNo) {
    return wolfNo
  }
  return '行者'
}

function normalizeTitleName(value) {
  const titleName = String(value || '').trim()
  return titleName.length > 0 ? titleName : ''
}

function normalizeTitleColor(value) {
  const titleColor = String(value || '').trim()
  return titleColor.length > 0 ? titleColor : ''
}

function attachDisplayTitle(target, titleName, titleColor) {
  if (!target || typeof target !== 'object') {
    return target
  }
  const normalizedTitleName = normalizeTitleName(titleName)
  const normalizedTitleColor = normalizeTitleColor(titleColor)
  return {
    ...target,
    displayName: resolveDisplayName(target),
    displayTitleName: normalizedTitleName,
    displayTitleColor: normalizedTitleColor
  }
}

function formatNameWithTitle(user, titleName) {
  const displayName = resolveDisplayName(user)
  const normalizedTitleName = normalizeTitleName(titleName)
  if (!normalizedTitleName) {
    return displayName
  }
  return `[${normalizedTitleName}]${displayName}`
}

module.exports = {
  resolveDisplayName,
  normalizeTitleName,
  normalizeTitleColor,
  attachDisplayTitle,
  formatNameWithTitle
}
