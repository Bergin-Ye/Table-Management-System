import request from './request'

export function getUsers() {
  return request.get('/admin/users')
}

export function updateUserRole(userId, role) {
  return request.put(`/admin/users/${userId}/role`, { role })
}

export function deleteUser(userId) {
  return request.delete(`/admin/users/${userId}`)
}

export function resetUserPassword(userId, password) {
  return request.put(`/admin/users/${userId}/reset-password`, { password })
}
