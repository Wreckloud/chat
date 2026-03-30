import { defineStore } from 'pinia'

const AUTH_STORAGE_KEY = 'wolfchat_web_auth'

function safeParseAuth(raw) {
  if (!raw) {
    return null
  }
  try {
    const parsed = JSON.parse(raw)
    if (!parsed || typeof parsed !== 'object') {
      return null
    }
    return parsed
  } catch (error) {
    return null
  }
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: '',
    userInfo: null
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.token),
    userId: (state) => state.userInfo?.userId || null
  },
  actions: {
    setAuth(token, userInfo) {
      this.token = token || ''
      this.userInfo = userInfo || null
      this.persist()
    },
    clearAuth() {
      this.token = ''
      this.userInfo = null
      localStorage.removeItem(AUTH_STORAGE_KEY)
    },
    persist() {
      localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify({
        token: this.token,
        userInfo: this.userInfo
      }))
    },
    restore() {
      const data = safeParseAuth(localStorage.getItem(AUTH_STORAGE_KEY))
      if (!data?.token) {
        return
      }
      this.token = data.token
      this.userInfo = data.userInfo || null
    }
  }
})

