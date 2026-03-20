function normalizeDisplayName(displayName) {
  return String(displayName || '').trim()
}

function buildMentionPrefix(displayName) {
  const normalizedName = normalizeDisplayName(displayName)
  if (!normalizedName) {
    return ''
  }
  return `@${normalizedName} `
}

function prependMention(content, displayName) {
  const normalizedContent = String(content || '').trimStart()
  const mentionPrefix = buildMentionPrefix(displayName)
  if (!mentionPrefix) {
    return normalizedContent.trim()
  }
  if (!normalizedContent) {
    return mentionPrefix
  }
  if (normalizedContent.startsWith(mentionPrefix)) {
    return normalizedContent
  }
  const strippedLeadingMention = normalizedContent.startsWith('@')
    ? normalizedContent.replace(/^@\S+\s*/, '')
    : normalizedContent
  if (!strippedLeadingMention) {
    return mentionPrefix
  }
  return `${mentionPrefix}${strippedLeadingMention}`
}

function removeLeadingMention(content, displayName) {
  const normalizedContent = String(content || '').trim()
  if (!normalizedContent) {
    return ''
  }
  const mentionPrefix = buildMentionPrefix(displayName)
  if (mentionPrefix && normalizedContent.startsWith(mentionPrefix)) {
    return normalizedContent.slice(mentionPrefix.length).trim()
  }
  return normalizedContent.replace(/^@\S+\s*/, '').trim()
}

module.exports = {
  buildMentionPrefix,
  prependMention,
  removeLeadingMention
}
