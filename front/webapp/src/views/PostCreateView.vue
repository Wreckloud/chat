<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import {
  createThread,
  getEditableThread,
  getLatestThreadDraft,
  publishThreadDraft,
  restoreThread,
  saveThreadDraft,
  updateThread
} from '@/api/modules'
import { uploadForumThreadImage, uploadForumThreadVideo } from '@/utils/media-upload'

const THREAD_IMAGE_MAX_COUNT = 9
const route = useRoute()
const router = useRouter()

const mode = ref('create')
const threadId = ref(0)
const draftThreadId = ref(0)
const loading = ref(false)
const errorMessage = ref('')
const title = ref('')
const content = ref('')
const mediaImages = ref([])
const mediaVideo = ref(null)
const uploadProgressPercent = ref(0)
const uploadProgressText = ref('')

const imageInputRef = ref(null)
const videoInputRef = ref(null)
const objectUrlSet = new Set()
let cleanSnapshot = ''

const pageTitle = computed(() => {
  if (mode.value === 'edit' && threadId.value > 0) {
    return '编辑主题'
  }
  if (mode.value === 'restore' && threadId.value > 0) {
    return '重新发布主题'
  }
  return '发布主题'
})

const pageHint = computed(() => {
  if (mode.value === 'edit') {
    return '编辑后会显示编辑时间'
  }
  if (mode.value === 'restore') {
    return '可编辑后重新发布'
  }
  if (draftThreadId.value > 0) {
    return '已载入上次草稿'
  }
  return '标题 120 字内'
})

const submitButtonText = computed(() => {
  if (loading.value) {
    return '处理中...'
  }
  if (mode.value === 'edit' && threadId.value > 0) {
    return '保存'
  }
  if (mode.value === 'restore' && threadId.value > 0) {
    return '重新发布'
  }
  return '发布'
})

const canSubmit = computed(() => {
  const hasTitle = Boolean(title.value.trim())
  const hasContentOrMedia = Boolean(content.value.trim())
    || mediaImages.value.length > 0
    || Boolean(mediaVideo.value)
  return !loading.value && hasTitle && hasContentOrMedia
})

function registerObjectUrl(url) {
  if (!url) {
    return
  }
  objectUrlSet.add(url)
}

function revokeObjectUrls() {
  objectUrlSet.forEach((url) => {
    URL.revokeObjectURL(url)
  })
  objectUrlSet.clear()
}

function safeMode(rawMode) {
  const normalized = String(rawMode || '').trim().toLowerCase()
  if (normalized === 'edit' || normalized === 'restore' || normalized === 'draft') {
    return normalized
  }
  return 'create'
}

function buildSnapshot() {
  const imageSignature = mediaImages.value
    .map((item) => `${item.source}:${item.key}:${item.previewUrl}`)
    .join('|')
  const video = mediaVideo.value
  const videoSignature = video
    ? `${video.source}:${video.key}:${video.posterKey}:${video.previewUrl}:${video.posterPreviewUrl}`
    : ''
  return JSON.stringify({
    title: title.value.trim(),
    content: content.value.trim(),
    imageSignature,
    videoSignature
  })
}

function markClean() {
  cleanSnapshot = buildSnapshot()
}

function hasUnsavedChanges() {
  return cleanSnapshot !== buildSnapshot()
}

function mapEditorToState(editor) {
  title.value = String(editor?.title || '')
  content.value = String(editor?.content || '')
  mediaImages.value = (Array.isArray(editor?.imageUrls) ? editor.imageUrls : [])
    .map((url, index) => {
      const safeUrl = String(url || '').trim()
      if (!safeUrl) {
        return null
      }
      return {
        source: 'remote',
        key: String((editor?.imageKeys || [])[index] || '').trim(),
        previewUrl: safeUrl,
        file: null
      }
    })
    .filter(Boolean)
  if (editor?.videoUrl) {
    mediaVideo.value = {
      source: 'remote',
      key: String(editor.videoKey || '').trim(),
      posterKey: String(editor.videoPosterKey || '').trim(),
      previewUrl: String(editor.videoUrl || '').trim(),
      posterPreviewUrl: String(editor.videoPosterUrl || '').trim(),
      file: null
    }
  } else {
    mediaVideo.value = null
  }
}

async function loadEditor() {
  const routeMode = safeMode(route.query.mode)
  const routeThreadId = Number(route.query.threadId || 0)
  mode.value = routeMode
  threadId.value = routeThreadId > 0 ? routeThreadId : 0
  draftThreadId.value = 0
  loading.value = true
  errorMessage.value = ''
  try {
    if ((routeMode === 'edit' || routeMode === 'restore' || routeMode === 'draft') && threadId.value > 0) {
      const editor = await getEditableThread(threadId.value)
      if (routeMode === 'draft' && String(editor?.status || '') === 'DRAFT') {
        mode.value = 'create'
        draftThreadId.value = Number(editor?.threadId || 0)
        threadId.value = 0
      }
      mapEditorToState(editor || {})
    } else {
      const draft = await getLatestThreadDraft()
      if (draft && String(draft?.status || '') === 'DRAFT') {
        draftThreadId.value = Number(draft?.threadId || 0)
        mapEditorToState(draft)
      }
    }
    markClean()
  } catch (error) {
    errorMessage.value = error.message || '初始化编辑器失败'
  } finally {
    loading.value = false
  }
}

function triggerImagePick() {
  if (loading.value) {
    return
  }
  imageInputRef.value?.click()
}

function triggerVideoPick() {
  if (loading.value) {
    return
  }
  videoInputRef.value?.click()
}

function clearMedia() {
  if (loading.value) {
    return
  }
  mediaImages.value.forEach((item) => {
    if (item.source === 'local') {
      URL.revokeObjectURL(item.previewUrl)
      objectUrlSet.delete(item.previewUrl)
    }
  })
  mediaImages.value = []
  if (mediaVideo.value && mediaVideo.value.source === 'local') {
    URL.revokeObjectURL(mediaVideo.value.previewUrl)
    objectUrlSet.delete(mediaVideo.value.previewUrl)
  }
  mediaVideo.value = null
}

function handleImagePicked(event) {
  const fileList = Array.from(event?.target?.files || [])
  event.target.value = ''
  if (fileList.length === 0) {
    return
  }
  if (mediaVideo.value) {
    errorMessage.value = '已选择视频，请先清除视频后再选图片'
    return
  }
  const remain = THREAD_IMAGE_MAX_COUNT - mediaImages.value.length
  if (remain <= 0) {
    errorMessage.value = `最多选择 ${THREAD_IMAGE_MAX_COUNT} 张图片`
    return
  }
  if (fileList.length > remain) {
    errorMessage.value = `最多还可选择 ${remain} 张图片`
  }
  const nextLocal = fileList.slice(0, remain).map((file) => {
    const previewUrl = URL.createObjectURL(file)
    registerObjectUrl(previewUrl)
    return {
      source: 'local',
      key: '',
      previewUrl,
      file
    }
  })
  mediaImages.value = mediaImages.value.concat(nextLocal)
}

function handleVideoPicked(event) {
  const file = event?.target?.files?.[0]
  event.target.value = ''
  if (!file) {
    return
  }
  if (mediaImages.value.length > 0) {
    errorMessage.value = '已选择图片，请先清除图片后再选视频'
    return
  }
  if (mediaVideo.value && mediaVideo.value.source === 'local') {
    URL.revokeObjectURL(mediaVideo.value.previewUrl)
    objectUrlSet.delete(mediaVideo.value.previewUrl)
  }
  const previewUrl = URL.createObjectURL(file)
  registerObjectUrl(previewUrl)
  mediaVideo.value = {
    source: 'local',
    key: '',
    posterKey: '',
    previewUrl,
    posterPreviewUrl: '',
    file
  }
}

function removeImage(index) {
  if (!Number.isInteger(index) || index < 0 || index >= mediaImages.value.length) {
    return
  }
  const target = mediaImages.value[index]
  if (target?.source === 'local') {
    URL.revokeObjectURL(target.previewUrl)
    objectUrlSet.delete(target.previewUrl)
  }
  const nextList = mediaImages.value.slice()
  nextList.splice(index, 1)
  mediaImages.value = nextList
}

async function buildThreadPayload({ draftMode = false } = {}) {
  const safeTitle = title.value.trim()
  const safeContent = content.value.trim()
  const hasMedia = mediaImages.value.length > 0 || Boolean(mediaVideo.value)
  if (!draftMode) {
    if (!safeTitle) {
      throw new Error('主题标题不能为空')
    }
    if (!safeContent && !hasMedia) {
      throw new Error('主题内容不能为空')
    }
  }

  const localImageList = mediaImages.value.filter((item) => item.source === 'local')
  const localVideo = mediaVideo.value && mediaVideo.value.source === 'local'
    ? mediaVideo.value
    : null
  const totalUploadCount = localImageList.length + (localVideo ? 1 : 0)
  let completedUploadCount = 0

  const updateProgress = (progress, text) => {
    uploadProgressPercent.value = Math.max(0, Math.min(100, Math.round(progress)))
    uploadProgressText.value = text
  }

  const imageKeys = []
  for (const item of mediaImages.value) {
    if (item.source === 'remote') {
      if (item.key) {
        imageKeys.push(item.key)
      }
      continue
    }
    const uploadRes = await uploadForumThreadImage(item.file, {
      onProgress: (progress) => {
        const ratio = totalUploadCount <= 0
          ? 0
          : (completedUploadCount + progress / 100) / totalUploadCount
        const percent = ratio * 90
        updateProgress(percent, `上传媒体 ${Math.round(percent)}%`)
      }
    })
    imageKeys.push(uploadRes.mediaKey)
    completedUploadCount += 1
    if (totalUploadCount > 0) {
      updateProgress((completedUploadCount / totalUploadCount) * 90, `上传媒体 ${Math.round((completedUploadCount / totalUploadCount) * 90)}%`)
    }
  }

  let videoKey = ''
  let videoPosterKey = ''
  if (mediaVideo.value) {
    if (mediaVideo.value.source === 'remote') {
      videoKey = mediaVideo.value.key
      videoPosterKey = mediaVideo.value.posterKey
    } else {
      const uploadRes = await uploadForumThreadVideo(mediaVideo.value.file, {
        onProgress: (progress) => {
          const ratio = totalUploadCount <= 0
            ? 0
            : (completedUploadCount + progress / 100) / totalUploadCount
          const percent = ratio * 90
          updateProgress(percent, `上传媒体 ${Math.round(percent)}%`)
        }
      })
      videoKey = uploadRes.mediaKey
      videoPosterKey = uploadRes.mediaPosterKey
      completedUploadCount += 1
      if (totalUploadCount > 0) {
        updateProgress((completedUploadCount / totalUploadCount) * 90, `上传媒体 ${Math.round((completedUploadCount / totalUploadCount) * 90)}%`)
      }
    }
  }

  const payload = {
    title: safeTitle,
    content: safeContent
  }
  if (imageKeys.length > 0) {
    payload.imageKeys = imageKeys
  }
  if (videoKey) {
    payload.videoKey = videoKey
  }
  if (videoPosterKey) {
    payload.videoPosterKey = videoPosterKey
  }
  return payload
}

async function saveDraft(silentError = false) {
  if (loading.value) {
    return false
  }
  loading.value = true
  try {
    uploadProgressPercent.value = 0
    uploadProgressText.value = '保存草稿中...'
    const payload = await buildThreadPayload({ draftMode: true })
    if (draftThreadId.value > 0 && draftThreadId.value !== threadId.value) {
      payload.threadId = draftThreadId.value
    }
    const editor = await saveThreadDraft(payload)
    const nextDraftThreadId = Number(editor?.threadId || 0)
    if (nextDraftThreadId > 0) {
      draftThreadId.value = nextDraftThreadId
    }
    markClean()
    return true
  } catch (error) {
    if (!silentError) {
      errorMessage.value = error.message || '保存草稿失败'
    }
    return false
  } finally {
    loading.value = false
    uploadProgressPercent.value = 0
    uploadProgressText.value = ''
  }
}

async function handleSubmit() {
  if (!canSubmit.value || loading.value) {
    return
  }
  loading.value = true
  errorMessage.value = ''
  try {
    uploadProgressPercent.value = 0
    uploadProgressText.value = '准备发布...'
    const payload = await buildThreadPayload({ draftMode: false })
    uploadProgressPercent.value = 95
    uploadProgressText.value = '提交主题中...'

    let created = null
    if (mode.value === 'edit' && threadId.value > 0) {
      created = await updateThread(threadId.value, payload)
    } else if (mode.value === 'restore' && threadId.value > 0) {
      created = await restoreThread(threadId.value, payload)
    } else if (draftThreadId.value > 0) {
      created = await publishThreadDraft(draftThreadId.value, payload)
    } else {
      created = await createThread(payload)
    }

    const createdThreadId = Number(created?.threadId || 0)
    if (!createdThreadId) {
      throw new Error('发布结果异常')
    }
    uploadProgressPercent.value = 100
    uploadProgressText.value = '发布完成，正在跳转...'
    markClean()
    router.replace(`/post/${createdThreadId}`)
  } catch (error) {
    errorMessage.value = error.message || '发布失败'
  } finally {
    loading.value = false
    uploadProgressPercent.value = 0
    uploadProgressText.value = ''
  }
}

async function handleBack() {
  if (!hasUnsavedChanges()) {
    router.back()
    return
  }
  const shouldSaveDraft = window.confirm('检测到未保存内容，确定保存草稿并离开？点击“取消”将直接丢弃。')
  if (shouldSaveDraft) {
    const saved = await saveDraft(false)
    if (!saved) {
      return
    }
  }
  router.back()
}

onBeforeRouteLeave(async () => {
  if (!hasUnsavedChanges() || loading.value) {
    return true
  }
  const shouldSaveDraft = window.confirm('检测到未保存内容，确定保存草稿并离开？点击“取消”将直接丢弃。')
  if (!shouldSaveDraft) {
    return true
  }
  const saved = await saveDraft(false)
  return saved
})

onMounted(() => {
  loadEditor()
})

onBeforeUnmount(() => {
  revokeObjectUrls()
})
</script>

<template>
  <section class="post-create-page">
    <header class="page-header">
      <button class="button page-action-btn" @click="handleBack">返回</button>
      <h1 class="page-title">{{ pageTitle }}</h1>
      <span class="page-action">{{ pageHint }}</span>
    </header>

    <div class="page-body editor-body">
      <p v-if="errorMessage" class="error">{{ errorMessage }}</p>

      <article class="card editor-card">
        <div class="field-row">
          <span class="field-label">标题</span>
          <input
            v-model="title"
            class="input field-input"
            maxlength="120"
            placeholder="输入主题标题"
          />
        </div>

        <div class="field-row column">
          <span class="field-label">正文</span>
          <span class="text-muted">最多 5000 字，可仅发媒体</span>
        </div>
        <textarea
          v-model="content"
          class="editor-textarea"
          maxlength="5000"
          placeholder="写下你的主题内容..."
        />

        <div class="field-row column media-head-row">
          <span class="field-label">媒体</span>
          <span class="text-muted">9 图或 1 视频（二选一）</span>
        </div>

        <div v-if="mediaImages.length > 0" class="media-grid">
          <div v-for="(item, index) in mediaImages" :key="`${item.previewUrl}_${index}`" class="media-image-item">
            <img :src="item.previewUrl" alt="" class="media-image" />
            <button class="media-remove" @click.stop="removeImage(index)">×</button>
          </div>
        </div>

        <div v-if="mediaVideo" class="media-video-wrap">
          <video
            class="media-video"
            :src="mediaVideo.previewUrl"
            :poster="mediaVideo.posterPreviewUrl || ''"
            controls
            preload="metadata"
          />
        </div>
      </article>
    </div>

    <footer class="post-submit-dock">
      <div class="post-submit-main-row">
        <button class="button button-primary publish-button" :disabled="!canSubmit || loading" @click="handleSubmit">
          {{ submitButtonText }}
        </button>
        <div class="post-submit-actions">
          <template v-if="mediaImages.length === 0 && !mediaVideo">
            <button class="button action-btn" :disabled="loading" @click="triggerImagePick">图片</button>
            <button class="button action-btn" :disabled="loading" @click="triggerVideoPick">视频</button>
          </template>
          <button v-else class="button action-btn" :disabled="loading" @click="clearMedia">清除媒体</button>
        </div>
      </div>
      <div v-if="loading && uploadProgressText" class="progress-wrap">
        <div class="progress-head">
          <span>{{ uploadProgressText }}</span>
          <span>{{ uploadProgressPercent }}%</span>
        </div>
        <div class="progress-track">
          <div class="progress-fill" :style="{ width: `${uploadProgressPercent}%` }" />
        </div>
      </div>
    </footer>

    <input ref="imageInputRef" class="hidden-input" type="file" accept="image/*" multiple @change="handleImagePicked" />
    <input ref="videoInputRef" class="hidden-input" type="file" accept="video/*" @change="handleVideoPicked" />
  </section>
</template>

<style scoped>
.post-create-page {
  min-height: 100%;
  display: flex;
  flex-direction: column;
}

.editor-body {
  padding-bottom: 86px;
}

.editor-card {
  padding: 10px;
}

.field-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
}

.field-row.column {
  align-items: flex-start;
  flex-direction: column;
  gap: 4px;
}

.field-label {
  color: var(--text-main);
  font-size: 14px;
}

.field-input {
  flex: 1;
}

.editor-textarea {
  width: 100%;
  min-height: 180px;
  border: 1px solid var(--line);
  background: var(--retro-theme-item-bg);
  color: var(--retro-body-text);
  padding: 10px;
  resize: vertical;
  outline: none;
  margin-bottom: 8px;
  font-family: inherit;
}

.media-head-row {
  margin-top: 8px;
}

.media-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.media-image-item {
  position: relative;
  border: 1px solid var(--line);
  height: 100px;
  background: var(--retro-theme-item-bg);
}

.media-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.media-remove {
  position: absolute;
  top: 2px;
  right: 2px;
  width: 20px;
  height: 20px;
  border: 1px solid var(--line);
  background: rgba(0, 0, 0, 0.55);
  color: #fff;
  cursor: pointer;
}

.media-video-wrap {
  border: 1px solid var(--line);
  background: var(--retro-theme-item-bg);
}

.media-video {
  width: 100%;
  max-height: 320px;
}

.post-submit-dock {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 52px;
  border-top: 1px solid var(--line);
  background: var(--panel);
  padding: 8px 10px;
  z-index: 30;
}

.post-submit-main-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.publish-button {
  flex: 1;
  height: 36px;
}

.post-submit-actions {
  display: flex;
  gap: 6px;
}

.action-btn {
  height: 36px;
  min-width: 68px;
}

.progress-wrap {
  margin-top: 8px;
}

.progress-head {
  display: flex;
  justify-content: space-between;
  color: var(--text-sub);
  font-size: 12px;
  margin-bottom: 4px;
}

.progress-track {
  width: 100%;
  height: 6px;
  border: 1px solid var(--line);
  background: var(--retro-theme-item-bg);
}

.progress-fill {
  height: 100%;
  background: var(--retro-accent-color);
}

</style>
