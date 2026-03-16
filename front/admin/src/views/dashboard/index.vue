<template>
  <div class="dashboard-page">
    <el-row :gutter="12">
      <el-col :span="6" v-for="card in cards" :key="card.key">
        <div class="page-card stat-card">
          <div class="stat-title">{{ card.title }}</div>
          <div class="stat-value">{{ card.value }}</div>
          <div class="stat-desc">{{ card.desc }}</div>
        </div>
      </el-col>
    </el-row>

    <div class="page-card section-card">
      <div class="section-title">系统概览</div>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="当前在线">{{ overview.onlineUserCount }}</el-descriptions-item>
        <el-descriptions-item label="近24小时登录失败">{{ overview.loginFail24h }}</el-descriptions-item>
        <el-descriptions-item label="今日发帖">{{ overview.todayThreadCount }}</el-descriptions-item>
        <el-descriptions-item label="今日回帖">{{ overview.todayReplyCount }}</el-descriptions-item>
      </el-descriptions>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchDashboardOverview } from '@/api/dashboard'

const overview = reactive({
  userCount: 0,
  activeUser7d: 0,
  threadCount: 0,
  replyCount: 0,
  onlineUserCount: 0,
  loginFail24h: 0,
  todayThreadCount: 0,
  todayReplyCount: 0
})

const cards = computed(() => [
  { key: 'u', title: '用户总数', value: overview.userCount, desc: '系统注册用户' },
  { key: 'a', title: '近7日活跃', value: overview.activeUser7d, desc: '活跃用户数' },
  { key: 't', title: '主题总数', value: overview.threadCount, desc: '社区主题累计' },
  { key: 'r', title: '回复总数', value: overview.replyCount, desc: '社区回复累计' }
])

async function loadOverview() {
  try {
    const data = await fetchDashboardOverview()
    Object.assign(overview, {
      ...overview,
      ...(data || {})
    })
  } catch (error) {
    ElMessage.error(error?.message || '加载概览失败')
  }
}

onMounted(() => {
  loadOverview()
})
</script>

<style scoped lang="scss">
.dashboard-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.stat-card {
  padding: 14px;
  min-height: 128px;
}

.stat-title {
  font-size: 13px;
  color: #54657a;
}

.stat-value {
  margin-top: 10px;
  font-size: 30px;
  font-weight: 700;
  color: #173327;
}

.stat-desc {
  margin-top: 10px;
  font-size: 12px;
  color: #7a8998;
}

.section-card {
  padding: 14px;
}

.section-title {
  margin-bottom: 10px;
  font-size: 15px;
  font-weight: 600;
}
</style>
