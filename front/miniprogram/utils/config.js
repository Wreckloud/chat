/**
 * API 基础地址配置
 *
 * 单人维护约定：
 * 1) 默认走本地联调地址，便于开发阶段直接调试
 * 2) 如需切到线上，可在控制台执行：
 *    wx.setStorageSync('WOLFCHAT_BASE_URL', 'https://api.wreckloud.com/api')
 * 3) 如需切回本地，可在控制台执行：
 *    wx.setStorageSync('WOLFCHAT_BASE_URL', 'http://127.0.0.1:8080/api')
 */
const DEFAULT_BASE_URL = 'http://127.0.0.1:8080/api'
const STORAGE_KEY = 'WOLFCHAT_BASE_URL'

function normalizeBaseURL(url) {
  if (!url || typeof url !== 'string') {
    return DEFAULT_BASE_URL
  }
  const trimmed = url.trim()
  if (!trimmed) {
    return DEFAULT_BASE_URL
  }
  return trimmed.endsWith('/') ? trimmed.slice(0, -1) : trimmed
}

function readRuntimeBaseURL() {
  try {
    if (typeof wx !== 'undefined' && typeof wx.getStorageSync === 'function') {
      return wx.getStorageSync(STORAGE_KEY) || ''
    }
  } catch (e) {
    // 忽略读取失败，回落默认值
  }
  return ''
}

const config = {
  // API 基础地址（支持运行时覆盖）
  baseURL: normalizeBaseURL(readRuntimeBaseURL() || DEFAULT_BASE_URL),

  // 请求超时时间（毫秒）
  timeout: 10000
}

module.exports = config


