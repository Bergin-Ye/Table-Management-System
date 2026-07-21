import request from './request'

/**
 * OCR 图片识别 — 上传工单图片，返回结构化字段自动填充
 * POST /api/original-record/ocr-recognize
 */
export function recognize(imageFile) {
  const formData = new FormData()
  formData.append('image', imageFile)
  return request.post('/original-record/ocr-recognize', formData)
}
