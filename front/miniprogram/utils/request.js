/**
 * 请求封装（自动携带 token）
 */
const config = require('./config')
const auth = require('./auth')
const AUTH_ERROR_CODES = [2001, 2002, 2003]
const CLIENT_TYPE = 'MINIPROGRAM'
const DEBUG_HTTP_KEY = 'WOLFCHAT_DEBUG_HTTP'

function createRequestError(message, extra = {}) {
  const error = new Error(message)
  Object.keys(extra).forEach(key => {
    error[key] = extra[key]
  })
  return error
}

function resolveClientVersion() {
  try {
    if (typeof wx === 'undefined') {
      return ''
    }
    if (typeof wx.getAccountInfoSync === 'function') {
      const accountInfo = wx.getAccountInfoSync() || {}
      const miniProgram = accountInfo.miniProgram || {}
      // 小程序版本优先；开发版通常为空，回退到 envVersion 方便区分环境。
      return miniProgram.version || miniProgram.envVersion || ''
    }
  } catch (e) {
    // 忽略版本读取异常，避免影响主流程。
  }
  return ''
}

function shouldDebugRequest(url) {
  try {
    if (typeof wx !== 'undefined' && typeof wx.getStorageSync === 'function') {
      if (wx.getStorageSync(DEBUG_HTTP_KEY)) {
        return true
      }
    }
  } catch (e) {
    // 忽略调试开关读取失败
  }
  // 认证链路默认打印，优先排查登录/注册未命中后端的问题。
  return /\/auth\/(login|register)/.test(url || '')
}

function safeDataPreview(data) {
  if (!data || typeof data !== 'object') {
    return data
  }
  const copy = { ...data }
  if (Object.prototype.hasOwnProperty.call(copy, 'loginKey')) {
    copy.loginKey = '***'
  }
  if (Object.prototype.hasOwnProperty.call(copy, 'password')) {
    copy.password = '***'
  }
  if (Object.prototype.hasOwnProperty.call(copy, 'confirmPassword')) {
    copy.confirmPassword = '***'
  }
  return copy
}

/**
 * 发起请求
 */
function request(options) {
  return new Promise((resolve, reject) => {
    const requestUrl = config.baseURL + options.url
    const requestMethod = options.method || 'GET'
    const debugRequest = shouldDebugRequest(options.url || '')
    // 获取 token
    const token = auth.getToken()
    
    // 设置请求头
    const header = {
      'Content-Type': 'application/json',
      'X-Client-Type': CLIENT_TYPE,
      ...options.header
    }
    const clientVersion = resolveClientVersion()
    if (clientVersion && !header['X-Client-Version']) {
      header['X-Client-Version'] = clientVersion
    }
    if (debugRequest) {
      console.log('[HTTP] request', {
        url: requestUrl,
        method: requestMethod,
        data: safeDataPreview(options.data || {})
      })
    }
    
    // 如果已登录，自动携带 token
    if (token) {
      header['Authorization'] = `Bearer ${token}`
    }

    // 发起请求
    wx.request({
      url: requestUrl,
      method: requestMethod,
      data: options.data || {},
      header: header,
      timeout: options.timeout || config.timeout,
      success(res) {
        if (debugRequest) {
          console.log('[HTTP] response', {
            url: requestUrl,
            method: requestMethod,
            statusCode: res.statusCode,
            body: res.data
          })
        }
        // HTTP 状态码 200-299 视为成功
        if (res.statusCode >= 200 && res.statusCode < 300) {
          const responseData = res.data || {}
          const code = responseData.code

          // 检查业务状态码
          if (code === 0) {
            resolve(responseData)
          } else {
            if (AUTH_ERROR_CODES.includes(code)) {
              auth.handleAuthExpired()
            }
            reject(createRequestError(responseData.message || '请求失败', {
              code: Number(code) || 0,
              kind: 'business',
              requestUrl,
              raw: responseData
            }))
          }
        } else {
          // HTTP 错误
          reject(createRequestError('网络请求失败，请稍后重试', {
            kind: 'http',
            statusCode: res.statusCode,
            requestUrl,
            raw: res
          }))
        }
      },
      fail(err) {
        if (debugRequest) {
          console.log('[HTTP] fail', {
            url: requestUrl,
            method: requestMethod,
            error: err
          })
        }
        reject(createRequestError('网络异常，请稍后重试', {
          kind: 'network',
          requestUrl,
          raw: err
        }))
      }
    })
  })
}

module.exports = {
  request,
  get: (url, data, options = {}) => request({ ...options, url, method: 'GET', data }),
  post: (url, data, options = {}) => request({ ...options, url, method: 'POST', data }),
  put: (url, data, options = {}) => request({ ...options, url, method: 'PUT', data }),
  del: (url, data, options = {}) => request({ ...options, url, method: 'DELETE', data })
}
