<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { listMessages, markConversationRead } from '@/api/modules'
import { formatClock } from '@/utils/time'
import { uploadChatFile, uploadChatImage, uploadChatVideo } from '@/utils/media-upload'
import { connect, sendWithAck, subscribe } from '@/realtime/ws-client'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const conversationId = computed(() => Number(route.params.conversationId))
const loading = ref(false)
const sending = ref(false)
const mediaSending = ref(false)
const errorMessage = ref('')
const messages = ref([])
const text = ref('')
const messageListRef = ref(null)
const morePanelVisible = ref(false)
const uploadText = ref('')
const uploadProgress = ref(0)

const imageInputRef = ref(null)
const videoInputRef = ref(null)
const fileInputRef = ref(null)

const myUserId = computed(() => Number(authStore.userId || 0))

let wsUnsubscribe = null

const targetProfile = computed(() => {
  const targetMessage = messages.value.find((item) => Number(item?.senderId || 0) !== myUserId.value)
  return {
    nickname: String(targetMessage?.senderNickname || '').trim() || '同学会话',
    titleName: String(targetMessage?.senderEquippedTitleName || '').trim(),
    titleColor: String(targetMessage?.senderEquippedTitleColor || '').trim()
  }
})

function isSelf(item) {
  return Number(item?.senderId || 0) === myUserId.value
}

function senderName(item) {
  return String(item?.senderNickname || '').trim() || `同学${item?.senderId || ''}`
}

function normalizeFileMessageName(fileName) {
  const trimmed = String(fileName || '').trim()
  if (!trimmed) {
    return '文件'
  }
  return trimmed.length > 120 ? `${trimmed.slice(0, 120)}...` : trimmed
}

async function loadMessages() {
  if (!conversationId.value) {
    return
  }
  loading.value = true
  errorMessage.value = ''
  try {
    const pageData = await listMessages(conversationId.value, 1, 80)
    messages.value = Array.isArray(pageData.records) ? pageData.records : []
    await markConversationRead(conversationId.value)
    await nextTick()
    scrollToBottom()
  } catch (error) {
    errorMessage.value = error.message || '加载消息失败'
  } finally {
    loading.value = false
  }
}

function findMessageIndex(target) {
  const targetMessageId = Number(target?.messageId || 0)
  const targetClientMsgId = String(target?.clientMsgId || '')
  return messages.value.findIndex((item) => {
    const messageId = Number(item?.messageId || 0)
    if (targetMessageId > 0 && messageId > 0 && targetMessageId === messageId) {
      return true
    }
    const clientMsgId = String(item?.clientMsgId || '')
    return Boolean(targetClientMsgId) && targetClientMsgId === clientMsgId
  })
}

function upsertMessage(nextMessage) {
  const index = findMessageIndex(nextMessage)
  if (index < 0) {
    messages.value = messages.value.concat(nextMessage)
    return
  }
  const nextList = messages.value.slice()
  nextList[index] = {
    ...nextList[index],
    ...nextMessage
  }
  messages.value = nextList
}

function resolveMessageType(item) {
  return String(item?.msgType || 'TEXT').toUpperCase()
}

function buildFileLabel(item) {
  const fileName = String(item?.content || '文件').trim() || '文件'
  const size = Number(item?.mediaSize || 0)
  if (!size || Number.isNaN(size)) {
    return `[文件] ${fileName}`
  }
  const mb = size / (1024 * 1024)
  if (mb < 1) {
    return `[文件] ${fileName} (${(size / 1024).toFixed(1)}KB)`
  }
  return `[文件] ${fileName} (${mb.toFixed(1)}MB)`
}

function scrollToBottom() {
  const element = messageListRef.value
  if (!element) {
    return
  }
  element.scrollTop = element.scrollHeight
}

function handleWsPayload(payload) {
  const type = String(payload?.type || '').toUpperCase()
  if (type !== 'MESSAGE') {
    return
  }
  const message = payload?.data || {}
  if (Number(message.conversationId) !== Number(conversationId.value)) {
    return
  }
  upsertMessage(message)
  nextTick(() => scrollToBottom())
  if (Number(message.senderId || 0) !== myUserId.value) {
    markConversationRead(conversationId.value).catch(() => {})
  }
}

async function sendWsMessage(payload, options = {}) {
  const message = await sendWithAck({
    type: 'SEND',
    conversationId: Number(conversationId.value),
    ...payload
  }, options)
  upsertMessage(message)
  await nextTick()
  scrollToBottom()
}

async function handleSend() {
  const content = text.value.trim()
  if (!content || sending.value || mediaSending.value) {
    return
  }
  sending.value = true
  try {
    await sendWsMessage({
      msgType: 'TEXT',
      content
    })
    text.value = ''
  } catch (error) {
    errorMessage.value = error.message || '发送失败'
  } finally {
    sending.value = false
  }
}

function toggleMorePanel() {
  if (sending.value || mediaSending.value) {
    return
  }
  morePanelVisible.value = !morePanelVisible.value
}

function hideMorePanel() {
  morePanelVisible.value = false
}

function triggerImagePick() {
  imageInputRef.value?.click()
}

function triggerVideoPick() {
  videoInputRef.value?.click()
}

function triggerFilePick() {
  fileInputRef.value?.click()
}

async function sendImageFiles(fileList) {
  const files = Array.from(fileList || [])
  if (files.length === 0) {
    return
  }
  mediaSending.value = true
  try {
    for (const file of files) {
      uploadText.value = `上传图片: ${file.name}`
      uploadProgress.value = 0
      const media = await uploadChatImage(file, {
        onProgress: (progress) => {
          uploadProgress.value = progress
        }
      })
      await sendWsMessage({
        msgType: 'IMAGE',
        mediaKey: media.mediaKey,
        mediaWidth: media.mediaWidth,
        mediaHeight: media.mediaHeight,
        mediaSize: media.mediaSize,
        mediaMimeType: media.mediaMimeType
      })
    }
  } finally {
    mediaSending.value = false
    uploadText.value = ''
    uploadProgress.value = 0
  }
}

async function sendVideoFile(file) {
  if (!file) {
    return
  }
  mediaSending.value = true
  try {
    uploadText.value = `上传视频: ${file.name}`
    uploadProgress.value = 0
    const media = await uploadChatVideo(file, {
      onProgress: (progress) => {
        uploadProgress.value = progress
      }
    })
    await sendWsMessage({
      msgType: 'VIDEO',
      mediaKey: media.mediaKey,
      mediaPosterKey: media.mediaPosterKey,
      mediaWidth: media.mediaWidth,
      mediaHeight: media.mediaHeight,
      mediaSize: media.mediaSize,
      mediaMimeType: media.mediaMimeType
    })
  } finally {
    mediaSending.value = false
    uploadText.value = ''
    uploadProgress.value = 0
  }
}

async function sendFileList(fileList) {
  const files = Array.from(fileList || [])
  if (files.length === 0) {
    return
  }
  mediaSending.value = true
  try {
    for (const file of files) {
      uploadText.value = `上传文件: ${file.name}`
      uploadProgress.value = 0
      const media = await uploadChatFile(file, {
        onProgress: (progress) => {
          uploadProgress.value = progress
        }
      })
      await sendWsMessage({
        msgType: 'FILE',
        content: normalizeFileMessageName(media.fileName),
        mediaKey: media.mediaKey,
        mediaSize: media.mediaSize,
        mediaMimeType: media.mediaMimeType
      })
    }
  } finally {
    mediaSending.value = false
    uploadText.value = ''
    uploadProgress.value = 0
  }
}

async function handleImagePicked(event) {
  const files = event?.target?.files
  event.target.value = ''
  hideMorePanel()
  if (!files || files.length === 0) {
    return
  }
  try {
    await sendImageFiles(files)
  } catch (error) {
    errorMessage.value = error.message || '图片发送失败'
  }
}

async function handleVideoPicked(event) {
  const file = event?.target?.files?.[0]
  event.target.value = ''
  hideMorePanel()
  if (!file) {
    return
  }
  try {
    await sendVideoFile(file)
  } catch (error) {
    errorMessage.value = error.message || '视频发送失败'
  }
}

async function handleFilePicked(event) {
  const files = event?.target?.files
  event.target.value = ''
  hideMorePanel()
  if (!files || files.length === 0) {
    return
  }
  try {
    await sendFileList(files)
  } catch (error) {
    errorMessage.value = error.message || '文件发送失败'
  }
}

async function handleShareLink() {
  hideMorePanel()
  const value = window.prompt('输入要分享的链接')
  if (!value) {
    return
  }
  const link = String(value || '').trim()
  if (!/^https?:\/\//i.test(link)) {
    errorMessage.value = '链接格式不正确，请输入 http(s) 开头的校园资讯链接'
    return
  }
  try {
    await sendWsMessage({
      msgType: 'TEXT',
      content: link
    })
  } catch (error) {
    errorMessage.value = error.message || '链接发送失败'
  }
}

onMounted(async () => {
  await loadMessages()
  connect()
  wsUnsubscribe = subscribe((payload) => handleWsPayload(payload))
})

onBeforeUnmount(() => {
  if (wsUnsubscribe) {
    wsUnsubscribe()
    wsUnsubscribe = null
  }
})
</script>

<template>
  <section class="im-page chat-detail-page">
    <header class="im-header page-header">
      <button class="button page-action-btn" @click="router.back()">返回</button>
      <div class="im-header-main">
        <div class="im-header-name-line">
          <strong class="im-header-name">{{ targetProfile.nickname }}</strong>
          <span
            v-if="targetProfile.titleName"
            class="im-header-title"
            :style="{ color: targetProfile.titleColor || '' }"
          >
            [{{ targetProfile.titleName }}]
          </span>
        </div>
      </div>
      <span class="im-header-action">私聊</span>
    </header>

    <div ref="messageListRef" class="im-message-list">
      <p v-if="errorMessage" class="error page-body">{{ errorMessage }}</p>
      <p v-if="loading" class="text-muted page-body">加载中...</p>
      <p v-if="!loading && messages.length === 0" class="empty page-body">今天还没有聊天内容</p>

      <article
        v-for="item in messages"
        :key="item.messageId || item.clientMsgId"
        :class="['im-cluster', { 'im-cluster-self': isSelf(item) }]"
      >
        <div class="im-avatar-box">
          <img v-if="item.senderAvatar" :src="item.senderAvatar" alt="" class="im-avatar" />
          <div v-else class="im-avatar placeholder">狼</div>
        </div>

        <div class="im-message-col">
          <div class="im-meta-col">
            <div class="im-name-line">
              <span
                v-if="isSelf(item) && item.senderEquippedTitleName"
                class="im-title-badge"
                :style="{ color: item.senderEquippedTitleColor || '' }"
              >
                [{{ item.senderEquippedTitleName }}]
              </span>
              <span class="im-name">{{ senderName(item) }}</span>
              <span
                v-if="!isSelf(item) && item.senderEquippedTitleName"
                class="im-title-badge"
                :style="{ color: item.senderEquippedTitleColor || '' }"
              >
                [{{ item.senderEquippedTitleName }}]
              </span>
            </div>
            <div class="im-time-line">
              <span class="im-meta-time">{{ formatClock(item.createTime) }}</span>
            </div>
          </div>

          <div class="im-line">
            <div v-if="item.replyToMessageId && item.replyToPreview" class="im-reply-quote">
              <span class="im-reply-quote-label">回复</span>
              <span class="im-reply-quote-text">{{ item.replyToPreview }}</span>
            </div>

            <img
              v-if="resolveMessageType(item) === 'IMAGE' && item.mediaUrl"
              :src="item.mediaUrl"
              alt=""
              class="im-image"
            />
            <video
              v-else-if="resolveMessageType(item) === 'VIDEO' && item.mediaUrl"
              class="im-video-player"
              :src="item.mediaUrl"
              :poster="item.mediaPosterUrl || ''"
              controls
              preload="metadata"
            />
            <a
              v-else-if="resolveMessageType(item) === 'FILE' && item.mediaUrl"
              :href="item.mediaUrl"
              class="im-text im-file"
              target="_blank"
              rel="noreferrer"
            >
              {{ buildFileLabel(item) }}
            </a>
            <span v-else-if="resolveMessageType(item) === 'RECALL'" class="im-text im-recall-text">{{ item.content || '该消息已撤回' }}</span>
            <span v-else class="im-text">{{ item.content || '[新消息]' }}</span>

            <span v-if="Number(item.deliveryStatus || 1) === 2 && isSelf(item)" class="im-send-failed">发送未成功</span>
          </div>
        </div>
      </article>
    </div>

    <footer class="im-composer">
      <div class="im-composer-main-row">
        <input
          v-model="text"
          class="input im-composer-input"
          placeholder="和同学聊聊今天的课程..."
          @keyup.enter="handleSend"
        />
        <div class="im-composer-actions">
          <button class="button im-more-btn" :disabled="sending || mediaSending" @click="toggleMorePanel">更多</button>
          <button class="button button-primary im-send-btn" :disabled="sending || mediaSending || !text.trim()" @click="handleSend">
            {{ sending ? '发送中' : '发送' }}
          </button>
        </div>
      </div>

      <div v-if="mediaSending && uploadText" class="im-upload-progress">
        <span>{{ uploadText }}</span>
        <span>{{ uploadProgress }}%</span>
      </div>

      <div v-if="morePanelVisible" class="im-more-panel">
        <button class="button im-more-action" @click="triggerImagePick">多图</button>
        <button class="button im-more-action" @click="triggerVideoPick">视频</button>
        <button class="button im-more-action" @click="triggerFilePick">文件</button>
        <button class="button im-more-action" @click="handleShareLink">链接</button>
      </div>
    </footer>

    <input ref="imageInputRef" class="hidden-input" type="file" accept="image/*" multiple @change="handleImagePicked" />
    <input ref="videoInputRef" class="hidden-input" type="file" accept="video/*" @change="handleVideoPicked" />
    <input ref="fileInputRef" class="hidden-input" type="file" multiple @change="handleFilePicked" />
  </section>
</template>

<style scoped>
.chat-detail-page {
  min-height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--retro-page-bg);
}

.im-header {
  gap: 10px;
}

.im-header-main {
  flex: 1;
  min-width: 0;
}

.im-header-name-line {
  display: flex;
  align-items: flex-start;
  gap: 4px;
}

.im-header-name {
  min-width: 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--retro-page-text);
  line-height: 1.15;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.im-header-title {
  font-size: 11px;
  line-height: 1.15;
  color: var(--retro-accent-text);
  white-space: nowrap;
}

.im-header-action {
  font-size: 12px;
  color: var(--retro-muted-text-2);
  white-space: nowrap;
}

.im-message-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 8px 10px 0;
}

.im-cluster {
  display: flex;
  align-items: flex-start;
  margin-bottom: 8px;
}

.im-cluster-self {
  flex-direction: row-reverse;
}

.im-avatar-box {
  width: 38px;
  height: 38px;
  border: 1px solid var(--retro-avatar-border);
  background: var(--retro-theme-item-bg);
  flex-shrink: 0;
  overflow: hidden;
}

.im-avatar {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.im-avatar.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--retro-muted-text);
  font-size: 12px;
}

.im-message-col {
  flex: 0 1 auto;
  min-width: 0;
  max-width: min(560px, calc(100% - 56px));
  margin-left: 10px;
  display: flex;
  flex-direction: column;
}

.im-cluster-self .im-message-col {
  margin-left: 0;
  margin-right: 10px;
  align-items: flex-end;
}

.im-meta-col {
  width: 100%;
}

.im-name-line {
  display: flex;
  align-items: flex-start;
  gap: 4px;
}

.im-cluster-self .im-name-line {
  justify-content: flex-end;
}

.im-name {
  max-width: 260px;
  font-size: 14px;
  line-height: 1.15;
  font-weight: 600;
  color: var(--retro-strong-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.im-title-badge {
  max-width: 92px;
  font-size: 11px;
  line-height: 1.15;
  color: var(--retro-accent-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.im-time-line {
  margin-top: 2px;
  display: flex;
}

.im-cluster-self .im-time-line {
  justify-content: flex-end;
}

.im-meta-time {
  font-size: 12px;
  line-height: 1.2;
  color: var(--retro-muted-text-2);
  white-space: nowrap;
}

.im-line {
  margin-top: 4px;
  width: 100%;
}

.im-cluster-self .im-line {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.im-reply-quote {
  margin-bottom: 6px;
  padding: 6px 8px;
  border-left: 3px solid var(--retro-theme-item-active-border);
  background: var(--retro-theme-item-active-bg);
  max-width: 340px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.im-cluster-self .im-reply-quote {
  border-left: 0;
  border-right: 3px solid var(--retro-theme-item-active-border);
  text-align: right;
}

.im-reply-quote-label {
  font-size: 11px;
  line-height: 1.35;
  color: var(--retro-muted-text-2);
}

.im-reply-quote-text {
  font-size: 13px;
  line-height: 1.4;
  color: var(--retro-record-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.im-text {
  display: block;
  max-width: 340px;
  font-size: 14px;
  line-height: 1.5;
  color: var(--retro-record-text);
  white-space: pre-wrap;
  word-break: break-word;
}

.im-cluster-self .im-text {
  text-align: right;
}

.im-file {
  color: var(--retro-link-text);
  text-decoration: underline;
}

.im-recall-text {
  color: var(--retro-muted-text-2);
}

.im-image {
  display: block;
  width: min(240px, 100%);
  max-height: 320px;
  border: 1px solid var(--retro-avatar-border);
  background: var(--retro-theme-item-bg);
}

.im-video-player {
  display: block;
  width: min(320px, 100%);
  max-height: 260px;
  border: 1px solid var(--retro-avatar-border);
  background: var(--retro-theme-item-bg);
}

.im-send-failed {
  display: block;
  margin-top: 2px;
  font-size: 11px;
  line-height: 1.2;
  color: var(--retro-btn-danger-text);
}

.im-composer {
  border-top: 1px solid var(--retro-composer-border);
  background: var(--retro-composer-bg);
  padding: 8px 10px;
}

.im-composer-main-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.im-composer-input {
  flex: 1;
  min-width: 0;
  height: 34px;
}

.im-composer-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.im-more-btn,
.im-send-btn {
  min-width: 62px;
  height: 34px;
}

.im-upload-progress {
  margin-top: 6px;
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--retro-muted-text);
}

.im-more-panel {
  margin-top: 8px;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
}

.im-more-action {
  height: 34px;
}
</style>
