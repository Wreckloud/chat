Component({
  properties: {
    image: {
      type: String,
      value: ''
    },
    size: {
      type: String,
      value: 'medium'
    },
    shape: {
      type: String,
      value: 'square'
    }
  },

  data: {
    imageAvailable: false
  },

  observers: {
    // 有图片则优先显示图片，失败时退回 slot 文本首字
    image(value) {
      this.setData({
        imageAvailable: Boolean(value)
      })
    }
  },

  methods: {
    onImageError() {
      this.setData({
        imageAvailable: false
      })
    }
  }
})
