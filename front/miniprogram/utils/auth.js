/**
 * WolfChat 认证工具
 * @author Wreckloud
 * @date 2024-12-18
 */

const config = require('./config.js');

const auth = {
  /**
   * 保存Token
   */
  setToken(token) {
    wx.setStorageSync(config.tokenKey, token);
  },

  /**
   * 获取Token
   */
  getToken() {
    return wx.getStorageSync(config.tokenKey) || '';
  },

  /**
   * 清除Token
   */
  clearToken() {
    wx.removeStorageSync(config.tokenKey);
  },

  /**
   * 保存用户信息
   */
  setUser(user) {
    wx.setStorageSync(config.userKey, user);
  },

  /**
   * 获取用户信息
   */
  getUser() {
    return wx.getStorageSync(config.userKey) || null;
  },

  /**
   * 获取用户ID
   */
  getUserId() {
    const user = this.getUser();
    return user ? user.userId : null;
  },

  /**
   * 清除用户信息
   */
  clearUser() {
    wx.removeStorageSync(config.userKey);
  },

  /**
   * 检查是否已登录
   */
  isLogin() {
    return !!this.getToken();
  },

  /**
   * 登出
   */
  logout() {
    this.clearToken();
    this.clearUser();
    wx.reLaunch({
      url: '/pages/login/login'
    });
  },

  /**
   * 检查登录状态，未登录则跳转登录页
   */
  checkLogin() {
    if (!this.isLogin()) {
      wx.showToast({
        title: '请先登录',
        icon: 'none',
        duration: 2000
      });
      setTimeout(() => {
        wx.reLaunch({
          url: '/pages/login/login'
        });
      }, 2000);
      return false;
    }
    return true;
  }
};

module.exports = auth;

