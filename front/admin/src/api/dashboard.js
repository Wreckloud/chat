import request from '@/utils/request'

export function fetchDashboardOverview() {
  return request({
    url: '/admin/dashboard/overview',
    method: 'get'
  })
}

export function fetchDashboardTrend(params) {
  return request({
    url: '/admin/dashboard/trend',
    method: 'get',
    params
  })
}
