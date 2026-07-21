import request from './request'

/**
 * OCR 图片识别 — 上传工单图片，通义千问 Qwen3.5-OCR 提取结构化字段
 * @param {File} image 图片文件
 * @returns {Promise} { fields: {...}, rawText: "...", filledCount: N }
 */
export function recognizeOcr(image) {
  const formData = new FormData()
  formData.append('image', image)
  return request.post('/ocr/recognize', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
