import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import { useAuthStore } from './stores/auth'
import { useThemeStore } from './stores/theme'
import { configure as configureWs, disconnect as disconnectWs } from './realtime/ws-client'
import './styles.css'

const app = createApp(App)
const pinia = createPinia()
app.use(pinia)

const authStore = useAuthStore(pinia)
authStore.restore()
const themeStore = useThemeStore(pinia)
themeStore.restore()
configureWs({
  tokenProvider: () => authStore.token,
  onAuthExpired: () => {
    authStore.clearAuth()
    router.replace('/login')
  }
})

router.beforeEach((to) => {
  if (!authStore.token) {
    disconnectWs()
  }
  if (to.meta.requiresAuth && !authStore.token) {
    return '/login'
  }
  if (to.path === '/login' && authStore.token) {
    return '/chat'
  }
  return true
})

app.use(router)
app.mount('#app')
