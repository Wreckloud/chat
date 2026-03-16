/**
 * 关注列表页面
 */
const request = require('../../utils/request')
const auth = require('../../utils/auth')
const { normalizeUserList, openUserProfile } = require('../../utils/user')
const { toastError, toastSuccess } = require('../../utils/ui')
const { applyPageTheme } = require('../../utils/page-theme')
const pageLifecycleHelper = require('../../utils/page-lifecycle-helper')

const FOLLOW_API_BY_TAB = {
  following: '/follow/following',
  followers: '/follow/followers',
  mutual: '/follow/mutual'
}

Page({
  data: {
    active: 'following',
    list: [],
    loading: false,
    themeClass: 'theme-retro-blue'
  },

  onLoad() {
    pageLifecycleHelper.handleProtectedPageLoad(auth)
  },

  onShow() {
    pageLifecycleHelper.handleProtectedPageShow(auth, {
      afterShow: () => {
        this.applyTheme()
        this.loadList()
      }
    })
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

    const url = FOLLOW_API_BY_TAB[this.data.active] || FOLLOW_API_BY_TAB.following

    try {
      const res = await request.get(url)
      this.setData({
        list: normalizeUserList(res.data || [])
      })
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
      await request.post(`/follow/${userId}`)
      toastSuccess('关注成功')
      await this.loadList()
    } catch (error) {
      toastError(error, '关注失败')
    }
  },

  async handleUnfollow(e) {
    const userId = e.currentTarget.dataset.id
    if (!userId) return

    try {
      await request.del(`/follow/${userId}`)
      toastSuccess('已取消关注')
      await this.loadList()
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
