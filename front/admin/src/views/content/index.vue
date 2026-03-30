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

        <el-tab-pane label="聊天室治理" name="lobby">
          <el-table :data="lobbyRows" v-loading="loadingLobbyMessages" stripe>
            <el-table-column prop="messageId" label="消息ID" width="90" />
            <el-table-column prop="senderNickname" label="发送者" width="130" />
            <el-table-column prop="msgType" label="类型" width="120" />
            <el-table-column label="内容" min-width="280">
              <template #default="{ row }">
                <span v-if="row.msgType === 'TEXT'">{{ row.content || '-' }}</span>
                <span v-else-if="row.msgType === 'RECALL'" class="recalled-text">该消息已撤回</span>
                <span v-else>{{ row.content || row.mediaKey || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="时间" min-width="160" />
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button
                  size="small"
                  type="danger"
                  :disabled="row.recalled === true"
                  @click="recallLobby(row)">
                  {{ row.recalled ? '已撤回' : '撤回' }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pager-wrap">
            <el-pagination
              background
              layout="total, prev, pager, next, sizes"
              :total="lobbyPager.total"
              :current-page="lobbyPager.page"
              :page-size="lobbyPager.size"
              :page-sizes="[10, 20, 50]"
              @current-change="onLobbyPageChange"
              @size-change="onLobbySizeChange" />
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
  fetchLobbyMessagePage,
  updateThreadLock,
  updateThreadSticky,
  updateThreadEssence,
  deleteThread,
  deleteReply,
  recallLobbyMessage
} from '@/api/content'

const activeTab = ref('thread')
const loadingThreads = ref(false)
const loadingReplies = ref(false)
const loadingLobbyMessages = ref(false)
const threadRows = ref([])
const replyRows = ref([])
const lobbyRows = ref([])
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
const lobbyPager = reactive({
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

async function loadLobbyMessages() {
  loadingLobbyMessages.value = true
  try {
    const data = await fetchLobbyMessagePage({ page: lobbyPager.page, size: lobbyPager.size })
    lobbyRows.value = data?.list || []
    lobbyPager.total = Number(data?.total || 0)
  } catch (error) {
    ElMessage.error(error?.message || '加载聊天室消息失败')
  } finally {
    loadingLobbyMessages.value = false
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

async function recallLobby(row) {
  if (row.recalled === true) {
    return
  }
  try {
    await recallLobbyMessage(row.messageId)
    ElMessage.success('撤回成功')
    loadLobbyMessages()
  } catch (error) {
    ElMessage.error(error?.message || '撤回失败')
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

function onLobbyPageChange(page) {
  lobbyPager.page = page
  loadLobbyMessages()
}

function onLobbySizeChange(size) {
  lobbyPager.size = size
  lobbyPager.page = 1
  loadLobbyMessages()
}

watch(activeTab, (tab) => {
  if (tab === 'thread') {
    loadThreads()
    return
  }
  if (tab === 'reply') {
    loadReplies()
    return
  }
  loadLobbyMessages()
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

.recalled-text {
  color: #7a7f8a;
}
</style>
