const STRANGER_MESSAGE_LIMIT = 3
const REPLY_PREVIEW_MAX_LENGTH = 60

function buildSystemNoticeBlocks(policy) {
  const notices = [
    {
      type: 'system-notice',
      key: 'sys_safety',
      text: '系统提示：以下内容均由用户发布，请注意甄别与财产安全。'
    }
  ]

  if (policy && policy.mutualFollow) {
    notices.push({
      type: 'system-notice',
      key: 'sys_friend',
      text: '系统提示：你们已经是好友了。'
    })
    return notices
  }

  if (policy && policy.canSendFreely) {
    notices.push({
      type: 'system-notice',
      key: 'sys_interaction',
      text: '系统提示：你们已产生互动，可继续聊天。'
    })
    return notices
  }

  if (policy) {
    const limit = Number(policy.strangerMessageLimit) || STRANGER_MESSAGE_LIMIT
    const remaining = Math.max(0, Number(policy.strangerMessageRemaining) || 0)
    notices.push({
      type: 'system-notice',
      key: 'sys_limit',
      text: `系统提示：未互关时单向最多发送 ${limit} 条消息，当前剩余 ${remaining} 条，等待对方回复后解除限制。`
    })
  }

  return notices
}

function prependSystemNoticeBlocks(systemNoticeBlocks, messageBlocks) {
  const notices = Array.isArray(systemNoticeBlocks) ? systemNoticeBlocks : []
  const blocks = Array.isArray(messageBlocks) ? messageBlocks : []
  if (notices.length === 0) {
    return blocks
  }
  return [...notices, ...blocks]
}

function stripSystemNoticeBlocks(messageBlocks) {
  const blocks = Array.isArray(messageBlocks) ? messageBlocks : []
  return blocks.filter(item => item && item.type !== 'system-notice')
}

function buildReplyDraft(dataset) {
  const data = dataset || {}
  const messageId = Number(data.messageId)
  if (!messageId) {
    return null
  }

  const msgType = String(data.msgType || 'TEXT').toUpperCase()
  let preview = String(data.replyToPreview || data.messageContent || '').trim()
  if (!preview) {
    if (msgType === 'IMAGE') {
      preview = '[图片]'
    } else if (msgType === 'VIDEO') {
      preview = '[视频]'
    } else if (msgType === 'FILE') {
      preview = '[文件]'
    } else {
      preview = '[消息]'
    }
  }
  if (preview.length > REPLY_PREVIEW_MAX_LENGTH) {
    preview = `${preview.slice(0, REPLY_PREVIEW_MAX_LENGTH)}...`
  }

  return {
    messageId,
    preview
  }
}

function buildReplyPayload(replyDraft) {
  const draft = replyDraft || null
  if (!draft || !draft.messageId) {
    return {}
  }
  return {
    replyToMessageId: Number(draft.messageId)
  }
}

module.exports = {
  buildSystemNoticeBlocks,
  prependSystemNoticeBlocks,
  stripSystemNoticeBlocks,
  buildReplyDraft,
  buildReplyPayload
}
