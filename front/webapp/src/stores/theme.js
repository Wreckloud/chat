import { defineStore } from 'pinia'

const THEME_STORAGE_KEY = 'wolfchat_web_theme_name'
const DARK_MODE_STORAGE_KEY = 'wolfchat_web_dark_mode'
const DEFAULT_THEME_NAME = 'retro_steel'

function normalizeThemeName(value) {
  const raw = String(value || '').trim()
  if (raw === DEFAULT_THEME_NAME) {
    return raw
  }
  return DEFAULT_THEME_NAME
}

function normalizeDarkMode(value) {
  return value === true || value === 'true' || value === 1 || value === '1'
}

export const useThemeStore = defineStore('theme', {
  state: () => ({
    themeName: DEFAULT_THEME_NAME,
    darkModeEnabled: false
  }),
  getters: {
    rootClass(state) {
      const classNames = ['theme-retro-steel']
      if (state.darkModeEnabled) {
        classNames.push('theme-dark')
      }
      return classNames.join(' ')
    }
  },
  actions: {
    restore() {
      const storedThemeName = localStorage.getItem(THEME_STORAGE_KEY)
      const storedDarkMode = localStorage.getItem(DARK_MODE_STORAGE_KEY)
      this.themeName = normalizeThemeName(storedThemeName)
      this.darkModeEnabled = normalizeDarkMode(storedDarkMode)
    },
    persist() {
      localStorage.setItem(THEME_STORAGE_KEY, this.themeName)
      localStorage.setItem(DARK_MODE_STORAGE_KEY, this.darkModeEnabled ? 'true' : 'false')
    },
    setThemeName(themeName) {
      this.themeName = normalizeThemeName(themeName)
      this.persist()
    },
    setDarkModeEnabled(enabled) {
      this.darkModeEnabled = normalizeDarkMode(enabled)
      this.persist()
    },
    toggleDarkMode() {
      this.darkModeEnabled = !this.darkModeEnabled
      this.persist()
    }
  }
})
