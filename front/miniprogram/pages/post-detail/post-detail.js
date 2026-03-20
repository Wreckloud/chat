/**
 * 主题详情页面
 */
const request = require('../../utils/request')
const auth = require('../../utils/auth')
const { uploadForumReplyImage } = require('../../utils/oss')
const { normalizeUser, openUserProfile } = require('../../utils/user')
const { toastError } = require('../../utils/ui')
const time = require('../../utils/time')
const { applyPageTheme } = require('../../utils/page-theme')
const forumViewHelper = require('../../utils/forum-view-helper')
const imHelper = require('../../utils/im-helper')
const replyMentionHelper = require('../../utils/reply-mention-helper')
const pageLifecycleHelper = require('../../utils/page-lifecycle-helper')
const postReplyLayoutHelper = require('../../utils/post-reply-layout-helper')
const { COMMON_KAOMOJI_LIST, appendKaomojiWithSpace } = require('../../utils/kaomoji')

const EMPTY_REPLY_TARGET_DATA = {
  replyTargetId: null,
  replyTargetHint: ''
}

const REPLY_SORT_OPTIONS = [
  { key: 'floor', text: '按楼层' },
  { key: 'hot', text: '按热度' },
  { key: 'author', text: '只看楼主' }
]

function buildReplyTargetHint(reply) {
  if (!reply) return ''
  const author = reply.author && (reply.author.displayName || reply.author.nickname || reply.author.wolfNo)
    ? (reply.author.displayName || reply.author.nickname || reply.author.wolfNo)
    : '行者'
  return `回复 @${author} #${reply.floorNo}`
}

function prependReplyMention(content, reply) {
  if (!reply || !reply.author) {
    return String(content || '').trim()
  }
  const displayName = reply.author.displayName || reply.author.nickname || reply.author.wolfNo
  return replyMentionHelper.prependMention(content, displayName)
}

function removeLeadingReplyMention(content, reply) {
  if (!reply || !reply.author) {
    return String(content || '').trim().replace(/^@\S+\s*/, '').trim()
  }
  const displayName = reply.author.displayName || reply.author.nickname || reply.author.wolfNo
  return replyMentionHelper.removeLeadingMention(content, displayName)
}

function normalizeReplySortKey(sortKey) {
  const target = typeof sortKey === 'string' ? sortKey.trim().toLowerCase() : ''
  if (REPLY_SORT_OPTIONS.some(item => item.key === target)) {
    return target
  }
  return 'floor'
}

function findReplyById(replies, replyId) {
  if (!Array.isArray(replies) || !replyId) {
    return null
  }
  const targetId = Number(replyId)
  if (!targetId) {
    return null
  }
  return replies.find(item => Number(item.replyId) === targetId) || null
}

function resolveNextReplySort(currentSort) {
  const index = REPLY_SORT_OPTIONS.findIndex(item => item.key === currentSort)
  if (index < 0) {
    return REPLY_SORT_OPTIONS[0]
  }
  return REPLY_SORT_OPTIONS[(index + 1) % REPLY_SORT_OPTIONS.length]
}

function buildReplyTree(replies) {
  if (!Array.isArray(replies) || replies.length === 0) {
    return []
  }
  const nodeMap = new Map()
  replies.forEach(item => {
    const node = {
      ...item,
      children: []
    }
    nodeMap.set(Number(item.replyId), node)
  })

  const childReplyIdSet = new Set()
  replies.forEach(item => {
    const node = nodeMap.get(Number(item.replyId))
    const quoteReplyId = Number(item.quoteReplyId) || 0
    if (!quoteReplyId) {
      return
    }

    const directParent = nodeMap.get(quoteReplyId)
    if (!directParent) {
      return
    }
    directParent.children.push(node)
    childReplyIdSet.add(Number(item.replyId))
  })

  const roots = []
  replies.forEach(item => {
    const replyId = Number(item.replyId)
    if (childReplyIdSet.has(replyId)) {
      return
    }
    const node = nodeMap.get(replyId)
    if (node) {
      roots.push(node)
    }
  })

  return roots.map(item => withReplyNodeMeta(item, 0))
}

function withReplyNodeMeta(node, depth) {
  const safeDepth = Number.isFinite(depth) ? Math.max(0, depth) : 0
  const clampedDepth = Math.min(safeDepth, 6)
  const children = Array.isArray(node.children)
    ? node.children.map(child => withReplyNodeMeta(child, safeDepth + 1))
    : []
  return {
    ...node,
    depth: safeDepth,
    indentRpx: clampedDepth * 20,
    children
  }
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
    canManageThread
  }
}

Page({
  data: {
    threadId: null,
    currentUserId: null,
    thread: null,
    content: '',
    replies: [],
    replyTree: [],
    replyPage: 1,
    replySize: 20,
    replyHasMore: true,
    replyLoading: false,
    replyContent: '',
    replyTargetId: null,
    replyTargetHint: '',
    replySort: 'floor',
    replySortText: '按楼层',
    canReplySubmit: false,
    replyEmojiPanelVisible: false,
    replyEmojiList: COMMON_KAOMOJI_LIST,
    replyImage: null,
    replyFocused: false,
    keyboardHeightPx: 0,
    replyDockHeightPx: 88,
    replyListBottomPx: 88,
    replyScrollIntoView: '',
    canManageThread: false,
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
      toastError(error, '加载主题详情失败')
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
      canManageThread: permission.canManageThread
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
    const replySort = normalizeReplySortKey(this.data.replySort)
    this.setData({ replyLoading: true })
    try {
      const res = await request.get(`/forum/threads/${this.data.threadId}/replies`, {
        page,
        size: this.data.replySize,
        sort: replySort
      })
      const list = (res.data.list || []).map(item => (
        forumViewHelper.mapReply(item, normalizeUser, time, {
          currentUserId: this.data.currentUserId,
          canManageThread: this.data.canManageThread
        })
      ))
      const mergedReplies = forumViewHelper.mergePagedList(this.data.replies, list, reset)
      const replyTree = buildReplyTree(mergedReplies)
      const total = Number(res.data.total) || 0
      const hasMore = forumViewHelper.resolveHasMoreByTotal(mergedReplies.length, total)
      const replyTarget = findReplyById(mergedReplies, this.data.replyTargetId)
      this.setData({
        replies: mergedReplies,
        replyTree,
        replyHasMore: hasMore,
        replyPage: hasMore ? page + 1 : page,
        replyTargetId: replyTarget ? replyTarget.replyId : null,
        replyTargetHint: replyTarget ? buildReplyTargetHint(replyTarget) : ''
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

  chooseReplyTarget(e) {
    const replyId = Number(e.currentTarget.dataset.replyId)
    if (!replyId) {
      return
    }
    const reply = findReplyById(this.data.replies, replyId)
    if (!reply) {
      return
    }
    const nextReplyContent = prependReplyMention(this.data.replyContent, reply)
    this.setData({
      replyTargetId: reply.replyId,
      replyTargetHint: buildReplyTargetHint(reply),
      replyContent: nextReplyContent,
      canReplySubmit: this.resolveCanReplySubmit(nextReplyContent, this.data.replyImage, this.data.replySubmitting, this.data.thread)
    }, () => {
      postReplyLayoutHelper.measureReplyDockHeight(this)
    })
  },

  clearReplyTarget() {
    const targetReply = this.data.replyTargetId
      ? findReplyById(this.data.replies, this.data.replyTargetId)
      : null
    const nextReplyContent = removeLeadingReplyMention(this.data.replyContent, targetReply)
    this.setData({
      ...EMPTY_REPLY_TARGET_DATA,
      replyContent: nextReplyContent,
      canReplySubmit: this.resolveCanReplySubmit(
        nextReplyContent,
        this.data.replyImage,
        this.data.replySubmitting,
        this.data.thread
      )
    }, () => {
      postReplyLayoutHelper.measureReplyDockHeight(this)
    })
  },

  onSwitchReplySort() {
    if (this.data.replyLoading) {
      return
    }
    const selected = resolveNextReplySort(this.data.replySort)
    if (!selected || selected.key === this.data.replySort) {
      return
    }
    this.setData({
      replySort: selected.key,
      replySortText: selected.text
    })
    this.loadReplies({ reset: true, notifyError: true })
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
    const nextHeight = Number(e && e.detail ? e.detail.height : 0)
    if (nextHeight > 0 && this.data.replyEmojiPanelVisible) {
      this.setData({ replyEmojiPanelVisible: false }, () => {
        postReplyLayoutHelper.measureReplyDockHeight(this)
      })
    }
    postReplyLayoutHelper.onReplyKeyboardHeightChange(this, e)
  },

  onReplyFocus(e) {
    if (this.data.replyEmojiPanelVisible) {
      this.setData({ replyEmojiPanelVisible: false })
    }
    postReplyLayoutHelper.onReplyFocus(this, e)
  },

  onReplyBlur() {
    postReplyLayoutHelper.onReplyBlur(this)
  },

  onReplySubmitTap() {
    this.keepReplyFocusAfterSend = this.data.replyFocused || this.data.keyboardHeightPx > 0
    this.handleReply()
  },

  toggleReplyEmojiPanel() {
    if (this.data.replySubmitting) {
      return
    }
    const nextVisible = !this.data.replyEmojiPanelVisible
    if (nextVisible) {
      wx.hideKeyboard()
      this.setData({
        replyFocused: false,
        keyboardHeightPx: 0,
        replyListBottomPx: this.data.replyDockHeightPx,
        replyEmojiPanelVisible: true
      }, () => {
        postReplyLayoutHelper.measureReplyDockHeight(this)
      })
      return
    }
    this.setData({
      replyEmojiPanelVisible: false
    }, () => {
      postReplyLayoutHelper.measureReplyDockHeight(this)
    })
  },

  onSelectReplyEmoji(e) {
    const dataset = e && e.currentTarget ? (e.currentTarget.dataset || {}) : {}
    const emoji = String(dataset.emoji || '').trim()
    if (!emoji) {
      return
    }
    const nextReplyContent = appendKaomojiWithSpace(this.data.replyContent, emoji)
    this.setData({
      replyContent: nextReplyContent,
      canReplySubmit: this.resolveCanReplySubmit(nextReplyContent, this.data.replyImage, this.data.replySubmitting, this.data.thread)
    })
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
    let content = this.data.replyContent.trim()
    const replyImage = this.data.replyImage
    const hasImage = !!(replyImage && replyImage.path)
    if (this.data.thread && this.data.thread.status === 'LOCKED') {
      this.keepReplyFocusAfterSend = false
      return
    }

    const targetReply = this.data.replyTargetId
      ? findReplyById(this.data.replies, this.data.replyTargetId)
      : null
    if (targetReply) {
      content = prependReplyMention(content, targetReply)
    }
    if (!content && !hasImage) {
      this.keepReplyFocusAfterSend = false
      return
    }

    const keepFocused = this.keepReplyFocusAfterSend || this.data.replyFocused || this.data.keyboardHeightPx > 0
    this.keepReplyFocusAfterSend = keepFocused

    const payload = { content }
    if (this.data.replyTargetId) {
      payload.quoteReplyId = this.data.replyTargetId
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
        ...EMPTY_REPLY_TARGET_DATA
      }, () => {
        postReplyLayoutHelper.measureReplyDockHeight(this)
      })
      await Promise.all([
        this.loadThreadDetail(),
        this.loadReplies({ reset: true, notifyError: false })
      ])
    } catch (error) {
      toastError(error, hasImage ? '图片回帖失败' : '回帖失败')
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
      toastError(error, '更新主题点赞失败')
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
    }, () => {
      const replyTree = buildReplyTree(this.data.replies)
      this.setData({ replyTree })
    })

    try {
      await request.put(`/forum/replies/${replyId}/like`, { liked: nextLiked })
    } catch (error) {
      this.setData({
        [`replies[${index}].likedByCurrentUser`]: currentReply.likedByCurrentUser,
        [`replies[${index}].likeCount`]: currentReply.likeCount
      }, () => {
        const replyTree = buildReplyTree(this.data.replies)
        this.setData({ replyTree })
      })
      toastError(error, '更新回复点赞失败')
    } finally {
      this.setData({
        [`replyLikeLoadingMap.${replyId}`]: false
      })
    }
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
      wx.navigateBack()
    } catch (error) {
      toastError(error, '删除主题失败')
    }
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
      await this.loadThreadDetail()
      await this.loadReplies({ reset: true, notifyError: false })
    } catch (error) {
      toastError(error, '删除回复失败')
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

