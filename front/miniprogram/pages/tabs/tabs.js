/**
 * Tab主页
 * @author Wreckloud
 * @date 2024-12-18
 */

const groupApi = require('../../api/group.js');
const auth = require('../../utils/auth.js');

Page({
  data: {
    active: 1, // 默认显示群聊Tab
    groups: [],
    userInfo: {}
  },

  /**
   * 页面加载
   */
  onLoad() {
    // 检查登录状态
    if (!auth.checkLogin()) {
      return;
    }

    // 加载用户信息
    this.loadUserInfo();
  },

  /**
   * 页面显示
   */
  onShow() {
    // 每次显示都刷新群组列表
    if (auth.isLogin()) {
      this.loadGroupList();
    }
  },

  /**
   * 下拉刷新
   */
  onPullDownRefresh() {
    this.loadGroupList();
    wx.stopPullDownRefresh();
  },

  /**
   * Tab切换
   */
  onChange(e) {
    this.setData({
      active: e.detail.index
    });
  },

  /**
   * 加载用户信息
   */
  loadUserInfo() {
    const userInfo = auth.getUser();
    this.setData({ userInfo });
  },

  /**
   * 加载群组列表
   */
  loadGroupList() {
    groupApi.getMyGroups()
      .then(groups => {
        this.setData({ groups });
      })
      .catch(err => {
        console.error('加载群组列表失败:', err);
      });
  },

  /**
   * 跳转到群组详情
   */
  gotoGroupDetail(e) {
    const groupId = e.currentTarget.dataset.id;
    wx.navigateTo({
      url: `/pages/group-detail/group-detail?groupId=${groupId}`
    });
  },

  /**
   * 跳转到创建群组
   */
  gotoCreateGroup() {
    wx.navigateTo({
      url: '/pages/group-create/group-create'
    });
  },

  /**
   * 退出登录
   */
  logout() {
    wx.showModal({
      title: '提示',
      content: '确定要退出登录吗？',
      success: (res) => {
        if (res.confirm) {
          auth.logout();
        }
      }
    });
  }
});

