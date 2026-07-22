import request from './request'

export function smsLogin(phoneNumber, code) {
  return request.post('/auth/sms-login', { phoneNumber, code })
}

export function register(data) {
  return request.post('/auth/register', data)
}

export function getMe() {
  return request.get('/auth/me')
}

export function sendSmsCode(phoneNumber) {
  return request.post('/sms/send-code', { phoneNumber })
}
