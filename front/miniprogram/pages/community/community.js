/**
 * 社区首页
 */
const request = require('../../utils/request')
const auth = require('../../utils/auth')

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
        this.setData({
          posts: res.data.list || []
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
  }
})
