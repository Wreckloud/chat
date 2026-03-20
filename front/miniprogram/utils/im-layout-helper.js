const BLUR_RESET_DELAY_MS = 16
const REFOCUS_DELAY_MS = 24

function resolveKeyboardHeight(event) {
  const rawHeight = event && event.detail ? Number(event.detail.height) : 0
  if (!Number.isFinite(rawHeight) || rawHeight <= 0) {
    return 0
  }
  return Math.floor(rawHeight)
}

function clearScrollToBottomTimer(page) {
  if (!page || !page.scrollToBottomTimer) {
    return
  }
  clearTimeout(page.scrollToBottomTimer)
  page.scrollToBottomTimer = null
}

function scrollToBottom(page) {
  clearScrollToBottomTimer(page)
  const scrollToAnchor = () => {
    page.setData({ scrollIntoView: 'im-bottom-anchor' })
    page.scrollToBottomTimer = setTimeout(() => {
      if (page.data.scrollIntoView) {
        page.setData({ scrollIntoView: '' })
      }
      page.scrollToBottomTimer = null
    }, 120)
  }

  if (page.data.scrollIntoView) {
    page.setData({ scrollIntoView: '' }, scrollToAnchor)
    return
  }
  scrollToAnchor()
}

function clearRefocusComposerTimer(page) {
  if (!page || !page.refocusComposerTimer) {
    return
  }
  clearTimeout(page.refocusComposerTimer)
  page.refocusComposerTimer = null
}

function updateKeyboardLayout(page, keyboardHeight, options = {}) {
  if (!page || !page.data) {
    return false
  }
  const prevHeight = Number(page.data.keyboardHeightPx) || 0
  const nextHeight = Number.isFinite(keyboardHeight) && keyboardHeight > 0
    ? Math.floor(keyboardHeight)
    : 0
  const dockHeight = Number(page.data.dockHeightPx) || 0
  const nextBottom = dockHeight + nextHeight
  if (nextHeight === prevHeight && nextBottom === page.data.messageListBottomPx) {
    return false
  }

  page.setData({
    keyboardHeightPx: nextHeight,
    messageListBottomPx: nextBottom
  }, () => {
    if (options.scrollOnKeyboardOpen && prevHeight === 0 && nextHeight > 0) {
      scrollToBottom(page)
    }
  })
  return true
}

function resetKeyboardHeight(page) {
  if (!page || !page.data) {
    return
  }
  updateKeyboardLayout(page, 0)
}

function handleKeyboardHeightChange(page, event, closeMorePanel, closeEmojiPanel) {
  if (!page || !page.data) {
    return
  }
  const nextHeight = resolveKeyboardHeight(event)
  if (nextHeight > 0) {
    page.lastKeyboardOpenedAt = Date.now()
    page.lastKeyboardHeightPx = nextHeight
  }
  if (nextHeight > 0 && page.data.morePanelVisible && typeof closeMorePanel === 'function') {
    closeMorePanel()
  }
  if (nextHeight > 0 && page.data.emojiPanelVisible && typeof closeEmojiPanel === 'function') {
    closeEmojiPanel()
  }

  if (nextHeight === 0 && page.keepComposerOpenUntilSendFinish) {
    return
  }

  updateKeyboardLayout(page, nextHeight, {
    scrollOnKeyboardOpen: true
  })
}

function handleComposerFocus(page, event, closeMorePanel, closeEmojiPanel) {
  if (!page || !page.data) {
    return
  }
  if (!page.data.composerFocused) {
    page.setData({ composerFocused: true })
  }

  const eventHeight = resolveKeyboardHeight(event)
  if (eventHeight > 0) {
    page.lastKeyboardOpenedAt = Date.now()
    page.lastKeyboardHeightPx = eventHeight
    const changed = updateKeyboardLayout(page, eventHeight)
    if (!changed) {
      scrollToBottom(page)
    }
  } else {
    const cachedHeight = Number(page.lastKeyboardHeightPx || 0)
    if (cachedHeight > 0) {
      updateKeyboardLayout(page, cachedHeight)
    }
  }

  if (!page.data.morePanelVisible || typeof closeMorePanel !== 'function') {
    if (page.data.emojiPanelVisible && typeof closeEmojiPanel === 'function') {
      closeEmojiPanel()
    }
    return
  }
  closeMorePanel()
  if (page.data.emojiPanelVisible && typeof closeEmojiPanel === 'function') {
    closeEmojiPanel()
  }
}

function handleComposerBlur(page) {
  if (!page) {
    return
  }
  if (page.keepComposerOpenUntilSendFinish) {
    refocusComposerInput(page)
    return
  }
  page.setData({ composerFocused: false })
  setTimeout(() => {
    if (page.pageUnloaded || !page.data || page.data.composerFocused) {
      return
    }
    if (page.data.keyboardHeightPx > 0) {
      resetKeyboardHeight(page)
    }
  }, BLUR_RESET_DELAY_MS)
}

function measureDockHeight(page) {
  if (!page || typeof wx === 'undefined') {
    return
  }
  wx.nextTick(() => {
    const query = wx.createSelectorQuery().in(page)
    query.select('#im-dock').boundingClientRect(rect => {
      if (!rect || !rect.height || !page.data) {
        return
      }
      const nextDockHeight = Math.ceil(rect.height)
      const nextBottom = nextDockHeight + page.data.keyboardHeightPx
      if (nextDockHeight === page.data.dockHeightPx && nextBottom === page.data.messageListBottomPx) {
        return
      }
      page.setData({
        dockHeightPx: nextDockHeight,
        messageListBottomPx: nextBottom
      })
    }).exec()
  })
}

function shouldKeepComposerAfterSend(page) {
  if (!page || !page.data) {
    return false
  }
  if (page.data.morePanelVisible) {
    return false
  }
  if (page.data.keyboardHeightPx > 0 || page.data.composerFocused) {
    return true
  }

  const lastKeyboardOpenedAt = Number(page.lastKeyboardOpenedAt || 0)
  if (lastKeyboardOpenedAt <= 0) {
    return false
  }
  return Date.now() - lastKeyboardOpenedAt <= 1200
}

function refocusComposerInput(page) {
  if (!page || page.pageUnloaded || !page.data || page.data.morePanelVisible) {
    return
  }
  if (page.data.composerFocused && page.data.keyboardHeightPx > 0) {
    return
  }

  clearRefocusComposerTimer(page)
  page.setData({ composerFocused: false }, () => {
    page.refocusComposerTimer = setTimeout(() => {
      page.refocusComposerTimer = null
      if (page.pageUnloaded || !page.data || page.data.morePanelVisible) {
        return
      }
      page.setData({ composerFocused: true })
    }, REFOCUS_DELAY_MS)
  })
}

module.exports = {
  clearScrollToBottomTimer,
  scrollToBottom,
  clearRefocusComposerTimer,
  resetKeyboardHeight,
  handleKeyboardHeightChange,
  handleComposerFocus,
  handleComposerBlur,
  measureDockHeight,
  shouldKeepComposerAfterSend,
  refocusComposerInput
}
