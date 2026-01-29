/**
 * 请求封装（自动携带 token）
 */
const config = require('./config')
const auth = require('./auth')

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
          // 检查业务状态码
          if (res.data.code === 0) {
            resolve(res.data)
          } else {
            // 业务错误
            reject(new Error(res.data.message || '请求失败'))
          }
        } else {
          // HTTP 错误
          reject(new Error(`请求失败: ${res.statusCode}`))
        }
      },
      fail(err) {
        reject(err)
      }
    })
  })
}

module.exports = {
  request,
  get: (url, data, options = {}) => request({ ...options, url, method: 'GET', data }),
  post: (url, data, options = {}) => request({ ...options, url, method: 'POST', data })
}





