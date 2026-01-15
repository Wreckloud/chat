/**
 * WolfChat 小程序入口文件
 * @author Wreckloud
 * @date 2024-12-18
 */

const auth = require('./utils/auth.js');

App({
  /**
   * 小程序初始化
   */
  onLaunch(options) {
    console.info('[App] WolfChat 小程序启动', options);
    
    // 检查登录状态
    const isLogin = auth.isLogin();
    console.info('[App] 登录状态:', isLogin);
    
    // 获取系统信息
    try {
      const systemInfo = wx.getSystemInfoSync(); // TODO:以后可以换新的api
      this.globalData.systemInfo = systemInfo;
      console.debug('[App] 系统信息获取成功', {
        platform: systemInfo.platform,
        system: systemInfo.system,
        version: systemInfo.version
      });
    } catch (e) {
      console.error('[App] 获取系统信息失败', e);
    }
  },

  /**
   * 小程序显示
   */
  onShow(options) {
    console.debug('[App] onShow', options);
  },

  /**
   * 小程序隐藏
   */
  onHide() {
    console.debug('[App] onHide');
  },

  /**
   * 小程序错误
   */
  onError(error) {
    console.error('[App] 小程序全局错误', error);
  },

  /**
   * 全局数据
   */
  globalData: {
    systemInfo: null,
    userInfo: null
  }
});
