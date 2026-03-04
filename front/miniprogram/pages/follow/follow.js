/**
 * 关注列表页面
 */
const request = require('../../utils/request')
const auth = require('../../utils/auth')
const { normalizeUserList, openUserProfile } = require('../../utils/user')
const { toastError, toastSuccess } = require('../../utils/ui')
const { applyPageTheme } = require('../../utils/page-theme')

Page({
  data: {
    active: 'following',
    list: [],
    loading: false,
    themeClass: 'theme-retro-blue'
  },

  onLoad() {
    if (!auth.requireLogin()) {
      return
    }
  },

  onShow() {
    if (!auth.requireLogin()) return
    this.applyTheme()
    this.loadList()
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
          list: normalizeUserList(res.data || [])
        })
      }
    } catch (error) {
      toastError(error, '加载失败')
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
        toastSuccess('关注成功')
        this.loadList()
      }
    } catch (error) {
      toastError(error, '关注失败')
    }
  },

  async handleUnfollow(e) {
    const userId = e.currentTarget.dataset.id
    if (!userId) return

    try {
      const res = await request.del(`/follow/${userId}`)
      if (res.code === 0) {
        toastSuccess('已取消关注')
        this.loadList()
      }
    } catch (error) {
      toastError(error, '操作失败')
    }
  },

  goUserProfile(e) {
    const user = e.currentTarget.dataset.user
    if (!user || !user.userId) return
    openUserProfile(user)
  },

  applyTheme() {
    applyPageTheme(this)
  }
})
