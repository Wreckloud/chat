/**
 * 聊天首页
 */
const auth = require('../../utils/auth')
const request = require('../../utils/request')
const time = require('../../utils/time')

Page({
  data: {
    // 会话列表数据
    conversationList: [],
    loading: false
  },

  onLoad() {
    // 检查登录状态
    if (!auth.isLoggedIn()) {
      wx.redirectTo({
        url: '/pages/login/login'
      })
      return
    }
    
    // 加载会话列表
    this.loadConversations()
  },

  onShow() {
    // 每次显示时刷新会话列表
    if (auth.isLoggedIn()) {
      this.loadConversations()
    }
  },

  /**
   * 加载会话列表
   */
  loadConversations() {
    if (this.data.loading) return

    this.setData({ loading: true })

    request.get('/conversations')
      .then(res => {
        const list = res.data || []
        // 格式化时间
        list.forEach(item => {
          if (item.lastMessageTime) {
            item.formattedTime = time.formatTime(item.lastMessageTime)
          }
        })
        
        this.setData({
          conversationList: list,
          loading: false
        })
      })
      .catch(err => {
        console.error('加载会话列表失败:', err)
        wx.showToast({
          title: err.message || '加载失败',
          icon: 'none',
          duration: 2000
        })
        this.setData({ loading: false })
      })
  },

  /**
   * 进入聊天详情
   */
  goChatDetail(e) {
    const conversationId = e.currentTarget.dataset.id
    wx.navigateTo({
      url: `/pages/chat-detail/chat-detail?conversationId=${conversationId}`
    })
  },

  /**
   * 下拉刷新
   */
  onPullDownRefresh() {
    this.loadConversations()
    wx.stopPullDownRefresh()
  },

  /**
   * 去互关列表找行者
   */
  goToMutual() {
    wx.switchTab({
      url: '/pages/follow/follow'
    })
  }
})

