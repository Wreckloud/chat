/**
 * 行者主页
 */
const auth = require('../../utils/auth')
const request = require('../../utils/request')
const { normalizeUser } = require('../../utils/user')
const { toastError, toastSuccess } = require('../../utils/ui')
const { applyPageTheme } = require('../../utils/page-theme')
const pageLifecycleHelper = require('../../utils/page-lifecycle-helper')

Page({
  data: {
    userInfo: null,
    isSelf: false,
    isFollowing: false,
    isMutual: false,
    loading: false,
    followLoading: false,
    chatLoading: false,
    themeClass: 'theme-retro-blue'
  },

  onLoad(options) {
    let targetUserId = 0
    pageLifecycleHelper.handleProtectedPageLoad(auth, {
      beforeInit: () => {
        const userId = Number(options && options.userId)
        if (!userId) {
          toastError('用户信息缺失')
          setTimeout(() => {
            wx.navigateBack()
          }, 1500)
          return false
        }

        const currentUser = auth.getUserInfo()
        const currentUserId = currentUser ? currentUser.userId : null
        const isSelf = currentUserId === userId
        this.setData({
          userInfo: null,
          isSelf
        })
        targetUserId = userId
        return true
      },
      afterInit: () => {
        wx.setNavigationBarTitle({
          title: '行者主页'
        })
        this.loadUserInfo(targetUserId)
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

  async loadUserInfo(userId) {
    if (this.data.loading) return
    this.setData({ loading: true })

    try {
      const res = await request.get(`/users/${userId}`)
      this.setData({
        userInfo: normalizeUser(res.data)
      })
    } catch (error) {
      toastError(error, '加载失败')
    } finally {
      this.setData({ loading: false })
    }

    if (!this.data.isSelf) {
      this.loadFollowStatus()
    }
  },

  async loadFollowStatus() {
    if (this.data.loading) return
    if (!this.data.userInfo || !this.data.userInfo.userId) return
    this.setData({ loading: true })

    try {
      const [followingRes, mutualRes] = await Promise.all([
        request.get('/follow/following'),
        request.get('/follow/mutual')
      ])

      const userId = this.data.userInfo.userId
      const following = (followingRes.data || []).some(item => item.userId === userId)
      const mutual = (mutualRes.data || []).some(item => item.userId === userId)

      this.setData({
        isFollowing: following,
        isMutual: mutual
      })
    } catch (error) {
      toastError(error, '加载失败')
    } finally {
      this.setData({ loading: false })
    }
  },

  async toggleFollow() {
    if (this.data.followLoading || this.data.isSelf) return
    const userId = this.data.userInfo.userId
    this.setData({ followLoading: true })

    try {
      if (this.data.isFollowing) {
        await request.del(`/follow/${userId}`)
        toastSuccess('已取消关注')
      } else {
        await request.post(`/follow/${userId}`)
        toastSuccess('关注成功')
      }
      await this.loadFollowStatus()
    } catch (error) {
      toastError(error, '操作失败')
    } finally {
      this.setData({ followLoading: false })
    }
  },

  async goChat() {
    if (this.data.chatLoading || this.data.isSelf) return
    const userId = this.data.userInfo.userId
    this.setData({ chatLoading: true })

    try {
      const res = await request.post(`/conversations/${userId}`)
      const conversationId = res.data
      wx.navigateTo({
        url: `/pages/chat-detail/chat-detail?conversationId=${conversationId}`
      })
    } catch (error) {
      toastError(error, '无法发起聊天')
    } finally {
      this.setData({ chatLoading: false })
    }
  },

  applyTheme() {
    applyPageTheme(this)
  }
})
