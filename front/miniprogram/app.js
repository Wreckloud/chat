/**
 * WolfChat 小程序入口文件
 * @author Wreckloud
 * @date 2024-12-18
 */
const { getThemeName, applyTabBar } = require('./utils/theme')
const { refreshAllTabBadges } = require('./utils/tab-badge')

App({
  syncTheme() {
    applyTabBar(getThemeName())
  },

  onLaunch() {
    this.syncTheme()
    refreshAllTabBadges()
  },

  onShow() {
    this.syncTheme()
    refreshAllTabBadges()
  },

  onError(error) {
    console.error('[App] 小程序全局错误', error)
  }
})
