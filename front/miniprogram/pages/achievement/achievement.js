/**
 * 成就与头衔管理
 */
const request = require('../../utils/request')
const auth = require('../../utils/auth')
const { toastError } = require('../../utils/ui')
const { applyPageTheme } = require('../../utils/page-theme')
const pageLifecycleHelper = require('../../utils/page-lifecycle-helper')
const { normalizeTitleName, normalizeTitleColor } = require('../../utils/title')
const time = require('../../utils/time')

function mapAchievement(item) {
  const titleName = normalizeTitleName(item && item.titleName)
  const titleColor = normalizeTitleColor(item && item.titleColor)
  const unlocked = item && item.unlocked === true
  const equipped = item && item.equipped === true
  return {
    achievementCode: item && item.achievementCode ? item.achievementCode : '',
    name: item && item.name ? item.name : '未命名成就',
    description: item && item.description ? item.description : '',
    titleName,
    titleColor,
    unlocked,
    equipped,
    unlockTimeText: unlocked ? time.formatPostTime(item.unlockTime) : '--'
  }
}

Page({
  data: {
    loading: false,
    equipping: false,
    userInfo: null,
    achievements: [],
    themeClass: 'theme-retro-blue'
  },

  onLoad() {
    pageLifecycleHelper.handleProtectedPageLoad(auth, {
      afterInit: () => {
        wx.setNavigationBarTitle({ title: '成就头衔' })
      }
    })
  },

  onShow() {
    pageLifecycleHelper.handleProtectedPageShow(auth, {
      afterShow: () => {
        this.applyTheme()
        this.loadData()
      }
    })
  },

  async loadData() {
    if (this.data.loading) {
      return
    }

    this.setData({ loading: true })
    try {
      const [userRes, achievementRes] = await Promise.all([
        request.get('/users/me'),
        request.get('/users/me/achievements')
      ])
      const userInfo = userRes && userRes.data ? userRes.data : null
      if (userInfo) {
        auth.setUserInfo(userInfo)
      }

      const achievements = Array.isArray(achievementRes && achievementRes.data)
        ? achievementRes.data.map(mapAchievement)
        : []

      this.setData({
        userInfo,
        achievements
      })
    } catch (error) {
      toastError(error, '加载成就失败')
    } finally {
      this.setData({ loading: false })
    }
  },

  async handleEquip(e) {
    const achievementCode = e.currentTarget.dataset.code
    if (!achievementCode || this.data.equipping) {
      return
    }

    this.setData({ equipping: true })
    try {
      await request.put('/users/me/title/equip', { achievementCode })
      await this.loadData()
    } catch (error) {
      toastError(error, '佩戴失败')
    } finally {
      this.setData({ equipping: false })
    }
  },

  async handleUnequip() {
    if (this.data.equipping) {
      return
    }
    this.setData({ equipping: true })
    try {
      await request.put('/users/me/title/unequip')
      await this.loadData()
    } catch (error) {
      toastError(error, '卸下失败')
    } finally {
      this.setData({ equipping: false })
    }
  },

  applyTheme() {
    applyPageTheme(this)
  }
})
