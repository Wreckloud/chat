import { createRouter, createWebHistory } from 'vue-router'
import { readToken } from '@/utils/auth-storage'

const routes = [
  {
    path: '/login',
    name: 'AdminLogin',
    component: () => import('@/views/login/index.vue'),
    meta: {
      public: true,
      title: '管理端登录'
    }
  },
  {
    path: '/',
    component: () => import('@/layout/index.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'AdminDashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: {
          title: '控制台'
        }
      },
      {
        path: 'users',
        name: 'AdminUsers',
        component: () => import('@/views/users/index.vue'),
        meta: {
          title: '用户管理'
        }
      },
      {
        path: 'content',
        name: 'AdminContent',
        component: () => import('@/views/content/index.vue'),
        meta: {
          title: '内容治理'
        }
      },
      {
        path: 'audit',
        name: 'AdminAudit',
        component: () => import('@/views/audit/index.vue'),
        meta: {
          title: '审计日志'
        }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'AdminNotFound',
    component: () => import('@/views/not-found/index.vue'),
    meta: {
      public: true,
      title: '页面不存在'
    }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = readToken()
  const needAuth = to.meta.public !== true
  if (needAuth && !token) {
    next('/login')
    return
  }
  if (to.path === '/login' && token) {
    next('/dashboard')
    return
  }
  next()
})

export default router
