import request from './request'

export function recognize(imageFile) {
  const formData = new FormData()
  formData.append('image', imageFile)
  return request.post('/ocr/recognize', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
