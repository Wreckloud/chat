function buildCommonImPageMethods(options) {
  const {
    imPageHelper,
    imSendHelper,
    imHelper,
    toastError,
    uploadImage,
    uploadVideo,
    uploadFile
  } = options

  return {
    onMessageInput(e) {
      imPageHelper.onMessageInput(this, e)
    },

    onKeyboardHeightChange(e) {
      imPageHelper.onKeyboardHeightChange(this, e)
    },

    onComposerFocus(e) {
      imPageHelper.onComposerFocus(this, e)
    },

    onComposerBlur() {
      imPageHelper.onComposerBlur(this)
    },

    onSendButtonTap() {
      imPageHelper.onSendButtonTap(this, () => this.sendMessage())
    },

    async sendMessage() {
      return imPageHelper.sendComposerTextMessage(this, imSendHelper)
    },

    setMorePanelVisible(visible) {
      imPageHelper.setMorePanelVisible(this, visible)
    },

    onClickMore() {
      imPageHelper.toggleMorePanel(this)
    },

    async onMoreActionTap(e) {
      await imPageHelper.onDefaultMoreActionTap(this, e, imSendHelper, {
        uploadImage,
        uploadVideo,
        uploadFile
      })
    },

    onTapLink(e) {
      imSendHelper.onTapLink(e)
    },

    onTapVideo(e) {
      imSendHelper.onTapVideo(this, e, toastError)
    },

    onTapFile(e) {
      imSendHelper.onTapFile(this, e, toastError)
    },

    onLongPressMessage(e) {
      imPageHelper.onLongPressMessage(this, e)
    },

    scrollToBottom() {
      imHelper.scrollToBottom(this)
    },

    onMessageListUpper() {
      imPageHelper.onMessageListUpper(this, () => this.loadMessages())
    },

    previewImage(e) {
      imPageHelper.previewImage(this, e)
    }
  }
}

module.exports = buildCommonImPageMethods
