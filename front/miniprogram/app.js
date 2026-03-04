/**
 * WolfChat 小程序入口文件
 * @author Wreckloud
 * @date 2024-12-18
 */
const { getThemeName, applyTabBar } = require('./utils/theme')

App({
  syncTheme() {
    applyTabBar(getThemeName())
  },

  onLaunch() {
    this.syncTheme()
  },

  onShow() {
    this.syncTheme()
  },

  onError(error) {
    console.error('[App] 小程序全局错误', error)
  }
})
