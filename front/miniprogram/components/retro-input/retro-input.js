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
      this.clearRevealTimer()
    }
  },

  methods: {
    syncInputType() {
      const isPassword = this.properties.type === 'password'
      const canReveal = this.properties.holdToReveal && isPassword
      const isRevealing = canReveal && this.data.isRevealing
      this.setData({
        inputType: isRevealing ? 'text' : this.properties.type,
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
      this.clearRevealTimer()
      this.revealTimer = setTimeout(() => {
        this.revealTimer = null
        this.setData({
          isRevealing: true,
          inputType: 'text'
        })
      }, 140)
    },

    onRevealEnd() {
      this.clearRevealTimer()
      if (!this.data.isRevealing) {
        return
      }
      this.setData({
        isRevealing: false,
        inputType: this.properties.type
      })
    },

    clearRevealTimer() {
      if (this.revealTimer) {
        clearTimeout(this.revealTimer)
        this.revealTimer = null
      }
    },

    // 透传原生 input 事件，保持页面层调用方式一致
    onInput(e) {
      this.triggerEvent('input', e.detail, {
        bubbles: true,
        composed: true
      })
    },

    onChange(e) {
      this.triggerEvent('change', e.detail, {
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
