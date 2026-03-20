const { getThemeContext, applyNavigationBar, applyTabBar } = require('./theme')

/**
 * 同步页面主题样式和导航配色
 */
function applyPageTheme(page, options = {}) {
  const themeContext = getThemeContext(options.themeName, {
    darkModeEnabled: options.darkModeEnabled
  })
  const payload = {
    themeClass: themeContext.themeClass
  }

  if (typeof options.extraData === 'function') {
    const extra = options.extraData(themeContext)
    if (extra && typeof extra === 'object') {
      Object.assign(payload, extra)
    }
  }

  page.setData(payload)
  applyNavigationBar(themeContext.themeName, {
    darkModeEnabled: themeContext.darkModeEnabled
  })
  if (options.tabBar) {
    applyTabBar(themeContext.themeName, {
      darkModeEnabled: themeContext.darkModeEnabled
    })
  }

  return themeContext
}

module.exports = {
  applyPageTheme
}
