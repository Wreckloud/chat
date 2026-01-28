/**
 * 关注列表页面
 */
const request = require('../../utils/request')
const auth = require('../../utils/auth')

Page({
  data: {
    active: 'following',
    list: [],
    loading: false
  },

  onLoad() {
    if (!auth.isLoggedIn()) {
      wx.redirectTo({ url: '/pages/login/login' })
      return
    }
    this.loadList()
  },

  onShow() {
    if (auth.isLoggedIn()) {
      this.loadList()
    }
  },

  switchTab(e) {
    const tab = e.currentTarget.dataset.tab
    if (tab === this.data.active) return
    this.setData({ active: tab })
    this.loadList()
  },

  async loadList() {
    if (this.data.loading) return
    this.setData({ loading: true })

    let url = '/follow/following'
    if (this.data.active === 'followers') {
      url = '/follow/followers'
    } else if (this.data.active === 'mutual') {
      url = '/follow/mutual'
    }

    try {
      const res = await request.get(url)
      if (res.code === 0 && res.data) {
        this.setData({
          list: res.data || []
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

  async handleFollow(e) {
    const userId = e.currentTarget.dataset.id
    if (!userId) return

    try {
      const res = await request.post(`/follow/${userId}`)
      if (res.code === 0) {
        wx.showToast({
          title: '关注成功',
          icon: 'success'
        })
        this.loadList()
      }
    } catch (error) {
      wx.showToast({
        title: error.message || '关注失败',
        icon: 'none'
      })
    }
  },

  async handleUnfollow(e) {
    const userId = e.currentTarget.dataset.id
    if (!userId) return

    try {
      const res = await request.request({
        url: `/follow/${userId}`,
        method: 'DELETE'
      })
      if (res.code === 0) {
        wx.showToast({
          title: '已取消关注',
          icon: 'success'
        })
        this.loadList()
      }
    } catch (error) {
      wx.showToast({
        title: error.message || '操作失败',
        icon: 'none'
      })
    }
  }
})
