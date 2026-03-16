const ADMIN_TOKEN_KEY = 'wolfchat_admin_token'
const ADMIN_INFO_KEY = 'wolfchat_admin_info'

function readToken() {
  return localStorage.getItem(ADMIN_TOKEN_KEY) || ''
}

function writeToken(token) {
  localStorage.setItem(ADMIN_TOKEN_KEY, token || '')
}

function clearToken() {
  localStorage.removeItem(ADMIN_TOKEN_KEY)
}

function readAdminInfo() {
  const raw = localStorage.getItem(ADMIN_INFO_KEY)
  if (!raw) {
    return null
  }
  try {
    return JSON.parse(raw)
  } catch (error) {
    return null
  }
}

function writeAdminInfo(adminInfo) {
  if (!adminInfo) {
    localStorage.removeItem(ADMIN_INFO_KEY)
    return
  }
  localStorage.setItem(ADMIN_INFO_KEY, JSON.stringify(adminInfo))
}

function clearAdminInfo() {
  localStorage.removeItem(ADMIN_INFO_KEY)
}

export {
  ADMIN_TOKEN_KEY,
  ADMIN_INFO_KEY,
  readToken,
  writeToken,
  clearToken,
  readAdminInfo,
  writeAdminInfo,
  clearAdminInfo
}
