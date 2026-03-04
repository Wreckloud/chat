/**
 * 轻量右滑容器
 * 支持横向滑出右侧操作按钮，并通过 click 事件回传被点击的 action 对象
 */
const ACTION_WIDTH_RPX = 128

Component({
  properties: {
    right: {
      type: Array,
      value: []
    }
  },

  data: {
    actionWidthPx: 0,
    maxOffset: 0,
    offsetX: 0,
    dragging: false
  },

  lifetimes: {
    attached() {
      const info = wx.getSystemInfoSync()
      const actionWidthPx = (info.windowWidth * ACTION_WIDTH_RPX) / 750
      this.setData({ actionWidthPx }, () => {
        this.syncMaxOffset(this.properties.right)
      })
    }
  },

  observers: {
    right(nextRight) {
      this.syncMaxOffset(nextRight)
    }
  },

  methods: {
    /**
     * 根据动作数量同步最大滑动距离，保证按钮配置变化时偏移量仍然合法
     */
    syncMaxOffset(rightActions) {
      const count = Array.isArray(rightActions) ? rightActions.length : 0
      const maxOffset = Math.max(0, Math.round(count * this.data.actionWidthPx))
      const nextOffset = Math.max(-maxOffset, Math.min(0, this.data.offsetX))
      this.setData({
        maxOffset,
        offsetX: nextOffset
      })
    },

    onTouchStart(e) {
      const actions = this.properties.right || []
      if (!actions.length) return
      const touch = e.touches && e.touches[0]
      if (!touch) return
      this.startX = touch.clientX
      this.startY = touch.clientY
      this.startOffset = this.data.offsetX
      this.lockDirection = ''
      this.setData({ dragging: true })
    },

    onTouchMove(e) {
      if (typeof this.startX !== 'number') return
      const touch = e.touches && e.touches[0]
      if (!touch) return

      const deltaX = touch.clientX - this.startX
      const deltaY = touch.clientY - this.startY

      // 首次有效移动时锁定方向，避免纵向滚动被误判为横向滑动
      if (!this.lockDirection) {
        if (Math.abs(deltaX) < 6 && Math.abs(deltaY) < 6) {
          return
        }
        this.lockDirection = Math.abs(deltaX) >= Math.abs(deltaY) ? 'x' : 'y'
      }

      if (this.lockDirection !== 'x') {
        return
      }

      const delta = deltaX
      const nextOffset = Math.max(
        -this.data.maxOffset,
        Math.min(0, this.startOffset + delta)
      )
      this.setData({ offsetX: nextOffset })
    },

    onTouchEnd() {
      if (typeof this.startX !== 'number') return

      const threshold = this.data.maxOffset * 0.35
      let nextOffset = this.data.offsetX
      if (this.lockDirection === 'x') {
        // 超过阈值才展开，降低误触开合的概率
        const shouldOpen = Math.abs(this.data.offsetX) > threshold
        nextOffset = shouldOpen ? -this.data.maxOffset : 0
      }
      this.setData({
        dragging: false,
        offsetX: nextOffset
      })
      this.startX = null
      this.startY = null
      this.startOffset = 0
      this.lockDirection = ''
    },

    onActionTap(e) {
      const index = Number(e.currentTarget.dataset.index)
      const action = (this.properties.right || [])[index]
      if (!action) return
      this.setData({ offsetX: 0 })
      this.triggerEvent('click', action, {
        bubbles: true,
        composed: true
      })
    }
  }
})
