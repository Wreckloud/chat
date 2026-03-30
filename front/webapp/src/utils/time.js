function pad(value) {
  return String(value).padStart(2, '0')
}

function toDate(dateValue) {
  if (!dateValue) {
    return null
  }
  const normalized = typeof dateValue === 'string' ? dateValue.replace(' ', 'T') : dateValue
  const date = new Date(normalized)
  return Number.isNaN(date.getTime()) ? null : date
}

export function formatClock(dateValue) {
  const date = toDate(dateValue)
  if (!date) {
    return '--:--'
  }
  return `${pad(date.getHours())}:${pad(date.getMinutes())}`
}

export function formatDateTime(dateValue) {
  const date = toDate(dateValue)
  if (!date) {
    return '--'
  }
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

export function formatRelative(dateValue) {
  const date = toDate(dateValue)
  if (!date) {
    return '--'
  }
  const now = Date.now()
  const deltaMs = Math.max(0, now - date.getTime())
  const minute = 60 * 1000
  const hour = 60 * minute
  const day = 24 * hour
  if (deltaMs < minute) {
    return '刚刚'
  }
  if (deltaMs < hour) {
    return `${Math.floor(deltaMs / minute)}分钟前`
  }
  if (deltaMs < day) {
    return `${Math.floor(deltaMs / hour)}小时前`
  }
  return formatDateTime(date)
}

