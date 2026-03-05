Component({
  properties: {
    theme: {
      type: String,
      value: 'default'
    },
    size: {
      type: String,
      value: 'medium'
    },
    variant: {
      type: String,
      value: 'solid'
    },
    block: {
      type: Boolean,
      value: false
    },
    loading: {
      type: Boolean,
      value: false
    },
    disabled: {
      type: Boolean,
      value: false
    }
  },

  methods: {
    onTap(e) {
      if (this.data.disabled || this.data.loading) {
        return
      }
      this.triggerEvent('tap', e.detail || {}, {
        bubbles: false,
        composed: false
      })
    }
  }
})
