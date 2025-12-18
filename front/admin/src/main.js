/**
 * WolfChat 管理端入口文件
 * @author Wreckloud
 * @date 2024-12-18
 */

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import zhCn from 'element-plus/es/locale/lang/zh-cn'

import App from './App.vue'
import router from './router'
import './styles/index.scss'
import logger from './utils/logger'

// 创建应用
const app = createApp(App)

// 注册 Element Plus 图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 使用插件
app.use(createPinia())
app.use(router)
app.use(ElementPlus, {
  locale: zhCn,
  size: 'default'
})

// 全局错误处理
app.config.errorHandler = (err, instance, info) => {
  logger.error('Vue', `全局错误: ${info}`, err)
  console.error('Vue Error:', err, info)
}

// 挂载应用
app.mount('#app')

logger.info('Application', 'WolfChat 管理端启动成功 🐺')

