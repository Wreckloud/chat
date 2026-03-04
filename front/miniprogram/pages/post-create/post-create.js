/**
 * 发帖页面
 */
const request = require('../../utils/request')
const auth = require('../../utils/auth')
const { toastError, toastSuccess } = require('../../utils/ui')
const { applyPageTheme } = require('../../utils/page-theme')

Page({
  data: {
    content: '',
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
      toastError('请输入内容')
      return
    }

    this.setData({ loading: true })

    try {
      const res = await request.post('/posts', { content })
      if (res.code === 0) {
        toastSuccess('发布成功')
        setTimeout(() => {
          wx.navigateBack()
        }, 800)
      }
    } catch (error) {
      toastError(error, '发布失败')
    } finally {
      this.setData({ loading: false })
    }
  },

  applyTheme() {
    applyPageTheme(this)
  }
})
