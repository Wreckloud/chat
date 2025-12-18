/**
 * WolfChat 用户管理API
 * @author Wreckloud
 * @date 2024-12-18
 */

import request from '@/utils/request'

/**
 * 获取用户列表（分页）
 * @param {Object} params - 查询参数
 * @param {Number} params.current - 当前页
 * @param {Number} params.size - 每页大小
 * @param {String} params.keyword - 搜索关键词（用户名/手机号/WF号）
 * @param {Number} params.status - 状态筛选：1正常 2禁用 3注销
 */
export const getUserList = (params) => {
  return request({
    url: '/admin/users/list',
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
 * @param {Number} userId - 用户ID
 */
export const getUserDetail = (userId) => {
  return request({
    url: `/admin/users/${userId}`,
    method: 'GET'
  })
}

/**
 * 更新用户状态
 * @param {Object} data - 请求数据
 * @param {Number} data.userId - 用户ID
 * @param {Number} data.status - 状态：1正常 2禁用 3注销
 * @param {String} data.reason - 操作原因
 */
export const updateUserStatus = (data) => {
  return request({
    url: '/admin/users/status',
    method: 'PUT',
    data
  })
}

/**
 * 禁用用户（快捷方法）
 * @param {Number} userId - 用户ID
 * @param {String} reason - 禁用原因
 */
export const disableUser = (userId, reason = '') => {
  return updateUserStatus({ userId, status: 2, reason })
}

/**
 * 启用用户（快捷方法）
 * @param {Number} userId - 用户ID
 * @param {String} reason - 启用原因
 */
export const enableUser = (userId, reason = '') => {
  return updateUserStatus({ userId, status: 1, reason })
}

