/**
 * 帖子详情页面
 */
const request = require('../../utils/request')
const auth = require('../../utils/auth')

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
      wx.showToast({
        title: '参数错误',
        icon: 'none'
      })
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
        this.setData({
          post: res.data.post,
          comments: res.data.comments || []
        })
      }
    } catch (error) {
      wx.showToast({
        title: error.message || '加载失败',
        icon: 'none'
      })
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
      wx.showToast({
        title: '请输入评论内容',
        icon: 'none'
      })
      return
    }

    try {
      const res = await request.post(`/posts/${this.data.postId}/comments`, { content })
      if (res.code === 0) {
        this.setData({ commentContent: '' })
        this.loadDetail()
      }
    } catch (error) {
      wx.showToast({
        title: error.message || '评论失败',
        icon: 'none'
      })
    }
  },

  async handleFollowAuthor() {
    if (!this.data.post || !this.data.post.author) return
    const authorId = this.data.post.author.userId
    if (!authorId) return

    try {
      const res = await request.post(`/follow/${authorId}`)
      if (res.code === 0) {
        wx.showToast({
          title: '关注成功',
          icon: 'success'
        })
      }
    } catch (error) {
      wx.showToast({
        title: error.message || '关注失败',
        icon: 'none'
      })
    }
  }
})
