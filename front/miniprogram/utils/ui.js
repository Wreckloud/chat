/**
 * 统一 UI 提示
 */
const LOGIN_ERROR_CODES = new Set([2004, 2005, 2006, 2007, 2011, 2012])
const SYSTEM_ERROR_CODES = new Set([1002, 1003])
const LOGIN_ERROR_MESSAGE = '账号或密码错误'
const NETWORK_ERROR_MESSAGE = '网络异常，请稍后重试'
const SYSTEM_ERROR_MESSAGE = '系统繁忙，请稍后重试'

function normalizeErrorCode(error) {
  if (!error || error.code == null) {
    return 0
  }
  const code = Number(error.code)
  return Number.isFinite(code) ? code : 0
}

function resolveErrorMessage(error, fallback, options = {}) {
  const scene = String(options.scene || '').trim().toLowerCase()
  if (scene === 'login') {
    return LOGIN_ERROR_MESSAGE
  }

  const code = normalizeErrorCode(error)
  if (LOGIN_ERROR_CODES.has(code)) {
    return LOGIN_ERROR_MESSAGE
  }
  if (error && (error.kind === 'network' || error.kind === 'http')) {
    return NETWORK_ERROR_MESSAGE
  }
  if (SYSTEM_ERROR_CODES.has(code)) {
    return SYSTEM_ERROR_MESSAGE
  }

  if (typeof error === 'string' && error.trim()) {
    return error
  }
  if (error && typeof error.message === 'string' && error.message.trim()) {
    return error.message
  }
  return fallback
}

function toastError(error, fallback = '操作失败', options = {}) {
  const message = resolveErrorMessage(error, fallback, options)
  if (!message) {
    return
  }
  wx.showToast({
    title: message,
    icon: 'none'
  })
}

function toastSuccess(message = '操作成功', options = {}) {
  if (options.silent) {
    return
  }
  wx.showToast({
    title: message,
    icon: 'success'
  })
}

module.exports = {
  toastError,
  toastSuccess
}
