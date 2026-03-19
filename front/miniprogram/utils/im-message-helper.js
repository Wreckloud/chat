const time = require('./time')

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

function resolveMessageIdNumber(message) {
  const messageId = resolveMessageId(message)
  if (messageId == null) {
    return 0
  }
  const parsed = Number(messageId)
  if (!Number.isFinite(parsed) || parsed <= 0) {
    return 0
  }
  return parsed
}

function resolveMessageTimestamp(message) {
  const parsedDate = time.parseDateTime(message && message.createTime)
  if (!parsedDate) {
    return Number.NaN
  }
  return parsedDate.getTime()
}

function compareMessageOrder(left, right) {
  const leftTime = resolveMessageTimestamp(left)
  const rightTime = resolveMessageTimestamp(right)
  const leftTimeValid = Number.isFinite(leftTime)
  const rightTimeValid = Number.isFinite(rightTime)

  if (leftTimeValid && rightTimeValid && leftTime !== rightTime) {
    return leftTime - rightTime
  }
  if (leftTimeValid && !rightTimeValid) {
    return -1
  }
  if (!leftTimeValid && rightTimeValid) {
    return 1
  }

  const leftId = resolveMessageIdNumber(left)
  const rightId = resolveMessageIdNumber(right)
  if (leftId > 0 && rightId > 0 && leftId !== rightId) {
    return leftId - rightId
  }
  if (leftId > 0 && rightId <= 0) {
    return -1
  }
  if (leftId <= 0 && rightId > 0) {
    return 1
  }
  return 0
}

function sortMessagesByOrder(messages) {
  const safeList = Array.isArray(messages) ? messages : []
  if (safeList.length <= 1) {
    return safeList.slice()
  }
  return safeList.slice().sort(compareMessageOrder)
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
  return sortMessagesByOrder(merged)
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
  const upsertOnDuplicate = options.upsertOnDuplicate === true
  const hasBuildMessageBlocks = typeof options.buildMessageBlocks === 'function'

  const findDuplicateIndex = () => list.findIndex(item => {
    if (typeof options.isDuplicate === 'function') {
      return options.isDuplicate(item, message)
    }
    if (messageId != null) {
      return resolveMessageId(item) === messageId
    }
    return Number(item.messageId) === Number(message.messageId)
  })

  // 先走 O(1) messageId 判重，命中后再定位索引做更新或忽略。
  let isDuplicate = messageId != null && messageIdSet.has(messageId)
  let duplicateIndex = -1
  if (isDuplicate || messageId == null || typeof options.isDuplicate === 'function') {
    duplicateIndex = findDuplicateIndex()
    isDuplicate = duplicateIndex >= 0
  }
  if (isDuplicate) {
    if (!upsertOnDuplicate || duplicateIndex < 0) {
      return false
    }
    const nextMessages = list.slice()
    nextMessages[duplicateIndex] = {
      ...nextMessages[duplicateIndex],
      ...message
    }
    const messages = hasBuildMessageBlocks
      ? sortMessagesByOrder(nextMessages)
      : nextMessages
    const updates = { messages }
    if (hasBuildMessageBlocks) {
      updates.messageBlocks = options.buildMessageBlocks(messages)
    } else if (typeof options.appendMessageBlock === 'function') {
      updates.messageBlocks = options.appendMessageBlock({
        messageBlocks: Array.isArray(page.data.messageBlocks) ? page.data.messageBlocks : [],
        message,
        previousMessage: duplicateIndex > 0 ? nextMessages[duplicateIndex - 1] : null,
        messageIndex: duplicateIndex
      })
    }
    page.setData(updates)
    if (typeof options.afterAppend === 'function') {
      options.afterAppend(message, messages, { updated: true })
    }
    return false
  }

  if (typeof options.beforeAppend === 'function') {
    options.beforeAppend(message)
  }

  const messages = hasBuildMessageBlocks
    ? sortMessagesByOrder([...list, message])
    : [...list, message]
  if (messageId != null) {
    messageIdSet.add(messageId)
  }
  const updates = { messages }
  if (hasBuildMessageBlocks) {
    updates.messageBlocks = options.buildMessageBlocks(messages)
  } else if (typeof options.appendMessageBlock === 'function') {
    const previousMessage = list.length > 0 ? list[list.length - 1] : null
    const currentBlocks = Array.isArray(page.data.messageBlocks) ? page.data.messageBlocks : []
    const nextBlocks = options.appendMessageBlock({
      messageBlocks: currentBlocks,
      message,
      previousMessage,
      messageIndex: messages.length - 1
    })
    if (Array.isArray(nextBlocks)) {
      updates.messageBlocks = nextBlocks
    }
  }
  page.setData(updates)

  if (typeof options.afterAppend === 'function') {
    options.afterAppend(message, messages, { updated: false })
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
