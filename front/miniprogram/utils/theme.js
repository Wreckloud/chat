/**
 * 主题中心
 * 负责主题持久化、页面主题类和导航栏配色
 */
const THEME_KEY = 'wolfchat_theme'
const DARK_MODE_KEY = 'wolfchat_dark_mode'
const DEFAULT_THEME = 'retro_blue'

const THEMES = {
  retro_blue: {
    name: 'retro_blue',
    label: '雾蓝',
    description: '清透克制',
    preview: ['#dce4ef', '#c7d2e4', '#536b87'],
    darkPreview: ['#151c28', '#223243', '#8db5ee'],
    className: 'theme-retro-blue',
    nav: {
      frontColor: '#ffffff',
      backgroundColor: '#536b87',
      darkBackgroundColor: '#273442'
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
    label: '深绿',
    description: '沉稳不刺眼',
    preview: ['#d8e6dc', '#a8c2b1', '#0a3e1e'],
    darkPreview: ['#15211b', '#24382d', '#83c59e'],
    className: 'theme-retro-olive',
    nav: {
      frontColor: '#ffffff',
      backgroundColor: '#0a3e1e',
      darkBackgroundColor: '#14261c'
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
  },
  retro_amber: {
    name: 'retro_amber',
    label: '暖棕',
    description: '纸页木纹感',
    preview: ['#f2e8d5', '#dcc6a2', '#7a5b2e'],
    darkPreview: ['#211a13', '#372c21', '#d5af75'],
    className: 'theme-retro-amber',
    nav: {
      frontColor: '#ffffff',
      backgroundColor: '#7a5b2e',
      darkBackgroundColor: '#332a1d'
    },
    tabBar: {
      color: '#7a6a51',
      selectedColor: '#7a5b2e',
      backgroundColor: '#ffffff',
      borderStyle: 'black'
    },
    swipeAction: {
      pin: '#8a6328',
      unread: '#877256',
      more: '#8f795b'
    }
  },
  retro_steel: {
    name: 'retro_steel',
    label: '钢灰',
    description: '信息密也清楚',
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
  },
  retro_wine: {
    name: 'retro_wine',
    label: '酒红',
    description: '夜聊更有氛围',
    preview: ['#eddfe3', '#d5b3bd', '#6c3948'],
    darkPreview: ['#20171d', '#342630', '#d39ab0'],
    className: 'theme-retro-wine',
    nav: {
      frontColor: '#ffffff',
      backgroundColor: '#6c3948',
      darkBackgroundColor: '#33242a'
    },
    tabBar: {
      color: '#7f6a72',
      selectedColor: '#6c3948',
      backgroundColor: '#ffffff',
      borderStyle: 'black'
    },
    swipeAction: {
      pin: '#7d4156',
      unread: '#7c6570',
      more: '#866f79'
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
