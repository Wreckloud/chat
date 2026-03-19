/**
 * 请求封装（自动携带 token）
 */
const config = require('./config')
const auth = require('./auth')
const AUTH_ERROR_CODES = [2001, 2002, 2003]

function createRequestError(message, extra = {}) {
  const error = new Error(message)
  Object.keys(extra).forEach(key => {
    error[key] = extra[key]
  })
  return error
}

/**
 * 发起请求
 */
function request(options) {
  return new Promise((resolve, reject) => {
    // 获取 token
    const token = auth.getToken()
    
    // 设置请求头
    const header = {
      'Content-Type': 'application/json',
      ...options.header
    }
    
    // 如果已登录，自动携带 token
    if (token) {
      header['Authorization'] = `Bearer ${token}`
    }

    // 发起请求
    wx.request({
      url: config.baseURL + options.url,
      method: options.method || 'GET',
      data: options.data || {},
      header: header,
      timeout: options.timeout || config.timeout,
      success(res) {
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
              raw: responseData
            }))
          }
        } else {
          // HTTP 错误
          reject(createRequestError('网络请求失败，请稍后重试', {
            kind: 'http',
            statusCode: res.statusCode,
            raw: res
          }))
        }
      },
      fail(err) {
        reject(createRequestError('网络异常，请稍后重试', {
          kind: 'network',
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
