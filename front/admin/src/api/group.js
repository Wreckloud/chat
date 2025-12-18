/**
 * WolfChat 群组管理API
 * @author Wreckloud
 * @date 2024-12-18
 */

import request from '@/utils/request'

/**
 * 获取我的群组列表
 */
export const getMyGroups = () => {
  return request({
    url: '/group/my-groups',
    method: 'GET'
  })
}

/**
 * 创建群组
 */
export const createGroup = (data) => {
  return request({
    url: '/group',
    method: 'POST',
    data
  })
}

/**
 * 获取群组详情
 */
export const getGroupDetail = (groupId) => {
  return request({
    url: `/group/${groupId}`,
    method: 'GET'
  })
}

/**
 * 更新群组信息
 */
export const updateGroup = (groupId, data) => {
  return request({
    url: `/group/${groupId}`,
    method: 'PUT',
    data
  })
}

/**
 * 解散群组
 */
export const dismissGroup = (groupId) => {
  return request({
    url: `/group/${groupId}/dismiss`,
    method: 'DELETE'
  })
}


/**
 * 邀请成员
 */
export const inviteMembers = (groupId, data) => {
  return request({
    url: `/group/member/${groupId}/invite`,
    method: 'POST',
    data
  })
}

/**
 * 移除群成员
 */
export const removeGroupMember = (groupId, userId) => {
  return request({
    url: `/group/member/${groupId}/member/${userId}`,
    method: 'DELETE'
  })
}

/**
 * 退出群组
 */
export const leaveGroup = (groupId) => {
  return request({
    url: `/group/member/${groupId}/quit`,
    method: 'POST'
  })
}

/**
 * 设置/取消管理员
 */
export const setAdmin = (groupId, data) => {
  return request({
    url: `/group/member/${groupId}/set-admin`,
    method: 'POST',
    data
  })
}

/**
 * 更新群昵称
 * 注意：使用查询参数，不是RequestBody
 */
export const updateNickname = (groupId, nickname) => {
  return request({
    url: `/group/member/${groupId}/nickname?nickname=${encodeURIComponent(nickname)}`,
    method: 'PUT'
  })
}

/**
 * 获取成员列表
 */
export const getGroupMembers = (groupId) => {
  return request({
    url: `/group/member/${groupId}/members`,
    method: 'GET'
  })
}


/**
 * 发布群公告
 */
export const publishNotice = (groupId, data) => {
  return request({
    url: `/group/setting/${groupId}/notice`,
    method: 'POST',
    data
  })
}

/**
 * 查询群公告列表
 */
export const getGroupNotices = (groupId) => {
  return request({
    url: `/group/setting/${groupId}/notice`,
    method: 'GET'
  })
}

/**
 * 删除群公告
 */
export const deleteGroupNotice = (groupId, noticeId) => {
  return request({
    url: `/group/setting/${groupId}/notice/${noticeId}`,
    method: 'DELETE'
  })
}

/**
 * 全员禁言/解除
 */
export const muteAll = (groupId, isMuted) => {
  return request({
    url: `/group/setting/${groupId}/mute-all?isMuted=${isMuted}`,
    method: 'PUT'
  })
}

/**
 * 禁言/解禁成员
 */
export const muteMember = (groupId, userId, data) => {
  return request({
    url: `/group/setting/${groupId}/member/${userId}/mute`,
    method: 'PUT',
    data
  })
}

/**
 * 转让群主
 */
export const transferOwner = (groupId, data) => {
  return request({
    url: `/group/setting/${groupId}/transfer`,
    method: 'POST',
    data
  })
}

