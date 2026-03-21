Component({
  properties: {
    value: {
      type: String,
      value: ''
    },
    placeholder: {
      type: String,
      value: ''
    },
    maxlength: {
      type: Number,
      value: 500
    },
    disabled: {
      type: Boolean,
      value: false
    },
    autoHeight: {
      type: Boolean,
      value: true
    },
    focus: {
      type: Boolean,
      value: false
    },
    adjustPosition: {
      type: Boolean,
      value: true
    },
    holdKeyboard: {
      type: Boolean,
      value: false
    },
    confirmHold: {
      type: Boolean,
      value: false
    },
    confirmType: {
      type: String,
      value: 'done'
    }
  },

  methods: {
    onInput(e) {
      this.triggerEvent('input', e.detail, {
        bubbles: true,
        composed: true
      })
    },
    onFocus(e) {
      this.triggerEvent('focus', e.detail, {
        bubbles: true,
        composed: true
      })
    },
    onBlur(e) {
      this.triggerEvent('blur', e.detail, {
        bubbles: true,
        composed: true
      })
    },
    onKeyboardHeightChange(e) {
      this.triggerEvent('keyboardheightchange', e.detail, {
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
