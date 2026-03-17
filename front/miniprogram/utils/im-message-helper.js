function resolveMessageId(message) {
  const raw = message ? message.messageId : null
  if (raw == null) {
    return null
  }
  const id = String(raw).trim()
  if (!id || id === '0') {
    return null
  }
  return id
}

function buildMessageIdSet(messages) {
  const idSet = new Set()
  const safeList = Array.isArray(messages) ? messages : []
  for (let index = 0; index < safeList.length; index++) {
    const messageId = resolveMessageId(safeList[index])
    if (messageId != null) {
      idSet.add(messageId)
    }
  }
  return idSet
}

function ensureMessageIdSet(page, messages) {
  if (!page) {
    return new Set()
  }
  if (!(page.messageIdSet instanceof Set)) {
    page.messageIdSet = buildMessageIdSet(messages)
  }
  return page.messageIdSet
}

function mergeUniqueMessages(messages) {
  const safeList = Array.isArray(messages) ? messages : []
  if (safeList.length <= 1) {
    return safeList.slice()
  }

  const merged = []
  const idSet = new Set()
  for (let index = 0; index < safeList.length; index++) {
    const item = safeList[index]
    const messageId = resolveMessageId(item)
    if (messageId == null) {
      merged.push(item)
      continue
    }
    if (idSet.has(messageId)) {
      continue
    }
    idSet.add(messageId)
    merged.push(item)
  }
  return merged
}

function appendMessage(page, message, options = {}) {
  const isValidMessage = typeof options.isValidMessage === 'function'
    ? options.isValidMessage(message)
    : Boolean(message)
  if (!isValidMessage) {
    return false
  }

  const list = Array.isArray(page.data.messages) ? page.data.messages : []
  const messageIdSet = ensureMessageIdSet(page, list)
  const messageId = resolveMessageId(message)

  // 先走 O(1) messageId 判重，避免长会话时每次 append 线性扫描。
  let isDuplicate = messageId != null && messageIdSet.has(messageId)
  if (!isDuplicate) {
    // 自定义判重或无 messageId 场景再回退到遍历判重。
    isDuplicate = list.some(item => {
      if (typeof options.isDuplicate === 'function') {
        return options.isDuplicate(item, message)
      }
      if (messageId != null) {
        return resolveMessageId(item) === messageId
      }
      return Number(item.messageId) === Number(message.messageId)
    })
  }
  if (isDuplicate) {
    return false
  }

  if (typeof options.beforeAppend === 'function') {
    options.beforeAppend(message)
  }

  const messages = [...list, message]
  if (messageId != null) {
    messageIdSet.add(messageId)
  }
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
      const merged = pageNo === 1 ? ordered : [...ordered, ...previous]
      const messages = mergeUniqueMessages(merged)
      page.messageIdSet = buildMessageIdSet(messages)

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
