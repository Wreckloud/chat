/**
 * WolfChat 统计相关API
 * @author Wreckloud
 * @date 2024-12-18
 */

import request from '@/utils/request'

/**
 * 获取系统统计数据
 * 注意：此接口暂未实现，会返回404
 */
export const getSystemStats = () => {
  return request({
    url: '/admin/statistics',
    method: 'GET'
  })
}

/**
 * 获取用户统计
 * 注意：此接口暂未实现，会返回404
 */
export const getUserStats = () => {
  return request({
    url: '/admin/statistics/users',
    method: 'GET'
  })
}

/**
 * 获取群组统计
 * 注意：此接口暂未实现，会返回404
 */
export const getGroupStats = () => {
  return request({
    url: '/admin/statistics/groups',
    method: 'GET'
  })
}

