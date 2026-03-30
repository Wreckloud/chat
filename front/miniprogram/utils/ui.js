/**
 * 统一 UI 提示
 */
const LOGIN_ERROR_CODES = new Set([2004, 2005, 2006, 2011, 2012])
const SYSTEM_ERROR_CODES = new Set([1002, 1003])
const ACTIONABLE_BUSINESS_MESSAGE_BY_CODE = {
  1001: '请求参数有误，请检查后重试',
  2001: '登录状态已失效，请重新登录',
  2002: '登录状态已失效，请重新登录',
  2003: '登录状态已失效，请重新登录',
  2007: '该用户已不可用',
  2008: '原密码错误',
  2009: '两次输入的新密码不一致',
  2010: '邮箱已被占用',
  2011: '该账号未绑定邮箱',
  2012: '该邮箱尚未认证',
  2014: '邮件发送失败，请稍后重试',
  2015: '发送过于频繁，请稍后再试',
  2016: '今日发送次数已达上限',
  2017: '认证链接无效或已过期',
  2018: '当前暂不支持换绑邮箱',
  2019: '重置链接无效或已过期',
  2020: '当前账号无权限执行此操作',
  2022: '验证码错误或已过期，请重新获取',
  2023: '验证码错误次数过多，请重新获取',
  3001: '号码分配失败，请稍后重试',
  3002: '号码资源不足，请稍后重试',
  3003: '用户不存在或已不可用',
  3004: '不能关注自己',
  3005: '已经关注过了',
  3006: '关注关系不存在',
  3010: '会话不存在',
  3011: '无权访问该会话',
  3012: '消息内容不能为空',
  3013: '暂不支持该文件类型',
  3014: '媒体文件超过上传限制，请压缩后重试',
  3015: '文件服务暂不可用，请稍后重试',
  3016: '主题不存在或已删除',
  3017: '主题已锁定，暂不可回复',
  3018: '引用的楼层不存在',
  3020: '当前操作无权限',
  3023: '通知不存在或已处理',
  3024: '未互关可发送条数已达上限，请等待对方回复'
}
const LOGIN_ERROR_MESSAGE = '账号或密码错误'
const NETWORK_ERROR_MESSAGE = '网络异常，请稍后重试'
const SYSTEM_ERROR_MESSAGE = '系统繁忙，请稍后重试'

function normalizeErrorCode(error) {
  if (!error || error.code == null) {
    return 0
  }
  const code = Number(error.code)
  return Number.isFinite(code) ? code : 0
}

function resolveErrorMessage(error, fallback, options = {}) {
  const scene = String(options.scene || '').trim().toLowerCase()
  if (scene === 'login') {
    return LOGIN_ERROR_MESSAGE
  }

  const code = normalizeErrorCode(error)
  if (LOGIN_ERROR_CODES.has(code)) {
    return LOGIN_ERROR_MESSAGE
  }
  if (code === 3014) {
    if (error && typeof error.message === 'string' && error.message.trim()) {
      return error.message.trim()
    }
    return ACTIONABLE_BUSINESS_MESSAGE_BY_CODE[3014]
  }
  if (code > 0 && Object.prototype.hasOwnProperty.call(ACTIONABLE_BUSINESS_MESSAGE_BY_CODE, code)) {
    return ACTIONABLE_BUSINESS_MESSAGE_BY_CODE[code]
  }
  if (error && (error.kind === 'network' || error.kind === 'http')) {
    return NETWORK_ERROR_MESSAGE
  }
  if (SYSTEM_ERROR_CODES.has(code)) {
    return SYSTEM_ERROR_MESSAGE
  }
  // 后端业务错误未进入可展示白名单时，不向用户透传后端原文。
  if (error && error.kind === 'business') {
    return fallback
  }

  if (typeof error === 'string' && error.trim()) {
    return error
  }
  if (error && error.expose === true && typeof error.message === 'string' && error.message.trim()) {
    return error.message
  }
  return fallback
}

function toastError(error, fallback = '操作失败', options = {}) {
  const message = resolveErrorMessage(error, fallback, options)
  if (!message) {
    return
  }
  wx.showToast({
    title: message,
    icon: 'none'
  })
}

function toastSuccess(message = '操作成功', options = {}) {
  if (options.silent) {
    return
  }
  wx.showToast({
    title: message,
    icon: 'success'
  })
}

function confirmAction(options = {}) {
  return new Promise(resolve => {
    wx.showModal({
      ...options,
      success(res) {
        resolve(!!(res && res.confirm))
      },
      fail() {
        resolve(false)
      }
    })
  })
}

module.exports = {
  toastError,
  toastSuccess,
  confirmAction
}
