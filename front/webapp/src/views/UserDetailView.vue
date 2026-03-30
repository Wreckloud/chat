<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import {
  createConversation,
  followUser,
  getUserHome,
  unfollowUser
} from '@/api/modules'
import { formatRelative } from '@/utils/time'
import { mapThreadList } from '@/utils/forum'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const targetUserId = computed(() => Number(route.params.userId || 0))
const loading = ref(false)
const followLoading = ref(false)
const chatLoading = ref(false)
const errorMessage = ref('')
const home = ref(null)
const latestThreads = ref([])

const isSelf = computed(() => home.value?.self === true)

function displayCount(value) {
  const safeValue = Number(value || 0)
  return Number.isFinite(safeValue) && safeValue >= 0 ? safeValue : 0
}

function openPostDetail(thread) {
  const threadId = Number(thread?.threadId || 0)
  if (!threadId) {
    return
  }
  router.push(`/post/${threadId}`)
}

function openAllThreads() {
  if (!home.value?.user?.userId) {
    return
  }
  const currentUserId = Number(authStore.userId || 0)
  router.push(`/user/${home.value.user.userId}/posts?currentUserId=${currentUserId}`)
}

async function loadHome() {
  if (!targetUserId.value) {
    return
  }
  loading.value = true
  errorMessage.value = ''
  try {
    const data = await getUserHome(targetUserId.value)
    home.value = data || null
    latestThreads.value = mapThreadList(Array.isArray(data?.latestThreads) ? data.latestThreads : [])
  } catch (error) {
    errorMessage.value = error.message || '加载行者主页失败'
  } finally {
    loading.value = false
  }
}

async function toggleFollow() {
  if (!home.value || isSelf.value || followLoading.value) {
    return
  }
  followLoading.value = true
  try {
    if (home.value.following === true) {
      const confirmed = window.confirm('取消后将不再优先看到对方动态，可随时重新关注。')
      if (!confirmed) {
        return
      }
      await unfollowUser(home.value.user.userId)
    } else {
      await followUser(home.value.user.userId)
    }
    await loadHome()
  } catch (error) {
    errorMessage.value = error.message || '更新关注状态失败'
  } finally {
    followLoading.value = false
  }
}

async function startConversation() {
  if (!home.value || isSelf.value || chatLoading.value) {
    return
  }
  chatLoading.value = true
  try {
    const conversationId = await createConversation(home.value.user.userId)
    router.push(`/chat/${conversationId}`)
  } catch (error) {
    errorMessage.value = error.message || '无法发起聊天'
  } finally {
    chatLoading.value = false
  }
}

onMounted(() => {
  loadHome()
})
</script>

<template>
  <section class="user-home-page">
    <header class="page-header">
      <button class="button page-action-btn" @click="router.back()">返回</button>
      <h1 class="page-title">行者主页</h1>
      <button class="button page-action-btn" :disabled="loading" @click="loadHome">刷新</button>
    </header>

    <div class="page-body">
      <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
      <p v-if="loading && !home" class="text-muted">加载中...</p>

      <article v-if="home?.user" class="card profile-card">
        <div class="profile-main">
          <div class="profile-avatar-box">
            <img v-if="home.user.avatar" :src="home.user.avatar" alt="" class="profile-avatar" />
            <div v-else class="profile-avatar placeholder">狼</div>
          </div>
          <div class="profile-meta">
            <div class="name-line">
              <strong>{{ home.user.nickname || home.user.wolfNo }}</strong>
              <span v-if="home.user.equippedTitleName" class="title-badge">[{{ home.user.equippedTitleName }}]</span>
            </div>
            <p class="text-muted">狼藉号 {{ home.user.wolfNo || '--' }}</p>
            <p class="text-muted">最近活跃 {{ formatRelative(home.lastActiveAt) }}</p>
          </div>
        </div>
        <div v-if="!isSelf" class="profile-actions">
          <button class="button" :disabled="followLoading" @click="toggleFollow">
            {{ home.following ? '取消关注' : '关注' }}
          </button>
          <button class="button button-primary" :disabled="chatLoading" @click="startConversation">
            发消息
          </button>
        </div>
      </article>

      <article v-if="home" class="card stats-card">
        <div class="stat-grid">
          <div class="stat-item">
            <div class="stat-value">{{ displayCount(home.activeDayCount) }}</div>
            <div class="stat-label">活跃天数</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ displayCount(home.totalLikeCount) }}</div>
            <div class="stat-label">总获赞</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ displayCount(home.followerCount) }}</div>
            <div class="stat-label">粉丝</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ displayCount(home.followingCount) }}</div>
            <div class="stat-label">关注</div>
          </div>
        </div>
      </article>

      <article v-if="home" class="card showcase-card">
        <div class="section-head">
          <strong>头衔橱窗</strong>
        </div>
        <div class="showcase-list">
          <span
            v-for="item in (home.showcaseTitles || [])"
            :key="`${item.achievementCode}_${item.titleName}`"
            class="title-badge"
            :style="{ color: item.titleColor || '' }"
          >
            [{{ item.titleName }}]
          </span>
          <span v-if="!home.showcaseTitles || home.showcaseTitles.length === 0" class="text-muted">暂未展示头衔</span>
        </div>
      </article>

      <article v-if="home" class="card latest-card">
        <div class="section-head">
          <strong>最新主题</strong>
          <button class="button page-action-btn" @click="openAllThreads">查看全部</button>
        </div>
        <div v-if="latestThreads.length === 0" class="empty">暂无主题</div>
        <div v-else>
          <article
            v-for="thread in latestThreads"
            :key="thread.threadId"
            class="latest-item"
            @click="openPostDetail(thread)"
          >
            <h3 class="latest-title">{{ thread.title }}</h3>
            <img
              v-if="thread.videoPosterUrl || thread.singleImagePreviewUrl"
              :src="thread.videoPosterUrl || thread.singleImagePreviewUrl"
              alt=""
              class="latest-media"
            />
            <p class="latest-preview">{{ thread.contentPreview || '暂无正文' }}</p>
            <div class="latest-meta">
              <span>{{ thread.timePrefix }} {{ thread.timeRelativeText }}</span>
              <span>回 {{ thread.replyCount || 0 }} / 阅 {{ thread.viewCount || 0 }} / 赞 {{ thread.likeCount || 0 }}</span>
            </div>
          </article>
        </div>
      </article>
    </div>
  </section>
</template>

<style scoped>
.user-home-page {
  min-height: 100%;
}

.profile-card {
  padding: 10px;
  margin-bottom: 8px;
}

.profile-main {
  display: flex;
  gap: 10px;
}

.profile-avatar-box {
  width: 56px;
  height: 56px;
  border: 1px solid var(--line);
  flex: 0 0 auto;
}

.profile-avatar {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.profile-avatar.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--retro-muted-text);
  background: var(--retro-theme-item-bg);
}

.profile-meta {
  min-width: 0;
}

.name-line {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 4px;
}

.profile-meta p {
  margin: 0 0 4px;
}

.profile-actions {
  margin-top: 10px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.stats-card,
.showcase-card,
.latest-card {
  padding: 10px;
  margin-bottom: 8px;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
}

.stat-item {
  border: 1px solid var(--line);
  background: var(--retro-theme-item-bg);
  padding: 8px 6px;
  text-align: center;
}

.stat-value {
  font-size: 18px;
  color: var(--text-main);
}

.stat-label {
  font-size: 12px;
  color: var(--text-sub);
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.showcase-list {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.title-badge {
  color: var(--retro-accent-text);
  font-size: 12px;
}

.latest-item {
  border: 1px solid var(--line);
  padding: 8px;
  margin-bottom: 8px;
  cursor: pointer;
}

.latest-title {
  margin: 0 0 8px;
  font-size: 15px;
}

.latest-media {
  width: 100%;
  max-height: 200px;
  object-fit: cover;
  border: 1px solid var(--line);
  margin-bottom: 8px;
}

.latest-preview {
  margin: 0 0 6px;
  color: var(--text-sub);
}

.latest-meta {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  color: var(--text-sub);
  font-size: 12px;
}

</style>
