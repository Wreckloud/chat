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
import { fetchActionLogPage, fetchLoginLogPage } from '@/api/audit'

const activeTab = ref('action')
const loadingActions = ref(false)
const loadingLogins = ref(false)
const actionRows = ref([])
const loginRows = ref([])
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

watch(activeTab, (tab) => {
  if (tab === 'action') {
    loadActionLogs()
    return
  }
  loadLoginLogs()
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
</style>
