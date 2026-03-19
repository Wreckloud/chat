/**
 * 主题详情页面
 */
const request = require('../../utils/request')
const auth = require('../../utils/auth')
const { uploadForumReplyImage } = require('../../utils/oss')
const { normalizeUser, openUserProfile } = require('../../utils/user')
const { toastError, toastSuccess } = require('../../utils/ui')
const time = require('../../utils/time')
const { applyPageTheme } = require('../../utils/page-theme')
const forumViewHelper = require('../../utils/forum-view-helper')
const imHelper = require('../../utils/im-helper')
const pageLifecycleHelper = require('../../utils/page-lifecycle-helper')
const postReplyLayoutHelper = require('../../utils/post-reply-layout-helper')

const EMPTY_QUOTE_DATA = {
  quoteReplyId: null,
  quoteHint: ''
}

function buildQuoteHint(reply) {
  if (!reply) return ''
  const author = reply.author && (reply.author.displayName || reply.author.nickname || reply.author.wolfNo)
    ? (reply.author.displayName || reply.author.nickname || reply.author.wolfNo)
    : '行者'
  const content = (reply.content || '').replace(/\s+/g, ' ').trim()
  const contentPreview = content.length > 24 ? `${content.slice(0, 24)}...` : content
  return `引用 #${reply.floorNo} ${author}: ${contentPreview}`
}

function confirmAction(options) {
  return new Promise(resolve => {
    wx.showModal({
      ...options,
      success(res) {
        resolve(!!res.confirm)
      },
      fail() {
        resolve(false)
      }
    })
  })
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

function resolveThreadPermission(thread, currentUserId) {
  const canManageThread = !!currentUserId && currentUserId === thread.author?.userId
  return {
    canManageThread,
    canStickyToggle: canManageThread && thread.threadType !== 'ANNOUNCEMENT'
  }
}

Page({
  data: {
    threadId: null,
    currentUserId: null,
    thread: null,
    content: '',
    replies: [],
    replyPage: 1,
    replySize: 20,
    replyHasMore: true,
    replyLoading: false,
    replyContent: '',
    quoteReplyId: null,
    quoteHint: '',
    canReplySubmit: false,
    replyImage: null,
    replyFocused: false,
    keyboardHeightPx: 0,
    replyDockHeightPx: 88,
    replyListBottomPx: 88,
    replyScrollIntoView: '',
    canManageThread: false,
    canStickyToggle: false,
    replySubmitting: false,
    threadLikeLoading: false,
    replyLikeLoadingMap: {},
    loading: false,
    themeClass: 'theme-retro-blue'
  },

  onLoad(options) {
    pageLifecycleHelper.handleProtectedPageLoad(auth, {
      beforeInit: () => {
        const threadId = Number(options.threadId)
        if (!threadId) {
          toastError('参数错误')
          return false
        }
        const userInfo = auth.getUserInfo()
        this.setData({
          threadId,
          currentUserId: userInfo && userInfo.userId ? userInfo.userId : null
        })
        return true
      },
      afterInit: () => {
        this.loadPage()
      }
    })
  },

  onShow() {
    pageLifecycleHelper.handleProtectedPageShow(auth, {
      afterShow: () => {
        this.applyTheme()
        postReplyLayoutHelper.measureReplyDockHeight(this)
      }
    })
  },

  onReady() {
    postReplyLayoutHelper.measureReplyDockHeight(this)
  },

  onUnload() {
    this.pageUnloaded = true
    postReplyLayoutHelper.cleanupReplyLayout(this)
  },

  async loadPage() {
    if (this.data.loading) return
    this.setData({ loading: true })
    try {
      await this.loadThreadDetail()
      await this.loadReplies({ reset: true, notifyError: false })
      this.reportView()
    } catch (error) {
      toastError(error, '加载失败')
    } finally {
      this.setData({ loading: false })
    }
  },

  async loadThreadDetail() {
    const res = await request.get(`/forum/threads/${this.data.threadId}`)
    const detail = res.data || {}
    const thread = forumViewHelper.mapThread(detail.thread || {}, normalizeUser, time)
    const permission = resolveThreadPermission(thread, this.data.currentUserId)
    this.setData({
      thread,
      content: detail.content || '',
      canManageThread: permission.canManageThread,
      canStickyToggle: permission.canStickyToggle
    }, () => {
      this.syncReplySubmitState()
    })
  },

  async loadReplies(options = {}) {
    const reset = !!options.reset
    const notifyError = options.notifyError !== false
    if (this.data.replyLoading) return
    if (!reset && !this.data.replyHasMore) return

    const page = reset ? 1 : this.data.replyPage
    this.setData({ replyLoading: true })
    try {
      const res = await request.get(`/forum/threads/${this.data.threadId}/replies`, {
        page,
        size: this.data.replySize
      })
      const list = (res.data.list || []).map(item => (
        forumViewHelper.mapReply(item, normalizeUser, time, {
          currentUserId: this.data.currentUserId,
          canManageThread: this.data.canManageThread
        })
      ))
      const mergedReplies = forumViewHelper.mergePagedList(this.data.replies, list, reset)
      const total = Number(res.data.total) || 0
      const hasMore = forumViewHelper.resolveHasMoreByTotal(mergedReplies.length, total)
      this.setData({
        replies: mergedReplies,
        replyHasMore: hasMore,
        replyPage: hasMore ? page + 1 : page
      }, () => {
        if (reset) {
          this.scrollReplyListToBottom()
        }
      })
    } catch (error) {
      if (notifyError) {
        toastError(error, '加载回复失败')
        return
      }
      throw error
    } finally {
      this.setData({ replyLoading: false })
    }
  },

  async reportView() {
    try {
      await request.put(`/forum/threads/${this.data.threadId}/view`)
    } catch (error) {
      // 浏览上报失败不打断阅读
    }
  },

  onReplyInput(e) {
    const replyContent = e.detail.value || ''
    const canReplySubmit = this.resolveCanReplySubmit(
      replyContent,
      this.data.replyImage,
      this.data.replySubmitting,
      this.data.thread
    )
    if (replyContent === this.data.replyContent && canReplySubmit === this.data.canReplySubmit) {
      return
    }
    this.setData({
      replyContent,
      canReplySubmit
    })
  },

  chooseQuote(e) {
    const reply = e.currentTarget.dataset.reply
    if (!reply || !reply.replyId) {
      return
    }
    this.setData({
      quoteReplyId: reply.replyId,
      quoteHint: buildQuoteHint(reply)
    }, () => {
      postReplyLayoutHelper.measureReplyDockHeight(this)
    })
  },

  clearQuote() {
    this.setData(EMPTY_QUOTE_DATA, () => {
      postReplyLayoutHelper.measureReplyDockHeight(this)
    })
  },

  onTapUserElement(e) {
    const userId = Number(e.currentTarget.dataset.userId)
    if (!userId) {
      return
    }
    openUserProfile({ userId })
  },

  previewThreadImage(e) {
    const current = e.currentTarget.dataset.url
    if (!current) {
      return
    }
    const urls = Array.isArray(this.data.thread && this.data.thread.imageUrls)
      ? this.data.thread.imageUrls.filter(Boolean)
      : []
    wx.previewImage({
      current,
      urls: urls.length > 0 ? urls : [current]
    })
  },

  previewThreadVideo(e) {
    const url = e.currentTarget.dataset.url
    if (!url) {
      return
    }
    wx.previewMedia({
      current: 0,
      sources: [{
        url,
        type: 'video'
      }]
    })
  },

  previewReplyImage(e) {
    const current = e.currentTarget.dataset.url
    if (!current) {
      return
    }
    wx.previewImage({
      current,
      urls: [current]
    })
  },

  onReplyKeyboardHeightChange(e) {
    postReplyLayoutHelper.onReplyKeyboardHeightChange(this, e)
  },

  onReplyFocus(e) {
    postReplyLayoutHelper.onReplyFocus(this, e)
  },

  onReplyBlur() {
    postReplyLayoutHelper.onReplyBlur(this)
  },

  onReplySubmitTap() {
    this.keepReplyFocusAfterSend = this.data.replyFocused || this.data.keyboardHeightPx > 0
    this.handleReply()
  },

  async chooseReplyImage() {
    if (this.data.replySubmitting) {
      return
    }
    try {
      const chooseRes = await imHelper.chooseMedia({
        count: 1,
        mediaType: ['image'],
        sourceType: ['album']
      })
      const tempFile = Array.isArray(chooseRes.tempFiles) ? chooseRes.tempFiles[0] : null
      const normalized = normalizeImageTempFile(tempFile)
      if (!normalized) {
        return
      }
      this.setData({
        replyImage: normalized
      }, () => {
        postReplyLayoutHelper.measureReplyDockHeight(this)
        this.syncReplySubmitState()
      })
    } catch (error) {
      if (imHelper.isUserCancelError(error)) {
        return
      }
      toastError(error, '选择图片失败')
    }
  },

  clearReplyImage() {
    if (!this.data.replyImage) {
      return
    }
    this.setData({
      replyImage: null
    }, () => {
      postReplyLayoutHelper.measureReplyDockHeight(this)
      this.syncReplySubmitState()
    })
  },

  async handleReply() {
    if (this.data.replySubmitting) return
    const content = this.data.replyContent.trim()
    const replyImage = this.data.replyImage
    const hasImage = !!(replyImage && replyImage.path)
    if (!content && !hasImage) {
      this.keepReplyFocusAfterSend = false
      return
    }
    if (this.data.thread && this.data.thread.status === 'LOCKED') {
      this.keepReplyFocusAfterSend = false
      return
    }

    const keepFocused = this.keepReplyFocusAfterSend || this.data.replyFocused || this.data.keyboardHeightPx > 0
    this.keepReplyFocusAfterSend = keepFocused

    const payload = { content }
    if (this.data.quoteReplyId) {
      payload.quoteReplyId = this.data.quoteReplyId
    }

    this.setData({
      replySubmitting: true,
      canReplySubmit: false
    })
    try {
      if (hasImage) {
        const media = await uploadForumReplyImage({
          tempFilePath: replyImage.path,
          size: replyImage.size,
          width: replyImage.width,
          height: replyImage.height
        })
        payload.imageKey = media.mediaKey
      }
      await request.post(`/forum/threads/${this.data.threadId}/replies`, payload)
      this.setData({
        replyContent: '',
        replyImage: null,
        ...EMPTY_QUOTE_DATA
      }, () => {
        postReplyLayoutHelper.measureReplyDockHeight(this)
      })
      await Promise.all([
        this.loadThreadDetail(),
        this.loadReplies({ reset: true, notifyError: false })
      ])
    } catch (error) {
      toastError(error, '回复失败')
    } finally {
      this.keepReplyFocusAfterSend = false
      this.setData({
        replySubmitting: false,
        canReplySubmit: this.resolveCanReplySubmit(this.data.replyContent, this.data.replyImage, false, this.data.thread)
      })
      if (keepFocused) {
        this.refocusReplyInput()
      }
    }
  },

  async handleToggleThreadLike() {
    const thread = this.data.thread
    if (!thread || this.data.threadLikeLoading) return

    const nextLiked = !thread.likedByCurrentUser
    const nextLikeCount = Math.max((Number(thread.likeCount) || 0) + (nextLiked ? 1 : -1), 0)
    this.setData({
      threadLikeLoading: true,
      'thread.likedByCurrentUser': nextLiked,
      'thread.likeCount': nextLikeCount
    })

    try {
      await request.put(`/forum/threads/${this.data.threadId}/like`, { liked: nextLiked })
    } catch (error) {
      this.setData({
        'thread.likedByCurrentUser': thread.likedByCurrentUser,
        'thread.likeCount': thread.likeCount
      })
      toastError(error, '操作失败')
    } finally {
      this.setData({ threadLikeLoading: false })
    }
  },

  async handleToggleReplyLike(e) {
    const replyId = Number(e.currentTarget.dataset.id)
    if (!replyId) return

    const index = this.data.replies.findIndex(item => Number(item.replyId) === replyId)
    if (index < 0) return
    if (this.data.replyLikeLoadingMap[replyId]) return

    const currentReply = this.data.replies[index]
    const nextLiked = !currentReply.likedByCurrentUser
    const nextLikeCount = Math.max((Number(currentReply.likeCount) || 0) + (nextLiked ? 1 : -1), 0)

    this.setData({
      [`replies[${index}].likedByCurrentUser`]: nextLiked,
      [`replies[${index}].likeCount`]: nextLikeCount,
      [`replyLikeLoadingMap.${replyId}`]: true
    })

    try {
      await request.put(`/forum/replies/${replyId}/like`, { liked: nextLiked })
    } catch (error) {
      this.setData({
        [`replies[${index}].likedByCurrentUser`]: currentReply.likedByCurrentUser,
        [`replies[${index}].likeCount`]: currentReply.likeCount
      })
      toastError(error, '操作失败')
    } finally {
      this.setData({
        [`replyLikeLoadingMap.${replyId}`]: false
      })
    }
  },

  async executeThreadAction(options) {
    const confirmed = await confirmAction({
      title: options.title,
      content: options.confirmContent,
      confirmText: options.confirmText || '确认',
      confirmColor: options.confirmColor
    })
    if (!confirmed) return

    try {
      await request.put(options.url, options.payload || {})
      toastSuccess(options.successText)
      await this.loadThreadDetail()
      if (options.refreshReplies) {
        await this.loadReplies({ reset: true, notifyError: false })
      }
    } catch (error) {
      toastError(error, options.failText)
    }
  },

  async handleToggleLock() {
    if (!this.data.thread || !this.data.canManageThread) return
    const nextLocked = this.data.thread.status !== 'LOCKED'
    await this.executeThreadAction({
      title: nextLocked ? '锁定主题' : '解锁主题',
      confirmContent: nextLocked ? '确认锁定主题吗？' : '确认解锁主题吗？',
      url: `/forum/threads/${this.data.threadId}/lock`,
      payload: { locked: nextLocked },
      successText: nextLocked ? '已锁定' : '已解锁',
      failText: nextLocked ? '锁定主题失败' : '解锁主题失败',
      refreshReplies: true
    })
  },

  async handleDeleteThread() {
    if (!this.data.thread || !this.data.canManageThread) return
    const confirmed = await confirmAction({
      title: '删除主题',
      content: '删除后主题与楼层将被隐藏，确认删除吗？',
      confirmColor: '#b4474f',
      confirmText: '删除'
    })
    if (!confirmed) return

    try {
      await request.del(`/forum/threads/${this.data.threadId}`)
      toastSuccess('主题已删除')
      setTimeout(() => {
        wx.navigateBack()
      }, 350)
    } catch (error) {
      toastError(error, '删除失败')
    }
  },

  async handleToggleSticky() {
    if (!this.data.thread || !this.data.canStickyToggle) return
    const nextSticky = this.data.thread.threadType !== 'STICKY'
    await this.executeThreadAction({
      title: nextSticky ? '置顶主题' : '取消置顶',
      confirmContent: nextSticky ? '确认置顶主题吗？' : '确认取消置顶吗？',
      url: `/forum/threads/${this.data.threadId}/sticky`,
      payload: { sticky: nextSticky },
      successText: nextSticky ? '已置顶' : '已取消置顶',
      failText: nextSticky ? '置顶主题失败' : '取消置顶失败'
    })
  },

  async handleToggleEssence() {
    if (!this.data.thread || !this.data.canManageThread) return
    const nextEssence = !this.data.thread.isEssence
    await this.executeThreadAction({
      title: nextEssence ? '设为精华' : '取消精华',
      confirmContent: nextEssence ? '确认设为精华吗？' : '确认取消精华吗？',
      url: `/forum/threads/${this.data.threadId}/essence`,
      payload: { essence: nextEssence },
      successText: nextEssence ? '已设为精华' : '已取消精华',
      failText: nextEssence ? '设精华失败' : '取消精华失败'
    })
  },

  async handleDeleteReply(e) {
    const replyId = Number(e.currentTarget.dataset.id)
    if (!replyId) return
    const confirmed = await confirmAction({
      title: '删除回复',
      content: '确认删除该楼层回复吗？',
      confirmColor: '#b4474f',
      confirmText: '删除'
    })
    if (!confirmed) return

    try {
      await request.del(`/forum/replies/${replyId}`)
      toastSuccess('回复已删除')
      await this.loadThreadDetail()
      await this.loadReplies({ reset: true, notifyError: false })
    } catch (error) {
      toastError(error, '删除失败')
    }
  },

  loadMoreReplies() {
    this.loadReplies({ reset: false, notifyError: true })
  },

  onReachBottom() {
    this.loadMoreReplies()
  },

  refocusReplyInput() {
    postReplyLayoutHelper.refocusReplyInput(this)
  },

  scrollReplyListToBottom() {
    postReplyLayoutHelper.scrollReplyListToBottom(this)
  },

  measureReplyDockHeight() {
    postReplyLayoutHelper.measureReplyDockHeight(this)
  },

  applyTheme() {
    applyPageTheme(this)
  },

  syncReplySubmitState() {
    this.setData({
      canReplySubmit: this.resolveCanReplySubmit(this.data.replyContent, this.data.replyImage, this.data.replySubmitting, this.data.thread)
    })
  },

  resolveCanReplySubmit(replyContent, replyImage, replySubmitting, thread) {
    if (replySubmitting) {
      return false
    }
    const hasContent = !!(replyContent && String(replyContent).trim())
    const hasImage = !!(replyImage && replyImage.path)
    if (!hasContent && !hasImage) {
      return false
    }
    if (thread && thread.status === 'LOCKED') {
      return false
    }
    return true
  }
})
