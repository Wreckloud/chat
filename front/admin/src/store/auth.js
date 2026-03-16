import { defineStore } from 'pinia'
import {
  readToken,
  writeToken,
  clearToken,
  readAdminInfo,
  writeAdminInfo,
  clearAdminInfo
} from '@/utils/auth-storage'

export const useAuthStore = defineStore('admin-auth', {
  state: () => ({
    token: readToken(),
    adminInfo: readAdminInfo()
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.token)
  },
  actions: {
    setToken(token) {
      this.token = token || ''
      if (this.token) {
        writeToken(this.token)
      } else {
        clearToken()
      }
    },
    setAdminInfo(adminInfo) {
      this.adminInfo = adminInfo || null
      writeAdminInfo(this.adminInfo)
    },
    loginSuccess(token, adminInfo) {
      this.setToken(token)
      this.setAdminInfo(adminInfo)
    },
    logout() {
      this.token = ''
      this.adminInfo = null
      clearToken()
      clearAdminInfo()
    }
  }
})
