/**
 * 发布主题页面
 */
const request = require('../../utils/request')
const auth = require('../../utils/auth')
const { uploadForumThreadImage, uploadForumThreadVideo } = require('../../utils/media-upload')
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

function normalizeImageTempFile(file) {
  if (!file || !file.tempFilePath) {
    return null
  }
  return {
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
    path: file.tempFilePath,
    posterPath,
    size: Number(file.size) || 0,
    width: Number(file.width) || 0,
    height: Number(file.height) || 0,
    duration: Number(file.duration) || 0
  }
}

Page({
  data: {
    title: '',
    content: '',
    emojiPanelVisible: false,
    emojiList: COMMON_KAOMOJI_LIST,
    mediaImages: [],
    mediaVideo: null,
    canSubmit: false,
    loading: false,
    themeClass: 'theme-retro-blue'
  },

  onLoad() {
    pageLifecycleHelper.handleProtectedPageLoad(auth, {
      afterInit: () => {
        this.syncCanSubmitState()
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

  onTitleInput(e) {
    this.setData({
      title: e.detail.value || ''
    }, () => {
      this.syncCanSubmitState()
    })
  },

  onContentInput(e) {
    this.setData({
      content: e.detail.value || ''
    }, () => {
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
    this.setData({
      content: nextContent
    }, () => {
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
    const urls = this.data.mediaImages.map(item => item.path).filter(Boolean)
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
    if (!mediaVideo || !mediaVideo.path) {
      return
    }
    wx.previewMedia({
      current: 0,
      sources: [{
        url: mediaVideo.path,
        type: 'video'
      }]
    })
  },

  async handleSubmit() {
    if (this.data.loading) return

    const title = this.data.title.trim()
    const content = this.data.content.trim()
    const hasImages = this.data.mediaImages.length > 0
    const hasVideo = !!this.data.mediaVideo
    if (!title || (!content && !hasImages && !hasVideo)) {
      return
    }

    this.setData({ loading: true })
    try {
      const payload = { title, content }
      if (hasImages) {
        const imageKeys = []
        for (let index = 0; index < this.data.mediaImages.length; index++) {
          const media = await uploadForumThreadImage({
            tempFilePath: this.data.mediaImages[index].path,
            size: this.data.mediaImages[index].size,
            width: this.data.mediaImages[index].width,
            height: this.data.mediaImages[index].height
          })
          imageKeys.push(media.mediaKey)
        }
        payload.imageKeys = imageKeys
      }
      if (hasVideo) {
        const media = await uploadForumThreadVideo({
          tempFilePath: this.data.mediaVideo.path,
          size: this.data.mediaVideo.size,
          width: this.data.mediaVideo.width,
          height: this.data.mediaVideo.height,
          duration: this.data.mediaVideo.duration
        })
        payload.videoKey = media.mediaKey
        if (media.mediaPosterKey) {
          payload.videoPosterKey = media.mediaPosterKey
        }
      }

      const res = await request.post('/forum/threads', payload)
      const threadId = res.data && res.data.threadId
      if (!threadId) {
        throw new Error('发布结果异常')
      }
      wx.redirectTo({
        url: `/pages/post-detail/post-detail?threadId=${threadId}`
      })
    } catch (error) {
      toastError(error, '发布失败')
    } finally {
      this.setData({ loading: false }, () => {
        this.syncCanSubmitState()
      })
    }
  },

  syncCanSubmitState() {
    const hasTitle = !!this.data.title.trim()
    const hasBody = !!this.data.content.trim() || this.data.mediaImages.length > 0 || !!this.data.mediaVideo
    const canSubmit = hasTitle && hasBody && !this.data.loading
    if (canSubmit !== this.data.canSubmit) {
      this.setData({ canSubmit })
    }
  },

  applyTheme() {
    applyPageTheme(this)
  }
})
