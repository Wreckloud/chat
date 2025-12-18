<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="title">控制台</h2>
      <p class="subtitle">欢迎回来，{{ userInfo.username }} 🐺</p>
    </div>
    
    <!-- 数据统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <div class="stat-card wolf-card">
          <div class="stat-icon" style="background: linear-gradient(135deg, #0A3E1E, #14733A)">
            <el-icon size="32"><User /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.userCount }}</div>
            <div class="stat-label">用户总数</div>
          </div>
        </div>
      </el-col>
      
      <el-col :span="6">
        <div class="stat-card wolf-card">
          <div class="stat-icon" style="background: linear-gradient(135deg, #4CAF50, #66BB6A)">
            <el-icon size="32"><ChatDotSquare /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.groupCount }}</div>
            <div class="stat-label">群组总数</div>
          </div>
        </div>
      </el-col>
      
      <el-col :span="6">
        <div class="stat-card wolf-card">
          <div class="stat-icon" style="background: linear-gradient(135deg, #2196F3, #42A5F5)">
            <el-icon size="32"><ChatLineRound /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.messageCount }}</div>
            <div class="stat-label">消息总数</div>
          </div>
        </div>
      </el-col>
      
      <el-col :span="6">
        <div class="stat-card wolf-card">
          <div class="stat-icon" style="background: linear-gradient(135deg, #FF9800, #FFA726)">
            <el-icon size="32"><TrendCharts /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.activeUserCount }}</div>
            <div class="stat-label">活跃用户</div>
          </div>
        </div>
      </el-col>
    </el-row>
    
    <!-- 快捷操作 -->
    <div class="quick-actions wolf-card mt-20">
      <h3 class="section-title">快捷操作</h3>
      <el-row :gutter="16">
        <el-col :span="6">
          <el-button type="primary" :icon="User" @click="goToUsers">
            用户管理
          </el-button>
        </el-col>
        <el-col :span="6">
          <el-button type="success" :icon="ChatDotSquare" @click="goToGroups">
            群组管理
          </el-button>
        </el-col>
      </el-row>
    </div>
    
    <!-- 系统信息 -->
    <div class="system-info wolf-card mt-20">
      <h3 class="section-title">系统信息</h3>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="系统名称">WolfChat 即时通讯系统</el-descriptions-item>
        <el-descriptions-item label="系统版本">v1.0.0</el-descriptions-item>
        <el-descriptions-item label="后端版本">Spring Boot 2.6.1</el-descriptions-item>
        <el-descriptions-item label="前端版本">Vue 3 + Element Plus</el-descriptions-item>
        <el-descriptions-item label="数据库">MySQL 8.0</el-descriptions-item>
        <el-descriptions-item label="缓存">Redis 7.0</el-descriptions-item>
      </el-descriptions>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/store/auth'
import { getMyGroups } from '@/api/group'
import logger from '@/utils/logger'

const router = useRouter()
const authStore = useAuthStore()

// 用户信息
const userInfo = computed(() => authStore.userInfo)

// 统计数据
const stats = ref({
  userCount: 0,
  groupCount: 0,
  messageCount: 0,
  activeUserCount: 0
})

// 加载统计数据
const loadStats = async () => {
  logger.info('Dashboard', '加载统计数据')
  
  try {
    // 获取我的群组数量（使用现有接口）
    const groups = await getMyGroups()
    stats.value.groupCount = groups ? groups.length : 0
    
    logger.info('Dashboard', '统计数据加载成功', stats.value)
  } catch (error) {
    logger.error('Dashboard', '加载统计数据失败', error)
    // 接口失败时保持为0
  }
  
  // TODO: 其他统计数据需要后端实现专门的统计接口
  // stats.value.userCount - 需要 /admin/statistics/users
  // stats.value.messageCount - 需要 /admin/statistics/messages
  // stats.value.activeUserCount - 需要 /admin/statistics/active-users
}

// 跳转到用户管理
const goToUsers = () => {
  logger.action('Dashboard', 'goToUsers')
  router.push('/users')
}

// 跳转到群组管理
const goToGroups = () => {
  logger.action('Dashboard', 'goToGroups')
  router.push('/groups')
}

onMounted(() => {
  loadStats()
})
</script>

<style lang="scss" scoped>
.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 24px;
  
  .stat-icon {
    width: 64px;
    height: 64px;
    border-radius: $border-radius-large;
    display: flex;
    align-items: center;
    justify-content: center;
    color: $background-white;
    flex-shrink: 0;
  }
  
  .stat-content {
    flex: 1;
    
    .stat-value {
      font-size: 32px;
      font-weight: 600;
      color: $primary-color;
      line-height: 1;
      margin-bottom: 8px;
    }
    
    .stat-label {
      font-size: 14px;
      color: $text-secondary;
    }
  }
}

.quick-actions,
.system-info {
  .section-title {
    font-size: 16px;
    font-weight: 500;
    color: $text-primary;
    margin-bottom: 16px;
    
    &::before {
      content: '';
      display: inline-block;
      width: 4px;
      height: 16px;
      background: $primary-color;
      margin-right: 8px;
      vertical-align: middle;
    }
  }
}
</style>

