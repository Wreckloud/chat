/**
 * 主题中心
 * 负责主题持久化、页面主题类和导航栏配色
 */
const THEME_KEY = 'wolfchat_theme'
const DARK_MODE_KEY = 'wolfchat_dark_mode'
const DEFAULT_THEME = 'retro_steel'

const THEMES = {
  retro_steel: {
    name: 'retro_steel',
    label: '默认主题（钢灰）',
    description: '稳定清晰，耐看不乱',
    preview: ['#dfe5ec', '#c4ced8', '#4f6278'],
    darkPreview: ['#161d26', '#253445', '#8eb3d8'],
    className: 'theme-retro-steel',
    nav: {
      frontColor: '#ffffff',
      backgroundColor: '#4f6278',
      darkBackgroundColor: '#252f3a'
    },
    tabBar: {
      color: '#6a7685',
      selectedColor: '#4f6278',
      backgroundColor: '#ffffff',
      borderStyle: 'black'
    },
    swipeAction: {
      pin: '#3f668f',
      unread: '#6b7787',
      more: '#6f8095'
    }
  }
}

function normalizeThemeName(themeName) {
  return Object.prototype.hasOwnProperty.call(THEMES, themeName)
    ? themeName
    : DEFAULT_THEME
}

function getThemeName() {
  const saved = wx.getStorageSync(THEME_KEY)
  return normalizeThemeName(saved)
}

function setThemeName(themeName) {
  const normalized = normalizeThemeName(themeName)
  wx.setStorageSync(THEME_KEY, normalized)
  return normalized
}

function getTheme(themeName) {
  const name = normalizeThemeName(themeName || getThemeName())
  return THEMES[name]
}

function normalizeDarkModeEnabled(enabled) {
  return enabled === true || enabled === 'true' || enabled === 1 || enabled === '1'
}

function getDarkModeEnabled() {
  const saved = wx.getStorageSync(DARK_MODE_KEY)
  return normalizeDarkModeEnabled(saved)
}

function setDarkModeEnabled(enabled) {
  const normalized = normalizeDarkModeEnabled(enabled)
  wx.setStorageSync(DARK_MODE_KEY, normalized)
  return normalized
}

function buildThemeClass(theme, darkModeEnabled) {
  return darkModeEnabled ? `${theme.className} theme-dark` : theme.className
}

function getThemeContext(themeName, options = {}) {
  const theme = getTheme(themeName)
  const darkModeEnabled = options.darkModeEnabled === undefined
    ? getDarkModeEnabled()
    : normalizeDarkModeEnabled(options.darkModeEnabled)
  return {
    themeName: theme.name,
    themeClass: buildThemeClass(theme, darkModeEnabled),
    darkModeEnabled,
    theme
  }
}

function getSwipeActionStyles(themeName, options = {}) {
  const themeContext = getThemeContext(themeName, options)
  const swipeAction = themeContext.theme.swipeAction
  return {
    pin: `color:#ffffff;background-color:${swipeAction.pin};`,
    unread: `color:#ffffff;background-color:${swipeAction.unread};`,
    more: `color:#ffffff;background-color:${swipeAction.more};`
  }
}

function applyNavigationBar(themeName, options = {}) {
  const themeContext = getThemeContext(themeName, options)
  const theme = themeContext.theme
  const nav = themeContext.darkModeEnabled
    ? {
      frontColor: '#ffffff',
      backgroundColor: theme.nav.darkBackgroundColor || '#1c2531'
    }
    : theme.nav

  wx.setNavigationBarColor({
    frontColor: nav.frontColor,
    backgroundColor: nav.backgroundColor
  })
}

function applyTabBar(themeName, options = {}) {
  const themeContext = getThemeContext(themeName, options)
  const theme = themeContext.theme
  const tabBar = themeContext.darkModeEnabled
    ? {
      color: '#8d9cae',
      selectedColor: theme.tabBar.selectedColor,
      backgroundColor: '#141b24',
      borderStyle: 'white'
    }
    : theme.tabBar

  if (!tabBar) {
    return
  }
  wx.setTabBarStyle({
    color: tabBar.color,
    selectedColor: tabBar.selectedColor,
    backgroundColor: tabBar.backgroundColor,
    borderStyle: tabBar.borderStyle
  })
}

function listThemes(options = {}) {
  const darkModeEnabled = normalizeDarkModeEnabled(options.darkModeEnabled)
  return Object.keys(THEMES).map(themeName => {
    const theme = THEMES[themeName]
    return {
      name: theme.name,
      label: theme.label,
      description: theme.description,
      preview: darkModeEnabled && Array.isArray(theme.darkPreview) ? theme.darkPreview : theme.preview
    }
  })
}

module.exports = {
  THEMES,
  DEFAULT_THEME,
  getThemeName,
  setThemeName,
  getDarkModeEnabled,
  setDarkModeEnabled,
  getTheme,
  getThemeContext,
  getSwipeActionStyles,
  applyNavigationBar,
  applyTabBar,
  listThemes
}
