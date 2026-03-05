/**
 * 评估密码强度（仅提示，不阻止提交）
 * 规则：长度<6或字符类型过少为弱；2类为中；3类及以上为强
 */
function evaluatePasswordStrength(password) {
  const source = password || ''
  if (!source) {
    return {
      level: '',
      text: ''
    }
  }

  let score = 0
  if (/[0-9]/.test(source)) score += 1
  if (/[a-z]/.test(source)) score += 1
  if (/[A-Z]/.test(source)) score += 1
  if (/[^a-zA-Z0-9]/.test(source)) score += 1

  if (source.length < 6 || score <= 1) {
    return {
      level: 'weak',
      text: '强度偏弱，建议至少 8 位并混合字母和数字'
    }
  }

  if (score <= 2) {
    return {
      level: 'medium',
      text: '强度中等，可加入大写字母或符号提高安全性'
    }
  }

  return {
    level: 'strong',
    text: '强度较高，当前密码可用'
  }
}

function getPasswordStrengthInlineText(level) {
  if (level === 'weak') {
    return '弱 · 建议混合字符'
  }
  if (level === 'medium') {
    return '中 · 可加符号'
  }
  if (level === 'strong') {
    return '强 · 可用'
  }
  return ''
}

module.exports = {
  evaluatePasswordStrength,
  getPasswordStrengthInlineText
}
