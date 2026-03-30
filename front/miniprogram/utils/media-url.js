/**
 * 媒体 URL 规范化
 * 仅做字符串清洗，不在此处强制改写协议。
 * 协议适配（http/https）应由环境配置与后端部署策略解决。
 */

function normalizeMediaUrl(url) {
  const raw = String(url || '').trim()
  if (!raw) {
    return ''
  }
  return raw
}

module.exports = {
  normalizeMediaUrl
}
