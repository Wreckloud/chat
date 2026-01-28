/**
 * 聊天首页
 */
const auth = require('../../utils/auth')

Page({
  data: {
    // 聊天列表数据
    chatList: []
  },

  onLoad() {
    // 检查登录状态
    if (!auth.isLoggedIn()) {
      wx.redirectTo({
        url: '/pages/login/login'
      })
      return
    }
    
    // 加载聊天列表
    this.loadChatList()
  },

  onShow() {
    // 每次显示时刷新聊天列表
    if (auth.isLoggedIn()) {
      this.loadChatList()
    }
  },

  /**
   * 加载聊天列表
   */
  loadChatList() {
    // TODO: 调用后端接口获取聊天列表
    // 暂时使用空列表
    this.setData({
      chatList: []
    })
  },

  /**
   * 进入聊天详情
   */
  goChatDetail(e) {
    const chatId = e.currentTarget.dataset.id
    // TODO: 跳转到聊天详情页
    console.log('进入聊天详情:', chatId)
  }
})

