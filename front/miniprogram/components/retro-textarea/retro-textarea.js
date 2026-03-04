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
    }
  },

  methods: {
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
    }
  }
})
