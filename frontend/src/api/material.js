import request from './request'

export function getList(params) {
  return request.get('/material', { params })
}

export function search(keyword) {
  return request.get('/material/search', { params: { keyword } })
}

export function getDetail(id) {
  return request.get(`/material/${id}`)
}

export function create(data) {
  return request.post('/material', data)
}

export function update(id, data) {
  return request.put(`/material/${id}`, data)
}

export function remove(id) {
  return request.delete(`/material/${id}`)
}

export function batchDelete(ids) {
  return request.post('/material/batch-delete', { ids })
}
