const { PASSWORD_STRENGTH_COPY } = require('../constants/copy')

/**
 * 评估密码强度（仅提示，不阻止提交）
 * 规则：长度<6或字符类型过少为弱；2类为中；3类及以上为强
 */
function evaluatePasswordStrength(password) {
  const source = password || ''
  if (!source) {
    return {
      level: ''
    }
  }

  let score = 0
  if (/[0-9]/.test(source)) score += 1
  if (/[a-z]/.test(source)) score += 1
  if (/[A-Z]/.test(source)) score += 1
  if (/[^a-zA-Z0-9]/.test(source)) score += 1

  if (source.length < 6 || score <= 1) {
    return {
      level: 'weak'
    }
  }

  if (score <= 2) {
    return {
      level: 'medium'
    }
  }

  return {
    level: 'strong'
  }
}

function getPasswordStrengthInlineText(level) {
  if (level === 'weak') {
    return PASSWORD_STRENGTH_COPY.weakInline
  }
  if (level === 'medium') {
    return PASSWORD_STRENGTH_COPY.mediumInline
  }
  if (level === 'strong') {
    return PASSWORD_STRENGTH_COPY.strongInline
  }
  return ''
}

module.exports = {
  evaluatePasswordStrength,
  getPasswordStrengthInlineText
}
