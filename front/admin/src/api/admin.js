/**
 * WolfChat 管理员API
 * @author Wreckloud
 * @date 2024-12-18
 */

import request from '@/utils/request'

/**
 * 获取群组列表（分页） - 管理员专用
 * @param {Object} params - 查询参数
 * @param {Number} params.current - 当前页
 * @param {Number} params.size - 每页大小
 * @param {String} params.keyword - 搜索关键词（群名称）
 * @param {Number} params.status - 状态筛选：1正常 2已解散
 */
export const getAdminGroupList = (params) => {
  return request({
    url: '/admin/groups/list',
    method: 'GET',
    params
  })
}

/**
 * 获取群组详情 - 管理员专用
 * @param {Number} groupId - 群组ID
 */
export const getAdminGroupDetail = (groupId) => {
  return request({
    url: `/admin/groups/${groupId}`,
    method: 'GET'
  })
}

/**
 * 强制解散群组 - 管理员专用
 * @param {Object} data - 请求数据
 * @param {Number} data.groupId - 群组ID
 * @param {String} data.reason - 解散原因
 */
export const adminDismissGroup = (data) => {
  return request({
    url: '/admin/groups/dismiss',
    method: 'DELETE',
    data
  })
}

/**
 * 获取系统统计数据
 */
export const getStatistics = () => {
  return request({
    url: '/admin/statistics',
    method: 'GET'
  })
}

/**
 * 获取操作日志列表（分页）
 * @param {Object} params - 查询参数
 * @param {Number} params.current - 当前页
 * @param {Number} params.size - 每页大小
 * @param {Number} params.adminId - 管理员ID筛选
 * @param {String} params.action - 操作类型筛选
 */
export const getAdminLogs = (params) => {
  return request({
    url: '/admin/logs/list',
    method: 'GET',
    params
  })
}

