/**
 * 发布主题页面
 */
const request = require('../../utils/request')
const auth = require('../../utils/auth')
const { toastError, toastSuccess } = require('../../utils/ui')
const { applyPageTheme } = require('../../utils/page-theme')
const pageLifecycleHelper = require('../../utils/page-lifecycle-helper')

Page({
  data: {
    title: '',
    content: '',
    boards: [],
    boardIndex: 0,
    selectedBoardName: '',
    loading: false,
    themeClass: 'theme-retro-blue'
  },

  onLoad(options) {
    pageLifecycleHelper.handleProtectedPageLoad(auth, {
      afterInit: () => {
        const preselectBoardId = Number(options.boardId)
        if (preselectBoardId) {
          this.preselectBoardId = preselectBoardId
        }
        this.loadBoards()
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

  async loadBoards() {
    try {
      const res = await request.get('/forum/boards')
      const boards = res.data || []
      let boardIndex = 0
      if (this.preselectBoardId && boards.length > 0) {
        const index = boards.findIndex(item => item.boardId === this.preselectBoardId)
        if (index >= 0) {
          boardIndex = index
        }
      }
      const selectedBoardName = boards[boardIndex] ? boards[boardIndex].name : ''
      this.setData({ boards, boardIndex, selectedBoardName })
    } catch (error) {
      toastError(error, '加载版块失败')
    }
  },

  onBoardChange(e) {
    const boardIndex = Number(e.detail.value)
    const board = this.data.boards[boardIndex]
    this.setData({
      boardIndex,
      selectedBoardName: board ? board.name : ''
    })
  },

  onTitleInput(e) {
    this.setData({
      title: e.detail.value || ''
    })
  },

  onContentInput(e) {
    this.setData({
      content: e.detail.value || ''
    })
  },

  async handleSubmit() {
    if (this.data.loading) return

    const title = this.data.title.trim()
    const content = this.data.content.trim()
    const board = this.data.boards[this.data.boardIndex]
    if (!board || !board.boardId) {
      toastError('请选择版块')
      return
    }
    if (!title) {
      toastError('请输入主题标题')
      return
    }
    if (!content) {
      toastError('请输入主题内容')
      return
    }

    this.setData({ loading: true })
    try {
      const res = await request.post(`/forum/boards/${board.boardId}/threads`, { title, content })
      const threadId = res.data && res.data.threadId
      if (!threadId) {
        throw new Error('发布结果异常')
      }
      toastSuccess('发布成功')
      setTimeout(() => {
        wx.redirectTo({
          url: `/pages/post-detail/post-detail?threadId=${threadId}`
        })
      }, 500)
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
