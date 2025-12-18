/**
 * WolfChat 群成员相关API
 * @author Wreckloud
 * @date 2024-12-18
 */

const { get, post, put, del } = require('../utils/request.js');

/**
 * 邀请成员
 * @param {Number} groupId 群组ID
 * @param {Array} userIds 用户ID列表
 */
const inviteMembers = (groupId, userIds) => {
  return post(`/group/member/${groupId}/invite`, { userIds });
};

/**
 * 踢出成员
 * @param {Number} groupId 群组ID
 * @param {Number} userId 用户ID
 */
const kickMember = (groupId, userId) => {
  return del(`/group/member/${groupId}/member/${userId}`);
};

/**
 * 退出群组
 * @param {Number} groupId 群组ID
 */
const quitGroup = (groupId) => {
  return post(`/group/member/${groupId}/quit`);
};

/**
 * 设置/取消管理员
 * @param {Number} groupId 群组ID
 * @param {Number} userId 用户ID
 * @param {Boolean} isAdmin 是否设置为管理员
 */
const setAdmin = (groupId, userId, isAdmin) => {
  return post(`/group/member/${groupId}/set-admin`, { userId, isAdmin });
};

/**
 * 查询成员列表
 * @param {Number} groupId 群组ID
 */
const getMembers = (groupId) => {
  return get(`/group/member/${groupId}/members`);
};

/**
 * 修改群昵称
 * @param {Number} groupId 群组ID
 * @param {String} nickname 新昵称
 */
const updateNickname = (groupId, nickname) => {
  return put(`/group/member/${groupId}/nickname?nickname=${encodeURIComponent(nickname)}`);
};

module.exports = {
  inviteMembers,
  kickMember,
  quitGroup,
  setAdmin,
  getMembers,
  updateNickname
};

