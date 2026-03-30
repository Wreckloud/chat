<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import {
  getLobbyMeta,
  getNoticeUnreadSummary,
  listConversations,
  listMutualFollows
} from '@/api/modules'
import { formatClock, formatRelative } from '@/utils/time'
import { connect, subscribe } from '@/realtime/ws-client'

const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const errorMessage = ref('')
const keyword = ref('')
const conversations = ref([])
const friendUserIdSet = ref(new Set())
const noticeUnreadTotal = ref(0)
const lobbyMeta = ref({
  onlineCount: 0,
  latestMessageSenderName: '',
  latestMessagePreview: '',
  latestMessageAt: ''
})
const baseFilter = ref('all')
const unreadOnly = ref(false)
const onlineOnly = ref(false)

let wsUnsubscribe = null
let reloadTimer = null
let lobbyMetaTimer = null

const filterItems = computed(() => [
  { key: 'all', label: '全部', active: baseFilter.value === 'all', mode: 'base' },
  { key: 'friend', label: '好友', active: baseFilter.value === 'friend', mode: 'base' },
  { key: 'unread', label: '未读', active: unreadOnly.value, mode: 'toggle' },
  { key: 'online', label: '在线', active: onlineOnly.value, mode: 'toggle' }
])

const filteredList = computed(() => {
  const searchValue = keyword.value.trim().toLowerCase()
  let list = conversations.value.slice()
  if (baseFilter.value === 'friend') {
    list = list.filter((item) => friendUserIdSet.value.has(Number(item.targetUserId || 0)))
  }
  if (unreadOnly.value) {
    list = list.filter((item) => Number(item.unreadCount || 0) > 0)
  }
  if (onlineOnly.value) {
    list = list.filter((item) => item.isOnline === true)
  }
  if (!searchValue) {
    return list
  }
  return list.filter((item) => (
    String(item.targetNickname || '').toLowerCase().includes(searchValue) ||
    String(item.targetWolfNo || '').toLowerCase().includes(searchValue) ||
    String(item.lastMessage || '').toLowerCase().includes(searchValue)
  ))
})

const emptyText = computed(() => {
  if (keyword.value.trim()) {
    return '没找到相关会话'
  }
  if (baseFilter.value === 'friend' && onlineOnly.value) {
    return '当前没有好友在线'
  }
  if (baseFilter.value === 'friend') {
    return '还没有好友会话'
  }
  if (onlineOnly.value) {
    return '现在没有在线同学'
  }
  if (unreadOnly.value) {
    return '目前没有未读消息'
  }
  return '还没有聊天记录'
})

function sortByMessageTime(list) {
  return list.slice().sort((left, right) => {
    const leftTime = String(left.lastMessageTime || '')
    const rightTime = String(right.lastMessageTime || '')
    if (leftTime !== rightTime) {
      return rightTime.localeCompare(leftTime)
    }
    return Number(right.conversationId || 0) - Number(left.conversationId || 0)
  })
}

function buildMessagePreview(message) {
  const msgType = String(message?.msgType || '').toUpperCase()
  if (msgType === 'RECALL') {
    return '[消息已撤回]'
  }
  if (msgType === 'IMAGE') {
    return '[图片]'
  }
  if (msgType === 'VIDEO') {
    return '[视频]'
  }
  if (msgType === 'FILE') {
    return '[文件]'
  }
  if (msgType === 'SYSTEM') {
    return message?.content || '[系统消息]'
  }
  return message?.content || '[消息]'
}

function scheduleReloadConversations() {
  if (reloadTimer) {
    return
  }
  reloadTimer = setTimeout(() => {
    reloadTimer = null
    loadConversations()
  }, 320)
}

async function loadFriendUserIds() {
  try {
    const list = await listMutualFollows()
    const idSet = new Set()
    ;(Array.isArray(list) ? list : []).forEach((item) => {
      const userId = Number(item?.userId || 0)
      if (userId > 0) {
        idSet.add(userId)
      }
    })
    friendUserIdSet.value = idSet
  } catch (error) {
    friendUserIdSet.value = new Set()
  }
}

async function loadNoticeUnread() {
  try {
    const summary = await getNoticeUnreadSummary()
    noticeUnreadTotal.value = Number(summary?.totalUnread || 0)
  } catch (error) {
    noticeUnreadTotal.value = 0
  }
}

async function loadLobbyMeta() {
  try {
    const data = await getLobbyMeta()
    lobbyMeta.value = {
      onlineCount: Number(data?.onlineCount || 0),
      latestMessageSenderName: String(data?.latestMessageSenderName || ''),
      latestMessagePreview: String(data?.latestMessagePreview || ''),
      latestMessageAt: data?.latestMessageAt || ''
    }
  } catch (error) {
    // 忽略大厅元信息错误，避免影响会话列表主流程
  }
}

function buildLobbyPreview() {
  const sender = lobbyMeta.value.latestMessageSenderName
  const text = lobbyMeta.value.latestMessagePreview
  if (!sender && !text) {
    return '广场里还没人发言'
  }
  if (!sender) {
    return text
  }
  return `${sender}: ${text || ''}`
}

function scheduleLobbyMetaRefresh() {
  if (lobbyMetaTimer) {
    return
  }
  lobbyMetaTimer = setTimeout(() => {
    lobbyMetaTimer = null
    loadLobbyMeta()
  }, 500)
}

function clearLobbyMetaRefreshTimer() {
  if (!lobbyMetaTimer) {
    return
  }
  clearTimeout(lobbyMetaTimer)
  lobbyMetaTimer = null
}

async function loadConversations() {
  loading.value = true
  errorMessage.value = ''
  try {
    const data = await listConversations()
    conversations.value = sortByMessageTime(Array.isArray(data) ? data : [])
  } catch (error) {
    errorMessage.value = error.message || '加载会话失败'
  } finally {
    loading.value = false
  }
}

function applyPresence(payload) {
  const targetUserId = Number(payload?.userId || 0)
  if (!targetUserId) {
    return
  }
  let changed = false
  const nextList = conversations.value.map((item) => {
    if (Number(item.targetUserId) !== targetUserId) {
      return item
    }
    changed = true
    return {
      ...item,
      isOnline: payload.online === true,
      lastSeenAt: payload.online ? '' : (payload.lastSeenAt || item.lastSeenAt || '')
    }
  })
  if (changed) {
    conversations.value = nextList
  }
}

function applyIncomingMessage(message) {
  const conversationId = Number(message?.conversationId || 0)
  if (!conversationId) {
    return
  }
  const myUserId = Number(authStore.userId || 0)
  let found = false
  const nextList = conversations.value.map((item) => {
    if (Number(item.conversationId) !== conversationId) {
      return item
    }
    found = true
    const incomingType = String(message?.msgType || '').toUpperCase()
    const isRecall = incomingType === 'RECALL'
    const fromMe = Number(message?.senderId || 0) === myUserId
    return {
      ...item,
      lastMessage: buildMessagePreview(message),
      lastMessageTime: message?.createTime || item.lastMessageTime,
      isOnline: true,
      lastSeenAt: '',
      unreadCount: !isRecall && !fromMe ? (Number(item.unreadCount || 0) + 1) : Number(item.unreadCount || 0)
    }
  })
  if (!found) {
    scheduleReloadConversations()
    return
  }
  conversations.value = sortByMessageTime(nextList)
  loadNoticeUnread()
}

function handleWsPayload(payload) {
  const type = String(payload?.type || '').toUpperCase()
  if (!type) {
    return
  }
  if (type === 'PRESENCE') {
    applyPresence(payload?.data || {})
    scheduleLobbyMetaRefresh()
    return
  }
  if (type === 'MESSAGE') {
    applyIncomingMessage(payload?.data || {})
    return
  }
  if (type === 'LOBBY_MESSAGE') {
    scheduleLobbyMetaRefresh()
  }
}

function switchFilter(key, mode) {
  if (mode === 'base') {
    baseFilter.value = key
    return
  }
  if (key === 'unread') {
    unreadOnly.value = !unreadOnly.value
    return
  }
  if (key === 'online') {
    onlineOnly.value = !onlineOnly.value
  }
}

function openConversation(item) {
  router.push(`/chat/${item.conversationId}`)
}

function goLobby() {
  router.push('/lobby')
}

function goNotice() {
  router.push('/notice')
}

onMounted(async () => {
  await Promise.all([
    loadConversations(),
    loadFriendUserIds(),
    loadNoticeUnread(),
    loadLobbyMeta()
  ])
  connect()
  wsUnsubscribe = subscribe((payload) => handleWsPayload(payload))
})

onUnmounted(() => {
  if (wsUnsubscribe) {
    wsUnsubscribe()
    wsUnsubscribe = null
  }
  if (reloadTimer) {
    clearTimeout(reloadTimer)
    reloadTimer = null
  }
  clearLobbyMetaRefreshTimer()
})
</script>

<template>
  <section class="chat-page">
    <header class="session-toolbar page-header">
      <h1 class="session-title page-title">校园会话</h1>
      <span class="session-total">共 {{ filteredList.length }} 条</span>
    </header>

    <div class="tab-strip">
      <div class="tab-list">
        <button
          v-for="item in filterItems"
          :key="item.key"
          :class="['button tab-item', { 'tab-item-active': item.active }]"
          @click="switchFilter(item.key, item.mode)"
        >
          {{ item.label }}
        </button>
      </div>
      <div class="tab-tools">
        <div class="tab-search">
          <input
            v-model="keyword"
            class="input tab-search-input"
            maxlength="40"
            placeholder="搜索同学或会话"
          />
          <button class="button tab-search-action" @click="loadConversations">⌕</button>
        </div>
        <button class="button tab-tool tab-tool-notice" @click="goNotice">
          铃
          <span v-if="noticeUnreadTotal > 0" class="tab-tool-badge u-unread-blink">[{{ noticeUnreadTotal > 99 ? '99+' : noticeUnreadTotal }}]</span>
        </button>
      </div>
    </div>

    <div class="lobby-entry-wrap">
      <article class="session-row session-row-lobby" @click="goLobby">
        <div class="session-pin-bar"></div>
        <div class="lobby-main">
          <div class="lobby-header">
            <div class="session-user-meta lobby-user-meta">
              <div class="session-name-line">
                <strong class="session-name">校园广场</strong>
                <span class="lobby-channel-tag">[公共]</span>
              </div>
              <span class="session-presence">最近发言 {{ formatRelative(lobbyMeta.latestMessageAt) }}</span>
            </div>
            <div class="lobby-side">
              <span class="lobby-online-badge">[在线 {{ lobbyMeta.onlineCount }}]</span>
            </div>
          </div>
          <div class="lobby-preview-line">
            <span class="session-preview lobby-preview-text">{{ buildLobbyPreview() }}</span>
          </div>
        </div>
      </article>
    </div>

    <div class="session-board">
      <p v-if="errorMessage" class="error page-body">{{ errorMessage }}</p>
      <p v-else-if="loading" class="text-muted page-body">加载中...</p>
      <div v-else-if="filteredList.length === 0" class="empty">{{ emptyText }}</div>
      <article
        v-for="item in filteredList" :key="item.conversationId"
        class="session-row"
        @click="openConversation(item)"
      >
        <div class="session-pin-bar"></div>
        <div class="session-user">
          <div class="avatar-box">
            <img v-if="item.targetAvatar" :src="item.targetAvatar" alt="" class="avatar" />
            <div v-else class="avatar placeholder">狼</div>
          </div>
          <div class="session-user-meta">
            <div class="session-name-line">
              <strong class="session-name">{{ item.targetNickname || `同学${item.targetUserId}` }}</strong>
              <span
                v-if="item.targetEquippedTitleName"
                class="session-title-badge"
                :style="{ color: item.targetEquippedTitleColor || '' }"
              >
                [{{ item.targetEquippedTitleName }}]
              </span>
              <span v-if="item.unreadCount > 0" class="session-unread u-unread-blink">[{{ item.unreadCount }}]</span>
            </div>
            <span class="session-presence">{{ item.isOnline ? '在线' : `上次在线 ${formatRelative(item.lastSeenAt)}` }}</span>
          </div>
        </div>
        <div class="session-preview-col">
          <span class="session-preview">{{ item.lastMessage || '还没聊过' }}</span>
        </div>
        <span class="session-time">{{ formatClock(item.lastMessageTime) }}</span>
      </article>
    </div>
  </section>
</template>

<style scoped>
.chat-page {
  min-height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--retro-page-bg);
}

.session-toolbar {
  justify-content: space-between;
}

.session-title {
  min-width: 0;
}

.session-total {
  margin-left: auto;
  font-size: 12px;
  color: var(--retro-muted-text);
  white-space: nowrap;
}

.tab-strip {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 10px;
  padding: 8px 0 6px;
}

.tab-list {
  flex: 1;
  min-width: 0;
  display: flex;
  gap: 6px;
}

.tab-item {
  min-width: 50px;
  height: 30px;
  padding: 0 10px;
  font-size: 12px;
  border-color: var(--retro-panel-border);
  background: var(--retro-panel-bg);
  color: var(--retro-muted-text);
}

.tab-item-active {
  border-color: var(--retro-accent-color);
  color: var(--retro-page-text);
  background: var(--retro-theme-item-active-bg);
}

.tab-tools {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 6px;
}

.tab-search {
  width: 168px;
  height: 30px;
  border: 1px solid var(--retro-panel-border);
  background: var(--retro-panel-bg);
  display: flex;
  align-items: center;
}

.tab-search-input {
  flex: 1;
  min-width: 0;
  height: 100%;
  padding: 0 8px;
  border: 0;
  background: transparent;
  color: var(--retro-page-text);
  font-size: 12px;
}

.tab-search-action {
  width: 34px;
  height: 100%;
  border: 0;
  border-left: 1px solid var(--retro-panel-border);
  padding: 0;
  font-size: 12px;
  background: transparent;
}

.tab-tool {
  position: relative;
  width: 36px;
  height: 30px;
  padding: 0;
  border-color: var(--retro-panel-border);
  background: var(--retro-panel-bg);
  font-size: 12px;
}

.tab-tool-badge {
  position: absolute;
  right: -6px;
  top: -10px;
  font-size: 11px;
  line-height: 1.1;
  color: var(--retro-accent-text);
}

.lobby-entry-wrap {
  margin: 8px 10px 0;
  border: 1px solid var(--retro-panel-border);
  background: var(--retro-panel-bg);
  box-shadow: var(--retro-panel-shadow);
}

.session-board {
  margin: 8px 10px 14px;
  border: 1px solid var(--retro-panel-border);
  background: var(--retro-panel-bg);
  box-shadow: var(--retro-panel-shadow);
  overflow: hidden;
  flex: 1;
  min-height: 0;
  overflow-y: auto;
}

.session-row {
  position: relative;
  display: flex;
  align-items: center;
  min-height: 62px;
  padding: 8px 10px 8px 14px;
  border-bottom: 1px solid var(--retro-row-border);
  background: var(--retro-record-peer-bg);
  cursor: pointer;
  overflow: hidden;
}

.session-row:last-child {
  border-bottom: 0;
}

.session-row-lobby {
  align-items: flex-start;
  min-height: 0;
  padding-top: 10px;
  padding-bottom: 10px;
  background:
    repeating-linear-gradient(
      0deg,
      rgba(0, 0, 0, 0.025) 0,
      rgba(0, 0, 0, 0.025) 1px,
      rgba(0, 0, 0, 0) 1px,
      rgba(0, 0, 0, 0) 5px
    ),
    var(--retro-record-peer-bg);
}

.session-pin-bar {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
}

.avatar-box {
  width: 38px;
  height: 38px;
  border: 1px solid var(--retro-avatar-border);
  flex: 0 0 auto;
  background: var(--retro-theme-item-bg);
  overflow: hidden;
}

.avatar {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--retro-muted-text);
  font-size: 12px;
}

.session-user {
  width: 48%;
  min-width: 0;
  display: flex;
  align-items: flex-start;
}

.session-user-meta {
  min-width: 0;
  margin-left: 10px;
}

.session-name-line {
  display: flex;
  align-items: flex-start;
  gap: 4px;
}

.session-name {
  max-width: 138px;
  font-size: 15px;
  font-weight: 600;
  color: var(--retro-strong-text);
  line-height: 1.15;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.session-title-badge {
  max-width: 92px;
  font-size: 11px;
  line-height: 1.15;
  color: var(--retro-accent-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.session-unread {
  font-size: 11px;
  line-height: 1.15;
  color: var(--retro-accent-text);
  white-space: nowrap;
}

.session-presence {
  margin-top: 3px;
  font-size: 12px;
  line-height: 1.2;
  color: var(--retro-muted-text);
}

.session-preview-col {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  margin-left: 8px;
}

.session-preview {
  display: block;
  flex: 1;
  min-width: 0;
  font-size: 12px;
  line-height: 1.15;
  color: var(--retro-body-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.session-time {
  margin-left: 10px;
  width: 60px;
  text-align: right;
  font-size: 12px;
  line-height: 1.15;
  color: var(--retro-muted-text-2);
  white-space: nowrap;
}

.lobby-main {
  width: 100%;
  min-width: 0;
}

.lobby-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.lobby-user-meta {
  margin-left: 0;
}

.lobby-side {
  width: 68px;
  flex-shrink: 0;
  display: flex;
  justify-content: flex-end;
}

.lobby-online-badge {
  font-size: 11px;
  line-height: 1.15;
  color: var(--retro-accent-text);
  white-space: nowrap;
}

.lobby-channel-tag {
  font-size: 11px;
  line-height: 1.15;
  color: var(--retro-accent-text);
  margin-top: 1px;
}

.lobby-preview-line {
  margin-top: 6px;
  min-width: 0;
}

.lobby-preview-text {
  display: block;
}

</style>
