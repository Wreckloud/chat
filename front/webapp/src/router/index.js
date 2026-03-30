import { createRouter, createWebHashHistory } from 'vue-router'

import LoginView from '@/views/LoginView.vue'
import ChatView from '@/views/ChatView.vue'
import ChatDetailView from '@/views/ChatDetailView.vue'
import LobbyView from '@/views/LobbyView.vue'
import CommunityView from '@/views/CommunityView.vue'
import PostDetailView from '@/views/PostDetailView.vue'
import MeView from '@/views/MeView.vue'
import NoticeView from '@/views/NoticeView.vue'
import PostCreateView from '@/views/PostCreateView.vue'
import UserDetailView from '@/views/UserDetailView.vue'
import UserPostsView from '@/views/UserPostsView.vue'
import FollowView from '@/views/FollowView.vue'
import AchievementView from '@/views/AchievementView.vue'
import ProfileView from '@/views/ProfileView.vue'
import EmailView from '@/views/EmailView.vue'
import PasswordView from '@/views/PasswordView.vue'
import ResetPasswordView from '@/views/ResetPasswordView.vue'

const routes = [
  { path: '/', redirect: '/chat' },
  { path: '/login', component: LoginView, meta: { title: '登录' } },
  { path: '/reset-password', component: ResetPasswordView, meta: { title: '找回密码' } },
  { path: '/chat', component: ChatView, meta: { requiresAuth: true, tabbar: true, title: '会话' } },
  { path: '/chat/:conversationId', component: ChatDetailView, meta: { requiresAuth: true, title: '会话详情' } },
  { path: '/lobby', component: LobbyView, meta: { requiresAuth: true, title: '公共聊天室' } },
  { path: '/notice', component: NoticeView, meta: { requiresAuth: true, title: '系统通知' } },
  { path: '/community', component: CommunityView, meta: { requiresAuth: true, tabbar: true, title: '社区' } },
  { path: '/post/create', component: PostCreateView, meta: { requiresAuth: true, title: '发布主题' } },
  { path: '/post/:threadId', component: PostDetailView, meta: { requiresAuth: true, title: '主题详情' } },
  { path: '/user/:userId', component: UserDetailView, meta: { requiresAuth: true, title: '行者主页' } },
  { path: '/user/:userId/posts', component: UserPostsView, meta: { requiresAuth: true, title: '用户帖子' } },
  { path: '/follow', component: FollowView, meta: { requiresAuth: true, title: '关注列表' } },
  { path: '/achievement', component: AchievementView, meta: { requiresAuth: true, title: '成就头衔' } },
  { path: '/profile', component: ProfileView, meta: { requiresAuth: true, title: '编辑资料' } },
  { path: '/email', component: EmailView, meta: { requiresAuth: true, title: '邮箱管理' } },
  { path: '/password', component: PasswordView, meta: { requiresAuth: true, title: '修改密码' } },
  { path: '/me', component: MeView, meta: { requiresAuth: true, tabbar: true, title: '我的' } }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

export default router
