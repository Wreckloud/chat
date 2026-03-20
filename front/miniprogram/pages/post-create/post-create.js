/**
 * 发布/编辑主题页面
 */
const request = require('../../utils/request')
const auth = require('../../utils/auth')
const { uploadForumThreadImage, uploadForumThreadVideo } = require('../../utils/oss')
const { toastError } = require('../../utils/ui')
const { applyPageTheme } = require('../../utils/page-theme')
const imHelper = require('../../utils/im-helper')
const pageLifecycleHelper = require('../../utils/page-lifecycle-helper')
const { COMMON_KAOMOJI_LIST, appendKaomojiWithSpace } = require('../../utils/kaomoji')

const THREAD_IMAGE_MAX_COUNT = 9

function resolveChooseImageErrorMessage(error) {
  const raw = String((error && error.errMsg) || (error && error.message) || '').toLowerCase()
  if (!raw) {
    return '选择图片失败'
  }
  if (raw.includes('count') || raw.includes('超出') || raw.includes('exceed')) {
    return `最多选择 ${THREAD_IMAGE_MAX_COUNT} 张图片`
  }
  return '选择图片失败'
}

function resolveMode(rawMode) {
  const normalized = String(rawMode || '').trim().toLowerCase()
  if (normalized === 'edit') {
    return 'edit'
  }
  if (normalized === 'restore') {
    return 'restore'
  }
  if (normalized === 'draft') {
    return 'draft'
  }
  return 'create'
}

function normalizeImageTempFile(file) {
  if (!file || !file.tempFilePath) {
    return null
  }
  return {
    source: 'local',
    key: '',
    previewUrl: file.tempFilePath,
    path: file.tempFilePath,
    size: Number(file.size) || 0,
    width: Number(file.width) || 0,
    height: Number(file.height) || 0
  }
}

function normalizeVideoTempFile(file) {
  if (!file || !file.tempFilePath) {
    return null
  }
  const posterPath = String(file.thumbTempFilePath || file.coverTempFilePath || '').trim()
  return {
    source: 'local',
    key: '',
    posterKey: '',
    previewUrl: file.tempFilePath,
    path: file.tempFilePath,
    posterPreviewUrl: posterPath,
    posterPath,
    size: Number(file.size) || 0,
    width: Number(file.width) || 0,
    height: Number(file.height) || 0,
    duration: Number(file.duration) || 0
  }
}

function buildRemoteImageMedia(url, key) {
  const normalizedUrl = String(url || '').trim()
  if (!normalizedUrl) {
    return null
  }
  return {
    source: 'remote',
    key: String(key || '').trim(),
    previewUrl: normalizedUrl,
    path: '',
    size: 0,
    width: 0,
    height: 0
  }
}

function buildRemoteVideoMedia(videoUrl, videoKey, posterUrl, posterKey) {
  const normalizedVideoUrl = String(videoUrl || '').trim()
  if (!normalizedVideoUrl) {
    return null
  }
  return {
    source: 'remote',
    key: String(videoKey || '').trim(),
    posterKey: String(posterKey || '').trim(),
    previewUrl: normalizedVideoUrl,
    path: '',
    posterPreviewUrl: String(posterUrl || '').trim(),
    posterPath: '',
    size: 0,
    width: 0,
    height: 0,
    duration: 0
  }
}

function buildEditorSnapshot(data) {
  const imageSignature = (Array.isArray(data.mediaImages) ? data.mediaImages : [])
    .map(item => `${item.source || ''}:${item.key || ''}:${item.previewUrl || ''}`)
    .join('|')
  const video = data.mediaVideo || null
  const videoSignature = video
    ? `${video.source || ''}:${video.key || ''}:${video.posterKey || ''}:${video.previewUrl || ''}:${video.posterPreviewUrl || ''}`
    : ''
  return JSON.stringify({
    title: String(data.title || '').trim(),
    content: String(data.content || '').trim(),
    imageSignature,
    videoSignature
  })
}

function normalizeEditorImageList(editor) {
  const imageUrls = Array.isArray(editor && editor.imageUrls) ? editor.imageUrls : []
  const imageKeys = Array.isArray(editor && editor.imageKeys) ? editor.imageKeys : []
  const list = []
  for (let index = 0; index < imageUrls.length; index++) {
    const media = buildRemoteImageMedia(imageUrls[index], imageKeys[index])
    if (media) {
      list.push(media)
    }
  }
  return list
}

function normalizeEditorVideo(editor) {
  if (!editor) {
    return null
  }
  return buildRemoteVideoMedia(
    editor.videoUrl,
    editor.videoKey,
    editor.videoPosterUrl,
    editor.videoPosterKey
  )
}

function showLeaveActionSheet() {
  return new Promise((resolve) => {
    wx.showActionSheet({
      itemList: ['保存草稿并退出', '不保存并退出'],
      success(res) {
        resolve(Number.isInteger(res.tapIndex) ? res.tapIndex : -1)
      },
      fail() {
        resolve(-1)
      }
    })
  })
}

Page({
  data: {
    mode: 'create',
    threadId: 0,
    draftThreadId: 0,
    pageTitle: '发布主题',
    pageHint: '标题 120 字内',
    submitButtonText: '发布',

    title: '',
    content: '',
    emojiPanelVisible: false,
    emojiList: COMMON_KAOMOJI_LIST,
    mediaImages: [],
    mediaVideo: null,
    canSubmit: false,

    loading: false,
    uploadProgressPercent: 0,
    uploadProgressText: '',

    themeClass: 'theme-retro-blue'
  },

  onLoad(options) {
    pageLifecycleHelper.handleProtectedPageLoad(auth, {
      beforeInit: () => {
        const mode = resolveMode(options && options.mode)
        const threadId = Number(options && options.threadId) || 0
        this.setData({
          mode,
          threadId
        })
        return true
      },
      afterInit: () => {
        this.initEditor()
      }
    })
  },

  onShow() {
    pageLifecycleHelper.handleProtectedPageShow(auth, {
      afterShow: () => {
        this.applyTheme()
      }
    })
  },

  onBackPress() {
    if (this.skipBackIntercept) {
      return false
    }
    if (this.data.loading || this.backPrompting) {
      return true
    }
    if (!this.hasUnsavedChanges()) {
      return false
    }
    this.handleBackWithDraftPrompt()
    return true
  },

  async initEditor() {
    try {
      if (this.data.mode === 'edit' && this.data.threadId > 0) {
        await this.loadEditableThread(this.data.threadId)
      } else if (this.data.mode === 'restore' && this.data.threadId > 0) {
        await this.loadEditableDeletedThread(this.data.threadId)
      } else if (this.data.mode === 'draft' && this.data.threadId > 0) {
        await this.loadEditableDraft(this.data.threadId)
      } else {
        await this.loadLatestDraft()
      }
    } catch (error) {
      toastError(error, '初始化编辑器失败')
    } finally {
      this.updatePageMeta()
      this.syncCanSubmitState()
      this.recordCleanSnapshot()
    }
  },

  async loadEditableThread(threadId) {
    const res = await request.get(`/forum/threads/${threadId}/editable`)
    const editor = res.data || null
    if (!editor) {
      return
    }
    this.setData({
      title: String(editor.title || ''),
      content: String(editor.content || ''),
      mediaImages: normalizeEditorImageList(editor),
      mediaVideo: normalizeEditorVideo(editor),
      draftThreadId: 0
    })
  },

  async loadEditableDeletedThread(threadId) {
    const res = await request.get(`/forum/threads/${threadId}/editable`)
    const editor = res.data || null
    if (!editor || editor.status !== 'DELETED') {
      throw new Error('仅支持重新发布垃圾站主题')
    }
    this.setData({
      title: String(editor.title || ''),
      content: String(editor.content || ''),
      mediaImages: normalizeEditorImageList(editor),
      mediaVideo: normalizeEditorVideo(editor),
      draftThreadId: 0
    })
  },

  async loadEditableDraft(threadId) {
    const res = await request.get(`/forum/threads/${threadId}/editable`)
    const editor = res.data || null
    if (!editor || editor.status !== 'DRAFT') {
      return
    }
    this.setData({
      mode: 'create',
      threadId: 0,
      draftThreadId: Number(editor.threadId) || 0,
      title: String(editor.title || ''),
      content: String(editor.content || ''),
      mediaImages: normalizeEditorImageList(editor),
      mediaVideo: normalizeEditorVideo(editor)
    })
  },

  async loadLatestDraft() {
    const res = await request.get('/forum/threads/drafts/latest')
    const editor = res.data || null
    if (!editor || editor.status !== 'DRAFT') {
      return
    }
    this.setData({
      draftThreadId: Number(editor.threadId) || 0,
      title: String(editor.title || ''),
      content: String(editor.content || ''),
      mediaImages: normalizeEditorImageList(editor),
      mediaVideo: normalizeEditorVideo(editor)
    })
  },

  onTitleInput(e) {
    this.setData({ title: e.detail.value || '' }, () => {
      this.syncCanSubmitState()
    })
  },

  onContentInput(e) {
    this.setData({ content: e.detail.value || '' }, () => {
      this.syncCanSubmitState()
    })
  },

  toggleEmojiPanel() {
    this.setData({ emojiPanelVisible: !this.data.emojiPanelVisible })
  },

  onSelectEmoji(e) {
    const dataset = e && e.currentTarget ? (e.currentTarget.dataset || {}) : {}
    const emoji = String(dataset.emoji || '').trim()
    if (!emoji) {
      return
    }
    const nextContent = appendKaomojiWithSpace(this.data.content, emoji)
    this.setData({ content: nextContent }, () => {
      this.syncCanSubmitState()
    })
  },

  async chooseThreadImages() {
    if (this.data.loading || this.data.mediaVideo) {
      if (this.data.mediaVideo) {
        toastError('已选择视频，请先清除视频后再选图片')
      }
      return
    }
    const remain = THREAD_IMAGE_MAX_COUNT - this.data.mediaImages.length
    if (remain <= 0) {
      toastError(`最多选择 ${THREAD_IMAGE_MAX_COUNT} 张图片`)
      return
    }
    try {
      const chooseRes = await imHelper.chooseMedia({
        count: remain,
        mediaType: ['image'],
        sourceType: ['album']
      })
      const tempFiles = Array.isArray(chooseRes.tempFiles) ? chooseRes.tempFiles : []
      if (tempFiles.length === 0) {
        return
      }
      const normalizedList = tempFiles.map(normalizeImageTempFile).filter(Boolean)
      if (normalizedList.length === 0) {
        return
      }
      const merged = [...this.data.mediaImages, ...normalizedList].slice(0, THREAD_IMAGE_MAX_COUNT)
      this.setData({ mediaImages: merged }, () => {
        this.syncCanSubmitState()
      })
    } catch (error) {
      if (imHelper.isUserCancelError(error)) {
        return
      }
      toastError(resolveChooseImageErrorMessage(error))
    }
  },

  removeThreadImage(e) {
    const index = Number(e.currentTarget.dataset.index)
    if (!Number.isInteger(index) || index < 0 || index >= this.data.mediaImages.length) {
      return
    }
    const nextList = this.data.mediaImages.slice()
    nextList.splice(index, 1)
    this.setData({ mediaImages: nextList }, () => {
      this.syncCanSubmitState()
    })
  },

  previewThreadImage(e) {
    const current = e.currentTarget.dataset.url
    if (!current) {
      return
    }
    const urls = this.data.mediaImages.map(item => item.previewUrl).filter(Boolean)
    wx.previewImage({
      current,
      urls: urls.length > 0 ? urls : [current]
    })
  },

  async chooseThreadVideo() {
    if (this.data.loading || this.data.mediaImages.length > 0) {
      if (this.data.mediaImages.length > 0) {
        toastError('已选择图片，请先清除图片后再选视频')
      }
      return
    }
    try {
      const chooseRes = await imHelper.chooseMedia({
        count: 1,
        mediaType: ['video'],
        sourceType: ['album']
      })
      const tempFile = Array.isArray(chooseRes.tempFiles) ? chooseRes.tempFiles[0] : null
      const normalized = normalizeVideoTempFile(tempFile)
      if (!normalized) {
        return
      }
      this.setData({ mediaVideo: normalized }, () => {
        this.syncCanSubmitState()
      })
    } catch (error) {
      if (imHelper.isUserCancelError(error)) {
        return
      }
      toastError(error, '选择视频失败')
    }
  },

  clearThreadVideo() {
    if (!this.data.mediaVideo) {
      return
    }
    this.setData({ mediaVideo: null }, () => {
      this.syncCanSubmitState()
    })
  },

  previewThreadVideo() {
    const mediaVideo = this.data.mediaVideo
    if (!mediaVideo || !mediaVideo.previewUrl) {
      return
    }
    wx.previewMedia({
      current: 0,
      sources: [{
        url: mediaVideo.previewUrl,
        type: 'video'
      }]
    })
  },

  async handleSubmit() {
    if (this.data.loading) {
      return
    }
    const keepMode = this.data.mode
    const keepThreadId = this.data.threadId
    const keepDraftThreadId = this.data.draftThreadId

    this.setData({
      loading: true,
      uploadProgressPercent: 0,
      uploadProgressText: '准备发布...'
    })
    try {
      const payload = await this.buildThreadPayload({
        draftMode: false,
        onProgress: (percent, text) => {
          this.setData({
            uploadProgressPercent: percent,
            uploadProgressText: text
          })
        }
      })

      let res = null
      if (keepMode === 'edit' && keepThreadId > 0) {
        this.setData({ uploadProgressPercent: 95, uploadProgressText: '保存更新中...' })
        res = await request.put(`/forum/threads/${keepThreadId}`, payload)
      } else if (keepMode === 'restore' && keepThreadId > 0) {
        this.setData({ uploadProgressPercent: 95, uploadProgressText: '重新发布中...' })
        res = await request.put(`/forum/threads/${keepThreadId}/restore`, payload)
      } else if (keepDraftThreadId > 0) {
        this.setData({ uploadProgressPercent: 95, uploadProgressText: '发布草稿中...' })
        res = await request.put(`/forum/threads/${keepDraftThreadId}/publish`, payload)
      } else {
        this.setData({ uploadProgressPercent: 95, uploadProgressText: '发布主题中...' })
        res = await request.post('/forum/threads', payload)
      }

      const threadId = Number(res && res.data && res.data.threadId) || 0
      if (!threadId) {
        throw new Error('发布结果异常')
      }
      this.setData({ uploadProgressPercent: 100, uploadProgressText: '发布完成，正在跳转...' })
      this.skipBackIntercept = true
      wx.redirectTo({
        url: `/pages/post-detail/post-detail?threadId=${threadId}`
      })
    } catch (error) {
      toastError(error, keepMode === 'edit' ? '更新失败' : '发布失败')
    } finally {
      this.setData({
        loading: false,
        uploadProgressPercent: 0,
        uploadProgressText: ''
      }, () => {
        this.syncCanSubmitState()
      })
    }
  },

  async handleBackWithDraftPrompt() {
    if (this.backPrompting) {
      return
    }
    this.backPrompting = true
    try {
      const action = await showLeaveActionSheet()
      if (action === 0) {
        const saved = await this.saveDraft({ silentError: false })
        if (!saved) {
          return
        }
        this.navigateBackSafely()
        return
      }
      if (action === 1) {
        this.navigateBackSafely()
      }
    } finally {
      this.backPrompting = false
    }
  },

  async saveDraft(options = {}) {
    const silentError = options.silentError === true
    if (this.data.loading) {
      return false
    }
    this.setData({
      loading: true,
      uploadProgressPercent: 0,
      uploadProgressText: '保存草稿中...'
    })
    try {
      const payload = await this.buildThreadPayload({
        draftMode: true,
        onProgress: (percent, text) => {
          this.setData({
            uploadProgressPercent: percent,
            uploadProgressText: text
          })
        }
      })

      const canUpdateExistingDraft = this.data.draftThreadId > 0
        && this.data.draftThreadId !== this.data.threadId
      if (canUpdateExistingDraft) {
        payload.threadId = this.data.draftThreadId
      }

      this.setData({
        uploadProgressPercent: 95,
        uploadProgressText: '提交草稿中...'
      })
      const res = await request.post('/forum/threads/drafts', payload)
      const editor = res && res.data ? res.data : null
      if (editor && Number(editor.threadId) > 0) {
        this.setData({
          draftThreadId: Number(editor.threadId)
        }, () => {
          this.updatePageMeta()
        })
      }
      this.recordCleanSnapshot()
      return true
    } catch (error) {
      if (!silentError) {
        toastError(error, '保存草稿失败')
      }
      return false
    } finally {
      this.setData({
        loading: false,
        uploadProgressPercent: 0,
        uploadProgressText: ''
      }, () => {
        this.syncCanSubmitState()
      })
    }
  },

  async buildThreadPayload(options = {}) {
    const draftMode = options.draftMode === true
    const onProgress = typeof options.onProgress === 'function' ? options.onProgress : null

    const title = String(this.data.title || '').trim()
    const content = String(this.data.content || '').trim()
    const mediaImages = Array.isArray(this.data.mediaImages) ? this.data.mediaImages.slice() : []
    const mediaVideo = this.data.mediaVideo || null
    const hasMedia = mediaImages.length > 0 || !!mediaVideo
    if (!draftMode) {
      if (!title) {
        throw new Error('主题标题不能为空')
      }
      if (!content && !hasMedia) {
        throw new Error('主题内容不能为空')
      }
    }

    const localImageList = mediaImages.filter(item => item && item.source === 'local')
    const localVideo = mediaVideo && mediaVideo.source === 'local' ? mediaVideo : null
    const totalBytes = localImageList.reduce((sum, item) => sum + (Number(item.size) || 0), 0)
      + (localVideo ? (Number(localVideo.size) || 0) : 0)
    let uploadedBytes = 0

    const updateUploadProgress = (progress, text) => {
      if (!onProgress) {
        return
      }
      const normalized = Math.max(0, Math.min(100, Math.round(Number(progress) || 0)))
      onProgress(normalized, text)
    }

    const uploadStageWeight = totalBytes > 0 ? 90 : 0
    if (totalBytes > 0) {
      updateUploadProgress(0, '上传媒体 0%')
    }

    const imageKeys = []
    for (let index = 0; index < mediaImages.length; index++) {
      const media = mediaImages[index]
      if (!media) {
        continue
      }
      if (media.source === 'remote') {
        const mediaKey = String(media.key || '').trim()
        if (mediaKey) {
          imageKeys.push(mediaKey)
        }
        continue
      }
      const imageSize = Number(media.size) || 0
      const uploadRes = await uploadForumThreadImage({
        tempFilePath: media.path,
        size: media.size,
        width: media.width,
        height: media.height
      }, {
        onProgress: (fileProgress) => {
          if (totalBytes <= 0 || uploadStageWeight <= 0) {
            return
          }
          const ratio = Math.max(0, Math.min(1, (uploadedBytes + imageSize * (fileProgress / 100)) / totalBytes))
          const percent = Math.round(ratio * uploadStageWeight)
          updateUploadProgress(percent, `上传媒体 ${percent}%`)
        }
      })
      imageKeys.push(uploadRes.mediaKey)
      uploadedBytes += imageSize
      if (totalBytes > 0 && uploadStageWeight > 0) {
        const percent = Math.round((uploadedBytes / totalBytes) * uploadStageWeight)
        updateUploadProgress(percent, `上传媒体 ${percent}%`)
      }
    }

    let videoKey = ''
    let videoPosterKey = ''
    if (mediaVideo) {
      if (mediaVideo.source === 'remote') {
        videoKey = String(mediaVideo.key || '').trim()
        videoPosterKey = String(mediaVideo.posterKey || '').trim()
      } else {
        const videoSize = Number(mediaVideo.size) || 0
        const uploadRes = await uploadForumThreadVideo({
          tempFilePath: mediaVideo.path,
          size: mediaVideo.size,
          width: mediaVideo.width,
          height: mediaVideo.height,
          duration: mediaVideo.duration
        }, {
          onProgress: (fileProgress) => {
            if (totalBytes <= 0 || uploadStageWeight <= 0) {
              return
            }
            const ratio = Math.max(0, Math.min(1, (uploadedBytes + videoSize * (fileProgress / 100)) / totalBytes))
            const percent = Math.round(ratio * uploadStageWeight)
            updateUploadProgress(percent, `上传媒体 ${percent}%`)
          }
        })
        videoKey = uploadRes.mediaKey
        videoPosterKey = uploadRes.mediaPosterKey
        uploadedBytes += videoSize
        if (totalBytes > 0 && uploadStageWeight > 0) {
          const percent = Math.round((uploadedBytes / totalBytes) * uploadStageWeight)
          updateUploadProgress(percent, `上传媒体 ${percent}%`)
        }
      }
    }

    const payload = {
      title,
      content
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
  },

  hasUnsavedChanges() {
    return this.cleanSnapshot !== buildEditorSnapshot(this.data)
  },

  recordCleanSnapshot() {
    this.cleanSnapshot = buildEditorSnapshot(this.data)
  },

  navigateBackSafely() {
    this.skipBackIntercept = true
    wx.navigateBack()
  },

  syncCanSubmitState() {
    const hasTitle = !!String(this.data.title || '').trim()
    const hasBody = !!String(this.data.content || '').trim()
      || this.data.mediaImages.length > 0
      || !!this.data.mediaVideo
    const canSubmit = hasTitle && hasBody && !this.data.loading
    if (canSubmit !== this.data.canSubmit) {
      this.setData({ canSubmit })
    }
  },

  updatePageMeta() {
    let pageTitle = '发布主题'
    let pageHint = '标题 120 字内'
    let submitButtonText = '发布'

    if (this.data.mode === 'edit' && this.data.threadId > 0) {
      pageTitle = '编辑主题'
      pageHint = '编辑后会显示编辑时间'
      submitButtonText = '保存'
    } else if (this.data.mode === 'restore' && this.data.threadId > 0) {
      pageTitle = '重新发布主题'
      pageHint = '可编辑后重新发布'
      submitButtonText = '重新发布'
    } else if (this.data.draftThreadId > 0) {
      pageTitle = '发布主题'
      pageHint = '已载入上次草稿'
      submitButtonText = '发布'
    }

    this.setData({
      pageTitle,
      pageHint,
      submitButtonText
    })
  },

  applyTheme() {
    applyPageTheme(this)
  }
})
