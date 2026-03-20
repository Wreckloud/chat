<template>
  <div class="audit-page">
    <div class="page-card table-card">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="管理员操作日志" name="action">
          <el-table :data="actionRows" v-loading="loadingActions" stripe>
            <el-table-column prop="actionLogId" label="日志ID" width="90" />
            <el-table-column prop="operatorName" label="操作人" width="120" />
            <el-table-column prop="actionType" label="操作类型" width="140" />
            <el-table-column prop="targetType" label="目标类型" width="120" />
            <el-table-column prop="targetId" label="目标ID" width="100" />
            <el-table-column prop="detail" label="详情" min-width="220" />
            <el-table-column prop="createTime" label="时间" min-width="160" />
          </el-table>
          <div class="pager-wrap">
            <el-pagination
              background
              layout="total, prev, pager, next, sizes"
              :total="actionPager.total"
              :current-page="actionPager.page"
              :page-size="actionPager.size"
              :page-sizes="[10, 20, 50]"
              @current-change="onActionPageChange"
              @size-change="onActionSizeChange" />
          </div>
        </el-tab-pane>
        <el-tab-pane label="登录日志" name="login">
          <div class="risk-overview">
            <div class="risk-item">
              <div class="risk-label">账号锁定</div>
              <div class="risk-value">{{ riskOverview.accountLockCount }}</div>
            </div>
            <div class="risk-item">
              <div class="risk-label">IP锁定</div>
              <div class="risk-value">{{ riskOverview.ipLockCount }}</div>
            </div>
            <div class="risk-item">
              <div class="risk-label">账号失败桶</div>
              <div class="risk-value">{{ riskOverview.accountFailBucketCount }}</div>
            </div>
            <div class="risk-item">
              <div class="risk-label">IP失败桶</div>
              <div class="risk-value">{{ riskOverview.ipFailBucketCount }}</div>
            </div>
            <el-button :loading="loadingRiskOverview" @click="loadLoginRiskOverview">刷新风控</el-button>
          </div>

          <div class="risk-check-wrap">
            <el-input v-model="riskQuery.account" placeholder="输入账号（狼藉号或邮箱）" clearable />
            <el-input v-model="riskQuery.ip" placeholder="输入IP" clearable />
            <el-button type="primary" :loading="checkingRisk" @click="handleCheckRisk">检测</el-button>
          </div>
          <div v-if="riskCheck" class="risk-check-result">
            <span>账号锁定: <b>{{ riskCheck.accountLocked ? '是' : '否' }}</b></span>
            <span>账号锁定剩余: <b>{{ riskCheck.accountLockTtlSeconds }}s</b></span>
            <span>账号失败次数: <b>{{ riskCheck.accountFailCount }}</b></span>
            <span>IP锁定: <b>{{ riskCheck.ipLocked ? '是' : '否' }}</b></span>
            <span>IP锁定剩余: <b>{{ riskCheck.ipLockTtlSeconds }}s</b></span>
            <span>IP失败次数: <b>{{ riskCheck.ipFailCount }}</b></span>
          </div>

          <el-table :data="loginRows" v-loading="loadingLogins" stripe>
            <el-table-column prop="logId" label="日志ID" width="90" />
            <el-table-column prop="accountMask" label="账号" min-width="180" />
            <el-table-column prop="loginMethod" label="方式" width="110" />
            <el-table-column prop="loginResult" label="结果" width="100" />
            <el-table-column prop="ip" label="IP" width="140" />
            <el-table-column prop="clientType" label="客户端" width="140" />
            <el-table-column prop="loginTime" label="时间" min-width="160" />
          </el-table>
          <div class="pager-wrap">
            <el-pagination
              background
              layout="total, prev, pager, next, sizes"
              :total="loginPager.total"
              :current-page="loginPager.page"
              :page-size="loginPager.size"
              :page-sizes="[10, 20, 50]"
              @current-change="onLoginPageChange"
              @size-change="onLoginSizeChange" />
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  checkLoginRisk,
  fetchActionLogPage,
  fetchLoginLogPage,
  fetchLoginRiskOverview
} from '@/api/audit'

const activeTab = ref('action')
const loadingActions = ref(false)
const loadingLogins = ref(false)
const loadingRiskOverview = ref(false)
const checkingRisk = ref(false)
const actionRows = ref([])
const loginRows = ref([])
const riskCheck = ref(null)
const riskOverview = reactive({
  accountLockCount: 0,
  ipLockCount: 0,
  accountFailBucketCount: 0,
  ipFailBucketCount: 0
})
const riskQuery = reactive({
  account: '',
  ip: ''
})
const actionPager = reactive({
  page: 1,
  size: 20,
  total: 0
})
const loginPager = reactive({
  page: 1,
  size: 20,
  total: 0
})

async function loadActionLogs() {
  loadingActions.value = true
  try {
    const data = await fetchActionLogPage({ page: actionPager.page, size: actionPager.size })
    actionRows.value = data?.list || []
    actionPager.total = Number(data?.total || 0)
  } catch (error) {
    ElMessage.error(error?.message || '加载操作日志失败')
  } finally {
    loadingActions.value = false
  }
}

async function loadLoginLogs() {
  loadingLogins.value = true
  try {
    const data = await fetchLoginLogPage({ page: loginPager.page, size: loginPager.size })
    loginRows.value = data?.list || []
    loginPager.total = Number(data?.total || 0)
  } catch (error) {
    ElMessage.error(error?.message || '加载登录日志失败')
  } finally {
    loadingLogins.value = false
  }
}

async function loadLoginRiskOverview() {
  loadingRiskOverview.value = true
  try {
    const data = await fetchLoginRiskOverview()
    riskOverview.accountLockCount = Number(data?.accountLockCount || 0)
    riskOverview.ipLockCount = Number(data?.ipLockCount || 0)
    riskOverview.accountFailBucketCount = Number(data?.accountFailBucketCount || 0)
    riskOverview.ipFailBucketCount = Number(data?.ipFailBucketCount || 0)
  } catch (error) {
    ElMessage.error(error?.message || '加载风控总览失败')
  } finally {
    loadingRiskOverview.value = false
  }
}

async function handleCheckRisk() {
  checkingRisk.value = true
  try {
    const data = await checkLoginRisk({
      account: riskQuery.account || '',
      ip: riskQuery.ip || ''
    })
    riskCheck.value = data || null
  } catch (error) {
    ElMessage.error(error?.message || '检测失败')
  } finally {
    checkingRisk.value = false
  }
}

watch(activeTab, (tab) => {
  if (tab === 'action') {
    loadActionLogs()
    return
  }
  loadLoginLogs()
  loadLoginRiskOverview()
})

function onActionPageChange(page) {
  actionPager.page = page
  loadActionLogs()
}

function onActionSizeChange(size) {
  actionPager.size = size
  actionPager.page = 1
  loadActionLogs()
}

function onLoginPageChange(page) {
  loginPager.page = page
  loadLoginLogs()
}

function onLoginSizeChange(size) {
  loginPager.size = size
  loginPager.page = 1
  loadLoginLogs()
}

onMounted(() => {
  loadActionLogs()
})
</script>

<style scoped lang="scss">
.audit-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.table-card {
  padding: 14px;
}

.pager-wrap {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}

.risk-overview {
  margin-bottom: 12px;
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 8px;
}

.risk-item {
  padding: 8px 10px;
  border: 1px solid var(--line-color, #d7d7d7);
  background: #fff;
}

.risk-label {
  font-size: 12px;
  color: #7a7f8a;
}

.risk-value {
  margin-top: 4px;
  font-size: 18px;
  font-weight: 700;
  color: #1f3d8a;
}

.risk-check-wrap {
  margin-bottom: 8px;
  display: grid;
  grid-template-columns: 1.5fr 1fr auto;
  gap: 8px;
}

.risk-check-result {
  margin-bottom: 12px;
  padding: 8px 10px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 6px 10px;
  border: 1px solid var(--line-color, #d7d7d7);
  background: #fff;
  font-size: 12px;
  color: #4d5566;
}
</style>
