/**
 * WolfChat 用户管理API
 * @author Wreckloud
 * @date 2024-12-18
 */

import request from '@/utils/request'

/**
 * 获取用户列表（分页）
 * 注意：此接口为管理员专用，暂未实现，会返回404
 */
export const getUserList = (params) => {
  return request({
    url: '/admin/users',
    method: 'GET',
    params
  })
}

/**
 * 根据ID查询用户（测试接口，不需要登录）
 */
export const getUserById = (userId) => {
  return request({
    url: `/account/user/${userId}`,
    method: 'GET'
  })
}

/**
 * 获取用户详情
 * 注意：此接口为管理员专用，暂未实现，会返回404
 */
export const getUserDetail = (userId) => {
  return request({
    url: `/admin/users/${userId}`,
    method: 'GET'
  })
}

/**
 * 禁用用户
 * 注意：此接口为管理员专用，暂未实现，会返回404
 */
export const disableUser = (userId) => {
  return request({
    url: `/admin/users/${userId}/disable`,
    method: 'PUT'
  })
}

/**
 * 启用用户
 * 注意：此接口为管理员专用，暂未实现，会返回404
 */
export const enableUser = (userId) => {
  return request({
    url: `/admin/users/${userId}/enable`,
    method: 'PUT'
  })
}

