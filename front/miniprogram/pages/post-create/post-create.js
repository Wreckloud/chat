/**
 * 发帖页面
 */
const request = require('../../utils/request')
const auth = require('../../utils/auth')

Page({
  data: {
    content: '',
    loading: false
  },

  onLoad() {
    if (!auth.isLoggedIn()) {
      wx.redirectTo({ url: '/pages/login/login' })
    }
  },

  onContentInput(e) {
    this.setData({
      content: e.detail.value || ''
    })
  },

  async handleSubmit() {
    if (this.data.loading) return

    const content = this.data.content.trim()
    if (!content) {
      wx.showToast({
        title: '请输入内容',
        icon: 'none'
      })
      return
    }

    this.setData({ loading: true })

    try {
      const res = await request.post('/posts', { content })
      if (res.code === 0) {
        wx.showToast({
          title: '发布成功',
          icon: 'success'
        })
        setTimeout(() => {
          wx.navigateBack()
        }, 800)
      }
    } catch (error) {
      wx.showToast({
        title: error.message || '发布失败',
        icon: 'none'
      })
    } finally {
      this.setData({ loading: false })
    }
  }
})
