<template>
  <el-container class="admin-shell">
    <el-aside class="admin-aside" width="220px">
      <div class="brand">
        <div class="brand-title">WolfChat</div>
        <div class="brand-sub">管理后台</div>
      </div>

      <el-menu
        :default-active="activeMenu"
        class="admin-menu"
        background-color="transparent"
        text-color="#cfe6d8"
        active-text-color="#ffffff"
        router>
        <el-menu-item index="/dashboard">
          <span>控制台</span>
        </el-menu-item>
        <el-menu-item index="/users">
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/content">
          <span>内容治理</span>
        </el-menu-item>
        <el-menu-item index="/ai">
          <span>AI管控</span>
        </el-menu-item>
        <el-menu-item index="/audit">
          <span>审计日志</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="admin-header">
        <div class="header-title">{{ pageTitle }}</div>
        <div class="header-right">
          <span class="admin-name">{{ adminName }}</span>
          <el-button link type="primary" @click="handleLogout">退出</el-button>
        </div>
      </el-header>

      <el-main class="admin-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/store/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const activeMenu = computed(() => route.path)
const pageTitle = computed(() => route.meta?.title || '管理后台')
const adminName = computed(() => authStore.adminInfo?.nickname || authStore.adminInfo?.username || '管理员')

function handleLogout() {
  ElMessageBox.confirm('确认退出当前管理账号？', '提示', {
    type: 'warning'
  }).then(() => {
    authStore.logout()
    router.replace('/login')
  }).catch(() => {})
}
</script>

<style scoped lang="scss">
.admin-shell {
  width: 100%;
  height: 100vh;
}

.admin-aside {
  background: linear-gradient(180deg, $side-bg 0%, $side-bg-deep 100%);
  color: $side-text;
  border-right: 1px solid rgba(255, 255, 255, 0.08);
}

.brand {
  padding: 18px 16px 14px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.brand-title {
  font-size: 20px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 0.5px;
}

.brand-sub {
  margin-top: 4px;
  font-size: 12px;
  color: #b7d3c0;
}

.admin-menu {
  border-right: 0;
  padding-top: 10px;
}

.admin-menu :deep(.el-menu-item) {
  margin: 4px 10px;
  border-radius: 4px;
}

.admin-menu :deep(.el-menu-item.is-active) {
  background: $side-active-bg;
  color: $side-active-text;
}

.admin-header {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: $header-bg;
  border-bottom: 1px solid $header-border;
  padding: 0 18px;
}

.header-title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2d3d;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.admin-name {
  font-size: 13px;
  color: #4a5d6f;
}

.admin-main {
  background: $app-bg;
  padding: 14px;
}
</style>
