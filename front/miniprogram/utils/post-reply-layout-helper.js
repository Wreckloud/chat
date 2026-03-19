const BLUR_RESET_DELAY_MS = 90
const REFOCUS_DELAY_MS = 24

function resolveKeyboardHeight(event) {
  const height = Number(event && event.detail ? event.detail.height : 0)
  if (!Number.isFinite(height) || height <= 0) {
    return 0
  }
  return Math.floor(height)
}

function onReplyKeyboardHeightChange(page, event) {
  if (!page || !page.data) {
    return
  }
  const nextHeight = resolveKeyboardHeight(event)
  if (nextHeight > 0) {
    page.lastReplyKeyboardHeightPx = nextHeight
  }
  if (nextHeight === 0 && page.keepReplyFocusAfterSend) {
    return
  }
  const nextBottom = page.data.replyDockHeightPx + nextHeight
  if (nextHeight === page.data.keyboardHeightPx && nextBottom === page.data.replyListBottomPx) {
    return
  }
  page.setData({
    keyboardHeightPx: nextHeight,
    replyListBottomPx: nextBottom
  }, () => {
    if (nextHeight > 0) {
      scrollReplyListToBottom(page)
    }
  })
}

function onReplyFocus(page, event) {
  if (!page || !page.data) {
    return
  }
  const focusHeight = resolveKeyboardHeight(event) || Number(page.lastReplyKeyboardHeightPx || 0)
  if (focusHeight > 0) {
    page.lastReplyKeyboardHeightPx = focusHeight
  }
  const nextBottom = page.data.replyDockHeightPx + focusHeight
  const updates = {}
  if (!page.data.replyFocused) {
    updates.replyFocused = true
  }
  if (focusHeight !== page.data.keyboardHeightPx || nextBottom !== page.data.replyListBottomPx) {
    updates.keyboardHeightPx = focusHeight
    updates.replyListBottomPx = nextBottom
  }
  if (Object.keys(updates).length > 0) {
    page.setData(updates, () => {
      scrollReplyListToBottom(page)
    })
    return
  }
  scrollReplyListToBottom(page)
}

function onReplyBlur(page) {
  if (!page || !page.data) {
    return
  }
  if (page.keepReplyFocusAfterSend) {
    refocusReplyInput(page)
    return
  }
  page.setData({ replyFocused: false })
  setTimeout(() => {
    if (page.pageUnloaded || !page.data || page.data.replyFocused || page.keepReplyFocusAfterSend) {
      return
    }
    page.setData({
      keyboardHeightPx: 0,
      replyListBottomPx: page.data.replyDockHeightPx
    })
  }, BLUR_RESET_DELAY_MS)
}

function refocusReplyInput(page) {
  if (!page || page.pageUnloaded) {
    return
  }
  if (page.replyRefocusTimer) {
    clearTimeout(page.replyRefocusTimer)
    page.replyRefocusTimer = null
  }
  page.setData({ replyFocused: false }, () => {
    page.replyRefocusTimer = setTimeout(() => {
      page.replyRefocusTimer = null
      if (page.pageUnloaded) {
        return
      }
      page.setData({ replyFocused: true })
    }, REFOCUS_DELAY_MS)
  })
}

function scrollReplyListToBottom(page) {
  if (!page || !page.data) {
    return
  }
  if (page.replyScrollResetTimer) {
    clearTimeout(page.replyScrollResetTimer)
    page.replyScrollResetTimer = null
  }
  if (page.data.replyScrollIntoView) {
    page.setData({ replyScrollIntoView: '' }, () => {
      scrollReplyListToBottom(page)
    })
    return
  }
  page.setData({ replyScrollIntoView: 'reply-bottom-anchor' })
  page.replyScrollResetTimer = setTimeout(() => {
    page.replyScrollResetTimer = null
    if (page.data.replyScrollIntoView) {
      page.setData({ replyScrollIntoView: '' })
    }
  }, 120)
}

function measureReplyDockHeight(page) {
  if (!page || typeof wx === 'undefined') {
    return
  }
  wx.nextTick(() => {
    const query = wx.createSelectorQuery().in(page)
    query.select('#reply-dock').boundingClientRect(rect => {
      if (!rect || !rect.height || !page.data) {
        return
      }
      const nextDockHeight = Math.ceil(rect.height)
      const nextBottom = nextDockHeight + page.data.keyboardHeightPx
      if (nextDockHeight === page.data.replyDockHeightPx && nextBottom === page.data.replyListBottomPx) {
        return
      }
      page.setData({
        replyDockHeightPx: nextDockHeight,
        replyListBottomPx: nextBottom
      })
    }).exec()
  })
}

function cleanupReplyLayout(page) {
  if (!page) {
    return
  }
  if (page.replyScrollResetTimer) {
    clearTimeout(page.replyScrollResetTimer)
    page.replyScrollResetTimer = null
  }
  if (page.replyRefocusTimer) {
    clearTimeout(page.replyRefocusTimer)
    page.replyRefocusTimer = null
  }
}

module.exports = {
  onReplyKeyboardHeightChange,
  onReplyFocus,
  onReplyBlur,
  refocusReplyInput,
  scrollReplyListToBottom,
  measureReplyDockHeight,
  cleanupReplyLayout
}

