<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import {
  createReply,
  deleteReply,
  deleteThread,
  getThreadDetail,
  listThreadReplies,
  updateReplyLikeStatus,
  updateThreadLikeStatus
} from '@/api/modules'
import { mapReply, mapThread } from '@/utils/forum'
import { formatDateTime, formatRelative } from '@/utils/time'
import { uploadForumReplyImage } from '@/utils/media-upload'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const threadId = computed(() => Number(route.params.threadId || 0))
const currentUserId = computed(() => Number(authStore.userId || 0))
const loading = ref(false)
const replyLoading = ref(false)
const sending = ref(false)
const errorMessage = ref('')
const thread = ref(null)
const content = ref('')
const replyList = ref([])
const replyText = ref('')
const quoteReply = ref(null)
const selectedReplyImage = ref(null)
const replySort = ref('floor')
const replyImageInputRef = ref(null)
const uploadProgress = ref(0)
const uploadText = ref('')
const replyContainerRef = ref(null)
const replyInputRef = ref(null)

const objectUrlSet = new Set()

const canManageThread = computed(() => Number(thread.value?.author?.userId || 0) === currentUserId.value)

const canSubmitReply = computed(() => {
  if (sending.value) {
    return false
  }
  return Boolean(replyText.value.trim()) || Boolean(selectedReplyImage.value)
})

const groupedReplies = computed(() => {
  const map = new Map()
  replyList.value.forEach((reply) => {
    map.set(Number(reply.replyId || 0), reply)
  })
  const rootList = []
  const rootChildMap = new Map()

  function resolveRootId(reply) {
    const seen = new Set()
    let cursor = reply
    while (cursor && cursor.quoteReplyId && map.has(Number(cursor.quoteReplyId || 0))) {
      if (seen.has(Number(cursor.replyId || 0))) {
        return Number(reply.replyId || 0)
      }
      seen.add(Number(cursor.replyId || 0))
      const parent = map.get(Number(cursor.quoteReplyId || 0))
      if (!parent) {
        break
      }
      cursor = parent
    }
    return Number(cursor?.replyId || reply.replyId || 0)
  }

  replyList.value.forEach((reply) => {
    const quoteReplyId = Number(reply.quoteReplyId || 0)
    if (!quoteReplyId || !map.has(quoteReplyId)) {
      rootList.push(reply)
      return
    }
    const rootId = resolveRootId(reply)
    if (!rootId || rootId === Number(reply.replyId || 0)) {
      rootList.push(reply)
      return
    }
    const group = rootChildMap.get(rootId) || []
    group.push(reply)
    rootChildMap.set(rootId, group)
  })

  return rootList.map((rootReply) => ({
    root: rootReply,
    children: (rootChildMap.get(Number(rootReply.replyId || 0)) || [])
      .slice()
      .sort((left, right) => Number(left.floorNo || 0) - Number(right.floorNo || 0))
  }))
})

function registerObjectUrl(url) {
  if (!url) {
    return
  }
  objectUrlSet.add(url)
}

function revokeObjectUrls() {
  objectUrlSet.forEach((url) => URL.revokeObjectURL(url))
  objectUrlSet.clear()
}

function openUser(user, event) {
  event?.stopPropagation?.()
  const targetUserId = Number(user?.userId || 0)
  if (!targetUserId) {
    return
  }
  router.push(`/user/${targetUserId}`)
}

function focusReplyInput() {
  nextTick(() => {
    replyInputRef.value?.focus()
  })
}

function chooseReplyTarget(reply) {
  quoteReply.value = {
    replyId: Number(reply?.replyId || 0),
    floorNo: Number(reply?.floorNo || 0),
    nickname: String(reply?.author?.displayName || reply?.author?.nickname || '').trim()
  }
  const targetName = quoteReply.value.nickname
  const prefix = targetName ? `@${targetName} ` : ''
  if (!replyText.value.trim().startsWith(prefix)) {
    replyText.value = `${prefix}${replyText.value.trimStart()}`
  }
  focusReplyInput()
}

function clearReplyTarget() {
  quoteReply.value = null
  replyText.value = replyText.value.replace(/^@\S+\s+/, '')
}

function triggerReplyImagePick() {
  if (sending.value) {
    return
  }
  replyImageInputRef.value?.click()
}

function clearReplyImage() {
  if (!selectedReplyImage.value) {
    return
  }
  URL.revokeObjectURL(selectedReplyImage.value.previewUrl)
  objectUrlSet.delete(selectedReplyImage.value.previewUrl)
  selectedReplyImage.value = null
}

function handleReplyImagePicked(event) {
  const file = event?.target?.files?.[0]
  event.target.value = ''
  if (!file) {
    return
  }
  if (selectedReplyImage.value) {
    clearReplyImage()
  }
  const previewUrl = URL.createObjectURL(file)
  registerObjectUrl(previewUrl)
  selectedReplyImage.value = {
    file,
    previewUrl
  }
}

async function loadDetail() {
  if (!threadId.value) {
    return
  }
  loading.value = true
  errorMessage.value = ''
  try {
    const data = await getThreadDetail(threadId.value)
    thread.value = mapThread(data?.thread || {})
    content.value = data?.content || ''
  } catch (error) {
    errorMessage.value = error.message || '加载主题失败'
  } finally {
    loading.value = false
  }
}

async function loadReplies() {
  if (!threadId.value) {
    return
  }
  replyLoading.value = true
  try {
    const data = await listThreadReplies(threadId.value, 1, 50, replySort.value)
    const rawList = Array.isArray(data?.list) ? data.list : []
    replyList.value = rawList.map((item) => mapReply(item, {
      currentUserId: currentUserId.value,
      canManageThread: canManageThread.value
    }))
  } catch (error) {
    errorMessage.value = error.message || '加载回复失败'
  } finally {
    replyLoading.value = false
  }
}

async function refreshAll() {
  await loadDetail()
  await loadReplies()
}

async function handleReply() {
  if (!canSubmitReply.value || sending.value) {
    return
  }
  sending.value = true
  errorMessage.value = ''
  uploadProgress.value = 0
  uploadText.value = ''
  try {
    let imageKey = ''
    if (selectedReplyImage.value) {
      uploadText.value = '上传图片中...'
      const uploaded = await uploadForumReplyImage(selectedReplyImage.value.file, {
        onProgress: (progress) => {
          uploadProgress.value = progress
          uploadText.value = `上传图片 ${progress}%`
        }
      })
      imageKey = uploaded.mediaKey
    }
    const payload = {
      content: replyText.value.trim()
    }
    if (quoteReply.value?.replyId) {
      payload.quoteReplyId = quoteReply.value.replyId
    }
    if (imageKey) {
      payload.imageKey = imageKey
    }
    const created = await createReply(threadId.value, payload)
    replyList.value = replyList.value.concat(mapReply(created, {
      currentUserId: currentUserId.value,
      canManageThread: canManageThread.value
    }))
    replyText.value = ''
    clearReplyImage()
    quoteReply.value = null
    await loadDetail()
    await nextTick()
    if (replyContainerRef.value) {
      replyContainerRef.value.scrollTop = replyContainerRef.value.scrollHeight
    }
  } catch (error) {
    errorMessage.value = error.message || '发布回复失败'
  } finally {
    sending.value = false
    uploadProgress.value = 0
    uploadText.value = ''
  }
}

async function toggleThreadLike() {
  if (!thread.value?.threadId) {
    return
  }
  const nextLiked = !(thread.value.likedByCurrentUser === true)
  try {
    await updateThreadLikeStatus(thread.value.threadId, nextLiked)
    thread.value = {
      ...thread.value,
      likedByCurrentUser: nextLiked,
      likeCount: Math.max(0, Number(thread.value.likeCount || 0) + (nextLiked ? 1 : -1))
    }
  } catch (error) {
    errorMessage.value = error.message || '更新点赞失败'
  }
}

async function toggleReplyLike(reply) {
  const replyId = Number(reply?.replyId || 0)
  if (!replyId) {
    return
  }
  const nextLiked = !(reply.likedByCurrentUser === true)
  try {
    await updateReplyLikeStatus(replyId, nextLiked)
    replyList.value = replyList.value.map((item) => {
      if (Number(item.replyId || 0) !== replyId) {
        return item
      }
      return {
        ...item,
        likedByCurrentUser: nextLiked,
        likeCount: Math.max(0, Number(item.likeCount || 0) + (nextLiked ? 1 : -1))
      }
    })
  } catch (error) {
    errorMessage.value = error.message || '更新点赞失败'
  }
}

async function removeReply(reply) {
  const replyId = Number(reply?.replyId || 0)
  if (!replyId) {
    return
  }
  const confirmed = window.confirm('确认删除该回复？')
  if (!confirmed) {
    return
  }
  try {
    await deleteReply(replyId)
    await loadReplies()
    await loadDetail()
  } catch (error) {
    errorMessage.value = error.message || '删除回复失败'
  }
}

function goEditThread() {
  const currentThreadId = Number(thread.value?.threadId || 0)
  if (!currentThreadId) {
    return
  }
  router.push(`/post/create?mode=edit&threadId=${currentThreadId}`)
}

async function removeThreadSelf() {
  const currentThreadId = Number(thread.value?.threadId || 0)
  if (!currentThreadId) {
    return
  }
  const confirmed = window.confirm('删除后将进入垃圾站，确认删除吗？')
  if (!confirmed) {
    return
  }
  try {
    await deleteThread(currentThreadId)
    router.replace('/community')
  } catch (error) {
    errorMessage.value = error.message || '删除主题失败'
  }
}

function changeReplySort(nextSort) {
  if (replySort.value === nextSort) {
    return
  }
  replySort.value = nextSort
  loadReplies()
}

onMounted(async () => {
  await refreshAll()
})

onBeforeUnmount(() => {
  revokeObjectUrls()
})
</script>

<template>
  <section class="post-detail-page">
    <header class="page-header">
      <button class="button page-action-btn" @click="router.back()">返回</button>
      <h1 class="page-title">主题详情</h1>
      <button class="button page-action-btn" :disabled="loading || replyLoading" @click="refreshAll">刷新</button>
    </header>

    <div ref="replyContainerRef" class="detail-scroll">
      <div class="page-body">
        <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
        <p v-if="loading" class="text-muted">加载中...</p>

        <article v-if="thread" class="card detail-card">
          <h2 class="detail-title">{{ thread.title }}</h2>
          <div class="detail-author-line" @click="openUser(thread.author, $event)">
            <strong class="detail-author-name">{{ thread.author.displayName || '行者' }}</strong>
            <span v-if="thread.author.displayTitleName" class="detail-title-badge" :style="{ color: thread.author.displayTitleColor || '' }">
              [{{ thread.author.displayTitleName }}]
            </span>
            <span class="detail-time">{{ thread.timePrefix }} {{ thread.timeRelativeText }}</span>
          </div>
          <div class="detail-meta">
            <span>赞 {{ thread.likeCount || 0 }} / 回 {{ thread.replyCount || 0 }} / 阅 {{ thread.viewCount || 0 }}</span>
            <span>{{ formatDateTime(thread.editTime || thread.createTime) }}</span>
          </div>

          <div v-if="thread.hasVideo && thread.videoUrl" class="detail-video-wrap">
            <video
              class="detail-video"
              :src="thread.videoUrl"
              :poster="thread.videoPosterUrl || ''"
              controls
              preload="metadata"
            />
          </div>
          <img v-else-if="thread.imageUrls?.[0]" :src="thread.imageUrls[0]" alt="" class="detail-image" />

          <p class="detail-content">{{ content || '暂无正文' }}</p>

          <div class="detail-actions">
            <button class="button action-btn" @click="toggleThreadLike">{{ thread.likedByCurrentUser ? '取消点赞' : '点赞' }}</button>
            <button class="button action-btn" @click="focusReplyInput">回复</button>
            <template v-if="canManageThread">
              <button class="button action-btn" @click="goEditThread">编辑</button>
              <button class="button action-btn danger" @click="removeThreadSelf">删除</button>
            </template>
          </div>
        </article>

        <section class="reply-section">
          <div class="reply-head">
            <h3 class="reply-title">楼层回复</h3>
            <div class="reply-sort">
              <button :class="['sort-btn', { active: replySort === 'floor' }]" @click="changeReplySort('floor')">按楼层</button>
              <button :class="['sort-btn', { active: replySort === 'hot' }]" @click="changeReplySort('hot')">按热度</button>
              <button :class="['sort-btn', { active: replySort === 'owner' }]" @click="changeReplySort('owner')">只看楼主</button>
            </div>
          </div>

          <p v-if="replyLoading" class="text-muted">回复加载中...</p>
          <div v-else-if="groupedReplies.length === 0" class="empty">暂无回复</div>

          <article v-for="group in groupedReplies" :key="group.root.replyId" class="card reply-item">
            <div class="reply-top">
              <div class="reply-author-line" @click="openUser(group.root.author, $event)">
                <strong>#{{ group.root.floorNo }} {{ group.root.author.displayName || '行者' }}</strong>
                <span v-if="group.root.author.displayTitleName" class="reply-title-badge" :style="{ color: group.root.author.displayTitleColor || '' }">
                  [{{ group.root.author.displayTitleName }}]
                </span>
                <span class="text-muted">{{ formatRelative(group.root.createTime) }}</span>
              </div>
              <div class="reply-actions-inline">
                <button class="link-btn" @click="toggleReplyLike(group.root)">
                  {{ group.root.likedByCurrentUser ? '取消赞' : '点赞' }} {{ group.root.likeCount || 0 }}
                </button>
                <button class="link-btn" @click="chooseReplyTarget(group.root)">回复</button>
                <button v-if="group.root.canDelete" class="link-btn danger-text" @click="removeReply(group.root)">删除</button>
              </div>
            </div>
            <img v-if="group.root.imageUrl" :src="group.root.imageUrl" alt="" class="reply-image" />
            <p class="reply-content">{{ group.root.content || '[图片回复]' }}</p>

            <div v-if="group.children.length > 0" class="reply-sub-list">
              <article v-for="sub in group.children" :key="sub.replyId" class="reply-sub-item">
                <div class="reply-sub-head">
                  <div class="reply-sub-name-line">
                    <strong>#{{ sub.floorNo }} {{ sub.author.displayName || '行者' }}</strong>
                    <span v-if="sub.author.displayTitleName" class="reply-title-badge" :style="{ color: sub.author.displayTitleColor || '' }">
                      [{{ sub.author.displayTitleName }}]
                    </span>
                    <span class="text-muted">{{ formatRelative(sub.createTime) }}</span>
                  </div>
                  <button v-if="sub.canDelete" class="link-btn danger-text" @click="removeReply(sub)">删除</button>
                </div>
                <p class="reply-sub-content">
                  回复 @{{ sub.quoteAuthor.displayName || sub.quoteAuthor.nickname || '行者' }}:
                  {{ sub.content || '[图片回复]' }}
                </p>
                <img v-if="sub.imageUrl" :src="sub.imageUrl" alt="" class="reply-sub-image" />
                <div class="reply-sub-actions">
                  <button class="link-btn" @click="toggleReplyLike(sub)">
                    {{ sub.likedByCurrentUser ? '取消赞' : '点赞' }} {{ sub.likeCount || 0 }}
                  </button>
                  <button class="link-btn" @click="chooseReplyTarget(sub)">回复</button>
                </div>
              </article>
            </div>
          </article>
        </section>
      </div>
    </div>

    <footer class="im-composer composer">
      <div v-if="quoteReply" class="quote-bar">
        <span>回复 #{{ quoteReply.floorNo }} @{{ quoteReply.nickname }}</span>
        <button class="link-btn" @click="clearReplyTarget">取消</button>
      </div>
      <div v-if="selectedReplyImage" class="selected-image-bar">
        <img :src="selectedReplyImage.previewUrl" alt="" class="selected-image-preview" />
        <button class="link-btn danger-text" @click="clearReplyImage">移除图片</button>
      </div>
      <div class="im-composer-row">
        <input
          ref="replyInputRef"
          v-model="replyText"
          class="input im-composer-input"
          placeholder="写下你的回复..."
          @keyup.enter="handleReply"
        />
        <button class="button im-composer-more" :disabled="sending" @click="triggerReplyImagePick">图片</button>
        <button class="button button-primary im-composer-send" :disabled="!canSubmitReply" @click="handleReply">
          {{ sending ? '回帖中' : '回帖' }}
        </button>
      </div>
      <div v-if="sending && uploadText" class="im-upload-progress">
        <span>{{ uploadText }}</span>
        <span>{{ uploadProgress }}%</span>
      </div>
    </footer>
    <input ref="replyImageInputRef" class="hidden-input" type="file" accept="image/*" @change="handleReplyImagePicked" />
  </section>
</template>

<style scoped>
.post-detail-page {
  min-height: 100%;
  display: flex;
  flex-direction: column;
}

.detail-scroll {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
}

.detail-card {
  padding: 10px;
  margin-bottom: 10px;
}

.detail-title {
  margin: 0 0 8px;
  font-size: 20px;
}

.detail-author-line {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 6px;
  cursor: pointer;
}

.detail-author-name {
  color: var(--text-main);
}

.detail-title-badge {
  color: var(--accent);
  font-size: 12px;
}

.detail-time {
  color: var(--text-sub);
  font-size: 12px;
}

.detail-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: var(--text-sub);
  font-size: 12px;
  margin-bottom: 8px;
}

.detail-image,
.detail-video {
  width: 100%;
  max-height: 360px;
  object-fit: contain;
  border: 1px solid var(--line);
  background: var(--retro-media-bg);
  margin-bottom: 8px;
}

.detail-content {
  margin: 0 0 8px;
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.6;
}

.detail-actions {
  display: flex;
  align-items: center;
  gap: 6px;
}

.action-btn {
  height: 28px;
  font-size: 12px;
}

.action-btn.danger {
  border-color: var(--retro-btn-danger-border);
  color: var(--retro-btn-danger-text);
}

.reply-section {
  margin-bottom: 12px;
}

.reply-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.reply-title {
  margin: 0;
  font-size: 18px;
}

.reply-sort {
  display: flex;
  gap: 4px;
}

.sort-btn {
  border: 1px solid var(--line);
  background: transparent;
  color: var(--text-sub);
  font-size: 12px;
  height: 26px;
  padding: 0 8px;
  cursor: pointer;
}

.sort-btn.active {
  color: var(--retro-accent-text);
  border-color: var(--retro-theme-item-active-border);
  background: var(--retro-theme-item-active-bg);
}

.reply-item {
  padding: 8px;
  margin-bottom: 8px;
}

.reply-top {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 6px;
}

.reply-author-line {
  display: flex;
  align-items: center;
  gap: 4px;
  min-width: 0;
  cursor: pointer;
}

.reply-actions-inline {
  display: flex;
  align-items: center;
  gap: 8px;
  white-space: nowrap;
}

.reply-content {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.6;
}

.reply-image {
  display: block;
  max-width: 220px;
  max-height: 220px;
  object-fit: cover;
  border: 1px solid var(--line);
  margin-bottom: 6px;
}

.reply-sub-list {
  margin-top: 8px;
  border-left: 2px solid var(--line);
  padding-left: 8px;
  background: var(--retro-theme-item-active-bg);
}

.reply-sub-item {
  padding: 6px 0;
}

.reply-sub-item + .reply-sub-item {
  border-top: 1px dashed var(--line);
}

.reply-sub-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 4px;
}

.reply-sub-name-line {
  display: flex;
  align-items: center;
  gap: 4px;
  min-width: 0;
}

.reply-sub-content {
  margin: 0 0 4px;
  color: var(--text-main);
  font-size: 13px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
}

.reply-sub-image {
  display: block;
  max-width: 180px;
  max-height: 180px;
  object-fit: cover;
  border: 1px solid var(--line);
  margin-bottom: 4px;
}

.reply-sub-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.link-btn {
  border: none;
  background: transparent;
  color: var(--accent);
  font-size: 12px;
  padding: 0;
  cursor: pointer;
}

.danger-text {
  color: var(--retro-btn-danger-text);
}

.quote-bar,
.selected-image-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  border: 1px solid var(--line);
  background: var(--retro-theme-item-bg);
  color: var(--text-sub);
  font-size: 12px;
  padding: 6px 8px;
  margin-bottom: 6px;
}

.selected-image-preview {
  width: 48px;
  height: 48px;
  object-fit: cover;
  border: 1px solid var(--line);
}

</style>
