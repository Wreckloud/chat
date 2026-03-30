import request from '@/utils/request'

export function fetchAiConfig() {
  return request({
    url: '/admin/ai/config',
    method: 'get'
  })
}

export function updateAiConfig(data) {
  return request({
    url: '/admin/ai/config',
    method: 'put',
    data
  })
}

