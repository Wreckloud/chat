/**
 * 帖子详情页面
 */
const request = require('../../utils/request')
const auth = require('../../utils/auth')
const { normalizeUser, openUserProfile } = require('../../utils/user')
const { toastError, toastSuccess } = require('../../utils/ui')
const time = require('../../utils/time')

Page({
  data: {
    postId: null,
    post: null,
    comments: [],
    commentContent: '',
    loading: false
  },

  onLoad(options) {
    if (!auth.isLoggedIn()) {
      wx.redirectTo({ url: '/pages/login/login' })
      return
    }

    const postId = Number(options.postId)
    if (!postId) {
      toastError('参数错误', '参数错误')
      return
    }

    this.setData({ postId })
    this.loadDetail()
  },

  async loadDetail() {
    if (this.data.loading) return
    this.setData({ loading: true })

    try {
      const res = await request.get(`/posts/${this.data.postId}`)
      if (res.code === 0 && res.data) {
        const post = res.data.post || {}
        const comments = res.data.comments || []
        // 作者头像兜底，避免空白
        this.setData({
          post: {
            ...post,
            author: normalizeUser(post.author) || {},
            timeText: time.formatTime(post.createTime)
          },
          comments: comments.map(item => ({
            ...item,
            author: normalizeUser(item.author) || {},
            timeText: time.formatTime(item.createTime)
          }))
        })
      }
    } catch (error) {
      toastError(error, '加载失败')
    } finally {
      this.setData({ loading: false })
    }
  },

  onCommentInput(e) {
    this.setData({
      commentContent: e.detail.value || ''
    })
  },

  async handleComment() {
    const content = this.data.commentContent.trim()
    if (!content) {
      toastError('请输入评论内容', '请输入评论内容')
      return
    }

    try {
      const res = await request.post(`/posts/${this.data.postId}/comments`, { content })
      if (res.code === 0) {
        this.setData({ commentContent: '' })
        this.loadDetail()
      }
    } catch (error) {
      toastError(error, '评论失败')
    }
  },

  async handleFollowAuthor() {
    if (!this.data.post || !this.data.post.author) return
    const authorId = this.data.post.author.userId
    if (!authorId) return

    try {
      const res = await request.post(`/follow/${authorId}`)
      if (res.code === 0) {
        toastSuccess('关注成功')
      }
    } catch (error) {
      toastError(error, '关注失败')
    }
  },

  goUserProfile(e) {
    const user = e.currentTarget.dataset.user
    if (!user || !user.userId) return
    openUserProfile(user)
  }
})
