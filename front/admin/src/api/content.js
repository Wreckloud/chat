import request from '@/utils/request'

export function fetchThreadPage(params) {
  return request({
    url: '/admin/forum/threads',
    method: 'get',
    params
  })
}

export function fetchReplyPage(params) {
  return request({
    url: '/admin/forum/replies',
    method: 'get',
    params
  })
}

export function updateThreadLock(threadId, locked) {
  return request({
    url: `/admin/forum/threads/${threadId}/lock`,
    method: 'put',
    data: { locked }
  })
}

export function updateThreadSticky(threadId, sticky) {
  return request({
    url: `/admin/forum/threads/${threadId}/sticky`,
    method: 'put',
    data: { sticky }
  })
}

export function updateThreadEssence(threadId, essence) {
  return request({
    url: `/admin/forum/threads/${threadId}/essence`,
    method: 'put',
    data: { essence }
  })
}

export function deleteThread(threadId) {
  return request({
    url: `/admin/forum/threads/${threadId}`,
    method: 'delete'
  })
}

export function deleteReply(replyId) {
  return request({
    url: `/admin/forum/replies/${replyId}`,
    method: 'delete'
  })
}
