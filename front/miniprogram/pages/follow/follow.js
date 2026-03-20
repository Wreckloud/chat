/**
 * 关注列表页面
 */
const request = require('../../utils/request')
const auth = require('../../utils/auth')
const { normalizeUserList, openUserProfile } = require('../../utils/user')
const { toastError, confirmAction } = require('../../utils/ui')
const { applyPageTheme } = require('../../utils/page-theme')
const pageLifecycleHelper = require('../../utils/page-lifecycle-helper')
const { attachDisplayTitle } = require('../../utils/title')

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
      const list = normalizeUserList(res.data || []).map(item => attachDisplayTitle(
        item,
        item.equippedTitleName,
        item.equippedTitleColor
      ))
      this.setData({
        list
      })
    } catch (error) {
      toastError(error, '加载关注列表失败')
    } finally {
      this.setData({ loading: false })
    }
  },

  async handleFollow(e) {
    const userId = e.currentTarget.dataset.id
    if (!userId) return

    try {
      await request.post(`/follow/${userId}`)
      await this.loadList()
    } catch (error) {
      toastError(error, '关注失败')
    }
  },

  async handleUnfollow(e) {
    const userId = e.currentTarget.dataset.id
    if (!userId) return

    const confirmed = await confirmAction({
      title: '取消关注？',
      content: '取消后将不再优先看到对方动态，可随时重新关注。',
      confirmColor: '#b4474f',
      confirmText: '取消关注',
      cancelText: '再想想'
    })
    if (!confirmed) return

    try {
      await request.del(`/follow/${userId}`)
      await this.loadList()
    } catch (error) {
      toastError(error, '取消关注失败')
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
