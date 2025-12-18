/**
 * 群组详情页面
 * @author Wreckloud
 * @date 2024-12-18
 */

const groupApi = require('../../api/group.js');
const memberApi = require('../../api/member.js');
const settingApi = require('../../api/setting.js');
const auth = require('../../utils/auth.js');
const Toast = require('@vant/weapp/toast/toast');

Page({
  data: {
    groupId: null,
    group: {},
    members: [],
    myRole: '',
    
    // 弹窗状态
    showInvite: false,
    showNickname: false,
    showSetting: false,
    showMemberAction: false,
    showPublishNotice: false,
    
    // 输入值
    inviteInput: '',
    nicknameInput: '',
    noticeContent: '',
    isNoticePinned: false,
    
    // 公告
    latestNotice: '',
    
    // 成员操作
    selectedMember: null,
    memberActions: [],
    
    // 群设置操作
    settingActions: []
  },

  /**
   * 页面加载
   */
  onLoad(options) {
    const groupId = options.groupId;
    if (!groupId) {
      wx.showToast({
        title: '参数错误',
        icon: 'none'
      });
      return;
    }

    this.setData({ groupId: Number(groupId) });
    this.loadGroupDetail();
  },

  /**
   * 加载群组详情
   */
  loadGroupDetail() {
    const { groupId } = this.data;
    
    groupApi.getGroupDetail(groupId)
      .then(group => {
        // 获取当前用户角色
        const myUserId = auth.getUserId();
        const myMember = group.members.find(m => m.userId === myUserId);
        const myRole = myMember ? myMember.role : 'MEMBER';

        // 按角色排序成员
        const sortedMembers = this.sortMembers(group.members);

        // 构建设置操作列表
        const settingActions = this.buildSettingActions(myRole);

        this.setData({
          group,
          members: sortedMembers,
          myRole,
          settingActions
        });

        // 加载最新公告
        this.loadLatestNotice();
      })
      .catch(err => {
        console.error('加载群组详情失败:', err);
      });
  },

  /**
   * 按角色排序成员
   */
  sortMembers(members) {
    const roleOrder = { 'OWNER': 0, 'ADMIN': 1, 'MEMBER': 2 };
    return members.sort((a, b) => roleOrder[a.role] - roleOrder[b.role]);
  },

  /**
   * 构建设置操作列表
   */
  buildSettingActions(myRole) {
    const actions = [];
    
    if (myRole === 'OWNER' || myRole === 'ADMIN') {
      actions.push({ name: '发布公告', value: 'publish_notice' });
      actions.push({ name: '修改群信息', value: 'edit_group' });
    }
    
    if (myRole === 'OWNER') {
      actions.push({ name: '全员禁言', value: 'mute_all' });
      actions.push({ name: '转让群主', value: 'transfer' });
    }
    
    return actions;
  },

  /**
   * 加载最新公告
   */
  loadLatestNotice() {
    const { groupId } = this.data;
    
    settingApi.getNotices(groupId)
      .then(notices => {
        if (notices && notices.length > 0) {
          const latest = notices[0];
          this.setData({
            latestNotice: latest.content.substring(0, 20) + '...'
          });
        }
      })
      .catch(err => {
        console.error('加载公告失败:', err);
      });
  },

  /**
   * 显示邀请对话框
   */
  showInviteDialog() {
    this.setData({
      showInvite: true,
      inviteInput: ''
    });
  },

  /**
   * 关闭邀请对话框
   */
  closeInviteDialog() {
    this.setData({
      showInvite: false
    });
  },

  /**
   * 邀请输入变化
   */
  onInviteInputChange(e) {
    this.setData({
      inviteInput: e.detail
    });
  },

  /**
   * 邀请成员
   */
  inviteMembers() {
    const { groupId, inviteInput } = this.data;
    
    if (!inviteInput) {
      Toast.fail('请输入WF号');
      return;
    }

    const userIds = [Number(inviteInput)];
    
    memberApi.inviteMembers(groupId, userIds)
      .then(() => {
        Toast.success('邀请成功');
        this.closeInviteDialog();
        // 刷新列表
        setTimeout(() => {
          this.loadGroupDetail();
        }, 1000);
      })
      .catch(err => {
        console.error('邀请成员失败:', err);
      });
  },

  /**
   * 显示修改昵称对话框
   */
  showNicknameDialog() {
    this.setData({
      showNickname: true,
      nicknameInput: ''
    });
  },

  /**
   * 关闭昵称对话框
   */
  closeNicknameDialog() {
    this.setData({
      showNickname: false
    });
  },

  /**
   * 昵称输入变化
   */
  onNicknameInputChange(e) {
    this.setData({
      nicknameInput: e.detail
    });
  },

  /**
   * 修改群昵称
   */
  updateNickname() {
    const { groupId, nicknameInput } = this.data;
    
    if (!nicknameInput) {
      Toast.fail('请输入群昵称');
      return;
    }

    memberApi.updateNickname(groupId, nicknameInput)
      .then(() => {
        Toast.success('修改成功');
        this.closeNicknameDialog();
        this.loadGroupDetail();
      })
      .catch(err => {
        console.error('修改昵称失败:', err);
      });
  },

  /**
   * 显示群公告列表
   */
  showNoticeList() {
    const { groupId } = this.data;
    wx.navigateTo({
      url: `/pages/notice-list/notice-list?groupId=${groupId}`
    });
  },

  /**
   * 显示群设置菜单
   */
  showGroupSetting() {
    this.setData({
      showSetting: true
    });
  },

  /**
   * 关闭群设置菜单
   */
  closeGroupSetting() {
    this.setData({
      showSetting: false
    });
  },

  /**
   * 群设置选择
   */
  onSettingSelect(e) {
    const value = e.detail.value;
    
    switch (value) {
      case 'publish_notice':
        this.showPublishNoticeDialog();
        break;
      case 'edit_group':
        this.editGroup();
        break;
      case 'mute_all':
        this.toggleMuteAll();
        break;
      case 'transfer':
        this.showTransferDialog();
        break;
    }
  },

  /**
   * 显示发布公告对话框
   */
  showPublishNoticeDialog() {
    this.setData({
      showPublishNotice: true,
      noticeContent: '',
      isNoticePinned: false
    });
  },

  /**
   * 关闭发布公告对话框
   */
  closePublishNoticeDialog() {
    this.setData({
      showPublishNotice: false
    });
  },

  /**
   * 公告内容变化
   */
  onNoticeContentChange(e) {
    this.setData({
      noticeContent: e.detail
    });
  },

  /**
   * 置顶开关变化
   */
  onNoticePinnedChange(e) {
    this.setData({
      isNoticePinned: e.detail
    });
  },

  /**
   * 发布公告
   */
  publishNotice() {
    const { groupId, noticeContent, isNoticePinned } = this.data;
    
    if (!noticeContent) {
      Toast.fail('请输入公告内容');
      return;
    }

    settingApi.publishNotice(groupId, {
      content: noticeContent,
      isPinned: isNoticePinned
    })
      .then(() => {
        Toast.success('发布成功');
        this.closePublishNoticeDialog();
        this.loadLatestNotice();
      })
      .catch(err => {
        console.error('发布公告失败:', err);
      });
  },

  /**
   * 修改群信息
   */
  editGroup() {
    wx.showModal({
      title: '提示',
      content: '此功能开发中...',
      showCancel: false
    });
  },

  /**
   * 全员禁言切换
   */
  toggleMuteAll() {
    const { groupId, group } = this.data;
    const isMuted = !group.isAllMuted;
    
    wx.showModal({
      title: '确认',
      content: `确定要${isMuted ? '开启' : '关闭'}全员禁言吗？`,
      success: (res) => {
        if (res.confirm) {
          settingApi.muteAll(groupId, isMuted)
            .then(() => {
              Toast.success(isMuted ? '已开启全员禁言' : '已关闭全员禁言');
              this.loadGroupDetail();
            })
            .catch(err => {
              console.error('设置全员禁言失败:', err);
            });
        }
      }
    });
  },

  /**
   * 显示转让对话框
   */
  showTransferDialog() {
    wx.showModal({
      title: '提示',
      content: '转让功能开发中...',
      showCancel: false
    });
  },

  /**
   * 长按成员显示操作菜单
   */
  showMemberActions(e) {
    const member = e.currentTarget.dataset.member;
    const { myRole } = this.data;
    
    // 不能操作自己
    const myUserId = auth.getUserId();
    if (member.userId === myUserId) {
      return;
    }

    // 构建操作列表
    const actions = this.buildMemberActions(myRole, member.role);
    
    if (actions.length === 0) {
      return;
    }

    this.setData({
      selectedMember: member,
      memberActions: actions,
      showMemberAction: true
    });
  },

  /**
   * 构建成员操作列表
   */
  buildMemberActions(myRole, targetRole) {
    const actions = [];
    
    // 群主可以操作所有人
    if (myRole === 'OWNER') {
      if (targetRole !== 'OWNER') {
        actions.push({ name: '踢出群组', value: 'kick', color: '#ee0a24' });
        
        if (targetRole === 'ADMIN') {
          actions.push({ name: '取消管理员', value: 'unset_admin' });
        } else {
          actions.push({ name: '设为管理员', value: 'set_admin' });
        }
        
        actions.push({ name: '禁言', value: 'mute' });
      }
    }
    // 管理员只能操作普通成员
    else if (myRole === 'ADMIN') {
      if (targetRole === 'MEMBER') {
        actions.push({ name: '踢出群组', value: 'kick', color: '#ee0a24' });
        actions.push({ name: '禁言', value: 'mute' });
      }
    }
    
    return actions;
  },

  /**
   * 关闭成员操作菜单
   */
  closeMemberActions() {
    this.setData({
      showMemberAction: false
    });
  },

  /**
   * 成员操作选择
   */
  onMemberActionSelect(e) {
    const value = e.detail.value;
    const { selectedMember } = this.data;
    
    switch (value) {
      case 'kick':
        this.kickMember(selectedMember.userId);
        break;
      case 'set_admin':
        this.setAdmin(selectedMember.userId, true);
        break;
      case 'unset_admin':
        this.setAdmin(selectedMember.userId, false);
        break;
      case 'mute':
        this.muteMember(selectedMember.userId);
        break;
    }
  },

  /**
   * 踢出成员
   */
  kickMember(userId) {
    const { groupId } = this.data;
    
    wx.showModal({
      title: '确认',
      content: '确定要踢出该成员吗？',
      success: (res) => {
        if (res.confirm) {
          memberApi.kickMember(groupId, userId)
            .then(() => {
              Toast.success('已踢出');
              this.loadGroupDetail();
            })
            .catch(err => {
              console.error('踢出成员失败:', err);
            });
        }
      }
    });
  },

  /**
   * 设置/取消管理员
   */
  setAdmin(userId, isAdmin) {
    const { groupId } = this.data;
    
    memberApi.setAdmin(groupId, userId, isAdmin)
      .then(() => {
        Toast.success(isAdmin ? '已设为管理员' : '已取消管理员');
        this.loadGroupDetail();
      })
      .catch(err => {
        console.error('设置管理员失败:', err);
      });
  },

  /**
   * 禁言成员
   */
  muteMember(userId) {
    const { groupId } = this.data;
    
    settingApi.muteMember(groupId, userId, {
      isMuted: true,
      muteDuration: 3600 // 禁言1小时
    })
      .then(() => {
        Toast.success('已禁言');
      })
      .catch(err => {
        console.error('禁言失败:', err);
      });
  },

  /**
   * 退出群组
   */
  quitGroup() {
    const { groupId } = this.data;
    
    wx.showModal({
      title: '确认',
      content: '确定要退出群组吗？',
      success: (res) => {
        if (res.confirm) {
          memberApi.quitGroup(groupId)
            .then(() => {
              Toast.success('已退出');
              setTimeout(() => {
                wx.navigateBack();
              }, 1000);
            })
            .catch(err => {
              console.error('退出群组失败:', err);
            });
        }
      }
    });
  },

  /**
   * 解散群组
   */
  disbandGroup() {
    const { groupId } = this.data;
    
    wx.showModal({
      title: '确认',
      content: '确定要解散群组吗？解散后将无法恢复！',
      confirmText: '确定解散',
      confirmColor: '#ee0a24',
      success: (res) => {
        if (res.confirm) {
          groupApi.disbandGroup(groupId)
            .then(() => {
              Toast.success('已解散');
              setTimeout(() => {
                wx.navigateBack();
              }, 1000);
            })
            .catch(err => {
              console.error('解散群组失败:', err);
            });
        }
      }
    });
  }
});

