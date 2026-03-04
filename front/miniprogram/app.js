/**
 * WolfChat 小程序入口文件
 * @author Wreckloud
 * @date 2024-12-18
 */
const { getThemeName, applyTabBar } = require('./utils/theme')

App({
  /**
   * 小程序初始化
   */
  onLaunch() {
    applyTabBar(getThemeName())
  },

  /**
   * 小程序显示
   */
  onShow() {
    applyTabBar(getThemeName())
  },

  /**
   * 小程序隐藏
   */
  onHide() {
  },

  /**
   * 小程序错误
   */
  onError(error) {
    console.error('[App] 小程序全局错误', error);
  },

  // 暂无全局数据
});
