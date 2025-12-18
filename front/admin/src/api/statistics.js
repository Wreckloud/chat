/**
 * WolfChat 统计相关API
 * @author Wreckloud
 * @date 2024-12-18
 */

import request from '@/utils/request'

/**
 * 获取系统统计数据（所有统计数据）
 * 返回数据包括：
 * - totalUsers: 总用户数
 * - todayNewUsers: 今日新增用户数
 * - activeUsers: 活跃用户数（7天内登录）
 * - disabledUsers: 禁用用户数
 * - totalGroups: 总群组数
 * - todayNewGroups: 今日新增群组数
 * - activeGroups: 活跃群组数（7天内有消息）
 * - disbandedGroups: 已解散群组数
 */
export const getSystemStats = () => {
  return request({
    url: '/admin/statistics',
    method: 'GET'
  })
}

