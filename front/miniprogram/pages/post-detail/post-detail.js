/**
 * 主题详情页面
 */
const request = require('../../utils/request')
const auth = require('../../utils/auth')
const { normalizeUser } = require('../../utils/user')
const { toastError, toastSuccess } = require('../../utils/ui')
const time = require('../../utils/time')
const { applyPageTheme } = require('../../utils/page-theme')
const forumViewHelper = require('../../utils/forum-view-helper')
const pageLifecycleHelper = require('../../utils/page-lifecycle-helper')

const EMPTY_QUOTE_DATA = {
  quoteReplyId: null,
  quoteHint: ''
}

function buildQuoteHint(reply) {
  if (!reply) return ''
  const author = reply.author && (reply.author.nickname || reply.author.wolfNo) ? (reply.author.nickname || reply.author.wolfNo) : '行者'
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
    canManageThread: false,
    canStickyToggle: false,
    replySubmitting: false,
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
      }
    })
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
    this.setData({
      replyContent: e.detail.value || ''
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
    })
  },

  clearQuote() {
    this.setData(EMPTY_QUOTE_DATA)
  },

  async handleReply() {
    if (this.data.replySubmitting) return
    const content = this.data.replyContent.trim()
    if (!content) {
      toastError('请输入回复内容')
      return
    }
    if (this.data.thread && this.data.thread.status === 'LOCKED') {
      toastError('主题已锁定，暂不可回复')
      return
    }

    const payload = { content }
    if (this.data.quoteReplyId) {
      payload.quoteReplyId = this.data.quoteReplyId
    }

    this.setData({ replySubmitting: true })
    try {
      await request.post(`/forum/threads/${this.data.threadId}/replies`, payload)
      toastSuccess('回复成功')
      this.setData({
        replyContent: '',
        ...EMPTY_QUOTE_DATA
      })
      await Promise.all([
        this.loadThreadDetail(),
        this.loadReplies({ reset: true, notifyError: false })
      ])
    } catch (error) {
      toastError(error, '回复失败')
    } finally {
      this.setData({ replySubmitting: false })
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

  applyTheme() {
    applyPageTheme(this)
  }
})
