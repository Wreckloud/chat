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

function formatWeekday(date) {
  const weekdays = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
  return weekdays[date.getDay()]
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
 * 消息分隔日期（仅日期，不包含时分）
 */
function formatDateLabel(dateStr) {
  const date = toDate(dateStr)
  if (!date) return ''

  const year = date.getFullYear()
  const month = date.getMonth() + 1
  const day = date.getDate()
  return `${year}年${month}月${day}日${formatWeekday(date)}`
}

/**
 * 消息头部时间（日期 + 时分）
 */
function formatMessageMetaTime(dateStr) {
  const date = toDate(dateStr)
  if (!date) return ''

  const year = date.getFullYear()
  const month = date.getMonth() + 1
  const day = date.getDate()
  const hours = date.getHours().toString().padStart(2, '0')
  const minutes = date.getMinutes().toString().padStart(2, '0')
  return `${year}年${month}月${day}日${formatWeekday(date)} ${hours}:${minutes}`
}

module.exports = {
  formatTime,
  formatDateLabel,
  formatMessageMetaTime
}
