<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getNoticeUnreadSummary, listNotices, markAllNoticeRead, markNoticeRead } from '@/api/modules'
import { formatDateTime } from '@/utils/time'

const router = useRouter()

const tabs = [
  { key: 'all', label: '全部' },
  { key: 'interaction', label: '互动' },
  { key: 'follow', label: '关注' },
  { key: 'achievement', label: '成就' }
]

const loading = ref(false)
const errorMessage = ref('')
const activeTab = ref('all')
const notices = ref([])
const unreadSummary = ref({
  totalUnread: 0,
  interactionUnread: 0,
  followUnread: 0,
  achievementUnread: 0
})

function isInteractionNotice(item) {
  const type = String(item?.noticeType || '')
  return (
    type.endsWith('_REPLIED') ||
    type.endsWith('_LIKED')
  )
}

function isFollowNotice(item) {
  const type = String(item?.noticeType || '')
  return type.includes('FOLLOW')
}

function isAchievementNotice(item) {
  const type = String(item?.noticeType || '')
  return type.includes('ACHIEVEMENT')
}

const filteredNotices = computed(() => {
  if (activeTab.value === 'all') {
    return notices.value
  }
  if (activeTab.value === 'interaction') {
    return notices.value.filter((item) => isInteractionNotice(item))
  }
  if (activeTab.value === 'follow') {
    return notices.value.filter((item) => isFollowNotice(item))
  }
  if (activeTab.value === 'achievement') {
    return notices.value.filter((item) => isAchievementNotice(item))
  }
  return notices.value
})

function toDate(dateValue) {
  if (!dateValue) {
    return null
  }
  const normalized = typeof dateValue === 'string' ? dateValue.replace(' ', 'T') : dateValue
  const date = new Date(normalized)
  if (Number.isNaN(date.getTime())) {
    return null
  }
  return date
}

function pad(value) {
  return String(value).padStart(2, '0')
}

function resolveGroupTitle(dateValue) {
  const date = toDate(dateValue)
  if (!date) {
    return '更早'
  }
  const now = new Date()
  const isToday = (
    date.getFullYear() === now.getFullYear() &&
    date.getMonth() === now.getMonth() &&
    date.getDate() === now.getDate()
  )
  if (isToday) {
    return '今天'
  }
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

const groupedNotices = computed(() => {
  const groups = []
  filteredNotices.value.forEach((item) => {
    const groupTitle = resolveGroupTitle(item?.createTime)
    const existing = groups.find((group) => group.groupTitle === groupTitle)
    if (existing) {
      existing.items.push(item)
      return
    }
    groups.push({
      groupTitle,
      items: [item]
    })
  })
  return groups
})

function getTabUnread(tabKey) {
  if (tabKey === 'all') {
    return Number(unreadSummary.value.totalUnread || 0)
  }
  if (tabKey === 'interaction') {
    return Number(unreadSummary.value.interactionUnread || 0)
  }
  if (tabKey === 'follow') {
    return Number(unreadSummary.value.followUnread || 0)
  }
  if (tabKey === 'achievement') {
    return Number(unreadSummary.value.achievementUnread || 0)
  }
  return 0
}

function resolveWebActionUrl(actionUrl) {
  const path = String(actionUrl || '')
  if (!path) {
    return ''
  }
  const threadMatched = path.match(/threadId=(\d+)/)
  if (threadMatched) {
    return `/post/${threadMatched[1]}`
  }
  const conversationMatched = path.match(/conversationId=(\d+)/)
  if (conversationMatched) {
    return `/chat/${conversationMatched[1]}`
  }
  if (path.includes('/pages/lobby/lobby')) {
    return '/lobby'
  }
  if (path.includes('/pages/notice/notice')) {
    return '/notice'
  }
  return ''
}

async function loadUnreadSummary() {
  try {
    const data = await getNoticeUnreadSummary()
    unreadSummary.value = {
      totalUnread: Number(data?.totalUnread || 0),
      interactionUnread: Number(data?.interactionUnread || 0),
      followUnread: Number(data?.followUnread || 0),
      achievementUnread: Number(data?.achievementUnread || 0)
    }
  } catch (error) {
    unreadSummary.value = {
      totalUnread: 0,
      interactionUnread: 0,
      followUnread: 0,
      achievementUnread: 0
    }
  }
}

async function loadNotices() {
  loading.value = true
  errorMessage.value = ''
  try {
    const data = await listNotices(1, 120)
    notices.value = Array.isArray(data?.list) ? data.list : []
  } catch (error) {
    errorMessage.value = error.message || '加载通知失败'
  } finally {
    loading.value = false
  }
}

async function markAllReadAndRefresh() {
  try {
    await markAllNoticeRead()
    await Promise.all([loadUnreadSummary(), loadNotices()])
  } catch (error) {
    errorMessage.value = error.message || '操作失败'
  }
}

async function handleOpenNotice(item) {
  const noticeId = Number(item?.noticeId || 0)
  if (noticeId > 0 && item?.read !== true) {
    try {
      await markNoticeRead(noticeId)
    } catch (error) {
      // 忽略已读失败，不阻断跳转
    }
  }
  const routePath = resolveWebActionUrl(item?.actionUrl || '')
  await Promise.all([loadUnreadSummary(), loadNotices()])
  if (routePath) {
    router.push(routePath)
  }
}

onMounted(async () => {
  await Promise.all([loadUnreadSummary(), loadNotices()])
})
</script>

<template>
  <section class="notice-page">
    <header class="page-header">
      <button class="button page-action-btn" @click="router.back()">返回</button>
      <h1 class="page-title">系统通知</h1>
      <button class="button page-action-btn" :disabled="loading" @click="markAllReadAndRefresh">全部已读</button>
    </header>

    <div class="page-body">
      <div class="retro-tab-row retro-tab-row-fill">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          :class="['button retro-tab-btn', { active: activeTab === tab.key }]"
          @click="activeTab = tab.key"
        >
          {{ tab.label }}<span v-if="getTabUnread(tab.key) > 0" class="retro-status-tag">[{{ getTabUnread(tab.key) }}]</span>
        </button>
      </div>

      <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
      <p v-if="loading" class="text-muted">加载中...</p>
      <div v-if="!loading && groupedNotices.length === 0" class="empty">暂无通知</div>

      <section v-for="group in groupedNotices" :key="group.groupTitle" class="notice-group">
        <div class="notice-group-head">
          <span class="notice-group-title">{{ group.groupTitle }}</span>
          <span class="text-muted">{{ group.items.length }}</span>
        </div>
        <article v-for="item in group.items" :key="item.noticeId" class="card retro-list-card notice-item">
          <div class="notice-content">
            <span v-if="item.read !== true" class="unread-tag">[未读]</span>
            <span>{{ item.content }}</span>
          </div>
          <div class="notice-meta">
            <span class="text-muted">{{ item.typeLabel || '通知' }}</span>
            <div class="notice-meta-right">
              <span class="text-muted">{{ formatDateTime(item.createTime) }}</span>
              <button class="button page-action-btn" @click="handleOpenNotice(item)">查看</button>
            </div>
          </div>
        </article>
      </section>
    </div>
  </section>
</template>

<style scoped>
.notice-page {
  min-height: 100%;
}

.notice-group {
  margin-bottom: 10px;
}

.notice-group-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.notice-group-title {
  color: var(--retro-strong-text);
  font-size: 13px;
}

.notice-item {
  margin-bottom: 6px;
}

.notice-content {
  line-height: 1.6;
  margin-bottom: 4px;
}

.notice-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.notice-meta-right {
  display: flex;
  align-items: center;
  gap: 6px;
}

.unread-tag {
  color: var(--retro-muted-text);
  margin-right: 4px;
}

</style>
