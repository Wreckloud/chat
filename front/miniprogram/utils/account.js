/**
 * 账号输入处理工具
 */
const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

function normalizeText(value) {
  return (value || '').trim()
}

function normalizeEmail(value) {
  return normalizeText(value).toLowerCase()
}

function isValidEmail(value) {
  return EMAIL_REGEX.test(value)
}

module.exports = {
  normalizeText,
  normalizeEmail,
  isValidEmail
}
