const REPLY_PREVIEW_MAX_LENGTH = 60
const replyMentionHelper = require('./reply-mention-helper')

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
  const senderName = String(data.senderName || '').trim()
  const isSelf = Number(data.isSelf) === 1
  const mentionName = !isSelf ? senderName : ''

  return {
    messageId,
    preview,
    mentionName,
    mentionPrefix: replyMentionHelper.buildMentionPrefix(mentionName)
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
  buildReplyDraft,
  buildReplyPayload
}

