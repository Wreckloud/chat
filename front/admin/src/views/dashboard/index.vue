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
      <div class="section-header">
        <div class="section-title">核心趋势</div>
        <el-radio-group v-model="trendDays" size="small" @change="loadTrend">
          <el-radio-button :label="7">近7天</el-radio-button>
          <el-radio-button :label="14">近14天</el-radio-button>
          <el-radio-button :label="30">近30天</el-radio-button>
        </el-radio-group>
      </div>
      <div class="quick-overview">
        <div class="quick-item">
          <div class="quick-label">发帖总量</div>
          <div class="quick-value">{{ trendSummary.threadTotal }}</div>
        </div>
        <div class="quick-item">
          <div class="quick-label">回帖总量</div>
          <div class="quick-value">{{ trendSummary.replyTotal }}</div>
        </div>
        <div class="quick-item">
          <div class="quick-label">大厅消息总量</div>
          <div class="quick-value">{{ trendSummary.lobbyTotal }}</div>
        </div>
        <div class="quick-item">
          <div class="quick-label">登录失败次数</div>
          <div class="quick-value">{{ trendSummary.loginFailTotal }}</div>
        </div>
      </div>
      <div class="chart-grid">
        <div ref="trendLineChartRef" class="chart-panel" v-loading="loadingTrend"></div>
        <div ref="clientPieChartRef" class="chart-panel" v-loading="loadingTrend"></div>
      </div>
    </div>

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
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { fetchDashboardOverview, fetchDashboardTrend } from '@/api/dashboard'

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

const trend = reactive({
  dateLabels: [],
  registerCounts: [],
  loginSuccessCounts: [],
  loginFailCounts: [],
  threadCounts: [],
  replyCounts: [],
  lobbyMessageCounts: [],
  loginClientTypeDistribution: []
})

const trendDays = ref(7)
const loadingTrend = ref(false)
const trendLineChartRef = ref(null)
const clientPieChartRef = ref(null)
let trendLineChart = null
let clientPieChart = null

const cards = computed(() => [
  { key: 'u', title: '用户总数', value: overview.userCount, desc: '系统注册用户' },
  { key: 'a', title: '近7日活跃', value: overview.activeUser7d, desc: '最近登录用户' },
  { key: 't', title: '主题总数', value: overview.threadCount, desc: '社区主题累计' },
  { key: 'r', title: '回复总数', value: overview.replyCount, desc: '社区回复累计' }
])

const trendSummary = computed(() => ({
  threadTotal: sumArray(trend.threadCounts),
  replyTotal: sumArray(trend.replyCounts),
  lobbyTotal: sumArray(trend.lobbyMessageCounts),
  loginFailTotal: sumArray(trend.loginFailCounts)
}))

function sumArray(source) {
  return (source || []).reduce((sum, item) => sum + Number(item || 0), 0)
}

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

async function loadTrend() {
  loadingTrend.value = true
  try {
    const data = await fetchDashboardTrend({ days: trendDays.value })
    Object.assign(trend, {
      dateLabels: data?.dateLabels || [],
      registerCounts: data?.registerCounts || [],
      loginSuccessCounts: data?.loginSuccessCounts || [],
      loginFailCounts: data?.loginFailCounts || [],
      threadCounts: data?.threadCounts || [],
      replyCounts: data?.replyCounts || [],
      lobbyMessageCounts: data?.lobbyMessageCounts || [],
      loginClientTypeDistribution: data?.loginClientTypeDistribution || []
    })
    await nextTick()
    renderCharts()
  } catch (error) {
    ElMessage.error(error?.message || '加载趋势失败')
  } finally {
    loadingTrend.value = false
  }
}

function initCharts() {
  if (trendLineChartRef.value && !trendLineChart) {
    trendLineChart = echarts.init(trendLineChartRef.value)
  }
  if (clientPieChartRef.value && !clientPieChart) {
    clientPieChart = echarts.init(clientPieChartRef.value)
  }
}

function renderCharts() {
  initCharts()
  renderTrendLineChart()
  renderClientPieChart()
}

function renderTrendLineChart() {
  if (!trendLineChart) {
    return
  }
  trendLineChart.setOption({
    color: ['#3d7ef8', '#2f9e7d', '#e49f4a', '#8f6de4', '#cf5b5b'],
    tooltip: { trigger: 'axis' },
    legend: {
      bottom: 0,
      data: ['新增用户', '发帖', '回帖', '大厅消息', '登录失败']
    },
    grid: {
      left: 36,
      right: 20,
      top: 30,
      bottom: 40
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: trend.dateLabels
    },
    yAxis: {
      type: 'value',
      minInterval: 1
    },
    series: [
      {
        name: '新增用户',
        type: 'line',
        smooth: true,
        data: trend.registerCounts
      },
      {
        name: '发帖',
        type: 'line',
        smooth: true,
        data: trend.threadCounts
      },
      {
        name: '回帖',
        type: 'line',
        smooth: true,
        data: trend.replyCounts
      },
      {
        name: '大厅消息',
        type: 'line',
        smooth: true,
        data: trend.lobbyMessageCounts
      },
      {
        name: '登录失败',
        type: 'line',
        smooth: true,
        data: trend.loginFailCounts
      }
    ]
  })
}

function renderClientPieChart() {
  if (!clientPieChart) {
    return
  }
  const distribution = trend.loginClientTypeDistribution || []
  clientPieChart.setOption({
    color: ['#3d7ef8', '#2f9e7d', '#e49f4a', '#8f6de4', '#cf5b5b', '#4f657d'],
    title: {
      text: '登录客户端分布',
      left: 'center',
      top: 10,
      textStyle: {
        fontSize: 14,
        fontWeight: 600
      }
    },
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)'
    },
    legend: {
      type: 'scroll',
      orient: 'vertical',
      right: 8,
      top: 36,
      bottom: 12
    },
    series: [
      {
        type: 'pie',
        radius: ['42%', '68%'],
        center: ['35%', '57%'],
        label: {
          formatter: '{b}\n{c}'
        },
        data: distribution.map(item => ({
          name: item.clientType,
          value: Number(item.count || 0)
        }))
      }
    ]
  })
}

function handleResize() {
  trendLineChart?.resize()
  clientPieChart?.resize()
}

onMounted(async () => {
  await loadOverview()
  await loadTrend()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  trendLineChart?.dispose()
  clientPieChart?.dispose()
  trendLineChart = null
  clientPieChart = null
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

.section-header {
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
}

.quick-overview {
  margin-bottom: 10px;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
}

.quick-item {
  padding: 8px 10px;
  border: 1px solid #e0e5eb;
  border-radius: 4px;
  background: #f7fafc;
}

.quick-label {
  font-size: 12px;
  color: #607285;
}

.quick-value {
  margin-top: 4px;
  font-size: 20px;
  font-weight: 700;
  color: #1f3d8a;
}

.chart-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 10px;
}

.chart-panel {
  height: 320px;
  border: 1px solid #e0e5eb;
  border-radius: 4px;
  background: #fff;
}
</style>
