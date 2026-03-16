import request from '@/utils/request'

export function fetchDashboardOverview() {
  return request({
    url: '/admin/dashboard/overview',
    method: 'get'
  })
}
