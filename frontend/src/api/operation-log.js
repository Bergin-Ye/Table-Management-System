import request from './request'

export function getList(params) {
  return request.get('/operation-log', { params })
}
