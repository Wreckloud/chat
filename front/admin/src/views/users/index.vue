<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="title">用户管理</h2>
      <p class="subtitle">管理系统中的所有用户</p>
    </div>
    
    <!-- 搜索栏 -->
    <div class="search-bar wolf-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="用户名">
          <el-input
            v-model="searchForm.username"
            placeholder="请输入用户名"
            clearable
          />
        </el-form-item>
        <el-form-item label="WF号">
          <el-input
            v-model="searchForm.wfNo"
            placeholder="请输入WF号"
            clearable
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">
            搜索
          </el-button>
          <el-button :icon="RefreshRight" @click="handleReset">
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </div>
    
    <!-- 用户列表 -->
    <div class="user-list wolf-card mt-20">
      <el-table
        v-loading="loading"
        :data="userList"
        stripe
        style="width: 100%"
      >
        <el-table-column prop="id" label="用户ID" width="80" />
        <el-table-column prop="wfNo" label="WF号" width="120" />
        <el-table-column label="用户名" width="150">
          <template #default="{ row }">
            <div class="user-info">
              <el-avatar :size="32" :src="row.avatar">
                {{ row.username?.charAt(0) }}
              </el-avatar>
              <span class="ml-10">{{ row.username }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="mobile" label="手机号" width="130" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="注册时间" width="180" />
        <el-table-column label="操作" fixed="right" width="150">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 1"
              type="danger"
              size="small"
              text
              @click="handleDisable(row)"
            >
              禁用
            </el-button>
            <el-button
              v-else
              type="success"
              size="small"
              text
              @click="handleEnable(row)"
            >
              启用
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 分页 -->
      <div class="pagination mt-20">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadUserList"
          @current-change="loadUserList"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, RefreshRight } from '@element-plus/icons-vue'
import { getUserList, disableUser, enableUser } from '@/api/user'
import logger from '@/utils/logger'

// 搜索表单
const searchForm = reactive({
  username: '',
  wfNo: ''
})

// 用户列表
const userList = ref([])

// 加载状态
const loading = ref(false)

// 分页信息
const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
})

/**
 * 加载用户列表
 */
const loadUserList = async () => {
  logger.info('UsersPage', '加载用户列表', { page: pagination.page })
  
  loading.value = true
  
  try {
    // 构建搜索关键词（用户名或WF号）
    const keyword = searchForm.username || searchForm.wfNo || undefined
    
    const res = await getUserList({
      current: pagination.page,
      size: pagination.size,
      keyword: keyword,
      status: undefined // 可选：状态筛选
    })
    
    userList.value = res.records || []
    pagination.total = res.total || 0
    
    logger.info('UsersPage', '用户列表加载成功', { count: userList.value.length })
  } catch (error) {
    logger.error('UsersPage', '加载用户列表失败', error)
    ElMessage.error('加载用户列表失败：' + (error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

/**
 * 搜索
 */
const handleSearch = () => {
  logger.action('UsersPage', 'search', searchForm)
  pagination.page = 1
  loadUserList()
}

/**
 * 重置
 */
const handleReset = () => {
  logger.action('UsersPage', 'reset')
  searchForm.username = ''
  searchForm.wfNo = ''
  pagination.page = 1
  loadUserList()
}

/**
 * 禁用用户
 */
const handleDisable = (row) => {
  ElMessageBox.confirm(`确定要禁用用户 "${row.username}" 吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    logger.action('UsersPage', 'disableUser', { userId: row.id })
    
    try {
      await disableUser(row.id, '管理员操作')
      ElMessage.success('禁用成功')
      loadUserList()
    } catch (error) {
      logger.error('UsersPage', '禁用用户失败', error)
      ElMessage.error('禁用用户失败：' + (error.message || '未知错误'))
    }
  })
}

/**
 * 启用用户
 */
const handleEnable = (row) => {
  logger.action('UsersPage', 'enableUser', { userId: row.id })
  
  enableUser(row.id, '管理员操作').then(() => {
    ElMessage.success('启用成功')
    loadUserList()
  }).catch((error) => {
    logger.error('UsersPage', '启用用户失败', error)
    ElMessage.error('启用用户失败：' + (error.message || '未知错误'))
  })
}

onMounted(() => {
  loadUserList()
})
</script>

<style lang="scss" scoped>
.search-bar {
  padding: 20px;
}

.user-list {
  padding: 20px;
  
  .user-info {
    display: flex;
    align-items: center;
  }
  
  .pagination {
    display: flex;
    justify-content: flex-end;
  }
}
</style>

