Component({
  properties: {
    value: {
      type: String,
      value: ''
    },
    label: {
      type: String,
      value: ''
    },
    placeholder: {
      type: String,
      value: ''
    },
    type: {
      type: String,
      value: 'text'
    },
    maxlength: {
      type: Number,
      value: 140
    },
    disabled: {
      type: Boolean,
      value: false
    },
    confirmType: {
      type: String,
      value: 'done'
    },
    holdToReveal: {
      type: Boolean,
      value: false
    },
    metaLevel: {
      type: String,
      value: ''
    },
    metaText: {
      type: String,
      value: ''
    },
    required: {
      type: Boolean,
      value: false
    }
  },

  data: {
    inputType: 'text',
    inputPassword: false,
    isRevealing: false
  },

  observers: {
    'type, holdToReveal': function () {
      this.syncInputType()
    }
  },

  lifetimes: {
    attached() {
      this.syncInputType()
    },
    detached() {
      this.clearTapRevealTimer()
    }
  },

  methods: {
    syncInputType() {
      const isPassword = this.properties.type === 'password'
      const canReveal = this.properties.holdToReveal && isPassword
      const isRevealing = canReveal && this.data.isRevealing
      const nextInputType = isPassword ? 'text' : this.properties.type
      this.setData({
        inputType: nextInputType,
        inputPassword: isPassword && !isRevealing,
        isRevealing: isRevealing
      })
    },

    onRevealStart() {
      if (!(this.properties.holdToReveal && this.properties.type === 'password')) {
        return
      }
      if (this.properties.disabled) {
        return
      }
      this.ignoreTapUntil = Date.now() + 300
      this.clearTapRevealTimer()
      this.showReveal()
    },

    onRevealEnd() {
      if (!(this.properties.holdToReveal && this.properties.type === 'password')) {
        return
      }
      this.hideReveal()
    },

    onRevealTap() {
      if (!(this.properties.holdToReveal && this.properties.type === 'password')) {
        return
      }
      if (this.properties.disabled) {
        return
      }
      if (this.ignoreTapUntil && Date.now() < this.ignoreTapUntil) {
        return
      }
      this.clearTapRevealTimer()
      this.showReveal()
      this.tapRevealTimer = setTimeout(() => {
        this.tapRevealTimer = null
        this.hideReveal()
      }, 1200)
    },

    showReveal() {
      if (this.data.isRevealing && !this.data.inputPassword) {
        return
      }
      this.setData({
        isRevealing: true,
        inputPassword: false
      })
    },

    hideReveal() {
      if (!this.data.isRevealing && this.data.inputPassword) {
        return
      }
      this.setData({
        isRevealing: false,
        inputPassword: this.properties.type === 'password'
      })
    },

    clearTapRevealTimer() {
      if (!this.tapRevealTimer) {
        return
      }
      clearTimeout(this.tapRevealTimer)
      this.tapRevealTimer = null
    },

    // 透传原生 input 事件，保持页面层调用方式一致
    onInput(e) {
      this.triggerEvent('input', e.detail, {
        bubbles: true,
        composed: true
      })
    },

    onConfirm(e) {
      this.triggerEvent('confirm', e.detail, {
        bubbles: true,
        composed: true
      })
    }
  }
})
