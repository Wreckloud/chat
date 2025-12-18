/**
 * WolfChat 日志工具
 * 参考后端日志风格，统一管理前端日志输出
 * @author Wreckloud
 * @date 2024-12-18
 */

const config = require('./config.js');

/**
 * 日志级别
 */
const LOG_LEVEL = {
  DEBUG: 0,
  INFO: 1,
  WARN: 2,
  ERROR: 3
};

/**
 * 当前日志级别（开发环境：DEBUG，生产环境：INFO）
 */
const CURRENT_LEVEL = LOG_LEVEL.DEBUG;

/**
 * 格式化时间
 */
function formatTime() {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  const hour = String(now.getHours()).padStart(2, '0');
  const minute = String(now.getMinutes()).padStart(2, '0');
  const second = String(now.getSeconds()).padStart(2, '0');
  const ms = String(now.getMilliseconds()).padStart(3, '0');
  
  return `${year}-${month}-${day} ${hour}:${minute}:${second}.${ms}`;
}

/**
 * 日志工具类
 */
const logger = {
  /**
   * DEBUG 日志
   * @param {String} tag 标签（类名或模块名）
   * @param {String} message 日志信息
   * @param  {...any} args 额外参数
   */
  debug(tag, message, ...args) {
    if (CURRENT_LEVEL <= LOG_LEVEL.DEBUG) {
      const time = formatTime();
      console.log(`[DEBUG] ${time} [${tag}] - ${message}`, ...args);
    }
  },

  /**
   * INFO 日志
   * @param {String} tag 标签（类名或模块名）
   * @param {String} message 日志信息
   * @param  {...any} args 额外参数
   */
  info(tag, message, ...args) {
    if (CURRENT_LEVEL <= LOG_LEVEL.INFO) {
      const time = formatTime();
      console.log(`[INFO ] ${time} [${tag}] - ${message}`, ...args);
    }
  },

  /**
   * WARN 日志
   * @param {String} tag 标签（类名或模块名）
   * @param {String} message 日志信息
   * @param  {...any} args 额外参数
   */
  warn(tag, message, ...args) {
    if (CURRENT_LEVEL <= LOG_LEVEL.WARN) {
      const time = formatTime();
      console.warn(`[WARN ] ${time} [${tag}] - ${message}`, ...args);
    }
  },

  /**
   * ERROR 日志
   * @param {String} tag 标签（类名或模块名）
   * @param {String} message 日志信息
   * @param  {...any} args 额外参数
   */
  error(tag, message, ...args) {
    if (CURRENT_LEVEL <= LOG_LEVEL.ERROR) {
      const time = formatTime();
      console.error(`[ERROR] ${time} [${tag}] - ${message}`, ...args);
    }
  },

  /**
   * 网络请求日志
   * @param {String} method 请求方法
   * @param {String} url 请求地址
   * @param {Object} data 请求数据
   */
  request(method, url, data) {
    const time = formatTime();
    console.log(`[HTTP ] ${time} [Request] - ${method} ${url}`, data || '');
  },

  /**
   * 网络响应日志
   * @param {String} method 请求方法
   * @param {String} url 请求地址
   * @param {Number} code 响应码
   * @param {Object} data 响应数据
   */
  response(method, url, code, data) {
    const time = formatTime();
    if (code === 0) {
      console.log(`[HTTP ] ${time} [Response] - ${method} ${url} - Success`, data || '');
    } else {
      console.warn(`[HTTP ] ${time} [Response] - ${method} ${url} - Error(${code})`, data || '');
    }
  },

  /**
   * 页面生命周期日志
   * @param {String} page 页面名称
   * @param {String} lifecycle 生命周期
   * @param {Object} options 参数
   */
  lifecycle(page, lifecycle, options) {
    const time = formatTime();
    console.log(`[PAGE ] ${time} [${page}] - ${lifecycle}`, options || '');
  },

  /**
   * 用户操作日志
   * @param {String} page 页面名称
   * @param {String} action 操作名称
   * @param {Object} data 操作数据
   */
  action(page, action, data) {
    const time = formatTime();
    console.log(`[ACTN ] ${time} [${page}] - ${action}`, data || '');
  }
};

module.exports = logger;

