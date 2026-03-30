<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useThemeStore } from '@/stores/theme'
import { deactivateCurrentUser, getCurrentUser, getNoticeUnreadSummary } from '@/api/modules'

const router = useRouter()
const authStore = useAuthStore()
const themeStore = useThemeStore()
const loading = ref(false)
const deactivating = ref(false)
const errorMessage = ref('')
const noticeUnreadCount = ref(0)

const quickActions = computed(() => [
  { key: 'follow', title: '关注列表', desc: '管理互关关系' },
  { key: 'achievement', title: '成就头衔', desc: '查看成就并管理佩戴' },
  { key: 'notice', title: '系统通知', desc: '查看系统提醒', badge: noticeUnreadCount.value },
  { key: 'posts', title: '我的帖子', desc: '查看我发布的全部主题' }
])

const accountActions = computed(() => [
  { key: 'email', title: '邮箱管理', desc: '绑定与认证邮箱' },
  { key: 'password', title: '修改密码', desc: '更新登录凭证' }
])

const defaultThemeOption = computed(() => ({
  name: 'retro_steel',
  label: '默认主题（钢灰）',
  description: '稳定清晰，耐看不乱',
  preview: themeStore.darkModeEnabled
    ? ['#161d26', '#253445', '#8eb3d8']
    : ['#dfe5ec', '#c4ced8', '#4f6278']
}))

async function loadProfile() {
  loading.value = true
  errorMessage.value = ''
  try {
    const [user, unreadSummary] = await Promise.all([
      getCurrentUser(),
      getNoticeUnreadSummary()
    ])
    authStore.setAuth(authStore.token, user)
    noticeUnreadCount.value = Number(unreadSummary?.totalUnread || 0)
  } catch (error) {
    errorMessage.value = error.message || '加载资料失败'
  } finally {
    loading.value = false
  }
}

function goUserHome() {
  const userId = Number(authStore.userInfo?.userId || 0)
  if (!userId) {
    return
  }
  router.push(`/user/${userId}`)
}

function goProfileEdit() {
  router.push('/profile')
}

function handleQuickAction(actionKey) {
  const userId = Number(authStore.userInfo?.userId || 0)
  if (actionKey === 'follow') {
    router.push('/follow')
    return
  }
  if (actionKey === 'achievement') {
    router.push('/achievement')
    return
  }
  if (actionKey === 'notice') {
    router.push('/notice')
    return
  }
  if (actionKey === 'posts') {
    if (!userId) {
      return
    }
    router.push(`/user/${userId}/posts?currentUserId=${userId}`)
    return
  }
}

function handleAccountAction(actionKey) {
  if (actionKey === 'email') {
    router.push('/email')
    return
  }
  if (actionKey === 'password') {
    router.push('/password')
  }
}

function handleSelectTheme(themeName) {
  themeStore.setThemeName(themeName)
}

function handleToggleDarkMode() {
  themeStore.toggleDarkMode()
}

function handleLogout() {
  authStore.clearAuth()
  router.replace('/login')
}

async function handleDeactivateAccount() {
  if (deactivating.value) {
    return
  }
  const firstConfirm = window.confirm('注销后将立即退出登录，且当前账号不可恢复。确定继续吗？')
  if (!firstConfirm) {
    return
  }
  const secondConfirm = window.confirm('该操作用于测试环境，是否立即注销当前账号？')
  if (!secondConfirm) {
    return
  }
  deactivating.value = true
  errorMessage.value = ''
  try {
    await deactivateCurrentUser()
    authStore.clearAuth()
    router.replace('/login')
  } catch (error) {
    errorMessage.value = error.message || '注销失败'
  } finally {
    deactivating.value = false
  }
}

onMounted(loadProfile)
</script>

<template>
  <section class="me-page">
    <header class="page-header">
      <h1 class="page-title">我的</h1>
      <button class="button page-action-btn" :disabled="loading" @click="loadProfile">刷新</button>
    </header>

    <div class="page-body">
      <p v-if="errorMessage" class="error">{{ errorMessage }}</p>

      <article class="card identity-card" @click="goUserHome">
        <button class="identity-edit-entry" @click.stop="goProfileEdit">[编辑资料]</button>
        <div class="identity-main">
          <div class="avatar-box">
            <img v-if="authStore.userInfo?.avatar" :src="authStore.userInfo.avatar" alt="" class="avatar" />
            <div v-else class="avatar placeholder">狼</div>
          </div>
          <div class="identity-meta">
            <div class="identity-name-line">
              <strong class="identity-name">{{ authStore.userInfo?.nickname || authStore.userInfo?.wolfNo || '行者' }}</strong>
              <span v-if="authStore.userInfo?.equippedTitleName" class="identity-title-badge">
                [{{ authStore.userInfo.equippedTitleName }}]
              </span>
            </div>
            <p class="text-muted">狼藉号 {{ authStore.userInfo?.wolfNo || '--' }}</p>
            <p class="text-muted">{{ authStore.userInfo?.email || '未绑定邮箱' }}</p>
          </div>
        </div>
      </article>

      <article class="card section-card">
        <div class="section-head">
          <strong>常用功能</strong>
          <span class="text-muted">个人高频入口</span>
        </div>
        <div class="quick-grid">
          <div
            v-for="item in quickActions"
            :key="item.key"
            class="quick-item"
            @click="handleQuickAction(item.key)"
          >
            <div class="quick-title-row">
              <span class="quick-title">{{ item.title }}</span>
              <span v-if="item.badge > 0" class="quick-badge">[{{ item.badge > 99 ? '99+' : item.badge }}]</span>
            </div>
            <div class="quick-desc">{{ item.desc }}</div>
          </div>
        </div>
      </article>

      <article class="card section-card">
        <div class="section-head">
          <strong>主题色</strong>
          <span class="text-muted">界面风格切换</span>
        </div>

        <div class="theme-list">
          <div class="theme-row">
            <div
              class="theme-item"
              :class="{ 'theme-item-active': themeStore.themeName === defaultThemeOption.name }"
              @click="handleSelectTheme(defaultThemeOption.name)"
            >
              <div class="theme-selected" v-if="themeStore.themeName === defaultThemeOption.name">[已选]</div>
              <div class="theme-bars">
                <div v-for="color in defaultThemeOption.preview" :key="color" class="theme-bar" :style="`background:${color}`"></div>
              </div>
              <div class="theme-main">
                <div class="theme-name">{{ defaultThemeOption.label }}</div>
                <div class="theme-desc">{{ defaultThemeOption.description }}</div>
              </div>
            </div>

            <div class="theme-item theme-item-dark" :class="{ 'theme-item-active': themeStore.darkModeEnabled }" @click="handleToggleDarkMode">
              <div class="theme-selected" :class="{ 'theme-selected-muted': !themeStore.darkModeEnabled }">
                {{ themeStore.darkModeEnabled ? '[已开]' : '[关闭]' }}
              </div>
              <div class="theme-bars theme-bars-dark">
                <div class="theme-bar theme-bar-dark-1"></div>
                <div class="theme-bar theme-bar-dark-2"></div>
                <div class="theme-bar theme-bar-dark-3"></div>
              </div>
              <div class="theme-main">
                <div class="theme-name">夜行模式</div>
                <div class="theme-desc">整站随主题一起变暗</div>
              </div>
            </div>
          </div>

          <div class="theme-row">
            <div class="theme-item theme-item-placeholder">
              <div class="theme-bars theme-bars-placeholder">
                <div class="theme-bar theme-bar-placeholder-1"></div>
                <div class="theme-bar theme-bar-placeholder-2"></div>
                <div class="theme-bar theme-bar-placeholder-3"></div>
              </div>
              <div class="theme-main">
                <div class="theme-name">施工中</div>
                <div class="theme-desc">尽请期待 comming soon.</div>
              </div>
            </div>
          </div>
        </div>
      </article>

      <article class="card section-card danger-card">
        <div class="section-head">
          <strong>账号操作</strong>
          <span class="text-muted">账号与安全</span>
        </div>

        <div class="quick-grid">
          <div
            v-for="item in accountActions"
            :key="item.key"
            class="quick-item"
            @click="handleAccountAction(item.key)"
          >
            <div class="quick-title">{{ item.title }}</div>
            <div class="quick-desc">{{ item.desc }}</div>
          </div>
        </div>

        <div class="account-divider"></div>

        <div class="danger-actions">
          <button class="button logout-btn" @click="handleLogout">退出登录</button>
          <button class="button deactivate-btn" :disabled="deactivating" @click="handleDeactivateAccount">
            {{ deactivating ? '注销中...' : '注销账号（测试）' }}
          </button>
        </div>
      </article>
    </div>
  </section>
</template>

<style scoped>
.me-page {
  min-height: 100%;
}

.identity-card,
.section-card {
  padding: 10px;
  margin-bottom: 10px;
}

.identity-card {
  cursor: pointer;
  position: relative;
}

.identity-edit-entry {
  position: absolute;
  right: 10px;
  top: 10px;
  border: none;
  background: transparent;
  color: var(--retro-accent-text);
  font-size: 12px;
  cursor: pointer;
}

.identity-main {
  display: flex;
  gap: 10px;
}

.avatar-box {
  width: 56px;
  height: 56px;
  border: 1px solid var(--retro-panel-border);
  background: var(--retro-theme-item-bg);
}

.avatar {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--retro-muted-text);
}

.identity-meta {
  min-width: 0;
}

.identity-name-line {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 4px;
}

.identity-title-badge {
  color: var(--retro-accent-text);
  font-size: 12px;
}

.identity-meta p {
  margin: 0 0 4px;
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.quick-item {
  border: 1px solid var(--retro-theme-item-border);
  padding: 8px;
  cursor: pointer;
  background: var(--retro-theme-item-bg);
}

.quick-title-row {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 4px;
}

.quick-title {
  color: var(--retro-body-text);
  font-size: 14px;
}

.quick-badge {
  color: var(--retro-accent-text);
  font-size: 12px;
}

.quick-desc {
  color: var(--retro-muted-text);
  font-size: 12px;
}

.theme-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.theme-row {
  display: flex;
  gap: 8px;
}

.theme-item {
  position: relative;
  width: calc((100% - 8px) / 2);
  min-height: 82px;
  box-sizing: border-box;
  display: flex;
  align-items: stretch;
  gap: 8px;
  padding: 0;
  border: 1px solid var(--retro-theme-item-border);
  background: var(--retro-theme-item-bg);
  overflow: hidden;
  cursor: pointer;
}

.theme-item-active {
  border-color: var(--retro-theme-item-active-border);
  background: var(--retro-theme-item-active-bg);
}

.theme-main {
  flex: 1;
  min-width: 0;
  padding: 10px 56px 10px 10px;
}

.theme-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--retro-strong-text);
}

.theme-desc {
  margin-top: 4px;
  font-size: 12px;
  color: var(--retro-muted-text);
  line-height: 1.25;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.theme-bars {
  width: 20px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  margin: 0;
  align-self: stretch;
}

.theme-bar {
  flex: 1;
  min-height: 0;
  border-radius: 0;
}

.theme-selected {
  position: absolute;
  top: 8px;
  right: 10px;
  font-size: 12px;
  color: var(--retro-accent-text);
}

.theme-selected-muted {
  color: var(--retro-muted-text);
}

.theme-item-placeholder {
  width: 100%;
  cursor: default;
}

.theme-bars-dark {
  background: #1b2330;
}

.theme-bar-dark-1 {
  background: #1f2938;
}

.theme-bar-dark-2 {
  background: #16202c;
}

.theme-bar-dark-3 {
  background: #0f1620;
}

.theme-bars-placeholder {
  background: #2d3a48;
}

.theme-bar-placeholder-1 {
  background: #3f5268;
}

.theme-bar-placeholder-2 {
  background: #697f97;
}

.theme-bar-placeholder-3 {
  background: #98acc4;
}

.danger-card {
  border-color: var(--retro-btn-danger-border);
}

.account-divider {
  margin: 10px 0;
  border-top: 1px solid var(--retro-row-border);
}

.danger-actions {
  display: grid;
  gap: 8px;
}

.logout-btn {
  width: 100%;
  border-color: var(--retro-btn-danger-border);
  color: var(--retro-btn-danger-text);
}

.deactivate-btn {
  width: 100%;
  border-color: var(--retro-btn-danger-border);
  color: var(--retro-btn-danger-text);
}

</style>
