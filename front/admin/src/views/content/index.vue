<template>
  <div class="content-page">
    <div class="page-card table-card">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="主题治理" name="thread">
          <el-table :data="threadRows" v-loading="loadingThreads" stripe>
            <el-table-column prop="threadId" label="主题ID" width="90" />
            <el-table-column prop="title" label="标题" min-width="220" />
            <el-table-column prop="authorNickname" label="作者" width="130" />
            <el-table-column prop="status" label="状态" width="100" />
            <el-table-column prop="replyCount" label="回复" width="80" />
            <el-table-column prop="likeCount" label="点赞" width="80" />
            <el-table-column label="操作" width="330" fixed="right">
              <template #default="{ row }">
                <el-button size="small" @click="toggleLock(row)">锁帖</el-button>
                <el-button size="small" :disabled="row.threadType === 'ANNOUNCEMENT'" @click="toggleSticky(row)">置顶</el-button>
                <el-button size="small" @click="toggleEssence(row)">精华</el-button>
                <el-button size="small" type="danger" @click="removeThread(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pager-wrap">
            <el-pagination
              background
              layout="total, prev, pager, next, sizes"
              :total="threadPager.total"
              :current-page="threadPager.page"
              :page-size="threadPager.size"
              :page-sizes="[10, 20, 50]"
              @current-change="onThreadPageChange"
              @size-change="onThreadSizeChange" />
          </div>
        </el-tab-pane>

        <el-tab-pane label="回复治理" name="reply">
          <el-table :data="replyRows" v-loading="loadingReplies" stripe>
            <el-table-column prop="replyId" label="回复ID" width="90" />
            <el-table-column prop="threadId" label="主题ID" width="90" />
            <el-table-column prop="authorNickname" label="作者" width="130" />
            <el-table-column prop="content" label="内容" min-width="280" />
            <el-table-column prop="likeCount" label="点赞" width="80" />
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button size="small" type="danger" @click="removeReply(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pager-wrap">
            <el-pagination
              background
              layout="total, prev, pager, next, sizes"
              :total="replyPager.total"
              :current-page="replyPager.page"
              :page-size="replyPager.size"
              :page-sizes="[10, 20, 50]"
              @current-change="onReplyPageChange"
              @size-change="onReplySizeChange" />
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
  fetchThreadPage,
  fetchReplyPage,
  updateThreadLock,
  updateThreadSticky,
  updateThreadEssence,
  deleteThread,
  deleteReply
} from '@/api/content'

const activeTab = ref('thread')
const loadingThreads = ref(false)
const loadingReplies = ref(false)
const threadRows = ref([])
const replyRows = ref([])
const threadPager = reactive({
  page: 1,
  size: 20,
  total: 0
})
const replyPager = reactive({
  page: 1,
  size: 20,
  total: 0
})

async function loadThreads() {
  loadingThreads.value = true
  try {
    const data = await fetchThreadPage({ page: threadPager.page, size: threadPager.size })
    threadRows.value = data?.list || []
    threadPager.total = Number(data?.total || 0)
  } catch (error) {
    ElMessage.error(error?.message || '加载主题失败')
  } finally {
    loadingThreads.value = false
  }
}

async function loadReplies() {
  loadingReplies.value = true
  try {
    const data = await fetchReplyPage({ page: replyPager.page, size: replyPager.size })
    replyRows.value = data?.list || []
    replyPager.total = Number(data?.total || 0)
  } catch (error) {
    ElMessage.error(error?.message || '加载回复失败')
  } finally {
    loadingReplies.value = false
  }
}

async function toggleLock(row) {
  try {
    await updateThreadLock(row.threadId, row.status !== 'LOCKED')
    ElMessage.success('操作成功')
    loadThreads()
  } catch (error) {
    ElMessage.error(error?.message || '操作失败')
  }
}

async function toggleSticky(row) {
  if (row.threadType === 'ANNOUNCEMENT') {
    return
  }
  try {
    await updateThreadSticky(row.threadId, row.threadType !== 'STICKY')
    ElMessage.success('操作成功')
    loadThreads()
  } catch (error) {
    ElMessage.error(error?.message || '操作失败')
  }
}

async function toggleEssence(row) {
  try {
    await updateThreadEssence(row.threadId, row.isEssence !== true)
    ElMessage.success('操作成功')
    loadThreads()
  } catch (error) {
    ElMessage.error(error?.message || '操作失败')
  }
}

async function removeThread(row) {
  try {
    await deleteThread(row.threadId)
    ElMessage.success('删除成功')
    if (threadRows.value.length === 1 && threadPager.page > 1) {
      threadPager.page -= 1
    }
    loadThreads()
  } catch (error) {
    ElMessage.error(error?.message || '删除失败')
  }
}

async function removeReply(row) {
  try {
    await deleteReply(row.replyId)
    ElMessage.success('删除成功')
    if (replyRows.value.length === 1 && replyPager.page > 1) {
      replyPager.page -= 1
    }
    loadReplies()
  } catch (error) {
    ElMessage.error(error?.message || '删除失败')
  }
}

function onThreadPageChange(page) {
  threadPager.page = page
  loadThreads()
}

function onThreadSizeChange(size) {
  threadPager.size = size
  threadPager.page = 1
  loadThreads()
}

function onReplyPageChange(page) {
  replyPager.page = page
  loadReplies()
}

function onReplySizeChange(size) {
  replyPager.size = size
  replyPager.page = 1
  loadReplies()
}

watch(activeTab, (tab) => {
  if (tab === 'thread') {
    loadThreads()
    return
  }
  loadReplies()
})

onMounted(() => {
  loadThreads()
})
</script>

<style scoped lang="scss">
.content-page {
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
