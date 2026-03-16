import request from '@/utils/request'

export function fetchActionLogPage(params) {
  return request({
    url: '/admin/audit/actions',
    method: 'get',
    params
  })
}

export function fetchLoginLogPage(params) {
  return request({
    url: '/admin/audit/logins',
    method: 'get',
    params
  })
}
