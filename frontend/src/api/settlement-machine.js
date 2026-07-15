import request from './request'

export function getList(params) {
  return request.get('/settlement-machine', { params })
}

export function getDetail(id) {
  return request.get(`/settlement-machine/${id}`)
}

export function create(data) {
  return request.post('/settlement-machine', data)
}

export function update(id, data) {
  return request.put(`/settlement-machine/${id}`, data)
}

export function remove(id) {
  return request.delete(`/settlement-machine/${id}`)
}

export function batchDelete(ids) {
  return request.post('/settlement-machine/batch-delete', { ids })
}
