import http from './http'

export function login(payload) {
  return http.post('/auth/login', payload)
}

export function register(payload) {
  return http.post('/auth/register', payload)
}

export function sendResetPasswordLink(payload) {
  return http.post('/auth/password/reset-link/send', payload)
}

export function getCurrentUser() {
  return http.get('/users/me')
}

export function updateCurrentUserProfile(payload) {
  return http.put('/users/profile', payload)
}

export function changeCurrentUserPassword(payload) {
  return http.put('/users/password', payload)
}

export function sendBindEmailLink(payload) {
  return http.post('/users/email-link/send', payload)
}

export function deactivateCurrentUser() {
  return http.delete('/users/me')
}

export function listConversations() {
  return http.get('/conversations')
}

export function createConversation(targetUserId) {
  return http.post(`/conversations/${targetUserId}`)
}

export function markConversationRead(conversationId) {
  return http.put(`/conversations/${conversationId}/read`)
}

export function listMessages(conversationId, page = 1, size = 50) {
  return http.get(`/conversations/${conversationId}/messages`, { params: { page, size } })
}

export function sendTextMessage(conversationId, content) {
  return http.post(`/conversations/${conversationId}/messages`, {
    content,
    msgType: 'TEXT'
  })
}

export function applyChatImageUploadPolicy(payload) {
  return http.post('/media/chat/image/upload-policy', payload)
}

export function applyChatVideoUploadPolicy(payload) {
  return http.post('/media/chat/video/upload-policy', payload)
}

export function applyChatFileUploadPolicy(payload) {
  return http.post('/media/chat/file/upload-policy', payload)
}

export function applyForumThreadImageUploadPolicy(payload) {
  return http.post('/media/forum/thread/image/upload-policy', payload)
}

export function applyForumThreadVideoUploadPolicy(payload) {
  return http.post('/media/forum/thread/video/upload-policy', payload)
}

export function applyForumReplyImageUploadPolicy(payload) {
  return http.post('/media/forum/reply/image/upload-policy', payload)
}

export function listFeedThreads(tab, page = 1, size = 20) {
  return http.get('/forum/feed', { params: { tab, page, size } })
}

export function searchThreads(keyword, page = 1, size = 20) {
  return http.get('/forum/search', { params: { keyword, page, size } })
}

export function getThreadDetail(threadId) {
  return http.get(`/forum/threads/${threadId}`)
}

export function listThreadReplies(threadId, page = 1, size = 20, sort = 'floor') {
  return http.get(`/forum/threads/${threadId}/replies`, { params: { page, size, sort } })
}

export function createReply(threadId, payload) {
  return http.post(`/forum/threads/${threadId}/replies`, payload)
}

export function createThread(payload) {
  return http.post('/forum/threads', payload)
}

export function saveThreadDraft(payload) {
  return http.post('/forum/threads/drafts', payload)
}

export function getLatestThreadDraft() {
  return http.get('/forum/threads/drafts/latest')
}

export function getEditableThread(threadId) {
  return http.get(`/forum/threads/${threadId}/editable`)
}

export function updateThread(threadId, payload) {
  return http.put(`/forum/threads/${threadId}`, payload)
}

export function publishThreadDraft(threadId, payload) {
  return http.put(`/forum/threads/${threadId}/publish`, payload)
}

export function restoreThread(threadId, payload) {
  return http.put(`/forum/threads/${threadId}/restore`, payload)
}

export function updateThreadLikeStatus(threadId, liked) {
  return http.put(`/forum/threads/${threadId}/like`, { liked: Boolean(liked) })
}

export function deleteThread(threadId) {
  return http.delete(`/forum/threads/${threadId}`)
}

export function purgeThread(threadId) {
  return http.delete(`/forum/threads/${threadId}/purge`)
}

export function deleteReply(replyId) {
  return http.delete(`/forum/replies/${replyId}`)
}

export function updateReplyLikeStatus(replyId, liked) {
  return http.put(`/forum/replies/${replyId}/like`, { liked: Boolean(liked) })
}

export function getUserHome(userId) {
  return http.get(`/users/${userId}/home`)
}

export function listUserThreads(userId, page = 1, size = 20) {
  return http.get(`/users/${userId}/threads`, { params: { page, size } })
}

export function listMyThreads(tab = 'mine', keyword = '', page = 1, size = 20) {
  const params = { tab, page, size }
  const trimmedKeyword = String(keyword || '').trim()
  if (trimmedKeyword) {
    params.keyword = trimmedKeyword
  }
  return http.get('/users/me/threads', { params })
}

export function listMutualFollows() {
  return http.get('/follow/mutual')
}

export function listFollowing() {
  return http.get('/follow/following')
}

export function listFollowers() {
  return http.get('/follow/followers')
}

export function followUser(targetUserId) {
  return http.post(`/follow/${targetUserId}`)
}

export function unfollowUser(targetUserId) {
  return http.delete(`/follow/${targetUserId}`)
}

export function listMyAchievements() {
  return http.get('/users/me/achievements')
}

export function equipTitle(achievementCode) {
  return http.put('/users/me/title/equip', { achievementCode })
}

export function unequipTitle() {
  return http.put('/users/me/title/unequip')
}

export function getNoticeUnreadSummary() {
  return http.get('/notices/unread-summary')
}

export function getNoticeUnreadCount() {
  return http.get('/notices/unread-count')
}

export function listNotices(page = 1, size = 20) {
  return http.get('/notices', { params: { page, size } })
}

export function markNoticeRead(noticeId) {
  return http.put(`/notices/${noticeId}/read`)
}

export function markAllNoticeRead() {
  return http.put('/notices/read-all')
}

export function getLobbyMeta() {
  return http.get('/lobby/meta')
}

export function listLobbyMessages(page = 1, size = 40) {
  return http.get('/lobby/messages', { params: { page, size } })
}
