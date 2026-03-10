/**
 * 主题中心
 * 负责主题持久化、页面主题类和导航栏配色
 */
const THEME_KEY = 'wolfchat_theme'
const DEFAULT_THEME = 'retro_blue'

const THEMES = {
  retro_blue: {
    name: 'retro_blue',
    label: '雾蓝经典',
    description: '清爽论坛蓝',
    preview: ['#dce4ef', '#c7d2e4', '#536b87'],
    className: 'theme-retro-blue',
    nav: {
      frontColor: '#ffffff',
      backgroundColor: '#536b87'
    },
    tabBar: {
      color: '#7a8798',
      selectedColor: '#536b87',
      backgroundColor: '#ffffff',
      borderStyle: 'black'
    },
    swipeAction: {
      pin: '#315f9f',
      unread: '#5e6d82',
      more: '#6f7f95'
    }
  },
  retro_olive: {
    name: 'retro_olive',
    label: '森林深绿',
    description: '以 #0a3e1e 为主色',
    preview: ['#d8e6dc', '#a8c2b1', '#0a3e1e'],
    className: 'theme-retro-olive',
    nav: {
      frontColor: '#ffffff',
      backgroundColor: '#0a3e1e'
    },
    tabBar: {
      color: '#6a756d',
      selectedColor: '#0a3e1e',
      backgroundColor: '#ffffff',
      borderStyle: 'black'
    },
    swipeAction: {
      pin: '#0f5f30',
      unread: '#5f7567',
      more: '#637a6c'
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

function getThemeContext(themeName) {
  const theme = getTheme(themeName)
  return {
    themeName: theme.name,
    themeClass: theme.className,
    theme
  }
}

function getSwipeActionStyles(themeName) {
  const theme = getTheme(themeName)
  return {
    pin: `color:#ffffff;background-color:${theme.swipeAction.pin};`,
    unread: `color:#ffffff;background-color:${theme.swipeAction.unread};`,
    more: `color:#ffffff;background-color:${theme.swipeAction.more};`
  }
}

function applyNavigationBar(themeName) {
  const theme = getTheme(themeName)
  wx.setNavigationBarColor({
    frontColor: theme.nav.frontColor,
    backgroundColor: theme.nav.backgroundColor
  })
}

function applyTabBar(themeName) {
  const theme = getTheme(themeName)
  const tabBar = theme.tabBar
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

function listThemes() {
  return Object.keys(THEMES).map(themeName => {
    const theme = THEMES[themeName]
    return {
      name: theme.name,
      label: theme.label,
      description: theme.description,
      preview: theme.preview
    }
  })
}

module.exports = {
  THEMES,
  DEFAULT_THEME,
  getThemeName,
  setThemeName,
  getTheme,
  getThemeContext,
  getSwipeActionStyles,
  applyNavigationBar,
  applyTabBar,
  listThemes
}
