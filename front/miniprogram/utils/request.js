/**
 * WolfChat 网络请求封装
 * @author Wreckloud
 * @date 2024-12-18
 */

const config = require('./config.js');
const auth = require('./auth.js');
const logger = require('./logger.js');

/**
 * 网络请求封装
 */
const request = (options) => {
  return new Promise((resolve, reject) => {
    // 显示加载提示
    if (options.showLoading !== false) {
      wx.showLoading({
        title: options.loadingText || '加载中...',
        mask: true
      });
    }

    // 获取Token
    const token = auth.getToken();

    // 构建请求头
    const header = {
      'Content-Type': 'application/json',
      ...options.header
    };

    // 如果有Token，添加到请求头
    if (token) {
      header['Authorization'] = `Bearer ${token}`;
    }

    // 记录请求日志
    const method = options.method || 'GET';
    const fullUrl = config.baseUrl + options.url;
    logger.request(method, fullUrl, options.data);

    // 发起请求
    wx.request({
      url: fullUrl,
      method: method,
      data: options.data || {},
      header: header,
      timeout: options.timeout || config.timeout,
      success: (res) => {
        // 隐藏加载提示
        wx.hideLoading();

        // 处理响应
        const data = res.data;

        // 记录响应日志
        logger.response(method, fullUrl, data.code, data);

        // 判断业务状态码
        if (data.code === 0) {
          // 成功
          resolve(data.data);
        } else if (data.code === -1001 || data.code === -1002 || data.code === -1003) {
          // Token相关错误，需要重新登录
          logger.warn('Request', 'Token过期，需要重新登录', data);
          wx.showToast({
            title: '登录已过期',
            icon: 'none',
            duration: 2000
          });
          auth.logout();
          reject(data);
        } else {
          // 业务错误
          logger.error('Request', '业务错误', data);
          if (options.showError !== false) {
            wx.showToast({
              title: data.message || '请求失败',
              icon: 'none',
              duration: 2000
            });
          }
          reject(data);
        }
      },
      fail: (err) => {
        // 隐藏加载提示
        wx.hideLoading();

        // 网络错误日志
        logger.error('Request', '网络请求失败', { url: fullUrl, error: err });
        
        if (options.showError !== false) {
          wx.showToast({
            title: '网络请求失败',
            icon: 'none',
            duration: 2000
          });
        }
        reject(err);
      }
    });
  });
};

/**
 * GET请求
 */
const get = (url, data, options = {}) => {
  return request({
    url,
    method: 'GET',
    data,
    ...options
  });
};

/**
 * POST请求
 */
const post = (url, data, options = {}) => {
  return request({
    url,
    method: 'POST',
    data,
    ...options
  });
};

/**
 * PUT请求
 */
const put = (url, data, options = {}) => {
  return request({
    url,
    method: 'PUT',
    data,
    ...options
  });
};

/**
 * DELETE请求
 */
const del = (url, data, options = {}) => {
  return request({
    url,
    method: 'DELETE',
    data,
    ...options
  });
};

module.exports = {
  request,
  get,
  post,
  put,
  del
};

