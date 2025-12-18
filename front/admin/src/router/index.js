/**
 * WolfChat 路由配置
 * @author Wreckloud
 * @date 2024-12-18
 */

import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/store/auth'
import logger from '@/utils/logger'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', requireAuth: false }
  },
  {
    path: '/',
    component: () => import('@/layout/index.vue'),
    redirect: '/dashboard',
    meta: { requireAuth: true },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '控制台', icon: 'DataLine' }
      },
      {
        path: 'users',
        name: 'Users',
        component: () => import('@/views/users/index.vue'),
        meta: { title: '用户管理', icon: 'User' }
      },
      {
        path: 'groups',
        name: 'Groups',
        component: () => import('@/views/groups/index.vue'),
        meta: { title: '群组管理', icon: 'ChatDotSquare' }
      },
      {
        path: 'groups/:id',
        name: 'GroupDetail',
        component: () => import('@/views/groups/detail.vue'),
        meta: { title: '群组详情', hidden: true }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  logger.route(from.path, to.path)
  
  const authStore = useAuthStore()
  
  // 设置页面标题
  document.title = `${to.meta.title || 'WolfChat'} - 管理后台 🐺`
  
  // 判断是否需要登录
  if (to.meta.requireAuth !== false && !authStore.isLogin()) {
    logger.warn('Router', '未登录，跳转到登录页', { to: to.path })
    next('/login')
  } else if (to.path === '/login' && authStore.isLogin()) {
    logger.info('Router', '已登录，跳转到首页')
    next('/')
  } else {
    next()
  }
})

export default router

