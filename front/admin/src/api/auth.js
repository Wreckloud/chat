import request from '@/utils/request'

export function loginByPassword(payload) {
  return request({
    url: '/admin/auth/login',
    method: 'post',
    data: payload
  })
}

export function fetchAdminProfile() {
  return request({
    url: '/admin/auth/me',
    method: 'get'
  })
}
