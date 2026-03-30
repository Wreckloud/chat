<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import {
  deleteThread,
  listMyThreads,
  listUserThreads,
  purgeThread
} from '@/api/modules'
import { mapThreadList, mergePageList, resolveHasMore } from '@/utils/forum'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const userId = computed(() => Number(route.params.userId || 0))
const currentUserId = computed(() => {
  const queryUserId = Number(route.query.currentUserId || 0)
  if (queryUserId > 0) {
    return queryUserId
  }
  return Number(authStore.userId || 0)
})
const isSelf = computed(() => userId.value > 0 && userId.value === currentUserId.value)

const tabs = [
  { key: 'mine', label: '我的' },
  { key: 'draft', label: '草稿' },
  { key: 'trash', label: '垃圾站' }
]

const activeTab = ref('mine')
const keyword = ref('')
const page = ref(1)
const size = 20
const total = ref(0)
const hasMore = ref(true)
const loading = ref(false)
const loadingMore = ref(false)
const errorMessage = ref('')
const threads = ref([])

const pageTitle = computed(() => (isSelf.value ? '我的帖子' : '行者帖子'))

function getPreviewMedia(item) {
  if (item.videoPosterUrl) {
    return item.videoPosterUrl
  }
  if (item.hasSingleImagePreview) {
    return item.singleImagePreviewUrl
  }
  if (Array.isArray(item.previewImageUrls) && item.previewImageUrls.length > 0) {
    return item.previewImageUrls[0]
  }
  return ''
}

async function loadThreads(reset = false) {
  if (loading.value || loadingMore.value) {
    return
  }
  if (!reset && !hasMore.value) {
    return
  }
  if (reset) {
    loading.value = true
    page.value = 1
    hasMore.value = true
  } else {
    loadingMore.value = true
  }
  errorMessage.value = ''

  try {
    const targetPage = reset ? 1 : page.value
    let data
    if (isSelf.value) {
      data = await listMyThreads(activeTab.value, keyword.value, targetPage, size)
    } else {
      data = await listUserThreads(userId.value, targetPage, size)
    }
    const nextList = mapThreadList(Array.isArray(data?.list) ? data.list : [])
    threads.value = mergePageList(threads.value, nextList, reset)
    total.value = Number(data?.total || threads.value.length)
    hasMore.value = resolveHasMore(total.value, threads.value.length)
    page.value = hasMore.value ? targetPage + 1 : targetPage
  } catch (error) {
    errorMessage.value = error.message || '加载帖子失败'
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

function openThread(thread) {
  const threadId = Number(thread?.threadId || 0)
  if (!threadId) {
    return
  }
  if (isSelf.value && activeTab.value === 'draft') {
    router.push(`/post/create?mode=draft&threadId=${threadId}`)
    return
  }
  if (isSelf.value && activeTab.value === 'trash') {
    return
  }
  router.push(`/post/${threadId}`)
}

function switchTab(tabKey) {
  if (!isSelf.value || activeTab.value === tabKey) {
    return
  }
  activeTab.value = tabKey
}

async function removeThread(thread) {
  const threadId = Number(thread?.threadId || 0)
  if (!threadId) {
    return
  }
  const confirmed = window.confirm('删除后将进入垃圾站，确认删除吗？')
  if (!confirmed) {
    return
  }
  try {
    await deleteThread(threadId)
    await loadThreads(true)
  } catch (error) {
    errorMessage.value = error.message || '删除主题失败'
  }
}

function editThread(thread) {
  const threadId = Number(thread?.threadId || 0)
  if (!threadId) {
    return
  }
  router.push(`/post/create?mode=edit&threadId=${threadId}`)
}

function restoreDeletedThread(thread) {
  const threadId = Number(thread?.threadId || 0)
  if (!threadId) {
    return
  }
  router.push(`/post/create?mode=restore&threadId=${threadId}`)
}

async function purgeDeletedThread(thread) {
  const threadId = Number(thread?.threadId || 0)
  if (!threadId) {
    return
  }
  const confirmed = window.confirm('彻底删除后将不会再出现在你的列表中，确认继续吗？')
  if (!confirmed) {
    return
  }
  try {
    await purgeThread(threadId)
    await loadThreads(true)
  } catch (error) {
    errorMessage.value = error.message || '彻底删除失败'
  }
}

watch(activeTab, () => {
  keyword.value = ''
  loadThreads(true)
})

onMounted(() => {
  if (!userId.value) {
    errorMessage.value = '用户参数缺失'
    return
  }
  loadThreads(true)
})
</script>

<template>
  <section class="user-posts-page">
    <header class="page-header">
      <button class="button page-action-btn" @click="router.back()">返回</button>
      <h1 class="page-title">{{ pageTitle }}</h1>
      <button class="button page-action-btn" :disabled="loading || loadingMore" @click="loadThreads(true)">刷新</button>
    </header>

    <div class="page-body">
      <div v-if="isSelf" class="retro-tab-row retro-tab-row-fill">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          :class="['button retro-tab-btn', { active: activeTab === tab.key }]"
          @click="switchTab(tab.key)"
        >
          {{ tab.label }}
        </button>
      </div>

      <div v-if="isSelf" class="retro-search-row">
        <input
          v-model="keyword"
          class="input search-input retro-search-input"
          placeholder="搜索帖子"
          @keyup.enter="loadThreads(true)"
        />
        <button class="button" :disabled="loading || loadingMore" @click="loadThreads(true)">搜索</button>
      </div>

      <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
      <p v-if="loading && threads.length === 0" class="text-muted">加载中...</p>
      <div v-if="!loading && threads.length === 0" class="empty">暂无帖子</div>

      <article
        v-for="thread in threads"
        :key="thread.threadId"
        class="card retro-list-card thread-card"
        @click="openThread(thread)"
      >
        <h3 class="thread-title">{{ thread.title }}</h3>
        <img v-if="getPreviewMedia(thread)" :src="getPreviewMedia(thread)" alt="" class="thread-media" />
        <p class="thread-preview">{{ thread.contentPreview || '暂无正文' }}</p>
        <div class="thread-meta">
          <span>{{ thread.timePrefix }} {{ thread.timeRelativeText }}</span>
          <span>回 {{ thread.replyCount || 0 }} / 阅 {{ thread.viewCount || 0 }} / 赞 {{ thread.likeCount || 0 }}</span>
        </div>
        <div v-if="isSelf" class="thread-actions" @click.stop>
          <template v-if="activeTab === 'mine'">
            <button class="button action-btn" @click="editThread(thread)">编辑</button>
            <button class="button action-btn danger" @click="removeThread(thread)">删除</button>
          </template>
          <template v-else-if="activeTab === 'trash'">
            <button class="button action-btn" @click="restoreDeletedThread(thread)">重新发布</button>
            <button class="button action-btn danger" @click="purgeDeletedThread(thread)">彻底删除</button>
          </template>
        </div>
      </article>

      <button v-if="hasMore && threads.length > 0" class="button load-more" :disabled="loadingMore" @click="loadThreads(false)">
        {{ loadingMore ? '加载中...' : '加载更多' }}
      </button>
    </div>
  </section>
</template>

<style scoped>
.user-posts-page {
  min-height: 100%;
}

.thread-card {
  cursor: pointer;
}

.thread-title {
  margin: 0 0 8px;
  font-size: 16px;
}

.thread-media {
  width: 100%;
  max-height: 220px;
  object-fit: cover;
  border: 1px solid var(--line);
  margin-bottom: 8px;
}

.thread-preview {
  margin: 0 0 8px;
  color: var(--text-sub);
  line-height: 1.6;
}

.thread-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  color: var(--text-sub);
  font-size: 12px;
}

.thread-actions {
  display: flex;
  justify-content: flex-end;
  gap: 6px;
  margin-top: 8px;
}

.action-btn {
  height: 28px;
  font-size: 12px;
}

.action-btn.danger {
  border-color: var(--retro-btn-danger-border);
  color: var(--retro-btn-danger-text);
}

.load-more {
  width: 100%;
}

</style>
