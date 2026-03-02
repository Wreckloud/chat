/**
 * 社区首页
 */
const request = require('../../utils/request')
const auth = require('../../utils/auth')
const { normalizeUser, openUserProfile } = require('../../utils/user')
const { toastError } = require('../../utils/ui')

Page({
  data: {
    posts: [],
    page: 1,
    size: 10,
    loading: false
  },

  onLoad() {
    // tabBar 页面，检查登录状态
    if (!auth.isLoggedIn()) {
      wx.redirectTo({ url: '/pages/login/login' })
      return
    }
    this.loadPosts()
  },

  onShow() {
    if (auth.isLoggedIn()) {
      this.loadPosts()
    }
  },

  async loadPosts() {
    if (this.data.loading) return
    this.setData({ loading: true })

    try {
      const res = await request.get('/posts', {
        page: this.data.page,
        size: this.data.size
      })
      if (res.code === 0 && res.data) {
        const list = (res.data.list || []).map(item => ({
          ...item,
          author: normalizeUser(item.author) || {}
        }))
        this.setData({
          posts: list
        })
      }
    } catch (error) {
      toastError(error, '加载失败')
    } finally {
      this.setData({ loading: false })
    }
  },

  goCreatePost() {
    wx.navigateTo({
      url: '/pages/post-create/post-create'
    })
  },

  goPostDetail(e) {
    const postId = e.currentTarget.dataset.id
    wx.navigateTo({
      url: `/pages/post-detail/post-detail?postId=${postId}`
    })
  },

  goUserProfile(e) {
    const user = e.currentTarget.dataset.user
    if (!user || !user.userId) return
    openUserProfile(user)
  }
})
