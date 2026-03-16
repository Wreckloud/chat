import request from '@/utils/request'

export function fetchUserPage(params) {
  return request({
    url: '/admin/users',
    method: 'get',
    params
  })
}

export function updateUserStatus(userId, status) {
  return request({
    url: `/admin/users/${userId}/status`,
    method: 'put',
    data: { status }
  })
}

export function createUserBan(userId, payload) {
  return request({
    url: `/admin/users/${userId}/ban`,
    method: 'post',
    data: payload
  })
}

export function liftUserBan(banId) {
  return request({
    url: `/admin/bans/${banId}/lift`,
    method: 'put'
  })
}
