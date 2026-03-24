/**
 * 请求封装（自动携带 token）
 */
const config = require('./config')
const auth = require('./auth')
const AUTH_ERROR_CODES = [2001, 2002, 2003]
const CLIENT_TYPE = 'MINIPROGRAM'

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
      return miniProgram.version || miniProgram.envVersion || ''
    }
  } catch (e) {
    // 忽略版本读取异常
  }
  return ''
}

/**
 * 发起请求
 */
function request(options) {
  return new Promise((resolve, reject) => {
    const requestUrl = config.baseURL + options.url
    const requestMethod = options.method || 'GET'
    const token = auth.getToken()

    const header = {
      'Content-Type': 'application/json',
      'X-Client-Type': CLIENT_TYPE,
      ...options.header
    }
    const clientVersion = resolveClientVersion()
    if (clientVersion && !header['X-Client-Version']) {
      header['X-Client-Version'] = clientVersion
    }
    if (token) {
      header['Authorization'] = `Bearer ${token}`
    }

    wx.request({
      url: requestUrl,
      method: requestMethod,
      data: options.data || {},
      header: header,
      timeout: options.timeout || config.timeout,
      success(res) {
        if (res.statusCode >= 200 && res.statusCode < 300) {
          const responseData = res.data || {}
          const code = responseData.code

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
          reject(createRequestError('网络请求失败，请稍后重试', {
            kind: 'http',
            statusCode: res.statusCode,
            requestUrl,
            raw: res
          }))
        }
      },
      fail(err) {
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
