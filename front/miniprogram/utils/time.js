/**
 * 时间格式化工具（ISO）
 */

const ONE_MINUTE_MS = 60 * 1000
const ONE_HOUR_MS = 60 * ONE_MINUTE_MS
const ONE_DAY_MS = 24 * ONE_HOUR_MS
const WEEKDAY_SHORT = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']

function parseDateTime(dateStr) {
  if (!dateStr) return null
  if (dateStr instanceof Date) {
    return Number.isNaN(dateStr.getTime()) ? null : dateStr
  }

  const raw = String(dateStr).trim()
  if (!raw) return null

  const normalized = raw.replace(' ', 'T')
  const hasTimezone = /[zZ]$|[+-]\d{2}:\d{2}$/.test(normalized)
  if (hasTimezone) {
    const zonedDate = new Date(normalized)
    return Number.isNaN(zonedDate.getTime()) ? null : zonedDate
  }

  const localMatch = normalized.match(
    /^(\d{4})-(\d{1,2})-(\d{1,2})T(\d{1,2}):(\d{1,2})(?::(\d{1,2})(?:\.(\d{1,3}))?)?$/
  )
  if (localMatch) {
    const year = Number(localMatch[1])
    const month = Number(localMatch[2])
    const day = Number(localMatch[3])
    const hour = Number(localMatch[4])
    const minute = Number(localMatch[5])
    const second = localMatch[6] ? Number(localMatch[6]) : 0
    const millisecond = localMatch[7]
      ? Number(localMatch[7].padEnd(3, '0').slice(0, 3))
      : 0
    if (![year, month, day, hour, minute, second, millisecond].every(Number.isFinite)) {
      return null
    }
    return new Date(year, month - 1, day, hour, minute, second, millisecond)
  }

  const fallbackDate = new Date(raw)
  return Number.isNaN(fallbackDate.getTime()) ? null : fallbackDate
}

function pad2(value) {
  return String(value).padStart(2, '0')
}

function formatHHmm(date) {
  return `${pad2(date.getHours())}:${pad2(date.getMinutes())}`
}

function formatMMDD(date) {
  return `${pad2(date.getMonth() + 1)}-${pad2(date.getDate())}`
}

function formatYYYYMMDD(date) {
  return `${date.getFullYear()}-${pad2(date.getMonth() + 1)}-${pad2(date.getDate())}`
}

function formatWeekdayShort(date) {
  return WEEKDAY_SHORT[date.getDay()]
}

function getDayStart(date) {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate())
}

function getDayDiff(targetDate, now) {
  const targetStart = getDayStart(targetDate).getTime()
  const nowStart = getDayStart(now).getTime()
  return Math.floor((nowStart - targetStart) / ONE_DAY_MS)
}

/**
 * 会话列表时间：
 * 今天 HH:mm，昨天，近7天 周X，今年 MM-DD，跨年 YYYY-MM-DD
 */
function formatConversationTime(dateStr) {
  const date = parseDateTime(dateStr)
  if (!date) return ''

  const now = new Date()
  const dayDiff = getDayDiff(date, now)

  if (dayDiff === 0) {
    return formatHHmm(date)
  }
  if (dayDiff === 1) {
    return '昨天'
  }
  if (dayDiff > 1 && dayDiff < 7) {
    return formatWeekdayShort(date)
  }
  if (date.getFullYear() === now.getFullYear()) {
    return formatMMDD(date)
  }
  return formatYYYYMMDD(date)
}

/**
 * 聊天分割线日期：
 * 今天，昨天，其他 YYYY年M月D日 周X
 */
function formatMessageDividerDate(dateStr) {
  const date = parseDateTime(dateStr)
  if (!date) return ''

  const now = new Date()
  const dayDiff = getDayDiff(date, now)

  if (dayDiff === 0) {
    return '今天'
  }
  if (dayDiff === 1) {
    return '昨天'
  }
  return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日 ${formatWeekdayShort(date)}`
}

/**
 * 消息头部时间：仅显示 HH:mm
 */
function formatMessageMetaTime(dateStr) {
  const date = parseDateTime(dateStr)
  if (!date) return ''
  return formatHHmm(date)
}

/**
 * 上次在线文案：
 * 刚刚在线 / N分钟前在线 / N小时前在线 / 昨天 HH:mm 在线 / YYYY-MM-DD HH:mm 在线
 */
function formatLastSeenText(dateStr) {
  const date = parseDateTime(dateStr)
  if (!date) {
    return '上次在线 未知'
  }

  const now = new Date()
  let diffMs = now.getTime() - date.getTime()
  if (diffMs < 0) {
    diffMs = 0
  }

  const dayDiff = getDayDiff(date, now)
  if (dayDiff === 0 && diffMs < ONE_MINUTE_MS) {
    return '刚刚在线'
  }
  if (dayDiff === 0 && diffMs < ONE_HOUR_MS) {
    const minutes = Math.max(1, Math.floor(diffMs / ONE_MINUTE_MS))
    return `${minutes} 分钟前在线`
  }
  if (dayDiff === 0) {
    const hours = Math.max(1, Math.floor(diffMs / ONE_HOUR_MS))
    return `${hours} 小时前在线`
  }
  if (dayDiff === 1) {
    return `昨天 ${formatHHmm(date)} 在线`
  }
  return `${formatYYYYMMDD(date)} ${formatHHmm(date)} 在线`
}

/**
 * 帖子/评论时间：
 * 今天 HH:mm，昨天 HH:mm，今年 MM-DD HH:mm，跨年 YYYY-MM-DD HH:mm
 */
function formatPostTime(dateStr) {
  const date = parseDateTime(dateStr)
  if (!date) return ''

  const now = new Date()
  const dayDiff = getDayDiff(date, now)
  const hm = formatHHmm(date)

  if (dayDiff === 0) {
    return hm
  }
  if (dayDiff === 1) {
    return `昨天 ${hm}`
  }
  if (date.getFullYear() === now.getFullYear()) {
    return `${formatMMDD(date)} ${hm}`
  }
  return `${formatYYYYMMDD(date)} ${hm}`
}

/**
 * 社区时间文案：
 * N秒前 / N分钟前 / N小时前 / 昨天 HH:mm / MM-DD HH:mm / YYYY-MM-DD HH:mm
 */
function formatRelativeTime(dateStr) {
  const date = parseDateTime(dateStr)
  if (!date) return ''

  const now = new Date()
  let diffMs = now.getTime() - date.getTime()
  if (diffMs < 0) {
    diffMs = 0
  }

  const dayDiff = getDayDiff(date, now)
  if (dayDiff === 0 && diffMs < ONE_MINUTE_MS) {
    const seconds = Math.max(1, Math.floor(diffMs / 1000))
    return `${seconds} 秒前`
  }
  if (dayDiff === 0 && diffMs < ONE_HOUR_MS) {
    const minutes = Math.max(1, Math.floor(diffMs / ONE_MINUTE_MS))
    return `${minutes} 分钟前`
  }
  if (dayDiff === 0) {
    const hours = Math.max(1, Math.floor(diffMs / ONE_HOUR_MS))
    return `${hours} 小时前`
  }

  const hm = formatHHmm(date)
  if (dayDiff === 1) {
    return `昨天 ${hm}`
  }
  if (date.getFullYear() === now.getFullYear()) {
    return `${formatMMDD(date)} ${hm}`
  }
  return `${formatYYYYMMDD(date)} ${hm}`
}

module.exports = {
  parseDateTime,
  formatConversationTime,
  formatMessageDividerDate,
  formatMessageMetaTime,
  formatLastSeenText,
  formatPostTime,
  formatRelativeTime
}
