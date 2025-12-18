<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="title">群组管理</h2>
      <p class="subtitle">管理系统中的所有群组</p>
    </div>
    
    <!-- 搜索栏 -->
    <div class="search-bar wolf-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="群名称">
          <el-input
            v-model="searchForm.groupName"
            placeholder="请输入群名称"
            clearable
          />
        </el-form-item>
        <el-form-item label="群状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable>
            <el-option label="正常" :value="1" />
            <el-option label="已解散" :value="2" />
          </el-select>
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
    
    <!-- 群组列表 -->
    <div class="group-list wolf-card mt-20">
      <el-table
        v-loading="loading"
        :data="groupList"
        stripe
        style="width: 100%"
      >
        <el-table-column prop="id" label="群组ID" width="80" />
        <el-table-column label="群名称" width="200">
          <template #default="{ row }">
            <div class="group-info">
              <el-avatar :size="32" :src="row.groupAvatar">
                {{ row.groupName?.charAt(0) }}
              </el-avatar>
              <span class="ml-10">{{ row.groupName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="groupIntro" label="群简介" show-overflow-tooltip />
        <el-table-column prop="memberCount" label="成员数" width="100">
          <template #default="{ row }">
            <el-tag>{{ row.memberCount }} / {{ row.maxMembers }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '正常' : '已解散' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" fixed="right" width="150">
          <template #default="{ row }">
            <el-button
              type="primary"
              size="small"
              text
              @click="handleView(row)"
            >
              查看
            </el-button>
            <el-button
              v-if="row.status === 1"
              type="danger"
              size="small"
              text
              @click="handleDismiss(row)"
            >
              解散
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
          @size-change="loadGroupList"
          @current-change="loadGroupList"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, RefreshRight } from '@element-plus/icons-vue'
import { getAdminGroupList, adminDismissGroup } from '@/api/admin'
import logger from '@/utils/logger'

const router = useRouter()

// 搜索表单
const searchForm = reactive({
  groupName: '',
  status: null
})

// 群组列表
const groupList = ref([])

// 加载状态
const loading = ref(false)

// 分页信息
const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
})

/**
 * 加载群组列表
 */
const loadGroupList = async () => {
  logger.info('GroupsPage', '加载群组列表', { page: pagination.page })
  
  loading.value = true
  
  try {
    const res = await getAdminGroupList({
      current: pagination.page,
      size: pagination.size,
      keyword: searchForm.groupName || undefined,
      status: searchForm.status
    })
    
    groupList.value = res.records || []
    pagination.total = res.total || 0
    
    logger.info('GroupsPage', '群组列表加载成功', { count: groupList.value.length })
  } catch (error) {
    logger.error('GroupsPage', '加载群组列表失败', error)
    ElMessage.error('加载群组列表失败：' + (error.message || '未知错误'))
    groupList.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

/**
 * 搜索
 */
const handleSearch = () => {
  logger.action('GroupsPage', 'search', searchForm)
  pagination.page = 1
  loadGroupList()
}

/**
 * 重置
 */
const handleReset = () => {
  logger.action('GroupsPage', 'reset')
  searchForm.groupName = ''
  searchForm.status = null
  pagination.page = 1
  loadGroupList()
}

/**
 * 查看群组详情
 */
const handleView = (row) => {
  logger.action('GroupsPage', 'viewGroup', { groupId: row.id })
  router.push(`/groups/${row.id}`)
}

/**
 * 解散群组
 */
const handleDismiss = (row) => {
  ElMessageBox.confirm(`确定要解散群组 "${row.groupName}" 吗？此操作不可恢复！`, '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    logger.action('GroupsPage', 'dismissGroup', { groupId: row.id })
    
    try {
      await adminDismissGroup({ 
        groupId: row.id, 
        reason: '管理员操作' 
      })
      ElMessage.success('解散成功')
      loadGroupList()
    } catch (error) {
      logger.error('GroupsPage', '解散群组失败', error)
      ElMessage.error('解散群组失败：' + (error.message || '未知错误'))
    }
  })
}

onMounted(() => {
  loadGroupList()
})
</script>

<style lang="scss" scoped>
.search-bar {
  padding: 20px;
}

.group-list {
  padding: 20px;
  
  .group-info {
    display: flex;
    align-items: center;
  }
  
  .pagination {
    display: flex;
    justify-content: flex-end;
  }
}
</style>

