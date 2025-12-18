/**
 * WolfChat 小程序入口文件
 * @author Wreckloud
 * @date 2024-12-18
 */

const auth = require('./utils/auth.js');
const logger = require('./utils/logger.js');

App({
  /**
   * 小程序初始化
   */
  onLaunch(options) {
    logger.info('Application', 'WolfChat 小程序启动 🐺', options);
    
    // 检查登录状态
    const isLogin = auth.isLogin();
    logger.info('Application', `登录状态: ${isLogin}`);
    
    // 获取系统信息（使用新API）
    try {
      const systemInfo = wx.getSystemInfoSync();
      this.globalData.systemInfo = systemInfo;
      logger.debug('Application', '系统信息获取成功', {
        platform: systemInfo.platform,
        system: systemInfo.system,
        version: systemInfo.version
      });
    } catch (e) {
      logger.error('Application', '获取系统信息失败', e);
    }
  },

  /**
   * 小程序显示
   */
  onShow(options) {
    logger.lifecycle('Application', 'onShow', options);
  },

  /**
   * 小程序隐藏
   */
  onHide() {
    logger.lifecycle('Application', 'onHide');
  },

  /**
   * 小程序错误
   */
  onError(error) {
    logger.error('Application', '小程序全局错误', error);
  },

  /**
   * 全局数据
   */
  globalData: {
    systemInfo: null,
    userInfo: null
  }
});
