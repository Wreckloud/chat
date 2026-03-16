<template>
  <div class="users-page">
    <div class="page-card filter-card">
      <el-form :inline="true" :model="filters">
        <el-form-item label="关键词">
          <el-input v-model.trim="filters.keyword" placeholder="狼藉号/昵称" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" placeholder="全部" clearable>
            <el-option label="正常" value="NORMAL" />
            <el-option label="禁用" value="DISABLED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="page-card table-card">
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="userId" label="用户ID" width="90" />
        <el-table-column prop="wolfNo" label="狼藉号" width="130" />
        <el-table-column prop="nickname" label="昵称" min-width="160" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'NORMAL' ? 'success' : 'danger'">
              {{ row.status === 'NORMAL' ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="activeDayCount" label="活跃天数" width="110" />
        <el-table-column prop="lastLoginAt" label="最近登录" min-width="160" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button
              size="small"
              type="warning"
              @click="handleToggleStatus(row)">
              {{ row.status === 'NORMAL' ? '禁用' : '启用' }}
            </el-button>
            <el-button size="small" type="danger" @click="handleBan(row)">
              封禁
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager-wrap">
        <el-pagination
          background
          layout="total, prev, pager, next, sizes"
          :total="total"
          :current-page="pager.page"
          :page-size="pager.size"
          :page-sizes="[10, 20, 50]"
          @current-change="onPageChange"
          @size-change="onSizeChange" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchUserPage, updateUserStatus, createUserBan } from '@/api/user'

const loading = ref(false)
const total = ref(0)
const tableData = ref([])
const filters = reactive({
  keyword: '',
  status: ''
})
const pager = reactive({
  page: 1,
  size: 20
})

function buildQuery() {
  return {
    page: pager.page,
    size: pager.size,
    keyword: filters.keyword || undefined,
    status: filters.status || undefined
  }
}

async function loadUsers() {
  loading.value = true
  try {
    const data = await fetchUserPage(buildQuery())
    tableData.value = data?.list || []
    total.value = Number(data?.total || 0)
  } catch (error) {
    ElMessage.error(error?.message || '加载用户列表失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pager.page = 1
  loadUsers()
}

function handleReset() {
  filters.keyword = ''
  filters.status = ''
  pager.page = 1
  loadUsers()
}

function onPageChange(page) {
  pager.page = page
  loadUsers()
}

function onSizeChange(size) {
  pager.size = size
  pager.page = 1
  loadUsers()
}

async function handleToggleStatus(row) {
  const targetStatus = row.status === 'NORMAL' ? 'DISABLED' : 'NORMAL'
  try {
    await updateUserStatus(row.userId, targetStatus)
    ElMessage.success('状态更新成功')
    loadUsers()
  } catch (error) {
    ElMessage.error(error?.message || '状态更新失败')
  }
}

async function handleBan(row) {
  try {
    await ElMessageBox.confirm(`确认封禁用户 ${row.nickname || row.wolfNo}？`, '提示', { type: 'warning' })
    await createUserBan(row.userId, {
      reason: '管理端封禁',
      durationHours: 24
    })
    ElMessage.success('封禁操作已提交')
    loadUsers()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error?.message || '封禁失败')
    }
  }
}

onMounted(() => {
  loadUsers()
})
</script>

<style scoped lang="scss">
.users-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.filter-card,
.table-card {
  padding: 14px;
}

.pager-wrap {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}
</style>
