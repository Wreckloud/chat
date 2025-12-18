<template>
  <div class="page-container">
    <!-- 返回按钮 -->
    <el-button :icon="ArrowLeft" @click="goBack" class="mb-20">
      返回列表
    </el-button>
    
    <div v-loading="loading">
      <!-- 群组基本信息 -->
      <div class="group-detail-card wolf-card">
        <h3 class="section-title">群组信息</h3>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="群组ID">
            {{ groupDetail.groupId }}
          </el-descriptions-item>
          <el-descriptions-item label="群名称">
            {{ groupDetail.groupName }}
          </el-descriptions-item>
          <el-descriptions-item label="群简介" :span="2">
            {{ groupDetail.groupIntro || '暂无简介' }}
          </el-descriptions-item>
          <el-descriptions-item label="群主">
            {{ groupDetail.ownerName || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="成员数量">
            {{ groupDetail.memberCount }} / {{ groupDetail.maxMembers }}
          </el-descriptions-item>
          <el-descriptions-item label="全员禁言">
            <el-tag :type="groupDetail.isAllMuted ? 'warning' : 'success'">
              {{ groupDetail.isAllMuted ? '是' : '否' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="需要审批">
            <el-tag :type="groupDetail.isNeedApproval ? 'warning' : 'success'">
              {{ groupDetail.isNeedApproval ? '是' : '否' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">
            {{ groupDetail.createTime }}
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="groupDetail.status === 1 ? 'success' : 'info'">
              {{ groupDetail.status === 1 ? '正常' : '已解散' }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>
      </div>
      
      <!-- 群成员列表 -->
      <div class="group-members wolf-card mt-20">
        <h3 class="section-title">群成员列表</h3>
        <el-table
          :data="memberList"
          stripe
          style="width: 100%"
        >
          <el-table-column prop="userId" label="用户ID" width="80" />
          <el-table-column label="用户名" width="200">
            <template #default="{ row }">
              <div class="user-info">
                <el-avatar :size="32" :src="row.avatar">
                  {{ row.username?.charAt(0) }}
                </el-avatar>
                <span class="ml-10">{{ row.username }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="nickname" label="群昵称" />
          <el-table-column label="角色" width="100">
            <template #default="{ row }">
              <el-tag :type="getRoleType(row.role)">
                {{ getRoleText(row.role) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="禁言" width="80">
            <template #default="{ row }">
              <el-tag :type="row.isMuted ? 'danger' : 'success'">
                {{ row.isMuted ? '是' : '否' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="joinTime" label="加入时间" width="180" />
          <el-table-column label="操作" fixed="right" width="120">
            <template #default="{ row }">
              <el-button
                v-if="row.role !== 1"
                type="danger"
                size="small"
                text
                @click="handleRemoveMember(row)"
              >
                移除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
      
      <!-- 群公告列表 -->
      <div class="group-notices wolf-card mt-20">
        <h3 class="section-title">群公告</h3>
        <el-table
          :data="noticeList"
          stripe
          style="width: 100%"
        >
          <el-table-column prop="noticeId" label="公告ID" width="80" />
          <el-table-column prop="content" label="公告内容" show-overflow-tooltip />
          <el-table-column prop="publisherName" label="发布人" width="120" />
          <el-table-column label="是否置顶" width="100">
            <template #default="{ row }">
              <el-tag :type="row.isPinned ? 'warning' : ''">
                {{ row.isPinned ? '是' : '否' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="publishTime" label="发布时间" width="180" />
          <el-table-column label="操作" fixed="right" width="100">
            <template #default="{ row }">
              <el-button
                type="danger"
                size="small"
                text
                @click="handleDeleteNotice(row)"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { getGroupDetail, getGroupMembers, getGroupNotices, removeGroupMember, deleteGroupNotice } from '@/api/group'
import logger from '@/utils/logger'

const router = useRouter()
const route = useRoute()

// 群组ID
const groupId = ref(route.params.id)

// 加载状态
const loading = ref(false)

// 群组详情
const groupDetail = ref({})

// 成员列表
const memberList = ref([])

// 公告列表
const noticeList = ref([])

/**
 * 加载群组详情
 */
const loadGroupDetail = async () => {
  logger.info('GroupDetailPage', '加载群组详情', { groupId: groupId.value })
  
  loading.value = true
  
  try {
    const detail = await getGroupDetail(groupId.value)
    groupDetail.value = detail
    
    logger.info('GroupDetailPage', '群组详情加载成功')
  } catch (error) {
    logger.error('GroupDetailPage', '加载群组详情失败', error)
    // 接口不存在时会报404，不使用假数据
  } finally {
    loading.value = false
  }
}

/**
 * 加载成员列表
 */
const loadMemberList = async () => {
  try {
    const members = await getGroupMembers(groupId.value)
    memberList.value = members
  } catch (error) {
    logger.error('GroupDetailPage', '加载成员列表失败', error)
    // 接口不存在时会报404，不使用假数据
  }
}

/**
 * 加载公告列表
 */
const loadNoticeList = async () => {
  try {
    const notices = await getGroupNotices(groupId.value)
    noticeList.value = notices
  } catch (error) {
    logger.error('GroupDetailPage', '加载公告列表失败', error)
    // 接口不存在时会报404，不使用假数据
  }
}

/**
 * 获取角色类型
 */
const getRoleType = (role) => {
  const types = { 1: 'danger', 2: 'warning', 3: '' }
  return types[role] || ''
}

/**
 * 获取角色文本
 */
const getRoleText = (role) => {
  const texts = { 1: '群主', 2: '管理员', 3: '成员' }
  return texts[role] || '未知'
}

/**
 * 移除成员
 */
const handleRemoveMember = (row) => {
  ElMessageBox.confirm(`确定要移除成员 "${row.username}" 吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    logger.action('GroupDetailPage', 'removeMember', { userId: row.userId, groupId: groupId.value })
    
    try {
      await removeGroupMember(groupId.value, row.userId)
      ElMessage.success('移除成功')
      loadMemberList()
    } catch (error) {
      logger.error('GroupDetailPage', '移除成员失败', error)
    }
  })
}

/**
 * 删除公告
 */
const handleDeleteNotice = (row) => {
  ElMessageBox.confirm('确定要删除这条公告吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    logger.action('GroupDetailPage', 'deleteNotice', { noticeId: row.noticeId, groupId: groupId.value })
    
    try {
      await deleteGroupNotice(groupId.value, row.noticeId)
      ElMessage.success('删除成功')
      loadNoticeList()
    } catch (error) {
      logger.error('GroupDetailPage', '删除公告失败', error)
    }
  })
}

/**
 * 返回列表
 */
const goBack = () => {
  router.back()
}

onMounted(() => {
  loadGroupDetail()
  loadMemberList()
  loadNoticeList()
})
</script>

<style lang="scss" scoped>
.group-detail-card,
.group-members,
.group-notices {
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
  
  .user-info {
    display: flex;
    align-items: center;
  }
}
</style>

