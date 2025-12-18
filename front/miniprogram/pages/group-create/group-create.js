/**
 * 创建群组页面
 * @author Wreckloud
 * @date 2024-12-18
 */

const groupApi = require('../../api/group.js');
const logger = require('../../utils/logger.js');

Page({
  data: {
    groupName: '',
    groupIntro: '',
    memberInput: '',
    memberIds: [],
    createLoading: false
  },

  /**
   * 群名称输入
   */
  onGroupNameChange(e) {
    this.setData({
      groupName: e.detail
    });
  },

  /**
   * 群简介输入
   */
  onGroupIntroChange(e) {
    this.setData({
      groupIntro: e.detail
    });
  },

  /**
   * 成员输入
   */
  onMemberInputChange(e) {
    this.setData({
      memberInput: e.detail
    });
  },

  /**
   * 添加成员
   */
  addMember() {
    const { memberInput, memberIds } = this.data;

    if (!memberInput) {
      wx.showToast({
        title: '请输入WF号',
        icon: 'none'
      });
      return;
    }

    // 检查是否已添加
    if (memberIds.includes(Number(memberInput))) {
      wx.showToast({
        title: '该成员已添加',
        icon: 'none'
      });
      return;
    }

    // 添加到列表
    memberIds.push(Number(memberInput));
    this.setData({
      memberIds,
      memberInput: ''
    });
  },

  /**
   * 移除成员
   */
  removeMember(e) {
    const id = e.currentTarget.dataset.id;
    const memberIds = this.data.memberIds.filter(item => item !== id);
    this.setData({ memberIds });
  },

  /**
   * 创建群组
   */
  createGroup() {
    const { groupName, groupIntro, memberIds } = this.data;

    logger.action('GroupCreatePage', 'createGroup', { groupName, memberCount: memberIds.length });

    // 验证
    if (!groupName) {
      logger.warn('GroupCreatePage', '群名称为空');
      wx.showToast({
        title: '请输入群名称',
        icon: 'none'
      });
      return;
    }

    if (groupName.length < 2) {
      logger.warn('GroupCreatePage', '群名称长度不足', { length: groupName.length });
      wx.showToast({
        title: '群名称至少2个字符',
        icon: 'none'
      });
      return;
    }

    // 显示加载
    this.setData({ createLoading: true });

    const requestData = {
      groupName,
      groupIntro: groupIntro || null,
      memberIds: memberIds.length > 0 ? memberIds : null
    };

    logger.info('GroupCreatePage', '开始创建群组', requestData);

    // 调用创建接口
    groupApi.createGroup(requestData)
      .then(group => {
        logger.info('GroupCreatePage', '创建群组成功', { groupId: group.groupId });
        
        wx.showToast({
          title: '创建成功',
          icon: 'success',
          duration: 1500
        });

        // 跳转到群组详情
        setTimeout(() => {
          wx.redirectTo({
            url: `/pages/group-detail/group-detail?groupId=${group.groupId}`
          });
        }, 1500);
      })
      .catch(err => {
        logger.error('GroupCreatePage', '创建群组失败', err);
        this.setData({ createLoading: false });
        
        // 显示错误信息（如果后端返回了message）
        if (err && err.message) {
          wx.showToast({
            title: err.message,
            icon: 'none',
            duration: 3000
          });
        }
      });
  }
});

