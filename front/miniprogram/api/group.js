/**
 * WolfChat 群组相关API
 * @author Wreckloud
 * @date 2024-12-18
 */

const { get, post, put, del } = require('../utils/request.js');

/**
 * 创建群组
 * @param {Object} data { groupName, groupIntro, memberIds }
 */
const createGroup = (data) => {
  return post('/group/create', data);
};

/**
 * 查询群组详情
 * @param {Number} groupId 群组ID
 */
const getGroupDetail = (groupId) => {
  return get(`/group/${groupId}`);
};

/**
 * 查询我的群组列表
 */
const getMyGroups = () => {
  return get('/group/my-groups');
};

/**
 * 修改群信息
 * @param {Number} groupId 群组ID
 * @param {Object} data { groupName, groupAvatar, groupIntro }
 */
const updateGroup = (groupId, data) => {
  return put(`/group/${groupId}`, data);
};

/**
 * 解散群组
 * @param {Number} groupId 群组ID
 */
const disbandGroup = (groupId) => {
  return del(`/group/${groupId}`);
};

module.exports = {
  createGroup,
  getGroupDetail,
  getMyGroups,
  updateGroup,
  disbandGroup
};

