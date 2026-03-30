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
      return imPageHelper.sendComposerTextMessage(this, imSendHelper, {
        onError: (error) => {
          if (typeof toastError === 'function') {
            toastError(error, '发送消息失败')
          }
        }
      })
    },

    setMorePanelVisible(visible) {
      imPageHelper.setMorePanelVisible(this, visible)
    },

    onClickMore() {
      imPageHelper.toggleMorePanel(this)
    },

    setEmojiPanelVisible(visible) {
      imPageHelper.setEmojiPanelVisible(this, visible)
    },

    onToggleEmojiPanel() {
      imPageHelper.toggleEmojiPanel(this)
    },

    onSelectEmoji(e) {
      imPageHelper.onSelectEmoji(this, e)
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

    onTapFile(e) {
      imSendHelper.onTapFile(this, e, toastError)
    },

    onLongPressMessage(e) {
      imPageHelper.onLongPressMessage(this, e)
    },

    onLongPressMessageRow(e) {
      if (typeof this.handleLongPressMessageRow === 'function') {
        this.handleLongPressMessageRow(e)
        return
      }
      imPageHelper.onLongPressMessage(this, e)
    },

    onLongPressUserMeta(e) {
      if (typeof this.handleLongPressUserMeta === 'function') {
        this.handleLongPressUserMeta(e)
      }
    },

    onClearReplyDraft() {
      if (typeof this.clearReplyDraft === 'function') {
        this.clearReplyDraft()
      }
    },

    scrollToBottom() {
      imHelper.scrollToBottom(this)
    },

    onMessageListUpper() {
      imPageHelper.onMessageListUpper(this, () => this.loadMessages())
    },

    onTapMessageList() {
      imPageHelper.onTapMessageList(this)
    },

    previewImage(e) {
      imPageHelper.previewImage(this, e)
    },

    onTapReplyQuote(e) {
      if (typeof this.handleTapReplyQuote === 'function') {
        this.handleTapReplyQuote(e)
      }
    },

    onMessageVideoLoadedMetadata(e) {
      if (typeof this.handleMessageVideoLoadedMetadata === 'function') {
        this.handleMessageVideoLoadedMetadata(e)
      }
    },

    onMessageVideoError(e) {
      if (typeof this.handleMessageVideoError === 'function') {
        this.handleMessageVideoError(e)
      }
    }
  }
}

module.exports = buildCommonImPageMethods
