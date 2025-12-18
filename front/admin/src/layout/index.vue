<template>
  <el-container class="layout-container">
    <!-- 侧边栏 -->
    <el-aside :width="isCollapse ? '64px' : '200px'" class="layout-aside">
      <div class="logo-container" :class="{ collapsed: isCollapse }">
        <span class="logo-icon">🐺</span>
        <span v-if="!isCollapse" class="logo-text">WolfChat</span>
      </div>
      
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        :collapse-transition="false"
        router
        class="layout-menu"
      >
        <el-menu-item
          v-for="route in menuRoutes"
          :key="route.path"
          :index="route.path"
        >
          <el-icon><component :is="route.meta.icon" /></el-icon>
          <template #title>{{ route.meta.title }}</template>
        </el-menu-item>
      </el-menu>
    </el-aside>
    
    <!-- 主体区域 -->
    <el-container>
      <!-- 顶部栏 -->
      <el-header class="layout-header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="toggleCollapse">
            <Fold v-if="!isCollapse" />
            <Expand v-else />
          </el-icon>
        </div>
        
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <div class="user-info">
              <el-avatar :size="32" :src="userInfo.avatar">
                {{ userInfo.username?.charAt(0) }}
              </el-avatar>
              <span class="username">{{ userInfo.username }}</span>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>
                  <el-icon><User /></el-icon>
                  WF号: {{ userInfo.wfNo }}
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      
      <!-- 内容区域 -->
      <el-main class="layout-main">
        <router-view v-slot="{ Component }">
          <transition name="fade-transform" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/store/auth'
import { ElMessageBox } from 'element-plus'
import logger from '@/utils/logger'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

// 是否折叠侧边栏
const isCollapse = ref(false)

// 用户信息
const userInfo = computed(() => authStore.userInfo)

// 当前激活的菜单
const activeMenu = computed(() => {
  const { path } = route
  // 如果是详情页，激活父级菜单
  if (path.includes('/groups/')) {
    return '/groups'
  }
  return path
})

// 菜单路由（过滤掉隐藏的路由）
const menuRoutes = computed(() => {
  const mainRoute = router.options.routes.find(r => r.path === '/')
  return mainRoute?.children.filter(r => !r.meta?.hidden) || []
})

// 切换侧边栏折叠状态
const toggleCollapse = () => {
  isCollapse.value = !isCollapse.value
  logger.action('Layout', 'toggleCollapse', { isCollapse: isCollapse.value })
}

// 处理下拉菜单命令
const handleCommand = (command) => {
  if (command === 'logout') {
    ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => {
      logger.action('Layout', 'logout', '用户退出登录')
      authStore.logout()
    }).catch(() => {
      // 取消
    })
  }
}
</script>

<style lang="scss" scoped>
.layout-container {
  width: 100%;
  height: 100vh;
}

.layout-aside {
  background: $primary-color;
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.1);
  transition: width 0.3s;
  overflow: hidden;
  
  .logo-container {
    height: $header-height;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 10px;
    padding: 0 20px;
    background: $primary-dark;
    transition: all 0.3s;
    
    &.collapsed {
      padding: 0;
      justify-content: center;
    }
    
    .logo-icon {
      font-size: 24px;
      flex-shrink: 0;
    }
    
    .logo-text {
      font-size: 20px;
      font-weight: 600;
      color: $background-white;
      white-space: nowrap;
    }
  }
  
  .layout-menu {
    border-right: none;
    background: $primary-color;
    
    :deep(.el-menu-item) {
      color: rgba(255, 255, 255, 0.8);
      
      &:hover {
        background: $primary-light !important;
        color: $background-white;
      }
      
      &.is-active {
        background: $primary-light !important;
        color: $background-white;
      }
    }
  }
}

.layout-header {
  background: $background-white;
  border-bottom: 1px solid $border-light;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
  
  .header-left {
    display: flex;
    align-items: center;
    
    .collapse-btn {
      font-size: 20px;
      cursor: pointer;
      color: $text-regular;
      transition: $transition-fast;
      
      &:hover {
        color: $primary-color;
      }
    }
  }
  
  .header-right {
    .user-info {
      display: flex;
      align-items: center;
      gap: 10px;
      cursor: pointer;
      
      .username {
        font-size: 14px;
        color: $text-primary;
      }
    }
  }
}

.layout-main {
  background: $background-base;
  overflow-y: auto;
}

// 页面切换动画
.fade-transform-enter-active,
.fade-transform-leave-active {
  transition: all 0.3s;
}

.fade-transform-enter-from {
  opacity: 0;
  transform: translateX(-30px);
}

.fade-transform-leave-to {
  opacity: 0;
  transform: translateX(30px);
}
</style>

