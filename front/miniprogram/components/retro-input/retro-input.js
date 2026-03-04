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
    }
  },

  methods: {
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
