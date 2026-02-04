/**
 * 时间格式化工具
 */

/**
 * 格式化时间 - 用于会话列表
 * @param {string} dateStr - ISO时间字符串
 * @returns {string} 格式化后的时间
 */
function formatTime(dateStr) {
  if (!dateStr) return ''

  const date = new Date(dateStr)
  const now = new Date()
  
  // 获取今天0点
  const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  
  // 获取昨天0点
  const yesterdayStart = new Date(todayStart)
  yesterdayStart.setDate(yesterdayStart.getDate() - 1)
  
  // 格式化小时和分钟
  const hours = date.getHours().toString().padStart(2, '0')
  const minutes = date.getMinutes().toString().padStart(2, '0')
  const timeStr = `${hours}:${minutes}`
  
  // 今天
  if (date >= todayStart) {
    return timeStr
  }
  
  // 昨天
  if (date >= yesterdayStart) {
    return `昨天 ${timeStr}`
  }
  
  const year = date.getFullYear()
  const month = (date.getMonth() + 1).toString().padStart(2, '0')
  const day = date.getDate().toString().padStart(2, '0')
  
  // 本年
  if (year === now.getFullYear()) {
    return `${month}-${day} ${timeStr}`
  }
  
  // 跨年
  return `${year}-${month}-${day}`
}

/**
 * 格式化消息时间 - 用于消息时间戳
 * @param {string} dateStr - ISO时间字符串
 * @returns {string} HH:mm格式
 */
function formatMessageTime(dateStr) {
  if (!dateStr) return ''
  
  const date = new Date(dateStr)
  const hours = date.getHours().toString().padStart(2, '0')
  const minutes = date.getMinutes().toString().padStart(2, '0')
  
  return `${hours}:${minutes}`
}

/**
 * 判断是否需要显示消息时间
 * 相邻消息间隔超过5分钟才显示
 * @param {string} currentTime - 当前消息时间
 * @param {string} prevTime - 上一条消息时间
 * @returns {boolean}
 */
function shouldShowTime(currentTime, prevTime) {
  if (!prevTime) return true
  
  const current = new Date(currentTime)
  const prev = new Date(prevTime)
  
  // 5分钟 = 300000毫秒
  return (current - prev) > 300000
}

module.exports = {
  formatTime,
  formatMessageTime,
  shouldShowTime
}

