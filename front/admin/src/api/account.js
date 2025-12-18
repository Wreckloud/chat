/**
 * WolfChat 账户相关API
 * @author Wreckloud
 * @date 2024-12-18
 */

import request from '@/utils/request'

/**
 * 手机号登录
 */
export const loginByMobile = (data) => {
  return request({
    url: '/account/login/mobile',
    method: 'POST',
    data
  })
}

/**
 * 发送短信验证码
 */
export const sendSmsCode = (mobile) => {
  return request({
    url: `/account/sms/send?mobile=${mobile}`,
    method: 'POST'
  })
}

/**
 * 获取当前用户信息
 */
export const getCurrentUser = () => {
  return request({
    url: '/account/current',
    method: 'GET'
  })
}

