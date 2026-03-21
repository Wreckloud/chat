/**
 * 行者主页
 */
const auth = require('../../utils/auth')
const request = require('../../utils/request')
const { normalizeUser } = require('../../utils/user')
const { toastError, confirmAction } = require('../../utils/ui')
const time = require('../../utils/time')
const { applyPageTheme } = require('../../utils/page-theme')
const pageLifecycleHelper = require('../../utils/page-lifecycle-helper')
const { attachDisplayTitle, normalizeTitleName, normalizeTitleColor } = require('../../utils/title')

function formatCount(value) {
  const number = Number(value)
  return Number.isFinite(number) && number >= 0 ? String(number) : '0'
}

function mapThread(item) {
  const imageUrls = Array.isArray(item.imageUrls)
    ? item.imageUrls.filter(url => typeof url === 'string' && url.trim())
    : []
  const videoUrl = typeof item.videoUrl === 'string' ? item.videoUrl : ''
  const videoPosterUrl = typeof item.videoPosterUrl === 'string' ? item.videoPosterUrl : ''
  const hasVideo = !!videoUrl
  const hasSingleImagePreview = !hasVideo && imageUrls.length === 1
  const singleImagePreviewUrl = hasSingleImagePreview ? imageUrls[0] : ''
  const previewImageUrls = imageUrls.slice(0, hasVideo ? 2 : 3)
  const editTimeText = time.formatPostTime(item.editTime)
  const editTimeRelativeText = time.formatRelativeTime(item.editTime)
  const hasEditTime = !!editTimeText
  return {
    ...item,
    viewCount: Number(item.viewCount) || 0,
    replyCount: Number(item.replyCount) || 0,
    likeCount: Number(item.likeCount) || 0,
    contentPreview: typeof item.contentPreview === 'string' ? item.contentPreview.trim() : '',
    imageUrls,
    videoUrl,
    hasVideo,
    videoPosterUrl,
    hasSingleImagePreview,
    singleImagePreviewUrl,
    previewImageUrls,
    hasMoreImages: imageUrls.length > previewImageUrls.length,
    createTimeText: time.formatPostTime(item.createTime),
    createTimeRelativeText: time.formatRelativeTime(item.createTime),
    editTimeText,
    editTimeRelativeText,
    hasEditTime,
    timePrefix: hasEditTime ? '编辑于' : '发布于',
    timeText: hasEditTime ? editTimeText : time.formatPostTime(item.createTime),
    timeRelativeText: hasEditTime ? editTimeRelativeText : time.formatRelativeTime(item.createTime),
    lastReplyTimeText: time.formatPostTime(item.lastReplyTime)
  }
}

Page({
  data: {
    targetUserId: 0,
    userInfo: null,
    loading: false,
    isSelf: false,
    isTargetDisabled: false,
    isFollowing: false,
    isMutual: false,
    followLoading: false,
    chatLoading: false,
    activeDayCountText: '0',
    totalLikeCountText: '0',
    followerCountText: '0',
    followingCountText: '0',
    threadCountText: '0',
    replyCountText: '0',
    lastActiveText: '--',
    showcaseTitles: [],
    latestThreads: [],
    themeClass: 'theme-retro-blue'
  },

  onLoad(options) {
    pageLifecycleHelper.handleProtectedPageLoad(auth, {
      beforeInit: () => {
        const userId = Number(options && options.userId)
        if (!userId) {
          toastError('用户信息缺失')
          setTimeout(() => {
            wx.navigateBack()
          }, 250)
          return false
        }
        this.setData({
          targetUserId: userId
        })
        return true
      },
      afterInit: () => {
        wx.setNavigationBarTitle({ title: '行者主页' })
        this.loadHome()
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

  async loadHome() {
    if (!this.data.targetUserId || this.data.loading) {
      return
    }
    this.setData({ loading: true })
    try {
      const res = await request.get(`/users/${this.data.targetUserId}/home`)
      const home = res.data || {}
      const userInfo = attachDisplayTitle(
        normalizeUser(home.user),
        home.user && home.user.equippedTitleName,
        home.user && home.user.equippedTitleColor
      )
      const isTargetDisabled = userInfo && userInfo.status === 'DISABLED'
      const showcaseTitles = Array.isArray(home.showcaseTitles)
        ? home.showcaseTitles.slice(0, 3).map(item => ({
          achievementCode: item.achievementCode || '',
          titleName: normalizeTitleName(item.titleName),
          titleColor: normalizeTitleColor(item.titleColor)
        })).filter(item => item.titleName)
        : []
      this.setData({
        userInfo,
        isSelf: home.self === true,
        isTargetDisabled,
        isFollowing: home.following === true,
        isMutual: home.mutual === true,
        activeDayCountText: formatCount(home.activeDayCount),
        totalLikeCountText: formatCount(home.totalLikeCount),
        followerCountText: formatCount(home.followerCount),
        followingCountText: formatCount(home.followingCount),
        threadCountText: formatCount(home.threadCount),
        replyCountText: formatCount(home.replyCount),
        lastActiveText: time.formatPostTime(home.lastActiveAt) || '--',
        showcaseTitles,
        latestThreads: Array.isArray(home.latestThreads) ? home.latestThreads.map(mapThread) : []
      })
    } catch (error) {
      toastError(error, '加载行者主页失败')
    } finally {
      this.setData({ loading: false })
    }
  },

  async toggleFollow() {
    if (this.data.followLoading || this.data.isSelf || !this.data.userInfo) return
    if (this.data.isTargetDisabled) {
      toastError('该用户已不可用')
      return
    }
    if (this.data.isFollowing) {
      const confirmed = await confirmAction({
        title: '取消关注？',
        content: '取消后将不再优先看到对方动态，可随时重新关注。',
        confirmColor: '#b4474f',
        confirmText: '取消关注',
        cancelText: '再想想'
      })
      if (!confirmed) {
        return
      }
    }
    this.setData({ followLoading: true })
    try {
      if (this.data.isFollowing) {
        await request.del(`/follow/${this.data.userInfo.userId}`)
      } else {
        await request.post(`/follow/${this.data.userInfo.userId}`)
      }
      await this.loadHome()
    } catch (error) {
      toastError(error, '更新关注状态失败')
    } finally {
      this.setData({ followLoading: false })
    }
  },

  async goChat() {
    if (this.data.chatLoading || this.data.isSelf || !this.data.userInfo) return
    if (this.data.isTargetDisabled) {
      toastError('该用户已不可用')
      return
    }
    this.setData({ chatLoading: true })
    try {
      const res = await request.post(`/conversations/${this.data.userInfo.userId}`)
      wx.navigateTo({
        url: `/pages/chat-detail/chat-detail?conversationId=${res.data}`
      })
    } catch (error) {
      toastError(error, '无法发起聊天')
    } finally {
      this.setData({ chatLoading: false })
    }
  },

  onTapThread(e) {
    const threadId = Number(e.currentTarget.dataset.threadId)
    if (!threadId) return
    wx.navigateTo({
      url: `/pages/post-detail/post-detail?threadId=${threadId}`
    })
  },

  goAllThreads() {
    if (!this.data.userInfo || !this.data.userInfo.userId) return
    const nickname = encodeURIComponent(this.data.userInfo.nickname || this.data.userInfo.wolfNo || '行者')
    wx.navigateTo({
      url: `/pages/user-posts/user-posts?userId=${this.data.userInfo.userId}&nickname=${nickname}`
    })
  },

  applyTheme() {
    applyPageTheme(this)
  }
})
