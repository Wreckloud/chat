function appendMessage(page, message, options = {}) {
  const isValidMessage = typeof options.isValidMessage === 'function'
    ? options.isValidMessage(message)
    : Boolean(message)
  if (!isValidMessage) {
    return false
  }

  const list = Array.isArray(page.data.messages) ? page.data.messages : []
  // 以 messageId 去重，避免 ACK 与推送并发导致重复渲染。
  const isDuplicate = list.some(item => {
    if (typeof options.isDuplicate === 'function') {
      return options.isDuplicate(item, message)
    }
    return Number(item.messageId) === Number(message.messageId)
  })
  if (isDuplicate) {
    return false
  }

  if (typeof options.beforeAppend === 'function') {
    options.beforeAppend(message)
  }

  const messages = [...list, message]
  const updates = { messages }
  if (typeof options.buildMessageBlocks === 'function') {
    updates.messageBlocks = options.buildMessageBlocks(messages)
  }
  page.setData(updates)

  if (typeof options.afterAppend === 'function') {
    options.afterAppend(message, messages)
  }
  return true
}

function loadPagedMessages(page, request, options = {}) {
  if (page.data.loading || !page.data.hasMore) {
    return Promise.resolve()
  }

  const pageNo = Number(page.data.currentPage) || 1
  const pageSize = Number(page.data.pageSize) || 20
  page.setData({ loading: true })

  return request.get(options.url, {
    page: pageNo,
    size: pageSize
  })
    .then(res => {
      const records = typeof options.parseRecords === 'function'
        ? options.parseRecords(res)
        : (((res && res.data && res.data.records) || []))

      const safeRecords = Array.isArray(records) ? records : []
      // 历史接口按倒序分页，前端统一 reverse 后再拼接，保证时间线从旧到新。
      const ordered = safeRecords.slice().reverse()
      const previous = Array.isArray(page.data.messages) ? page.data.messages : []
      const messages = pageNo === 1 ? ordered : [...ordered, ...previous]

      page.setData({
        messages,
        messageBlocks: typeof options.buildMessageBlocks === 'function'
          ? options.buildMessageBlocks(messages)
          : [],
        loading: false,
        hasMore: safeRecords.length >= pageSize,
        currentPage: pageNo + 1
      })

      if (typeof options.onLoaded === 'function') {
        options.onLoaded({
          messages,
          records: safeRecords,
          pageNo
        })
      }

      if (pageNo === 1 && typeof options.onFirstPageLoaded === 'function') {
        options.onFirstPageLoaded(messages)
      }
      return messages
    })
    .catch(error => {
      page.setData({ loading: false })
      if (typeof options.onError === 'function') {
        options.onError(error)
      }
    })
}

module.exports = {
  appendMessage,
  loadPagedMessages
}
