/**
 * WolfChat 账号相关API
 * @author Wreckloud
 * @date 2024-12-18
 */

const { get, post } = require('../utils/request.js');

/**
 * 发送短信验证码
 * @param {String} mobile 手机号
 */
const sendSmsCode = (mobile) => {
  // ⚠️ 注意：后端使用 @RequestParam，需要使用查询参数
  return post(`/account/sms/send?mobile=${mobile}`);
};

/**
 * 手机号验证码登录
 * @param {Object} data { mobile, smsCodeKey, smsCode }
 */
const loginByMobile = (data) => {
  return post('/account/login/mobile', data);
};

/**
 * 手机号注册
 * @param {Object} data { mobile, password, smsCodeKey, smsCode, username }
 */
const registerByMobile = (data) => {
  return post('/account/register/mobile', data);
};

/**
 * 获取用户信息
 * @param {Number} userId 用户ID
 */
const getUserInfo = (userId) => {
  return get(`/account/user/${userId}`);
};

module.exports = {
  sendSmsCode,
  loginByMobile,
  registerByMobile,
  getUserInfo
};

