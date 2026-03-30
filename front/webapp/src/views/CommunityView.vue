<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { listFeedThreads, searchThreads } from '@/api/modules'
import { mapThreadList, mergePageList, resolveHasMore } from '@/utils/forum'

const router = useRouter()

const tabs = [
  { key: 'recommend', label: '推荐' },
  { key: 'hot', label: '热议' },
  { key: 'friends', label: '好友' },
  { key: 'latest', label: '最新' }
]

const activeTab = ref('recommend')
const keyword = ref('')
const loading = ref(false)
const loadingMore = ref(false)
const errorMessage = ref('')
const list = ref([])
const page = ref(1)
const size = 20
const total = ref(0)

const hasMore = computed(() => resolveHasMore(total.value, list.value.length))

function goCreatePost() {
  router.push('/post/create')
}

function openDetail(item) {
  router.push(`/post/${item.threadId}`)
}

function openUserProfile(user, event) {
  event.stopPropagation()
  const targetUserId = Number(user?.userId || 0)
  if (!targetUserId) {
    return
  }
  router.push(`/user/${targetUserId}`)
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
  } else {
    loadingMore.value = true
  }
  errorMessage.value = ''

  try {
    const targetPage = reset ? 1 : page.value
    const data = keyword.value.trim()
      ? await searchThreads(keyword.value.trim(), targetPage, size)
      : await listFeedThreads(activeTab.value, targetPage, size)
    const nextList = mapThreadList(Array.isArray(data?.list) ? data.list : [])
    list.value = mergePageList(list.value, nextList, reset)
    total.value = Number(data?.total || list.value.length)
    page.value = hasMore.value ? targetPage + 1 : targetPage
  } catch (error) {
    errorMessage.value = error.message || '加载帖子失败'
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

function changeTab(tabKey) {
  if (activeTab.value === tabKey) {
    return
  }
  activeTab.value = tabKey
}

function handleSearch() {
  loadThreads(true)
}

watch(activeTab, () => {
  keyword.value = ''
  loadThreads(true)
})

onMounted(() => loadThreads(true))
</script>

<template>
  <section class="community-page">
    <header class="page-header">
      <h1 class="page-title">社区广场</h1>
      <div class="page-header-actions">
        <button class="button page-action-btn" @click="goCreatePost">发主题</button>
        <button class="button page-action-btn" :disabled="loading || loadingMore" @click="loadThreads(true)">刷新</button>
      </div>
    </header>

    <div class="page-body">
      <div class="retro-tab-row retro-tab-row-fill">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          :class="['button retro-tab-btn', { active: activeTab === tab.key }]"
          @click="changeTab(tab.key)"
        >
          {{ tab.label }}
        </button>
      </div>

      <div class="retro-search-row">
        <input
          v-model="keyword"
          class="input search-input retro-search-input"
          placeholder="搜索帖子"
          @keyup.enter="handleSearch"
        />
        <button class="button" :disabled="loading || loadingMore" @click="handleSearch">搜索</button>
      </div>

      <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
      <p v-if="loading && list.length === 0" class="text-muted">加载中...</p>
      <div v-if="!loading && list.length === 0" class="empty">暂无帖子</div>

      <article
        v-for="item in list"
        :key="item.threadId"
        class="card retro-list-card thread-card"
        @click="openDetail(item)"
      >
        <h3 class="thread-title">{{ item.title }}</h3>

        <div v-if="item.hasVideo || item.hasSingleImagePreview || (item.previewImageUrls && item.previewImageUrls.length > 0)" class="thread-media-line">
          <div v-if="item.hasVideo" class="thread-video-cell">
            <img :src="item.videoPosterUrl" alt="" class="thread-media-image" />
            <div class="thread-video-mask">
              <div class="thread-video-play-icon" />
            </div>
          </div>
          <div v-else-if="item.hasSingleImagePreview" class="thread-image-single">
            <img :src="item.singleImagePreviewUrl" alt="" class="thread-media-image" />
          </div>
          <div v-else class="thread-image-grid">
            <div v-for="(previewUrl, previewIndex) in item.previewImageUrls" :key="`${item.threadId}_${previewUrl}`" class="thread-image-cell">
              <img :src="previewUrl" alt="" class="thread-media-image" />
              <div v-if="item.hasMoreImages && previewIndex === item.previewImageUrls.length - 1" class="thread-media-more">
                +{{ item.imageUrls.length - item.previewImageUrls.length }}
              </div>
            </div>
          </div>
        </div>

        <p
          v-if="item.contentPreview"
          :class="['thread-preview', item.hasVideo || item.previewImageUrls.length > 0 ? 'thread-preview-media' : 'thread-preview-text']"
        >
          {{ item.contentPreview }}
        </p>

        <div class="thread-meta-line">
          <div class="thread-author-line" @click.stop="openUserProfile(item.author, $event)">
            <strong class="thread-author-name">{{ item.author.displayName || '行者' }}</strong>
            <span v-if="item.author.displayTitleName" class="thread-title-badge" :style="{ color: item.author.displayTitleColor || '' }">
              [{{ item.author.displayTitleName }}]
            </span>
            <span class="thread-time">{{ item.timePrefix }} {{ item.timeRelativeText || item.timeText }}</span>
          </div>
          <span class="thread-stat">回 {{ item.replyCount || 0 }} / 阅 {{ item.viewCount || 0 }} / 赞 {{ item.likeCount || 0 }}</span>
        </div>
      </article>

      <button v-if="hasMore && list.length > 0" class="button load-more" :disabled="loadingMore" @click="loadThreads(false)">
        {{ loadingMore ? '加载中...' : '加载更多' }}
      </button>
      <p v-else-if="list.length > 0" class="text-muted end-text">没有更多主题</p>
    </div>
  </section>
</template>

<style scoped>
.community-page {
  min-height: 100%;
}

.thread-card {
  cursor: pointer;
}

.thread-title {
  margin: 0 0 8px;
  font-size: 18px;
}

.thread-media-line {
  margin-bottom: 8px;
}

.thread-video-cell,
.thread-image-single {
  position: relative;
  width: 160px;
  height: 104px;
  border: 1px solid var(--line);
  background: var(--retro-theme-item-bg);
  overflow: hidden;
}

.thread-image-grid {
  display: flex;
  gap: 6px;
}

.thread-image-cell {
  position: relative;
  width: 96px;
  height: 96px;
  border: 1px solid var(--line);
  background: var(--retro-theme-item-bg);
  overflow: hidden;
}

.thread-media-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.thread-video-mask {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(to top, rgba(0, 0, 0, 0.38), transparent 55%);
}

.thread-video-play-icon {
  width: 0;
  height: 0;
  border-top: 8px solid transparent;
  border-bottom: 8px solid transparent;
  border-left: 13px solid rgba(255, 255, 255, 0.9);
}

.thread-media-more {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.45);
  color: var(--retro-overlay-text);
  font-size: 14px;
}

.thread-preview {
  margin: 0 0 8px;
  color: var(--text-sub);
  line-height: 1.6;
  word-break: break-word;
}

.thread-preview-text {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.thread-preview-media {
  white-space: nowrap;
  text-overflow: ellipsis;
  overflow: hidden;
}

.thread-meta-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.thread-author-line {
  display: flex;
  align-items: center;
  gap: 4px;
  min-width: 0;
}

.thread-author-name {
  color: var(--text-main);
}

.thread-title-badge {
  color: var(--accent);
  font-size: 12px;
}

.thread-time {
  color: var(--text-sub);
  font-size: 12px;
}

.thread-stat {
  color: var(--text-sub);
  font-size: 12px;
  white-space: nowrap;
}

.load-more {
  width: 100%;
}

.end-text {
  text-align: center;
  margin: 10px 0 4px;
}
</style>
