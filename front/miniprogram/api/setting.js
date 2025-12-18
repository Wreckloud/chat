/**
 * WolfChat 群设置相关API
 * @author Wreckloud
 * @date 2024-12-18
 */

const { get, post, put, del } = require('../utils/request.js');

/**
 * 发布群公告
 * @param {Number} groupId 群组ID
 * @param {Object} data { content, isPinned }
 */
const publishNotice = (groupId, data) => {
  return post(`/group/setting/${groupId}/notice`, data);
};

/**
 * 查询群公告列表
 * @param {Number} groupId 群组ID
 */
const getNotices = (groupId) => {
  return get(`/group/setting/${groupId}/notices`);
};

/**
 * 删除群公告
 * @param {Number} groupId 群组ID
 * @param {Number} noticeId 公告ID
 */
const deleteNotice = (groupId, noticeId) => {
  return del(`/group/setting/${groupId}/notice/${noticeId}`);
};

/**
 * 全员禁言/解除禁言
 * @param {Number} groupId 群组ID
 * @param {Boolean} isMuted 是否禁言
 */
const muteAll = (groupId, isMuted) => {
  // ⚠️ 注意：后端使用 @RequestParam，需要使用查询参数
  return put(`/group/setting/${groupId}/mute-all?isMuted=${isMuted}`);
};

/**
 * 禁言/解禁单个成员
 * @param {Number} groupId 群组ID
 * @param {Number} userId 用户ID
 * @param {Object} data { isMuted, muteDuration }
 */
const muteMember = (groupId, userId, data) => {
  return put(`/group/setting/${groupId}/member/${userId}/mute`, data);
};

/**
 * 转让群主
 * @param {Number} groupId 群组ID
 * @param {Number} newOwnerId 新群主ID
 */
const transferOwner = (groupId, newOwnerId) => {
  return post(`/group/setting/${groupId}/transfer`, { newOwnerId });
};

module.exports = {
  publishNotice,
  getNotices,
  deleteNotice,
  muteAll,
  muteMember,
  transferOwner
};

