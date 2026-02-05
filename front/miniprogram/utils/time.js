/**
 * 时间格式化工具（ISO）
 */

function toDate(dateStr) {
  if (!dateStr) return null

  // 统一要求后端返回 ISO 本地时间：yyyy-MM-ddTHH:mm:ss
  const parts = dateStr.replace('T', ' ').split(' ')
  if (parts.length < 2) return null

  const datePart = parts[0]
  const timePart = parts[1]
  const d = datePart.split('-').map(Number)
  const t = timePart.split(':').map(Number)
  if (d.length !== 3 || t.length < 2) return null

  const year = d[0]
  const month = d[1]
  const day = d[2]
  const hour = t[0]
  const minute = t[1]
  const second = t.length > 2 ? t[2] : 0
  return new Date(year, month - 1, day, hour, minute, second)
}

/**
 * 会话列表时间
 * @param {string} dateStr
 */
function formatTime(dateStr) {
  const date = toDate(dateStr)
  if (!date) return ''

  const now = new Date()
  const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const yesterdayStart = new Date(todayStart)
  yesterdayStart.setDate(yesterdayStart.getDate() - 1)

  const hours = date.getHours().toString().padStart(2, '0')
  const minutes = date.getMinutes().toString().padStart(2, '0')
  const timeStr = hours + ':' + minutes

  if (date >= todayStart) {
    return timeStr
  }

  if (date >= yesterdayStart) {
    return '昨天 ' + timeStr
  }

  const year = date.getFullYear()
  const month = (date.getMonth() + 1).toString().padStart(2, '0')
  const day = date.getDate().toString().padStart(2, '0')

  if (year === now.getFullYear()) {
    return month + '-' + day + ' ' + timeStr
  }

  return year + '-' + month + '-' + day
}

/**
 * 消息时间
 * @param {string} dateStr
 */
function formatMessageTime(dateStr) {
  const date = toDate(dateStr)
  if (!date) return ''

  const hours = date.getHours().toString().padStart(2, '0')
  const minutes = date.getMinutes().toString().padStart(2, '0')

  return hours + ':' + minutes
}

/**
 * 是否显示消息时间（间隔 > 5 分钟）
 */
function shouldShowTime(currentTime, prevTime) {
  if (!prevTime) return true

  const current = toDate(currentTime)
  const prev = toDate(prevTime)
  if (!current || !prev) return false

  return (current - prev) > 300000
}

module.exports = {
  formatTime,
  formatMessageTime,
  shouldShowTime
}
